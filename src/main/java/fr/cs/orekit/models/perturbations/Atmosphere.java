package fr.cs.orekit.models.perturbations;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;

/** Interface for atmospheric models.
 * @version $Id:Atmosphere.java 1310 2007-07-05 16:04:25Z luc $
 * @author Luc Maisonobe
 */
public interface Atmosphere extends Serializable {

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @exception OrekitException if date is out of range of solar activity model
     * or if some frame conversion cannot be performed
     */
    double getDensity(AbsoluteDate date, Vector3D position, Frame frame)
        throws OrekitException;

    /** Get the inertial velocity of atmosphere molecules.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @exception OrekitException if some conversion cannot be performed
     */
    Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame)
        throws OrekitException;

}
