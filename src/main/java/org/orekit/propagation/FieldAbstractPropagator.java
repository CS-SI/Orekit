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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Common handling of {@link Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator}
 * methods, including all propagation modes support and discrete events support for
 * any simple propagation method.
 * </p>
 * @author Luc Maisonobe
 */
public abstract class FieldAbstractPropagator<T extends RealFieldElement<T>> implements FieldPropagator<T> {

    /** Propagation mode. */
    private int mode;

    /** Fixed step size. */
    private T fixedStepSize;

    /** Step handler. */
    private FieldOrekitStepHandler<T> stepHandler;

    /** Start date. */
    private FieldAbsoluteDate<T> startDate;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Additional state providers. */
    private final List<FieldAdditionalStateProvider<T>> additionalStateProviders;

    /** States managed by neither additional equations nor state providers. */
    private final Map<String, TimeSpanMap<T[]>> unmanagedStates;

    /** Field used.*/
    private final Field<T> field;

    /** Initial state. */
    private FieldSpacecraftState<T> initialState;

    /** Build a new instance.
     * @param field setting the field
     */
    protected FieldAbstractPropagator(final Field<T> field) {
        mode                     = SLAVE_MODE;
        stepHandler              = null;
        this.field               = field;
        fixedStepSize            = field.getZero().add(Double.NaN);
        additionalStateProviders = new ArrayList<>();
        unmanagedStates          = new HashMap<>();
    }

    /** Set a start date.
     * @param startDate start date
     */
    protected void setStartDate(final FieldAbsoluteDate<T> startDate) {
        this.startDate = startDate;
    }

    /** Get the start date.
     * @return start date
     */
    protected FieldAbsoluteDate<T> getStartDate() {
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

    /** Field getter.
     * @return field used*/
    public Field<T> getField() {
        return field;
    }

    /** {@inheritDoc} */
    public FieldSpacecraftState<T> getInitialState() {
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
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        initialState = state;
        setStartDate(state.getDate());
    }

    /** {@inheritDoc} */
    public void setSlaveMode() {
        mode          = SLAVE_MODE;
        stepHandler   = null;
        fixedStepSize = field.getZero().add(Double.NaN);
    }

    /** {@inheritDoc} */
    public void setMasterMode(final T h,
                              final FieldOrekitFixedStepHandler<T> handler) {
        setMasterMode(new FieldOrekitStepNormalizer<>(h, handler));
        fixedStepSize = h;
    }

    /** {@inheritDoc} */
    public void setMasterMode(final FieldOrekitStepHandler<T> handler) {
        mode          = MASTER_MODE;
        stepHandler   = handler;
        fixedStepSize = field.getZero().add(Double.NaN);
    }

    /** {@inheritDoc} */
    public void setEphemerisMode() {
        mode          = EPHEMERIS_GENERATION_MODE;
        stepHandler   = null;
        fixedStepSize = field.getZero().add(Double.NaN);
    }

    /** {@inheritDoc} */
    public void addAdditionalStateProvider(final FieldAdditionalStateProvider<T> additionalStateProvider) {

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
    public List<FieldAdditionalStateProvider<T>> getAdditionalStateProviders() {
        return Collections.unmodifiableList(additionalStateProviders);
    }

    /** Update state by adding all additional states.
     * @param original original state
     * @return updated state, with all additional states included
     * @see #addAdditionalStateProvider(FieldAdditionalStateProvider)
     */
    protected FieldSpacecraftState<T> updateAdditionalStates(final FieldSpacecraftState<T> original) {

        // start with original state,
        // which may already contain additional states, for example in interpolated ephemerides
        FieldSpacecraftState<T> updated = original;

        // update the states not managed by providers
        for (final Map.Entry<String, TimeSpanMap<T[]>> entry : unmanagedStates.entrySet()) {
            updated = updated.addAdditionalState(entry.getKey(),
                                                 entry.getValue().get(original.getDate().toAbsoluteDate()));
        }

        // update the additional states managed by providers
        for (final FieldAdditionalStateProvider<T> provider : additionalStateProviders) {

            updated = updated.addAdditionalState(provider.getName(),
                                                 provider.getAdditionalState(updated));
        }

        return updated;

    }

    /** {@inheritDoc} */
    public boolean isAdditionalStateManaged(final String name) {
        for (final FieldAdditionalStateProvider<T> provider : additionalStateProviders) {
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
    protected T getFixedStepSize() {
        return fixedStepSize;
    }

    /** Get the step handler.
     * @return step handler
     */
    protected FieldOrekitStepHandler<T> getStepHandler() {
        return stepHandler;
    }

    /** {@inheritDoc} */
    public abstract FieldBoundedPropagator<T> getGeneratedEphemeris();

    /** {@inheritDoc} */
    public abstract <D extends FieldEventDetector<T>> void addEventDetector(D detector);

    /** {@inheritDoc} */
    public abstract Collection<FieldEventDetector<T>> getEventsDetectors();

    /** {@inheritDoc} */
    public abstract void clearEventsDetectors();

    /** {@inheritDoc} */
    public FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> target) {
        if (startDate == null) {
            startDate = getInitialState().getDate();
        }
        return propagate(startDate, target);
    }

    /** {@inheritDoc} */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
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
            for (final Map.Entry<String, T[]> initial : initialState.getAdditionalStates().entrySet()) {
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
    protected void stateChanged(final FieldSpacecraftState<T> state) {
        final AbsoluteDate date    = state.getDate().toAbsoluteDate();
        final boolean      forward = date.durationFrom(getStartDate().toAbsoluteDate()) >= 0.0;
        for (final Map.Entry<String, T[]> changed : state.getAdditionalStates().entrySet()) {
            final TimeSpanMap<T[]> tsm = unmanagedStates.get(changed.getKey());
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
