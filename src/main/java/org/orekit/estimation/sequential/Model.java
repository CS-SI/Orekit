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
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
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
class Model implements KalmanEstimation, NonLinearProcess<MeasurementDecorator> {

    /** Builder for propagator. */
    private final NumericalPropagatorBuilder builder;

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

    /** Propagator for the reference trajectory, up to current date. */
    private NumericalPropagator referenceTrajectory;

    /** Mapper for extracting Jacobians from integrated state. */
    private JacobiansMapper jacobiansMapper;

    /** Process noise matrix.
     * Second moment of the process noise. Often named Q.
     */
    private RealMatrix processNoiseMatrix;

    /** Initial estimate. */
    private final ProcessEstimate initialEstimate;

    /** Current number of measurement. */
    private int currentMeasurementNumber;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Predicted state. */
    private RealVector predictedState;

    /** Corrected state. */
    private RealVector correctedState;

    /** Predicted spacecraft states. */
    private SpacecraftState[] predictedSpacecraftStates;

    /** Corrected spacecraft states. */
    private SpacecraftState[] correctedSpacecraftStates;

    /** Corrected covariance. */
    private RealMatrix correctedCovariance;

    /** Predicted measurement. */
    private EstimatedMeasurement<?> predictedMeasurement;

    /** Corrected measurement. */
    private EstimatedMeasurement<?> correctedMeasurement;

    /** Kalman process model constructor (package private).
     * @param propagatorBuilder propagator builder used to evaluate the orbit.
     * @param estimatedOrbitalParameters orbital parameters to estimate
     * @param estimatedPropagationParameters propagation parameters to estimate
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param physicalInitialCovariance "Physical" initial covariance matrix (i.e. not normalized)
     * @param physicalProcessNoiseMatrix "Physical" process noise matrix (i.e.not normalized)
     * @throws OrekitException propagation exception.
     */
    Model(final NumericalPropagatorBuilder propagatorBuilder,
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
        final int m = nbOrbitalParameters + nbPropagationParameters + nbMeasurementsParameters;
        // Covariance
        if (physicalInitialCovariance.getColumnDimension() != m) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                      physicalInitialCovariance.getColumnDimension(),
                                      m);
        }
        if (physicalInitialCovariance.getRowDimension() != m) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                      physicalInitialCovariance.getRowDimension(),
                                      m);
        }
        // Process noise
        if (physicalProcessNoiseMatrix.getColumnDimension() != m) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                      physicalInitialCovariance.getColumnDimension(),
                                      m);
        }
        if (physicalProcessNoiseMatrix.getRowDimension() != m) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                      physicalInitialCovariance.getRowDimension(),
                                      m);
        }

        // Initialize normalized estimated covariance matrix and process noise matrix
        this.processNoiseMatrix      = normalizeCovarianceMatrix(physicalProcessNoiseMatrix);

        // Build the reference propagator and add its partial derivatives equations implementation
        updateReferenceTrajectory(getEstimatedPropagator());

        // Initialize the estimated normalized state and fill its values
        correctedState      = new ArrayRealVector(nbOrbitalParameters + nbPropagationParameters + nbMeasurementsParameters);
        correctedCovariance = normalizeCovarianceMatrix(physicalInitialCovariance);

        int i = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            correctedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedPropagationParameters.getDrivers()) {
            correctedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            correctedState.setEntry(i++, driver.getNormalizedValue());
        }
        initialEstimate = new ProcessEstimate(0.0, correctedState, correctedCovariance);

        this.currentMeasurementNumber = 0;
        this.currentDate              = propagatorBuilder.getInitialOrbitDate();

        this.predictedSpacecraftStates = new SpacecraftState[1];
        this.correctedSpacecraftStates = new SpacecraftState[1];

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
        return unNormalizeStateVector(correctedState);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        return unNormalizeCovarianceMatrix(correctedCovariance);
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

    /** Get the normalized initial estimate.
     * @return normalized initial estimate
     */
    public ProcessEstimate getNormalizedInitialEstimate() {
        return initialEstimate;
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

    /** Get the normalized error state transition matrix (STM) from previous point to current point.
     * The STM contains the partial derivatives of current state with respect to previous state.
     * The  STM is an MxM matrix where M is the size of the state vector.
     * M = nbOrb + nbPropag + nbMeas
     * @param predictedSpacecraftState current spacecraft state
     * @return the normalized error state transition matrix
     * @throws OrekitException if Jacobians cannot be computed
     */
    private RealMatrix getErrorStateTransitionMatrix(final SpacecraftState predictedSpacecraftState)
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
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(nbOrbitalParameters +
                                                                    nbPropagationParameters +
                                                                    nbMeasurementsParameters);
        // Derivatives of the state vector with respect to initial state vector
        if (nbOrbitalParameters > 0) {
            final double[][] dYdY0 = new double[nbOrbitalParameters][nbOrbitalParameters];
            jacobiansMapper.getStateJacobian(predictedSpacecraftState, dYdY0 );

            // Fill upper left corner (dY/dY0)
            stm.setSubMatrix(dYdY0, 0, 0);
        }

        // Derivatives of the state vector with respect to propagation parameters
        if (nbPropagationParameters > 0) {
            final double[][] dYdPp  = new double[nbOrbitalParameters][nbPropagationParameters];
            jacobiansMapper.getParametersJacobian(predictedSpacecraftState, dYdPp);

            // Fill 1st row, 2nd column (dY/dPp)
            stm.setSubMatrix(dYdPp, 0, nbOrbitalParameters);
        }

        // Normalization of the STM
        // normalized(STM)ij = STMij*Sj/Si
        final double[] scales = getParametersScale();
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < scales.length; j++ ) {
                stm.setEntry(i, j, stm.getEntry(i, j) * scales[j] / scales[i]);
            }
        }

        // Return the error state transition matrix
        return stm;
    }

    /** Get the normalized measurement matrix H.
     * H contains the partial derivatives of the measurement with respect to the state.
     * H is an NxM matrix where N is the size of the measurement vector and M the size of the state vector.
     * @param predictedSpacecraftState the spacecraft state associated with the measurement
     * @return the normalized measurement matrix H
     * @throws OrekitException if Jacobians cannot be computed
     */
    private RealMatrix getMeasurementMatrix(final SpacecraftState predictedSpacecraftState)
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
        if (nbOrb > 0) {
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
        if (nbPropag > 0) {
            final double[][] aYPp  = new double[6][nbPropag];
            jacobiansMapper.getParametersJacobian(predictedSpacecraftState, aYPp);
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
        if (nbMeas > 0) {
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
    private void updateReferenceTrajectory(final NumericalPropagator propagator)
        throws OrekitException {

        // Update the reference trajectory propagator
        referenceTrajectory = propagator;

        // Link the partial derivatives to this new propagator
        final String equationName = KalmanEstimator.class.getName() + "-derivatives";
        final PartialDerivativesEquations pde = new PartialDerivativesEquations(equationName, referenceTrajectory);

        // Reset the Jacobians
        final SpacecraftState rawState = referenceTrajectory.getInitialState();
        final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);
        referenceTrajectory.resetInitialState(stateWithDerivatives);
        jacobiansMapper = pde.getMapper();

    }

    /** Gather the different scaling factors of the estimated parameters in an array.
     * @return the array of scales (i.e. scaling factors)
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

    /** Un-normalize a state vector.
     * A state vector S is of size M = nbOrb + nbPropag + nbMeas
     * For each parameter i the normalized value of the state vector is:
     * Sn[i] = S[i] / scale[i]
     * @param normalizedStateVector The normalized state vector in input
     * @return the "physical" state vector
     */
    private RealVector unNormalizeStateVector(final RealVector normalizedStateVector) {

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
    private RealMatrix normalizeCovarianceMatrix(final RealMatrix physicalCovarianceMatrix) {

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
    private RealMatrix unNormalizeCovarianceMatrix(final RealMatrix normalizedCovarianceMatrix) {

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
     * @throws OrekitException if modifier cannot be applied
     */
    private <T extends ObservedMeasurement<T>> void applyDynamicOutlierFilter(final EstimatedMeasurement<T> measurement,
                                                                              final RealMatrix innovationCovarianceMatrix)
        throws OrekitException {

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
                                           final MeasurementDecorator measurement)
        throws OrekitExceptionWrapper {
        try {

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
            // - N = size of the current measurement
            //  Example:
            //   * 1 for Range, RangeRate and TurnAroundRange
            //   * 2 for Angular (Azimuth/Elevation or Right-ascension/Declination)
            //   * 6 for Position/Velocity
            // - M = size of the state vector. N = nbOrb + nbPropag + nbMeas

            // Propagate the reference trajectory to measurement date
            predictedSpacecraftStates[0] = referenceTrajectory.propagate(observedMeasurement.getDate());

            // Predict the state vector (Mx1)
            predictState(predictedSpacecraftStates[0].getOrbit());

            // Get the error state transition matrix (MxM)
            final RealMatrix stateTransitionMatrix = getErrorStateTransitionMatrix(predictedSpacecraftStates[0]);

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
                                                                predictedSpacecraftStates);

            // Normalized measurement matrix (NxM)
            final RealMatrix measurementMatrix = getMeasurementMatrix(predictedSpacecraftStates[0]);

            return new NonLinearEvolution(measurement.getTime(),
                                          predictedState, stateTransitionMatrix, processNoiseMatrix,
                                          measurementMatrix);


        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final NonLinearEvolution evolution,
                                    final RealMatrix innovationCovarianceMatrix)
        throws OrekitExceptionWrapper {

        try {
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
        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }

    }

    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @exception OrekitException if measurement cannot be re-estimated from corrected state
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement)
        throws OrekitException {
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        updateParameters();

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final NumericalPropagator estimatedPropagator = getEstimatedPropagator();
        correctedSpacecraftStates[0] = estimatedPropagator.getInitialState();

        // Compute the estimated measurement using estimated spacecraft state
        correctedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            correctedSpacecraftStates);
        // Update the trajectory
        // ---------------------
        updateReferenceTrajectory(estimatedPropagator);

    }

    /** Set the predicted normalized state vector.
     * The predicted/propagated orbit is used to update the state vector
     * @param predictedOrbit the predicted orbit at measurement date
     * @throws OrekitException if the propagator builder could not be reset
     */
    private void predictState(final Orbit predictedOrbit)
        throws OrekitException {

        // First, update the builder with the predicted orbit
        // This updates the orbital drivers with the values of the predicted orbit
        builder.resetOrbit(predictedOrbit);

        // Predicted state is initialized to previous estimated state
        predictedState = correctedState.copy();

        // The orbital parameters in the state vector are replaced with their predicted values
        // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
        if (nbOrbitalParameters > 0) {
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction

            // Orbital parameters counter
            int jOrb = 0;
            for (DelegatingDriver orbitalDriver : builder.getOrbitalParametersDrivers().getDrivers()) {
                if (orbitalDriver.isSelected()) {
                    predictedState.setEntry(jOrb++, orbitalDriver.getNormalizedValue());
                }
            }
        }

    }

    /** Update the estimated parameters after the correction phase of the filter.
     * The min/max allowed values are handled by the parameter themselves.
     * @throws OrekitException if setting the normalized values failed
     */
    private void updateParameters() throws OrekitException {
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
