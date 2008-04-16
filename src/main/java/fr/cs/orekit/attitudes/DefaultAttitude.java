package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class provides a default attitude law.

 * <p>
 * The attitude law is defined as having the Z axis pointed towards central body,
 * Y opposite to angular momentum and X roughly along velocity (it would be perfectly
 * aligned only for circular orbits or at perigee and apogee of non-circular orbits).</p>
 * <p>
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  Luc Maisonobe
 */
public class DefaultAttitude implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 7353978128982305892L;

    /** Private constructor for the singleton.
     */
    private DefaultAttitude() {
    }

    /** Get the unique instance of this class.
     * @return the unique instance
     */
    public static AttitudeLaw getInstance() {
       return LazyHolder.instance;
    }

    /** {@inheritDoc} */
    public Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame)
        throws OrekitException {
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D momentum = Vector3D.crossProduct(p, v);
        final double angularVelocity =
            Vector3D.dotProduct(momentum, momentum) / Vector3D.dotProduct(p, p);
        return new Attitude(frame,
                            new Rotation(p, momentum, Vector3D.minusK, Vector3D.minusJ),
                            new Vector3D(angularVelocity, Vector3D.minusJ));
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all version of java.</p>
     */
    private static class LazyHolder {
        private static final AttitudeLaw instance = new DefaultAttitude();
    }

}
