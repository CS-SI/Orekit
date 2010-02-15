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

import java.util.Collection;

import org.apache.commons.math.ConvergenceException;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.CombinedEventsDetectorsManager;
import org.orekit.propagation.sampling.BasicStepInterpolator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

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
public abstract class AbstractPropagator implements Propagator {

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

    /** Internal steps interpolator. */
    private final BasicStepInterpolator interpolator;

    /** Build a new instance.
     */
    protected AbstractPropagator() {
        eventsDetectorsManager = new CombinedEventsDetectorsManager();
        interpolator = new BasicStepInterpolator(new UnboundedPropagatorView());
        setSlaveMode();
    }

    /** {@inheritDoc} */
    public int getMode() {
        return mode;
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
    public SpacecraftState propagate(final AbsoluteDate target)
        throws PropagationException {
        try {

            // initial state
            interpolator.storeDate(getInitialState().getDate());
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
                addEndDateChecker(getInitialState().getDate(), target, eventsDetectorsManager);

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

            }

            // return the last computed state
            return state;

        } catch (OrekitException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(oe.getMessage(), oe);

        } catch (ConvergenceException ce) {
            throw new PropagationException(ce.getMessage(), ce);
        }
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** {@inheritDoc} */
    public abstract SpacecraftState getInitialState();

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any discrete events. It always stop exactly at
     * the specified date.</p>
     * @param date target date for propagation
     * @return state at specified date
     * @exception PropagationException if propagation cannot reach specified date
     */
    protected abstract SpacecraftState basicPropagate(final AbsoluteDate date)
        throws PropagationException;

    /** {@link BoundedPropagator} (but not really bounded) view of the instance. */
    private class UnboundedPropagatorView implements BoundedPropagator {

        /** Serializable UID. */
        private static final long serialVersionUID = -3340036098040553110L;

        /** {@inheritDoc} */
        public AbsoluteDate getMinDate() {
            return AbsoluteDate.PAST_INFINITY;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getMaxDate() {
            return AbsoluteDate.FUTURE_INFINITY;
        }

        /** {@inheritDoc} */
        public SpacecraftState propagate(final AbsoluteDate target)
            throws PropagationException {
            return basicPropagate(target);
        }

        /** {@inheritDoc} */
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {
            return propagate(date).getPVCoordinates(frame);
        }


    }

    /** Add an event handler for end date checking.
     * <p>This method can be used to simplify handling of integration end date.
     * It leverages the nominal stop condition with the exceptional stop
     * conditions.</p>
     * @param startDate propagation start date
     * @param endDate desired end date
     * @param manager manager containing the user-defined handlers
     * @return a new manager containing all the user-defined handlers plus a
     * dedicated manager triggering a stop event at entDate
     */
    protected CombinedEventsDetectorsManager addEndDateChecker(final AbsoluteDate startDate,
                                                               final AbsoluteDate endDate,
                                                               final CombinedEventsDetectorsManager manager) {
        final CombinedEventsDetectorsManager newManager = new CombinedEventsDetectorsManager();
        for (final EventDetector detector : manager.getEventsDetectors()) {
            newManager.addEventDetector(detector);
        }
        final double dt = endDate.durationFrom(startDate);
        newManager.addEventDetector(new EndDateDetector(endDate, Double.POSITIVE_INFINITY, Math.ulp(dt)));
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

}
