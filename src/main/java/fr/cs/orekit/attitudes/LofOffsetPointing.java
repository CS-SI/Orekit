package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.BodyShape;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.Line;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class provides a default attitude law.

 * <p>
 * The attitude pointing law is defined by an attitude law and
 * the satellite axis vector chosen for pointing.
 * <p>
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  Véronique Pommier-Maurussane
 */
public class LofOffsetPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Rotation from local orbital frame. */
    private final AttitudeLaw attitudeLaw;

    /** Body shape. */
    private final BodyShape shape;

    /** Chosen satellite axis for pointing, given in satellite frame. */
    private final Vector3D satPointingVector;

    /** Creates new instance.
     * @param shape Body shape
     * @param attLaw Attitude law
     * @param satPointingVector satellite vector defining the pointing direction
     */
    public LofOffsetPointing(final BodyShape shape, final AttitudeLaw attLaw,
                             final Vector3D satPointingVector) {
        super(shape.getBodyFrame());
        this.shape = shape;
        this.attitudeLaw = attLaw;
        this.satPointingVector = satPointingVector;
    }

    /** Compute the system state at given date in given frame.
     * @param date date when system state shall be computed
     * @param pv satellite position/velocity in given frame
     * @param frame the frame in which pv is defined
     * @return satellite attitude state at date
     * @throws OrekitException if some specific error occurs
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p> */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame)
        throws OrekitException {
        return attitudeLaw.getState(date, pv, frame);
    }

    /** Get target expressed in body frame at given date.
     * @param date computation date.
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame Frame in which satellite position-velocity is given.
     * @return target position/velocity in body frame
     * @throws OrekitException if some specific error occurs
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    protected PVCoordinates getTargetInBodyFrame(final AbsoluteDate date,
                                                 final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // Compute satellite state at given date in given frame
        final Rotation satRot = getState(date, pv, frame).getRotation();

        // Compute satellite pointing axis in given frame
        final Vector3D vectorFrame = satRot.applyInverseTo(satPointingVector);

        // Compute satellite pointing axis and position/velocity in body frame
        final Transform t = frame.getTransformTo(shape.getBodyFrame(), date);
        final Vector3D vectorBodyFrame = t.transformVector(vectorFrame);
        final PVCoordinates pvBodyFrame = t.transformPVCoordinates(pv);

        // Line from satellite following pointing direction
        final Line groundLine = new Line(pvBodyFrame.getPosition(), vectorBodyFrame);

        // Intersection with body shape
        final GeodeticPoint gpGround =
            shape.getIntersectionPoint(groundLine, pvBodyFrame.getPosition(),
                                       shape.getBodyFrame(), date);

        // Case with no intersection
        if (gpGround == null) {
            throw new OrekitException("attitude pointing law misses the Earth", new Object[0]);
        }

        return new PVCoordinates(shape.transform(gpGround), Vector3D.zero);
    }



}
