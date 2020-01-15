package benchmark.synthetic;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import benchmark.synthetic.TimerDebug;
import benchmark.synthetic.transactions.ArrayAccessWriteTxnWithMultipleFutures;
import benchmark.synthetic.transactions.BaseTransaction;
import benchmark.synthetic.transactions.BaseTransactionV2;
import benchmark.synthetic.transactions.BaseTransactionWithMultipleFuture;
import benchmark.synthetic.transactions.PatternTxn6ByAnyFutures;
import benchmark.synthetic.transactions.PatternTxn7;
import benchmark.synthetic.transactions.PatternTxnBankBenchmark;
import benchmark.synthetic.transactions.ReadOnlyTxnBy16;
import benchmark.synthetic.transactions.TransactionBy2;
import benchmark.synthetic.transactions.TransactionByAnySibling;
import benchmark.synthetic.transactions.TransactionaVariantOne;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.graph.DAG.DAGStatus;

public class ArrayAccessStressor{


	/**
	 * @return the bankOperationPecentage
	 */
	public float getBankOperationPecentage() {
		return bankOperationPecentage;
	}

	/**
	 * @param bankOperationPecentage the bankOperationPecentage to set
	 */
	public void setBankOperationPecentage(float bankOperationPecentage) {
		this.bankOperationPecentage = bankOperationPecentage;
	}

	/**
	 * @return the bankAgencyNum
	 */
	public int getBankAgencyNum() {
		return bankAgencyNum;
	}

	/**
	 * @param bankAgencyNum the bankAgencyNum to set
	 */
	public void setBankAgencyNum(int bankAgencyNum) {
		this.bankAgencyNum = bankAgencyNum;
	}

	/**
	 * @return the stressors
	 */
	public List<Stressor> getStressors() {
		return stressors;
	}

	/**
	 * @param stressors the stressors to set
	 */
	public void setStressors(List<Stressor> stressors) {
		this.stressors = stressors;
	}

	private long startTime = 0;
	private long endTime = 0;
	private boolean running = true;
	private boolean active = true;
	private long simulationDuration = 30L;
	//FIXME:remove volatile should also work, because only one thread is accessing threadsStartingPoint
	private volatile CountDownLatch threadsStartingPoint;
	private int threadsNum = 1;
	private int siblingNum = 0;
	private int bankAgencyNum = 1;
	private int max_num_of_core;
	private int num_of_read_in_prefix_disjoint_read;
	private String write_in_high_contenction = "false";
	private int cpu_work_amount_between_memory_read = 0;
	private int num_of_operations; // for PatternTxn7 Only
	private int num_of_hot_spots_in_the_whole_array;
	private int num_of_read_and_write_to_hot_spots;
	private boolean sequentialFlag;
	private float bankOperationPecentage;
	
	public int getNum_of_rounds_for_transfer() {
		return num_of_rounds_for_transfer;
	}

	public void setNum_of_rounds_for_transfer(int num_of_rounds_for_transfer) {
		this.num_of_rounds_for_transfer = num_of_rounds_for_transfer;
	}

	private int num_of_rounds_for_transfer;

	private Timer finishTimer = new Timer("Finish-Timer");

	private List<Stressor> stressors = new LinkedList<Stressor>();
	private int streaming;

	public void setThreadsNum(int num){
		this.threadsNum = num;
	}

	public void setSimuDuration(int num){
		this.simulationDuration = num;
	}

	public void setSiblingNum(int num){
		this.siblingNum = num;
	}

	public void setMaxNumofCores(int num){
		this.max_num_of_core = num;
	}

	public void setNumReadInPrefixDisjointRead(int num){
		this.num_of_read_in_prefix_disjoint_read = num;
	}

	public void setNumofHotspots(int num){
		this.num_of_hot_spots_in_the_whole_array = num;
	}

	public void setNumofReadAndWriteToHotSpots(int num){
		this.num_of_read_and_write_to_hot_spots = num;
	}

	public void setWriteInHighContention(String contention){
		//		if(contention.equalsIgnoreCase("true"))
		this.write_in_high_contenction = contention;
	}

	public void setCpuWorkBetweenMemoryRead(int amount){
		this.cpu_work_amount_between_memory_read = amount;
	}
	
	/**
	 * @param num_of_operations the num_of_operations to set
	 */
	public void setNum_of_operations(int num_of_operations) {
		this.num_of_operations = num_of_operations;
	}
	
	public void setSequentialFlag(boolean sequentialExecution){
		this.sequentialFlag = sequentialExecution;
	}
	
	public boolean getSequentialFlag(){
		return this.sequentialFlag;
	}

	public Map<String, String> stress() throws InterruptedException{
		finishTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				finishThreadsActivities();
			}


		}, simulationDuration*1000);

		executeTransactionsInAllThreads();

		finishTimer.cancel();

		return processResults(stressors);
	}

	private Map<String, String> processResults(List<Stressor> stressors) {
		int failures = 0;
		int readOnlyTxn = 0;
		int writeTxn = 0;
		int readOnlyTxnFailure = 0;
		int writeTxnFailure = 0;
		float txn_running_duration_all_thead = 0;
		int txn_num_all_thread = 0;
		int val = 0;
		Map<String,String> results = new LinkedHashMap<String,String>();
		for(Stressor s:stressors){
			failures += s.failure;
			readOnlyTxn += s.readOnlyTxn;
			writeTxn += s.writeTxn;
			readOnlyTxnFailure += s.readOnlyTxnFailure;
			writeTxnFailure += s.writeTxnFailure;
			txn_running_duration_all_thead += s.total_txn_exe_duration_single_thread;
			txn_num_all_thread += s.txn_num;
			val += s.val;
		}
		long simuDuration = endTime-startTime;
		float throughput = (float)(writeTxn+readOnlyTxn)/simuDuration*1000;
		float abort_rate = (float)failures/(writeTxn+readOnlyTxn+failures);

		float average_running_time_all_thread  = (float)txn_running_duration_all_thead/txn_num_all_thread/1000000;
		results.put("Duration", str(simuDuration));
		results.put("read_only_txn", str(readOnlyTxn));
		results.put("write_txn", str(writeTxn));
		results.put("failures", str(failures));
		results.put("read_only_failures", str(readOnlyTxnFailure));
		results.put("write_txn_failure", str(writeTxnFailure));
		results.put("throughput", str(throughput));
		results.put("abort_rate", str(abort_rate));
		results.put("avarage_txn_running_time_in_millis", str(average_running_time_all_thread));
		results.put("transfer time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getTransferTime())/1000));
		results.put("get total time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getTotalTime())/1000));
		results.put("reading time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getReadingTime())/1000));
		results.put("writing time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getWritingTime())/1000));
		
		results.put("val", str(val));
		results.put("internal abort rate: ", str(TimerDebug.getFutureAbortRate()));
		results.put("commit duration in microSecond  ", str(TimerDebug.getAverage(TimerDebug.getCommitTime())/1000));
		results.put("forward validation time in microSecond", str(TimerDebug.getAverage(TimerDebug.getforwardValidationTime())/1000));
		results.put("backward validation time in microSecond", str(TimerDebug.getAverage(TimerDebug.getBackwardValidationTime())/1000));
		return results;
	}

	private String str(Object o) {
		return String.valueOf(o);
	}

	//FIXME:may not need to be synchronized because only one threads will be accessing
	private synchronized void finishThreadsActivities() {
		if (!running) {
			return;
		}
		running = false;
		for (Stressor stressor : stressors) {
			stressor.finish();
		}
		notifyAll();
	}		

	public void executeTransactionsInAllThreads() throws InterruptedException{
		threadsStartingPoint = new CountDownLatch(1);
		for(int threadsIndex = 0 ; threadsIndex < threadsNum ; threadsIndex++){
			Stressor stressor = createSressor(threadsIndex);
			stressors.add(stressor);
			stressor.start();
		}
		threadsStartingPoint.countDown();
		startTime = System.currentTimeMillis();

		blockWhileRunning();
		
		for(Stressor stressor:stressors){
			stressor.join();
		}
		endTime = System.currentTimeMillis();

	}

	private synchronized void blockWhileRunning() throws InterruptedException {
		//FIXME:improve so that this thread is not busy checking
		while(this.running){
			wait();
		}
	}

	private Stressor createSressor(int threadsIndex) {
		return new Stressor(threadsIndex);

	}

	public class Stressor extends Thread{
		private int readOnlyTxn = 0;
		private int writeTxn = 0;
		private int failure = 0;
		private int readOnlyTxnFailure = 0;
		private int writeTxnFailure = 0;
		private float average_running_time = 0;
		private long total_txn_exe_duration_single_thread = 0;
		private int txn_num = 0;
		private int val = 0;


		public Stressor(int threadsIndex) {
			super("Stressor Num:"+threadsIndex);
		}

		public final synchronized void finish() {
			active = true;
			running = false;
			notifyAll();
		}

		private synchronized boolean assertRunning() {
			return running;
		}

		public void run(){
			try {
				threadsStartingPoint.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean successful = true;
			long txn_start_timing = System.nanoTime();

			while(assertRunning()){
				
				boolean readOnly;

				ArrayAccessTransaction  arrayAccessTransaction = createTransaction();
				readOnly = arrayAccessTransaction.isReadOnly();
				if(successful == true){
					txn_start_timing = System.nanoTime();
					txn_num += 1;
				}
				successful = true;
		//		Transaction.current.set(null);
				
				Transaction tx = Transaction.begin(readOnly);
				
				try {
					//					long start = System.nanoTime();
					val = arrayAccessTransaction.executeTransaction(tx,siblingNum, streaming);
					//					System.out.println("executeion time: "+ (System.nanoTime()-start));
					
				}
				catch(Throwable e){
//					System.out.println("to abort: "+tx.getTopLevelTrasanction());
//					e.printStackTrace();
					if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException){
						tx.getTopLevelTrasanction().getDAG().setDagStatus(DAGStatus.ABORTED);
						tx.abortTx();
//						tx.getTopLevelTrasanction().abortTx();
//						System.out.println("abort due to early abort exception");
					}else{
						e.printStackTrace();
						tx.abortTx();
					}
					successful = false;
					failure++;
					if(arrayAccessTransaction instanceof ReadOnlyArrayAccess
							|| arrayAccessTransaction instanceof ReadOnlyTxnBy16){
						readOnlyTxnFailure++;
					}
					else if (arrayAccessTransaction instanceof ReadWriteArrayAccess)
						writeTxnFailure++;
					else if (arrayAccessTransaction instanceof BaseTransactionV2 || 
							arrayAccessTransaction instanceof BaseTransaction ||
							arrayAccessTransaction instanceof TransactionaVariantOne ||
							arrayAccessTransaction instanceof TransactionBy2 ||
							arrayAccessTransaction instanceof TransactionByAnySibling ||
							arrayAccessTransaction instanceof BaseTransactionWithMultipleFuture
							|| arrayAccessTransaction instanceof ArrayAccessWriteTxnWithMultipleFutures)
						writeTxnFailure++;
				} 

				if(successful){
					if(tx instanceof ReadWriteTransaction && tx.getTopLevelTrasanction().getDAG().getDagStatus()==DAGStatus.ABORTED){
				//		System.out.println("top level cascade abort");
						tx.abortTx();
					}else{
						try{
						//	System.out.println("to commit: "+tx.getTopLevelTrasanction());
							tx.commitTx(true);
							if(arrayAccessTransaction instanceof ReadOnlyArrayAccess
									|| arrayAccessTransaction instanceof ReadOnlyTxnBy16){
								readOnlyTxn++;
							}
							else if (arrayAccessTransaction instanceof ReadWriteArrayAccess){
								writeTxn++;
							} else if (arrayAccessTransaction instanceof BaseTransactionV2 || 
									arrayAccessTransaction instanceof BaseTransaction ||
									arrayAccessTransaction instanceof TransactionaVariantOne ||
									arrayAccessTransaction instanceof TransactionBy2 ||
									arrayAccessTransaction instanceof TransactionByAnySibling ||
									arrayAccessTransaction instanceof BaseTransactionWithMultipleFuture ||
									arrayAccessTransaction instanceof ArrayAccessWriteTxnWithMultipleFutures){
							//	System.out.println("sucessfully commit a transaction");
								writeTxn++;
							}
						}
						catch(RuntimeException exception ){
							exception.printStackTrace();
							System.out.println("fail");
							successful = false;
							tx.abort();
							failure++;
							if(arrayAccessTransaction instanceof ReadOnlyArrayAccess
									|| arrayAccessTransaction instanceof ReadOnlyTxnBy16){ 
								readOnlyTxnFailure++;
							}
							else if (arrayAccessTransaction instanceof ReadWriteArrayAccess){
								writeTxnFailure++;
							}
							else if (arrayAccessTransaction instanceof BaseTransactionV2 || 
									arrayAccessTransaction instanceof BaseTransaction ||
									arrayAccessTransaction instanceof TransactionaVariantOne ||
									arrayAccessTransaction instanceof TransactionBy2 ||
									arrayAccessTransaction instanceof TransactionByAnySibling ||
									arrayAccessTransaction instanceof BaseTransactionWithMultipleFuture ||
									arrayAccessTransaction instanceof ArrayAccessWriteTxnWithMultipleFutures)
								writeTxnFailure++;
						}}
				}
				if(successful){
					long single_txn_duration = System.nanoTime()-txn_start_timing;
					total_txn_exe_duration_single_thread += single_txn_duration;
				}
			}

			if(!successful){
				txn_num -= 1;
			}


		}
		
		private ArrayAccessTransaction createTransaction() {
//			return new PatternTxn6ByAnyFutures(max_num_of_core, 
//					num_of_read_in_prefix_disjoint_read, 
//					num_of_hot_spots_in_the_whole_array, 
//					num_of_read_and_write_to_hot_spots, 
//					write_in_high_contenction, 
//					cpu_work_amount_between_memory_read);
//			return new PatternTxn7(cpu_work_amount_between_memory_read,num_of_operations);
			
			// to decide which bank agency to access, i.e. which part of the array to manipulate
			int bank_agency_index = (int) (Math.random()*1000%bankAgencyNum);
			return new PatternTxnBankBenchmark(max_num_of_core, 
					num_of_hot_spots_in_the_whole_array, 
					write_in_high_contenction, 
					cpu_work_amount_between_memory_read,bank_agency_index,bankAgencyNum,bankOperationPecentage,num_of_rounds_for_transfer);	
			
		}
	}

	public void setStreaming(int streaming) {
		this.streaming = streaming;
		
	}
}