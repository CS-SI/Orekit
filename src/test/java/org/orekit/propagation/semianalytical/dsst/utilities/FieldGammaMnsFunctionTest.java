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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FieldGammaMnsFunctionTest {

    int      nMax;

    @Test
    public void testIndex()
        throws NoSuchMethodException, SecurityException,
               IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method indexM = FieldGammaMnsFunction.class.getDeclaredMethod("index",
                                                                 Integer.TYPE, Integer.TYPE, Integer.TYPE);
        indexM.setAccessible(true);
        int i = 0;
        for (int n = 0; n <= nMax; ++n) {
            for (int m = 0; m <= n; ++m) {
                for (int s = -n; s <= n; ++s) {
                    Assertions.assertEquals(i++, indexM.invoke(null, m, n, s));
                }
            }
        }
    }

    @Test
    public void testPrecomputedRatios()
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        doTestPrecomputedRatios(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPrecomputedRatios(Field<T> field)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        final T zero = field.getZero();
        java.lang.reflect.Field precomputedF = FieldGammaMnsFunction.class.getDeclaredField("PRECOMPUTED_RATIOS");
        precomputedF.setAccessible(true);
        new FieldGammaMnsFunction<>(nMax, zero.add(0.5), +1, field);
        double[] precomputed = (double[]) precomputedF.get(null);
        int i = 0;
        for (int n = 0; n <= nMax; ++n) {
            for (int m = 0; m <= n; ++m) {
                for (int s = -n; s <= n; ++s) {
                    // compare against naive implementation
                    double r = naiveRatio(m, n, s);
                    Assertions.assertEquals(r, precomputed[i++], 2.0e-14 * r);
                }
            }
        }
    }

    @Test
    public void testReallocate()
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        doTestReallocate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestReallocate(Field<T> field)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        final T zero = field.getZero();
        java.lang.reflect.Field precomputedF = FieldGammaMnsFunction.class.getDeclaredField("PRECOMPUTED_RATIOS");
        precomputedF.setAccessible(true);
        precomputedF.set(null, new double[0]);
        new FieldGammaMnsFunction<>(nMax, zero.add(0.5), +1, field);
        double[] orginalPrecomputed = (double[]) precomputedF.get(null);
        Assertions.assertEquals((nMax + 1) * (nMax + 2) * (4 * nMax + 3) / 6, orginalPrecomputed.length);
        new FieldGammaMnsFunction<>(nMax + 3, zero.add(0.5), +1, field);
        double[] reallocatedPrecomputed = (double[]) precomputedF.get(null);
        Assertions.assertEquals((nMax + 4) * (nMax + 5) * (4 * nMax + 15) / 6, reallocatedPrecomputed.length);
        for (int i = 0; i < orginalPrecomputed.length; ++i) {
            Assertions.assertEquals(orginalPrecomputed[i], reallocatedPrecomputed[i],
                                1.0e-15 * orginalPrecomputed[i]);
        }
    }


    @Test
    public void testValue() {
        doTestValue(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestValue(Field<T> field) {
        final T zero = field.getZero();
        for (int bigI : new int[] { -1, +1 }) {
            for (T gamma = zero; gamma.getReal() <= 1; gamma = gamma.add(1.0 / 64.0)) {
                FieldGammaMnsFunction<T> gammaMNS = new FieldGammaMnsFunction<>(nMax, gamma, bigI, field);
                for (int n = 0; n <= nMax; ++n) {
                    for (int m = 0; m <= n; ++m) {
                        for (int s = -n; s <= n; ++s) {
                            // compare against naive implementation
                            final T v = naiveValue(bigI, gamma, m, n, s, field);
                            final T g = gammaMNS.getValue(m, n, s);
                            if (Double.isInfinite(v.getReal())) {
                                Assertions.assertTrue(Double.isInfinite(g.getReal()));
                                Assertions.assertTrue(v.multiply(g).getReal() > 0);
                            } else {
                                Assertions.assertEquals(v.getReal(), g.getReal(), FastMath.abs(v).multiply(2.0e-14).getReal());
                            }
                        }
                    }
                }
            }
        }
    }

    private <T extends CalculusFieldElement<T>> T naiveValue(final int bigI, final T gamma,
                                       final int m, final int n, final int s,
                                       final Field<T> field) {
        final T zero = field.getZero();
        final T f = gamma.multiply(bigI).add(1.);
        if (s <= -m) {
            return FastMath.pow(zero.subtract(1.), zero.add(m - s)).multiply(FastMath.pow(zero.add(2.), zero.add(s))).multiply(FastMath.pow(f, -bigI * m));
        } else if (s < m) {
            return FastMath.pow(zero.subtract(1.), zero.add(m - s)).multiply(FastMath.pow(zero.add(2), zero.subtract(m))).multiply(naiveRatio(m, n, s)).multiply(FastMath.pow(f, bigI * s));
        } else {
            return  FastMath.pow(zero.add(2.), zero.subtract(s)).multiply(FastMath.pow(f, bigI * m));
        }
    }

    private double naiveRatio(final int m, final int n, final int s) {
        return (CombinatoricsUtils.factorialDouble(n + m) * CombinatoricsUtils.factorialDouble(n - m)) /
               (CombinatoricsUtils.factorialDouble(n + s) * CombinatoricsUtils.factorialDouble(n - s));
    }

    @BeforeEach
    public void setUp() {
        nMax = 12;
    }
}
