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

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.hipparchus.complex.Complex;
import org.hipparchus.exception.NullArgumentException;
import org.hipparchus.random.MersenneTwister;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        SortedMap<NSKey, Double> Vns = CoefficientsFactory.computeVns(order);

        // Odd terms are null
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < i + 1; j++) {
                if ((i - j) % 2 != 0) {
                    Assertions.assertEquals(0d, Vns.get(new NSKey(i, j)), eps0);
                }
            }
        }

        // Check the first coefficients :
        Assertions.assertEquals(1, Vns.get(new NSKey(0, 0)), eps0);
        Assertions.assertEquals(0.5, Vns.get(new NSKey(1, 1)), eps0);
        Assertions.assertEquals(-0.5, Vns.get(new NSKey(2, 0)), eps0);
        Assertions.assertEquals(1 / 8d, Vns.get(new NSKey(2, 2)), eps0);
        Assertions.assertEquals(-1 / 8d, Vns.get(new NSKey(3, 1)), eps0);
        Assertions.assertEquals(1 / 48d, Vns.get(new NSKey(3, 3)), eps0);
        Assertions.assertEquals(3 / 8d, Vns.get(new NSKey(4, 0)), eps0);
        Assertions.assertEquals(-1 / 48d, Vns.get(new NSKey(4, 2)), eps0);
        Assertions.assertEquals(1 / 384d, Vns.get(new NSKey(4, 4)), eps0);
        Assertions.assertEquals(1 / 16d, Vns.get(new NSKey(5, 1)), eps0);
        Assertions.assertEquals(-1 / 384d, Vns.get(new NSKey(5, 3)), eps0);
        Assertions.assertEquals(1 / 3840d, Vns.get(new NSKey(5, 5)), eps0);
        Assertions.assertEquals(Vns.lastKey().getN(), order - 1);
        Assertions.assertEquals(Vns.lastKey().getS(), order - 1);
    }

    /**
     * Test the direct computation method : the getVmns is using the Vns computation to compute the
     * current element
     */
    @Test
    public void testVmns() {
        Assertions.assertEquals(getVmns2(0, 0, 0), CoefficientsFactory.getVmns(0, 0, 0), eps0);
        Assertions.assertEquals(getVmns2(0, 1, 1), CoefficientsFactory.getVmns(0, 1, 1), eps0);
        Assertions.assertEquals(getVmns2(0, 2, 2), CoefficientsFactory.getVmns(0, 2, 2), eps0);
        Assertions.assertEquals(getVmns2(0, 3, 1), CoefficientsFactory.getVmns(0, 3, 1), eps0);
        Assertions.assertEquals(getVmns2(0, 3, 3), CoefficientsFactory.getVmns(0, 3, 3), eps0);
        Assertions.assertEquals(getVmns2(2, 2, 2), CoefficientsFactory.getVmns(2, 2, 2), eps0);
        final double vmnsp = getVmns2(12, 26, 20);
        Assertions.assertEquals(vmnsp,
                            CoefficientsFactory.getVmns(12, 26, 20),
                            FastMath.abs(eps12 * vmnsp));
        final double vmnsm = getVmns2(12, 27, -21);
        Assertions.assertEquals(vmnsm,
                            CoefficientsFactory.getVmns(12, 27, -21),
                            Math.abs(eps12 * vmnsm));
    }

    /** Error if m > n */
    @Test
    public void testVmnsError() {
        Assertions.assertThrows(OrekitException.class, () -> {
            // if m > n
            CoefficientsFactory.getVmns(3, 2, 1);
        });
    }

    @Test
    public void testKey() {
        // test cases mostly written to improve coverage and make SonarQube happy...
        NSKey key21 = new NSKey(2, 1);
        Assertions.assertEquals(key21, key21);
        Assertions.assertEquals(key21, new NSKey(2, 1));
        Assertions.assertNotEquals(key21, null);
        Assertions.assertNotEquals(key21, new NSKey(2, 0));
        Assertions.assertNotEquals(key21, new NSKey(3, 1));
        Assertions.assertEquals(-1719365209, key21.hashCode());
        Assertions.assertEquals(-1719365465, new NSKey(3, 1).hashCode());
    }

    /**
     * Qns test based on two computation method. As methods are independent, if they give the same
     * results, we assume them to be consistent.
     */
    @Test
    public void testQns() {
        Assertions.assertEquals(1., getQnsPolynomialValue(0, 0, 0), 0.);
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
                    Assertions.assertEquals(qns[n][s], qp, FastMath.abs(eps10 * qns[n][s]));
                }
            }
        }
    }

    @Test
    public void testQnsField() {
        doTestQnsField(Binary64Field.getInstance());
    }

    /**
     * Qns test based on two computation method. As methods are independent, if they give the same
     * results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestQnsField(Field<T> field) {
        final T zero = field.getZero();
        Assertions.assertEquals(1., getQnsPolynomialValue(0, 0, 0), 0.);
        // Method comparison :
        final int nmax = 10;
        final int smax = 10;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int g = 0; g < 1000; g++) {
            final T gamma = zero.add(random.nextDouble());
            T[][] qns = CoefficientsFactory.computeQns(gamma, nmax, smax);
            for (int n = 0; n <= nmax; n++) {
                final int sdim = FastMath.min(smax + 2, n);
                for (int s = 0; s <= sdim; s++) {
                    final T qp = getQnsPolynomialValue(gamma, n, s);
                    Assertions.assertEquals(qns[n][s].getReal(), qp.getReal(), FastMath.abs(qns[n][s].multiply(eps10)).getReal());
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
                Assertions.assertEquals(GsHs[0], GH[0][j], FastMath.abs(eps12 * GsHs[0]));
                Assertions.assertEquals(GsHs[1], GH[1][j], FastMath.abs(eps12 * GsHs[1]));
            }
        }
    }

    @Test
    public void testGsHsField() {
        doTestGsHsField(Binary64Field.getInstance());
    }

    /** Gs and Hs computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestGsHsField(Field<T> field) {
        final T zero = field.getZero();
        final int s = 50;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int i = 0; i < 10; i++) {
            final T k = zero.add(random.nextDouble());
            final T h = zero.add(random.nextDouble());
            final T a = zero.add(random.nextDouble());
            final T b = zero.add(random.nextDouble());
            final T[][] GH = CoefficientsFactory.computeGsHs(k, h, a, b, s, field);
            for (int j = 1; j < s; j++) {
                final T[] GsHs = getGsHs(k, h, a, b, j, field);
                Assertions.assertEquals(GsHs[0].getReal(), GH[0][j].getReal(), FastMath.abs(GsHs[0].multiply(eps12)).getReal());
                Assertions.assertEquals(GsHs[1].getReal(), GH[1][j].getReal(), FastMath.abs(GsHs[1].multiply(eps12)).getReal());
            }
        }
    }

    /**
     * Direct computation for the Vmns coefficient from equation 2.7.1 - (6)
     */
    private static double getVmns2(final int m,
                                   final int n,
                                   final int s) {
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
    private static <T extends CalculusFieldElement<T>> T getQnsPolynomialValue(final T gamma, final int n, final int s) {
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
    private static <T extends CalculusFieldElement<T>> T[] getGsHs(final T k, final T h,
                                    final T a, final T b, final int s,
                                    final Field<T> field) {
        final FieldComplex<T> as   = new FieldComplex<>(k, h).pow(s);
        final FieldComplex<T> bs   = new FieldComplex<>(a, b.negate()).pow(s);
        final FieldComplex<T> asbs = as.multiply(bs);
        final T[] values = MathArrays.buildArray(field, 2);
        values[0] = asbs.getReal();
        values[1] = asbs.getImaginary();
        return values;
    }

    private static class FieldComplex <T extends CalculusFieldElement<T>> {

        /** The imaginary part. */
        private final T imaginary;

        /** The real part. */
        private final T real;

        /**
         * Create a complex number given the real and imaginary parts.
         *
         * @param real Real part.
         * @param imaginary Imaginary part.
         */
        FieldComplex(final T real, final T imaginary) {
            this.real = real;
            this.imaginary = imaginary;
        }

        /**
         * Access the real part.
         *
         * @return the real part.
         */
        public T getReal() {
            return real;
        }

        /**
         * Access the imaginary part.
         *
         * @return the imaginary part.
         */
        public T getImaginary() {
            return imaginary;
        }

        /**
         * Create a complex number given the real and imaginary parts.
         *
         * @param realPart Real part.
         * @param imaginaryPart Imaginary part.
         * @return a new complex number instance.
         *
         * @see #valueOf(double, double)
         */
        protected FieldComplex<T> createComplex(final T realPart, final T imaginaryPart) {
            return new FieldComplex<>(realPart, imaginaryPart);
        }

        /**
         * Returns a {@code Complex} whose value is {@code this * factor}.
         * Implements preliminary checks for {@code NaN} and infinity followed by
         * the definitional formula:
         * <p>
         *   {@code (a + bi)(c + di) = (ac - bd) + (ad + bc)i}
         * </p>
         * <p>
         * Returns finite values in components of the result per the definitional
         * formula in all remaining cases.</p>
         *
         * @param  factor value to be multiplied by this {@code Complex}.
         * @return {@code this * factor}.
         * @throws NullArgumentException if {@code factor} is {@code null}.
         */
        public FieldComplex<T> multiply(final FieldComplex<T> factor) throws NullArgumentException {
            return createComplex(real.multiply(factor.real).subtract(imaginary.multiply(factor.imaginary)),
                                 real.multiply(factor.imaginary).add(imaginary.multiply(factor.real)));
        }

        /**
         * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor}
         * interpreted as a integer number.
         *
         * @param  factor value to be multiplied by this {@code Complex}.
         * @return {@code this * factor}.
         * @see #multiply(Complex)
         */
        public FieldComplex<T> multiply(final int factor) {
            return createComplex(real.multiply(factor), imaginary.multiply(factor));
        }

        /**
         * Returns of value of this complex number raised to the power of {@code x}.
         *
         * @param  x exponent to which this {@code Complex} is to be raised.
         * @return <code>this<sup>x</sup></code>.
         * @see #pow(Complex)
         */
         public FieldComplex<T> pow(int x) {
            return this.log().multiply(x).exp();
        }

         /**
          * Compute the
          * <a href="http://mathworld.wolfram.com/NaturalLogarithm.html" TARGET="_top">
          * natural logarithm</a> of this complex number.
          * Implements the formula:
          * <pre>
          *  <code>
          *   log(a + bi) = ln(|a + bi|) + arg(a + bi)i
          *  </code>
          * </pre>
          * where ln on the right hand side is {@link FastMath#log},
          * {@code |a + bi|} is the modulus, {@link Complex#abs},  and
          * {@code arg(a + bi) = }{@link FastMath#atan2}(b, a).
          * <p>
          * Returns {@link Complex#NaN} if either real or imaginary part of the
          * input argument is {@code NaN}.
          * </p>
          * Infinite (or critical) values in real or imaginary parts of the input may
          * result in infinite or NaN values returned in parts of the result.
          * <pre>
          *  Examples:
          *  <code>
          *   log(1 &plusmn; INFINITY i) = INFINITY &plusmn; (&pi;/2)i
          *   log(INFINITY + i) = INFINITY + 0i
          *   log(-INFINITY + i) = INFINITY + &pi;i
          *   log(INFINITY &plusmn; INFINITY i) = INFINITY &plusmn; (&pi;/4)i
          *   log(-INFINITY &plusmn; INFINITY i) = INFINITY &plusmn; (3&pi;/4)i
          *   log(0 + 0i) = -INFINITY + 0i
          *  </code>
          * </pre>
          *
          * @return the value <code>ln &nbsp; this</code>, the natural logarithm
          * of {@code this}.
          */
         public FieldComplex<T> log() {
             return createComplex(FastMath.log(abs()),
                                  FastMath.atan2(imaginary, real));
         }

         /**
          * Compute the
          * <a href="http://mathworld.wolfram.com/ExponentialFunction.html" TARGET="_top">
          * exponential function</a> of this complex number.
          * Implements the formula:
          * <pre>
          *  <code>
          *   exp(a + bi) = exp(a)cos(b) + exp(a)sin(b)i
          *  </code>
          * </pre>
          * where the (real) functions on the right-hand side are
          * {@link FastMath#exp}, {@link FastMath#cos}, and
          * {@link FastMath#sin}.
          * <p>
          * Returns {@link Complex#NaN} if either real or imaginary part of the
          * input argument is {@code NaN}.
          * </p>
          * Infinite values in real or imaginary parts of the input may result in
          * infinite or NaN values returned in parts of the result.
          * <pre>
          *  Examples:
          *  <code>
          *   exp(1 &plusmn; INFINITY i) = NaN + NaN i
          *   exp(INFINITY + i) = INFINITY + INFINITY i
          *   exp(-INFINITY + i) = 0 + 0i
          *   exp(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
          *  </code>
          * </pre>
          *
          * @return <code><i>e</i><sup>this</sup></code>.
          */
         public FieldComplex<T> exp() {
             T expReal = FastMath.exp(real);
             return createComplex(expReal.multiply(FastMath.cos(imaginary)),
                                  expReal.multiply(FastMath.sin(imaginary)));
         }

         /**
          * Return the absolute value of this complex number.
          * Returns {@code NaN} if either real or imaginary part is {@code NaN}
          * and {@code Double.POSITIVE_INFINITY} if neither part is {@code NaN},
          * but at least one part is infinite.
          *
          * @return the absolute value.
          */
         public T abs() {
             if (FastMath.abs(real).getReal() < FastMath.abs(imaginary).getReal()) {
                 if (imaginary.getReal() == 0.0) {
                     return FastMath.abs(real);
                 }
                 T q = real.divide(imaginary);
                 return FastMath.abs(imaginary).multiply(FastMath.sqrt(q.multiply(q).add(1.)));
             } else {
                 if (real.getReal() == 0.0) {
                     return FastMath.abs(imaginary);
                 }
                 T q = imaginary.divide(real);
                 return FastMath.abs(real).multiply(FastMath.sqrt(q.multiply(q).add(1.)));
             }
         }

    }
}
