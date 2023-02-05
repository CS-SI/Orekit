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

import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathArrays.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class AbstractLambdaMethodTest {

    protected abstract AbstractLambdaMethod buildReducer();
    protected abstract RealMatrix buildCovariance(AbstractLambdaMethod reducer);

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
        Assertions.assertEquals(0.0, refLow.subtract(getLow(reducer)).getNorm1(), 9.9e-13 * refLow.getNorm1());
        Assertions.assertEquals(0.0, refDiag.subtract(getDiag(reducer)).getNorm1(), 6.7e-13 * refDiag.getNorm1());
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

        final AbstractLambdaMethod reducer = buildReducer();
        IntegerLeastSquareSolution[] solutions = reducer.solveILS(2, floatAmbiguities, indirection, covariance);


        Assertions.assertEquals(2, solutions.length);
        Assertions.assertEquals(0.2183310953369383, solutions[0].getSquaredDistance(), 1.0e-15);
        Assertions.assertEquals(5l, solutions[0].getSolution()[0]);
        Assertions.assertEquals(3l, solutions[0].getSolution()[1]);
        Assertions.assertEquals(4l, solutions[0].getSolution()[2]);
        Assertions.assertEquals(0.3072725757902666, solutions[1].getSquaredDistance(), 1.0e-12);
        Assertions.assertEquals(6l, solutions[1].getSolution()[0]);
        Assertions.assertEquals(4l, solutions[1].getSolution()[1]);
        Assertions.assertEquals(4l, solutions[1].getSolution()[2]);

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
        final RealMatrix rebuilt = buildCovariance(reducer);
        Assertions.assertEquals(0.0,
                            rebuilt.subtract(extracted).getNorm1(),
                            2.5e-13 * extracted.getNorm1());

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
        Assertions.assertEquals(nbSol, solutions.length);
        for (int i = 0; i < nbSol - 1; ++i) {
            Assertions.assertTrue(solutions[i].getSquaredDistance() <= solutions[i + 1].getSquaredDistance());
        }
        for (int i = 0; i < nbSol; ++i) {
            final RealMatrix a = MatrixUtils.createRealMatrix(floatAmbiguities.length, 1);
            for (int k = 0; k < a.getRowDimension(); ++k) {
                a.setEntry(k, 0, solutions[i].getSolution()[k]);
            }
            final double squaredNorm = a.subtract(aHat).transposeMultiply(invCov).multiply(a.subtract(aHat)).getEntry(0, 0);
            Assertions.assertEquals(squaredNorm, solutions[i].getSquaredDistance(), 6.0e-10 * squaredNorm);
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
        Assertions.assertTrue(min > -1.6e-10);

    }

    protected int getN(final AbstractLambdaMethod reducer) {
        try {
            final Field nField = AbstractLambdaMethod.class.getDeclaredField("n");
            nField.setAccessible(true);
            return ((Integer) nField.get(reducer)).intValue();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
            return -1;
        }
    }

    protected RealMatrix getLow(final AbstractLambdaMethod reducer) {
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
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
    }

    protected DiagonalMatrix getDiag(final AbstractLambdaMethod reducer) {
        try {
            final Field diagField = AbstractLambdaMethod.class.getDeclaredField("diag");
            diagField.setAccessible(true);
            return new DiagonalMatrix((double[]) diagField.get(reducer));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
    }

    protected RealMatrix getZTransformation(final AbstractLambdaMethod reducer) {
        return new QRDecomposer(1.0e-10).decompose(dogetZInverse(reducer)).getInverse();
    }

    protected RealMatrix getZInverseTransformation(final AbstractLambdaMethod reducer) {
        return dogetZInverse(reducer);
    }

    protected RealMatrix dogetZInverse(final AbstractLambdaMethod reducer) {
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
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
    }

    protected double[] getDecorrelated(final AbstractLambdaMethod reducer) {
        try {
            final Field decorrelatedField = AbstractLambdaMethod.class.getDeclaredField("decorrelated");
            decorrelatedField.setAccessible(true);
            return (double[]) decorrelatedField.get(reducer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
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

    protected int[] createRandomIndirectionArray(final int n, final RandomGenerator random) {
        final int[] all = new int[n];
        for (int i = 0; i < all.length; ++i) {
            all[i] = i;
        }
        MathArrays.shuffle(all, 0, Position.TAIL, random);
        return Arrays.copyOf(all, FastMath.max(2, 1 + random.nextInt(n)));
    }

    protected IntegerGaussTransformation createRandomIntegerGaussTransformation(final RealMatrix low,
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

    protected static class IntegerGaussTransformation {
        protected final int i;
        protected final int j;
        protected final RealMatrix z;
        IntegerGaussTransformation(int n, int i, int j, int mu) {
            this.i = i;
            this.j = j;
            this.z = MatrixUtils.createRealIdentityMatrix(n);
            z.setEntry(i, j, -mu);
        }
    }

    protected Permutation createRandomPermutation(final RealMatrix low,
                                                final RealMatrix diag,
                                                final RandomGenerator random) {
        final int    n     = low.getRowDimension();
        final int    i     = random.nextInt(n - 1);
        final double dk0   = diag.getEntry(i, i);
        final double dk1   = diag.getEntry(i + 1, i + 1);
        final double lk1k0 = low.getEntry(i + 1, i);
        return new Permutation(n, i, dk0 + lk1k0 * lk1k0 * dk1);
    }

    protected void initializeProblem(final AbstractLambdaMethod method,
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
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    protected static class Permutation {
        protected final int i;
        protected double delta;
        protected final RealMatrix z;
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

}

