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

import java.lang.reflect.Field;
import java.util.Arrays;

import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathArrays.Position;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractLambdaMethodTest {

    protected abstract AbstractLambdaMethod buildReducer();

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
        AbstractLambdaMethod reducer = buildReducer();
        reducer.initializeSearch(new double[indirection.length], indirection, covariance, 2);
        reducer.ltdlDecomposition();
        Assert.assertEquals(0.0, refLow.subtract(getLow(reducer)).getNorm(), 9.9e-13 * refLow.getNorm());
        Assert.assertEquals(0.0, refDiag.subtract(getDiag(reducer)).getNorm(), 6.7e-13 * refDiag.getNorm());
    }

    @Test
    public void testRandomDecomposition() {

        RandomGenerator random = new Well19937a(0x7aa94f3683fd08c1l);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));
            final RealMatrix covariance  = createRandomSymmetricMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform decomposition test
            doTestDecomposition(indirection, covariance);

        }

    }

    @Test
    public void testIntegerGaussTransformation() {

        RandomGenerator random = new Well19937a(0x08e9e32dcd0f9dbdl);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));
   
            final RealMatrix covariance  = createRandomSymmetricMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform integer Gauss transformation test
            doTestIntegerGaussTransformation(random, indirection, covariance);

        }
    }

    @Test
    public void testPermutation() {

        RandomGenerator random = new Well19937a(0xf824c33093974ee5l);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));
   
            final RealMatrix covariance  = createRandomSymmetricMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform permutation transformation test
            doTestPermutation(random, indirection, covariance);

        }
    }

    @Test
    public void testJoostenTiberiusFAQ() {
        // this test corresponds to the "LAMBDA: FAQs" paper by Peter Joosten and Christian Tiberius

        final double[] floatAmbiguities = new double[] {
            5.450, 3.100, 2.970
        };
        final int[] indirection = new int[] { 0, 1, 2 };
        final RealMatrix id = MatrixUtils.createRealIdentityMatrix(3);
        final RealMatrix covariance = MatrixUtils.createRealMatrix(new double[][] {
            { 6.290, 5.978, 0.544 },
            { 5.978, 6.292, 2.340 },
            { 0.544, 2.340, 6.288 }
        });

        final AbstractLambdaMethod reducer = buildReducer();
        IntegerLeastSquareSolution[] solutions = reducer.solveILS(2, floatAmbiguities, indirection, covariance);

        final RealMatrix rebuiltInverseCovariance = getZTransformation(reducer).
                                                    multiply(getLow(reducer).
                                                             multiply(getDiag(reducer)).
                                                             multiplyTransposed(getLow(reducer))).
                                                    multiplyTransposed(getZTransformation(reducer));
        Assert.assertEquals(0.0,
                            id.subtract(rebuiltInverseCovariance.multiply(covariance)).getNorm(),
                            2.7e-14);

        final RealMatrix zRef = MatrixUtils.createRealMatrix(new double[][] {
            { -2,  3,  1 },
            {  3, -3, -1 },
            { -1,  1,  0 }
        });

        Assert.assertEquals(0.0,
                            id.subtract(zRef.transposeMultiply(getZInverseTransformation(reducer).transpose())).getNorm(),
                            1.0e-15);

        Assert.assertEquals(2, solutions.length);
        Assert.assertEquals(0.2183310953369383, solutions[0].getSquaredDistance(), 1.0e-15);
        Assert.assertEquals(5l, solutions[0].getSolution()[0]);
        Assert.assertEquals(3l, solutions[0].getSolution()[1]);
        Assert.assertEquals(4l, solutions[0].getSolution()[2]);
        Assert.assertEquals(0.3072725757902666, solutions[1].getSquaredDistance(), 1.0e-15);
        Assert.assertEquals(6l, solutions[1].getSolution()[0]);
        Assert.assertEquals(4l, solutions[1].getSolution()[1]);
        Assert.assertEquals(4l, solutions[1].getSolution()[2]);

    }

    private void doTestDecomposition(final int[] indirection, final RealMatrix covariance) {
        final AbstractLambdaMethod reducer = buildReducer();
        reducer.initializeSearch(new double[indirection.length], indirection, covariance, 2);
        reducer.ltdlDecomposition();
        final RealMatrix extracted = MatrixUtils.createRealMatrix(indirection.length, indirection.length);
        for (int i = 0; i < indirection.length; ++i) {
            for (int j = 0; j < indirection.length; ++j) {
                extracted.setEntry(i, j, covariance.getEntry(indirection[i], indirection[j]));
            }
        }
        final RealMatrix rebuilt = getLow(reducer).
                                   transposeMultiply(getDiag(reducer)).
                                   multiply(getLow(reducer));
        Assert.assertEquals(0.0,
                            rebuilt.subtract(extracted).getNorm(),
                            2.5e-13 * extracted.getNorm());

    }

    private void doTestIntegerGaussTransformation(final RandomGenerator random,
                                                  final int[] indirection, final RealMatrix covariance) {
        final int n = indirection.length;
        final double[] floatAmbiguities = new double[n];
        for (int i = 0; i < n; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        final AbstractLambdaMethod reducer = buildReducer();
        reducer.initializeSearch(floatAmbiguities, indirection, covariance, 2);
        reducer.ltdlDecomposition();
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(n);
        RealMatrix zRef     = identity;
        RealMatrix lowRef   = getLow(reducer);
        RealMatrix diagRef  = getDiag(reducer);
        double[]   aBase    = getDecorrelated(reducer).clone();
        double[]   aRef     = aBase;
        Assert.assertEquals(0.0,
                            zRef.subtract(getZTransformation(reducer)).getNorm(),
                            1.0e-15);
        for (int k = 0; k < 10; ++k) {
            final IntegerGaussTransformation gauss = createRandomIntegerGaussTransformation(getLow(reducer), random);
            reducer.integerGaussTransformation(gauss.i, gauss.j);

            // check accumulated Z transform, with reference based on naive matrix multiplication
            zRef = zRef.multiply(gauss.z);
            Assert.assertEquals(0.0,
                                zRef.subtract(getZTransformation(reducer)).getNorm(),
                                Precision.SAFE_MIN);

            // check Z and Z⁻¹
            Assert.assertEquals(0.0,
                                identity.subtract(getZTransformation(reducer).multiply(getZInverseTransformation(reducer))).getNorm(),
                                Precision.SAFE_MIN);

            // check diagonal part, which should not change
            Assert.assertEquals(0.0,
                                diagRef.subtract(getDiag(reducer)).getNorm(),
                                Precision.SAFE_MIN);

            // check accumulated low triangular part, with reference based on naive matrix multiplication
            lowRef = lowRef.multiply(gauss.z);
            Assert.assertEquals(0.0,
                                lowRef.subtract(getLow(reducer)).getNorm(),
                                Precision.SAFE_MIN);
            Assert.assertTrue(getLow(reducer).getEntry(gauss.i, gauss.j) <= 0.5);

            // check ambiguities, with reference based on single step naive matrix multiplication
            aRef = gauss.z.transpose().operate(aRef);
            for (int i = 0; i < aRef.length; ++i) {
                Assert.assertEquals(aRef[i], getDecorrelated(reducer)[i], 4.0e-14);
            }

            // check ambiguities, with reference based on accumulated naive matrix multiplication
            final double[] aRef2 = zRef.transpose().operate(aBase);
            for (int i = 0; i < aRef2.length; ++i) {
                Assert.assertEquals(aRef2[i], getDecorrelated(reducer)[i], 2.3e-13);
            }

        }
    }

    private void doTestPermutation(final RandomGenerator random,
                                   final int[] indirection, final RealMatrix covariance) {
        final double[] floatAmbiguities = new double[indirection.length];
        for (int i = 0; i < floatAmbiguities.length; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        final AbstractLambdaMethod reducer = buildReducer();
        reducer.initializeSearch(floatAmbiguities, indirection, covariance, 2);
        reducer.ltdlDecomposition();
        RealMatrix filteredCovariance = filterCovariance(covariance, indirection);
        RealMatrix zRef               = MatrixUtils.createRealIdentityMatrix(indirection.length);
        double[]   aBase              = getDecorrelated(reducer).clone();
        double[]   aRef               = aBase.clone();
        Assert.assertEquals(0.0,
                            zRef.subtract(getZTransformation(reducer)).getNorm(),
                            1.0e-15);
        for (int k = 0; k < 10; ++k) {
            final Permutation permutation = createRandomPermutation(getLow(reducer),
                                                                    getDiag(reducer),
                                                                    random);
            reducer.permutation(permutation.i, permutation.delta);

            // check accumulated Z transform, with reference based on naive matrix multiplication
            zRef = zRef.multiply(permutation.z);
            Assert.assertEquals(0.0,
                                zRef.subtract(getZTransformation(reducer)).getNorm(),
                                Precision.SAFE_MIN);

            // check rebuilt permuted covariance
            RealMatrix rebuilt  = getLow(reducer).transposeMultiply(getDiag(reducer)).multiply(getLow(reducer));
            RealMatrix permuted = zRef.transposeMultiply(filteredCovariance).multiply(zRef);
            Assert.assertEquals(0.0,
                                permuted.subtract(rebuilt).getNorm(),
                                2.7e-12 * filteredCovariance.getNorm());

            // check ambiguities, with reference based on direct permutation
            final double tmp = aRef[permutation.i];
            aRef[permutation.i]     = aRef[permutation.i + 1];
            aRef[permutation.i + 1] = tmp;
            for (int i = 0; i < aRef.length; ++i) {
                Assert.assertEquals(aRef[i], getDecorrelated(reducer)[i], 4.0e-14);
            }

            // check ambiguities, with reference based on accumulated naive matrix multiplication
            final double[] aRef2 = zRef.transpose().operate(aBase);
            for (int i = 0; i < aRef2.length; ++i) {
                Assert.assertEquals(aRef2[i], getDecorrelated(reducer)[i], 4.0e-14);
            }

        }
    }

    private int getN(final AbstractLambdaMethod reducer) {
        try {
            final Field nField = AbstractLambdaMethod.class.getDeclaredField("n");
            nField.setAccessible(true);
            return ((Integer) nField.get(reducer)).intValue();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return -1;
        }
    }

    private RealMatrix getLow(final AbstractLambdaMethod reducer) {
        try {
            final int n = getN(reducer);
            final Field lowField = AbstractLambdaMethod.class.getDeclaredField("low");
            lowField.setAccessible(true);
            final double[] low = (double[]) lowField.get(reducer);
            final RealMatrix lowM = MatrixUtils.createRealMatrix(n, n);
            int k = 0;
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < i; ++j) {
                    lowM.setEntry(i, j, low[k++]);
                }
                lowM.setEntry(i, i, 1.0);
            }
            return lowM;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    public DiagonalMatrix getDiag(final AbstractLambdaMethod reducer) {
        try {
            final Field diagField = AbstractLambdaMethod.class.getDeclaredField("diag");
            diagField.setAccessible(true);
            final double[] diag = (double[]) diagField.get(reducer);
            return MatrixUtils.createRealDiagonalMatrix(diag);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    public RealMatrix getZTransformation(final AbstractLambdaMethod reducer) {
        return dogetZs(reducer, "zTransformation");
    }

    public RealMatrix getZInverseTransformation(final AbstractLambdaMethod reducer) {
        return dogetZs(reducer, "zInverseTransformation");
    }

    private RealMatrix dogetZs(final AbstractLambdaMethod reducer, final String fieldName) {
        try {
            final int n = getN(reducer);
            final Field zField = AbstractLambdaMethod.class.getDeclaredField(fieldName);
            zField.setAccessible(true);
            final int[] z = (int[]) zField.get(reducer);
            final RealMatrix zM = MatrixUtils.createRealMatrix(n, n);
            int k = 0;
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < n; ++j) {
                    zM.setEntry(i, j, z[k++]);
                }
            }
            return zM;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    public double[] getDecorrelated(final AbstractLambdaMethod reducer) {
        try {
            final Field decorrelatedField = AbstractLambdaMethod.class.getDeclaredField("decorrelated");
            decorrelatedField.setAccessible(true);
            return (double[]) decorrelatedField.get(reducer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    private RealMatrix createRandomSymmetricMatrix(final int n, final RandomGenerator random) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(n, n);
        for (int i = 0; i < n; ++i) {                
            for (int j = 0; j <= i; ++j) {
                final double entry = 20 * random.nextDouble() - 10;
                matrix.setEntry(i, j, entry);
                matrix.setEntry(j, i, entry);
            }
        }
        return matrix;
    }

    private int[] createRandomIndirectionArray(final int n, final RandomGenerator random) {
        final int[] all = new int[n];
        for (int i = 0; i < all.length; ++i) {
            all[i] = i;
        }
        MathArrays.shuffle(all, 0, Position.TAIL, random);
        return Arrays.copyOf(all, FastMath.max(2, 1 + random.nextInt(n)));
    }

    private IntegerGaussTransformation createRandomIntegerGaussTransformation(final RealMatrix low,
                                                                              final RandomGenerator random) {
        final int n = low.getRowDimension();
        int i = random.nextInt(n);
        int j = i;
        while (j == i) {
            j = random.nextInt(n);
        }
        if (i < j) {
            final int tmp = i;
            i = j;
            j = tmp;
        }
        return new IntegerGaussTransformation(n, i, j, (int) FastMath.rint(low.getEntry(i, j))); 
    }

    private static class IntegerGaussTransformation {
        private final int i;
        private final int j;
        private final RealMatrix z;
        IntegerGaussTransformation(int n, int i, int j, int mu) {
            this.i = i;
            this.j = j;
            this.z = MatrixUtils.createRealIdentityMatrix(n);
            z.setEntry(i, j, -mu);
        }
    }

    private Permutation createRandomPermutation(final RealMatrix low,
                                                final RealMatrix diag,
                                                final RandomGenerator random) {
        final int    n     = low.getRowDimension();
        final int    i     = random.nextInt(n - 1);
        final double dk0   = diag.getEntry(i, i);
        final double dk1   = diag.getEntry(i + 1, i + 1);
        final double lk1k0 = low.getEntry(i + 1, i);
        return new Permutation(n, i, dk0 + lk1k0 * lk1k0 * dk1); 
    }

    private static class Permutation {
        private final int i;
        private double delta;
        private final RealMatrix z;
        Permutation(int n, int i, double delta) {
            this.i     = i;
            this.delta = delta;
            this.z     = MatrixUtils.createRealIdentityMatrix(n);
            z.setEntry(i,     i,     0);
            z.setEntry(i,     i + 1, 1);
            z.setEntry(i + 1, i,     1);
            z.setEntry(i + 1, i + 1, 0);
        }
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

    RealMatrix inverse(final RealMatrix m) {
        return new QRDecomposer(1.0e-10).decompose(m).getInverse();
    }

}

