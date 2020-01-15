package jvstm.graph.test;

import jvstm.VBox;

public class TransactionTest {
	
	static public VBox<Integer> control_X = new VBox<Integer>();
    static public VBox<Integer> control_Y = new VBox<Integer>();
    static public VBox<Integer> control_Z = new VBox<Integer>();
    static public VBox<Integer> control_A = new VBox<Integer>();
    static public VBox<Integer> control_B = new VBox<Integer>();
    static public VBox<Integer> control_C = new VBox<Integer>();
    
    
    
	
	public static void main(String[] args){
		TestCaseB txn1= new TestCaseB();
		Thread t1 = new Thread(txn1);
		t1.start();
		
	}

}
