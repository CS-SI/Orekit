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
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

public class LambdaMethodTest extends AbstractLambdaMethodTest {

    protected AbstractLambdaMethod buildReducer() {
        return new LambdaMethod();
    }

    protected RealMatrix buildCovariance(AbstractLambdaMethod reducer) {
        return getLow(reducer).
                        transposeMultiply(getDiag(reducer)).
                        multiply(getLow(reducer));
    }

    @Test
    public void testPermutation() {

        RandomGenerator random = new Well19937a(0xf824c33093974ee5l);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));

            final RealMatrix covariance  = createRandomSymmetricPositiveDefiniteMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform ILS resolution test
            doTestPermutation(random, indirection, covariance);

        }
    }

    @Test
    public void testCustomComparator() {

        // Initialize
        final double[] floatAmbiguities = new double[] {
            5.450, 3.100, 2.970
        };
        final int[] indirection = new int[] { 0, 1, 2 };
        final RealMatrix covariance = MatrixUtils.createRealMatrix(new double[][] {
            { 6.290, 5.978, 0.544 },
            { 5.978, 6.292, 2.340 },
            { 0.544, 2.340, 6.288 }
        });


        final AbstractLambdaMethod reducer = buildReducer();

        // Using this comparator, only one solution can be added to the list
        // In reality, two solutions are found with these inputs (see testJoostenTiberiusFAQ())
        reducer.setComparator(new Comparator<IntegerLeastSquareSolution>() {

            @Override
            public int compare(final IntegerLeastSquareSolution o1,
                               final IntegerLeastSquareSolution o2) {
                return 0;
            }
        });

        IntegerLeastSquareSolution[] solutions = reducer.solveILS(1, floatAmbiguities, indirection, covariance);

        // Verify
        Assertions.assertEquals(1, solutions.length);
        Assertions.assertEquals(0.2183310953369383, solutions[0].getSquaredDistance(), 1.0e-15);
        Assertions.assertEquals(5l, solutions[0].getSolution()[0]);
        Assertions.assertEquals(3l, solutions[0].getSolution()[1]);
        Assertions.assertEquals(4l, solutions[0].getSolution()[2]);
    }

    private void doTestPermutation(final RandomGenerator random,
                                   final int[] indirection, final RealMatrix covariance) {
        final double[] floatAmbiguities = new double[indirection.length];
        for (int i = 0; i < floatAmbiguities.length; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        final AbstractLambdaMethod reducer = buildReducer();
        initializeProblem(reducer, floatAmbiguities, indirection, covariance, 2);
        reducer.ltdlDecomposition();
        RealMatrix filteredCovariance = filterCovariance(covariance, indirection);
        RealMatrix zRef               = MatrixUtils.createRealIdentityMatrix(indirection.length);
        double[]   aBase              = getDecorrelated(reducer).clone();
        double[]   aRef               = aBase.clone();
        Assertions.assertEquals(0.0,
                            zRef.subtract(getZTransformation(reducer)).getNorm1(),
                            1.0e-15);
        for (int k = 0; k < 10; ++k) {
            final Permutation permutation = createRandomPermutation(getLow(reducer),
                                                                    getDiag(reducer),
                                                                    random);
            reducer.permutation(permutation.i, permutation.delta);

            // check accumulated Z transform, with reference based on naive matrix multiplication
            zRef = zRef.multiply(permutation.z);
            Assertions.assertEquals(0.0,
                                zRef.subtract(getZTransformation(reducer)).getNorm1(),
                                Precision.SAFE_MIN);

            // check rebuilt permuted covariance
            RealMatrix rebuilt  = getLow(reducer).transposeMultiply(getDiag(reducer)).multiply(getLow(reducer));
            RealMatrix permuted = zRef.transposeMultiply(filteredCovariance).multiply(zRef);
            Assertions.assertEquals(0.0,
                                permuted.subtract(rebuilt).getNorm1(),
                                2.7e-12 * filteredCovariance.getNorm1());

            // check ambiguities, with reference based on direct permutation
            final double tmp = aRef[permutation.i];
            aRef[permutation.i]     = aRef[permutation.i + 1];
            aRef[permutation.i + 1] = tmp;
            for (int i = 0; i < aRef.length; ++i) {
                Assertions.assertEquals(aRef[i], getDecorrelated(reducer)[i], 4.0e-14);
            }

            // check ambiguities, with reference based on accumulated naive matrix multiplication
            final double[] aRef2 = zRef.transpose().operate(aBase);
            for (int i = 0; i < aRef2.length; ++i) {
                Assertions.assertEquals(aRef2[i], getDecorrelated(reducer)[i], 4.0e-14);
            }

        }
    }

    @Test
    public void testIntegerGaussTransformation() {

        RandomGenerator random = new Well19937a(0x08e9e32dcd0f9dbdl);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));

            final RealMatrix covariance  = createRandomSymmetricPositiveDefiniteMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform integer Gauss transformation test
            doTestIntegerGaussTransformation(random, indirection, covariance);

        }
    }

    private void doTestIntegerGaussTransformation(final RandomGenerator random,
                                                  final int[] indirection, final RealMatrix covariance) {
        final int n = indirection.length;
        final double[] floatAmbiguities = new double[n];
        for (int i = 0; i < n; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        final AbstractLambdaMethod reducer = buildReducer();
        initializeProblem(reducer, floatAmbiguities, indirection, covariance, 2);
        reducer.ltdlDecomposition();
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(n);
        RealMatrix zRef     = identity;
        RealMatrix lowRef   = getLow(reducer);
        RealMatrix diagRef  = getDiag(reducer);
        double[]   aBase    = getDecorrelated(reducer).clone();
        double[]   aRef     = aBase;
        Assertions.assertEquals(0.0,
                            zRef.subtract(getZTransformation(reducer)).getNorm1(),
                            1.0e-15);
        for (int k = 0; k < 10; ++k) {
            final IntegerGaussTransformation gauss = createRandomIntegerGaussTransformation(getLow(reducer), random);
            reducer.integerGaussTransformation(gauss.i, gauss.j);

            // check accumulated Z transform, with reference based on naive matrix multiplication
            zRef = zRef.multiply(gauss.z);
            Assertions.assertEquals(0.0,
                                zRef.subtract(getZTransformation(reducer)).getNorm1(),
                                1.5e-15 * zRef.getNorm1());

            // check diagonal part, which should not change
            Assertions.assertEquals(0.0,
                                diagRef.subtract(getDiag(reducer)).getNorm1(),
                                Precision.SAFE_MIN);

            // check accumulated low triangular part, with reference based on naive matrix multiplication
            lowRef = lowRef.multiply(gauss.z);
            Assertions.assertEquals(0.0,
                                lowRef.subtract(getLow(reducer)).getNorm1(),
                                Precision.SAFE_MIN);
            Assertions.assertTrue(getLow(reducer).getEntry(gauss.i, gauss.j) <= 0.5);

            // check ambiguities, with reference based on single step naive matrix multiplication
            aRef = gauss.z.transpose().operate(aRef);
            for (int i = 0; i < aRef.length; ++i) {
                Assertions.assertEquals(aRef[i], getDecorrelated(reducer)[i], 4.0e-14);
            }

            // check ambiguities, with reference based on accumulated naive matrix multiplication
            final double[] aRef2 = zRef.transpose().operate(aBase);
            for (int i = 0; i < aRef2.length; ++i) {
                Assertions.assertEquals(aRef2[i], getDecorrelated(reducer)[i], 2.3e-13);
            }

        }
    }

}
