package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;


/**
 * This class handles ground pointing attitude laws.
 *
 * <p>This class is a basic model for different kind of ground pointing
 * attitude laws, such as : body center pointing, nadir pointing, 
 * target pointing, etc...
 * </p>
 * <p>
 * The object <code>GroundPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     AttitudeLaw
 * @version $Id:OrbitalParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  V. Pommier-Maurussane
 */
public abstract class GroundPointing implements AttitudeLaw {
   

    /** Body frame. */
    protected final Frame bodyFrame;

    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param frame the frame that rotates with the body
     */
    protected GroundPointing(Frame bodyFrame) {
            this.bodyFrame = bodyFrame;
    }
    
    /** Get target point in body frame.
     * @return target in body frame
     */
    protected abstract PVCoordinates getTargetInBodyFrame(AbsoluteDate date, PVCoordinates pv, 
                                                           Frame frame)
                       throws OrekitException;
    
    /** Compute the target ground point at given date in given frame.
     * @param date date when the point shall be computed
     * @param pv position-velocity of the point
     * @param frame frame in which the point shall be computed
     * @throws OrekitException if some specific error occurs
     * 
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    public PVCoordinates getObservedGroundPoint(AbsoluteDate date, PVCoordinates pv, 
                                                     Frame frame) 
        throws OrekitException {
        
        /* Get target in body frame */
        PVCoordinates targetInBodyFrame = getTargetInBodyFrame(date, pv, frame);

        /* Transform to given frame */
        Transform t = bodyFrame.getTransformTo(frame, date);        
        
        /* Target in given frame. */
        return t.transformPVCoordinates(targetInBodyFrame);
    }
    
    /** Compute the system state at given date in given frame.
     * @param date date when system state shall be computed
     * @param pv satellite position/velocity in given frame
     * @param frame the frame in which pv is defined
     * @throws OrekitException if some specific error occurs
     * 
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    public Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame) 
        throws OrekitException
    {
        /* Construction of the satellite-target position/velocity vector */
        PVCoordinates pointing =  new PVCoordinates(1, getObservedGroundPoint(date, pv, frame), -1, pv);
        Vector3D p = pointing.getPosition();
        Vector3D v = pointing.getVelocity();
            
        /* New orekit exception if null position. */
        if (p.equals(Vector3D.zero)) {
            throw new OrekitException("satellite smashed on its target",
                                      new Object[] {p});
        }
        
        /* Attitude rotation in given frame : 
         * line of sight -> z satellite axis, 
         * satellite velocity -> x satellite axis. */
        Rotation r = new Rotation(p, pv.getVelocity(), Vector3D.plusK, Vector3D.plusI);
        
        /* Attitude spin */
        Vector3D spin = new Vector3D(1/Vector3D.dotProduct(p, p), Vector3D.crossProduct(p, v));
        
        return new Attitude(frame, r, r.applyTo(spin));
    }
}
