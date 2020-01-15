package jvstm.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jvstm.graph.Node.Type;

public class TransactionaFutureDAGSingleList extends DiGraphSingleList<Node> {
	
	public Node upstreamNode(Node node){
		 List<Node> list = new ArrayList<Node>();
		 list = inboundNeighbours(node);
		 //by transactional future DAG construction, one node can only have one parent
		 if(list.size() > 1){
			 throw new RuntimeException();
		 }else if(list.size()==0){
			 return null;
		 }else 
			 return list.get(0);		 
	 }
	 
	 public DirectChildren<Node> directDownStreamNodes(Node node){
		 List<Node> possibleDirectChildren = new ArrayList<Node>();
		 
//		 for(Node from: neighbours.keySet()){
//			 for(Edge<Node> to: neighbours.get(from)){
//				 if(from.equals(node))
//					 possibleDirectChildren.add(to.to_vertex);
//			 }
//		 }
		 for(Edge<Node> v: neighbours.get(node)){
			 possibleDirectChildren.add(v.to_vertex);
		 }
		 
		 
		 if(possibleDirectChildren.size()>2){
			 throw new RuntimeException();
		 }else if(possibleDirectChildren.size() == 0){
			 return null;
		 }else if(possibleDirectChildren.size() == 1){
			 if(possibleDirectChildren.get(0).type == Type.begin_future)
				 return new DirectChildren<Node>(possibleDirectChildren.get(0),null,1);
			 else
				 return new DirectChildren<Node>(null,possibleDirectChildren.get(0),1);
		 }else //inList.size() == 2
			 if(possibleDirectChildren.get(0).type == Type.begin_future)
				 return new DirectChildren<Node>(possibleDirectChildren.get(0),possibleDirectChildren.get(1),2);
			 else
				 return new DirectChildren<Node>(possibleDirectChildren.get(1),possibleDirectChildren.get(0),2);
	 }

	 public Node getSubsequentContinuation(Node node_beforeConti) {
		 Set<Node> subtxn_as_continuation = new HashSet<Node>();
		 List<Node> outbound_txn = this.outboundNeighbors(node_beforeConti);
		 if(outbound_txn.size() == 2){
			 if(outbound_txn.get(0).getType() == Type.continuation){
				 subtxn_as_continuation.add(outbound_txn.get(0));
			 }else if(outbound_txn.get(1).getType() == Type.continuation){
				 subtxn_as_continuation.add(outbound_txn.get(1));
			 }
			 return subtxn_as_continuation.iterator().next();
		 }else{
			 return outbound_txn.get(0);
		 }
		 
	 }
	
}
