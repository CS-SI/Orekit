package org.orekit.propagation.numerical;

import org.orekit.attitudes.AttitudeLaw;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Common interface for all propagator mode handlers initialization
 * 
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface ModeHandler {

    /** Initialize the mode handler.
     * @param reference reference date
     * @param frame reference frame
     * @param mu central body attraction coefficient
     * @param attitudeLaw attitude law
     */
    void initialize(AbsoluteDate reference, Frame frame, double mu, AttitudeLaw attitudeLaw);

}