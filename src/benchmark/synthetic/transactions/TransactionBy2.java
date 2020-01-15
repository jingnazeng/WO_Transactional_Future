package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;
import benchmark.synthetic.ArrayAccess;
import benchmark.synthetic.ArrayAccessTransaction;

/**
 * refer to slides Oct.1st 2018
 * create transaction resemble JTF high contention workload (Figure 5 in ICPP paper)
 * This transaction is with 2 subtxn, namly, one transactional future and one continuation
 * **/

public class TransactionBy2 implements ArrayAccessTransaction{
	private int num_of_read = 1;
	private int num_of_write = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private String high_contention = "false";
	private boolean seqExe;
	VBox<Integer>[] array;
	private int value;
	
	public TransactionBy2(int read, int write,String high_contention, int amount, boolean seqExe){
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
		int value_read_sum = read(tx,sibling);
//		int value_read_sum=0;
		writeSequentially(value_read_sum);

		return value;
	}
	
	private int read(Transaction tx, int sibling) throws Throwable {
		if(seqExe){
			return readSequentially();
		}else{
			//T1 logic
			FutureRepresentation<?> tf = ((ReadWriteTransaction)tx).submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture(),true));
			
			while(true){
				try{
					//T2 logic
					//	Thread.sleep(1000);
					//	System.out.println("start T2 logic");
					value += readSub();
					
					Integer result =  (Integer) Transaction.current().evalWeakOrderingFuture(tf);
					value += result;
					break;
				}catch(Throwable e){
					if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException){
//						e.printStackTrace();
						System.out.println("early abort exception");
						throw e;
					}
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

	class WeakFutureTask extends WeakOrderingTransactionalFutureTask<Integer>{


		WeakFutureTask(Callable<Integer> c){
			super(c);
		}

		WeakFutureTask(Callable<Integer> c,boolean readOnly){
			super(c,readOnly);
		}
	}
	
	class AsychOpsFuture implements Callable<Integer>{

		@Override
		public Integer call() throws Exception {
			//Tf logic
			int value_read_sum = readSub();
			return value_read_sum;

		}
	}
	
	
	private int read(int num_of_read){

		for(int i = 0;i < num_of_read;i ++){
			//read N items at random in the array, on whose basis (sum/avg/std_dev of the values read) we change M items at random in the array

			int index = (int)(Math.random()*(array_length));

			//before accessing memory, assign some CPU computation work
			if(cpu_work_amount_between_memory_read!=0){
				double sqrt_amount = 0;
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				index = (int)sqrt_amount % array_length;

				//System.out.println("index: "+index);
			}
			//				array.get(index).get();
			value += array[index].get();
		}
		return value;
	
	
	}
	
	private int readSub() {
		return read(num_of_read/2);
	}

	private int readSequentially() {
		return read(num_of_read);
	}

	private void writeSequentially(int value) {
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