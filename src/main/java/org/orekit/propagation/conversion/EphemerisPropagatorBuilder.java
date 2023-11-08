/* Copyright 2022 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.conversion;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.StateCovariance;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeStampedPair;
import org.orekit.utils.ParameterDriversList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builder for Ephemeris propagator.
 *
 * @author Bryan Cazabonne
 * @author Vincent Cucchietti
 * @since 11.3
 */
public class EphemerisPropagatorBuilder extends AbstractPropagatorBuilder {

    /** Default position scale (not used for ephemeris based estimation). */
    private static final double DEFAULT_SCALE = 10.0;

    /** List of spacecraft states. */
    private final List<SpacecraftState> states;

    /** List of covariances. **/
    private final Optional<List<StateCovariance>> covariances;

    /** Spacecraft state interpolator. */
    private final TimeInterpolator<SpacecraftState> stateInterpolator;

    /** State covariance interpolator. */
    private final Optional<TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>>> covarianceInterpolator;

    /** Attitude provider. */
    private final AttitudeProvider provider;

    /**
     * Constructor using the default attitude provider.
     * <p>
     * The default attitude provider is an {@link org.orekit.attitudes.FrameAlignedProvider inertial provider} built from the frame of the first
     * spacecraft state instance in given list
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     */
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states,
                                      final TimeInterpolator<SpacecraftState> stateInterpolator) {
        this(states, stateInterpolator, states.isEmpty() ? null : new FrameAlignedProvider(states.get(0).getFrame()));
    }

    /**
     * Constructor.
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param attitudeProvider attitude law to use
     */
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states,
                                      final TimeInterpolator<SpacecraftState> stateInterpolator,
                                      final AttitudeProvider attitudeProvider) {
        this(states, stateInterpolator, new ArrayList<>(), null, attitudeProvider);
    }

    /**
     * Constructor with covariances and default attitude provider.
     * <p>
     * The default attitude provider is an {@link FrameAlignedProvider inertial provider} built from the frame of the first
     * spacecraft state instance in given list
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param covariances tabulated covariances associated to tabulated states
     * @param covarianceInterpolator covariance interpolator
     *
     * @see StateCovariance
     * @see FrameAlignedProvider
     */
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states,
                                      final TimeInterpolator<SpacecraftState> stateInterpolator,
                                      final List<StateCovariance> covariances,
                                      final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator) {
        this(states, stateInterpolator, covariances, covarianceInterpolator,
             states.isEmpty() ? null : new FrameAlignedProvider(states.get(0).getFrame()));
    }

    /**
     * Constructor.
     *
     * @param states list of spacecraft states
     * @param stateInterpolator spacecraft state interpolator
     * @param covariances tabulated covariances associated to tabulated states
     * @param covarianceInterpolator covariance interpolator
     * @param attitudeProvider attitude law to use
     */
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states,
                                      final TimeInterpolator<SpacecraftState> stateInterpolator,
                                      final List<StateCovariance> covariances,
                                      final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator,
                                      final AttitudeProvider attitudeProvider) {
        super(states.get(0).getOrbit(), PositionAngleType.TRUE, DEFAULT_SCALE, false, attitudeProvider);
        deselectDynamicParameters();

        // Check input consistency the same way Ephemeris is checking consistency
        Ephemeris.checkInputConsistency(states, stateInterpolator, covariances, covarianceInterpolator);

        this.states                 = states;
        this.stateInterpolator      = stateInterpolator;
        this.covariances            = Optional.ofNullable(covariances);
        this.covarianceInterpolator = Optional.ofNullable(covarianceInterpolator);
        this.provider               = attitudeProvider;
    }

    /** {@inheritDoc} */
    @Override
    public EphemerisPropagatorBuilder copy() {
        return new EphemerisPropagatorBuilder(states, stateInterpolator,
                                              covariances.orElse(null), covarianceInterpolator.orElse(null),
                                              provider);
    }

    /** {@inheritDoc}. */
    @Override
    public Propagator buildPropagator(final double[] normalizedParameters) {
        if (covariances.isPresent() && covarianceInterpolator.isPresent()) {
            return new Ephemeris(states, stateInterpolator, covariances.get(), covarianceInterpolator.get(), provider);
        }
        return new Ephemeris(states, stateInterpolator, provider);

    }

    /** {@inheritDoc} */
    @Override
    public AbstractBatchLSModel buildLeastSquaresModel(final PropagatorBuilder[] builders,
                                                       final List<ObservedMeasurement<?>> measurements,
                                                       final ParameterDriversList estimatedMeasurementsParameters,
                                                       final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

}
