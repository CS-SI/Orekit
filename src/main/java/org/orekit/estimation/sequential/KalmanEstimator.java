/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.extended.ExtendedKalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.propagation.conversion.IntegratedPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;


/**
 * Implementation of a Kalman filter to perform orbit determination.
 * <p>
 * The filter uses a {@link IntegratedPropagatorBuilder} to initialize its reference trajectory {@link NumericalPropagator}
 * or {@link DSSTPropagator} .
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
 * All the variables seen by Hipparchus (states, covariances, measurement matrices...) are normalized
 * using a specific scale for each estimated parameters or standard deviation noise for each measurement components.
 * </p>
 *
 * <p>A {@link KalmanEstimator} object is built using the {@link KalmanEstimatorBuilder#build() build}
 * method of a {@link KalmanEstimatorBuilder}.</p>
 *
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Luc Maisonobe
 * @since 9.2
 */
public class KalmanEstimator {

    /** Builders for orbit propagators. */
    private List<IntegratedPropagatorBuilder> propagatorBuilders;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Kalman filter process model. */
    private final KalmanODModel processModel;

    /** Filter. */
    private final ExtendedKalmanFilter<MeasurementDecorator> filter;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Kalman filter estimator constructor (package private).
     * @param decomposer decomposer to use for the correction phase
     * @param propagatorBuilders propagators builders used to evaluate the orbit.
     * @param processNoiseMatricesProviders providers for process noise matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     */
    KalmanEstimator(final MatrixDecomposer decomposer,
                    final List<IntegratedPropagatorBuilder> propagatorBuilders,
                    final List<CovarianceMatrixProvider> processNoiseMatricesProviders,
                    final ParameterDriversList estimatedMeasurementParameters) {

        this.propagatorBuilders = propagatorBuilders;
        this.referenceDate      = propagatorBuilders.get(0).getInitialOrbitDate();
        this.observer           = null;

        // Build the process model and measurement model
        this.processModel = propagatorBuilders.get(0).buildKalmanModel(propagatorBuilders,
                                                                   processNoiseMatricesProviders,
                                                                   estimatedMeasurementParameters);
        //this.processModel = new KalmanModel(propagatorBuilders, processNoiseMatricesProviders,
                                      //estimatedMeasurementParameters);

        this.filter = new ExtendedKalmanFilter<>(decomposer, processModel, processModel.getEstimate());

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
     * with one builder generating Keplerian orbits the names would be
     * simply "a", "e", "i"... but if there are several builders the
     * names would be "a[0]", "e[0]", "i[0]"..."a[1]", "e[1]", "i[1]"...
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
     * @return estimated propagators
     */
    public AbstractIntegratedPropagator[] estimationStep(final ObservedMeasurement<?> observedMeasurement) {
        try {
            final ProcessEstimate estimate = filter.estimationStep(decorate(observedMeasurement));
            processModel.finalizeEstimation(observedMeasurement, estimate);
            if (observer != null) {
                observer.evaluationPerformed(processModel);
            }
            return processModel.getEstimatedPropagators();
        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }

    /** Process several measurements.
     * @param observedMeasurements the measurements to process in <em>chronologically sorted</em> order
     * @return estimated propagators
     */
    public AbstractIntegratedPropagator[] processMeasurements(final Iterable<ObservedMeasurement<?>> observedMeasurements) {
        AbstractIntegratedPropagator[] propagators = null;
        for (ObservedMeasurement<?> observedMeasurement : observedMeasurements) {
            propagators = estimationStep(observedMeasurement);
        }
        return propagators;
    }

    /** Decorate an observed measurement.
     * <p>
     * The "physical" measurement noise matrix is the covariance matrix of the measurement.
     * Normalizing it consists in applying the following equation: Rn[i,j] =  R[i,j]/σ[i]/σ[j]
     * Thus the normalized measurement noise matrix is the matrix of the correlation coefficients
     * between the different components of the measurement.
     * </p>
     * @param observedMeasurement the measurement
     * @return decorated measurement
     */
    private MeasurementDecorator decorate(final ObservedMeasurement<?> observedMeasurement) {

        // Normalized measurement noise matrix contains 1 on its diagonal and correlation coefficients
        // of the measurement on its non-diagonal elements.
        // Indeed, the "physical" measurement noise matrix is the covariance matrix of the measurement
        // Normalizing it leaves us with the matrix of the correlation coefficients
        final RealMatrix covariance;
        if (observedMeasurement instanceof PV) {
            // For PV measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final PV pv = (PV) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(pv.getCorrelationCoefficientsMatrix());
        } else {
            // For other measurements we do not have a covariance matrix.
            // Thus the correlation coefficients matrix is an identity matrix.
            covariance = MatrixUtils.createRealIdentityMatrix(observedMeasurement.getDimension());
        }

        return new MeasurementDecorator(observedMeasurement, covariance, referenceDate);

    }

}
