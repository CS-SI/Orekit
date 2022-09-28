/* Copyright 2002-2022 CS GROUP
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
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.UnscentedTransformProvider;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/**
 * Implementation of an Unscented Kalman filter to perform orbit determination.
 * <p>
 * The filter uses a {@link NumericalPropagatorBuilder} to initialize its reference trajectory.
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
 * method of a {@link UnscentedKalmanEstimatorBuilder}.</p>
 *
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class UnscentedKalmanEstimator {

    /** Builders for orbit propagators. */
    private List<NumericalPropagatorBuilder> propagatorBuilders;

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
                             final List<NumericalPropagatorBuilder> propagatorBuilders,
                             final List<CovarianceMatrixProvider> processNoiseMatricesProviders,
                             final ParameterDriversList estimatedMeasurementParameters,
                             final CovarianceMatrixProvider measurementProcessNoiseMatrix,
                             final UnscentedTransformProvider utProvider) {

        this.propagatorBuilders = propagatorBuilders;
        this.referenceDate      = propagatorBuilders.get(0).getInitialOrbitDate();
        this.observer           = null;

        // Build the process model and measurement model
        this.processModel = new UnscentedKalmanModel(propagatorBuilders, processNoiseMatricesProviders,
                                                     estimatedMeasurementParameters, measurementProcessNoiseMatrix);

        this.filter = new UnscentedKalmanFilter<>(decomposer, processModel, processModel.getEstimate(), utProvider);

    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Get the current measurement number.
     * @return current measurement number
     */
    public int getCurrentMeasurementNumber() {
        return processModel.getCurrentMeasurementNumber();
    }

    /** Get the current date.
     * @return current date
     */
    public AbsoluteDate getCurrentDate() {
        return processModel.getCurrentDate();
    }

    /** Get the "physical" estimated state (i.e. not normalized)
     * @return the "physical" estimated state
     */
    public RealVector getPhysicalEstimatedState() {
        return processModel.getPhysicalEstimatedState();
    }

    /** Get the "physical" estimated covariance matrix (i.e. not normalized)
     * @return the "physical" estimated covariance matrix
     */
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        return processModel.getPhysicalEstimatedCovarianceMatrix();
    }

    /** Get the orbital parameters supported by this estimator.
     * <p>
     * If there are more than one propagator builder, then the names
     * of the drivers have an index marker in square brackets appended
     * to them in order to distinguish the various orbits. So for example
     * with one builder generating equinoctial orbits the names would be
     * simply "a", "ex", "ey"... but if there are several builders the
     * names would be "a[0]", "ex[0]", "ey[0]"..."a[1]", "ex[1]", "ey[1]"...
     * </p>
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     */
    public ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (int i = 0; i < propagatorBuilders.size(); ++i) {
            final String suffix = propagatorBuilders.size() > 1 ? "[" + i + "]" : null;
            for (final ParameterDriver driver : propagatorBuilders.get(i).getOrbitalParametersDrivers().getDrivers()) {
                if (driver.isSelected() || !estimatedOnly) {
                    if (suffix != null && !driver.getName().endsWith(suffix)) {
                        // we add suffix only conditionally because the method may already have been called
                        // and suffixes may have already been appended
                        driver.setName(driver.getName() + suffix);
                    }
                    estimated.add(driver);
                }
            }
        }
        return estimated;
    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     */
    public ParameterDriversList getPropagationParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (PropagatorBuilder builder : propagatorBuilders) {
            for (final DelegatingDriver delegating : builder.getPropagationParametersDrivers().getDrivers()) {
                if (delegating.isSelected() || !estimatedOnly) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        estimated.add(driver);
                    }
                }
            }
        }
        return estimated;
    }

    /** Get the list of estimated measurements parameters.
     * @return the list of estimated measurements parameters
     */
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return processModel.getEstimatedMeasurementsParameters();
    }

    /** Process a single measurement.
     * <p>
     * Update the filter with the new measurement by calling the estimate method.
     * </p>
     * @param observedMeasurement the measurement to process
     * @return estimated propagator
     */
    public Propagator[] estimationStep(final ObservedMeasurement<?> observedMeasurement) {
        final ProcessEstimate estimate = filter.estimationStep(KalmanEstimatorUtil.decorate(observedMeasurement, referenceDate));
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
