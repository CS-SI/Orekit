/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation;

import java.util.Collection;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

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

    /** Propagation mode. */
    private int mode;

    /** Fixed step size. */
    private double fixedStepSize;

    /** Step handler. */
    private OrekitStepHandler stepHandler;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Build a new instance.
     */
    protected AbstractPropagator() {
        setSlaveMode();
    }

    /** Set a start date.
     * @param startDate start date
     */
    protected void setStartDate(final AbsoluteDate startDate) {
        this.startDate = startDate;
    }

    /** Get the start date.
     * @return start date
     */
    protected AbsoluteDate getStartDate() {
        return startDate;
    }

    /**  {@inheritDoc} */
    public AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /**  {@inheritDoc} */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws PropagationException {
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
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        initialState = state;
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
        setMasterMode(new OrekitStepNormalizer(h, handler));
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

    /** Get the fixed step size.
     * @return fixed step size (or NaN if there are no fixed step size).
     */
    protected double getFixedStepSize() {
        return fixedStepSize;
    }

    /** Get the step handler.
     * @return step handler
     */
    protected OrekitStepHandler getStepHandler() {
        return stepHandler;
    }

    /** {@inheritDoc} */
    public abstract BoundedPropagator getGeneratedEphemeris();

    /** {@inheritDoc} */
    public abstract void addEventDetector(final EventDetector detector);

    /** {@inheritDoc} */
    public abstract Collection<EventDetector> getEventsDetectors();

    /** {@inheritDoc} */
    public abstract void clearEventsDetectors();

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
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

}
