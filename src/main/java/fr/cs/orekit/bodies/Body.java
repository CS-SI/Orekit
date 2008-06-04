package fr.cs.orekit.bodies;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;

/** Interface for celestial bodies like Sun or Moon.
 * @version $Id$
 * @author L. Maisonobe
 */
public interface Body {

    /** Get the position of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return position of the body (m)
     * @exception OrekitException if position cannot be computed in given frame
     */
    public abstract Vector3D getPosition(AbsoluteDate date, Frame frame)
        throws OrekitException;

}