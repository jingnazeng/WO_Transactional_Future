package jvstm.graph;

import java.util.List;

public interface DiGraph<V> {
	

	void addVertex(V vertex);
	void deleteVertex(V vertex);
	void addEdge(V source, V destination);
	void deleteEdge(V source, V destination);
	List<V> inboundNeighbours(V vertex);
	List<V> outboundNeighbors(V vertex);
	 

}
