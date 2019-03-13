/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathArrays.Position;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.utils.ParameterDriver;

public class LambdaReducerTest {

    @Test
    public void testSimpleFullDecomposition() {
        final RealMatrix refLow = MatrixUtils.createRealMatrix(new double[][] {
            { 1.0, 0.0, 0.0, 0.0 },
            { 2.0, 1.0, 0.0, 0.0 },
            { 3.0, 4.0, 1.0, 0.0 },
            { 5.0, 6.0, 7.0, 1.0 }
        });
        final RealMatrix refDiag = MatrixUtils.createRealDiagonalMatrix(new double[] {
            5.0, 7.0, 9.0, 11.0
        });
        final RealMatrix covariance = refLow.transposeMultiply(refDiag).multiply(refLow);
        final int[] indirection = new int[] { 0, 1, 2, 3 };
        LambdaReducer reducer = new LambdaReducer(buildDrivers(indirection.length), indirection, covariance);
        reducer.ltdlDecomposition();
        Assert.assertEquals(0.0, refLow.subtract(reducer.getLow()).getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, refDiag.subtract(reducer.getDiag()).getNorm(), 1.0e-15);
    }

    @Test
    public void testRandomDecomposition() {

        RandomGenerator random = new Well19937a(0x7aa94f3683fd08c1l);
        for (int k = 0; k < 1000; ++k) {
            // generate a random symmetric matrix
            final int n = 1 + random.nextInt(20);
            final RealMatrix covariance = MatrixUtils.createRealMatrix(n, n);
            for (int i = 0; i < n; ++i) {                
                for (int j = 0; j <= i; ++j) {
                    final double entry = 20 * random.nextDouble() - 10;
                    covariance.setEntry(i, j, entry);
                    covariance.setEntry(j, i, entry);
                }
            }

            // generate an indirection array
            final int[] all = new int[n];
            for (int i = 0; i < all.length; ++i) {
                all[i] = i;
            }
            MathArrays.shuffle(all, 0, Position.TAIL, random);
            final int[] indirection = Arrays.copyOf(all, 1 + random.nextInt(n));

            doTestDecomposition(indirection, covariance);

        }

    }

    private void doTestDecomposition(final int[] indirection, final RealMatrix covariance) {
        final LambdaReducer reducer = new LambdaReducer(buildDrivers(indirection.length), indirection, covariance);
        reducer.ltdlDecomposition();
        final RealMatrix extracted = MatrixUtils.createRealMatrix(indirection.length, indirection.length);
        for (int i = 0; i < indirection.length; ++i) {
            for (int j = 0; j < indirection.length; ++j) {
                extracted.setEntry(i, j, covariance.getEntry(indirection[i], indirection[j]));
            }
        }
        final RealMatrix rebuilt = reducer.getLow().
                                   transposeMultiply(reducer.getDiag()).
                                   multiply(reducer.getLow());
        double maxError = 0;
        for (int i = 0; i < indirection.length; ++i) {
            for (int j = 0; j < indirection.length; ++j) {
                maxError = FastMath.max(maxError,
                                        FastMath.abs(covariance.getEntry(indirection[i], indirection[j]) -
                                                     rebuilt.getEntry(i, j)));
            }
        }
        Assert.assertEquals(0.0, maxError, 2.0e-11);

    }

    private List<ParameterDriver> buildDrivers(final int n) {
        List<ParameterDriver> list = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            list.add(new ParameterDriver("ambiguity-" + i, 0.0, 1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        }
        return list;
    }
}

