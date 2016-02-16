package barn1474;

import spacesettlers.objects.*;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

class AStar {

	//These variables are used to keep from passing a dozen different values around internally.
	//I never said this class was thread-safe, and it isn't.
	//So stop your whining, and think of them as instance variables.
	//Those can be used to pass values around internally.
	//This is no different.
	private static double radius;
	private static Ship me;
	private static AbstractObject origin, destination;
	private static Vector2D destVel;

	/**
	 * Performs A* algorithm on a rotated grid of points.
	 * Throws a NoPathException if it can't find a way through.
	 * @return a Path object containing the desired path.
	 * @param a The object from which to pull the starting position. Assumed that you'll start here, stationary.
	 * @param b The object to get to. Assumed we're going to match its velocity.
	 * @param me The ship who's going to be doing this. The only object besides a and b which the collision checker ignores. Also uses my radius in calculations, not a's radius, since we're assuming a is actually stationary.
	 */
	static Path doAStar(Toroidal2DPhysics space, AbstractObject a, AbstractObject b, Ship me) {
		radius = me.getRadius();
		AStar.me = me;
		origin = a;
		destination = b;
		destVel = b.getPosition().getTranslationalVelocity();

		//Something about a priority queue
			//Needs a score, obvs.
			//Which node it is
			//Who its parent is
		//Initialize queue to a's location.
			//Score is just the heuristic
			//Node is what it is
			//Parent is, liek, -1.
		//Forever
			//Is priority queue empty?
				//Fuck
				//throw new NoPathException();
			//Grab top element of priority queue
				//Is goal?
					//Fuck yeah
					//break
				//Am I visited?
					//Continue
				//Mark me as visited
				//For each cardinal direction
					//If result invalid or visited, continue
					//Compute step
					//Is step not okay?
						//continue
					//Add a node to priority queue
		return new Path(new Vector2D[0], b.getId(), 0, 0);
	}

	static class Step {
		double startTime;
		double duration;
		Vector2D startPos;
		Vector2D velocity;
		Step(double t, double dt, Vector2D p, Vector2D v) {
			startTime = t;
			duration = dt;
			startPos = p;
			velocity = v;
		}
	}

	/**
	 * Compares UUIDs to check if two AbstractObjects are the same thing.
	 * Seemed to fix an error where it wasn't recognizing a ship as being myself, so it stays.
	 */
	static boolean sameThing(AbstractObject a, AbstractObject b) {
		return a.getId().equals(b.getId());
	}

	static boolean stepOkay(Toroidal2DPhysics space, Step s) {
		for (Asteroid a : space.getAsteroids()) {
			if (sameThing(a, origin)) continue;
			if (sameThing(a, destination)) continue;
			if (thingOccludesStep(space, a, s)) return false;
		}
		for (Base a : space.getBases()) {
			if (sameThing(a, origin)) continue;
			if (sameThing(a, destination)) continue;
			if (thingOccludesStep(space, a, s)) return false;
		}
		for (Ship a : space.getShips()) {
			if (sameThing(a, origin)) continue;
			if (sameThing(a, destination)) continue;
			if (sameThing(a, me)) continue;
			if (thingOccludesStep(space, a, s)) return false;
		}
		return true;
	}

	static boolean thingOccludesStep(Toroidal2DPhysics space, AbstractObject a, Step s) {
		Position aPos = a.getPosition();
		//See how it's moving relative to the step
		Vector2D velocity = aPos.getTranslationalVelocity().subtract(destVel);
		//Move it forward in time to where it is when the step starts
		Vector2D startPos = new Vector2D(aPos).add(velocity.multiply(s.startTime));
		//put in step's reference frame
		startPos = space.findShortestDistanceVector(new Position(s.startPos), new Position(startPos));
		//Shift velocity into the step's reference frame
		velocity = velocity.subtract(s.velocity);
		double radius = AStar.radius + a.getRadius();
		Vector2D dir = velocity.unit();
		double timeToPass;
		if (velocity.equals(new Vector2D())) {
			//If it isn't moving, we want to compare to where it is now, which this achieves.
			timeToPass = -1;
		} else {
			timeToPass = startPos.dot(dir)/-velocity.getMagnitude();
		}

		//If it's going to make it's closest approach sometime during this step
		if (timeToPass >= 0 && timeToPass <= s.duration) {
			return radius >= Math.abs(startPos.dot(new Vector2D(dir.getYValue(), -dir.getXValue())));
		}
		if (timeToPass > 0) startPos = startPos.add(velocity.multiply(s.duration));
		return radius >= startPos.getMagnitude();
	}
}
