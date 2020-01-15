package jvstm.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * using two individual maps to track inbound and outbound adjacency list 
 * fast when getting "inboundNeighbors and outboundNeighbours"
 * more space usage as we need to store two adjacency list 
 * 
 * */

public class DiGraphDoubleList<V> implements DiGraph<V> {
	
	static class Edge<V>{
		V vertex;
		
		Edge(V v){
			this.vertex = v;		
		}
	}
		
	/**
	 * a map is used to map a vertex to a list of outbound vertex 
	 * */	
	Map<V,List<Edge<V>>> outboundNeighboursMap = new HashMap<V,List<Edge<V>>>();
	
	/**
	 * a map is used to map a vertex to a list of inbound vertex 
	 * */	
	Map<V,List<Edge<V>>> inboundNeighboursMap = new HashMap<V,List<Edge<V>>>();

	@Override
	public void addVertex(V vertex) {

		 if(outboundNeighboursMap.containsKey(vertex))
			 return;
		 outboundNeighboursMap.put(vertex, new ArrayList<Edge<V>>());
		 inboundNeighboursMap.put(vertex, new ArrayList<Edge<V>>());
	 		
	}

	@Override
	public void deleteVertex(V vertex) {

		 outboundNeighboursMap.remove(vertex);
		 for(V v: inboundNeighbours(vertex)){
			 outboundNeighboursMap.get(v).remove(vertex);
		 }
		 
		 inboundNeighboursMap.remove(vertex);
		 for(V v:outboundNeighbors(vertex)){
			 inboundNeighboursMap.get(v).remove(vertex);
		 }
		 
	 
	}

	@Override
	public void addEdge(V source, V destination) {
		 this.addVertex(source);
		 this.addVertex(destination);
		 outboundNeighboursMap.get(source).add(new Edge<V>(destination));	
		 inboundNeighboursMap.get(destination).add(new Edge<V>(source));
	}

	@Override
	public void deleteEdge(V source, V destination) {
		outboundNeighboursMap.get(source).remove(destination);
		inboundNeighboursMap.get(destination).remove(source);
	}

	@Override
	public List<V> inboundNeighbours(V vertex) {
		 List<V> list = new ArrayList<V>();
		 for(Edge<V> e: inboundNeighboursMap.get(vertex))
			 list.add(e.vertex);
		 return list;
	}

	@Override
	public List<V> outboundNeighbors(V vertex) {
		 List<V> list = new ArrayList<V>();
		 for(Edge<V> e: outboundNeighboursMap.get(vertex))
			 list.add(e.vertex);
		 return list;
	}

}
