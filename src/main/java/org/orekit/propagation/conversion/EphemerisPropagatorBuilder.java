/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.conversion;

import java.util.List;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.EphemerisBatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.AbstractKalmanModel;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.EphemerisKalmanModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Builder for Keplerian propagator.
 * @author Pascal Parraud
 * @author Nicolas Fialton (for orbit determination)
 */
public class EphemerisPropagatorBuilder implements PropagatorBuilder {

    /**
     * List of the spacecraft states.
     */
    private final List<SpacecraftState> states;

    /**Number of Interpolation points. */
    private final int interpolationPoints;

    /** Position angle.*/
    private PositionAngle positionAngle;

    /** The extrapolation threshold beyond which the propagation will fail. **/
    private final double extrapolationThreshold;

    /** Attittude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param states List of the spacecraft states
     * @param interpolationPoints number of iterpolations points used by the Ephemeris propagator
     * @param positionAngle the angle position (anomaly) of the spacecraft
     */
    @DefaultDataContext
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states, final int interpolationPoints, final PositionAngle positionAngle) {
        this(states, interpolationPoints, positionAngle,
             Ephemeris.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param states List of the spacecraft states
     * @param interpolationPoints number of iterpolations points used by the Ephemeris propagator
     * @param positionAngle the angle position (anomaly) of the spacecraft
     * @param extrapolationThreshold
     */
    @DefaultDataContext
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states, final int interpolationPoints, final PositionAngle positionAngle, final double extrapolationThreshold) {
        this(states, interpolationPoints, positionAngle,
             extrapolationThreshold, Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Build a new instance.
    *
    * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
    *
    * @param states List of the spacecraft states
    * @param interpolationPoints number of iterpolations points used by the Ephemeris propagator
    * @param positionAngle the angle position (anomaly) of the spacecraft
    * @param extrapolationThreshold
    * @param attitudeProvider
    */
    @DefaultDataContext
    public EphemerisPropagatorBuilder(final List<SpacecraftState> states, final int interpolationPoints, final PositionAngle positionAngle, final double extrapolationThreshold, final AttitudeProvider attitudeProvider) {
        this.states = states;
        this.interpolationPoints = interpolationPoints;
        this.positionAngle = positionAngle;
        this.extrapolationThreshold = extrapolationThreshold;
        this.attitudeProvider = attitudeProvider;
    }


    /** {@inheritDoc} */
    public Ephemeris buildPropagator(final double[] normalizedParameters) {

        // We don't have to set (orbital and propagation) parameters with the Ephemeris propagator therefore normalizedParameters is not used

        final Ephemeris propagator = new Ephemeris(states, interpolationPoints, extrapolationThreshold, attitudeProvider);
        propagator.resetInitialState(propagator.getInitialState());
        return propagator;
    }

    @Override
    public AbstractBatchLSModel buildLSModel(final PropagatorBuilder[] builders,
                                             final List<ObservedMeasurement<?>> measurements,
                                             final ParameterDriversList estimatedMeasurementsParameters,
                                             final ModelObserver observer) {
        return new EphemerisBatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    @Override
    public AbstractKalmanModel buildKalmanModel(final List<PropagatorBuilder> propagatorBuilders,
                                                final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                                final ParameterDriversList estimatedMeasurementsParameters,
                                                final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        return new EphemerisKalmanModel(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementsParameters, measurementProcessNoiseMatrix);
    }

    @Override
    public double[] getSelectedNormalizedParameters() {
        // We don't have to set (orbital and propagation) parameters with the Ephemeris propagator therefore normalizedParameters is not used
        return new double[0];
    }

    @Override
    public OrbitType getOrbitType() {
        // Orbit type must by the same for all elements in the list
        return states.get(0).getOrbit().getType();
    }

    @Override
    public PositionAngle getPositionAngle() {
        return positionAngle;
    }

    @Override
    public AbsoluteDate getInitialOrbitDate() {
        // TODO Can we realy use the first ?
        return states.get(0).getDate();
    }

    @Override
    public Frame getFrame() {
        // Orbit frame must by the same for all elements in the list
        return states.get(0).getFrame();
    }

    @Override
    public ParameterDriversList getOrbitalParametersDrivers() {
        return new ParameterDriversList();
    }

    @Override
    public ParameterDriversList getPropagationParametersDrivers() {
        return new ParameterDriversList();
    }

    @Override
    public void resetOrbit(final Orbit newOrbit) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

}

