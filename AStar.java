package barn1474;

import spacesettlers.objects.*;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import java.util.PriorityQueue;

public class AStar {
    //Definition of the 8 directions, in terms of Displacement X and Displacement Y
    static final int[] dxs = {-1, -1, -1,  0,  1,  1,  1,  0};
    static final int[] dys = {-1,  0,  1,  1,  1,  0, -1, -1};
    //sqrt(2)
    static final double S2 = 1.4142;
    //How long each cardinal direction is
    static final double[] dists = {S2, 1, S2, 1, S2, 1, S2, 1};
    //How fast we move in each dimension when going N, S, E, W
    static final double V1 = Path.CRUISE_SPEED;
    //How fast we move in each dimension when going at a 45 deg angle
    static final double V2 = V1/S2;
    //Velocity vectors for each of the 8 directions
    static final double[] vxs = {-V2, -V1, -V2,   0,  V2,  V1,  V2,   0};
    static final double[] vys = {-V2,   0,  V2,  V1,  V2,   0, -V2, -V1};

    //How many units are between the grid points
    static final int STEP_SIZE = 10;

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
     * @return a Path object containing the desired path, or <code>null</code> if no such path exists
     * @param a The object from which to pull the starting position. Assumed that you'll start here, stationary.
     * @param b The object to get to. Assumed we're going to match its velocity.
     * @param me The ship who's going to be doing this. The only object besides a and b which the collision checker ignores. Also used for radius and mass of navigating object.
     */
    public static Path doAStar(Toroidal2DPhysics space, AbstractObject a, AbstractObject b, Ship me) {
        radius = me.getRadius();
        AStar.me = me;
        origin = a;
        destination = b;
        destVel = b.getPosition().getTranslationalVelocity();
        Vector2D destPos = new Vector2D(b.getPosition());

        Vector2D offset = space.findShortestDistanceVector(destination.getPosition(), origin.getPosition());
        double dist = offset.getMagnitude();
        int steps = (int)Math.ceil(dist/STEP_SIZE);
        //How long it takes the ship to travel one grid space
        double timePer = dist/steps/V1;
        //Vector for one grid square towards the ship from the target
        Vector2D vecNorm = offset.divide(steps);
        //Vector for one grid square sideways
        Vector2D vecTan = new Vector2D(vecNorm.getYValue(), -vecNorm.getXValue());
        //The constructed grid is roughly half as wide as it is long.
        int halfWidth = (steps+2)/4;
        int width = 2*halfWidth+1;
        int numNodes = width*(steps+1);
        int startNode = numNodes-1-halfWidth;
        int goalNode = halfWidth;
        //parents[n] is the parent of node #n
        int[] parents = new int[numNodes];
        //Whether node #n has been visited
        boolean[] visited = new boolean[numNodes];
        //Queue for A*
        PriorityQueue<Node> q = new PriorityQueue<Node>();
        q.add(new Node(startNode, -1, 0, steps*timePer));
        Node top;
        while (true) {
            top = q.poll();
            if (top == null) return null;
            int index = top.index;
            if (visited[index]) continue;
            parents[index] = top.parent;
            if (index == goalNode) break;
            visited[index] = true;
            //n and t are the two grid coordinates
            int n = index / width;
            int t = index % width - halfWidth;
            for (int dir = 0; dir < 8; dir++) {
                int N = n+dxs[dir];
                int T = t+dys[dir];
                if (Math.abs(T) > halfWidth || N < 0 || N > steps) continue;
                if (visited[N*width+T+halfWidth]) continue;
                //Check to make sure no obstacles would get in the way of this step
                if (!stepOkay(space, new Step(top.g, dists[dir]*timePer, destPos.add(vecNorm.multiply(n)).add(vecTan.multiply(t)), new Vector2D(vxs[dir], vys[dir])))) continue;
                //Set things up so A holds displacement along the longer axis, and B along the shorter axis
                int B = Math.abs(T);
                int A = N;
                if (B > A) {
                    A = B;
                    B = N;
                }
                //Change it so N is now axis-aligned displacement, and T is 45-degree displacement
                A -= B;
                //Use these values to compute h(n) for the new node
                q.add(new Node(N*width+T+halfWidth, index, top.g+dists[dir]*timePer, (A+S2*B)*timePer));
                /* Node index,       parent,         g(n),                 h(n) */
            }
        }
        //Once over to compute number of nodes in path
        //Start at -1 because we do't want to count the end as a ndoe
        numNodes = -1;
        for (int n = goalNode; n != startNode; n = parents[n]) numNodes++;

        Vector2D[] waypoints = new Vector2D[numNodes];
        //Add the path elements to the vector,
        //Computing energy consumption as we go
        double ecost = Toroidal2DPhysics.ENERGY_PENALTY*me.getMass()*Path.CRUISE_SPEED;
        double energy = ecost; // This covers the initial acceleration from assumed standstill
        Vector2D prev = null;
        int prevN = 0;
        int prevT = 0;
        //boolean needSep = false;
        //Iterate backwards through path using the parent array
        for (int n = parents[goalNode]; n != startNode; n = parents[n]) {
            int N = n/width;
            int T = n%width - halfWidth;
            Vector2D direction = new Vector2D(N-prevN, T-prevT);
            //If we have displacement along both axes, divide by sqrt(2) to make it a unit vector.
            if (((N-prevN)&(T-prevT)) == 1) direction.divide(S2);
            if (prev != null) {
                energy += ecost*direction.subtract(prev).getMagnitude();
            }
            prev = direction;
            prevN = N;
            prevT = T;
            //Compute a displacement from the objective given the grid coordinates
            waypoints[--numNodes] = vecNorm.multiply(N).add(vecTan.multiply(T));
            /*if (T != 0) {
            	needSep = true;
            	System.out.printf("Added a point with T = %d\n", T);
            }*/
        }
        //if (needSep) System.out.println("End call");
        return new Path(waypoints, b.getId(), energy, top.g);
    }

    static class Node implements Comparable<Node> {
        int index;
        int parent;
        double g, h;
        public int compareTo(Node other) {
            double f = g+h;
            double o = other.g+other.h;
            if (f < o) return -1;
            if (f > o) return 1;
            return 0;
        }
        Node(int i, int p, double G, double H) {
            index = i;
            parent = p;
            g = G;
            h = H;
        }
    }

    static class Step {
        //From now
        double startTime;
        //From startTime
        double duration;
        //Global
        Vector2D startPos;
        //Relative to destination
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

    /**
     * @return true iff nothing would block the path of 'me' as it travels along Step s
     */
    static boolean stepOkay(Toroidal2DPhysics space, Step s) {
        for (Asteroid a : space.getAsteroids()) {
            if (a.isMineable()) continue; // Collisions with mineable asteroids are happy accidents
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
        for (AbstractWeapon a : space.getWeapons()) {
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
        double myRadius = radius + a.getRadius();
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
            return myRadius >= Math.abs(startPos.dot(new Vector2D(dir.getYValue(), -dir.getXValue())));
        }
        if (timeToPass > 0) startPos = startPos.add(velocity.multiply(s.duration));
        return myRadius >= startPos.getMagnitude();
    }
}
