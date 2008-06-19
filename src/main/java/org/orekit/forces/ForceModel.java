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
package org.orekit.forces;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.OrekitSwitchingFunction;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.TimeDerivativesEquations;

/** This interface represents a force modifying spacecraft motion.
 *
 * <p>Objects implementing this interface are intended to be added to a
 * {@link NumericalPropagator numerical propagator}  before the propagation is started.
 * The propagator will call at each step the {@link #addContribution(SpacecraftState,
 * TimeDerivativesEquations)} method. The force model instance will extract all the
 * state data it needs (date,position, velocity, frame, attitude, mass) from the first
 * parameter. From these state data, it will compute the perturbing acceleration. It
 * will then add this acceleration to the second parameter which will take thins
 * contribution into account and will use the Gauss equations to evaluate its impact
 * on the global state derivative.</p>
 *
 * <p>Force models which create discontinuous acceleration patters (typically for maneuvers
 * start/stop or solar eclipses entry/exit) must use one or more {@link
 * org.orekit.propagation.numerical.OrekitSwitchingFunction switching functions} to the
 * propagator thanks to the {@link #getSwitchingFunctions()} method which is called once
 * just before propagation starts. The switching functions will be checked by the propagator
 * to ensure accurate propagation and switch event crossing.</p>
 *
 * @author Mathieu Roméro
 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public interface ForceModel extends Serializable {

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param s current state information: date, kinematics, attitude
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
