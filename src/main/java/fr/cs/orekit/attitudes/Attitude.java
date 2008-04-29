package fr.cs.orekit.attitudes;

import java.io.Serializable;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.frames.Frame;

/** This class handles attitude definition.

 * <p>This class represents the rotation between a reference frame and
 * the satellite frame, as well as the spin of the satellite (axis and
 * rotation rate).</p>
 * <p>The instance <code>Attitude</code> is guaranteed to be immutable.</p>
 * @see     Orbit
 * @version $Id:KeplerianParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  V. Pommier-Maurussane
 */

public class Attitude implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 2840719248123327714L;

    /** Reference frame. */
    private final Frame referenceFrame;

     /** Attitude defined by a rotation. */
    private final Rotation attitude;

    /** Spin (spin axis AND velocity).  */
    private final Vector3D spin;

    /** Creates a new instance.
     * @param referenceFrame reference frame from which attitude is defined
     * @param attitude rotation between reference frame and satellite frame
     * @param spin satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     */
    public Attitude(final Frame referenceFrame, final Rotation attitude, final Vector3D spin) {
        this.referenceFrame = referenceFrame;
        this.attitude       = attitude;
        this.spin           = spin;
    }

    /** Get the reference frame.
     * @return referenceFrame satellite rotation from reference frame.
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    /** Get the attitude rotation.
     * @return attitude satellite rotation from reference frame.
     */
    public Rotation getRotation() {
        return attitude;
    }

    /** Get the satellite spin.
     * <p>The spin vector is defined in <strong>satellite</strong> frame.</p>
     * @return spin satellite spin (axis and velocity).
     */
    public Vector3D getSpin() {
        return spin;
    }

}
