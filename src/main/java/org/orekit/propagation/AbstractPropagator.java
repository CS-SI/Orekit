/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Common handling of {@link Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator}
 * methods, including all propagation modes support and discrete events support for
 * any simple propagation method.
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

    /** Additional state providers. */
    private final List<AdditionalStateProvider> additionalStateProviders;

    /** States managed by neither additional equations nor state providers. */
    private final Map<String, TimeSpanMap<double[]>> unmanagedStates;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Build a new instance.
     */
    protected AbstractPropagator() {
        mode                     = SLAVE_MODE;
        stepHandler              = null;
        fixedStepSize            = Double.NaN;
        additionalStateProviders = new ArrayList<AdditionalStateProvider>();
        unmanagedStates          = new HashMap<>();
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
    public SpacecraftState getInitialState() {
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
    public void resetInitialState(final SpacecraftState state) {
        initialState = state;
        setStartDate(state.getDate());
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

    /** {@inheritDoc} */
    @Override
    public void setEphemerisMode(final OrekitStepHandler handler) {
        mode          = EPHEMERIS_GENERATION_MODE;
        stepHandler   = handler;
        fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    public void addAdditionalStateProvider(final AdditionalStateProvider additionalStateProvider) {

        // check if the name is already used
        if (isAdditionalStateManaged(additionalStateProvider.getName())) {
            // this additional state is already registered, complain
            throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE,
                                      additionalStateProvider.getName());
        }

        // this is really a new name, add it
        additionalStateProviders.add(additionalStateProvider);

    }

    /** {@inheritDoc} */
    public List<AdditionalStateProvider> getAdditionalStateProviders() {
        return Collections.unmodifiableList(additionalStateProviders);
    }

    /** Update state by adding all additional states.
     * @param original original state
     * @return updated state, with all additional states included
     * @see #addAdditionalStateProvider(AdditionalStateProvider)
     */
    protected SpacecraftState updateAdditionalStates(final SpacecraftState original) {

        // start with original state,
        // which may already contain additional states, for example in interpolated ephemerides
        SpacecraftState updated = original;

        // update the states not managed by providers
        for (final Map.Entry<String, TimeSpanMap<double[]>> entry : unmanagedStates.entrySet()) {
            updated = updated.addAdditionalState(entry.getKey(),
                                                 entry.getValue().get(original.getDate()));
        }

        // update the additional states managed by providers
        for (final AdditionalStateProvider provider : additionalStateProviders) {
            updated = updated.addAdditionalState(provider.getName(),
                                                 provider.getAdditionalState(updated));
        }

        return updated;

    }

    /** {@inheritDoc} */
    public boolean isAdditionalStateManaged(final String name) {
        for (final AdditionalStateProvider provider : additionalStateProviders) {
            if (provider.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public String[] getManagedAdditionalStates() {
        final String[] managed = new String[additionalStateProviders.size()];
        for (int i = 0; i < managed.length; ++i) {
            managed[i] = additionalStateProviders.get(i).getName();
        }
        return managed;
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
    public abstract <T extends EventDetector> void addEventDetector(T detector);

    /** {@inheritDoc} */
    public abstract Collection<EventDetector> getEventsDetectors();

    /** {@inheritDoc} */
    public abstract void clearEventsDetectors();

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate target) {
        if (startDate == null) {
            startDate = getInitialState().getDate();
        }
        return propagate(startDate, target);
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Initialize propagation.
     * @since 10.1
     */
    protected void initializePropagation() {

        unmanagedStates.clear();

        if (initialState != null) {
            // there is an initial state
            // (null initial states occur for example in interpolated ephemerides)
            // copy the additional states present in initialState but otherwise not managed
            for (final Map.Entry<String, double[]> initial : initialState.getAdditionalStates().entrySet()) {
                if (!isAdditionalStateManaged(initial.getKey())) {
                    // this additional state is in the initial state, but is unknown to the propagator
                    // we store it in a way event handlers may change it
                    unmanagedStates.put(initial.getKey(), new TimeSpanMap<>(initial.getValue()));
                }
            }
        }
    }

    /** Notify about a state change.
     * @param state new state
     */
    protected void stateChanged(final SpacecraftState state) {
        final AbsoluteDate date    = state.getDate();
        final boolean      forward = date.durationFrom(getStartDate()) >= 0.0;
        for (final Map.Entry<String, double[]> changed : state.getAdditionalStates().entrySet()) {
            final TimeSpanMap<double[]> tsm = unmanagedStates.get(changed.getKey());
            if (tsm != null) {
                // this is an unmanaged state
                if (forward) {
                    tsm.addValidAfter(changed.getValue(), date);
                } else {
                    tsm.addValidBefore(changed.getValue(), date);
                }
            }
        }
    }

}
