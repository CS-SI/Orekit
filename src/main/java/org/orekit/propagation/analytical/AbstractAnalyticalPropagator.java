/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Common handling of {@link org.orekit.propagation.Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link
 * org.orekit.propagation.Propagator Propagator} methods, including all propagation
 * modes support and discrete events support for any simple propagation method. Only
 * two methods must be implemented by derived classes: {@link #propagateOrbit(AbsoluteDate)}
 * and {@link #getMass(AbsoluteDate)}. The first method should perform straightforward
 * propagation starting from some internally stored initial state up to the specified target date.
 * </p>
 * @author Luc Maisonobe
 */
public abstract class AbstractAnalyticalPropagator extends AbstractPropagator {

    /** Internal steps interpolator. */
    private final BasicStepInterpolator interpolator;

    /** Provider for attitude computation. */
    private PVCoordinatesProvider pvProvider;

    /** Start date of last propagation. */
    private AbsoluteDate lastPropagationStart;

    /** End date of last propagation. */
    private AbsoluteDate lastPropagationEnd;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Indicator for last step. */
    private boolean isLastStep;

    /** Event steps. */
    private final Collection<EventState<?>> eventsStates;

    /** Build a new instance.
     * @param attitudeProvider provider for attitude computation
     */
    protected AbstractAnalyticalPropagator(final AttitudeProvider attitudeProvider) {
        setAttitudeProvider(attitudeProvider);
        interpolator             = new BasicStepInterpolator();
        pvProvider               = new LocalPVProvider();
        lastPropagationStart     = AbsoluteDate.PAST_INFINITY;
        lastPropagationEnd       = AbsoluteDate.FUTURE_INFINITY;
        statesInitialized        = false;
        eventsStates             = new ArrayList<EventState<?>>();
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris() {
        return new BoundedPropagatorView(lastPropagationStart, lastPropagationEnd);
    }

    /** {@inheritDoc} */
    public <T extends EventDetector> void addEventDetector(final T detector) {
        eventsStates.add(new EventState<T>(detector));
    }

    /** {@inheritDoc} */
    public Collection<EventDetector> getEventsDetectors() {
        final List<EventDetector> list = new ArrayList<EventDetector>();
        for (final EventState<?> state : eventsStates) {
            list.add(state.getEventDetector());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        eventsStates.clear();
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target)
        throws PropagationException {
        try {

            lastPropagationStart = start;

            final double dt      = target.durationFrom(start);
            final double epsilon = FastMath.ulp(dt);
            interpolator.storeDate(start);
            SpacecraftState state = interpolator.getInterpolatedState();

            // evaluate step size
            final double stepSize;
            if (getMode() == MASTER_MODE) {
                if (Double.isNaN(getFixedStepSize())) {
                    stepSize = FastMath.copySign(state.getKeplerianPeriod() / 100, dt);
                } else {
                    stepSize = FastMath.copySign(getFixedStepSize(), dt);
                }
            } else {
                stepSize = dt;
            }

            // initialize event detectors
            for (final EventState<?> es : eventsStates) {
                es.init(state, target);
            }

            // initialize step handler
            if (getStepHandler() != null) {
                getStepHandler().init(state, target);
            }

            // iterate over the propagation range
            statesInitialized = false;
            isLastStep = false;
            do {

                // go ahead one step size
                interpolator.shift();
                final AbsoluteDate t = interpolator.getCurrentDate().shiftedBy(stepSize);
                if ((dt == 0) || ((dt > 0) ^ (t.compareTo(target) <= 0))) {
                    // current step exceeds target
                    interpolator.storeDate(target);
                } else {
                    // current step is within range
                    interpolator.storeDate(t);
                }

                // accept the step, trigger events and step handlers
                state = acceptStep(target, epsilon);

            } while (!isLastStep);

            // return the last computed state
            lastPropagationEnd = state.getDate();
            setStartDate(state.getDate());
            return state;

        } catch (PropagationException pe) {
            throw pe;
        } catch (OrekitException oe) {
            throw PropagationException.unwrap(oe);
        } catch (TooManyEvaluationsException tmee) {
            throw PropagationException.unwrap(tmee);
        } catch (NoBracketingException nbe) {
            throw PropagationException.unwrap(nbe);
        }
    }

    /** Accept a step, triggering events and step handlers.
     * @param target final propagation time
     * @param epsilon threshold for end date detection
     * @return state at the end of the step
     * @exception OrekitException if the switching function cannot be evaluated
     * @exception TooManyEvaluationsException if an event cannot be located
     * @exception NoBracketingException if bracketing cannot be performed
     */
    protected SpacecraftState acceptStep(final AbsoluteDate target, final double epsilon)
        throws OrekitException, TooManyEvaluationsException, NoBracketingException {

        AbsoluteDate previousT = interpolator.getGlobalPreviousDate();
        AbsoluteDate currentT  = interpolator.getGlobalCurrentDate();

        // initialize the events states if needed
        if (!statesInitialized) {

            if (!eventsStates.isEmpty()) {
                // initialize the events states
                final AbsoluteDate t0 = interpolator.getPreviousDate();
                interpolator.setInterpolatedDate(t0);
                final SpacecraftState y = interpolator.getInterpolatedState();
                for (final EventState<?> state : eventsStates) {
                    state.reinitializeBegin(y, interpolator.isForward());
                }
            }

            statesInitialized = true;

        }

        // search for next events that may occur during the step
        final List<EventState<?>> occurringEvents = new ArrayList<EventState<?>>();
        for (final EventState<?> state : eventsStates) {
            if (state.evaluateStep(interpolator)) {
                // the event occurs during the current step
                occurringEvents.add(state);
            }
        }

        // chronological or reverse chronological sorter, according to propagation direction
        final int orderingSign = interpolator.isForward() ? +1 : -1;
        final Comparator<EventState<?>> sorter = new Comparator<EventState<?>>() {

            /** {@inheritDoc} */
            public int compare(final EventState<?> es0, final EventState<?> es1) {
                return orderingSign * es0.getEventTime().compareTo(es1.getEventTime());
            }

        };

        while (!occurringEvents.isEmpty()) {

            // handle the chronologically first event
            Collections.sort(occurringEvents, sorter);
            final Iterator<EventState<?>> iterator = occurringEvents.iterator();
            final EventState<?> currentEvent = iterator.next();
            iterator.remove();

            // restrict the interpolator to the first part of the step, up to the event
            final AbsoluteDate eventT = currentEvent.getEventTime();
            interpolator.setSoftPreviousDate(previousT);
            interpolator.setSoftCurrentDate(eventT);

            // trigger the event
            interpolator.setInterpolatedDate(eventT);
            final SpacecraftState eventY = interpolator.getInterpolatedState();
            currentEvent.stepAccepted(eventY);
            isLastStep = currentEvent.stop();

            // handle the first part of the step, up to the event
            if (getStepHandler() != null) {
                getStepHandler().handleStep(interpolator, isLastStep);
            }

            if (isLastStep) {
                // the event asked to stop integration
                return eventY;
            }

            final SpacecraftState resetState = currentEvent.reset(eventY);
            if (resetState != null) {
                resetInitialState(resetState);
                return resetState;
            }

            // prepare handling of the remaining part of the step
            previousT = eventT;
            interpolator.setSoftPreviousDate(eventT);
            interpolator.setSoftCurrentDate(currentT);

            // check if the same event occurs again in the remaining part of the step
            if (currentEvent.evaluateStep(interpolator)) {
                // the event occurs during the current step
                occurringEvents.add(currentEvent);
            }

        }

        final double remaining = target.durationFrom(currentT);
        if (interpolator.isForward()) {
            isLastStep = remaining <  epsilon;
        } else {
            isLastStep = remaining > -epsilon;
        }
        if (isLastStep) {
            currentT = target;
        }

        interpolator.setInterpolatedDate(currentT);
        final SpacecraftState currentY = interpolator.getInterpolatedState();
        for (final EventState<?> state : eventsStates) {
            state.stepAccepted(currentY);
            isLastStep = isLastStep || state.stop();
        }

        // handle the remaining part of the step, after all events if any
        if (getStepHandler() != null) {
            getStepHandler().handleStep(interpolator, isLastStep);
        }

        return currentY;

    }

    /** Get the mass.
     * @param date target date for the orbit
     * @return mass mass
     * @exception PropagationException if some parameters are out of bounds
     */
    protected abstract double getMass(final AbsoluteDate date)
        throws PropagationException;

    /** Get PV coordinates provider.
     * @return PV coordinates provider
     */
    public PVCoordinatesProvider getPvProvider() {
        return pvProvider;
    }

    /** Extrapolate an orbit up to a specific target date.
     * @param date target date for the orbit
     * @return extrapolated parameters
     * @exception PropagationException if some parameters are out of bounds
     */
    protected abstract Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException;

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any discrete events, not additional states. It always
     * stop exactly at the specified date.</p>
     * @param date target date for propagation
     * @return state at specified date
     * @exception PropagationException if propagation cannot reach specified date
     */
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
        try {

            // evaluate orbit
            final Orbit orbit = propagateOrbit(date);

            // evaluate attitude
            final Attitude attitude =
                getAttitudeProvider().getAttitude(pvProvider, date, orbit.getFrame());

            // build raw state
            return new SpacecraftState(orbit, attitude, getMass(date));

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** Internal PVCoordinatesProvider for attitude computation. */
    private class LocalPVProvider implements PVCoordinatesProvider {

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagateOrbit(date).getPVCoordinates(frame);
        }

    }

    /** {@link BoundedPropagator} view of the instance. */
    private class BoundedPropagatorView extends AbstractAnalyticalPropagator implements BoundedPropagator {

        /** Min date. */
        private final AbsoluteDate minDate;

        /** Max date. */
        private final AbsoluteDate maxDate;

        /** Simple constructor.
         * @param startDate start date of the propagation
         * @param endDate end date of the propagation
         */
        BoundedPropagatorView(final AbsoluteDate startDate, final AbsoluteDate endDate) {
            super(AbstractAnalyticalPropagator.this.getAttitudeProvider());
            if (startDate.compareTo(endDate) <= 0) {
                minDate = startDate;
                maxDate = endDate;
            } else {
                minDate = endDate;
                maxDate = startDate;
            }
        }

        /** {@inheritDoc} */
        public AbsoluteDate getMinDate() {
            return minDate;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getMaxDate() {
            return maxDate;
        }

        /** {@inheritDoc} */
        protected Orbit propagateOrbit(final AbsoluteDate target)
            throws PropagationException {
            return AbstractAnalyticalPropagator.this.propagateOrbit(target);
        }

        /** {@inheritDoc} */
        public double getMass(final AbsoluteDate date) throws PropagationException {
            return AbstractAnalyticalPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        public void resetInitialState(final SpacecraftState state) throws PropagationException {
            AbstractAnalyticalPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        public SpacecraftState getInitialState() throws PropagationException {
            return AbstractAnalyticalPropagator.this.getInitialState();
        }

    }

    /** Internal class for local propagation. */
    private class BasicStepInterpolator implements OrekitStepInterpolator {

        /** Global previous date. */
        private AbsoluteDate globalPreviousDate;

        /** Global current date. */
        private AbsoluteDate globalCurrentDate;

        /** Soft previous date. */
        private AbsoluteDate softPreviousDate;

        /** Soft current date. */
        private AbsoluteDate softCurrentDate;

        /** Interpolated state. */
        private SpacecraftState interpolatedState;

        /** Forward propagation indicator. */
        private boolean forward;

        /** Build a new instance from a basic propagator.
         */
        public BasicStepInterpolator() {
            globalPreviousDate = AbsoluteDate.PAST_INFINITY;
            globalCurrentDate  = AbsoluteDate.PAST_INFINITY;
            softPreviousDate   = AbsoluteDate.PAST_INFINITY;
            softCurrentDate    = AbsoluteDate.PAST_INFINITY;
        }

        /** Restrict step range to a limited part of the global step.
         * <p>
         * This method can be used to restrict a step and make it appear
         * as if the original step was smaller. Calling this method
         * <em>only</em> changes the value returned by {@link #getPreviousDate()},
         * it does not change any other property
         * </p>
         * @param softPreviousDate start of the restricted step
         */
        public void setSoftPreviousDate(final AbsoluteDate softPreviousDate) {
            this.softPreviousDate = softPreviousDate;
        }

        /** Restrict step range to a limited part of the global step.
         * <p>
         * This method can be used to restrict a step and make it appear
         * as if the original step was smaller. Calling this method
         * <em>only</em> changes the value returned by {@link #getCurrentDate()},
         * it does not change any other property
         * </p>
         * @param softCurrentDate end of the restricted step
         */
        public void setSoftCurrentDate(final AbsoluteDate softCurrentDate) {
            this.softCurrentDate  = softCurrentDate;
        }

        /**
         * Get the previous global grid point time.
         * @return previous global grid point time
         */
        public AbsoluteDate getGlobalPreviousDate() {
            return globalPreviousDate;
        }

        /**
         * Get the current global grid point time.
         * @return current global grid point time
         */
        public AbsoluteDate getGlobalCurrentDate() {
            return globalCurrentDate;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getCurrentDate() {
            return softCurrentDate;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getInterpolatedDate() {
            return interpolatedState.getDate();
        }

        /** {@inheritDoc} */
        public SpacecraftState getInterpolatedState() throws OrekitException {
            return interpolatedState;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getPreviousDate() {
            return softPreviousDate;
        }

        /** {@inheritDoc} */
        public boolean isForward() {
            return forward;
        }

        /** {@inheritDoc} */
        public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {

            // compute the basic spacecraft state
            final SpacecraftState basicState = basicPropagate(date);

            // add the additional states
            interpolatedState = updateAdditionalStates(basicState);

        }

        /** Shift one step forward.
         * Copy the current date into the previous date, hence preparing the
         * interpolator for future calls to {@link #storeDate storeDate}
         */
        public void shift() {
            globalPreviousDate = globalCurrentDate;
            softPreviousDate   = globalPreviousDate;
            softCurrentDate    = globalCurrentDate;
        }

        /** Store the current step date.
         * @param date current date
         * @exception PropagationException if the state cannot be propagated at specified date
         */
        public void storeDate(final AbsoluteDate date)
            throws PropagationException {
            globalCurrentDate = date;
            softCurrentDate   = globalCurrentDate;
            forward           = globalCurrentDate.compareTo(globalPreviousDate) >= 0;
            setInterpolatedDate(globalCurrentDate);
        }

    }

}
