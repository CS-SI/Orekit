/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GammaMnsFunctionTest {

    int      nMax;
    double[] fact;

    @Test
    public void testIndex()
        throws NoSuchMethodException, SecurityException,
               IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method indexM = GammaMnsFunction.class.getDeclaredMethod("index",
                                                                 Integer.TYPE, Integer.TYPE, Integer.TYPE);
        indexM.setAccessible(true);
        int i = 0;
        for (int n = 0; n <= nMax; ++n) {
            for (int m = 0; m <= n; ++m) {
                for (int s = -n; s <= n; ++s) {
                    Assert.assertEquals(i++, indexM.invoke(null, m, n, s));
                }
            }
        }
    }

    @Test
    public void testPrecomputedRatios()
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field precomputedF = GammaMnsFunction.class.getDeclaredField("PRECOMPUTED_RATIOS");
        precomputedF.setAccessible(true);
        new GammaMnsFunction(nMax, 0.5, +1);
        double[] precomputed = (double[]) precomputedF.get(null);
        int i = 0;
        for (int n = 0; n <= nMax; ++n) {
            for (int m = 0; m <= n; ++m) {
                for (int s = -n; s <= n; ++s) {
                    // compare against naive implementation
                    double r = naiveRatio(m, n, s);
                    Assert.assertEquals(r, precomputed[i++], 2.0e-14 * r);
                }
            }
        }
    }

    @Test
    public void testReallocate()
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field precomputedF = GammaMnsFunction.class.getDeclaredField("PRECOMPUTED_RATIOS");
        precomputedF.setAccessible(true);
        precomputedF.set(null, new double[0]);
        new GammaMnsFunction(nMax, 0.5, +1);
        double[] orginalPrecomputed = (double[]) precomputedF.get(null);
        Assert.assertEquals((nMax + 1) * (nMax + 2) * (4 * nMax + 3) / 6, orginalPrecomputed.length);
        new GammaMnsFunction(nMax + 3, 0.5, +1);
        double[] reallocatedPrecomputed = (double[]) precomputedF.get(null);
        Assert.assertEquals((nMax + 4) * (nMax + 5) * (4 * nMax + 15) / 6, reallocatedPrecomputed.length);
        for (int i = 0; i < orginalPrecomputed.length; ++i) {
            Assert.assertEquals(orginalPrecomputed[i], reallocatedPrecomputed[i],
                                1.0e-15 * orginalPrecomputed[i]);
        }
    }

    @Test
    public void testValue() {
        for (int bigI : new int[] { -1, +1 }) {
            for (double gamma = 0; gamma <= 1; gamma += 1.0 / 64.0) {
                GammaMnsFunction gammaMNS = new GammaMnsFunction(nMax, gamma, bigI);
                for (int n = 0; n <= nMax; ++n) {
                    for (int m = 0; m <= n; ++m) {
                        for (int s = -n; s <= n; ++s) {
                            // compare against naive implementation
                            final double v = naiveValue(bigI, gamma, m, n, s);
                            final double g = gammaMNS.getValue(m, n, s);
                            if (Double.isInfinite(v)) {
                                Assert.assertTrue(Double.isInfinite(g));
                                Assert.assertTrue(v * g > 0);
                            } else {
                                Assert.assertEquals(v, g, 2.0e-14 * FastMath.abs(v));
                            }
                        }
                    }
                }
            }
        }
    }

    private double naiveValue(final int bigI, final double gamma,
                                       final int m, final int n, final int s) {
        final double f = 1 + bigI * gamma;
        if (s <= -m) {
            return FastMath.pow(-1, m - s ) * FastMath.pow(2, s) * FastMath.pow(f, -bigI * m);
        } else if (s < m) {
            return FastMath.pow(-1, m - s ) * FastMath.pow(2, -m) * naiveRatio(m, n, s) * FastMath.pow(f, bigI * s);
        } else {
            return  FastMath.pow(2, -s) * FastMath.pow(f, bigI * m);
        }
    }

    private double naiveRatio(final int m, final int n, final int s) {
        return (CombinatoricsUtils.factorialDouble(n + m) * CombinatoricsUtils.factorialDouble(n - m)) /
               (CombinatoricsUtils.factorialDouble(n + s) * CombinatoricsUtils.factorialDouble(n - s));
    }

    @Before
    public void setUp() {
        nMax = 12;
        fact = new double[2 * nMax + 1];
        fact[0] = 1;
        for (int i = 1; i < fact.length; ++i) {
            fact[i] = i * fact[i - 1];
        }
    }

}
