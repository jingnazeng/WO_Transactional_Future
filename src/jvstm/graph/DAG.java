package jvstm.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import benchmark.synthetic.TimerDebug;
import jvstm.ReadWriteTransaction;
import jvstm.TopLevelTransaction;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.WeakOrderingFutureSubTransaction;
import jvstm.graph.Node.Status;
import jvstm.graph.Node.Type;

public class DAG {
	
	/**
	 * @return the dagStatus
	 */
	public DAGStatus getDagStatus() {
		return dagStatus;
	}

	/**
	 * @param dagStatus the dagStatus to set
	 */
	public void setDagStatus(DAGStatus dagStatus) {
		this.dagStatus = dagStatus;
	}

	/**
	 * @return the graph
	 */
	public TransactionaFutureDAGSingleList getGraph() {
		return graph;
	}

	/**
	 * @param graph the graph to set
	 */
	public void setGraph(TransactionaFutureDAGSingleList graph) {
		this.graph = graph;
	}

	/**
	 * @return the graphLock
	 */
	public GraphLock getGraphLock() {
		return graphLock;
	}

	/**
	 * @param graphLock the graphLock to set
	 */
	public void setGraphLock(GraphLock graphLock) {
		this.graphLock = graphLock;
	}


	TransactionaFutureDAGSingleList graph = new TransactionaFutureDAGSingleList();
	GraphLock graphLock = new GraphLock(this);

	
	DAGStatus dagStatus = DAGStatus.ACTIVE;
	public static enum DAGStatus {ACTIVE, ABORTED};
	
	
	public Node getParentNode(Node node){
		return graph.upstreamNode(node);
	}
	
	public DirectChildren<Node> getDirectChildrenNode(Node node){
		return graph.directDownStreamNodes(node);
	}
	
	
	public <T> T mostRecentCommittedVerstion(jvstm.VBox<T> vbox, Node node){
		//navigate upwards from T and for each iCommitted sub-transaction T’ preceding T in the graph
		Node previousNode = node.getParent(); //this.getParentNode(node); 
		//System.out.println("------------------");
		//System.out.println("traversing the DAG");
		//System.out.println("------------------");
		int i = 0;
		int c = 0;
		
		while(previousNode!=null){
			//System.out.println("going back one node: " + i++ + ", " + c);
			if(previousNode.status == Status.iCommitted){
				c++;
				Map<VBox, Object> boxesWritten = ((ReadWriteTransaction) previousNode.getAsscociatedTxn()).getBoxesWritten();
				if (boxesWritten != ReadWriteTransaction.EMPTY_MAP) {
					T value = (T) boxesWritten.get(vbox);
					if (value != null) {
						return (value == ReadWriteTransaction.NULL_VALUE) ? null : value;
					}
				}
			}
			previousNode = previousNode.getParent(); //this.getParentNode(previousNode);
		}
		//System.out.println("------------------");
		return null;
	}
	
	/**
	 *  return all ancestors of V up to the root/top-level 
	 **/
	List<Node> getAncestors(Node v){
		List<Node> txns = new ArrayList<Node>();
		List<Node> inbound = graph.inboundNeighbours(v);
		if(inbound.size()==0)
			return txns;
		for(Node inboundVertex: inbound){
			txns.add(inboundVertex);
			List<Node> further_ancestor = getAncestors(inboundVertex);
			if(further_ancestor.size()!=0){
				txns.addAll(further_ancestor);
			}
		}
		return txns;
	}
	
	/**
	 *  return  all iCommited Vertices from current Vertex 
	 *  navigating forwards until current end of the top level transaction
	 **/
	List<Node> getForwardICommittedTxn(Node v){
		List<Node> txns = new ArrayList<Node>();
		List<Node> outbound = graph.outboundNeighbors(v);
		if(outbound.size()==0)
			return txns;
		for(Node outboundVertex: outbound){
			if(outboundVertex.getStatus() == Status.iCommitted)
				txns.add(outboundVertex);
			List<Node> further_forward = getForwardICommittedTxn(outboundVertex);
			if(further_forward.size()!=0){
				txns.addAll(further_forward);
			}
		}
		return txns;
	}
	
	/**
	 *  return  all Vertices from current Vertex 
	 *  navigating forwards until current end of the top level transaction
	 **/
//	List<Node> getForwardTxn(Node v){
//		List<Node> txns = new ArrayList<Node>();
//		txns.addAll(getContinuation(v));
//		List<Node> outbound = graph.outboundNeighbors(v);
//		if(outbound.size()==0)
//			return txns;
//		for(Node outboundVertex: outbound){			
//			txns.add(outboundVertex);
//			List<Node> further_forward = getForwardICommittedTxn(outboundVertex);
//			if(further_forward.size()!=0){
//				txns.addAll(further_forward);
//			}
//		}
//		return txns;
//	}
	
	List<Node> getForwardTxn(Node v){
		List<Node> txns = new ArrayList<Node>();

		Node parent = graph.inboundNeighbours(v).get(0);
		Node first_conti = graph.getSubsequentContinuation(parent);	
		txns.add(first_conti);	
		addAllForwardNode(txns,first_conti);
		
		return txns;
	}
	
	
	private void addAllForwardNode(List<Node> txns, Node first_conti) {
		List<Node> outbound_txn = graph.outboundNeighbors(first_conti);
		Node first;
		Node second;
		if(outbound_txn.size() == 0){
			return;
		}else if(outbound_txn.size() == 1){
			first = outbound_txn.get(0);
			txns.add(first);
			addAllForwardNode(txns,first);
		}else{
			first = outbound_txn.get(0);
			addAllForwardNode(txns,first);
			second = outbound_txn.get(1);
			addAllForwardNode(txns,second);
			txns.add(first);
			txns.add(second);
		}
	}

	/**
	 * return all the internal committed ancestors of the input Vertex up to the root/top-level
	 * */
	List<Node> getICommitedAncestors(Node v){
		List<Node> txns = new ArrayList<Node>();
		List<Node> inbound = graph.inboundNeighbours(v);
		if(inbound.size()==0)
			return txns;
		for(Node inboundVertex: inbound){
			if(inboundVertex.getStatus() == Status.iCommitted)
				txns.add(inboundVertex);
			List<Node> further_ancestor = getAncestors(inboundVertex);
			if(further_ancestor.size()!=0){
				txns.addAll(further_ancestor);
			}
		}
		return txns;
	}
	
	
	/**
	 * start a transaction by add a vertex V_Begin in DAG 
	 * @return 
	 * */
	public Node startTxn(TopLevelTransaction toplevelTxn){
		Node v_begin_top_level = new Node();
		v_begin_top_level.setType(Type.begin_toplevel);
		v_begin_top_level.setStatus(Status.active);
		v_begin_top_level.setAsscociatedTxn(toplevelTxn);
		v_begin_top_level.setResidingDAG(this);
		
		graph.addVertex(v_begin_top_level);
		return v_begin_top_level;
	}
	
	
	/**
	 * update the DAG, add two vertices VB  (representing F) and VC (representing C), 
	 * and two edges directed from spawning txn T to F and C
	 * @return 
	 * */
	public Node futureSubmission(Transaction submittingTxn){
		
		Node submittingVertex = (Node) submittingTxn.getAssociatedNode();
		//iCommit continuation which submits this Future
		iCommitContinuationUponSubmitFuture(submittingTxn);
		
		// create vertices representing Future and Continuation
		Node v_begin_future = new Node();
		v_begin_future.setType(Type.begin_future);
		v_begin_future.setStatus(Status.active);
		v_begin_future.setResidingDAG(this);
		
		
		Node v_conti = new Node();
		v_conti.setType(Type.continuation);
		v_conti.setStatus(Status.active);
		v_conti.setResidingDAG(this);
		
		v_begin_future.setFutureSubmittingNode(submittingVertex);
		v_begin_future.setFutureConti(v_conti);
		
		// add edges from spawning vertex to the Future and the Continuation
		// Below we are manipulating the DAG by adding vertices to it. 
		//Here we could have a concurrent forward validation by a previously submitted/concurrent future, 
		//which looks into the spawning node while we are adding edges to it. 
		//Lets protect the spawning with a write lock and leave as an optimization to do it lock-free (e.g., via HTM).

		this.graphLock.writeLock();
		graph.addVertex(v_begin_future);
		graph.addVertex(v_conti);
		
		graph.addEdge(submittingVertex, v_begin_future);
		graph.addEdge(submittingVertex, v_conti);	
	//	System.out.println(graph);
		this.graphLock.writeUnlock();
		
		return v_begin_future;
	}

	/**
	 * upon iCommit the continuation when submitting a future 
	 * */
	public void iCommitContinuationUponSubmitFuture(Transaction submittingTxn) {
		Node submittingVertex = (Node) submittingTxn.getAssociatedNode();
		//lock on DAG upon every read operation, continuation's iCommit is waived from validation 
		if(submittingVertex == null){
			System.out.println("submittingTxn: "+ submittingTxn);
			System.out.println("submittingVertex: "+ submittingVertex);
			System.out.println("DAG: "+ submittingTxn.getTopLevelTrasanction().getDAG().graph);
		}
		submittingVertex.setStatus(Status.iCommitted);
	}
	
	/**
	 * upon iCommit the continuation when evaluation a future 
	 * 
	 * OR
	 * 
	 * upcon iCommit the eval nade when the top-level transaction commits
	 * */
	public void iCommitNode(Node node) {
		//lock on DAG upon every read operation, continuation's iCommit is waived from validation 
		//TODO: is it also true for iCommit at evaluation?
		if(this.dagStatus == DAGStatus.ABORTED){
			node.getAsscociatedTxn().abortTx();
		}else{
			node.getAsscociatedTxn().commitTx(false);
			node.setStatus(Status.iCommitted);
		}
	}
	
	boolean evaluate(Node future,Node lastConti){
		Node eval = new Node();
		future.setFutureEval(eval);
		future.setLastConti(lastConti);
		
		this.graphLock.writeLock();
		graph.addVertex(eval);
		graph.addEdge(lastConti, eval);
		this.graphLock.writeUnlock();
		
		future.setStatus(Status.evaluated);
		while(future.getStatus()!=Status.iCommitted){
			//wait
		}
		return true;
	}
	
	/**
	 *  commit the re-executed future sub-txn
	 * 
	 * */
	
	public void iCommitReexecutedFuture(Node future) {
		this.graphLock.writeLock();
		future.status = Status.iCommitted;
		this.graphLock.writeUnlock();

	}
	
	public void iCommitFutureUponCompletion(Node future) throws FutureRestartException{
	//	System.out.println("try to get write lock on graph");
		this.graphLock.writeLock();
		future.setStatus(Status.completed);
		//try first to serialize F at its submission, waive backwards validation
		if(forwardValidation(future)){ // can serialize upon submission
			updateAndSerializeFutureAtSubmission(future);
		//	System.out.println("serialized at submission: "+ future.getType().toString());
			this.graphLock.writeUnlock();
			future.setStatus(Status.iCommitted);
			return;
		}
		else {// serialize at evaluation
			
			this.graphLock.writeUnlock();
//			System.out.println("wait for future to become evaluated");
			
			try {
				
				future.latchOnEvaluatedStatus.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			while(future.getStatus()!=Status.evaluated){
//				
//			}
			//System.out.println("DONE wait for future to become evaluated");
			this.graphLock.writeLock();
			TimerDebug.startBackwardValidation((int) (Thread.currentThread().getId()%56));
			boolean backwardResult = backwardValidationAgainstContinuation(future);
			TimerDebug.endbackwardValidation((int) (Thread.currentThread().getId()%56));
			if(backwardResult==true){
				updateAndSerializeFutureAtEvaluation(future);
		//		System.out.println("serialized at evaluation: "+ future.getType().toString() );
				this.graphLock.writeUnlock();
				future.setStatus(Status.iCommitted);
				return;
			}
			else{
			//	System.out.println("speculate at evaluation, means future need to restart");
				speculateFutureAtEvaluation(future);
				this.graphLock.writeUnlock();
				throw new FutureRestartException();
			}
		}
		
	}
	
	/** the methods below assume to be called in a DAG exclusive lock or in an atomically do block
    *   this method must create a new node in the DAG that serializes “upon evaluation” and 
    *	is  marked as Future and contains all RSs and WSs of the vertices encountered navigating backwards 
    *	till the beginning node of the future that is committing
	*/


	private void updateAndSerializeFutureAtSubmission(Node future) {
//		if(future!=null)
//			return;
		Node consolidatedFuture = consolidatedFuture(future);
		
		//put consolidatedFuture after target node, future submission node
		//first find the node right after the submission point
		consolidatedFuture.setStatus(Status.iCommitted);
//		this.graphLock.writeLock();
		graph.addVertex(consolidatedFuture);
		graph.addEdge(consolidatedFuture.getFutureSubmittingNode(),consolidatedFuture);
		Node future_conti = consolidatedFuture.getFutureConti();
		graph.addEdge(consolidatedFuture, future_conti);
		graph.deleteEdge(consolidatedFuture.futureSubmittingNode, future_conti);
//		this.graphLock.writeUnlock();
	}

	private Node consolidatedFuture(Node future_committing) {
		Node f_consolidated = new Node();
		//navigating backwards in the DAG from the committing node up to the spawning node(not included)
		Set<Node> subTxns = getAllSubTxnofFuture(future_committing);
		for(Node node: subTxns){
		//	f_consolidated.readList.putAll(node.readList);
		//	f_consolidated.writeList.putAll(node.writeList);
		}
		f_consolidated.setType(Type.begin_future);		
		Node the_first_future = findFirstFutureNode(future_committing);
		f_consolidated.setFutureSubmittingNode(the_first_future.futureSubmittingNode);
		f_consolidated.setFutureConti(the_first_future.getFutureConti());
		f_consolidated.setFutureEval(future_committing.getFutureEval());
		f_consolidated.setLastConti(future_committing.getLastConti());
		f_consolidated.setAsscociatedTxn(future_committing.getAsscociatedTxn());
		future_committing.getAsscociatedTxn().setAssociatedNode(f_consolidated);
		f_consolidated.setStatus(future_committing.getStatus());
		f_consolidated.setResidingDAG(future_committing.getResidingDAG());
		
		this.graphLock.writeLock();
			for(Node node: subTxns){
				graph.deleteVertex(node);
			}
		this.graphLock.writeUnlock();
		
		return f_consolidated;
	}

	private Node findFirstFutureNode(Node future_committing) {
		if(future_committing.type == Type.begin_future){
			return future_committing;
		}else{
			Node upstreamNode = graph.upstreamNode(future_committing);
			while(upstreamNode.type != Type.begin_future ){
				upstreamNode = graph.upstreamNode(upstreamNode);
			}
			return upstreamNode;
		}
	}

	private Set<Node> getAllSubTxnofFuture(Node future_committing) {
		Set<Node> subtxn_as_future = new HashSet<Node>();
		subtxn_as_future.add(future_committing);
		if(future_committing.type == Type.begin_future){
			return subtxn_as_future;
		}
		Node inbound_Txn = graph.upstreamNode(future_committing);
		while(inbound_Txn.type != Type.begin_future ){
			subtxn_as_future.add(inbound_Txn);
			// by construction, the inboundNeighbour only contains one node
			inbound_Txn = graph.upstreamNode(inbound_Txn);
		}
		return subtxn_as_future;
		
	}

	private void updateAndSerializeFutureAtEvaluation(Node future) {

		Node consolidatedFuture = consolidatedFuture(future);
		consolidatedFuture.setStatus(Status.iCommitted);
		//put consolidatedFuture before target node, future evaluation node
		
//		this.graphLock.writeLock();
		graph.addVertex(consolidatedFuture);
		graph.addEdge(consolidatedFuture.getLastConti(),consolidatedFuture);
		graph.addEdge(consolidatedFuture, consolidatedFuture.getFutureEval());
		graph.deleteEdge(consolidatedFuture.getLastConti(), consolidatedFuture.getFutureEval());
//		this.graphLock.writeUnlock();		
	}
	
	/**
	 * speculate future at evaluation point and wait for re-execution
	 * **/
	private void speculateFutureAtEvaluation(Node future) {

		Node consolidatedFuture = consolidatedFuture(future);
		consolidatedFuture.setStatus(Status.waitToRestart);
		
		//put consolidatedFuture before target node, future evaluation node
				
//		this.graphLock.writeLock();
		graph.addVertex(consolidatedFuture);
		graph.addEdge(consolidatedFuture.getLastConti(),consolidatedFuture);
		graph.addEdge(consolidatedFuture, consolidatedFuture.getFutureEval());
		graph.deleteEdge(consolidatedFuture.getLastConti(), consolidatedFuture.getFutureEval());
//		this.graphLock.writeUnlock();		
	}

/***	private boolean backwardValidationAgainstContinuation(Node future) {
//		for(K read_key: future.readList.keySet()){
//			for(Node txn: getContinuation(future)){
//				if(txn.writeList.containsKey(read_key))
//					return false;
//			}
//		}
		return true;
//	}
 * 
 * @param future
 * @return whether backward validation succeed
 */
	
	private boolean backwardValidationAgainstContinuation(Node future) {
		for(Node txn: getContinuation(future)){
			for(VBox writeKey: ((WeakOrderingFutureSubTransaction)txn.getAsscociatedTxn()).getBoxesWritten().keySet()){
				if(((WeakOrderingFutureSubTransaction)future.getAsscociatedTxn()).getGlobalReads().contains(writeKey)
						|| ((WeakOrderingFutureSubTransaction)future.getAsscociatedTxn()).getNestedReads().containsKey(writeKey) )
				{
//					if(((WeakOrderingFutureSubTransaction)future.getAsscociatedTxn()).getGlobalReads().contains(writeKey)
//							)
//						System.out.println("conflict write key: "+ writeKey);
//					else if(((WeakOrderingFutureSubTransaction)future.getAsscociatedTxn()).getNestedReads().containsKey(writeKey) ){
//						System.out.println("nested reads size: "+ ((WeakOrderingFutureSubTransaction)future.getAsscociatedTxn()).getNestedReads().size());
//						System.out.println("nested reads conflict write key: "+ writeKey);
//					}
					
					return false;			
				}
			}
		}
		return true;
	}


	/**
	 *  returns the sub-transactions that compose the continuation of a given future F
 	 *	navigate from evaluation node associated with the future in the graph backwards till we find the spawning node 	
	 *	this is guaranteed to be found by construction
   	 *  the set of nodes over which we navigate may (or may not) include nested futures 
	 *  that were spawned within that continuation. In particular, the set of nodes will not 
	 *  include uncommitted (nested)future, but can include committed (nested)future
	 * 
	 * */
	private Set<Node> getContinuation(Node future) {

		Set<Node> subtxn_as_continuation = new HashSet<Node>();
		Node parent = graph.inboundNeighbours(future).get(0); // by construction, the future has only one inbound neighbour, which is its parent, i.e. spawning node
		
		Node first_conti = graph.getSubsequentContinuation(parent);		
		subtxn_as_continuation.add(first_conti);
		
		Node future_eval = future.getFutureEval();
		Node conti_element = graph.getSubsequentContinuation(first_conti);
		
		while(conti_element!=future_eval){
			subtxn_as_continuation.add(conti_element);
//			System.out.println("current graph structure");
//			System.out.println(future.getResidingDAG().graph);
//			System.out.println("conti_element: "+ conti_element);
//			System.out.println("future_eval: "+ future_eval);
			
			conti_element = graph.getSubsequentContinuation(conti_element);
		}
		
		return subtxn_as_continuation;	
	
	}
	

/**	private boolean forwardValidation(Node future) {
//		for(K write_key: future.writeList.keySet()){
//			for(Node txn: getForwardTxn(future)){
//				if(txn.readList.containsKey(write_key))
//					return false;
//			}
//		}
//		return true;
//	}
*/	
	private boolean forwardValidation(Node future) {
//		if(future!=null)
//			return true;
		TimerDebug.startForwardValidation((int) (Thread.currentThread().getId()%56));
		WeakOrderingFutureSubTransaction txn_future = (WeakOrderingFutureSubTransaction) future.getAsscociatedTxn();
		Map<VBox, Object> writeSet = txn_future.getBoxesWritten();
		//	System.out.println("future write set size: "+ writeSet.size());
		List<Node> forwardTxns = getForwardTxn(future);
		for(Node txn: forwardTxns){
			for(VBox write_key: writeSet.keySet()){
				try{
					//			System.out.println("DEBUG*****");
					//				System.out.println("txn node: "+ txn);
					//				System.out.println("asscociated txn: "+txn.getAsscociatedTxn());
					//				System.out.println("global read size"+((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()).getGlobalReads().size());
					//					System.out.println("nested read size"+((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()).getNestedReads().size());
					//				if (txn.getAsscociatedTxn() == null){
					//					continue;
					//				}
					if(txn.getStatus() != Status.iCommitted){
						continue;
					}
					WeakOrderingFutureSubTransaction sub_txn = (WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn();

					if(
							sub_txn.getGlobalReads().
							contains(write_key)
							|| sub_txn.getNestedReads().containsKey(write_key))
						return false;
				}catch(NullPointerException e){
					//					e.printStackTrace();
					//					System.err.println("DAG status: "+ this.dagStatus );
					//					System.err.println("((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()): "+ ((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()));
					//					System.err.println("((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()).getGlobalReads(): "+ ((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()).getGlobalReads());
					//					System.err.println("((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()).getNestedReads(): "+((WeakOrderingFutureSubTransaction) txn.getAsscociatedTxn()).getNestedReads());
				}
			}
		}
		TimerDebug.endForwardValidation((int) (Thread.currentThread().getId()%56));
		return true;
	}

	public Node futureEvaluationCreateEvalNode(Node futureNode, Node contiNode) {
		
		Node evalNode = new Node();
		evalNode.setType(Type.evaluation);
		evalNode.setStatus(Status.active);		
		return evalNode;
		
	}
	
	public void futureEvaluationSetEvalNode(Node futureNode, Node contiNode, Node evalNode){

//		System.out.println("prepare to get write lock: "+Transaction.current().getTopLevelTrasanction());
		this.graphLock.writeLock();
//		System.out.println("got write lock by: "+ Transaction.current().getTopLevelTrasanction());
		//node the future node may already been serialized at submission or evaluation
		graph.addEdge(contiNode, evalNode);
		//mark the future as completed , and this allows F to serialize upon evaluation
		if(!futureNode.getStatus().equals(Status.iCommitted)) {
			futureNode.setStatus(Status.evaluated);
		} 
		futureNode.latchOnEvaluatedStatus.countDown();
	//	System.out.println("SET future node status to evaluated ");
		futureNode.setLastConti(contiNode);
		futureNode.setFutureEval(evalNode);
		//conclude the continuation node
		iCommitNode(contiNode);
		this.graphLock.writeUnlock();
	}

	public void concludeLastEval(Node evalNode) {
		this.graphLock.writeLock();
		iCommitNode(evalNode);
		this.graphLock.writeUnlock();
		
	}

}
