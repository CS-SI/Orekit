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
package org.orekit.estimation.measurements;

import java.util.Arrays;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a position-velocity state.
 * @author Luc Maisonobe
 * @since 8.0
 */
public class PV extends AbstractMeasurement<PV> {

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
     * The first double is the position's standard deviation, common to the 3 position's components.
     * The second double is the position's standard deviation, common to the 3 position's components.
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition theoretical standard deviation on position components
     * @param sigmaVelocity theoretical standard deviation on velocity components
     * @param baseWeight base weight
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double sigmaPosition, final double sigmaVelocity, final double baseWeight) {
        this(date, position, velocity, sigmaPosition, sigmaVelocity, baseWeight, 0);
    }

    /** Constructor with two double for the standard deviations.
     * The first double is the position's standard deviation, common to the 3 position's components.
     * The second double is the position's standard deviation, common to the 3 position's components.
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition theoretical standard deviation on position components
     * @param sigmaVelocity theoretical standard deviation on velocity components
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     * @since 9.0
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double sigmaPosition, final double sigmaVelocity, final double baseWeight,
              final int propagatorIndex) {
        this(date, position, velocity,
             new double[] {
                 sigmaPosition,
                 sigmaPosition,
                 sigmaPosition,
                 sigmaVelocity,
                 sigmaVelocity,
                 sigmaVelocity
             }, baseWeight, propagatorIndex);
    }

    /** Constructor with two vectors for the standard deviations and default value for propagator index..
     * One 3-sized vectors for position standard deviations.
     * One 3-sized vectors for velocity standard deviations.
     * The 3-sized vectors are the square root of the diagonal elements of the covariance matrix.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition 3-sized vector of the standard deviations of the position
     * @param sigmaVelocity 3-sized vector of the standard deviations of the velocity
     * @param baseWeight base weight
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[] sigmaPosition, final double[] sigmaVelocity, final double baseWeight) {
        this(date, position, velocity, sigmaPosition, sigmaVelocity, baseWeight, 0);
    }

    /** Constructor with two vectors for the standard deviations.
     * One 3-sized vectors for position standard deviations.
     * One 3-sized vectors for velocity standard deviations.
     * The 3-sized vectors are the square root of the diagonal elements of the covariance matrix.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition 3-sized vector of the standard deviations of the position
     * @param sigmaVelocity 3-sized vector of the standard deviations of the velocity
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[] sigmaPosition, final double[] sigmaVelocity,
              final double baseWeight, final int propagatorIndex) {
        this(date, position, velocity,
             new double[] {
                 sigmaPosition[0],
                 sigmaPosition[1],
                 sigmaPosition[2],
                 sigmaVelocity[0],
                 sigmaVelocity[1],
                 sigmaVelocity[2]
             }, baseWeight, propagatorIndex);
    }

    /** Constructor with one vector for the standard deviations and default value for propagator index.
     * The 6-sized vector is the square root of the diagonal elements of the covariance matrix.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPV 6-sized vector of the standard deviations
     * @param baseWeight base weight
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[] sigmaPV, final double baseWeight) {
        this(date, position, velocity, sigmaPV, baseWeight, 0);
    }

    /** Constructor with one vector for the standard deviations.
     * The 6-sized vector is the square root of the diagonal elements of the covariance matrix.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPV 6-sized vector of the standard deviations
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[] sigmaPV, final double baseWeight, final int propagatorIndex) {
        this(date, position, velocity,
             new double[][] {{
                     FastMath.pow(sigmaPV[0],  2.), 0., 0., 0., 0., 0.
                 }, {
                     0., FastMath.pow(sigmaPV[1],  2.), 0., 0., 0., 0.
                 }, {
                     0., 0., FastMath.pow(sigmaPV[2],  2.), 0., 0., 0.
                 }, {
                     0., 0., 0., FastMath.pow(sigmaPV[3],  2.), 0., 0.
                 }, {
                     0., 0., 0., 0., FastMath.pow(sigmaPV[4],  2.), 0.
                 }, {
                     0., 0., 0., 0., 0., FastMath.pow(sigmaPV[5],  2.)
                 }
             }, baseWeight, 0);
    }

    /**
     * Constructor with 2 smaller covariance matrices and default value for propagator index.
     * One 3x3 covariance matrix for position and one 3x3 covariance matrix for velocity.
     * The fact that the covariance matrices are symmetric and positive definite is not checked.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param positionCovarianceMatrix 3x3 covariance matrix of the position
     * @param velocityCovarianceMatrix 3x3 covariance matrix of the velocity
     * @param baseWeight base weight
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[][] positionCovarianceMatrix, final double[][] velocityCovarianceMatrix,
              final double baseWeight) {
        this(date, position, velocity, positionCovarianceMatrix, velocityCovarianceMatrix, baseWeight, 0);
    }

    /**
     * Constructor with 2 smaller covariance matrices.
     * One 3x3 covariance matrix for position and one 3x3 covariance matrix for velocity.
     * The fact that the covariance matrices are symmetric and positive definite is not checked.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param positionCovarianceMatrix 3x3 covariance matrix of the position
     * @param velocityCovarianceMatrix 3x3 covariance matrix of the velocity
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[][] positionCovarianceMatrix, final double[][] velocityCovarianceMatrix,
              final double baseWeight, final int propagatorIndex) {
        this(date, position, velocity,
             new double[][] {{
                     positionCovarianceMatrix[0][0], positionCovarianceMatrix[0][1], positionCovarianceMatrix[0][2], 0., 0., 0.
                 }, {
                     positionCovarianceMatrix[1][0], positionCovarianceMatrix[1][1], positionCovarianceMatrix[1][2], 0., 0., 0.
                 }, {
                     positionCovarianceMatrix[2][0], positionCovarianceMatrix[2][1], positionCovarianceMatrix[2][2], 0., 0., 0.
                 }, {
                     0., 0., 0., velocityCovarianceMatrix[0][0], velocityCovarianceMatrix[0][1], velocityCovarianceMatrix[0][2]
                 }, {
                     0., 0., 0., velocityCovarianceMatrix[1][0], velocityCovarianceMatrix[1][1], velocityCovarianceMatrix[1][2]
                 }, {
                     0., 0., 0., velocityCovarianceMatrix[2][0], velocityCovarianceMatrix[2][1], velocityCovarianceMatrix[2][2]
                 }
             }, baseWeight, 0);

        // Check the sizes of the matrices
        if (positionCovarianceMatrix[0].length != 3 || positionCovarianceMatrix[1].length != 3) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                                   positionCovarianceMatrix[0].length, positionCovarianceMatrix[1],
                                                   3, 3);
        }
        if (velocityCovarianceMatrix[0].length != 3 || velocityCovarianceMatrix[1].length != 3) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                                   velocityCovarianceMatrix[0].length, velocityCovarianceMatrix[1],
                                                   3, 3);
        }
    }

    /**
     * Constructor with full covariance matrix but default index for propagator.
     * The fact that the covariance matrix is symmetric and positive definite is not checked.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param covarianceMatrix 6x6 covariance matrix of the PV measurement
     * @param baseWeight base weight
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[][] covarianceMatrix, final double baseWeight) {
        this(date, position, velocity, covarianceMatrix, baseWeight, 0);
    }

    /** Constructor with full covariance matrix and all inputs.
     * The fact that the covariance matrix is symmetric and positive definite is not checked.
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param covarianceMatrix 6x6 covariance matrix of the PV measurement
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     * @since 9.2
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double[][] covarianceMatrix, final double baseWeight, final int propagatorIndex) {
        super(date,
              new double[] {
                  position.getX(), position.getY(), position.getZ(),
                  velocity.getX(), velocity.getY(), velocity.getZ()
              }, new double[] {
                  FastMath.sqrt(covarianceMatrix[0][0]),
                  FastMath.sqrt(covarianceMatrix[1][1]),
                  FastMath.sqrt(covarianceMatrix[2][2]),
                  FastMath.sqrt(covarianceMatrix[3][3]),
                  FastMath.sqrt(covarianceMatrix[4][4]),
                  FastMath.sqrt(covarianceMatrix[5][5])
              }, new double[] {
                  baseWeight, baseWeight, baseWeight,
                  baseWeight, baseWeight, baseWeight
              }, Arrays.asList(propagatorIndex));

        // Check the size of the covariance matrix, should be 6x6
        if (covarianceMatrix[0].length != 6 || covarianceMatrix[1].length != 6) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                                   covarianceMatrix[0].length, covarianceMatrix[1],
                                                   6, 6);
        }
        this.covarianceMatrix = covarianceMatrix;
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
        return covarianceMatrix;
    }

    /** Get the correlation coefficients matrix.
     * <br>This is the 6x6 matrix M such that:</br>
     * <br>Mij = Pij/(σi.σj)</br>
     * <br>Where: <ul>
     * <li> P is the covariance matrix
     * <li> σi is the i-th standard deviation (σi² = Pii)
     * </ul>
     * @return the correlation coefficient matrix (6x6)
     */
    public double[][] getCorrelationCoefficientsMatrix() {

        // Get the standard deviations
        final double[] sigmas = getTheoreticalStandardDeviation();

        // Initialize the correlation coefficients matric to the covariance matrix
        final double[][] corrCoefMatrix = covarianceMatrix.clone();

        // Divide by the standard deviations
        for (int i = 0; i < sigmas.length; i++) {
            for (int j = 0; j < sigmas.length; j++) {
                corrCoefMatrix[i][j] /= sigmas[i] * sigmas[j];
            }
        }
        return corrCoefMatrix;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<PV> theoreticalEvaluation(final int iteration, final int evaluation,
                                                             final SpacecraftState[] states)
        throws OrekitException {

        // PV value
        final TimeStampedPVCoordinates pv = states[getPropagatorsIndices().get(0)].getPVCoordinates();

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
}
