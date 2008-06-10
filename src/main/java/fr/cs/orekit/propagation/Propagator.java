/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * @author Mathieu Roméro
 * @author Luc Maisonobe
 *
 * @version $Revision$ $Date$
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
