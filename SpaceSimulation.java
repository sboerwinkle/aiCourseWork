
package barn1474;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import spacesettlers.clients.ImmutableTeamInfo;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.simulator.Toroidal2DPhysics;

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

