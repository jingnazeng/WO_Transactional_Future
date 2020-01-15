package contlib.RBTreeBenchmark.SequentialVersion;

import java.util.ArrayList;

public class TestResults {
	
	
	static ArrayList<Long> resultsSequentially = new ArrayList<Long>();
	static ArrayList<ArrayList<Long>> resultsSpeculation = new ArrayList<ArrayList<Long>>();
	static ArrayList<Long> resultsSpeculation2 = new ArrayList<Long>();
	
	static boolean sequentially = true;
	
	static public void reset() {
		resultsSequentially = new ArrayList<Long>();
		resultsSpeculation = new ArrayList<ArrayList<Long>>();
		resultsSpeculation2 = new ArrayList<Long>();
		sequentially = true;
	}
	
	static public void compare() {
		
		
		System.out.println(resultsSequentially.size());
		int totalsize =0;
		
		for(ArrayList<Long> a: resultsSpeculation) {
			totalsize += a.size();
		}
		
		System.out.println((resultsSpeculation2.size() + totalsize));
	
		boolean found = false;
		
		for(Long l: resultsSequentially) {
			if(!resultsSpeculation2.contains(l)) {
				for(ArrayList<Long> a: resultsSpeculation) {
					if(a.contains(l)) {
						found = true;
						break;
					}
				}
				if(found) { found = false; break;}
				else System.out.println("Error");
			}
		}
	}
	
	 static public void findMaxAdd(Long l,int iteration) {
		 if(resultsSpeculation.size() <= iteration)
			 resultsSpeculation.add(iteration, new ArrayList<Long>());
		
		if(sequentially) {
			resultsSequentially.add(l);
		}
		else
			resultsSpeculation.get(iteration).add(l);
	}
	 
	 static public void findMinAdd(Long l) {
		 
 		if(sequentially) {
			resultsSequentially.add(l);
		}
		else
				resultsSpeculation2.add(l);
	}


}
