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
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Class defining the process model dynamics to use with a {@link UnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 */
public class UnscentedKalmanModel implements KalmanEstimation, UnscentedProcess<MeasurementDecorator> {


//    /** Builder for propagator. */
//    private final List<OrbitDeterminationPropagatorBuilder> builders;

    
    /** Builder for propagator. */
    private final OrbitDeterminationPropagatorBuilder builder;

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
    
//    /** Reference Values */
//    private final double[] referenceValues;
    
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

    /** Predicted measurements. */
    private List<EstimatedMeasurement<?>> predictedMeasurements;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

    /** Unscented Kalman process model constructor (package private).
     * @param propagatorBuilder propagators builders used to evaluate the orbits.
     * @param covarianceMatrixProvider provider for covariance matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     * @param propagationType propagation type
     * @param stateType state type
     */
    protected UnscentedKalmanModel(final OrbitDeterminationPropagatorBuilder propagatorBuilder,
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

//        // Compute the reference values
//        int l = 0;
//        this.referenceValues = new double[estimatedOrbitalParameters.getNbParams()];
//        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
//            referenceValues[l++] = driver.getReferenceValue();
//        }

        // Initialize the estimated normalized state and fill its values
        final RealVector correctedState = MatrixUtils.createRealVector(columns);

        int p = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedPropagationParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }

//        // Set up initial covariance
//        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(columns, columns);

        // Number of estimated measurement parameters
        final int nbMeas = estimatedMeasurementParameters.getNbParams();

        // Number of estimated dynamic parameters (orbital + propagation)
        final int nbDyn  = estimatedOrbitalParameters.getNbParams() +
                           estimatedPropagationParameters.getNbParams();


//        this.builders = new ArrayList<OrbitDeterminationPropagatorBuilder>(2 * (nbDyn + nbMeas) + 1);
//        for (int i = 0; i < 2 * (nbDyn + nbMeas) + 1; ++i) {
//            builders.add(propagatorBuilder);
//        }

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

        final RealMatrix correctedCovariance = normalizeCovarianceMatrix(noiseK);

        // Initialize corrected estimate
        this.correctedEstimate = new ProcessEstimate(0.0, correctedState, correctedCovariance);

    }

//    /**
//     * Create propagators using sigma points as states.
//     *
//     * @param sigmaPoints sigma points
//     * @return propagators created by sigma points
//     */
//    public List<Propagator> createPropagators(final RealVector[] sigmaPoints) {
//
//        final List<Propagator> propagators = new ArrayList<Propagator>(sigmaPoints.length);
//
//        // Set up the propagators
//        for (int i = 0; i < sigmaPoints.length; ++i) {
//
//            // Get the number of selected orbital drivers in the builder
//            final int nbOrb    = estimatedOrbitalParameters.getNbParams();
//
//            // Get the list of selected propagation drivers in the builder and its size
//            final int nbParams = estimatedPropagationParameters.getNbParams();
//
//            // Init the array of normalized parameters for the builder
//            final double[] propagatorArray = new double[nbOrb + nbParams];
//            // Add the orbital drivers normalized values
//            for (int j = 0; j < nbOrb; ++j) {
//                propagatorArray[j] = sigmaPoints[i].getEntry(j);
//            }
//
//            // Add the propagation drivers normalized values
//            for (int j = 0; j < nbParams; ++j) {
//                propagatorArray[nbOrb + j] =
//                                sigmaPoints[i].getEntry(propagationParameterColumns.get(estimatedPropagationParameters.getDrivers().get(j).getName()));
//            }
//
//            // Build the propagator
//            propagators.add(builders.get(i).buildPropagator(propagatorArray));
//        }
//
//        return propagators;
//    }

    @Override
    public UnscentedEvolution getEvolution(final double previousTime, final RealVector[] sigmaPoints,
           final MeasurementDecorator measurement) {
        System.out.println("Mesure: " + Arrays.toString(measurement.getObservedMeasurement().getObservedValue()));
        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builder.getInitialOrbitDate());
            }
        }

        // Prepare list of predicted measurements
        predictedMeasurements = new ArrayList<EstimatedMeasurement<?>>(sigmaPoints.length);

        // Increment measurement number
        ++currentMeasurementNumber;

//        // Update the previous epoch (used by the propagator parallelizer)
//        final AbsoluteDate previousDate = currentDate;

        // Update the current date
        currentDate = measurement.getObservedMeasurement().getDate();

        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedStates = new RealVector[sigmaPoints.length];
        final RealVector[] measurements    = new RealVector[sigmaPoints.length];

//        final RealVector temp = new ArrayRealVector(sigmaPoints[0].getDimension());
//        // Predict states
//        final List<SpacecraftState> predictedSpacecraftStates = predictStates(previousDate, observedMeasurement.getDate(), sigmaPoints);

        // Loop on sigma points
        for (int k = 0; k < sigmaPoints.length; ++k) {

            // Create the propagator corresponding to the current sigma point
            final Propagator propagator = getEstimatedPropagator(sigmaPoints[k].toArray());

            // Propagate
            final SpacecraftState predicted = propagator.propagate(currentDate);
            resetOrbit(predicted.getOrbit());
            final double[] normalizedPredictedState = new double[sigmaPoints[k].getDimension()];
            int index = 0;
            for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
                normalizedPredictedState[index++] = driver.getNormalizedValue();
            }
            while (index < sigmaPoints[k].getDimension()) {
                normalizedPredictedState[index] = sigmaPoints[k].getEntry(index);
                index++;
            }
            
            predictedStates[k] = new ArrayRealVector(normalizedPredictedState);

            // Estimate the measurement
            final EstimatedMeasurement<?> estimated = observedMeasurement.estimate(currentMeasurementNumber,
                                                                                   currentMeasurementNumber, 
                                                                                   new SpacecraftState[] {
                                                                                           predicted
                                                                                   });
            predictedMeasurements.add(estimated);
//            Vector3D p = new Vector3D(Arrays.copyOfRange(estimated.getEstimatedValue(), 0, 2));
//            Vector3D v = new Vector3D(Arrays.copyOfRange(estimated.getEstimatedValue(), 3, 5));
//            TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(currentDate, p, v);
//            Orbit orbit = new KeplerianOrbit(pv, correctedSpacecraftState.getFrame(), currentDate, correctedSpacecraftState.getOrbit().getMu());
            measurements[k] = new ArrayRealVector(estimated.getEstimatedValue());
            
//            // Update the builder with the predicted orbit
//            // This updates the orbital drivers with the values of the predicted orbit
//            builders.get(k).resetOrbit(predictedSpacecraftStates.get(k).getOrbit());
//
//            // The orbital parameters in the state vector are replaced with their predicted values
//            // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
//            // As the propagator builder was previously updated with the predicted orbit,
//            // the selected orbital drivers are already up to date with the prediction
//            int jOrb = 0;
//
//            for (DelegatingDriver orbitalDriver : builders.get(k).getOrbitalParametersDrivers().getDrivers()) {
//                if (orbitalDriver.isSelected()) {
//                    temp.setEntry(jOrb++, orbitalDriver.getValue());
//                }
//            }
//            predictedSampleStates[k] = normalizeState(temp,referenceValues);
//
//            // Predict the measurement based on predicted sigma points
//            predictedMeasurements.add(observedMeasurement.estimate(currentMeasurementNumber,
//                    currentMeasurementNumber,
//                    new SpacecraftState[] { predictedSpacecraftStates.get(k)
//                        }));
//
//            measurements[k] = new ArrayRealVector(predictedMeasurements.get(k).getEstimatedValue());

        }
//        // set predicted measurement to gather observed measurement
//        // set estimated measurement in getInnovation()
        predictedMeasurement = predictedMeasurements.get(0);

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

        final RealMatrix normalizedProcessNoise = normalizeCovarianceMatrix(noiseK);

        return new UnscentedEvolution(measurement.getTime(), predictedStates, measurements, normalizedProcessNoise);
    }

    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas, final RealVector predictedNormalizedState,
            final RealMatrix innovationCovarianceMatrix) {

        // update the predicted spacecraft state with predictedNormalizedState
        predictedSpacecraftState = getEstimatedPropagator(predictedNormalizedState.toArray()).getInitialState();

        // set estimated value to the predicted value by the filter
        predictedMeasurement.setEstimatedValue(predictedMeas.toArray());

        applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);
        if (predictedMeasurement.getStatus() == EstimatedMeasurement.Status.REJECTED)  {
            // set innovation to null to notify filter measurement is rejected
            return null;
        } else {
            // Normalized innovation of the measurement (Nx1)
            final double[] observed  = predictedMeasurement.getObservedMeasurement().getObservedValue();
            final double[] estimated = predictedMeasurement.getEstimatedValue();
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
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        correctedEstimate = estimate;
        updateParameters();
        // get the corrected spacecraft state previously updated in the builder
        correctedSpacecraftState = getEstimatedPropagator(correctedEstimate.getState().toArray()).getInitialState(); // TODO initialize using correctedEstimate (see finalizeEstimation de AbstractKalmanModel)


        // Compute the estimated measurement using estimated spacecraft state
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            new SpacecraftState[] {correctedSpacecraftState});

    }


    /** Get the current corrected estimate.
     * @return current corrected estimate
     */
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** Get the propagator estimated with the values in normalizedParameters
     * @param normalizedParameters normalized parameters
     * @return propagator based on the the values in normalizedParameters
     */
    public Propagator getEstimatedPropagator(final double[] normalizedParameters) {
        // Return propagator built with current instantiation of the propagator builder
        return builder.buildPropagator(normalizedParameters);
    }
    
    /** Get the propagators estimated with the values set in the propagator builder.
     * @return propagators based on the current values in the builder
     */
    public Propagator getEstimatedPropagator() {
        return builder.buildPropagator(builder.getSelectedNormalizedParameters());
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
    public RealVector[] getPhysicalEstimatedSigmaPoints (final RealVector[] sigmaPoints) {
        final RealVector[] physicalSigmaPoints = new ArrayRealVector[sigmaPoints.length];
        final List<Double> refValues = new ArrayList<>();
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            refValues.add(driver.getReferenceValue());
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            refValues.add(driver.getReferenceValue());
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            refValues.add(driver.getReferenceValue());
        }
        for (int i = 0; i < sigmaPoints.length; ++i) {
            RealVector temp = new ArrayRealVector(sigmaPoints[i].getDimension());
            for (int j = 0; j < sigmaPoints[i].getDimension(); ++j) {
               temp.setEntry(j, sigmaPoints[i].getEntry(j) * scale [j] + refValues.get(j));
            }
            physicalSigmaPoints[i] = temp;
        }
        return physicalSigmaPoints;
    }
    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        // Un-normalize the estimated covariance matrix (P) from Hipparchus and return it.
        // The covariance P is an mxm matrix where m = nbOrb + nbPropag + nbMeas
        // For each element [i,j] of P the corresponding normalized value is:
        // Pn[i,j] = P[i,j] / (scale[i]*scale[j])
        // Consequently: P[i,j] = Pn[i,j] * scale[i] * scale[j]

        // Normalized covariance matrix
        final RealMatrix normalizedP = correctedEstimate.getCovariance();

        // Initialize physical covariance matrix
        final int nbParams = normalizedP.getRowDimension();
        final RealMatrix physicalP = MatrixUtils.createRealMatrix(nbParams, nbParams);

        // Un-normalize the covairance matrix
        for (int i = 0; i < nbParams; ++i) {
            for (int j = 0; j < nbParams; ++j) {
                physicalP.setEntry(i, j, normalizedP.getEntry(i, j) * scale[i] * scale[j]);
            }
        }
        return physicalP;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalStateTransitionMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalMeasurementJacobian() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalInnovationCovarianceMatrix() {
        // Un-normalize the innovation covariance matrix (S) from Hipparchus and return it.
        // S is an nxn matrix where n is the size of the measurement being processed by the filter
        // For each element [i,j] of normalized S (Sn) the corresponding physical value is:
        // S[i,j] = Sn[i,j] * σ[i] * σ[j]

        // Normalized matrix
        final RealMatrix normalizedS = correctedEstimate.getInnovationCovariance();

        if (normalizedS == null) {
            return null;
        } else {
            // Get current measurement sigmas
            final double[] sigmas = correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();

            // Initialize physical matrix
            final int nbMeas = sigmas.length;
            final RealMatrix physicalS = MatrixUtils.createRealMatrix(nbMeas, nbMeas);

            // Un-normalize the matrix
            for (int i = 0; i < nbMeas; ++i) {
                for (int j = 0; j < nbMeas; ++j) {
                    physicalS.setEntry(i, j, normalizedS.getEntry(i, j) * sigmas[i] *   sigmas[j]);
                }
            }
            return physicalS;
        }
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

        // Normalized matrix
        final RealMatrix normalizedK = correctedEstimate.getKalmanGain();

        if (normalizedK == null) {
            return null;
        } else {
            // Get current measurement sigmas
            final double[] sigmas = correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();

            // Initialize physical matrix
            final int nbLine = normalizedK.getRowDimension();
            final int nbCol  = normalizedK.getColumnDimension();
            final RealMatrix physicalK = MatrixUtils.createRealMatrix(nbLine, nbCol);

            // Un-normalize the matrix
            for (int i = 0; i < nbLine; ++i) {
                for (int j = 0; j < nbCol; ++j) {
                    physicalK.setEntry(i, j, normalizedK.getEntry(i, j) * scale[i] / sigmas[j]);
                }
            }
            return physicalK;
        }
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

//    private List<SpacecraftState> predictStates(final AbsoluteDate start, final AbsoluteDate target, final RealVector[] sigmaPoints) {
//
//        final List<Propagator> propagators = createPropagators(sigmaPoints);
//        final List<SpacecraftState> predictedSpacecraftStates = new ArrayList<SpacecraftState>(sigmaPoints.length);
//        // PropagatorsParallelizer propagator = new PropagatorsParallelizer(propagators, new UnscentedStepHandler());
//
//        for (int i = 0; i < sigmaPoints.length; ++i) {
//            predictedSpacecraftStates.add(propagators.get(i).propagate(start, target));
//        }
//        // predictedSpacecraftStates = propagator.propagate(start, target);
//
//        return predictedSpacecraftStates;
//    }

//    /**
//     * Normalize state.
//     * @param physicalState
//     * @param referenceValues
//     * @return
//     */
//    private RealVector normalizeState(final RealVector physicalState, final double[] referenceValues) {
//        final int nbParams = physicalState.getDimension();
//        final RealVector normalizedStateVector = MatrixUtils.createRealVector(nbParams);
//        
//        for (int i = 0; i < nbParams; ++i) {
//            normalizedStateVector.setEntry(i, (physicalState.getEntry(i) - referenceValues[i]) / scale[i]);
//        }
//        return normalizedStateVector;
//    }
    
    /** Normalize a covariance matrix.
     * The covariance P is an mxm matrix where m = nbOrb + nbPropag + nbMeas
     * For each element [i,j] of P the corresponding normalized value is:
     * Pn[i,j] = P[i,j] / (scale[i]*scale[j])
     * @param physicalCovarianceMatrix The "physical" covariance matrix in input
     * @return the normalized covariance matrix
     */
    private RealMatrix normalizeCovarianceMatrix(final RealMatrix physicalCovarianceMatrix) {

        // Initialize output matrix
        final int nbParams = physicalCovarianceMatrix.getRowDimension();
        final RealMatrix normalizedCovarianceMatrix = MatrixUtils.createRealMatrix(nbParams, nbParams);

        // Normalize the state matrix
        for (int i = 0; i < nbParams; ++i) {
            for (int j = 0; j < nbParams; ++j) {
                normalizedCovarianceMatrix.setEntry(i, j,
                                                    physicalCovarianceMatrix.getEntry(i, j) /
                                                    (scale[i] * scale[j]));
            }
        }
        return normalizedCovarianceMatrix;
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
            final StringBuilder builder = new StringBuilder();
            for (final ParameterDriver driver : orbitalParameters.getDrivers()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(driver.getName());
            }
            for (final ParameterDriver driver : propagationParameters.getDrivers()) {
                if (driver.isSelected()) {
                    builder.append(driver.getName());
                }
            }
            for (final ParameterDriver driver : measurementParameters.getDrivers()) {
                if (driver.isSelected()) {
                    builder.append(driver.getName());
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
     * @param innovationCovarianceMatrix So called innovation covariance matrix S, with:<p>
     *        S = H.Ppred.Ht + R<p>
     *        Where:<p>
     *         - H is the normalized measurement matrix (Ht its transpose)<p>
     *         - Ppred is the normalized predicted covariance matrix<p>
     *         - R is the normalized measurement noise matrix
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

    /** Update the estimated parameters after the correction phase of the filter.
     * The min/max allowed values are handled by the parameter themselves.
     */
    private void updateParameters() {
        final RealVector correctedState = correctedEstimate.getState();
        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(driver.getNormalizedValue() + correctedState.getEntry(i++));
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(driver.getNormalizedValue() + correctedState.getEntry(i++));
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(driver.getNormalizedValue() + correctedState.getEntry(i++));
        }
    }

    /** Reset the orbit in the propagator builder.
     * @param newOrbit New orbit to set in the propagator builder
     */
    public void resetOrbit(final Orbit newOrbit) {

        // Map the new orbit in an array of double
        final double[] orbitArray = new double[6];
        builder.getOrbitType().mapOrbitToArray(newOrbit, builder.getPositionAngle(), orbitArray, null);

        // Update all the orbital drivers, selected or unselected
        // Reset values and reference values
        final List<DelegatingDriver> orbitalDriversList = getEstimatedOrbitalParameters().getDrivers();
        int i = 0;
        for (DelegatingDriver driver : orbitalDriversList) {
            driver.setValue(orbitArray[i++]);
        }

    }
    
    protected void setParameters(final double[] parameters) {

        int index = 0;

        // manage orbital parameters
        for (final ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            if (driver.isSelected()) {
                driver.setValue(parameters[index++]);
            }
        }

        // manage propagation parameters
        for (final ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
            if (driver.isSelected()) {
                driver.setValue(parameters[index++]);
            }
        }

    }
    


}
