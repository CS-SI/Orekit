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

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.LOFType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.StateCovariance;

/** Provider for a temporal evolution of the process noise matrix.
 * All parameters (orbital or propagation) are time dependent and provided as {@link UnivariateFunction}.
 * The argument of the functions is a duration in seconds (between current and previous spacecraft state).
 * The output of the functions must be of the dimension of a standard deviation.
 * The method {@link #getProcessNoiseMatrix} then square the values so that they are consistent with a covariance matrix.
 * <p>
 * The orbital parameters evolutions are provided in LOF frame and Cartesian (PV);
 * then converted in inertial frame and current {@link org.orekit.orbits.OrbitType} and {@link PositionAngleType}
 * when method {@link #getProcessNoiseMatrix} is called.
 * </p>
 * <p>
 * The time-dependent functions define a process noise matrix that is diagonal
 * <em>in the Local Orbital Frame</em>, corresponds to Cartesian elements, abd represents
 * the temporal evolution of (the standard deviation of) the process noise model. The
 * first function is therefore the standard deviation along the LOF X axis, the second
 * function represents the standard deviation along the LOF Y axis...
 * This allows to set up simply a process noise representing an uncertainty that grows
 * mainly along the track. The 6x6 upper left part of output matrix will however not be
 * diagonal as it will be converted to the same inertial frame and orbit type as the
 * {@link SpacecraftState state} used by the {@link KalmanEstimator Kalman estimator}.
 * </p>
 * <p>
 * The propagation and measurements parameters are not associated to a specific frame and
 * are appended as is in the lower right part diagonal of the output matrix. This implies
 * this simplified model does not include correlation between the parameters and the orbit,
 * but only evolution of the parameters themselves. If such correlations are needed, users
 * must set up a custom {@link CovarianceMatrixProvider covariance matrix provider}. In most
 * cases, the parameters are constant and their evolution noise is always 0, so the
 * functions can be set to {@code x -> 0}.
 * </p>
 * <p>
 * This class always provides one initial noise matrix or initial covariance matrix and one process noise matrix.
 * </p>
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.2
 */
public class UnivariateProcessNoise extends AbstractCovarianceMatrixProvider {

    /** Local Orbital Frame (LOF) type used. */
    private final LOFType lofType;

    /** Position angle for the orbital process noise matrix. */
    private final PositionAngleType positionAngleType;

    /** Array of univariate functions for the six orbital parameters process noise evolution in LOF frame and Cartesian formalism. */
    private final UnivariateFunction[] lofCartesianOrbitalParametersEvolution;

    /** Array of univariate functions for the propagation parameters process noise evolution. */
    private final UnivariateFunction[] propagationParametersEvolution;

    /** Array of univariate functions for the measurements parameters process noise evolution. */
    private final UnivariateFunction[] measurementsParametersEvolution;

    /** Simple constructor.
     * @param initialCovarianceMatrix initial covariance matrix
     * @param lofType the LOF type used
     * @param positionAngleType the position angle used for the computation of the process noise
     * @param lofCartesianOrbitalParametersEvolution Array of univariate functions for the six orbital parameters process noise evolution in LOF frame and Cartesian orbit type
     * @param propagationParametersEvolution Array of univariate functions for the propagation parameters process noise evolution
     */
    public UnivariateProcessNoise(final RealMatrix initialCovarianceMatrix,
                                  final LOFType lofType,
                                  final PositionAngleType positionAngleType,
                                  final UnivariateFunction[] lofCartesianOrbitalParametersEvolution,
                                  final UnivariateFunction[] propagationParametersEvolution) {

        // Call the new constructor with an empty array for measurements parameters
        this(initialCovarianceMatrix, lofType, positionAngleType, lofCartesianOrbitalParametersEvolution, propagationParametersEvolution, new UnivariateFunction[0]);
    }

    /** Simple constructor.
     * @param initialCovarianceMatrix initial covariance matrix
     * @param lofType the LOF type used
     * @param positionAngleType the position angle used for the computation of the process noise
     * @param lofCartesianOrbitalParametersEvolution Array of univariate functions for the six orbital parameters process noise evolution in LOF frame and Cartesian orbit type
     * @param propagationParametersEvolution Array of univariate functions for the propagation parameters process noise evolution
     * @param measurementsParametersEvolution Array of univariate functions for the measurements parameters process noise evolution
     * @since 10.3
     */
    public UnivariateProcessNoise(final RealMatrix initialCovarianceMatrix,
                                  final LOFType lofType,
                                  final PositionAngleType positionAngleType,
                                  final UnivariateFunction[] lofCartesianOrbitalParametersEvolution,
                                  final UnivariateFunction[] propagationParametersEvolution,
                                  final UnivariateFunction[] measurementsParametersEvolution) {

        super(initialCovarianceMatrix);
        this.lofType = lofType;
        this.positionAngleType = positionAngleType;
        this.lofCartesianOrbitalParametersEvolution  = lofCartesianOrbitalParametersEvolution.clone();
        this.propagationParametersEvolution = propagationParametersEvolution.clone();
        this.measurementsParametersEvolution = measurementsParametersEvolution.clone();
    }

    /** Getter for the lofType.
     * @return the lofType
     */
    public LOFType getLofType() {
        return lofType;
    }

    /** Getter for the positionAngle.
     * @return the positionAngle
     */
    public PositionAngleType getPositionAngleType() {
        return positionAngleType;
    }

    /** Getter for the lofCartesianOrbitalParametersEvolution.
     * @return the lofCartesianOrbitalParametersEvolution
     */
    public UnivariateFunction[] getLofCartesianOrbitalParametersEvolution() {
        return lofCartesianOrbitalParametersEvolution.clone();
    }

    /** Getter for the propagationParametersEvolution.
     * @return the propagationParametersEvolution
     */
    public UnivariateFunction[] getPropagationParametersEvolution() {
        return propagationParametersEvolution.clone();
    }

    /** Getter for the measurementsParametersEvolution.
     * @return the measurementsParametersEvolution
     */
    public UnivariateFunction[] getMeasurementsParametersEvolution() {
        return measurementsParametersEvolution.clone();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getProcessNoiseMatrix(final SpacecraftState previous,
                                            final SpacecraftState current) {

        // Number of estimated parameters
        final int nbOrb    = lofCartesianOrbitalParametersEvolution.length;
        final int nbPropag = propagationParametersEvolution.length;
        final int nbMeas   = measurementsParametersEvolution.length;

        // Initialize process noise matrix
        final RealMatrix processNoiseMatrix = MatrixUtils.createRealMatrix(nbOrb + nbPropag + nbMeas,
                                                                           nbOrb + nbPropag + nbMeas);

        // Orbital parameters
        if (nbOrb != 0) {
            // Orbital parameters process noise matrix in inertial frame and current orbit type
            final RealMatrix inertialOrbitalProcessNoiseMatrix = getInertialOrbitalProcessNoiseMatrix(previous, current);
            processNoiseMatrix.setSubMatrix(inertialOrbitalProcessNoiseMatrix.getData(), 0, 0);
        }

        // Propagation parameters
        if (nbPropag != 0) {
            // Propagation parameters process noise matrix
            final RealMatrix propagationProcessNoiseMatrix = getPropagationProcessNoiseMatrix(previous, current);
            processNoiseMatrix.setSubMatrix(propagationProcessNoiseMatrix.getData(), nbOrb, nbOrb);
        }

        // Measurement parameters
        if (nbMeas != 0) {
            // Measurement parameters process noise matrix
            final RealMatrix measurementsProcessNoiseMatrix = getMeasurementsProcessNoiseMatrix(previous, current);
            processNoiseMatrix.setSubMatrix(measurementsProcessNoiseMatrix.getData(), nbOrb + nbPropag, nbOrb + nbPropag);
        }

        return processNoiseMatrix;
    }

    /** Get the process noise for the six orbital parameters in current spacecraft inertial frame and current orbit type.
     * @param previous previous state
     * @param current current state
     * @return physical (i.e. non normalized) orbital process noise matrix in inertial frame
     */
    private RealMatrix getInertialOrbitalProcessNoiseMatrix(final SpacecraftState previous,
                                                            final SpacecraftState current) {

        // ΔT = duration in seconds from previous to current spacecraft state
        final double deltaT = current.getDate().durationFrom(previous.getDate());

        // Evaluate the functions, using ΔT as argument
        final int      lofOrbitalProcessNoiseLength = lofCartesianOrbitalParametersEvolution.length;
        final double[] lofOrbitalProcessNoiseValues = new double[lofOrbitalProcessNoiseLength];

        // The function return a value which dimension is that of a standard deviation
        // It needs to be squared before being put in the process noise covariance matrix
        for (int i = 0; i < lofOrbitalProcessNoiseLength; i++) {
            final double functionValue =  lofCartesianOrbitalParametersEvolution[i].value(deltaT);
            lofOrbitalProcessNoiseValues[i] = functionValue * functionValue;
        }

        // Form the diagonal matrix in LOF frame and Cartesian formalism
        final RealMatrix lofCartesianProcessNoiseMatrix = MatrixUtils.createRealDiagonalMatrix(lofOrbitalProcessNoiseValues);

        // Create state covariance object in LOF
        final StateCovariance lofCartesianProcessNoiseCov =
                        new StateCovariance(lofCartesianProcessNoiseMatrix, current.getDate(), lofType);

        // Convert to Cartesian in orbital frame
        final StateCovariance inertialCartesianProcessNoiseCov =
                        lofCartesianProcessNoiseCov.changeCovarianceFrame(current.getOrbit(), current.getFrame());

        // Convert to current orbit type and position angle
        final StateCovariance inertialOrbitalProcessNoiseCov =
                        inertialCartesianProcessNoiseCov.changeCovarianceType(current.getOrbit(),
                                                                              current.getOrbit().getType(), positionAngleType);
        // Return inertial orbital covariance matrix
        return inertialOrbitalProcessNoiseCov.getMatrix();
    }

    /** Get the process noise for the propagation parameters.
     * @param previous previous state
     * @param current current state
     * @return physical (i.e. non normalized) propagation process noise matrix
     */
    private RealMatrix getPropagationProcessNoiseMatrix(final SpacecraftState previous,
                                                        final SpacecraftState current) {

        // ΔT = duration from previous to current spacecraft state (in seconds)
        final double deltaT = current.getDate().durationFrom(previous.getDate());

        // Evaluate the functions, using ΔT as argument
        final int      propagationProcessNoiseLength = propagationParametersEvolution.length;
        final double[] propagationProcessNoiseValues = new double[propagationProcessNoiseLength];

        // The function return a value which dimension is that of a standard deviation
        // It needs to be squared before being put in the process noise covariance matrix
        for (int i = 0; i < propagationProcessNoiseLength; i++) {
            final double functionValue =  propagationParametersEvolution[i].value(deltaT);
            propagationProcessNoiseValues[i] = functionValue * functionValue;
        }

        // Form the diagonal matrix corresponding to propagation parameters process noise
        return MatrixUtils.createRealDiagonalMatrix(propagationProcessNoiseValues);
    }

    /** Get the process noise for the measurements parameters.
     * @param previous previous state
     * @param current current state
     * @return physical (i.e. non normalized) measurements process noise matrix
     */
    private RealMatrix getMeasurementsProcessNoiseMatrix(final SpacecraftState previous,
                                                         final SpacecraftState current) {

        // ΔT = duration from previous to current spacecraft state (in seconds)
        final double deltaT = current.getDate().durationFrom(previous.getDate());

        // Evaluate the functions, using ΔT as argument
        final int      measurementsProcessNoiseLength = measurementsParametersEvolution.length;
        final double[] measurementsProcessNoiseValues = new double[measurementsProcessNoiseLength];

        // The function return a value which dimension is that of a standard deviation
        // It needs to be squared before being put in the process noise covariance matrix
        for (int i = 0; i < measurementsProcessNoiseLength; i++) {
            final double functionValue =  measurementsParametersEvolution[i].value(deltaT);
            measurementsProcessNoiseValues[i] = functionValue * functionValue;
        }

        // Form the diagonal matrix corresponding to propagation parameters process noise
        return MatrixUtils.createRealDiagonalMatrix(measurementsProcessNoiseValues);
    }

}
