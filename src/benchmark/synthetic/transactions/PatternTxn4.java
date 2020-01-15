package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;

// by reading and writing a "hot spot" in the same array.

public class PatternTxn4 extends ArrayAccessWriteTxnWithMultipleFutures{
	VBox<Integer>[] array;
	private int value;
	private int num_of_write = 1;
	private int num_of_read = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private int number_of_hot_spots_read_and_write=10;
	private String high_contention = "false";
	
	public PatternTxn4(int read, int write, String high_contention, int spin){
		this.num_of_read = read;
		this.num_of_write = write;
		this.cpu_work_amount_between_memory_read = spin;
		this.high_contention = high_contention;
	}

	@Override
	public int executeTransaction(int sibling) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {

		array = ArrayAccess.getArray();
		array_length = array.length;
		
		FutureRepresentation<Integer> f1 = ((ReadWriteTransaction)tx).
				submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
		while(true){
			try{
				//T2 logic
//				value += array[1].get();
//				System.out.println("T2 read x: " + array[1].get());
				readHotSpots(number_of_hot_spots_read_and_write);
				//Tf2 logic
				FutureRepresentation<Integer> f2  = ((ReadWriteTransaction)Transaction.current()).
						submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture2()));

				while(true){
					try{
//						value += array[2].get();
//						System.out.println("T3 read y: " + array[2].get());
						readHotSpots(number_of_hot_spots_read_and_write);
						value += (Integer) Transaction.current().evalWeakOrderingFuture(f1);
						break;
					}catch(Throwable e){
						if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
							throw e;
						else{
							e.printStackTrace();
							System.out.println("T3 restarting");
						}
					}

				}
				value += (Integer) Transaction.current().evalWeakOrderingFuture(f2);
				break;
			}catch(Throwable e){
				if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
					throw e;
				else{
					e.printStackTrace();
					System.out.println("T2 restarting");
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


	class AsychOpsFuture1 implements Callable<Integer>{

		public AsychOpsFuture1(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			readSequentially();
//			int val = (int) (Math.random()*10);
//			array[1].put(val); 
//			System.out.println("Tf1 write x to be :" + val);
			writeHotSpots(number_of_hot_spots_read_and_write);
			return value;

		}
	}


	class AsychOpsFuture2 implements Callable<Integer>{

		public AsychOpsFuture2(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			readSequentially();
//			int val = (int) (Math.random()*10);
//			array[2].put(val); 
//			System.out.println("Tf2 write y to be :" + val);
			writeHotSpots(number_of_hot_spots_read_and_write);
			return value;

		}
		
	}
	
	
	private int readSequentially() {
		return read(0,num_of_read);
	}
	
	private int read(int min, int max) {
		for(int i = min;i < max;i ++){
				double sqrt_amount = 0;
				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				int index = ((int)(sqrt_amount+Math.random()*array_length)) % array_length;
				
				value = array[index].get();
			}
		return value;
	}
	
	private void writeHotSpots(int number_of_hot_spots_read_and_write) {

		int index =0;
		for(int i=0; i<number_of_hot_spots_read_and_write;i++){
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
			
//			array[index].put((int)(Math.random()*1000)+value);
			int j = 0;
			try{
			j = array[index].get();
			}catch(Exception e){
// 				System.out.println("READ conflict..");
				throw e;
			}
			try{
			if(j>500){
				array[index].put(j-(int)(Math.random()*1000));
			}
			else{
				array[index].put(j+(int)(Math.random()*1000));
			}
			}catch(Exception e){
//				System.out.println("write conflict..");
				throw e;
			}
		}
	
	}
	
	private void readHotSpots(int number_of_hot_spots_read_and_write) {
		int index = 0;
		for(int i = 0;i < number_of_hot_spots_read_and_write;i ++){
			if(high_contention.equalsIgnoreCase("false")){
				index = (int)(Math.random()*(array_length));
			}
			else if(high_contention.equals("true")){
				//System.out.println("in high contention");
				index = (int)(Math.random()*num_of_write);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(num_of_write*10));
			}
			try{
				value += array[index].get();
			}catch(Exception e){
				//System.out.println("READ conflict..");
				throw e;
			}
		}
	}
}
