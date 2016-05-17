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

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketedUnivariateSolver;
import org.hipparchus.analysis.solvers.BracketedUnivariateSolver.Interval;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitInternalError;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

import java.io.Serializable;

/** This class handles the state for one {@link EventDetector
 * event detector} during integration steps.
 *
 * <p>This class is heavily based on the class with the same name from the
 * Hipparchus library. The changes performed consist in replacing
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

    /**
     * Time to stop propagation if the event is a stop event. Used to enable stopping at
     * an event and then restarting after that event.
     */
    private AbsoluteDate stopTime;

    /** Time after the current event. */
    private AbsoluteDate afterEvent;

    /** Value of the g function after the current event. */
    private double afterG;

    /** The earliest time considered for events. */
    private AbsoluteDate earliestTimeConsidered;

    /** Integration direction. */
    private boolean forward;

    /** Variation direction around pending event.
     *  (this is considered with respect to the integration direction)
     */
    private boolean increasing;

    /** Simple constructor.
     * @param detector monitored event detector
     */
    public EventState(final T detector) {
        this.detector     = detector;

        // some dummy values ...
        lastT                  = AbsoluteDate.PAST_INFINITY;
        lastG                  = Double.NaN;
        t0                     = null;
        g0                     = Double.NaN;
        g0Positive             = true;
        pendingEvent           = false;
        pendingEventTime       = null;
        stopTime               = null;
        increasing             = true;
        earliestTimeConsidered = null;
        afterEvent             = null;
        afterG                 = Double.NaN;

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
     * @param interpolator interpolator valid for the current step
     * @exception OrekitException if the event detector
     * value cannot be evaluated at the beginning of the step
     */
    public void reinitializeBegin(final OrekitStepInterpolator interpolator)
        throws OrekitException {
        forward = interpolator.isForward();
        final SpacecraftState s0 = interpolator.getPreviousState();
        this.t0 = s0.getDate();
        g0 = g(s0);
        while (g0 == 0) {
            // extremely rare case: there is a zero EXACTLY at interval start
            // we will use the sign slightly after step beginning to force ignoring this zero
            // try moving forward by half a convergence interval
            final double dt = (forward ? 0.5 : -0.5) * detector.getThreshold();
            AbsoluteDate startDate = t0.shiftedBy(dt);
            // if convergence is too small move an ulp
            if (t0.equals(startDate)) {
                startDate = nextAfter(startDate);
            }
            t0 = startDate;
            g0 = g(interpolator.getInterpolatedState(t0));
        }
        g0Positive = g0 > 0;
        // "last" event was increasing
        increasing = g0Positive;
    }

    /** Evaluate the impact of the proposed step on the event detector.
     * @param interpolator step interpolator for the proposed step
     * @return true if the event detector triggers an event before
     * the end of the proposed step (this implies the step should be
     * rejected)
     * @exception OrekitException if the switching function
     * cannot be evaluated
     * @exception MathRuntimeException if an event cannot be located
     */
    public boolean evaluateStep(final OrekitStepInterpolator interpolator)
        throws OrekitException, MathRuntimeException {

        forward = interpolator.isForward();
        final SpacecraftState s1 = interpolator.getCurrentState();
        final AbsoluteDate t1 = s1.getDate();
        final double dt = t1.durationFrom(t0);
        if (FastMath.abs(dt) < detector.getThreshold()) {
            // we cannot do anything on such a small step, don't trigger any events
            return false;
        }
        // number of points to check in the current step
        final int n = FastMath.max(1, (int) FastMath.ceil(FastMath.abs(dt) / detector.getMaxCheckInterval()));
        final double h = dt / n;


        AbsoluteDate ta = t0;
        double ga = g0;
        for (int i = 0; i < n; ++i) {

            // evaluate handler value at the end of the substep
            final AbsoluteDate tb = (i == n - 1) ? t1 : t0.shiftedBy((i + 1) * h);
            final double gb = g(interpolator.getInterpolatedState(tb));

            // check events occurrence
            if (gb == 0.0 || (g0Positive ^ (gb > 0))) {
                // there is a sign change: an event is expected during this step
                if (findRoot(interpolator, ta, ga, tb, gb)) {
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

    }

    /**
     * Find a root in a bracketing interval.
     *
     * <p> When calling this method one of the following must be true. Either ga == 0, gb
     * == 0, (ga < 0  and gb > 0), or (ga > 0 and gb < 0).
     *
     * @param interpolator that covers the interval.
     * @param ta           earliest possible time for root.
     * @param ga           g(ta).
     * @param tb           latest possible time for root.
     * @param gb           g(tb).
     * @return if a zero crossing was found.
     * @throws OrekitException if the event detector throws one
     */
    private boolean findRoot(final OrekitStepInterpolator interpolator,
                             final AbsoluteDate ta, final double ga,
                             final AbsoluteDate tb, final double gb)
            throws OrekitException {
        // check there appears to be a root in [ta, tb]
        check(ga == 0.0 || gb == 0.0 || (ga > 0.0 && gb < 0.0) || (ga < 0.0 && gb > 0.0));

        final double convergence = detector.getThreshold();
        final int maxIterationCount = detector.getMaxIterationCount();
        final BracketedUnivariateSolver<UnivariateFunction> solver =
                new BracketingNthOrderBrentSolver(0, convergence, 0, 5);

        // event time, just at or before the actual root.
        AbsoluteDate beforeRootT = null;
        double beforeRootG = Double.NaN;
        // time on the other side of the root.
        // Initialized the the loop below executes once.
        AbsoluteDate afterRootT = ta;
        double afterRootG = 0.0;

        // check for some conditions that the root finders don't like
        // these conditions cannot not happen in the loop below
        // the ga == 0.0 case is handled by the loop below
        if (ta.equals(tb)) {
            // both non-zero but times are the same. Probably due to reset state
            beforeRootT = ta;
            beforeRootG = ga;
            afterRootT = shiftedBy(beforeRootT, convergence);
            afterRootG = g(interpolator.getInterpolatedState(afterRootT));
        } else if (ga != 0.0 && gb == 0.0) {
            // hard: ga != 0.0 and gb == 0.0
            // look past gb by up to convergence to find next sign
            // throw an exception if g(t) = 0.0 in [tb, tb + convergence]
            beforeRootT = tb;
            beforeRootG = gb;
            afterRootT = shiftedBy(beforeRootT, convergence);
            afterRootG = g(interpolator.getInterpolatedState(afterRootT));
        } else if (ga != 0.0) {
            final double newGa = g(interpolator.getInterpolatedState(ta));
            if (ga > 0 != newGa > 0) {
                // both non-zero, step sign change at ta, possibly due to reset state
                beforeRootT = ta;
                beforeRootG = newGa;
                afterRootT = minTime(shiftedBy(beforeRootT, convergence), tb);
                afterRootG = g(interpolator.getInterpolatedState(afterRootT));
            }
        }

        // loop to skip through "fake" roots, i.e. where g(t) = g'(t) = 0.0
        // executed once if we didn't hit a special case above
        AbsoluteDate loopT = ta;
        double loopG = ga;
        while ((afterRootG == 0.0 || afterRootG > 0.0 == g0Positive) &&
                strictlyAfter(afterRootT, tb)) {
            if (loopG == 0.0) {
                // ga == 0.0 and gb may or may not be 0.0
                // handle the root at ta first
                beforeRootT = loopT;
                beforeRootG = loopG;
                afterRootT = minTime(shiftedBy(beforeRootT, convergence), tb);
                afterRootG = g(interpolator.getInterpolatedState(afterRootT));
            } else {
                // both non-zero, the usual case, use a root finder.
                try {
                    // time zero for evaluating the function f. Needs to be final
                    final AbsoluteDate fT0 = loopT;
                    final UnivariateFunction f = dt -> {
                        try {
                            return g(interpolator.getInterpolatedState(fT0.shiftedBy(dt)));
                        } catch (OrekitException oe) {
                            throw new OrekitExceptionWrapper(oe);
                        }
                    };
                    // tb as a double for use in f
                    final double tbDouble = tb.durationFrom(fT0);
                    if (forward) {
                        final Interval interval =
                                solver.solveInterval(maxIterationCount, f, 0, tbDouble);
                        beforeRootT = fT0.shiftedBy(interval.getLeftAbscissa());
                        beforeRootG = interval.getLeftValue();
                        afterRootT = fT0.shiftedBy(interval.getRightAbscissa());
                        afterRootG = interval.getRightValue();
                    } else {
                        final Interval interval =
                                solver.solveInterval(maxIterationCount, f, tbDouble, 0);
                        beforeRootT = fT0.shiftedBy(interval.getRightAbscissa());
                        beforeRootG = interval.getRightValue();
                        afterRootT = fT0.shiftedBy(interval.getLeftAbscissa());
                        afterRootG = interval.getLeftValue();
                    }
                } catch (OrekitExceptionWrapper oew) {
                    throw oew.getException();
                }
            }
            // tolerance is set to less than 1 ulp
            // assume tolerance is 1 ulp
            if (beforeRootT.equals(afterRootT)) {
                afterRootT = nextAfter(afterRootT);
                afterRootG = g(interpolator.getInterpolatedState(afterRootT));
            }
            // check loop is making some progress
            check((forward && afterRootT.compareTo(beforeRootT) > 0) ||
                  (!forward && afterRootT.compareTo(beforeRootT) < 0));
            // setup next iteration
            loopT = afterRootT;
            loopG = afterRootG;
        }

        // figure out the result of root finding, and return accordingly
        if (afterRootG == 0.0 || afterRootG > 0.0 == g0Positive) {
            // loop gave up and didn't find any crossing within this step
            return false;
        } else {
            // real crossing
            check(beforeRootT != null && !Double.isNaN(beforeRootG));
            // variation direction, with respect to the integration direction
            increasing = !g0Positive;
            pendingEventTime = beforeRootT;
            stopTime = beforeRootG == 0.0 ? beforeRootT : afterRootT;
            pendingEvent = true;
            afterEvent = afterRootT;
            afterG = afterRootG;

            // check increasing set correctly
            check(afterG > 0 == increasing);
            check(increasing == gb >= ga);

            return true;
        }

    }

    /**
     * Get the next number after the given number in the current propagation direction.
     *
     * @param t input time
     * @return t +/- 1 ulp depending on the direction.
     */
    private AbsoluteDate nextAfter(final AbsoluteDate t) {
        return t.shiftedBy(forward ? +Precision.EPSILON : -Precision.EPSILON);
    }

    /** Get the occurrence time of the event triggered in the current
     * step.
     * @return occurrence time of the event triggered in the current
     * step.
     */
    public AbsoluteDate getEventDate() {
        return pendingEventTime;
    }

    /**
     * Try to accept the current history up to the given time.
     *
     * <p> It is not necessary to call this method before calling {@link
     * #doEvent(SpacecraftState)} with the same state. It is necessary to call this
     * method before you call {@link #doEvent(SpacecraftState)} on some other event
     * detector.
     *
     * @param state        to try to accept.
     * @param interpolator to use to find the new root, if any.
     * @return if the event detector has an event it has not detected before that is on or
     * before the same time as {@code state}. In other words {@code false} means continue
     * on while {@code true} means stop and handle my event first.
     * @exception OrekitException if the g function throws one
     */
    public boolean tryAdvance(final SpacecraftState state,
                              final OrekitStepInterpolator interpolator)
        throws OrekitException {
        // check this is only called before a pending event.
        check(!(pendingEvent && strictlyAfter(pendingEventTime, state.getDate())));

        final AbsoluteDate t = state.getDate();

        // just found an event and we know the next time we want to search again
        if (strictlyAfter(t, earliestTimeConsidered)) {
            return false;
        }

        final double g = g(state);
        final boolean positive = g > 0;

        // check for new root, pendingEventTime may be null if there is not pending event
        if ((g == 0.0 && t.equals(pendingEventTime)) || positive == g0Positive) {
            // at a root we already found, or g function has expected sign
            t0 = t;
            g0 = g; // g0Positive is the same
            return false;
        } else {
            // found a root we didn't expect -> find precise location
            return findRoot(interpolator, t0, g0, t, g);
        }
    }

    /**
     * Notify the user's listener of the event. The event occurs wholly within this method
     * call including a call to {@link EventDetector#resetState(SpacecraftState)}
     * if necessary.
     *
     * @param state the state at the time of the event. This must be at the same time as
     *              the current value of {@link #getEventDate()}.
     * @return the user's requested action and the new state if the action is {@link
     * org.orekit.propagation.events.handlers.EventHandler.Action#RESET_STATE Action.RESET_STATE}.
     * Otherwise the new state is {@code state}. The stop time indicates what time propagation
     * should stop if the action is {@link
     * org.orekit.propagation.events.handlers.EventHandler.Action#STOP Action.STOP}.
     * This guarantees the integration will stop on or after the root, so that integration
     * may be restarted safely.
     * @exception OrekitException if the event detector throws one
     */
    public EventOccurrence doEvent(final SpacecraftState state)
        throws OrekitException {
        // check event is pending and is at the same time
        check(pendingEvent);
        check(state.getDate().equals(this.pendingEventTime));

        final EventHandler.Action action = detector.eventOccurred(state, increasing == forward);
        final SpacecraftState newState;
        if (action == EventHandler.Action.RESET_STATE) {
            newState = detector.resetState(state);
        } else {
            newState = state;
        }
        // clear pending event
        pendingEvent     = false;
        pendingEventTime = null;
        // setup for next search
        earliestTimeConsidered = afterEvent;
        t0 = afterEvent;
        g0 = afterG;
        g0Positive = increasing;
        // check g0Positive set correctly
        check(g0 == 0.0 || g0Positive == (g0 > 0));
        return new EventOccurrence(action, newState, stopTime);
    }

    /**
     * Shift a time value along the current integration direction: {@link #forward}.
     *
     * @param t     the time to shift.
     * @param delta the amount to shift.
     * @return t + delta if forward, else t - delta. If the result has to be rounded it
     * will be rounded to be before the true value of t + delta.
     */
    private AbsoluteDate shiftedBy(final AbsoluteDate t, final double delta) {
        if (forward) {
            final AbsoluteDate ret = t.shiftedBy(delta);
            if (ret.durationFrom(t) > delta) {
                return ret.shiftedBy(-Precision.EPSILON);
            } else {
                return ret;
            }
        } else {
            final AbsoluteDate ret = t.shiftedBy(-delta);
            if (t.durationFrom(ret) > delta) {
                return ret.shiftedBy(+Precision.EPSILON);
            } else {
                return ret;
            }
        }
    }

    /**
     * Get the time that happens first along the current propagation direction: {@link
     * #forward}.
     *
     * @param a first time
     * @param b second time
     * @return min(a, b) if forward, else max (a, b)
     */
    private AbsoluteDate minTime(final AbsoluteDate a, final AbsoluteDate b) {
        return (forward ^ (a.compareTo(b) > 0)) ? a : b;
    }

    /**
     * Check the ordering of two times.
     *
     * @param t1 the first time.
     * @param t2 the second time.
     * @return true if {@code t2} is strictly after {@code t1} in the propagation
     * direction.
     */
    private boolean strictlyAfter(final AbsoluteDate t1, final AbsoluteDate t2) {
        if (t1 == null || t2 == null) {
            return false;
        } else {
            return forward ? t1.compareTo(t2) < 0 : t2.compareTo(t1) < 0;
        }
    }

    /**
     * Same as keyword assert, but throw a {@link MathRuntimeException}.
     *
     * @param condition to check
     * @throws MathRuntimeException if {@code condition} is false.
     */
    private void check(final boolean condition) throws MathRuntimeException {
        if (!condition) {
            throw new OrekitInternalError(null);
        }
    }

    /**
     * Class to hold the data related to an event occurrence that is needed to decide how
     * to modify integration.
     */
    public static class EventOccurrence {

        /** User requested action. */
        private final EventHandler.Action action;
        /** New state for a reset action. */
        private final SpacecraftState newState;
        /** The time to stop propagation if the action is a stop event. */
        private final AbsoluteDate stopDate;

        /**
         * Create a new occurrence of an event.
         *
         * @param action   the user requested action.
         * @param newState for a reset event. Should be the current state unless the
         *                 action is {@link Action#RESET_STATE}.
         * @param stopDate to stop propagation if the action is {@link Action#STOP}. Used
         *                 to move the stop time to just after the root.
         */
        EventOccurrence(final EventHandler.Action action,
                        final SpacecraftState newState,
                        final AbsoluteDate stopDate) {
            this.action = action;
            this.newState = newState;
            this.stopDate = stopDate;
        }

        /**
         * Get the user requested action.
         *
         * @return the action.
         */
        public EventHandler.Action getAction() {
            return action;
        }

        /**
         * Get the new state for a reset action.
         *
         * @return the new state.
         */
        public SpacecraftState getNewState() {
            return newState;
        }

        /**
         * Get the new time for a stop action.
         *
         * @return when to stop propagation.
         */
        public AbsoluteDate getStopDate() {
            return stopDate;
        }

    }

}
