package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;

public class Patter2Txn extends ArrayAccessWriteTxnWithMultipleFutures {
	VBox<Integer>[] array;
	private int value;

	public Patter2Txn(){
	}

	@Override
	public int executeTransaction(int sibling) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {
		array = ArrayAccess.getArray();

		FutureRepresentation<Integer> f1 = ((ReadWriteTransaction)tx).
				submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
		while(true){
			try{
				//T2 logic
				value += array[1].get();
				System.out.println("T2 read x: " + array[1].get());
				//Tf2 logic
				FutureRepresentation<Integer> f2  = ((ReadWriteTransaction)Transaction.current()).
						submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture2()));

				while(true){
					try{
						value += array[2].get();
						System.out.println("T3 read y: " + array[2].get());
						value += (Integer) Transaction.current().evalWeakOrderingFuture(f1);
						break;
					}catch(Throwable e){
						if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
							throw e;
						else{
							e.printStackTrace();
							System.out.println("T3 restarting");
						}
					}

				}
				value += (Integer) Transaction.current().evalWeakOrderingFuture(f2);
				break;
			}catch(Throwable e){
				if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else{
					e.printStackTrace();
					System.out.println("T2 restarting");
				}
			}
		}

		return value;
	}
	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<Integer>{
		WeakFutureTask(Callable<Integer> c){
			super(c);
		}
	}


	class AsychOpsFuture1 implements Callable<Integer>{

		public AsychOpsFuture1(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			Thread.sleep(10000);
			int val = (int) (Math.random()*10);
			array[1].put(val); 
			System.out.println("Tf1 write x to be :" + val);
			return value;

		}
	}


	class AsychOpsFuture2 implements Callable<Integer>{

		public AsychOpsFuture2(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			Thread.sleep(100);
			int val = (int) (Math.random()*10);
			array[2].put(val); 
			System.out.println("Tf2 write y to be :" + val);
			return value;

		}
	}



}
