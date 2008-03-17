package fr.cs.orekit.attitudes.models;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.bodies.ThirdBody;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

//TODO Validation of the javadoc by the headchief
/** Third body pointing attitute representation.
 *
 * <p> Perfectly automatised attitude, as it does not consider the
 *  perturbing couples, the captors and spacecraft dynamic.</p>
 *
 * @author F. Maussion
 */
public class ThirdBodyPointingAttitude implements AttitudeKinematicsProvider {

    /** Initializes the first rotation.
     * <p> The initial attidute of the spacecraft is defined by the rotation
     * axis (it is the axis wich is pointing to the third body) of the
     * spacecraft and two planes. They are defined by the third body
     * direction and the same vector expressed in inertial frame and in the
     * spacecraft frame (approximatively). In fact, the correct vector will
     * be calculated internaly, the important information is a correct
     * definition of the planes : see
     *  {@link Rotation#Rotation(Vector3D, Vector3D, Vector3D, Vector3D)}. <p>
     * @param body the third body to point at
     * @param initDate the initial date
     * @param pv the initial {@link PVCoordinates}
     * @param frame the inertial frame in which are defined the coordinates
     * @param spin the spacecraft rotation angle first time derivate (rad/s)
     * @param rotAxis the rotation axis of the spacecraft (in spacecraft frame)
     * @param inertial the vector which defines the first plane in inertial frame
     * @param spacecraft the vector which defines the same plane in spacecraft frame
     * @throws OrekitException
     */
    public ThirdBodyPointingAttitude(ThirdBody body,AbsoluteDate initDate,PVCoordinates pv, Frame frame,
                                     double spin, Vector3D rotAxis, Vector3D inertial, Vector3D spacecraft)
    throws OrekitException {

        this.spin = spin;
        this.body = body;
        this.rotAxis = rotAxis.normalize();
        this.initDate = initDate;
        this.initFrame = frame;
        Vector3D initBodyDir = body.getPosition(initDate, frame).subtract(pv.getPosition());
        this.R0 = new Rotation(initBodyDir, inertial, this.rotAxis, spacecraft);

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
        return new AttitudeKinematics(getAttitude(date, pv, frame),
                                      new Vector3D(spin, rotAxis));
    }

    /** Get the attitude rotation.
     * <p> The {@link Rotation} returned by this method represents the rotation
     * to apply to a vector expressed in the inertial frame to obtain the same vector
     * defined in the spacecraft frame </p>
     */
    private Rotation getAttitude(AbsoluteDate date, PVCoordinates pv, Frame frame)
    throws OrekitException {

        Vector3D newBodyDir = body.getPosition(date, frame).subtract(pv.getPosition());

        newBodyDir = R0.applyTo(newBodyDir);

        Rotation RBody = new Rotation(newBodyDir, rotAxis);

        Rotation RSpin = new Rotation(rotAxis, spin*date.minus(initDate));

        Rotation finalRot = RSpin.applyTo(RBody.applyTo(R0));

        return initFrame.getTransformTo(frame, date).getRotation().applyTo(finalRot);

    }

    /** The body to point at. */
    private final ThirdBody body;

    /** The spin (rad/s). */
    private final double spin;

    /** The spacecraft spin vector. */
    private final Vector3D rotAxis;

    /** The initial (definition) date. */
    private final AbsoluteDate initDate;

    /** The initial rotation. */
    private final Rotation R0;

    /** The definition frame. */
    private Frame initFrame;

}
