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

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.frames.LOFType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;

/** Provider for a temporal evolution of the process noise matrix.
 * All parameters (orbital or propagation) are time dependent and provided as {@link UnivariateFunction}.
 * The argument of the functions is a duration in seconds (between current and previous spacecraft state).
 * The output of the functions must be of the dimension of a standard deviation.
 * The method {@link #getProcessNoiseMatrix} then square the values so that they are consistent with a covariance matrix.
 * <p>
 * The orbital parameters evolutions are provided in LOF frame and Cartesian (PV);
 * then converted in inertial frame and current {@link OrbitType} and {@link PositionAngle}
 * when method {@link #getProcessNoiseMatrix} is called.
 * </p>
 * <p>
 * The time-dependent process noise matrix output is diagonal.
 * Each diagonal element is a function that represents the temporal evolution of (the standard deviation of) the process noise model.
 * The orbital parameters evolution is given in LOF frame and represents the model error
 * for the position and velocity in LOF (always 6 parameters).
 * The propagation parameters are not associated to a specific frame.
 * </p>
 * <p>
 * This class always provides one initial noise matrix or initial covariance matrix and one process noise matrix.
 * </p>
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.2
 */
public class UnivariateProcessNoise extends AbstractCovarianceMatrixProvider {

    /** LOF used. */
    private final LOFType lofType;

    /** Position angle for the orbital process noise matrix. */
    private final PositionAngle positionAngle;

    /** Array of univariate functions for the six orbital parameters process noise evolution in LOF frame and Cartesian formalism. */
    private final UnivariateFunction[] lofCartesianOrbitalParametersEvolution;

    /** Array of univariate functions for the propagation parameters process noise evolution. */
    private final UnivariateFunction[] propagationParametersEvolution;

    /** Simple constructor.
     * @param initialCovarianceMatrix initial covariance matrix
     * @param lofType the LOF type used
     * @param positionAngle the position angle used for the computation of the process noise
     * @param lofCartesianOrbitalParametersEvolution Array of univariate functions for the six orbital parameters process noise evolution in LOF frame and Cartesian orbit type
     * @param propagationParametersEvolution Array of univariate functions for the propagation parameters process noise evolution
     * @throws OrekitException if lofOrbitalParametersEvolution array size is different from 6
     */
    public UnivariateProcessNoise(final RealMatrix initialCovarianceMatrix,
                                     final LOFType lofType,
                                     final PositionAngle positionAngle,
                                     final UnivariateFunction[] lofCartesianOrbitalParametersEvolution,
                                     final UnivariateFunction[] propagationParametersEvolution)
        throws OrekitException {
        super(initialCovarianceMatrix);
        this.lofType = lofType;
        this.positionAngle = positionAngle;
        this.lofCartesianOrbitalParametersEvolution  = lofCartesianOrbitalParametersEvolution;
        this.propagationParametersEvolution = propagationParametersEvolution;

        // Ensure that the orbital evolution array size is 6
        if (lofCartesianOrbitalParametersEvolution.length != 6) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                      lofCartesianOrbitalParametersEvolution, 6);
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getProcessNoiseMatrix(final SpacecraftState previous,
                                            final SpacecraftState current) {

        // Orbital parameters process noise matrix in inertial frame and current orbit type
        final RealMatrix inertialOrbitalProcessNoiseMatrix = getInertialOrbitalProcessNoiseMatrix(previous, current);

        if (propagationParametersEvolution.length == 0) {

            // no propagation parameters contribution, just return the orbital part
            return inertialOrbitalProcessNoiseMatrix;

        } else {

            // Propagation parameters process noise matrix
            final RealMatrix propagationProcessNoiseMatrix = getPropagationProcessNoiseMatrix(previous, current);

            // Concatenate the matrices
            final int        orbitalMatrixSize     = lofCartesianOrbitalParametersEvolution.length;
            final int        propagationMatrixSize = propagationParametersEvolution.length;
            final RealMatrix processNoiseMatrix    = MatrixUtils.createRealMatrix(orbitalMatrixSize + propagationMatrixSize,
                                                                                  orbitalMatrixSize + propagationMatrixSize);
            processNoiseMatrix.setSubMatrix(inertialOrbitalProcessNoiseMatrix.getData(), 0, 0);
            processNoiseMatrix.setSubMatrix(propagationProcessNoiseMatrix.getData(), orbitalMatrixSize, orbitalMatrixSize);
            return processNoiseMatrix;

        }
    }

    /** Get the process noise for the six orbital parameters in current spacecraft inertial frame and current orbit type.
     * @param previous previous state
     * @param current current state
     * @return physical (i.e. non normalized) orbital process noise matrix in inertial frame
     * @exception OrekitException if matrix cannot be computed
     */
    private RealMatrix getInertialOrbitalProcessNoiseMatrix(final SpacecraftState previous,
                                                            final SpacecraftState current) {

        // ΔT = duration in seconds from previous to current spacecraft state
        final double deltaT = current.getDate().durationFrom(previous.getDate());

        // Evaluate the functions, using ΔT as argument
        final int      lofOrbitalprocessNoiseLength = lofCartesianOrbitalParametersEvolution.length;
        final double[] lofOrbitalProcessNoiseValues = new double[lofOrbitalprocessNoiseLength];

        // The function return a value which dimension is that of a standard deviation
        // It needs to be squared before being put in the process noise covariance matrix
        for (int i = 0; i < lofOrbitalprocessNoiseLength; i++) {
            final double functionValue =  lofCartesianOrbitalParametersEvolution[i].value(deltaT);
            lofOrbitalProcessNoiseValues[i] = functionValue * functionValue;
        }

        // Form the diagonal matrix in LOF frame and Cartesian formalism
        final RealMatrix lofCartesianProcessNoiseMatrix = MatrixUtils.createRealDiagonalMatrix(lofOrbitalProcessNoiseValues);

        // Get the rotation matrix from LOF to inertial frame
        final double[][] lofToInertialRotation =
                        lofType.transformFromInertial(current.getDate(), current.getPVCoordinates()).
                        getInverse().getRotation().getMatrix();
        // FIXME: Check the order of the rotation (transpose or not ??)

        // Jacobian from LOF to inertial frame
        final RealMatrix jacLofToInertial = MatrixUtils.createRealMatrix(6, 6);
        jacLofToInertial.setSubMatrix(lofToInertialRotation, 0, 0);
        jacLofToInertial.setSubMatrix(lofToInertialRotation, 3, 3);

        // Jacobian from orbit parameters to Cartesian parameters
        final double[][] dYdC = new double[6][6];
        current.getOrbit().getJacobianWrtCartesian(positionAngle, dYdC);
        final RealMatrix jacParameterstoCartesian = MatrixUtils.createRealMatrix(dYdC);

        // Complete Jacobian of the transformation
        final RealMatrix jacobian = jacParameterstoCartesian.multiply(jacLofToInertial);

        // Return the orbital process noise matrix in inertial frame and proper orbit type
        final RealMatrix inertialOrbitalProcessNoiseMatrix = jacobian.
                         multiply(lofCartesianProcessNoiseMatrix).
                         multiply(jacobian.transpose());

        return inertialOrbitalProcessNoiseMatrix;
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
        final int      propagationProcessNoiseLength = lofCartesianOrbitalParametersEvolution.length;
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

}
