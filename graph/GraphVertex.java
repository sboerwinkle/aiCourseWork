package barn1474.graph;


/**
 * Represents one vertex on a graph that will be used for A star
 * @author barnett
 *
 * @param <Type>
 */
public class GraphVertex <Type extends Object> implements Comparable<GraphVertex<?>>{
	private Type object;
	private double fValue;
	private double hValue;
	private double pathCost;
	private GraphVertex<?> parent;
	
	public GraphVertex(Type object, double hValue){
		this.object = object;	
		this.hValue = hValue;
		this.parent = null;
		this.pathCost = 0;
		this.fValue = pathCost + hValue;
	}
	
	public Type getData(){
		return this.object;
	}

	
	public double getPathCost(){
		return pathCost;
	}
	
	public double getValue(){
		return fValue;
	}
	
	public void setPathCost(double cost){
		this.pathCost = cost;
		this.fValue = hValue + pathCost;
	}

	public GraphVertex<?> getParent(){
		return parent;
	}
	
	public void setParent(GraphVertex<?> P){
		parent = P;
	}

	@Override
	public int compareTo(GraphVertex<?> other) {
		if (getValue() > other.getValue()) return 1;
		if (getValue() < other.getValue()) return -1;
		return 0;
	}
}
