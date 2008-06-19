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
package org.orekit.propagation.events;

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
 * <p>Switching functions are a useful solution to meet the requirements
 * of integrators concerning discontinuities problems. The value of the
 * switching function is asked by the integrator at each step. When the
 * value of the g function changes of sign, the step is rejected and reduced,
 * until the roots of the function is reached with the {@link #getThreshold()}
 * precision. </p>
 *
 * <p> Once the g function root is reached, we are sure the integrator
 *  won't miss the event relative to this date: a discontinuity in
 *  acceleration, a change in the state ... This event can be initiated
 *  by the {@link #eventOccurred(SpacecraftState)}
 *  method, which is called when the step is placed on the wanted date. <p>
 *
 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public interface OrekitSwitchingFunction extends Serializable {

    /** Stop indicator.
     * <p>This value should be used as the return value of the {@link
     * #eventOccurred eventOccurred} method when the propagation should be
     * stopped after the event ending the current step.</p>
     */
    int STOP = 0;

    /** Reset state indicator.
     * <p>This value should be used as the return value of the {@link
     * #eventOccurred eventOccurred} method when the propagation should
     * go on after the event ending the current step, with a new state
     * (which will be retrieved thanks to the {@link #resetState
     * resetState} method).</p>
     */
    int RESET_STATE = 1;

    /** Reset derivatives indicator.
     * <p>This value should be used as the return value of the {@link
     * #eventOccurred eventOccurred} method when the propagation should
     * go on after the event ending the current step, with recomputed
     * derivatives vector.</p>
     */
    int RESET_DERIVATIVES = 2;

    /** Continue indicator.
     * <p>This value should be used as the return value of the {@link
     * #eventOccurred eventOccurred} method when the propagation should go
     * on after the event ending the current step.</p>
     */
    int CONTINUE = 3;

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
     * @return one of {@link #STOP}, {@link #RESET_STATE}, {@link #RESET_DERIVATIVES}
     * or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    int eventOccurred(SpacecraftState s) throws OrekitException;

    /** Reset the state prior to continue propagation.
     * <p>This method is called after the step handler has returned and
     * before the next step is started, but only when {@link
     * #eventOccurred} has itself returned the {@link #RESET_STATE}
     * indicator. It allows the user to reset the state for the next step,
     * without perturbing the step handler of the finishing step. If the
     * {@link #eventOccurred} never returns the {@link #RESET_STATE} indicator,
     * this function will never be called, and it is safe to simply return null.</p>
     * @param oldState old state
     * @return new state
     * @exception OrekitException if the state cannot be reseted
     */
    SpacecraftState resetState(SpacecraftState oldState) throws OrekitException;

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
