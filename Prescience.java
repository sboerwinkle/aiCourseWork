
package barn1474;

import java.awt.Color;
import java.io.FileNotFoundException;
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
import barn1474.evolution.BbbChromosome;
import barn1474.evolution.BbbIndividual;
import barn1474.evolution.BbbPopulation;
import barn1474.russell.ShipStateEnum;

class Prescience extends Thread {

    //If set to true the thread will quit.
    boolean exit;
    
    private static final boolean doNotLearn = true;

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

    boolean useGA;
    //genetic learning population
    BbbPopulation population;
    Markov mark;
    
    String knowledgeFile;
    
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


    Prescience(Toroidal2DPhysics space, String knowledgeFile, boolean useGA) {
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
        workingGraphics = new HashSet<SpacewarGraphics>();
        workingActions = new HashMap<UUID, AbstractAction>();
        workingShipStates = new HashMap<UUID, ShipState>();
        random = new Random(20);
	this.useGA = useGA;
	if (!doNotLearn){
		if (useGA) {
			//load population
			this.population = new BbbPopulation();
			this.population.readFromFile(knowledgeFile);

		} else {
			mark = new Markov(new double[] {0, 0, 0, 0}, new double[] {10, 10, 50, 10}, knowledgeFile);
		}
	}
			
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
        	//create new state, with an individual genome
        	if (doNotLearn) {
        		//use set values for ship behavior, no learning
        		//values are from best individual, fitness 8000
        		state = new ShipState(ship, aimPoint, new BbbIndividual(new BbbChromosome(15.267305481, 7.179784536, 5.044921937, 15.06209076)));
        	}
        	else {
        		if (useGA) {
        			state = new ShipState(ship, aimPoint, this.population.getNextIndividual());
        		}
        		else {
        			double[] vs = mark.getParameters();
        			state = new ShipState(ship, aimPoint, new BbbIndividual(new BbbChromosome(vs[0], vs[1], vs[2], vs[3])));
        		}

        	}
        } else {
        	state.setShip(ship);
        }

        currentShipState = state.getState();

        /* To be critically damped, the parameters must satisfy:
        * 2 * sqrt(Kp) = Kv*/

        double krv = state.getGenome().getChromosome().getGene(0);
        double krp = state.getGenome().getChromosome().getGene(1);

        //Initialize with zeros for the lateral movement because we don't care
        LibrePD pdController = new LibrePD(krv,krp,0,0);


        switch(currentShipState) {
        case GATHERING_ENERGY:
            goalObject = knowledge.getEnergySources(2000).getClosestTo(ship.getPosition());
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

        Path p = AStar.doAStar(space, ship, goalObject, ship);
        //System.out.println("goalObject: " + goalObject + " p.isValid: " + p.isValid());
        Vector2D thrust = (p == null) ? new Vector2D() : p.getThrust(space, ship);
        /*double angle = thrust.getAngle();
        System.out.println(angle-oldAngle);
        oldAngle = angle;*/
        //Vector2D thrust = KnowledgeRepOne.doStepGetThrust(ship, space);

        movement = pdController.getRawAction(space,ship.getPosition(),goal,aimPoint);
        //return movement;

        //KnowledgeRepOne data = KnowledgeRepOne.get(ship);
        //workingGraphics.clear();
        //workingGraphics.addAll(p.getGraphics());
        //workingGraphics.addAll(data.getNavGraphics(space, ship));*/

        return new RawAction(thrust,movement.getMovement(space,ship).getAngularAccleration());
        //return new RawAction(thrust,0);
    }


    //The following functions are the external interface used by
    //my team client

    public void exit(Toroidal2DPhysics space) {
        exit = true;

        if (!doNotLearn){
        	//make sure to evaluate the individuals
            for (Map.Entry<UUID,ShipState> e : shipStates.entrySet()) {
            	Ship s = (Ship)space.getObjectById(e.getKey());
            	double score = s.getDamageInflicted();

            	if (useGA) {
            		e.getValue().getGenome().setFitness(score);
            		// save
            		population.writeToFile(knowledgeFile);
            	} else {
            		BbbChromosome c = e.getValue().getGenome().getChromosome();
            		mark.doEndTimes(score, new double[] {c.getGene(0), c.getGene(1), c.getGene(2), c.getGene(3)});
            	}
            }
        }
        
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

                if(aimPoint != null && state.getShooting() && knowledgeUpdates - lastShotTick > 1) {

                    double angularShotLimit = state.getGenome().getChromosome().getGene(2);
                    double intersectionTimeLimit = state.getGenome().getChromosome().getGene(3);

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
