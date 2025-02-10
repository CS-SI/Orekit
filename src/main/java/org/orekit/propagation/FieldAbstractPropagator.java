/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.sampling.FieldStepHandlerMultiplexer;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldTimeSpanMap;

/** Common handling of {@link Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator}
 * methods, including all propagation modes support and discrete events support for
 * any simple propagation method.
 * </p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 */
public abstract class FieldAbstractPropagator<T extends CalculusFieldElement<T>> implements FieldPropagator<T> {

    /** Multiplexer for step handlers. */
    private final FieldStepHandlerMultiplexer<T> multiplexer;

    /** Start date. */
    private FieldAbsoluteDate<T> startDate;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Additional data providers. */
    private final List<FieldAdditionalDataProvider<T>> additionalDataProviders;

    /** States managed by neither additional equations nor state providers. */
    private final Map<String, FieldTimeSpanMap<T[], T>> unmanagedStates;

    /** Field used.*/
    private final Field<T> field;

    /** Initial state. */
    private FieldSpacecraftState<T> initialState;

    /** Build a new instance.
     * @param field setting the field
     */
    protected FieldAbstractPropagator(final Field<T> field) {
        this.field               = field;
        multiplexer              = new FieldStepHandlerMultiplexer<>();
        additionalDataProviders  = new ArrayList<>();
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
    public Frame getFrame() {
        return initialState.getFrame();
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        initialState = state;
        setStartDate(state.getDate());
    }

    /** {@inheritDoc} */
    public FieldStepHandlerMultiplexer<T> getMultiplexer() {
        return multiplexer;
    }

    /** {@inheritDoc} */
    public void addAdditionalDataProvider(final FieldAdditionalDataProvider<T> additionalDataProvider) {

        // check if the name is already used
        if (isAdditionalDataManaged(additionalDataProvider.getName())) {
            // this additional data is already registered, complain
            throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE,
                                      additionalDataProvider.getName());
        }

        // this is really a new name, add it
        additionalDataProviders.add(additionalDataProvider);

    }

    /** {@inheritDoc} */
    public List<FieldAdditionalDataProvider<T>> getAdditionalDataProviders() {
        return Collections.unmodifiableList(additionalDataProviders);
    }

    /** Update state by adding unmanaged states.
     * @param original original state
     * @return updated state, with unmanaged states included
     * @see #updateAdditionalData(FieldSpacecraftState)
     */
    protected FieldSpacecraftState<T> updateUnmanagedData(final FieldSpacecraftState<T> original) {

        // start with original state,
        // which may already contain additional states, for example in interpolated ephemerides
        FieldSpacecraftState<T> updated = original;

        // update the states not managed by providers
        for (final Map.Entry<String, FieldTimeSpanMap<T[], T>> entry : unmanagedStates.entrySet()) {
            updated = updated.addAdditionalData(entry.getKey(),
                                                 entry.getValue().get(original.getDate()));
        }

        return updated;

    }

    /** Update state by adding all additional data.
     * @param original original state
     * @return updated state, with all additional data included
     * @see #addAdditionalDataProvider(FieldAdditionalDataProvider)
     */
    public FieldSpacecraftState<T> updateAdditionalData(final FieldSpacecraftState<T> original) {

        // start with original state and unmanaged states
        FieldSpacecraftState<T> updated = updateUnmanagedData(original);

        // set up queue for providers
        final Queue<FieldAdditionalDataProvider<T>> pending = new LinkedList<>(getAdditionalDataProviders());

        // update the additional data managed by providers, taking care of dependencies
        int yieldCount = 0;
        while (!pending.isEmpty()) {
            final FieldAdditionalDataProvider<T> provider = pending.remove();
            if (provider.yields(updated)) {
                // this generator has to wait for another one,
                // we put it again in the pending queue
                pending.add(provider);
                if (++yieldCount >= pending.size()) {
                    // all pending providers yielded!, they probably need data not yet initialized
                    // we let the propagation proceed, if these data are really needed right now
                    // an appropriate exception will be triggered when caller tries to access them
                    break;
                }
            } else {
                // we can use this provider right now
                updated    = provider.update(updated);
                yieldCount = 0;
            }
        }

        return updated;

    }

    /**
     * Initialize the additional data providers at the start of propagation.
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     * @since 11.2
     */
    protected void initializeAdditionalData(final FieldAbsoluteDate<T> target) {
        for (final FieldAdditionalDataProvider<T> provider : additionalDataProviders) {
            provider.init(initialState, target);
        }
    }

    /** {@inheritDoc} */
    public boolean isAdditionalDataManaged(final String name) {
        for (final FieldAdditionalDataProvider<T> provider : additionalDataProviders) {
            if (provider.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public String[] getManagedAdditionalData() {
        final String[] managed = new String[additionalDataProviders.size()];
        for (int i = 0; i < managed.length; ++i) {
            managed[i] = additionalDataProviders.get(i).getName();
        }
        return managed;
    }

    /** {@inheritDoc} */
    public FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> target) {
        if (startDate == null) {
            startDate = getInitialState().getDate();
        }
        return propagate(startDate, target);
    }

    /** Initialize propagation.
     * @since 10.1
     */
    protected void initializePropagation() {

        unmanagedStates.clear();

        if (initialState != null) {
            // there is an initial state
            // (null initial states occur for example in interpolated ephemerides)
            // copy the additional data present in initialState but otherwise not managed
            for (final FieldArrayDictionary<T>.Entry initial : initialState.getAdditionalDataValues().getData()) {
                if (!isAdditionalDataManaged(initial.getKey())) {
                    // this additional state is in the initial state, but is unknown to the propagator
                    // we store it in a way event handlers may change it
                    unmanagedStates.put(initial.getKey(),
                                        new FieldTimeSpanMap<>(initial.getValue(),
                                                               initialState.getDate().getField()));
                }
            }
        }
    }

    /** Notify about a state change.
     * @param state new state
     */
    protected void stateChanged(final FieldSpacecraftState<T> state) {
        final FieldAbsoluteDate<T> date    = state.getDate();
        final boolean              forward = date.durationFrom(getStartDate()).getReal() >= 0.0;
        for (final  FieldArrayDictionary<T>.Entry changed : state.getAdditionalDataValues().getData()) {
            final FieldTimeSpanMap<T[], T> tsm = unmanagedStates.get(changed.getKey());
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
