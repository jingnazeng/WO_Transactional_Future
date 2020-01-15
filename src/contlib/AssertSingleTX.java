package contlib;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jvstm.ParallelNestedTransaction;
import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

public class AssertSingleTX implements Runnable {

	static ExecutorService executorService = Executors.newSingleThreadExecutor();
    static public VBox<Integer> control = new VBox<Integer>();
    static public VBox<Integer> control2 = new VBox<Integer>();

    
    private class Async implements Callable<Integer> {
    	public int i;
    	public boolean isReadOnly;
    	
    	public Integer call(){

    		int j = control.get();
    		System.out.println("nested: " + i + " Read: " + isReadOnly +  " reads control1: " + j);
    		
    		if(!isReadOnly) {
    			control.put(j + 1);	
    		}
    	
    		
    		return 1;
    	}
    }

	
	public void runSpeculation(int i){
		Random r = new Random ();
//		boolean isReadOnlyFuture = (r.nextInt(2) == 1);
		boolean isReadOnlyFuture =false;
		if(i == 3) {
			isReadOnlyFuture = true;
		}
		Async c = new Async();
		c.i= i;
		c.isReadOnly = isReadOnlyFuture;
		if(!isReadOnlyFuture) {
			Future<Integer> result = Transaction.current().manageNestedParallelTxs(new ParallelTask<Integer>(c));
		}
		else {
			Future<Integer> result = Transaction.current().manageNestedParallelTxs(new ParallelTask<Integer>(c){
	            @Override
	            protected boolean isReadOnly() {
	                    return true;
	            }
			});
		}
		
		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx =Transaction.begin();


		int j = control.get();
		System.out.println("nested: " + (i+1) + " reads control1: " + j);

		control.put(j + 1);	
		


	}
	
	public void run() {
		Transaction tx =Transaction.begin();

		int i =1;
		int iteration = 0;
		while(iteration < 10) {
		 runSpeculation(i);
		 i +=2;
		 iteration++;
		}
		Transaction.commit();
		
	}

	public static void main(String[] args) {
		AssertSingleTX  r = new AssertSingleTX ();
		control.put(0);
		control2.put(0);
		Continuation.runWithContinuationSupport(r);
		System.out.println( "Finished control=?: " + control.get());
		System.out.println( "Finished control2=: " + control2.get());
	}
}
