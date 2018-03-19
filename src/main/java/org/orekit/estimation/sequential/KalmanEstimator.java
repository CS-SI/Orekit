/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.extended.ExtendedKalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;


/**
 * Implementation of a Kalman filter to perform orbit determination.
 * <p>
 * The filter uses a {@link NumericalPropagatorBuilder} to initialize its reference trajectory {@link NumericalPropagator}.
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
 * </p>
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

    /** Builder for a numerical propagator. */
    private NumericalPropagatorBuilder propagatorBuilder;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Kalman filter process model. */
    private final Model processModel;

    /** Filter. */
    private final ExtendedKalmanFilter<MeasurementDecorator> filter;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Kalman filter estimator constructor (package private).
     * @param decomposer decomposer to use for the correction phase
     * @param propagatorBuilder propagator builder used to evaluate the orbit.
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param physicalInitialCovariance physical initial covariance matrix
     * @param physicalInitialProcessNoiseMatrix physical process noise matrix
     * @throws OrekitException propagation exception.
     */
    KalmanEstimator(final MatrixDecomposer decomposer,
                    final NumericalPropagatorBuilder propagatorBuilder,
                    final ParameterDriversList estimatedMeasurementParameters,
                    final RealMatrix physicalInitialCovariance,
                    final RealMatrix physicalInitialProcessNoiseMatrix)
        throws OrekitException {

        this.propagatorBuilder = propagatorBuilder;
        this.referenceDate     = propagatorBuilder.getInitialOrbitDate();
        this.observer          = null;

        // Build the process model and measurement model
        this.processModel = new Model(propagatorBuilder,
                                      getOrbitalParametersDrivers(true),
                                      getPropagationParametersDrivers(true),
                                      estimatedMeasurementParameters,
                                      physicalInitialCovariance,
                                      physicalInitialProcessNoiseMatrix);

        this.filter = new ExtendedKalmanFilter<>(decomposer, processModel, processModel.getNormalizedInitialEstimate());

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
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly)
        throws OrekitException {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (final DelegatingDriver delegating : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            if (delegating.isSelected() || !estimatedOnly) {
                for (final ParameterDriver driver : delegating.getRawDrivers()) {
                    estimated.add(driver);
                }
            }
        }
        return estimated;
    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public ParameterDriversList getPropagationParametersDrivers(final boolean estimatedOnly)
        throws OrekitException {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (final DelegatingDriver delegating : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
            if (delegating.isSelected() || !estimatedOnly) {
                for (final ParameterDriver driver : delegating.getRawDrivers()) {
                    estimated.add(driver);
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
     * @throws OrekitException if an error occurred during the estimation
     */
    public NumericalPropagator estimationStep(final ObservedMeasurement<?> observedMeasurement)
        throws OrekitException {
        try {
            filter.estimationStep(decorate(observedMeasurement));
            processModel.finalizeEstimation(observedMeasurement);
            if (observer != null) {
                observer.evaluationPerformed(processModel);
            }
            return processModel.getEstimatedPropagator();
        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        } catch (OrekitExceptionWrapper oew) {
            throw oew.getException();
        }
    }

    /** Process several measurements.
     * @param observedMeasurements the measurements to process in <em>chronologically sorted</em> order
     * @return a stream of estimated propagators
     * @throws OrekitException if an error occurred during the estimation
     */
    public NumericalPropagator processMeasurements(final Iterable<ObservedMeasurement<?>> observedMeasurements)
        throws OrekitException {
        NumericalPropagator propagator = null;
        for (ObservedMeasurement<?> observedMeasurement : observedMeasurements) {
            propagator = estimationStep(observedMeasurement);
        }
        return propagator;
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
