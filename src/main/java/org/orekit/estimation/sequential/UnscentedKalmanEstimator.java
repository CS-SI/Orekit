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

import java.util.List;

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.unscented.UnscentedKalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.util.UnscentedTransformProvider;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
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
public class UnscentedKalmanEstimator extends AbstractKalmanEstimator {

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Unscented Kalman filter process model. */
    private final UnscentedKalmanModel processModel;

    /** Filter. */
    private final UnscentedKalmanFilter<MeasurementDecorator> filter;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

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
        super(propagatorBuilders);
        this.referenceDate = propagatorBuilders.get(0).getInitialOrbitDate();
        this.observer      = null;

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

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Process a single measurement.
     * <p>
     * Update the filter with the new measurement by calling the estimate method.
     * </p>
     * @param observedMeasurement the measurement to process
     * @return estimated propagator
     */
    public Propagator[] estimationStep(final ObservedMeasurement<?> observedMeasurement) {
        final ProcessEstimate estimate = filter.estimationStep(KalmanEstimatorUtil.decorateUnscented(observedMeasurement, referenceDate));
        processModel.finalizeEstimation(observedMeasurement, estimate);
        if (observer != null) {
            observer.evaluationPerformed(processModel);
        }
        return processModel.getEstimatedPropagators();
    }

    /** Process several measurements.
     * @param observedMeasurements the measurements to process in <em>chronologically sorted</em> order
     * @return estimated propagator
     */
    public Propagator[] processMeasurements(final Iterable<ObservedMeasurement<?>> observedMeasurements) {
        Propagator[] propagators = null;
        for (ObservedMeasurement<?> observedMeasurement : observedMeasurements) {
            propagators = estimationStep(observedMeasurement);
        }
        return propagators;
    }

}
