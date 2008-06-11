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
package org.orekit.propagation.numerical;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;


/** This interface represents space-dynamics aware switching functions.
 *
 * <p>It mirrors the {@link org.apache.commons.math.ode.SwitchingFunction
 * SwitchingFunction} interface from <a href="http://commons.apache.org/math/">
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 *
 * <p>It should be implemented by all real force models before they
 * can be taken into account by the orbit extrapolation methods.</p>
 *
 * <p>  Switching functions are a useful solution to meet the requirements
 * of integrators concerning discontinuities problems. The value of the
 * switching function is asked by the integrator at each step. When the
 * value of the g function changes of sign, the step is rejected and reduced,
 * until the roots of the function is reached with the {@link #getThreshold()}
 * precision. </p>
 *
 * <p> Once the g function root is reached, we are sure the integrator
 *  won't miss the event relative to this date : a discontinuity in
 *  acceleration, a change in the state... This event can be initiated
 *  by the {@link #eventOccurred(SpacecraftState)}
 *  method, wich is called when the step is placed on the wanted date. <p>
 *
 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public interface OrekitSwitchingFunction extends Serializable {

    /** Compute the value of the switching function.
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    double g(SpacecraftState s) throws OrekitException;

    /** Handle an event and choose what to do next.
     * @param s the current state information : date, cinematics, attitude
     * @exception OrekitException if some specific error occurs
     */
    void eventOccurred(SpacecraftState s) throws OrekitException;

    /** Get the convergence threshold in the event time search.
     * @return convergence threshold
     */
    double getThreshold();

    /** Get maximal time interval between switching function checks.
     * @return maximal time interval between switching function checks
     */
    double getMaxCheckInterval();

    /** Get maximal number of iterations in the event time search.
     * @return maximal number of iterations in the event time search
     */
    int getMaxIterationCount();

}
