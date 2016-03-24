
package barn1474.russell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.awt.Color;
import barn1474.russell.TextGraphics;
import barn1474.russell.Knowledge;
import barn1474.russell.Prescience;
import barn1474.russell.SpaceSimulation;
import barn1474.russell.LibrePD;
import barn1474.russell.ShipStateEnum;
import barn1474.russell.ShipState;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.RawAction;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.objects.Base;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.objects.weapons.Missile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

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

    //Thread pool for my spaceSim;
    ExecutorService executor;

    //Number of times knowledge object has been updated
    //Used for timeing.
    long knowledgeUpdates;

    //These objects are used for communicating with
    //the outside world. They should be carefully synchronized.

    Map<UUID, AbstractAction> teamActions;

    HashSet<SpacewarGraphics> graphics;
    Map<UUID, ShipState> shipStates;

    Random random;
    Set<SpacewarGraphics> workingGraphics = new HashSet<SpacewarGraphics>();
    Map<UUID, AbstractAction> workingActions = new HashMap<UUID, AbstractAction>();
    Map<UUID, ShipState> workingShipStates = new HashMap<UUID, ShipState>();
    /////////////////////////////////////////////////////////


    Prescience(Toroidal2DPhysics space) {
        this.spaceSim = new SpaceSimulation(space,space.getTimestep()*SIMULATION_TIMESTEP_SCALING_FACTOR);
        this.knowledge = new Knowledge(space);
        this.newKnowledge = false;
        this.graphics = new HashSet<SpacewarGraphics>();
        this.teamActions = new HashMap<UUID, AbstractAction>();
        this.exit=false;
        this.executor = Executors.newSingleThreadExecutor();
        this.knowledgeUpdates = 0;
        this.shipStates = new HashMap<UUID, ShipState>();
        workingGraphics = new HashSet<SpacewarGraphics>();
        workingActions = new HashMap<UUID, AbstractAction>();
        workingShipStates = new HashMap<UUID, ShipState>();
        random = new Random(20);
        //Look at me being such a nice person and not causing everyone else
        //to time out by setting my thread priority too high.
        Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1);
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

        while(!exit) {
            //If we don't have new information to use then wait until
            //we do.
            if(!newKnowledge) {
                Thread.yield();
            } else {
                //The Knowledge isn't new anymore
                newKnowledge = false;
                workingActions.clear();
                long ticksSinceKnowledgeUpdate = knowledgeUpdates % 20;
                if(ticksSinceKnowledgeUpdate == 0) {
                    workingGraphics.clear();

                    space = knowledge.getSpace();

                    spaceSim = runSimulation(space);
                    simulationKnowledge = new Knowledge(spaceSim,knowledge.getTeamObjects());

                    for(AbstractObject obj : spaceSim.getAllObjects()) {
                        if(obj instanceof Ship) {
                            Ship ship = (Ship) obj;
                            SpacewarGraphics graphic = new CircleGraphics(1, ship.getTeamColor(), ship.getPosition().deepCopy());
                            workingGraphics.add(graphic);


                        } else {
                            SpacewarGraphics graphic = new CircleGraphics(1, Color.RED, obj.getPosition().deepCopy());
                            workingGraphics.add(graphic);
                        }
                    }
                }

                for(AbstractActionableObject obj: knowledge.getTeamObjects()) {
                    if(obj instanceof Ship) {
                        Ship ship = (Ship) obj;
                        AbstractAction movement = getShipMovement(ship);
                        workingActions.put(ship.getId(),movement);

                    }
                }
            }

            synchronized(shipStates) {
                shipStates.clear();
                shipStates.putAll(workingShipStates);
            }
            synchronized(teamActions) {
                teamActions.clear();
                teamActions.putAll(workingActions);
            }

            synchronized(graphics) {
                graphics.addAll(workingGraphics);
            }

        }

    }

    public AbstractAction getShipMovement(Ship ship) {
        if(simulationKnowledge == null)
            simulationKnowledge = knowledge;

        Toroidal2DPhysics space = knowledge.getSpace();
        AbstractAction movement = null;
        ShipState state = null;
        ShipStateEnum currentShipState = null;
        Position aimPoint = null;
        Position goal = null;
        AbstractObject goalObject = null;

        state = workingShipStates.get(ship.getId());

        if(state == null) {
            state = new ShipState(ship,aimPoint);
        } else {
            state.setShip(ship);
        }

        currentShipState = state.getState();

        /* To be critically damped, the parameters must satisfy:
        * 2 * sqrt(Kp) = Kv*/

	double krv = currentShipState.getGenome().getChromosome().getGene(0);
	double krp = currentShipState.getGenome().getChromosome().getGene(1);

	//Initialize with zeros for the lateral movement because we don't care
        LibrePD pdController = new LibrePD(krv,krp,0,0);


        switch(currentShipState) {
        case GATHERING_ENERGY:
            goalObject = knowledge.getEnergySources(500).getClosestTo(ship.getPosition());
            goal = goalObject.getPosition();
            state.setShooting(false);
            break;
        case DELIVERING_RESOURCES:
            goalObject = knowledge.getAllTeamObjects()
                         .getBases()
                         .getClosestTo(ship.getPosition());
            goal = goalObject.getPosition();
            state.setShooting(true);
            break;
        case GATHERING_RESOURCES:
        default:
            goalObject = knowledge.getMineableAsteroids().getClosestTo(ship.getPosition());
            goal = goalObject.getPosition();
            state.setShooting(true);

        }


        aimPoint = simulationKnowledge.getNonActionable().
                   getShips().
                   getClosestTo(ship.getPosition()).
                   getPosition();
        state.setAimPoint(aimPoint);


        workingShipStates.put(ship.getId(),state);

        SpacewarGraphics aimpointgraphic = new CircleGraphics(3, Color.GREEN,aimPoint);
        workingGraphics.add(aimpointgraphic);

	Vector2D thrust = KnowledgeRepOne.doStepGetThrust(ship, space);

        movement = pdController.getRawAction(space,ship.getPosition(),goal,aimPoint);

        return new RawAction(thrust,movement.getMovement(space,ship).getAngularAcceleration());
    }


    //The following functions are the external interface used by
    //my team client

    public void exit() {
        exit = true;
        synchronized(executor) {
            executor.shutdownNow();
        }
    }

    public Map<UUID, AbstractAction>
    getMovementStart(Toroidal2DPhysics space,
                     Set<AbstractActionableObject> actionableObjects) {
        Map<UUID,AbstractAction> newActions;

        //Update what we know about the world
        //This space object is what will be used
        //throughout my AI
        synchronized(knowledge) {
            knowledge.update(space,actionableObjects);
            knowledgeUpdates++;
        }
        newKnowledge = true;

        synchronized(teamActions) {
            newActions = new HashMap<UUID, AbstractAction>(teamActions);
        }
        return newActions;

    }

    public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
    }

    public Set<SpacewarGraphics> getGraphics() {
        HashSet<SpacewarGraphics> newGraphics;
        synchronized(graphics) {
            newGraphics = new HashSet<SpacewarGraphics>(graphics);
            graphics.clear();
        }
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
        if(purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
            for(AbstractActionableObject obj : actionableObjects) {
                if(obj instanceof Ship) {
                    boolean safeToMakeBase = true;;
                    for(AbstractActionableObject baseMaybe : actionableObjects) {
                        if(baseMaybe instanceof Base) {
                            if(space.findShortestDistance(obj.getPosition(),baseMaybe.getPosition()) < 200)
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

		double angularShotLimit = state.getGenome().getChromosome().getGene(2);
		double intersectionTimeLimit = state.getGenome().getChromosome().getGene(3);

                if(aimPoint != null && state.getShooting() && knowledgeUpdates - lastShotTick > 1) {

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
