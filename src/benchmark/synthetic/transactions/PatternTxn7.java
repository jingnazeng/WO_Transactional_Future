package benchmark.synthetic.transactions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;


/**
 * Workload:
 * no contention
 * 1 top-level and 56 futures:
 * each future executes: 
 * for I in 1..NumberOfOperations {r(x),w(x);} 
 * *******
 * Y-axis: throughput
 * X-axis: NUMBER of operations in a future. N={1,…,100K}
 * One line: no spin => here JTF will win
 * Other line: 1K => here WRF will be as good as JTF
 * 
 * **/

public class PatternTxn7 extends ArrayAccessWriteTxnWithMultipleFutures{
	VBox<Integer>[] array;
	private int value;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private int num_of_operations;
	
	public PatternTxn7(int spin, int num_of_operations){
		this.cpu_work_amount_between_memory_read = spin;
		this.num_of_operations = num_of_operations;
	}

	@Override
	public int executeTransaction(int sibling) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

	public int executeFutCont(int num_of_futures, List<FutureRepresentation<Integer>> submittedFutures) {
		if (num_of_futures == 0) {
			return 0;
		}
		FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
				submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
		submittedFutures.add(f);
		while(true){
			try{
			//	readHotSpots(number_of_hot_spots_read_and_write);
				num_of_futures = executeFutCont(--num_of_futures, submittedFutures);
				return num_of_futures;
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
	
	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {

		array = ArrayAccess.getArray();
		array_length = array.length;

		FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
				submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
		List<FutureRepresentation<Integer>> submittedFutures = new ArrayList<FutureRepresentation<Integer>>();
		submittedFutures.add(f);
		while(true){
			try{
				//	readHotSpots(number_of_hot_spots_read_and_write);
				executeFutCont(sibling-1, submittedFutures);
				//System.out.println(String.format("number of submiited futures is %d from %d siblings"
				//submittedFutures.size(), sibling));
				for (FutureRepresentation<Integer> submittedFuture : submittedFutures)
					value += (Integer) Transaction.current().evalWeakOrderingFuture(submittedFuture);
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

		return value;
	}
	

	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<Integer>{
		WeakFutureTask(Callable<Integer> c){
			super(c);
		}
	}


	class AsychOpsFuture implements Callable<Integer>{

		public AsychOpsFuture(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf logic
			double sqrt_amount = 0;
			for(int i = 0; i < num_of_operations; i++){

				//no contention: read and write to different section of the array
				int threadNum = (int) (Thread.currentThread().getId() % 56);
				int partition = array_length / 56;
				int read_index = (int) (Math.random()*partition + threadNum*partition);
				value += array[read_index].get();

				int write_index = (int) (Math.random()*partition + threadNum*partition);	
				array[write_index].put((int)(Math.random()*1000));


				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
			}

			return (int) (value+sqrt_amount);

		}
	}


	
	
	
	
}