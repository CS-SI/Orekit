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
package org.orekit.propagation;

import java.io.Serializable;

import org.orekit.errors.PropagationException;
import org.orekit.time.AbsoluteDate;


/** This interface provides a way to propagate an orbit at any time.
 *
 * <p>This interface is the top-level abstraction for orbit propagation.
 * It is implemented by analytical models which have no time limit,
 * by orbit readers based on external data files, by numerical integrators
 * using rich force models and by continuous models built after numerical
 * integration has been completed and dense output data as been
 * gathered.</p>

 * @author Mathieu Roméro
 * @author Luc Maisonobe
 *
 * @version $Revision$ $Date$
 */

public interface Propagator extends Serializable {

    /** Get the spacecraft state at a specific date.
     * @param date desired date for the orbit
     * @return the spacecraft state at the specified date
     * @exception PropagationException if state cannot be propagated
     */
    SpacecraftState propagate(AbsoluteDate date)
        throws PropagationException;

}
