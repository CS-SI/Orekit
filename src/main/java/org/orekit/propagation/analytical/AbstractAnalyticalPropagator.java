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

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventState;
import org.orekit.propagation.events.EventState.EventOccurrence;
import org.orekit.propagation.events.handlers.EventHandler.Action;
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
        throws OrekitException {
        try {

            lastPropagationStart = start;

            final double dt       = target.durationFrom(start);
            final double epsilon  = FastMath.ulp(dt);
            SpacecraftState state = updateAdditionalStates(basicPropagate(start));

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
                final SpacecraftState previous = state;
                AbsoluteDate t = previous.getDate().shiftedBy(stepSize);
                if ((dt == 0) || ((dt > 0) ^ (t.compareTo(target) <= 0)) ||
                        (FastMath.abs(target.durationFrom(t)) <= epsilon)) {
                    // current step exceeds target
                    // or is target to within double precision
                    t = target;
                }
                final SpacecraftState current = updateAdditionalStates(basicPropagate(t));
                final BasicStepInterpolator interpolator = new BasicStepInterpolator(dt >= 0, previous, current);


                // accept the step, trigger events and step handlers
                state = acceptStep(interpolator, target, epsilon);

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
     * @param epsilon threshold for end date detection
     * @return state at the end of the step
     * @exception OrekitException if the switching function cannot be evaluated
     * @exception MathRuntimeException if an event cannot be located
     */
    protected SpacecraftState acceptStep(final BasicStepInterpolator interpolator,
                                         final AbsoluteDate target, final double epsilon)
        throws OrekitException, MathRuntimeException {

        SpacecraftState       previous = interpolator.getPreviousState();
        final SpacecraftState current  = interpolator.getCurrentState();

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

        for (final EventState<?> state : eventsStates) {
            if (state.evaluateStep(interpolator)) {
                // the event occurs during the current step
                occurringEvents.add(state);
            }
        }

        BasicStepInterpolator restricted = interpolator;

        do {

            eventLoop:
            while (!occurringEvents.isEmpty()) {

                // handle the chronologically first event
                final EventState<?> currentEvent = occurringEvents.poll();

                // get state at event time
                SpacecraftState eventState = restricted.getInterpolatedState(currentEvent.getEventDate());

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

                final EventOccurrence occurrence = currentEvent.doEvent(eventState);
                final Action action = occurrence.getAction();
                isLastStep = action == Action.STOP;

                if (isLastStep) {
                    // ensure the event is after the root if it is returned STOP
                    // this lets the user integrate to a STOP event and then restart
                    // integration from the same time.
                    eventState = interpolator.getInterpolatedState(occurrence.getStopDate());
                    restricted = new BasicStepInterpolator(restricted.isForward(), previous, eventState);
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
                    final SpacecraftState resetState = occurrence.getNewState();
                    if (resetState != null) {
                        resetIntermediateState(resetState, interpolator.isForward());
                        return resetState;
                    }
                }
                // at this point we know action == Action.CONTINUE

                // prepare handling of the remaining part of the step
                previous = eventState;
                restricted         = new BasicStepInterpolator(restricted.isForward(), eventState, current);

                // check if the same event occurs again in the remaining part of the step
                if (currentEvent.evaluateStep(restricted)) {
                    // the event occurs during the current step
                    occurringEvents.add(currentEvent);
                }

            }

            // last part of the step, after the last event
            // may be a new event here if the last event modified the g function of
            // another event detector.
            for (final EventState<?> state : eventsStates) {
                if (state.tryAdvance(current, interpolator)) {
                    occurringEvents.add(state);
                }
            }

        } while (!occurringEvents.isEmpty());

        isLastStep = target.equals(current.getDate());

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
    protected abstract double getMass(AbsoluteDate date)
        throws OrekitException;

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
     * @exception OrekitException if initial state cannot be reset
     */
    protected abstract void resetIntermediateState(SpacecraftState state, boolean forward)
        throws OrekitException;

    /** Extrapolate an orbit up to a specific target date.
     * @param date target date for the orbit
     * @return extrapolated parameters
     * @exception OrekitException if some parameters are out of bounds
     */
    protected abstract Orbit propagateOrbit(AbsoluteDate date)
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
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws OrekitException {
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

    /** Internal PVCoordinatesProvider for attitude computation. */
    private class LocalPVProvider implements PVCoordinatesProvider {

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagateOrbit(date).getPVCoordinates(frame);
        }

    }

    /** {@link BoundedPropagator} view of the instance. */
    private class BoundedPropagatorView
        extends AbstractAnalyticalPropagator
        implements BoundedPropagator, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151117L;

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

            try {
                // copy the same additional state providers as the original propagator
                for (AdditionalStateProvider provider : AbstractAnalyticalPropagator.this.getAdditionalStateProviders()) {
                    addAdditionalStateProvider(provider);
                }
            } catch (OrekitException oe) {
                // as the providers are already compatible with each other,
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
        protected Orbit propagateOrbit(final AbsoluteDate target)
            throws OrekitException {
            return AbstractAnalyticalPropagator.this.propagateOrbit(target);
        }

        /** {@inheritDoc} */
        public double getMass(final AbsoluteDate date) throws OrekitException {
            return AbstractAnalyticalPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        public void resetInitialState(final SpacecraftState state) throws OrekitException {
            AbstractAnalyticalPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        protected void resetIntermediateState(final SpacecraftState state, final boolean forward)
            throws OrekitException {
            AbstractAnalyticalPropagator.this.resetIntermediateState(state, forward);
        }

        /** {@inheritDoc} */
        public SpacecraftState getInitialState() throws OrekitException {
            return AbstractAnalyticalPropagator.this.getInitialState();
        }

        /** {@inheritDoc} */
        public Frame getFrame() {
            return AbstractAnalyticalPropagator.this.getFrame();
        }

        /** Replace the instance with a data transfer object for serialization.
         * @return data transfer object that will be serialized
         * @exception NotSerializableException if attitude provider or additional
         * state provider is not serializable
         */
        private Object writeReplace() throws NotSerializableException {
            return new DataTransferObject(minDate, maxDate, AbstractAnalyticalPropagator.this);
        }

    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151117L;

        /** Min date. */
        private final AbsoluteDate minDate;

        /** Max date. */
        private final AbsoluteDate maxDate;

        /** Underlying propagator. */
        private final AbstractAnalyticalPropagator propagator;

        /** Simple constructor.
         * @param minDate min date
         * @param maxDate max date
         * @param propagator underlying propagator
         */
        DataTransferObject(final AbsoluteDate minDate, final AbsoluteDate maxDate,
                           final AbstractAnalyticalPropagator propagator) {
            this.minDate    = minDate;
            this.maxDate    = maxDate;
            this.propagator = propagator;
        }

        /** Replace the deserialized data transfer object with an {@link BoundedPropagatorView}.
         * @return replacement {@link BoundedPropagatorView}
         */
        private Object readResolve() {
            propagator.lastPropagationStart = minDate;
            propagator.lastPropagationEnd   = maxDate;
            return propagator.getGeneratedEphemeris();
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
            this.forward             = isForward;
            this.previousState   = previousState;
            this.currentState    = currentState;
        }

        /** {@inheritDoc} */
        public SpacecraftState getPreviousState() {
            return previousState;
        }

        @Override
        public boolean isPreviousStateInterpolated() {
            // no difference in analytical propagators
            return false;
        }

        /** {@inheritDoc} */
        public SpacecraftState getCurrentState() {
            return currentState;
        }

        @Override
        public boolean isCurrentStateInterpolated() {
            // no difference in analytical propagators
            return false;
        }

        /** {@inheritDoc} */
        public SpacecraftState getInterpolatedState(final AbsoluteDate date)
            throws OrekitException {

            // compute the basic spacecraft state
            final SpacecraftState basicState = basicPropagate(date);

            // add the additional states
            return updateAdditionalStates(basicState);

        }

        /** {@inheritDoc} */
        public boolean isForward() {
            return forward;
        }

    }

}
