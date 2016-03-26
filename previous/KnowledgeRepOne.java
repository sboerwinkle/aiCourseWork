package barn1474.previous;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Vector2D;
import barn1474.AStar;
import barn1474.NavigationObjects;
import barn1474.Path;
import barn1474.russell.ShipStateEnum;
import barn1474.russell.TextGraphics;

public class KnowledgeRepOne {
	
	static final int NEAR_BEACON_RADIUS = 50;
	static final int NEAR_ASTEROID_RADIUS = 50;
	static final int LOW_ENERGY = 2500;

	//Initial state for ships. In project 2 we are searching for beacons. 
	private static final ShipStateEnum INITIAL_STATE = ShipStateEnum.GATHERING_ENERGY;  
	
	//how many timesteps to set the A-star replan counter to
	private static final int ASTAR_INTERVAL = 10;
	
	//amount of gathered resources it takes for the ship to head home
	private static final int SHIP_FULL = 1000;
	
	//the number of timesteps at the end of the game that should be used for frantic
	//last minute gathering of resources
	private static final int END_OF_TIME = 1000;
	
	private static final double APPROACH_VELOCITY = 1.0;

	static HashMap<UUID, KnowledgeRepOne> knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();

	public static KnowledgeRepOne get(Ship ship) {
		return knowledgeMap.get(ship.getId());
	}

	public static Vector2D doStepGetThrust(Ship ship, Toroidal2DPhysics space) {
		//add any new ships to knowledge map
		if (!knowledgeMap.containsKey(ship.getId())) {
			knowledgeMap.put(ship.getId(), new KnowledgeRepOne());
			knowledgeMap.get(ship.getId()).setState(INITIAL_STATE);
		}
		KnowledgeRepOne data = knowledgeMap.get(ship.getId());
		
		//store the old state so we can force a replan if it changes
		ShipStateEnum oldState = data.getState();
		
		//first set the state of the ship accordingly
		if(space.getMaxTime() - space.getCurrentTimestep() < END_OF_TIME){
			data.setState(ShipStateEnum.GATHERING_RESOURCES);
		}
		else if(ship.getResources().getTotal() > SHIP_FULL) {
			data.setState(ShipStateEnum.DELIVERING_RESOURCES);
		}
		else {
			data.setState(ShipStateEnum.GATHERING_ENERGY);
		}
		
		
		if (oldState != data.getState()) {
			data.setObjective(null);
		}
		
		//update info in case path was nullified
		data.update(space);
		
		//for debugging
		//myGraphics.add(new TextGraphics("State: " + data.getState(), new Position(10.0, 10.0), Color.WHITE));
		//myGraphics.add(new TextGraphics("Mass: " + ship.getMass(), new Position(10.0, 20.0), Color.WHITE));
		//myGraphics.add(new TextGraphics("Resources: " + ship.getResources().getTotal(), new Position(10.0, 30.0), Color.WHITE));
		
		//do we need to replan any main objectives?
		if (!data.isObjectiveListValid()){
			
			switch(data.getState()) {
			case DELIVERING_RESOURCES:
				//choose the closest base for now
				Base myBase = data.getNearestBase(space, ship);
				//now select objects between here and there to visit
				data.setObjectiveList(NavigationObjects.getObjectsToVisit(space, ship, myBase));
				break;
			case GATHERING_ENERGY:
				//main case for this project
				//choose the closest beacon as the final destination.
				Beacon beacon = data.getNearestBeacon(space, ship);
				//now select objects between here and there to visit
				data.setObjectiveList(NavigationObjects.getObjectsToVisit(space, ship, beacon));
				break;
			case GATHERING_RESOURCES:
				//if we're in this mode it's because time is running short
				//just go back and forth between resources and home
				Asteroid asteroid = data.getNearestAsteroid(space, ship);
				Base mybase = data.getNearestBase(space, ship);
				LinkedList<AbstractObject> list = new LinkedList<AbstractObject>();
				if (asteroid != null){
					list.addFirst(mybase);
					list.addFirst(asteroid);
				}
				data.setObjectiveList(list);
				break;
			default:
				break;
			}

		
		}
		
		//do we need to set the next local objective?
		if (data.getObjective() == null || (data.getObjective() instanceof Base && ship.getResources().getTotal() == 0)) {
			data.setTimeTilAStar(0);
			data.setObjective(data.popNextObjective());
		}
		
		//the navigational A Star is done once every so many ticks
		if (data.getTimeTilAStar() == 0) {
			data.setTimeTilAStar(ASTAR_INTERVAL);
			data.setPath(AStar.doAStar(space, ship, data.getObjective(), ship));
		}
		data.decrementTimeTilAStar();

		//thrust gets the low level where to go next
		return data.getThrust(space, ship);
	}
	
	/**
	 * A state that tells us what we are doing now
	 */
	private ShipStateEnum state;
	
	/**
	 * This will either be a beacon (project2), or the base.
	 * This is a UUID, so it's persistent.
	 */
	private UUID objectiveID = null;
	/**
	 * The object corresponding to objectiveID.
	 * This is an object, so we actually do our calculations with this.
	 */
	private AbstractObject objective = null;
	
	/**
	 * Holds the solution from navigational searches for current objective
	 */
	private Path path = null;
	
	/**
	 * Holds higher level list of objectives
	 */
	private LinkedList<AbstractObject> objectiveList;
	
	/**
	 * Constructor
	 */
	public KnowledgeRepOne(){
		objectiveList = new LinkedList<AbstractObject>();
	}
	
	
	/**
	 * Things to draw on the screen
	 * @return set of Spacewar Graphics
	 */
	public HashSet<SpacewarGraphics> getGraphics() {
		if (path == null) return new HashSet<SpacewarGraphics>();
		return path.getGraphics();
	}

	/**
	 * Counter for when to do replanning
	 */
	private int timeTilAStar = 0;

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
	
	public AbstractObject getObjective() {
		return objective;
	}
	
	public void setObjective(AbstractObject objective) {
		this.objective = objective;
		objectiveID = (objective == null ? null : objective.getId());
	}
	
	/**
	 * Checks for nullified path in the navigation, and updates the pointers to the
	 * objects that form the intermediate goals
	 * @param space
	 */
	public void update(Toroidal2DPhysics space){
		//check for nulls in immediate path goal
		if (path != null && !path.isValid()) path = null;
		if (path == null) {
			setObjective(null);
		}
		else {
			setObjective(space.getObjectById(objectiveID));
		}
		
		if (objective != null && !objective.isAlive()) {
			setObjective(null);
		}
		
		//need to refresh the main objectives list
		LinkedList<AbstractObject> refreshList = new LinkedList<AbstractObject>();
		for (AbstractObject obj : objectiveList){
			refreshList.addLast(space.getObjectById(obj.getId()));
		}
		setObjectiveList(refreshList);
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
	
	/**
	 * Sets the countdown timer for A Star navigation replan
	 * @param timeTilAStar
	 */
	public void setTimeTilAStar(int timeTilAStar) {
		this.timeTilAStar = timeTilAStar;
	}
	
	public void decrementTimeTilAStar(){
		this.timeTilAStar--;
	}

	/**
	 * Add a single object to the top of the objectives list.
	 * @param objective
	 */
	public void pushObjective(AbstractObject objective){
		objectiveList.push(objective);
	}
	
	/**
	 * Pops the next objective off the objectives list
	 * @return the next objective
	 */
	public AbstractObject popNextObjective() {
		return objectiveList.pollFirst();
	}

	/**
	 * A way to check what the final objective is in the list
	 * @return the final objective
	 */
	public AbstractObject peekFinalObjective() {
		return objectiveList.getLast();
	}
	
	/**
	 * Checks the list of objects to visit to see if any of them is not alive
	 * @return should return false if list is empty, or if any object in the list is null or not alive 
	 */
	public boolean isObjectiveListValid() {
		if (objectiveList.isEmpty() && objective == null) return false; //even if the list is empty, the last objective could be stored as the immediate
		for (AbstractObject obj : objectiveList){
			if (obj == null || !obj.isAlive()) return false;
		}
		return true;
	}


	/**
	 * For importing a list of objectives that will be sequential goals for the navigation system
	 * @param objectiveList
	 */
	public void setObjectiveList(LinkedList<AbstractObject> objectiveList) {
		this.objectiveList.clear();
		this.objectiveList = objectiveList;
	}
	
	/**
	 * Helpful graphics for debugging navigation algorithms
	 * @param space
	 * @param me
	 * @return
	 */
	public HashSet<SpacewarGraphics> getNavGraphics(Toroidal2DPhysics space, Ship me) {
		HashSet<SpacewarGraphics> ret = new HashSet<SpacewarGraphics>();
		
		//draw from the ship to the immediate objective
		int i =0;
		TextGraphics text = new TextGraphics(i + "", objective.getPosition(), Color.WHITE);
		LineGraphics line = new LineGraphics(me.getPosition(), objective.getPosition(), space.findShortestDistanceVector(me.getPosition(), objective.getPosition()));
		line.setLineColor(Color.RED);
		ret.add(line);
		ret.add(text);

		AbstractObject prevObj = objective; // now start drawing from the immediate objective
		for (AbstractObject nextObj : objectiveList) {
			i++;
			text = new TextGraphics(i + "", nextObj.getPosition(), Color.WHITE);
			line = new LineGraphics(prevObj.getPosition(), nextObj.getPosition(), space.findShortestDistanceVector(prevObj.getPosition(), nextObj.getPosition()));
			line.setLineColor(Color.RED);
			ret.add(line);
			ret.add(text);
			prevObj = nextObj;
			
		}
		
		return ret;
	}
	
}