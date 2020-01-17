/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.IntegratedPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * <p>
 * This class is an adaption of the {@link KalmanModel} class
 * but for the {@link DSSTPropagator DSST propagator}.
 * </p>
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Bryan Cazabonne
 * @since 10.0
 */
public class DSSTKalmanModel implements KalmanODModel {

    /** Builders for propagators. */
    private final List<IntegratedPropagatorBuilder> builders;

    /** Estimated orbital parameters. */
    private final ParameterDriversList allEstimatedOrbitalParameters;

    /** Estimated propagation drivers. */
    private final ParameterDriversList allEstimatedPropagationParameters;

    /** Per-builder estimated propagation drivers. */
    private final ParameterDriversList[] estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Start columns for each estimated orbit. */
    private final int[] orbitsStartColumns;

    /** End columns for each estimated orbit. */
    private final int[] orbitsEndColumns;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> measurementParameterColumns;

    /** Providers for covariance matrices. */
    private final List<CovarianceMatrixProvider> covarianceMatricesProviders;

    /** Indirection arrays to extract the noise components for estimated parameters. */
    private final int[][] covarianceIndirection;

    /** Scaling factors. */
    private final double[] scale;

    /** Mappers for extracting Jacobians from integrated states. */
    private final DSSTJacobiansMapper[] mappers;

    /** Propagators for the reference trajectories, up to current date. */
    private DSSTPropagator[] referenceTrajectories;

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

    /** Type of the orbit used for the propagation.*/
    private PropagationType propagationType;

    /** Type of the elements used to define the orbital state.*/
    private PropagationType stateType;

    /** Kalman process model constructor (package private).
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     */
    public DSSTKalmanModel(final List<IntegratedPropagatorBuilder> propagatorBuilders,
          final List<CovarianceMatrixProvider> covarianceMatricesProviders,
          final ParameterDriversList estimatedMeasurementParameters,
          final PropagationType propagationType,
          final PropagationType stateType) {

        this.builders                        = propagatorBuilders;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.referenceDate                   = propagatorBuilders.get(0).getInitialOrbitDate();
        this.currentDate                     = referenceDate;
        this.propagationType                 = propagationType;
        this.stateType                       = stateType;

        final Map<String, Integer> orbitalParameterColumns = new HashMap<>(6 * builders.size());
        orbitsStartColumns      = new int[builders.size()];
        orbitsEndColumns        = new int[builders.size()];
        int columns = 0;
        allEstimatedOrbitalParameters = new ParameterDriversList();
        for (int k = 0; k < builders.size(); ++k) {
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
        final Map<String, Integer> propagationParameterColumns = new HashMap<>(estimatedPropagationParametersNames.size());
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
        this.covarianceIndirection       = new int[covarianceMatricesProviders.size()][columns];
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
        mappers = new DSSTJacobiansMapper[builders.size()];
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
            final RealMatrix noiseK = covarianceMatricesProviders.get(k).
                                      getInitialCovarianceMatrix(correctedSpacecraftStates[k]);
            checkDimension(noiseK.getRowDimension(),
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
        final RealMatrix correctedCovariance = normalizeCovarianceMatrix(physicalProcessNoise);

        correctedEstimate = new ProcessEstimate(0.0, correctedState, correctedCovariance);

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

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalStateTransitionMatrix() {
        //  Un-normalize the state transition matrix (φ) from Hipparchus and return it.
        // φ is an mxm matrix where m = nbOrb + nbPropag + nbMeas
        // For each element [i,j] of normalized φ (φn), the corresponding physical value is:
        // φ[i,j] = φn[i,j] * scale[i] / scale[j]

        // Normalized matrix
        final RealMatrix normalizedSTM = correctedEstimate.getStateTransitionMatrix();

        if (normalizedSTM == null) {
            return null;
        } else {
            // Initialize physical matrix
            final int nbParams = normalizedSTM.getRowDimension();
            final RealMatrix physicalSTM = MatrixUtils.createRealMatrix(nbParams, nbParams);

            // Un-normalize the matrix
            for (int i = 0; i < nbParams; ++i) {
                for (int j = 0; j < nbParams; ++j) {
                    physicalSTM.setEntry(i, j,
                                         normalizedSTM.getEntry(i, j) * scale[i] / scale[j]);
                }
            }
            return physicalSTM;
        }
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

        // Normalized matrix
        final RealMatrix normalizedH = correctedEstimate.getMeasurementJacobian();

        if (normalizedH == null) {
            return null;
        } else {
            // Get current measurement sigmas
            final double[] sigmas = correctedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();

            // Initialize physical matrix
            final int nbLine = normalizedH.getRowDimension();
            final int nbCol  = normalizedH.getColumnDimension();
            final RealMatrix physicalH = MatrixUtils.createRealMatrix(nbLine, nbCol);

            // Un-normalize the matrix
            for (int i = 0; i < nbLine; ++i) {
                for (int j = 0; j < nbCol; ++j) {
                    physicalH.setEntry(i, j, normalizedH.getEntry(i, j) * sigmas[i] / scale[j]);
                }
            }
            return physicalH;
        }
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
    public ProcessEstimate getEstimate() {
        return correctedEstimate;
    }

    /** {@inheritDoc} */
    public DSSTPropagator[] getEstimatedPropagators() {

        // Return propagators built with current instantiation of the propagator builders
        final DSSTPropagator[] propagators = new DSSTPropagator[builders.size()];
        for (int k = 0; k < builders.size(); ++k) {
            propagators[k] = (DSSTPropagator) builders.get(k).buildPropagator(builders.get(k).getSelectedNormalizedParameters());
        }
        return propagators;
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

            // Short period derivatives
            mappers[k].setShortPeriodJacobians(predictedSpacecraftStates[k]);

            // Derivatives of the state vector with respect to initial state vector
            final double[][] dYdY0 = new double[6][6];
            mappers[k].getStateJacobian(predictedSpacecraftStates[k], dYdY0 );

            // Fill upper left corner (dY/dY0)
            final List<ParameterDriversList.DelegatingDriver> drivers =
                            builders.get(k).getOrbitalParametersDrivers().getDrivers();
            for (int i = 0; i < dYdY0.length; ++i) {
                if (drivers.get(i).isSelected()) {
                    int jOrb = orbitsStartColumns[k];
                    for (int j = 0; j < dYdY0[i].length; ++j) {
                        if (drivers.get(j).isSelected()) {
                            stm.setEntry(i, jOrb++, dYdY0[i][j]);
                        }
                    }
                }
            }

            // Derivatives of the state vector with respect to propagation parameters
            final int nbParams = estimatedPropagationParameters[k].getNbParams();
            if (nbParams > 0) {
                final double[][] dYdPp  = new double[6][nbParams];
                mappers[k].getParametersJacobian(predictedSpacecraftStates[k], dYdPp);

                // Fill 1st row, 2nd column (dY/dPp)
                for (int i = 0; i < dYdPp.length; ++i) {
                    for (int j = 0; j < nbParams; ++j) {
                        stm.setEntry(i, orbitsEndColumns[k] + j, dYdPp[i][j]);
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
            predictedOrbit.getJacobianWrtParameters(builders.get(p).getPositionAngle(), aCY);   //dC/dY
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
                // Short period derivatives
                mappers[p].setShortPeriodJacobians(evaluationStates[k]);
                final double[][] aYPp  = new double[6][nbParams];
                mappers[p].getParametersJacobian(evaluationStates[k], aYPp);
                final RealMatrix dYdPp = new Array2DRowRealMatrix(aYPp, false);
                final RealMatrix dMdPp = dMdY.multiply(dYdPp);
                for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                    for (int j = 0; j < nbParams; ++j) {
                        final ParameterDriver delegating = allEstimatedPropagationParameters.getDrivers().get(j);
                        measurementMatrix.setEntry(i, orbitsEndColumns[p] + j,
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


    /** Update the reference trajectories using the propagators as input.
     * @param propagators The new propagators to use
     */
    private void updateReferenceTrajectories(final DSSTPropagator[] propagators) {

        // Update the reference trajectory propagator
        referenceTrajectories = propagators;

        for (int k = 0; k < propagators.length; ++k) {
            // Link the partial derivatives to this new propagator
            final String equationName = KalmanEstimator.class.getName() + "-derivatives-" + k;
            final DSSTPartialDerivativesEquations pde = new DSSTPartialDerivativesEquations(equationName, referenceTrajectories[k], propagationType);

            // Reset the Jacobians
            final SpacecraftState rawState = referenceTrajectories[k].getInitialState();
            final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);
            referenceTrajectories[k].setInitialState(stateWithDerivatives, stateType);
            mappers[k] = pde.getMapper();
        }

    }

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
                                                            filterRelevant(observedMeasurement, predictedSpacecraftStates));

        // Normalized measurement matrix (nxm)
        final RealMatrix measurementMatrix = getMeasurementMatrix();

        // compute process noise matrix
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(previousState.getDimension(),
                                                                             previousState.getDimension());
        for (int k = 0; k < covarianceMatricesProviders.size(); ++k) {
            final RealMatrix noiseK = covarianceMatricesProviders.get(k).
                                      getProcessNoiseMatrix(correctedSpacecraftStates[k],
                                                            predictedSpacecraftStates[k]);
            checkDimension(noiseK.getRowDimension(),
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
        final RealMatrix normalizedProcessNoise = normalizeCovarianceMatrix(physicalProcessNoise);

        return new NonLinearEvolution(measurement.getTime(), predictedState,
                                      stateTransitionMatrix, normalizedProcessNoise, measurementMatrix);

    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final NonLinearEvolution evolution,
                                    final RealMatrix innovationCovarianceMatrix) {

        // Apply the dynamic outlier filter, if it exists
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

    /** {@inheritDoc} */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        correctedEstimate = estimate;
        updateParameters();

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final DSSTPropagator[] estimatedPropagators = getEstimatedPropagators();
        for (int k = 0; k < estimatedPropagators.length; ++k) {
            correctedSpacecraftStates[k] = estimatedPropagators[k].getInitialState();
        }

        // Compute the estimated measurement using estimated spacecraft state
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            filterRelevant(observedMeasurement, correctedSpacecraftStates));
        // Update the trajectory
        // ---------------------
        updateReferenceTrajectories(estimatedPropagators);

    }

    /** Filter relevant states for a measurement.
     * @param observedMeasurement measurement to consider
     * @param allStates all states
     * @return array containing only the states relevant to the measurement
     * @since 10.1
     */
    private SpacecraftState[] filterRelevant(final ObservedMeasurement<?> observedMeasurement, final SpacecraftState[] allStates) {
        final List<ObservableSatellite> satellites = observedMeasurement.getSatellites();
        final SpacecraftState[] relevantStates = new SpacecraftState[satellites.size()];
        for (int i = 0; i < relevantStates.length; ++i) {
            relevantStates[i] = allStates[satellites.get(i).getPropagatorIndex()];
        }
        return relevantStates;
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

}
