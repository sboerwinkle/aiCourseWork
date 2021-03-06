
package barn1474;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.powerups.PowerupDoubleHealingBaseEnergy;
import spacesettlers.objects.powerups.PowerupDoubleMaxEnergy;
import spacesettlers.objects.powerups.PowerupDoubleWeapon;
import spacesettlers.objects.powerups.PowerupToggleShield;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Movement;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;


public class Knowledge {
	
	private static final double BASE_ENERGY_THRESHOLD = 4000;
	private static final double SHIP_ENERGY_THRESHOLD = 3000;
	private static final double SHIP_RESOURCE_THRESHOLD = 1000;
	
    //Our knowledge object holds a copy of everything
    //currently in space. All knowledge will be derived from this object.
    //We need our own allObjects because space doesn't have an accessor for
    //it's allObjects
    Set<AbstractObject> allObjects;
    Toroidal2DPhysics space;
    Set<AbstractActionableObject> teamObjects;
    
    public Knowledge(Toroidal2DPhysics gamespace) {
        allObjects = new HashSet<AbstractObject>();
        this.allObjects.addAll(gamespace.getAllObjects());
        this.teamObjects = null;
        this.space=gamespace;
    }

    public Knowledge(Knowledge other) {
	    this.space = other.space;
	    allObjects = new HashSet<AbstractObject>(other.allObjects);
	    teamObjects = new HashSet<AbstractActionableObject>(other.teamObjects);
    }

    public Knowledge(Toroidal2DPhysics gamespace, Set<AbstractActionableObject> teamObjects) {
        allObjects = new HashSet<AbstractObject>();
        this.allObjects.addAll(gamespace.getAllObjects());
        this.teamObjects = teamObjects;
        this.space=gamespace;
    }

    //This constructor is used for creating smaller knowledge sets
    public Knowledge(Toroidal2DPhysics gamespace,Set<AbstractActionableObject> teamObjects, Set<AbstractObject> objects) {
        allObjects = new HashSet<AbstractObject>();
        allObjects.addAll(objects);
        this.teamObjects = teamObjects;
        space = gamespace;
    }

    public void update(Toroidal2DPhysics gamespace, Set<AbstractActionableObject> teamObjects) {
        allObjects.clear();
        this.teamObjects = teamObjects;
        allObjects.addAll(gamespace.getAllObjects());
        space=gamespace;
    }
    
    public boolean isTeamObject(AbstractObject obj) {
        UUID id = obj.getId();
        for(AbstractActionableObject actObj : teamObjects) {
            if(id.equals(actObj.getId())) {
                return true;
            }
        }
        return false;
    }
    
//
//
//What follows are the functions that don't return a knowledge object;
//
//

    public Set<AbstractObject> getObjects() {
        return allObjects;
    }

    public Set<AbstractActionableObject> getTeamObjects() {
        return teamObjects;
    }

    public Toroidal2DPhysics getSpace() {
        return space;
    }

    public AbstractObject getClosestTo(Position location) {
        double closestDistance = Double.MAX_VALUE;
        AbstractObject closest = null;
        for(AbstractObject obj: allObjects) {
            double dist = space.findShortestDistance(location,obj.getPosition());
            if(dist < closestDistance) {
                closestDistance = dist;
                closest = obj;
            }
        }

        return closest;
    }

    public AbstractObject getById(UUID id) {
        for(AbstractObject obj : allObjects) {
            if (obj.getId().equals(id)) {

                return obj;
            }
        }
        return null;
    }

    //
    //Boolean functions for planning
    //
    
    public Boolean HasEnergy(AbstractObject o) {
    	if (o instanceof Ship) {
    		Ship s = (Ship)o;
    		return s.getEnergy() > SHIP_ENERGY_THRESHOLD;
    	}
    	if (o instanceof Base) {
    		Base b = (Base)o;
    		return b.getEnergy() > BASE_ENERGY_THRESHOLD;
    	}
    	return false;
    }
    
    public Boolean HasResources(Ship s) {
    	return s.getResources().getTotal() > SHIP_RESOURCE_THRESHOLD;
    }
    
    //returns true if there are less ships than double the number of bases,
    // so we should buy a ship before a base
    public Boolean PurchasePriorityIsShip(){
    	return this.getAllTeamObjects().getShips().getObjects().size() < 
    			this.getAllTeamObjects().getBases().getObjects().size()*2;
    	
    }

    //
    //Everything from here on returns a knowledge object
    //

    public Knowledge getAllTeamObjects() {
        Set<AbstractObject> obj = new HashSet<AbstractObject>(teamObjects);
        return new Knowledge(space,teamObjects,obj);
    }


    public Knowledge addObjects(Set<AbstractObject> objects) {
        allObjects.addAll(objects);
        return this;
    }

    public Knowledge join(Knowledge know) {
        return new Knowledge(space,teamObjects).addObjects(know.getObjects());
    }

    public Knowledge difference(Knowledge know) {
	    return setDiff(know.allObjects);
    }

    public Knowledge setDiff(Set<AbstractObject> set) {
	    Knowledge ret = new Knowledge(space, teamObjects, allObjects);
	    ret.allObjects.removeAll(set);
	    return ret;
    }


    //Return a set of all objects with
    //minimum distance vectors having magnitude
    //between inner and outer

    public Knowledge getNonActionable() {
        Set<AbstractObject> nonActionables = new HashSet<AbstractObject>();
        try {
            for(AbstractObject obj : allObjects) {
                if(!isTeamObject(obj))
                    nonActionables.add(obj);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new Knowledge(space,teamObjects,nonActionables);
    }

    public Knowledge getEnergySources(int minEnergy) {
        Set<AbstractObject> sources = new HashSet<AbstractObject>();
        for(AbstractObject obj : allObjects) {
            if(obj instanceof Beacon) {
                if(Beacon.BEACON_ENERGY_BOOST > minEnergy)
                    sources.add(obj);
            } else if(obj instanceof Base && isTeamObject(obj)) {
                Base b = (Base) obj;
                if(b.getEnergy() > minEnergy)
                    sources.add(obj);

            }
        }
        return new Knowledge(space,teamObjects,sources);

    }



    public Knowledge getObjectsInRange(Position location, double inner, double outer) {
        Set<AbstractObject> objects = new HashSet<AbstractObject>();
        for(AbstractObject object : allObjects) {
            double dist = space.findShortestDistance(location,object.getPosition());
            if((dist > inner) &&
                    (dist < outer)) {
                objects.add(object);
            }

        }
        return new Knowledge(space,teamObjects,objects);
    }

    public Knowledge getObjectsInRange(Position location, double outer) {
        return new Knowledge(space,teamObjects,getObjectsInRange(location,0,outer).getObjects());
    }



    /*get all instances of Asteroid.*/
    public Knowledge getAsteroids() {
        Set<AbstractObject> Asteroids = new HashSet<AbstractObject>();
        for( AbstractObject obj : allObjects ) {
            if( obj instanceof Asteroid) {
                Asteroids.add(obj);
            }
        }
        return new Knowledge(space, teamObjects, Asteroids);
    }

    /*get all instances of Beacon.*/
    public Knowledge getBeacons() {
        Set<AbstractObject> Beacons = new HashSet<AbstractObject>();
        for( AbstractObject obj : allObjects ) {
            if( obj instanceof Beacon) {
                Beacons.add(obj);
            }
        }
        return new Knowledge(space, teamObjects, Beacons);
    }

    /*gets all mineable asteroids and beacons.*/
    public Knowledge getDelicious() {
	    Set<AbstractObject> Delish = new HashSet<AbstractObject>();
	    for (AbstractObject obj : allObjects) {
		    if (obj instanceof Beacon || (obj instanceof Asteroid && ((Asteroid)obj).isMineable()))
			    Delish.add(obj);
	    }
	    return new Knowledge(space, teamObjects, Delish);
    }

    /*get all instances of Base.*/
    public Knowledge getBases() {
        Set<AbstractObject> Bases = new HashSet<AbstractObject>();
        for( AbstractObject obj : allObjects ) {
            if( obj instanceof Base) {
                Bases.add(obj);
            }
        }
        return new Knowledge(space, teamObjects, Bases);
    }

    /*get all instances of Ship.*/
    public Knowledge getShips() {
        Set<AbstractObject> Ships = new HashSet<AbstractObject>();
        for( AbstractObject obj : allObjects ) {
            if( obj instanceof Ship) {
                Ships.add(obj);
            }
        }
        return new Knowledge(space, teamObjects, Ships);
    }



    public Knowledge getMineableAsteroids() {
        Set<AbstractObject> asteroids = getAsteroids().getObjects();
        Set<AbstractObject> mineable_asteroids = new HashSet<AbstractObject>();

        for(AbstractObject asteroid : asteroids) {
            //this is purely to satisfy the java
            //compiler because it uses static typing
            //rather than type inference. :P
            Asteroid aster = (Asteroid) asteroid;
            if(aster.isMineable()) {
                mineable_asteroids.add(aster);
            }
        }

        return new Knowledge(space,teamObjects, mineable_asteroids);

    }


}
