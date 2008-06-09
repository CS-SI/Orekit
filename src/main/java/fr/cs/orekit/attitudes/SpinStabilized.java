package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles a spin stabilized attitude law.
 * <p>Spin stabilized laws are handled as wrappers for an underlying
 * non-rotating law. This underlying law is typically an instance
 * of {@link CelestialBodyPointed} with the pointing axis equal to
 * the rotation axis, but can in fact be anything.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * </p>
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  L. Maisonobe
 */
public class SpinStabilized implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -7025790361794748354L;

    /** Underlying non-rotating attitude law.  */
    private final AttitudeLaw nonRotatingLaw;

    /** Start date of the rotation. */
    private final AbsoluteDate start;

    /** Rotation axis in satellite frame. */
    private final Vector3D axis;

    /** Spin rate in radians per seconds. */
    private final double rate;

    /** Spin vector. */
    private final Vector3D spin;

    /** Creates a new instance.
     * @param nonRotatingLaw underlying non-rotating attitude law
     * @param start start date of the rotation
     * @param axis rotation axis in satellite frame
     * @param rate spin rate in radians per seconds
     */
    public SpinStabilized(final AttitudeLaw nonRotatingLaw,
                          final AbsoluteDate start,
                          final Vector3D axis, final double rate) {
        this.nonRotatingLaw = nonRotatingLaw;
        this.start          = start;
        this.axis           = axis;
        this.rate           = rate;
        this.spin           = new Vector3D(rate / axis.getNorm(), axis);
    }

    /** Get the underlying non-rotating attitude law
     * @return underlying non-rotating attitude law
     */
    public AttitudeLaw getNonRotatingLaw() {
        return nonRotatingLaw;
    }

    /** {@inheritDoc} */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // get attitude from underlying non-rotating law
        final Attitude base = nonRotatingLaw.getState(date, pv, frame);
        final Transform baseTransform = new Transform(base.getRotation(), base.getSpin());

        // compute spin transform due to spin from reference to current date
        final Transform spinInfluence =
            new Transform(new Rotation(axis, rate * date.minus(start)), spin);

        // combine the two transforms
        final Transform combined = new Transform(baseTransform, spinInfluence);

        // build the attitude
        return new Attitude(frame, combined.getRotation(), combined.getRotationRate());

    }

}
