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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.unscented.UnscentedEvolution;
import org.hipparchus.filtering.kalman.unscented.UnscentedKalmanFilter;
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
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Class defining the process model dynamics to use with a {@link UnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 */
public class SemiAnalyticalUnscentedKalmanModel implements KalmanEstimation, UnscentedProcess<MeasurementDecorator> {

    /** Initial builder for propagator. */
    private final DSSTPropagatorBuilder builder;

    /** Builders for propagators. */
    private List<DSSTPropagatorBuilder> propagatorBuilders;

    /** Propagators. */
    private List<Propagator> propagators;

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

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

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

    /** Nominal mean spacecraft states. */
    private List<SpacecraftState> nominalMeanSpacecraftStates;

    /** Previous nominal mean spacecraft states. */
    private List<SpacecraftState> previousNominalMeanSpacecraftStates;

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
    protected SemiAnalyticalUnscentedKalmanModel(final DSSTPropagatorBuilder propagatorBuilder,
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
        this.propagators                     = new ArrayList<>();
        this.propagatorBuilders              = new ArrayList<>();
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

    /** Get the observer for Kalman Filter estimations.
     * @return the observer for Kalman Filter estimations
     */
    public KalmanObserver getObserver() {
        return observer;
    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Process a single measurement.
     * <p>
     * Update the filter with the new measurements.
     * </p>
     * @param observedMeasurements the list of measurements to process
     * @param filter Extended Kalman Filter
     * @return estimated propagator
     */
    public DSSTPropagator processMeasurements(final List<ObservedMeasurement<?>> observedMeasurements,
                                              final UnscentedKalmanFilter<MeasurementDecorator> filter) {
        try {
            // Compute sigma points
            
            final RealVector[] sigmaPoints = filter.unscentedTransform();

            // Create builders using the sigma points
            propagatorBuilders = getEstimatedBuilders(sigmaPoints);

            // Create propagators using the sigma points
            propagators = getEstimatedPropagators();

            // Initialize spacecraft states
            initializeMeanSpacecraftStates(sigmaPoints);

            // Sort the measurement
            observedMeasurements.sort(new ChronologicalComparator());

            final UskfMeasurementHandler stepHandler = new UskfMeasurementHandler(this, filter, observedMeasurements, builder.getInitialOrbitDate(), propagators, propagatorBuilders, sigmaPoints);
            final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, stepHandler);

            parallelizer.propagate(observedMeasurements.get(0).getDate(), observedMeasurements.get(observedMeasurements.size() - 1).getDate());

            // Return the last estimated propagator
            return getEstimatedPropagator(correctedEstimate.getState().toArray());

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }
    @Override
    public UnscentedEvolution getEvolution(final double previousTime, final RealVector[] sigmaPoints,
           final MeasurementDecorator measurement) {

        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        final RealVector[] predictedMeasurements = new RealVector[sigmaPoints.length];

        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builder.getInitialOrbitDate());
            }
        }

        // Increment measurement number
        ++currentMeasurementNumber;

        // Update the current date
        currentDate = measurement.getObservedMeasurement().getDate();
        final RealVector[] nominalMeanElementsStates = computeMeanElementsStates();
        // Calculate the predicted osculating elements
        final RealVector[] osculating = computeOsculatingElementsStates();
        System.out.println("============================================================");
        System.out.println("                    MEASUREMENT PREDICTION                  ");
        System.out.println("============================================================");
        for (int i = 0; i < osculating.length; i++) {
            final Orbit osculatingOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(osculating[i].toArray(), null, builder.getPositionAngle(),
                    currentDate, nominalMeanSpacecraftStates.get(i).getMu(),
                    nominalMeanSpacecraftStates.get(i).getFrame());
//            final Orbit meanOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(nominalMeanElementsStates[i].toArray(), null, builder.getPositionAngle(),
//                    currentDate, nominalMeanSpacecraftStates.get(i).getMu(),
//                    nominalMeanSpacecraftStates.get(i).getFrame());

            final EstimatedMeasurement<?> estimated = observedMeasurement.estimate(currentMeasurementNumber,
                    currentMeasurementNumber,
                    new SpacecraftState[] {
                        new SpacecraftState(osculatingOrbit,
                        nominalMeanSpacecraftStates.get(i).getAttitude(),
                        nominalMeanSpacecraftStates.get(i).getMass(),
                        nominalMeanSpacecraftStates.get(i).getAdditionalStatesValues(),
                        nominalMeanSpacecraftStates.get(i).getAdditionalStatesDerivatives())
                        });
            predictedMeasurements[i] = new ArrayRealVector(estimated.getEstimatedValue());
            System.out.println("--------------------------------------------------------------------------------------");
            System.out.println("Osculating orbit: " + osculatingOrbit);
            System.out.println("Predicted osculating orbit measurement n°" + i + ": " + predictedMeasurements[i].getEntry(0));
            System.out.println("--------------------------------------------------------------------------------------");
            
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



        return new UnscentedEvolution(measurement.getTime(), nominalMeanElementsStates, predictedMeasurements, noiseK);
    }

    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas, final RealVector predictedNormalizedState,
            final RealMatrix innovationCovarianceMatrix) {

        // update the predicted spacecraft state with predictedNormalizedState
        SpacecraftState newPredictedState = getEstimatedSpacecraftState(predictedNormalizedState.toArray());

        final double[] array    = new double[6];
        final double[] arrayDot = new double[6];
        builder.getOrbitType().mapOrbitToArray(newPredictedState.getOrbit(), builder.getPositionAngle(), array, arrayDot);;


        newPredictedState = new SpacecraftState(builder.getOrbitType().mapArrayToOrbit(array, arrayDot, builder.getPositionAngle(), currentDate, newPredictedState.getMu(), newPredictedState.getFrame()));
        // Calculate the corrected osculating elements
        final double[] osculating = computeOsculatingElements(newPredictedState);
        final Orbit osculatingOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(osculating, null, builder.getPositionAngle(),
                                                                            currentDate, newPredictedState.getMu(),
                                                                            newPredictedState.getFrame());
        predictedSpacecraftState = new SpacecraftState(osculatingOrbit);
        predictedMeasurement = measurement.getObservedMeasurement().estimate(currentMeasurementNumber, currentMeasurementNumber, getPredictedSpacecraftStates());
        // set estimated value to the predicted value by the filter
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
        correctedEstimate = estimate;
        previousNominalMeanSpacecraftStates = nominalMeanSpacecraftStates;
        
        final DSSTPropagatorBuilder copy = getEstimatedBuilder(estimate.getState().toArray());

        // update the predicted spacecraft state with predictedNormalizedState
        SpacecraftState newCorrectedState = getEstimatedSpacecraftState(estimate.getState().toArray());
        final double[] array    = new double[6];
        final double[] arrayDot = new double[6];
        copy.getOrbitType().mapOrbitToArray(newCorrectedState.getOrbit(), builder.getPositionAngle(), array, arrayDot);
        newCorrectedState = new SpacecraftState(copy.getOrbitType().mapArrayToOrbit(array, arrayDot, copy.getPositionAngle(), currentDate, newCorrectedState.getMu(), newCorrectedState.getFrame()));
        // Calculate the corrected osculating elements
        final double[] osculating = computeOsculatingElements(newCorrectedState);
        final Orbit osculatingOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(osculating, null, builder.getPositionAngle(),
                                                                            currentDate, newCorrectedState.getMu(),
                                                                            newCorrectedState.getFrame());
        correctedSpacecraftState = new SpacecraftState(osculatingOrbit);

        // Compute the corrected measurements
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            new SpacecraftState[] {
                                                                new SpacecraftState(osculatingOrbit,
                                                                                    correctedSpacecraftState.getAttitude(),
                                                                                    correctedSpacecraftState.getMass(),
                                                                                    correctedSpacecraftState.getAdditionalStatesValues(),
                                                                                    correctedSpacecraftState.getAdditionalStatesDerivatives())
                                                            });

        // Update current date in the builder
        builder.resetOrbit(correctedSpacecraftState.getOrbit()); 
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("                          FINALIZING ESTIMATION (OREKIT)                              ");
        System.out.println("--------------------------------------------------------------------------------------");
        double [] measurement = observedMeasurement.getObservedValue();
        System.out.println("Observed Measurement: (" + measurement[0] + ")" );
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Predicted Spacecraft state: ");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(predictedSpacecraftState.getOrbit());
        System.out.println("Cartesian: " + predictedSpacecraftState.getOrbit().getPVCoordinates());
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        measurement = predictedMeasurement.getEstimatedValue();
        System.out.println("Predicted Measurement: (" + measurement[0] + ")" );
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Corrected Spacecraft state: " );
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(correctedSpacecraftState.getOrbit());
        System.out.println("Cartesian: " + correctedSpacecraftState.getOrbit().getPVCoordinates());
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        measurement = correctedMeasurement.getEstimatedValue();
        System.out.println("Corrected Measurement: (" + measurement[0] + ")" );
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Kalman Gain: " + getPhysicalKalmanGain());
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Covariance matrix: " + getPhysicalEstimatedCovarianceMatrix());
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");

    }


    /** Get the current corrected estimate.
     * @return current corrected estimate
     */
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** Get the propagator estimated with the values in the state.
     * @param state state            // Initialize spacecraft states
            initializeNominalMeanSpacecraftStates(sigmaPoints);
     * @return estimated builder
     */
    public DSSTPropagator getEstimatedPropagator(final double[] state) {
        return getEstimatedBuilder(state).buildPropagator(state);
    }
    /** Get the builder estimated with the values of parameters.
     * @param parameters array containing orbital parameters
     * @return builder based on the values of parameters
     */
    public DSSTPropagatorBuilder getEstimatedBuilder(final double[] parameters) {
        final DSSTPropagatorBuilder copy = builder.copy();
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
        final DSSTPropagatorBuilder copy = builder.copy();
        int j = 0;
        for (DelegatingDriver orbitalDriver : copy.getOrbitalParametersDrivers().getDrivers()) {
            if (orbitalDriver.isSelected()) {
                orbitalDriver.setValue(parameters[j]);
                orbitalDriver.setReferenceValue(parameters[j]);
                parameters[j++] = 0;
            }
        }

        final SpacecraftState spacecraftState = copy.buildPropagator(parameters).getInitialState();
        return spacecraftState;
    }

    public List<DSSTPropagatorBuilder> getEstimatedBuilders(final RealVector[] sigmaPoints) {
        /** Propagators */
        final List<DSSTPropagatorBuilder> builders = new ArrayList<DSSTPropagatorBuilder>();
        for (int i = 0; i < sigmaPoints.length; i++) {
            final double[] currentPoint = sigmaPoints[i].copy().toArray();
            builders.add(getEstimatedBuilder(currentPoint));
        }
        return builders;
    }

    public List<Propagator> getEstimatedPropagators() {
        /** Propagators */
        final List<Propagator> estimatedPropagators = new ArrayList<>();
        final double[] currentPoint = new double[estimatedOrbitalParameters.getNbParams()];
        for (int i = 0; i < currentPoint.length; i++) {
            currentPoint[i] = 0;
        }
        for (int i = 0; i < propagatorBuilders.size(); i++) {
            estimatedPropagators.add(propagatorBuilders.get(i).buildPropagator(currentPoint));
        }
        return estimatedPropagators;
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

    /** Update the DSST short periodic terms.
     * @param state current mean state
     * @param dsstBuilder builder associated to the state
     */
    public void updateShortPeriods(final SpacecraftState state, final DSSTPropagatorBuilder dsstBuilder) {
        // Loop on DSST force models
        for (final DSSTForceModel model : dsstBuilder.getAllForceModels()) {
            model.updateShortPeriodTerms(model.getParameters(), state);
        }

    }
    public void initializeMeanSpacecraftStates(final RealVector[] sigmaPoints) {
        nominalMeanSpacecraftStates = new ArrayList<>();
        previousNominalMeanSpacecraftStates = new ArrayList<>();
        for (int i = 0; i < propagators.size(); i++) {
            nominalMeanSpacecraftStates.add(getEstimatedSpacecraftState(sigmaPoints[i].toArray()));
            previousNominalMeanSpacecraftStates.add(nominalMeanSpacecraftStates.get(i));
        }
    }
    /** Initialize the short periodic terms for the Kalman Filter.
     * @param meanState mean state for auxiliary elements
     * @param propagator propagator associated to the state
     */
    public void initializeShortPeriodicTerms(final SpacecraftState meanState, final Propagator propagator) {

        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        for (final DSSTForceModel force :  ((DSSTPropagator) propagator).getAllForceModels()) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(new AuxiliaryElements(meanState.getOrbit(), 1), PropagationType.OSCULATING, force.getParameters()));
        }
        ((DSSTPropagator) propagator).setShortPeriodTerms(shortPeriodTerms);
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


    /** Compute the predicted osculating elements.
     * @return the predicted osculating element
     */
    private RealVector[] computeOsculatingElementsStates() {

        // Number of estimated orbital parameters
        final RealVector[] osculatingElementsStates = new RealVector[nominalMeanSpacecraftStates.size()];
        final int nbOrb = getNumberSelectedOrbitalDrivers();
        for (int i = 0; i < nominalMeanSpacecraftStates.size(); i++) {
            final double[] shortPeriodTerms = ((DSSTPropagator) propagators.get(i)).getShortPeriodTermsValue(nominalMeanSpacecraftStates.get(i));
            // Nominal mean elements
            final double[] nominalMeanElements = new double[nbOrb];
            OrbitType.EQUINOCTIAL.mapOrbitToArray(nominalMeanSpacecraftStates.get(i).getOrbit(), propagatorBuilders.get(i).getPositionAngle(), nominalMeanElements, null);
         // Ref [1] Eq. 3.6
            final double[] osculatingElements = new double[nbOrb];
            for (int j = 0; j < nbOrb; j++) {
                osculatingElements[j] = nominalMeanElements[j] +
                                        shortPeriodTerms[j];
            }
            osculatingElementsStates[i] = new ArrayRealVector(osculatingElements);
        }

        return osculatingElementsStates;

    }
    /** Convert the nominal mean spacecraft state into array of RealVector.
     * @return mean elements 
     */
    private RealVector[] computeMeanElementsStates() {
        // Number of estimated orbital parameters
        final RealVector[] meanElementsStates = new RealVector[nominalMeanSpacecraftStates.size()];
        final int nbOrb = getNumberSelectedOrbitalDrivers();
        for (int i = 0; i < nominalMeanSpacecraftStates.size(); i++) {
            // Nominal mean elements
            final double[] nominalMeanElements = new double[nbOrb];
            OrbitType.EQUINOCTIAL.mapOrbitToArray(nominalMeanSpacecraftStates.get(i).getOrbit(), propagatorBuilders.get(i).getPositionAngle(), nominalMeanElements, null);
            meanElementsStates[i] = new ArrayRealVector(nominalMeanElements);

        }
        return meanElementsStates;

    }

    /** Compute the predicted osculating elements.
     * @param s Spacecraft state whose osculating elements are supposed to be computed
     * @return the predicted osculating element
     */
    protected double[] computeOsculatingElements(final SpacecraftState s) {

        // Number of estimated orbital parameters
        final int nbOrb = getNumberSelectedOrbitalDrivers();
        final double[] shortPeriodTerms = ((DSSTPropagator) propagators.get(0)).getShortPeriodTermsValue(s);
        // Nominal mean elements
        final double[] nominalMeanElements = new double[nbOrb];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(s.getOrbit(), propagatorBuilders.get(0).getPositionAngle(), nominalMeanElements, null);
        // Ref [1] Eq. 3.6
        final double[] osculatingElements = new double[nbOrb];
        for (int j = 0; j < nbOrb; j++) {
            osculatingElements[j] = nominalMeanElements[j] +
                                        shortPeriodTerms[j];
        }
        return osculatingElements;

    }
    /** Compute the predicted osculating elements.
     * @param s Spacecraft state whose osculating elements are supposed to be computed
     * @return the predicted osculating element
     */
    protected double[] computeOsculatingElements(final SpacecraftState s, final int index) {

        // Number of estimated orbital parameters
        final int nbOrb = getNumberSelectedOrbitalDrivers();
        final double[] shortPeriodTerms = ((DSSTPropagator) propagators.get(index)).getShortPeriodTermsValue(s);
        // Nominal mean elements
        final double[] nominalMeanElements = new double[nbOrb];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(s.getOrbit(), propagatorBuilders.get(index).getPositionAngle(), nominalMeanElements, null);
        // Ref [1] Eq. 3.6
        final double[] osculatingElements = new double[nbOrb];
        for (int j = 0; j < nbOrb; j++) {
            osculatingElements[j] = nominalMeanElements[j] +
                                        shortPeriodTerms[j];
        }
        return osculatingElements;

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

    /**
     * Update the nominal Spacecraft state using interpolated state.
     * @param interpolatedState the interpolated state
     * @param propagatorBuilder builder associated to the state
     * @param index index of the sigma point associated to the state
     */
    public void updateNominalSpacecraftState(final SpacecraftState interpolatedState, final DSSTPropagatorBuilder propagatorBuilder, final int index) {
        previousNominalMeanSpacecraftStates.set(index, nominalMeanSpacecraftStates.get(index));
        nominalMeanSpacecraftStates.set(index, interpolatedState);
        propagatorBuilder.resetOrbit(interpolatedState.getOrbit(), PropagationType.MEAN);
    }

    public void finalizeOperationsObservationGrid(final RealVector[] sigmaPoints) {
        for (int i = 0; i < sigmaPoints.length; i++) {
            int j = 0;
            for (final DelegatingDriver driver : propagatorBuilders.get(i).getOrbitalParametersDrivers().getDrivers()) {
                if (driver.isSelected()) {
                    driver.setValue(sigmaPoints[i].getEntry(j++));
                }
            }
            for (final DelegatingDriver driver : propagatorBuilders.get(i).getPropagationParametersDrivers().getDrivers()) {
                if (driver.isSelected()) {
                    driver.setValue(sigmaPoints[i].getEntry(j++));
                }
            }
        }

    }
}
