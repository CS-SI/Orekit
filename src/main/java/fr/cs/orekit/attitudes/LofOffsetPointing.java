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
    private final Vector3D satVector;

    /** Creates new instance.
     * @param shape Body shape
     * @param attLaw Attitude law 
     * @param alpha3 angle of the third elementary rotation
     */
    public LofOffsetPointing(BodyShape shape, AttitudeLaw attLaw, Vector3D satVector) {
        super(shape.getBodyFrame());
        this.shape = shape;
        this.attitudeLaw = attLaw;
        this.satVector = satVector;
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
    public Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame) 
        throws OrekitException
    {
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
    protected PVCoordinates getTargetInBodyFrame(AbsoluteDate date,
                                                 PVCoordinates pv, Frame frame) 
        throws OrekitException {

        // Compute satellite state at given date in given frame
        Rotation satRot = getState(date, pv, frame).getRotation();

        // Compute satellite Z axis in given frame
        Vector3D vectorFrame = satRot.applyInverseTo(satVector);

        // Compute satellite Z axis and position/velocity in body frame
        Transform t = frame.getTransformTo(shape.getBodyFrame(), date);
        Vector3D vectorBodyFrame = t.transformVector(vectorFrame);
        PVCoordinates pvBodyFrame = t.transformPVCoordinates(pv);

        // Line from satellite following Z direction
        Line groundLine = new Line(pvBodyFrame.getPosition(), vectorBodyFrame);
        System.out.println("Contains body center = " + groundLine.contains(Vector3D.zero));

        // Intersection with body shape
        GeodeticPoint gpGround = shape.getIntersectionPoint(groundLine, pvBodyFrame.getPosition(), 
                                                            shape.getBodyFrame(), date);

        return new PVCoordinates(shape.transform(gpGround), Vector3D.zero);
    }



}
