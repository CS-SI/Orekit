/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        initializeProblem(reducer, new double[indirection.length], indirection, covariance, 2);
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
            final RealMatrix covariance  = createRandomSymmetricPositiveDefiniteMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform decomposition test
            doTestDecomposition(indirection, covariance);

        }

    }

    @Test
    public void testRandomProblems() {
        
        RandomGenerator random = new Well19937a(0x1c68f36088a9133al);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));
            final RealMatrix covariance  = createRandomSymmetricPositiveDefiniteMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform decomposition test
            doTestILS(random, indirection, covariance);

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
        Assert.assertEquals(solutions[0], solutions[0]);
        Assert.assertNotEquals(solutions[0], solutions[1]);
        Assert.assertNotEquals(solutions[0], "");
        Assert.assertNotEquals(solutions[0], null);
        Assert.assertEquals(-958532080, solutions[0].hashCode());

    }

    private void doTestDecomposition(final int[] indirection, final RealMatrix covariance) {
        final AbstractLambdaMethod reducer = buildReducer();
        initializeProblem(reducer, new double[indirection.length], indirection, covariance, 2);
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
        initializeProblem(reducer, floatAmbiguities, indirection, covariance, 2);
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
                                1.5e-15 * zRef.getNorm());

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
        initializeProblem(reducer, floatAmbiguities, indirection, covariance, 2);
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

    private void doTestILS(final RandomGenerator random,
                           final int[] indirection, final RealMatrix covariance) {
        final double[] floatAmbiguities = new double[indirection.length];
        for (int i = 0; i < floatAmbiguities.length; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        final RealMatrix aHat        = MatrixUtils.createColumnRealMatrix(floatAmbiguities);
        final RealMatrix invCov      = new QRDecomposer(1.0e-10).
                                       decompose(filterCovariance(covariance, indirection)).
                                       getInverse();
        final AbstractLambdaMethod reducer = buildReducer();
        final int nbSol = 10;
        final IntegerLeastSquareSolution[] solutions =
                        reducer.solveILS(nbSol, floatAmbiguities, indirection, covariance);

        // check solutions are consistent
        Assert.assertEquals(nbSol, solutions.length);
        for (int i = 0; i < nbSol - 1; ++i) {
            Assert.assertTrue(solutions[i].getSquaredDistance() <= solutions[i + 1].getSquaredDistance());
        }
        for (int i = 0; i < nbSol; ++i) {
            final RealMatrix a = MatrixUtils.createRealMatrix(floatAmbiguities.length, 1);
            for (int k = 0; k < a.getRowDimension(); ++k) {
                a.setEntry(k, 0, solutions[i].getSolution()[k]);
            }
            final double squaredNorm = a.subtract(aHat).transposeMultiply(invCov).multiply(a.subtract(aHat)).getEntry(0, 0);
            Assert.assertEquals(squaredNorm, solutions[i].getSquaredDistance(), 6.0e-10 * squaredNorm);
        }

        // check we can't find a better solution than the first one in the array
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 1000; ++i) {
            final RealMatrix a = MatrixUtils.createRealMatrix(floatAmbiguities.length, 1);
            for (int k = 0; k < a.getRowDimension(); ++k) {
                long close = FastMath.round(floatAmbiguities[k]);
                a.setEntry(k, 0, close + random.nextInt(11) - 5);
            }            
            final double squaredNorm = a.subtract(aHat).transposeMultiply(invCov).multiply(a.subtract(aHat)).getEntry(0, 0);
            min = FastMath.min(min, (squaredNorm - solutions[0].getSquaredDistance()) / solutions[0].getSquaredDistance());
        }
        Assert.assertTrue(min > -1.0e-10);

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

    private DiagonalMatrix getDiag(final AbstractLambdaMethod reducer) {
        try {
            final Field diagField = AbstractLambdaMethod.class.getDeclaredField("diag");
            diagField.setAccessible(true);
            return new DiagonalMatrix((double[]) diagField.get(reducer));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    private RealMatrix getZTransformation(final AbstractLambdaMethod reducer) {
        return new QRDecomposer(1.0e-10).decompose(dogetZInverse(reducer)).getInverse();
    }

    private RealMatrix getZInverseTransformation(final AbstractLambdaMethod reducer) {
        return dogetZInverse(reducer);
    }

    private RealMatrix dogetZInverse(final AbstractLambdaMethod reducer) {
        try {
            final int n = getN(reducer);
            final Field zField = AbstractLambdaMethod.class.getDeclaredField("zInverseTransformation");
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

    private double[] getDecorrelated(final AbstractLambdaMethod reducer) {
        try {
            final Field decorrelatedField = AbstractLambdaMethod.class.getDeclaredField("decorrelated");
            decorrelatedField.setAccessible(true);
            return (double[]) decorrelatedField.get(reducer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    private RealMatrix createRandomSymmetricPositiveDefiniteMatrix(final int n, final RandomGenerator random) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(n, n);
        for (int i = 0; i < n; ++i) {                
            for (int j = 0; j < n; ++j) {
                matrix.setEntry(i, j, 20 * random.nextDouble() - 10);
            }
        }
        return matrix.transposeMultiply(matrix);
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

    private void initializeProblem(final AbstractLambdaMethod method,
                                   final double[] floatAmbiguities, final int[] indirection,
                                   final RealMatrix globalCovariance, final int nbSol) {
        try {
            final Method initializeMethod = AbstractLambdaMethod.class.getDeclaredMethod("initializeProblem",
                                                                                         double[].class,
                                                                                         int[].class,
                                                                                         RealMatrix.class,
                                                                                         Integer.TYPE);
            initializeMethod.setAccessible(true);
            initializeMethod.invoke(method, floatAmbiguities, indirection, globalCovariance, nbSol);
        } catch (NoSuchMethodException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException e) {
            Assert.fail(e.getLocalizedMessage());
        }
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

