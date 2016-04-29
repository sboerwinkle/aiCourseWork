
package barn1474;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;

/**
 * A team of random agents
 *
 * The agents pick a random location in space and aim for it.  They shoot somewhat randomly also.
 * @author amy
 *
 */
public class RussellTeamClient extends spacesettlers.clients.TeamClient {
    Prescience prescience;

    @Override
    public void initialize(Toroidal2DPhysics space) {
        this.prescience = new Prescience(space, getKnowledgeFile());
        Knowledge.setTeamName(getTeamName());
        try {
            prescience.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutDown(Toroidal2DPhysics space) {
        try {
            prescience.exit(space);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public Map<UUID, AbstractAction>
    getMovementStart(Toroidal2DPhysics space,
                     Set<AbstractActionableObject> actionableObjects) {
        Map<UUID, AbstractAction> newActions = null;
        try {
            newActions = prescience.getMovementStart(space,actionableObjects);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return newActions;
    }

    @Override
    public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
        try {
            prescience.getMovementEnd(space,actionableObjects);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<SpacewarGraphics> getGraphics() {
        Set<SpacewarGraphics> graphics = null;
        try {
            graphics = prescience.getGraphics();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return graphics;
    }


    @Override
    /**
     * Random never purchases
     */
    public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
            Set<AbstractActionableObject> actionableObjects,
            ResourcePile resourcesAvailable,
            PurchaseCosts purchaseCosts) {
        Map<UUID, PurchaseTypes> purchases = null;
        try {
            purchases = prescience.getTeamPurchases(space,actionableObjects,resourcesAvailable,purchaseCosts);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return purchases;
    }

    /**
     * This is the new way to shoot (and use any other power up once they exist)
     */
    public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
            Set<AbstractActionableObject> actionableObjects) {
        Map<UUID, SpaceSettlersPowerupEnum> powerups = null;
        try {
            powerups = prescience.getPowerups(space,actionableObjects);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return powerups;
    }


}
