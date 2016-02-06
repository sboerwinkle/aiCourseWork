package barn1474;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.actions.RawAction;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
/**
 * A team of random agents
 * 
 * The agents pick a random location in space and aim for it.  They shoot somewhat randomly also.
 * @author 
 *
 */
public class myTeamClient extends TeamClient {
	HashSet<SpacewarGraphics> graphics;
	Random random;
	boolean fired = false;

	public static int RANDOM_MOVE_RADIUS = 200;
	public static double SHOOT_PROBABILITY = 0.1;
	
	
	private static HashSet <Ship> myShips; 			//Store the list of our ships so we know who our friends are directly
	private static String myTeamName;				//Store team name as a string for access by static methods
	private static final int NEAR_BASE_RADIUS = 500;		//radius that determines what is near
	private static final int NEAR_SHIP_RADIUS = 500;		
	private static final int NEAR_ASTR_RADIUS = 500;
	private static final int NEAR_BEACON_RADIUS = 500;
	private static final double FRONT_NEAR_ANGLE = Math.PI;	//the forward vision sweep angle
	private static final int LOW_FUEL = 1000;
	
	
	private static class KnowledgeRepTwo {
		
		//make these hashmaps for when we have multiple ships on our team
		private static HashMap <UUID,Base> nearBase;
		private static HashMap <UUID, Ship> nearEnemy;
		private static HashMap <UUID, Asteroid> mineableAsteroid;
		private static HashMap <UUID, Beacon> nearBeacon;
		
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
	
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		graphics = new HashSet<SpacewarGraphics>();
		random = new Random();
		
		KnowledgeRepTwo.nearBase = new HashMap <UUID, Base>();
		KnowledgeRepTwo.nearEnemy = new HashMap <UUID, Ship>();
		KnowledgeRepTwo.mineableAsteroid = new HashMap <UUID, Asteroid>();
		KnowledgeRepTwo.nearBeacon = new HashMap <UUID, Beacon>();
		
		//Store team name into a static string
		myTeamName = getTeamName();
		
		//make a local list of our ships, to verify identities
		for (Ship s : space.getShips()){
			if (s.getTeamName().equalsIgnoreCase(myTeamName)){
				myShips.add(s);
			}
		}
	
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub
	}


	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> myActions = new HashMap<UUID, AbstractAction>();
		
		
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				AbstractAction current = ship.getCurrentAction();
				
				
				//SpacewarGraphics newgraphic = new CircleGraphics(NEAR_BASE_RADIUS, getTeamColor(), ship.getPosition());
				//graphics.add(newgraphic);
				
				// if we finished, make a new spot in space to aim for
				if (current == null || current.isMovementFinished(space)) {
					
					//let's get going!
					if (ship.getPosition().getTotalTranslationalVelocity() < 10){
						myActions.put(ship.getId(), new RawAction(10, 0));
					}
					//if we are out of gas we need to get more energy...
					else if (ship.getEnergy() < LOW_FUEL) {
						if (KnowledgeRepTwo.isBeaconNear(space, ship)){
							myActions.put(ship.getId(), new MoveToObjectAction(space, ship.getPosition(), KnowledgeRepTwo.nearBeacon.get(ship.getId())));
						}
						else if (KnowledgeRepTwo.isHomeBaseNear(space, ship)){
							myActions.put(ship.getId(), new MoveToObjectAction(space, ship.getPosition(), KnowledgeRepTwo.nearBase.get(ship.getId())));
						}
						else { 
							//conserve energy by doing nothing
							myActions.put(ship.getId(), new DoNothingAction());
						}
					}
					else if (ship.getResources().getTotal() > 0) { //head home with resources
						if (KnowledgeRepTwo.isHomeBaseNear(space, ship)) {
							myActions.put(ship.getId(),  new MoveToObjectAction(space, ship.getPosition(), KnowledgeRepTwo.nearBase.get(ship.getId())));
						}
						//do nothing
						else myActions.put(ship.getId(), new DoNothingAction());
					}
					//ship does not have resources and is not low on fuel
					else {
						if (KnowledgeRepTwo.isMineableAsteroidAhead(space, ship)) {
							myActions.put(ship.getId(), new MoveToObjectAction(space, ship.getPosition(), KnowledgeRepTwo.mineableAsteroid.get(ship.getId())));
						}
						else myActions.put(ship.getId(), new DoNothingAction());
					}
					
				} else {
					myActions.put(ship.getId(), new DoNothingAction());
				}
				
			} else {
				// it is a base
				myActions.put(actionable.getId(), new DoNothingAction());
		}
			

		}
	
		
		return myActions;
	
	}


	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> newGraphics = new HashSet<SpacewarGraphics>(graphics);  
		graphics.clear();
		return newGraphics;
	}


	@Override
	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>();

	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		return powerupMap;
	}


}
