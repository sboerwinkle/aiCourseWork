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

	HashSet<SpacewarGraphics> myGraphics;

	///**
	// * Map of ship UUID to its knowledge representation
	// */
	//HashMap<UUID, KnowledgeRepOne> knowledgeMap; //Moved to a static field of KnowledgeRepOne

	/**
	 * Manages the population for genetic learning.
	 */
	BbbPopulation population;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		myGraphics = new HashSet<SpacewarGraphics>();
		//knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();
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
		//load chromosomes into ships
		
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		//make sure to evaluate the individuals
		
		//then save
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

				Vector2D thrust = KnowledgeRepOne.doStepGetThrust(ship, space);

				myActions.put(ship.getId(), new RawAction(thrust, 0));

				KnowledgeRepOne data = KnowledgeRepOne.get(ship);
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

						KnowledgeRepOne data = KnowledgeRepOne.get(ship);
						if (data != null) {
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
