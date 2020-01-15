package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import benchmark.synthetic.ArrayAccessTransaction;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;

public class BaseTransaction implements ArrayAccessTransaction{
	private int num_of_read = 1;
	private int num_of_write = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private String high_contention = "false";
	private boolean seqExe;
	VBox<Integer>[] array;
	private int value;
	
	public BaseTransaction(int read, int write,String high_contention, int amount, boolean seqExe){
		this.num_of_read = read;
		this.num_of_write = write;
		this.high_contention = high_contention;
		this.cpu_work_amount_between_memory_read = amount;
		this.seqExe = seqExe;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {
		array = ArrayAccess.getArray();
		array_length = array.length;

		if(seqExe){
			int value_read_sum = read();

			write(value_read_sum);
			
			int value_read = read();
			write(value_read);

		}else{
			//T1 logic
			FutureRepresentation<?> tf = ((ReadWriteTransaction)tx).submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
			while(true){
				try{
					//T2 logic
					//	Thread.sleep(1000);
					//	System.out.println("start T2 logic");
					int value_read = read();
					write(value_read);
					String result = (String) Transaction.current().evalWeakOrderingFuture(tf);
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
			//T3 logic
			//	System.out.println("T3 read X: " + TransactionTest.control_X.get());
		}
		return value;

	}
	
	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<String>{
		
		
		WeakFutureTask(Callable<String> c){
			super(c);
			
		}
		
		
	}
	
	class AsychOpsFuture implements Callable<String>{

		@Override
		public String call() throws Exception {
			//Tf logic
			int value_read_sum = read();

			write(value_read_sum);

			return "done";

		}

	}
	
	private int read() {
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
		return value;
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
//				System.out.println("the index: "+ index);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(num_of_write*10));
			}
			
			array[index].put((int)(Math.random()*1000)+value);
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
