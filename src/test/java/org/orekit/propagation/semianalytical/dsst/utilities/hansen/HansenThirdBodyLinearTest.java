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
package org.orekit.propagation.semianalytical.dsst.utilities.hansen;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HansenThirdBodyLinearTest {

    private static double hansen(int n, int s, double chi) {
        if (n == 0 && s == 0) {
            return 1.0;
        } else if (n == 0 && s == 1) {
            return -1.0;
        } else if (n == 1 && s == 0) {
            return 1 + 0.5 * (1 - 1.0 / (chi * chi));
        } else if (n >= 1 && n == s - 1) {
            return (-(2 * (double)s - 1) / (double)s ) * hansen(s - 2, s - 1, chi);
        } else if (n >= 1 && n == s) {
            return ((2 * (double)s + 1) / ((double)s + 1) ) * hansen(s - 1, s, chi);
        } else {
            return ((2 * (double)n + 1) / ((double)n + 1)) * hansen(n - 1, s, chi) -
                   ((((double)n + (double)s) * ((double)n - (double)s)) / ((double)n * ((double)n + 1) * chi * chi)) * hansen(n - 2, s, chi);
        }
    }

    @Test
    public void testLinearVsRecursive00() {
        doTestLinearVsRecursive(0.0, 1.1e-12);
    }

    @Test
    public void testLinearVsRecursive01() {
        doTestLinearVsRecursive(0.1, 2.7e-13);
    }

    @Test
    public void testLinearVsRecursive02() {
        doTestLinearVsRecursive(0.2, 9.5e-14);
    }

    @Test
    public void testLinearVsRecursive03() {
        doTestLinearVsRecursive(0.3, 5.6e-14);
    }

    @Test
    public void testLinearVsRecursive04() {
        doTestLinearVsRecursive(0.4, 1.5e-14);
    }

    @Test
    public void testLinearVsRecursive05() {
        doTestLinearVsRecursive(0.5, 5.9e-15);
    }

    @Test
    public void testLinearVsRecursive06() {
        doTestLinearVsRecursive(0.6, 3.7e-15);
    }

    @Test
    public void testLinearVsRecursive07() {
        doTestLinearVsRecursive(0.7, 1.7e-15);
    }

    @Test
    public void testLinearVsRecursive08() {
        doTestLinearVsRecursive(0.8, 9.5e-16);
    }

    @Test
    public void testLinearVsRecursive09() {
        doTestLinearVsRecursive(0.9, 8.3e-16);
    }

    private void doTestLinearVsRecursive(final double ecc, final double tol) {
        final int N = 22;
        final double chi = 1.0 / FastMath.sqrt(1 - ecc * ecc);
        final HansenThirdBodyLinear[] htbl = new HansenThirdBodyLinear[N + 1];

        for (int s = 0; s <= N; s++) {
            htbl[s] = new HansenThirdBodyLinear(N, s);
            htbl[s].computeInitValues(1 / chi, 1 / (chi * chi), 1 / (chi * chi * chi));
        }

        double maxRelativeError = 0;
        for (int s = 0; s <= N; s++) {
            for (int n = FastMath.max(2, s); n <= N; n++) {
                final double hansenRec = hansen(n, s, chi);
                final double hansenLin = htbl[s].getValue(n, 1 / chi);
                final double relativeError = FastMath.abs((hansenLin - hansenRec) / hansenRec);
                maxRelativeError = FastMath.max(maxRelativeError, relativeError);
            }
        }

        Assertions.assertEquals(0.0, maxRelativeError, tol);

    }


}
