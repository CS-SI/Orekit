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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.filtering.kalman.KalmanFilterSmoother;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.ArrayList;
import java.util.List;

/** Implementation of a Kalman smoother to perform orbit determination.
 *
 * @author Mark Rutten
 */
public class KalmanSmoother extends AbstractSequentialEstimator {

    /** Underlying estimator. */
    private final AbstractSequentialEstimator estimator;

    /** Smoother. */
    private final KalmanFilterSmoother<MeasurementDecorator> smoother;

    /** Reference states for unnormalising estimates. */
    private final List<RealVector> referenceStates;

    /** Constructor.
     * @param estimator the underlying (forward) Kalman or unscented estimator
     */
    public KalmanSmoother(final AbstractSequentialEstimator estimator) {
        super(estimator.getMatrixDecomposer(), estimator.getBuilders());

        this.estimator = estimator;
        this.smoother = new KalmanFilterSmoother<>(estimator.getKalmanFilter(), getMatrixDecomposer());
        this.referenceStates = new ArrayList<>();
        referenceStates.add(getReferenceState());
    }

    /** {@inheritDoc} */
    @Override
    protected KalmanFilter<MeasurementDecorator> getKalmanFilter() {
        return smoother;
    }

    /** {@inheritDoc} */
    @Override
    protected SequentialModel getProcessModel() {
        return estimator.getProcessModel();
    }

    /** {@inheritDoc} */
    @Override
    protected KalmanEstimation getKalmanEstimation() {
        return estimator.getKalmanEstimation();
    }

    /** {@inheritDoc} */
    @Override
    public Propagator[] estimationStep(final ObservedMeasurement<?> observedMeasurement) {
        try {
            final ProcessEstimate estimate = getKalmanFilter().estimationStep(
                    KalmanEstimatorUtil.decorate(observedMeasurement, getReferenceDate())
            );
            // Get the reference state before finalize resets it
            referenceStates.add(getReferenceState());
            getProcessModel().finalizeEstimation(observedMeasurement, estimate);
            if (getObserver() != null) {
                getObserver().evaluationPerformed(getKalmanEstimation());
            }
            return getProcessModel().getEstimatedPropagators();
        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }


    public List<PhysicalEstimatedState> backwardsSmooth() {

        // Backwards smoothing step
        final List<ProcessEstimate> normalisedStates = smoother.backwardsSmooth();

        // Reference date
        final AbsoluteDate referenceDate = estimator.getReferenceDate();

        // Covariance scaling factors
        final double[] covarianceScale = getProcessModel().getScale();

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
        final RealVector referenceState = MatrixUtils.createRealVector(getProcessModel().getScale().length);
        int i = 0;
        for (final ParameterDriversList.DelegatingDriver driver : getOrbitalParametersDrivers(true).getDrivers()) {
            referenceState.setEntry(i++, driver.getReferenceValue());
        }
        for (final ParameterDriversList.DelegatingDriver driver : getPropagationParametersDrivers(true).getDrivers()) {
            referenceState.setEntry(i++, driver.getReferenceValue());
        }
        for (final ParameterDriversList.DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
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
        final RealVector physicalState = MatrixUtils.createRealVector(getProcessModel().getScale().length);
        int i = 0;
        for (final ParameterDriversList.DelegatingDriver driver : getOrbitalParametersDrivers(true).getDrivers()) {
            physicalState.setEntry(i, setResetDriver(driver, referenceState.getEntry(i), normalisedState.getEntry(i)));
            i += 1;
        }
        for (final ParameterDriversList.DelegatingDriver driver : getPropagationParametersDrivers(true).getDrivers()) {
            physicalState.setEntry(i, setResetDriver(driver, referenceState.getEntry(i), normalisedState.getEntry(i)));
            i += 1;
        }
        for (final ParameterDriversList.DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
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
