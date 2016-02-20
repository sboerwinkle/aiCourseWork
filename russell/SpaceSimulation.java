
package barn1474.russell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.awt.Color;
import barn1474.russell.TextGraphics;
import barn1474.russell.Knowledge;
import barn1474.russell.Prescience;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.clients.ImmutableTeamInfo;

class SpaceSimulation extends Toroidal2DPhysics implements Callable<SpaceSimulation> {

    SpaceSimulation(Toroidal2DPhysics space, double timeStep ) {
        super(space.getHeight(),space.getWidth(),timeStep);
        this.setTeamInfo(new HashSet<ImmutableTeamInfo>(space.getTeamInfo()));

        for(AbstractObject obj : space.getAllObjects()) {
            AbstractObject newObject = obj.deepClone();
            this.addObject(newObject);
        }
    }

    public SpaceSimulation call() {
        Map<UUID, SpaceSettlersPowerupEnum> powerups = new HashMap<UUID, SpaceSettlersPowerupEnum>();
        try {
            advanceTime(getCurrentTimestep() + 1, powerups);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return this;
    }

}

