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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathArrays.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class IntegerBootstrappingTest {


    /** test the resolution for the tiberius example. */
    @Test
    public void testJoostenTiberiusFAQ() {
        // this test corresponds to the "LAMBDA: FAQs" paper by Peter Joosten and Christian Tiberius

        final double[] floatAmbiguities = new double[] {
            5.450, 3.100, 2.970
        };
        final int[] indirection = new int[] { 0, 1, 2 };
        final RealMatrix covariance = MatrixUtils.createRealMatrix(new double[][] {
            { 6.290, 5.978, 0.544 },
            { 5.978, 6.292, 2.340 },
            { 0.544, 2.340, 6.288 }
        });

        final IntegerBootstrapping bootstrap = new IntegerBootstrapping(0.8);
        IntegerLeastSquareSolution[] solutions = bootstrap.solveILS(1, floatAmbiguities, indirection, covariance);
        if (solutions.length != 0) {
            Assertions.assertTrue(solutions.length == 1);
        }
    }

    @Test
    public void testRandomProblems() throws FileNotFoundException, UnsupportedEncodingException {
        RandomGenerator random = new Well19937a(0x1c68f36088a9133al);
        int[] count = new int[3];
        for (int k = 0; k < 10000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));
            final RealMatrix covariance  = createRandomSymmetricPositiveDefiniteMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform decomposition test
            ++ count[doTestILS(random, indirection, covariance)];
        }

        Assertions.assertEquals(  50, count[0]);
        Assertions.assertEquals(9950, count[1]);

    }

    @Test
    public void testEquals() {
        // Initialize comparator
        final IntegerLeastSquareComparator comparator = new IntegerLeastSquareComparator();
        // Verify
        Assertions.assertEquals(0,  comparator.compare(new IntegerLeastSquareSolution(new long[] { 1l, 2l, 3l}, 4.0), new IntegerLeastSquareSolution(new long[] { 9l, 9l, 9l}, 4.0)));
        Assertions.assertEquals(-1, comparator.compare(new IntegerLeastSquareSolution(new long[] { 1l, 2l, 3l}, 4.0), new IntegerLeastSquareSolution(new long[] { 9l, 9l, 9l}, 9.0)));
    }

    private int doTestILS(final RandomGenerator random,
                          final int[] indirection, final RealMatrix covariance) {
        final double[] floatAmbiguities = new double[indirection.length];
        for (int i = 0; i < floatAmbiguities.length; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        IntegerBootstrapping bootstrap = new IntegerBootstrapping(0.3);
        final IntegerLeastSquareSolution[] solutions =
                        bootstrap.solveILS(1, floatAmbiguities, indirection, covariance);

        // check solution exist if and only if its probability great enough
        if (solutions.length != 0) {
            Assertions.assertTrue(1 / (solutions[0].getSquaredDistance()) > 0.3);
        }

        return solutions.length;

    }

    protected RealMatrix createRandomSymmetricPositiveDefiniteMatrix(final int n, final RandomGenerator random) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(n, n);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                matrix.setEntry(i, j, 20 * random.nextDouble() - 10);
            }
        }
        return matrix.transposeMultiply(matrix);
    }

    RealMatrix filterCovariance(final RealMatrix covariance, int[] indirection) {
        RealMatrix filtered = MatrixUtils.createRealMatrix(indirection.length, indirection.length);
        for (int i = 0; i < indirection.length; ++i) {
            for (int j = 0; j <= i; ++j) {
                filtered.setEntry(i, j, covariance.getEntry(indirection[i], indirection[j]));
                filtered.setEntry(j, i, covariance.getEntry(indirection[i], indirection[j]));
            }
        }
        return filtered;
    }

    protected int[] createRandomIndirectionArray(final int n, final RandomGenerator random) {
        final int[] all = new int[n];
        for (int i = 0; i < all.length; ++i) {
            all[i] = i;
        }
        MathArrays.shuffle(all, 0, Position.TAIL, random);
        return Arrays.copyOf(all, FastMath.max(2, 1 + random.nextInt(n)));
    }



}
