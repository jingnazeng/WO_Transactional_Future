package benchmark.vacation;

import java.util.concurrent.atomic.AtomicInteger;

import jvstm.Transaction;

public class Vacation {
	
	public static void abortWait(long INTERVAL){
	    long start = System.nanoTime();
	    long end=0;
	    do{
	        end = System.nanoTime();
	    }while(start + INTERVAL >= end);
	    //System.out.println(end - start);
	}
	
	public static AtomicInteger abortedTX = new AtomicInteger(0);
	public static AtomicInteger committedTX = new AtomicInteger(0);
	
	public static long potentialPara = 0;
	public static long txnSDuration = 0;
	public static long txnCDuration = 0;
	public static long validateDuration = 0 ;
	public static long ensurecommitstatus = 0;
	
    public Vacation() {
    }

    public static void displayUsage(String appName) {
	System.out.println("Usage: %s [options]\n" + appName);
	System.out.println("\nOptions:                                             (defaults)\n");
	System.out.println("    c <UINT>   Number of [c]lients                   (%i)\n" + Definitions.PARAM_DEFAULT_CLIENTS);
	System.out.println("    n <UINT>   [n]umber of user queries/transaction  (%i)\n" + Definitions.PARAM_DEFAULT_NUMBER);
	System.out.println("    q <UINT>   Percentage of relations [q]ueried     (%i)\n" + Definitions.PARAM_DEFAULT_QUERIES);
	System.out.println("    r <UINT>   Number of possible [r]elations        (%i)\n" + Definitions.PARAM_DEFAULT_RELATIONS);
	System.out
		.println("    t <UINT>   Number of [t]ransactions              (%i)\n" + Definitions.PARAM_DEFAULT_TRANSACTIONS);
	System.out.println("    u <UINT>   Percentage of [u]ser transactions     (%i)\n" + Definitions.PARAM_DEFAULT_USER);
	System.exit(1);
    }

    int CLIENTS;
    int NUMBER;
    int QUERIES;
    int RELATIONS;
    int TRANSACTIONS;
    int USER;

    public void setDefaultParams() {
	CLIENTS = Definitions.PARAM_DEFAULT_CLIENTS;
	NUMBER = Definitions.PARAM_DEFAULT_NUMBER;
	QUERIES = Definitions.PARAM_DEFAULT_QUERIES;
	RELATIONS = Definitions.PARAM_DEFAULT_RELATIONS;
	TRANSACTIONS = Definitions.PARAM_DEFAULT_TRANSACTIONS;
	USER = Definitions.PARAM_DEFAULT_USER;
    }

    public void parseArgs(String argv[]) {
	int opterr = 0;

	setDefaultParams();
	for (int i = 0; i < argv.length; i++) {
	    String arg = argv[i];
	    if (arg.equals("-c"))
		CLIENTS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-n"))
		NUMBER = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-q"))
		QUERIES = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-r"))
		RELATIONS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-t"))
		TRANSACTIONS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-u"))
		USER = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-nest"))
		Operation.transactionalFutureEnabled = Boolean.parseBoolean(argv[++i]);
	    else if (arg.equals("-sib"))
		Operation.numberOfTransactionalFuture = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-updatePar"))
		Operation.parallelizeUpdateTables = Boolean.parseBoolean(argv[++i]);
	    else if (arg.equals("-readOnly"))
		Operation.readOnly = Boolean.parseBoolean(argv[++i]);
	    else if (arg.equals("-unsafe"))
		Operation.unsafe = Boolean.parseBoolean(argv[++i]);
	    else
		opterr++;
	}

	if (opterr > 0) {
	    displayUsage(argv[0]);
	}
    }

    public Manager initializeManager() {
	int i;
	int t;
	// System.out.println("Initializing manager... ");

	Random randomPtr = new Random();
	randomPtr.random_alloc();
	Manager managerPtr = new Manager();

	int numRelation = RELATIONS;
	int ids[] = new int[numRelation];
	for (i = 0; i < numRelation; i++) {
	    ids[i] = i + 1;
	}

	for (t = 0; t < 4; t++) {

	    /* Shuffle ids */
	    for (i = 0; i < numRelation; i++) {
		int x = randomPtr.posrandom_generate() % numRelation;
		int y = randomPtr.posrandom_generate() % numRelation;
		int tmp = ids[x];
		ids[x] = ids[y];
		ids[y] = tmp;
	    }

	    /* Populate table */
	    for (i = 0; i < numRelation; i++) {
		boolean status = false;
		int id = ids[i];
		int num = ((randomPtr.posrandom_generate() % 5) + 1) * 100;
		int price = ((randomPtr.posrandom_generate() % 5) * 10) + 50;
		if (t == 0) {
		    status = managerPtr.manager_addCar(id, num, price);
		} else if (t == 1) {
		    status = managerPtr.manager_addFlight(id, num, price);
		} else if (t == 2) {
		    status = managerPtr.manager_addRoom(id, num, price);
		} else if (t == 3) {
		    status = managerPtr.manager_addCustomer(id);
		}
		assert (status);
	    }

	} /* for t */

	// System.out.println("done.");
	return managerPtr;
    }

    public Client[] initializeClients(Manager managerPtr) {
	Random randomPtr;
	Client clients[];
	int i;
	int numClient = CLIENTS;
	int numTransaction = TRANSACTIONS;
	int numTransactionPerClient;
	int numQueryPerTransaction = NUMBER;
	int numRelation = RELATIONS;
	int percentQuery = QUERIES;
	int queryRange;
	int percentUser = USER;

	// System.out.println("Initializing clients... ");

	randomPtr = new Random();
	randomPtr.random_alloc();

	clients = new Client[numClient];

	numTransactionPerClient = (int) ((double) numTransaction / (double) numClient + 0.5);
	queryRange = (int) (percentQuery / 100.0 * numRelation + 0.5);

	for (i = 0; i < numClient; i++) {
	    clients[i] = new Client(i, managerPtr, numTransactionPerClient, numQueryPerTransaction, queryRange, percentUser);
	}

	// System.out.println("done.");
	// System.out.println("    Transactions        = " + numTransaction);
	// System.out.println("    Clients             = " + numClient);
	// System.out.println("    Transactions/client = " +
	// numTransactionPerClient);
	// System.out.println("    Queries/transaction = " +
	// numQueryPerTransaction);
	// System.out.println("    Relations           = " + numRelation);
	// System.out.println("    Query percent       = " + percentQuery);
	// System.out.println("    Query range         = " + queryRange);
	// System.out.println("    Percent user        = " + percentUser);

	return clients;
    }

    void checkTables(Manager managerPtr) {
	int i;
	int numRelation = RELATIONS;
	RBTree<Integer, Customer> customerTablePtr = managerPtr.customerTable;
	RBTree<Integer, Reservation> tables[] = new RBTree[3];
	tables[0] = managerPtr.carTable;
	tables[1] = managerPtr.flightTable;
	tables[2] = managerPtr.roomTable;
	int numTable = 3;

	int t;

	// System.out.println("Checking tables... ");

	/* Check for unique customer IDs */
	int percentQuery = QUERIES;
	int queryRange = (int) (percentQuery / 100.0 * numRelation + 0.5);
	int maxCustomerId = queryRange + 1;
	for (i = 1; i <= maxCustomerId; i++) {
	    if (customerTablePtr.get(i) != null) {
		if (customerTablePtr.remove(i)) {
		    assert (customerTablePtr.get(i) == null);
		}
	    }
	}

	/* Check reservation tables for consistency and unique ids */
	for (t = 0; t < numTable; t++) {
	    RBTree<Integer, Reservation> tablePtr = tables[t];
	    for (i = 1; i <= numRelation; i++) {
		if (tablePtr.get(i) != null) {
		    boolean status = false;
		    if (t == 0) {
			status = managerPtr.manager_addCar(i, 0, 0);
		    } else if (t == 1) {
			status = managerPtr.manager_addFlight(i, 0, 0);
		    } else if (t == 2) {
			status = managerPtr.manager_addRoom(i, 0, 0);
		    }
		    assert (status);
		    if (tablePtr.remove(i)) {
			assert (!tablePtr.remove(i));
		    }
		}
	    }
	}

	// System.out.println("done.");
    }

    public static void main(String argv[]) throws InterruptedException {
	Manager manager;
	Client clients[];
	long start;
	long stop;

	/* Initialization */
	Vacation vac = new Vacation();
	vac.parseArgs(argv);
	manager = vac.initializeManager();
	clients = vac.initializeClients(manager);
	int numThread = vac.CLIENTS;

	/* Run transactions */
	// System.out.println("Running clients... ");

	// Barrier.setBarrier(numThread);

	start = System.currentTimeMillis();
	for (int i = 1; i < numThread; i++) {
	    clients[i].start();
	}
	clients[0].run();
	for (int i = 1; i < numThread; i++) {
	    clients[i].join();
	}

	stop = System.currentTimeMillis();

	// System.out.print("done.");
	long diff = stop - start;
//	if(Operation.transactionalFutureEnabled || vac.CLIENTS >1) {
		System.out.println("Execution_Time_Millis: "+diff);
		System.out.println("throughput: "+ (committedTX.get()*1.0)/(diff/1000));
		System.out.println("abort_rate: "+abortedTX.floatValue()/(abortedTX.get()+committedTX.get()));
		System.out.print(diff + " " + committedTX +" " +abortedTX  +" " +Transaction.getFuturesAborts() + " "+ Transaction.getInterFuturesAborts() + ";");
//	}
//	else{
//		System.out.println("Total_Duration:"+diff + " ");
//		System.out.println("potential: "+ Vacation.potentialPara);
//		System.out.println("start duration: "+ Vacation.txnSDuration);
//		System.out.println("commit duration: "+Vacation.txnCDuration);
//		System.out.println("validate duration: "+ Vacation.validateDuration);
//		System.out.println("ensure status duration: " + Vacation.ensurecommitstatus);
//	}
	vac.checkTables(manager);

	/* Clean up */
	// System.out.println("Deallocating memory... ");
	// System.out.println("done.");
    }

}
