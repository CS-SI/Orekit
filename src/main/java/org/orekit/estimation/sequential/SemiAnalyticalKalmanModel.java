/* Copyright 2002-2021 CS GROUP
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
import org.hipparchus.filtering.kalman.extended.NonLinearEvolution;
import org.hipparchus.filtering.kalman.extended.NonLinearProcess;
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
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Abstract class defining the process model dynamics to use with a {@link SemiAnalyticalKalmanEstimator}.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @author Julie Bayard
 */
public  class SemiAnalyticalKalmanModel implements KalmanEstimation, NonLinearProcess<MeasurementDecorator> {

    /** Builders for propagators. */
    private final DSSTPropagatorBuilder builder;

    /** Estimated orbital parameters. */
    private final ParameterDriversList estimatedOrbitalParameters;

    /** Per-builder estimated propagation drivers. */
    private final ParameterDriversList estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Map for propagation parameters columns. */
    private final Map<String, Integer> propagationParameterColumns;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> measurementParameterColumns;

    /** Scaling factors. */
    private final double[] scale;

    /** Mappers for extracting Jacobians from integrated states. */
    private DSSTJacobiansMapper mapper;

    /** Propagators for the reference trajectories, up to current date. */
    private DSSTPropagator dsstPropagator;

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

    /** Type of the orbit used for the propagation.*/
    private PropagationType propagationType = PropagationType.OSCULATING;

    /** Number of orbital parameters. */
    private final int nbOrbitalParameters = 6;

    /** Number of propagation parameters. */
    private int nbPropagationParameters;

    /** Number of measurement parameters. */
    private int nbMeasurementParameters;

    /** Old inverted dY/dY0 used in the estimation of the state transition matrix. */
    private RealMatrix old_dYdY0 = MatrixUtils.createRealIdentityMatrix(6);

    /** Old dY/dP used in the estimation of the state transition matrix. */
    private RealMatrix old_dYdP;

    /** Previous state from the previous step Z. */
    private RealVector oldState;

    /** Previous correction state delta Z. */
    private RealVector oldCorrectionState;

    /** Boolean if there are propagation parameters. */
    private Boolean usePropagationDrivers;



    /** Kalman process model constructor (package private).
     * This constructor is used whenever state type and propagation type do not matter.
     * It is used for {@link KalmanModel} and {@link TLEKalmanModel}.
     * @param propagatorBuilder propagators builders used to evaluate the orbits.
     * @param propagator used during the whole estimation, built with propagatorBuilder.
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    protected SemiAnalyticalKalmanModel(final DSSTPropagatorBuilder propagatorBuilder,
                                        final DSSTPropagator propagator,
                                        final ParameterDriversList estimatedMeasurementParameters,
                                        final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        this(propagatorBuilder, propagator, estimatedMeasurementParameters,
             measurementProcessNoiseMatrix, PropagationType.MEAN);
    }

    /** Kalman process model constructor (package private).
     * This constructor is used whenever propagation type and/or state type are to be specified.
     * It is used for {@link DSSTKalmanModel}.
     * @param propagatorBuilder propagators builders used to evaluate the orbits.
     * @param propagator used during the whole estimation, built with propagatorBuilder.
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     * @param stateType type of the elements used to define the orbital state (mean or osculating), applicable only for DSST
     */
    protected SemiAnalyticalKalmanModel(final DSSTPropagatorBuilder propagatorBuilder,
                                        final DSSTPropagator propagator,
                                        final ParameterDriversList estimatedMeasurementParameters,
                                        final CovarianceMatrixProvider measurementProcessNoiseMatrix,
                                        final PropagationType stateType) {

        this.builder                         = propagatorBuilder;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.nbMeasurementParameters         = estimatedMeasurementParameters.getNbParams();
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.currentMeasurementNumber        = 0;
        this.referenceDate                   = propagatorBuilder.getInitialOrbitDate();
        this.currentDate                     = referenceDate;
        this.oldCorrectionState              = MatrixUtils.createRealVector(nbOrbitalParameters);
        this.dsstPropagator                  = propagator;
        this.predictedSpacecraftState        = dsstPropagator.getInitialState();
        this.correctedSpacecraftState        = predictedSpacecraftState;


        final Map<String, Integer> orbitalParameterColumns = new HashMap<>(nbOrbitalParameters);
        int columns = 0;
        estimatedOrbitalParameters = new ParameterDriversList();

        // Gather all orbital parameters
        for (final ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(currentDate);
            }
            if (driver.isSelected()) {
                estimatedOrbitalParameters.add(driver);
                orbitalParameterColumns.put(driver.getName(), columns++);
            }
        }

        // Gather all the propagation drivers names in a list
        estimatedPropagationParameters    = new ParameterDriversList();
        final List<String> estimatedPropagationParametersNames = new ArrayList<>();
        for (final ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(currentDate);
            }
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
        this.nbPropagationParameters = estimatedPropagationParametersNames.size();


        // Create the derivative of the state on propagation parameters if there are propagation parameters
        if (nbPropagationParameters != 0) {
            old_dYdP = MatrixUtils.createRealMatrix(nbOrbitalParameters, nbPropagationParameters);
            this.usePropagationDrivers = true;
        }
        else {
            this.usePropagationDrivers = false;
        }


        // Populate the map of propagation drivers' columns and update the total number of columns
        propagationParameterColumns = new HashMap<>(nbPropagationParameters);
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

        // Link the partial derivatives to the propagator
        final String equationName = KalmanEstimator.class.getName() + "-derivatives-";
        final DSSTPartialDerivativesEquations pde = new DSSTPartialDerivativesEquations(equationName, getReferenceTrajectory(), propagationType);

        // Reset the Jacobians
        final SpacecraftState rawState = getReferenceTrajectory().getInitialState();
        final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);
        ((DSSTPropagator) getReferenceTrajectory()).setInitialState(stateWithDerivatives, stateType);
        mapper = pde.getMapper();

        // Update Jacobian mappers
        setMapper(mapper);


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

        // Set up initial covariance
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(columns, columns);


        checkDimension(nbOrbitalParameters + nbPropagationParameters + nbMeasurementParameters,
                       builder.getOrbitalParametersDrivers(),
                       builder.getPropagationParametersDrivers(),
                       estimatedMeasurementsParameters);

        correctedEstimate = new ProcessEstimate(0.0, correctedState, physicalProcessNoise);
        this.oldState = correctedEstimate.getState();
    }

    /** Analytical computation of derivatives.
     * This method allow to compute analytical derivatives.
     * @param state mean state used to calculate short period perturbations
     */
    private void analyticalDerivativeComputations(final SpacecraftState state) {
        mapper.setShortPeriodJacobians(state);
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
            final StringBuilder strBuilder = new StringBuilder();
            for (final ParameterDriver driver : orbitalParameters.getDrivers()) {
                if (strBuilder.length() > 0) {
                    strBuilder.append(", ");
                }
                strBuilder.append(driver.getName());
            }
            for (final ParameterDriver driver : propagationParameters.getDrivers()) {
                if (driver.isSelected()) {
                    strBuilder.append(driver.getName());
                }
            }
            for (final ParameterDriver driver : measurementParameters.getDrivers()) {
                if (driver.isSelected()) {
                    strBuilder.append(driver.getName());
                }
            }
            throw new OrekitException(OrekitMessages.DIMENSION_INCONSISTENT_WITH_PARAMETERS,
                                      dimension, strBuilder.toString());
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
        return new SpacecraftState[] {predictedSpacecraftState};
    }

    /** Change the predicted spacecraft state to the specified state.
     * It must only be used by the @link{SemiAnalyticalKalmanEstimator}.
     * @param state the state to switch with
     */
    public void setPredictedSpacecraftState(final SpacecraftState state) {
        this.predictedSpacecraftState = state;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState[] getCorrectedSpacecraftStates() {
        return new SpacecraftState[] {correctedSpacecraftState};
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

        // Short period derivatives
        analyticalDerivativeComputations(predictedSpacecraftState);

        // Derivatives of the state vector with respect to initial state vector
        final double[][] dYdY0List = new double[nbOrbitalParameters][nbOrbitalParameters];

        // In case of MEAN propagation : short period terms are not added
        if (propagationType == PropagationType.MEAN) {
            mapper.getB2(predictedSpacecraftState, dYdY0List);
        }

        // In case of OSCULATING propagation : short periodic terms are included 
        else {
            mapper.getStateJacobian(predictedSpacecraftState, dYdY0List);
        }

        final RealMatrix dYdY0 = MatrixUtils.createRealMatrix(dYdY0List);

        final RealMatrix dYdYi = dYdY0.multiply(MatrixUtils.inverse(old_dYdY0));


        // Fill dYi/dYi-1
        final List<ParameterDriversList.DelegatingDriver> drivers =
                        builder.getOrbitalParametersDrivers().getDrivers();
        for (int i = 0; i < nbOrbitalParameters; ++i) {
            if (drivers.get(i).isSelected()) {
                int jOrb = 0;
                for (int j = 0; j < nbOrbitalParameters; ++j) {
                    if (drivers.get(j).isSelected()) {
                        stm.setEntry(i, jOrb++, dYdYi.getEntry(i, j));
                    }
                }
            }
        }

        // Derivatives of the state vector with respect to propagation parameters
        if (nbPropagationParameters > 0) {
            final double[][] dYdP0List  = new double[nbOrbitalParameters][nbPropagationParameters];

            // In case of MEAN propagation : short period terms are not added
            if (propagationType == PropagationType.MEAN) {
                mapper.getB3(predictedSpacecraftState, dYdP0List);
            }

            // In case of OSCULATING propagation : short periodic terms are included
            else {
                mapper.getParametersJacobian(predictedSpacecraftState, dYdP0List);
            }

            final RealMatrix dYdP0 = MatrixUtils.createRealMatrix(dYdP0List);
            final RealMatrix dYdPi = dYdP0.subtract(dYdYi.multiply(old_dYdP));

            // Fill 1st row, 2nd column (dY/dPp)
            for (int i = 0; i < nbOrbitalParameters; ++i) {
                for (int j = 0; j < nbPropagationParameters; ++j) {
                    stm.setEntry(i, j, dYdPi.getEntry(i, j));
                }
            }
            old_dYdP = dYdP0;
        }


        // Normalization of the STM
        // normalized(STM)ij = STMij*Sj/Si
        for (int i = 0; i < scale.length; i++) {
            for (int j = 0; j < scale.length; j++ ) {
                stm.setEntry(i, j, stm.getEntry(i, j) * scale[j] / scale[i]);
            }
        }

        old_dYdY0 = dYdY0;

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
        final SpacecraftState      evaluationState    = predictedMeasurement.getStates()[0];
        final ObservedMeasurement<?> observedMeasurement = predictedMeasurement.getObservedMeasurement();
        final double[] sigma  = predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();

        // Initialize measurement matrix H: nxm
        // n: Number of measurements in current measurement
        // m: State vector size


        // Predicted orbit
        final Orbit predictedOrbit = evaluationState.getOrbit();

        // Measurement matrix's columns related to orbital and propagation parameters
        // ----------------------------------------------------------

        // Partial derivatives of the current Cartesian coordinates with respect to current orbital state
        final double[][] aCY = new double[nbOrbitalParameters][nbOrbitalParameters];
        predictedOrbit.getJacobianWrtParameters(builder.getPositionAngle(), aCY);   //dC/dY
        final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

        // Jacobian of the measurement with respect to current Cartesian coordinates
        final RealMatrix dMdC = new Array2DRowRealMatrix(predictedMeasurement.getStateDerivatives(0), false);

        // Jacobian of the measurement with respect to current orbital state
        RealMatrix dMdY = dMdC.multiply(dCdY);

        // In case of OSCULATING propagation : short periodic terms are already included
        // In case of MEAN propagation : the measurement matrix must be multiplied by I+B1|B4 to include short periodic terms
        if (propagationType == PropagationType.MEAN) {
            // Compute factor dShortPeriod_dMeanState = I+B1 | B4
            final RealMatrix dShortPeriod_dMeanState = MatrixUtils.createRealMatrix(nbOrbitalParameters, nbOrbitalParameters + nbPropagationParameters);

            // Compute B1 and B4
            final double[][] IpB1 = new double[nbOrbitalParameters][nbOrbitalParameters];
            mapper.getB1(IpB1);
            // Add identity matrix to B1
            for (int i = 0; i < nbOrbitalParameters; i++) {
                IpB1[i][i] += 1;
            }
            dShortPeriod_dMeanState.setSubMatrix(IpB1, 0, 0);

            // If there are not propagation parameters, B4 is null
            if (this.usePropagationDrivers) {
                final double[][] B4 = new double[nbOrbitalParameters][nbPropagationParameters];
                mapper.getB4(B4);
                dShortPeriod_dMeanState.setSubMatrix(B4, 0, nbOrbitalParameters);

            }
            dMdY = dMdY.multiply(dShortPeriod_dMeanState);
        }

        final RealMatrix measurementMatrix = MatrixUtils.createRealMatrix(observedMeasurement.getDimension(), correctedEstimate.getState().getDimension());
        for (int i = 0; i < nbMeasurementParameters; i++) {
            for (int j = 0; j < nbOrbitalParameters; j++) {
                final double driverScale = builder.getOrbitalParametersDrivers().getDrivers().get(j).getScale();
                measurementMatrix.setEntry(i, j, dMdY.getEntry(i, j) / sigma[i] * driverScale);
            }
            for (int j = 0; j < nbPropagationParameters; j++) {
                final double driverScale = estimatedPropagationParameters.getDrivers().get(j).getScale();
                measurementMatrix.setEntry(i, j + nbOrbitalParameters,
                                           dMdY.getEntry(i, j + nbOrbitalParameters) / sigma[i] * driverScale);
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
                        measurementMatrix.setEntry(i, driverColumn, aMPm[i] / sigma[i] * driver.getScale());
                    }
                }
            }
        }

        return measurementMatrix;
    }


    /** Compute osculating state from state.
     * <p>
     * Compute and add the short periodic variation to the mean {@link SpacecraftState}.
     * </p>
     * @param stm the state transition matrix
     * @return osculating state
     */
    private double[] computeOsculatingElements(final RealMatrix stm) {

        final RealVector stateTransitionTerm;

        // In case of MEAN propagation : the stm must be multiplied by I+B1 to include short periodic terms
        if (propagationType == PropagationType.MEAN) {
            final double[][] B1 = new double[nbOrbitalParameters][nbOrbitalParameters];
            mapper.getB1(B1);
            final RealMatrix B1Matrix = MatrixUtils.createRealMatrix(B1);
            stateTransitionTerm = B1Matrix.add(MatrixUtils.createRealIdentityMatrix(nbOrbitalParameters)).multiply(stm).operate(oldCorrectionState);
            final double[] shortPeriodTerms = dsstPropagator.getShortPeriodTermsValue(predictedSpacecraftState);

            final double[] mean = new double[6];
            OrbitType.EQUINOCTIAL.mapOrbitToArray(predictedSpacecraftState.getOrbit(), builder.getPositionAngle(), mean, null);
            final double[] y = mean.clone();

            for (int i = 0; i < shortPeriodTerms.length; i++) {
                y[i] += shortPeriodTerms[i] + stateTransitionTerm.getEntry(i);
            }
            return y;

        }

        // In case of OSCULATING propagation : short periodic terms are already included
        else {
            final double[] osculating = new double[6];
            OrbitType.EQUINOCTIAL.mapOrbitToArray(predictedSpacecraftState.getOrbit(), builder.getPositionAngle(), osculating, null);
            return osculating;
        }

    }


    private SpacecraftState elementsToSpacecraftState(final double[] elements) {
        return new SpacecraftState(OrbitType.EQUINOCTIAL.mapArrayToOrbit(elements, null, builder.getPositionAngle(),
                                                                         currentDate, predictedSpacecraftState.getMu(), predictedSpacecraftState.getFrame()),
                                   predictedSpacecraftState.getAttitude(), predictedSpacecraftState.getMass(), predictedSpacecraftState.getAdditionalStates());
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
                driver.setReferenceDate(builder.getInitialOrbitDate());
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
        //predictState();

        // Get the error state transition matrix (mxm)
        final RealMatrix stateTransitionMatrix = getErrorStateTransitionMatrix();

        final double[] predictedOsculatedElements = computeOsculatingElements(stateTransitionMatrix);

        //elementsToSpacecraftState(final double[] elements, final SpacecraftState state, final double mass)

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
                                                            new SpacecraftState[] {elementsToSpacecraftState(predictedOsculatedElements)});

        // Short period terms value
        //final double[] spt = referenceTrajectory.getShortPeriodTermsValue(predictedSpacecraftState);

        // Normalized measurement matrix (nxm)
        final RealMatrix measurementMatrix = getMeasurementMatrix();

        // compute process noise matrix (no process noise in skf)
        final RealMatrix physicalProcessNoise = MatrixUtils.createRealMatrix(previousState.getDimension(),
                                                                             previousState.getDimension());


        return new NonLinearEvolution(measurement.getTime(), MatrixUtils.createRealVector(predictedOsculatedElements),
                                      stateTransitionMatrix, physicalProcessNoise, measurementMatrix);

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

    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        correctedEstimate = estimate;

        // Udpate the state and its correction used during the computation of the next correction.
        oldCorrectionState = estimate.getState().subtract(oldState);
        oldState = estimate.getState();
        //updateParameters() pour mettre à jour l'état avant de getEstimatedPropagtor;

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        correctedSpacecraftState = dsstPropagator.getInitialState();

        // Compute the estimated measurement using estimated spacecraft state
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            new SpacecraftState[] {correctedSpacecraftState});
        // Update the trajectory
        // ---------------------
        //updateReferenceTrajectory(dsstPropagator, stateType);
        /** Update the estimated parameters after the correction phase of the filter.
         * The min/max allowed values are handled by the parameter themselves.
         */

        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(oldState.getEntry(i));
            oldState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(oldState.getEntry(i));
            oldState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(oldState.getEntry(i));
            oldState.setEntry(i++, driver.getNormalizedValue());

        }

    }

    /** Set the predicted normalized state vector.
     * The predicted/propagated orbit is used to update the state vector
     * @return predicted state
     */
    private RealVector predictState() {


        // Predicted state is initialized to previous estimated state
        final RealVector predictedState = correctedEstimate.getState().copy();

        // Orbital parameters counter
        int jOrb = 0;

        // Propagate the reference trajectory to measurement date
        predictedSpacecraftState = dsstPropagator.propagate(currentDate);

        // Update the builder with the predicted orbit
        // This updates the orbital drivers with the values of the predicted orbit
        builder.resetOrbit(predictedSpacecraftState.getOrbit());

        // The orbital parameters in the state vector are replaced with their predicted values
        // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
        // As the propagator builder was previously updated with the predicted orbit,
        // the selected orbital drivers are already up to date with the prediction
        for (DelegatingDriver orbitalDriver : builder.getOrbitalParametersDrivers().getDrivers()) {
            if (orbitalDriver.isSelected()) {
                predictedState.setEntry(jOrb++, orbitalDriver.getNormalizedValue());
            }
        }


        return predictedState;

    }

    /** Update the estimated parameters after the correction phase of the filter.
     * The min/max allowed values are handled by the parameter themselves.
     */
    @SuppressWarnings("unused")
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
    public DSSTPropagatorBuilder getBuilder() {
        return builder;
    }

    /** Getter for the reference trajectories.
     * @return the referencetrajectories
     */
    public DSSTPropagator getReferenceTrajectory() {
        return dsstPropagator;
    }

    /** Getter for the jacobian mappers.
     * @return the jacobian mappers
     */
    public DSSTJacobiansMapper getMapper() {
        return mapper;
    }

    /** Setter for the jacobian mappers.
     * @param mapper the jacobian mappers to set
     */
    public void setMapper(final DSSTJacobiansMapper mapper) {
        this.mapper = mapper;
    }
}
