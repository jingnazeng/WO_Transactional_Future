package contlib.RBTreeBenchmark;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

import contlib.Continuation;
import contlib.RBTreeBenchmark.SequentialVersion.RBTreeReadOnlyTest;
import jvstm.ParallelTask;
import jvstm.Transaction;

public class RBTreeReadWriteTest implements Runnable {

	static RBTreeJvstm stSpeculation = new RBTreeJvstm();
	static ArrayList<Integer> valuesToSearch = new ArrayList<Integer>();
	
	static public long populateN = 1200000;
	static public long populateMaxValue = 2000000;
	static Random rand = new Random();

//	e.g ((rand.nextInt(25)) == 0) 0,04
	
	static public long searchRange = 100000;
	static public long taskSearchRange = 50000; 
	
	static public boolean futureWriteProbability = true;
	static public boolean futureReadProbability = true;
	static public int futureNWrites =(int) taskSearchRange;
	static public int futureNReads =(int) taskSearchRange;
	
	static public boolean continuationWriteProbability = false;
	static public boolean continuationReadProbability = false;
	static public int continuationNWrites = 0;
	static public int continuationNReads = 0;
	
	static long tStart;
	static long tEnd;
	static long tDelta;
	static double elapsedSeconds;
	
	static private class AsyncFindMax implements Callable<Void> {

		long startingMaxValue;
		long maxValue;
		int futureNWrites;
		int futureNReads;

		public AsyncFindMax(long startingMaxValue, long maxValue) {
			this.futureNWrites = RBTreeReadWriteTest.futureNWrites;
			this.futureNReads = RBTreeReadWriteTest.futureNReads;
			this.startingMaxValue = startingMaxValue;
			this.maxValue = maxValue;
		}

		public Void call() {
			long lastValue = startingMaxValue + taskSearchRange;
			
			for (; (startingMaxValue <= lastValue && startingMaxValue <= maxValue) && (this.futureNReads !=0); startingMaxValue++,this.futureNReads--) {
				if (!stSpeculation.contains((int) startingMaxValue) && this.futureNWrites !=0) {
					stSpeculation.add((int) startingMaxValue);
					this.futureNWrites--;
				}

			}
			return null;
		}

	}
	
	static private void searchContinuation(long startingMinValue, long minValue) {
		long lastValue = startingMinValue - taskSearchRange;
		int nReads = continuationNReads;
		int nWrite = continuationNWrites;

		for(;(startingMinValue >= lastValue && startingMinValue > 0 && startingMinValue >= minValue) && (nReads !=0) ;startingMinValue--,nReads--) {
			if((!stSpeculation.contains((int) startingMinValue)) && nWrite !=0) {
				stSpeculation.add((int) startingMinValue);
				nWrite--;
        	}
		}
	}
	
	static private void populateTree() {

		startTiming();
		for(int i = 1; i <= populateN; i++) {
        	Double random =  Math.random()* populateMaxValue;
        	stSpeculation.add(random.intValue());
		}
		finishTiming();
	}
	
	static private void startTiming() {
		tStart = System.currentTimeMillis();
	}
	
	static private void finishTiming() {

		tEnd = System.currentTimeMillis();
		tDelta = tEnd - tStart;
		elapsedSeconds = tDelta / 1000.0;
		System.out.println("Elapsed time "+ (elapsedSeconds) +"seconds");
	}
	
	
	static private void search(long startingValue) {
		long maxValue =  startingValue + searchRange;
		long minValue =  startingValue - searchRange;
		
		long startingMinValue = startingValue;
		long startingMaxValue = startingValue;

		Transaction.begin();
		while(true) {
			if(startingMaxValue > maxValue || startingMinValue < minValue) break;
			
			//there is always a last iteration for the maxvalues
			
			if(startingMaxValue <= maxValue) {
	        	Callable<Void> c = new AsyncFindMax(startingMaxValue,maxValue);
	    		Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c));
			}
			
    		Transaction.setNextCheckpoint(Continuation.capture());
    		Transaction.begin();
    		
    		if(startingMinValue > 0 && startingMinValue >= minValue) {
    			searchContinuation(startingMinValue, minValue);
    		}
			
        	startingMaxValue = startingMaxValue + taskSearchRange;
        	startingMinValue = startingMinValue - taskSearchRange;
		}
		Transaction.commit();
	}
	
	public void run() {


		startTiming();
		for(int i =0; i < valuesToSearch.size(); i++) {
			System.out.println("Searching: " + valuesToSearch.get(i));
			search(valuesToSearch.get(i));
		}
		finishTiming();
		
	}
	

	public static void main(String[] args) {
		RBTreeReadWriteTest test = new RBTreeReadWriteTest();
		System.out.println("Populating tree.");
		populateTree();
		
		Double toSearch;
		for(int i = 0; i < 1; i++) {
			toSearch = Math.random()* populateMaxValue;
			valuesToSearch.add(toSearch.intValue());
		}

		 Continuation.runWithContinuationSupport(test);
		

	}

}
