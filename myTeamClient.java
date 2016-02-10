package boer2245;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import boer2245.KnowledgeRepTwo.shipState;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.actions.RawAction;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * Cooperative agents grab resources and bring them home.
 *
 * @author Barnett and Boerwinkle
 *
 */
public class myTeamClient extends TeamClient {

	private static final double APPROACH_VELOCITY = 1.0;

	boolean useSecondAi;

	HashMap<UUID, KnowledgeRepOne> knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();

	@Override
	public void initialize(Toroidal2DPhysics space) {
		KnowledgeRepTwo.initializeKnowledge();

		//Store team name into a static string
		KnowledgeRepTwo.myTeamName = getTeamName();

		//initialize list of ships and states
		KnowledgeRepTwo.myShips = new HashMap<UUID, shipState>();
		for (Ship s : space.getShips()){
			if (s.getTeamName().equalsIgnoreCase(KnowledgeRepTwo.myTeamName)){
				KnowledgeRepTwo.myShips.put(s.getId(),KnowledgeRepTwo.shipState.GETTING_RESOURCES);
			}
		}

		//useSecondAi = Math.random() < 0.5;
		useSecondAi = false;
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {}

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> myActions = new HashMap<UUID, AbstractAction>();

		for (AbstractObject actionable :  actionableObjects) {
			if (!(actionable instanceof Ship)) {
				// it is a base and nobody cares about them, lol
				myActions.put(actionable.getId(), new DoNothingAction());
				continue;
			}
			Ship ship = (Ship) actionable;
			if (useSecondAi) {
				AbstractAction current = ship.getCurrentAction();

				// change state based on what's going on
				if (KnowledgeRepTwo.isOutOfGas(ship)) {KnowledgeRepTwo.myShips.put(ship.getId(), KnowledgeRepTwo.shipState.GETTING_GAS);}
				else if (ship.getResources().getTotal() > 0) {KnowledgeRepTwo.myShips.put(ship.getId(), KnowledgeRepTwo.shipState.GOING_HOME);}
				else if (ship.getResources().getTotal() == 0) {KnowledgeRepTwo.myShips.put(ship.getId(), KnowledgeRepTwo.shipState.GETTING_RESOURCES);}

				// if there is bacon really close just get it without changing state
				if (KnowledgeRepTwo.isBeaconNear(space, ship)){
					myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.nearBeacon.get(ship.getId()).getPosition(), ship.getPosition().getTranslationalVelocity()));
				}
				else { //do something according to what state is guiding us
					try{
						switch(KnowledgeRepTwo.myShips.get(ship.getId())){
						case GETTING_RESOURCES:
							Asteroid newAsteroid = null;
							for (Base base : space.getBases()){
								if (base.getTeamName().equals(getTeamName())){
									newAsteroid = KnowledgeRepTwo.asteroidNearestBase(space, ship, base);
									break; //for now only one base
								}
							}
							myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.getObjectIntercept(newAsteroid), newAsteroid.getPosition().getTranslationalVelocity()));
							/*
							if (KnowledgeRepTwo.isMineableAsteroidAhead(space, ship)) {
								myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.getObjectIntercept(KnowledgeRepTwo.mineableAsteroid.get(ship.getId())), KnowledgeRepTwo.mineableAsteroid.get(ship.getId()).getPosition().getTranslationalVelocity()));
							}
							else {
								myActions.put(ship.getId(), current);
							}*/
							break;
						case GETTING_GAS:
							AbstractObject gas = KnowledgeRepTwo.myNearestRefuel(space, ship);
							if (gas instanceof Base){ //base needs 0 approach velocity
								myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), gas.getPosition()));
							}
							else { //beacon needs final velocity
								myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), gas.getPosition(), ship.getPosition().getTranslationalVelocity()));
							}

							break;
						case GOING_HOME:
							myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.myNearestBase(space, ship).getPosition()));
							break;
						}
					}
					catch (NoObjectReturnedException e) {

						myActions.put(ship.getId(), current);
					}
				}
			} else {
				if (!knowledgeMap.containsKey(ship.getId())) {
					knowledgeMap.put(ship.getId(), new KnowledgeRepOne());
				}
				KnowledgeRepOne data = knowledgeMap.get(ship.getId());
				data.updateKnowledge(space, ship);

				Vector2D thrust = data.getThrust(space, ship);
				myActions.put(ship.getId(), new RawAction(thrust, 0));
			}
		}
		return myActions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		return new HashSet<SpacewarGraphics>();
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
					if (!useSecondAi) {
						if (knowledgeMap.containsKey(ship.getId())) {
							KnowledgeRepOne data = knowledgeMap.get(ship.getId());
							data.objectiveID = null;
							data.objective = null;
						}
					} else {
						//TODO: Does AI #2 need to react at all in this situation?
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
