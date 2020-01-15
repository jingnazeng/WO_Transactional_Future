package contlib;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jvstm.ParallelNestedTransaction;
import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;
import contlib.RBTreeBenchmark.SequentialVersion.RBTreeReadWriteTest;



/* in this test i run two txs and while one of it only does one write to control, the other does 2 speculations, that is 4 nested thread
 * while f1 and c2 write to control 2, f3 and f4 writes to control, 4 and 3 abort early which forces the second tx to reexecute
 * on the second reexecution, c2 aborts because of f1, and while c4 and f3 read what the other transaction wrote, c4 aborts because of f3
 */
public class AssertInterTX implements Runnable {
	
	static protected ExecutorService executor = Executors.newFixedThreadPool(10);
    static public VBox<Integer> control = new VBox<Integer>();
    static public VBox<Integer> control2 = new VBox<Integer>();
    public int id;
    
   static public class ThreadCreate implements Runnable {
	   public int id;
		@Override
		public void run() {
			while(true) {
				AssertInterTX  r = new AssertInterTX();
				r.id = this.id;
				try {
					System.out.println("Running");
					Continuation.runWithContinuationSupport(r);
					return;
				}
				catch(Exception n) {
					System.out.println("Reexecuting " +Transaction.current.get().sequentialVersion); 
					//n.printStackTrace();
					Transaction.abort();
				}
			}
		}
    }
   
   
   private class Async implements Callable<Integer> {
   	public int i;
   	public boolean isReadOnly;
   	
   	public Integer call(){
   		
   		if(i != 1) {
   			int j = control.get();
   	   		
   	   		System.out.println("nested: " + Transaction.current.get().sequentialVersion + " id: " + id  + " i: " +i+   " reads control1: " + j);
   			control.put(j + 1);	
   		}
   		if(id == 1 && i ==1) {
   			int j = control2.get();
   			System.out.println("nested: " + Transaction.current.get().sequentialVersion + " id: " + id  + " i: " +i +  " reads control2: " + j);
   			control2.put(j + 1);
   		}
   		return 1;
   	}
   }

	
	public void runSpeculation(int i){
		Async c = new Async();
		c.i= i;
		Future<Integer> result = Transaction.current().manageNestedParallelTxs(new ParallelTask<Integer>(c));
		
		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx =Transaction.begin();

		
		

   		if(i != 1) {
   			int j = control.get();
   			System.out.println("nested: " + Transaction.current.get().sequentialVersion  + " id: " + id + " i: " +i  + " reads control1: " + j);
   			control.put(j + 1);	
   		}
   		if(id == 1 && i ==1) {
   			int j = control2.get();
   			System.out.println("nested: " + Transaction.current.get().sequentialVersion + " id: " + id  + " i: " +i +  " reads control2: " + j);
   			control2.put(j + 1);
   		}
	}

	@Override
	public void run() {
		Transaction tx =Transaction.begin();

		int i =1;
		int iteration = 0;
		if(id==0) {
			control.put(1);
		}
		else{
			while(iteration < 2) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 runSpeculation(i);
				 i +=2;
				 iteration++;
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Transaction.commit();
		System.out.println("tx finished");
		
	} 
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, InterruptedException {
		control.put(0);
		control2.put(0);
		
		ArrayList<Callable<Object>> transactions = new ArrayList<Callable<Object>>();
		
		for(int i = 0; i< 2 ; i++) {
			ThreadCreate test = new ThreadCreate();
			test.id =i;
			transactions.add(Executors.callable(test));
		}

		executor.invokeAll(transactions);
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		
		System.out.println( "Finished control=?: " + control.get());
		System.out.println( "Finished control2=?: " + control2.get());
	}

}
