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
package org.orekit.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.sampling.StepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
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

    /** Multiplexer for step handlers. */
    private StepHandlerMultiplexer multiplexer;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Providers for additional states. */
    private final List<AdditionalStateProvider> additionalStateProviders;

    /** States managed by no generators. */
    private final Map<String, TimeSpanMap<double[]>> unmanagedStates;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Harvester for State Transition Matrix and Jacobian matrix. */
    private AbstractMatricesHarvester harvester;

    /** Build a new instance.
     */
    protected AbstractPropagator() {
        multiplexer              = new StepHandlerMultiplexer();
        additionalStateProviders = new ArrayList<>();
        unmanagedStates          = new HashMap<>();
        harvester                = null;
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
    public Frame getFrame() {
        return initialState.getFrame();
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        initialState = state;
        setStartDate(state.getDate());
    }

    /** {@inheritDoc} */
    public StepHandlerMultiplexer getMultiplexer() {
        return multiplexer;
    }

    /** {@inheritDoc} */
    @Override
    public void addAdditionalStateProvider(final AdditionalStateProvider provider) {

        // check if the name is already used
        if (isAdditionalStateManaged(provider.getName())) {
            // this additional state is already registered, complain
            throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE,
                                      provider.getName());
        }

        // this is really a new name, add it
        additionalStateProviders.add(provider);

    }

    /** {@inheritDoc} */
    @Override
    public List<AdditionalStateProvider> getAdditionalStateProviders() {
        return Collections.unmodifiableList(additionalStateProviders);
    }

    /** {@inheritDoc} */
    @Override
    public MatricesHarvester setupMatricesComputation(final String stmName, final RealMatrix initialStm,
                                                      final DoubleArrayDictionary initialJacobianColumns) {
        if (stmName == null) {
            throw new OrekitException(OrekitMessages.NULL_ARGUMENT, "stmName");
        }
        harvester = createHarvester(stmName, initialStm, initialJacobianColumns);
        return harvester;
    }

    /** Create the harvester suitable for propagator.
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     * @return harvester to retrieve computed matrices during and after propagation
     * @since 11.1
     */
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // FIXME: not implemented as of 11.1
        throw new UnsupportedOperationException();
    }

    /** Get the harvester.
     * @return harvester, or null if it was not created
     * @since 11.1
     */
    protected AbstractMatricesHarvester getHarvester() {
        return harvester;
    }

    /** Update state by adding unmanaged states.
     * @param original original state
     * @return updated state, with unmanaged states included
     * @see #updateAdditionalStates(SpacecraftState)
     */
    protected SpacecraftState updateUnmanagedStates(final SpacecraftState original) {

        // start with original state,
        // which may already contain additional states, for example in interpolated ephemerides
        SpacecraftState updated = original;

        // update the states not managed by providers
        for (final Map.Entry<String, TimeSpanMap<double[]>> entry : unmanagedStates.entrySet()) {
            updated = updated.addAdditionalState(entry.getKey(),
                                                 entry.getValue().get(original.getDate()));
        }

        return updated;

    }

    /** Update state by adding all additional states.
     * @param original original state
     * @return updated state, with all additional states included
     * (including {@link #updateUnmanagedStates(SpacecraftState) unmanaged} states)
     * @see #addAdditionalStateProvider(AdditionalStateProvider)
     * @see #updateUnmanagedStates(SpacecraftState)
     */
    protected SpacecraftState updateAdditionalStates(final SpacecraftState original) {

        // start with original state and unmanaged states
        SpacecraftState updated = updateUnmanagedStates(original);

        // set up queue for providers
        final Queue<AdditionalStateProvider> pending = new LinkedList<>(getAdditionalStateProviders());

        // update the additional states managed by providers, taking care of dependencies
        int yieldCount = 0;
        while (!pending.isEmpty()) {
            final AdditionalStateProvider provider = pending.remove();
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
                updated    = updated.addAdditionalState(provider.getName(), provider.getAdditionalState(updated));
                yieldCount = 0;
            }
        }

        return updated;

    }

    /**
     * Initialize the additional state providers at the start of propagation.
     * @param target date of propagation. Not equal to {@code initialState.getDate()}.
     * @since 11.2
     */
    protected void initializeAdditionalStates(final AbsoluteDate target) {
        for (final AdditionalStateProvider provider : additionalStateProviders) {
            provider.init(initialState, target);
        }
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
            for (final DoubleArrayDictionary.Entry initial : initialState.getAdditionalStatesValues().getData()) {
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
        for (final DoubleArrayDictionary.Entry changed : state.getAdditionalStatesValues().getData()) {
            final TimeSpanMap<double[]> tsm = unmanagedStates.get(changed.getKey());
            if (tsm != null) {
                // this is an unmanaged state
                if (forward) {
                    tsm.addValidAfter(changed.getValue(), date, false);
                } else {
                    tsm.addValidBefore(changed.getValue(), date, false);
                }
            }
        }
    }

}
