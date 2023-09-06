/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.ode.events.Action;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventState;
import org.orekit.propagation.events.EventState.EventOccurrence;
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
        pvProvider           = new LocalPVProvider();
        lastPropagationStart = AbsoluteDate.PAST_INFINITY;
        lastPropagationEnd   = AbsoluteDate.FUTURE_INFINITY;
        statesInitialized    = false;
        eventsStates         = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public EphemerisGenerator getEphemerisGenerator() {
        return () -> new BoundedPropagatorView(lastPropagationStart, lastPropagationEnd);
    }

    /** {@inheritDoc} */
    public <T extends EventDetector> void addEventDetector(final T detector) {
        eventsStates.add(new EventState<>(detector));
    }

    /** {@inheritDoc} */
    public Collection<EventDetector> getEventsDetectors() {
        final List<EventDetector> list = new ArrayList<>();
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
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target) {
        checkStartDateIsNotInfinity(start);
        try {
            initializePropagation();

            lastPropagationStart = start;

            // Initialize additional states
            initializeAdditionalStates(target);

            final boolean isForward = target.compareTo(start) >= 0;
            SpacecraftState state   = updateAdditionalStates(basicPropagate(start));

            // initialize event detectors
            for (final EventState<?> es : eventsStates) {
                es.init(state, target);
            }

            // initialize step handlers
            getMultiplexer().init(state, target);

            // iterate over the propagation range, need loop due to reset events
            statesInitialized = false;
            isLastStep = false;
            do {

                // attempt to advance to the target date
                final SpacecraftState previous = state;
                final SpacecraftState current = updateAdditionalStates(basicPropagate(target));
                final OrekitStepInterpolator interpolator =
                        new BasicStepInterpolator(isForward, previous, current);

                // accept the step, trigger events and step handlers
                state = acceptStep(interpolator, target);

                // Update the potential changes in the spacecraft state due to the events
                // especially the potential attitude transition
                state = updateAdditionalStates(basicPropagate(state.getDate()));

            } while (!isLastStep);

            // return the last computed state
            lastPropagationEnd = state.getDate();
            setStartDate(state.getDate());
            return state;

        } catch (MathRuntimeException mrte) {
            throw OrekitException.unwrap(mrte);
        }
    }

    /**
     * Check the starting date is not {@code AbsoluteDate.PAST_INFINITY} or {@code AbsoluteDate.FUTURE_INFINITY}.
     * @param start propagation starting date
     */
    private void checkStartDateIsNotInfinity(final AbsoluteDate start) {
        if (start.isEqualTo(AbsoluteDate.PAST_INFINITY) || start.isEqualTo(AbsoluteDate.FUTURE_INFINITY)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.CANNOT_START_PROPAGATION_FROM_INFINITY);
        }
    }

    /** Accept a step, triggering events and step handlers.
     * @param interpolator interpolator for the current step
     * @param target final propagation time
     * @return state at the end of the step
     * @exception MathRuntimeException if an event cannot be located
     */
    protected SpacecraftState acceptStep(final OrekitStepInterpolator interpolator,
                                         final AbsoluteDate target)
        throws MathRuntimeException {

        SpacecraftState        previous   = interpolator.getPreviousState();
        final SpacecraftState  current    = interpolator.getCurrentState();
        OrekitStepInterpolator restricted = interpolator;


        // initialize the events states if needed
        if (!statesInitialized) {

            if (!eventsStates.isEmpty()) {
                // initialize the events states
                for (final EventState<?> state : eventsStates) {
                    state.reinitializeBegin(interpolator);
                }
            }

            statesInitialized = true;

        }

        // search for next events that may occur during the step
        final int orderingSign = interpolator.isForward() ? +1 : -1;
        final Queue<EventState<?>> occurringEvents = new PriorityQueue<>(new Comparator<EventState<?>>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final EventState<?> es0, final EventState<?> es1) {
                return orderingSign * es0.getEventDate().compareTo(es1.getEventDate());
            }
        });

        boolean doneWithStep = false;
        resetEvents:
        do {

            // Evaluate all event detectors for events
            occurringEvents.clear();
            for (final EventState<?> state : eventsStates) {
                if (state.evaluateStep(interpolator)) {
                    // the event occurs during the current step
                    occurringEvents.add(state);
                }
            }

            do {

                eventLoop:
                while (!occurringEvents.isEmpty()) {

                    // handle the chronologically first event
                    final EventState<?> currentEvent = occurringEvents.poll();

                    // get state at event time
                    SpacecraftState eventState = restricted.getInterpolatedState(currentEvent.getEventDate());

                    // restrict the interpolator to the first part of the step, up to the event
                    restricted = restricted.restrictStep(previous, eventState);

                    // try to advance all event states to current time
                    for (final EventState<?> state : eventsStates) {
                        if (state != currentEvent && state.tryAdvance(eventState, interpolator)) {
                            // we need to handle another event first
                            // remove event we just updated to prevent heap corruption
                            occurringEvents.remove(state);
                            // add it back to update its position in the heap
                            occurringEvents.add(state);
                            // re-queue the event we were processing
                            occurringEvents.add(currentEvent);
                            continue eventLoop;
                        }
                    }
                    // all event detectors agree we can advance to the current event time

                    // handle the first part of the step, up to the event
                    getMultiplexer().handleStep(restricted);

                    // acknowledge event occurrence
                    final EventOccurrence occurrence = currentEvent.doEvent(eventState);
                    final Action action = occurrence.getAction();
                    isLastStep = action == Action.STOP;

                    if (isLastStep) {

                        // ensure the event is after the root if it is returned STOP
                        // this lets the user integrate to a STOP event and then restart
                        // integration from the same time.
                        final SpacecraftState savedState = eventState;
                        eventState = interpolator.getInterpolatedState(occurrence.getStopDate());
                        restricted = restricted.restrictStep(savedState, eventState);

                        // handle the almost zero size last part of the final step, at event time
                        getMultiplexer().handleStep(restricted);
                        getMultiplexer().finish(restricted.getCurrentState());

                    }

                    if (isLastStep) {
                        // the event asked to stop integration
                        return eventState;
                    }

                    if (action == Action.RESET_DERIVATIVES || action == Action.RESET_STATE) {
                        // some event handler has triggered changes that
                        // invalidate the derivatives, we need to recompute them
                        final SpacecraftState resetState = occurrence.getNewState();
                        resetIntermediateState(resetState, interpolator.isForward());
                        return resetState;
                    }
                    // at this point action == Action.CONTINUE or Action.RESET_EVENTS

                    // prepare handling of the remaining part of the step
                    previous = eventState;
                    restricted = new BasicStepInterpolator(restricted.isForward(), eventState, current);

                    if (action == Action.RESET_EVENTS) {
                        continue resetEvents;
                    }

                    // at this point action == Action.CONTINUE
                    // check if the same event occurs again in the remaining part of the step
                    if (currentEvent.evaluateStep(restricted)) {
                        // the event occurs during the current step
                        occurringEvents.add(currentEvent);
                    }

                }

                // last part of the step, after the last event. Advance all detectors to
                // the end of the step. Should only detect a new event here if an event
                // modified the g function of another detector. Detecting such events here
                // is unreliable and RESET_EVENTS should be used instead. Might as well
                // re-check here because we have to loop through all the detectors anyway
                // and the alternative is to throw an exception.
                for (final EventState<?> state : eventsStates) {
                    if (state.tryAdvance(current, interpolator)) {
                        occurringEvents.add(state);
                    }
                }

            } while (!occurringEvents.isEmpty());

            doneWithStep = true;
        } while (!doneWithStep);

        isLastStep = target.equals(current.getDate());

        // handle the remaining part of the step, after all events if any
        getMultiplexer().handleStep(restricted);
        if (isLastStep) {
            getMultiplexer().finish(restricted.getCurrentState());
        }

        return current;

    }

    /** Get the mass.
     * @param date target date for the orbit
     * @return mass mass
     */
    protected abstract double getMass(AbsoluteDate date);

    /** Get PV coordinates provider.
     * @return PV coordinates provider
     */
    public PVCoordinatesProvider getPvProvider() {
        return pvProvider;
    }

    /** Reset an intermediate state.
     * @param state new intermediate state to consider
     * @param forward if true, the intermediate state is valid for
     * propagations after itself
     */
    protected abstract void resetIntermediateState(SpacecraftState state, boolean forward);

    /** Extrapolate an orbit up to a specific target date.
     * @param date target date for the orbit
     * @return extrapolated parameters
     */
    protected abstract Orbit propagateOrbit(AbsoluteDate date);

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any discrete events, not additional states. It always
     * stop exactly at the specified date.</p>
     * @param date target date for propagation
     * @return state at specified date
     */
    protected SpacecraftState basicPropagate(final AbsoluteDate date) {
        try {

            // evaluate orbit
            final Orbit orbit = propagateOrbit(date);

            // evaluate attitude
            final Attitude attitude =
                getAttitudeProvider().getAttitude(pvProvider, date, orbit.getFrame());

            // build raw state
            return new SpacecraftState(orbit, attitude, getMass(date));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

    /**
     * Get the names of the parameters in the matrix returned by {@link MatricesHarvester#getParametersJacobian}.
     * @return names of the parameters (i.e. columns) of the Jacobian matrix
     * @since 11.1
     */
    protected List<String> getJacobiansColumnsNames() {
        return Collections.emptyList();
    }

    /** Internal PVCoordinatesProvider for attitude computation. */
    private class LocalPVProvider implements PVCoordinatesProvider {

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
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
            super.resetInitialState(AbstractAnalyticalPropagator.this.getInitialState());
            if (startDate.compareTo(endDate) <= 0) {
                minDate = startDate;
                maxDate = endDate;
            } else {
                minDate = endDate;
                maxDate = startDate;
            }

            try {
                // copy the same additional state providers as the original propagator
                for (AdditionalStateProvider provider : AbstractAnalyticalPropagator.this.getAdditionalStateProviders()) {
                    addAdditionalStateProvider(provider);
                }
            } catch (OrekitException oe) {
                // as the generators are already compatible with each other,
                // this should never happen
                throw new OrekitInternalError(null);
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
        protected Orbit propagateOrbit(final AbsoluteDate target) {
            return AbstractAnalyticalPropagator.this.propagateOrbit(target);
        }

        /** {@inheritDoc} */
        public double getMass(final AbsoluteDate date) {
            return AbstractAnalyticalPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
            return propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        public void resetInitialState(final SpacecraftState state) {
            super.resetInitialState(state);
            AbstractAnalyticalPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
            AbstractAnalyticalPropagator.this.resetIntermediateState(state, forward);
        }

        /** {@inheritDoc} */
        public SpacecraftState getInitialState() {
            return AbstractAnalyticalPropagator.this.getInitialState();
        }

        /** {@inheritDoc} */
        public Frame getFrame() {
            return AbstractAnalyticalPropagator.this.getFrame();
        }

    }

    /** Internal class for local propagation. */
    private class BasicStepInterpolator implements OrekitStepInterpolator {

        /** Previous state. */
        private final SpacecraftState previousState;

        /** Current state. */
        private final SpacecraftState currentState;

        /** Forward propagation indicator. */
        private final boolean forward;

        /** Simple constructor.
         * @param isForward integration direction indicator
         * @param previousState start of the step
         * @param currentState end of the step
         */
        BasicStepInterpolator(final boolean isForward,
                              final SpacecraftState previousState,
                              final SpacecraftState currentState) {
            this.forward         = isForward;
            this.previousState   = previousState;
            this.currentState    = currentState;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState getPreviousState() {
            return previousState;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isPreviousStateInterpolated() {
            // no difference in analytical propagators
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState getCurrentState() {
            return currentState;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCurrentStateInterpolated() {
            // no difference in analytical propagators
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState getInterpolatedState(final AbsoluteDate date) {

            // compute the basic spacecraft state
            final SpacecraftState basicState = basicPropagate(date);

            // add the additional states
            return updateAdditionalStates(basicState);

        }

        /** {@inheritDoc} */
        @Override
        public boolean isForward() {
            return forward;
        }

        /** {@inheritDoc} */
        @Override
        public BasicStepInterpolator restrictStep(final SpacecraftState newPreviousState,
                                                  final SpacecraftState newCurrentState) {
            return new BasicStepInterpolator(forward, newPreviousState, newCurrentState);
        }

    }

}
