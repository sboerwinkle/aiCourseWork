
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
public class RussellTeamClientMC extends RussellTeamClient {
    @Override
    public void initialize(Toroidal2DPhysics space) {
        this.prescience = new Prescience(space, getKnowledgeFile(), false);
        try {
            prescience.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
