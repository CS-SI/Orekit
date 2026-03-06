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

import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeasurementQualityTest {

    @Test
    void testGetWeights() {
        // GIVEN
        final double expectedWeight = 5.;
        // WHEN
        final MeasurementQuality measurementQuality = new MeasurementQuality(new double[2][2], expectedWeight);
        final double[] weights = measurementQuality.getWeights();
        // THEN
        assertEquals(2, measurementQuality.getDimension());
        assertEquals(2, weights.length);
        assertEquals(weights.length, measurementQuality.getStandardDeviations().length);
        for (int i = 0; i < measurementQuality.getWeights().length; i++) {
            assertEquals(expectedWeight, weights[i]);
        }
    }

    @Test
    void testGetWeights2() {
        // GIVEN
        final double[] sigmas = new double[] {2, 3, 4, 5, 6};
        final double expectedWeight = 5.;
        // WHEN
        final MeasurementQuality measurementQuality = new MeasurementQuality(sigmas, expectedWeight);
        final double[] weights = measurementQuality.getWeights();
        // THEN
        assertNotEquals(sigmas, measurementQuality.getStandardDeviations());  // check deep copy
        assertEquals(sigmas.length, weights.length);
        for (int i = 0; i < sigmas.length; i++) {
            assertEquals(expectedWeight, weights[i]);
        }
    }

    @Test
    void testUnidimensionalConstructor() {
        // GIVEN
        final double sigma = 1.;
        final double weight = 2.;
        // WHEN
        final MeasurementQuality measurementQuality =  new MeasurementQuality(sigma, weight);
        // THEN
        assertEquals(1, measurementQuality.getStandardDeviations().length);
        assertEquals(sigma, measurementQuality.getStandardDeviations()[0]);
        assertEquals(weight, measurementQuality.getWeights()[0]);
    }

    @Test
    void testConstructorMissedSizeCovarianceException() {
        // GIVEN
        final double[][] covariance = new double[2][1];
        // WHEN & THEN
        assertThrows(OrekitException.class, () -> new MeasurementQuality(covariance, new double[] {1}));
    }

    @Test
    void testConstructorMissSizedWeightsException() {
        // GIVEN
        final double[][] covariance = new double[2][2];
        // WHEN & THEN
        assertThrows(OrekitException.class, () -> new MeasurementQuality(covariance, new double[] {1}));
    }

    @Test
    void testConstructorSigmasException() {
        // GIVEN
        final double[] sigmas = new double[] {2, 3};
        // WHEN & THEN
        assertThrows(OrekitException.class, () -> new MeasurementQuality(sigmas, new double[] {1}));
    }

    @Test
    void testGetCovarianceMatrixFromSigmas() {
        // GIVEN
        final double[] sigmas = new double[] {2, 3};
        final double[] weights = new double[] {1, 1};
        // WHEN
        final MeasurementQuality measurementQuality = new MeasurementQuality(sigmas, weights);
        final RealMatrix covariance = measurementQuality.getCovarianceMatrix();
        // THEN
        final double[][] coefficients = covariance.getData();
        assertNotEquals(weights, measurementQuality.getWeights());  // deep copy
        assertArrayEquals(sigmas, measurementQuality.getStandardDeviations());
        assertEquals(2, coefficients.length);
        assertEquals(coefficients.length, coefficients[0].length);
        assertEquals(0., coefficients[0][1]);
        assertEquals(coefficients[1][0], coefficients[0][1]);
        for (int i = 0; i < sigmas.length; i++) {
            assertEquals(sigmas[i] * sigmas[i], coefficients[i][i]);
        }
    }

    @Test
    void testGetCovarianceMatrix() {
        // GIVEN
        final double[][] expectedCovariance = new double[][] {{3, 1}, {1, 2}};
        // WHEN
        final MeasurementQuality measurementQuality = new MeasurementQuality(expectedCovariance, 1);
        final double[][] covariance = measurementQuality.getCovarianceMatrix().getData();
        // THEN
        for (int i = 0; i < expectedCovariance.length; i++) {
            assertArrayEquals(expectedCovariance[i], covariance[i]);
        }
    }
}
