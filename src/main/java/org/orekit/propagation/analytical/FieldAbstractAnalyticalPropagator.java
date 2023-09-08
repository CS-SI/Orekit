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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.ode.events.Action;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.FieldAbstractPropagator;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldEphemerisGenerator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventState;
import org.orekit.propagation.events.FieldEventState.EventOccurrence;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Common handling of {@link org.orekit.propagation.FieldPropagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link
 * org.orekit.propagation.FieldPropagator FieldPropagator} methods, including all propagation
 * modes support and discrete events support for any simple propagation method. Only
 * two methods must be implemented by derived classes: {@link #propagateOrbit(FieldAbsoluteDate, CalculusFieldElement[])}
 * and {@link #getMass(FieldAbsoluteDate)}. The first method should perform straightforward
 * propagation starting from some internally stored initial state up to the specified target date.
 * </p>
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */

public abstract class FieldAbstractAnalyticalPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractPropagator<T>
                                                                                           implements ParameterDriversProvider {

    /** Provider for attitude computation. */
    private FieldPVCoordinatesProvider<T> pvProvider;

    /** Start date of last propagation. */
    private FieldAbsoluteDate<T> lastPropagationStart;

    /** End date of last propagation. */
    private FieldAbsoluteDate<T> lastPropagationEnd;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Indicator for last step. */
    private boolean isLastStep;

    /** Event steps. */
    private final Collection<FieldEventState<?, T>> eventsStates;

    /** Build a new instance.
     * @param attitudeProvider provider for attitude computation
     * @param field field used as default
     */
    protected FieldAbstractAnalyticalPropagator(final Field<T> field, final AttitudeProvider attitudeProvider) {
        super(field);
        setAttitudeProvider(attitudeProvider);
        pvProvider           = new FieldLocalPVProvider();
        lastPropagationStart = FieldAbsoluteDate.getPastInfinity(field);
        lastPropagationEnd   = FieldAbsoluteDate.getFutureInfinity(field);
        statesInitialized    = false;
        eventsStates         = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public FieldEphemerisGenerator<T> getEphemerisGenerator() {
        return () -> new FieldBoundedPropagatorView(lastPropagationStart, lastPropagationEnd);
    }

    /** {@inheritDoc} */
    public <D extends FieldEventDetector<T>> void addEventDetector(final D detector) {
        eventsStates.add(new FieldEventState<>(detector));
    }

    /** {@inheritDoc} */
    @Override
    public Collection<FieldEventDetector<T>> getEventsDetectors() {
        final List<FieldEventDetector<T>> list = new ArrayList<>();
        for (final FieldEventState<?, T> state : eventsStates) {
            list.add(state.getEventDetector());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** {@inheritDoc} */
    @Override
    public void clearEventsDetectors() {
        eventsStates.clear();
    }
    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> start, final FieldAbsoluteDate<T> target) {
        try {

            initializePropagation();

            lastPropagationStart = start;

            // Initialize additional states
            initializeAdditionalStates(target);

            final boolean           isForward = target.compareTo(start) >= 0;
            FieldSpacecraftState<T> state   = updateAdditionalStates(basicPropagate(start));

            // initialize event detectors
            for (final FieldEventState<?, T> es : eventsStates) {
                es.init(state, target);
            }

            // initialize step handlers
            getMultiplexer().init(state, target);

            // iterate over the propagation range, need loop due to reset events
            statesInitialized = false;
            isLastStep = false;
            do {

                // attempt to advance to the target date
                final FieldSpacecraftState<T> previous = state;
                final FieldSpacecraftState<T> current = updateAdditionalStates(basicPropagate(target));
                final FieldBasicStepInterpolator interpolator =
                        new FieldBasicStepInterpolator(isForward, previous, current);

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

    /** Accept a step, triggering events and step handlers.
     * @param interpolator interpolator for the current step
     * @param target final propagation time
     * @return state at the end of the step
     * @exception MathRuntimeException if an event cannot be located
     */
    protected FieldSpacecraftState<T> acceptStep(final FieldBasicStepInterpolator interpolator,
                                                 final FieldAbsoluteDate<T> target)
        throws MathRuntimeException {

        FieldSpacecraftState<T>       previous   = interpolator.getPreviousState();
        final FieldSpacecraftState<T> current    = interpolator.getCurrentState();
        FieldBasicStepInterpolator    restricted = interpolator;

        // initialize the events states if needed
        if (!statesInitialized) {

            if (!eventsStates.isEmpty()) {
                // initialize the events states
                for (final FieldEventState<?, T> state : eventsStates) {
                    state.reinitializeBegin(interpolator);
                }
            }

            statesInitialized = true;

        }

        // search for next events that may occur during the step
        final int orderingSign = interpolator.isForward() ? +1 : -1;
        final Queue<FieldEventState<?, T>> occurringEvents = new PriorityQueue<>(new Comparator<FieldEventState<?, T>>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final FieldEventState<?, T> es0, final FieldEventState<?, T> es1) {
                return orderingSign * es0.getEventDate().compareTo(es1.getEventDate());
            }
        });

        boolean doneWithStep = false;
        resetEvents:
        do {

            // Evaluate all event detectors for events
            occurringEvents.clear();
            for (final FieldEventState<?, T> state : eventsStates) {
                if (state.evaluateStep(interpolator)) {
                    // the event occurs during the current step
                    occurringEvents.add(state);
                }
            }


            do {

                eventLoop:
                while (!occurringEvents.isEmpty()) {

                    // handle the chronologically first event
                    final FieldEventState<?, T> currentEvent = occurringEvents.poll();

                    // get state at event time
                    FieldSpacecraftState<T> eventState = restricted.getInterpolatedState(currentEvent.getEventDate());

                    // restrict the interpolator to the first part of the step, up to the event
                    restricted = restricted.restrictStep(previous, eventState);

                    // try to advance all event states to current time
                    for (final FieldEventState<?, T> state : eventsStates) {
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
                    final EventOccurrence<T> occurrence = currentEvent.doEvent(eventState);
                    final Action action = occurrence.getAction();
                    isLastStep = action == Action.STOP;
                    if (isLastStep) {

                        // ensure the event is after the root if it is returned STOP
                        // this lets the user integrate to a STOP event and then restart
                        // integration from the same time.
                        final FieldSpacecraftState<T> savedState = eventState;
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
                        final FieldSpacecraftState<T> resetState = occurrence.getNewState();
                        resetIntermediateState(resetState, interpolator.isForward());
                        return resetState;
                    }
                    // at this point action == Action.CONTINUE or Action.RESET_EVENTS

                    // prepare handling of the remaining part of the step
                    previous = eventState;
                    restricted = new FieldBasicStepInterpolator(restricted.isForward(), eventState, current);

                    if (action == Action.RESET_EVENTS) {
                        continue resetEvents;
                    }

                    // at this pint action == Action.CONTINUE
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
                for (final FieldEventState<?, T> state : eventsStates) {
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
    protected abstract T getMass(FieldAbsoluteDate<T> date);

    /** Get PV coordinates provider.
     * @return PV coordinates provider
     */
    public FieldPVCoordinatesProvider<T> getPvProvider() {
        return pvProvider;
    }

    /** Reset an intermediate state.
     * @param state new intermediate state to consider
     * @param forward if true, the intermediate state is valid for
     * propagations after itself
     */
    protected abstract void resetIntermediateState(FieldSpacecraftState<T> state, boolean forward);

    /** Extrapolate an orbit up to a specific target date.
     * @param date target date for the orbit
     * @param parameters model parameters
     * @return extrapolated parameters
     */
    protected abstract FieldOrbit<T> propagateOrbit(FieldAbsoluteDate<T> date, T[] parameters);

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any discrete events, not additional states. It always
     * stop exactly at the specified date.</p>
     * @param date target date for propagation
     * @return state at specified date
     */
    protected FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) {
        try {

            // evaluate orbit
            final FieldOrbit<T> orbit = propagateOrbit(date, getParameters(date.getField(), date.getDate()));

            // evaluate attitude
            final FieldAttitude<T> attitude =
                getAttitudeProvider().getAttitude(pvProvider, date, orbit.getFrame());

            // build raw state
            return new FieldSpacecraftState<>(orbit, attitude, getMass(date));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

    /** Internal FieldPVCoordinatesProvider<T> for attitude computation. */
    private class FieldLocalPVProvider implements FieldPVCoordinatesProvider<T> {

        /** {@inheritDoc} */
        @Override
        public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
            return propagateOrbit(date, getParameters(date.getField(), date)).getPVCoordinates(frame);
        }

    }

    /** {@link BoundedPropagator} view of the instance. */
    private class FieldBoundedPropagatorView extends FieldAbstractAnalyticalPropagator<T>
        implements FieldBoundedPropagator<T> {

        /** Min date. */
        private final FieldAbsoluteDate<T> minDate;

        /** Max date. */
        private final FieldAbsoluteDate<T> maxDate;

        /** Simple constructor.
         * @param startDate start date of the propagation
         * @param endDate end date of the propagation
         */
        FieldBoundedPropagatorView(final FieldAbsoluteDate<T> startDate, final FieldAbsoluteDate<T> endDate) {
            super(startDate.durationFrom(endDate).getField(), FieldAbstractAnalyticalPropagator.this.getAttitudeProvider());
            super.resetInitialState(FieldAbstractAnalyticalPropagator.this.getInitialState());
            if (startDate.compareTo(endDate) <= 0) {
                minDate = startDate;
                maxDate = endDate;
            } else {
                minDate = endDate;
                maxDate = startDate;
            }

            try {
                // copy the same additional state providers as the original propagator
                for (FieldAdditionalStateProvider<T> provider : FieldAbstractAnalyticalPropagator.this.getAdditionalStateProviders()) {
                    addAdditionalStateProvider(provider);
                }
            } catch (OrekitException oe) {
                // as the providers are already compatible with each other,
                // this should never happen
                throw new OrekitInternalError(null);
            }

        }

        /** {@inheritDoc} */
        @Override
        public FieldAbsoluteDate<T> getMinDate() {
            return minDate;
        }

        /** {@inheritDoc} */
        @Override
        public FieldAbsoluteDate<T> getMaxDate() {
            return maxDate;
        }

        /** {@inheritDoc} */
        @Override
        protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> target, final T[] parameters) {
            return FieldAbstractAnalyticalPropagator.this.propagateOrbit(target, parameters);
        }

        /** {@inheritDoc} */
        @Override
        public T getMass(final FieldAbsoluteDate<T> date) {
            return FieldAbstractAnalyticalPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
            return propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        @Override
        public void resetInitialState(final FieldSpacecraftState<T> state) {
            super.resetInitialState(state);
            FieldAbstractAnalyticalPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        @Override
        protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
            FieldAbstractAnalyticalPropagator.this.resetIntermediateState(state, forward);
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> getInitialState() {
            return FieldAbstractAnalyticalPropagator.this.getInitialState();
        }

        /** {@inheritDoc} */
        @Override
        public Frame getFrame() {
            return FieldAbstractAnalyticalPropagator.this.getFrame();
        }

        /** {@inheritDoc} */
        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return FieldAbstractAnalyticalPropagator.this.getParametersDrivers();
        }
    }

    /** Internal class for local propagation. */
    private class FieldBasicStepInterpolator implements FieldOrekitStepInterpolator<T> {

        /** Previous state. */
        private final FieldSpacecraftState<T> previousState;

        /** Current state. */
        private final FieldSpacecraftState<T> currentState;

        /** Forward propagation indicator. */
        private final boolean forward;

        /** Simple constructor.
         * @param isForward integration direction indicator
         * @param previousState start of the step
         * @param currentState end of the step
         */
        FieldBasicStepInterpolator(final boolean isForward,
                                   final FieldSpacecraftState<T> previousState,
                                   final FieldSpacecraftState<T> currentState) {
            this.forward             = isForward;
            this.previousState   = previousState;
            this.currentState    = currentState;
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> getPreviousState() {
            return previousState;
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> getCurrentState() {
            return currentState;
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> getInterpolatedState(final FieldAbsoluteDate<T> date) {

            // compute the basic spacecraft state
            final FieldSpacecraftState<T> basicState = basicPropagate(date);

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
        public FieldBasicStepInterpolator restrictStep(final FieldSpacecraftState<T> newPreviousState,
                                                       final FieldSpacecraftState<T> newCurrentState) {
            return new FieldBasicStepInterpolator(forward, newPreviousState, newCurrentState);
        }

    }

}
