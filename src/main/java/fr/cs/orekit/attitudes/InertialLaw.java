package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles an inertial attitude law.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * </p>
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  L. Maisonobe
 */
public class InertialLaw implements AttitudeLaw {

    /** Dummy attitude law, perfectly aligned with the J<sub>2000</sub> frame. */
    public static final InertialLaw J2000_ALIGNED =
        new InertialLaw(Rotation.IDENTITY);

    /** Serializable UID. */
    private static final long serialVersionUID = -8661629460150215557L;
 
    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     * @param rotation rotation from J2000 to the desired satellite frame
     */
    public InertialLaw(final Rotation rotation) {
        satelliteFrame = new Frame(Frame.getJ2000(), new Transform(rotation), null);
    }

    /** {@inheritDoc} */
    public Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame)
        throws OrekitException {
        Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(frame, t.getRotation(), t.getRotationRate());
    }

}
