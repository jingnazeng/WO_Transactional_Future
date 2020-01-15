package jvstm.graph.test;

import java.util.concurrent.Callable;

import jvstm.Transaction;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;



/**
 * 	An execution simulate below case
 * 
 *                TF1
 *              ---------------
 *             |               |
 *             |               |
 *      --T1---|-T2-|--T3------|--T4---|--T5-
 *                  |                  |
 *                  |------------------|
 * 						TF2
 * 
 * 
 * 		T1  w(x=0) w(y=0) w(z=0)
 * 		T2  r(x)
 * 		Tf1 r(x),w(y=3),w(z=3)
 * 		Tf2 r(y)
 *      T3  r(z) 
 *      T4  r(y)
 *      T5  r(x),r(y),r(z)
 *        
 *        
 * 		in this test case, Tf1 is serialized at evaluation point, restart to read the updated value of x		
 * 
 * */


public class TestCaseB extends Thread {

	
	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<String>{
		
		
		WeakFutureTask(Callable<String> c){
			super(c);
			
		}
		
		
	}
	
	class AsychOpsF1 implements Callable<String>{

		@Override
		public String call() throws Exception {
			//Tf1 logic
			Thread.sleep(5);
			Integer value_read = TransactionTest.control_X.get();
			System.out.println("Tf1 read x: " + value_read);
			TransactionTest.control_Y.put(3);
			System.out.println("Tf1 write y=3");
			TransactionTest.control_Z.put(3);
			System.out.println("Tf1 write z=3");
			return "done";

		}
		
	}
	
	class AsychOpsF2 implements Callable<String>{

		@Override
		public String call() throws Exception {
			//Tf2 logic
			Thread.sleep(10);
			TransactionTest.control_Z.put(4);;
			System.out.println("Tf2 write Z: " + 4);
			Integer value_read = TransactionTest.control_Y.get();
			System.out.println("Tf2 read Y: " + value_read);
			return "done";
		}
	}
	
	public void run(){
		boolean first_run = true;

		Transaction tx = Transaction.begin(false);
		while(true){
			try{
				//T1 logic
				TransactionTest.control_X.put(0);
				System.out.println("T1 write x=0");
				TransactionTest.control_Y.put(0);
				System.out.println("T1 write y=0");
				TransactionTest.control_Z.put(0);
				System.out.println("T1 write z=0");
				
				FutureRepresentation<String> tf1 = null;
				if(first_run){
					//Tf1 submission
					tf1 = 
							Transaction.current().submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsF1()));
					System.out.println("*******tf1 submitted*******");
				}
				
				while(true){
					try{
						//T2 logic
						//Thread.sleep(5);
						int value_read = TransactionTest.control_Y.get();
						System.out.println("T2 read value Y: "+ value_read);
						FutureRepresentation<String> tf2 = null;
						if(first_run){
							//Tf2 submission
							tf2 = 
									Transaction.current().submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsF2()));
							System.out.println("*******tf2 submitted*******");
						}
						while(true){
							try{
								//T3 logic
								int value_read_by_T3 = TransactionTest.control_Z.get();
								System.out.println("T3 read value Z: "+ value_read_by_T3);
								if(first_run){
									//TF1 is evaluated
									String result = Transaction.current().evalWeakOrderingFuture(tf1);
								}
								break;
							}catch(Throwable e){
								System.out.println("T3 restarting");
								e.printStackTrace();
							}
						}
						//T4 logic
						int value_read_by_T4 = TransactionTest.control_Y.get();
						System.out.println("T4 read value Y: "+ value_read_by_T4);
						if(first_run){
						//TF2 is evaluated
						String result = Transaction.current().evalWeakOrderingFuture(tf2);
						}
						break;
					}catch(Throwable e){
						System.out.println("T2 + T3 + T4 restarting");
						first_run = false;
					}
				}
				//T5 logic
				System.out.println("T5 read X: " + TransactionTest.control_X.get());
				System.out.println("T5 read Y: " + TransactionTest.control_Y.get());
				System.out.println("T5 read Z: " + TransactionTest.control_Z.get());
				tx.commitTx(true);
				System.out.println("Test case B finished executing");
				return;
			}catch(Throwable e){
				tx.abortTx();
			}
		}
	}


}

