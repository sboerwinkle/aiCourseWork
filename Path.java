package barn1474;

import java.util.UUID;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

class Path {
	static final double CRUISE_SPEED = 50;
	static final double DIST_EPSILON = 5;

	int numWaypointsCompleted;
	Vector2D[] waypoints;
	UUID objectiveID;
	AbstractObject objective;
	/**
	 * An estimate, calculated by AStar.
	 */
	double energyCost;
	/**
	 * An estimate, calculated by AStar.
	 */
	double timeCost;

	public Path(Vector2D[] w, UUID o, double e, double t) {
		waypoints = w;
		objectiveID = o;
		objective = null;
		energyCost = e;
		timeCost = t;
		numWaypointsCompleted = 0;
	}

	/**
	 * Keeps track of our progress through the A* generated set of waypoints.
	 * Assumes objective is set
	 */
	Position getCurrentWaypoint() {
		if (numWaypointsCompleted < waypoints.length)
			return new Position(new Vector2D(objective.getPosition()).add(waypoints[numWaypointsCompleted]));
		return objective.getPosition();
	}

	/**
	 * Gives the thrust vector to head towards the target.
	 * Assumes we can accelerate instantly.
	 */
	Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
		if (objectiveID == null) return new Vector2D(0, 0);
		objective = space.getObjectById(objectiveID);
		if (!objective.isAlive()) {
			//Invalidate if the target died
			objectiveID = null;
			return new Vector2D(0, 0);
		}

		Vector2D vec = space.findShortestDistanceVector(me.getPosition(), getCurrentWaypoint());
		if (vec.getMagnitude() < DIST_EPSILON) {
			if (numWaypointsCompleted == waypoints.length) {
				//Invalidate if we've reached the end.
				objectiveID = null;
				return new Vector2D(0, 0);
			}
			numWaypointsCompleted++;
			vec = space.findShortestDistanceVector(me.getPosition(), getCurrentWaypoint());
		}
		vec = vec.unit().multiply(CRUISE_SPEED);
		vec = vec.add(objective.getPosition().getTranslationalVelocity());
		vec = vec.subtract(me.getPosition().getTranslationalVelocity());
		return vec.divide(space.getTimestep());
	}

	/**
	 * Returns whether the path is valid.
	 * Note that paths invalidate themselves on completion.
	 */
	boolean isValid() {
		return objectiveID != null;
	}
}
