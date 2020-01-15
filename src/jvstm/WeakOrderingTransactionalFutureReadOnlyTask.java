package jvstm;

import java.util.concurrent.Callable;

import jvstm.graph.Node;

public class WeakOrderingTransactionalFutureReadOnlyTask<T> extends TransactionalTask<T> {

	Node node_representing_future;

	public WeakOrderingTransactionalFutureReadOnlyTask(){}

	public WeakOrderingTransactionalFutureReadOnlyTask(Callable<T> c) {
		super();
		asyncMethod = c;	
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
//					tx = Transaction.startWeakOrderingTransactionalFuture(isReadOnly(), seqNumber);
					tx = Transaction.beginParallelNested(isReadOnly(), seqNumber);
					tx.setAsyncMethod(asyncMethod);
				} else {
					tx = Transaction.begin(isReadOnly());
				}

				try {
					result = asyncMethod.call();
					tx.commitTx(true); //should not call commit otherwise it will commit the parent
					tx = null;
					break;
				}
				catch(ExecuteParallelNestedTxSequentiallyException e){
					//"inter-tx W-W conflict"
					tx.abort();
					tx = null;
					return null;//this is pretty dirty....
				}
				catch(EarlyAbortException eae) {
					//versao mais recente, committed por uma top-level tx
					//						eae.printStackTrace();
					tx.abort();
					tx = null;
					if (super.parent != null) {
						throw eae;
					}
				} 
				catch (CommitException ce) {
					//conflito sequencial
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
