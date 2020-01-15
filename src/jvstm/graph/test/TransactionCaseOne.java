package jvstm.graph.test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;


/**
 * 	An execution simulate below case
 * 
 *     |
 *     | T1 
 *     |-------
 *     |	  |
 *     |T2 	  | Tf
 *     |      |
 *     |------|
 *     |T3
 *     |
 * 
 * 		T1 w(x=1)
 * 		T2 r(x)<-1 or 2
 * 		Tf w(x=2)
 * 		T3 w(y=2)
 * 
 * */

public class TransactionCaseOne extends Thread {
	
	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<String>{
		
		
		WeakFutureTask(Callable<String> c){
			super(c);
			
		}
		
		
	}
	
	class AsychOps implements Callable<String>{

		@Override
		public String call() throws Exception {
			TransactionTest.control_X.put(2);
			String read_value = String.valueOf(TransactionTest.control_X.get());
			return read_value;

		}
		
	}
	
	public void run(){

		Transaction tx = Transaction.begin(false);
		while(true){
			try{
				//T1 logic
				TransactionTest.control_X.put(1);
				
				//Tf logic
				FutureRepresentation<String> tf = ((ReadWriteTransaction)tx).submitWeakOrderingFuture(new WeakFutureTask(new AsychOps()));

				while(true){
					try{
						//T2 logic
						int value_read = TransactionTest.control_X.get();
						System.out.println("T2 read value X: "+ value_read);
						String result = ((ReadWriteTransaction)tx).evalWeakOrderingFuture(tf);
					//	tf.get();
						break;
					}catch(Throwable e){
						System.out.println("T2 restarting");
					}
				}
				//T3 logic
				TransactionTest.control_X.put(6);
				TransactionTest.control_Y.put(7);
				tx.commitTx(true);
				System.out.println("Transaction case one finished executing");
				return;
			}catch(Throwable e){
				tx.abortTx();
			}
		}
	}

}
