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
import jvstm.graph.Node;
import jvstm.graph.Node.Status;


public class PatternTxn6ByAnyFutures extends ArrayAccessWriteTxnWithMultipleFutures{
	VBox<Integer>[] array;
	private int value;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private String high_contention = "false";
	private int max_num_of_core;
	private int hot_spot;
	private int number_of_hot_spots_read_and_write;
	private int num_of_prefix_sequential_read;
	
	public PatternTxn6ByAnyFutures(int max_num_of_core, int prefix_disjoint_read, int hot_spot_in_the_array,int num_of_hot_spots_read_and_write, String high_contention, int spin){
		this.max_num_of_core = max_num_of_core;
		this.num_of_prefix_sequential_read = prefix_disjoint_read;
		this.hot_spot = hot_spot_in_the_array;
		this.cpu_work_amount_between_memory_read = spin;
		this.high_contention = high_contention;
		this.number_of_hot_spots_read_and_write = num_of_hot_spots_read_and_write;
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
				readHotSpots(number_of_hot_spots_read_and_write);
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
		
		//int num_of_futures = 2;
		
		/*int nextOp = 0;
		int spawnedFutures = 0;
		List<FutureRepresentation<Integer>> submittedFutures = new ArrayList<FutureRepresentation<Integer>>(sibling);
		while (nextOp < max_num_of_core) {
			FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
					submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
			submittedFutures.add(f);
			spawnedFutures++;
			nextOp++;
			while(spawnedFutures > sibling) {
				for(int i = 0; i < submittedFutures.size(); i++) {
					FutureRepresentation<Integer> submittedFuture = submittedFutures.get(i);
					Node node = submittedFuture.getFuture_node();
					if(node.getStatus().equals(Status.iCommitted)) {
						value += (Integer) Transaction.current().evalWeakOrderingFuture(submittedFuture);
						spawnedFutures--;
						submittedFutures.remove(i);
						break;
					}
				}
			}
		}
		if(nextOp < max_num_of_core) {
			
		}*/
		
		for(int i = 1; i <= max_num_of_core/sibling ; i++){
			FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
					submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
			List<FutureRepresentation<Integer>> submittedFutures = new ArrayList<FutureRepresentation<Integer>>();
			submittedFutures.add(f);
			while(true){
				try{
					readHotSpots(number_of_hot_spots_read_and_write);
					executeFutCont(sibling-1, submittedFutures);
					//System.out.println(String.format("number of submiited futures is %d from %d siblings"
					//		,submittedFutures.size(), sibling));
					for (FutureRepresentation<Integer> submittedFuture : submittedFutures)
						value += (Integer) Transaction.current().evalWeakOrderingFuture(submittedFuture);
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
			//Tf1 logic
			readSequentially();
			writeHotSpots(number_of_hot_spots_read_and_write);
			
		
			return value;

		}
	}


	
	private int readSequentially() {
		return read(0,num_of_prefix_sequential_read);
	}
	
	private int read(int min, int max) {
		for(int i = min;i < max;i ++){
				double sqrt_amount = 0;
				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				int index = ((int)(sqrt_amount+Math.random()*array_length)) 
						% (array_length-hot_spot);
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
				index = (int)(Math.random()*hot_spot);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(hot_spot*10));
			}
			
			index = array_length - 1 - index; // write to a disjoint hotspt set w.r.t. long prefix read 
			try{
				array[index].put((int)(Math.random()*1000));
			}catch(Exception e){
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
				index = (int)(Math.random()*hot_spot);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(hot_spot*10));
			}
			index = array_length - 1 - index; // read to a disjoint hotspt set w.r.t. long prefix read
			try{
				value += array[index].get();
			}catch(Exception e){
				throw e;
			}
		}
	}
}