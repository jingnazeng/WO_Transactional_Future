package contlib.RBTreeBenchmark.SequentialVersion;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

import contlib.Continuation;
import contlib.RBTreeBenchmark.RBTreeJvstm;
import jvstm.ParallelNestedTransaction;
import jvstm.ParallelTask;
import jvstm.Transaction;

public class RBTreeReadOnlyTest implements Runnable {
	
	/*Test configuration variables*/
	static private RBTreeJvstm stTree = new RBTreeJvstm();
	static private int valueToSearch;
	static int nThreads;
	static int nFutures;
	static int nIterations = 10;
	static private int writeProbability;
	static private Random randPopulator;
	static public int populateN = 1000000;
	static public int populateMaxValue = 2000000;
	static protected int searchRange = 100000;
	static protected int taskSearchRange;

	static volatile protected boolean sequentially;
	

	
	static int addedValuesSeq=0;

	static long tStart;
	static long tEnd;
	static long tDelta;
	static double elapsedSeconds;
	
	private class AsyncFindMax implements Callable<Void> {

		final long startingMaxValue;
		long maxValue;
		

		public AsyncFindMax(long startingMaxValue, long maxValue) {
			this.startingMaxValue = startingMaxValue;
			this.maxValue = maxValue;
		}

		public Void call() {
			long lastValue = startingMaxValue + taskSearchRange;
			long iterator = startingMaxValue;//this shows why callable attributes must become transactional		
			int total =0;
			Random rand = new Random();
			
			for (; (iterator <= lastValue && iterator <= maxValue); iterator++) {
//				boolean toWrite = (rand.nextInt(writeProbability) == -1);
				boolean isPresent = stTree.contains((int) iterator);
				if (isPresent) {
//						if(sequentially)
//							addedValuesSeq++;
//						if(!sequentially)
//							total++;
				}
//				if(toWrite && !isPresent)
//					stTree.add((int) iterator);
//				if(toWrite && isPresent)
//					stTree.remove((int) iterator);
			}
//			System.out.println("added " +total+ " by max" );
			return null;
		}

	}
	
	private class AsyncFindMin implements Callable<Void> {
		
		final long startingMinValue; 
		long minValue;
		
		public AsyncFindMin(long startingMinValue, long minValue){
			this.startingMinValue = startingMinValue;
			this.minValue = minValue;
		}
		
		public Void call(){
			long lastValue = startingMinValue - taskSearchRange;
			long iterator = startingMinValue;
			int total = 0;
			Random rand = new Random();

			for(;iterator >= lastValue && iterator > 0 && iterator >= minValue ;iterator--) {
//				boolean toWrite = (rand.nextInt(writeProbability) == -1);
				boolean isPresent = stTree.contains((int) iterator);
				if (isPresent) {
//						if(sequentially)
//							addedValuesSeq++;
//						if(!sequentially)
//							total++;
				}
//				if(toWrite && !isPresent)
//					stTree.add((int) iterator);
//				if(toWrite && isPresent)
//					stTree.remove((int) iterator);
			}
//			System.out.println("added " +total+ " by min" );
			return null;
		}
	}
	
	static private void startTiming() {
		tStart = System.currentTimeMillis();
	}
	
	static private double finishTiming() {

		tEnd = System.currentTimeMillis();
		tDelta = tEnd - tStart;
		return (tDelta);
	}
	
	
	private void searchSequentially(long startingValue) {

		long maxValue =  startingValue + searchRange;
		long minValue =  startingValue - searchRange;
		
		long startingMinValue = startingValue;
		long startingMaxValue = startingValue;
		
		Transaction.begin();
		while(true) {
			if(startingMaxValue >= maxValue || startingMinValue <= minValue) break;
			
			if(startingMaxValue <= maxValue) {
	        	Callable<Void> c = new AsyncFindMax(startingMaxValue,maxValue);
	    		try {
					c.call();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
    		if(startingMinValue > 0 && startingMinValue >= minValue) {
    			Callable<Void> c = new AsyncFindMin(startingMinValue,minValue);
	    		try {
					c.call();
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
        	startingMaxValue = startingMaxValue + taskSearchRange;
        	startingMinValue = startingMinValue - taskSearchRange;
		}
		Transaction.commit();
		
	}
	
	private void searchWithFuturesOnly(long startingValue) {
		long maxValue =  startingValue + searchRange;
		long minValue =  startingValue - searchRange;
		
		long startingMinValue = startingValue;
		long startingMaxValue = startingValue;

		Transaction.begin();
		while(true) {
			if(startingMaxValue >= maxValue || startingMinValue <= minValue) break;
			
			if(startingMaxValue <= maxValue) {
	        	Callable<Void> c = new AsyncFindMax(startingMaxValue,maxValue);
	    		Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c){
		            @Override
		            protected boolean isReadOnly() {
		                    return true;
		            }
				});
			}
			
    		Transaction.setNextCheckpoint(Continuation.capture());
    		Transaction.begin();
    		
    		if(startingMinValue > 0 && startingMinValue >= minValue) {
	        	Callable<Void> c = new AsyncFindMin(startingMinValue,minValue);
	    		Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c){
		            @Override
		            protected boolean isReadOnly() {
		                    return true;
		            }
				});
	    		
	    		Transaction.setNextCheckpoint(Continuation.capture());
	    		Transaction.begin();
    		}

        	startingMaxValue = startingMaxValue + taskSearchRange;
        	startingMinValue = startingMinValue - taskSearchRange;
		}
		Transaction.commit();
	}

	public void run() {
		if(sequentially) {
			searchSequentially(valueToSearch);
		}
		else {
			searchWithFuturesOnly(valueToSearch);
		}
	}
	
	
	static private void populateTree() {
		startTiming();
		Transaction.begin();
		for(int i = 1; i <= populateN; i++) {
        	stTree.add(randPopulator.nextInt(populateMaxValue));
		}
		Transaction.commit();
		System.out.println( "Time: " + finishTiming());
	}
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, InterruptedException {
		RBTreeReadOnlyTest test = new RBTreeReadOnlyTest();
		nThreads = Integer.parseInt(args[0]);
		nFutures = Integer.parseInt(args[1]);
		writeProbability =	Integer.parseInt(args[2])+1;
		randPopulator = new Random(0);
		Random randSearchGenerator = new Random(0);
		taskSearchRange = searchRange / (nFutures/2);
		PrintWriter writer = new PrintWriter("RW_" + nThreads + "_" + nFutures + "_" + writeProbability + "-"+ args[3] + ".csv", "UTF-8");
		
		
		writer.println("RBTreeReadWriteTest,,,,,,");
		writer.println("Sequential,Paralela,,,");
		writer.println("Tempo(sec),Tempo,Aborted Continuations,Aborted Futures,");
		writer.println(",,,,,,");
		
		double tempoSequencial = 0;
		double tempoParalelo = 0;
		double tempoTotalSequencial = 0;
		double tempoTotalParalelo = 0;
		int totalFuturesAborted = 0;
		int totalContinuationAborted = 0;
		
		System.out.println("Populating tree");
		populateTree();	
		
		while(nIterations > 0) {
			valueToSearch = randSearchGenerator.nextInt(populateMaxValue);
			
			
			sequentially = true;
			startTiming(); 
			Continuation.runWithContinuationSupport(test);
			tempoSequencial += finishTiming();
			System.out.println("SEQ: " + tempoSequencial);
			
			sequentially = false;
			startTiming(); 
			Continuation.runWithContinuationSupport(test);
			tempoParalelo += finishTiming();
			System.out.println("PARALELO: " + tempoParalelo);
			
			System.gc();
			
			tempoTotalSequencial += tempoSequencial;
			tempoTotalParalelo += tempoParalelo;
			tempoSequencial = 0;
			tempoParalelo = 0;

			nIterations--;
		} 
//		System.out.println("------------- "+ addedValuesSeq);
		String iterationResults = "";
		iterationResults += "\""+ String.valueOf(tempoTotalSequencial)+"\""+",";
		iterationResults += "\""+String.valueOf(tempoTotalParalelo)+ "(" + String.valueOf(tempoTotalSequencial / tempoTotalParalelo) + ")"+"\"" + ",";
	

		writer.println(iterationResults);
		
		writer.close();

	}

}