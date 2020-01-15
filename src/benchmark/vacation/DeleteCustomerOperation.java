package benchmark.vacation;

import jvstm.CommitException;
import jvstm.Transaction;

public class DeleteCustomerOperation extends Operation {

    final private Manager managerPtr;
    final private int customerId;
    
    public DeleteCustomerOperation(Manager managerPtr, Random randomPtr, int queryRange) {
	this.managerPtr = managerPtr; 
	this.customerId = randomPtr.posrandom_generate() % queryRange + 1;
    }
    
    @Override
    public void doOperation() {
    	long interval = 100;
	while (true) {
	    Transaction tx = Transaction.begin();
	    // Debug.print("[Delete] Started top level tx in thread " +
	    // Thread.currentThread().getId() + " " + tx);
	    try {
		int bill = managerPtr.manager_queryCustomerBill(customerId);
		if (bill >= 0) {
		    managerPtr.manager_deleteCustomer(customerId);
		}
		tx.commit();
		Vacation.committedTX.incrementAndGet();
		// Debug.print("[Delete] Committed top level tx in thread " +
		// Thread.currentThread().getId() + " " + tx);
		tx = null;
		return;
	    } catch (CommitException ce) {
		tx.abort();
		Vacation.abortedTX.incrementAndGet();
		Vacation.abortWait(interval);
		interval *= 2;
		// Debug.print("[Delete] Aborted top level tx in thread " +
		// Thread.currentThread().getId() + " " + tx);
		tx = null;
	    } finally {
		if (tx != null) {
		    tx.abort();
		}
	    }
	}
    }

}
