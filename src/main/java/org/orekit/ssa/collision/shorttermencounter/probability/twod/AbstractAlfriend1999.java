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
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.FieldLUDecomposition;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.MathArrays;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

/**
 * Abstract class for Alfriend1999 normal and maximised methods as they share lots of similarities.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public abstract class AbstractAlfriend1999 extends AbstractShortTermEncounter2DPOCMethod {

    /**
     * Default constructor.
     *
     * @param name name of the method
     */
    protected AbstractAlfriend1999(final String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    public ProbabilityOfCollision compute(final double xm, final double ym, final double sigmaX, final double sigmaY,
                                          final double radius) {
        // Reconstruct necessary values from inputs
        final double squaredMahalanobisDistance =
                ShortTermEncounter2DDefinition.computeSquaredMahalanobisDistance(xm, ym, sigmaX, sigmaY);

        // Compute covariance matrix determinant
        final double covarianceMatrixDeterminant = computeCovarianceMatrixDeterminant(sigmaX, sigmaY);

        // Compute probability of collision
        final double value = computeValue(radius, squaredMahalanobisDistance, covarianceMatrixDeterminant);

        return new ProbabilityOfCollision(value, getName(), this.isAMaximumProbabilityOfCollisionMethod());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final T xm, final T ym, final T sigmaX,
                                                                                      final T sigmaY,
                                                                                      final T radius) {
        // Reconstruct necessary values from inputs
        final T squaredMahalanobisDistance =
                FieldShortTermEncounter2DDefinition.computeSquaredMahalanobisDistance(xm, ym, sigmaX, sigmaY);

        // Compute covariance matrix determinant
        final T covarianceMatrixDeterminant = computeCovarianceMatrixDeterminant(sigmaX, sigmaY);

        // Compute probability of collision
        final T value = computeValue(radius, squaredMahalanobisDistance, covarianceMatrixDeterminant);

        return new FieldProbabilityOfCollision<>(value, getName(), this.isAMaximumProbabilityOfCollisionMethod());
    }

    /**
     * Compute the covariance matrix determinant.
     *
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     *
     * @return covariance matrix determinant
     */
    private double computeCovarianceMatrixDeterminant(final double sigmaX, final double sigmaY) {
        // Rebuild covariance matrix
        final RealMatrix covarianceMatrix = new BlockRealMatrix(new double[][] {
                { sigmaX * sigmaX, 0 },
                { 0, sigmaY * sigmaY }
        });

        // Compute determinant
        return new LUDecomposition(covarianceMatrix).getDeterminant();
    }

    /**
     * Compute the covariance matrix determinant.
     *
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param <T> type of the field elements
     *
     * @return covariance matrix determinant
     */
    private <T extends CalculusFieldElement<T>> T computeCovarianceMatrixDeterminant(final T sigmaX, final T sigmaY) {
        // Rebuild covariance matrix
        final T[][] covarianceMatrixData = MathArrays.buildArray(sigmaX.getField(), 2, 2);
        covarianceMatrixData[0][0] = sigmaX.multiply(sigmaX);
        covarianceMatrixData[1][1] = sigmaY.multiply(sigmaY);

        final FieldMatrix<T> covarianceMatrix = new BlockFieldMatrix<>(covarianceMatrixData);

        // Compute determinant
        return new FieldLUDecomposition<>(covarianceMatrix).getDeterminant();
    }

    /**
     * Compute the value of the probability of collision.
     *
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param squaredMahalanobisDistance squared Mahalanobis distance
     * @param covarianceMatrixDeterminant covariance matrix determinant
     *
     * @return value of the probability of collision
     */
    abstract double computeValue(double radius, double squaredMahalanobisDistance, double covarianceMatrixDeterminant);

    /**
     * Compute the value of the probability of collision.
     *
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param squaredMahalanobisDistance squared Mahalanobis distance
     * @param covarianceMatrixDeterminant covariance matrix determinant
     * @param <T> type of the field elements
     *
     * @return value of the probability of collision
     */
    abstract <T extends CalculusFieldElement<T>> T computeValue(T radius, T squaredMahalanobisDistance,
                                                                T covarianceMatrixDeterminant);
}
