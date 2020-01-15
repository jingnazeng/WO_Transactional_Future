package benchmark.synthetic;

public class TimerDebug {
	private static final long SAMPLING_BATCH_RATE = 10;
	private static final float EXP_AVG_WEIGHT = 0.5f;
	public static long[] commitStartingTime = new long[56];
	public static float[] commitTime = new float[56];
	private static long[] commitIndex = new long[56];
	private static long[] commitFlag = new long[56]; 
	
	public static long[] transferTimeStartingTime = new long[56];
	public static float[] transferTime = new float[56];
	private static long[] transferTimeIndex = new long[56];
	private static long[] transferTimeFlag = new long[56]; 
	
	public static long[] getTotalStartingTime = new long[56];
	public static float[] getTotalTime = new float[56];
	private static long[] getTotalIndex = new long[56];
	private static long[] getTotalFlag = new long[56]; 
	
	public static long[] readingStartingTime = new long[56];
	public static float[] readingTime = new float[56];
	private static long[] readingTimeIndex = new long[56];
	private static long[] readingTimeFlag = new long[56]; 
	
	public static long[] writingStartingTime = new long[56];
	public static float[] writingTime = new float[56];
	private static long[] writingTimeIndex = new long[56];
	private static long[] writingTimeFlag = new long[56]; 
	
	public static long[]  forwardValidationStartingTime = new long[56];
	public static float[] forwardValidationTime = new float[56];
	private static long[] forwardValidationIndex = new long[56];
	private static long[] forwardValidationFlag = new long[56]; 
	
	public static long[]  backwardValidationStartingTime = new long[56];
	public static float[] backwardValidationTime = new float[56];
	private static long[] backwardValidationIndex = new long[56];
	private static long[] backwardValidationFlag = new long[56]; 
	
	public static long[] futureLauched = new long[56];
	public static long[] abortCount = new long[56];
	public static long[] committedFuture = new long[56];
	

	public static float getAverage(float[] entries) {
		int count = 0;
		float sum = 0;
		for(float entry : entries) {
			if(entry!=0){
				count++;
				sum += entry;
			}
		}

		return sum / count;
	}


	public static float getAverage(long[] times) {
		int count = 0;
		long sum = 0;
		for(long time : times) {
			if(time!=0){
				count++;
				sum += time;
			}
		}

		return sum/ count;
	}
	
	public static long getSum(long[] times) {
		long sum = 0;
		for(long time : times) {
			if(time!=0){
				sum += time;
			}
		}

		return sum;
	}


	public static void startCommit(Integer threadID) {
		if((((commitIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((commitFlag[threadID]%2)==0) {
				commitStartingTime[threadID] = System.nanoTime();
				commitFlag[threadID]++;
			}
		}
	}

	public static void endCommit(Integer threadID) {
		if((commitFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - commitStartingTime[threadID];
			commitTime[threadID] = commitTime[threadID] + (EXP_AVG_WEIGHT*(sample-commitTime[threadID]));
			commitFlag[threadID]++;
		}
	}

	public static float[] getCommitTime(){
		return commitTime;
	}
	
	public static void startForwardValidation(Integer threadID) {
		if((((forwardValidationIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((forwardValidationFlag[threadID]%2)==0) {
				forwardValidationStartingTime[threadID] = System.nanoTime();
				forwardValidationFlag[threadID]++;
			}
		}
	}

	public static void endForwardValidation(Integer threadID) {
		if((forwardValidationFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - forwardValidationStartingTime[threadID];
			forwardValidationTime[threadID] = forwardValidationTime[threadID] + (EXP_AVG_WEIGHT*(sample-forwardValidationTime[threadID]));
			forwardValidationFlag[threadID]++;
		}
	}

	public static float[] getforwardValidationTime(){
		return forwardValidationTime;
	}

	
	public static void startBackwardValidation(Integer threadID) {
		if((((backwardValidationIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((backwardValidationFlag[threadID]%2)==0) {
				backwardValidationStartingTime[threadID] = System.nanoTime();
				backwardValidationFlag[threadID]++;
			}
		}
	}

	public static void endbackwardValidation(Integer threadID) {
		if((backwardValidationFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - backwardValidationStartingTime[threadID];
			backwardValidationTime[threadID] = backwardValidationTime[threadID] + (EXP_AVG_WEIGHT*(sample-backwardValidationTime[threadID]));
			backwardValidationFlag[threadID]++;
	//		System.out.println("backward validation duration: "+ sample);
		}
	}
	
	public static void startTransferTime(Integer threadID) {
		if((((transferTimeIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((transferTimeFlag[threadID]%2)==0) {
				transferTimeStartingTime[threadID] = System.nanoTime();
				transferTimeFlag[threadID]++;
			}
		}
	}

	public static void endTransferTime(Integer threadID) {
		if((transferTimeFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - transferTimeStartingTime[threadID];
			transferTime[threadID] = transferTime[threadID] + (EXP_AVG_WEIGHT*(sample-transferTime[threadID]));
			transferTimeFlag[threadID]++;
		}
	}
	
	public static void startGetTotalTime(Integer threadID) {
		if((((getTotalIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((getTotalFlag[threadID]%2)==0) {
				getTotalStartingTime[threadID] = System.nanoTime();
				getTotalFlag[threadID]++;
			}
		}
	}

	public static void endGetTotalTime(Integer threadID) {
		if((getTotalFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - getTotalStartingTime[threadID];
			getTotalTime[threadID] = getTotalTime[threadID] + (EXP_AVG_WEIGHT*(sample-getTotalTime[threadID]));
			getTotalFlag[threadID]++;
		}
	}
	
	public static void startReadingTime(Integer threadID) {
		if((((readingTimeIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((readingTimeFlag[threadID]%2)==0) {
				readingStartingTime[threadID] = System.nanoTime();
				readingTimeFlag[threadID]++;
			}
		}
	}

	public static void endReadingTime(Integer threadID) {
		if((readingTimeFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - readingStartingTime[threadID];
			readingTime[threadID] = readingTime[threadID] + (EXP_AVG_WEIGHT*(sample-readingTime[threadID]));
			readingTimeFlag[threadID]++;
		}
	}
	
	public static void startWritingTime(Integer threadID) {
		if((((writingTimeIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((writingTimeFlag[threadID]%2)==0) {
				writingStartingTime[threadID] = System.nanoTime();
				writingTimeFlag[threadID]++;
			}
		}
	}

	public static void endWritingTime(Integer threadID) {
		if((writingTimeFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - writingStartingTime[threadID];
			writingTime[threadID] = writingTime[threadID] + (EXP_AVG_WEIGHT*(sample-writingTime[threadID]));
			writingTimeFlag[threadID]++;
		}
	}
	
	
	
	public static float[] getTransferTime(){
		return transferTime;
	}

	public static float[] getTotalTime(){
		return getTotalTime;
	}
	
	public static float[] getBackwardValidationTime(){
		return backwardValidationTime;
	}
	
	public static float[] getReadingTime(){
		return readingTime;
	}
	
	public static float[] getWritingTime(){
		return writingTime;
	}
	
	
	public static void startFuture(Integer threadID) {
		futureLauched[threadID]++;
	}
	
	public static void abortIncrement(Integer threadID) {
		abortCount[threadID]++;
	}
	
	public static void futureCommit(Integer threadID) {
		committedFuture[threadID]++;
	}
	
	public static float getFutureAbortRate(){
		long abortCountAccu = getSum(abortCount);
		long commitedFutureAccu = getSum(committedFuture);
		float abort_rate = (float) ((abortCountAccu*1.0)/(abortCountAccu+commitedFutureAccu));
		return abort_rate;
		
	}
	
}
