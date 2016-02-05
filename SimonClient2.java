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
import spacesettlers.utilities.Vector2D;
import spacesettlers.clients.TeamClient;
/**
 * Simon's team; plays the aggressive route.
 *
 * Tries to get to beacons whilst avoiding asteroids, and shoots when opportune. Also, it always spins with a constant speed.
 * @author Simon
 *
 */
public class SimonClient2 extends TeamClient {

	static final double CRUISE_SPEED = 100;
	static final double GOAL_SPIN = 1.7;
	static final double MAX_FIRE_RANGE = 100;
	static final double ANGLE_EPSILON = Math.PI/32;
	static final double DIST_EPSILON = 10; // WTF I don't even know this is a total guess

	private class KnowledgeRepOne {
		//TODO: Make "Ship me" a instance variable initialized by constructor?
		//Keeps track of the closest beacon and the closest ship
		Beacon closestBeacon;
		Ship closestShip;
		//If there's an asteroid on the path to the beacon, skirts around it first.
		Position target;
		//Roughly, our goal is to fly to the beacon whilst shooting at the ship

		boolean shipIsClose(Space space, Ship me, Position loc) {
			return space.getShortestDistance(me.getPosition(), loc) < DIST_EPSILON;
		}

		boolean shouldShoot(Space space, Ship me) {
			if (closestShip == null) return false;
			Position p = me.getPosition();
			Vector2D displacement = space.findShortestDistanceVector(p, closestShip.getPosition());
			//Construct coordinate system around my orientation
			double unx = Math.cos(p.getOrientation());
			double uny = Math.sin(p.getOrientation());
			//Compute the normal and other component
			double norm = unx*displacement.getX() + uny*displacement.getY();
			double tan = uny*displacement.getX() - unx*displacement.getY();
			if (norm < 0 || norm > MAX_FIRE_RANGE) return false;
			if (tan > closestShip.radius) return false;
			return true;
		}

		Vector2D getThrust(Space space, Ship me) {
			Vector2D displacement = space.findShortestDistanceVector(me.getPosition(), target);
			displacement = displacement.unit()*CRUISE_SPEED;
			return displacement.subtract(me.getPosition().getTranslationalVelocity()).divide(space.getTimestep());
		}

		void updateKnowledge(Space space, Ship me) {
			//Find best beacaon
			Beacon best;
			for (Beacon b : beacons) {
				if (better(b, best, me)) best = b;
			}
			//If it's not the one we were pursuing previously, reset our target
			if (best != closestBeacon) {
				closestBeacon = best;
				target = location(best);
			} else {
				//Temporarily change our target to directly at the beacon
				Position tmp = target;
				target = location(best);
				//If that wouldn't work and I'm not close enough to the temporary target to try anyway,
				//*sigh* back to the temporary target
				if (!targetSafe(me, false) && !shipIsClose(space, me, tmp)) target = tmp;
			}
			//Scoot the target to avoid asteroids
			for (int i = 0; i < 10; i++) {
				if (targetSafe(me, true)) break;
			}
			//Find best ship
			closestShip = pickNearestEnemyShip(space, ship);
		}

		//Note: No guarantee that iterating over this method will produce a safe tempTarget, so cap iterations.
		boolean targetSafe(Ship me, boolean update) {
			for(Asteroid a : asteroids) {
				if (asteroidBlocksTarget(a, me, update)) return false;
			}
			return true;
		}

		//If the asteroid does in fact block the target, this function takes the liberty of reassigning the target.
		boolean asteroidBlocksTarget(Asteroid a, Ship me, boolean update) {
			//Set up a coordinate system along the vector from me to my target
			double dist = //Distance from me to target
			double norm = //Coordinate of asteroid in coordinate system;
			double tan = //Other coordinate of asteroid in coordinate system;
			double radius = //my radius plus the asteroid's radius
			if (norm > dist + radius || norm < 0) return false;
			if (Math.abs(tan) > radius) return false;
			if (!update) return true;
			tan += radius*1.2; //There, now we're looking at a point to the right of the asteroid
			//Convert back to global coordinate system, store in target
			return true;
		}
		/**
		 * Find the nearest ship on another team and aim for it.
		 * Copied from AggressiveHeuristicAsteroidCollectorSingletonTeamClient (what a mouthful)
		 * @param space
		 * @param ship
		 * @return
		 */
		private Ship pickNearestEnemyShip(Toroidal2DPhysics space, Ship ship) {
			double minDistance = Double.POSITIVE_INFINITY;
			Ship nearestShip = null;
			for (Ship otherShip : space.getShips()) {
				// don't aim for our own team (or ourself)
				if (otherShip.getTeamName().equals(ship.getTeamName())) {
					continue;
				}
				
				double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
				if (distance < minDistance) {
					minDistance = distance;
					nearestShip = otherShip;
				}
			}
			
			return nearestShip;
		}
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
	}

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
