package fr.cs.aerospace.orekit.extrapolation;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Orbit;

/** This interface provides a continuous ephemeris.
 *
 * <p>This interface provides a mean to retrieve orbital parameters at
 * any time. It should be implemented by analytical
 * models which have no time limit, by orbit readers based on external
 * data files and by continuous models built after numerical
 * integration has been completed and dense output data as been
 * gathered.</p>

 * <p>This interface is typically used by algorithms that need to go
 * back and forth in time to look for special conditions (convergence
 * status, constraints limits, parameter values ...).</p>

 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 *
 */

public interface Ephemeris {
    
    /** Get the orbit at a specific date.
     * @param date desired date for the orbit
     * @param orbit placeholder where to put the orbit (may be null)
     * @return the orbit at the specified date (it is either the specified
     * instance or a newly allocated object)
     */    
    public Orbit getOrbit(RDate date, Orbit orbit)
      throws ExtrapolationException;

}
