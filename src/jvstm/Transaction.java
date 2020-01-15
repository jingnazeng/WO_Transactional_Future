/*
 * JVSTM: a Java library for Software Transactional Memory
 * Copyright (C) 2005 INESC-ID Software Engineering Group
 * http://www.esw.inesc-id.pt
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Author's contact:
 * INESC-ID Software Engineering Group
 * Rua Alves Redol 9
 * 1000 - 029 Lisboa
 * Portugal
 */
package jvstm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;

import benchmark.synthetic.TimerDebug;
import contlib.Continuation;
import jvstm.Transaction;
import jvstm.TransactionalTask;
import jvstm.gc.GCTask;
import jvstm.gc.TxContext;
import jvstm.graph.DAG;
import jvstm.graph.FutureRepresentation;
import jvstm.graph.Node;

public abstract class Transaction {
    // static part starts here

	/*
     * The mostRecentCommittedRecord static field is volatile to ensure correct
     * synchronization among different threads:
     *
     * - A newly created transaction reads the value of this field at
     *   the very beginning of its existence, before trying to
     *   access any box.
     *
     * - A write transaction writes to this field at the very end,
     *   after commiting all the boxes to their new values.
     *
     * This way, because of the new semantics of the Java Memory
     * Model, as specified by JSR133 (which is incorporated in the
     * newest Java Language Specification), we know that all the
     * values written previously in the commit of write transaction
     * will be visible to any other transaction that is created with
     * the new value of the committed field.
     *
     * This change is sufficient to ensure the correct synchronization
     * guarantees, even if we remove all the remaining volatile
     * declarations from the VBox and VBoxBody classes.
     */
    public static volatile ActiveTransactionsRecord mostRecentCommittedRecord = ActiveTransactionsRecord.makeSentinelRecord();

    public static final ThreadLocal<Transaction> current= new ThreadLocal<Transaction>();

    // a per thread TxContext
    private static final ThreadLocal<TxContext> threadTxContext = new ThreadLocal<TxContext>() {
        @Override protected TxContext initialValue() {
            return Transaction.allTxContexts.enqueue(new TxContext(Thread.currentThread()));
        }
    };

    // List of all tx contexts.  The GC thread will iterate this list to GC any unused ActiveTxRecords.
    public static TxContext allTxContexts = null;
    public static final GCTask gcTask; // added by FMC for unit test purpose
    static final String GC_PROP = "jvstm.gc.disabled";

    static {
        // initialize the allTxContexts
        Transaction.allTxContexts = new TxContext(null);

        // start the GC thread.
        boolean gcDisabled = Boolean.getBoolean(GC_PROP);
        Logger logger = Logger.getLogger("jvstm");
//        logger.info(String.format(
//                "********** GC vbodies = %b (disable/enable it in property %s)",
//                !gcDisabled,
//                GC_PROP));
        gcTask = new GCTask(mostRecentCommittedRecord);
        if(!gcDisabled){
            Thread gc = new Thread(gcTask);
            gc.setDaemon(true);
            gc.start();
        }
    }

    private static TransactionFactory TRANSACTION_FACTORY = new DefaultTransactionFactory();

    public static void setTransactionFactory(TransactionFactory factory) {
        TRANSACTION_FACTORY = factory;
    }

    public static Transaction current() {
        return current.get();
    }
    

    public static TxContext context() {
        return Transaction.threadTxContext.get();
    }
    

    // This method is called during the commit of a write transaction.  Even though it is possible
    // for more than one method to write to this slot at the same time, this could only cause a new
    // transaction to see some record that might not be the most recent one.  However, this is ok,
    // because when a transactio begin it will check for another more recent record.
    public static void setMostRecentCommittedRecord(ActiveTransactionsRecord record) {
        mostRecentCommittedRecord = record;
    }

    public static void addTxQueueListener(TxQueueListener listener) {
        ActiveTransactionsRecord.addListener(listener);
    }

    public static boolean isInTransaction() {
        return current.get() != null;
    }

    public static ActiveTransactionsRecord getRecordForNewTransaction() {
        ActiveTransactionsRecord rec = Transaction.mostRecentCommittedRecord;

        TxContext ctx = threadTxContext.get();
        ctx.oldestRequiredVersion = rec; // volatile write

        while (true) {
            while ((rec.getNext() != null) && (rec.getNext().isCommitted())) {
                rec = rec.getNext();
            }
            if (rec != ctx.oldestRequiredVersion) {
                // a more recent record exists, so backoff and try again with the new one
                ctx.oldestRequiredVersion = rec; // volatile write
            } else {
                return rec;
            }
        }
    }

    /** Warning: this method has limited usability.  See the UnsafeSingleThreaded class for
     * details */
    public static Transaction beginUnsafeSingleThreaded() {
        Transaction parent = current.get();
        if (parent != null) {
            throw new Error("Unsafe single-threaded transactions cannot be nested");
        }

        ActiveTransactionsRecord activeRecord = getRecordForNewTransaction();
        Transaction tx = new UnsafeSingleThreadedTransaction(activeRecord);
        tx.start();
        return tx;
    }

    public static Transaction beginInevitable() {
        Transaction parent = current.get();
        if (parent != null) {
            throw new Error("Inevitable transactions cannot be nested");
        }

        ActiveTransactionsRecord activeRecord = getRecordForNewTransaction();
        Transaction tx = new InevitableTransaction(activeRecord);
        tx.start();
        return tx;
    }
    
    public static Transaction begin() {
        return begin(false);
    }

    public static Transaction begin(boolean readOnly) {
        ActiveTransactionsRecord activeRecord = null;
        Transaction parent = current();

        if (TRANSACTION_FACTORY.reuseTopLevelReadOnlyTransactions() && parent == null && readOnly) {
            Transaction tx = getRecordForNewTransaction().tx;
            tx.start();
            return tx;
        }

        if (parent == null) {
            activeRecord = getRecordForNewTransaction();
        }

        return beginWithActiveRecord(readOnly, activeRecord);
    }

    // activeRecord may be null, iff the parent is also null, in which case activeRecord is not used, so it's ok!
    protected static Transaction beginWithActiveRecord(boolean readOnly, ActiveTransactionsRecord activeRecord) {
        Transaction parent = current.get();
        Transaction tx = null;

        if (parent == null) {
            if (readOnly) {
                tx = TRANSACTION_FACTORY.makeReadOnlyTopLevelTransaction(activeRecord);
            } else {
                tx = TRANSACTION_FACTORY.makeTopLevelTransaction(activeRecord);
            }
        } else {
            // passing the readOnly parameter to makeNestedTransaction is a temporary solution to
            // support the correct semantics in the composition of @Atomic annotations.  Ideally, we
            // should adjust the code generation of @Atomic to let WriteOnReadExceptions pass to the
            // parent
        	//added; makes a continuation
        	//tx = parent.makeParallelNestedTransaction(readOnly);
        	// make a subtransaction supporting weak ordering txn
        	  tx = parent.makeWeakOrderingTransactionalFuture(readOnly);
//            tx = parent.makeNestedTransaction(readOnly);
        }
        tx.start();

        return tx;
    }

    /* When this method succeeds, it commits the current transaction and begins another in a consistent state with the
     * previous transaction, i.e., if the committing transaction is read-only, then the next transaction begins in the
     * same version that the read-only transaction used; if the committing transaction is read-write and the commit
     * succeeds, the new transaction begins in the version that the read-write transaction produced.
     */
    public static Transaction commitAndBegin(boolean readOnly) {
        Transaction tx = current.get();
        return tx.commitAndBeginTx(readOnly);
    }

    public static Transaction beginParallelNested(boolean readOnly, int seqNumber) {
    		
        Transaction parent = current.get();
        Transaction tx = parent.makeParallelNestedTransaction(readOnly,seqNumber);
        tx.start();
        return tx;
    }
    
    private Node associatedNode;

    /**
     * @return the associatedNode
     */
    public Node getAssociatedNode() {
    	return associatedNode;
    }

    /**
     * @param associatedNode the associatedNode to set
     */
    public void setAssociatedNode(Node associatedNode) {
    	this.associatedNode = associatedNode;
    }

    public static Transaction startWeakOrderingTransactionalFuture(boolean readOnly, int seqNumber) {
		
        Transaction parent = current.get();
        Transaction tx = parent.makeWeakOrderingTransactionalFuture(readOnly);
        tx.start();
        return tx;
    }

    public static Transaction beginUnsafeMultithreaded() {
        Transaction parent = current.get();
        Transaction tx = parent.makeUnsafeMultithreaded();
        tx.start();
        return tx;
    }

    public static void initThreadPool(int numberThreads) {
        nestedParPool = Executors.newFixedThreadPool(numberThreads, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }

    public static void initCachedThreadPool() {
        nestedParPool = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }

    protected static ExecutorService nestedParPool = Executors.newFixedThreadPool(Runtime.getRuntime()
            .availableProcessors() * 2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    public <E> Future<E> manageNestedParallelTxs(TransactionalTask<E> callable) {
        return manageNestedParallelTxs(callable, nestedParPool);
    }

    protected boolean flattenNested = false;

    public <E> Future<E> manageNestedParallelTxs(TransactionalTask<E> callable, ExecutorService threadPool) {
    	callable.seqNumber = this.sequentialVersion+1;
    	//added
    	return threadPool.submit(callable);
    }
    
    public <E> Future<E> submitReadOnlyWeakOrderingFuture(
    		TransactionalTask<E> callable) {
    	return manageNestedParallelTxs(callable, nestedParPool);
	}
    
    public <E> Future<E> submitStrongOrderingFuture(TransactionalTask<E> callable) {
    	 return manageNestedParallelTxs(callable, nestedParPool);
    }
    
    public <E> FutureRepresentation<E> submitWeakOrderingFuture(TransactionalTask<E> callable) {
    	
    	// internal commit the sub-transaction before at future submission point
    	DAG dagFortxn = this.getTopLevelTrasanction().getDAG();
    	dagFortxn.iCommitContinuationUponSubmitFuture(this);
    	
    	TimerDebug.startFuture((int) (Thread.currentThread().getId()%56));
    	
    	dagFortxn.getGraphLock().writeLock();
    	Node future_node ;
    	Future<E> f;
//    	synchronized(dagFortxn){
    	future_node = dagFortxn.futureSubmission(this);
    	//run the "future" operations in the context of a parallel nesting transaction
    	f = manageNestedParallelTxs(callable, nestedParPool);
    	//literally start a parallel transactions as continuation
    	
    	//associate the continuation sub-transactions with corresponding nodes in DAG
    	Transaction coninuationTxn = Transaction.begin();
   // 	System.out.println("continuation transaction, should be T2: "+ coninuationTxn);
    	Node conti_node = dagFortxn.getDirectChildrenNode(this.getAssociatedNode()).getConti();
    	conti_node.setAsscociatedTxn(coninuationTxn);
    	coninuationTxn.setAssociatedNode(conti_node);
//    	}
    	dagFortxn.getGraphLock().writeUnlock();
    	return  new FutureRepresentation<E>(f,future_node);
    }
    
 
    
    public static void abort() {
        Transaction tx = current.get();
//        System.out.println("abort 1 : "+ tx);
        tx.abortTx();
    }

    public static void commit() {
        Transaction tx = current.get();
        //added
        while(tx != null) {
        	tx.commitTx(true);
        	tx = current.get();	
        }

    }

    public static void checkpoint() {
        Transaction tx = current.get();
        tx.commitTx(false);
    }

    public static SuspendedTransaction suspend() {
        return current.get().suspendTx();
    }

    public static Transaction resume(SuspendedTransaction suspendedTx) {
        if (current.get() != null) {
            throw new ResumeException("Can't resume a transaction into a thread with an active transaction already");
        }

        // In the previous lines I'm checking that the current thread
        // has no transaction, because, otherwise, we would lose the
        // current transaction.
        //
        // Likewise, I should not allow that the same transaction is
        // associated with more than one thread.  For that, however, I
        // would have to keep track of which thread owns each
        // transaction and change that atomically.  I recall having
        // the thread in each transaction but I removed sometime ago.
        // So, until I investigate this further, whoever is using this
        // resume stuff must be carefull, because the system will not
        // detect that the same transaction is being used in two
        // different threads.

        TxContext currentTxContext = context();
        currentTxContext.oldestRequiredVersion = suspendedTx.txContext.oldestRequiredVersion;
        /* NOTE: we CANNOT set
         * suspendedTx.txContext.oldestRequiredVersion = null;
         *
         * Doing so would allow the GCTask to miss out on this version, by seeing null in the oldestRequiredVersion of
         * both the currentTxContext and the suspendedTx.txContext.  The TxContext in suspendedTx will be removed from
         * the list when the now resumed transaction becomes GCed.
         */

        // set the transaction in this thread
        current.set(suspendedTx.theTx);
        // return the resumed transaction
        return suspendedTx.theTx;
    }

    // the transaction version that is used to read boxes. Must always represent a consistent state
    // of the world
    protected int number;
    protected final Transaction parent;
    /*
     * This orec identifies newly created objects by this transaction.
     * We say these objects are in captured memory corresponding to memory
     * allocated inside a transaction that cannot escape (i.e., is captured
     * by) its allocating transaction.
     * Later and before performing an STM barrier we can check if an object is
     * in captured memory and in that case we can read or update it in place
     * avoiding the STM barrier.
     */
    public final OwnershipRecord orecForNewObjects = new OwnershipRecord();
    //added
    // sequentialVersion should be seqID in the master thesis
    public int sequentialVersion = 0;
    protected volatile ArrayList<Integer> sequentialRWVersion = new ArrayList<Integer>();
    //added
    // seqClock is an integer that represetns the seqID of the last child that has committed 
    protected volatile int seqClock = 0;
    protected volatile Object toReexecute = null;
    protected volatile boolean earlyAbort = false;
    
    //added
    protected Continuation speculationCheckpoint = null;
    protected Continuation nextSpeculationCheckpoint = null;
    protected Object asyncMethod = null;
    
    
    
    
    //added
    public static void setNextCheckpoint(Continuation c){
    	Transaction tx = Transaction.current.get();
    	tx.nextSpeculationCheckpoint = c;
    }
    
    //added
    public void setAsyncMethod(Object c){
    	asyncMethod = c;
    }
    
    //added
    protected void resetSpeculationState(){
        sequentialVersion = 0;
        seqClock = 0;
        toReexecute = null;
    }
    
    public Transaction(Transaction parent, int number) {
        this.parent = parent;
        this.number = number;
    }

    public Transaction(int number) {
        this(null, number);
    }

    public Transaction(Transaction parent) {
        this(parent, parent.getNumber());
    }

    public void start() {
        current.set(this);
    }

    protected Transaction getParent() {
        return parent;
    }

    public int getNumber() {
        return number;
    }

    protected void setNumber(int number) {
        this.number = number;
    }

    public void abortTx() {
        finishTx();
    }

    public void commitTx(boolean finishAlso) {
        doCommit();

        if (finishAlso) {
            finishTx();
        }
    }

    private void finishTx() {
        finish();
        current.set(this.getParent());
    }

    protected void finish() {
        // intentionally empty
    }

    public SuspendedTransaction suspendTx() {
        // remove the transaction from the thread
        current.set(null);

        TxContext newTxContext = new TxContext(this);
        // create a new SuspendedTransaction holding the transaction and its context.
        SuspendedTransaction suspendedTx = new SuspendedTransaction(this, newTxContext);
        // enqueue the new TxContext to hold the transaction's required record
        Transaction.allTxContexts.enqueue(newTxContext);
        TxContext currentTxContext = context();
        // the order is important! We must not let go of the required version, so we set it ahead before clearing it in
        // the current context
        newTxContext.oldestRequiredVersion = currentTxContext.oldestRequiredVersion;
        currentTxContext.oldestRequiredVersion = null;

        return suspendedTx;
        // the currentTxContext is left to be reused by this thread
    }

    protected abstract Transaction commitAndBeginTx(boolean readOnly);

    public abstract Transaction makeNestedTransaction(boolean readOnly);
    
    //added
    public abstract Transaction makeParallelNestedTransaction(boolean readOnly,int seqNumber);
    
    public abstract Transaction makeWeakOrderingTransactionalFuture(boolean readOnly);

    public abstract <T> T getBoxValue(VBox<T> vbox);

    public abstract <T> void setBoxValue(VBox<T> vbox, T value);

    public abstract <T> T getPerTxValue(PerTxBox<T> box, T initial);

    public abstract <T> void setPerTxValue(PerTxBox<T> box, T value);

    public abstract <T> T getArrayValue(VArrayEntry<T> entry);

    public abstract <T> void setArrayValue(VArrayEntry<T> entry, T value);

    protected abstract void doCommit();

    public abstract Transaction makeUnsafeMultithreaded();

    public abstract Transaction makeParallelNestedTransaction(boolean readOnly);

    public abstract boolean isWriteTransaction();

    public static void transactionallyDo(TransactionalCommand command) {
        while (true) {
            Transaction tx = Transaction.begin();
            try {
                command.doIt();
                tx.commit();
                tx = null;
                return;
            } catch (CommitException ce) {
                tx.abort();
                tx = null;
            } finally {
                if (tx != null) {
                    tx.abort();
                }
            }
        }
    }

    public static <T> T doIt(Callable<T> xaction) throws Exception {
        return doIt(xaction, false);
    }

    public static <T> T doIt(Callable<T> xaction, boolean tryReadOnly) throws Exception {
        T result = null;
        while (true) {
            Transaction.begin(tryReadOnly);
            boolean finished = false;
            try {
                result = xaction.call();
                Transaction.commit();
                finished = true;
                return result;
            } catch (CommitException ce) {
                Transaction.abort();
                finished = true;
            } catch (WriteOnReadException wore) {
                Transaction.abort();
                finished = true;
                tryReadOnly = false;
            } finally {
                if (! finished) {
                    Transaction.abort();
                }
            }
        }
    }
    
    static public int futuresAborted=0;
    static public int interFuturesAborted =0;

    static public int getFuturesAborts() {
		return futuresAborted;
	}
    static public int getInterFuturesAborts() {
		return interFuturesAborted;
	}
	
    static public synchronized void incFuturesAborts() {
		futuresAborted++;
	}
	
    static public synchronized void incInterFuturesAborts() {
		interFuturesAborted++;
	}
    
    //get top level transaction in order to get the corresponding DAG instance
    public TopLevelTransaction getTopLevelTrasanction(){
    	Transaction parentTxn;
    	if(this.parent == null )
    		return (TopLevelTransaction) this;
    	else
    		parentTxn = this.parent;
    	while(parentTxn.parent!=null){
    		parentTxn = parentTxn.parent;
    	}
    	return (TopLevelTransaction) parentTxn;
    }

    public abstract <V> V evalWeakOrderingFuture(FutureRepresentation<V> tf) throws InterruptedException, ExecutionException;


}
