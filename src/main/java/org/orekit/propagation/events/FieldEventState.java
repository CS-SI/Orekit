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

import java.util.function.DoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketedUnivariateSolver;
import org.hipparchus.analysis.solvers.BracketedUnivariateSolver.Interval;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.FieldAbsoluteDate;

/** This class handles the state for one {@link FieldEventDetector
 * event detector} during integration steps.
 *
 * <p>This class is heavily based on the class with the same name from the
 * Hipparchus library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link FieldAbsoluteDate}, {@link FieldSpacecraftState}).</p>
 * <p>Each time the propagator proposes a step, the event detector
 * should be checked. This class handles the state of one detector
 * during one propagation step, with references to the state at the
 * end of the preceding step. This information is used to determine if
 * the detector should trigger an event or not during the proposed
 * step (and hence the step should be reduced to ensure the event
 * occurs at a bound rather than inside the step).</p>
 * @author Luc Maisonobe
 * @param <D> class type for the generic version
 * @param <T> type of the field elements
 */
public class FieldEventState<D extends FieldEventDetector<T>, T extends CalculusFieldElement<T>> {

    /** Event detector. */
    private D detector;

    /** Event handler. */
    private FieldEventHandler<T> handler;

    /** Time of the previous call to g. */
    private FieldAbsoluteDate<T> lastT;

    /** Value from the previous call to g. */
    private T lastG;

    /** Time at the beginning of the step. */
    private FieldAbsoluteDate<T> t0;

    /** Value of the event detector at the beginning of the step. */
    private T g0;

    /** Simulated sign of g0 (we cheat when crossing events). */
    private boolean g0Positive;

    /** Indicator of event expected during the step. */
    private boolean pendingEvent;

    /** Occurrence time of the pending event. */
    private FieldAbsoluteDate<T> pendingEventTime;

    /**
     * Time to stop propagation if the event is a stop event. Used to enable stopping at
     * an event and then restarting after that event.
     */
    private FieldAbsoluteDate<T> stopTime;

    /** Time after the current event. */
    private FieldAbsoluteDate<T> afterEvent;

    /** Value of the g function after the current event. */
    private T afterG;

    /** The earliest time considered for events. */
    private FieldAbsoluteDate<T> earliestTimeConsidered;

    /** Integration direction. */
    private boolean forward;

    /** Variation direction around pending event.
     *  (this is considered with respect to the integration direction)
     */
    private boolean increasing;

    /** Simple constructor.
     * @param detector monitored event detector
     */
    public FieldEventState(final D detector) {

        this.detector = detector;
        this.handler  = detector.getHandler();

        // some dummy values ...
        final Field<T> field   = detector.getThreshold().getField();
        final T nan            = field.getZero().add(Double.NaN);
        lastT                  = FieldAbsoluteDate.getPastInfinity(field);
        lastG                  = nan;
        t0                     = null;
        g0                     = nan;
        g0Positive             = true;
        pendingEvent           = false;
        pendingEventTime       = null;
        stopTime               = null;
        increasing             = true;
        earliestTimeConsidered = null;
        afterEvent             = null;
        afterG                 = nan;

    }

    /** Get the underlying event detector.
     * @return underlying event detector
     */
    public D getEventDetector() {
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
     *
     */
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        detector.init(s0, t);
        final Field<T> field = detector.getThreshold().getField();
        lastT = FieldAbsoluteDate.getPastInfinity(field);
        lastG = field.getZero().add(Double.NaN);
    }

    /** Compute the value of the switching function.
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    private T g(final FieldSpacecraftState<T> s) {
        if (!s.getDate().equals(lastT)) {
            lastT = s.getDate();
            lastG = detector.g(s);
        }
        return lastG;
    }

    /** Reinitialize the beginning of the step.
     * @param interpolator interpolator valid for the current step
     */
    public void reinitializeBegin(final FieldOrekitStepInterpolator<T> interpolator) {
        forward = interpolator.isForward();
        final FieldSpacecraftState<T> s0 = interpolator.getPreviousState();
        this.t0 = s0.getDate();
        g0 = g(s0);
        while (g0.getReal() == 0) {
            // extremely rare case: there is a zero EXACTLY at interval start
            // we will use the sign slightly after step beginning to force ignoring this zero
            // try moving forward by half a convergence interval
            final T dt = detector.getThreshold().multiply(forward ? 0.5 : -0.5);
            FieldAbsoluteDate<T> startDate = t0.shiftedBy(dt);
            // if convergence is too small move an ulp
            if (t0.equals(startDate)) {
                startDate = nextAfter(startDate);
            }
            t0 = startDate;
            g0 = g(interpolator.getInterpolatedState(t0));
        }
        g0Positive = g0.getReal() > 0;
        // "last" event was increasing
        increasing = g0Positive;
    }

    /** Evaluate the impact of the proposed step on the event detector.
     * @param interpolator step interpolator for the proposed step
     * @return true if the event detector triggers an event before
     * the end of the proposed step (this implies the step should be
     * rejected)
     * @exception MathRuntimeException if an event cannot be located
     */
    public boolean evaluateStep(final FieldOrekitStepInterpolator<T> interpolator)
        throws MathRuntimeException {
        forward = interpolator.isForward();
        final FieldSpacecraftState<T> s0 = interpolator.getPreviousState();
        final FieldSpacecraftState<T> s1 = interpolator.getCurrentState();
        final FieldAbsoluteDate<T> t1 = s1.getDate();
        final T dt = t1.durationFrom(t0);
        if (FastMath.abs(dt.getReal()) < detector.getThreshold().getReal()) {
            // we cannot do anything on such a small step, don't trigger any events
            pendingEvent     = false;
            pendingEventTime = null;
            return false;
        }

        FieldAbsoluteDate<T> ta = t0;
        T ga = g0;
        for (FieldSpacecraftState<T> sb = nextCheck(s0, s1, interpolator);
             sb != null;
             sb = nextCheck(sb, s1, interpolator)) {

            // evaluate handler value at the end of the substep
            final FieldAbsoluteDate<T> tb = sb.getDate();
            final T gb = g(sb);

            // check events occurrence
            if (gb.getReal() == 0.0 || (g0Positive ^ gb.getReal() > 0)) {
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

    /** Estimate next state to check.
     * @param done state already checked
     * @param target target state towards which we are checking
     * @param interpolator step interpolator for the proposed step
     * @return intermediate state to check, or exactly {@code null}
     * if we already have {@code done == target}
     * @since 12.0
     */
    private FieldSpacecraftState<T> nextCheck(final FieldSpacecraftState<T> done, final FieldSpacecraftState<T> target,
                                              final FieldOrekitStepInterpolator<T> interpolator) {
        if (done == target) {
            // we have already reached target
            return null;
        } else {
            // we have to select some intermediate state
            // attempting to split the remaining time in an integer number of checks
            final T dt            = target.getDate().durationFrom(done.getDate());
            final double maxCheck = detector.getMaxCheckInterval().currentInterval(done);
            final int    n        = FastMath.max(1, (int) FastMath.ceil(FastMath.abs(dt).divide(maxCheck).getReal()));
            return n == 1 ? target : interpolator.getInterpolatedState(done.getDate().shiftedBy(dt.divide(n)));
        }
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
     */
    private boolean findRoot(final FieldOrekitStepInterpolator<T> interpolator,
                             final FieldAbsoluteDate<T> ta, final T ga,
                             final FieldAbsoluteDate<T> tb, final T gb) {

        final T zero = ga.getField().getZero();

        // check there appears to be a root in [ta, tb]
        check(ga.getReal() == 0.0 || gb.getReal() == 0.0 || ga.getReal() > 0.0 && gb.getReal() < 0.0 || ga.getReal() < 0.0 && gb.getReal() > 0.0);
        final T convergence = detector.getThreshold();
        final int maxIterationCount = detector.getMaxIterationCount();
        final BracketedUnivariateSolver<UnivariateFunction> solver =
                new BracketingNthOrderBrentSolver(0, convergence.getReal(), 0, 5);

        // prepare loop below
        FieldAbsoluteDate<T> loopT = ta;
        T loopG = ga;

        // event time, just at or before the actual root.
        FieldAbsoluteDate<T> beforeRootT = null;
        T beforeRootG = zero.add(Double.NaN);
        // time on the other side of the root.
        // Initialized the the loop below executes once.
        FieldAbsoluteDate<T> afterRootT = ta;
        T afterRootG = zero;

        // check for some conditions that the root finders don't like
        // these conditions cannot not happen in the loop below
        // the ga == 0.0 case is handled by the loop below
        if (ta.equals(tb)) {
            // both non-zero but times are the same. Probably due to reset state
            beforeRootT = ta;
            beforeRootG = ga;
            afterRootT = shiftedBy(beforeRootT, convergence);
            afterRootG = g(interpolator.getInterpolatedState(afterRootT));
        } else if (ga.getReal() != 0.0 && gb.getReal() == 0.0) {
            // hard: ga != 0.0 and gb == 0.0
            // look past gb by up to convergence to find next sign
            // throw an exception if g(t) = 0.0 in [tb, tb + convergence]
            beforeRootT = tb;
            beforeRootG = gb;
            afterRootT = shiftedBy(beforeRootT, convergence);
            afterRootG = g(interpolator.getInterpolatedState(afterRootT));
        } else if (ga.getReal() != 0.0) {
            final T newGa = g(interpolator.getInterpolatedState(ta));
            if (ga.getReal() > 0 != newGa.getReal() > 0) {
                // both non-zero, step sign change at ta, possibly due to reset state
                final FieldAbsoluteDate<T> nextT = minTime(shiftedBy(ta, convergence), tb);
                final T                    nextG = g(interpolator.getInterpolatedState(nextT));
                if (nextG.getReal() > 0.0 == g0Positive) {
                    // the sign change between ga and newGa just moved the root less than one convergence
                    // threshold later, we are still in a regular search for another root before tb,
                    // we just need to fix the bracketing interval
                    // (see issue https://github.com/Hipparchus-Math/hipparchus/issues/184)
                    loopT = nextT;
                    loopG = nextG;
                } else {
                    beforeRootT = ta;
                    beforeRootG = newGa;
                    afterRootT  = nextT;
                    afterRootG  = nextG;
                }
            }
        }

        // loop to skip through "fake" roots, i.e. where g(t) = g'(t) = 0.0
        // executed once if we didn't hit a special case above
        while ((afterRootG.getReal() == 0.0 || afterRootG.getReal() > 0.0 == g0Positive) &&
                strictlyAfter(afterRootT, tb)) {
            if (loopG.getReal() == 0.0) {
                // ga == 0.0 and gb may or may not be 0.0
                // handle the root at ta first
                beforeRootT = loopT;
                beforeRootG = loopG;
                afterRootT = minTime(shiftedBy(beforeRootT, convergence), tb);
                afterRootG = g(interpolator.getInterpolatedState(afterRootT));
            } else {
                // both non-zero, the usual case, use a root finder.
                // time zero for evaluating the function f. Needs to be final
                final FieldAbsoluteDate<T> fT0 = loopT;
                final double tbDouble = tb.durationFrom(fT0).getReal();
                final double middle   = 0.5 * tbDouble;
                final DoubleFunction<FieldAbsoluteDate<T>> date = dt -> {
                    // use either fT0 or tb as the base time for shifts
                    // in order to ensure we reproduce exactly those times
                    // using only one reference time like fT0 would imply
                    // to use ft0.shiftedBy(tbDouble), which may be different
                    // from tb due to numerical noise (see issue 921)
                    if (forward == dt <= middle) {
                        // use start of interval as reference
                        return fT0.shiftedBy(dt);
                    } else {
                        // use end of interval as reference
                        return tb.shiftedBy(dt - tbDouble);
                    }
                };
                final UnivariateFunction f = dt -> g(interpolator.getInterpolatedState(date.apply(dt))).getReal();
                if (forward) {
                    try {
                        final Interval interval =
                                solver.solveInterval(maxIterationCount, f, 0, tbDouble);
                        beforeRootT = date.apply(interval.getLeftAbscissa());
                        beforeRootG = zero.add(interval.getLeftValue());
                        afterRootT  = date.apply(interval.getRightAbscissa());
                        afterRootG  = zero.add(interval.getRightValue());
                        // CHECKSTYLE: stop IllegalCatch check
                    } catch (RuntimeException e) {
                        // CHECKSTYLE: resume IllegalCatch check
                        throw new OrekitException(e, OrekitMessages.FIND_ROOT,
                                detector, loopT, loopG, tb, gb, lastT, lastG);
                    }
                } else {
                    try {
                        final Interval interval =
                                solver.solveInterval(maxIterationCount, f, tbDouble, 0);
                        beforeRootT = date.apply(interval.getRightAbscissa());
                        beforeRootG = zero.add(interval.getRightValue());
                        afterRootT  = date.apply(interval.getLeftAbscissa());
                        afterRootG  = zero.add(interval.getLeftValue());
                        // CHECKSTYLE: stop IllegalCatch check
                    } catch (RuntimeException e) {
                        // CHECKSTYLE: resume IllegalCatch check
                        throw new OrekitException(e, OrekitMessages.FIND_ROOT,
                                detector, tb, gb, loopT, loopG, lastT, lastG);
                    }
                }
            }
            // tolerance is set to less than 1 ulp
            // assume tolerance is 1 ulp
            if (beforeRootT.equals(afterRootT)) {
                afterRootT = nextAfter(afterRootT);
                afterRootG = g(interpolator.getInterpolatedState(afterRootT));
            }
            // check loop is making some progress
            check(forward && afterRootT.compareTo(beforeRootT) > 0 ||
                  !forward && afterRootT.compareTo(beforeRootT) < 0);
            // setup next iteration
            loopT = afterRootT;
            loopG = afterRootG;
        }

        // figure out the result of root finding, and return accordingly
        if (afterRootG.getReal() == 0.0 || afterRootG.getReal() > 0.0 == g0Positive) {
            // loop gave up and didn't find any crossing within this step
            return false;
        } else {
            // real crossing
            check(beforeRootT != null && !Double.isNaN(beforeRootG.getReal()));
            // variation direction, with respect to the integration direction
            increasing = !g0Positive;
            pendingEventTime = beforeRootT;
            stopTime = beforeRootG.getReal() == 0.0 ? beforeRootT : afterRootT;
            pendingEvent = true;
            afterEvent = afterRootT;
            afterG = afterRootG;

            // check increasing set correctly
            check(afterG.getReal() > 0 == increasing);
            check(increasing == gb.getReal() >= ga.getReal());

            return true;
        }

    }

    /**
     * Get the next number after the given number in the current propagation direction.
     *
     * @param t input time
     * @return t +/- 1 ulp depending on the direction.
     */
    private FieldAbsoluteDate<T> nextAfter(final FieldAbsoluteDate<T> t) {
        return t.shiftedBy(forward ? +Precision.EPSILON : -Precision.EPSILON);
    }


    /** Get the occurrence time of the event triggered in the current
     * step.
     * @return occurrence time of the event triggered in the current
     * step.
     */
    public FieldAbsoluteDate<T> getEventDate() {
        return pendingEventTime;
    }

    /**
     * Try to accept the current history up to the given time.
     *
     * <p> It is not necessary to call this method before calling {@link
     * #doEvent(FieldSpacecraftState)} with the same state. It is necessary to call this
     * method before you call {@link #doEvent(FieldSpacecraftState)} on some other event
     * detector.
     *
     * @param state        to try to accept.
     * @param interpolator to use to find the new root, if any.
     * @return if the event detector has an event it has not detected before that is on or
     * before the same time as {@code state}. In other words {@code false} means continue
     * on while {@code true} means stop and handle my event first.
     */
    public boolean tryAdvance(final FieldSpacecraftState<T> state,
                              final FieldOrekitStepInterpolator<T> interpolator) {
        final FieldAbsoluteDate<T> t = state.getDate();
        // check this is only called before a pending event.
        check(!pendingEvent || !strictlyAfter(pendingEventTime, t));

        final boolean meFirst;

        if (strictlyAfter(t, earliestTimeConsidered)) {
            // just found an event and we know the next time we want to search again
            meFirst = false;
        } else {
            // check g function to see if there is a new event
            final T g = g(state);
            final boolean positive = g.getReal() > 0;

            if (positive == g0Positive) {
                // g function has expected sign
                g0 = g; // g0Positive is the same
                meFirst = false;
            } else {
                // found a root we didn't expect -> find precise location
                final FieldAbsoluteDate<T> oldPendingEventTime = pendingEventTime;
                final boolean foundRoot = findRoot(interpolator, t0, g0, t, g);
                // make sure the new root is not the same as the old root, if one exists
                meFirst = foundRoot && !pendingEventTime.equals(oldPendingEventTime);
            }
        }

        if (!meFirst) {
            // advance t0 to the current time so we can't find events that occur before t
            t0 = t;
        }

        return meFirst;
    }

    /**
     * Notify the user's listener of the event. The event occurs wholly within this method
     * call including a call to {@link FieldEventHandler#resetState(FieldEventDetector,
     * FieldSpacecraftState)} if necessary.
     *
     * @param state the state at the time of the event. This must be at the same time as
     *              the current value of {@link #getEventDate()}.
     * @return the user's requested action and the new state if the action is {@link
     * Action#RESET_STATE}. Otherwise
     * the new state is {@code state}. The stop time indicates what time propagation should
     * stop if the action is {@link Action#STOP}.
     * This guarantees the integration will stop on or after the root, so that integration
     * may be restarted safely.
     */
    public EventOccurrence<T> doEvent(final FieldSpacecraftState<T> state) {
        // check event is pending and is at the same time
        check(pendingEvent);
        check(state.getDate().equals(this.pendingEventTime));

        final Action action = handler.eventOccurred(state, detector, increasing == forward);
        final FieldSpacecraftState<T> newState;
        if (action == Action.RESET_STATE) {
            newState = handler.resetState(detector, state);
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
        check(g0.getReal() == 0.0 || g0Positive == g0.getReal() > 0);
        return new EventOccurrence<T>(action, newState, stopTime);
    }

    /**
     * Shift a time value along the current integration direction: {@link #forward}.
     *
     * @param t     the time to shift.
     * @param delta the amount to shift.
     * @return t + delta if forward, else t - delta. If the result has to be rounded it
     * will be rounded to be before the true value of t + delta.
     */
    private FieldAbsoluteDate<T> shiftedBy(final FieldAbsoluteDate<T> t, final T delta) {
        if (forward) {
            final FieldAbsoluteDate<T> ret = t.shiftedBy(delta);
            if (ret.durationFrom(t).getReal() > delta.getReal()) {
                return ret.shiftedBy(-Precision.EPSILON);
            } else {
                return ret;
            }
        } else {
            final FieldAbsoluteDate<T> ret = t.shiftedBy(delta.negate());
            if (t.durationFrom(ret).getReal() > delta.getReal()) {
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
    private FieldAbsoluteDate<T> minTime(final FieldAbsoluteDate<T> a, final FieldAbsoluteDate<T> b) {
        return (forward ^ a.compareTo(b) > 0) ? a : b;
    }

    /**
     * Check the ordering of two times.
     *
     * @param t1 the first time.
     * @param t2 the second time.
     * @return true if {@code t2} is strictly after {@code t1} in the propagation
     * direction.
     */
    private boolean strictlyAfter(final FieldAbsoluteDate<T> t1, final FieldAbsoluteDate<T> t2) {
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
     * @param <T> type of the field elements
     */
    public static class EventOccurrence<T extends CalculusFieldElement<T>> {

        /** User requested action. */
        private final Action action;
        /** New state for a reset action. */
        private final FieldSpacecraftState<T> newState;
        /** The time to stop propagation if the action is a stop event. */
        private final FieldAbsoluteDate<T> stopDate;

        /**
         * Create a new occurrence of an event.
         *
         * @param action   the user requested action.
         * @param newState for a reset event. Should be the current state unless the
         *                 action is {@link Action#RESET_STATE}.
         * @param stopDate to stop propagation if the action is {@link Action#STOP}. Used
         *                 to move the stop time to just after the root.
         */
        EventOccurrence(final Action action,
                        final FieldSpacecraftState<T> newState,
                        final FieldAbsoluteDate<T> stopDate) {
            this.action = action;
            this.newState = newState;
            this.stopDate = stopDate;
        }

        /**
         * Get the user requested action.
         *
         * @return the action.
         */
        public Action getAction() {
            return action;
        }

        /**
         * Get the new state for a reset action.
         *
         * @return the new state.
         */
        public FieldSpacecraftState<T> getNewState() {
            return newState;
        }

        /**
         * Get the new time for a stop action.
         *
         * @return when to stop propagation.
         */
        public FieldAbsoluteDate<T> getStopDate() {
            return stopDate;
        }

    }

    /**Get PendingEvent.
     * @return if there is a pending event or not
     * */

    public boolean getPendingEvent() {
        return pendingEvent;
    }

}
