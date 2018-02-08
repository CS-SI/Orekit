/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;


/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanProcessModel {

    /** Builder for propagator. */
    private NumericalPropagatorBuilder builder;

    /** Propagator for the reference trajectory, up to current date. */
    private NumericalPropagator referenceTrajectory;

    /** Partial derivative equations for the reference trajectory propagator. */
    private PartialDerivativesEquations referenceTrajectoryPartialDerivatives;

    /** Estimated orbital parameters. */
    private final ParameterDriversList estimatedOrbitalParameters;

    /** Total number of estimated orbital parameters. */
    private final int nbOrbitalParameters;

    /** Estimated propagation parameters. */
    private final ParameterDriversList estimatedPropagationParameters;

    /** Total number of estimated propagation parameters. */
    private final int nbPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Map for measurements parameters columns in the measurement matrix. */
    private final Map<String, Integer> measurementParameterColumns;

    /** Total number of estimated measurement parameters. */
    private final int nbMeasurementsParameters;

    /** Process noise matrix.
     * Second moment of the process noise. Often named Q.
     */
    private RealMatrix processNoiseMatrix;

    /** Initial covariance matrix. */
    private RealMatrix initialCovarianceMatrix;

    /** Kalman process model constructor (package private).
     * @param propagatorBuilder propagator builder used to evaluate the orbit.
     * @param estimatedOrbitalParameters orbital parameters to estimate
     * @param estimatedPropagationParameters propagation parameters to estimate
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param physicalInitialCovariance "Physical" initial covariance matrix (ie. not normalized)
     * @param physicalProcessNoiseMatrix "Physical" process noise matrix (ie.not normalized)
     * @throws OrekitException propagation exception.
     */
    KalmanProcessModel(final NumericalPropagatorBuilder propagatorBuilder,
                       final ParameterDriversList estimatedOrbitalParameters,
                       final ParameterDriversList estimatedPropagationParameters,
                       final ParameterDriversList estimatedMeasurementParameters,
                       final RealMatrix physicalInitialCovariance,
                       final RealMatrix physicalProcessNoiseMatrix)
                                          throws OrekitException {

        this.builder                         = propagatorBuilder;
        this.estimatedOrbitalParameters      = estimatedOrbitalParameters;
        this.estimatedPropagationParameters  = estimatedPropagationParameters;
        this.estimatedMeasurementsParameters = estimatedMeasurementParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());

        // Populate the map of measurement drivers' columns and update the total number of columns
        int columns = estimatedOrbitalParameters.getNbParams() + estimatedPropagationParameters.getNbParams();
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            measurementParameterColumns.put(parameter.getName(), columns);
            ++columns;
        }

        // Count the number of parameters per type
        this.nbOrbitalParameters      = estimatedOrbitalParameters.getNbParams();
        this.nbPropagationParameters  = estimatedPropagationParameters.getNbParams();
        this.nbMeasurementsParameters = estimatedMeasurementsParameters.getNbParams();

        // Check the size consistency of the covariance and process noise matrices
        final int M = nbOrbitalParameters + nbPropagationParameters + nbMeasurementsParameters;
        // Covariance
        if (physicalInitialCovariance.getColumnDimension() != M) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                   physicalInitialCovariance.getColumnDimension(),
                                                   M);
        }
        if (physicalInitialCovariance.getRowDimension() != M) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                   physicalInitialCovariance.getRowDimension(),
                                                   M);
        }
        // Process noise
        if (physicalProcessNoiseMatrix.getColumnDimension() != M) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                   physicalInitialCovariance.getColumnDimension(),
                                                   M);
        }
        if (physicalProcessNoiseMatrix.getRowDimension() != M) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                   physicalInitialCovariance.getRowDimension(),
                                                   M);
        }

        // Initialize normalized estimated covariance matrix and process noise matrix
        this.initialCovarianceMatrix = normalizeCovarianceMatrix(physicalInitialCovariance);
        this.processNoiseMatrix      = normalizeCovarianceMatrix(physicalProcessNoiseMatrix);

        // Build the reference propagator and add its partial derivatives equations implementation
        updateReferenceTrajectory(getEstimatedPropagator());
    }

    /** Get the list of estimated orbital parameters.
     * @return the list of estimated orbital parameters
     */
    public ParameterDriversList getEstimatedOrbitalParameters() {
        return estimatedOrbitalParameters;
    }

    /** Get the number of estimated orbital parameters.
     * @return the number of estimated orbital parameters
     */
    public int getNbOrbitalParameters() {
        return nbOrbitalParameters;
    }

    /** Get the list of estimated propagation parameters.
     * @return the list of estimated propagation parameters
     */
    public ParameterDriversList getEstimatedPropagationParameters() {
        return estimatedPropagationParameters;
    }

    /** Get the number of estimated propagation parameters.
     * @return the number of estimated propagation parameters
     */
    public int getNbPropagationParameters() {
        return nbPropagationParameters;
    }

    /** Get the list of estimated measurements parameters.
     * @return the list of estimated measurements parameters
     */
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return estimatedMeasurementsParameters;
    }

    /** Get the number of estimated measurements parameters.
     * @return the number of estimated measurements parameters
     */
    public int getNbMeasurementsParameters() {
        return nbMeasurementsParameters;
    }

    /** Get the partial derivatives equations of the reference trajectory.
     * @return the PDE associated with the reference trajectory propagator
     */
    public PartialDerivativesEquations getReferenceTrajectoryPartialDerivatives() {
        return referenceTrajectoryPartialDerivatives;
    }

    /** Get the first initial normalized estimated state.
     * @return the first normalized estimated state
     */
    public RealVector getInitialEstimatedState()
    {
        // Initialize the estimated normalized state and fill its values
        final RealVector firstState = new ArrayRealVector(nbOrbitalParameters + nbPropagationParameters + nbMeasurementsParameters);

        int i = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            firstState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedPropagationParameters.getDrivers()) {
            firstState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            firstState.setEntry(i++, driver.getNormalizedValue());
        }
        return firstState;
    }

    /** Get the initial normalized error covariance matrix.
     * @return the initial normalized covariance matrix
     */
    public RealMatrix getInitialCovarianceMatrix() {
        return initialCovarianceMatrix;
    }

    /** Get the normalized process noise matrix.
     * @return the normalized process noise matrix
     */
    public RealMatrix getProcessNoiseMatrix() {
        return processNoiseMatrix;
    }

    /** Get the propagator estimated with the values set in the propagator builder.
     * @return a numerical propagator based on the current values in the builder
     * @throws OrekitException if propagator cannot be build
     */
    public NumericalPropagator getEstimatedPropagator()
                    throws OrekitException {

        // Return a propagator built with current instantiation of the propagator builder
        return builder.buildPropagator(builder.getSelectedNormalizedParameters());
    }

    /** Initialize the partial derivatives equations.
     * @exception OrekitException if orbit cannot be created with the current point
     */
    void initializeDerivatives()
        throws OrekitException {

        // Reset the Jacobians
        final SpacecraftState rawState = referenceTrajectory.getInitialState();
        final SpacecraftState stateWithDerivatives =
                        referenceTrajectoryPartialDerivatives.setInitialJacobians(rawState);
        referenceTrajectory.resetInitialState(stateWithDerivatives);
    }

    /** Propagate the reference trajectory to a given date.
     * @param targetDate date when to propagate to
     * @throws OrekitException if the propagation failed
     * @return the spacecraft state at target date
     */
    SpacecraftState propagateReferenceTrajectory(final AbsoluteDate targetDate)
                    throws OrekitException {
        return referenceTrajectory.propagate(targetDate);
    }

    /** Get the normalized error state transition matrix (STM) from previous point to current point.
     * The STM contains the partial derivatives of current state with respect to previous state.
     * The  STM is an MxM matrix where M is the size of the state vector.
     * M = nbOrb + nbPropag + nbMeas
     * @param predictedSpacecraftState current spacecraft state
     * @param derivatives partial derivatives equations of current state with respect to previous state
     * @return the normalized error state transition matrix
     * @throws OrekitException if Jacobians cannot be computed
     */
    public RealMatrix getErrorStateTransitionMatrix(final SpacecraftState predictedSpacecraftState,
                                                    final PartialDerivativesEquations derivatives)
                    throws OrekitException {

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
        final RealMatrix STM = MatrixUtils.createRealIdentityMatrix(nbOrbitalParameters +
                                                                    nbPropagationParameters +
                                                                    nbMeasurementsParameters);
        // Mappers for the Jacobians
        final JacobiansMapper referenceTrajectoryJacobiansMapper = derivatives.getMapper();

        // Derivatives of the state vector with respect to initial state vector
        if (nbOrbitalParameters > 0) {
            final double[][] dYdY0 = new double[nbOrbitalParameters][nbOrbitalParameters];
            referenceTrajectoryJacobiansMapper.getStateJacobian(predictedSpacecraftState, dYdY0 );

            // Fill upper left corner (dY/dY0)
            STM.setSubMatrix(dYdY0, 0, 0);
        }

        // Derivatives of the state vector with respect to propagation parameters
        if (nbPropagationParameters > 0) {
            final double[][] dYdPp  = new double[nbOrbitalParameters][nbPropagationParameters];
            referenceTrajectoryJacobiansMapper.getParametersJacobian(predictedSpacecraftState, dYdPp);

            // Fill 1st row, 2nd column (dY/dPp)
            STM.setSubMatrix(dYdPp, 0, nbOrbitalParameters);
        }

        // Normalization of the STM
        // normalized(STM)ij = STMij*Sj/Si
        final double[] scales = getParametersScale();
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < scales.length; j++ ) {
                STM.setEntry(i, j, STM.getEntry(i, j) * scales[j] / scales[i]);
            }
        }

        // Return the error state transition matrix
        return STM;
    }

    /** Get the normalized measurement matrix H.
     * H contains the partial derivatives of the measurement with respect to the state.
     * H is an NxM matrix where N is the size of the measurement vector and M the size of the state vector.
     * @param predictedSpacecraftState the spacecraft state associated with the measurement
     * @param predictedMeasurement the measurement used to compute the measurement matrix
     * @return the normalized measurement matrix H
     * @throws OrekitException if Jacobians cannot be computed
     */
    RealMatrix getMeasurementMatrix(final SpacecraftState predictedSpacecraftState,
                                    final EstimatedMeasurement<?> predictedMeasurement)
        throws OrekitException {

        // Number of parameters
        final int nbOrb    = estimatedOrbitalParameters.getNbParams();
        final int nbPropag = estimatedPropagationParameters.getNbParams();
        final int nbMeas   = estimatedMeasurementsParameters.getNbParams();

        // Initialize measurement matrix H: N x M
        // N: Number of measurements in current measurement
        // M: State vector size
        final RealMatrix measurementMatrix = MatrixUtils.
                        createRealMatrix(predictedMeasurement.getEstimatedValue().length,
                                         nbOrb + nbPropag + nbMeas);

        // Predicted orbit
        final Orbit predictedOrbit = predictedSpacecraftState.getOrbit();

        // Observed measurement characteristics
        final ObservedMeasurement<?> observedMeasurement = predictedMeasurement.getObservedMeasurement();
        final double[] sigma  = observedMeasurement.getTheoreticalStandardDeviation();

        // Measurement matrix's columns related to orbital parameters
        // ----------------------------------------------------------

        // Partial derivatives of the current Cartesian coordinates with respect to current orbital state
        final double[][] aCY = new double[6][6];
        predictedOrbit.getJacobianWrtParameters(builder.getPositionAngle(), aCY);   //dC/dY
        final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

        // Jacobian of the measurement with respect to current Cartesian coordinates
        final RealMatrix dMdC = new Array2DRowRealMatrix(predictedMeasurement.getStateDerivatives(0), false);

        // Jacobian of the measurement with respect to current orbital state
        final RealMatrix dMdY = dMdC.multiply(dCdY);

        // Fill the normalized measurement matrix's columns related to estimated orbital parameters
        if ( nbOrb > 0 ) {
            for (int i = 0; i < dMdY.getRowDimension(); ++i) {
                int jOrb = 0;
                for (int j = 0; j < dMdY.getColumnDimension(); ++j) {
                    final DelegatingDriver delegating = builder.getOrbitalParametersDrivers().getDrivers().get(j);
                    if (delegating.isSelected()) {
                        measurementMatrix.setEntry(i, jOrb++,
                                                   dMdY.getEntry(i, j) / sigma[i] * delegating.getScale());
                    }
                }
            }
        }

        // Normalized measurement matrix's columns related to propagation parameters
        // --------------------------------------------------------------

        // Jacobian of the measurement with respect to propagation parameters
        if ( nbPropag > 0 ) {
            final double[][] aYPp  = new double[6][nbPropag];
            referenceTrajectoryPartialDerivatives.getMapper().getParametersJacobian(predictedSpacecraftState, aYPp);
            final RealMatrix dYdPp = new Array2DRowRealMatrix(aYPp, false);
            final RealMatrix dMdPp = dMdY.multiply(dYdPp);
            for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                for (int j = 0; j < nbPropag; ++j) {
                    final DelegatingDriver delegating = estimatedPropagationParameters.getDrivers().get(j);
                    measurementMatrix.setEntry(i, nbOrb + j,
                                               dMdPp.getEntry(i, j) / sigma[i] * delegating.getScale());
                }
            }
        }

        // Normalized measurement matrix's columns related to measurement parameters
        // --------------------------------------------------------------

        // Jacobian of the measurement with respect to measurement parameters
        if ( nbMeas > 0 ) {
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


    /** Update the reference trajectory using the propagator as input.
     * @param propagator The new propagator to use
     * @throws OrekitException if setting up the partial derivatives failed
     */
    public void updateReferenceTrajectory(final NumericalPropagator propagator) throws OrekitException {

        // Update the reference trajectory propagator
        referenceTrajectory = propagator;

        // Link the partial derivatives to this new propagator
        final String equationName = KalmanEstimatorReal.class.getName() + "-derivatives";
        this.referenceTrajectoryPartialDerivatives = new PartialDerivativesEquations(equationName, referenceTrajectory);

    }

    /** Gather the different scaling factors of the estimated parameters in an array.
     * @return the array of scales (ie. scaling factors)
     */
    private double[] getParametersScale() {

        // Retrieve the scale factors
        final double[] scale = new double[nbOrbitalParameters +
                                          nbPropagationParameters +
                                          nbMeasurementsParameters];
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
        return scale;
    }

    /** Normalize a state vector.
     * A state vector S is of size M = nbOrb + nbPropag + nbMeas
     * For each parameter i the normalized value of the state vector is:
     * Sn[i] = S[i] / scale[i]
     * @param physicalStateVector The "physical" state vector in input
     * @return the normalized state vector
     */
    public RealVector normalizeStateVector(final RealVector physicalStateVector) {

        // Initialize output matrix
        final int nbParams = physicalStateVector.getDimension();
        final RealVector normalizedStateVector = new ArrayRealVector(nbParams);

        // Get the scaling factors
        final double[] scale = getParametersScale();

        // Normalize the state matrix
        for (int i = 0; i < nbParams; ++i) {
            normalizedStateVector.setEntry(i, physicalStateVector.getEntry(i) / scale[i]);
        }
        return normalizedStateVector;
    }

    /** Un-normalize a state vector.
     * A state vector S is of size M = nbOrb + nbPropag + nbMeas
     * For each parameter i the normalized value of the state vector is:
     * Sn[i] = S[i] / scale[i]
     * @param normalizedStateVector The normalized state vector in input
     * @return the "physical" state vector
     */
    public RealVector unNormalizeStateVector(final RealVector normalizedStateVector) {

        // Initialize output matrix
        final int nbParams = normalizedStateVector.getDimension();
        final RealVector physicalStateVector = new ArrayRealVector(nbParams);

        // Retrieve the scaling factors
        final double[] scale = getParametersScale();

        // Normalize the state matrix
        for (int i = 0; i < nbParams; ++i) {
            physicalStateVector.setEntry(i, normalizedStateVector.getEntry(i) * scale[i]);
        }
        return physicalStateVector;
    }

    /** Normalize a covariance matrix.
     * The covariance P is an MxM matrix where M = nbOrb + nbPropag + nbMeas
     * For each element [i,j] of P the corresponding normalized value is:
     * Pn[i,j] = P[i,j] / (scale[i]*scale[j])
     * @param physicalCovarianceMatrix The "physical" covariance matrix in input
     * @return the normalized covariance matrix
     */
    public RealMatrix normalizeCovarianceMatrix(final RealMatrix physicalCovarianceMatrix) {

        // Initialize output matrix
        final int nbParams = physicalCovarianceMatrix.getRowDimension();
        final RealMatrix normalizedCovarianceMatrix = MatrixUtils.createRealMatrix(nbParams, nbParams);

        // Retrieve the scaling factors
        final double[] scale = getParametersScale();

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

    /** Un-normalize a covariance matrix.
     * The covariance P is an MxM matrix where M = nbOrb + nbPropag + nbMeas
     * For each element [i,j] of P the corresponding normalized value is:
     * Pn[i,j] = P[i,j] / (scale[i]*scale[j])
     * @param normalizedCovarianceMatrix The normalized covariance matrix in input
     * @return the "physical" covariance matrix
     */
    public RealMatrix unNormalizeCovarianceMatrix(final RealMatrix normalizedCovarianceMatrix) {

        // Initialize output matrix
        final int nbParams = normalizedCovarianceMatrix.getRowDimension();
        final RealMatrix physicalCovarianceMatrix = MatrixUtils.createRealMatrix(nbParams, nbParams);

        // Retrieve the scaling factors
        final double[] scale = getParametersScale();

        // Normalize the state matrix
        for (int i = 0; i < nbParams; ++i) {
            for (int j = 0; j < nbParams; ++j) {
                physicalCovarianceMatrix.setEntry(i, j,
                                               normalizedCovarianceMatrix.getEntry(i, j) *
                                               (scale[i] * scale[j]));
            }
        }
        return physicalCovarianceMatrix;
    }
}
