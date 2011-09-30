/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.exception.NoBracketingException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Common handling of {@link Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator}
 * methods, including all propagation modes support and discrete events support for
 * any simple propagation method. Only two methods must be implemented by derived
 * classes: {@link #propagateOrbit(AbsoluteDate)} and {@link #getMass(AbsoluteDate)}.
 * The first method should perform straightforward propagation starting from some
 * internally stored initial state up to the specified target date.
 * </p>
 * @author Luc Maisonobe
 */
public abstract class AbstractPropagator implements Propagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 2434402795728927604L;

    /** Propagation mode. */
    private int mode;

    /** Fixed step size. */
    private double fixedStepSize;

    /** Step handler. */
    private OrekitStepHandler stepHandler;

    /** Event steps. */
    private final Collection<EventState> eventsStates;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Additional state providers. */
    private final List<AdditionalStateProvider> additionalStateProviders;

    /** Internal steps interpolator. */
    private final BasicStepInterpolator interpolator;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Provider for attitude computation. */
    private PVCoordinatesProvider pvProvider;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Indicator for last step. */
    private boolean isLastStep;


    /** Build a new instance.
     * @param attitudeProvider provider for attitude computation
     */
    protected AbstractPropagator(final AttitudeProvider attitudeProvider) {
        eventsStates           = new ArrayList<EventState>();
        statesInitialized      = false;
        additionalStateProviders = new ArrayList<AdditionalStateProvider>();
        interpolator           = new BasicStepInterpolator();
        this.pvProvider        = new LocalPVProvider();
        this.attitudeProvider  = attitudeProvider;
        setSlaveMode();
    }

    /** Set a start date.
     * @param startDate start date
     */
    protected void setStartDate(final AbsoluteDate startDate) {
        this.startDate = startDate;
    }

    /**  {@inheritDoc} */
    public AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /**  {@inheritDoc} */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
    }

    /** Get PV coordinates provider.
     * @return PV coordinates provider
     */
    public PVCoordinatesProvider getPvProvider() {
        return pvProvider;
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws OrekitException {
        return initialState;
    }

    /** {@inheritDoc} */
    public int getMode() {
        return mode;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return initialState.getFrame();
    }

    /** {@inheritDoc} */
    public void setSlaveMode() {
        mode          = SLAVE_MODE;
        stepHandler   = null;
        fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    public void setMasterMode(final double h,
                              final OrekitFixedStepHandler handler) {
        mode          = MASTER_MODE;
        stepHandler   = new OrekitStepNormalizer(h, handler);
        fixedStepSize = h;
    }

    /** {@inheritDoc} */
    public void setMasterMode(final OrekitStepHandler handler) {
        mode          = MASTER_MODE;
        stepHandler   = handler;
        fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    public void setEphemerisMode() {
        mode          = EPHEMERIS_GENERATION_MODE;
        stepHandler   = null;
        fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris() {
        return new UnboundedPropagatorView();
    }

    /** {@inheritDoc} */
    public void addEventDetector(final EventDetector detector) {
        eventsStates.add(new EventState(detector));
    }

    /** {@inheritDoc} */
    public Collection<EventDetector> getEventsDetectors() {
        final List<EventDetector> list = new ArrayList<EventDetector>();
        for (final EventState state : eventsStates) {
            list.add(state.getEventDetector());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        eventsStates.clear();
    }

    /** Add a set of user-specified state parameters to be computed along with the orbit propagation.
     * @param additionalStateProvider provider for additional state
     * @exception OrekitException if an additional state with the same name is already present
     */
    public void addAdditionalStateProvider(final AdditionalStateProvider additionalStateProvider)
        throws OrekitException {

        // check if the name is already used
        for (final AdditionalStateProvider provider : additionalStateProviders) {
            if (provider.getName().equals(additionalStateProvider.getName())) {
                // this additional state is already registered, complain
                throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE,
                                          additionalStateProvider.getName());
            }
        }

        // this is really a new name, add it
        additionalStateProviders.add(additionalStateProvider);

    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
        try {
            if (startDate == null) {
                startDate = getInitialState().getDate();
            }
            return propagate(startDate, target);
        } catch (OrekitException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }
            throw new PropagationException(oe);

        }
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target)
        throws PropagationException {
        try {

            final double epsilon = FastMath.ulp(target.durationFrom(start));
            interpolator.storeDate(start);
            SpacecraftState state = interpolator.getInterpolatedState();

            // evaluate step size
            final double stepSize;
            if ((mode == MASTER_MODE) && !Double.isNaN(fixedStepSize)) {
                stepSize = state.getKeplerianPeriod() / 100;
            } else {
                stepSize = target.durationFrom(interpolator.getCurrentDate());
            }

            // iterate over the propagation range
            statesInitialized = false;
            isLastStep = false;
            do {

                // go ahead one step size
                interpolator.shift();
                interpolator.storeDate(interpolator.getCurrentDate().shiftedBy(stepSize));

                // accept the step, trigger events and step handlers
                state = acceptStep(target, epsilon);

            } while (!isLastStep);

            // return the last computed state
            startDate = state.getDate();
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

        AbsoluteDate previousT      = interpolator.getGlobalPreviousDate();
        final AbsoluteDate currentT = interpolator.getGlobalCurrentDate();

        // initialize the events states if needed
        if (!statesInitialized) {

            // initialize the events states
            final AbsoluteDate t0 = interpolator.getPreviousDate();
            interpolator.setInterpolatedDate(t0);
            final SpacecraftState y = interpolator.getInterpolatedState();
            for (final EventState state : eventsStates) {
                state.reinitializeBegin(y);
            }

            statesInitialized = true;

        }

        // search for next events that may occur during the step
        final int orderingSign = interpolator.isForward() ? +1 : -1;
        final SortedSet<EventState> occuringEvents =
            new TreeSet<EventState>(new Comparator<EventState>() {

                /** {@inheritDoc} */
                public int compare(final EventState es0, final EventState es1) {
                    return orderingSign * es0.getEventTime().compareTo(es1.getEventTime());
                }

            });


        for (final EventState state : eventsStates) {
            if (state.evaluateStep(interpolator)) {
                // the event occurs during the current step
                occuringEvents.add(state);
            }
        }

        while (!occuringEvents.isEmpty()) {

            // handle the chronologically first event
            final Iterator<EventState> iterator = occuringEvents.iterator();
            final EventState currentEvent = iterator.next();
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
            if (stepHandler != null) {
                stepHandler.handleStep(interpolator, isLastStep);
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
                occuringEvents.add(currentEvent);
            }

        }

        interpolator.setInterpolatedDate(currentT);
        final SpacecraftState currentY = interpolator.getInterpolatedState();
        for (final EventState state : eventsStates) {
            state.stepAccepted(currentY);
            isLastStep = isLastStep || state.stop();
        }
        final double remaining = target.durationFrom(currentT);
        if (interpolator.isForward()) {
            isLastStep = isLastStep || (remaining <  epsilon);
        } else {
            isLastStep = isLastStep || (remaining > -epsilon);
        }

        // handle the remaining part of the step, after all events if any
        if (stepHandler != null) {
            stepHandler.handleStep(interpolator, isLastStep);
        }

        return currentY;

    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any discrete events. It always stop exactly at
     * the specified date.</p>
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
                attitudeProvider.getAttitude(pvProvider, date, orbit.getFrame());

            return new SpacecraftState(orbit, attitude, getMass(date));

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** Extrapolate an orbit up to a specific target date.
     * @param date target date for the orbit
     * @return extrapolated parameters
     * @exception PropagationException if some parameters are out of bounds
     */
    protected abstract Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException;

    /** Get the mass.
     * @param date target date for the orbit
     * @return mass mass
     * @exception PropagationException if some parameters are out of bounds
     */
    protected abstract double getMass(final AbsoluteDate date)
        throws PropagationException;

    /** Internal PVCoordinatesProvider for attitude computation. */
    private class LocalPVProvider implements PVCoordinatesProvider, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -5121444553818793467L;

        /** {@inheritDoc} */
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagateOrbit(date).getPVCoordinates(frame);
        }

    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        initialState = state;
    }

    /** {@link BoundedPropagator} (but not really bounded) view of the instance. */
    private class UnboundedPropagatorView extends AbstractPropagator implements BoundedPropagator {

        /** Serializable UID. */
        private static final long serialVersionUID = -3340036098040553110L;

        /** Simple constructor.
         */
        public UnboundedPropagatorView() {
            super(AbstractPropagator.this.getAttitudeProvider());
        }

        /** {@inheritDoc} */
        public AbsoluteDate getMinDate() {
            return AbsoluteDate.PAST_INFINITY;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getMaxDate() {
            return AbsoluteDate.FUTURE_INFINITY;
        }

        /** {@inheritDoc} */
        protected Orbit propagateOrbit(final AbsoluteDate target)
            throws PropagationException {
            return AbstractPropagator.this.propagateOrbit(target);
        }

        /** {@inheritDoc} */
        public double getMass(final AbsoluteDate date) throws PropagationException {
            return AbstractPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        public void resetInitialState(final SpacecraftState state)
            throws PropagationException {
            AbstractPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        public SpacecraftState getInitialState() throws OrekitException {
            return AbstractPropagator.this.getInitialState();
        }

    }

    /** Internal class for local propagation. */
    private class BasicStepInterpolator implements OrekitStepInterpolator {

        /** Serializable UID. */
        private static final long serialVersionUID = 26269718303505539L;

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

        /** Additional states. */
        private Map<String, double[]> additionalStates;

        /** Forward propagation indicator. */
        private boolean forward;

        /** Build a new instance from a basic propagator.
         */
        public BasicStepInterpolator() {
            globalPreviousDate = AbsoluteDate.PAST_INFINITY;
            globalCurrentDate  = AbsoluteDate.PAST_INFINITY;
            softPreviousDate   = AbsoluteDate.PAST_INFINITY;
            softCurrentDate    = AbsoluteDate.PAST_INFINITY;
            additionalStates   = new HashMap<String, double[]>();
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
        public double[] getInterpolatedAdditionalState(final String name)
            throws OrekitException {
            final double[] state = additionalStates.get(name);
            if (state == null) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_EQUATION, name);
            }
            return state;
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
        public void setInterpolatedDate(final AbsoluteDate date)
            throws PropagationException {
            try {

                // compute the raw spacecraft state
                interpolatedState = basicPropagate(date);

                // compute additional states
                additionalStates.clear();
                for (final AdditionalStateProvider provider : additionalStateProviders) {
                    additionalStates.put(provider.getName(), provider.getAdditionalState(interpolatedState));
                }

            } catch (PropagationException pe) {
                // simply re-throw this exception which has the required type
            } catch (OrekitException oe) {
                // wrap other exceptions
                throw new PropagationException(oe);
            }
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
            forward     = globalCurrentDate.compareTo(globalPreviousDate) >= 0;
            setInterpolatedDate(globalCurrentDate);
        }

    }

}
