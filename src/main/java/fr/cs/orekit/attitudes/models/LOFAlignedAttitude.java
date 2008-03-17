package fr.cs.orekit.attitudes.models;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.Translator;
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
     * @param frameType the LOF (must be one of {@link #TNW} or {@link #QSW})
     */
    public LOFAlignedAttitude(double mu, int frameType) {
        this.mu = mu;
        this.frameType = frameType;
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

        Rotation R;

        Vector3D W = Vector3D.crossProduct(pv.getPosition(),pv.getVelocity()).normalize();

        switch (frameType) {

        // if frame = QSW -----------------------------------------
        case QSW :

            Vector3D Q = pv.getPosition().normalize();

            Vector3D S = Vector3D.crossProduct(W,Q);

            R = new Rotation(Q , S, Vector3D.minusI, Vector3D.plusK);

            break; // end QSW

            // if frame = TNW -----------------------------------------
        case TNW :

            Vector3D T = pv.getVelocity().normalize();

            Vector3D N = Vector3D.crossProduct(W,T);

            R = new Rotation(N , T, Vector3D.plusI, Vector3D.plusK);

            break; // end TNW

        default :
            throw new IllegalArgumentException(Translator.getInstance().translate(
            "Choosen frame type is not correct"));
        }

        //  compute semi-major axis
        double r       = pv.getPosition().getNorm();
        double V2      = Vector3D.dotProduct(pv.getVelocity(), pv.getVelocity());
        double rV2OnMu = r * V2 / mu;
        double a       = r / (2 - rV2OnMu);
        Vector3D spin = new Vector3D(Math.sqrt(mu/(a*a*a)), Vector3D.plusJ);

        return new AttitudeKinematics(R , spin);

    }

    /** Central body gravitation coefficient */
    private double mu;

    /** Frame type */
    private int frameType;

}
