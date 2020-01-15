package benchmark.synthetic.transactions;

import java.util.concurrent.Callable;

import benchmark.synthetic.ArrayAccess;
import jvstm.EarlyAbortException;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingTransactionalFutureTask;
import jvstm.graph.FutureRepresentation;

/** for futures: Long prefix: first read a disjoint set of keys, then write to a hot spot 
***	for continuations : read a "hot spot" 
**  this patter5 is generalized to more threads, and this class is an example of 4 futures..
**/

public class PatternTxn6By8Futures extends ArrayAccessWriteTxnWithMultipleFutures{
	VBox<Integer>[] array;
	private int value;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private String high_contention = "false";
	private int max_num_of_core;
	private int hot_spot;
	private int number_of_hot_spots_read_and_write;
	private int num_of_prefix_sequential_read;
	
	public PatternTxn6By8Futures(int max_num_of_core, int prefix_disjoint_read, int hot_spot_in_the_array,int num_of_hot_spots_read_and_write, String high_contention, int spin){
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

	@Override
	public int executeTransaction(Transaction tx, int sibling, int streaming) throws Throwable {

		array = ArrayAccess.getArray();
		array_length = array.length;
		
		int num_of_futures = 8;
		
		for(int i = 1; i <= max_num_of_core/num_of_futures ; i++){

			FutureRepresentation<Integer> f1 = ((ReadWriteTransaction)Transaction.current()).
					submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
			FutureRepresentation<Integer> f2;
			FutureRepresentation<Integer> f3;
			FutureRepresentation<Integer> f4;
			FutureRepresentation<Integer> f5;
			FutureRepresentation<Integer> f6;
			FutureRepresentation<Integer> f7;
			FutureRepresentation<Integer> f8;
			while(true){
				try{
					//T2 logic
					readHotSpots(number_of_hot_spots_read_and_write);
					//Tf2 logic
					f2  = ((ReadWriteTransaction)Transaction.current()).
							submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture2()));

					while(true){
						try{
							//T3 logic
							readHotSpots(number_of_hot_spots_read_and_write);
							//Tf3 logic
							f3  = ((ReadWriteTransaction)Transaction.current()).
									submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture3()));
							while(true){
								try{
									//T4 logic
									readHotSpots(number_of_hot_spots_read_and_write);
									//Tf4 logic
									f4  = ((ReadWriteTransaction)Transaction.current()).
											submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture4()));
									while(true){
										try{
											//T5 logic
											readHotSpots(number_of_hot_spots_read_and_write);
											f5  = ((ReadWriteTransaction)Transaction.current()).
													submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
											while(true){
												try{
													//T6 logic
													readHotSpots(number_of_hot_spots_read_and_write);
													f6  = ((ReadWriteTransaction)Transaction.current()).
															submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
													while(true){
														try{
															//T7 logic
															readHotSpots(number_of_hot_spots_read_and_write);
															f7  = ((ReadWriteTransaction)Transaction.current()).
																	submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
															while(true){
																try{
																	//T8 logic
																	readHotSpots(number_of_hot_spots_read_and_write);
																	f8  = ((ReadWriteTransaction)Transaction.current()).
																			submitWeakOrderingFuture(new WeakFutureTask(new AsychOpsFuture1()));
																	while(true){
																		try{
																			//T9 logic
																			readHotSpots(number_of_hot_spots_read_and_write);
																			value += (Integer) Transaction.current().evalWeakOrderingFuture(f1);
																			break;
																		}catch(Throwable e){
																			if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
																				throw e;
																			else{
																				e.printStackTrace();
																				System.out.println("T9 restarting");
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
																		System.out.println("T8 restarting");
																	}
																}
															}
															value += (Integer) Transaction.current().evalWeakOrderingFuture(f3);
															break;
														}catch(Throwable e){
															if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
																throw e;
															else{
																e.printStackTrace();
																System.out.println("T7 restarting");
															}
														}
													}
													value += (Integer) Transaction.current().evalWeakOrderingFuture(f4);
													break;
												}catch(Throwable e){
													if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
														throw e;
													else{
														e.printStackTrace();
														System.out.println("T6 restarting");
													}
												}
											}
											value += (Integer) Transaction.current().evalWeakOrderingFuture(f5);
											break;
										}catch(Throwable e){
											if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
												throw e;
											else{
												e.printStackTrace();
												System.out.println("T5 restarting");
											}
										}
									}
									value += (Integer) Transaction.current().evalWeakOrderingFuture(f6);
									break;
								}catch(Throwable e){
									if(e.getCause() instanceof EarlyAbortException || e instanceof EarlyAbortException)
										throw e;
									else{
										e.printStackTrace();
										System.out.println("T4 restarting");
									}
								}

							}
							value += (Integer) Transaction.current().evalWeakOrderingFuture(f7);
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
					value += (Integer) Transaction.current().evalWeakOrderingFuture(f8);
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


	class AsychOpsFuture1 implements Callable<Integer>{

		public AsychOpsFuture1(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf1 logic
			readSequentially();
			writeHotSpots(number_of_hot_spots_read_and_write);
			
		
			return value;

		}
	}


	class AsychOpsFuture2 implements Callable<Integer>{

		public AsychOpsFuture2(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf2 logic
			readSequentially();

			writeHotSpots(number_of_hot_spots_read_and_write);
		
		
			return value;
		}
	}
	
	class AsychOpsFuture3 implements Callable<Integer>{

		public AsychOpsFuture3(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf2 logic
			readSequentially();

			writeHotSpots(number_of_hot_spots_read_and_write);
		
		
			return value;
		}
	}
	
	class AsychOpsFuture4 implements Callable<Integer>{

		public AsychOpsFuture4(){

		}
		@Override
		public Integer call() throws Exception {
			//Tf2 logic
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