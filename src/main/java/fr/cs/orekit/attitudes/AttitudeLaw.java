package fr.cs.orekit.attitudes;

import java.io.Serializable;

import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.utils.PVCoordinates;
import fr.cs.orekit.errors.OrekitException;

/** This interface represents an attitude law model set.
*
* <p>It should be implemented by all real attitude law models before they
* can be taken into account by the attitude simulation methods.</p>
*
*
* @author V.Pommier-Maurussane
*/

public interface AttitudeLaw extends Serializable {
    
    /** Compute the system state at given date.
     * @param date date when system state shall be computed
     * @throws OrekitException if some specific error occurs
     */
    public abstract Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame)
        throws OrekitException;

}
