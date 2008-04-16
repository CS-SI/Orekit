package fr.cs.orekit.attitudes;

import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles body center pointing attitude law.

 * <p>
 * This class represents the attitude law where the satellite z axis is
 * pointing to the body frame center.</p>
 * <p>
 * The object <code>BodyCenterPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  V. Pommier-Maurussane
 */
public class BodyCenterPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -1080963665744011149L;

    /** Creates new instance.
     * @param bodyFrame Body frame
     */
    public BodyCenterPointing(Frame bodyFrame) {
        super(bodyFrame);
    }

    /** Get target expressed in body frame at given date.
     * @param date Date for computing.
     * @param pv Satellite position-velocity vector at given date in given frame.
     * @param frame Frame in which satellite position-velocity is given.
     * 
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    protected PVCoordinates getTargetInBodyFrame(AbsoluteDate date,
                                                 PVCoordinates pv, Frame frame) {
        return PVCoordinates.ZERO;
    }

}
