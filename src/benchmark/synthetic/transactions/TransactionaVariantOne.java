package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;
import benchmark.synthetic.ArrayAccess;
import benchmark.synthetic.ArrayAccessTransaction;

/**
 *  the continuation reads only, no writes, whereas its futures do write
 *  
 * */

public class TransactionaVariantOne implements ArrayAccessTransaction{
	private int num_of_read = 1;
	private int num_of_write = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private String high_contention = "false";
	VBox<Integer>[] array;
	private int value;
	
	public TransactionaVariantOne(int read, int write,String high_contention, int amount){
		this.num_of_read = read;
		this.num_of_write = write;
		this.high_contention = high_contention;
		this.cpu_work_amount_between_memory_read = amount;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {
		array = ArrayAccess.getArray();
		array_length = array.length;

		FutureRepresentation<String> tf = ((ReadWriteTransaction)tx).submitWeakOrderingFuture(new WeakFutureTask(new AsychOps()));

		while(true){
			try{
				//T2 logic
				//	Thread.sleep(1000);
				//	System.out.println("start T2 logic");
				value += array[0].get();
				read();
				//	write(value);
				String result = (String) Transaction.current().evalWeakOrderingFuture(tf);
				break;
			}catch(Throwable e){
				System.out.println("T2 restarting");
			}
		}
		return value;

	}
	
	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<String>{
		
		
		WeakFutureTask(Callable<String> c){
			super(c);
			
		}
		
		
	}
	
	class AsychOps implements Callable<String>{

		@Override
		public String call() throws Exception {
			
			
			//Tf logic
			array[0].put(1);
//			read();
//
//			write(value);
			longComputation();

			return "done";

		}

		private void longComputation() {

			if(cpu_work_amount_between_memory_read!=0){
				double sqrt_amount = 0;
				for(int j = 0; j < cpu_work_amount_between_memory_read*1000; j++){
					sqrt_amount += Math.sqrt(j);
				}
				int index1 = (int)sqrt_amount % array_length;
				
				value += array[index1].get();
//				System.out.println("index: "+index);
			}
		}

	}
	
	private void read() {
		for(int i = 0;i < num_of_read;i ++){
			//read N items at random in the array, on whose basis (sum/avg/std_dev of the values read) we change M items at random in the array

			int index = (int)(Math.random()*(array_length));

			//before accessing memory, assign some CPU computation work
			if(cpu_work_amount_between_memory_read!=0){
				double sqrt_amount = 0;
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				int index1 = (int)sqrt_amount % array_length;

				//System.out.println("index: "+index);
			}
			//				array.get(index).get();
			value += array[index].get();
		}
	}

	private void write(int value) {

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
				array[index].put(j-(int)(Math.random()*1000)+value);
			}
			else{
//				array.get(index).put(j+(int)(Math.random()*1000));
				array[index].put(j+(int)(Math.random()*1000)+value);
			}
		}
	
		
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public int executeTransaction(int sibling) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

}
