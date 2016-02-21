package barn1474;

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
		AbstractObject nextObjective = finalObjective;
		
		//add the endpoint of our search so it will be the end of the list
		objectsList.addFirst(finalObjective);
		
		//keep adding new objects, using greedy best search until we are done
		while(true){
			nextObjective = findNextObject(space, me, nextObjective);
			//check when to stop search
			if (nextObjective == null
					//don't go for objects outside of the radius from me to final objective
					|| space.findShortestDistance(me.getPosition(), nextObjective.getPosition()) > space.findShortestDistance(me.getPosition(), finalObjective.getPosition())){
				break;
			}
			objectsList.addFirst(nextObjective);
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
		double bestDist = Double.MAX_VALUE;
		double dist;
		AbstractObject bestObject = null;
		
		//we are looking for beacons and mineable asteroids
		for (Beacon beacon : space.getBeacons()){
			dist = sumOfDistances(space, me, beacon, finalObjective); 
			if (dist > bestDist) continue;
			bestDist = dist;
			bestObject = beacon;
		}
		for (Asteroid asteroid : space.getAsteroids()){
			if (!asteroid.isMineable()) continue;
			dist = sumOfDistances(space, me, asteroid, finalObjective);
			if (dist > bestDist) continue;
			bestDist = dist;
			bestObject = asteroid;
			
			
		}
		return bestObject;
	}
	
	/**
	 * Calculating the sum of distances from a start object through a middle object to the end.
	 * Will be used as the heuristic for a greedy best search.
	 * @param space
	 * @param start
	 * @param middle
	 * @param end
	 * @return the shortest distance from start to middle to end
	 */
	public static double sumOfDistances(Toroidal2DPhysics space, AbstractObject start, AbstractObject middle, AbstractObject end){
		return space.findShortestDistance(start.getPosition(), middle.getPosition()) + space.findShortestDistance(middle.getPosition(), end.getPosition());
	}
}
