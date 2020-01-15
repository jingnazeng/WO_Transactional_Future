package benchmark.synthetic.transactions;

import benchmark.synthetic.ArrayAccessTransaction;

public abstract class ArrayAccessWriteTxnWithMultipleFutures implements ArrayAccessTransaction {
	public boolean isReadOnly(){
		return false;
	};
}
