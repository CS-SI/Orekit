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
import org.hipparchus.filtering.kalman.unscented.UnscentedEvolution;
import org.hipparchus.filtering.kalman.unscented.UnscentedProcess;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Class defining the process model dynamics to use with a {@link UnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class UnscentedKalmanModel implements KalmanEstimation, UnscentedProcess<MeasurementDecorator> {

    /** Builders for propagators. */
    private final List<PropagatorBuilder> builders;

    /** Estimated orbital parameters. */
    private final ParameterDriversList allEstimatedOrbitalParameters;

    /** Estimated propagation drivers. */
    private final ParameterDriversList allEstimatedPropagationParameters;

    /** Per-builder estimated orbital parameters drivers. */
    private final ParameterDriversList[] estimatedOrbitalParameters;

    /** Per-builder estimated propagation drivers. */
    private final ParameterDriversList[] estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Start columns for each estimated orbit. */
    private final int[] orbitsStartColumns;

    /** End columns for each estimated orbit. */
    private final int[] orbitsEndColumns;

    /** Map for propagation parameters columns. */
    private final Map<String, Integer> propagationParameterColumns;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> measurementParameterColumns;

    /** Providers for covariance matrices. */
    private final List<CovarianceMatrixProvider> covarianceMatricesProviders;

    /** Process noise matrix provider for measurement parameters. */
    private final CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Indirection arrays to extract the noise components for estimated parameters. */
    private final int[][] covarianceIndirection;

    /** Scaling factors. */
    private final double[] scale;

    /** Reference values. */
    private final double[] referenceValues;

    /** Current corrected estimate. */
    private ProcessEstimate correctedEstimate;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Predicted spacecraft states. */
    private final SpacecraftState[] predictedSpacecraftStates;

    /** Corrected spacecraft states. */
    private final SpacecraftState[] correctedSpacecraftStates;

    /** Predicted measurement. */
    private EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

    /** Unscented Kalman process model constructor (package private).
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders provider for covariance matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    protected UnscentedKalmanModel(final List<PropagatorBuilder> propagatorBuilders,
                                   final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                   final ParameterDriversList estimatedMeasurementParameters,
                                   final CovarianceMatrixProvider measurementProcessNoiseMatrix) {


        this.builders                        = propagatorBuilders;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.currentDate                     = propagatorBuilders.get(0).getInitialOrbitDate();

        final Map<String, Integer> orbitalParameterColumns = new HashMap<>(6 * builders.size());
        orbitsStartColumns      = new int[builders.size()];
        orbitsEndColumns        = new int[builders.size()];
        int columns = 0;
        allEstimatedOrbitalParameters = new ParameterDriversList();
        estimatedOrbitalParameters    = new ParameterDriversList[builders.size()];
        for (int k = 0; k < builders.size(); ++k) {
            estimatedOrbitalParameters[k] = new ParameterDriversList();
            orbitsStartColumns[k] = columns;
            final String suffix = propagatorBuilders.size() > 1 ? "[" + k + "]" : null;
            for (final ParameterDriver driver : builders.get(k).getOrbitalParametersDrivers().getDrivers()) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(currentDate);
                }
                if (suffix != null && !driver.getName().endsWith(suffix)) {
                    // we add suffix only conditionally because the method may already have been called
                    // and suffixes may have already been appended
                    driver.setName(driver.getName() + suffix);
                }
                if (driver.isSelected()) {
                    allEstimatedOrbitalParameters.add(driver);
                    estimatedOrbitalParameters[k].add(driver);
                    orbitalParameterColumns.put(driver.getName(), columns++);
                }
            }
            orbitsEndColumns[k] = columns;
        }

        // Gather all the propagation drivers names in a list
        allEstimatedPropagationParameters = new ParameterDriversList();
        estimatedPropagationParameters    = new ParameterDriversList[builders.size()];
        final List<String> estimatedPropagationParametersNames = new ArrayList<>();
        for (int k = 0; k < builders.size(); ++k) {
            estimatedPropagationParameters[k] = new ParameterDriversList();
            for (final ParameterDriver driver : builders.get(k).getPropagationParametersDrivers().getDrivers()) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(currentDate);
                }
                if (driver.isSelected()) {
                    allEstimatedPropagationParameters.add(driver);
                    estimatedPropagationParameters[k].add(driver);
                    final String driverName = driver.getName();
                    // Add the driver name if it has not been added yet
                    if (!estimatedPropagationParametersNames.contains(driverName)) {
                        estimatedPropagationParametersNames.add(driverName);
                    }
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

        // Populate the map of measurement drivers' columns and update the total number of columns
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            if (parameter.getReferenceDate() == null) {
                parameter.setReferenceDate(currentDate);
            }
            measurementParameterColumns.put(parameter.getName(), columns);
            ++columns;
        }

        // Store providers for process noise matrices
        this.covarianceMatricesProviders = covarianceMatricesProviders;
        this.measurementProcessNoiseMatrix = measurementProcessNoiseMatrix;
        this.covarianceIndirection       = new int[builders.size()][columns];
        for (int k = 0; k < covarianceIndirection.length; ++k) {
            final ParameterDriversList orbitDrivers      = builders.get(k).getOrbitalParametersDrivers();
            final ParameterDriversList parametersDrivers = builders.get(k).getPropagationParametersDrivers();
            Arrays.fill(covarianceIndirection[k], -1);
            int i = 0;
            for (final ParameterDriver driver : orbitDrivers.getDrivers()) {
                final Integer c = orbitalParameterColumns.get(driver.getName());
                if (c != null) {
                    covarianceIndirection[k][i++] = c;
                }
            }
            for (final ParameterDriver driver : parametersDrivers.getDrivers()) {
                final Integer c = propagationParameterColumns.get(driver.getName());
                if (c != null) {
                    covarianceIndirection[k][i++] = c;
                }
            }
            for (final ParameterDriver driver : estimatedMeasurementParameters.getDrivers()) {
                final Integer c = measurementParameterColumns.get(driver.getName());
                if (c != null) {
                    covarianceIndirection[k][i++] = c;
                }
            }
        }

        // Compute the scale factors
        this.scale = new double[columns];
        int index = 0;
        for (final ParameterDriver driver : allEstimatedOrbitalParameters.getDrivers()) {
            scale[index++] = driver.getScale();
        }
        for (final ParameterDriver driver : allEstimatedPropagationParameters.getDrivers()) {
            scale[index++] = driver.getScale();
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            scale[index++] = driver.getScale();
        }

        // Populate predicted states
        this.predictedSpacecraftStates = new SpacecraftState[builders.size()];
        for (int i = 0; i < builders.size(); ++i) {
            predictedSpacecraftStates[i] = builders.get(i)
                    .buildPropagator(builders.get(i).getSelectedNormalizedParameters()).getInitialState();
        }
        this.correctedSpacecraftStates = predictedSpacecraftStates.clone();

        // Initialize the estimated normalized state and fill its values
        final RealVector correctedState      = MatrixUtils.createRealVector(columns);
        int p = 0;
        for (final ParameterDriver driver : allEstimatedOrbitalParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : allEstimatedPropagationParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }

        // Record the initial reference values
        this.referenceValues = new double[columns];
        index = 0;
        for (final ParameterDriver driver : allEstimatedOrbitalParameters.getDrivers()) {
            referenceValues[index++] = driver.getReferenceValue();
        }
        for (final ParameterDriver driver : allEstimatedPropagationParameters.getDrivers()) {
            referenceValues[index++] = driver.getReferenceValue();
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            referenceValues[index++] = driver.getReferenceValue();
        }


        // Set up initial covariance
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(columns, columns);
        for (int k = 0; k < covarianceMatricesProviders.size(); ++k) {

            // Number of estimated measurement parameters
            final int nbMeas = estimatedMeasurementParameters.getNbParams();

            // Number of estimated dynamic parameters (orbital + propagation)
            final int nbDyn  = orbitsEndColumns[k] - orbitsStartColumns[k] +
                    estimatedPropagationParameters[k].getNbParams();

            // Covariance matrix
            final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
            if (nbDyn > 0) {
                final RealMatrix noiseP = covarianceMatricesProviders.get(k).
                        getInitialCovarianceMatrix(correctedSpacecraftStates[k]);
                noiseK.setSubMatrix(noiseP.getData(), 0, 0);
            }
            if (measurementProcessNoiseMatrix != null) {
                final RealMatrix noiseM = measurementProcessNoiseMatrix.
                        getInitialCovarianceMatrix(correctedSpacecraftStates[k]);
                noiseK.setSubMatrix(noiseM.getData(), nbDyn, nbDyn);
            }

            KalmanEstimatorUtil.checkDimension(noiseK.getRowDimension(),
                    builders.get(k).getOrbitalParametersDrivers(),
                    builders.get(k).getPropagationParametersDrivers(),
                    estimatedMeasurementsParameters);

            final int[] indK = covarianceIndirection[k];
            for (int i = 0; i < indK.length; ++i) {
                if (indK[i] >= 0) {
                    for (int j = 0; j < indK.length; ++j) {
                        if (indK[j] >= 0) {
                            physicalProcessNoise.setEntry(indK[i], indK[j], noiseK.getEntry(i, j));
                        }
                    }
                }
            }

        }
        final RealMatrix correctedCovariance = KalmanEstimatorUtil.normalizeCovarianceMatrix(physicalProcessNoise, scale);

        this.correctedEstimate = new ProcessEstimate(0.0, correctedState, correctedCovariance);
    }

    /** {@inheritDoc} */
    @Override
    public UnscentedEvolution getEvolution(final double previousTime, final RealVector[] sigmaPoints,
                                           final MeasurementDecorator measurement) {

        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builders.get(0).getInitialOrbitDate());
            }
        }

        // Increment measurement number
        ++currentMeasurementNumber;

        // Update the current date
        currentDate = measurement.getObservedMeasurement().getDate();

        // Initialize array of predicted sigma points
        final RealVector[] predictedSigmaPoints = new RealVector[sigmaPoints.length];

        // Propagate each sigma point
        for (int i = 0; i < sigmaPoints.length; i++) {

            // Set parameters for this sigma point
            final RealVector sigmaPoint = sigmaPoints[i].copy();
            updateParameters(sigmaPoint);

            // Get propagators
            final Propagator[] propagators = getEstimatedPropagators();

            // Do prediction
            predictedSigmaPoints[i] = predictState(observedMeasurement.getDate(), sigmaPoint, propagators);
        }

        // Reset the driver reference values based on the first sigma point
        int d = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            driver.setReferenceValue(referenceValues[d]);
            driver.setNormalizedValue(predictedSigmaPoints[0].getEntry(d));
            referenceValues[d] = driver.getValue();

            // Make remaining sigma points relative to the first
            for (int i = 1; i < predictedSigmaPoints.length; ++i) {
                predictedSigmaPoints[i].setEntry(d, predictedSigmaPoints[i].getEntry(d) - predictedSigmaPoints[0].getEntry(d));
            }
            predictedSigmaPoints[0].setEntry(d, 0.0);

            d += 1;
        }

        // compute process noise matrix
        // TODO: predictedSpacecraftStates are not valid here
        final RealMatrix physicalProcessNoise =
                MatrixUtils.createRealMatrix(sigmaPoints[0].getDimension(), sigmaPoints[0].getDimension());
        for (int k = 0; k < covarianceMatricesProviders.size(); ++k) {

            // Number of estimated measurement parameters
            final int nbMeas = estimatedMeasurementsParameters.getNbParams();

            // Number of estimated dynamic parameters (orbital + propagation)
            final int nbDyn  = orbitsEndColumns[k] - orbitsStartColumns[k] +
                    estimatedPropagationParameters[k].getNbParams();

            // Covariance matrix
            final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
            if (nbDyn > 0) {
                final RealMatrix noiseP = covarianceMatricesProviders.get(k).
                        getProcessNoiseMatrix(correctedSpacecraftStates[k],
                                predictedSpacecraftStates[k]);
                noiseK.setSubMatrix(noiseP.getData(), 0, 0);
            }
            if (measurementProcessNoiseMatrix != null) {
                final RealMatrix noiseM = measurementProcessNoiseMatrix.
                        getProcessNoiseMatrix(correctedSpacecraftStates[k],
                                predictedSpacecraftStates[k]);
                noiseK.setSubMatrix(noiseM.getData(), nbDyn, nbDyn);
            }

            KalmanEstimatorUtil.checkDimension(noiseK.getRowDimension(),
                    builders.get(k).getOrbitalParametersDrivers(),
                    builders.get(k).getPropagationParametersDrivers(),
                    estimatedMeasurementsParameters);

            final int[] indK = covarianceIndirection[k];
            for (int i = 0; i < indK.length; ++i) {
                if (indK[i] >= 0) {
                    for (int j = 0; j < indK.length; ++j) {
                        if (indK[j] >= 0) {
                            physicalProcessNoise.setEntry(indK[i], indK[j], noiseK.getEntry(i, j));
                        }
                    }
                }
            }

        }
        final RealMatrix normalizedProcessNoise = KalmanEstimatorUtil.normalizeCovarianceMatrix(physicalProcessNoise, scale);

        // Return
        return new UnscentedEvolution(measurement.getTime(), predictedSigmaPoints, normalizedProcessNoise);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector[] getPredictedMeasurements(final RealVector[] predictedSigmaPoints, final MeasurementDecorator measurement) {

        // Observed measurement
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();

        // Standard deviation as a vector
        final RealVector theoreticalStandardDeviation =
                MatrixUtils.createRealVector(observedMeasurement.getTheoreticalStandardDeviation());

        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedMeasurements = new RealVector[predictedSigmaPoints.length];

        // Loop on sigma points to predict measurements
        for (int i = 0; i < predictedSigmaPoints.length; i++) {
            // Set parameters for this sigma point
            final RealVector predictedSigmaPoint = predictedSigmaPoints[i].copy();
            updateParameters(predictedSigmaPoint);

            // Get propagators
            Propagator[] propagators = getEstimatedPropagators();

            // "updateParameters" sets the correct orbital and parameters info, but doesn't reset the time.
            for (int k = 0; k < propagators.length; ++k) {
                final SpacecraftState predictedState = propagators[k].getInitialState();
                final Orbit predictedOrbit = builders.get(k).getOrbitType().convertType(
                        new CartesianOrbit(predictedState.getPVCoordinates(),
                                predictedState.getFrame(),
                                observedMeasurement.getDate(),
                                predictedState.getMu()
                        )
                );
                builders.get(k).resetOrbit(predictedOrbit);
            }
            propagators = getEstimatedPropagators();

            // Predicted states
            final SpacecraftState[] predictedStates = new SpacecraftState[propagators.length];
            for (int k = 0; k < propagators.length; ++k) {
                predictedStates[k] = propagators[k].getInitialState();
            }

            // Calculated estimated measurement from predicted sigma point
            final EstimatedMeasurement<?> estimated = estimateMeasurement(observedMeasurement, currentMeasurementNumber,
                    predictedStates);
            predictedMeasurements[i] = new ArrayRealVector(estimated.getEstimatedValue())
                    .ebeDivide(theoreticalStandardDeviation);
        }

        // Return the predicted measurements
        return predictedMeasurements;

    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas,
                                    final RealVector predictedState, final RealMatrix innovationCovarianceMatrix) {
        // Set parameters from predicted state
        final RealVector predictedStateCopy = predictedState.copy();
        updateParameters(predictedStateCopy);

        // Standard deviation as a vector
        final RealVector theoreticalStandardDeviation =
                MatrixUtils.createRealVector(measurement.getObservedMeasurement().getTheoreticalStandardDeviation());

        // Get propagators
        Propagator[] propagators = getEstimatedPropagators();

        // "updateParameters" sets the correct orbital info, but doesn't reset the time.
        for (int k = 0; k < propagators.length; ++k) {
            final SpacecraftState predicted = propagators[k].getInitialState();
            final Orbit predictedOrbit = builders.get(k).getOrbitType().convertType(
                    new CartesianOrbit(predicted.getPVCoordinates(),
                            predicted.getFrame(),
                            measurement.getObservedMeasurement().getDate(),
                            predicted.getMu()
                    )
            );
            builders.get(k).resetOrbit(predictedOrbit);
        }
        propagators = getEstimatedPropagators();

        // Predicted states
        for (int k = 0; k < propagators.length; ++k) {
            predictedSpacecraftStates[k] = propagators[k].getInitialState();
        }

        // set estimated value to the predicted value from the filter
        predictedMeasurement = estimateMeasurement(measurement.getObservedMeasurement(), currentMeasurementNumber,
                predictedSpacecraftStates);
        predictedMeasurement.setEstimatedValue(predictedMeas.ebeMultiply(theoreticalStandardDeviation).toArray());

        // Check for outliers
        KalmanEstimatorUtil.applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);

        // Compute the innovation vector
        return KalmanEstimatorUtil.computeInnovationVector(predictedMeasurement,
                predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }


    private RealVector predictState(final AbsoluteDate date,
                                    final RealVector previousState,
                                    final Propagator[] propagators) {

        // Initialise predicted state
        final RealVector predictedState = previousState.copy();

        // Orbital parameters counter
        int jOrb = 0;

        // Loop over propagators
        for (int k = 0; k < propagators.length; ++k) {

            // Record original state
            final SpacecraftState originalState = propagators[k].getInitialState();

            // Propagate
            final SpacecraftState predicted = propagators[k].propagate(date);

            // Update the builder with the predicted orbit
            // This updates the orbital drivers with the values of the predicted orbit
            builders.get(k).resetOrbit(predicted.getOrbit());

            // The orbital parameters in the state vector are replaced with their predicted values
            // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction
            for (DelegatingDriver orbitalDriver : builders.get(k).getOrbitalParametersDrivers().getDrivers()) {
                if (orbitalDriver.isSelected()) {
                    orbitalDriver.setReferenceValue(referenceValues[jOrb]);
                    predictedState.setEntry(jOrb, orbitalDriver.getNormalizedValue());

                    jOrb += 1;
                }
            }

            // Set the builder back to the original time
            builders.get(k).resetOrbit(originalState.getOrbit());

        }

        return predictedState;
    }


    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        correctedEstimate = estimate;
        updateParameters(estimate.getState());

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final Propagator[] estimatedPropagators = getEstimatedPropagators();
        for (int k = 0; k < estimatedPropagators.length; ++k) {
            correctedSpacecraftStates[k] = estimatedPropagators[k].getInitialState();
        }

        // Corrected measurement
        correctedMeasurement = estimateMeasurement(observedMeasurement, currentMeasurementNumber,
                getCorrectedSpacecraftStates());
    }

    /**
     * Estimate measurement (without derivatives).
     * @param <T> measurement type
     * @param observedMeasurement observed measurement
     * @param measurementNumber measurement number
     * @param spacecraftStates states
     * @return estimated measurement
     * @since 12.1
     */
    private static <T extends ObservedMeasurement<T>> EstimatedMeasurement<T> estimateMeasurement(final ObservedMeasurement<T> observedMeasurement,
                                                                                                  final int measurementNumber,
                                                                                                  final SpacecraftState[] spacecraftStates) {
        final EstimatedMeasurementBase<T> estimatedMeasurementBase = observedMeasurement.
                estimateWithoutDerivatives(measurementNumber, measurementNumber,
                KalmanEstimatorUtil.filterRelevant(observedMeasurement, spacecraftStates));
        final EstimatedMeasurement<T> estimatedMeasurement = new EstimatedMeasurement<>(estimatedMeasurementBase.getObservedMeasurement(),
                estimatedMeasurementBase.getIteration(), estimatedMeasurementBase.getCount(),
                estimatedMeasurementBase.getStates(), estimatedMeasurementBase.getParticipants());
        estimatedMeasurement.setEstimatedValue(estimatedMeasurementBase.getEstimatedValue());
        return estimatedMeasurement;
    }

    /** Update parameter drivers with a normalised state, adjusting state according to the driver limits.
     * @param normalizedState the input state
     * The min/max allowed values are handled by the parameter themselves.
     */
    private void updateParameters(final RealVector normalizedState) {
        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setReferenceValue(referenceValues[i]);
            driver.setNormalizedValue(normalizedState.getEntry(i));
            normalizedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(normalizedState.getEntry(i));
            normalizedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(normalizedState.getEntry(i));
            normalizedState.setEntry(i++, driver.getNormalizedValue());
        }
    }

    /** Get the propagators estimated with the values set in the propagators builders.
     * @return propagators based on the current values in the builder
     */
    public Propagator[] getEstimatedPropagators() {
        // Return propagators built with current instantiation of the propagator builders
        final Propagator[] propagators = new Propagator[builders.size()];
        for (int k = 0; k < builders.size(); ++k) {
            propagators[k] = builders.get(k).buildPropagator();
        }
        return propagators;
    }

    /** Get the current corrected estimate.
     * @return current corrected estimate
     */
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getEstimatedOrbitalParameters() {
        return allEstimatedOrbitalParameters;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getEstimatedPropagationParameters() {
        return allEstimatedPropagationParameters;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return estimatedMeasurementsParameters;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState[] getPredictedSpacecraftStates() {
        return predictedSpacecraftStates.clone();
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState[] getCorrectedSpacecraftStates() {
        return correctedSpacecraftStates.clone();
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getPhysicalEstimatedState() {
        // Method {@link ParameterDriver#getValue()} is used to get
        // the physical values of the state.
        // The scales'array is used to get the size of the state vector
        final RealVector physicalEstimatedState = new ArrayRealVector(getEstimate().getState().getDimension());
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
        // Un-normalize the estimated covariance matrix (P) from Hipparchus and return it.
        // The covariance P is an mxm matrix where m = nbOrb + nbPropag + nbMeas
        // For each element [i,j] of P the corresponding normalized value is:
        // Pn[i,j] = P[i,j] / (scale[i]*scale[j])
        // Consequently: P[i,j] = Pn[i,j] * scale[i] * scale[j]
        return KalmanEstimatorUtil.unnormalizeCovarianceMatrix(correctedEstimate.getCovariance(), scale);
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
        // Un-normalize the innovation covariance matrix (S) from Hipparchus and return it.
        // S is an nxn matrix where n is the size of the measurement being processed by the filter
        // For each element [i,j] of normalized S (Sn) the corresponding physical value is:
        // S[i,j] = Sn[i,j] * σ[i] * σ[j]
        return correctedEstimate.getInnovationCovariance() == null ?
                null : KalmanEstimatorUtil.unnormalizeInnovationCovarianceMatrix(correctedEstimate.getInnovationCovariance(),
                predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalKalmanGain() {
        // Un-normalize the Kalman gain (K) from Hipparchus and return it.
        // K is an mxn matrix where:
        //  - m = nbOrb + nbPropag + nbMeas is the number of estimated parameters
        //  - n is the size of the measurement being processed by the filter
        // For each element [i,j] of normalized K (Kn) the corresponding physical value is:
        // K[i,j] = Kn[i,j] * scale[i] / σ[j]
        return correctedEstimate.getKalmanGain() == null ?
                null : KalmanEstimatorUtil.unnormalizeKalmanGainMatrix(correctedEstimate.getKalmanGain(),
                scale,
                correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
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

}
