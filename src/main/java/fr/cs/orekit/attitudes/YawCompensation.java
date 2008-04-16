package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.BodyShape;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;


/**
 * This class handles nadir pointing attitude law.

 * <p>
 * This class represents the attitude law where the satellite z axis is
 * pointing to the vertical of the ground point under satellite.</p>
 * <p>
 * The object <code>NadirPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  V. Pommier-Maurussane
 */
public class YawCompensation extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 5518690549569411172L;
    
    /** Basic ground pointing attitude law.  */
    private final GroundPointing groundPointingLaw;

    /** Get the attitude rotation.
     * @return attitude satellite rotation from reference frame.
     */
    public GroundPointing getGroundPointingLaw() {
        return groundPointingLaw;
    }

    /** Creates a new instance
     * @param groundPointingLaw Ground pointing attitude law before yaw compensation
     */
    public YawCompensation(Frame bodyFrame, GroundPointing groundPointingLaw, BodyShape shape) {
        super(bodyFrame);
        this.groundPointingLaw = groundPointingLaw;
    }

    /** Get target expressed in body frame at given date.
     * @param date Date for computing.
     * @param pv Satellite position-velocity vector at given date in given frame.
     * @param frame Frame in which satellite position-velocity is given.
     */
    protected PVCoordinates getTargetInBodyFrame(AbsoluteDate date,
                                                 PVCoordinates pv, Frame frame)
        throws OrekitException{
        /* Return basic attitude law target. */
        return groundPointingLaw.getTargetInBodyFrame(date, pv, frame);
    }

    
    /** Compute the system state at given date.
     * @param date date when system state shall be computed
     */
    public Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame)  
    throws OrekitException {
        
        /* 1/ Get attitude from base attitude law */
        Attitude base = groundPointingLaw.getState(date, pv, frame);
        
        /* 2/ Add yaw compensation */
        
        /* Z satellite axis is unchanged */
        Vector3D zsat1 = Vector3D.plusK;
        Vector3D zsat2 = Vector3D.plusK;
        
        /* X satellite axis shall be aligned to target relative velocity */
        Vector3D satVelocity = pv.getVelocity();
        Vector3D targetVelocity = groundPointingLaw.getObservedGroundPoint(date, pv, frame).getVelocity();
        Vector3D relativeVelocity = targetVelocity.subtract(satVelocity);
        Vector3D xsat = Vector3D.plusI;
        
        /* Create rotation transforming zsat to zsat and xsat to -relativeVelocity */   
        Rotation compensation = new Rotation(zsat1, xsat, zsat2, relativeVelocity.negate());
        
        /* 3/ Combination of base attitude and yaw compensation */
        return new Attitude(frame, compensation.applyTo(base.getRotation()), compensation.applyTo(base.getSpin()));
    }

}
