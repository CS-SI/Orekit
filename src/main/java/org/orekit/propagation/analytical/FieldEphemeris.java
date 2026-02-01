/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.analytical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.FieldSpacecraftStateInterpolator;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.time.AbstractFieldTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.FieldBoundedPVCoordinatesProvider;
import org.orekit.utils.FieldDataDictionary;
import org.orekit.utils.ImmutableFieldTimeStampedCache;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is designed to accept and handle tabulated orbital entries. Tabulated entries are classified and then
 * extrapolated in way to obtain continuous output, with accuracy and computation methods configured by the user.
 *
 * @see Ephemeris
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @author Romain Serra
 * @since 14.0
 */
public class FieldEphemeris<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T>
        implements FieldBoundedPVCoordinatesProvider<T> {

    /** First date in range. */
    private final FieldAbsoluteDate<T> minDate;

    /** Last date in range. */
    private final FieldAbsoluteDate<T> maxDate;

    /** Reference frame. */
    private final Frame frame;

    /** Names of the additional states. */
    private final String[] additional;

    /** List of spacecraft states. */
    private final ImmutableFieldTimeStampedCache<FieldSpacecraftState<T>, T> statesCache;

    /** Spacecraft state interpolator. */
    private final FieldTimeInterpolator<FieldSpacecraftState<T>, T> stateInterpolator;

    /**
     * Legacy constructor with tabulated states and default Hermite interpolation.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20 points)
     * in order to avoid <a href="ht@tps://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and numerical
     * problems (including NaN appearing).
     *
     * @param states list of spacecraft states
     * @param interpolationPoints number of points to use in interpolation
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     * @see SpacecraftStateInterpolator
     */
    public FieldEphemeris(final List<FieldSpacecraftState<T>> states, final int interpolationPoints)
            throws MathIllegalArgumentException {
        this(states, new FieldSpacecraftStateInterpolator<>(interpolationPoints,
                                                     states.get(0).getFrame(),
                                                     states.get(0).getFrame()));
    }

    /**
     * Constructor with tabulated states.
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     */
    public FieldEphemeris(final List<FieldSpacecraftState<T>> states, final FieldTimeInterpolator<FieldSpacecraftState<T>, T> stateInterpolator)
            throws MathIllegalArgumentException {
        this(states, stateInterpolator, new FrameAlignedProvider(states.get(0).getFrame()));
    }

    /**
     * Constructor with tabulated states.
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param attitudeProvider attitude law to use
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     */
    public FieldEphemeris(final List<FieldSpacecraftState<T>> states,
                          final FieldTimeInterpolator<FieldSpacecraftState<T>, T> stateInterpolator,
                          final AttitudeProvider attitudeProvider)
            throws MathIllegalArgumentException {
        super(states.get(0).getDate().getField(), attitudeProvider);

        // Check input consistency
        checkInputConsistency(states, stateInterpolator);

        // Initialize variables
        final FieldSpacecraftState<T> s0 = states.get(0);
        minDate = s0.getDate();
        maxDate = states.get(states.size() - 1).getDate();
        frame   = s0.getFrame();

        final List<FieldDataDictionary<T>.Entry> as = s0.getAdditionalDataValues().getData();
        additional = new String[as.size()];
        for (int i = 0; i < additional.length; ++i) {
            additional[i] = as.get(i).getKey();
        }

        this.statesCache       = new ImmutableFieldTimeStampedCache<>(stateInterpolator.getNbInterpolationPoints(), states);
        this.stateInterpolator = stateInterpolator;

        // Initialize initial state
        super.resetInitialState(getInitialState());
    }

    /**
     * Check input consistency between states and the interpolator.
     *
     * @param states spacecraft states sample
     * @param interpolator spacecraft state interpolator
     */
    public void checkInputConsistency(final List<FieldSpacecraftState<T>> states,
                                      final FieldTimeInterpolator<FieldSpacecraftState<T>, T> interpolator) {
        checkStatesDefinitionsConsistency(states);

        // Check that every interpolator used in the state interpolator are compatible with the sample size
        AbstractFieldTimeInterpolator.checkInterpolatorCompatibilityWithSampleSize(interpolator, states.size());
    }

    /**
     * Check that all state are either orbit defined or based on absolute position-velocity-acceleration.
     *
     * @param states spacecraft state sample
     */
    public void checkStatesDefinitionsConsistency(final List<FieldSpacecraftState<T>> states) {
        // Check all states handle the same additional states and are defined the same way (orbit or absolute PVA)
        final FieldSpacecraftState<T> s0               = states.get(0);
        final boolean         s0IsOrbitDefined = s0.isOrbitDefined();
        for (final FieldSpacecraftState<T> state : states) {
            s0.ensureCompatibleAdditionalStates(state);
            if (s0IsOrbitDefined != state.isOrbitDefined()) {
                throw new OrekitIllegalArgumentException(OrekitMessages.DIFFERENT_STATE_DEFINITION);
            }
        }
    }

    /**
     * Get the first date of the range.
     *
     * @return the first date of the range
     */
    public FieldAbsoluteDate<T> getMinDate() {
        return minDate;
    }

    /**
     * Get the last date of the range.
     *
     * @return the last date of the range
     */
    public FieldAbsoluteDate<T> getMaxDate() {
        return maxDate;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) {

        final FieldAbsoluteDate<T> centralDate =
                AbstractFieldTimeInterpolator.getCentralDate(date, statesCache, stateInterpolator.getExtrapolationThreshold());
        final FieldSpacecraftState<T>  evaluatedState   = stateInterpolator.interpolate(date, statesCache.getNeighbors(centralDate));
        final AttitudeProvider attitudeProvider = getAttitudeProvider();
        final FieldAttitude<T> calculatedAttitude;
        // Verify if orbit is defined
        if (evaluatedState.isOrbitDefined()) {
            calculatedAttitude =
                    attitudeProvider.getAttitude(evaluatedState.getOrbit(), date, evaluatedState.getFrame());
            return new FieldSpacecraftState<>(evaluatedState.getOrbit(), calculatedAttitude, evaluatedState.getMass(),
                                       evaluatedState.getMassRate(), evaluatedState.getAdditionalDataValues(),
                                       evaluatedState.getAdditionalStatesDerivatives());
        }
        else {
            calculatedAttitude =
                    attitudeProvider.getAttitude(evaluatedState.getAbsPVA(), date, evaluatedState.getFrame());
            return new FieldSpacecraftState<>(evaluatedState.getAbsPVA(), calculatedAttitude, evaluatedState.getMass(),
                                       evaluatedState.getMassRate(), evaluatedState.getAdditionalDataValues(),
                                       evaluatedState.getAdditionalStatesDerivatives());
        }
    }

    /** {@inheritDoc} */
    public FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return basicPropagate(date).getMass();
    }

    /**
     * Try (and fail) to reset the initial state.
     * <p>
     * This method always throws an exception, as ephemerides cannot be reset.
     * </p>
     *
     * @param state new initial state to consider
     */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        resetIntermediateState(state, true);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<T> getInitialState() {
        return basicPropagate(getMinDate());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAdditionalDataManaged(final String name) {

        // the additional state may be managed by a specific provider in the base class
        if (super.isAdditionalDataManaged(name)) {
            return true;
        }

        // the additional state may be managed in the states sample
        for (final String a : additional) {
            if (a.equals(name)) {
                return true;
            }
        }

        return false;

    }

    /** {@inheritDoc} */
    @Override
    public String[] getManagedAdditionalData() {
        final String[] upperManaged = super.getManagedAdditionalData();
        final String[] managed      = new String[upperManaged.length + additional.length];
        System.arraycopy(upperManaged, 0, managed, 0, upperManaged.length);
        System.arraycopy(additional, 0, managed, upperManaged.length, additional.length);
        return managed;
    }

    /** Get state interpolator.
     * @return state interpolator
     */
    public FieldTimeInterpolator<FieldSpacecraftState<T>, T> getStateInterpolator() {
        return stateInterpolator;
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return new ArrayList<>();
    }

}
