/* Copyright 2022-2026 Romain Serra
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

import java.util.Arrays;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Data container for measurement's expected quality (used in orbit determination).
 * @author Romain Serra
 * @since 14.0
 */
public class MeasurementQuality {

    /** Theoretical covariance matrix. */
    private final double[][] covarianceMatrix;

    /** Theoretical standard deviations (held separate for fast access). */
    private final double[] sigmas;

    /** Base weights. */
    private final double[] weights;

    /** Measurement dimension. */
    private final int measurementDimension;

    /**
     * Constructor with sigmas as input for unidimensional measurement and unit weight.
     * @param standardDeviation measurement standard deviation
     */
    public MeasurementQuality(final double standardDeviation) {
        this(standardDeviation, 1.);
    }

    /**
     * Constructor with sigmas as input and unit value for weights (assuming no correlation).
     * @param standardDeviations measurement standard deviations matrix
     */
    public MeasurementQuality(final double[] standardDeviations) {
        this(standardDeviations, 1.);
    }

    /**
     * Constructor with sigmas as input and same value for weights (assuming no correlation).
     * @param standardDeviations measurement standard deviations matrix
     * @param weight measurement component's weight (same for all)
     */
    public MeasurementQuality(final double[] standardDeviations, final double weight) {
        this(standardDeviations, buildWeights(weight, standardDeviations.length));
    }

    /**
     * Constructor with same value for weights.
     * @param covarianceMatrix measurement covariance matrix
     * @param weight measurement component's weight (same for all)
     */
    public MeasurementQuality(final double[][] covarianceMatrix, final double weight) {
        this(covarianceMatrix, buildWeights(weight, covarianceMatrix.length));
    }

    /**
     * Constructor for unidimensional measurement.
     * @param standardDeviation measurement standard deviation
     * @param weight measurement weight
     */
    public MeasurementQuality(final double standardDeviation, final double weight) {
        this.measurementDimension = 1;
        this.weights = new double[] {weight};
        this.sigmas = new double[] {standardDeviation};
        this.covarianceMatrix = new double[][] {new double[] {standardDeviation * standardDeviation}};
    }

    /**
     * Constructor with full covariance.
     * @param covarianceMatrix measurement covariance matrix
     * @param weights measurement component's weights
     */
    public MeasurementQuality(final double[][] covarianceMatrix, final double[] weights) {
        if (covarianceMatrix.length != weights.length) {
            throw new OrekitException(OrekitMessages.WRONG_MEASUREMENT_COVARIANCE_DIMENSION, covarianceMatrix.length, weights.length);
        } else if (covarianceMatrix.length != covarianceMatrix[0].length) {
            throw new OrekitException(OrekitMessages.COVARIANCE_MUST_BE_SQUARE);
        }
        this.measurementDimension = covarianceMatrix.length;
        this.weights = weights.clone();
        this.covarianceMatrix = new double[measurementDimension][];
        this.sigmas = new double[measurementDimension];
        for (int i = 0; i < measurementDimension; i++) {
            this.covarianceMatrix[i] = covarianceMatrix[i].clone();
            this.sigmas[i] = FastMath.sqrt(covarianceMatrix[i][i]);
        }
    }

    /**
     * Constructor with sigmas.
     * @param standardDeviations measurement standard deviations
     * @param weights measurement component's weights
     */
    public MeasurementQuality(final double[] standardDeviations, final double[] weights) {
        if (standardDeviations.length != weights.length) {
            throw new OrekitException(OrekitMessages.WRONG_MEASUREMENT_COVARIANCE_DIMENSION, standardDeviations.length,
                    weights.length);
        }
        this.measurementDimension = standardDeviations.length;
        this.weights = weights.clone();
        this.sigmas = standardDeviations.clone();
        this.covarianceMatrix = new double[measurementDimension][measurementDimension];
        for (int i = 0; i < sigmas.length; i++) {
            covarianceMatrix[i][i] = sigmas[i] * sigmas[i];
        }
    }

    /**
     * Build array of weights.
     * @param weight value to use for all weights
     * @param dimension total dimension
     * @return weights
     */
    private static double[] buildWeights(final double weight, final int dimension) {
        final double[] weights = new double[dimension];
        Arrays.fill(weights, weight);
        return weights;
    }

    /**
     * Getter for the measurement dimension.
     * @return dimension
     */
    public int getDimension() {
        return measurementDimension;
    }

    /**
     * Getter for the measurement covariance matrix.
     * @return covariance
     */
    public RealMatrix getCovarianceMatrix() {
        final double[][] coefficients = new double[measurementDimension][measurementDimension];
        for (int i = 0; i < measurementDimension; i++) {
            coefficients[i] = covarianceMatrix[i].clone();
        }
        return MatrixUtils.createRealMatrix(coefficients);
    }

    /**
     * Getter for the standard deviations a.k.a. sigmas for each component of the measurement.
     * @return standard deviations
     */
    public double[] getStandardDeviations() {
        return sigmas.clone();
    }

    /** Get the correlation coefficients matrix.
     * <p>This is the square, symmetric matrix M such that:
     * <p>Mij = Pij/(σi.σj)
     * <p>Where:
     * <ul>
     * <li>P is the covariance matrix
     * <li>σi is the i-th standard deviation (σi² = Pii)
     * </ul>
     * @return the correlation coefficient matrix
     */
    public RealMatrix getCorrelationMatrix() {
        // Initialize the correlation coefficients matric to the covariance matrix
        final double[][] corrCoefMatrix = new double[measurementDimension][measurementDimension];

        // Divide by the standard deviations
        for (int i = 0; i < corrCoefMatrix.length; i++) {
            for (int j = 0; j < corrCoefMatrix[0].length; j++) {
                corrCoefMatrix[i][j] = covarianceMatrix[i][j] / (sigmas[i] * sigmas[j]);
            }
        }
        return MatrixUtils.createRealMatrix(corrCoefMatrix);
    }

    /**
     * Getter for the weights corresponding to each component of the measurement.
     * @return weights
     */
    public double[] getWeights() {
        return weights.clone();
    }

}
