package barn1474;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import barn1474.KnowledgeRepTwo.shipState;
/**
 * A team of agents that will generically go and get resources and bring them home
 * 
 * The
 * @author barnett
 *
 */
public class myTeamClient extends TeamClient {
	HashSet<SpacewarGraphics> graphics;
	Random random;
	
	private static final double APPROACH_VELOCITY = 1.0;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		graphics = new HashSet<SpacewarGraphics>();
		random = new Random();
		
		KnowledgeRepTwo.initializeKnowledge();
		
		//Store team name into a static string
		KnowledgeRepTwo.myTeamName = getTeamName();
		
		//initialize list of ships and states
		KnowledgeRepTwo.myShips = new HashMap<UUID, shipState>();
		for (Ship s : space.getShips()){
			if (s.getTeamName().equalsIgnoreCase(KnowledgeRepTwo.myTeamName)){
				KnowledgeRepTwo.myShips.put(s.getId(),KnowledgeRepTwo.shipState.GETTING_RESOURCES);
			}
		}
	
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub
	}


	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> myActions = new HashMap<UUID, AbstractAction>();
		
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				AbstractAction current = ship.getCurrentAction();
				
				// change state based on what's going on
				if (KnowledgeRepTwo.isOutOfGas(ship)) {KnowledgeRepTwo.myShips.put(ship.getId(), KnowledgeRepTwo.shipState.GETTING_GAS);}
				else if (ship.getResources().getTotal() > 0) {KnowledgeRepTwo.myShips.put(ship.getId(), KnowledgeRepTwo.shipState.GOING_HOME);}
				else if (ship.getResources().getTotal() == 0) {KnowledgeRepTwo.myShips.put(ship.getId(), KnowledgeRepTwo.shipState.GETTING_RESOURCES);}
				
				// if there is bacon really close just get it without changing state
				if (KnowledgeRepTwo.isBeaconNear(space, ship)){
					myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.nearBeacon.get(ship.getId()).getPosition(), ship.getPosition().getTranslationalVelocity()));
				}
				else { //do something according to what state is guiding us
					try{
						switch(KnowledgeRepTwo.myShips.get(ship.getId())){
						case GETTING_RESOURCES:
							Asteroid newAsteroid = null;
							for (Base base : space.getBases()){
								if (base.getTeamName().equals(getTeamName())){
									newAsteroid = KnowledgeRepTwo.asteroidNearestBase(space, ship, base);
									break; //for now only one base
								}
							}
							myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.getObjectIntercept(newAsteroid), newAsteroid.getPosition().getTranslationalVelocity()));
							/*
							if (KnowledgeRepTwo.isMineableAsteroidAhead(space, ship)) {
								myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.getObjectIntercept(KnowledgeRepTwo.mineableAsteroid.get(ship.getId())), KnowledgeRepTwo.mineableAsteroid.get(ship.getId()).getPosition().getTranslationalVelocity()));
							}
							else {
								myActions.put(ship.getId(), current);
							}*/
							break;
						case GETTING_GAS:
							AbstractObject gas = KnowledgeRepTwo.myNearestRefuel(space, ship);
							if (gas instanceof Base){ //base needs 0 approach velocity
								myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), gas.getPosition()));
							}
							else { //beacon needs final velocity
								myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), gas.getPosition(), ship.getPosition().getTranslationalVelocity()));
							}
							
							break;
						case GOING_HOME:
							myActions.put(ship.getId(), new MoveAction(space, ship.getPosition(), KnowledgeRepTwo.myNearestBase(space, ship).getPosition()));
							break;
						}
					}
					catch (NoObjectReturnedException e) {
							
						myActions.put(ship.getId(), current);
					}
				}
				
			}
			else // it is a base
			{
				myActions.put(actionable.getId(), new DoNothingAction());
			}
			
		}
		
		return myActions;
	
	}


	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> newGraphics = new HashSet<SpacewarGraphics>(graphics);  
		graphics.clear();
		return newGraphics;
	}


	@Override
	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>();

	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		return powerupMap;
	}


}
