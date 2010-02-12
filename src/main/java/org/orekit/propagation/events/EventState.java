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
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.BrentSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** This class handles the state for one {@link EventDetector
 * event detector} during integration steps.
 *
 * <p>This class is heavily based on the class with the same name from the
 * Apache commons-math library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 * <p>Each time the propagator proposes a step, the event detector
 * should be checked. This class handles the state of one detector
 * during one propagation step, with references to the state at the
 * end of the preceding step. This information is used to determine if
 * the detector should trigger an event or not during the proposed
 * step (and hence the step should be reduced to ensure the event
 * occurs at a bound rather than inside the step).</p>
 *
 * @version $Revision$ $Date$
 */
class EventState implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 7498385558553820471L;

    /** Event detector. */
    private EventDetector detector;

    /** Time at the beginning of the step. */
    private AbsoluteDate t0;

    /** Value of the event detector at the beginning of the step. */
    private double g0;

    /** Simulated sign of g0 (we cheat when crossing events). */
    private boolean g0Positive;

    /** Indicator of event expected during the step. */
    private boolean pendingEvent;

    /** Occurrence time of the pending event. */
    private AbsoluteDate pendingEventTime;

    /** Occurrence time of the previous event. */
    private AbsoluteDate previousEventTime;

    /** Integration direction. */
    private boolean forward;

    /** Variation direction around pending event.
     *  (this is considered with respect to the integration direction)
     */
    private boolean increasing;

    /** Next action indicator. */
    private int nextAction;

    /** Simple constructor.
     * @param detector monitored event detector
     */
    public EventState(final EventDetector detector) {
        this.detector     = detector;

        // some dummy values ...
        t0                = null;
        g0                = Double.NaN;
        g0Positive        = true;
        pendingEvent      = false;
        pendingEventTime  = null;
        previousEventTime = null;
        increasing        = true;
        nextAction        = EventDetector.CONTINUE;

    }

    /** Get the underlying event detector.
     * @return underlying event detector
     */
    public EventDetector getEventDetector() {
        return detector;
    }

    /** Reinitialize the beginning of the step.
     * @param state0 state value at the beginning of the step
     * @exception OrekitException if the event detector
     * value cannot be evaluated at the beginning of the step
     */
    public void reinitializeBegin(final SpacecraftState state0)
        throws OrekitException {
        this.t0 = state0.getDate();
        g0 = detector.g(state0);
        g0Positive = g0 >= 0;
    }

    /** Evaluate the impact of the proposed step on the event detector.
     * @param interpolator step interpolator for the proposed step
     * @return true if the event detector triggers an event before
     * the end of the proposed step (this implies the step should be
     * rejected)
     * @exception OrekitException if the switching function
     * cannot be evaluated
     * @exception ConvergenceException if an event cannot be located
     */
    public boolean evaluateStep(final OrekitStepInterpolator interpolator)
        throws OrekitException, ConvergenceException {

        try {

            forward = interpolator.isForward();
            final AbsoluteDate t1 = interpolator.getCurrentDate();
            final double dt = t1.durationFrom(t0);
            final int    n  = Math.max(1, (int) Math.ceil(Math.abs(dt) / detector.getMaxCheckInterval()));
            final double h  = dt / n;

            AbsoluteDate ta = t0;
            double ga = g0;
            final AbsoluteDate start = (t1.compareTo(t0) > 0) ?
                    t0.shiftedBy(detector.getThreshold()) :
                    t0.shiftedBy(-detector.getThreshold());
            for (int i = 0; i < n; ++i) {

                // evaluate detector value at the end of the substep
                final AbsoluteDate tb = start.shiftedBy((i + 1) * h);
                interpolator.setInterpolatedDate(tb);
                final double gb = detector.g(interpolator.getInterpolatedState());

                // check events occurrence
                if (g0Positive ^ (gb >= 0)) {
                    // there is a sign change: an event is expected during this step

                    // variation direction, with respect to the integration direction
                    increasing = gb >= ga;

                    final UnivariateRealFunction f = new UnivariateRealFunction() {
                        private static final long serialVersionUID = 642356050167522213L;
                        public double value(final double t) throws FunctionEvaluationException {
                            try {
                                final AbsoluteDate date = t0.shiftedBy(t);
                                interpolator.setInterpolatedDate(date);
                                return detector.g(interpolator.getInterpolatedState());
                            } catch (OrekitException e) {
                                throw new FunctionEvaluationException(e, t);
                            }
                        }
                    };
                    final UnivariateRealSolver solver = new BrentSolver();
                    solver.setAbsoluteAccuracy(detector.getThreshold());
                    solver.setMaximalIterationCount(detector.getMaxIterationCount());

                    double dtA = ta.durationFrom(t0);
                    final double dtB = tb.durationFrom(t0);
                    if (ga * gb > 0) {
                        // this is a corner case:
                        // - there was an event near ta,
                        // - there is another event between ta and tb
                        // - when ta was computed, convergence was reached on the "wrong side" of the interval
                        // this implies that the real sign of ga is the same as gb, so we need to slightly
                        // shift ta to make sure ga and gb get opposite signs and the solver won't complain
                        // about bracketing
                        final double epsilon = (forward ? 0.25 : -0.25) * detector.getThreshold();
                        for (int k = 0; (k < 4) && (ga * gb > 0); ++k) {
                            dtA += epsilon;
                            ga = f.value(dtA);
                        }
                        if (ga * gb > 0) {
                            // this should never happen
                            throw OrekitException.createInternalError(null);
                        }
                    }

                    final double dtRoot = (dtA <= dtB) ? solver.solve(f, dtA, dtB) : solver.solve(f, dtB, dtA);
                    final AbsoluteDate root = t0.shiftedBy(dtRoot);

                    if ((previousEventTime != null) &&
                        (Math.abs(root.durationFrom(ta)) <= detector.getThreshold()) &&
                        (Math.abs(root.durationFrom(previousEventTime)) <= detector.getThreshold())) {
                            // we have either found nothing or found (again ?) a past event, we simply ignore it
                        ta = tb;
                        ga = gb;
                    } else if ((previousEventTime == null) ||
                               (Math.abs(previousEventTime.durationFrom(root)) > detector.getThreshold())) {
                        pendingEventTime = root;
                        if (pendingEvent && (Math.abs(t1.durationFrom(pendingEventTime)) <= detector.getThreshold())) {
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
                    } else {
                        // no sign change: there is no event for now
                        ta = tb;
                        ga = gb;
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

    /** Acknowledge the fact the step has been accepted by the propagator.
     * @param state value of the state vector at the end of the step
     * @exception OrekitException if the value of the switching
     * function cannot be evaluated
     */
    public void stepAccepted(final SpacecraftState state)
        throws OrekitException {

        t0 = state.getDate();
        g0 = detector.g(state);

        if (pendingEvent) {
            // force the sign to its value "just after the event"
            previousEventTime = state.getDate();
            g0Positive        = increasing;
            nextAction        = detector.eventOccurred(state, !(increasing ^ forward));
        } else {
            g0Positive = g0 >= 0;
            nextAction = EventDetector.CONTINUE;
        }
    }

    /** Check if the propagation should be stopped at the end of the
     * current step.
     * @return true if the propagation should be stopped
     */
    public boolean stop() {
        return nextAction == EventDetector.STOP;
    }

    /** Let the event detector reset the state if it wants.
     * @param oldState value of the state vector at the beginning of the next step
     * @return new state (oldState of no reset is needed)
     * @exception OrekitException if the state cannot be reset by the event
     * detector
     */
    public SpacecraftState reset(final SpacecraftState oldState)
        throws OrekitException {

        if (!pendingEvent) {
            return oldState;
        }

        final SpacecraftState newState =
            (nextAction == EventDetector.RESET_STATE) ?
            detector.resetState(oldState) : oldState;
        pendingEvent      = false;
        pendingEventTime  = null;

        return newState;

    }

}
