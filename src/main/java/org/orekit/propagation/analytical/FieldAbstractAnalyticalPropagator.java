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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldAbstractPropagator;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventState;
import org.orekit.propagation.events.FieldEventState.EventOccurrence;
import org.orekit.propagation.events.handlers.FieldEventHandler.Action;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Common handling of {@link org.orekit.propagation.FieldPropagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link
 * org.orekit.propagation.FieldPropagator FieldPropagator} methods, including all propagation
 * modes support and discrete events support for any simple propagation method. Only
 * two methods must be implemented by derived classes: {@link #propagateOrbit(FieldAbsoluteDate)}
 * and {@link #getMass(FieldAbsoluteDate)}. The first method should perform straightforward
 * propagation starting from some internally stored initial state up to the specified target date.
 * </p>
 * @author Luc Maisonobe
 */

public abstract class FieldAbstractAnalyticalPropagator<T extends RealFieldElement<T>> extends FieldAbstractPropagator<T> {

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
    protected FieldAbstractAnalyticalPropagator(final Field<T> field, final FieldAttitudeProvider<T> attitudeProvider) {
        super(field);
        setAttitudeProvider(attitudeProvider);
        pvProvider           = new FieldLocalPVProvider();
        lastPropagationStart = FieldAbsoluteDate.getPastInfinity(field);
        lastPropagationEnd   = FieldAbsoluteDate.getFutureInfinity(field);
        statesInitialized    = false;
        eventsStates         = new ArrayList<FieldEventState<?, T>>();
    }

    /** {@inheritDoc} */
    public FieldBoundedPropagator<T> getGeneratedEphemeris() {
        return new FieldBoundedPropagatorView(lastPropagationStart, lastPropagationEnd);
    }

    /** {@inheritDoc} */
    public Collection<FieldEventDetector<T>> getEventsDetectors() {
        final List<FieldEventDetector<T>> list = new ArrayList<FieldEventDetector<T>>();
        for (final FieldEventState<?, T> state : eventsStates) {
            list.add(state.getEventDetector());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        eventsStates.clear();
    }
    /** {@inheritDoc} */
    public FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> start, final FieldAbsoluteDate<T> target)
        throws OrekitException {
        try {
            lastPropagationStart = start;

            final T dt       = target.durationFrom(start);

            final double epsilon  = FastMath.ulp(dt.getReal());

            FieldSpacecraftState<T> state = updateAdditionalStates(basicPropagate(start));

            // evaluate step size
            final T stepSize;
            if (getMode() == MASTER_MODE) {
                if (Double.isNaN(getFixedStepSize().getReal())) {
                    stepSize = state.getKeplerianPeriod().divide(100).copySign(dt);
                } else {
                    stepSize = getFixedStepSize().copySign(dt);
                }
            } else {
                stepSize = dt;
            }

            // initialize event detectors
            for (final FieldEventState<?, T> es : eventsStates) {
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
                final FieldSpacecraftState<T> previous = state;
                FieldAbsoluteDate<T> t = previous.getDate().shiftedBy(stepSize);
                if ((dt.getReal() == 0) || ((dt.getReal() > 0) ^ (t.compareTo(target) <= 0))) {
                    // current step exceeds target
                    t = target;
                }
                final FieldSpacecraftState<T> current = updateAdditionalStates(basicPropagate(t));
                final FieldBasicStepInterpolator interpolator = new FieldBasicStepInterpolator(dt.getReal() >= 0, previous, current);



                // accept the step, trigger events and step handlers
                state = acceptStep(interpolator, target, epsilon);
            } while (!isLastStep);
            // return the last computed state
            lastPropagationEnd = state.getDate();
            setStartDate(state.getDate());
            return state;

        } catch (OrekitException pe) {
            throw pe;
        } catch (MathRuntimeException mrte) {
            throw OrekitException.unwrap(mrte);
        }
    }

    /** Accept a step, triggering events and step handlers.
     * @param interpolator interpolator for the current step
     * @param target final propagation time
     * @param epsilon threshold for end date detection
     * @return state at the end of the step
     * @exception OrekitException if the switching function cannot be evaluated
     * @exception MathRuntimeException if an event cannot be located
     */
    protected FieldSpacecraftState<T> acceptStep(final FieldBasicStepInterpolator interpolator,
                                         final FieldAbsoluteDate<T> target, final double epsilon)
        throws OrekitException, MathRuntimeException {

        FieldSpacecraftState<T>       previous = interpolator.getPreviousState();
        final FieldSpacecraftState<T> current  = interpolator.getCurrentState();
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
        for (final FieldEventState<?, T> state : eventsStates) {
            if (state.evaluateStep(interpolator)) {
                // the event occurs during the current step
                occurringEvents.add(state);
            }
        }



        FieldBasicStepInterpolator restricted = interpolator;

        do {
            eventLoop:
            while (!occurringEvents.isEmpty()) {
                // handle the chronologically first event
                final FieldEventState<?, T> currentEvent = occurringEvents.poll();

                // get state at event time
                FieldSpacecraftState<T> eventState = restricted.getInterpolatedState(currentEvent.getEventDate());
                // try to advance all event states to current time
                for (final FieldEventState<?, T> state : eventsStates) {
                    if (state != currentEvent && state.tryAdvance(eventState, interpolator)) {
                        // we need to handle another event first
                        occurringEvents.add(currentEvent);
                        occurringEvents.remove(state); // remove if already has pending event
                        occurringEvents.add(state);
                        continue eventLoop;
                    }
                }
                // all event detectors agree we can advance to the current event time
                final EventOccurrence<T> occurrence = currentEvent.doEvent(eventState);
                final Action action = occurrence.getAction();
                isLastStep = action == Action.STOP;
                if (isLastStep) {
                    // ensure the event is after the root if it is returned STOP
                    // this lets the user integrate to a STOP event and then restart
                    // integration from the same time.
                    eventState = interpolator.getInterpolatedState(occurrence.getStopDate());
                    restricted = new FieldBasicStepInterpolator(restricted.isForward(), previous, eventState);
                }
                // handle the first part of the step, up to the event
                if (getStepHandler() != null) {
                    getStepHandler().handleStep(restricted, isLastStep);
                }

                if (isLastStep) {
                    // the event asked to stop integration
                    return eventState;
                }

                if (action == Action.RESET_DERIVATIVES || action == Action.RESET_STATE) {
                    // some event handler has triggered changes that
                    // invalidate the derivatives, we need to recompute them
                    final FieldSpacecraftState<T> resetState = occurrence.getNewState();
                    if (resetState != null) {
                        resetIntermediateState( resetState, interpolator.isForward());
                        return resetState;
                    }
                }
                // at this point we know action == Action.CONTINUE

                // prepare handling of the remaining part of the step
                previous = eventState;
                restricted         = new FieldBasicStepInterpolator(restricted.isForward(), eventState, current);

                // check if the same event occurs again in the remaining part of the step
                if (currentEvent.evaluateStep(restricted)) {
                    // the event occurs during the current step
                    occurringEvents.add(currentEvent);
                }

            }

            // last part of the step, after the last event
            // may be a new event here if the last event modified the g function of
            // another event detector.
            for (final FieldEventState<?, T> state : eventsStates) {
                if (state.tryAdvance(current, interpolator)) {
                    occurringEvents.add(state);
                }
            }

        } while (!occurringEvents.isEmpty());

        final T remaining = target.durationFrom(current.getDate());
        if (interpolator.isForward()) {
            isLastStep = remaining.getReal() <  epsilon;
        } else {
            isLastStep = remaining.getReal() > -epsilon;
        }

        // handle the remaining part of the step, after all events if any
        if (getStepHandler() != null) {
            getStepHandler().handleStep(interpolator, isLastStep);
        }
        return current;

    }

    /** Get the mass.
     * @param date target date for the orbit
     * @return mass mass
     * @exception OrekitException if some parameters are out of bounds
     */
    protected abstract T getMass(FieldAbsoluteDate<T> date)
        throws OrekitException;

    /** Get PV coordinates provider.
     * @return PV coordinates provider
     */
    public FieldPVCoordinatesProvider<T> getPvProvider() {
        return pvProvider;
    }

    /** {@inheritDoc} */
    public <D extends FieldEventDetector<T>> void addEventDetector(final D detector) {
        eventsStates.add(new FieldEventState<D, T>(detector));
    }

    /** Reset an intermediate state.
     * @param state new intermediate state to consider
     * @param forward if true, the intermediate state is valid for
     * propagations after itself
     * @exception OrekitException if initial state cannot be reset
     */
    protected abstract void resetIntermediateState(FieldSpacecraftState<T> state, boolean forward)
        throws OrekitException;

    /** Extrapolate an orbit up to a specific target date.
     * @param date target date for the orbit
     * @return extrapolated parameters
     * @exception OrekitException if some parameters are out of bounds
     */
    protected abstract FieldOrbit<T> propagateOrbit(FieldAbsoluteDate<T> date)
        throws OrekitException;

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any discrete events, not additional states. It always
     * stop exactly at the specified date.</p>
     * @param date target date for propagation
     * @return state at specified date
     * @exception OrekitException if propagation cannot reach specified date
     */
    protected FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) throws OrekitException {
        try {

            // evaluate orbit
            final FieldOrbit<T> orbit = propagateOrbit(date);

            // evaluate attitude
            final FieldAttitude<T> attitude =
                getAttitudeProvider().getAttitude(pvProvider, date, orbit.getFrame());

            // build raw state
            return new FieldSpacecraftState<T>(orbit, attitude, getMass(date));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

    /** Internal FieldPVCoordinatesProvider<T> for attitude computation. */
    private class FieldLocalPVProvider implements FieldPVCoordinatesProvider<T> {

        /** {@inheritDoc} */
        public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame)
            throws OrekitException {
            return propagateOrbit(date).getPVCoordinates(frame);
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
        public FieldAbsoluteDate<T> getMinDate() {
            return minDate;
        }

        /** {@inheritDoc} */
        public FieldAbsoluteDate<T> getMaxDate() {
            return maxDate;
        }

        /** {@inheritDoc} */
        protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> target)
            throws OrekitException {
            return FieldAbstractAnalyticalPropagator.this.propagateOrbit(target);
        }

        /** {@inheritDoc} */
        public T getMass(final FieldAbsoluteDate<T> date) throws OrekitException {
            return FieldAbstractAnalyticalPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame)
            throws OrekitException {
            return propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        public void resetInitialState(final FieldSpacecraftState<T> state) throws OrekitException {
            FieldAbstractAnalyticalPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward)
            throws OrekitException {
            FieldAbstractAnalyticalPropagator.this.resetIntermediateState(state, forward);
        }

        /** {@inheritDoc} */
        public FieldSpacecraftState<T> getInitialState() throws OrekitException {
            return FieldAbstractAnalyticalPropagator.this.getInitialState();
        }

        /** {@inheritDoc} */
        public Frame getFrame() {
            return FieldAbstractAnalyticalPropagator.this.getFrame();
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
        public FieldSpacecraftState<T> getPreviousState() {
            return previousState;
        }

        /** {@inheritDoc} */
        public FieldSpacecraftState<T> getCurrentState() {
            return currentState;
        }

        /** {@inheritDoc} */
        public FieldSpacecraftState<T> getInterpolatedState(final FieldAbsoluteDate<T> date)
            throws OrekitException {

            // compute the basic spacecraft state
            final FieldSpacecraftState<T> basicState = basicPropagate(date);

            // add the additional states
            return updateAdditionalStates(basicState);

        }

        /** {@inheritDoc} */
        public boolean isForward() {
            return forward;
        }

    }

}
