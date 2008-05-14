package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class provides a default attitude law.

 * <p>
 * The attitude law is defined as a rotation offset from local orbital frame.
 * This rotation can be defined by 
 * NB : Local orbital frame is defined as follows : 
 *       Z axis pointed towards central body,
 *       Y opposite to angular momentum 
 *       X roughly along velocity (it would be perfectly aligned only for 
 *       circular orbits or at perigee and apogee of non-circular orbits).</p>
 * <p>
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  Véronique Pommier-Maurussane
 */
public class LofOffset implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Rotation from local orbital frame.  */
    private final Rotation offset;

    /** Creates new instance.
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     */
    public LofOffset(RotationOrder order, double alpha1, double alpha2, double alpha3) {
        this.offset = new Rotation(order, alpha1, alpha2, alpha3);
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
    public Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame) {

        // Construction of the local orbital frame
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D momentum = Vector3D.crossProduct(p, v);
        final double angularVelocity =
            Vector3D.dotProduct(momentum, momentum) / Vector3D.dotProduct(p, p);
        
        Rotation lofRot = new Rotation(p, momentum, Vector3D.minusK, Vector3D.minusJ);
        Vector3D spinAxis = new Vector3D(angularVelocity, Vector3D.minusJ);
        
        // Compose with offset rotation
        return new Attitude(frame, offset.applyTo(lofRot), offset.applyTo(spinAxis));

    }
    


}
