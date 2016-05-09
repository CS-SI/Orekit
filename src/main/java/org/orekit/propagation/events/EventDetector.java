/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.time.AbsoluteDate;

/** This interface represents space-dynamics aware events detectors.
 *
 * <p>It mirrors the {@link org.hipparchus.ode.events.ODEEventHandler
 * ODEEventHandler} interface from <a href="https://hipparchus.org/">
 * Hipparchus</a> but provides a space-dynamics interface to the
 * methods.</p>
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
 */
public interface EventDetector extends Serializable {

    /** Initialize event handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It
     * may be used by the event handler to initialize some internal data
     * if needed.
     * </p>
     * <p>
     * The default implementation does nothing
     * </p>
     * @param s0 initial state
     * @param t target time for the integration
     */
    default void init(SpacecraftState s0, AbsoluteDate t) {
        // nothing by default
    }

    /** Compute the value of the switching function.
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    double g(SpacecraftState s) throws OrekitException;

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

    /** Handle the event.
     * @param s SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @return the Action that the calling detector should pass back to the evaluation system
     * @exception OrekitException if some specific error occurs
     * @since 7.0
     */
    Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException;

    /** Reset the state prior to continue propagation.
     * <p>This method is called after the step handler has returned and
     * before the next step is started, but only when {@link
     * #eventOccurred} has itself returned the {@link Action#RESET_STATE}
     * indicator. It allows the user to reset the state for the next step,
     * without perturbing the step handler of the finishing step. If the
     * {@link #eventOccurred} never returns the {@link Action#RESET_STATE}
     * indicator, this function will never be called, and it is safe to simply return null.</p>
     * <p>
     * The default implementation simply returns its argument.
     * </p>
     * @param oldState old state
     * @return new state
     * @exception OrekitException if the state cannot be reseted
     * @since 7.0
     */
    default SpacecraftState resetState(SpacecraftState oldState) throws OrekitException {
        return oldState;
    }

}
