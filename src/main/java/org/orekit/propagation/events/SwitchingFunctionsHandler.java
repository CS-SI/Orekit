/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.ConvergenceException;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class handles several {@link OrekitSwitchingFunction switching
 * functions} during integration.
 *
 * <p>This class is heavily based on the class with the same name from the
 * Apache commons-math library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 * @see OrekitSwitchingFunction
 * @version $Revision$ $Date$
 */

public class SwitchingFunctionsHandler implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 6280340975051401661L;

    /** Switching functions. */
    private List<SwitchState> functions;

    /** First active switching function. */
    private SwitchState first;

    /** Initialization indicator. */
    private boolean initialized;

    /** Simple constructor.
     * Create an empty handler
     */
    public SwitchingFunctionsHandler() {
        functions   = new ArrayList<SwitchState>();
        first       = null;
        initialized = false;
    }

    /** Add a switching function.
     * @param function switching function
     * @see #getSwitchingFunctions()
     * @see #clearSwitchingFunctions()
     */
    public void addSwitchingFunction(OrekitSwitchingFunction function) {
        functions.add(new SwitchState(function));
    }

    /** Get all the switching functions that have been added to the handler.
     * @return an unmodifiable collection of the added switching functions
     * @see #addOrekitSwitchingFunction(OrekitSwitchingFunction, double, double, int)
     * @see #clearSwitchingFunctions()
     */
    public Collection<SwitchState> getSwitchingFunctions() {
        return Collections.unmodifiableCollection(functions);
    }

    /** Remove all the switching functions that have been added to the handler.
     * @see #addOrekitSwitchingFunction(OrekitSwitchingFunction, double, double, int)
     * @see #getSwitchingFunctions()
     */
    public void clearSwitchingFunctions() {
        functions.clear();
    }

    /** Check if the handler does not have any condition.
     * @return true if handler is empty
     */
    public boolean isEmpty() {
        return functions.isEmpty();
    }

    /** Evaluate the impact of the proposed step on all handled
     * switching functions.
     * @param interpolator step interpolator for the proposed step
     * @return true if at least one switching function triggers an event
     * before the end of the proposed step (this implies the step should
     * be rejected)
     * @exception OrekitException if the interpolator fails to
     * compute the function somewhere within the step
     * @exception ConvergenceException if an event cannot be located
     */
    public boolean evaluateStep(OrekitStepInterpolator interpolator)
        throws OrekitException, ConvergenceException {

        first = null;
        if (functions.isEmpty()) {
            // there is nothing to do, return now to avoid setting the
            // interpolator time (and hence avoid unneeded calls to the
            // user function due to interpolator finalization)
            return false;
        }

        if (! initialized) {

            // initialize the switching functions
            AbsoluteDate t0 = interpolator.getPreviousDate();
            interpolator.setInterpolatedDate(t0);
            SpacecraftState y = interpolator.getInterpolatedState();
            for (final SwitchState functionState : functions) {
                functionState.reinitializeBegin(y);
            }

            initialized = true;

        }

        // check events occurrence
        for (final SwitchState functionState : functions) {

            if (functionState.evaluateStep(interpolator)) {
                if (first == null) {
                    first = functionState;
                } else {
                    if (interpolator.isForward()) {
                        if (functionState.getEventTime().compareTo(first.getEventTime()) < 0) {
                            first = functionState;
                        }
                    } else {
                        if (functionState.getEventTime().compareTo(first.getEventTime()) > 0) {
                            first = functionState;
                        }
                    }
                }
            }

        }

        return first != null;

    }

    /** Get the occurrence time of the first event triggered in the
     * last evaluated step.
     * @return occurrence time of the first event triggered in the last
     * evaluated step, or null if no event is
     * triggered
     */
    public AbsoluteDate getEventTime() {
        return (first == null) ? null : first.getEventTime();
    }

    /** Inform the switching functions that the step has been accepted
     * by the integrator.
     * @param state state value at the end of the step
     * @exception OrekitException if the value of one of the
     * switching functions cannot be evaluated
     */
    public void stepAccepted(SpacecraftState state)
        throws OrekitException {
        for (final SwitchState functionState : functions) {
            functionState.stepAccepted(state);
        }
    }

    /** Check if the integration should be stopped at the end of the
     * current step.
     * @return true if the integration should be stopped
     */
    public boolean stop() {
        for (final SwitchState functionState : functions) {
            if (functionState.stop()) {
                return true;
            }
        }
        return false;
    }

    /** Let the switching functions reset the state if they want.
     * @param state state value at the beginning of the next step
     * @return true if the integrator should reset the derivatives too
     * @exception OrekitException if one of the switching functions
     * that should reset the state fails to do it
     */
    public boolean reset(SpacecraftState state)
        throws OrekitException {
        boolean resetDerivatives = false;
        for (final SwitchState functionState : functions) {
            if (functionState.reset(state)) {
                resetDerivatives = true;
            }
        }
        return resetDerivatives;
    }

}
