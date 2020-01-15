package benchmark.synthetic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jvstm.Transaction;
//import jvstm.TransactionalFuture;
import jvstm.VBox;

public class ReadWriteArrayAccess implements ArrayAccessTransaction {
	private int num_of_read = 1;
	private int num_of_write = 1;
	private int array_length = 0;
	private int sibling = 0;
	private String high_contention = "false";
	private int cpu_work_amount_between_memory_read = 0;
//	ArrayList<VBox<Integer>> array;
	VBox<Integer>[] array;
	private int value;
	
	public ReadWriteArrayAccess(int read, int write, String high_contention, int amount){
		this.num_of_read = read;
		this.num_of_write = write;
		this.high_contention = high_contention;
		this.cpu_work_amount_between_memory_read = amount;
	}

	public int executeTransaction(int sibling) throws Throwable {
		
		this.sibling = sibling;
		array = ArrayAccess.getArray();
//		array_length = array.size();
		array_length = array.length;
		
		//RW Transaction, first read(use transactional future as needed),
		//then write, write has not enable transactional future support
		//sequential write(in randomized location)
		int read_val = read();
		write();
		return read_val;
		
	}

	private void write() {
		int index =0;
		for(int i=0; i<num_of_write;i++){
			if(high_contention.equalsIgnoreCase("false")){
				index = (int)(Math.random()*(array_length));
			}
			else if(high_contention.equals("true")){
//				System.out.println("in high contention");
				index = (int)(Math.random()*num_of_write);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(num_of_write*10));
			}
//			System.out.println("RW TXN WRITE INDEX: "+ index);
//			int j = array.get(index).get();
			int j = array[index].get();
			if(j>500){
//				array.get(index).put(j-(int)(Math.random()*1000));
				array[index].put(j-(int)(Math.random()*1000));
			}
			else{
//				array.get(index).put(j+(int)(Math.random()*1000));
				array[index].put(j+(int)(Math.random()*1000));
			}
		}
	}

	private int read() throws InterruptedException, ExecutionException {
//		long start = System.nanoTime();
		if(sibling == 0){
//			System.out.println("RW TXN Top Level Only, Read Number: "+num_of_read);
			
			for(int i = 0;i < num_of_read;i ++){
				//int index = (int)(Math.random()*(array_length));
				//before accessing memory, assign some CPU computation work
				double sqrt_amount = 0;
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				int index = (int)sqrt_amount % array_length;
//				System.out.println("index: "+index);
//				array.get(index).get();
				value = array[index].get();
			}
//			System.out.println("Top Level Only Duration: "+ (System.nanoTime()-start));
		}else{
			List<Future<Integer>> results = new ArrayList<Future<Integer>>();
			
			int partition = num_of_read/(sibling+1);
			int last_partition = num_of_read%(sibling+1);
			
			for(int i = 1 ; i < (sibling+1); i++){

//				TransactionalFuture<Integer> task = new TransactionalFuture<Integer>
//					(new AsynchronousOperation((i-1)*partition, i*partition),true);

//				results.add(Transaction.current().submitFuture(task));
			}
			AsynchronousOperation continuation =  new AsynchronousOperation(num_of_read-partition-last_partition,num_of_read);

			try {
				value = continuation.call();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			for(Future<Integer> e: results){
				while(!e.isDone()){}
				value = e.get();
			}
//			System.out.println("With Transactional Future Duration: "+ (System.nanoTime()-start));
		}
		return value;
		
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
//			System.out.println("RW TXN sibling for item num: "+ (max-min));
			for(int i = min;i < max;i++){
				//int index = (int)(Math.random()*(array_length));
				double sqrt_amount = 0;
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				int index = (int)sqrt_amount % array_length;
//				System.out.println("index is : "+index);
//				array.get(index).get();
				value = array[index].get();
			}
			return value;
		}


	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

}
