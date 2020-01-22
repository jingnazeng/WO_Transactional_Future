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
import jvstm.graph.Node;
import jvstm.graph.Node.Status;

public class MakeReservationOperation extends Operation {

	final private Manager manager;
	final private int[] types;
	final private int[] ids;
	final private int[] maxPrices;
	final private int[] maxIds;
	final private int customerId;
	final private int numQuery;
	final private int max_num_of_core = 8;

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
					makeReservation(false, 0);
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

	private class AsyncOperation implements Callable<Boolean> {
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

				// add random thread halt to simulate remote database access
				 // define the range 
		        int max = 1000; 
		        int min = 1; 
		        int range = max - min + 1; 
				int random = (int)(Math.random() * range) + min; 
				if (random < 2) {
					try {
						System.out.println("random is " + random);
						System.out.println("to suspend for 10 millisecond");
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

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
			// boolean isFound = false;
			// int n;
			// for (n = min; n < max; n++) {
			// int t = types[n];
			// int id = ids[n];
			// int price = -1;
			// if (t == Definitions.RESERVATION_CAR) {
			// if (manager.manager_queryCar(id) >= 0) {
			// price = manager.manager_queryCarPrice(id);
			// }
			// } else if (t == Definitions.RESERVATION_FLIGHT) {
			// if (manager.manager_queryFlight(id) >= 0) {
			// price = manager.manager_queryFlightPrice(id);
			// }
			// } else if (t == Definitions.RESERVATION_ROOM) {
			// if (manager.manager_queryRoom(id) >= 0) {
			// price = manager.manager_queryRoomPrice(id);
			// }
			// } else {
			// assert (false);
			// }
			// if (price > maxPrices[t]) {
			// maxPrices[t] = price;
			// maxIds[t] = id;
			// isFound = true;
			// }
			// }
			// return isFound;
			assert (false);
			return false;
		}

	}

	private void makeReservationNotNested() {
		boolean isFound = false;
		int n;
//	long startTiming = System.currentTimeMillis();
		int nextOp = 0;
		while (nextOp < max_num_of_core) {
			nextOp++;
			for (n = 0; n < numQuery; n++) {
				int t = types[n];
				int id = ids[n];
				int price = -1;

				// add random thread halt to simulate remote database access
				 // define the range 
		        int max = 1000; 
		        int min = 1; 
		        int range = max - min + 1; 
				int random = (int)(Math.random() * range) + min; 
				if (random < 2) {
					try {
						System.out.println("random is " + random);
						System.out.println("to suspend for 10 millisecond");
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

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
	
	/**
	 * 
	 * streamingEnabled = 3 is the correct version that evaluates a future as soon as it finishes
	streamingEnabled = 2 is the buggy version of (3) because it adds a node in the graph in a wrong order (submission order) not the order they complete
	streamingEnabled = 1 is similar to JTF's streaming, as soon as the first submitted future finishes, a new future is submitted
	streamingEnabled = 0 is the older version where a futures are submitted in batches, and new futures are only submitted when all the batches completes
	 * 
	 * **/

	private void makeReservation(boolean readOnly, int streamingEnabled) throws Throwable {
		boolean isFound = false;
		boolean isFoundinContinuation = false;
		boolean value = false;

		int queriesPerTx = numQuery / Operation.numberOfTransactionalFuture;
		List<FutureRepresentation<Boolean>> submittedFutures = new ArrayList<FutureRepresentation<Boolean>>(
				Operation.numberOfTransactionalFuture);
		if (streamingEnabled == 3) {
			int nextOp = 0;
			int spawnedFutures = 0;

			while (nextOp < max_num_of_core) {

				FutureRepresentation<Boolean> f = ((ReadWriteTransaction) Transaction.current())
						.submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));

				submittedFutures.add(f);
				spawnedFutures++;
				nextOp++;
				if (spawnedFutures >= Operation.numberOfTransactionalFuture) {
					while (spawnedFutures >= Operation.numberOfTransactionalFuture) {

						int i = 0;

						for (i = 0; i < submittedFutures.size(); i++) {
							FutureRepresentation<Boolean> submittedFuture = submittedFutures.get(i);
							Node node = submittedFuture.getFuture_node();
							if (node.getStatus().equals(Status.completed)
									|| node.getStatus().equals(Status.iCommitted)) {
								Transaction.current().evalWeakOrderingFuture(submittedFuture);

								submittedFuture.setEvaluated();
								spawnedFutures--;
								submittedFutures.remove(i);

								value = (Boolean) submittedFuture.getF_ref().get();

								break;
							}
						}
					}
				}
			}
			for (FutureRepresentation<Boolean> submittedFuture : submittedFutures) {
				if (!submittedFuture.isEvaluated()) {
					Transaction.current().evalWeakOrderingFuture(submittedFuture);
					submittedFuture.setEvaluated();
				}
				value = (Boolean) submittedFuture.getF_ref().get();
			}
		} else if (streamingEnabled == 2) {
			int nextOp = 0;
			int spawnedFutures = 0;
			// List<FutureRepresentation<Integer>> toBeEvaluatedFutures = new
			// ArrayList<FutureRepresentation<Integer>>(sibling);

			while (nextOp < max_num_of_core) {

				FutureRepresentation<Boolean> f = ((ReadWriteTransaction) Transaction.current())
						.submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));

				submittedFutures.add(f);
				spawnedFutures++;
				nextOp++;
				// if(spawnedFutures >= sibling) {
				while (spawnedFutures >= Operation.numberOfTransactionalFuture) {

					int i = 0;

					for (i = 0; i < submittedFutures.size(); i++) {
						FutureRepresentation<Boolean> submittedFuture = submittedFutures.get(i);
						if (!submittedFuture.isEvaluated()) {

							Transaction.current().evalWeakOrderingFuture(submittedFuture);

							submittedFuture.setEvaluated();
						}
						Node node = submittedFuture.getFuture_node();
						if (node.getStatus().equals(Status.iCommitted)) {
							spawnedFutures--;
							submittedFutures.remove(i);

							value = (Boolean) submittedFuture.getF_ref().get();

							break;
						}
					}
				}
				// }
			}
			for (FutureRepresentation<Boolean> submittedFuture : submittedFutures) {
				if (!submittedFuture.isEvaluated()) {
					Transaction.current().evalWeakOrderingFuture(submittedFuture);
					submittedFuture.setEvaluated();
				}
				value = (Boolean) submittedFuture.getF_ref().get();
			}
		} else if (streamingEnabled == 1) {
			int spawnedFutures = 0;
			int nextOp = 0;

			while (nextOp < max_num_of_core) {
				while (spawnedFutures < Operation.numberOfTransactionalFuture) {
					nextOp++;
					spawnedFutures++;
					FutureRepresentation<Boolean> f = ((ReadWriteTransaction) Transaction.current())
							.submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
					submittedFutures.add(f);

				}
				Transaction.current().evalWeakOrderingFuture(submittedFutures.get(0));
				submittedFutures.get(0).setEvaluated();
				value = (Boolean) submittedFutures.get(0).getF_ref().get();
				submittedFutures.remove(0);
				spawnedFutures--;
			}

			for (int m = 0; m < submittedFutures.size(); m++) {
				Transaction.current().evalWeakOrderingFuture(submittedFutures.get(m));
				submittedFutures.get(m).setEvaluated();
				value = (Boolean) submittedFutures.get(m).getF_ref().get();
			}
		} else {
			int spawnedFutures = 0;
			int nextOp = 0;

			while (nextOp < max_num_of_core) {
				while (spawnedFutures < Operation.numberOfTransactionalFuture) {
					nextOp++;
					spawnedFutures++;
					FutureRepresentation<Boolean> f = ((ReadWriteTransaction) Transaction.current())
							.submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
					submittedFutures.add(f);
				}
				for (int m = 0; m < submittedFutures.size(); m++) {
					Transaction.current().evalWeakOrderingFuture(submittedFutures.get(m));
					submittedFutures.get(m).setEvaluated();
					value = (Boolean) submittedFutures.get(m).getF_ref().get();
				}
				submittedFutures.removeAll(submittedFutures);
				spawnedFutures = 0;
			}
		}

		isFound = value;

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

		int queriesPerTx = numQuery / Operation.numberOfTransactionalFuture;

		FutureRepresentation<Boolean> f = ((ReadWriteTransaction) Transaction.current())
				.submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
		List<FutureRepresentation<Boolean>> submittedFutures = new ArrayList<FutureRepresentation<Boolean>>();
		submittedFutures.add(f);
		while (true) {
			try {
				executeFutCont(Operation.numberOfTransactionalFuture - 1, submittedFutures);
				// System.out.println(String.format("number of submiited futures is %d from %d
				// siblings"
				// submittedFutures.size(), sibling));
				for (FutureRepresentation<Boolean> submittedFuture : submittedFutures) {
					isFound = (Boolean) Transaction.current().evalWeakOrderingFuture(submittedFuture);
					if (isFound = true)
						break;
				}
				break;
			} catch (Throwable e) {
				if (e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else {
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
		int queriesPerTx = numQuery / Operation.numberOfTransactionalFuture;
		FutureRepresentation<Boolean> f = ((ReadWriteTransaction) Transaction.current())
				.submitWeakOrderingFuture(new WeakFutureTask(new AsyncOperation(0, queriesPerTx)));
		submittedFutures.add(f);
		while (true) {
			try {
				num_of_futures = executeFutCont(--num_of_futures, submittedFutures);
				return num_of_futures;
			} catch (Throwable e) {
				if (e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else {
					e.printStackTrace();
					System.out.println("continuation restarting");
				}
			}
		}
	}

}
