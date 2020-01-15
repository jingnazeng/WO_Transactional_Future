package benchmark.vacation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jvstm.CommitException;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
//import jvstm.util.NestedWorkUnit;
import jvstm.ParallelTask;
//import stamp.vacation.jvstm.treemap.Definitions;
import jvstm.graph.FutureRepresentation;

public class UpdateTablesOperation extends Operation {

    final private Manager managerPtr;
    final private int[] types;
    final private int[] ids;
    final private int[] ops;
    final private int[] prices;
    final private int numUpdate;

    public UpdateTablesOperation(Manager managerPtr, Random randomPtr, int numQueryPerTransaction, int queryRange) {
	this.managerPtr = managerPtr;
	this.types = new int[numQueryPerTransaction];
	this.ids = new int[numQueryPerTransaction];
	this.ops = new int[numQueryPerTransaction];
	this.prices = new int[numQueryPerTransaction];

	int[] baseIds = new int[20];
	for (int i = 0; i < 20; i++) {
	    baseIds[i] = (randomPtr.random_generate() % queryRange) + 1;
	}
	
	this.numUpdate = numQueryPerTransaction;
	int n;
	for (n = 0; n < numUpdate; n++) {
	    types[n] = randomPtr.posrandom_generate() % Definitions.NUM_RESERVATION_TYPE;
	    ids[n] = baseIds[n % 20];
	    ops[n] = randomPtr.posrandom_generate() % 2;
	    if (ops[n] == 1) {
		prices[n] = ((randomPtr.posrandom_generate() % 5) * 10) + 50;
	    }
	}
    }

    @Override
    public void doOperation() throws Throwable {
    	long interval = 100;
	while (true) {
	    Transaction tx = Transaction.begin();
	    // Debug.print("[Update] Started top level tx in thread " +
	    // Thread.currentThread().getId() + " " + tx);
	    try {
		if (Operation.transactionalFutureEnabled && Operation.parallelizeUpdateTables) {
		    updateTables();
		} else {
		    updateTablesNotNested();
		}
		Transaction.commit();
		// Debug.print("[Update] Committed top level tx in thread " +
		// Thread.currentThread().getId() + " " + tx);
		tx = null;
		Vacation.committedTX.incrementAndGet();
		return;
	    } catch (CommitException ce) {
		Transaction.abort();
		Vacation.abortedTX.incrementAndGet();
		Vacation.abortWait(interval);
		interval *= 2;
		// Debug.print("[Update] Aborted top level tx in thread " +
		// Thread.currentThread().getId() + " " + tx);
		tx = null;
	    } finally {
		if (tx != null) {	
			Transaction.abort();
		}
	    }
	}
    }
    
    private class AsyncOperation implements Callable<Void>{
    	private List<Integer> operations;
    	
    	public AsyncOperation(List<Integer> operations) {
    		this.operations = operations;
    	}
    	
    	@Override
    	public Void call() {
    		for (int n : operations) {
    			int t = types[n];
    			int id = ids[n];
    			int doAdd = ops[n];
    			if (doAdd == 1) {
    			    int newPrice = prices[n];
    			    if (t == Definitions.RESERVATION_CAR) {
    				managerPtr.manager_addCar(id, 100, newPrice);
    			    } else if (t == Definitions.RESERVATION_FLIGHT) {
    				managerPtr.manager_addFlight(id, 100, newPrice);
    			    } else if (t == Definitions.RESERVATION_ROOM) {
    				managerPtr.manager_addRoom(id, 100, newPrice);
    			    } else {
    				assert (false);
    			    }
    			} else { /* do delete */
    			    if (t == Definitions.RESERVATION_CAR) {
    				managerPtr.manager_deleteCar(id, 100);
    			    } else if (t == Definitions.RESERVATION_FLIGHT) {
    				managerPtr.manager_deleteFlight(id);
    			    } else if (t == Definitions.RESERVATION_ROOM) {
    				managerPtr.manager_deleteRoom(id, 100);
    			    } else {
    				assert (false);
    			    }
    			}
    		    }
    		    return null;	
    	}
    }

    private class NestedWorker extends ParallelTask<Void> {
    	
    	public NestedWorker(Callable c) {
    		super(c);
    	}

	@Override
	public Void execute() throws Throwable {
//	    for (int n : operations) {
//		int t = types[n];
//		int id = ids[n];
//		int doAdd = ops[n];
//		if (doAdd == 1) {
//		    int newPrice = prices[n];
//		    if (t == Definitions.RESERVATION_CAR) {
//			managerPtr.manager_addCar(id, 100, newPrice);
//		    } else if (t == Definitions.RESERVATION_FLIGHT) {
//			managerPtr.manager_addFlight(id, 100, newPrice);
//		    } else if (t == Definitions.RESERVATION_ROOM) {
//			managerPtr.manager_addRoom(id, 100, newPrice);
//		    } else {
//			assert (false);
//		    }
//		} else { /* do delete */
//		    if (t == Definitions.RESERVATION_CAR) {
//			managerPtr.manager_deleteCar(id, 100);
//		    } else if (t == Definitions.RESERVATION_FLIGHT) {
//			managerPtr.manager_deleteFlight(id);
//		    } else if (t == Definitions.RESERVATION_ROOM) {
//			managerPtr.manager_deleteRoom(id, 100);
//		    } else {
//			assert (false);
//		    }
//		}
//	    }
		assert(false);
	    return null;
	}

    }

    private void updateTables() throws Throwable {
	int n;
	
//	List<Callable<Void>> workers = new ArrayList<Callable<Void>>();
	NestedWorker worker;
	List<Future<Void>> results = new ArrayList<Future<Void>>();
	List<Integer> type1 = new ArrayList<Integer>();
	List<Integer> type2 = new ArrayList<Integer>();
	List<Integer> type3 = new ArrayList<Integer>();
	for (n = 0; n < numUpdate; n ++) {
	    int t = types[n];
	    if (t == Definitions.RESERVATION_CAR) {
		type1.add(n);
	    } else if (t == Definitions.RESERVATION_FLIGHT) {
		type2.add(n);
	    } else if (t == Definitions.RESERVATION_ROOM) {
		type3.add(n);
	    }
	}
	
	FutureRepresentation<Void> f1 = ((ReadWriteTransaction)Transaction.current()).
			submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(type1)));
	FutureRepresentation<Void> f2;
	FutureRepresentation<Void> f3;
	while(true){
		try{
			//T2 logic empty
			//Tf2 logic
			f2  = ((ReadWriteTransaction)Transaction.current()).
					submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(type2)));

			while(true){
				try{
					//T3 logic Empty
					//Tf3 logic
					f3  = ((ReadWriteTransaction)Transaction.current()).
							submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(type3)));
					while(true){
								try{
									//T4 logic empty
									Transaction.current().evalWeakOrderingFuture(f1);
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

					Transaction.current().evalWeakOrderingFuture(f2);
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
			Transaction.current().evalWeakOrderingFuture(f3);
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

}



    private void updateTablesNotNested() {
	int n;
	for (n = 0; n < numUpdate; n++) {
	    int t = types[n];
	    int id = ids[n];
	    int doAdd = ops[n];
	    if (doAdd == 1) {
		int newPrice = prices[n];
		if (t == Definitions.RESERVATION_CAR) {
		    managerPtr.manager_addCar(id, 100, newPrice);
		} else if (t == Definitions.RESERVATION_FLIGHT) {
		    managerPtr.manager_addFlight(id, 100, newPrice);
		} else if (t == Definitions.RESERVATION_ROOM) {
		    managerPtr.manager_addRoom(id, 100, newPrice);
		} else {
		    assert (false);
		}
	    } else { /* do delete */
		if (t == Definitions.RESERVATION_CAR) {
		    managerPtr.manager_deleteCar(id, 100);
		} else if (t == Definitions.RESERVATION_FLIGHT) {
		    managerPtr.manager_deleteFlight(id);
		} else if (t == Definitions.RESERVATION_ROOM) {
		    managerPtr.manager_deleteRoom(id, 100);
		} else {
		    assert (false);
		}
	    }
	}
    }

}
