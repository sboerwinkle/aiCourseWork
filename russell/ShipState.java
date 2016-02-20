
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
import barn1474.russell.ShipState;
import barn1474.russell.ShipStateEnum;
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
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

class ShipState {
    Ship ship;
    Position aimPoint;
    ShipStateEnum state;
    boolean shooting;
    long lastShotTick;


    public ShipState(Ship ship,Position aimPoint) {

        this.ship = ship;
        this.aimPoint = aimPoint;
        state = ShipStateEnum.GATHERING_RESOURCES;
        long lastShotTick = 0;
        boolean shooting = true;
        stateUpdate();

    }

    public boolean getShooting() {
        return shooting;
    }

    public void setShooting(boolean val) {
        shooting = val;
    }

    public ShipStateEnum getState() {
        stateUpdate();
        return state;
    }

    public Position getAimPoint() {
        return aimPoint;
    }

    public void setAimPoint(Position newAimPoint) {
        aimPoint = newAimPoint;
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship newShip) {
        ship = newShip;
    }

    public long getLastShotTick() {
        return lastShotTick;
    }

    public void setLastShotTick(long newLastShotTick) {
        lastShotTick = newLastShotTick;
    }

    public void stateUpdate() {
        switch(state) {
        case GATHERING_RESOURCES:
            if(ship.getEnergy() < 2500) {
                state = ShipStateEnum.GATHERING_ENERGY;
            } else if(ship.getMass() > 300) {
                state = ShipStateEnum.DELIVERING_RESOURCES;
            }
            break;
        case GATHERING_ENERGY:
            if(ship.getEnergy() > 3500) {
                if(ship.getMass() > 300) {
                    state = ShipStateEnum.DELIVERING_RESOURCES;
                } else {
                    state = ShipStateEnum.GATHERING_RESOURCES;
                }
            }
            break;
        case DELIVERING_RESOURCES:
            if(ship.getEnergy() < 2500) {
                state = ShipStateEnum.GATHERING_ENERGY;
            } else if(ship.getMass() < 300) {
                state = ShipStateEnum.GATHERING_RESOURCES;
            }
            break;
        default:
            state = ShipStateEnum.GATHERING_RESOURCES;
        }
    }

}
