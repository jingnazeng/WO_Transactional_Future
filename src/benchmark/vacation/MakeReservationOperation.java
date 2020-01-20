package benchmark.vacation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import contlib.Continuation;
import jvstm.CommitException;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
//import jvstm.util.NestedWorkUnit;
import jvstm.ParallelTask;
import jvstm.graph.FutureRepresentation;

public class MakeReservationOperation extends Operation {

    final private Manager manager;
    final private int[] types;
    final private int[] ids;
    final private int[] maxPrices;
    final private int[] maxIds;
    final private int customerId;
    final private int numQuery;

    public MakeReservationOperation(Manager manager, Random random, int numQueryPerTx, int queryRange) {
	this.manager = manager;
	this.types = new int[numQueryPerTx];
	this.ids = new int[numQueryPerTx];

	this.maxPrices = new int[Definitions.NUM_RESERVATION_TYPE];
	this.maxIds = new int[Definitions.NUM_RESERVATION_TYPE];
	this.maxPrices[0] = -1;
	this.maxPrices[1] = -1;
	this.maxPrices[2] = -1;
	this.maxIds[0] = -1;
	this.maxIds[1] = -1;
	this.maxIds[2] = -1;
	int n;
	this.numQuery = numQueryPerTx;
	this.customerId = random.posrandom_generate() % queryRange + 1;

	int[] baseIds = new int[20];
	for (int i = 0; i < 20; i++) {
	    baseIds[i] = (random.random_generate() % queryRange) + 1;
	}

	for (n = 0; n < numQuery; n++) {
	    types[n] = random.random_generate() % Definitions.NUM_RESERVATION_TYPE;
	    ids[n] = baseIds[n % 20];
	}
    }

    @Override
    public void doOperation() throws Throwable {
    	long interval = 100;
	while (true) {
		long txnS = System.currentTimeMillis();
	    Transaction tx = Transaction.begin();
	    Vacation.txnSDuration += (System.currentTimeMillis() - txnS);
	    // Debug.print("[Reserve] Started top level tx in thread " +
	    // Thread.currentThread().getId() + " " + tx);
//	    try {
//		if (Operation.nestedParallelismOn) {
//		    makeReservation(false);
//		} else if (Operation.unsafe) {
//		    makeReservation(true);
//		} else {
//		    makeReservationNotNested();
//		}
	    try {
		if (Operation.transactionalFutureEnabled && Operation.readOnly) {
		    makeReservation(true);
		} else if (Operation.transactionalFutureEnabled) {
		    makeReservation(false);
		} else {
		    makeReservationNotNested();
		}
		long txnCTiming = System.currentTimeMillis();
		Transaction.commit();
		Vacation.txnCDuration += (System.currentTimeMillis() - txnCTiming);
		// Debug.print("[Reserve] Committed top level tx in thread " +
		// Thread.currentThread().getId() + " " + tx);
		tx = null;
		Vacation.committedTX.incrementAndGet();
		return;
	    } catch (CommitException ce) {
		Transaction.abort();
		Vacation.abortedTX.incrementAndGet();
		Vacation.abortWait(interval);
		interval *= 2;
		// Debug.print("[Reserve] Aborted top level tx in thread " +
		// Thread.currentThread().getId() + " " + tx);
		tx = null;
	    } finally {
		if (tx != null) {
			Transaction.abort();
		}
	    }
	}
    }
    
    private class AsyncOperation implements Callable<Boolean>{
    	private final int min;
    	private final int max;
    	
    	public AsyncOperation(int min, int max) {
    		this.min = min;
    		this.max = max;
    	}
    	
    	@Override
    	public Boolean call() {
    		   boolean isFound = false;
    		    int n;
    		    long startinfuture = System.currentTimeMillis();
    		    for (n = min; n < max; n++) {
    			int t = types[n];
    			int id = ids[n];
    			int price = -1;
    			if (t == Definitions.RESERVATION_CAR) {
    			    if (manager.manager_queryCar(id) >= 0) {
    				price = manager.manager_queryCarPrice(id);
    			    }
    			} else if (t == Definitions.RESERVATION_FLIGHT) {
    			    if (manager.manager_queryFlight(id) >= 0) {
    				price = manager.manager_queryFlightPrice(id);
    			    }
    			} else if (t == Definitions.RESERVATION_ROOM) {
    			    if (manager.manager_queryRoom(id) >= 0) {
    				price = manager.manager_queryRoomPrice(id);
    			    }
    			} else {
    			    assert (false);
    			}
    			if (price > maxPrices[t]) {
    			    maxPrices[t] = price;
    			    maxIds[t] = id;
    			    isFound = true;
    			}
    		    }
//    		    System.out.println((max-min)+" items and the future processing duration"+(System.currentTimeMillis()-startinfuture));
    		    return isFound;
    		
    	}
    }

    private class NestedWorker extends ParallelTask<Boolean> {

    	private final boolean readOnly;

    	public NestedWorker(boolean readOnly, Callable c) {
    		super(c);
    		this.readOnly = readOnly;
    	}

    	@Override
    	protected boolean isReadOnly() {
    		return readOnly;
    	}

    	@Override
    	public Boolean execute() throws Throwable {
    		//	    boolean isFound = false;
    		//	    int n;
    		//	    for (n = min; n < max; n++) {
    		//		int t = types[n];
    		//		int id = ids[n];
    		//		int price = -1;
    		//		if (t == Definitions.RESERVATION_CAR) {
    		//		    if (manager.manager_queryCar(id) >= 0) {
    		//			price = manager.manager_queryCarPrice(id);
    		//		    }
    		//		} else if (t == Definitions.RESERVATION_FLIGHT) {
    		//		    if (manager.manager_queryFlight(id) >= 0) {
    		//			price = manager.manager_queryFlightPrice(id);
    		//		    }
    		//		} else if (t == Definitions.RESERVATION_ROOM) {
    		//		    if (manager.manager_queryRoom(id) >= 0) {
    		//			price = manager.manager_queryRoomPrice(id);
    		//		    }
    		//		} else {
    		//		    assert (false);
    		//		}
    		//		if (price > maxPrices[t]) {
    		//		    maxPrices[t] = price;
    		//		    maxIds[t] = id;
    		//		    isFound = true;
    		//		}
    		//	    }
    		//	    return isFound;
    		assert(false);
    		return false;
    	}

    }

    private void makeReservationNotNested() {
	boolean isFound = false;
	int n;
//	long startTiming = System.currentTimeMillis();
	for (n = 0; n < numQuery; n++) {
	    int t = types[n];
	    int id = ids[n];
	    int price = -1;
	    if (t == Definitions.RESERVATION_CAR) {
		if (manager.manager_queryCar(id) >= 0) {
		    price = manager.manager_queryCarPrice(id);
		}
	    } else if (t == Definitions.RESERVATION_FLIGHT) {
		if (manager.manager_queryFlight(id) >= 0) {
		    price = manager.manager_queryFlightPrice(id);
		}
	    } else if (t == Definitions.RESERVATION_ROOM) {
		if (manager.manager_queryRoom(id) >= 0) {
		    price = manager.manager_queryRoomPrice(id);
		}
	    } else {
		assert (false);
	    }
	    if (price > maxPrices[t]) {
		maxPrices[t] = price;
		maxIds[t] = id;
		isFound = true;
	    }
	}
//	long finishTiming = System.currentTimeMillis();
//	System.out.println(numQuery+" items and the sequential durations "+(finishTiming - startTiming));
//	Vacation.potentialPara += finishTiming - startTiming;

	if (isFound) {
	    manager.manager_addCustomer(customerId);
	}
	if (maxIds[Definitions.RESERVATION_CAR] > 0) {
	    manager.manager_reserveCar(customerId, maxIds[Definitions.RESERVATION_CAR]);
	}
	if (maxIds[Definitions.RESERVATION_FLIGHT] > 0) {
	    manager.manager_reserveFlight(customerId, maxIds[Definitions.RESERVATION_FLIGHT]);
	}
	if (maxIds[Definitions.RESERVATION_ROOM] > 0) {
	    manager.manager_reserveRoom(customerId, maxIds[Definitions.RESERVATION_ROOM]);
	}
    }
    
    
    
    private void makeReservation(boolean readOnly, boolean streamline) throws Throwable {
	boolean isFound = false;
	boolean isFoundinContinuation = false;

	int queriesPerTx = numQuery / Operation.numberOfTransactionalFuture ;
	  
	  FutureRepresentation<Boolean> f = ((ReadWriteTransaction)Transaction.current()).
				submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
		List<FutureRepresentation<Boolean>> submittedFutures = new ArrayList<FutureRepresentation<Boolean>>();
		submittedFutures.add(f);
		while(true){
			try{
				executeFutCont(Operation.numberOfTransactionalFuture-1, submittedFutures);
				//System.out.println(String.format("number of submiited futures is %d from %d siblings"
				//submittedFutures.size(), sibling));
				for (FutureRepresentation<Boolean> submittedFuture : submittedFutures){
					isFound = (Boolean) Transaction.current().evalWeakOrderingFuture(submittedFuture);
					if(isFound = true)
						break;
				}
				break;
			}catch(Throwable e){
				if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else{
					e.printStackTrace();
					System.out.println("continuation restarting");
				}
			}
		}
	  


	if (isFound) {
	    manager.manager_addCustomer(customerId);
	}
	if (maxIds[Definitions.RESERVATION_CAR] > 0) {
	    manager.manager_reserveCar(customerId, maxIds[Definitions.RESERVATION_CAR]);
	}
	if (maxIds[Definitions.RESERVATION_FLIGHT] > 0) {
	    manager.manager_reserveFlight(customerId, maxIds[Definitions.RESERVATION_FLIGHT]);
	}
	if (maxIds[Definitions.RESERVATION_ROOM] > 0) {
	    manager.manager_reserveRoom(customerId, maxIds[Definitions.RESERVATION_ROOM]);
	}
    }

    private void makeReservation(boolean readOnly) throws Throwable {
	boolean isFound = false;
	boolean isFoundinContinuation = false;

	int queriesPerTx = numQuery / Operation.numberOfTransactionalFuture ;
	  
	  FutureRepresentation<Boolean> f = ((ReadWriteTransaction)Transaction.current()).
				submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
		List<FutureRepresentation<Boolean>> submittedFutures = new ArrayList<FutureRepresentation<Boolean>>();
		submittedFutures.add(f);
		while(true){
			try{
				executeFutCont(Operation.numberOfTransactionalFuture-1, submittedFutures);
				//System.out.println(String.format("number of submiited futures is %d from %d siblings"
				//submittedFutures.size(), sibling));
				for (FutureRepresentation<Boolean> submittedFuture : submittedFutures){
					isFound = (Boolean) Transaction.current().evalWeakOrderingFuture(submittedFuture);
					if(isFound = true)
						break;
				}
				break;
			}catch(Throwable e){
				if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else{
					e.printStackTrace();
					System.out.println("continuation restarting");
				}
			}
		}
	  


	if (isFound) {
	    manager.manager_addCustomer(customerId);
	}
	if (maxIds[Definitions.RESERVATION_CAR] > 0) {
	    manager.manager_reserveCar(customerId, maxIds[Definitions.RESERVATION_CAR]);
	}
	if (maxIds[Definitions.RESERVATION_FLIGHT] > 0) {
	    manager.manager_reserveFlight(customerId, maxIds[Definitions.RESERVATION_FLIGHT]);
	}
	if (maxIds[Definitions.RESERVATION_ROOM] > 0) {
	    manager.manager_reserveRoom(customerId, maxIds[Definitions.RESERVATION_ROOM]);
	}
    }

	public int executeFutCont(int num_of_futures, List<FutureRepresentation<Boolean>> submittedFutures) {
		if (num_of_futures == 0) {
			return 0;
		}
		int queriesPerTx = numQuery / Operation.numberOfTransactionalFuture ;
		FutureRepresentation<Boolean> f = ((ReadWriteTransaction)Transaction.current()).
				submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
		submittedFutures.add(f);
		while(true){
			try{
				num_of_futures = executeFutCont(--num_of_futures, submittedFutures);
				return num_of_futures;
			}catch(Throwable e){
				if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else{
					e.printStackTrace();
					System.out.println("continuation restarting");
				}
			}
		}
	}

}
