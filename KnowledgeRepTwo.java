package barn1474;

import java.util.HashMap;
import java.util.UUID;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class KnowledgeRepTwo {
	
	public enum shipState{
		GETTING_GAS,
		GOING_HOME,
		GETTING_RESOURCES
	}
	
	static HashMap <UUID, shipState> myShips; 	//Store the list of our ships and their states
	static String myTeamName;					//Store team name as a string for access by static methods
	
	private static final int LOW_FUEL = 1000;	
	private static final int NEAR_SHIP_RADIUS = 300;  //radius that determines what is near		
	private static final int NEAR_ASTR_RADIUS = 400;
	private static final int NEAR_BEACON_RADIUS = 50;
	private static final double FRONT_NEAR_ANGLE = Math.PI;	//the forward vision sweep angle
	private static final double VELOCITY_DISTANCE_FACTOR = 1.0;
	
	
	//make these hashmaps for when we have multiple ships on our team
	//these store the objects seen by each ship
	static HashMap <UUID, Base> nearBase;
	static HashMap <UUID, Ship> nearEnemy;
	static HashMap <UUID, Asteroid> mineableAsteroid;
	static HashMap <UUID, Beacon> nearBeacon;
	
	/**
	 * Initialize maps needed for knowledge representation
	 */
	public static void initializeKnowledge(){
		nearBase = new HashMap <UUID, Base>();
		nearEnemy = new HashMap <UUID, Ship>();
		mineableAsteroid = new HashMap <UUID, Asteroid>();
		nearBeacon = new HashMap <UUID, Beacon>();
	}
	
	/**
	 * Finds my base nearest to me
	 * @param space
	 * @param ship
	 * @return the base object
	 */
	public static Base myNearestBase(Toroidal2DPhysics space, Ship ship) throws NoObjectReturnedException{
		nearBase.put(ship.getId(), null);
		for (Base base :  space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(myTeamName) &&
					(nearBase.get(ship.getId()) == null ||
					space.findShortestDistance(base.getPosition(), ship.getPosition()) < space.findShortestDistance(nearBase.get(ship.getId()).getPosition(),ship.getPosition()))){
				nearBase.put(ship.getId(), base);
			}
		}
		if (nearBase.get(ship.getId()) == null){
			throw new NoObjectReturnedException();
		}
		return nearBase.get(ship.getId());
	}
	
	/**
	 * Find the closest way to refuel, be it a base or an energy beacon, assuming the base has enough energy in it
	 * @param space
	 * @param ship
	 * @return the base or beacon that is closest
	 */
	public static AbstractObject myNearestRefuel(Toroidal2DPhysics space, Ship ship) throws NoObjectReturnedException{
		Beacon closestBeacon = null;
		for (Beacon beacon :  space.getBeacons()) {
			if ((closestBeacon == null ||
					space.findShortestDistance(beacon.getPosition(), ship.getPosition()) < space.findShortestDistance(closestBeacon.getPosition(),ship.getPosition()))){
				closestBeacon = beacon;
			}
		}
		Base home = myNearestBase(space,ship); 
		if (home.getEnergy() > LOW_FUEL 
				&& space.findShortestDistance(home.getPosition(), ship.getPosition()) < space.findShortestDistance(closestBeacon.getPosition(), ship.getPosition())) {
			return home;
		}
		else {
			if (closestBeacon == null) {throw new NoObjectReturnedException();}
			return closestBeacon;
		}
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
				nearBeacon.put(ship.getId(), beacon);
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
			if (!myShips.containsKey(ship.getId()) && space.findShortestDistance(ship.getPosition(), myShip.getPosition()) < NEAR_SHIP_RADIUS){
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
	
	/**
	 * Find the mineable asteroid that is nearest base
	 * @param space
	 * @param ship
	 * @param base
	 * @return an asteroid
	 */
	public static Asteroid asteroidNearestBase(Toroidal2DPhysics space, Ship ship, Base base) throws NoObjectReturnedException {
		mineableAsteroid.put(ship.getId(), null);
		for (Asteroid asteroid :  space.getAsteroids()) {
			if (asteroid.isMineable() &&
					(mineableAsteroid.get(ship.getId()) == null ||
					space.findShortestDistance(asteroid.getPosition(), base.getPosition()) < space.findShortestDistance(mineableAsteroid.get(ship.getId()).getPosition(),base.getPosition()))){
				mineableAsteroid.put(ship.getId(), asteroid);
			}
		}
		if (mineableAsteroid.get(ship.getId()) == null){
			throw new NoObjectReturnedException();
		}
		return mineableAsteroid.get(ship.getId());
	}
	
	/**
	 * Detect if we are running low on energy
	 * @param myShip
	 * @return true if ship energy is less than threshold
	 */
	public static boolean isOutOfGas(Ship myShip){
		return (myShip.getEnergy() < LOW_FUEL);
	}
	
	/**
	 * Calculate a spot to aim for that is somewhere in front of the object if it is moving.
	 * @param object
	 * @return a Position where the object should be soon
	 */
	public static Position getObjectIntercept(AbstractObject object){
		double interceptX = object.getPosition().getX() + object.getPosition().getTranslationalVelocityX() * VELOCITY_DISTANCE_FACTOR;
		double interceptY = object.getPosition().getY() + object.getPosition().getTranslationalVelocityY() * VELOCITY_DISTANCE_FACTOR;
		return new Position(interceptX, interceptY);
	}
	
}
