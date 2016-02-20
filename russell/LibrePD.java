

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

class LibrePD {
    double krv,krp;
    double ktv,ktp;

    LibrePD(double krv, double krp, double ktv, double ktp) {
        /* To be critically damped, the parameters must satisfy:
        * 2 * sqrt(Kp) = Kv*/
        this.krv = krv;
        this.krp = krp;
        this.ktv = ktv;
        this.ktp = ktp;
    }

    public RawAction getRawAction(Toroidal2DPhysics space, Position position, Position destination, Position aimPoint) {
        //Calculate position vectors corresponding to out locations
        Vector2D positionVector = new Vector2D(position);
        Vector2D destinationVector = new Vector2D(destination);
        Vector2D aimPointVector = new Vector2D(aimPoint);

        double shipVelocityX = position.getTranslationalVelocityX();
        double shipVelocityY = position.getTranslationalVelocityY();

        Vector2D shipVelocityVector = new Vector2D(shipVelocityX, shipVelocityY);

        double aimPointVelocityX = aimPoint.getTranslationalVelocityX();
        double aimPointVelocityY = aimPoint.getTranslationalVelocityY();

        Vector2D aimPointVelocityVector = new Vector2D(aimPointVelocityX,aimPointVelocityY);

        double aimPointAngularVelocity = aimPointVelocityVector.getMagnitude() /
                                         space.findShortestDistanceVector(position,aimPoint).getMagnitude() *
                                         Math.sin(aimPointVelocityVector.getAngle() -
                                                 positionVector.getAngle());

        double shipRotationalVelocity = position.getAngularVelocity();

        double aimAngle = space.findShortestDistanceVector(position,aimPoint).getAngle();

        double shipAngle = position.getOrientation();

        double angleError = (aimAngle - shipAngle);

        if(angleError > Math.PI) {
            angleError -= 2 * Math.PI;
        } else if(angleError < -Math.PI) {
            angleError += 2 * Math.PI;

        }


        Vector2D positionError = space.findShortestDistanceVector(position,destination);

        double rotationalAcceleration = 0;
        Vector2D translationalAcceleration = new Vector2D(0,0);

        rotationalAcceleration = angleError * krp + (aimPointAngularVelocity - shipRotationalVelocity) * krv;

        translationalAcceleration.setX(positionError.getXValue()*ktp + (destination.getTranslationalVelocityX() - shipVelocityX)*ktv);
        translationalAcceleration.setY(positionError.getYValue()*ktp + (destination.getTranslationalVelocityY() -shipVelocityY)*ktv);

        RawAction movement = new RawAction(translationalAcceleration,rotationalAcceleration);

        return movement;

    }

}

