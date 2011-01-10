package org.orekit.attitudes;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** This class is a bridge between an {@link AttitudeLaw AttitudeLaw} and an {@link AttitudeProvider AttitudeProvider} .
 * <p>An attitude law wrapper provider provides a way to implement an {@link AttitudeProvider AttitudeProvider}
 * from an {@link AttitudeLaw AttitudeLaw}.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class AttitudeLawWrapperProvider implements AttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -5959598401777708386L;

    /** Attitude provider. */
    AttitudeLaw attLaw;
       
    /** Create new instance.
     * @param referenceFrame reference frame from which attitude is defined
     * @param attProvider attitude provider
     * @param pvProvider position-velocity provider
    */
    public AttitudeLawWrapperProvider(final AttitudeLaw attLaw) {
        this.attLaw = attLaw; 
    }

    /** @inherit */
    public Attitude getAttitude(PVCoordinatesProvider pvProv,
                                AbsoluteDate date, Frame frame)
        throws OrekitException {

        return attLaw.getAttitude(date);
    }

}
