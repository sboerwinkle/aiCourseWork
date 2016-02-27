package barn1474;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

/**
 * This class will find a list of desirable objects that are
 * on the way to a main objective.
 * @author barnett
 *
 */
public class NavigationObjects {
	
	private static final int MAX_DEPTH = 3;
	
	//we are going to need a persistent list of objects so we can subtract from it as we go
	static HashMap<AbstractObject,Double> candidateObjects;

	/**
	 * Makes the high level decision of which objects to visit. Uses a greedy best first search
	 * based on the sum of the distances between the ship, the object, and the master objective.
	 * @param space
	 * @param me
	 * @param finalObjective
	 * @return LinkedList of objects to visit, in order
	 */
	public static LinkedList<AbstractObject> getObjectsToVisit(Toroidal2DPhysics space, Ship me, AbstractObject finalObjective){

		LinkedList<AbstractObject> objectsList = new LinkedList<AbstractObject>();
		candidateObjects = new HashMap<AbstractObject,Double>();
		AbstractObject nextObjective = finalObjective;
		
		//set up the objects to search through
		for (Beacon beacon : space.getBeacons()){
			candidateObjects.put(beacon, heuristicFunction(space, me, beacon, finalObjective));
		}
		for (Asteroid asteroid : space.getAsteroids()){
			if (!asteroid.isMineable()) continue;
			candidateObjects.put(asteroid, heuristicFunction(space, me, asteroid, finalObjective));
		}
		
		//add the endpoint of our search so it will be the end of the list
		objectsList.addFirst(finalObjective);
		
		if (candidateObjects.containsKey(finalObjective)) candidateObjects.remove(finalObjective);
		
		//keep adding new objects to list, using greedy best search until we are done
		for (int i = MAX_DEPTH; i > 0; i--){
			nextObjective = findNextObject(space, me, nextObjective);
			//check when to stop search
			if (nextObjective == null
					//don't go for objects outside of the radius from final object to me
					|| space.findShortestDistance(finalObjective.getPosition(), nextObjective.getPosition()) > space.findShortestDistance(me.getPosition(), finalObjective.getPosition())){
				break;
			}
			objectsList.addFirst(nextObjective);
			candidateObjects.remove(nextObjective);
		}
		
		return objectsList;
		
	}
	
	/**
	 * Finds the next desirable object with the best heuristic
	 * @param space
	 * @param me
	 * @param finalObjective
	 * @return abstract object that is a desirable object to visit
	 */
	private static AbstractObject findNextObject(Toroidal2DPhysics space, Ship me, AbstractObject finalObjective){
		double bestH = Double.MAX_VALUE;
		double h;
		AbstractObject bestObject = null;
		
		//loop through candidate objects and get the one with the best heuristic value
		for (AbstractObject obj : candidateObjects.keySet()){
			h = candidateObjects.get(obj); 
			if (h > bestH) continue;
			bestH = h;
			bestObject = obj;
		}

		return bestObject;
	}
	
	/**
	 * Simple equivalence test for spacewar objects using UUID
	 * @param a
	 * @param b
	 * @return
	 */
	static boolean sameThing(AbstractObject a, AbstractObject b) {
		return a.getId().equals(b.getId());
	}
	
	/**
	 * Calculating the sum of distances from a start object through a middle object to the end.
	 * Will be used as the heuristic for a greedy best search.
	 * @param space
	 * @param start
	 * @param middle
	 * @param end
	 * @return h value
	 */
	public static double heuristicFunction(Toroidal2DPhysics space, AbstractObject start, AbstractObject middle, AbstractObject end){
		//heuristic function based on shortest detour
		return space.findShortestDistance(start.getPosition(), middle.getPosition()) + space.findShortestDistance(middle.getPosition(), end.getPosition());
	}
	
}
