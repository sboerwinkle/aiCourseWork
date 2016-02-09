package boer2245;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.io.PrintStream;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.RawAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import spacesettlers.clients.TeamClient;
/**
 * Simon's team; plays the cooperative route.
 *
 * Alternates between going after the base and going after a mineable asteroid (closest w/ clear line-of-sight at time of decision)
 * @author Simon
 *
 */
public class SimonClient2 extends TeamClient {

	HashMap<UUID, KnowledgeRepOne> knowledgeMap = new HashMap<UUID, KnowledgeRepOne>();

	//Rather than this PD jazz, we fly at constant speed. This is that speed.
	static final double CRUISE_SPEED = 50;

	private class KnowledgeRepOne {
		//This will either be a mineable asteroid, or the base.
		UUID objectiveID = null;
		AbstractObject objective = null;
		//If there's an asteroid on the path to the beacon, skirts around it first.
		//Since movement is done in the frame of reference of our target, this stores displacement from the target.
		Vector2D target;

		Position getTargetPosition() {
			return new Position(new Vector2D(objective.getPosition()).add(target));
		}

		boolean closeToTarget(Toroidal2DPhysics space, Ship me) {
			return space.findShortestDistance(me.getPosition(), getTargetPosition()) < me.getRadius();
		}

		Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
			if (objective == null) return new Vector2D(0, 0);
			Vector2D vec = space.findShortestDistanceVector(me.getPosition(), getTargetPosition());
			vec = vec.unit().multiply(CRUISE_SPEED);
			//vec = vec.add(objective.getPosition().getTranslationalVelocity());
			vec = vec.subtract(me.getPosition().getTranslationalVelocity());
			return vec.divide(space.getTimestep());
		}

		AbstractObject getHomeBase(Toroidal2DPhysics space, Ship me) {
			for (Base b : space.getBases()) {
				if (!b.isHomeBase()) continue;
				if (b.getTeamName().equals(me.getTeamName()))
					return b;
			}
			System.err.println("Can't find a home base for ship:\n" + me);
			return null;
		}

		void updateKnowledge(Toroidal2DPhysics space, Ship me) {
			//myOut.println("(got here)");
			//No use pursuing something that isn't there.
			objective = objectiveID == null ? null : space.getObjectById(objectiveID);
			if (objective != null && !objective.isAlive()) {
				objectiveID = null;
				objective = null;
			}
			//If we have no objective (but do have minerals), head home.
			if (objective == null) {// && me.getResources().getMass() != 0) {
				objective = getHomeBase(space, me);
				objectiveID = objective.getId();
				target = new Vector2D(0, 0);
			}
			//If he have no resources (And aren't already hunting an asteroid), pick an asteroid to hunt.
			if ((objective == null || objective instanceof Base) && me.getResources().getMass() == 0) {
				Asteroid besteroid = null;
				double bestDist = Double.MAX_VALUE;
				target = new Vector2D(0, 0);
				for (Asteroid a : space.getAsteroids()) {
					//It must be mineable
					if (!a.isMineable()) continue;
					//Close to me
					double dist = space.findShortestDistance(a.getPosition(), me.getPosition());
					if (dist > bestDist) continue;
					//And free of obstructions
					objective = a;
					if (!targetSafe(space, me, false)) continue;
					besteroid = a;
					bestDist = dist;
				}
				objective = besteroid;
				objectiveID = objective == null ? null : objective.getId();
				if (objective == null) return;
			} else {
				//If we're sticking with the old target, check to see if:
				//	We've reached our temporary target, or
				//	The path straight to the objective is clear.

				//If I'm close to the temporary target, try to head home
				if (closeToTarget(space, me)) {
					target = new Vector2D(0, 0);
				} else {
					//Temporarily change our target to directly at the objective
					Vector2D tmp = target;
					target = new Vector2D(0, 0);
					//If that wouldn't work, *sigh* back to the temporary target
					if (!targetSafe(space, me, false)) target = tmp;
				}
			}
			//myOut.println("(got there)");
			//Scoot the target to avoid obstacles
			for (int i = 0; i < 10; i++) {
				if (targetSafe(space, me, true)) break;
			}
		}

		boolean sameThing(AbstractObject a, AbstractObject b) {
			return a.getId().equals(b.getId());
		}

		//Note: No guarantee that iterating over this method will produce a safe tempTarget, so cap iterations.
		boolean targetSafe(Toroidal2DPhysics space, Ship me, boolean update) {
			for (Asteroid a : space.getAsteroids()) {
				if (sameThing(a, objective)) continue;
				if (thingBlocksTarget(space, a, me, update)) return false;
			}
			for (Base a : space.getBases()) {
				if (sameThing(a, objective)) continue;
				if (thingBlocksTarget(space, a, me, update)) return false;
			}
			for (Ship a : space.getShips()) {
				if (sameThing(a, me)) continue;
				if (thingBlocksTarget(space, a, me, update)) return false;
			}
			return true;
		}

		//If the asteroid does in fact block the target, this function takes the liberty of reassigning the target.
		boolean thingBlocksTarget(Toroidal2DPhysics space, AbstractObject a, Ship me, boolean update) {
			Position t = getTargetPosition();
			Vector2D displacement = space.findShortestDistanceVector(t, me.getPosition());
			double dist = displacement.getMagnitude();
			Vector2D unitNorm = displacement.unit();
			Vector2D unitTan = new Vector2D(-unitNorm.getYValue(), unitNorm.getXValue());
			displacement = space.findShortestDistanceVector(t, a.getPosition());
			//Set up a coordinate system along the vector from my target to me
			double norm = displacement.dot(unitNorm);
			double tan = displacement.dot(unitTan);
			double radius = me.getRadius() + a.getRadius();
			if (norm > dist || norm < -radius) return false;
			if (Math.abs(tan) > radius) return false;
			if (!update) return true;
			myOut.println("We've got coordinates of <"+norm+", "+tan+">, dist is "+dist+", radius is "+radius+".\nCurrent target position is readin' as "+target);
			//If we've gotten this far, it's our task to move "target" to point around the obstruction.
			Vector2D drift = a.getPosition().getTranslationalVelocity().subtract(objective.getPosition().getTranslationalVelocity());
			if (drift.dot(unitTan) > 0) tan -= radius*1.2;
			else tan += radius*1.2;

			//Convert back to objective's coordinate system, store in target
			target = unitNorm.multiply(norm).add(unitTan.multiply(tan)).add(target);
			return true;
		}
	}

	PrintStream myOut;

	@Override
	public void initialize(Toroidal2DPhysics space) {
		try {
			myOut = new PrintStream("log.txt");
		} catch (Exception e) {
			System.exit(2);
		}
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		myOut.close();
	}

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		//myOut.println("I'm being called");
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		for (AbstractObject actionable :  actionableObjects) {
			if (!(actionable instanceof Ship)) {
				//myOut.println("\tNot a ship");
				// it is a base and nobody cares about them, lol
				actions.put(actionable.getId(), new DoNothingAction());
				continue;
			}
			//myOut.println("\tA ship");
			Ship ship = (Ship) actionable;

			if (!knowledgeMap.containsKey(ship.getId())) {
				knowledgeMap.put(ship.getId(), new KnowledgeRepOne());
			}
			KnowledgeRepOne data = knowledgeMap.get(ship.getId());
			//myOut.println("\t\tIn again");
			//try {
				data.updateKnowledge(space, ship);
			/*} catch (Exception e) {
				e.printStackTrace(myOut);
			}*/
			//myOut.println("\t\tOut again");
			
			Vector2D thrust = data.getThrust(space, ship);
			actions.put(ship.getId(), new RawAction(thrust, 0));
		}
		return actions;
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
	 * For the time being, we'll focus on collection, not purchasing extra ships...
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects,
			ResourcePile resourcesAvailable,
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>();
	}

	/**
	 * Pacifism for the win
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		return new HashMap<UUID, SpaceSettlersPowerupEnum>();
	}
}
