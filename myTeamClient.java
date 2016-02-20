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
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Vector2D;

/**
 * Cooperative agents grab resources and bring them home.
 *
 * @author Barnett and Boerwinkle
 *
 */
public class myTeamClient extends TeamClient {

	HashSet<SpacewarGraphics> myGraphics;
	private static final double APPROACH_VELOCITY = 1.0;

	HashMap<UUID, KnowledgeRepOne> knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();

	@Override
	public void initialize(Toroidal2DPhysics space) {
		myGraphics = new HashSet<SpacewarGraphics>();

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
				}
				KnowledgeRepOne data = knowledgeMap.get(ship.getId());
				
					
					if (data.path != null && !data.path.isValid()) data.path = null;
					if (data.path == null) data.objectiveID = null;
					data.objective = data.objectiveID == null ? null : space.getObjectById(data.objectiveID);
					if (data.objective != null && !data.objective.isAlive()) {
						data.objectiveID = null;
						data.objective = null;
					}
					//If we have no objective (but do have minerals), head home.
					if (data.objective == null) {
						data.timeTilAStar = 0;
						data.objective = data.getNearestBase(space, ship);
						data.objectiveID = data.objective.getId();
					}
					//If he have no resources (And aren't already hunting an asteroid), pick an asteroid to hunt.
					if ((data.objective == null || data.objective instanceof Base) && ship.getResources().getMass() == 0) {
						data.timeTilAStar = 0;

						//data.objective = data.getNearestAsteroid(space, ship);
						data.objective = data.getNearestBeacon(space, ship);
						data.objectiveID = data.objective == null ? null : data.objective.getId();
						//if (objective == null) return;
					}
					if (data.timeTilAStar-- == 0) {
						data.timeTilAStar = 10;
						data.path = AStar.doAStar(space, ship, data.objective, ship);
					}
				

				Vector2D thrust = data.getThrust(space, ship);
				myGraphics.addAll(data.getGraphics());
				myActions.put(ship.getId(), new RawAction(thrust, 0));
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
							data.objectiveID = null;
							data.objective = null;
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
