package jvstm;

import java.util.concurrent.Callable;

import benchmark.synthetic.TimerDebug;
import jvstm.graph.DAG;
import jvstm.graph.DirectChildren;
import jvstm.graph.FutureRestartException;
import jvstm.graph.Node;
import jvstm.graph.DAG.DAGStatus;
import jvstm.graph.Node.Status;


public class WeakOrderingTransactionalFutureTask<T> extends TransactionalTask<T> {
	private boolean readOnly;
	Node node_representing_future;

	public WeakOrderingTransactionalFutureTask(){}

	public WeakOrderingTransactionalFutureTask(Callable<T> c) {
		super();
		asyncMethod = c;	
	}
	
	public WeakOrderingTransactionalFutureTask(Callable<T> c, boolean readOnly) {
		super();
		asyncMethod = c;	
		this.readOnly = readOnly;
	}
	
	public boolean isReadOnly(){
		return readOnly;
	}

	@Override
	public T execute() throws Throwable { T n = null; return n; }

	@Override
	public T call() throws Exception {
		T result =null;
		try {
			// parent may be null if this is a "top-level" task
			if (super.parent != null) {
				super.parent.start();
			}
			while(true) {
				Transaction tx;
				if (super.parent != null) {
					// we force the creation of a parallel nested, otherwise it would be a linear nested
					//we start to distinguish strong ordering futures and weak ordering futures
					//tx represents the future transaction reference
					tx = Transaction.startWeakOrderingTransactionalFuture(isReadOnly(), seqNumber);
				//	System.out.println("txn for future:  "+ tx);
					
					if(node_representing_future == null){ //this future sub-txn is executing for the first time
						//associate this transactional future sub-transaction with the corresponding node
						DAG dag = tx.getTopLevelTrasanction().getDAG();
						DirectChildren<Node> children = (dag.getDirectChildrenNode((tx.getParent().getAssociatedNode())));
						node_representing_future = children.getFuture();
						node_representing_future.setAsscociatedTxn(tx);
						tx.setAssociatedNode(node_representing_future);
					}else{ // this future sub-txn is re-executing
						node_representing_future.setAsscociatedTxn(tx);
						tx.setAssociatedNode(node_representing_future);
						//node_representing_future.setStatus(Status.evaluated);
					}
			        
					//added
					tx.setAsyncMethod(asyncMethod);
				} else {
					tx = Transaction.begin(isReadOnly());
				}
			
				try {
					//System.out.println("inside TF current txn: "+ Transaction.current());
					result = asyncMethod.call();
					//System.out.println("current txn: "+ Transaction.current());
					if(tx.getTopLevelTrasanction().getDAG().getDagStatus()==DAGStatus.ABORTED){
						tx.abortTx();
						tx  = null;
						break;
					}else{
//						System.out.println(Thread.currentThread().getId()%56);
						TimerDebug.startCommit((int) (Thread.currentThread().getId()%56));
						tx.commitTx(true); //should not call commit otherwise it will commit the parent
						TimerDebug.endCommit((int) (Thread.currentThread().getId()%56));
						TimerDebug.futureCommit((int) (Thread.currentThread().getId()%56));
						//parent.seqClock = this.seqNumber;
						//record the successful commit of a future for statistical computation
						tx = null;
						break;
					}
				}
					catch(ExecuteParallelNestedTxSequentiallyException e){
						//"inter-tx W-W conflict"
						tx.abort();
						//	parent.seqClock = this.seqNumber;
						tx = null;
						return null;//this is pretty dirty....
					}
					catch(EarlyAbortException eae) {
						//versao mais recente, committed por uma top-level tx
//						eae.printStackTrace();
//						System.out.println("abort 1.1b : "+tx);
						tx.getTopLevelTrasanction().getAssociatedNode().getResidingDAG().setDagStatus(DAGStatus.ABORTED);
						tx.getAssociatedNode().setStatus(Status.aborted);
						tx.abort();
						//	parent.seqClock = this.seqNumber;
						tx = null;
						if (super.parent != null) {
							throw eae;
						}
					} 
					catch (CommitException ce) {
						//conflito sequencial
						TimerDebug.abortIncrement((int) (Thread.currentThread().getId()%56));
						tx.abort();
						tx = null;
					}
					catch(FutureRestartException fe){
					//	System.out.println("we should be reexecuting the future subtxn now");
					//	System.out.println("current txn: "+ Transaction.current());
						TimerDebug.abortIncrement((int) (Thread.currentThread().getId()%56));
						node_representing_future = tx.getAssociatedNode();
						tx.abort();
						tx = null;
					}
				}
		
		}
		catch(EarlyAbortException eae) {
			throw eae;
		}
		catch (Throwable t) {
			t.printStackTrace();

			if (t instanceof Exception) {
				throw (Exception) t;
			} else {
				throw new RuntimeException(t);
			}
		}
		return result;
	}

}
