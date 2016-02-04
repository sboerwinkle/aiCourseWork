package boer2245;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.RawAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.clients.TeamClient;
/**
 * A team of spinning things.
 *
 * Heavily based off the random client by amy.
 * They do their level best to spin at a constant speed.
 * @author Simon
 *
 */
public class SimonClient extends TeamClient {
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
	}

	static double GOAL_SPIN = 1.7;
	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> randomActions = new HashMap<UUID, AbstractAction>();

		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				AbstractAction current = ship.getCurrentAction();

				// if we finished, make a new spot in space to aim for
				if (current == null || current.isMovementFinished(space)) {
					double vel = GOAL_SPIN - ship.getPosition().getAngularVelocity();
					if (vel == 0) continue;
					RawAction newAction = new RawAction(0, vel/space.getTimestep());
					randomActions.put(ship.getId(), newAction);
				} else {
					randomActions.put(ship.getId(), ship.getCurrentAction());
				}
			} else {
				// it is a base and random doesn't do anything to bases
				randomActions.put(actionable.getId(), new DoNothingAction());
			}
		}
		return randomActions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		return new HashSet<SpacewarGraphics>();
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
		return new HashMap<UUID, SpaceSettlersPowerupEnum>();
	}
}
