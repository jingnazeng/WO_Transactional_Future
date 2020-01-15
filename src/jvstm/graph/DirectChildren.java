package jvstm.graph;

public class DirectChildren<V> {

	/**
	 * @return the num_of_child
	 */
	public int getNum_of_child() {
		return num_of_child;
	}
	/**
	 * @param num_of_child the num_of_child to set
	 */
	public void setNum_of_child(int num_of_child) {
		this.num_of_child = num_of_child;
	}
	/**
	 * @return the future
	 */
	public Node getFuture() {
		return future;
	}
	/**
	 * @return the conti
	 */
	public Node getConti() {
		return conti;
	}
	/**
	 * @param future the future to set
	 */
	public void setFuture(Node future) {
		this.future = future;
	}
	/**
	 * @param conti the conti to set
	 */
	public void setConti(Node conti) {
		this.conti = conti;
	}
	Node future;
	Node conti;
	int num_of_child;

	DirectChildren(Node future, Node conti, int num){
		this.future = future;
		this.conti = conti;
		this.num_of_child = num;
	}
}
