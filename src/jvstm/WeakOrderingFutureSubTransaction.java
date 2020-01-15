package jvstm;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import jvstm.graph.DAG;
import jvstm.graph.FutureRestartException;
import jvstm.graph.GraphLock;
import jvstm.graph.Node;
import jvstm.graph.DAG.DAGStatus;
import jvstm.graph.Node.Type;
import jvstm.graph.Node.Status;
import jvstm.util.Cons;

public class WeakOrderingFutureSubTransaction extends ParallelNestedTransaction {
	
    /**
	 * @return the boxesWritten
	 */
	public Map<VBox, Object> getBoxesWritten() {
		return boxesWritten;
	}

	/**
	 * @param boxesWritten the boxesWritten to set
	 */
	public void setBoxesWritten(Map<VBox, Object> boxesWritten) {
		this.boxesWritten = boxesWritten;
	}
	 
	/**
	 * @return the nestedReads
	 */
	public Map<VBox, Object> getNestedReads() {
		return nestedReads;
	}

	/**
	 * @param nestedReads the nestedReads to set
	 */
	public void setNestedReads(Map<VBox, Object> nestedReads) {
		this.nestedReads = nestedReads;
	}

	private DAG dag;
	private GraphLock lockOnDAG;
	protected Map<VBox, Object> nestedReads;
	protected Map<VBox, Object> boxesWritten = EMPTY_MAP;
	

	public WeakOrderingFutureSubTransaction(ReadWriteTransaction parent) {
		super(parent);

		this.nestedReads = new HashMap<VBox, Object>();
		this.globalReads = Cons.empty();
	
//		//associate this transactional future sub-transaction with the corresponding node
//		DAG dag = this.getTopLevelTrasanction().getDAG();
//
//		DirectChildren<Node> children = (dag.getDirectChildrenNode((this.getParent().getAssociatedNode())));
//		children.getFuture().setAsscociatedTxn(this);
//		this.setAssociatedNode(children.getFuture());

//		this.lockOnDAG = this.getAssociatedNode().getResidingDAG().getGraphLock();
//		this.dag = this.getAssociatedNode().getResidingDAG();
		this.dag = this.parent.getAssociatedNode().getResidingDAG();
		this.lockOnDAG = dag.getGraphLock();
		
	}
	
    @Override
    public <T> void setBoxValue(jvstm.VBox<T> vbox, T value) {
//     	System.out.println("inside setBox value and the top level transaction is: "+ this.getTopLevelTrasanction());
//    	System.out.println("inside setBox value and the sub  transaction is: "+ this);
//    	System.out.println("the box being written is: "+ vbox);
//    	System.out.println("write lock owner is: "+ vbox.writeLock.owner);
    	
    	//if the subtxn has already writting to the key, no need to take the lock, overwrite directly.
    	if(boxesWritten!=EMPTY_MAP && boxesWritten.containsKey(vbox)){
    		boxesWritten.put(vbox, value == null ? NULL_VALUE : value);
    		return;
    	}
    	
    	if(vbox.getExclusiveLock(this.getTopLevelTrasanction())){
    		 if (boxesWritten == EMPTY_MAP) {
                 boxesWritten = new IdentityHashMap<VBox, Object>();
             }
    		 
             boxesWritten.put(vbox, value == null ? NULL_VALUE : value);
    	}else{ //other top-level transaction acquired the lock on x
    		// restart the whole transaction; flag sub-transactions to restart as well
//         	System.out.println("inside setBox value and the top level transaction is: "+ this.getTopLevelTrasanction());
//        	System.out.println("write lock owner is: "+ vbox.writeLock.owner);
    		//put the boxesWritten to top-level write-set in order to release the lock on VBOX
    		moveWriteSetToReleaseLock();
//    		System.out.println("early abort txn: "+this);
    		TransactionSignaller.SIGNALLER.signalEarlyAbort();
    		this.getTopLevelTrasanction().earlyAbort =true;
    	}   	
    }
    
	private void moveWriteSetToReleaseLock() {
		//TODO: the boxes written by toplevel is not atomically operated,irst it can be null and later populated by other threads
		try{
		if(this.boxesWritten.size()!=0){
		
			this.getTopLevelParent().boxesWritten.putAll(this.boxesWritten);
			
		}}catch(NullPointerException e){
//			System.out.println("DAG status: "+this.getTopLevelParent().getAssociatedNode().getResidingDAG().getDagStatus());
//			System.out.println("top level: "+this.getTopLevelParent());
//			System.out.println("boxes written by top-level: "+this.getTopLevelParent().boxesWritten
//					+ 
//					"by txn: "+this);
//			System.out.println("boxes written by sub txn: "+this.boxesWritten);
//			if(this.getTopLevelParent().boxesWritten!=null){
//				e.printStackTrace();
//			}
			if(this.getTopLevelParent().boxesWritten == null){
				this.getTopLevelParent().boxesWritten = new HashMap<VBox, Object>();;
			}
			this.getTopLevelParent().boxesWritten.putAll(this.boxesWritten);
		}
	}

	@Override
	public <T> T getBoxValue(VBox<T> vbox) {
		
		//TODO: what is the read lock trying to protect, may shrink the scope which the lock protects
		lockOnDAG.readAcquireLock();
		T value_read = null;
//		synchronized(dag){
		//	System.out.println("current DAG structure: "+ this.dag.getGraph());
		
		value_read = (T) this.getBoxesWritten().get(vbox);
		//	System.out.println("read the vbox: *** "+ vbox + " *** by transaction "+ this);
		if(value_read != null){
			
			lockOnDAG.readReleaselock();
			return value_read;
			
		}
		//if(lock on X is owned by the top-level txn of this txn) 
		// x has been updated by any sub-transaction in the family of this txn
		if(vbox.getWriteLock().getOwner_top_level_transaction_id() == this.getTopLevelTrasanction()){
			//System.out.println("BataVI");
			//value_read = dag.mostRecentCommittedVerstion(vbox,this.getAssociatedNode());
			if(value_read == null)
				try{
					value_read = readGlobal(vbox);
				}catch(EarlyAbortException eae) {
					moveWriteSetToReleaseLock();
					lockOnDAG.readReleaselock();
					throw eae;
				}
		}else
			try{
				value_read = readGlobal(vbox);
			}catch(EarlyAbortException eae) {
				moveWriteSetToReleaseLock();
				lockOnDAG.readReleaselock();
				throw eae;
			}
		
		this.nestedReads.put(vbox, value_read);
//		}
		lockOnDAG.readReleaselock();
		return value_read;

	}
	
	protected <T> T readGlobal(VBox<T> vbox) {
		VBoxBody<T> body = vbox.body;

		if (body.version > number) {
			this.getTopLevelTrasanction().earlyAbort =true;
			TransactionSignaller.SIGNALLER.signalEarlyAbort();
		}
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
	
	protected void canCommit(){
		checkEarlyAborts();
		Node node = this.getAssociatedNode();
		if(node.getStatus() == Status.waitToRestart){
			dag.iCommitReexecutedFuture(node);
		} else if (node.getType() == Type.begin_future){
			try {
				dag.iCommitFutureUponCompletion(node);
			} catch (FutureRestartException e) {
				throw e;
			} 
		}
	}
	
	@Override
	protected void tryCommit() {
		//		ReadWriteTransaction parent = getRWParent();
		//		NestedCommitRecord lastSeen;
		//		NestedCommitRecord newCommit;
		//		do {
		//			lastSeen = helpCommitAll(parent.nestedCommitQueue);
		//			try{
		//				snapshotValidation(lastSeen.commitNumber);
		//			}catch(CommitException e){
		//				super.manualAbort();
		//			} 
		//			Cons<VArrayEntry<?>> varrayReadsToPropagate = validateNestedArrayReads();
		//			newCommit = new NestedCommitRecord(this, this.mergedTxs, parent.mergedTxs, varrayReadsToPropagate, arrayWrites, arrayWritesCount, lastSeen.commitNumber + 1);
		//		} while (!lastSeen.next.compareAndSet(null, newCommit));//if there was a new commit, we need to re-validate again
		//		//jvstm+futures doesnt require this, since we know that after validate,
		//		//there wont be a new commit besides this tx commit
		//
		//		lastSeen = parent.nestedCommitQueue;
		//		while ((lastSeen != null) && (lastSeen.commitNumber <= newCommit.commitNumber)) {
		//			if (!lastSeen.recordCommitted) {
		//				lastSeen.helpCommit();
		//				parent.nestedCommitQueue = lastSeen;
		//			}
		//			lastSeen = lastSeen.next.get();
		//		}
		//
		//		boxesWritten = null;
		//		perTxValues = null;
		//		mergedTxs = null;
		if(this.getTopLevelTrasanction().getDAG().getDagStatus() == DAGStatus.ABORTED){
		//	System.out.println("top level aborted");
			this.abortTx();
		}else{
			this.getTopLevelParent().boxesWritten.putAll(this.boxesWritten);
		}
	}
	
	 protected void snapshotValidation(int lastSeenNumber) {

		 //TODO: is validatedNestedRead still useful when we have forward and backward validation? 
//       for (Map.Entry<VBox, Object> read : nestedReads.entrySet()) {
//           validateNestedRead(read);
//       }

       if (!this.globalReads.isEmpty()) {
           validateGlobalReads(globalReads, next);
       }
   }
	 
	  /*
	     * Validate a single read that obtained a VBoxBody Iterate over the inplace
	     * writes of that VBox: no entry may be found that belonged to an ancestor
	     */
	    protected void validateGlobalReads(Cons<ReadBlock> reads, int startIdx) {
//	        VBox[] array = reads.first().entries;
//	        // the first may not be full
//	        for (int i = startIdx + 1; i < array.length; i++) {
//	            InplaceWrite iter = array[i].inplace;
//	            do {
//	                int maxVersion = retrieveAncestorVersion(iter.orec.owner);
//	                if (maxVersion >= 0) {//added
//	                    TransactionSignaller.SIGNALLER.signalCommitFail(iter.orec.owner);
//	                }
//	                iter = iter.next;
//	            } while (iter != null);
//	        }
//
//	        // the rest are full
//	        for (ReadBlock block : reads.rest()) {
//	            array = block.entries;
//	            for (int i = 0; i < array.length; i++) {
//	                InplaceWrite iter = array[i].inplace;
//	                do {
//	                    int maxVersion = retrieveAncestorVersion(iter.orec.owner);
//	                    if (maxVersion >= 0) {//added
//	                        TransactionSignaller.SIGNALLER.signalCommitFail(iter.orec.owner);
//	                    }
//	                    iter = iter.next;
//	                } while (iter != null);
//	            }
//	        }
	    }
	    
	    protected Cons<VArrayEntry<?>> validateNestedArrayReads() {
			return arraysRead;}
	    
	    @Override
	    public void abortTx() {   
//	    	System.out.println("abort txn: "+ this.getTopLevelParent());
	    	manualAbort();
	    	Transaction.current.set(getRWParent());

	    }
	    
	    @Override
	    protected void manualAbort() {
//	    	System.out.println("boxes written null manualAbort method for txn: "+ this.getTopLevelParent());
//	        super.boxesWritten = null;
	        int i = 0;
	        for (ReadBlock block : globalReads) {
	            block.free = true;
	            i++;
	        }
	        blocksFree.get().addAndGet(i);
	        
	        for(VBox boxWritten: boxesWritten.keySet()){
	        	boolean success = boxWritten.releaseSubTxnLock();
	        	if(!success){
	        		//try again
	        		success = boxWritten.releaseSubTxnLock();
	        		if(!success)
	        			throw RELEASE_WRITELOCK_ERROR_EXCEPTION;
	        	}
//	        	System.out.println("released lock by aborted sub transaction: " + this);
//	        	System.out.println("----------------------------- " );
	        }
	      
	        this.globalReads = null;
	        this.nestedReads = null;
	        super.mergedTxs = null;
	    }

}
