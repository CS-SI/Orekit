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
package org.orekit.propagation.numerical.forces;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.OrekitSwitchingFunction;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.TimeDerivativesEquations;


/** This interface represents a force model set.
 *
 * <p>It should be implemented by all real force models before they
 * can be taken into account by the orbit extrapolation methods.</p>
 *
 * <p>For real problems, and according to the kind of forces we want to
 * represent (gravitational or non-gravitational perturbations), the
 * contribution of the perturbing acceleration is added like a disturbing term
 * in the partial derivatives coming from the Gauss equations or the Lagrange's
 * planetary equations.</p>
 *
 * @author Mathieu Roméro
 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */

public interface ForceModel extends Serializable {

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @exception OrekitException if some specific error occurs
     */
    void addContribution(SpacecraftState s, TimeDerivativesEquations adder)
        throws OrekitException;

    /** Get the switching functions internally used by the model itself.
     * @return array of switching functions or null if the model doesn't need
     * any switching function by itself
     */
    OrekitSwitchingFunction[] getSwitchingFunctions();

}
