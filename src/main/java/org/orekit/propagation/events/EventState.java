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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.AllowedSolution;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** This class handles the state for one {@link EventDetector
 * event detector} during integration steps.
 *
 * <p>This class is heavily based on the class with the same name from the
 * Apache Commons Math library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 * <p>Each time the propagator proposes a step, the event detector
 * should be checked. This class handles the state of one detector
 * during one propagation step, with references to the state at the
 * end of the preceding step. This information is used to determine if
 * the detector should trigger an event or not during the proposed
 * step (and hence the step should be reduced to ensure the event
 * occurs at a bound rather than inside the step).</p>
 * @author Luc Maisonobe
 * @param <T> class type for the generic version
 */
public class EventState<T extends EventDetector> implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 4489391420715269318L;

    /** Event detector. */
    private T detector;

    /** Time of the previous call to g. */
    private AbsoluteDate lastT;

    /** Value from the previous call to g. */
    private double lastG;

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
    private EventHandler.Action nextAction;

    /** Simple constructor.
     * @param detector monitored event detector
     */
    public EventState(final T detector) {
        this.detector     = detector;

        // some dummy values ...
        t0                = null;
        g0                = Double.NaN;
        g0Positive        = true;
        pendingEvent      = false;
        pendingEventTime  = null;
        previousEventTime = null;
        increasing        = true;
        nextAction        = EventHandler.Action.CONTINUE;

    }

    /** Get the underlying event detector.
     * @return underlying event detector
     */
    public T getEventDetector() {
        return detector;
    }

    /** Initialize event handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It
     * may be used by the event handler to initialize some internal data
     * if needed.
     * </p>
     * @param s0 initial state
     * @param t target time for the integration
     */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        detector.init(s0, t);
        lastT = AbsoluteDate.PAST_INFINITY;
        lastG = Double.NaN;
    }

    /** Compute the value of the switching function.
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    private double g(final SpacecraftState s) throws OrekitException {
        if (!s.getDate().equals(lastT)) {
            lastT = s.getDate();
            lastG = detector.g(s);
        }
        return lastG;
    }

    /** Reinitialize the beginning of the step.
     * @param state0 state value at the beginning of the step
     * @param isForward if true, step will be forward
     * @exception OrekitException if the event detector
     * value cannot be evaluated at the beginning of the step
     */
    public void reinitializeBegin(final SpacecraftState state0, final boolean isForward)
        throws OrekitException {
        this.t0 = state0.getDate();
        g0 = g(state0);
        if (g0 == 0) {
            // extremely rare case: there is a zero EXACTLY at interval start
            // we will use the sign slightly after step beginning to force ignoring this zero
            g0 = g(state0.shiftedBy((isForward ? 0.5 : -0.5) * detector.getThreshold()));
        }
        g0Positive = g0 >= 0;
    }

    /** Evaluate the impact of the proposed step on the event detector.
     * @param interpolator step interpolator for the proposed step
     * @return true if the event detector triggers an event before
     * the end of the proposed step (this implies the step should be
     * rejected)
     * @exception OrekitException if the switching function
     * cannot be evaluated
     * @exception TooManyEvaluationsException if an event cannot be located
     * @exception NoBracketingException if bracketing cannot be performed
     */
    public boolean evaluateStep(final OrekitStepInterpolator interpolator)
        throws OrekitException, TooManyEvaluationsException, NoBracketingException {

        try {

            final double convergence    = detector.getThreshold();
            final int maxIterationcount = detector.getMaxIterationCount();
            if (forward ^ interpolator.isForward()) {
                forward = !forward;
                pendingEvent      = false;
                pendingEventTime  = null;
                previousEventTime = null;
            }
            final AbsoluteDate t1 = interpolator.getCurrentDate();
            final double dt = t1.durationFrom(t0);
            if (FastMath.abs(dt) < convergence) {
                // we cannot do anything on such a small step, don't trigger any events
                return false;
            }
            final int    n = FastMath.max(1, (int) FastMath.ceil(FastMath.abs(dt) / detector.getMaxCheckInterval()));
            final double h = dt / n;

            final UnivariateFunction f = new UnivariateFunction() {
                public double value(final double t) throws LocalWrapperException {
                    try {
                        interpolator.setInterpolatedDate(t0.shiftedBy(t));
                        return g(interpolator.getInterpolatedState());
                    } catch (OrekitException oe) {
                        throw new LocalWrapperException(oe);
                    }
                }
            };

            final BracketingNthOrderBrentSolver solver =
                    new BracketingNthOrderBrentSolver(convergence, 5);

            AbsoluteDate ta = t0;
            double ga = g0;
            for (int i = 0; i < n; ++i) {

                // evaluate detector value at the end of the substep
                final AbsoluteDate tb = t0.shiftedBy((i + 1) * h);
                interpolator.setInterpolatedDate(tb);
                final double gb = g(interpolator.getInterpolatedState());

                // check events occurrence
                if (g0Positive ^ (gb >= 0)) {
                    // there is a sign change: an event is expected during this step

                    // variation direction, with respect to the integration direction
                    increasing = gb >= ga;

                    // find the event time making sure we select a solution just at or past the exact root
                    final double dtA = ta.durationFrom(t0);
                    final double dtB = tb.durationFrom(t0);
                    final double dtRoot = forward ?
                                          solver.solve(maxIterationcount, f, dtA, dtB, AllowedSolution.RIGHT_SIDE) :
                                          solver.solve(maxIterationcount, f, dtB, dtA, AllowedSolution.LEFT_SIDE);
                    final AbsoluteDate root = t0.shiftedBy(dtRoot);

                    if ((previousEventTime != null) &&
                        (FastMath.abs(root.durationFrom(ta)) <= convergence) &&
                        (FastMath.abs(root.durationFrom(previousEventTime)) <= convergence)) {
                        // we have either found nothing or found (again ?) a past event,
                        // retry the substep excluding this value, and taking care to have the
                        // required sign in case the g function is noisy around its zero and
                        // crosses the axis several times
                        do {
                            ta = forward ? ta.shiftedBy(convergence) : ta.shiftedBy(-convergence);
                            ga = f.value(ta.durationFrom(t0));
                        } while ((g0Positive ^ (ga >= 0)) && (forward ^ (ta.compareTo(tb) >= 0)));

                        if (forward ^ (ta.compareTo(tb) >= 0)) {
                            // we were able to skip this spurious root
                            --i;
                        } else {
                            // we can't avoid this root before the end of the step,
                            // we have to handle it despite it is close to the former one
                            // maybe we have two very close roots
                            pendingEventTime = root;
                            pendingEvent = true;
                            return true;
                        }

                    } else if ((previousEventTime == null) ||
                               (FastMath.abs(previousEventTime.durationFrom(root)) > convergence)) {
                        pendingEventTime = root;
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

        } catch (LocalWrapperException lwe) {
            throw lwe.getWrappedException();
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
        g0 = g(state);

        if (pendingEvent) {
            // force the sign to its value "just after the event"
            previousEventTime = state.getDate();
            g0Positive        = increasing;
            nextAction        = detector.eventOccurred(state, !(increasing ^ forward));
        } else {
            g0Positive = g0 >= 0;
            nextAction = EventHandler.Action.CONTINUE;
        }
    }

    /** Check if the propagation should be stopped at the end of the
     * current step.
     * @return true if the propagation should be stopped
     */
    public boolean stop() {
        return nextAction == EventHandler.Action.STOP;
    }

    /** Let the event detector reset the state if it wants.
     * @param oldState value of the state vector at the beginning of the next step
     * @return new state (null if no reset is needed)
     * @exception OrekitException if the state cannot be reset by the event
     * detector
     */
    public SpacecraftState reset(final SpacecraftState oldState)
        throws OrekitException {

        if (!pendingEvent) {
            return null;
        }

        final SpacecraftState newState;
        if (nextAction != EventHandler.Action.RESET_STATE) {
            newState = null;
        } else {
            newState = detector.resetState(oldState);
        }

        pendingEvent      = false;
        pendingEventTime  = null;

        return newState;

    }

    /** Local runtime exception wrapping OrekitException. */
    private static class LocalWrapperException extends RuntimeException {

        /** Serializable UID. */
        private static final long serialVersionUID = 2734331164409224983L;

        /** Wrapped exception. */
        private final OrekitException wrappedException;

        /** Simple constructor.
         * @param wrapped wrapped exception
         */
        LocalWrapperException(final OrekitException wrapped) {
            this.wrappedException = wrapped;
        }

        /** Get the wrapped exception.
         * @return wrapped exception
         */
        public OrekitException getWrappedException() {
            return wrappedException;
        }

    }

}
