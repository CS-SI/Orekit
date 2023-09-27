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

import java.util.ArrayList;
import java.util.Arrays;
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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

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

    /** Per-builder estimated orbital parameters. */
    private final ParameterDriversList[] estimatedOrbitalParameters;

    /** Per-builder estimated propagation parameters. */
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

    /** Provider for covariance matrice. */
    private final List<CovarianceMatrixProvider> covarianceMatrixProviders;

    /** Process noise matrix provider for measurement parameters. */
    private final CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Indirection arrays to extract the noise components for estimated parameters. */
    private final int[][] covarianceIndirection;

    /** Position angle types used during orbit determination. */
    private final PositionAngleType[] angleTypes;

    /** Orbit types used during orbit determination. */
    private final OrbitType[] orbitTypes;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Current corrected estimate. */
    private ProcessEstimate correctedEstimate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Previous date. */
    private AbsoluteDate previousDate;

    /** Predicted spacecraft states. */
    private SpacecraftState[] predictedSpacecraftStates;

    /** Corrected spacecraft states. */
    private SpacecraftState[] correctedSpacecraftStates;

    /** Predicted measurement. */
    private EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

    /** Unscented Kalman process model constructor (package private).
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatrixProviders provider for covariance matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    protected UnscentedKalmanModel(final List<PropagatorBuilder> propagatorBuilders,
                                   final List<CovarianceMatrixProvider> covarianceMatrixProviders,
                                   final ParameterDriversList estimatedMeasurementParameters,
                                   final CovarianceMatrixProvider measurementProcessNoiseMatrix) {

        this.builders                        = propagatorBuilders;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.currentDate                     = propagatorBuilders.get(0).getInitialOrbitDate();
        this.previousDate                    = currentDate;
        this.covarianceMatrixProviders       = covarianceMatrixProviders;
        this.measurementProcessNoiseMatrix   = measurementProcessNoiseMatrix;

        final Map<String, Integer> orbitalParameterColumns = new HashMap<>(6 * builders.size());
        orbitsStartColumns      = new int[builders.size()];
        orbitsEndColumns        = new int[builders.size()];
        int columns = 0;
        allEstimatedOrbitalParameters = new ParameterDriversList();
        estimatedOrbitalParameters    = new ParameterDriversList[builders.size()];
        // Set estimated orbital parameters
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

        // Set estimated propagation parameters
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
            propagationParameterColumns.put(driverName, columns++);
        }

        // Populate the map of measurement drivers' columns and update the total number of columns
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            // Verify if the driver reference date has been set
            if (parameter.getReferenceDate() == null) {
                parameter.setReferenceDate(currentDate);
            }
            measurementParameterColumns.put(parameter.getName(), columns);
            columns++;
        }

        // set angle and orbit types
        angleTypes = new PositionAngleType[builders.size()];
        orbitTypes = new OrbitType[builders.size()];
        for (int k = 0; k < builders.size(); k++) {
            angleTypes[k] = builders.get(k).getPositionAngleType();
            orbitTypes[k] = builders.get(k).getOrbitType();
        }

        // set covariance indirection
        this.covarianceIndirection = new int[covarianceMatrixProviders.size()][columns];

        for (int k = 0; k < covarianceIndirection.length; ++k) {
            final ParameterDriversList orbitDrivers      = builders.get(k).getOrbitalParametersDrivers();
            final ParameterDriversList parametersDrivers = builders.get(k).getPropagationParametersDrivers();
            Arrays.fill(covarianceIndirection[k], -1);
            int i = 0;
            for (final ParameterDriver driver : orbitDrivers.getDrivers()) {
                final Integer c = orbitalParameterColumns.get(driver.getName());
                covarianceIndirection[k][i++] = (c == null) ? -1 : c.intValue();
            }
            for (final ParameterDriver driver : parametersDrivers.getDrivers()) {
                final Integer c = propagationParameterColumns.get(driver.getName());
                if (c != null) {
                    covarianceIndirection[k][i++] = c.intValue();
                }
            }
            for (final ParameterDriver driver : estimatedMeasurementParameters.getDrivers()) {
                final Integer c = measurementParameterColumns.get(driver.getName());
                if (c != null) {
                    covarianceIndirection[k][i++] = c.intValue();
                }
            }
        }

        // Initialize the estimated state and fill its values
        final RealVector correctedState = MatrixUtils.createRealVector(columns);

        int p = 0;
        for (final ParameterDriver driver : allEstimatedOrbitalParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getValue());
        }
        for (final ParameterDriver driver : allEstimatedPropagationParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getValue());
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getValue());
        }

        this.predictedSpacecraftStates = new SpacecraftState[propagatorBuilders.size()];
        for (int i = 0; i < propagatorBuilders.size(); i++) {
            predictedSpacecraftStates[i] = propagatorBuilders.get(i).buildPropagator(propagatorBuilders.get(i).getSelectedNormalizedParameters()).getInitialState();
        }
        this.correctedSpacecraftStates = predictedSpacecraftStates.clone();

        // Number of estimated measurement parameters
        final int nbMeas = estimatedMeasurementParameters.getNbParams();

        final RealMatrix correctedCovariance = MatrixUtils.createRealMatrix(columns, columns);
        for (int k = 0; k < covarianceMatrixProviders.size(); k++) {
            // Number of estimated dynamic parameters (orbital + propagation)
            final int nbDyn  = orbitsEndColumns[k] - orbitsStartColumns[k] +
                               estimatedPropagationParameters[k].getNbParams();
            // Covariance matrix
            final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
            final RealMatrix noiseP = covarianceMatrixProviders.get(k).
                                      getInitialCovarianceMatrix(correctedSpacecraftStates[k]);
            noiseK.setSubMatrix(noiseP.getData(), 0, 0);
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
                            correctedCovariance.setEntry(indK[i], indK[j], noiseK.getEntry(i, j));
                        }
                    }
                }
            }
        }

        // Initialize corrected estimate
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

        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedStates       = new RealVector[sigmaPoints.length];

        // Loop on builders
        for (int k = 0; k < builders.size(); k++ ) {

            // Sigma points for the current builder
            final RealVector[] currentSigmaPoints = new ArrayRealVector[sigmaPoints.length];
            for (int i = 0; i < sigmaPoints.length; i++) {
                currentSigmaPoints[i] = sigmaPoints[i].getSubVector(orbitsStartColumns[k], orbitsEndColumns[k] - orbitsStartColumns[k]);
            }

            // Predict states
            final List<SpacecraftState> states = predictStates(currentSigmaPoints, k);

            // Loop on states
            for (int i = 0; i < states.size(); ++i) {
                if (k == 0) {
                    predictedStates[i] = new ArrayRealVector(sigmaPoints[i].getDimension());
                }
                // Current predicted state
                final SpacecraftState predicted = states.get(i);
                // First, convert the predicted state to an array
                final double[] predictedArray = new double[currentSigmaPoints[i].getDimension()];
                orbitTypes[k].mapOrbitToArray(predicted.getOrbit(), angleTypes[k], predictedArray, null);
                predictedStates[i].setSubVector(orbitsStartColumns[k], new ArrayRealVector(predictedArray));
            }

        }

        // compute process noise matrix
        final RealMatrix processNoise = MatrixUtils.createRealMatrix(sigmaPoints[0].getDimension(), sigmaPoints[0].getDimension());

        for (int k = 0; k < covarianceMatrixProviders.size(); ++k) {

            // Number of estimated measurement parameters
            final int nbMeas = estimatedMeasurementsParameters.getNbParams();

            // Number of estimated dynamic parameters (orbital + propagation)
            final int nbDyn  = orbitsEndColumns[k] - orbitsStartColumns[k] +
                               estimatedPropagationParameters[k].getNbParams();

            // Covariance matrix
            final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
            final RealMatrix noiseP = covarianceMatrixProviders.get(k).
                                      getProcessNoiseMatrix(correctedSpacecraftStates[k],
                                                            predictedSpacecraftStates[k]);
            noiseK.setSubMatrix(noiseP.getData(), 0, 0);
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
                            processNoise.setEntry(indK[i], indK[j], noiseK.getEntry(i, j));
                        }
                    }
                }
            }

        }

        // Update epoch
        previousDate = currentDate;

        // Return
        return new UnscentedEvolution(measurement.getTime(), predictedStates, processNoise);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector[] getPredictedMeasurements(final RealVector[] predictedSigmaPoints, final MeasurementDecorator measurement) {

        // Observed measurement
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();

        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedMeasurements = new RealVector[predictedSigmaPoints.length];

        // Initialize the relevant states used for measurement estimation
        final SpacecraftState[][] statesForMeasurementEstimation = new SpacecraftState[predictedSigmaPoints.length][builders.size()];

        // Convert sigma points to spacecraft states
        for (int k = 0; k < builders.size(); k++ ) {

            // Loop on sigma points
            for (int i = 0; i < predictedSigmaPoints.length; i++) {
                final SpacecraftState state = new SpacecraftState(orbitTypes[k].mapArrayToOrbit(predictedSigmaPoints[i].toArray(), null,
                                                                                                angleTypes[k], currentDate, builders.get(k).getMu(),
                                                                                                builders.get(k).getFrame()));
                statesForMeasurementEstimation[i][k] = state;
            }

        }

        // Loop on sigma points to predict measurements
        for (int i = 0; i < predictedSigmaPoints.length; i++) {
            final EstimatedMeasurement<?> estimated = observedMeasurement.estimate(currentMeasurementNumber, currentMeasurementNumber,
                                                                                   KalmanEstimatorUtil.filterRelevant(observedMeasurement,
                                                                                                                      statesForMeasurementEstimation[i]));
            predictedMeasurements[i] = new ArrayRealVector(estimated.getEstimatedValue());
        }

        // Return the predicted measurements
        return predictedMeasurements;

    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas,
                                    final RealVector predictedState, final RealMatrix innovationCovarianceMatrix) {

        // Loop on builders
        for (int k = 0; k < builders.size(); k++) {

            // Update predicted states
            final double[] predictedStateArray = predictedState.getSubVector(orbitsStartColumns[k], orbitsEndColumns[k] - orbitsStartColumns[k]).toArray();
            final Orbit predictedOrbit = orbitTypes[k].mapArrayToOrbit(predictedStateArray, null, angleTypes[k],
                                                                       currentDate, builders.get(k).getMu(), builders.get(k).getFrame());
            predictedSpacecraftStates[k] = new SpacecraftState(predictedOrbit);

            // Update the builder with the predicted orbit
            builders.get(k).resetOrbit(predictedOrbit);

        }

        // Predicted measurement
        predictedMeasurement = measurement.getObservedMeasurement().estimate(currentMeasurementNumber, currentMeasurementNumber,
                                                                             KalmanEstimatorUtil.filterRelevant(measurement.getObservedMeasurement(),
                                                                                                                predictedSpacecraftStates));
        predictedMeasurement.setEstimatedValue(predictedMeas.toArray());

        // set estimated value to the predicted value by the filter
        KalmanEstimatorUtil.applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);

        // Compute the innovation vector (not normalized for unscented Kalman filter)
        return KalmanEstimatorUtil.computeInnovationVector(predictedMeasurement);

    }

    /**
     * Predict the predicted states for the given sigma points.
     * @param sigmaPoints current sigma points
     * @param index the index corresponding to the satellite one is dealing with
     * @return predicted state for the given sigma point
     */
    private List<SpacecraftState> predictStates(final RealVector[] sigmaPoints, final int index) {

        // Loop on sigma points to create the propagator parallelizer
        final List<Propagator> propagators = new ArrayList<>(sigmaPoints.length);
        for (int k = 0; k < sigmaPoints.length; ++k) {
            // Current sigma point
            final double[] currentPoint = sigmaPoints[k].copy().toArray();
            // Create the corresponding orbit propagator
            final Propagator currentPropagator = createPropagator(currentPoint, index);
            // Add it to the list of propagators
            propagators.add(currentPropagator);
        }

        // Create the propagator parallelizer and predict states
        // (the shift is done to start a little bit before the previous measurement epoch)
        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, interpolators -> { });
        final List<SpacecraftState>   states       = parallelizer.propagate(previousDate.shiftedBy(-1.0e-3), currentDate);

        // Return
        return states;

    }

    /**
     * Create a propagator for the given sigma point.
     * @param point input sigma point
     * @param index the index corresponding to the satellite one is dealing with
     * @return the corresponding orbit propagator
     */
    private Propagator createPropagator(final double[] point, final int index) {
        // Create a new instance of the current propagator builder
        final PropagatorBuilder copy = builders.get(index).copy();
        // Convert the given sigma point to an orbit
        final Orbit orbit = orbitTypes[index].mapArrayToOrbit(point, null, angleTypes[index], copy.getInitialOrbitDate(),
                                                      copy.getMu(), copy.getFrame());
        copy.resetOrbit(orbit);
        // Create the propagator
        final Propagator propagator = copy.buildPropagator(copy.getSelectedNormalizedParameters());
        return propagator;
    }

    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {

        correctedEstimate = estimate;

        // Loop on builders
        for (int k = 0; k < builders.size(); k++ ) {

            // Update corrected state
            final double[] correctedStateArray = estimate.getState().getSubVector(orbitsStartColumns[k], orbitsEndColumns[k] - orbitsStartColumns[k]).toArray();
            final Orbit correctedOrbit = orbitTypes[k].mapArrayToOrbit(correctedStateArray, null, angleTypes[k],
                                                                   currentDate, builders.get(k).getMu(), builders.get(k).getFrame());
            correctedSpacecraftStates[k] = new SpacecraftState(correctedOrbit);

            // Update the builder
            builders.get(k).resetOrbit(correctedOrbit);

        }

        // Corrected measurement
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            KalmanEstimatorUtil.filterRelevant(observedMeasurement,
                                                                                               getCorrectedSpacecraftStates()));
    }

    /** Get the propagators estimated with the values set in the propagators builders.
     * @return propagators based on the current values in the builder
     */
    public Propagator[] getEstimatedPropagators() {
        // Return propagators built with current instantiation of the propagator builders
        final Propagator[] propagators = new Propagator[builders.size()];
        for (int k = 0; k < builders.size(); ++k) {
            propagators[k] = builders.get(k).buildPropagator(builders.get(k).getSelectedNormalizedParameters());
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

}
