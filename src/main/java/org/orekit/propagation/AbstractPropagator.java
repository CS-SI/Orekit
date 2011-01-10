/* Copyright 2002-2010 CS Communication & Systèmes
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.CombinedEventsDetectorsManager;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventObserver;
import org.orekit.propagation.events.OccurredEvent;
import org.orekit.propagation.numerical.AdditionalEquations;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Common handling of {@link Propagator} methods for analytical-like propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator}
 * methods, including all propagation modes support and discrete events support
 * for any simple propagation method. Only three methods must be implemented by
 * derived classes: {@link #getInitialState()}, {@link #basicPropagate(AbsoluteDate)}
 * and {@link #resetInitialState(SpacecraftState)}. The second method should perform
 * straightforward propagation starting from some internally stored initial state
 * up to the specified target date. The third method should reset the initial state
 * when called.
 * </p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public abstract class AbstractPropagator implements Propagator, EventObserver {

    /** Serializable UID. */
    private static final long serialVersionUID = 4797122381575498520L;

    /** Propagation mode. */
    private int mode;

    /** Fixed step size. */
    private double fixedStepSize;

    /** Step handler. */
    private OrekitStepHandler stepHandler;

    /** Manager for events detectors. */
    private final CombinedEventsDetectorsManager eventsDetectorsManager;

    /** List for occurred events. */
    private final List <OccurredEvent> occurredEvents;

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


    /** Build a new instance.
     * @param attitudeProvider provider for attitude computation
     */
    protected AbstractPropagator(final AttitudeProvider attitudeProvider) {
        eventsDetectorsManager = new CombinedEventsDetectorsManager(this);
        occurredEvents         = new ArrayList<OccurredEvent>();
        interpolator           = new BasicStepInterpolator();
        this.pvProvider        = new LocalPVProvider();
        this.attitudeProvider  = attitudeProvider;
        setSlaveMode();
    }

    /** Get attitude provider.
     * @return attitude provider
     */
    public AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /** Set attitude provider.
     * @param attitudeProvider attitude provider
     */
    protected void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
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
        eventsDetectorsManager.addEventDetector(detector);
    }

    /** {@inheritDoc} */
    public Collection<EventDetector> getEventsDetectors() {
        return eventsDetectorsManager.getEventsDetectors();
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        eventsDetectorsManager.clearEventsDetectors();
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

            interpolator.storeDate(start);
            SpacecraftState state = interpolator.getInterpolatedState();

            // evaluate step size
            final double stepSize;
            if (mode == MASTER_MODE) {
                if (Double.isNaN(fixedStepSize)) {
                    stepSize = state.getKeplerianPeriod() / 100;
                } else {
                    stepSize = fixedStepSize;
                }
            } else {
                stepSize = target.durationFrom(interpolator.getCurrentDate());
            }
            final CombinedEventsDetectorsManager manager =
                addEndDateChecker(start, target, eventsDetectorsManager);

            // iterate over the propagation range
            AbsoluteDate stepEnd = interpolator.getCurrentDate().shiftedBy(stepSize);
            for (boolean lastStep = false; !lastStep;) {

                interpolator.shift();
                boolean needUpdate = false;

                // attempt to perform one step, with the current step size
                // (this may loop if some discrete event is triggered)
                for (boolean loop = true; loop;) {

                    // go ahead one step size
                    interpolator.storeDate(stepEnd);

                    // check discrete events
                    if (manager.evaluateStep(interpolator)) {
                        needUpdate = true;
                        stepEnd = manager.getEventTime();
                    } else {
                        loop = false;
                    }

                }

                // handle the accepted step
                state = interpolator.getInterpolatedState();
                manager.stepAccepted(state);
                if (manager.stop()) {
                    lastStep = true;
                } else {
                    lastStep = stepEnd.compareTo(target) >= 0;
                }
                if (stepHandler != null) {
                    stepHandler.handleStep(interpolator, lastStep);
                }

                // let the events detectors reset the state if needed
                final SpacecraftState newState = manager.reset(state);
                if (newState != state) {
                    resetInitialState(newState);
                    state = newState;
                }

                if (needUpdate) {
                    // an event detector has reduced the step
                    // we need to adapt step size for next iteration
                    stepEnd = interpolator.getPreviousDate().shiftedBy(stepSize);
                } else {
                    stepEnd = interpolator.getCurrentDate().shiftedBy(stepSize);
                }
                if (interpolator.isForward()) {
                    if (stepEnd.compareTo(target) > 0) {
                        stepEnd = target;
                    }
                } else {
                    if (stepEnd.compareTo(target) < 0) {
                        stepEnd = target;
                    }
                }

            }

            // return the last computed state
            startDate = state.getDate();
            return state;

        } catch (OrekitException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(oe);

        } catch (ConvergenceException ce) {
            throw new PropagationException(ce, ce.getGeneralPattern(), ce.getArguments());
        }
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
    private class LocalPVProvider implements PVCoordinatesProvider {
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

    /** {@inheritDoc} */
    public void notify(final SpacecraftState s, final EventDetector detector) {
        // Add occurred event to occurred events list
        occurredEvents.add(new OccurredEvent(s, detector));
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

    /** Add an event handler for end date checking.
     * <p>This method can be used to simplify handling of integration end date.
     * It leverages the nominal stop condition with the exceptional stop
     * conditions.</p>
     * @param start propagation start date
     * @param end desired end date
     * @param manager manager containing the user-defined handlers
     * @return a new manager containing all the user-defined handlers plus a
     * dedicated manager triggering a stop event at entDate
     */
    protected CombinedEventsDetectorsManager addEndDateChecker(final AbsoluteDate start,
                                                               final AbsoluteDate end,
                                                               final CombinedEventsDetectorsManager manager) {
        final CombinedEventsDetectorsManager newManager = new CombinedEventsDetectorsManager(this);
        for (final EventDetector detector : manager.getEventsDetectors()) {
            newManager.addEventDetector(detector);
        }
        final double dt = end.durationFrom(start);
        newManager.addEventDetector(new EndDateDetector(end, Double.POSITIVE_INFINITY,
                                                        FastMath.ulp(dt)));
        return newManager;
    }

    /** Specialized event handler to stop integration. */
    private static class EndDateDetector extends AbstractDetector {

        /** Serializable version identifier. */
        private static final long serialVersionUID = -7950598937797923427L;

        /** Desired end date. */
        private final AbsoluteDate endDate;

        /** Build an instance.
         * @param endDate desired end date
         * @param maxCheck maximal check interval
         * @param threshold convergence threshold
         */
        public EndDateDetector(final AbsoluteDate endDate,
                               final double maxCheck, final double threshold) {
            super(maxCheck, threshold);
            this.endDate = endDate;
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s, final boolean increasing) {
            return STOP;
        }

        /** {@inheritDoc} */
        public double g(final SpacecraftState s) {
            return s.getDate().durationFrom(endDate);
        }

    }

    /** Internal class for local propagation. */
    private class BasicStepInterpolator implements OrekitStepInterpolator {

        /** Serializable UID. */
        private static final long serialVersionUID = 1585604180933740215L;

        /** Previous date. */
        private AbsoluteDate previousDate;

        /** Current date. */
        private AbsoluteDate currentDate;

        /** Interpolated State. */
        private SpacecraftState interpolatedState;

        /** Forward propagation indicator. */
        private boolean forward;

        /** Build a new instance from a basic propagator.
         */
        public BasicStepInterpolator() {
            previousDate     = AbsoluteDate.PAST_INFINITY;
            currentDate      = AbsoluteDate.PAST_INFINITY;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getCurrentDate() {
            return currentDate;
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
        public double[] getInterpolatedAdditionalState(final AdditionalEquations addEqu)
            throws OrekitException {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_EQUATION);
        }

        /** {@inheritDoc} */
        public AbsoluteDate getPreviousDate() {
            return previousDate;
        }

        /** {@inheritDoc} */
        public boolean isForward() {
            return forward;
        }

        /** {@inheritDoc} */
        public void setInterpolatedDate(final AbsoluteDate date)
            throws PropagationException {
            interpolatedState = basicPropagate(date);
        }

        /** Shift one step forward.
         * Copy the current date into the previous date, hence preparing the
         * interpolator for future calls to {@link #storeDate storeDate}
         */
        public void shift() {
            previousDate = currentDate;
        }

        /** Store the current step date.
         * @param date current date
         * @exception PropagationException if the state cannot be propagated at specified date
         */
        public void storeDate(final AbsoluteDate date)
            throws PropagationException {
            currentDate = date;
            forward     = currentDate.compareTo(previousDate) >= 0;
            setInterpolatedDate(currentDate);
        }

    }

}
