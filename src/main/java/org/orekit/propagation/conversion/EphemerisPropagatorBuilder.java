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

import java.util.List;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.AbstractKalmanModel;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.KalmanModel;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.utils.ParameterDriversList;

/** Builder for Ephemeris propagator.
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class EphemerisPropagatorBuilder extends AbstractPropagatorBuilder implements OrbitDeterminationPropagatorBuilder {

    /** Default position scale (not used for ephemeris based estimation). */
    private static final double DEFAULT_SCALE = 10.0;

    /** List of spacecraft states. */
    private final List<SpacecraftState> states;

    /** The extrapolation threshold beyond which the propagation will fail. **/
    private final double extrapolationThreshold;

    /** Number of points to use in interpolation. */
    private final int interpolationPoints;

    /** Attitude provider. */
    private final AttitudeProvider provider;

    /** Constructor.
     * @param states list of spacecraft states
     * @param interpolationPoints number of points to use in interpolation
     * @param extrapolationThreshold the extrapolation threshold beyond which the propagation will fail
     * @param attitudeProvider attitude provider
     */
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states,
                                      final int interpolationPoints,
                                      final double extrapolationThreshold,
                                      final AttitudeProvider attitudeProvider) {
        super(states.get(0).getOrbit(), PositionAngle.TRUE, DEFAULT_SCALE, false, attitudeProvider);
        deselectDynamicParameters();
        this.states                 = states;
        this.interpolationPoints    = interpolationPoints;
        this.extrapolationThreshold = extrapolationThreshold;
        this.provider               = attitudeProvider;
    }

    /** {@inheritDoc}. */
    @Override
    public Propagator buildPropagator(final double[] normalizedParameters) {
        return new Ephemeris(states, interpolationPoints, extrapolationThreshold, provider);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractBatchLSModel buildLSModel(final OrbitDeterminationPropagatorBuilder[] builders,
                                             final List<ObservedMeasurement<?>> measurements,
                                             final ParameterDriversList estimatedMeasurementsParameters,
                                             final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractKalmanModel buildKalmanModel(final List<OrbitDeterminationPropagatorBuilder> propagatorBuilders,
                                                final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                                final ParameterDriversList estimatedMeasurementsParameters,
                                                final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        return new KalmanModel(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementsParameters, measurementProcessNoiseMatrix);
    }

}
