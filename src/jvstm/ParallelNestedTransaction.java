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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import contlib.Continuation;
import jvstm.util.Cons;

/**
 * Parallel Nested Transaction is used to represent a part of a transaction that is
 * running (potentially) in parallel with other subparts of the same
 * transaction. The programmer is responsible for identifying the parts of a
 * transaction that he wants to run concurrently. Consequently, those parts may
 * not run in program order. The only guarantee is that their execution will be
 * equivalent to some sequential order (plus the properties of opacity). If that
 * guarantee is already provided by the disjoint accesses of each subpart,
 * consider using UnsafeParallelTransaction.
 *
 * @author nmld
 *
 */
public class ParallelNestedTransaction extends ReadWriteTransaction {

    /**
	 * @return the globalReads
	 */
	public Cons<ReadBlock> getGlobalReads() {
		return globalReads;
	}

	/**
	 * @param globalReads the globalReads to set
	 */
	public void setGlobalReads(Cons<ReadBlock> globalReads) {
		this.globalReads = globalReads;
	}

	protected static final ExecuteParallelNestedTxSequentiallyException EXECUTE_SEQUENTIALLY_EXCEPTION = new ExecuteParallelNestedTxSequentiallyException();
	protected static final ReleaseWriteLockErrorException RELEASE_WRITELOCK_ERROR_EXCEPTION = new ReleaseWriteLockErrorException();
	
    protected ThreadLocal<AtomicInteger> blocksFree = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    protected ThreadLocal<Cons<ReadBlock>> blocksPool = new ThreadLocal<Cons<ReadBlock>>() {
        @Override
        protected Cons<ReadBlock> initialValue() {
            return Cons.empty();
        }
    };

    protected Cons<ReadBlock> globalReads;
    protected Map<VBox, InplaceWrite> nestedReads;
    //debug variables
    
    protected HashSet<VBox> boxesWrittenInPlace = new HashSet<VBox>();
    //public double sequentialOrderWaitTime = 0;
    //public double validationTime = 0;
    //public double failedEnqueus = 0;
    
    public ParallelNestedTransaction(ReadWriteTransaction parent){
    	super(parent);
    }

    public ParallelNestedTransaction(ReadWriteTransaction parent, int sequentialVersion,Continuation c) {
        super(parent);
        //added
        this.sequentialVersion = sequentialVersion;
        this.speculationCheckpoint = c;

        int[] parentVers = parent.ancVersions;
        super.ancVersions = new int[parentVers.length + 1];
        getParentVersions();
        
        this.nestedReads = new HashMap<VBox, InplaceWrite>();
        this.globalReads = Cons.empty();
        this.boxesWritten = parent.boxesWritten;
        treeRoot = getTopLevelParent();
    }

    public ParallelNestedTransaction(ReadWriteTransaction parent, boolean multithreaded) {
        super(parent);
        super.ancVersions = EMPTY_VERSIONS;
        this.nestedReads = ReadWriteTransaction.EMPTY_MAP;
        this.globalReads = Cons.empty();
        treeRoot = getTopLevelParent();
    }

    @Override
    public Transaction makeUnsafeMultithreaded() {
        throw new Error("An Unsafe Parallel Transaction may only be spawned by another Unsafe or a Top-Level transaction");
    }

    @Override
    public Transaction makeNestedTransaction(boolean readOnly) {
        throw new Error(
                "A Parallel Nested Transaction cannot spawn a Linear Nested Transaction yet. Consider using a single Parallel Nested Transaction instead.");
    }

    @Override
    protected Transaction commitAndBeginTx(boolean readOnly) {
        commitTx(true);
        return beginWithActiveRecord(readOnly, null);
    }

    // Returns -2 if self; -1 if not anc; >= 0 as version on anc otherwise
    protected int retrieveAncestorVersion(Transaction tx) {
        if (tx == this)
            return -2;

        int i = 0;
        Transaction nextParent = parent;
        while (nextParent != null) {
            if (nextParent == tx) {
                return ancVersions[i];
            }
            nextParent = nextParent.parent;
            i++;
        }
        return -1;
    }

    private Transaction retrieveLowestCommonAncestor(Transaction tx) {
        Transaction current = tx;
        while (current != null) {
            if (retrieveAncestorVersion(current) >= 0) {
                return current;
            }
            current = current.parent;
        }
        return null;
    }

    @Override
    public void abortTx() {    	
        if (this.orec.version != OwnershipRecord.ABORTED) {
            manualAbort();
        }
        
    	Transaction.current.set(getRWParent());
        
    	if(this.sequentialVersion %2 == 0) {
    		waitInQueue(); //otherwise futures will get nullpointer exceptions when accessing their parents
    		ReadWriteTransaction toAbort = this.getRWParent();
        	ReadWriteTransaction root = getTopLevelParent();
        	
        	while(toAbort != root){
        		((ParallelNestedTransaction) toAbort).manualAbort();
        		toAbort = toAbort.getRWParent();
        		Transaction.current.set(toAbort);
        	}
        	toAbort.abort();
        	return;
    	}
    }

    protected void manualAbort() {//added
        
    	//we have to revert overwrites because some other transaction from another tree might wanna adquire ownership
    	//but we also need to revert our writes that are not overwrites, for example if f1 and c3 wrote and c3 aborts, c3 must revert so that t0 commits f1 write.
        for (VBox vbox : boxesWrittenInPlace) {
            revertOverwrite(vbox);
        }

        this.orec.version = OwnershipRecord.ABORTED; 
    	//for vboxes to which only this transaction or its children were the only ones of this tree to write
        for (ReadWriteTransaction child : mergedTxs) {
            child.orec.version = OwnershipRecord.ABORTED;
        }

        super.boxesWritten = null;

        int i = 0;
        for (ReadBlock block : globalReads) {
            block.free = true;
            i++;
        }
        blocksFree.get().addAndGet(i);

        this.globalReads = null;
        this.nestedReads = null;
        super.mergedTxs = null;
    }
    

    protected void revertOverwrite(VBox vboxWritten) {
        InplaceWrite write = vboxWritten.inplace;
        if (write.orec.owner != this) {
            return;
        }
        InplaceWrite overwritten = write;
        while (overwritten.next != null) {
            overwritten = overwritten.next;
            if (overwritten.orec.owner != this && overwritten.orec.version == OwnershipRecord.RUNNING) {
                write.tempValue = overwritten.tempValue;
                write.next = overwritten.next;
                overwritten.orec.owner = overwritten.orec.owner; // enforce
                                                                 // visibility
                write.orec = overwritten.orec;
                return;
            }
        }
    }

    protected <T> T readGlobal(VBox<T> vbox) {
        VBoxBody<T> body = vbox.body;
        
        if (body.version > number) {
        	if(this.sequentialVersion %2 == 0) {
        		waitInQueue(); //otherwise futures will get nullpointer exceptions when accessing their aprents
//        		earlyAbort(); //let it trow the exception and the user will call abort
        	}
        	else {
        		if(this.sequentialVersion !=1)
        		waitInQueue();//otherwise the last future will allow the last continuation to proceed and abort all other continuations
        						//before other previous futures finish
        		
        		getRWParent().earlyAbort =true;
        	}
        	TransactionSignaller.SIGNALLER.signalEarlyAbort();
        }
        //body can actually be null which cause a crash in jvstm

        ReadBlock readBlock = null;
        if (next < 0) {
            if (blocksFree.get().get() > 0) {
                for (ReadBlock poolBlock : blocksPool.get()) {
                    if (poolBlock.free) {
                        poolBlock.free = false;
                        readBlock = poolBlock;
                        blocksFree.get().decrementAndGet();
                        break;
                    }
                }
            } else {
                readBlock = new ReadBlock(blocksFree.get());
            }
            next = 999;
            globalReads = globalReads.cons(readBlock);
        } else {
            readBlock = globalReads.first();
        }
        readBlock.entries[next--] = vbox;
        return body.value;
    }

//    private void earlyAbort() {
//    	ReadWriteTransaction toAbort = this;
//    	ReadWriteTransaction root = getTopLevelParent();
//    	
//    	while(toAbort != root){
//    		((ParallelNestedTransaction) toAbort).abort();
//    		toAbort = toAbort.getRWParent();
//    	}
//		
//	}

	@Override
    public <T> T getBoxValue(VBox<T> vbox) {
        InplaceWrite<T> inplaceWrite = vbox.inplace;
        T value = inplaceWrite.tempValue;
        OwnershipRecord currentOwner = inplaceWrite.orec;
        if (currentOwner.version > 0 && currentOwner.version <= number) {
            value = readGlobal(vbox);
            return value;
        }

        while(true){
        	if(currentOwner.owner == this && currentOwner.version != OwnershipRecord.ABORTED){
        		//we dont add to our read-set since we dont need to validate what we wrote
        		value = inplaceWrite.tempValue;

        		return (value == NULL_VALUE) ? null : value;
        	}
        	else if(retrieveAncestorVersion(currentOwner.owner)>=0 && currentOwner.txTreeVersion <= retrieveAncestorVersion(currentOwner.owner)){
        		nestedReads.put(vbox, inplaceWrite); 
        		value = inplaceWrite.tempValue;
        		return (value == NULL_VALUE) ? null : value;
        	}

            inplaceWrite = inplaceWrite.next;
            if(inplaceWrite == null)
            	break;
            
            currentOwner = inplaceWrite.orec;
            continue;
        }
        if (boxesWritten != EMPTY_MAP) {
            value = (T) boxesWritten.get(vbox);
            if (value != null) {
                return (value == NULL_VALUE) ? null : value;
            }
        }
        return readGlobal(vbox);

    }

    protected Object[] executeSequentially(){
    	ReadWriteTransaction son = this;
    	ReadWriteTransaction parent = getRWParent();
    	Object[] returnValues = new Object[2];
    	returnValues[0] = null;
    	returnValues[1] = null;

    	while(parent != null){    
    		if(parent.toReexecute != null){
    			returnValues[0] = parent.toReexecute;
    			returnValues[1] = son;
    		}
    		son = parent;
    		parent = parent.getRWParent();
    	}	
    	return returnValues;
    }
    
    protected void abortSpeculationTillCheckpoint(ReadWriteTransaction toCommit){
    	ReadWriteTransaction toAbort = this;
    	
    	while(toAbort != toCommit){
    		((ParallelNestedTransaction) toAbort).manualAbort();
    		toAbort = toAbort.getRWParent();
    	}
    	Transaction.current.set(toCommit);
    }
    
    //added
    protected void earlyCommit(){ 
    	tryCommit();
        boxesWritten = null;
        perTxValues = EMPTY_MAP;
        mergedTxs = null;
        Transaction.current.set(this.getRWParent());
    }

    
    protected boolean commitSpeculationTillCheckpoint(Object[] checkpoint){
    	ReadWriteTransaction toCommit = (ReadWriteTransaction) checkpoint[1];
    	ReadWriteTransaction root = getTopLevelParent();
    	Callable asyncMethod = (Callable) checkpoint[0];
    	
    	if(checkpoint[0] != null){
    		toCommit = toCommit.getRWParent();
    		abortSpeculationTillCheckpoint(toCommit);
    	}

    	while(toCommit != root){
			((ParallelNestedTransaction) toCommit).earlyCommit();
			toCommit = toCommit.getRWParent();
		}
    	root.resetSpeculationState();
    	
    	if(checkpoint[0] != null){
    		if(asyncMethod instanceof Runnable){
    			Runnable run = (Runnable) asyncMethod;
    			run.run();
    		}
    		else{
    			Callable call = (Callable) asyncMethod;//dirty, client app doesnt receive the future return
    			try {
					call.call();
				} catch(EarlyAbortException e) {
					TransactionSignaller.SIGNALLER.signalEarlyAbort();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
    		}
			root.seqClock = 1;
    		Continuation.resume(((ReadWriteTransaction) checkpoint[1]).speculationCheckpoint);
    	}
    	return true;
    }
    	
    protected <T> void ownedbyAnotherTree(jvstm.VBox<T> vbox,T value) {
    	if(this.sequentialVersion % 2 == 0){
    		while(parent.seqClock != this.sequentialVersion - 1){}
    		Object[] checkpoint = executeSequentially();
    		if(checkpoint[1] == null) {checkpoint[1] = this;}
    		if(commitSpeculationTillCheckpoint(checkpoint)) {
    			vbox.put(value);
    		}
    	}
    	else{
    		if(this.sequentialVersion != 1) {
    			ReadWriteTransaction grandParent = (ReadWriteTransaction) this.parent.parent;
    			while(grandParent.seqClock != this.sequentialVersion - 2){}
    		}
    		getRWParent().toReexecute = this.asyncMethod;
    		throw EXECUTE_SEQUENTIALLY_EXCEPTION;
    	}
    }
    
    
    
    
    @Override
    public <T> void setBoxValue(jvstm.VBox<T> vbox, T value) {
        InplaceWrite<T> inplaceWrite = vbox.inplace;
        OwnershipRecord currentOwner = inplaceWrite.orec;
        if (currentOwner.owner == this) { // we are already the current writer
            inplaceWrite.tempValue = (value == null ? (T) NULL_VALUE : value);
            return;
        }

        if (currentOwner.version !=0 && currentOwner.version <= number) {
        	if (inplaceWrite.CASowner(currentOwner, this.orec)) {
                inplaceWrite.tempValue = (value == null ? (T) NULL_VALUE : value);
//                boxesWrittenInPlace = boxesWrittenInPlace.cons(vbox);
                boxesWrittenInPlace.add(vbox);
                return;
            }
        	inplaceWrite = vbox.inplace;
            currentOwner = inplaceWrite.orec;
        }
        
        if(currentOwner.owner.treeRoot != this.treeRoot && currentOwner.owner != this.treeRoot){
        	Transaction.incInterFuturesAborts();
        	ownedbyAnotherTree(vbox,value);
        	
        	return;
        }

        while(true){

        	if(currentOwner.owner.sequentialVersion < this.sequentialVersion){
        		if (vbox.CASinplace(inplaceWrite, new InplaceWrite<T>(this.orec, value, inplaceWrite))){
//        			boxesWrittenInPlace = boxesWrittenInPlace.cons(vbox);
        			boxesWrittenInPlace.add(vbox);
                    return;
                }
                inplaceWrite = vbox.inplace;
            	currentOwner = inplaceWrite.orec;
                continue;
        	}
        	while(true){
           		InplaceWrite<T> nextInplaceWrite = inplaceWrite.next;
           		OwnershipRecord nextOwner = null;
   		
        		while(nextInplaceWrite != null && (nextOwner = nextInplaceWrite.orec).owner.sequentialVersion > this.sequentialVersion){
        				inplaceWrite = nextInplaceWrite;
        				currentOwner = inplaceWrite.orec;
        				nextInplaceWrite = inplaceWrite.next;
        		}
	        	
        		if(nextInplaceWrite != null && nextOwner.owner == this){
        			nextInplaceWrite.tempValue = value;
        			nextInplaceWrite.orec = this.orec;
        			return;
        		}

            	if(currentOwner.owner == this) {//when "this" is the first of the list
            		inplaceWrite.tempValue = value;
            		inplaceWrite.orec = this.orec;	
        			return;
            	}
	        	if(inplaceWrite.CASnext(nextInplaceWrite, new InplaceWrite<T>(this.orec, (value == null ? (T) NULL_VALUE : value),nextInplaceWrite))){
//	        		boxesWrittenInPlace = boxesWrittenInPlace.cons(vbox);
	        		boxesWrittenInPlace.add(vbox);
	        		return;
	        	}
        	}
        }
    }

    /*
     * VArrays:
     * Here we ensure that the local array read over ancestors is consistent with concurrent nested commits
     * This procedure is blocking, accordingly to the support provided to VArrays.
     */
    @Override
    protected <T> T getLocalArrayValue(VArrayEntry<T> entry) {
        if (this.arrayWrites != EMPTY_MAP) {
            VArrayEntry<T> wsEntry = (VArrayEntry<T>) this.arrayWrites.get(entry);
            if (wsEntry != null) {
                return (wsEntry.getWriteValue() == null ? (T) NULL_VALUE : wsEntry.getWriteValue());
            }
        }

        ReadWriteTransaction iter = getRWParent();
        while (iter != null) {
            synchronized (iter) {
                if (iter.arrayWrites != EMPTY_MAP) {
                    VArrayEntry<T> wsEntry = (VArrayEntry<T>) iter.arrayWrites.get(entry);
                    if (wsEntry == null) {
                        iter = iter.getRWParent();
                        continue;
                    }

                    if (wsEntry.nestedVersion <= retrieveAncestorVersion(iter)) {
                        this.arraysRead = this.arraysRead.cons(entry);
                        entry.setReadOwner(iter);
                        return (wsEntry.getWriteValue() == null ? (T) NULL_VALUE : wsEntry.getWriteValue());
                    } else {
                        TransactionSignaller.SIGNALLER.signalCommitFail(iter);
                    }
                }
            }
            iter = iter.getRWParent();
        }

        return null;
    }

    /*
     * Both parallel nested transactions and perTxBoxes may be seen as alternatives to work
     * around inherently-conflicting workloads. An important question may be posed if we put
     * them together: when should a perTxBox be committed, if write by a parallel nested
     * transaction? Is it solving a conflict at top-level, or nested level of parallelism?
     *
     * Should the need for perTxBoxes arise in parNesting, that question shall have to be addressed.
     */
    @Override
    public <T> T getPerTxValue(PerTxBox<T> box, T initial) {
        throw new RuntimeException("Parallel Nested Transactions do not support PerTxBoxes");
    }

    @Override
    public <T> void setPerTxValue(PerTxBox<T> box, T value) {
        throw new RuntimeException("Parallel Nested Transactions do not support PerTxBoxes");
    }

    @Override
    protected void finish() {
        boxesWritten = null;
        perTxValues = null;
        mergedTxs = null;
    }

    @Override
    protected void doCommit() {
    	canCommit();
        tryCommit();
        boxesWritten = null;
        perTxValues = EMPTY_MAP;
        mergedTxs = null;
    }
    
    protected void getParentVersions(){
    	int size = ancVersions.length;
    	int iter =0;
    	ReadWriteTransaction parent = (ReadWriteTransaction) this.parent;
    	
    	while(iter < size){
    		super.ancVersions[iter] = parent.nestedCommitQueue.commitNumber;
    		
    		iter += 1;
    		parent = (ReadWriteTransaction)  parent.parent;
    	}
    	
    }

    @Override
    protected void cleanUp() {
        boxesWrittenInPlace = null;
        nestedReads = null;

        for (ReadBlock block : globalReads) {
            block.free = true;
            block.freeBlocks.incrementAndGet();
        }
        globalReads = null;

    }

    protected NestedCommitRecord helpCommitAll(NestedCommitRecord start) {
        NestedCommitRecord lastSeen = start;
        NestedCommitRecord current = lastSeen.next.get();
        while (current != null) {
            if (!current.recordCommitted) {
                current.helpCommit();
            }
            lastSeen = current;
            current = current.next.get();
        }
        return lastSeen;
    }
    
    protected void waitInQueue() {
    	if(this.sequentialVersion%2==0){
    		while(parent.seqClock != this.sequentialVersion - 1){Thread.yield();}
    	}
    	else {
    		while(parent.parent.seqClock != this.sequentialVersion - 2){Thread.yield();}
    	}		   	
    }

    protected void canCommit(){
    	//added
    	if(this.sequentialVersion != 1){
    		if(this.sequentialVersion%2==0){
    			waitInQueue();
    			checkEarlyAborts();
    			Object[] checkpoint = executeSequentially();
    			if(checkpoint[0] != null || checkpoint[1] != null){
    				commitSpeculationTillCheckpoint(checkpoint);
    			}
    		}
    		else{
    			waitInQueue();
    		}
    	}
    }
    protected void checkEarlyAborts() {
    	ReadWriteTransaction parent = this.getRWParent();
    	
    	while(parent != null){    
    		if(parent.earlyAbort){
    			TransactionSignaller.SIGNALLER.signalEarlyAbort();
    		}
    		parent = parent.getRWParent();
    	}		
	}
    
    @Override
    protected void tryCommit() {
        ReadWriteTransaction parent = getRWParent();
        NestedCommitRecord lastSeen;
        NestedCommitRecord newCommit;
        do {
            lastSeen = helpCommitAll(parent.nestedCommitQueue);
            try{
            	snapshotValidation(lastSeen.commitNumber);
            }catch(CommitException e){
            	this.manualAbort();
            	Transaction.incFuturesAborts();
            	if(speculationCheckpoint != null){
                    Transaction.current.set(getRWParent());
            		Continuation.resume(speculationCheckpoint);
            	}
            	throw e;
            } 
            Cons<VArrayEntry<?>> varrayReadsToPropagate = validateNestedArrayReads();
            newCommit = new NestedCommitRecord(this, this.mergedTxs, parent.mergedTxs, varrayReadsToPropagate, arrayWrites, arrayWritesCount, lastSeen.commitNumber + 1);
        } while (!lastSeen.next.compareAndSet(null, newCommit));//if there was a new commit, we need to re-validate again
        														//jvstm+futures doesnt require this, since we know that after validate,
        														//there wont be a new commit besides this tx commit

        lastSeen = parent.nestedCommitQueue;
        while ((lastSeen != null) && (lastSeen.commitNumber <= newCommit.commitNumber)) {
            if (!lastSeen.recordCommitted) {
                lastSeen.helpCommit();
                parent.nestedCommitQueue = lastSeen;
            }
            lastSeen = lastSeen.next.get();
        }
       speculationCheckpoint = null;
       boxesWritten = null;
       perTxValues = null;
       mergedTxs = null;
    }

    @Override
    protected void snapshotValidation(int lastSeenNumber) {
//        if (retrieveAncestorVersion(parent) == lastSeenNumber) {
//            return;
//        } //this would make it jump validation
    	
//        for (ParallelNestedTransaction mergedTx : mergedTxs) {
//            for (Map.Entry<VBox, InplaceWrite> read : mergedTx.nestedReads.entrySet()) {
//                validateNestedRead(read);
//            }
//        } this would be required in jvstm if a sibling for this parent as committed after this aprents children,
    	//in jvstm+futures this never happens
    	
    	//added
        for (Map.Entry<VBox, InplaceWrite> read : nestedReads.entrySet()) {
            validateNestedRead(read);
        }

        if (!this.globalReads.isEmpty()) {
            validateGlobalReads(globalReads, next);
        }
    }

    /*
     * Validate a single read that was a read-after-write over some ancestor
     * write. Iterate over the inplace writes of that VBox: if an entry is found
     * belonging to an ancestor, it must be the one that it was read, in which
     * case the search stops.
     */
    protected void validateNestedRead(Map.Entry<VBox, InplaceWrite> read) {
        InplaceWrite inplaceRead = read.getValue();
        InplaceWrite iter = read.getKey().inplace;
        do {
            if (iter == inplaceRead) {
                break;
            }
            int maxVersion = retrieveAncestorVersion(iter.orec.owner);
            if (maxVersion >= 0) {//added
            	TransactionSignaller.SIGNALLER.signalCommitFail(iter.orec.owner);
            }
            iter = iter.next;
        } while (iter != null);
    }

    /*
     * Validate a single read that obtained a VBoxBody Iterate over the inplace
     * writes of that VBox: no entry may be found that belonged to an ancestor
     */
    protected void validateGlobalReads(Cons<ReadBlock> reads, int startIdx) {
        VBox[] array = reads.first().entries;
        // the first may not be full
        for (int i = startIdx + 1; i < array.length; i++) {
            InplaceWrite iter = array[i].inplace;
            do {
                int maxVersion = retrieveAncestorVersion(iter.orec.owner);
                if (maxVersion >= 0) {//added
                    TransactionSignaller.SIGNALLER.signalCommitFail(iter.orec.owner);
                }
                iter = iter.next;
            } while (iter != null);
        }

        // the rest are full
        for (ReadBlock block : reads.rest()) {
            array = block.entries;
            for (int i = 0; i < array.length; i++) {
                InplaceWrite iter = array[i].inplace;
                do {
                    int maxVersion = retrieveAncestorVersion(iter.orec.owner);
                    if (maxVersion >= 0) {//added
                        TransactionSignaller.SIGNALLER.signalCommitFail(iter.orec.owner);
                    }
                    iter = iter.next;
                } while (iter != null);
            }
        }
    }

    protected Cons<VArrayEntry<?>> validateNestedArrayReads() {
        Map<VArrayEntry<?>, VArrayEntry<?>> parentArrayWrites = getRWParent().arrayWrites;
        Cons<VArrayEntry<?>> parentArrayReads = getRWParent().arraysRead;
        int maxVersionOnParent = retrieveAncestorVersion(parent);
        for (VArrayEntry<?> entry : arraysRead) {

            // If the read was performed on an ancestor of the parent, then
            // propagate it for further validation
            if (entry.owner != parent) {
                parentArrayReads = parentArrayReads.cons(entry);
            }

            synchronized (parent) {
                if (parentArrayWrites != EMPTY_MAP) {
                    // Verify if the parent contains a more recent write for the
                    // read that we performed somewhere in our ancestors
                    VArrayEntry<?> parentWrite = parentArrayWrites.get(entry);
                    if (parentWrite == null) {
                        continue;
                    }
                    if (parentWrite.nestedVersion > maxVersionOnParent) {
                        TransactionSignaller.SIGNALLER.signalCommitFail(parent);
                    }
                }
            }
        }

        return parentArrayReads;
    }
}
