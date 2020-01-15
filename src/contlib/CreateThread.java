package contlib;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//this class is a stub between the client app and the jdk callable and runnable classes. its purpose is to add continuation support
// to new client program threads.

public class CreateThread<T> implements Runnable, Callable<T> {
	
	private Runnable newRunnable = null;
	private Callable<T> newCallable = null;
	private Integer ticket = 0;
	private ArrayList<T> results = new ArrayList<T>();
	protected static final ThreadLocal<Integer> myTicket = new ThreadLocal<Integer>();

	@SuppressWarnings("unchecked")
	public CreateThread(Object newThread){
		myTicket.set(ticket);
		ticket += 1;
		
		if(newThread instanceof Runnable){
			newRunnable = (Runnable) newThread;
		}
		else if(newThread instanceof Callable){
			newCallable = (Callable<T>) newThread;
		}
		else
			throw new RuntimeException("JavaFutures: Cannot Create Thread");
	}
	
	@Override
	public void run() {	
		if(newRunnable != null){
			newRunnable.run();
		}
		else{
			try {
				results.add(myTicket.get(),newCallable.call());
			} catch (Exception e) {
				throw new  RuntimeException(e);
			}
		}
			
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public T call() throws InterruptedException, ExecutionException {
		Continuation.runWithContinuationSupport(this); //hoping this wont create a different thread;
		return results.get(myTicket.get());
	}

}
