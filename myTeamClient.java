package barn1474;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.actions.RawAction;
import spacesettlers.clients.TeamClient;
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
import spacesettlers.utilities.Vector2D;
import barn1474.evolution.BbbIndividual;
import barn1474.evolution.BbbPopulation;
import barn1474.russell.ShipStateEnum;

/**
 * Cooperative agents grab resources and bring them home.
 *
 * @author Barnett and Boerwinkle
 *
 */
public class myTeamClient extends TeamClient {

	//Initial state for ships. In project 2 we are searching for beacons. 
	private static final ShipStateEnum INITIAL_STATE = ShipStateEnum.GATHERING_ENERGY;  
	
	//how many timesteps to set the A-star replan counter to
	private static final int ASTAR_INTERVAL = 10;
	
	//amount of gathered resources it takes for the ship to head home
	private static final int SHIP_FULL = 1500;
	
	//the number of timesteps at the end of the game that should be used for frantic
	//last minute gathering of resources
	private static final int END_OF_TIME = 1000;
	
	HashSet<SpacewarGraphics> myGraphics;
	private static final double APPROACH_VELOCITY = 1.0;

	/**
	 * Map of ship UUID to its knowledge representation
	 */
	HashMap<UUID, KnowledgeRepOne> knowledgeMap;

	/**
	 * Manages the population for genetic learning.
	 */
	BbbPopulation population;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		myGraphics = new HashSet<SpacewarGraphics>();
		knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();
		population = new BbbPopulation();
		try {
			population.readFromFile(getKnowledgeFile());
		} catch (FileNotFoundException e) {
			// then just make some random ones
			BbbIndividual id;
			for (int j = 0; j < 50; j++){
				id = new BbbIndividual();
				population.add(id);
			}
		}
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		population.writeToFile(getKnowledgeFile());
	} 

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		HashMap<UUID, AbstractAction> myActions = new HashMap<UUID, AbstractAction>();
		myGraphics.clear();

		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

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
				Vector2D thrust = data.getThrust(space, ship);
				myActions.put(ship.getId(), new RawAction(thrust, 0));
				myGraphics.addAll(data.getGraphics());
				myGraphics.addAll(data.getNavGraphics(space, ship));
			}
			else {
				// it is a base and nobody cares about them, lol
				myActions.put(actionable.getId(), new DoNothingAction());
			}

			
		}
		return myActions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		return new HashSet<SpacewarGraphics>(myGraphics);
	}


	@Override
	/**
	 * Always buy bases whenever we can.
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects,
			ResourcePile resourcesAvailable,
			PurchaseCosts purchaseCosts) {
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();

		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					// Only place at ships with resources, which is a cheap and dirty way to prevent base clustering.
					if (ship.getResources().getMass() == 0) continue;
					purchases.put(ship.getId(), PurchaseTypes.BASE);
					// AI #1 needs to reset which base is the target in this event.

						if (knowledgeMap.containsKey(ship.getId())) {
							KnowledgeRepOne data = knowledgeMap.get(ship.getId());
							data.setObjective(null);
						}

					break;
				}
			}		
		}
		return purchases;
	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		return new HashMap<UUID, SpaceSettlersPowerupEnum>();
	}
}
