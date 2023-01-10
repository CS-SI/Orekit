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
package org.orekit.estimation.sequential;

import java.util.Collections;
import java.util.List;

import org.hipparchus.filtering.kalman.unscented.UnscentedKalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.util.UnscentedTransformProvider;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/**
 * Implementation of an Unscented Semi-analytical Kalman filter (USKF) to perform orbit determination.
 * <p>
 * The filter uses a {@link DSSTPropagatorBuilder}.
 * </p>
 * <p>
 * The estimated parameters are driven by {@link ParameterDriver} objects. They are of 3 different types:<ol>
 *   <li><b>Orbital parameters</b>:The position and velocity of the spacecraft, or, more generally, its orbit.<br>
 *       These parameters are retrieved from the reference trajectory propagator builder when the filter is initialized.</li>
 *   <li><b>Propagation parameters</b>: Some parameters modeling physical processes (SRP or drag coefficients etc...).<br>
 *       They are also retrieved from the propagator builder during the initialization phase.</li>
 *   <li><b>Measurements parameters</b>: Parameters related to measurements (station biases, positions etc...).<br>
 *       They are passed down to the filter in its constructor.</li>
 * </ol>
 * <p>
 * The Kalman filter implementation used is provided by the underlying mathematical library Hipparchus.
 * All the variables seen by Hipparchus (states, covariances...) are normalized
 * using a specific scale for each estimated parameters or standard deviation noise for each measurement components.
 * </p>
 *
 * <p>An {@link SemiAnalyticalUnscentedKalmanEstimator} object is built using the {@link SemiAnalyticalUnscentedKalmanEstimatorBuilder#build() build}
 * method of a {@link SemiAnalyticalUnscentedKalmanEstimatorBuilder}.</p>
 *
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class SemiAnalyticalUnscentedKalmanEstimator extends AbstractKalmanEstimator {

    /** Unscented Kalman filter process model. */
    private final SemiAnalyticalUnscentedKalmanModel processModel;

    /** Filter. */
    private final UnscentedKalmanFilter<MeasurementDecorator> filter;

    /** Unscented Kalman filter estimator constructor (package private).
     * @param decomposer decomposer to use for the correction phase
     * @param propagatorBuilder propagator builder used to evaluate the orbit.
     * @param processNoiseMatricesProvider provider for process noise matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     * @param utProvider provider for the unscented transform
     */
    SemiAnalyticalUnscentedKalmanEstimator(final MatrixDecomposer decomposer,
                                           final DSSTPropagatorBuilder propagatorBuilder,
                                           final CovarianceMatrixProvider processNoiseMatricesProvider,
                                           final ParameterDriversList estimatedMeasurementParameters,
                                           final CovarianceMatrixProvider measurementProcessNoiseMatrix,
                                           final UnscentedTransformProvider utProvider) {
        super(Collections.singletonList(propagatorBuilder));
        // Build the process model and measurement model
        this.processModel = new SemiAnalyticalUnscentedKalmanModel(propagatorBuilder, processNoiseMatricesProvider,
                                                                   estimatedMeasurementParameters, measurementProcessNoiseMatrix);

        // Unscented Kalman Filter of Hipparchus
        this.filter = new UnscentedKalmanFilter<>(decomposer, processModel, processModel.getEstimate(), utProvider);

    }

    /** {@inheritDoc}. */
    @Override
    protected KalmanEstimation getKalmanEstimation() {
        return processModel;
    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.processModel.setObserver(observer);
    }

    /** Process a single measurement.
     * <p>
     * Update the filter with the new measurement by calling the estimate method.
     * </p>
     * @param observedMeasurements the list of measurements to process
     * @return estimated propagators
     */
    public DSSTPropagator processMeasurements(final List<ObservedMeasurement<?>> observedMeasurements) {
        return processModel.processMeasurements(observedMeasurements, filter);
    }

}

