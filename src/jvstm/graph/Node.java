package jvstm.graph;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import jvstm.Transaction;
import jvstm.VBox;

public class Node{
	
	
	private Node parent = null;
	
	public void setParent(Node n) {
		this.parent = n;
	}
	
	public Node getParent() {
		return this.parent;
	}
	
	/**
	 * @return the residingDAG
	 */
	public DAG getResidingDAG() {
		return residingDAG;
	}
	/**
	 * @param residingDAG the residingDAG to set
	 */
	public void setResidingDAG(DAG residingDAG) {
		this.residingDAG = residingDAG;
	}
	/**
	 * @return the asscociatedTxn
	 */
	public Transaction getAsscociatedTxn() {
		return asscociatedTxn;
	}
	/**
	 * @param asscociatedTxn the asscociatedTxn to set
	 */
	public void setAsscociatedTxn(Transaction asscociatedTxn) {
		this.asscociatedTxn = asscociatedTxn;
	}
	/**
	 * @return the lastConti
	 */
	public Node getLastConti() {
		return lastConti;
	}
	/**
	 * @param lastConti the lastConti to set
	 */
	public void setLastConti(Node lastConti) {
		this.lastConti = lastConti;
	}
	/**
	 * @return the closingFuture
	 */
	public Node getClosingFuture() {
		return closingFuture;
	}
	/**
	 * @return the futureEval
	 */
	public Node getFutureEval() {
		return futureEval;
	}
	/**
	 * @param closingFuture the closingFuture to set
	 */
	public void setClosingFuture(Node closingFuture) {
		this.closingFuture = closingFuture;
	}
	/**
	 * @param futureEval the futureEval to set
	 */
	public void setFutureEval(Node futureEval) {
		this.futureEval = futureEval;
	}
	/**
	 * @return the futureSubmittingNode
	 */
	public Node getFutureSubmittingNode() {
		return futureSubmittingNode;
	}
	/**
	 * @return the futureConti
	 */
	public Node getFutureConti() {
		return futureConti;
	}
	/**
	 * @param futureSubmittingNode the futureSubmittingNode to set
	 */
	public void setFutureSubmittingNode(Node futureSubmittingNode) {
		this.futureSubmittingNode = futureSubmittingNode;
	}
	/**
	 * @param futureConti the futureConti to set
	 */
	public void setFutureConti(Node futureConti) {
		this.futureConti = futureConti;
	}
	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}
	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}
	public static enum Status {iCommitted, completed, active,evaluated, waitToRestart,aborted};
	public static enum Type {begin_toplevel, begin_future, continuation, evaluation};
	
	Transaction asscociatedTxn;
	Status status;
	Type type;
	//Map<VBox,T> readList = new HashMap<VBox,T>();
	//Map<VBox,T> writeList = new HashMap<VBox,T>();
	
	DAG residingDAG;
	
	//TODO:  different node types as subclass
	//below reference is only relevant to Type.begin_future
	Node futureSubmittingNode;
	Node futureConti;
	Node closingFuture;
	
	CountDownLatch latchOnEvaluatedStatus = new CountDownLatch(1);
	
	/**
	 *
	 * */
	Node futureEval;
	Node lastConti;
	
	public String toString(){
		return this.hashCode() +" "+this.type + "("+this.status+")";
	}
	
}
