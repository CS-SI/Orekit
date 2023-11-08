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
import org.hipparchus.filtering.kalman.extended.NonLinearEvolution;
import org.hipparchus.filtering.kalman.extended.NonLinearProcess;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanModel implements KalmanEstimation, NonLinearProcess<MeasurementDecorator> {

    /** Builders for propagators. */
    private final List<PropagatorBuilder> builders;

    /** Estimated orbital parameters. */
    private final ParameterDriversList allEstimatedOrbitalParameters;

    /** Estimated propagation drivers. */
    private final ParameterDriversList allEstimatedPropagationParameters;

    /** Per-builder estimated orbita parameters drivers.
     * @since 11.1
     */
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

    /** Harvesters for extracting Jacobians from integrated states. */
    private MatricesHarvester[] harvesters;

    /** Propagators for the reference trajectories, up to current date. */
    private Propagator[] referenceTrajectories;

    /** Current corrected estimate. */
    private ProcessEstimate correctedEstimate;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Predicted spacecraft states. */
    private SpacecraftState[] predictedSpacecraftStates;

    /** Corrected spacecraft states. */
    private SpacecraftState[] correctedSpacecraftStates;

    /** Predicted measurement. */
    private EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

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

        this.builders                        = propagatorBuilders;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.referenceDate                   = propagatorBuilders.get(0).getInitialOrbitDate();
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
                    covarianceIndirection[k][i++] = c.intValue();
                }
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

        // Build the reference propagators and add their partial derivatives equations implementation
        updateReferenceTrajectories(getEstimatedPropagators());
        this.predictedSpacecraftStates = new SpacecraftState[referenceTrajectories.length];
        for (int i = 0; i < predictedSpacecraftStates.length; ++i) {
            predictedSpacecraftStates[i] = referenceTrajectories[i].getInitialState();
        };
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

        correctedEstimate = new ProcessEstimate(0.0, correctedState, correctedCovariance);

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


    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalStateTransitionMatrix() {
        //  Un-normalize the state transition matrix (φ) from Hipparchus and return it.
        // φ is an mxm matrix where m = nbOrb + nbPropag + nbMeas
        // For each element [i,j] of normalized φ (φn), the corresponding physical value is:
        // φ[i,j] = φn[i,j] * scale[i] / scale[j]
        return correctedEstimate.getStateTransitionMatrix() == null ?
                null : KalmanEstimatorUtil.unnormalizeStateTransitionMatrix(correctedEstimate.getStateTransitionMatrix(), scale);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalMeasurementJacobian() {
        // Un-normalize the measurement matrix (H) from Hipparchus and return it.
        // H is an nxm matrix where:
        //  - m = nbOrb + nbPropag + nbMeas is the number of estimated parameters
        //  - n is the size of the measurement being processed by the filter
        // For each element [i,j] of normalized H (Hn) the corresponding physical value is:
        // H[i,j] = Hn[i,j] * σ[i] / scale[j]
        return correctedEstimate.getMeasurementJacobian() == null ?
                null : KalmanEstimatorUtil.unnormalizeMeasurementJacobian(correctedEstimate.getMeasurementJacobian(),
                                                                          scale,
                                                                          correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
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
        // Un-normalize the estimated covariance matrix (P) from Hipparchus and return it.
        // The covariance P is an mxm matrix where m = nbOrb + nbPropag + nbMeas
        // For each element [i,j] of P the corresponding normalized value is:
        // Pn[i,j] = P[i,j] / (scale[i]*scale[j])
        // Consequently: P[i,j] = Pn[i,j] * scale[i] * scale[j]
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
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(correctedEstimate.getState().getDimension());

        // loop over all orbits
        for (int k = 0; k < predictedSpacecraftStates.length; ++k) {

            // Indexes
            final int[] indK = covarianceIndirection[k];

            // Derivatives of the state vector with respect to initial state vector
            final int nbOrbParams = estimatedOrbitalParameters[k].getNbParams();
            if (nbOrbParams > 0) {

                // Reset reference (for example compute short periodic terms in DSST)
                harvesters[k].setReferenceState(predictedSpacecraftStates[k]);

                final RealMatrix dYdY0 = harvesters[k].getStateTransitionMatrix(predictedSpacecraftStates[k]);

                // Fill upper left corner (dY/dY0)
                for (int i = 0; i < dYdY0.getRowDimension(); ++i) {
                    for (int j = 0; j < nbOrbParams; ++j) {
                        stm.setEntry(indK[i], indK[j], dYdY0.getEntry(i, j));
                    }
                }
            }

            // Derivatives of the state vector with respect to propagation parameters
            final int nbParams = estimatedPropagationParameters[k].getNbParams();
            if (nbParams > 0) {
                final RealMatrix dYdPp = harvesters[k].getParametersJacobian(predictedSpacecraftStates[k]);

                // Fill 1st row, 2nd column (dY/dPp)
                for (int i = 0; i < dYdPp.getRowDimension(); ++i) {
                    for (int j = 0; j < nbParams; ++j) {
                        stm.setEntry(indK[i], indK[j + 6], dYdPp.getEntry(i, j));
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
        final SpacecraftState[]      evaluationStates    = predictedMeasurement.getStates();
        final ObservedMeasurement<?> observedMeasurement = predictedMeasurement.getObservedMeasurement();
        final double[] sigma  = observedMeasurement.getTheoreticalStandardDeviation();

        // Initialize measurement matrix H: nxm
        // n: Number of measurements in current measurement
        // m: State vector size
        final RealMatrix measurementMatrix = MatrixUtils.
                        createRealMatrix(observedMeasurement.getDimension(),
                                         correctedEstimate.getState().getDimension());

        // loop over all orbits involved in the measurement
        for (int k = 0; k < evaluationStates.length; ++k) {
            final int p = observedMeasurement.getSatellites().get(k).getPropagatorIndex();

            // Predicted orbit
            final Orbit predictedOrbit = evaluationStates[k].getOrbit();

            // Measurement matrix's columns related to orbital parameters
            // ----------------------------------------------------------

            // Partial derivatives of the current Cartesian coordinates with respect to current orbital state
            final double[][] aCY = new double[6][6];
            predictedOrbit.getJacobianWrtParameters(builders.get(p).getPositionAngleType(), aCY);   //dC/dY
            final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

            // Jacobian of the measurement with respect to current Cartesian coordinates
            final RealMatrix dMdC = new Array2DRowRealMatrix(predictedMeasurement.getStateDerivatives(k), false);

            // Jacobian of the measurement with respect to current orbital state
            final RealMatrix dMdY = dMdC.multiply(dCdY);

            // Fill the normalized measurement matrix's columns related to estimated orbital parameters
            for (int i = 0; i < dMdY.getRowDimension(); ++i) {
                int jOrb = orbitsStartColumns[p];
                for (int j = 0; j < dMdY.getColumnDimension(); ++j) {
                    final ParameterDriver driver = builders.get(p).getOrbitalParametersDrivers().getDrivers().get(j);
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
                driver.setReferenceDate(builders.get(0).getInitialOrbitDate());
            }
        }

        ++currentMeasurementNumber;
        currentDate = measurement.getObservedMeasurement().getDate();

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
        predictedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            KalmanEstimatorUtil.filterRelevant(observedMeasurement, predictedSpacecraftStates));

        // Normalized measurement matrix (nxm)
        final RealMatrix measurementMatrix = getMeasurementMatrix();

        // compute process noise matrix
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(previousState.getDimension(),
                                                                             previousState.getDimension());
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

        return new NonLinearEvolution(measurement.getTime(), predictedState,
                                      stateTransitionMatrix, normalizedProcessNoise, measurementMatrix);

    }


    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final NonLinearEvolution evolution,
                                    final RealMatrix innovationCovarianceMatrix) {

        // Apply the dynamic outlier filter, if it exists
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
        correctedEstimate = estimate;
        updateParameters();

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final Propagator[] estimatedPropagators = getEstimatedPropagators();
        for (int k = 0; k < estimatedPropagators.length; ++k) {
            correctedSpacecraftStates[k] = estimatedPropagators[k].getInitialState();
        }

        // Compute the estimated measurement using estimated spacecraft state
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            KalmanEstimatorUtil.filterRelevant(observedMeasurement, correctedSpacecraftStates));
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
        final RealVector predictedState = correctedEstimate.getState().copy();

        // Orbital parameters counter
        int jOrb = 0;

        for (int k = 0; k < predictedSpacecraftStates.length; ++k) {

            // Propagate the reference trajectory to measurement date
            predictedSpacecraftStates[k] = referenceTrajectories[k].propagate(date);

            // Update the builder with the predicted orbit
            // This updates the orbital drivers with the values of the predicted orbit
            builders.get(k).resetOrbit(predictedSpacecraftStates[k].getOrbit());

            // The orbital parameters in the state vector are replaced with their predicted values
            // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction
            for (DelegatingDriver orbitalDriver : builders.get(k).getOrbitalParametersDrivers().getDrivers()) {
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
        final RealVector correctedState = correctedEstimate.getState();
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

    /** Getter for the propagators.
     * @return the propagators
     */
    public List<PropagatorBuilder> getBuilders() {
        return builders;
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

    /** Get the propagators estimated with the values set in the propagators builders.
     * @return propagators based on the current values in the builder
     */
    public Propagator[] getEstimatedPropagators() {
        // Return propagators built with current instantiation of the propagator builders
        final Propagator[] propagators = new Propagator[getBuilders().size()];
        for (int k = 0; k < getBuilders().size(); ++k) {
            propagators[k] = getBuilders().get(k).buildPropagator(getBuilders().get(k).getSelectedNormalizedParameters());
        }
        return propagators;
    }

}
