package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import jvstm.EarlyAbortException;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureReadOnlyTask;
import benchmark.synthetic.ArrayAccess;
import benchmark.synthetic.ArrayAccessTransaction;

/**
 * create transaction resemble JTF readonly workload (Figure 5a in ICPP paper)
 * This transaction is with 16 subtxn, namly,15 transactional future and one continuation
 * **/

public class ReadOnlyTxnBy16 implements ArrayAccessTransaction{
	private int num_of_read = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private boolean seqExe;
	VBox<Integer>[] array;
	private int value;
	
	public ReadOnlyTxnBy16(int read,  int amount, boolean seqExe){
		this.num_of_read = read;
		this.cpu_work_amount_between_memory_read = amount;
		this.seqExe = seqExe;
	}

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {
		array = ArrayAccess.getArray();
		array_length = array.length;

		if(seqExe || sibling == 0 || sibling == 1){

			return readSequentially();
		
		}else{
			int num_of_sibling = 16;
			int partition = num_of_read/num_of_sibling;
			int last_partition = num_of_read%num_of_sibling;
			Future<?>[] future_ref = new Future<?>[num_of_sibling-1];
			//T1 logic
			//Tf1 logic
			future_ref[0] = Transaction.current().
					submitReadOnlyWeakOrderingFuture(new WeakFutureReadOnlyTask(new AsychOpsFuture(0,partition)));
			while(true){
				try{
					//	System.out.println("start T2 logic");
					//Tf2 logic
					for(int i = 2 ; i < num_of_sibling; i++){
						future_ref[i-1] = (Transaction.current()).
								submitReadOnlyWeakOrderingFuture
								(new WeakFutureReadOnlyTask
										(new AsychOpsFuture((i-1)*partition, i*partition)));
					}
					
					
					int value_read = readSub(num_of_read-partition-last_partition,num_of_read);
					value = value_read;
					
					for(int i =0; i < future_ref.length;i++){
						while(!future_ref[i].isDone()){
							
						}
						value = (Integer) future_ref[i].get();
					}
					
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
			return value;
		}
		

	}
	
	class WeakFutureReadOnlyTask extends WeakOrderingTransactionalFutureReadOnlyTask<Integer>{
		
		
		WeakFutureReadOnlyTask(Callable<Integer> c){
			super(c);
			
		}
		
		
	}
	
	class AsychOpsFuture implements Callable<Integer>{
		private int min;
		private int max;
		public AsychOpsFuture(int min, int max){
			this.min = min;
			this.max = max;
		}
		@Override
		public Integer call() throws Exception {
			//Tf logic
			int value_read_sum = readSub(min,max);

			return value_read_sum;

		}
	}
	
	
	
	private int readSub(int min, int max){
		return read(min, max);
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


	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public int executeTransaction(int sibling) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

}