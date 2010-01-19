/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

/** This interface represents space-dynamics aware events detectors.
 *
 * <p>It mirrors the {@link org.apache.commons.math.ode.events.EventHandler
 * EventHandler} interface from <a href="http://commons.apache.org/math/">
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 *
 * <p>Events detectors are a useful solution to meet the requirements
 * of propagators concerning discrete conditions. The state of each
 * event detector is queried by the integrator at each step. When the
 * sign of the underlying g switching function changes, the step is rejected
 * and reduced, in order to make sure the sign changes occur only at steps
 * boundaries.</p>
 *
 * <p>When step ends exactly at a switching function sign change, the corresponding
 * event is triggered, by calling the {@link #eventOccurred(SpacecraftState, boolean)}
 * method. The method can do whatever it needs with the event (logging it, performing
 * some processing, ignore it ...). The return value of the method will be used by
 * the propagator to stop or resume propagation, possibly changing the state vector.<p>
 *
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public interface EventDetector extends Serializable {

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

     * <p>The scheduling between this method and the {@link
     * org.orekit.propagation.sampling.OrekitStepHandler OrekitStepHandler} method {@link
     * org.orekit.propagation.sampling.OrekitStepHandler#handleStep(
     * org.orekit.propagation.sampling.OrekitStepInterpolator, boolean)
     * handleStep(interpolator, isLast)} is to call this method first and
     * <code>handleStep</code> afterwards. This scheduling allows the propagator to
     * pass <code>true</code> as the <code>isLast</code> parameter to the step
     * handler to make it aware the step will be the last one if this method
     * returns {@link #STOP}. As the interpolator may be used to navigate back
     * throughout the last step (as {@link
     * org.orekit.propagation.sampling.OrekitStepNormalizer OrekitStepNormalizer}
     * does for example), user code called by this method and user
     * code called by step handlers may experience apparently out of order values
     * of the independent time variable. As an example, if the same user object
     * implements both this {@link EventDetector EventDetector} interface and the
     * {@link org.orekit.propagation.sampling.OrekitFixedStepHandler OrekitFixedStepHandler}
     * interface, a <em>forward</em> integration may call its
     * <code>eventOccurred</code> method with a state at 2000-01-01T00:00:10 first
     * and call its <code>handleStep</code> method with a state at 2000-01-01T00:00:09
     * afterwards. Such out of order calls are limited to the size of the
     * integration step for {@link
     * org.orekit.propagation.sampling.OrekitStepHandler variable step handlers} and
     * to the size of the fixed step for {@link
     * org.orekit.propagation.sampling.OrekitFixedStepHandler fixed step handlers}.</p>

     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event (note that increase is measured with respect
     * to physical time, not with respect to propagation which may go backward in time)
     * @return one of {@link #STOP}, {@link #RESET_STATE}, {@link #RESET_DERIVATIVES}
     * or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException;

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
     * @return convergence threshold (s)
     */
    double getThreshold();

    /** Get maximal time interval between switching function checks.
     * @return maximal time interval (s) between switching function checks
     */
    double getMaxCheckInterval();

    /** Get maximal number of iterations in the event time search.
     * @return maximal number of iterations in the event time search
     */
    int getMaxIterationCount();

}
