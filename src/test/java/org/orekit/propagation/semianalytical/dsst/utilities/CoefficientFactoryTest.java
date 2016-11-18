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

import java.util.Map;
import java.util.TreeMap;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.hipparchus.complex.Complex;
import org.hipparchus.random.MersenneTwister;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;

public class CoefficientFactoryTest {

    private static final double eps0  = 0.;
    private static final double eps10 = 1e-10;
    private static final double eps12 = 1e-12;

    /** Map of the Qns derivatives, for each (n, s) couple. */
    private static Map<NSKey, PolynomialFunction> QNS_MAP = new TreeMap<NSKey, PolynomialFunction>();

    @Test
    public void testVns() {
        final int order = 100;
        TreeMap<NSKey, Double> Vns = CoefficientsFactory.computeVns(order);

        // Odd terms are null
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < i + 1; j++) {
                if ((i - j) % 2 != 0) {
                    Assert.assertEquals(0d, Vns.get(new NSKey(i, j)), eps0);
                }
            }
        }

        // Check the first coefficients :
        Assert.assertEquals(1, Vns.get(new NSKey(0, 0)), eps0);
        Assert.assertEquals(0.5, Vns.get(new NSKey(1, 1)), eps0);
        Assert.assertEquals(-0.5, Vns.get(new NSKey(2, 0)), eps0);
        Assert.assertEquals(1 / 8d, Vns.get(new NSKey(2, 2)), eps0);
        Assert.assertEquals(-1 / 8d, Vns.get(new NSKey(3, 1)), eps0);
        Assert.assertEquals(1 / 48d, Vns.get(new NSKey(3, 3)), eps0);
        Assert.assertEquals(3 / 8d, Vns.get(new NSKey(4, 0)), eps0);
        Assert.assertEquals(-1 / 48d, Vns.get(new NSKey(4, 2)), eps0);
        Assert.assertEquals(1 / 384d, Vns.get(new NSKey(4, 4)), eps0);
        Assert.assertEquals(1 / 16d, Vns.get(new NSKey(5, 1)), eps0);
        Assert.assertEquals(-1 / 384d, Vns.get(new NSKey(5, 3)), eps0);
        Assert.assertEquals(1 / 3840d, Vns.get(new NSKey(5, 5)), eps0);
        Assert.assertEquals(Vns.lastKey().getN(), order - 1);
        Assert.assertEquals(Vns.lastKey().getS(), order - 1);
    }

    /**
     * Test the direct computation method : the getVmns is using the Vns computation to compute the
     * current element
     */
    @Test
    public void testVmns() throws OrekitException {
        Assert.assertEquals(getVmns2(0, 0, 0), CoefficientsFactory.getVmns(0, 0, 0), eps0);
        Assert.assertEquals(getVmns2(0, 1, 1), CoefficientsFactory.getVmns(0, 1, 1), eps0);
        Assert.assertEquals(getVmns2(0, 2, 2), CoefficientsFactory.getVmns(0, 2, 2), eps0);
        Assert.assertEquals(getVmns2(0, 3, 1), CoefficientsFactory.getVmns(0, 3, 1), eps0);
        Assert.assertEquals(getVmns2(0, 3, 3), CoefficientsFactory.getVmns(0, 3, 3), eps0);
        Assert.assertEquals(getVmns2(2, 2, 2), CoefficientsFactory.getVmns(2, 2, 2), eps0);
        final double vmnsp = getVmns2(12, 26, 20);
        Assert.assertEquals(vmnsp,
                            CoefficientsFactory.getVmns(12, 26, 20),
                            FastMath.abs(eps12 * vmnsp));
        final double vmnsm = getVmns2(12, 27, -21);
        Assert.assertEquals(vmnsm,
                            CoefficientsFactory.getVmns(12, 27, -21),
                            Math.abs(eps12 * vmnsm));
    }

    /** Error if m > n */
    @Test(expected = OrekitException.class)
    public void testVmnsError() throws OrekitException {
        // if m > n
        CoefficientsFactory.getVmns(3, 2, 1);
    }

    /**
     * Qns test based on two computation method. As methods are independent, if they give the same
     * results, we assume them to be consistent.
     */
    @Test
    public void testQns() {
        Assert.assertEquals(1., getQnsPolynomialValue(0, 0, 0), 0.);
        // Method comparison :
        final int nmax = 10;
        final int smax = 10;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int g = 0; g < 1000; g++) {
            final double gamma = random.nextDouble();
            double[][] qns = CoefficientsFactory.computeQns(gamma, nmax, smax);
            for (int n = 0; n <= nmax; n++) {
                final int sdim = FastMath.min(smax + 2, n);
                for (int s = 0; s <= sdim; s++) {
                    final double qp = getQnsPolynomialValue(gamma, n, s);
                    Assert.assertEquals(qns[n][s], qp, FastMath.abs(eps10 * qns[n][s]));
                }
            }
        }
    }

    /** Gs and Hs computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void testGsHs() {
        final int s = 50;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final double[][] GH = CoefficientsFactory.computeGsHs(k, h, a, b, s);
            for (int j = 1; j < s; j++) {
                final double[] GsHs = getGsHs(k, h, a, b, j);
                Assert.assertEquals(GsHs[0], GH[0][j], FastMath.abs(eps12 * GsHs[0]));
                Assert.assertEquals(GsHs[1], GH[1][j], FastMath.abs(eps12 * GsHs[1]));
            }
        }
    }

    /**
     * Direct computation for the Vmns coefficient from equation 2.7.1 - (6)
     *
     * @throws OrekitException
     */
    private static double getVmns2(final int m,
                                   final int n,
                                   final int s) throws OrekitException {
        double vmsn = 0d;
        if ((n - s) % 2 == 0) {
            final int coef = (s > 0 || s % 2 == 0) ? 1 : -1;
            final int ss = (s > 0) ? s : -s;
            final double num = FastMath.pow(-1, (n - ss) / 2) *
                               CombinatoricsUtils.factorialDouble(n + ss) *
                               CombinatoricsUtils.factorialDouble(n - ss);
            final double den = FastMath.pow(2, n) *
                               CombinatoricsUtils.factorialDouble(n - m) *
                               CombinatoricsUtils.factorialDouble((n + ss) / 2) *
                               CombinatoricsUtils.factorialDouble((n - ss) / 2);
            vmsn = coef * num / den;
        }
        return vmsn;
    }

    /** Get the Q<sub>ns</sub> value from 2.8.1-(4) evaluated in γ This method is using the
     * Legendre polynomial to compute the Q<sub>ns</sub>'s one. This direct computation method
     * allows to store the polynomials value in a static map. If the Q<sub>ns</sub> had been
     * computed already, they just will be evaluated at γ
     *
     * @param gamma γ angle for which Q<sub>ns</sub> is evaluated
     * @param n n value
     * @param s s value
     * @return the polynomial value evaluated at γ
     */
    private static double getQnsPolynomialValue(final double gamma, final int n, final int s) {
        PolynomialFunction derivative;
        if (QNS_MAP.containsKey(new NSKey(n, s))) {
            derivative = QNS_MAP.get(new NSKey(n, s));
        } else {
            final PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(n);
            derivative = legendre;
            for (int i = 0; i < s; i++) {
                derivative = (PolynomialFunction) derivative.polynomialDerivative();
            }
            QNS_MAP.put(new NSKey(n, s), derivative);
        }
        return derivative.value(gamma);
    }

    /** Compute directly G<sub>s</sub> and H<sub>s</sub> coefficients from equation 3.1-(4).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param s development order
     * @return Array of G<sub>s</sub> and H<sub>s</sub> values for s.<br>
     *         The 1st element contains the G<sub>s</sub> value.
     *         The 2nd element contains the H<sub>s</sub> value.
     */
    private static double[] getGsHs(final double k, final double h,
                                    final double a, final double b, final int s) {
        final Complex as   = new Complex(k, h).pow(s);
        final Complex bs   = new Complex(a, -b).pow(s);
        final Complex asbs = as.multiply(bs);
        return new double[] {asbs.getReal(), asbs.getImaginary()};
    }
}
