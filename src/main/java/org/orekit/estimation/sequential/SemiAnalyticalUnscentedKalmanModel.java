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
import java.util.Comparator;
import java.util.List;

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.unscented.UnscentedEvolution;
import org.hipparchus.filtering.kalman.unscented.UnscentedKalmanFilter;
import org.hipparchus.filtering.kalman.unscented.UnscentedProcess;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
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

/** Class defining the process model dynamics to use with a {@link SemiAnalyticalUnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class SemiAnalyticalUnscentedKalmanModel implements KalmanEstimation, UnscentedProcess<MeasurementDecorator>, SemiAnalyticalProcess {

    /** Initial builder for propagator. */
    private final DSSTPropagatorBuilder builder;

    /** Estimated orbital parameters. */
    private final ParameterDriversList estimatedOrbitalParameters;

    /** Estimated propagation parameters. */
    private final ParameterDriversList estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Provider for covariance matrice. */
    private final CovarianceMatrixProvider covarianceMatrixProvider;

    /** Process noise matrix provider for measurement parameters. */
    private final CovarianceMatrixProvider measurementProcessNoiseMatrix;

    /** Position angle type used during orbit determination. */
    private final PositionAngleType angleType;

    /** Orbit type used during orbit determination. */
    private final OrbitType orbitType;

    /** Current corrected estimate. */
    private ProcessEstimate correctedEstimate;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Nominal mean spacecraft state. */
    private SpacecraftState nominalMeanSpacecraftState;

    /** Previous nominal mean spacecraft state. */
    private SpacecraftState previousNominalMeanSpacecraftState;

    /** Predicted osculating spacecraft state. */
    private SpacecraftState predictedSpacecraftState;

    /** Corrected mean spacecraft state. */
    private SpacecraftState correctedSpacecraftState;

    /** Predicted measurement. */
    private EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

    /** Predicted mean element filter correction. */
    private RealVector predictedFilterCorrection;

    /** Corrected mean element filter correction. */
    private RealVector correctedFilterCorrection;

    /** Propagators for the reference trajectories, up to current date. */
    private DSSTPropagator dsstPropagator;

    /** Short period terms for the nominal mean spacecraft state. */
    private RealVector shortPeriodicTerms;

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
        this.angleType                       = propagatorBuilder.getPositionAngleType();
        this.orbitType                       = propagatorBuilder.getOrbitType();
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.currentMeasurementNumber        = 0;
        this.currentDate                     = propagatorBuilder.getInitialOrbitDate();
        this.covarianceMatrixProvider        = covarianceMatrixProvider;
        this.measurementProcessNoiseMatrix   = measurementProcessNoiseMatrix;

        // Number of estimated parameters
        int columns = 0;

        // Set estimated orbital parameters
        this.estimatedOrbitalParameters = new ParameterDriversList();
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
        this.estimatedPropagationParameters = new ParameterDriversList();
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
                    ++columns;
                }
            }

        }
        estimatedPropagationParametersNames.sort(Comparator.naturalOrder());

        // Set the estimated measurement parameters
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            if (parameter.getReferenceDate() == null) {
                parameter.setReferenceDate(currentDate);
            }
            ++columns;
        }

        // Number of estimated measurement parameters
        final int nbMeas = estimatedMeasurementParameters.getNbParams();

        // Number of estimated dynamic parameters (orbital + propagation)
        final int nbDyn  = estimatedOrbitalParameters.getNbParams() +
                           estimatedPropagationParameters.getNbParams();

        // Build the reference propagator
        this.dsstPropagator = getEstimatedPropagator();
        final SpacecraftState meanState = dsstPropagator.initialIsOsculating() ?
                         DSSTPropagator.computeMeanState(dsstPropagator.getInitialState(), dsstPropagator.getAttitudeProvider(), dsstPropagator.getAllForceModels()) :
                         dsstPropagator.getInitialState();
        this.nominalMeanSpacecraftState         = meanState;
        this.predictedSpacecraftState           = meanState;
        this.correctedSpacecraftState           = predictedSpacecraftState;
        this.previousNominalMeanSpacecraftState = nominalMeanSpacecraftState;

        // Initialize the estimated mean element filter correction
        this.predictedFilterCorrection = MatrixUtils.createRealVector(columns);
        this.correctedFilterCorrection = predictedFilterCorrection;

        // Covariance matrix
        final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
        final RealMatrix noiseP = covarianceMatrixProvider.getInitialCovarianceMatrix(nominalMeanSpacecraftState);
        noiseK.setSubMatrix(noiseP.getData(), 0, 0);
        if (measurementProcessNoiseMatrix != null) {
            final RealMatrix noiseM = measurementProcessNoiseMatrix.getInitialCovarianceMatrix(nominalMeanSpacecraftState);
            noiseK.setSubMatrix(noiseM.getData(), nbDyn, nbDyn);
        }

        KalmanEstimatorUtil.checkDimension(noiseK.getRowDimension(),
                                           propagatorBuilder.getOrbitalParametersDrivers(),
                                           propagatorBuilder.getPropagationParametersDrivers(),
                                           estimatedMeasurementsParameters);

        // Initialize corrected estimate
        this.correctedEstimate = new ProcessEstimate(0.0, correctedFilterCorrection, noiseK);

    }

    /** {@inheritDoc} */
    @Override
    public KalmanObserver getObserver() {
        return observer;
    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Get the current corrected estimate.
     * <p>
     * For the Unscented Semi-analytical Kalman Filter
     * it corresponds to the corrected filter correction.
     * In other words, it doesn't represent an orbital state.
     * </p>
     * @return current corrected estimate
     */
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** Process measurements.
     * @param observedMeasurements the list of measurements to process
     * @param filter Unscented Kalman Filter
     * @return estimated propagator
     */
    public DSSTPropagator processMeasurements(final List<ObservedMeasurement<?>> observedMeasurements,
                                              final UnscentedKalmanFilter<MeasurementDecorator> filter) {

        // Sort the measurement
        observedMeasurements.sort(new ChronologicalComparator());
        final AbsoluteDate tStart             = observedMeasurements.get(0).getDate();
        final AbsoluteDate tEnd               = observedMeasurements.get(observedMeasurements.size() - 1).getDate();
        final double       overshootTimeRange = FastMath.nextAfter(tEnd.durationFrom(tStart),
                                                Double.POSITIVE_INFINITY);

        // Initialize step handler and set it to a parallelized propagator
        final SemiAnalyticalMeasurementHandler  stepHandler = new SemiAnalyticalMeasurementHandler(this, filter, observedMeasurements, builder.getInitialOrbitDate(), true);
        dsstPropagator.getMultiplexer().add(stepHandler);
        dsstPropagator.propagate(tStart, tStart.shiftedBy(overshootTimeRange));

        // Return the last estimated propagator
        return getEstimatedPropagator();

    }

    /** Get the propagator estimated with the values set in the propagator builder.
     * @return propagator based on the current values in the builder
     */
    public DSSTPropagator getEstimatedPropagator() {
        // Return propagator built with current instantiation of the propagator builder
        return builder.buildPropagator(builder.getSelectedNormalizedParameters());
    }

    /** {@inheritDoc} */
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

        // STM for the prediction of the filter correction
        final RealMatrix stm = getStm();

        // Predicted states
        final RealVector[] predictedStates = new RealVector[sigmaPoints.length];
        for (int k = 0; k < sigmaPoints.length; ++k) {
            // Predict filter correction for the current sigma point
            final RealVector predicted = stm.operate(sigmaPoints[k]);
            predictedStates[k] = predicted;
        }

        // Number of estimated measurement parameters
        final int nbMeas = getNumberSelectedMeasurementDrivers();

        // Number of estimated dynamic parameters (orbital + propagation)
        final int nbDyn  = getNumberSelectedOrbitalDrivers() + getNumberSelectedPropagationDrivers();

        // Covariance matrix
        final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
        final RealMatrix noiseP = covarianceMatrixProvider.getProcessNoiseMatrix(previousNominalMeanSpacecraftState, nominalMeanSpacecraftState);
        noiseK.setSubMatrix(noiseP.getData(), 0, 0);
        if (measurementProcessNoiseMatrix != null) {
            final RealMatrix noiseM = measurementProcessNoiseMatrix.getProcessNoiseMatrix(previousNominalMeanSpacecraftState, nominalMeanSpacecraftState);
            noiseK.setSubMatrix(noiseM.getData(), nbDyn, nbDyn);
        }

        // Verify dimension
        KalmanEstimatorUtil.checkDimension(noiseK.getRowDimension(),
                                           builder.getOrbitalParametersDrivers(),
                                           builder.getPropagationParametersDrivers(),
                                           estimatedMeasurementsParameters);

        // Return
        return new UnscentedEvolution(measurement.getTime(), predictedStates, noiseK);

    }

    /** {@inheritDoc} */
    @Override
    public RealVector[] getPredictedMeasurements(final RealVector[] predictedSigmaPoints, final MeasurementDecorator measurement) {

        // Observed measurement
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();

        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedMeasurements = new RealVector[predictedSigmaPoints.length];

        // Loop on sigma points
        for (int k = 0; k < predictedSigmaPoints.length; ++k) {

            // Calculate the predicted osculating elements for the current mean state
            final RealVector osculating = computeOsculatingElements(predictedSigmaPoints[k], nominalMeanSpacecraftState, shortPeriodicTerms);
            final Orbit osculatingOrbit = orbitType.mapArrayToOrbit(osculating.toArray(), null, angleType,
                                                                    currentDate, builder.getMu(), builder.getFrame());

            // Then, estimate the measurement
            final EstimatedMeasurement<?> estimated = observedMeasurement.estimate(currentMeasurementNumber,
                                                                                   currentMeasurementNumber,
                                                                                   new SpacecraftState[] {
                                                                                       new SpacecraftState(osculatingOrbit)
                                                                                   });
            predictedMeasurements[k] = new ArrayRealVector(estimated.getEstimatedValue());

        }

        // Return
        return predictedMeasurements;

    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas,
                                    final RealVector predictedState, final RealMatrix innovationCovarianceMatrix) {

        // Predicted filter correction
        predictedFilterCorrection = predictedState;

        // Predicted measurement
        final RealVector osculating = computeOsculatingElements(predictedFilterCorrection, nominalMeanSpacecraftState, shortPeriodicTerms);
        final Orbit osculatingOrbit = orbitType.mapArrayToOrbit(osculating.toArray(), null, angleType,
                                                                currentDate, builder.getMu(), builder.getFrame());
        predictedSpacecraftState = new SpacecraftState(osculatingOrbit);
        predictedMeasurement = measurement.getObservedMeasurement().estimate(currentMeasurementNumber, currentMeasurementNumber, getPredictedSpacecraftStates());
        predictedMeasurement.setEstimatedValue(predictedMeas.toArray());

        // Apply the dynamic outlier filter, if it exists
        KalmanEstimatorUtil.applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);

        // Compute the innovation vector (not normalized for unscented Kalman filter)
        return KalmanEstimatorUtil.computeInnovationVector(predictedMeasurement);

    }


    /** {@inheritDoc} */
    @Override
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the process estimate
        correctedEstimate = estimate;
        // Corrected filter correction
        correctedFilterCorrection = estimate.getState();

        // Update the previous nominal mean spacecraft state
        previousNominalMeanSpacecraftState = nominalMeanSpacecraftState;

        // Update the previous nominal mean spacecraft state
        // Calculate the corrected osculating elements
        final RealVector osculating = computeOsculatingElements(correctedFilterCorrection, nominalMeanSpacecraftState, shortPeriodicTerms);
        final Orbit osculatingOrbit = orbitType.mapArrayToOrbit(osculating.toArray(), null, builder.getPositionAngleType(),
                                                                currentDate, builder.getMu(), builder.getFrame());

        // Compute the corrected measurements
        correctedSpacecraftState = new SpacecraftState(osculatingOrbit);
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            getCorrectedSpacecraftStates());
        // Call the observer if the user add one
        if (observer != null) {
            observer.evaluationPerformed(this);
        }
    }

    /** Get the state transition matrix used to predict the filter correction.
     * <p>
     * The state transition matrix is not computed by the DSST propagator.
     * It is analytically calculated considering Keplerian contribution only
     * </p>
     * @return the state transition matrix used to predict the filter correction
     */
    private RealMatrix getStm() {

        // initialize the STM
        final int nbDym  = getNumberSelectedOrbitalDrivers() + getNumberSelectedPropagationDrivers();
        final int nbMeas = getNumberSelectedMeasurementDrivers();
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(nbDym + nbMeas);

        // State transition matrix using Keplerian contribution only
        final double mu  = builder.getMu();
        final double sma = previousNominalMeanSpacecraftState.getA();
        final double dt  = currentDate.durationFrom(previousNominalMeanSpacecraftState.getDate());
        final double contribution = -1.5 * dt * FastMath.sqrt(mu / FastMath.pow(sma, 5));
        stm.setEntry(5, 0, contribution);

        // Return
        return stm;

    }

    /** {@inheritDoc} */
    @Override
    public void finalizeOperationsObservationGrid() {
        // Update parameters
        updateParameters();
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

    /** {@inheritDoc}
     * <p>
     * Predicted state is osculating.
     * </p>
     */
    @Override
    public SpacecraftState[] getPredictedSpacecraftStates() {
        return new SpacecraftState[] {predictedSpacecraftState};
    }

    /** {@inheritDoc}
     * <p>
     * Corrected state is osculating.
     * </p>
     */
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

    /** {@inheritDoc} */
    @Override
    public void updateNominalSpacecraftState(final SpacecraftState nominal) {
        this.nominalMeanSpacecraftState = nominal;
        // Short period terms
        shortPeriodicTerms = new ArrayRealVector(dsstPropagator.getShortPeriodTermsValue(nominalMeanSpacecraftState));
        // Update the builder with the nominal mean elements orbit
        builder.resetOrbit(nominal.getOrbit(), PropagationType.MEAN);
    }

    /** {@inheritDoc} */
    @Override
    public void updateShortPeriods(final SpacecraftState state) {
        // Loop on DSST force models
        for (final DSSTForceModel model : dsstPropagator.getAllForceModels()) {
            model.updateShortPeriodTerms(model.getParameters(), state);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initializeShortPeriodicTerms(final SpacecraftState meanState) {
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        for (final DSSTForceModel force :  builder.getAllForceModels()) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(new AuxiliaryElements(meanState.getOrbit(), 1), PropagationType.OSCULATING, force.getParameters()));
        }
        dsstPropagator.setShortPeriodTerms(shortPeriodTerms);
    }

    /** Compute the predicted osculating elements.
     * @param filterCorrection physical kalman filter correction
     * @param meanState mean spacecraft state
     * @param shortPeriodTerms short period terms for the given mean state
     * @return the predicted osculating element
     */
    private RealVector computeOsculatingElements(final RealVector filterCorrection,
                                                 final SpacecraftState meanState,
                                                 final RealVector shortPeriodTerms) {

        // Convert the input predicted mean state to a SpacecraftState
        final RealVector stateVector = toRealVector(meanState);

        // Return
        return stateVector.add(filterCorrection).add(shortPeriodTerms);

    }

    /** Convert a SpacecraftState to a RealVector.
     * @param state the input SpacecraftState
     * @return the corresponding RealVector
     */
    private RealVector toRealVector(final SpacecraftState state) {

        // Convert orbit to array
        final double[] stateArray = new double[6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, stateArray, null);

        // Return the RealVector
        return new ArrayRealVector(stateArray);

    }

    /** Get the number of estimated orbital parameters.
     * @return the number of estimated orbital parameters
     */
    public int getNumberSelectedOrbitalDrivers() {
        return estimatedOrbitalParameters.getNbParams();
    }

    /** Get the number of estimated propagation parameters.
     * @return the number of estimated propagation parameters
     */
    public int getNumberSelectedPropagationDrivers() {
        return estimatedPropagationParameters.getNbParams();
    }

    /** Get the number of estimated measurement parameters.
     * @return the number of estimated measurement parameters
     */
    public int getNumberSelectedMeasurementDrivers() {
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
            driver.setValue(driver.getValue() + correctedState.getEntry(i++));
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setValue(driver.getValue() + correctedState.getEntry(i++));
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setValue(driver.getValue() + correctedState.getEntry(i++));
        }
    }

}
