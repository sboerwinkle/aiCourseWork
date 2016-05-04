package barn1474;

import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Movement;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import java.awt.Color;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;

public class AStar {
    //Definition of the 9 directions, in terms of Displacement X and Displacement Y
    //static final int[] dxs = {-1, -1, -1,  0,  1,  1,  1,  0, 0};
    //static final int[] dys = {-1,  0,  1,  1,  1,  0, -1, -1, 0};
    static final int numDirs = 5;
    static final int[] dxs = {-1, -1,  1,  1, 0};
    static final int[] dys = {-1,  1,  1, -1, 0};
    //sqrt(2)
    static final double S2 = 1.4142;
    //How long each cardinal direction is
    static final double[] dists = {S2, S2, S2, S2, 0};
    /*
    //How fast we move in each dimension when going N, S, E, W
    static final double V1 = Path.CRUISE_SPEED;
    //How fast we move in each dimension when going at a 45 deg angle
    static final double V2 = V1/S2;
    //Velocity vectors for each of the 8 directions
    static final double[] vxs = {-V2, -V1, -V2,   0,  V2,  V1,  V2,   0};
    static final double[] vys = {-V2,   0,  V2,  V1,  V2,   0, -V2, -V1};
    */

    //How many units of time are between the checked points (Now dynamic)
    //static final int STEP_SIZE = 15;

    //These variables are used to keep from passing a dozen different values around internally.
    //I never said this class was thread-safe, and it isn't.
    //So stop your whining, and think of them as instance variables.
    //Those can be used to pass values around internally.
    //This is no different.
    private static double timeStep;
    private static Ship me;
    private static AbstractObject destination;
    private static Toroidal2DPhysics space;
    private static Set<AbstractObject> obstacles;

    /**
     * Performs A* algorithm
     * @return a Path object containing the desired path, or <code>null</code> if no such path exists
     * @param b The object to get to.
     * @param me The ship who's going to be doing this.
     */
    public static Path doAStar(Toroidal2DPhysics space, Ship me, AbstractObject b, Knowledge obstacles) {
        AStar.me = me;
	AStar.space = space;
	AStar.obstacles = obstacles.getObjects();
	//System.out.printf("Avoiding %d obstacles\n", AStar.obstacles.size());
        destination = b;
	timeStep = space.getTimestep();

	double maxTime = Math.max(space.getWidth(), space.getHeight()) / space.MAX_TRANSLATIONAL_VELOCITY * 5 + 3 * space.MAX_TRANSLATIONAL_VELOCITY/Movement.MAX_TRANSLATIONAL_ACCELERATION;
	//int maxIterations = (int)(maxTime / timeStep);
        //Queue for A*
        PriorityQueue<Node> q = new PriorityQueue<Node>();
	Node origin = new Node(me.getPosition(), numDirs-1, null, 0);
        q.add(origin);
        Node top = null;
	boolean cond = true;
        while (cond) {
	    /*if (maxIterations-- == 0) {
		    System.out.println("Failed max iterations check");
		    return null;
	    }*/
            top = q.poll();
            if (top == null) {
		    //System.out.println("Ran out of options.");
		    return null;
	    }
	    if (top.g > maxTime) {
		    //System.out.println("Best option was too far out");
		    return null;
	    }
	    //if (top.h < timeStep * top.nextDuration) break;
	    Vector2D oldVel = top.p.getTranslationalVelocity();
	    double oldX = oldVel.getXValue();
	    double oldY = oldVel.getYValue();
            for (int dir = 0; dir < numDirs; dir++) {
		double timestep = timeStep * top.nextDuration;
		//Make sure we would venture outside the realm of possibility
		double vx = dxs[dir] * Movement.MAX_TRANSLATIONAL_ACCELERATION * timestep;
		if (Math.abs(oldX + vx) > space.MAX_TRANSLATIONAL_VELOCITY) continue;
		double vy = dys[dir] * Movement.MAX_TRANSLATIONAL_ACCELERATION * timestep;
		if (Math.abs(oldY + vy) > space.MAX_TRANSLATIONAL_VELOCITY) continue;
		//TODO: Keep track of expended energy, rule out paths that way?
                //Check to make sure no obstacles would get in the way of this step
		Step s = new Step(top, new Vector2D(vx, vy), timestep);
                if (!stepOkay(s)) continue;
		Position nextPos = new Position(top.p.getX()+(oldX+vx/2)*timestep, top.p.getY()+(oldY+vy/2)*timestep);
		nextPos.setxVelocity(oldX+vx);
		nextPos.setyVelocity(oldY+vy);
		Node next = new Node(nextPos, dir, top, top.g+timestep);
		if (thingOccludesStep(destination, s, 1.0)) {
			cond = false;
			top = next;
			break;
		}
                q.add(next);
            }
        }
	HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
        //Once over to compute number of nodes in path
        int numNodes = 0;
	int time = space.getCurrentTimestep();
        for (Node n = top.parent; n != null; n = n.parent) {
		time += n.nextDuration;
		numNodes++;
	}

	//Add 1 for the vector at the end
        Vector2D[] waypoints = new Vector2D[numNodes+1];
	//This is in fact supposed to be shorter by one
	int[] switchTimes = new int[numNodes];
        //Add the path elements to the array,
        //Computing energy consumption as we go
        double ecost = Toroidal2DPhysics.ENERGY_PENALTY*me.getMass()*Movement.MAX_TRANSLATIONAL_ACCELERATION*timeStep;
        double energy = 0;
        //Iterate backwards through path
	Node n = top;
        for (int i = numNodes-1; i >= 0; i--) {
	    graphics.add(new CircleGraphics(2, Color.RED, n.p));
	    energy += ecost * dists[n.dir] * n.parent.nextDuration;
	    waypoints[i] = n.thrust;
	    switchTimes[i] = time;
	    time -= n.parent.nextDuration;
	    n = n.parent;
        }
	//Okay, we've populated everything but the last element of waypoints.
	//It has to handle the final approach.
	//This one is important, because it's the only one we'll see if we start out very close to the target.
	//waypoints[numNodes] = space.findShortestDistanceVector(top.p, destination.getPosition()).unit().multiply(Movement.MAX_TRANSLATIONAL_ACCELERATION*S2);
	waypoints[numNodes] = new Vector2D();
        return new Path(waypoints, switchTimes, energy+S2*ecost/*to cover last vector*/, top.g, graphics);
    }

    static class Node implements Comparable<Node> {
	Position p;
	int dir;
	Vector2D thrust;
	int nextDuration;
	Node parent;
        double g, h;
        public int compareTo(Node other) {
            double f = g+h;
            double o = other.g+other.h;
	    //double f = h;
	    //double o = other.h;
            if (f < o) return -1;
            if (f > o) return 1;
            return 0;
        }
        Node(Position P, int Dir, /*double D,*/ Node Par, double G) {
	    p = P;
	    dir = Dir;
	    thrust = new Vector2D(Movement.MAX_TRANSLATIONAL_ACCELERATION*dxs[dir], Movement.MAX_TRANSLATIONAL_ACCELERATION*dys[dir]);
	    //duration = D;
	    parent = Par;
	    if (parent == null) {
		    nextDuration = 1;
	    } else {
		    nextDuration = 2*parent.nextDuration;
	    }
            g = G;
	    //Optimistically assumes we're going in the right direction
	    Vector2D displacement = space.findShortestDistanceVector(p, destination.getPosition());
	    double c = -displacement.getMagnitude();
	    double a = S2*Movement.MAX_TRANSLATIONAL_ACCELERATION/2;
	    double b = p.getTranslationalVelocity().scalarProject(displacement);
	    h = (-b + Math.sqrt(b*b-4*a*c))/(2*a);
	    //h = displacement.getMagnitude() / (S2*space.MAX_TRANSLATIONAL_VELOCITY);
        }
    }

    static class Step {
	Node n;
	Vector2D thrust;
	double timestep;
        Step(Node N, Vector2D t, double ts) {
	    n = N;
	    thrust = t;
	    timestep = ts;
        }
    }

    /**
     * Compares UUIDs to check if two AbstractObjects are the same thing.
     * Seemed to fix an error where it wasn't recognizing a ship as being myself, so it stays.
     */
    static boolean sameThing(AbstractObject a, AbstractObject b) {
        return a.getId().equals(b.getId());
    }

    /**
     * @return true iff nothing would block the path of 'me' as it travels along Step s
     */
    static boolean stepOkay(Step s) {
	for (AbstractObject o : obstacles) if (!sameThing(o, me) && !sameThing(o, destination) && thingOccludesStep(o, s, 1.3)) return false;
        return true;
    }

    static boolean thingOccludesStep(AbstractObject a, Step s, double ratio) {
        Position aPos = a.getPosition();
	//Velocity of the ship, relative to the asteroid
	Vector2D initVel = s.n.p.getTranslationalVelocity().subtract(aPos.getTranslationalVelocity());
	//Position of the asteroid, relative to the ship
	Vector2D initPos = new Vector2D(aPos).add(aPos.getTranslationalVelocity().multiply(s.n.g)).subtract(new Vector2D(s.n.p));
	double x = initPos.getXValue();
	double y = initPos.getYValue();
	//Do screen wrapping
	while (x > space.getWidth()/2) x -= space.getWidth();
	while (x < -space.getWidth()/2) x += space.getWidth();
	while (y > space.getHeight()/2) y -= space.getHeight();
	while (y < -space.getHeight()/2) y += space.getHeight();
	initPos = new Vector2D(x, y);
	double r = a.getRadius() + me.getRadius() * ratio;
	Vector2D netDisp = initVel.add(s.thrust.multiply(0.5)).multiply(s.timestep);
	//Technically thrust stores a*t
	//We want x(t) = (a*t^2)/2
	//Evaluated at t/2
	//Gives a*t^2/8
	//= thrust*t/8
	Vector2D maxError = s.thrust.multiply(-s.timestep/8);
	//Okay, the problem now reduces to this:
	//Does the circle with radius r and position initPos
	//Intersect the parallelogram defined by netDisp and maxError?

	double netDispMag = netDisp.getMagnitude();
	double maxErrorMag = maxError.getMagnitude();
	if (netDispMag == 0 && maxErrorMag == 0) {
		return false;
	}

	Vector2D netDispNorm, maxErrorNorm;
	if (netDisp.cross(maxError) > 0) { // This lets us figure out which way is "out" of the parallelogram
		netDispNorm = new Vector2D(netDisp.getYValue(), -netDisp.getXValue());
		maxErrorNorm = new Vector2D(-maxError.getYValue(), maxError.getXValue());
	} else {
		netDispNorm = new Vector2D(-netDisp.getYValue(), netDisp.getXValue());
		maxErrorNorm = new Vector2D(maxError.getYValue(), -maxError.getXValue());
	}

	Vector2D[] testWalls = {netDisp, maxError, netDisp, maxError};
	Vector2D[] testNormals = {netDispNorm, maxErrorNorm, netDispNorm.negate(), maxErrorNorm.negate()};
	double[] testMags = {netDispMag, maxErrorMag, netDispMag, maxErrorMag};
	Vector2D[] testPositions = {initPos, initPos, initPos.subtract(maxError), initPos.subtract(netDisp)};

	//Assume it's probably inside.
	boolean ret = true;

	for (int i = 0; i < 4; i++) {
		int val = occlusionHelper(testWalls[i], testNormals[i], testMags[i], testPositions[i], r);
		if (val == 2) return true;
		if (val == -2) return false;
		if (val == -1) ret = false;
	}

	//Return true if it's probably inside.
	return ret;
    }

    //Return value meanings:
    //0:  inconclusive
    //2:  Definitely occluded
    //-1: Probably OK
    //-2: Definitely OK
    private static int occlusionHelper(Vector2D wall, Vector2D normal, double normMag, Vector2D position, double r) {
	    if (normMag == 0) return 0;
	    double dot = position.dot(normal);
	    //If it's more than r outside, definitely safe (return false)
	    if (dot >= r*normMag) return -2;
	    //If it's inside, do nothing.
	    if (dot <= -r*normMag) return 0;
	    //If it's just sort of outside, check if the closest point is actually on the edge.
	    dot = position.dot(wall);
	    if (dot < 0) { //No?
		    //Check the appropriate corner. Is it too close to that corner?
		    if (position.dot(position) >= r*r) return -1; //No? It's probably outside.
		    return 2; //Yes? return true
	    }
	    if (dot > normMag*normMag) { //No?
		    //Check the appropriate corner. Is it too close to that corner?
		    position = position.subtract(wall);
		    if (position.dot(position) >= r*r) return -1; //No? It's probably outside.
		    return 2; //Yes? return true
	    }
	    //The closest point is, in fact on the edge?
	    //return true
	    return 2;
    }
}
