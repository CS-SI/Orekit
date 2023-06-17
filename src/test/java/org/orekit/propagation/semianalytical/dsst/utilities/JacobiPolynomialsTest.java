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

import java.util.Locale;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.random.UniformRandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JacobiPolynomialsTest {

    /** Threshold for test on value. */
    private static final double epsValue      = 1.e-20;
    
    /** Threshold for relative difference on 1st order derivative of jacobi polynomials. */
    // Order 8... ok
    //private static final double epsDerivative = 4.44e-13;
    // Order 16... ok
    private static final double epsDerivative = 1.09e-12;
    // Order 32... bad
    // private static final double epsDerivative = 7.89e-7;
    // Order 64... fails
    // private static final double epsDerivative = 78.44;

    /** Test value and 1st-order derivative of Jacobi polynomials.
     * <p>This test is designed to reproduce the behavior of a 16th order tesseral gravity field
     * <p>Values for the polynomials are uniformly drawn between [-1, 1] to reproduce a random cos
     */
    @Test
    public void testValueAndDerivative() {

        // GIVEN
        // -----

        // Test like a 16x16 gravity field
        final boolean print = true;
        final int maxMdailyTesseralSP = 16;
        final int maxEccPowTesseralSP = 4;

        // Uniform random generator
        final UniformRandomGenerator gen = new UniformRandomGenerator(new Well1024a(0x366a26b94e520f41l));

        // Normalizing value for random numbers
        final double sqrt3 = FastMath.sqrt(3.);

        // WHEN
        // ----

        int nTest = 0;
        for (int m = 1; m <= maxMdailyTesseralSP; m++) {

            for (int s = 0; s <= maxEccPowTesseralSP; s++) {

                final int nmin = FastMath.max(FastMath.max(2, m), FastMath.abs(s));
                final int v = FastMath.abs(m - s);
                final int w = FastMath.abs(m + s);

                for (int n = nmin; n <= maxMdailyTesseralSP; n++) {

                    final int l = FastMath.min(n - m, n - FastMath.abs(s));
                    doTestValueAndDerivativeOnePoint(l, v, w, gen.nextNormalizedDouble() / sqrt3, print);
                    nTest++;
                }                
            }
        }

        // THEN
        // ----

        Assertions.assertEquals(668, nTest);
    }

    /** Test value and derivative for a given polynomial and input value.
     * <p>Functions {@link JacobiPolynomials#getValueAndDerivative(int, int, int, double)} is compared to its "Gradient" counterpart.
     * @param l degree of the polynomial
     * @param v v value
     * @param w w value
     * @param x value to evaluate the polynomial on
     * @param print print the differences
     */
    private void doTestValueAndDerivativeOnePoint(final int l, final int v, final int w, final double x, final boolean print) {

        // WHEN
        // ----

        final double[] test = JacobiPolynomials.getValueAndDerivative(l, v, w, x);
        final Gradient ref  = JacobiPolynomials.getValue(l, v, w, Gradient.variable(1, 0, x));

        // THEN
        // ----

        final double refValue  = ref.getValue();
        final double testValue = test[0];

        final double refDer  = ref.getPartialDerivative(0);
        final double testDer = test[1];

        if (print) {
            System.out.format(Locale.US,"\tvalue : %n\t\tref  = %15.12e%n\t\ttest = %15.12e%n\t\tdiff = %15.12e%n",
                              refValue, testValue, FastMath.abs(refValue - testValue));
            System.out.format(Locale.US,"\tderiv : %n\t\tref  = %15.12e%n\t\ttest = %15.12e%n\t\tdiff = %15.12e%n",
                              refDer, testDer, FastMath.abs(refDer - testDer));
        }

        // Test value directly (absolute difference, should be a clean 0.)
        Assertions.assertEquals(refValue, testValue, epsValue);

        // Test relative difference for derivative (if possible)
        if (FastMath.abs(refDer) > Precision.SAFE_MIN) {
            Assertions.assertEquals(0., FastMath.abs((testDer - refDer) / refDer), epsDerivative);
        } else {
            // Absolute difference for 0 values
            Assertions.assertEquals(refDer, testDer, epsValue);
        }
    }
}
