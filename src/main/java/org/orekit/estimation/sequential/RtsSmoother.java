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

public class RtsSmoother implements KalmanObserver {

    /** Smoother. */
    private final KalmanSmoother smoother;

    /** Estimator reference date. */
    private final AbsoluteDate referenceDate;

    /** Covariance scales for unnormalising estimates. */
    private final double[] covarianceScale;

    /** Estimated orbital parameters. */
    private ParameterDriversList estimatedOrbitalParameters;

    /** Estimated propagation drivers. */
    private ParameterDriversList estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Reference states for unnormalising estimates. */
    private final List<RealVector> referenceStates;

    /** Constructor.
     * @param estimator the Kalman estimator
     */
    public RtsSmoother(final AbstractSequentialEstimator estimator) {
        this.smoother = new KalmanSmoother(estimator.getMatrixDecomposer());;
        this.referenceDate = estimator.getReferenceDate();
        this.covarianceScale = estimator.getProcessModel().getScale();
        this.estimatedOrbitalParameters = null;
        this.estimatedPropagationParameters = null;
        this.estimatedMeasurementsParameters = null;
        this.referenceStates = new ArrayList<>();

        // Add smoother observer to underlying filter
        estimator.getKalmanFilter().setObserver(smoother);
    }


    @Override
    public void init(final KalmanEstimation estimation) {
        // Get the estimated parameter drivers
        estimatedOrbitalParameters = estimation.getEstimatedOrbitalParameters();
        estimatedPropagationParameters = estimation.getEstimatedPropagationParameters();
        estimatedMeasurementsParameters = estimation.getEstimatedMeasurementsParameters();

        // Get the first reference state
        referenceStates.add(getReferenceState());
    }

    @Override
    public void evaluationPerformed(final KalmanEstimation estimation) {
        referenceStates.add(getReferenceState());
    }

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
