package barn1474.graph;

import java.util.Stack;


/**
 * A solution is a series of vertices
 * @author barnett
 *
 * @param <V>
 */
public class GraphSolution <V extends GraphVertex<?>> {

	Stack<GraphVertex<?>> solutionPath;
	
	public GraphSolution(){
		solutionPath = new Stack<GraphVertex<?>>();
	}
	
	/**
	 * Pushes the next parent onto the solution stack. The last node pushed should be the start node.
	 * @param parent
	 */
	public void addNextParent(GraphVertex<?> parent){
		solutionPath.push(parent);
	}
	
	/**
	 * Return first node in solution path and remove it from solution
	 * @return
	 */
	public GraphVertex<?> getNextNode(){
		return solutionPath.pop();
	}
	
	/**
	 * Just tell whether or not the queue of solution notes is empty
	 * @return
	 */
	public boolean isEmpty(){
		return solutionPath.isEmpty();
	}
	
}
