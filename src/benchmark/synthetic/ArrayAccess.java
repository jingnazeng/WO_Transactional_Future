package benchmark.synthetic;

import java.util.ArrayList;
import java.util.Map;

import jvstm.VBox;

public class ArrayAccess {
	
//	static ArrayList<VBox<Integer>> array;
	private static VBox<Integer>[] array;
	
	@SuppressWarnings("unchecked")
	public ArrayAccess(int arrayLength){
//		array = new ArrayList<VBox<Integer>>(arrayLength);
//		populateArray(array,arrayLength);
		
		setArray((VBox<Integer>[]) new VBox[arrayLength]);
		populatePlainArray(getArray(), arrayLength);
	}
	

	private void populatePlainArray(VBox<Integer>[] array2, int arrayLength) {
		for(int i = 0; i < arrayLength ; i++){
			getArray()[i] = new VBox<Integer>((int)(Math.random()*1000));
		}
	}


	private void populateArray(ArrayList<VBox<Integer>> array, int arrayLength) {
		for(int i = 0 ; i < arrayLength ; i++){
			array.add(new VBox<Integer>((int)(Math.random()*1000)));
		}
	}


	public static void main(String[] args) throws InterruptedException {
		//input order of parameters 
		//0: array length, number of items in the array
		//1: number of threads
		//2: number of siblings(futures or parallel nested branches)
		//3: number of bank agency
		//4: duration for the simulation,in seconds
		//5: max number of cores
		//6:  number of reads in prefix disjoint read
		//7: number of hot-spot in the whole array
		//8: number of read and write to the hot spots
		//9: whether write and read to hotspot is in high contention, should be always true
		//10: spin between each read
		
		new ArrayAccess(Integer.parseInt(args[0])*Integer.parseInt(args[3]));
		ArrayAccessStressor stressor = new ArrayAccessStressor();
		stressor.setThreadsNum(Integer.parseInt(args[1]));
		stressor.setSiblingNum(Integer.parseInt(args[2]));
		stressor.setBankAgencyNum(Integer.parseInt(args[3]));
		
		stressor.setSimuDuration(Integer.parseInt(args[4]));
		
		stressor.setMaxNumofCores(Integer.parseInt(args[5]));
		stressor.setNumReadInPrefixDisjointRead(Integer.parseInt(args[6]));
		stressor.setNumofHotspots(Integer.parseInt(args[7]));
		stressor.setNumofReadAndWriteToHotSpots(Integer.parseInt(args[8]));
		stressor.setWriteInHighContention(args[9]);
		stressor.setCpuWorkBetweenMemoryRead(Integer.parseInt(args[10]));
		
		stressor.setBankOperationPecentage(Float.parseFloat(args[11]));
		stressor.setNum_of_rounds_for_transfer(Integer.parseInt(args[12]));
		stressor.setStreaming(Integer.parseInt(args[13]));
		
		
		Map<String,String> results = stressor.stress();
		for(Map.Entry<String, String> pair:results.entrySet()){
			System.out.println(pair.getKey()+" : "+pair.getValue());
		}  
		
	}


	public static VBox<Integer>[] getArray() {
		return array;
	}


	public static void setArray(VBox<Integer>[] array) {
		ArrayAccess.array = array;
	}

}
