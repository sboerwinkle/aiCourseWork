package barn1474;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class KnowledgeRepTwo {
	static HashSet <Ship> myShips; 			//Store the list of our ships so we know who our friends are directly
	static String myTeamName;				//Store team name as a string for access by static methods
	
	private static final int NEAR_BASE_RADIUS = 500;		//radius that determines what is near
	private static final int NEAR_SHIP_RADIUS = 500;		
	private static final int NEAR_ASTR_RADIUS = 500;
	private static final int NEAR_BEACON_RADIUS = 500;
	private static final double FRONT_NEAR_ANGLE = Math.PI;	//the forward vision sweep angle
	
	
	//make these hashmaps for when we have multiple ships on our team
	static HashMap <UUID,Base> nearBase;
	static HashMap <UUID, Ship> nearEnemy;
	static HashMap <UUID, Asteroid> mineableAsteroid;
	static HashMap <UUID, Beacon> nearBeacon;
	
	/**
	 * Checks if the distance to any base is less than a specified 'near' radius
	 * @param space
	 * @param ship
	 * @return true if any base belonging to the ship's team is within radius of our ship, false otherwise
	 */
	public static boolean isHomeBaseNear(Toroidal2DPhysics space, Ship ship){
		for (Base base :  space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(myTeamName) && 
					space.findShortestDistance(base.getPosition(), ship.getPosition()) < NEAR_BASE_RADIUS){
				nearBase.put(ship.getId(), base);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if there is a beacon within a specified radius
	 * @param space
	 * @param ship
	 * @return true if there is at least one beacon in our radius, false otherwise
	 */
	public static boolean isBeaconNear(Toroidal2DPhysics space, Ship ship) {
		for (Beacon beacon : space.getBeacons()){
			if (space.findShortestDistance(beacon.getPosition(), ship.getPosition()) < NEAR_BEACON_RADIUS) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if the distance to any ship is less than a specified 'near' radius
	 * @param space
	 * @param myShip
	 * @return true if an enemy ship is within a radius of our ship, false otherwise
	 */
	public static boolean isEnemyNear(Toroidal2DPhysics space, Ship myShip) {
		for (Ship ship :  space.getShips()) {
			if (myShips.contains(ship) && space.findShortestDistance(ship.getPosition(), myShip.getPosition()) < NEAR_SHIP_RADIUS){
				nearEnemy.put(myShip.getId(), ship);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * If there is an asteroid within +- FRONT_NEAR_ANGLE in the direction of movement it is considered to be ahead
	 * @param space
	 * @param myShip
	 * @return
	 */
	public static boolean isMineableAsteroidAhead(Toroidal2DPhysics space, Ship myShip) {
		
		for (Asteroid asteroid : space.getAsteroids()){
			if (asteroid.isMineable()
					&& space.findShortestDistanceVector(myShip.getPosition(), asteroid.getPosition()).angleBetween(myShip.getPosition().getTranslationalVelocity()) < FRONT_NEAR_ANGLE
					&& space.findShortestDistance(myShip.getPosition(),  asteroid.getPosition()) < NEAR_ASTR_RADIUS){
				mineableAsteroid.put(myShip.getId(), asteroid);
				return true;
			}
		}
		
		
		return false;
	}
	
	
}
