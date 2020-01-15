
package jvstm.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class DiGraphSingleList<V> implements DiGraph<V>{
	
	static class Edge<V>{
		V to_vertex;
		
		Edge(V v){
			this.to_vertex = v;		
		}
		public String toString(){
			return to_vertex.toString();
		}
	}
		
	/**
	 * a map is used to map a vertex to its adjacent list
	 * */	
	Map<V,List<Edge<V>>> neighbours = new ConcurrentHashMap<V,List<Edge<V>>>();
	
	public String toString(){
		StringBuffer s = new StringBuffer();
		for(V v: neighbours.keySet())
			s.append("\n    "+v+" -> "+neighbours.get(v));
		return s.toString();

	}
	 
	 
	 /**
	  * add a vertex to the DAG
	  * */
	 @Override
	 public synchronized void addVertex(V vertex){
		 if(neighbours.containsKey(vertex))
			 return;
		 neighbours.put(vertex, new ArrayList<Edge<V>>());
		 //System.out.println(this.toString());
	 }
	 
	/**
	 * add a edge to the DAG, from source to destination
	 * */
	 public synchronized void addEdge(V source, V destination){
		 this.addVertex(source);
		 this.addVertex(destination);
		 ((Node) destination).setParent((Node)source);
		 neighbours.get(source).add(new Edge<V>(destination));
	 }
	 
	 public synchronized List<V> outboundNeighbors(V vertex){
		 List<V> list = new ArrayList<V>();
		 for(Edge<V> e: neighbours.get(vertex))
			 list.add(e.to_vertex);
		 return list;
	 }
	 
	 public synchronized List<V> inboundNeighbours(V vertex){
		 List<V> inList = new ArrayList<V>();
		 for(V from: neighbours.keySet()){
			 for(Edge<V> to: neighbours.get(from)){
				 if(to.to_vertex.equals(vertex))
					 inList.add(from);
			 }
		 }
		 return inList;
	 }
	 
	 public synchronized void deleteVertex(V vertex){
		 for(V v: inboundNeighbours(vertex)){
			 for(int i = 0; i < neighbours.size();i++){
				 if(neighbours.get(v).get(i).to_vertex.equals(vertex)){
					 neighbours.get(v).remove(i);
					 break;
				 }
			 }
		 }
		 neighbours.remove(vertex);
	 }
	 
	 public synchronized void deleteEdge(V source, V destination){
		 for(int i = 0; i < neighbours.get(source).size(); i++){
			 if(neighbours.get(source).get(i).to_vertex.equals(destination)){
				 //((Node) destination).setParent(null);
				 neighbours.get(source).remove(i);
				 break;
			 }
			 
		 }
	 }
	 
}
