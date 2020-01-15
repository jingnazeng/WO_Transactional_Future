package benchmark.synthetic;

import jvstm.Transaction;

public interface ArrayAccessTransaction {

	public int executeTransaction(int sibling) throws Throwable;
	
	public boolean isReadOnly();

	int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable;

}
