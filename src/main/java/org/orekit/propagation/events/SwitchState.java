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

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.BrentSolver;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class handles the state for one {@link OrekitSwitchingFunction
 * switching function} during integration steps.
 *
 * <p>This class is heavily based on the class with the same name from the
 * Apache commons-math library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 * <p>Each time the integrator proposes a step, the switching function
 * should be checked. This class handles the state of one function
 * during one integration step, with references to the state at the
 * end of the preceding step. This information is used to determine if
 * the function should trigger an event or not during the proposed
 * step (and hence the step should be reduced to ensure the event
 * occurs at a bound rather than inside the step).</p>
 *
 * @version $Revision$ $Date$
 */
class SwitchState implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 7498385558553820471L;

    /** Switching function. */
    private OrekitSwitchingFunction function;

    /** Time at the beginning of the step. */
    private AbsoluteDate t0;

    /** Value of the switching function at the beginning of the step. */
    private double g0;

    /** Simulated sign of g0 (we cheat when crossing events). */
    private boolean g0Positive;

    /** Indicator of event expected during the step. */
    private boolean pendingEvent;

    /** Occurrence time of the pending event. */
    private AbsoluteDate pendingEventTime;

    /** Occurrence time of the previous event. */
    private AbsoluteDate previousEventTime;

    /** Variation direction around pending event.
     *  (this is considered with respect to the integration direction)
     */
    private boolean increasing;

    /** Next action indicator. */
    private int nextAction;

    /** Simple constructor.
     * @param function switching function
     */
    public SwitchState(final OrekitSwitchingFunction function) {
        this.function     = function;

        // some dummy values ...
        t0                = null;
        g0                = Double.NaN;
        g0Positive        = true;
        pendingEvent      = false;
        pendingEventTime  = null;
        previousEventTime = null;
        increasing        = true;
        nextAction        = OrekitSwitchingFunction.CONTINUE;

    }

    /** Reinitialize the beginning of the step.
     * @param state0 state value at the beginning of the step
     * @exception OrekitException if the switching function
     * value cannot be evaluated at the beginning of the step
     */
    public void reinitializeBegin(final SpacecraftState state0)
        throws OrekitException {
        this.t0 = state0.getDate();
        g0 = function.g(state0);
        g0Positive = g0 >= 0;
    }

    /** Evaluate the impact of the proposed step on the switching function.
     * @param interpolator step interpolator for the proposed step
     * @return true if the switching function triggers an event before
     * the end of the proposed step (this implies the step should be
     * rejected)
     * @exception OrekitException if the switching function
     * cannot be evaluated
     * @exception ConvergenceException if an event cannot be located
     */
    public boolean evaluateStep(final OrekitStepInterpolator interpolator)
        throws OrekitException, ConvergenceException {

        try {

            final AbsoluteDate t1 = interpolator.getCurrentDate();
            final double dt = t1.minus(t0);
            final int    n  = Math.max(1, (int) Math.ceil(Math.abs(dt) / function.getMaxCheckInterval()));
            final double h  = dt / n;

            AbsoluteDate ta = t0;
            double ga = g0;
            final AbsoluteDate start = (t1.compareTo(t0) > 0) ?
                    new AbsoluteDate(t0,  function.getThreshold()) :
                    new AbsoluteDate(t0, -function.getThreshold());
            for (int i = 0; i < n; ++i) {

                // evaluate function value at the end of the substep
                final AbsoluteDate tb = new AbsoluteDate(start, i * h);
                interpolator.setInterpolatedDate(tb);
                final double gb = function.g(interpolator.getInterpolatedState());

                // check events occurrence
                if (g0Positive ^ (gb >= 0)) {
                    // there is a sign change: an event is expected during this step

                    // variation direction, with respect to the integration direction
                    increasing = gb >= ga;

                    final UnivariateRealSolver solver = new BrentSolver(new UnivariateRealFunction() {
                        public double value(final double t) throws FunctionEvaluationException {
                            try {
                                final AbsoluteDate date = new AbsoluteDate(t0, t);
                                interpolator.setInterpolatedDate(date);
                                return function.g(interpolator.getInterpolatedState());
                            } catch (OrekitException e) {
                                throw new FunctionEvaluationException(t, e);
                            }
                        }
                    });
                    solver.setAbsoluteAccuracy(function.getThreshold());
                    solver.setMaximalIterationCount(function.getMaxIterationCount());
                    final AbsoluteDate root = new AbsoluteDate(t0, solver.solve(ta.minus(t0), tb.minus(t0)));
                    if ((previousEventTime == null) ||
                        (Math.abs(previousEventTime.minus(root)) > function.getThreshold())) {
                        pendingEventTime = root;
                        if (pendingEvent && (Math.abs(t1.minus(pendingEventTime)) <= function.getThreshold())) {
                            // we were already waiting for this event which was
                            // found during a previous call for a step that was
                            // rejected, this step must now be accepted since it
                            // properly ends exactly at the event occurrence
                            return false;
                        }
                        // either we were not waiting for the event or it has
                        // moved in such a way the step cannot be accepted
                        pendingEvent = true;
                        return true;
                    }

                } else {
                    // no sign change: there is no event for now
                    ta = tb;
                    ga = gb;
                }

            }

            // no event during the whole step
            pendingEvent     = false;
            pendingEventTime = null;
            return false;

        } catch (FunctionEvaluationException e) {
            final Throwable cause = e.getCause();
            if ((cause != null) && (cause instanceof OrekitException)) {
                throw (OrekitException) cause;
            }
            throw new OrekitException(e.getMessage(), e);
        }

    }

    /** Get the occurrence time of the event triggered in the current
     * step.
     * @return occurrence time of the event triggered in the current
     * step.
     */
    public AbsoluteDate getEventTime() {
        return pendingEventTime;
    }

    /** Acknowledge the fact the step has been accepted by the integrator.
     * @param state value of the state vector at the end of the step
     * @exception OrekitException if the value of the switching
     * function cannot be evaluated
     */
    public void stepAccepted(final SpacecraftState state)
        throws OrekitException {

        t0 = state.getDate();
        g0 = function.g(state);

        if (pendingEvent) {
            // force the sign to its value "just after the event"
            previousEventTime = state.getDate();
            g0Positive        = increasing;
            nextAction        = function.eventOccurred(state);
        } else {
            g0Positive = g0 >= 0;
            nextAction = OrekitSwitchingFunction.CONTINUE;
        }
    }

    /** Check if the integration should be stopped at the end of the
     * current step.
     * @return true if the integration should be stopped
     */
    public boolean stop() {
        return nextAction == OrekitSwitchingFunction.STOP;
    }

    /** Let the switching function reset the state if it wants.
     * @param state value of the state vector at the beginning of the next step
     * @return true if the integrator should reset the derivatives too
     * @exception OrekitException if the state cannot be reseted by the switching
     * function
     */
    public boolean reset(final SpacecraftState state)
        throws OrekitException {

        if (!pendingEvent) {
            return false;
        }

        if (nextAction == OrekitSwitchingFunction.RESET_STATE) {
            function.resetState(state);
        }
        pendingEvent      = false;
        pendingEventTime  = null;

        return (nextAction == OrekitSwitchingFunction.RESET_STATE) ||
               (nextAction == OrekitSwitchingFunction.RESET_DERIVATIVES);

    }

}
