/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.filtering.kalman.unscented.UnscentedKalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.util.UnscentedTransformProvider;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/**
 * Implementation of an Unscented Kalman filter to perform orbit determination.
 * <p>
 * The filter uses a {@link PropagatorBuilder} to initialize its reference trajectory.
 * </p>
 * <p>
 * The estimated parameters are driven by {@link ParameterDriver} objects. They are of 3 different types:<ol>
 *   <li><b>Orbital parameters</b>:The position and velocity of the spacecraft, or, more generally, its orbit.<br>
 *       These parameters are retrieved from the reference trajectory propagator builder when the filter is initialized.</li>
 *   <li><b>Propagation parameters</b>: Some parameters modelling physical processes (SRP or drag coefficients etc...).<br>
 *       They are also retrieved from the propagator builder during the initialization phase.</li>
 *   <li><b>Measurements parameters</b>: Parameters related to measurements (station biases, positions etc...).<br>
 *       They are passed down to the filter in its constructor.</li>
 * </ol>
 * <p>
 * The total number of estimated parameters is m, the size of the state vector.
 * </p>
 * <p>
 * The Kalman filter implementation used is provided by the underlying mathematical library Hipparchus.
 * </p>
 *
 * <p>An {@link UnscentedKalmanEstimator} object is built using the {@link UnscentedKalmanEstimatorBuilder#build() build}
 * method of a {@link UnscentedKalmanEstimatorBuilder}. The builder is generalized to accept any {@link PropagatorBuilder}.
 * Howerver, it is absolutely not recommended to use a {@link DSSTPropagatorBuilder}.
 * A specific {@link SemiAnalyticalUnscentedKalmanEstimatorBuilder semi-analytical unscented Kalman Filter} is implemented
 * and shall be used.
 * </p>
 *
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class UnscentedKalmanEstimator extends AbstractSequentialEstimator {

    /** Unscented Kalman filter process model. */
    private final UnscentedKalmanModel processModel;

    /** Filter. */
    private final UnscentedKalmanFilter<MeasurementDecorator> filter;

    /** Unscented Kalman filter estimator constructor (package private).
     * @param decomposer decomposer to use for the correction phase
     * @param propagatorBuilders propagators builders used to evaluate the orbit.
     * @param processNoiseMatricesProviders providers for process noise matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     * @param utProvider provider for the unscented transform.
     */
    UnscentedKalmanEstimator(final MatrixDecomposer decomposer,
                             final List<PropagatorBuilder> propagatorBuilders,
                             final List<CovarianceMatrixProvider> processNoiseMatricesProviders,
                             final ParameterDriversList estimatedMeasurementParameters,
                             final CovarianceMatrixProvider measurementProcessNoiseMatrix,
                             final UnscentedTransformProvider utProvider) {
        super(decomposer, propagatorBuilders);

        // Build the process model and measurement model
        this.processModel = new UnscentedKalmanModel(propagatorBuilders, processNoiseMatricesProviders,
                                                     estimatedMeasurementParameters, measurementProcessNoiseMatrix);

        this.filter = new UnscentedKalmanFilter<>(decomposer, processModel, processModel.getEstimate(), utProvider);

    }

    /** {@inheritDoc}. */
    @Override
    protected KalmanEstimation getKalmanEstimation() {
        return processModel;
    }

    /** {@inheritDoc}. */
    @Override
    protected KalmanFilter<MeasurementDecorator> getKalmanFilter() {
        return filter;
    }

    /** {@inheritDoc}. */
    @Override
    protected SequentialModel getProcessModel() {
        return processModel;
    }

}
