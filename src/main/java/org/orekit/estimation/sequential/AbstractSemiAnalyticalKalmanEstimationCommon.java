/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeSpanMap.Span;

/** Class defining the common process model dynamics for semi-analytical Kalman filters.
 * <p>
 * This class handles the multi-satellite state vector management, column mapping,
 * and covariance assembly for semi-analytical (DSST-based) Kalman filters.
 * </p>
 * @author Bryan Cazabonne
 * @since 14.0
 */
abstract class AbstractSemiAnalyticalKalmanEstimationCommon implements KalmanEstimation {

    /** Builders for DSST propagators. */
    private final List<DSSTPropagatorBuilder> builders;

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

    /** Current corrected estimate. */
    private ProcessEstimate correctedEstimate;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Predicted spacecraft states. */
    private final SpacecraftState[] predictedSpacecraftStates;

    /** Corrected spacecraft states. */
    private final SpacecraftState[] correctedSpacecraftStates;

    /** Predicted measurement. */
    protected EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    protected EstimatedMeasurement<?> correctedMeasurement;

    /** Kalman process model constructor.
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    protected AbstractSemiAnalyticalKalmanEstimationCommon(final List<DSSTPropagatorBuilder> propagatorBuilders,
                                                           final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                                           final ParameterDriversList estimatedMeasurementParameters,
                                                           final CovarianceMatrixProvider measurementProcessNoiseMatrix) {

        this.builders                        = propagatorBuilders;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.referenceDate                   = propagatorBuilders.getFirst().getInitialOrbitDate();
        this.currentDate                     = referenceDate;

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
                    // For DSST, we iterate over the name spans
                    for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                        final String driverName = span.getData();
                        if (!estimatedPropagationParametersNames.contains(driverName)) {
                            estimatedPropagationParametersNames.add(driverName);
                        }
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
            for (Span<String> span = parameter.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                measurementParameterColumns.put(span.getData(), columns);
                ++columns;
            }
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
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    final Integer c = propagationParameterColumns.get(span.getData());
                    if (c != null) {
                        covarianceIndirection[k][i++] = c;
                    }
                }
            }
            for (final ParameterDriver driver : estimatedMeasurementParameters.getDrivers()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    final Integer c = measurementParameterColumns.get(span.getData());
                    if (c != null) {
                        covarianceIndirection[k][i++] = c;
                    }
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
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                scale[index++] = driver.getScale();
            }
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                scale[index++] = driver.getScale();
            }
        }

        // Populate predicted and corrected states
        this.predictedSpacecraftStates = new SpacecraftState[builders.size()];
        for (int i = 0; i < builders.size(); ++i) {
            predictedSpacecraftStates[i] = builders.get(i).buildPropagator().getInitialState();
        }
        this.correctedSpacecraftStates = predictedSpacecraftStates.clone();

        // Initialize the estimated normalized state and fill its values
        final RealVector correctedState = MatrixUtils.createRealVector(columns);

        int p = 0;
        for (final ParameterDriver driver : allEstimatedOrbitalParameters.getDrivers()) {
            correctedState.setEntry(p++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : allEstimatedPropagationParameters.getDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                correctedState.setEntry(p++, driver.getNormalizedValue(span.getStart()));
            }
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                correctedState.setEntry(p++, driver.getNormalizedValue(span.getStart()));
            }
        }

        // Set up initial covariance
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(columns, columns);
        for (int k = 0; k < covarianceMatricesProviders.size(); ++k) {

            // Number of estimated measurement parameters
            final int nbMeas = getNumberSelectedMeasurementDriversValuesToEstimate();

            // Number of estimated dynamic parameters (orbital + propagation)
            final int nbDyn  = orbitsEndColumns[k] - orbitsStartColumns[k] +
                    getNumberSelectedPropagationDriversValuesToEstimate(k);

            // Covariance matrix
            final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
            if (nbDyn > 0) {
                final RealMatrix noiseP = covarianceMatricesProviders.get(k).
                        getInitialCovarianceMatrix(correctedSpacecraftStates[k]);
                if (measurementProcessNoiseMatrix == null && noiseP.getRowDimension() != nbDyn + nbMeas) {
                    throw new OrekitException(OrekitMessages.WRONG_PROCESS_COVARIANCE_DIMENSION,
                            nbDyn + nbMeas, noiseP.getRowDimension());
                } else if (measurementProcessNoiseMatrix != null && noiseP.getRowDimension() != nbDyn) {
                    throw new OrekitException(OrekitMessages.WRONG_PROCESS_COVARIANCE_DIMENSION,
                            nbDyn, noiseP.getRowDimension());
                }
                noiseK.setSubMatrix(noiseP.getData(), 0, 0);
            }
            if (measurementProcessNoiseMatrix != null) {
                final RealMatrix noiseM = measurementProcessNoiseMatrix.
                        getInitialCovarianceMatrix(correctedSpacecraftStates[k]);
                if (noiseM.getRowDimension() != nbMeas) {
                    throw new OrekitException(OrekitMessages.WRONG_MEASUREMENT_COVARIANCE_DIMENSION,
                            nbMeas, noiseM.getRowDimension());
                }
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

        correctedEstimate = new ProcessEstimate(0.0, correctedState, correctedCovariance);

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalStateTransitionMatrix() {
        return correctedEstimate.getStateTransitionMatrix() == null ?
                null : KalmanEstimatorUtil.unnormalizeStateTransitionMatrix(correctedEstimate.getStateTransitionMatrix(), scale);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalMeasurementJacobian() {
        return correctedEstimate.getMeasurementJacobian() == null ?
                null : KalmanEstimatorUtil.unnormalizeMeasurementJacobian(correctedEstimate.getMeasurementJacobian(),
                scale,
                correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalInnovationCovarianceMatrix() {
        return correctedEstimate.getInnovationCovariance() == null ?
                null : KalmanEstimatorUtil.unnormalizeInnovationCovarianceMatrix(correctedEstimate.getInnovationCovariance(),
                predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalKalmanGain() {
        return correctedEstimate.getKalmanGain() == null ?
                null : KalmanEstimatorUtil.unnormalizeKalmanGainMatrix(correctedEstimate.getKalmanGain(),
                scale,
                correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
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
    public RealVector getPhysicalEstimatedState() {
        final RealVector physicalEstimatedState = new ArrayRealVector(scale.length);
        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                physicalEstimatedState.setEntry(i++, span.getData());
            }
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                physicalEstimatedState.setEntry(i++, span.getData());
            }
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                physicalEstimatedState.setEntry(i++, span.getData());
            }
        }

        return physicalEstimatedState;
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        return KalmanEstimatorUtil.unnormalizeCovarianceMatrix(correctedEstimate.getCovariance(), scale);
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

    /** Get the current corrected estimate.
     * @return current corrected estimate
     */
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** Getter for the propagators.
     * @return the propagators
     */
    public List<DSSTPropagatorBuilder> getBuilders() {
        return builders;
    }

    /** Get the propagators estimated with the values set in the propagators builders.
     * @return propagators based on the current values in the builder
     */
    public DSSTPropagator[] getEstimatedPropagators() {
        final DSSTPropagator[] propagators = new DSSTPropagator[getBuilders().size()];
        for (int k = 0; k < getBuilders().size(); ++k) {
            propagators[k] = (DSSTPropagator) getBuilders().get(k).buildPropagator();
        }
        return propagators;
    }

    /** Get the normalized process noise matrix.
     *
     * @param stateDimension state dimension
     * @return the normalized process noise matrix
     */
    protected RealMatrix getNormalizedProcessNoise(final int stateDimension) {
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(stateDimension, stateDimension);
        for (int k = 0; k < covarianceMatricesProviders.size(); ++k) {

            // Number of estimated measurement parameters
            final int nbMeas = getNumberSelectedMeasurementDriversValuesToEstimate();

            // Number of estimated dynamic parameters (orbital + propagation)
            final int nbDyn  = orbitsEndColumns[k] - orbitsStartColumns[k] +
                    getNumberSelectedPropagationDriversValuesToEstimate(k);

            // Covariance matrix
            final RealMatrix noiseK = MatrixUtils.createRealMatrix(nbDyn + nbMeas, nbDyn + nbMeas);
            if (nbDyn > 0) {
                final RealMatrix noiseP = covarianceMatricesProviders.get(k).
                        getProcessNoiseMatrix(correctedSpacecraftStates[k],
                                predictedSpacecraftStates[k]);
                if (measurementProcessNoiseMatrix == null && noiseP.getRowDimension() != nbDyn + nbMeas) {
                    throw new OrekitException(OrekitMessages.WRONG_PROCESS_COVARIANCE_DIMENSION,
                            nbDyn + nbMeas, noiseP.getRowDimension());
                } else if (measurementProcessNoiseMatrix != null && noiseP.getRowDimension() != nbDyn) {
                    throw new OrekitException(OrekitMessages.WRONG_PROCESS_COVARIANCE_DIMENSION,
                            nbDyn, noiseP.getRowDimension());
                }
                noiseK.setSubMatrix(noiseP.getData(), 0, 0);
            }
            if (measurementProcessNoiseMatrix != null) {
                final RealMatrix noiseM = measurementProcessNoiseMatrix.
                        getProcessNoiseMatrix(correctedSpacecraftStates[k],
                                predictedSpacecraftStates[k]);
                if (noiseM.getRowDimension() != nbMeas) {
                    throw new OrekitException(OrekitMessages.WRONG_MEASUREMENT_COVARIANCE_DIMENSION,
                            nbMeas, noiseM.getRowDimension());
                }
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
        return KalmanEstimatorUtil.normalizeCovarianceMatrix(physicalProcessNoise, scale);
    }

    /** Getter for the orbitsStartColumns.
     * @return the orbitsStartColumns
     */
    protected int[] getOrbitsStartColumns() {
        return orbitsStartColumns;
    }

    /** Getter for the orbitsEndColumns.
     * @return the orbitsEndColumns
     */
    protected int[] getOrbitsEndColumns() {
        return orbitsEndColumns;
    }

    /** Getter for the propagationParameterColumns.
     * @return the propagationParameterColumns
     */
    protected Map<String, Integer> getPropagationParameterColumns() {
        return propagationParameterColumns;
    }

    /** Getter for the measurementParameterColumns.
     * @return the measurementParameterColumns
     */
    protected Map<String, Integer> getMeasurementParameterColumns() {
        return measurementParameterColumns;
    }

    /** Getter for the estimatedPropagationParameters.
     * @return the estimatedPropagationParameters
     */
    protected ParameterDriversList[] getEstimatedPropagationParametersArray() {
        return estimatedPropagationParameters;
    }

    /** Getter for the estimatedOrbitalParameters.
     * @return the estimatedOrbitalParameters
     */
    protected ParameterDriversList[] getEstimatedOrbitalParametersArray() {
        return estimatedOrbitalParameters;
    }

    /** Getter for the covarianceIndirection.
     * @return the covarianceIndirection
     */
    protected int[][] getCovarianceIndirection() {
        return covarianceIndirection;
    }

    /** Getter for the scale.
     * @return the scale
     */
    protected double[] getScale() {
        return scale;
    }

    /** Getter for the correctedEstimate.
     * @return the correctedEstimate
     */
    protected ProcessEstimate getCorrectedEstimate() {
        return correctedEstimate;
    }

    /** Setter for the correctedEstimate.
     * @param correctedEstimate the correctedEstimate
     */
    protected void setCorrectedEstimate(final ProcessEstimate correctedEstimate) {
        this.correctedEstimate = correctedEstimate;
    }

    /** Getter for the referenceDate.
     * @return the referenceDate
     */
    protected AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** Increment current measurement number. */
    protected void incrementCurrentMeasurementNumber() {
        currentMeasurementNumber += 1;
    }

    /** Setter for the currentDate.
     * @param currentDate the currentDate
     */
    protected void setCurrentDate(final AbsoluteDate currentDate) {
        this.currentDate = currentDate;
    }

    /** Set correctedSpacecraftState at index.
     *
     * @param correctedSpacecraftState corrected S/C state to set
     * @param index index where to set in the array
     */
    protected void setCorrectedSpacecraftState(final SpacecraftState correctedSpacecraftState, final int index) {
        this.correctedSpacecraftStates[index] = correctedSpacecraftState;
    }

    /** Set predictedSpacecraftState at index.
     *
     * @param predictedSpacecraftState predicted S/C state to set
     * @param index index where to set in the array
     */
    protected void setPredictedSpacecraftState(final SpacecraftState predictedSpacecraftState, final int index) {
        this.predictedSpacecraftStates[index] = predictedSpacecraftState;
    }

    /** Setter for the predictedMeasurement.
     * @param predictedMeasurement the predictedMeasurement
     */
    protected void setPredictedMeasurement(final EstimatedMeasurement<?> predictedMeasurement) {
        this.predictedMeasurement = predictedMeasurement;
    }

    /** Setter for the correctedMeasurement.
     * @param correctedMeasurement the correctedMeasurement
     */
    protected void setCorrectedMeasurement(final EstimatedMeasurement<?> correctedMeasurement) {
        this.correctedMeasurement = correctedMeasurement;
    }

    /** Get the number of estimated orbital parameters values for a given satellite.
     * @param satelliteIndex satellite index
     * @return the number of estimated values for orbital parameters
     */
    protected int getNumberSelectedOrbitalDriversValuesToEstimate(final int satelliteIndex) {
        int nbOrbitalValuesToEstimate = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters[satelliteIndex].getDrivers()) {
            nbOrbitalValuesToEstimate += driver.getNbOfValues();
        }
        return nbOrbitalValuesToEstimate;
    }

    /** Get the number of estimated propagation parameters values for a given satellite.
     * @param satelliteIndex satellite index
     * @return the number of estimated values for propagation parameters
     */
    protected int getNumberSelectedPropagationDriversValuesToEstimate(final int satelliteIndex) {
        int nbPropagationValuesToEstimate = 0;
        for (final ParameterDriver driver : estimatedPropagationParameters[satelliteIndex].getDrivers()) {
            nbPropagationValuesToEstimate += driver.getNbOfValues();
        }
        return nbPropagationValuesToEstimate;
    }

    /** Get the number of estimated measurement parameters values.
     * @return the number of estimated values for measurement parameters
     */
    protected int getNumberSelectedMeasurementDriversValuesToEstimate() {
        int nbMeasurementValuesToEstimate = 0;
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            nbMeasurementValuesToEstimate += driver.getNbOfValues();
        }
        return nbMeasurementValuesToEstimate;
    }

    /** Get the number of estimated orbital parameters.
     * @return the number of estimated orbital parameters
     */
    protected int getNumberSelectedOrbitalDrivers() {
        return allEstimatedOrbitalParameters.getNbParams();
    }

    /** Get the number of estimated propagation parameters.
     * @return the number of estimated propagation parameters
     */
    protected int getNumberSelectedPropagationDrivers() {
        return allEstimatedPropagationParameters.getNbParams();
    }

    /** Get the number of estimated measurement parameters.
     * @return the number of estimated measurement parameters
     */
    protected int getNumberSelectedMeasurementDrivers() {
        return estimatedMeasurementsParameters.getNbParams();
    }

    /** Update the estimated parameters after the correction phase of the filter.
     * The min/max allowed values are handled by the parameter themselves.
     */
    protected void updateParameters() {
        final RealVector correctedState = correctedEstimate.getState();
        int i = 0;
        // Orbital parameters
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                driver.setNormalizedValue(driver.getNormalizedValue(span.getStart()) + correctedState.getEntry(i++), span.getStart());
            }
        }

        // Propagation parameters
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                driver.setNormalizedValue(driver.getNormalizedValue(span.getStart()) + correctedState.getEntry(i++), span.getStart());
            }
        }

        // Measurements parameters
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                driver.setNormalizedValue(driver.getNormalizedValue(span.getStart()) + correctedState.getEntry(i++), span.getStart());
            }
        }
    }

}
