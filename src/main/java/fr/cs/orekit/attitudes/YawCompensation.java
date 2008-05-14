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
    private static final long serialVersionUID = 5370690180672669249L;

    /** Basic ground pointing attitude law.  */
    private final GroundPointing groundPointingLaw;

    /** Creates a new instance.
     * @param groundPointingLaw Ground pointing attitude law before yaw compensation
     * @param shape Body shape
     */
    public YawCompensation(final GroundPointing groundPointingLaw, final BodyShape shape) {
        super(shape.getBodyFrame());
        this.groundPointingLaw = groundPointingLaw;
    }

    /** Get the attitude rotation.
     * @return attitude satellite rotation from reference frame.
     */
    public GroundPointing getGroundPointingLaw() {
        return groundPointingLaw;
    }

    /** Get target expressed in body frame at given date.
     * @param date computation date.
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return target position/velocity in body frame at date.
     * @throws OrekitException if some specific error occurs
     */
    protected PVCoordinates getTargetInBodyFrame(final AbsoluteDate date,
                                                 final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        /* Return basic attitude law target. */
        return groundPointingLaw.getTargetInBodyFrame(date, pv, frame);
    }

    /** Compute the base system state at given date, without yaw compensation.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return satellite base attitude state, i.e without yaw compensation.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getBaseState(final AbsoluteDate date,
                                 final PVCoordinates pv, final Frame frame)
        throws OrekitException {
        final Attitude base = groundPointingLaw.getState(date, pv, frame);
        return base;
    }

    /** Compute the system yaw compensation rotation at given date.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param base base satellite attitude in given frame.
     * @param frame the frame in which satellite position-velocity an attitude are given.
     * @return yaw compensation rotation at date, i.e rotation between non compensated
     * attitude state and compensated state.
     * @throws OrekitException if some specific error occurs
     */
    public Rotation getCompensation(final AbsoluteDate date, final PVCoordinates pv,
                                    final Attitude base, final Frame frame)
        throws OrekitException {

        // Compensation rotation definition :
        //  . Z satellite axis is unchanged
        //  . X satellite axis shall be aligned to target relative velocity
        final Vector3D satVelocity = pv.getVelocity();
        final PVCoordinates targetPV = groundPointingLaw.getObservedGroundPoint(date, pv, frame);
        final Vector3D targetVelocity = targetPV.getVelocity();
        final Vector3D relativeVelocity = targetVelocity.subtract(satVelocity);

        // Create rotation transforming zsat to zsat and relativeVelocity to -xsat
        final Rotation compensation = new Rotation(Vector3D.plusK, base.getRotation().applyTo(relativeVelocity),
                                                   Vector3D.plusK, Vector3D.minusI);

        return compensation;
    }
        /** Compute the system state at given date.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return satellite attitude state at date, in given frame.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        //1/ Get attitude from base attitude law
        final Attitude base = getBaseState(date, pv, frame);

        // 2/ Get yaw compensation
        final Rotation compensation = getCompensation(date, pv, base, frame);

        // 3/ Combination of base attitude and yaw compensation
        return new Attitude(frame, compensation.applyTo(base.getRotation()), compensation.applyTo(base.getSpin()));
    }

    /** Compute the yaw compensation angle at date.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return yaw compensation angle at date.
     * @throws OrekitException if some specific error occurs
     */
    public double getYawAngle(final AbsoluteDate date,
                              final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // Attitude rotation without yaw compensation
        final Rotation rotNoYaw = groundPointingLaw.getState(date, pv, frame).getRotation();

        // Attitude rotation without yaw compensation
        final Rotation rotYaw = getState(date, pv, frame).getRotation();

        // Compute yaw compensation angle by composition of both rotations
        final Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        final double yawAngle = compoRot.getAngle();

        return yawAngle;
    }

}
