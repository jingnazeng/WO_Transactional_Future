package jvstm.graph;

import java.util.concurrent.Future;

public class FutureRepresentation<E> {
	
	private boolean evaluted = false;
	
	public void setEvaluated() {
		evaluted = true;
	}
	
	public boolean isEvaluated() {
		return evaluted;
	}
	
	/**
	 * @return the f_ref
	 */
	public Future<E> getF_ref() {
		return f_ref;
	}
	/**
	 * @return the future_node
	 */
	public Node getFuture_node() {
		return future_node;
	}
	/**
	 * @param f_ref the f_ref to set
	 */
	public void setF_ref(Future<E> f_ref) {
		this.f_ref = f_ref;
	}
	/**
	 * @param future_node the future_node to set
	 */
	public void setFuture_node(Node future_node) {
		this.future_node = future_node;
	}
	Future<E> f_ref;
	Node future_node;
	
	public FutureRepresentation(Future<E> f_ref, Node future_node){
		this.f_ref = f_ref;
		this.future_node = future_node;
	}
	

}
