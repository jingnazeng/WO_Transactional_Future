package benchmark.vacation;

import java.util.ArrayList;
import java.util.Collections;

import jvstm.CommitException;
import jvstm.Transaction;

public class Client extends Thread implements Runnable {
	boolean runningWithContinuationSupport = false;
    final int id;
    final Manager managerPtr;
    final Random randomPtr;
    final int numOperation;
    final int numQueryPerTransaction;
    final int queryRange;
    final int percentUser;
    public ArrayList<Integer> futureAborts = new ArrayList<Integer>();
    public ArrayList<Integer> futureEarlyAborts = new ArrayList<Integer>();
    

    /*
     * ==========================================================================
     * === client_alloc -- Returns NULL on failure
     * ==============================
     * ===============================================
     */
    public Client(int id, Manager managerPtr, int numOperation, int numQueryPerTransaction, int queryRange, int percentUser) {
	this.randomPtr = new Random();
	this.randomPtr.random_alloc();
	this.id = id;
	this.managerPtr = managerPtr;
	randomPtr.random_seed(id);
	this.numOperation = numOperation;
	this.numQueryPerTransaction = numQueryPerTransaction;
	this.queryRange = queryRange;
	this.percentUser = percentUser;
    }

    /*
     * ==========================================================================
     * === selectAction
     * ==========================================================
     * ===================
     */
    public int selectAction(int r, int percentUser) {
	if (r < percentUser) {
	    return Definitions.ACTION_MAKE_RESERVATION;
	} else if ((r & 1) == 1) {
	    return Definitions.ACTION_DELETE_CUSTOMER;
	} else {
	    return Definitions.ACTION_UPDATE_TABLES;
	}
    }

    /*
     * ==========================================================================
     * === client_run -- Execute list operations on the database
     * ================
     * =============================================================
     */
    @Override
    public void run() {
	Operation[] operations = new Operation[numOperation];
	
	for (int i = 0; i < numOperation; i++) {
	    int r = randomPtr.posrandom_generate() % 100;
	    int action = selectAction(r, percentUser);

	    if (action == Definitions.ACTION_MAKE_RESERVATION) {
		operations[i] = new MakeReservationOperation(managerPtr, randomPtr, numQueryPerTransaction, queryRange);
	    } else if (action == Definitions.ACTION_DELETE_CUSTOMER) {
		operations[i] = new DeleteCustomerOperation(managerPtr, randomPtr, queryRange);
	    } else if (action == Definitions.ACTION_UPDATE_TABLES) {
		operations[i] = new UpdateTablesOperation(managerPtr, randomPtr, numQueryPerTransaction, queryRange);
	    } else {
		assert (false);
	    }
	}

	for (int i = 0; i < numOperation; i++) {
	    try {
			operations[i].doOperation();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	    if(operations[i] instanceof MakeReservationOperation || operations[i] instanceof UpdateTablesOperation) {
		    futureAborts.add(operations[i].futureAborts);
		    futureEarlyAborts.add(operations[i].futuresEarlyAborts);
	    }
	}
    }

}

/*
 * =============================================================================
 * 
 * End of client.c
 * 
 * =============================================================================
 */

