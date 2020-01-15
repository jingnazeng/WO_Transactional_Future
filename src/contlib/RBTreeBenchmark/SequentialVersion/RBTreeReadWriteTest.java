package contlib.RBTreeBenchmark.SequentialVersion;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import contlib.Continuation;
import contlib.RBTreeBenchmark.RBTreeJvstm;
import jvstm.CommitException;
import jvstm.EarlyAbortException;
import jvstm.ParallelNestedTransaction;
import jvstm.ParallelTask;
import jvstm.Transaction;

public class RBTreeReadWriteTest implements Runnable {
	
	/*Test configuration variables*/
	static private RBTreeJvstm stTree = new RBTreeJvstm();
	static int nThreads;
	static int nFutures;
	static int nIterations = 5;
	static private int writeProbability;
	static public int populateN = 1000000;
	static public int populateMaxValue = 2000000;
	static protected int searchRange = 50000;
	static protected int taskSearchRange;
	static protected ArrayList<Boolean> toWrites;
	
	static private Random randPopulator	= new Random(0);
	static Random randSearchGenerator =  new Random(0);
	static protected ExecutorService executor;
	static volatile protected boolean sequentially;
	
	
	/*For correctness*/
	static int addedValuesSeq=0;

	/*For measurements*/
	static long tStart;
	static long tEnd;
	static long tDelta;
	static double elapsedSeconds;
	
	private int valueToSearch;
	public int earlyAbort =0;
	public int commitaborts =0;
	public int futuresaborts =0;
	public int interFuturesAborts = 0;
	
	public void resetAborts() {
		futuresaborts =0;
		interFuturesAborts = 0;
		earlyAbort =0;
		commitaborts =0;
	}
	
	private class AsyncCall implements Runnable{

		@Override
		public void run() {
			searchWithFuturesOnly(valueToSearch);
		}
	}
	
	static public class AsyncFindMax implements Callable<Void> {

		final long startingMaxValue;
		long maxValue;
		boolean toWrite = false;
		

		public AsyncFindMax(long startingMaxValue, long maxValue, boolean toWrite) {
			this.startingMaxValue = startingMaxValue;
			this.maxValue = maxValue;
			this.toWrite = toWrite;
		}

		public Void call() {
			long lastValue = startingMaxValue + taskSearchRange;
			long iterator = startingMaxValue;
			int total =0;

			for (; (iterator <= lastValue && iterator <= maxValue); iterator++) {
				boolean isPresent = stTree.contains((int) iterator);
				if (isPresent) {
					//						if(sequentially)
					//							addedValuesSeq++;
					//					if(!sequentially)
					//							total++;
				}
				if(toWrite && !isPresent)
					stTree.add((int) iterator);
				if(toWrite && isPresent)
					stTree.remove((int) iterator);
			}

			//			System.out.println("added " +total+ " by max");
			return null;
		}

	}
	
	static public class AsyncFindMin implements Callable<Void> {

		final long startingMinValue; 
		long minValue;
		boolean toWrite = false;

		public AsyncFindMin(long startingMinValue, long minValue, boolean toWrite){
			this.startingMinValue = startingMinValue;
			this.minValue = minValue;
			this.toWrite = toWrite;
		}

		public Void call(){
			long lastValue = startingMinValue - taskSearchRange;
			long iterator = startingMinValue;
			int total = 0;

			for(;iterator >= lastValue && iterator > 0 && iterator >= minValue ;iterator--) {
				boolean isPresent = stTree.contains((int) iterator);
				if (isPresent) {
					//						if(sequentially)
					//							addedValuesSeq++;
					//						if(!sequentially)
					//							total++;
				}
				if(toWrite && !isPresent)
					stTree.add((int) iterator);
				if(toWrite && isPresent)
					stTree.remove((int) iterator);
			}
			//			System.out.println("added " +total+ " by min");
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
		boolean committed=false;
		while(!committed) {
			try {

				long maxValue =  startingValue + searchRange;
				long minValue =  startingValue - searchRange;

				long startingMinValue = startingValue;
				long startingMaxValue = startingValue;
				int nextToWrite = 0;
				
				Transaction.begin();
				while(true) {
					if(startingMaxValue >= maxValue || startingMinValue <= minValue || nextToWrite == 12) break;
					
					if(startingMaxValue <= maxValue) {
						Callable<Void> c = new AsyncFindMax(startingMaxValue,maxValue,toWrites.get(nextToWrite));
						c.call();
						nextToWrite +=1;
					}

					if(startingMinValue > 0 && startingMinValue >= minValue) {
						Callable<Void> c = new AsyncFindMin(startingMinValue, minValue,toWrites.get(nextToWrite));
						c.call();
						nextToWrite +=1;
					}

					startingMaxValue = startingMaxValue + taskSearchRange;
					startingMinValue = startingMinValue - taskSearchRange;
				}

				Transaction.commit();
				committed=true;
				System.out.println("EarlyAborts: " + earlyAbort + " CommitAborts: " + commitaborts);
				earlyAbort =0;
				commitaborts =0;
			}
			catch(EarlyAbortException e) {
				Transaction.abort();
				earlyAbort++;
			}
			catch(CommitException e) {
				Transaction.abort();
				commitaborts++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void searchWithFuturesOnly(long startingValue) {
		boolean committed=false;
		Transaction tx = null;
		while(!committed) {
			try {
				long maxValue =  startingValue + searchRange;
				long minValue =  startingValue - searchRange;

				long startingMinValue = startingValue;
				long startingMaxValue = startingValue;
				int nextToWrite = 0;
				
				tx = Transaction.begin();
				while(true) {
					if(startingMaxValue >= maxValue || startingMinValue <= minValue) break;

					if(startingMaxValue <= maxValue) {
						Callable<Void> c = new AsyncFindMax(startingMaxValue,maxValue, toWrites.get(nextToWrite));
						if(!toWrites.get(nextToWrite))
							Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c){
					            @Override
					            protected boolean isReadOnly() {
					                    return true;
					            }
							});
						else
							Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c));
						
						nextToWrite +=1;
						Transaction.setNextCheckpoint(Continuation.capture());
						Transaction.begin();
					}


					if(startingMinValue > 0 && startingMinValue >= minValue) {
						Callable<Void> c = new AsyncFindMin(startingMinValue,minValue, toWrites.get(nextToWrite));
						if(!toWrites.get(nextToWrite))
							Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c){
					            @Override
					            protected boolean isReadOnly() {
					                    return true;
					            }
							});
						else
							Transaction.current().manageNestedParallelTxs(new ParallelTask<Void>(c));
						
						nextToWrite +=1;
						Transaction.setNextCheckpoint(Continuation.capture());
						Transaction.begin();
					}

					startingMaxValue = startingMaxValue + taskSearchRange;
					startingMinValue = startingMinValue - taskSearchRange;
				}
				Transaction.commit();
				committed=true;
				System.out.println("EarlyAborts: " + earlyAbort + " CommitAborts: " + commitaborts);
			}
			catch(EarlyAbortException e) {
				Transaction.abort();
				earlyAbort++;
			}
			catch(CommitException e) {
				Transaction.abort();
				commitaborts++;
			}
			catch(Exception n) {
				System.out.println("UNHANDLED EXCEPTION "+ Thread.currentThread());
			
				n.printStackTrace();
				throw new RuntimeException(n);
			}
		}
		futuresaborts = tx.getFuturesAborts();
		interFuturesAborts = tx.getInterFuturesAborts();
	}

	public void run() {
		if(sequentially) {
			searchSequentially(valueToSearch);
		}
		else {
			AsyncCall asyncCall = new AsyncCall();
			Continuation.runWithContinuationSupport(asyncCall);
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
		nThreads = Integer.parseInt(args[0]);
		nFutures = Integer.parseInt(args[1]);
		writeProbability =	Integer.parseInt(args[2]);
		taskSearchRange = searchRange / (nFutures/2);
		PrintWriter writer = new PrintWriter("RW_" + nThreads + "_" + nFutures + "_" + writeProbability + "-"+ args[3] + ".csv", "UTF-8");
		
		writer.println("RBTreeReadWriteTest,,,,,,");
		writer.println("Sequential,Paralela,,,");
		writer.println("TempoAverage(sec),TempoAverage,");
		writer.println(",,,,,,");
		
		double tempoSequencial = 0;
		double tempoParalelo = 0;
		double tempoTotalSequencial = 0;
		double tempoTotalParalelo = 0;
		int totalFuturesAborted = 0;
		int totalContinuationAborted = 0;
		
		System.out.println("Populating tree");
		populateTree();	
		
		ArrayList<Integer> values2Search = generateRandomValues(nThreads);
		ArrayList<Callable<Object>> transactions = new ArrayList<Callable<Object>>();
		ArrayList<RBTreeReadWriteTest> trees = new ArrayList<RBTreeReadWriteTest>();
		ArrayList<Integer> futuresAborted = new ArrayList<Integer>(Collections.nCopies(nThreads, 0));
		ArrayList<Integer> interFuturesAborted = new ArrayList<Integer>(Collections.nCopies(nThreads, 0));
		ArrayList<Integer> earlyAborts = new ArrayList<Integer>(Collections.nCopies(nThreads, 0));
		ArrayList<Integer> commitAborts = new ArrayList<Integer>(Collections.nCopies(nThreads, 0));
	
		
		for(int i = 0; i< nThreads ; i++) {
			RBTreeReadWriteTest test = new RBTreeReadWriteTest();
			test.valueToSearch = values2Search.get(i);	
			transactions.add(Executors.callable(test));
			trees.add(test);
		}
		
		boolean startCounting = false;
		while(nIterations > 0) {
			
			//calculate probabilities
			Random rand = new Random();
			toWrites = new ArrayList<Boolean>(Collections.nCopies(nFutures, false));
			
			for(int i =0; i < nFutures; i++) {
				toWrites.set(i, (rand.nextInt(writeProbability) == 1)); 
			}
			
			executor = Executors.newFixedThreadPool((nThreads*nFutures)+nThreads);
			
			sequentially = true;
			startTiming(); 
			executor.invokeAll(transactions);
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			tempoSequencial += finishTiming();
			System.out.println("SEQ: " + tempoSequencial);
			
			System.gc();
			executor = Executors.newFixedThreadPool((nThreads*nFutures)+nThreads);
			sequentially = false;
			startTiming();
			executor.invokeAll(transactions);
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			tempoParalelo += finishTiming();
			
			
			System.out.println("PARALELO: " + tempoParalelo);
			
			
			System.gc();
			if(startCounting) {
				tempoTotalSequencial += tempoSequencial;
				tempoTotalParalelo += tempoParalelo;
				int ntree=0;
				for(RBTreeReadWriteTest tree: trees) {
					futuresAborted.set(ntree,futuresAborted.get(ntree) + tree.futuresaborts);
					interFuturesAborted.set(ntree,interFuturesAborted.get(ntree) + tree.interFuturesAborts);
					earlyAborts.set(ntree,earlyAborts.get(ntree) + tree.earlyAbort);
					commitAborts.set(ntree,commitAborts.get(ntree) + tree.commitaborts);
					ntree++;
					tree.resetAborts();
				}
			}
			else {
				startCounting=true;
			}
			tempoSequencial = 0;
			tempoParalelo = 0;
			nIterations--;
		} 
//		System.out.println("------------- "+ addedValuesSeq);
		String iterationResults = "";
		iterationResults += "\""+ String.valueOf(tempoTotalSequencial / 4)+"\""+",";
		iterationResults += "\""+String.valueOf(tempoTotalParalelo / 4)+ "(" + String.valueOf(tempoTotalSequencial / tempoTotalParalelo) + ")"+"\"" + ",";
		writer.println(iterationResults);
		
		//print number of each abort per thread
		writer.println("");
		for(int i = 0; i< nThreads ; i++) {
			writer.print("Thread " + i+ ",,,,");
		}
		writer.println("");
		for(int i = 0; i< nThreads ; i++) {
			writer.print("futuresAborted,interfuturesAborted,comitbort,earlyabort,");
		}
		writer.println("");
		for(int i = 0; i< nThreads ; i++) {
			writer.print(futuresAborted.get(i)  +",");
			writer.print(interFuturesAborted.get(i)  +",");
			writer.print(commitAborts.get(i)  +",");
			writer.print(earlyAborts.get(i)  +",");
		}
		
		//print total of each abort
		writer.println("");
		writer.println("futuresAborted,interfuturesAborted,comitbort,earlyabort,");
		int totalfuturesAborted =0;
		int totalinterFuturesAborted=0;
		int totalcommitAborts =0;
		int totalearlyaborts =0;
		for(int i = 0; i< nThreads ; i++) {
			totalfuturesAborted += futuresAborted.get(i);
			totalinterFuturesAborted += interFuturesAborted.get(i);
			totalcommitAborts += commitAborts.get(i);
			totalearlyaborts += earlyAborts.get(i);
		}
		writer.println(totalfuturesAborted +"," + totalinterFuturesAborted + "," +totalcommitAborts + ","+totalearlyaborts);
		
		//print futures readonly
		writer.println("");
		writer.println("FuturesReadOnly");
		for(int i = 0; i< nFutures ; i++) {
			writer.print(!toWrites.get(i)+",");
		}
		writer.println("");
		writer.close();

	}

	private static ArrayList<Integer> generateRandomValues(int n) {
		ArrayList<Integer> res = new ArrayList<Integer>(n);
		
		for(int i =0; i < n;i++) {
			res.add(randSearchGenerator.nextInt(populateMaxValue));		
		}
		
		return res;
	}

}