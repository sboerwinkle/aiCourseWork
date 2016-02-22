package barn1474;

import java.util.HashMap;
import java.util.HashSet;
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
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Vector2D;
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
	
	private static final int ASTAR_INTERVAL = 10;
	
	HashSet<SpacewarGraphics> myGraphics;
	private static final double APPROACH_VELOCITY = 1.0;

	HashMap<UUID, KnowledgeRepOne> knowledgeMap;

	@Override
	public void initialize(Toroidal2DPhysics space) {
		myGraphics = new HashSet<SpacewarGraphics>();
		knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {} 

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
				
				//first set the state of the ship accordingly
				
				
				
				//update info in case path was nullified
				data.update(space);
				
				
				//do we need to replan any main objectives?
				if (!data.isObjectiveListValid()){
					
					switch(data.getState()) {
					case DELIVERING_RESOURCES:
						//choose the closest base for now
						Base myBase = data.getNearestBase(space, ship);
						//now select objects between here and there to visi
						data.setObjectiveList(NavigationObjects.getObjectsToVisit(space, ship, myBase));
						break;
					case GATHERING_ENERGY:
						//main case for this project
						//choose the beacon farthest away as the final destination.
						Beacon beacon = data.getNearestBeacon(space, ship);
						//now select objects between here and there to visit
						data.setObjectiveList(NavigationObjects.getObjectsToVisit(space, ship, beacon));
						break;
					case GATHERING_RESOURCES:
						break;
					default:
						break;
					}

				
				}
				
				//do we need to set the next local objective?
				if (data.getObjective() == null) {
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
