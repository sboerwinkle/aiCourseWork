package barn1474;

import java.awt.Color;

import java.util.UUID;
import java.util.HashSet;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

public class Path {
    static final double CRUISE_SPEED = 50;
    //How close we have to be before we say we've hit a point
    static final double DIST_EPSILON = 5;

    int numWaypointsCompleted;
    Vector2D[] waypoints;
    //Persistent storage of objective
    UUID objectiveID;
    //Calculated from objectiveID approx. once per tick
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

    //Puts one red circle per waypoint left to complete
    public HashSet<SpacewarGraphics> getGraphics() {
        HashSet<SpacewarGraphics> ret = new HashSet<SpacewarGraphics>();
        for (int i = numWaypointsCompleted; i < waypoints.length; i++) {
            ret.add(new CircleGraphics(2, Color.RED, new Position(new Vector2D(objective.getPosition()).add(waypoints[i]))));
        }
        ret.add(new CircleGraphics(2, Color.RED, objective.getPosition()));
        return ret;
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
    public Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
        //System.out.printf("On waypoint %d/%d\n", numWaypointsCompleted, waypoints.length);
        if (objectiveID == null) return new Vector2D(0, 0);
        objective = space.getObjectById(objectiveID);
        if (!objective.isAlive()) {
            //Invalidate if the target died
            objectiveID = null;
            return new Vector2D(0, 0);
        }

        Vector2D vec = space.findShortestDistanceVector(me.getPosition(), getCurrentWaypoint());
        if (vec.getMagnitude() < DIST_EPSILON) { // Time to progress to the next waypoint
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
        vec = vec.subtract(me.getPosition().getTranslationalVelocity().divide(2));
        return vec.divide(space.getTimestep());
    }

    /*public static Vector2D testFunction(Toroidal2DPhysics space, Ship me, AbstractObject o) {
    	UUID i = o.getID();
    	AbstractObject o2 = space.getObjectById(i);
    	return space.findSho
    }*/

    /**
     * Returns whether the path is valid.
     * Note that paths invalidate themselves on completion.
     */
    public boolean isValid() {
        return objectiveID != null;
    }
}
