package benchmark.vacation;

import java.util.concurrent.Callable;

import jvstm.WeakOrderingTransactionalFutureTask;

class WeakFutureTask<T> extends WeakOrderingTransactionalFutureTask<T>{
	WeakFutureTask(Callable<T> c){
		super(c);
	}
}
