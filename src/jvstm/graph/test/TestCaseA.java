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
 * 		T1 w(x=0)
 * 		T2 r(x), w(x=2)
 * 		Tf r(x),w(x=3)
 * 		T3 r(x)
 * 
 * 		in this test case, Tf is serialized at evaluation point, restart to read the updated value of x		
 * 
 * */


public class TestCaseA extends Thread {

	
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
			TransactionTest.control_X.put(3);
			System.out.println("Tf write x=3");
			return "done";

		}
		
	}
	
	public void run(){

		Transaction tx = Transaction.begin(false);
		while(true){
			try{
				//T1 logic
				TransactionTest.control_X.put(0);
				System.out.println("T1 write X=0");
				//Tf logic
				FutureRepresentation<String> tf = ((ReadWriteTransaction)tx).submitWeakOrderingFuture(new WeakFutureTask(new AsychOps()));

				while(true){
					try{
						//T2 logic
						int value_read = TransactionTest.control_X.get();
						System.out.println("T2 read value X: "+ value_read);
						TransactionTest.control_X.put(2);
						System.out.println("T2 write x=2");
						String result = Transaction.current().evalWeakOrderingFuture(tf);
						break;
					}catch(Throwable e){
						System.out.println("T2 restarting");
					}
				}
				//T3 logic
				System.out.println("T3 read X: " + TransactionTest.control_X.get());
				tx.commitTx(true);
				System.out.println("Test case A finished executing");
				return;
			}catch(Throwable e){
				tx.abortTx();
			}
		}
	}


}

