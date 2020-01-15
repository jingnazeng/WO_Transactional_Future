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
 * 		T1 w(x=1),w(y=1)
 * 		T2 r(y), w(x=y+2)
 * 		Tf r(y)
 * 		T3 r(x),r(y)
 * 		
 * 		Tf is able to be serialized at submission
 * */
public class TransactionCaseThree extends Thread{

	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<String>{


		WeakFutureTask(Callable<String> c){
			super(c);

		}


	}

	class AsychOps implements Callable<String>{

		@Override
		public String call() throws Exception {
			//Tf logic
			Integer value_read = TransactionTest.control_X.get();
			System.out.println("Tf read x: " + value_read);
//			TransactionTest.control_Y.put(2);
//			System.out.println("Tf write Y=2");
//			String read_value = String.valueOf(TransactionTest.control_Y.get());
//			System.out.println("Tf read y: " + read_value);
			return value_read.toString();

		}

	}

	public void run(){

		Transaction tx = Transaction.begin(false);
		while(true){
			try{
				//T1 logic
				TransactionTest.control_X.put(1);
				TransactionTest.control_Y.put(1);
				System.out.println("T1 write X=1 at address: " + TransactionTest.control_X);
				System.out.println("T1 write Y=1 at address: "+ TransactionTest.control_Y);
				//Tf logic
				FutureRepresentation<String> tf = ((ReadWriteTransaction)tx).submitWeakOrderingFuture(new WeakFutureTask(new AsychOps()));

				while(true){
					try{
						//T2 logic
						int value_read = TransactionTest.control_Y.get();
						System.out.println("T2 read value Y: "+ value_read);
						TransactionTest.control_X.put(value_read+2);
						System.out.println("T2 write x=" + value_read +"+2   at address: "+TransactionTest.control_X);
						value_read = TransactionTest.control_X.get();
						System.out.println("T2 read value X: "+ value_read);
						String result = Transaction.current().evalWeakOrderingFuture(tf);
						//	tf.get();
						break;
					}catch(Throwable e){
						System.out.println("T2 restarting");
					}
				}
				//T3 logic
				System.out.println("T3 read X: " + TransactionTest.control_X.get());
				System.out.println("T3 read Y: " + TransactionTest.control_Y.get());
				tx.commitTx(true);
				System.out.println("Transaction case one finished executing");
				return;
			}catch(Throwable e){
				tx.abortTx();
			}
		}
	}
}
