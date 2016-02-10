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
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import spacesettlers.clients.TeamClient;

class KnowledgeRepOne {
	/**
	 * Rather than this PD jazz, we fly at constant speed. This is that speed.
	 */
	static final double CRUISE_SPEED = 50;
	/**
	 * This will either be a mineable asteroid, or the base.
	 * This is a UUID, so it's persistent.
	 */
	UUID objectiveID = null;
	/**
	 * The object corresponding to objectiveID.
	 * This is an object, so we actually do our calculations with this.
	 */
	AbstractObject objective = null;
	//If there's an asteroid on the path to the beacon, skirts around it first.
	/**
	 * Since movement is done in the frame of reference of our target, this stores displacement from the target.
	 */
	Vector2D target;

	/**
	 * Since the target is a displacement, this function converts to a global position
	 */
	Position getTargetPosition() {
		return new Position(new Vector2D(objective.getPosition()).add(target));
	}

	/**
	 * Are we close enough to target?
	 * @return whether or not the distance to target is less than the ship's radius
	 */
	boolean closeToTarget(Toroidal2DPhysics space, Ship me) {
		return space.findShortestDistance(me.getPosition(), getTargetPosition()) < me.getRadius();
	}

	/**
	 * Gives the thrust vector to head towards the target.
	 * Assumes we can accelerate instantly.
	 */
	Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
		if (objective == null) return new Vector2D(0, 0);
		Vector2D vec = space.findShortestDistanceVector(me.getPosition(), getTargetPosition());
		vec = vec.unit().multiply(CRUISE_SPEED);
		vec = vec.add(objective.getPosition().getTranslationalVelocity());
		vec = vec.subtract(me.getPosition().getTranslationalVelocity());
		return vec.divide(space.getTimestep());
	}

	/**
	 * Finds the home base corresponding to my ship
	 * @return The home base for our team, or null if none is found
	 */
	AbstractObject getNearestBase(Toroidal2DPhysics space, Ship me) {
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
	 * Updates our objective and target.
	 * Should be called once per tick.
	 */
	void updateKnowledge(Toroidal2DPhysics space, Ship me) {
		//myOut.println("(got here)");
		//No use pursuing something that isn't there.
		objective = objectiveID == null ? null : space.getObjectById(objectiveID);
		if (objective != null && !objective.isAlive()) {
			objectiveID = null;
			objective = null;
		}
		//If we have no objective (but do have minerals), head home.
		if (objective == null) {
			objective = getNearestBase(space, me);
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

	/**
	 * Compares UUIDs to check if two AbstractObjects are the same thing.
	 * Seemed to fix an error where it wasn't recognizing a ship as being myself, so it stays.
	 */
	boolean sameThing(AbstractObject a, AbstractObject b) {
		return a.getId().equals(b.getId());
	}

	/**
	 * Checks to see if there are any obstructions between me and my target, possibly updating the target if there are.
	 * Note that calling this function repeatedly isn't guaranteed to generate a safe target, so cap any iterations.
	 * @param update Whether or not to update the target in case of obstruction
	 */
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

	/**
	 * Helper to targetSafe, shouldn't be called directly.
	 * Checks to see if a particular object is in the way of our target.
	 * @param a The object to be tested
	 * @return true iff the object is in the way
	 * @see targetSafe
	 */
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
		//myOut.println("We've got coordinates of <"+norm+", "+tan+">, dist is "+dist+", radius is "+radius+".\nCurrent target position is readin' as "+target);
		//If we've gotten this far, it's our task to move "target" to point around the obstruction.
		Vector2D drift = a.getPosition().getTranslationalVelocity().subtract(objective.getPosition().getTranslationalVelocity());
		if (drift.dot(unitTan) > 0) tan -= radius*1.2;
		else tan += radius*1.2;

		//Convert back to objective's coordinate system, store in target
		target = unitNorm.multiply(norm).add(unitTan.multiply(tan)).add(target);
		return true;
	}
}
