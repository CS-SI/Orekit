package fr.cs.orekit.propagation;

import java.io.Serializable;

import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.time.AbsoluteDate;

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
 * @author M. Romero
 * @author L. Maisonobe
 *
 */

public interface Propagator extends Serializable {

    /** Get the orbit at a specific date.
     * @param date desired date for the orbit
     * @return the orbit at the specified date
     * @exception PropagationException if state cannot be extrapolated
     */
    SpacecraftState propagate(AbsoluteDate date)
        throws PropagationException;

}
