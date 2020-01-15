package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;

public class PatterTxn1 extends ArrayAccessWriteTxnWithMultipleFutures {
	VBox<Integer>[] array;
	private int value;

	public PatterTxn1(){
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
		FutureRepresentation<Integer> f2 = null;
		FutureRepresentation<Integer> f3 = null;
		FutureRepresentation<Integer> f4 = null;
		while(true){
			try{
				//T2 empty
				//Tf2 logic
				f2  = ((ReadWriteTransaction)Transaction.current()).
						submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture2()));

				while(true){
					try{
						//T3 empty
						f3  = ((ReadWriteTransaction)Transaction.current()).
								submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture3()));

						while(true){
							try{
								//T4 empty
								f4  = ((ReadWriteTransaction)Transaction.current()).
										submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture4()));
								
								break;
							}catch(Throwable e){
								if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
									throw e;
								else{
									e.printStackTrace();
									System.out.println("T4 restarting");
								}
							}

						}
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
				value += (Integer) Transaction.current().evalWeakOrderingFuture(f3);
				value += (Integer) Transaction.current().evalWeakOrderingFuture(f4);
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
			Thread.sleep(100);
			int val = (int) (Math.random()*10);
			array[0].put(val); 
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
			
			//					int val = (int) (Math.random()*10);
			int val = array[0].get(); 
			System.out.println("Tf2 read x to be :" + val);
			return value;

		}
	}


	class AsychOpsFuture3 implements Callable<Integer>{

		public AsychOpsFuture3(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			Thread.sleep(100);
			int val = (int) (Math.random()*10);
			array[1].put(val); 
			System.out.println("Tf3 write y to be :" + val);
			return value;

		}
	}

	class AsychOpsFuture4 implements Callable<Integer>{

		public AsychOpsFuture4(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			
			int val = array[1].get(); 
			System.out.println("Tf4 read y to be :" + val);
			return value;

		}
	}


}
