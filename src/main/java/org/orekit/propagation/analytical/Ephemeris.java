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
package org.orekit.propagation.analytical;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.StateCovariance;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeStampedPair;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ImmutableTimeStampedCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class is designed to accept and handle tabulated orbital entries. Tabulated entries are classified and then
 * extrapolated in way to obtain continuous output, with accuracy and computation methods configured by the user.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 */
public class Ephemeris extends AbstractAnalyticalPropagator implements BoundedPropagator {

    /** First date in range. */
    private final AbsoluteDate minDate;

    /** Last date in range. */
    private final AbsoluteDate maxDate;

    /** Reference frame. */
    private final Frame frame;

    /** Names of the additional states. */
    private final String[] additional;

    /** List of spacecraft states. */
    private final transient ImmutableTimeStampedCache<SpacecraftState> statesCache;

    /** List of covariances. **/
    private final transient ImmutableTimeStampedCache<StateCovariance> covariancesCache;

    /** Spacecraft state interpolator. */
    private final transient TimeInterpolator<SpacecraftState> stateInterpolator;

    /** State covariance interpolator. */
    private final transient TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator;

    /** Flag defining if states are defined using an orbit or an absolute position-velocity-acceleration. */
    private final transient boolean statesAreOrbitDefined;

    /**
     * Legacy constructor with tabulated states and default Hermite interpolation.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20 points)
     * in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and numerical
     * problems (including NaN appearing).
     *
     * @param states list of spacecraft states
     * @param interpolationPoints number of points to use in interpolation
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     * @see #Ephemeris(List, TimeInterpolator, List, TimeInterpolator)
     * @see SpacecraftStateInterpolator
     */
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints)
            throws MathIllegalArgumentException {
        // If states is empty an exception will be thrown in the other constructor
        this(states, new SpacecraftStateInterpolator(interpolationPoints,
                                                     states.get(0).getFrame(),
                                                     states.get(0).getFrame()),
             new ArrayList<>(), null);
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
     * @see #Ephemeris(List, TimeInterpolator, List, TimeInterpolator)
     */
    public Ephemeris(final List<SpacecraftState> states, final TimeInterpolator<SpacecraftState> stateInterpolator)
            throws MathIllegalArgumentException {
        this(states, stateInterpolator, new ArrayList<>(), null);
    }

    /**
     * Constructor with tabulated states.
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param attitudeProvider attitude law to use, null by default
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     * @see #Ephemeris(List, TimeInterpolator, List, TimeInterpolator)
     */
    public Ephemeris(final List<SpacecraftState> states, final TimeInterpolator<SpacecraftState> stateInterpolator,
                     final AttitudeProvider attitudeProvider)
            throws MathIllegalArgumentException {
        this(states, stateInterpolator, new ArrayList<>(), null, attitudeProvider);
    }

    /**
     * Constructor with tabulated states and associated covariances.
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param covariances tabulated covariances associated to tabulated states ephemeris bounds to be doing extrapolation
     * @param covarianceInterpolator covariance interpolator
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     * @throws OrekitIllegalArgumentException if number of states is different from the number of covariances
     * @throws OrekitIllegalStateException    if dates between states and associated covariances are different
     * @see #Ephemeris(List, TimeInterpolator, List, TimeInterpolator, AttitudeProvider)
     * @since 9.0
     */
    public Ephemeris(final List<SpacecraftState> states,
                     final TimeInterpolator<SpacecraftState> stateInterpolator,
                     final List<StateCovariance> covariances,
                     final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator)
            throws MathIllegalArgumentException {
        this(states, stateInterpolator, covariances, covarianceInterpolator,
             // if states is empty an exception will be thrown in the other constructor
             states.isEmpty() ? null : FrameAlignedProvider.of(states.get(0).getFrame()));
    }

    /**
     * Constructor with tabulated states and associated covariances.
     * <p>
     * The user is expected to explicitly define an attitude provider if they want to use one. Otherwise, it is null by
     * default
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param covariances tabulated covariances associated to tabulated states
     * @param covarianceInterpolator covariance interpolator
     * @param attitudeProvider attitude law to use, null by default
     *
     * @throws MathIllegalArgumentException   if the number of states is smaller than the number of points to use in
     *                                        interpolation
     * @throws OrekitIllegalArgumentException if states are not defined the same way (orbit or absolute
     *                                        position-velocity-acceleration)
     * @throws OrekitIllegalArgumentException if number of states is different from the number of covariances
     * @throws OrekitIllegalStateException    if dates between states and associated covariances are different
     * @since 10.1
     */
    public Ephemeris(final List<SpacecraftState> states,
                     final TimeInterpolator<SpacecraftState> stateInterpolator,
                     final List<StateCovariance> covariances,
                     final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator,
                     final AttitudeProvider attitudeProvider)
            throws MathIllegalArgumentException {

        super(attitudeProvider);

        // Check input consistency
        checkInputConsistency(states, stateInterpolator, covariances, covarianceInterpolator);

        // Initialize variables
        final SpacecraftState s0 = states.get(0);
        minDate = s0.getDate();
        maxDate = states.get(states.size() - 1).getDate();
        frame   = s0.getFrame();

        final List<DoubleArrayDictionary.Entry> as = s0.getAdditionalStatesValues().getData();
        additional = new String[as.size()];
        for (int i = 0; i < additional.length; ++i) {
            additional[i] = as.get(i).getKey();
        }

        this.statesCache       = new ImmutableTimeStampedCache<>(stateInterpolator.getNbInterpolationPoints(), states);
        this.stateInterpolator = stateInterpolator;

        this.covarianceInterpolator = covarianceInterpolator;
        if (covarianceInterpolator != null) {
            this.covariancesCache = new ImmutableTimeStampedCache<>(covarianceInterpolator.getNbInterpolationPoints(),
                                                                    covariances);
        } else {
            this.covariancesCache = null;
        }
        this.statesAreOrbitDefined = s0.isOrbitDefined();

        // Initialize initial state
        super.resetInitialState(getInitialState());
    }

    /**
     * Check input consistency between states, covariances and their associated interpolators.
     *
     * @param states spacecraft states sample
     * @param stateInterpolator spacecraft state interpolator
     * @param covariances covariances sample
     * @param covarianceInterpolator covariance interpolator
     */
    public static void checkInputConsistency(final List<SpacecraftState> states,
                                             final TimeInterpolator<SpacecraftState> stateInterpolator,
                                             final List<StateCovariance> covariances,
                                             final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator) {
        // Checks to perform is states are provided
        if (!states.isEmpty()) {
            // Check given that given states definition are consistent
            // (all defined by either orbits or absolute position-velocity-acceleration coordinates)
            SpacecraftStateInterpolator.checkStatesDefinitionsConsistency(states);

            // Check that every interpolator used in the state interpolator are compatible with the sample size
            AbstractTimeInterpolator.checkInterpolatorCompatibilityWithSampleSize(stateInterpolator, states.size());

            // Additional checks if covariances are provided
            if (!covariances.isEmpty()) {
                // Check that every interpolator used in the state covariance interpolator are compatible with the sample size
                AbstractTimeInterpolator.checkInterpolatorCompatibilityWithSampleSize(covarianceInterpolator,
                                                                                      covariances.size());
                // Check states and covariances consistency
                checkStatesAndCovariancesConsistency(states, covariances);
            }
        }
        else {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA, 0);
        }
    }

    /**
     * Check that given states and covariances are consistent.
     *
     * @param states tabulates states to check
     * @param covariances tabulated covariances associated to tabulated states to check
     */
    public static void checkStatesAndCovariancesConsistency(final List<SpacecraftState> states,
                                                            final List<StateCovariance> covariances) {
        final int nbStates = states.size();

        // Check that we have an equal number of states and covariances
        if (nbStates != covariances.size()) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                     states.size(),
                                                     covariances.size());
        }

        // Check that states and covariance are defined at the same date
        for (int i = 0; i < nbStates; i++) {
            if (!states.get(i).getDate().isCloseTo(covariances.get(i).getDate(),
                                                   TimeStampedPair.DEFAULT_DATE_EQUALITY_THRESHOLD)) {
                throw new OrekitIllegalStateException(OrekitMessages.STATE_AND_COVARIANCE_DATES_MISMATCH,
                                                      states.get(i).getDate(), covariances.get(i).getDate());
            }
        }
    }

    /**
     * Get the first date of the range.
     *
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate() {
        return minDate;
    }

    /**
     * Get the last date of the range.
     *
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return maxDate;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return frame;
    }

    /**
     * Get the covariance at given date.
     * <p>
     * BEWARE : If this instance has been created without sample of covariances and/or with spacecraft states defined with
     * absolute position-velocity-acceleration, it will return an empty {@link Optional}.
     *
     * @param date date at which the covariance is desired
     *
     * @return covariance at given date
     *
     * @see Optional
     */
    public Optional<StateCovariance> getCovariance(final AbsoluteDate date) {
        if (covarianceInterpolator != null && covariancesCache != null && statesAreOrbitDefined) {

            // Build list of time stamped pair of orbits and their associated covariances
            final List<TimeStampedPair<Orbit, StateCovariance>> sample = buildOrbitAndCovarianceSample();

            // Interpolate
            final TimeStampedPair<Orbit, StateCovariance> interpolatedOrbitAndCovariance =
                    covarianceInterpolator.interpolate(date, sample);

            return Optional.of(interpolatedOrbitAndCovariance.getSecond());
        }
        else {
            return Optional.empty();
        }

    }

    /** @return sample of orbits and their associated covariances */
    private List<TimeStampedPair<Orbit, StateCovariance>> buildOrbitAndCovarianceSample() {
        final List<TimeStampedPair<Orbit, StateCovariance>> sample      = new ArrayList<>();
        final List<SpacecraftState>                         states      = statesCache.getAll();
        final List<StateCovariance>                         covariances = covariancesCache.getAll();
        for (int i = 0; i < states.size(); i++) {
            sample.add(new TimeStampedPair<>(states.get(i).getOrbit(), covariances.get(i)));
        }

        return sample;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState basicPropagate(final AbsoluteDate date) {

        final AbsoluteDate centralDate =
                AbstractTimeInterpolator.getCentralDate(date, statesCache, stateInterpolator.getExtrapolationThreshold());
        final SpacecraftState  evaluatedState   = stateInterpolator.interpolate(date, statesCache.getNeighbors(centralDate));
        final AttitudeProvider attitudeProvider = getAttitudeProvider();
        if (attitudeProvider == null) {
            return evaluatedState;
        }
        else {
            final Attitude calculatedAttitude;
            // Verify if orbit is defined
            if (evaluatedState.isOrbitDefined()) {
                calculatedAttitude =
                        attitudeProvider.getAttitude(evaluatedState.getOrbit(), date, evaluatedState.getFrame());
                return new SpacecraftState(evaluatedState.getOrbit(), calculatedAttitude, evaluatedState.getMass(),
                                           evaluatedState.getAdditionalStatesValues(),
                                           evaluatedState.getAdditionalStatesDerivatives());
            }
            else {
                calculatedAttitude =
                        attitudeProvider.getAttitude(evaluatedState.getAbsPVA(), date, evaluatedState.getFrame());
                return new SpacecraftState(evaluatedState.getAbsPVA(), calculatedAttitude, evaluatedState.getMass(),
                                           evaluatedState.getAdditionalStatesValues(),
                                           evaluatedState.getAdditionalStatesDerivatives());
            }

        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
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
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() {
        return basicPropagate(getMinDate());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAdditionalStateManaged(final String name) {

        // the additional state may be managed by a specific provider in the base class
        if (super.isAdditionalStateManaged(name)) {
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
    public String[] getManagedAdditionalStates() {
        final String[] upperManaged = super.getManagedAdditionalStates();
        final String[] managed      = new String[upperManaged.length + additional.length];
        System.arraycopy(upperManaged, 0, managed, 0, upperManaged.length);
        System.arraycopy(additional, 0, managed, upperManaged.length, additional.length);
        return managed;
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // In order to not throw an Orekit exception during ephemeris based orbit determination
        // The default behavior of the method is overridden to return a null parameter
        return null;
    }

    /** Get state interpolator.
     * @return state interpolator
     */
    public TimeInterpolator<SpacecraftState> getStateInterpolator() {
        return stateInterpolator;
    }

    /** Get covariance interpolator.
     * @return optional covariance interpolator
     * @see Optional
     */
    public Optional<TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>>> getCovarianceInterpolator() {
        return Optional.ofNullable(covarianceInterpolator);
    }

}
