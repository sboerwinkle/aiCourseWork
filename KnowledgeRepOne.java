package barn1474;

import java.util.HashSet;
import java.util.UUID;

import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Vector2D;
import barn1474.russell.ShipStateEnum;

class KnowledgeRepOne {
	
	static final int NEAR_BEACON_RADIUS = 50;
	static final int NEAR_ASTEROID_RADIUS = 50;
	static final int LOW_ENERGY = 1000;
	
	/**
	 * A state that tells us what we are doing now
	 */
	ShipStateEnum state;
	
	/**
	 * This will either be a mineable asteroid, or the base.
	 * This is a UUID, so it's persistent.
	 */
	UUID objectiveID = null;
	/**
	 * The object corresponding to objectiveID.
	 * This is an object, so we actually do our calculations with this.
	 */
	AbstractObject objective = null;
	
	/**
	 * Holds the solution from navigational searches
	 */
	Path path = null;
	
	
	/**
	 * Things to draw on the screen
	 * @return set of Spacewar Graphics
	 */
	HashSet<SpacewarGraphics> getGraphics() {
		if (path == null) return new HashSet<SpacewarGraphics>();
		return path.getGraphics();
	}

	/**
	 * Counter for when to do replans
	 */
	int timeTilAStar = 0;

	/**
	 * Gives the thrust vector to head towards the target.
	 * Assumes we can accelerate instantly.
	 */
	Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
		if (path == null) return new Vector2D(0, 0);
		return path.getThrust(space, me);
	}

	/**
	 * Finds the home base corresponding to my ship
	 * @return The home base for our team, or null if none is found
	 */
	Base getNearestBase(Toroidal2DPhysics space, Ship me) {
		double nearestDist = Double.MAX_VALUE;
		Base nearestBase = null;
		for (Base b : space.getBases()) {
			if (!b.getTeamName().equals(me.getTeamName())) continue;
			double dist = space.findShortestDistance(b.getPosition(), me.getPosition());
			if (dist >= nearestDist) continue;
			nearestBase = b;
			nearestDist = dist;
		}
		return nearestBase;
	}

	/**
	 * Finds the mineable asteroid closest to the ship
	 * @param space
	 * @param me
	 * @return The mineable asteroid nearest to my ship, or null if there are none
	 */
	Asteroid getNearestAsteroid(Toroidal2DPhysics space, Ship me) {
		Asteroid besteroid = null;
		double bestDist = Double.MAX_VALUE;
		for (Asteroid a : space.getAsteroids()) {
			//It must be mineable
			if (!a.isMineable()) continue;
			//and close to me
			double dist = space.findShortestDistance(a.getPosition(), me.getPosition());
			if (dist > bestDist) continue;

			besteroid = a;
			bestDist = dist;
		}
		return besteroid;
	}
	
	/**
	 * Find the nearest beacon
	 * @param space
	 * @param me
	 * @return The beacon nearest to my ship, or null if there are none
	 */
	Beacon getNearestBeacon(Toroidal2DPhysics space, Ship me) {
		double bestDist = Double.MAX_VALUE;
		Beacon bestbeacon = null;
		for (Beacon b : space.getBeacons()){
			double dist = space.findShortestDistance(b.getPosition(), me.getPosition());
			if (dist > bestDist) continue;
			
			bestbeacon = b;
			bestDist = dist;
		}
		return bestbeacon;
	}
	
	/**
	 * Find the farthest beacon
	 * @param space
	 * @param me
	 * @return The beacon nearest to my ship, or null if there are none
	 */
	Beacon getFarthestBeacon(Toroidal2DPhysics space, Ship me) {
		double bestDist = Double.MIN_VALUE;
		Beacon bestbeacon = null;
		for (Beacon b : space.getBeacons()){
			double dist = space.findShortestDistance(b.getPosition(), me.getPosition());
			if (dist < bestDist) continue;
			
			bestbeacon = b;
			bestDist = dist;
		}
		return bestbeacon;
	}
	
	/**
	 * Check if there is a beacon within a specified radius
	 * @param space
	 * @param ship
	 * @return true if there is at least one beacon in our radius, false otherwise
	 */
	boolean isBeaconNear(Toroidal2DPhysics space, Ship me) {
		for (Beacon b : space.getBeacons()){
			if (space.findShortestDistance(b.getPosition(), me.getPosition()) < NEAR_BEACON_RADIUS) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if there are resources within a specified radius
	 * @param space
	 * @param ship
	 * @return true if there is at least one beacon in our radius, false otherwise
	 */
	boolean isResourceNear(Toroidal2DPhysics space, Ship me) {
		for (Asteroid a : space.getAsteroids()){
			if (a.isMineable() &&
					space.findShortestDistance(a.getPosition(), me.getPosition()) < NEAR_ASTEROID_RADIUS) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Find the closest way to refuel, be it a base or an energy beacon, assuming the base has enough energy in it
	 * @param space
	 * @param me
	 * @return the base or beacon that is closest
	 */
	AbstractObject myNearestRefuel(Toroidal2DPhysics space, Ship me) {
		Beacon closestBeacon = getNearestBeacon(space, me);
		Base home = getNearestBase(space, me); 
		if (home.getEnergy() > LOW_ENERGY
				&& space.findShortestDistance(home.getPosition(), me.getPosition()) < space.findShortestDistance(closestBeacon.getPosition(), me.getPosition())) {
			return home;
		}
		else {
			return closestBeacon;
		}
	}
	
	/**
	 * Detect if we are running low on energy
	 * @param myShip
	 * @return true if ship energy is less than threshold
	 */
	boolean isEnergyLow(Ship me){
		return (me.getEnergy() < LOW_ENERGY);
	}

	// Getters and setters
	public ShipStateEnum getState() {
		return state;
	}
	
	public void setState(ShipStateEnum state) {
		this.state = state;
	}
	
	public UUID getObjectiveID() {
		return objectiveID;
	}
	
	public void setObjectiveID(UUID objectiveID) {
		this.objectiveID = objectiveID;
	}
	
	public AbstractObject getObjective() {
		return objective;
	}
	
	public void setObjective(AbstractObject objective) {
		this.objective = objective;
	}
	
	public Path getPath() {
		return path;
	}
	
	public void setPath(Path path) {
		this.path = path;
	}
	
	public int getTimeTilAStar() {
		return timeTilAStar;
	}
	
	public void setTimeTilAStar(int timeTilAStar) {
		this.timeTilAStar = timeTilAStar;
	}
}