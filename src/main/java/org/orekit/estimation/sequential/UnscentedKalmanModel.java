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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.unscented.UnscentedEvolution;
import org.hipparchus.filtering.kalman.unscented.UnscentedProcess;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Class defining the process model dynamics to use with a {@link UnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 */
public class UnscentedKalmanModel implements KalmanEstimation, UnscentedProcess<MeasurementDecorator> {

    /** Builder for propagator. */
    private final NumericalPropagatorBuilder builder;

    /** Estimated orbital parameters. */
    private final ParameterDriversList estimatedOrbitalParameters;

    /** Estimated propagation parameters. */
    private final ParameterDriversList estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Map for propagation parameters columns. */
    private final Map<String, Integer> propagationParameterColumns;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> measurementParameterColumns;

    /** Scaling factors. */
    private final double[] scale;

    /** Provider for covariance matrice. */
    private final CovarianceMatrixProvider covarianceMatrixProvider;

    /** Process noise matrix provider for measurement parameters. */
    private final CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Current corrected estimate. */
    private ProcessEstimate correctedEstimate;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Predicted spacecraft states. */
    private SpacecraftState predictedSpacecraftState;

    /** Corrected spacecraft states. */
    private SpacecraftState correctedSpacecraftState;

    /** Predicted measurement. */
    private EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

    /** Unscented Kalman process model constructor (package private).
     * @param propagatorBuilder propagators builders used to evaluate the orbits.
     * @param covarianceMatrixProvider provider for covariance matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    protected UnscentedKalmanModel(final NumericalPropagatorBuilder propagatorBuilder,
                                   final CovarianceMatrixProvider covarianceMatrixProvider,
                                   final ParameterDriversList estimatedMeasurementParameters,
                                   final CovarianceMatrixProvider measurementProcessNoiseMatrix) {

        this.builder                         = propagatorBuilder;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.referenceDate                   = propagatorBuilder.getInitialOrbitDate();
        this.currentDate                     = referenceDate;
        this.covarianceMatrixProvider        = covarianceMatrixProvider;
        this.measurementProcessNoiseMatrix   = measurementProcessNoiseMatrix;

        // Number of estimated parameters
        int columns = 0;

        // Set estimated orbital parameters
        estimatedOrbitalParameters = new ParameterDriversList();
        for (final ParameterDriver driver : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {

            // Verify if the driver reference date has been set
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(currentDate);
            }

            // Verify if the driver is selected
            if (driver.isSelected()) {
                estimatedOrbitalParameters.add(driver);
                columns++;
            }

        }

        // Set estimated propagation parameters
        estimatedPropagationParameters = new ParameterDriversList();
        final List<String> estimatedPropagationParametersNames = new ArrayList<>();
        for (final ParameterDriver driver : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {

            // Verify if the driver reference date has been set
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(currentDate);
            }

            // Verify if the driver is selected
            if (driver.isSelected()) {
                estimatedPropagationParameters.add(driver);
                final String driverName = driver.getName();
                // Add the driver name if it has not been added yet
                if (!estimatedPropagationParametersNames.contains(driverName)) {
                    estimatedPropagationParametersNames.add(driverName);
                }
            }

        }
        estimatedPropagationParametersNames.sort(Comparator.naturalOrder());

        // Populate the map of propagation drivers' columns and update the total number of columns
        propagationParameterColumns = new HashMap<>(estimatedPropagationParametersNames.size());
        for (final String driverName : estimatedPropagationParametersNames) {
            propagationParameterColumns.put(driverName, columns);
            ++columns;
        }

        // Set the estimated measurement parameters
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            if (parameter.getReferenceDate() == null) {
                parameter.setReferenceDate(currentDate);
            }
            measurementParameterColumns.put(parameter.getName(), columns);
            ++columns;
        }

        // Compute the scale factors
        this.scale = new double[columns];
        int index = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            scale[index++] = driver.getScale();
        }
        for (final ParameterDriver driver : estimatedPropagationParameters.getDrivers()) {
            scale[index++] = driver.getScale();
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            scale[index++] = driver.getScale();
        }

        // Initialize the estimated normalized state and fill its values
        final RealVector correctedState = MatrixUtils.createRealVector(columns);

        int p = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getValue());
        }
        for (final ParameterDriver driver : estimatedPropagationParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getValue());
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getValue());
        }

        // Number of estimated measurement parameters
        final int nbMeas = estimatedMeasurementParameters.getNbParams();

        // Number of estimated dynamic parameters (orbital + propagation)
        final int nbDyn  = estimatedOrbitalParameters.getNbParams() +
                           estimatedPropagationParameters.getNbParams();

        this.predictedSpacecraftState = propagatorBuilder.buildPropagator(propagatorBuilder.getSelectedNormalizedParameters()).getInitialState();
        this.correctedSpacecraftState = predictedSpacecraftState;

        // Covariance matrix
        final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
        final RealMatrix noiseP = covarianceMatrixProvider.getInitialCovarianceMatrix(predictedSpacecraftState);

        noiseK.setSubMatrix(noiseP.getData(), 0, 0);

        if (measurementProcessNoiseMatrix != null) {
            final RealMatrix noiseM = measurementProcessNoiseMatrix.
                                      getInitialCovarianceMatrix(correctedSpacecraftState);
            noiseK.setSubMatrix(noiseM.getData(), nbDyn, nbDyn);
        }

        checkDimension(noiseK.getRowDimension(),
                       propagatorBuilder.getOrbitalParametersDrivers(),
                       propagatorBuilder.getPropagationParametersDrivers(),
                       estimatedMeasurementsParameters);

        // Initialize corrected estimate
        this.correctedEstimate = new ProcessEstimate(0.0, correctedState, noiseK);

    }

    @Override
    public UnscentedEvolution getEvolution(final double previousTime, final RealVector[] sigmaPoints,
           final MeasurementDecorator measurement) {

        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builder.getInitialOrbitDate());
            }
        }

        // Increment measurement number
        ++currentMeasurementNumber;

        // Update the current date
        currentDate = measurement.getObservedMeasurement().getDate();
        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedStates       = new RealVector[sigmaPoints.length];
        final RealVector[] predictedMeasurements = new RealVector[sigmaPoints.length];

        // Loop on sigma points
        for (int k = 0; k < sigmaPoints.length; ++k) {

            // Current point
            final double[] currentPoint = sigmaPoints[k].copy().toArray();

            final NumericalPropagatorBuilder copy = getEstimatedBuilder(currentPoint);
            // Propagate
            final SpacecraftState predicted = copy.buildPropagator(currentPoint).propagate(currentDate);
            copy.resetOrbit(predicted.getOrbit());

            // The orbital parameters in the state vector are replaced with their predicted values
            // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction
            int jOrb = 0;
            for (DelegatingDriver orbitalDriver : copy.getOrbitalParametersDrivers().getDrivers()) {
                if (orbitalDriver.isSelected()) {
                    currentPoint[jOrb++] = orbitalDriver.getValue();
                }
            }
            predictedStates[k] = new ArrayRealVector(currentPoint.length);
            for (int j = 0; j < currentPoint.length; j++) {
                predictedStates[k].setEntry(j, currentPoint[j]);
            }


            // Estimate the measurement
            final EstimatedMeasurement<?> estimated = observedMeasurement.estimate(currentMeasurementNumber,
                                                                                   currentMeasurementNumber,
                                                                                   new SpacecraftState[] {
                                                                                       predicted
                                                                                   });
            predictedMeasurements[k] = new ArrayRealVector(estimated.getEstimatedValue());

        }

        // Number of estimated measurement parameters
        final int nbMeas = getNumberSelectedMeasurementDrivers();

        // Number of estimated dynamic parameters (orbital + propagation)
        final int nbDyn  = getNumberSelectedOrbitalDrivers() + getNumberSelectedPropagationDrivers();

        // Covariance matrix
        final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
        final RealMatrix noiseP = covarianceMatrixProvider.getProcessNoiseMatrix(correctedSpacecraftState, predictedSpacecraftState);
        noiseK.setSubMatrix(noiseP.getData(), 0, 0);
        if (measurementProcessNoiseMatrix != null) {
            final RealMatrix noiseM = measurementProcessNoiseMatrix.getProcessNoiseMatrix(correctedSpacecraftState, predictedSpacecraftState);
            noiseK.setSubMatrix(noiseM.getData(), nbDyn, nbDyn);
        }

        // Verify dimension
        checkDimension(noiseK.getRowDimension(),
                       builder.getOrbitalParametersDrivers(),
                       builder.getPropagationParametersDrivers(),
                       estimatedMeasurementsParameters);

        return new UnscentedEvolution(measurement.getTime(), predictedStates, predictedMeasurements, noiseK);
    }

    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas, final RealVector predictedNormalizedState,
            final RealMatrix innovationCovarianceMatrix) {

        // update the predicted spacecraft state with predictedNormalizedState
        final SpacecraftState newPredictedState = getEstimatedSpacecraftState(predictedNormalizedState.toArray());

        final double[] array    = new double[6];
        final double[] arrayDot = new double[6];
        builder.getOrbitType().mapOrbitToArray(newPredictedState.getOrbit(), builder.getPositionAngle(), array, arrayDot);;
        predictedSpacecraftState = new SpacecraftState(builder.getOrbitType().mapArrayToOrbit(array, arrayDot, builder.getPositionAngle(), currentDate, newPredictedState.getMu(), newPredictedState.getFrame()));
        predictedMeasurement = measurement.getObservedMeasurement().estimate(currentMeasurementNumber, currentMeasurementNumber, getPredictedSpacecraftStates());


        // set estimated value to the predicted value by the filter
        applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);
        if (predictedMeasurement.getStatus() == EstimatedMeasurement.Status.REJECTED)  {
            // set innovation to null to notify filter measurement is rejected
            return null;
        } else {
            // Normalized innovation of the measurement (Nx1)
            final double[] observed  = predictedMeasurement.getObservedMeasurement().getObservedValue();
            final double[] estimated = predictedMeas.toArray();
            final double[] sigma     = predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();
            final double[] residuals = new double[observed.length];

            for (int i = 0; i < observed.length; i++) {
                residuals[i] = (observed[i] - estimated[i]) / sigma[i];
            }
            return MatrixUtils.createRealVector(residuals);
        }
    }


    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {

        correctedEstimate = estimate;

        final NumericalPropagatorBuilder copy = getEstimatedBuilder(estimate.getState().toArray());
        // update the predicted spacecraft state with predictedNormalizedState
        final SpacecraftState newCorrectedState = getEstimatedSpacecraftState(estimate.getState().toArray());
        final double[] array    = new double[6];
        final double[] arrayDot = new double[6];
        copy.getOrbitType().mapOrbitToArray(newCorrectedState.getOrbit(), builder.getPositionAngle(), array, arrayDot);
        correctedSpacecraftState = new SpacecraftState(copy.getOrbitType().mapArrayToOrbit(array, arrayDot, copy.getPositionAngle(), currentDate, newCorrectedState.getMu(), newCorrectedState.getFrame()));

        // Compute the estimated measurement using estimated spacecraft state
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            new SpacecraftState[] {correctedSpacecraftState});
        // Update current date in the builder
        builder.resetOrbit(correctedSpacecraftState.getOrbit());


    }


    /** Get the current corrected estimate.
     * @return current corrected estimate
     */
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** Get the propagator estimated with the values set in the propagator builder.
     * @return propagator based on the current values in the builder
     */
    public Propagator getEstimatedPropagator() {
        // Return propagator built with current instantiation of the propagator builder
        return builder.buildPropagator(builder.getSelectedNormalizedParameters());
    }
    /** Get the builder estimated with the values of parameters.
     * @param parameters array containing orbital parameters
     * @return builder based on the values of parameters
     */
    public NumericalPropagatorBuilder getEstimatedBuilder(final double[] parameters) {
        final NumericalPropagatorBuilder copy = builder.copy();
        int j = 0;
        for (DelegatingDriver orbitalDriver : copy.getOrbitalParametersDrivers().getDrivers()) {
            if (orbitalDriver.isSelected()) {
                orbitalDriver.setValue(parameters[j]);
                orbitalDriver.setReferenceValue(parameters[j]);
                parameters[j++] = 0;

            }
        }
        for (DelegatingDriver propagationDriver : copy.getPropagationParametersDrivers().getDrivers()) {
            if (propagationDriver.isSelected()) {
                propagationDriver.setValue(parameters[j]);
                propagationDriver.setReferenceValue(parameters[j]);
                parameters[j++] = 0;

            }
        }

        return copy;
    }

    /** Get the spacecraft state estimated with the values of parameters.
     * @param parameters array containing orbital parameters
     * @return spacecraftstate based on the values of parameters
     */
    public SpacecraftState getEstimatedSpacecraftState(final double[] parameters) {
        final NumericalPropagatorBuilder copy = builder.copy();
        int j = 0;
        for (DelegatingDriver orbitalDriver : copy.getOrbitalParametersDrivers().getDrivers()) {
            if (orbitalDriver.isSelected()) {
                orbitalDriver.setValue(parameters[j]);
                orbitalDriver.setReferenceValue(parameters[j]);
                parameters[j++] = 0;
            }
        }
        for (DelegatingDriver propagationDriver : copy.getPropagationParametersDrivers().getDrivers()) {
            if (propagationDriver.isSelected()) {
                propagationDriver.setValue(parameters[j]);
                propagationDriver.setReferenceValue(parameters[j]);
                parameters[j++] = 0;

            }
        }

        final SpacecraftState spacecraftState = copy.buildPropagator(parameters).getInitialState();
        return spacecraftState;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getEstimatedOrbitalParameters() {
        return estimatedOrbitalParameters;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getEstimatedPropagationParameters() {
        return estimatedPropagationParameters;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return estimatedMeasurementsParameters;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState[] getPredictedSpacecraftStates() {
        return new SpacecraftState[] {predictedSpacecraftState};
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState[] getCorrectedSpacecraftStates() {
        return new SpacecraftState[] {correctedSpacecraftState};
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getPhysicalEstimatedState() {
        // Method {@link ParameterDriver#getValue()} is used to get
        // the physical values of the state.
        // The scales'array is used to get the size of the state vector
        final RealVector physicalEstimatedState = new ArrayRealVector(scale.length);
        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            physicalEstimatedState.setEntry(i++, driver.getValue());
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            physicalEstimatedState.setEntry(i++, driver.getValue());
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            physicalEstimatedState.setEntry(i++, driver.getValue());
        }

        return physicalEstimatedState;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {

        return correctedEstimate.getCovariance();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalStateTransitionMatrix() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalMeasurementJacobian() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalInnovationCovarianceMatrix() {
        return correctedEstimate.getInnovationCovariance();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalKalmanGain() {
        return correctedEstimate.getKalmanGain();
    }
    /** {@inheritDoc} */
    @Override
    public int getCurrentMeasurementNumber() {
        return currentMeasurementNumber;
    }
    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getCurrentDate() {
        return currentDate;
    }
    /** {@inheritDoc} */
    @Override
    public EstimatedMeasurement<?> getPredictedMeasurement() {
        return predictedMeasurement;
    }
    /** {@inheritDoc} */
    @Override
    public EstimatedMeasurement<?> getCorrectedMeasurement() {
        return correctedMeasurement;
    }


    /** Check dimension.
     * @param dimension dimension to check
     * @param orbitalParameters orbital parameters
     * @param propagationParameters propagation parameters
     * @param measurementParameters measurements parameters
     */
    private void checkDimension(final int dimension,
                                final ParameterDriversList orbitalParameters,
                                final ParameterDriversList propagationParameters,
                                final ParameterDriversList measurementParameters) {

        // count parameters, taking care of counting all orbital parameters
        // regardless of them being estimated or not
        int requiredDimension = orbitalParameters.getNbParams();
        for (final ParameterDriver driver : propagationParameters.getDrivers()) {
            if (driver.isSelected()) {
                ++requiredDimension;
            }
        }
        for (final ParameterDriver driver : measurementParameters.getDrivers()) {
            if (driver.isSelected()) {
                ++requiredDimension;
            }
        }

        if (dimension != requiredDimension) {
            // there is a problem, set up an explicit error message
            final StringBuilder sBuilder = new StringBuilder();
            for (final ParameterDriver driver : orbitalParameters.getDrivers()) {
                if (sBuilder.length() > 0) {
                    sBuilder.append(", ");
                }
                sBuilder.append(driver.getName());
            }
            for (final ParameterDriver driver : propagationParameters.getDrivers()) {
                if (driver.isSelected()) {
                    sBuilder.append(driver.getName());
                }
            }
            for (final ParameterDriver driver : measurementParameters.getDrivers()) {
                if (driver.isSelected()) {
                    sBuilder.append(driver.getName());
                }
            }
            throw new OrekitException(OrekitMessages.DIMENSION_INCONSISTENT_WITH_PARAMETERS,
                                      dimension, builder.toString());
        }

    }

    /** Set and apply a dynamic outlier filter on a measurement.<p>
     * Loop on the modifiers to see if a dynamic outlier filter needs to be applied.<p>
     * Compute the sigma array using the matrix in input and set the filter.<p>
     * Apply the filter by calling the modify method on the estimated measurement.<p>
     * Reset the filter.
     * @param measurement measurement to filter
     * @param innovationCovarianceMatrix innovation covariance matrix
     * @param <T> the type of measurement
     */
    private <T extends ObservedMeasurement<T>> void applyDynamicOutlierFilter(final EstimatedMeasurement<T> measurement,
                                                                              final RealMatrix innovationCovarianceMatrix) {

        // Observed measurement associated to the predicted measurement
        final ObservedMeasurement<T> observedMeasurement = measurement.getObservedMeasurement();

        // Check if a dynamic filter was added to the measurement
        // If so, update its sigma value and apply it
        for (EstimationModifier<T> modifier : observedMeasurement.getModifiers()) {
            if (modifier instanceof DynamicOutlierFilter<?>) {
                final DynamicOutlierFilter<T> dynamicOutlierFilter = (DynamicOutlierFilter<T>) modifier;

                // Initialize the values of the sigma array used in the dynamic filter
                final double[] sigmaDynamic     = new double[innovationCovarianceMatrix.getColumnDimension()];
                final double[] sigmaMeasurement = observedMeasurement.getTheoreticalStandardDeviation();

                // Set the sigma value for each element of the measurement
                // Here we do use the value suggested by David A. Vallado (see [1]§10.6):
                // sigmaDynamic[i] = sqrt(diag(S))*sigma[i]
                // With S = H.Ppred.Ht + R
                // Where:
                //  - S is the measurement error matrix in input
                //  - H is the normalized measurement matrix (Ht its transpose)
                //  - Ppred is the normalized predicted covariance matrix
                //  - R is the normalized measurement noise matrix
                //  - sigma[i] is the theoretical standard deviation of the ith component of the measurement.
                //    It is used here to un-normalize the value before it is filtered
                for (int i = 0; i < sigmaDynamic.length; i++) {
                    sigmaDynamic[i] = FastMath.sqrt(innovationCovarianceMatrix.getEntry(i, i)) * sigmaMeasurement[i];
                }
                dynamicOutlierFilter.setSigma(sigmaDynamic);

                // Apply the modifier on the estimated measurement
                modifier.modify(measurement);

                // Re-initialize the value of the filter for the next measurement of the same type
                dynamicOutlierFilter.setSigma(null);
            }
        }
    }

    /** Get the number of estimated orbital parameters.
     * @return the number of estimated orbital parameters
     */
    private int getNumberSelectedOrbitalDrivers() {
        return estimatedOrbitalParameters.getNbParams();
    }

    /** Get the number of estimated propagation parameters.
     * @return the number of estimated propagation parameters
     */
    private int getNumberSelectedPropagationDrivers() {
        return estimatedPropagationParameters.getNbParams();
    }

    /** Get the number of estimated measurement parameters.
     * @return the number of estimated measurement parameters
     */
    private int getNumberSelectedMeasurementDrivers() {
        return estimatedMeasurementsParameters.getNbParams();
    }



}
