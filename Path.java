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
    int[] switchTimes;
    /**
     * An estimate, calculated by AStar.
     */
    double energyCost;
    /**
     * An estimate, calculated by AStar.
     */
    double timeCost;

    public Path(Vector2D[] w, int[] switchTimes, double e, double t) {
        waypoints = w;
	this.switchTimes = switchTimes;
        energyCost = e;
        timeCost = t;
        numWaypointsCompleted = 0;
    }

    ////Puts one red circle per waypoint left to complete
    //Actually returns nothing, haha
    public HashSet<SpacewarGraphics> getGraphics() {
	    return new HashSet<SpacewarGraphics>();
        /*HashSet<SpacewarGraphics> ret = new HashSet<SpacewarGraphics>();
        for (int i = numWaypointsCompleted; i < waypoints.length; i++) {
            ret.add(new CircleGraphics(2, Color.RED, new Position(new Vector2D(objective.getPosition()).add(waypoints[i]))));
        }
        ret.add(new CircleGraphics(2, Color.RED, objective.getPosition()));
        return ret;
	*/
    }

    /**
     * Gives the thrust vector to head towards the target.
     * Assumes we can accelerate instantly.
     */
    public Vector2D getThrust(Toroidal2DPhysics space, Ship me) {
	    int time = space.getCurrentTimestep();
	    while (numWaypointsCompleted < switchTimes.length && switchTimes[numWaypointsCompleted] <= time) {
		    numWaypointsCompleted++;
	    }
	    return waypoints[numWaypointsCompleted];
    }

    /**
     * Returns whether the path is valid.
     */
    public boolean isValid() {
        return numWaypointsCompleted < switchTimes.length;
    }
}
