package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;

// add a long prefix of reads to each sub-transaction

public class PatterTxn3 extends ArrayAccessWriteTxnWithMultipleFutures{
	VBox<Integer>[] array;
	private int value;
	
	private int num_of_read = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	
	public PatterTxn3(int read, int spin){
		this.num_of_read = read;
		this.cpu_work_amount_between_memory_read = spin;
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
				value += array[1].get();
				System.out.println("T2 read x: " + array[1].get());
				//Tf2 logic
				FutureRepresentation<Integer> f2  = ((ReadWriteTransaction)Transaction.current()).
						submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture2()));

				while(true){
					try{
						value += array[2].get();
						System.out.println("T3 read y: " + array[2].get());
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
			int val = (int) (Math.random()*10);
			array[1].put(val); 
			System.out.println("Tf1 write x to be :" + val);
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
			int val = (int) (Math.random()*10);
			array[2].put(val); 
			System.out.println("Tf2 write y to be :" + val);
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
				if(index < 3){
					index +=2;
				}
				
				value = array[index].get();
			}
		return value;
	}

}
