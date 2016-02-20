package barn1474;

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
	 * This will either be a mineable asteroid, or the base.
	 * This is a UUID, so it's persistent.
	 */
	UUID objectiveID = null;
	/**
	 * The object corresponding to objectiveID.
	 * This is an object, so we actually do our calculations with this.
	 */
	AbstractObject objective = null;
	Path path = null;

	HashSet<SpacewarGraphics> getGraphics() {
		if (path == null) return new HashSet<SpacewarGraphics>();
		return path.getGraphics();
	}

	int timeTilAStar = 0;

	/**
	 * Gives the thrust vector to head towards the target.
	 * Assumes we can accelerate instantly.
	 */
	Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
		if (path == null) return new Vector2D(0, 0);
		return path.getThrust(space, me);
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
		if (path != null && !path.isValid()) path = null;
		if (path == null) objectiveID = null;
		objective = objectiveID == null ? null : space.getObjectById(objectiveID);
		if (objective != null && !objective.isAlive()) {
			objectiveID = null;
			objective = null;
		}
		//If we have no objective (but do have minerals), head home.
		if (objective == null) {
			timeTilAStar = 0;
			objective = getNearestBase(space, me);
			objectiveID = objective.getId();
		}
		//If he have no resources (And aren't already hunting an asteroid), pick an asteroid to hunt.
		if ((objective == null || objective instanceof Base) && me.getResources().getMass() == 0) {
			timeTilAStar = 0;
			Asteroid besteroid = null;
			double bestDist = Double.MAX_VALUE;
			for (Asteroid a : space.getAsteroids()) {
				//It must be mineable
				if (!a.isMineable()) continue;
				//and close to me
				double dist = space.findShortestDistance(a.getPosition(), me.getPosition());
				if (dist > bestDist) continue;

				besteroid = a;
				bestDist = dist;
			}
			objective = besteroid;
			objectiveID = objective == null ? null : objective.getId();
			if (objective == null) return;
		}
		if (timeTilAStar-- == 0) {
			timeTilAStar = 10;
			path = AStar.doAStar(space, me, objective, me);
		}
	}
}
