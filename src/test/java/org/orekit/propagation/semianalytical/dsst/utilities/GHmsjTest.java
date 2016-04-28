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

import org.hipparchus.complex.Complex;
import org.hipparchus.random.MersenneTwister;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;

public class GHmsjTest {

    private static final double eps = 1e-10;

    /** Gmsj and Hmsj computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void testGHmsj() {
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(123456);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final GHmsjPolynomials gMSJ = new GHmsjPolynomials(k, h, a, b, 1);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final double[] GHmsj = getGHmsj(k, h, a, b, m, s, j);
                    Assert.assertEquals(GHmsj[0], gMSJ.getGmsj(m, s, j), FastMath.abs(eps * GHmsj[0]));
                    Assert.assertEquals(GHmsj[1], gMSJ.getHmsj(m, s, j), FastMath.abs(eps * GHmsj[1]));
                }
            }
        }
    }

    /** dG/dk and dH/dk computations test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void testdGHdk() {
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(456789);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final GHmsjPolynomials gMSJ = new GHmsjPolynomials(k, h, a, b, 1);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final double[] dGHdk = getdGHdk(k, h, a, b, m, s, j);
                    Assert.assertEquals(dGHdk[0], gMSJ.getdGmsdk(m, s, j), FastMath.abs(eps * dGHdk[0]));
                    Assert.assertEquals(dGHdk[1], gMSJ.getdHmsdk(m, s, j), FastMath.abs(eps * dGHdk[1]));
                }
            }
        }
    }

    /** dG/dh computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void testdGHdh() {
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(123789);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final GHmsjPolynomials gMSJ = new GHmsjPolynomials(k, h, a, b, 1);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final double[] dGHdh = getdGHdh(k, h, a, b, m, s, j);
                    Assert.assertEquals(dGHdh[0], gMSJ.getdGmsdh(m, s, j), FastMath.abs(eps * dGHdh[0]));
                    Assert.assertEquals(dGHdh[1], gMSJ.getdHmsdh(m, s, j), FastMath.abs(eps * dGHdh[1]));
                }
            }
        }
    }

    /** dG/dα and dH/dα computations test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void testdGHdAlpha() {
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final GHmsjPolynomials gMSJ = new GHmsjPolynomials(k, h, a, b, 1);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final double[] dGHda = getdGHda(k, h, a, b, m, s, j);
                    Assert.assertEquals(dGHda[0], gMSJ.getdGmsdAlpha(m, s, j), FastMath.abs(eps * dGHda[0]));
                    Assert.assertEquals(dGHda[1], gMSJ.getdHmsdAlpha(m, s, j), FastMath.abs(eps * dGHda[1]));
                }
            }
        }
    }

    /** dG/dβ and dH/dβ computations test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void testdGHdBeta() {
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(987654321);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final GHmsjPolynomials gMSJ = new GHmsjPolynomials(k, h, a, b, 1);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final double[] dGHdb = getdGHdb(k, h, a, b, m, s, j);
                    Assert.assertEquals(dGHdb[0], gMSJ.getdGmsdBeta(m, s, j), FastMath.abs(eps * dGHdb[0]));
                    Assert.assertEquals(dGHdb[1], gMSJ.getdHmsdBeta(m, s, j), FastMath.abs(eps * dGHdb[1]));
                }
            }
        }
    }

    /** Compute directly G<sup>j</sup><sub>ms</sub> and H<sup>j</sup><sub>ms</sub> from equation 2.7.1-(14).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param m order
     * @param s d'Alembert characteristic
     * @param j index
     * @return G<sub>ms</sub><sup>j</sup> and H<sup>j</sup><sub>ms</sub> values
     */
    private static double[] getGHmsj(final double k, final double h,
                                     final double a, final double b,
                                     final int m, final int s, final int j) {
        final Complex kh = new Complex(k, h * sgn(s - j)).pow(FastMath.abs(s - j));
        Complex ab;
        if (FastMath.abs(s) < m) {
            ab = new Complex(a, b).pow(m - s);
        } else {
            ab = new Complex(a, -b * sgn(s - m)).pow(FastMath.abs(s - m));
        }
        final Complex khab = kh.multiply(ab);

        return new double[] {khab.getReal(), khab.getImaginary()};
    }

    /** Compute directly dG/dk and dH/dk from equation 2.7.1-(14).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param m order
     * @param s d'Alembert characteristic
     * @param j index
     * @return dG/dk and dH/dk values
     */
    private static double[] getdGHdk(final double k, final double h,
                                     final double a, final double b,
                                     final int m, final int s, final int j) {
        final Complex kh = new Complex(k, h * sgn(s - j)).pow(FastMath.abs(s - j)-1).multiply(FastMath.abs(s - j));
        Complex ab;
        if (FastMath.abs(s) < m) {
            ab = new Complex(a, b).pow(m - s);
        } else {
            ab = new Complex(a, -b * sgn(s - m)).pow(FastMath.abs(s - m));
        }
        final Complex khab = kh.multiply(ab);

        return new double[] {khab.getReal(), khab.getImaginary()};
    }

    /** Compute directly dG/dh and dH/dh from equation 2.7.1-(14).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param m order
     * @param s d'Alembert characteristic
     * @param j index
     * @return dG/dh and dH/dh values
     */
    private static double[] getdGHdh(final double k, final double h,
                                     final double a, final double b,
                                     final int m, final int s, final int j) {
        final Complex kh1 = new Complex(k, h * sgn(s - j)).pow(FastMath.abs(s - j)-1);
        final Complex kh2 = new Complex(0., FastMath.abs(s - j) * sgn(s - j));
        final Complex kh = kh1.multiply(kh2);
        Complex ab;
        if (FastMath.abs(s) < m) {
            ab = new Complex(a, b).pow(m - s);
        } else {
            ab = new Complex(a, -b * sgn(s - m)).pow(FastMath.abs(s - m));
        }
        final Complex khab = kh.multiply(ab);

        return new double[] {khab.getReal(), khab.getImaginary()};
    }

    /** Compute directly dG/dα and dH/dα from equation 2.7.1-(14).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param m order
     * @param s d'Alembert characteristic
     * @param j index
     * @return dG/dα and dH/dα values
     */
    private static double[] getdGHda(final double k, final double h,
                                     final double a, final double b,
                                     final int m, final int s, final int j) {
        final Complex kh = new Complex(k, h * sgn(s - j)).pow(FastMath.abs(s - j));
        Complex ab;
        if (FastMath.abs(s) < m) {
            ab = new Complex(a, b).pow(m - s - 1).multiply(m - s);
        } else {
            ab = new Complex(a, -b * sgn(s - m)).pow(FastMath.abs(s - m) - 1).multiply(FastMath.abs(s - m));
        }
        final Complex khab = kh.multiply(ab);

        return new double[] {khab.getReal(), khab.getImaginary()};
    }

    /** Compute directly dG/dβ and dH/dβ from equation 2.7.1-(14).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param m order
     * @param s d'Alembert characteristic
     * @param j index
     * @return dG/dβ and dH/dβ values
     */
    private static double[] getdGHdb(final double k, final double h,
                                     final double a, final double b,
                                     final int m, final int s, final int j) {
        final Complex kh = new Complex(k, h * sgn(s - j)).pow(FastMath.abs(s - j));
        Complex ab;
        if (FastMath.abs(s) < m) {
            Complex ab1 = new Complex(a, b).pow(m - s - 1);
            Complex ab2 = new Complex(0., m - s);
            ab = ab1.multiply(ab2);
        } else {
            Complex ab1 = new Complex(a, -b * sgn(s - m)).pow(FastMath.abs(s - m) - 1);
            Complex ab2 = new Complex(0., FastMath.abs(s - m) * sgn(m - s));
            ab = ab1.multiply(ab2);
        }
        final Complex khab = kh.multiply(ab);

        return new double[] {khab.getReal(), khab.getImaginary()};
    }

    /** Get the sign of an integer.
     *  @param i number on which evaluation is done
     *  @return -1 or +1 depending on sign of i
     */
    private static int sgn(final int i) {
        return (i < 0) ? -1 : 1;
    }
}
