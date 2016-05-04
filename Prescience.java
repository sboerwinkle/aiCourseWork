
package barn1474;

import Vole.Vole;
import java.io.StringReader;
import java.io.StringWriter;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.actions.RawAction;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.objects.weapons.Missile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import barn1474.russell.ShipStateEnum;

class Prescience extends Thread {

    //If set to true the thread will quit.
    boolean exit;

    //My Knowledge representation
    Knowledge knowledge;
    //has to be a Boolean class for synchronized()
    boolean newKnowledge;

    //Space simulation object for predicting ship
    //movements.
    SpaceSimulation spaceSim;
    Knowledge simulationKnowledge;

    //Factor to multiply the old timestep by in SpaceSimulation
    final double SIMULATION_TIMESTEP_SCALING_FACTOR = 20;

    //Distance to try to space out bases when purchasing.
    final double SAFE_BASE_DIST = 200;
    
    //Thread pool for my spaceSim;
    ExecutorService executor;

    //Number of times knowledge object has been updated
    //Used for timing.
    long knowledgeUpdates;
    
    String knowledgeFile;
    
    //These objects are used for communicating with
    //the outside world. They should be carefully synchronized.

    Map<UUID, AbstractAction> teamActions;
    Map<UUID, Path> teamPaths = new HashMap<UUID, Path>();

    HashSet<SpacewarGraphics> graphics;
    Map<UUID, ShipState> shipStates;

    Random random;
    Set<SpacewarGraphics> workingGraphics = new HashSet<SpacewarGraphics>();
    Map<UUID, AbstractAction> workingActions = new HashMap<UUID, AbstractAction>();
    Map<UUID, Path> workingPaths = new HashMap<UUID, Path>();
    Map<UUID, ShipState> workingShipStates = new HashMap<UUID, ShipState>();
    HashSet<AbstractObject> workingTargets = new HashSet<AbstractObject>(); // The set of objects that are being pursued.
    /////////////////////////////////////////////////////////


    Prescience(Toroidal2DPhysics space, String knowledgeFile) {
        this.spaceSim = new SpaceSimulation(space,space.getTimestep()*SIMULATION_TIMESTEP_SCALING_FACTOR);
        this.knowledge = new Knowledge(space);
        this.newKnowledge = false;
        this.graphics = new HashSet<SpacewarGraphics>();
        this.teamActions = new HashMap<UUID, AbstractAction>();
        this.exit=false;
        this.executor = Executors.newSingleThreadExecutor();
        this.knowledgeUpdates = 0;
        this.knowledgeFile = knowledgeFile;
        this.shipStates = new HashMap<UUID, ShipState>();
        random = new Random(20);
			
    }

    public SpaceSimulation runSimulation(Toroidal2DPhysics space) {
        spaceSim = new SpaceSimulation(space,space.getTimestep()*SIMULATION_TIMESTEP_SCALING_FACTOR);
        Future<SpaceSimulation> future = executor.submit(spaceSim);
        try {
            spaceSim = future.get();
        } catch(Exception e) {
            e.printStackTrace();
            //throw new Exception("spaceSim Future.get() threw and exception!!!",e);
        }
        while(!future.isDone()) {
            Thread.yield();
        }

        return spaceSim;


    }

    public void run() {
        Toroidal2DPhysics space = null;

        //Look at me being such a nice person and not causing everyone else
        //to time out by setting my thread priority too high.
        Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1);

        while(!exit) {
            //If we don't have new information to use then wait until
            //we do.
            if(!newKnowledge) {
                Thread.yield();
            } else {
                //The Knowledge isn't new anymore
                newKnowledge = false;
                workingActions.clear();
		workingPaths.clear();
                //long ticksSinceKnowledgeUpdate = knowledgeUpdates % 20;
                workingGraphics.clear(); // Do it here since getShipMovement draws the goal now
		synchronized(knowledge) {
			simulationKnowledge = new Knowledge(knowledge);
		}
                /*if(ticksSinceKnowledgeUpdate == 0) {
                    //workingGraphics.clear();

                    space = knowledge.getSpace();

                    spaceSim = runSimulation(space);
                    simulationKnowledge = new Knowledge(spaceSim,knowledge.getTeamObjects());

                    for(AbstractObject obj : spaceSim.getAllObjects()) {
                        if(obj instanceof Ship) {
                            Ship ship = (Ship) obj;
                            //SpacewarGraphics graphic = new CircleGraphics(1, ship.getTeamColor(), ship.getPosition().deepCopy());
                            //workingGraphics.add(graphic);


                        } else {
                            //SpacewarGraphics graphic = new CircleGraphics(1, Color.RED, obj.getPosition().deepCopy());
                            //workingGraphics.add(graphic);
                        }
                    }
                }*/

		workingTargets.clear();
                for(AbstractActionableObject obj: simulationKnowledge.getTeamObjects()) {
                    if(obj instanceof Ship) {
                        Ship ship = (Ship) obj;
                        //AbstractAction movement = getShipMovement(ship);
                        //workingActions.put(ship.getId(),movement);
                        workingPaths.put(ship.getId(),getShipPath(ship));

                    }
                }
            }

            synchronized(shipStates) {
                shipStates.clear();
                shipStates.putAll(workingShipStates);
            }
            /*synchronized(teamActions) {
                teamActions.clear();
                teamActions.putAll(workingActions);
            }*/
            synchronized(teamPaths) {
                teamPaths.clear();
                teamPaths.putAll(workingPaths);
            }

            synchronized(graphics) {
		graphics.clear();
                graphics.addAll(workingGraphics);
            }

        }

    }

    public String getShipStateString(Ship ship) {
    	
    	boolean query1 = knowledge.HasEnergy(knowledge.getAllTeamObjects().getBases().getClosestTo(ship.getPosition()));
    	boolean query2 = knowledge.HasEnergy(ship);
    	boolean query3 = knowledge.HasResources(ship);
    	
    	String state = "(list #f";
    	state = query1 ? state + " #t" : state + " #f";
    	state = query2 ? state + " #t" : state + " #f";
    	state = query3 ? state + " #t" : state + " #f";
    	state = state + ")";
    	
    	return state;
    }
    
    //public AbstractAction getShipMovement(Ship ship) {
    public Path getShipPath(Ship ship) {

        Toroidal2DPhysics space = simulationKnowledge.getSpace();
        AbstractAction movement = null;
        ShipState state = null;
        ShipStateEnum currentShipState = null;
        Position aimPoint = null;
        Position goal = null;
        AbstractObject goalObject = null;

        state = workingShipStates.get(ship.getId());

        if(state == null) {
 
        	state = new ShipState(ship, aimPoint);
        	
        } else {
        	state.setShip(ship);
        }

        currentShipState = state.getState();

	String shipStateString = getShipStateString(ship);
	
	/*String program = "(define state " + shipStateString + ") (write (if (caddr state) (if (cadr state) 2 (if (car state) 2 0)) (if (cadr state) 1 0)) (current-output-port))";

	StringWriter output = new StringWriter();

	Vole vole = new Vole(new StringReader(program),output, output);
	vole.eval();
	vole.eval();

	output.flush();

	int actionNum = Integer.parseInt(output.toString());

	System.err.println(actionNum);*/

	String[] things = shipStateString.split("#");
	int actionNum = (things[4].startsWith("t") ? (things[3].startsWith("t") ? 2 : (things[2].startsWith("t") ? 2 : 0)) : (things[3].startsWith("t") ? 1 : 0));

	switch(actionNum){
		case 0:
			currentShipState = ShipStateEnum.GATHERING_ENERGY;
			break;
		case 1:
			currentShipState = ShipStateEnum.GATHERING_RESOURCES;
			break;
		case 2:
			currentShipState = ShipStateEnum.DELIVERING_RESOURCES;
			break;
		default:
			currentShipState = ShipStateEnum.DELIVERING_RESOURCES;
			break;
	}
	

        /* To be critically damped, the parameters must satisfy:
        * 2 * sqrt(Kp) = Kv*/

        //Initialize with zeros for the lateral movement because we don't care
        LibrePD pdController = new LibrePD(5.419930781363686,6.867343547631839,5.195916411362722,15.206805044350778);


        switch(currentShipState) {
        case GATHERING_ENERGY:
            goalObject = simulationKnowledge.getEnergySources(2000).setDiff(workingTargets).getClosestTo(ship.getPosition());
	    workingTargets.add(goalObject);
            goal = goalObject.getPosition();
            state.setShooting(false);
            break;
        case DELIVERING_RESOURCES:
            goalObject = simulationKnowledge.getAllTeamObjects()
                         .getBases()
                         .getClosestTo(ship.getPosition());
            goal = goalObject.getPosition();
            state.setShooting(true);
            break;
        case GATHERING_RESOURCES:
        default:
            goalObject = simulationKnowledge.getMineableAsteroids().setDiff(workingTargets).getClosestTo(ship.getPosition());
	    workingTargets.add(goalObject);
            goal = goalObject.getPosition();
            state.setShooting(true);

        }
	SpacewarGraphics graphic = new CircleGraphics(1, Color.RED, goal.deepCopy());
	workingGraphics.add(graphic);

        aimPoint = simulationKnowledge.getNonActionable().
                   getShips().
                   getClosestTo(ship.getPosition()).
                   getPosition();
        state.setAimPoint(aimPoint);

        workingShipStates.put(ship.getId(),state);

        SpacewarGraphics aimpointgraphic = new CircleGraphics(3, Color.GREEN,aimPoint);
        workingGraphics.add(aimpointgraphic);

        Path p = AStar.doAStar(space, ship, goalObject, simulationKnowledge.difference(simulationKnowledge.getMineableAsteroids()));

        movement = pdController.getRawAction(space,ship.getPosition(),goal,aimPoint);
        //return movement;

        //KnowledgeRepOne data = KnowledgeRepOne.get(ship);
        //workingGraphics.clear();
	if (p != null) workingGraphics.addAll(p.getGraphics());
        //workingGraphics.addAll(data.getNavGraphics(space, ship));*/

        //return new MyAction(p,movement.getMovement(space,ship).getAngularAccleration());
        //return new RawAction(thrust,0);
	return p;
    }


    //The following functions are the external interface used by
    //my team client

    public void exit(Toroidal2DPhysics space) {
        exit = true;
        
        synchronized(executor) {
            executor.shutdownNow();
        }
    }

    public Map<UUID, AbstractAction>
    getMovementStart(Toroidal2DPhysics space,
                     Set<AbstractActionableObject> actionableObjects) {
        Map<UUID,AbstractAction> newActions;
        Map<UUID,Path> newPaths;

        //Update what we know about the world
        //This space object is what will be used
        //throughout my AI
        synchronized(knowledge) {
            knowledge.update(space,actionableObjects);
            knowledgeUpdates++;
        }
        newKnowledge = true;

        /*synchronized(teamActions) {
            newActions = new HashMap<UUID, AbstractAction>(teamActions);
        }*/
	synchronized(teamPaths) {
		newPaths = new HashMap<UUID, Path>(teamPaths);
	}
	newActions = new HashMap<UUID, AbstractAction>();
	for (UUID u : newPaths.keySet()) {
		Path p = newPaths.get(u);
		Vector2D thrust;
		if (p == null) thrust = new Vector2D();
		else thrust = p.getThrust(space);
		newActions.put(u, new RawAction(thrust, 0));
	}
        return newActions;

    }

    public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
    }

    public Set<SpacewarGraphics> getGraphics() {
        HashSet<SpacewarGraphics> newGraphics;
        synchronized(graphics) {
            newGraphics = new HashSet<SpacewarGraphics>(graphics);
        }
	/*synchronized(teamPaths) {
		for (Path p : teamPaths.values()) newGraphics.addAll(p.getGraphics());
	}*/
        return newGraphics;
    }


    /**
     * Random never purchases
     */
    public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
            Set<AbstractActionableObject> actionableObjects,
            ResourcePile resourcesAvailable,
            PurchaseCosts purchaseCosts) {
        HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
        
        //implement purchasing logic based on planning actions
        
        if(purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
        	
        	if(knowledge.PurchasePriorityIsShip()){
        		for (AbstractActionableObject obj : actionableObjects){
        			if (obj instanceof Base){
        				purchases.put(obj.getId(), PurchaseTypes.SHIP);
        				break;
        			}
        		}
        		
        	}
        	
        	else if(purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)){
        		
        		//but put base at safe distance
            	for(AbstractActionableObject obj : actionableObjects) {
                    if(obj instanceof Ship) {
                        boolean safeToMakeBase = true;
                        for(AbstractActionableObject baseMaybe : actionableObjects) {
                            if(baseMaybe instanceof Base) {
                                if(space.findShortestDistance(obj.getPosition(),baseMaybe.getPosition()) < SAFE_BASE_DIST)
                                    safeToMakeBase = false;

                            }

                        }
                        if(safeToMakeBase) {
                            purchases.put(obj.getId(),PurchaseTypes.BASE);
                            break;
                        }

                    }
                }
        	}
        }
        
        else if(purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable) 
        		&& !knowledge.PurchasePriorityIsShip()){
        	
        	//but put base at safe distance
        	for(AbstractActionableObject obj : actionableObjects) {
                if(obj instanceof Ship) {
                    boolean safeToMakeBase = true;
                    for(AbstractActionableObject baseMaybe : actionableObjects) {
                        if(baseMaybe instanceof Base) {
                            if(space.findShortestDistance(obj.getPosition(),baseMaybe.getPosition()) < SAFE_BASE_DIST)
                                safeToMakeBase = false;
                        }
                    }
                    if(safeToMakeBase) {
                        purchases.put(obj.getId(),PurchaseTypes.BASE);
                        break;
                    }

                }
            }

        }
        return purchases;

    }

    /**
     * This is the new way to shoot (and use any other power up once they exist)
     */
    public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
            Set<AbstractActionableObject> actionableObjects) {
        HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();

        for(AbstractActionableObject obj : actionableObjects) {
            if(obj instanceof Ship) {
                Ship ship = (Ship) obj;
                ShipState state = null;
                Position aimPoint = null;
                long lastShotTick = 0;

                synchronized(shipStates) {
                    state = shipStates.get(ship.getId());
                    if(state != null) {
                        aimPoint = state.getAimPoint();
                        lastShotTick = state.getLastShotTick();
                    }
                }

                if(aimPoint != null && state.getShooting() && knowledgeUpdates - lastShotTick > 1) {

                    double angularShotLimit = 5.195916411362722;
                    double intersectionTimeLimit = 15.206805044350778;

                    boolean shoot = false;

                    Vector2D aimVector = space.findShortestDistanceVector(ship.getPosition(),aimPoint);
                    double aimDistance = aimVector.getMagnitude();

                    Position shipPosition = ship.getPosition();
                    Position aimPointPosition = ship.getPosition();

                    if((
                                Math.abs((shipPosition.getOrientation() -
                                          aimVector.getAngle()))
                                * aimDistance < angularShotLimit) &&
                            (
                                Math.abs(aimDistance/Missile.INITIAL_VELOCITY/space.getTimestep() - SIMULATION_TIMESTEP_SCALING_FACTOR - knowledgeUpdates %
                                         SIMULATION_TIMESTEP_SCALING_FACTOR) < intersectionTimeLimit
                            )) {
                        shoot = true;
                    }

                    if( shoot ) {
                        //shoot
                        synchronized(shipStates) {
                            state.setLastShotTick(knowledgeUpdates);
                        }
                        AbstractWeapon newBullet = ship.getNewWeapon(SpaceSettlersPowerupEnum.FIRE_MISSILE);
                        if(newBullet != null) {
                            powerupMap.put(ship.getId(), SpaceSettlersPowerupEnum.FIRE_MISSILE);
                        }
                    } else {
                        powerupMap.put(ship.getId(),SpaceSettlersPowerupEnum.DOUBLE_BASE_HEALING_SPEED);

                    }
                }
            }
        }

        return powerupMap;

    }
}
