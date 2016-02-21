package barn1474.graph;

import java.util.List;

/**
 * Class to implement an undirected, weighted graph
 * @author barnett
 *
 * @param <V>
 */
public abstract class Graph <V extends GraphVertex<?>> {

	public Graph(){
		
	}
	
	public abstract void addEdge(GraphVertex<?> V1, GraphVertex<?> V2, int weight);
	
	public abstract boolean hasEdge(GraphVertex<?> V1, GraphVertex<?> V2);
	public abstract int getEdgeWeight(GraphVertex<?> V1, GraphVertex<?> V2);
		
	public abstract boolean hasVertex(GraphVertex<?> Vertex);
	public abstract List<GraphVertex<?>> getNeighbors(GraphVertex<?> Vertex);
	
	

}
