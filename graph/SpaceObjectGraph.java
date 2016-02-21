package barn1474.graph;

import java.util.HashSet;
import java.util.List;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Beacon;
import spacesettlers.simulator.Toroidal2DPhysics;

public class SpaceObjectGraph extends Graph {

	SpaceObjectGraph(Toroidal2DPhysics space, AbstractObject startObject, AbstractObject goalObject){
		super();
		//need to get list of vertices
		HashSet<GraphVertex<AbstractObject>> vList = new HashSet<GraphVertex<AbstractObject>>();
		
		//add ship and goal
		vList.add(new GraphVertex<AbstractObject>(startObject, space.findShortestDistance(startObject.getPosition(), goalObject.getPosition())));
		vList.add(new GraphVertex<AbstractObject>(goalObject, 0));
		
		//add all beacons, though one of these will be the goal, hence we are using a hash set
		for (AbstractObject obj : space.getAllObjects()){
			if (obj instanceof Beacon){
				vList.add(new GraphVertex<AbstractObject>(obj, space.findShortestDistance(obj.getPosition(), goalObject.getPosition())));
				
			}
		}
		
	}
	@Override
	public void addEdge(GraphVertex V1, GraphVertex V2, int weight) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasEdge(GraphVertex V1, GraphVertex V2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getEdgeWeight(GraphVertex V1, GraphVertex V2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasVertex(GraphVertex Vertex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List getNeighbors(GraphVertex Vertex) {
		// TODO Auto-generated method stub
		return null;
	}

}
