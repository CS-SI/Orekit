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

import org.hipparchus.filtering.kalman.KalmanSmoother;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.ArrayList;
import java.util.List;

/** Perform an RTS (Rauch-Tung-Striebel) smoothing step over results from a sequential estimator.
 *
 * <p>The Kalman and Unscented sequential estimators produce a state (mean and covariance) after processing each
 * measurement.  This state is a statistical summary of all the information provided to the filter, from the
 * measurements and model of the spacecraft motion, up until the latest measurement.  A smoother produces estimates that
 * are summaries of information over <em>all</em> measurements, both past and future.</p>
 *
 * <p>For example, if a filter processes
 * measurements from time 1 to 10, then the filter state at time 5 uses measurement information up to time 5, while
 * the smoother state at time 5 uses measurement information from the entire interval, times 1 to 10.  This typically
 * results in more accurate estimates, with more information reducing the uncertainty.</p>
 *
 * <p>This smoother is implemented using the {@link KalmanObserver} mechanism.  The
 * smoother collects data from the forward estimation over the measurements, then applies a backward pass to
 * calculate the smoothed estimates.  Smoothed estimates are collected into a list of
 * {@link PhysicalEstimatedState}, containing a timestamp, mean and covariance over all estimated parameters
 * (orbital, propagation and measurement).  The order of the parameters in these states is the same as the
 * underlying sequential estimator, for example from a call to {@link KalmanEstimator#getPhysicalEstimatedState()}.</p>
 *
 * <p>The smoother is compatible with the Kalman and Unscented sequential estimators, but does not support the
 * semi-analytical equivalents.</p>
 *
 * <p>The following code snippet demonstrates how to attach the smoother to a filter and retrieve smoothed states:</p>
 *
 * <pre>
 *     // Build the Kalman filter
 *     final KalmanEstimator kalmanEstimator = new KalmanEstimatorBuilder().
 *         addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
 *         build();
 *
 *     // Add smoother observer to filter
 *     final RtsSmoother rtsSmoother = new RtsSmoother(kalmanEstimator);
 *     kalmanEstimator.setObserver(rtsSmoother);
 *
 *     // Perform forward filtering over the measurements
 *     Propagator[] estimated = kalmanEstimator.processMeasurements(measurements);
 *
 *     // Perform backwards smoothing and collect the results
 *     rtsSmoother.backwardsSmooth();
 * </pre>
 *
 * <p>Note that the smoother stores data from every filter step, leading to high memory usage for long-duration runs
 * with numerous measurements.</p>
 *
 * @see KalmanEstimatorBuilder
 * @see UnscentedKalmanEstimatorBuilder
 * @see "S&auml;rkk&auml; S. Bayesian Filtering and Smoothing. Cambridge University Press, 2013."
 * @author Mark Rutten
 * @since 13.0
 */
public class RtsSmoother implements KalmanObserver {

    /** Smoother. */
    private final KalmanSmoother smoother;

    /** Estimator reference date. */
    private final AbsoluteDate referenceDate;

    /** Covariance scales for unnormalising estimates. */
    private final double[] covarianceScale;

    /** Estimated orbital parameters. */
    private final ParameterDriversList estimatedOrbitalParameters;

    /** Estimated propagation drivers. */
    private final ParameterDriversList estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Reference states for unnormalising estimates. */
    private final List<RealVector> referenceStates;

    /** Smoother observer constructor from a sequential estimator.
     * This smoother constructor requires access to the underlying estimator to initialise some information not
     * available from {@link KalmanEstimation} during {@link RtsSmoother#init}, including the estimated parameters
     * drivers (orbital, propagation and measurements).
     * @param estimator the Kalman estimator
     */
    public RtsSmoother(final AbstractKalmanEstimator estimator) {
        this.smoother = new KalmanSmoother(estimator.getMatrixDecomposer());
        this.referenceDate = estimator.getReferenceDate();
        this.covarianceScale = estimator.getScale();
        this.estimatedOrbitalParameters = estimator.getOrbitalParametersDrivers(true);
        this.estimatedPropagationParameters = estimator.getPropagationParametersDrivers(true);
        this.estimatedMeasurementsParameters = estimator.getEstimatedMeasurementsParameters();
        this.referenceStates = new ArrayList<>();

        // Add smoother observer to underlying filter
        estimator.getKalmanFilter().setObserver(smoother);
    }

    /** {@inheritDoc}
     */
    @Override
    public void init(final KalmanEstimation estimation) {
        // Get the first reference state
        referenceStates.add(getReferenceState());
    }

    /** {@inheritDoc} This accumulates the filter states as the sequential estimator processes measurements.
     */
    @Override
    public void evaluationPerformed(final KalmanEstimation estimation) {
        referenceStates.add(getReferenceState());
    }

    /** Perform a RTS backwards smoothing recursion over the filtered states collected by the observer.
     * @return a list of {@link PhysicalEstimatedState}
     */
    public List<PhysicalEstimatedState> backwardsSmooth() {

        // Backwards smoothing step
        final List<ProcessEstimate> normalisedStates = smoother.backwardsSmooth();

        // Convert to physical states
        final List<PhysicalEstimatedState> smoothedStates = new ArrayList<>();
        for (int timeIndex = 0; timeIndex < normalisedStates.size(); ++timeIndex) {
            final ProcessEstimate normalisedState =  normalisedStates.get(timeIndex);
            final RealVector physicalState =
                    getPhysicalState(normalisedState.getState(), referenceStates.get(timeIndex));
            final RealMatrix physicalCovariance =
                    KalmanEstimatorUtil.unnormalizeCovarianceMatrix(normalisedState.getCovariance(), covarianceScale);
            smoothedStates.add(new PhysicalEstimatedState(
                    referenceDate.shiftedBy(normalisedState.getTime()),
                    physicalState,
                    physicalCovariance
            ));
        }
        return smoothedStates;
    }

    /** Get reference values from the estimation parameter drivers.
     * @return the reference state
     */
    private RealVector getReferenceState() {
        final RealVector referenceState = MatrixUtils.createRealVector(covarianceScale.length);
        int i = 0;
        for (final ParameterDriversList.DelegatingDriver driver : estimatedOrbitalParameters.getDrivers()) {
            referenceState.setEntry(i++, driver.getReferenceValue());
        }
        for (final ParameterDriversList.DelegatingDriver driver : estimatedPropagationParameters.getDrivers()) {
            referenceState.setEntry(i++, driver.getReferenceValue());
        }
        for (final ParameterDriversList.DelegatingDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            referenceState.setEntry(i++, driver.getReferenceValue());
        }
        return referenceState;
    }


    /** Get reference values from the estimation parameter drivers.
     * @param normalisedState the normalised state
     * @param referenceState the reference state
     * @return the reference state
     */
    private RealVector getPhysicalState(final RealVector normalisedState, final RealVector referenceState) {
        final RealVector physicalState = MatrixUtils.createRealVector(covarianceScale.length);
        int i = 0;
        for (final ParameterDriversList.DelegatingDriver driver : estimatedOrbitalParameters.getDrivers()) {
            physicalState.setEntry(i, setResetDriver(driver, referenceState.getEntry(i), normalisedState.getEntry(i)));
            i += 1;
        }
        for (final ParameterDriversList.DelegatingDriver driver : estimatedPropagationParameters.getDrivers()) {
            physicalState.setEntry(i, setResetDriver(driver, referenceState.getEntry(i), normalisedState.getEntry(i)));
            i += 1;
        }
        for (final ParameterDriversList.DelegatingDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            physicalState.setEntry(i, setResetDriver(driver, referenceState.getEntry(i), normalisedState.getEntry(i)));
            i += 1;
        }
        return physicalState;
    }

    /** Use a driver to extract a new value, given reference and normalised values,
     * resetting the state of the driver afterward.
     * @param driver the parameter driver
     * @param referenceValue the new reference value
     * @param normalisedVale the new normalised value
     * @return the reference state
     */
    private double setResetDriver(final ParameterDriver driver,
                                  final double referenceValue,
                                  final double normalisedVale) {
        // Record old driver parameters
        final double oldReference = driver.getReferenceValue();
        final double oldValue = driver.getNormalizedValue();

        // Set driver to new parameters
        driver.setReferenceValue(referenceValue);
        driver.setNormalizedValue(normalisedVale);
        final double physicalValue = driver.getValue();

        // Reset driver to old
        driver.setReferenceValue(oldReference);
        driver.setNormalizedValue(oldValue);

        return physicalValue;
    }

}
