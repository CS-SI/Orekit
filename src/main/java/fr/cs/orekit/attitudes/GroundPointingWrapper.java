package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** This class leverages common parts for compensation modes around ground pointing attitudes.
 * @author V. Pommier-Maurussane
 * @author L. Maisonobe
 */
public abstract class GroundPointingWrapper extends GroundPointing {

    /** Underlying ground pointing attitude law.  */
    private final GroundPointing groundPointingLaw;

    /** Creates a new instance.
     * @param groundPointingLaw ground pointing attitude law without compensation
     * @param sun sun motion model
     */
    public GroundPointingWrapper(final GroundPointing groundPointingLaw) {
        super(groundPointingLaw.getBodyFrame());
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
                                                 final PVCoordinates pv,
                                                 final Frame frame)
        throws OrekitException {
        // return basic attitude law target
        return groundPointingLaw.getTargetInBodyFrame(date, pv, frame);
    }

    /** Compute the base system state at given date, without compensation.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return satellite base attitude state, i.e without compensation.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getBaseState(final AbsoluteDate date, final PVCoordinates pv, final Frame frame)
        throws OrekitException {
        return groundPointingLaw.getState(date, pv, frame);
    }

    /** Compute the system state at given date.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return satellite attitude state at date, in given frame.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getState(final AbsoluteDate date, final PVCoordinates pv, final Frame frame)
        throws OrekitException {
            
        // 1/ Get attitude from base attitude law
        final Attitude base = getBaseState(date, pv, frame);

        // 2/ Get yaw compensation
        final Rotation compensation = getCompensation(date, pv, base, frame);

        // 3/ Combination of base attitude and yaw compensation
        return new Attitude(frame, compensation.applyTo(base.getRotation()), compensation.applyTo(base.getSpin()));
    }

    /** Compute the compensation rotation at given date.
     * @param date date when the system state shall be computed
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param base base satellite attitude in given frame.
     * @param frame the frame in which satellite position-velocity an attitude are given.
     * @return compensation rotation at date, i.e rotation between non compensated
     * attitude state and compensated state.
     * @throws OrekitException if some specific error occurs
     */
    public abstract Rotation getCompensation(final AbsoluteDate date, final PVCoordinates pv,
                                             final Attitude base, final Frame frame)
        throws OrekitException;

}