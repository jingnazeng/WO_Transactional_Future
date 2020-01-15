package benchmark.synthetic.transactions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import benchmark.synthetic.TimerDebug;
import benchmark.synthetic.transactions.PatternTxnBankBenchmark.AsychOpsFuture;
import benchmark.synthetic.transactions.PatternTxnBankBenchmark.WeakFutureTask;
import contlib.Continuation;
import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;
import jvstm.graph.Node;
import jvstm.graph.Node.Status;


public class PatternTxnBankBenchmark extends ArrayAccessWriteTxnWithMultipleFutures{
	VBox<Integer>[] array;
	private int value;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private String high_contention = "false";
	private int max_num_of_core;
	private int hot_spot;
	private int bank_agency_index;
	private int bank_agency_number;
	private int array_length_each_bank_agency; //basic array length that each bank agent can access
	private float bankOperationsPercentage;
	private int num_of_rounds_for_transfer;
	
	public PatternTxnBankBenchmark(int max_num_of_core, int hot_spot_in_the_array, String high_contention, 
			int spin, int bank_agency_index, int bank_agency_number, float bank_ops_percent, 
			int num_of_rounds_for_transfer){
		this.max_num_of_core = max_num_of_core;
		this.cpu_work_amount_between_memory_read = spin;
		this.hot_spot = hot_spot_in_the_array;
		this.high_contention = high_contention;
		this.bank_agency_index = bank_agency_index;
		this.bank_agency_number = bank_agency_number;
		this.bankOperationsPercentage = bank_ops_percent;
		this.num_of_rounds_for_transfer = num_of_rounds_for_transfer;
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
//				readHotSpots(number_of_hot_spots_read_and_write);
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
	public int executeTransaction(Transaction tx, int sibling, int streamingEnabled) throws Throwable {

		array = ArrayAccess.getArray();
		array_length = array.length;
		array_length_each_bank_agency = array_length/bank_agency_number;
		
		
		List<FutureRepresentation<Integer>> submittedFutures = new ArrayList<FutureRepresentation<Integer>>(sibling);
		if (streamingEnabled == 3) {
			int nextOp = 0;
			int spawnedFutures = 0;
			//List<FutureRepresentation<Integer>> toBeEvaluatedFutures = new ArrayList<FutureRepresentation<Integer>>(sibling);
			
			while (nextOp < max_num_of_core) {
				
				FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
						submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
				
				submittedFutures.add(f);
				spawnedFutures++;
				nextOp++;
				if(spawnedFutures >= sibling) {					
					while(spawnedFutures >= sibling) {
	
						int i = 0;
						
						for(i = 0; i < submittedFutures.size(); i++) {
							FutureRepresentation<Integer> submittedFuture = submittedFutures.get(i);
							Node node = submittedFuture.getFuture_node();
							if(node.getStatus().equals(Status.completed) || node.getStatus().equals(Status.iCommitted)) {
								Transaction.current().evalWeakOrderingFuture(submittedFuture);
								
								submittedFuture.setEvaluated();
								spawnedFutures--;
								submittedFutures.remove(i);
								
								value += (Integer) submittedFuture.getF_ref().get();
								
								break;
							}
						}	
					}		
				}	
			}
			for(FutureRepresentation<Integer> submittedFuture : submittedFutures) {
				if(!submittedFuture.isEvaluated()) {
					Transaction.current().evalWeakOrderingFuture(submittedFuture);
					submittedFuture.setEvaluated();
				}
				value += (Integer) submittedFuture.getF_ref().get();
			}
		} else if (streamingEnabled == 2) {
			int nextOp = 0;
			int spawnedFutures = 0;
			//List<FutureRepresentation<Integer>> toBeEvaluatedFutures = new ArrayList<FutureRepresentation<Integer>>(sibling);
			
			while (nextOp < max_num_of_core) {
				
				FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
						submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
				
				submittedFutures.add(f);
				spawnedFutures++;
				nextOp++;
				//if(spawnedFutures >= sibling) {					
					while(spawnedFutures >= sibling) {
	
						int i = 0;
						
						for(i = 0; i < submittedFutures.size(); i++) {
							FutureRepresentation<Integer> submittedFuture = submittedFutures.get(i);
							if(!submittedFuture.isEvaluated()) {
								
								Transaction.current().evalWeakOrderingFuture(submittedFuture);
								
								submittedFuture.setEvaluated();
							}
							Node node = submittedFuture.getFuture_node();
							if(node.getStatus().equals(Status.iCommitted)) {
								spawnedFutures--;
								submittedFutures.remove(i);
								
								value += (Integer) submittedFuture.getF_ref().get();
								
								break;
							}
						}	
					}		
				//}	
			}
			for(FutureRepresentation<Integer> submittedFuture : submittedFutures) {
				if(!submittedFuture.isEvaluated()) {
					Transaction.current().evalWeakOrderingFuture(submittedFuture);
					submittedFuture.setEvaluated();
				}
				value += (Integer) submittedFuture.getF_ref().get();
			}
		} else if(streamingEnabled == 1) {
			int spawnedFutures=0;
			int nextOp = 0;
			
			while(nextOp < max_num_of_core) {
				while(spawnedFutures < sibling) {
					nextOp++;
					spawnedFutures++;
					FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
							submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
					submittedFutures.add(f);
	
				}
				Transaction.current().evalWeakOrderingFuture(submittedFutures.get(0));
				submittedFutures.get(0).setEvaluated();
				value += (Integer) submittedFutures.get(0).getF_ref().get();
				submittedFutures.remove(0);
				spawnedFutures--;
			}
			
			for(int m = 0; m < submittedFutures.size(); m++){
				Transaction.current().evalWeakOrderingFuture(submittedFutures.get(m));
				submittedFutures.get(m).setEvaluated();
				value += (Integer) submittedFutures.get(m).getF_ref().get();
			}
		}else {
			int spawnedFutures=0;
			int nextOp = 0;
			
			while(nextOp < max_num_of_core) {
				while(spawnedFutures < sibling) {
					nextOp++;
					spawnedFutures++;
					FutureRepresentation<Integer> f = ((ReadWriteTransaction)Transaction.current()).
							submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture()));
					submittedFutures.add(f);
				}
				for(int m = 0; m < submittedFutures.size(); m++){
					Transaction.current().evalWeakOrderingFuture(submittedFutures.get(m));
					submittedFutures.get(m).setEvaluated();
					value += (Integer) submittedFutures.get(m).getF_ref().get();
				}
				submittedFutures.removeAll(submittedFutures);
				spawnedFutures = 0;
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
			int value_returned = 0;
			//different types of operation
			if(Math.random() < bankOperationsPercentage){
			//	System.out.println("get total amount ");
				TimerDebug.startGetTotalTime((int)Thread.currentThread().getId()%56);
				value_returned = getTotalAmountInTheBank();
				TimerDebug.endGetTotalTime((int)Thread.currentThread().getId()%56);
			}else{
			//	System.out.println("transfer ");
				TimerDebug.startTransferTime((int)Thread.currentThread().getId()%56);
				value_returned = transferBetweenTwoAccounts();
				TimerDebug.endTransferTime((int)Thread.currentThread().getId()%56);
			}
			if(value_returned < 0){
				throw new Exception();
			}
			return value_returned;
		}

		private int transferBetweenTwoAccounts() {
			
			double sqrt_amount = 0;
			for(int round=1; round<=num_of_rounds_for_transfer; round++) {
				int amount = (int) (Math.random() * 1000);
				int index_deposit;
				int index_withdrawal;
				do {
					index_deposit = array_length_each_bank_agency * bank_agency_index;
					index_withdrawal = array_length_each_bank_agency * bank_agency_index;

					if (high_contention.equalsIgnoreCase("false")) {
						index_deposit += (int) (Math.random() * (array_length_each_bank_agency));
						index_withdrawal += (int) (Math.random() * (array_length_each_bank_agency));
					} else if (high_contention.equals("true")) {
						//				System.out.println("in high contention");
						index_deposit += (int) (Math.random() * hot_spot);
						index_withdrawal += (int) (Math.random() * hot_spot);
					} else if (high_contention.equalsIgnoreCase("middle")) {
						index_deposit += (int) (Math.random() * (hot_spot * 10));
						index_withdrawal += (int) (Math.random() * (hot_spot * 10));
					}
				} while (index_deposit == index_withdrawal);

				//insert some spin to simulate operations delay to database

				//before accessing memory, assign some CPU computation work
				for (int j = 0; j < cpu_work_amount_between_memory_read; j++) {
					sqrt_amount += Math.sqrt(j + index_deposit);
				}

				array[index_deposit].put(array[index_deposit].get() + amount);
				array[index_withdrawal].put(array[index_withdrawal].get() - amount);
			}
			return (int) sqrt_amount;

		}
		
		private int getTotalAmountInTheBank() {
			double sqrt_amount=0;
			int value_local = 0;

			//Get total amount in the bank, traverse the array and get the total amount
			for(int i = array_length_each_bank_agency*bank_agency_index;i < array_length_each_bank_agency*(bank_agency_index+1);i++){
				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				value_local += array[i].get();
			}
			return (int) (sqrt_amount+value_local);

		}
		
	}
		
}