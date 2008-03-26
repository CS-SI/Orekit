package fr.cs.orekit.attitudes.models;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** This attitude is alined on the Local Orbital Frame.
 *
 * @author F. Maussion
 */
public class LOFAlignedAttitude implements AttitudeKinematicsProvider {

    /** Identifier for TNW frame. */
    public static final int TNW = 0;

    /** Identifier for QSW frame. */
    public static final int QSW = 1;

    /** Central body gravitation coefficient */
    private final double mu;

    /** Frame type */
    private final int frameType;

    /** Simple constructor.
     *
     * <p> The XYZ satellite frame is arbitrary defined as follows in QSW frame:
     *   <pre>
     *     X = - Q ( earth centered )
     *     Y =   W ( angular momentum)
     *     Z =   S
     *   </pre>
     *  and in TNW frame:
     *   <pre>
     *     X =  N
     *     Y =  W
     *     Z =  T
     *   </pre>
     * </p>
     *
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param frameType the LOF
     * @exception IllegalArgumentException if frame type is neither
     * {@link #TNW} nor {@link #QSW}
     * @see #QSW
     * @see #TNW
     */
    public LOFAlignedAttitude(double mu, int frameType) {
        this.mu = mu;
        this.frameType = frameType;
        if ((frameType != QSW) && (frameType != TNW)) {
            OrekitException.throwIllegalArgumentException("unsupported local orbital frame, " +
                                                          "supported types: {0}, {1}",
                                                          new Object[] { "QSW", "TNW" });
        }
    }

    /** Get the attitude representation in the selected frame.
     * @param date the current date
     * @param pv the coordinates in the inertial frame
     * @param frame the inertial frame in which are defined the coordinates
     * @return the attitude representation of the spacecraft
     * @throws OrekitException if some specific error occurs.
     */
    public AttitudeKinematics getAttitudeKinematics(AbsoluteDate date,
                                                    PVCoordinates pv, Frame frame)
    throws OrekitException {

        final Vector3D wLof =
            Vector3D.crossProduct(pv.getPosition(), pv.getVelocity()).normalize();

        Rotation rot;
        if (frameType == QSW) {
            final Vector3D qLof = pv.getPosition();
            final Vector3D sLof = Vector3D.crossProduct(wLof, qLof);
            rot = new Rotation(qLof, sLof, Vector3D.minusI, Vector3D.plusK);
        } else {
            final Vector3D tLof = pv.getVelocity();
            final Vector3D nLof = Vector3D.crossProduct(wLof, tLof);
            rot = new Rotation(nLof, tLof, Vector3D.plusI, Vector3D.plusK);
        }

        //  compute semi-major axis
        // TODO this is  NOT the proper way to do this!
        final double r       = pv.getPosition().getNorm();
        final double v2      = Vector3D.dotProduct(pv.getVelocity(), pv.getVelocity());
        final double rV2OnMu = r * v2 / mu;
        final double a       = r / (2 - rV2OnMu);
        final Vector3D spin  = new Vector3D(Math.sqrt(mu / a) / a, Vector3D.plusJ);

        return new AttitudeKinematics(rot, spin);

    }

}
