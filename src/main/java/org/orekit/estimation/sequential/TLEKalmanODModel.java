/* Copyright 2002-2020 CS GROUP
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
package org.orekit.estimation.sequential;

import java.util.List;

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.extended.NonLinearEvolution;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.ODPropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * <p>
 * This class is an adaption of the {@link KalmanModel} class
 * but for the {@link TLEPropagator TLE propagator}.
 * </p>
 * <p>
 * This class is to write, dummy methods have been set up
 * in order to compile TLE OD.
 */

public class TLEKalmanODModel implements KalmanODModel {

    public TLEKalmanODModel (final List<ODPropagatorBuilder> propagatorBuilders,
                             final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                             final ParameterDriversList estimatedMeasurementsParameters) {

    }

    /** {@inheritDoc} */
    public ProcessEstimate getEstimate() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public AbstractIntegratedPropagator[] getEstimatedPropagators() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                            final ProcessEstimate estimate) {
        // TODO Auto-generated method stub

    }

    @Override
    public ParameterDriversList getEstimatedOrbitalParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ParameterDriversList getEstimatedPropagationParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SpacecraftState[] getPredictedSpacecraftStates() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SpacecraftState[] getCorrectedSpacecraftStates() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealVector getPhysicalEstimatedState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealMatrix getPhysicalStateTransitionMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealMatrix getPhysicalMeasurementJacobian() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealMatrix getPhysicalInnovationCovarianceMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealMatrix getPhysicalKalmanGain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getCurrentMeasurementNumber() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public AbsoluteDate getCurrentDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EstimatedMeasurement<?> getPredictedMeasurement() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EstimatedMeasurement<?> getCorrectedMeasurement() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NonLinearEvolution getEvolution(final double previousTime,
                                           final RealVector previousState,
                                           final MeasurementDecorator measurement) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement,
                                    final NonLinearEvolution evolution,
                                    final RealMatrix innovationCovarianceMatrix) {
        // TODO Auto-generated method stub
        return null;
    }
}
