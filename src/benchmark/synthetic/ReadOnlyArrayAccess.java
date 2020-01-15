package benchmark.synthetic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

public class ReadOnlyArrayAccess implements ArrayAccessTransaction{
	private int num_of_read = 1;
	private int array_length = 0;
	private int num_of_sibling = 0;
	private int cpu_work_between_memory_access = 0;
	private int value_read = 0;
	
	VBox<Integer>[] array;
 	
	public ReadOnlyArrayAccess(int read, int amount){
		this.num_of_read = read;
		this.cpu_work_between_memory_access = amount;
	}

	public int executeTransaction(int sibling) throws Throwable {
		
		num_of_sibling = sibling;
		array = ArrayAccess.getArray();
		array_length = array.length;
		return read();
		
	}

	private int read() throws InterruptedException, ExecutionException {
		
		if(num_of_sibling == 0){
//			System.out.println("Top Level Only, Read Number: "+num_of_read);
			
			for(int i = 0;i < num_of_read;i ++){
				double sqrt_amount = 0;
				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_between_memory_access; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				int index = (int)sqrt_amount % array_length;
				
				value_read = array[index].get();
			}
		}else{
			List<Future<Integer>> results = new ArrayList<Future<Integer>>();
			
			int partition = num_of_read/(num_of_sibling+1);
			int last_partition = num_of_read%(num_of_sibling+1);
			
			for(int i = 1 ; i < (num_of_sibling+1); i++){

				ParallelTask<Integer> task = new ParallelTask<Integer>
					(new AsynchronousOperation((i-1)*partition, i*partition));

//				results.add(Transaction.current().submitFuture(task));
			}
			
			AsynchronousOperation continuation =
					new AsynchronousOperation(num_of_read-partition-last_partition,num_of_read);

			try {
				value_read = continuation.call();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for(Future<Integer> e: results){

				while(!e.isDone()){}
				value_read = e.get();
			}
		}
		return value_read;
	}
	
	private class AsynchronousOperation implements Callable<Integer>{
		
		private int min;
		private int max;
		public AsynchronousOperation(int min, int max){
			this.min = min;
			this.max = max;
			
		}

		@Override
		public Integer call() throws Exception {
//			System.out.println("Sibling Calls, Read Number: "+ (max-min));
			for(int i = min;i < max;i++){
				double sqrt_amount = 0;
				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_between_memory_access; j++){
					sqrt_amount += Math.sqrt(j+i);
//					System.out.println("i: " + i + "j: "
//					+ j + "sqrt sum :"+ sqrt_amount);
				}
				int index = (int)sqrt_amount % array_length;
//				System.out.println("the index is: "+ index);
//				array.get(index).get();
				value_read = array[index].get();
			}
			return value_read;
		}
		
		
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {
		return executeTransaction(sibling);
	}
 
	
}
