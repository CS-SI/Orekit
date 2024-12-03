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

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.extended.NonLinearEvolution;
import org.hipparchus.filtering.kalman.extended.NonLinearProcess;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.AbstractPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import java.util.List;
import java.util.Map;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanModel extends AbstractKalmanEstimationCommon implements NonLinearProcess<MeasurementDecorator> {


    /** Harvesters for extracting Jacobians from integrated states. */
    private MatricesHarvester[] harvesters;

    /** Propagators for the reference trajectories, up to current date. */
    private Propagator[] referenceTrajectories;

    /** Kalman process model constructor.
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    public KalmanModel(final List<PropagatorBuilder> propagatorBuilders,
                       final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                       final ParameterDriversList estimatedMeasurementParameters,
                       final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        super(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementParameters, measurementProcessNoiseMatrix);
        // Build the reference propagators and add their partial derivatives equations implementation
        updateReferenceTrajectories(getEstimatedPropagators());
    }

    /** Update the reference trajectories using the propagators as input.
     * @param propagators The new propagators to use
     */
    protected void updateReferenceTrajectories(final Propagator[] propagators) {

        // Update the reference trajectory propagator
        setReferenceTrajectories(propagators);

        // Jacobian harvesters
        harvesters = new MatricesHarvester[propagators.length];

        for (int k = 0; k < propagators.length; ++k) {
            // Link the partial derivatives to this new propagator
            final String equationName = KalmanEstimator.class.getName() + "-derivatives-" + k;
            harvesters[k] = getReferenceTrajectories()[k].setupMatricesComputation(equationName, null, null);
        }

    }

    /** Get the normalized error state transition matrix (STM) from previous point to current point.
     * The STM contains the partial derivatives of current state with respect to previous state.
     * The  STM is an mxm matrix where m is the size of the state vector.
     * m = nbOrb + nbPropag + nbMeas
     * @return the normalized error state transition matrix
     */
    private RealMatrix getErrorStateTransitionMatrix() {

        /* The state transition matrix is obtained as follows, with:
         *  - Y  : Current state vector
         *  - Y0 : Initial state vector
         *  - Pp : Current propagation parameter
         *  - Pp0: Initial propagation parameter
         *  - Mp : Current measurement parameter
         *  - Mp0: Initial measurement parameter
         *
         *       |        |         |         |   |        |        |   .    |
         *       | dY/dY0 | dY/dPp  | dY/dMp  |   | dY/dY0 | dY/dPp | ..0..  |
         *       |        |         |         |   |        |        |   .    |
         *       |--------|---------|---------|   |--------|--------|--------|
         *       |        |         |         |   |   .    | 1 0 0..|   .    |
         * STM = | dP/dY0 | dP/dPp0 | dP/dMp  | = | ..0..  | 0 1 0..| ..0..  |
         *       |        |         |         |   |   .    | 0 0 1..|   .    |
         *       |--------|---------|---------|   |--------|--------|--------|
         *       |        |         |         |   |   .    |   .    | 1 0 0..|
         *       | dM/dY0 | dM/dPp0 | dM/dMp0 |   | ..0..  | ..0..  | 0 1 0..|
         *       |        |         |         |   |   .    |   .    | 0 0 1..|
         */

        // Initialize to the proper size identity matrix
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(getCorrectedEstimate().getState().getDimension());

        // loop over all orbits
        final SpacecraftState[] predictedSpacecraftStates = getPredictedSpacecraftStates();
        final int[][] covarianceIndirection = getCovarianceIndirection();
        final ParameterDriversList[] estimatedOrbitalParameters = getEstimatedOrbitalParametersArray();
        final ParameterDriversList[] estimatedPropagationParameters = getEstimatedPropagationParametersArray();
        final double[] scale = getScale();
        for (int k = 0; k < predictedSpacecraftStates.length; ++k) {

            // Orbital drivers
            final List<DelegatingDriver> orbitalParameterDrivers =
                    getBuilders().get(k).getOrbitalParametersDrivers().getDrivers();

            // Indexes
            final int[] indK = covarianceIndirection[k];

            // Derivatives of the state vector with respect to initial state vector
            final int nbOrbParams = estimatedOrbitalParameters[k].getNbParams();
            if (nbOrbParams > 0) {

                // Reset reference (for example compute short periodic terms in DSST)
                harvesters[k].setReferenceState(predictedSpacecraftStates[k]);

                final RealMatrix dYdY0 = harvesters[k].getStateTransitionMatrix(predictedSpacecraftStates[k]);

                // Fill upper left corner (dY/dY0)
                int stmRow = 0;
                for (int i = 0; i < dYdY0.getRowDimension(); ++i) {
                    int stmCol = 0;
                    if (orbitalParameterDrivers.get(i).isSelected()) {
                        for (int j = 0; j < nbOrbParams; ++j) {
                            if (orbitalParameterDrivers.get(j).isSelected()) {
                                stm.setEntry(indK[stmRow], indK[stmCol], dYdY0.getEntry(i, j));
                                stmCol += 1;
                            }
                        }
                        stmRow += 1;
                    }
                }
            }

            // Derivatives of the state vector with respect to propagation parameters
            final int nbParams = estimatedPropagationParameters[k].getNbParams();
            if (nbOrbParams > 0 && nbParams > 0) {
                final RealMatrix dYdPp = harvesters[k].getParametersJacobian(predictedSpacecraftStates[k]);

                // Fill 1st row, 2nd column (dY/dPp)
                int stmRow = 0;
                for (int i = 0; i < dYdPp.getRowDimension(); ++i) {
                    if (orbitalParameterDrivers.get(i).isSelected()) {
                        for (int j = 0; j < nbParams; ++j) {
                            stm.setEntry(indK[stmRow], indK[j + nbOrbParams], dYdPp.getEntry(i, j));
                        }
                        stmRow += 1;
                    }
                }

            }

        }

        // Normalization of the STM
        // normalized(STM)ij = STMij*Sj/Si
        for (int i = 0; i < scale.length; i++) {
            for (int j = 0; j < scale.length; j++ ) {
                stm.setEntry(i, j, stm.getEntry(i, j) * scale[j] / scale[i]);
            }
        }

        // Return the error state transition matrix
        return stm;

    }

    /** Get the normalized measurement matrix H.
     * H contains the partial derivatives of the measurement with respect to the state.
     * H is an nxm matrix where n is the size of the measurement vector and m the size of the state vector.
     * @return the normalized measurement matrix H
     */
    private RealMatrix getMeasurementMatrix() {

        // Observed measurement characteristics
        final EstimatedMeasurement<?> predictedMeasurement = getPredictedMeasurement();
        final SpacecraftState[]      evaluationStates    = predictedMeasurement.getStates();
        final ObservedMeasurement<?> observedMeasurement = predictedMeasurement.getObservedMeasurement();
        final double[] sigma  = observedMeasurement.getTheoreticalStandardDeviation();

        // Initialize measurement matrix H: nxm
        // n: Number of measurements in current measurement
        // m: State vector size
        final RealMatrix measurementMatrix = MatrixUtils.
                        createRealMatrix(observedMeasurement.getDimension(),
                                         getCorrectedEstimate().getState().getDimension());

        // loop over all orbits involved in the measurement
        final int[] orbitsStartColumns = getOrbitsStartColumns();
        final ParameterDriversList[] estimatedPropagationParameters = getEstimatedPropagationParametersArray();
        final Map<String, Integer> propagationParameterColumns = getPropagationParameterColumns();
        final Map<String, Integer> measurementParameterColumns = getMeasurementParameterColumns();
        for (int k = 0; k < evaluationStates.length; ++k) {
            final int p = observedMeasurement.getSatellites().get(k).getPropagatorIndex();

            // Predicted orbit
            final Orbit predictedOrbit = evaluationStates[k].getOrbit();

            // Measurement matrix's columns related to orbital parameters
            // ----------------------------------------------------------

            // Partial derivatives of the current Cartesian coordinates with respect to current orbital state
            final double[][] aCY = new double[6][6];
            predictedOrbit.getJacobianWrtParameters(getBuilders().get(p).getPositionAngleType(), aCY);   //dC/dY
            final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

            // Jacobian of the measurement with respect to current Cartesian coordinates
            final RealMatrix dMdC = new Array2DRowRealMatrix(predictedMeasurement.getStateDerivatives(k), false);

            // Jacobian of the measurement with respect to current orbital state
            final RealMatrix dMdY = dMdC.multiply(dCdY);

            // Fill the normalized measurement matrix's columns related to estimated orbital parameters
            for (int i = 0; i < dMdY.getRowDimension(); ++i) {
                int jOrb = orbitsStartColumns[p];
                for (int j = 0; j < dMdY.getColumnDimension(); ++j) {
                    final ParameterDriver driver = getBuilders().get(p).getOrbitalParametersDrivers().getDrivers().get(j);
                    if (driver.isSelected()) {
                        measurementMatrix.setEntry(i, jOrb++,
                                                   dMdY.getEntry(i, j) / sigma[i] * driver.getScale());
                    }
                }
            }

            // Normalized measurement matrix's columns related to propagation parameters
            // --------------------------------------------------------------

            // Jacobian of the measurement with respect to propagation parameters
            final int nbParams = estimatedPropagationParameters[p].getNbParams();
            if (nbParams > 0) {
                final RealMatrix dYdPp = harvesters[p].getParametersJacobian(evaluationStates[k]);
                final RealMatrix dMdPp = dMdY.multiply(dYdPp);
                for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                    for (int j = 0; j < nbParams; ++j) {
                        final ParameterDriver delegating = estimatedPropagationParameters[p].getDrivers().get(j);
                        measurementMatrix.setEntry(i, propagationParameterColumns.get(delegating.getName()),
                                                   dMdPp.getEntry(i, j) / sigma[i] * delegating.getScale());
                    }
                }
            }

            // Normalized measurement matrix's columns related to measurement parameters
            // --------------------------------------------------------------

            // Jacobian of the measurement with respect to measurement parameters
            // Gather the measurement parameters linked to current measurement
            for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
                if (driver.isSelected()) {
                    // Derivatives of current measurement w/r to selected measurement parameter
                    final double[] aMPm = predictedMeasurement.getParameterDerivatives(driver);

                    // Check that the measurement parameter is managed by the filter
                    if (measurementParameterColumns.get(driver.getName()) != null) {
                        // Column of the driver in the measurement matrix
                        final int driverColumn = measurementParameterColumns.get(driver.getName());

                        // Fill the corresponding indexes of the measurement matrix
                        for (int i = 0; i < aMPm.length; ++i) {
                            measurementMatrix.setEntry(i, driverColumn,
                                                       aMPm[i] / sigma[i] * driver.getScale());
                        }
                    }
                }
            }
        }

        // Return the normalized measurement matrix
        return measurementMatrix;

    }

    /** {@inheritDoc} */
    @Override
    public NonLinearEvolution getEvolution(final double previousTime, final RealVector previousState,
                                           final MeasurementDecorator measurement) {

        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(getBuilders().get(0).getInitialOrbitDate());
            }
        }

        incrementCurrentMeasurementNumber();
        setCurrentDate(measurement.getObservedMeasurement().getDate());

        // Note:
        // - n = size of the current measurement
        //  Example:
        //   * 1 for Range, RangeRate and TurnAroundRange
        //   * 2 for Angular (Azimuth/Elevation or Right-ascension/Declination)
        //   * 6 for Position/Velocity
        // - m = size of the state vector. n = nbOrb + nbPropag + nbMeas

        // Predict the state vector (mx1)
        final RealVector predictedState = predictState(observedMeasurement.getDate());

        // Get the error state transition matrix (mxm)
        final RealMatrix stateTransitionMatrix = getErrorStateTransitionMatrix();

        // Predict the measurement based on predicted spacecraft state
        // Compute the innovations (i.e. residuals of the predicted measurement)
        // ------------------------------------------------------------

        // Predicted measurement
        // Note: here the "iteration/evaluation" formalism from the batch LS method
        // is twisted to fit the need of the Kalman filter.
        // The number of "iterations" is actually the number of measurements processed by the filter
        // so far. We use this to be able to apply the OutlierFilter modifiers on the predicted measurement.
        setPredictedMeasurement(observedMeasurement.estimate(getCurrentMeasurementNumber(),
                                                             getCurrentMeasurementNumber(),
                                                             KalmanEstimatorUtil.filterRelevant(observedMeasurement, getPredictedSpacecraftStates())));

        // Normalized measurement matrix (nxm)
        final RealMatrix measurementMatrix = getMeasurementMatrix();

        // compute process noise matrix
        final RealMatrix normalizedProcessNoise = getNormalizedProcessNoise(previousState.getDimension());

        return new NonLinearEvolution(measurement.getTime(), predictedState,
                                      stateTransitionMatrix, normalizedProcessNoise, measurementMatrix);

    }


    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final NonLinearEvolution evolution,
                                    final RealMatrix innovationCovarianceMatrix) {

        // Apply the dynamic outlier filter, if it exists
        final EstimatedMeasurement<?> predictedMeasurement = getPredictedMeasurement();
        KalmanEstimatorUtil.applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);
        // Compute the innovation vector
        return KalmanEstimatorUtil.computeInnovationVector(predictedMeasurement, predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }

    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        setCorrectedEstimate(estimate);
        updateParameters();

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final Propagator[] estimatedPropagators = getEstimatedPropagators();
        for (int k = 0; k < estimatedPropagators.length; ++k) {
            setCorrectedSpacecraftState(estimatedPropagators[k].getInitialState(), k);
        }

        // Compute the estimated measurement using estimated spacecraft state
        setCorrectedMeasurement(observedMeasurement.estimate(getCurrentMeasurementNumber(),
                                                             getCurrentMeasurementNumber(),
                                                             KalmanEstimatorUtil.filterRelevant(observedMeasurement, getCorrectedSpacecraftStates())));
        // Update the trajectory
        // ---------------------
        updateReferenceTrajectories(estimatedPropagators);

    }

    /** Set the predicted normalized state vector.
     * The predicted/propagated orbit is used to update the state vector
     * @param date prediction date
     * @return predicted state
     */
    private RealVector predictState(final AbsoluteDate date) {

        // Predicted state is initialized to previous estimated state
        final RealVector predictedState = getCorrectedEstimate().getState().copy();

        // Orbital parameters counter
        int jOrb = 0;

        for (int k = 0; k < getPredictedSpacecraftStates().length; ++k) {

            // Propagate the reference trajectory to measurement date
            final SpacecraftState predictedSpacecraftState = referenceTrajectories[k].propagate(date);
            setPredictedSpacecraftState(predictedSpacecraftState, k);

            // Update the builder with the predicted orbit
            // This updates the orbital drivers with the values of the predicted orbit
            getBuilders().get(k).resetOrbit(predictedSpacecraftState.getOrbit());

            // Additionally, for PropagatorBuilders which use mass, update the builder with the predicted mass value.
            // If any mass changes have occurred during this estimation step, such as maneuvers,
            // the updated mass value must be carried over so that new Propagators from this builder start with the updated mass.
            if (getBuilders().get(k) instanceof AbstractPropagatorBuilder) {
                ((AbstractPropagatorBuilder) (getBuilders().get(k))).setMass(predictedSpacecraftState.getMass());
            }

            // The orbital parameters in the state vector are replaced with their predicted values
            // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction
            for (DelegatingDriver orbitalDriver : getBuilders().get(k).getOrbitalParametersDrivers().getDrivers()) {
                if (orbitalDriver.isSelected()) {
                    predictedState.setEntry(jOrb++, orbitalDriver.getNormalizedValue());
                }
            }

        }

        return predictedState;

    }

    /** Update the estimated parameters after the correction phase of the filter.
     * The min/max allowed values are handled by the parameter themselves.
     */
    private void updateParameters() {
        final RealVector correctedState = getCorrectedEstimate().getState();
        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(correctedState.getEntry(i));
            correctedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(correctedState.getEntry(i));
            correctedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(correctedState.getEntry(i));
            correctedState.setEntry(i++, driver.getNormalizedValue());
        }
    }

    /** Getter for the reference trajectories.
     * @return the referencetrajectories
     */
    public Propagator[] getReferenceTrajectories() {
        return referenceTrajectories.clone();
    }

    /** Setter for the reference trajectories.
     * @param referenceTrajectories the reference trajectories to be setted
     */
    public void setReferenceTrajectories(final Propagator[] referenceTrajectories) {
        this.referenceTrajectories = referenceTrajectories.clone();
    }

}
