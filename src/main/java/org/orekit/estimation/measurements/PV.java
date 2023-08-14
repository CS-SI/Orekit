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
package org.orekit.estimation.measurements;

import java.util.Collections;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a position-velocity measurement.
 * <p>
 * For position-only measurement see {@link Position}.
 * </p>
 * @see Position
 * @author Luc Maisonobe
 * @since 8.0
 */
public class PV extends AbstractMeasurement<PV> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "PV";

    /** Identity matrix, for states derivatives. */
    private static final double[][] IDENTITY = new double[][] {
        {
            1, 0, 0, 0, 0, 0
        }, {
            0, 1, 0, 0, 0, 0
        }, {
            0, 0, 1, 0, 0, 0
        }, {
            0, 0, 0, 1, 0, 0
        }, {
            0, 0, 0, 0, 1, 0
        }, {
            0, 0, 0, 0, 0, 1
        }
    };

    /** Covariance matrix of the PV measurement (size 6x6). */
    private final double[][] covarianceMatrix;

    /** Constructor with two double for the standard deviations.
     * <p>The first double is the position's standard deviation, common to the 3 position's components.
     * The second double is the position's standard deviation, common to the 3 position's components.</p>
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition theoretical standard deviation on position components
     * @param sigmaVelocity theoretical standard deviation on velocity components
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double sigmaPosition, final double sigmaVelocity, final double baseWeight,
              final ObservableSatellite satellite) {
        this(date, position, velocity,
             new double[] {
                 sigmaPosition,
                 sigmaPosition,
                 sigmaPosition,
                 sigmaVelocity,
                 sigmaVelocity,
                 sigmaVelocity
             }, baseWeight, satellite);
    }

    /** Constructor with two vectors for the standard deviations.
     * <p>One 3-sized vectors for position standard deviations.
     * One 3-sized vectors for velocity standard deviations.
     * The 3-sized vectors are the square root of the diagonal elements of the covariance matrix.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition 3-sized vector of the standard deviations of the position
     * @param sigmaVelocity 3-sized vector of the standard deviations of the velocity
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[] sigmaPosition, final double[] sigmaVelocity,
              final double baseWeight, final ObservableSatellite satellite) {
        this(date, position, velocity,
             buildPvCovarianceMatrix(sigmaPosition, sigmaVelocity),
             baseWeight, satellite);
    }

    /** Constructor with one vector for the standard deviations.
     * <p>The 6-sized vector is the square root of the diagonal elements of the covariance matrix.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPV 6-sized vector of the standard deviations
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[] sigmaPV, final double baseWeight, final ObservableSatellite satellite) {
        this(date, position, velocity, buildPvCovarianceMatrix(sigmaPV), baseWeight, satellite);
    }

    /**
     * Constructor with 2 smaller covariance matrices.
     * <p>One 3x3 covariance matrix for position and one 3x3 covariance matrix for velocity.
     * The fact that the covariance matrices are symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param positionCovarianceMatrix 3x3 covariance matrix of the position
     * @param velocityCovarianceMatrix 3x3 covariance matrix of the velocity
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[][] positionCovarianceMatrix, final double[][] velocityCovarianceMatrix,
              final double baseWeight, final ObservableSatellite satellite) {
        this(date, position, velocity,
             buildPvCovarianceMatrix(positionCovarianceMatrix, velocityCovarianceMatrix),
             baseWeight, satellite);
    }

    /** Constructor with full covariance matrix and all inputs.
     * <p>The fact that the covariance matrix is symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param covarianceMatrix 6x6 covariance matrix of the PV measurement
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[][] covarianceMatrix, final double baseWeight, final ObservableSatellite satellite) {
        super(date,
              new double[] {
                  position.getX(), position.getY(), position.getZ(),
                  velocity.getX(), velocity.getY(), velocity.getZ()
              }, extractSigmas(covarianceMatrix),
              new double[] {
                  baseWeight, baseWeight, baseWeight,
                  baseWeight, baseWeight, baseWeight
              }, Collections.singletonList(satellite));
        this.covarianceMatrix = covarianceMatrix.clone();
    }

    /** Get the position.
     * @return position
     */
    public Vector3D getPosition() {
        final double[] pv = getObservedValue();
        return new Vector3D(pv[0], pv[1], pv[2]);
    }

    /** Get the velocity.
     * @return velocity
     */
    public Vector3D getVelocity() {
        final double[] pv = getObservedValue();
        return new Vector3D(pv[3], pv[4], pv[5]);
    }

    /** Get the covariance matrix.
     * @return the covariance matrix
     */
    public double[][] getCovarianceMatrix() {
        return covarianceMatrix.clone();
    }

    /** Get the correlation coefficients matrix.
     * <p>This is the 6x6 matrix M such that:
     * <p>Mij = Pij/(σi.σj)
     * <p>Where:
     * <ul>
     * <li>P is the covariance matrix
     * <li>σi is the i-th standard deviation (σi² = Pii)
     * </ul>
     * @return the correlation coefficient matrix (6x6)
     */
    public double[][] getCorrelationCoefficientsMatrix() {

        // Get the standard deviations
        final double[] sigmas = getTheoreticalStandardDeviation();

        // Initialize the correlation coefficients matric to the covariance matrix
        final double[][] corrCoefMatrix = new double[sigmas.length][sigmas.length];

        // Divide by the standard deviations
        for (int i = 0; i < sigmas.length; i++) {
            for (int j = 0; j < sigmas.length; j++) {
                corrCoefMatrix[i][j] = covarianceMatrix[i][j] / (sigmas[i] * sigmas[j]);
            }
        }
        return corrCoefMatrix;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<PV> theoreticalEvaluationWithoutDerivatives(final int iteration, final int evaluation,
                                                                                   final SpacecraftState[] states) {

        // PV value
        final TimeStampedPVCoordinates pv = states[0].getPVCoordinates();

        // prepare the evaluation
        final EstimatedMeasurementBase<PV> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation, states,
                                                       new TimeStampedPVCoordinates[] {
                                                           pv
                                                       });

        estimated.setEstimatedValue(new double[] {
            pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ(),
            pv.getVelocity().getX(), pv.getVelocity().getY(), pv.getVelocity().getZ()
        });

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<PV> theoreticalEvaluation(final int iteration, final int evaluation,
                                                             final SpacecraftState[] states) {

        // PV value
        final TimeStampedPVCoordinates pv = states[0].getPVCoordinates();

        // prepare the evaluation
        final EstimatedMeasurement<PV> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation, states,
                                                   new TimeStampedPVCoordinates[] {
                                                       pv
                                                   });

        estimated.setEstimatedValue(new double[] {
            pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ(),
            pv.getVelocity().getX(), pv.getVelocity().getY(), pv.getVelocity().getZ()
        });

        // partial derivatives with respect to state
        estimated.setStateDerivatives(0, IDENTITY);

        return estimated;
    }

    /** Extract standard deviations from a 6x6 PV covariance matrix.
     * Check the size of the PV covariance matrix first.
     * @param pvCovarianceMatrix the 6x6 PV covariance matrix
     * @return the standard deviations (6-sized vector), they are
     * the square roots of the diagonal elements of the covariance matrix in input.
     */
    private static double[] extractSigmas(final double[][] pvCovarianceMatrix) {

        // Check the size of the covariance matrix, should be 6x6
        if (pvCovarianceMatrix.length != 6 || pvCovarianceMatrix[0].length != 6) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                      pvCovarianceMatrix.length, pvCovarianceMatrix[0],
                                      6, 6);
        }

        // Extract the standard deviations (square roots of the diagonal elements)
        final double[] sigmas = new double[6];
        for (int i = 0; i < sigmas.length; i++) {
            sigmas[i] = FastMath.sqrt(pvCovarianceMatrix[i][i]);
        }
        return sigmas;
    }

    /** Build a 6x6 PV covariance matrix from two 3x3 matrices (covariances in position and velocity).
     * Check the size of the matrices first.
     * @param positionCovarianceMatrix the 3x3 covariance matrix in position
     * @param velocityCovarianceMatrix the 3x3 covariance matrix in velocity
     * @return the 6x6 PV covariance matrix
     */
    private static double[][] buildPvCovarianceMatrix(final double[][] positionCovarianceMatrix,
                                                      final double[][] velocityCovarianceMatrix) {
        // Check the sizes of the matrices first
        if (positionCovarianceMatrix.length != 3 || positionCovarianceMatrix[0].length != 3) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                      positionCovarianceMatrix.length, positionCovarianceMatrix[0],
                                      3, 3);
        }
        if (velocityCovarianceMatrix.length != 3 || velocityCovarianceMatrix[0].length != 3) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                      velocityCovarianceMatrix.length, velocityCovarianceMatrix[0],
                                      3, 3);
        }

        // Build the PV 6x6 covariance matrix
        final double[][] pvCovarianceMatrix = new double[6][6];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                pvCovarianceMatrix[i][j]         = positionCovarianceMatrix[i][j];
                pvCovarianceMatrix[i + 3][j + 3] = velocityCovarianceMatrix[i][j];
            }
        }
        return pvCovarianceMatrix;
    }

    /** Build a 6x6 PV covariance matrix from a 6-sized vector (position and velocity standard deviations).
     * Check the size of the vector first.
     * @param sigmaPV 6-sized vector with position standard deviations on the first 3 elements
     * and velocity standard deviations on the last 3 elements
     * @return the 6x6 PV covariance matrix
     */
    private static double[][] buildPvCovarianceMatrix(final double[] sigmaPV) {
        // Check the size of the vector first
        if (sigmaPV.length != 6) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH, sigmaPV.length, 6);

        }

        // Build the PV 6x6 covariance matrix
        final double[][] pvCovarianceMatrix = new double[6][6];
        for (int i = 0; i < sigmaPV.length; i++) {
            pvCovarianceMatrix[i][i] =  sigmaPV[i] * sigmaPV[i];
        }
        return pvCovarianceMatrix;
    }

    /** Build a 6x6 PV covariance matrix from two 3-sized vectors (position and velocity standard deviations).
     * Check the sizes of the vectors first.
     * @param sigmaPosition standard deviations of the position (3-size vector)
     * @param sigmaVelocity standard deviations of the velocity (3-size vector)
     * @return the 6x6 PV covariance matrix
     */
    private static double[][] buildPvCovarianceMatrix(final double[] sigmaPosition,
                                                      final double[] sigmaVelocity) {

        // Check the sizes of the vectors first
        if (sigmaPosition.length != 3) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH, sigmaPosition.length, 3);

        }
        if (sigmaVelocity.length != 3) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH, sigmaVelocity.length, 3);

        }

        // Build the PV 6x6 covariance matrix
        final double[][] pvCovarianceMatrix = new double[6][6];
        for (int i = 0; i < sigmaPosition.length; i++) {
            pvCovarianceMatrix[i][i]         =  sigmaPosition[i] * sigmaPosition[i];
            pvCovarianceMatrix[i + 3][i + 3] =  sigmaVelocity[i] * sigmaVelocity[i];
        }
        return pvCovarianceMatrix;
    }
}
