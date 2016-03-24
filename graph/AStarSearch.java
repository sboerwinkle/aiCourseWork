package barn1474.graph;

import java.util.HashSet;
import java.util.PriorityQueue;

import barn1474.bbbExceptions.NoObjectReturnedException;

public class AStarSearch <G extends Graph<? extends GraphVertex<? extends Object>>> {

	GraphVertex<?> start;
	GraphVertex<?> goal;
	HashSet<GraphVertex<?>> explored;
	PriorityQueue<GraphVertex<?>> frontier;
	G graph;
	GraphSolution<?> solution;
	
	public AStarSearch(G graph, GraphVertex<?> start, GraphVertex<?> goal) throws NoObjectReturnedException{
		this.start = start;
		this.goal = goal;
		this.graph = graph;
		explored = new HashSet<GraphVertex<?>>();
		frontier = new PriorityQueue<GraphVertex<?>>();
		solution = new GraphSolution<GraphVertex<?>>();
		
	}
	
	public GraphSolution<?> doGraphSearch() throws NoObjectReturnedException{
		start.setPathCost(0);
		start.setParent(null);
		GraphVertex<?> node = start;
		frontier.add(node);
		
		while (true){
			if (frontier.isEmpty()) throw new NoObjectReturnedException();
			node = frontier.poll();
			if (node == goal) {
				buildSolution(goal); //need to build solution
				return solution;
			}
			
			explored.add(node);
			
			double newCost;
			//iterate through children of node
			for (GraphVertex<?> child : graph.getNeighbors(node)){
				newCost = graph.getEdgeWeight(node, child);
				if (!explored.contains(child) && !frontier.contains(child)){
					child.setParent(node);
					child.setPathCost(node.getPathCost() + newCost);
					frontier.add(child);
				}
				else if (frontier.contains(child) && child.getPathCost() > node.getPathCost() + newCost) {
					child.setParent(node);
					child.setPathCost(node.getPathCost() + newCost);
					
				}
				
			}
			
		}
		
		
	}
	
	/**
	 * Add vertices to the solution stack, starting with goal and ending with start
	 * @param lastNode
	 */
	private void buildSolution(GraphVertex<?> lastNode){
		solution.addNextParent(lastNode);
		while(lastNode.getParent() != null){ //parent of start node is null
			lastNode = lastNode.getParent();
			solution.addNextParent(lastNode);
		}
		
	}
	
	
}
