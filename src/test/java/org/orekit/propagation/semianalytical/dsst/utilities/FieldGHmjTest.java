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
import org.hipparchus.complex.Complex;
import org.hipparchus.exception.NullArgumentException;
import org.hipparchus.random.MersenneTwister;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FieldGHmjTest {

    private static final double eps = 1e-10;

    @Test
    public void testGHmsj() {
        doTestGHmsj(Binary64Field.getInstance());
    }

    /** Gmsj and Hmsj computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestGHmsj(Field<T> field) {
        final T zero = field.getZero();
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(123456);
        for (int i = 0; i < 10; i++) {
            final T k = zero.add(random.nextDouble());
            final T h = zero.add(random.nextDouble());
            final T a = zero.add(random.nextDouble());
            final T b = zero.add(random.nextDouble());
            final FieldGHmsjPolynomials<T> gMSJ = new FieldGHmsjPolynomials<>(k, h, a, b, 1, field);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    T[] GHmsj = MathArrays.buildArray(field, 2);
                    GHmsj = getGHmsj(k, h, a, b, m, s, j, field);
                    Assertions.assertEquals(GHmsj[0].getReal(), gMSJ.getGmsj(m, s, j).getReal(), FastMath.abs(GHmsj[0].multiply(eps)).getReal());
                    Assertions.assertEquals(GHmsj[1].getReal(), gMSJ.getHmsj(m, s, j).getReal(), FastMath.abs(GHmsj[1].multiply(eps)).getReal());
                }
            }
        }
    }

    @Test
    public void testdGHdk() {
        doTestdGHdk(Binary64Field.getInstance());
    }

    /** dG/dk and dH/dk computations test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestdGHdk(Field<T> field) {
        final T zero = field.getZero();
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(456789);
        for (int i = 0; i < 10; i++) {
            final T k = zero.add(random.nextDouble());
            final T h = zero.add(random.nextDouble());
            final T a = zero.add(random.nextDouble());
            final T b = zero.add(random.nextDouble());
            final FieldGHmsjPolynomials<T> gMSJ = new FieldGHmsjPolynomials<>(k, h, a, b, 1, field);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final T[] dGHdk = getdGHdk(k, h, a, b, m, s, j, field);
                    Assertions.assertEquals(dGHdk[0].getReal(), gMSJ.getdGmsdk(m, s, j).getReal(), FastMath.abs(dGHdk[0].multiply(eps)).getReal());
                    Assertions.assertEquals(dGHdk[1].getReal(), gMSJ.getdHmsdk(m, s, j).getReal(), FastMath.abs(dGHdk[1].multiply(eps)).getReal());
                }
            }
        }
    }

    @Test
    public void testdGHdh() {
        doTestdGHdh(Binary64Field.getInstance());
    }

    /** dG/dh computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestdGHdh(Field<T> field) {
        final T zero = field.getZero();
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(123789);
        for (int i = 0; i < 10; i++) {
            final T k = zero.add(random.nextDouble());
            final T h = zero.add(random.nextDouble());
            final T a = zero.add(random.nextDouble());
            final T b = zero.add(random.nextDouble());
            final FieldGHmsjPolynomials<T> gMSJ = new FieldGHmsjPolynomials<>(k, h, a, b, 1, field);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final T[] dGHdh = getdGHdh(k, h, a, b, m, s, j, field);
                    Assertions.assertEquals(dGHdh[0].getReal(), gMSJ.getdGmsdh(m, s, j).getReal(), FastMath.abs(dGHdh[0].multiply(eps)).getReal());
                    Assertions.assertEquals(dGHdh[1].getReal(), gMSJ.getdHmsdh(m, s, j).getReal(), FastMath.abs(dGHdh[1].multiply(eps)).getReal());
                }
            }
        }
    }

    @Test
    public void testdGHdAlpha() {
        doTestdGHdAlpha(Binary64Field.getInstance());
    }

    /** dG/dα and dH/dα computations test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestdGHdAlpha(Field<T> field) {
        final T zero = field.getZero();
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int i = 0; i < 10; i++) {
            final T k = zero.add(random.nextDouble());
            final T h = zero.add(random.nextDouble());
            final T a = zero.add(random.nextDouble());
            final T b = zero.add(random.nextDouble());
            final FieldGHmsjPolynomials<T> gMSJ = new FieldGHmsjPolynomials<>(k, h, a, b, 1, field);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final T[] dGHda = getdGHda(k, h, a, b, m, s, j, field);
                    Assertions.assertEquals(dGHda[0].getReal(), gMSJ.getdGmsdAlpha(m, s, j).getReal(), FastMath.abs(dGHda[0].multiply(eps)).getReal());
                    Assertions.assertEquals(dGHda[1].getReal(), gMSJ.getdHmsdAlpha(m, s, j).getReal(), FastMath.abs(dGHda[1].multiply(eps)).getReal());
                }
            }
        }
    }

    @Test
    public void testdGHdBeta() {
        doTestdGHdBeta(Binary64Field.getInstance());
    }

    /** dG/dβ and dH/dβ computations test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    private <T extends CalculusFieldElement<T>> void doTestdGHdBeta(Field<T> field) {
        final T zero = field.getZero();
        final int sMax = 30;
        final int mMax = 20;
        final MersenneTwister random = new MersenneTwister(987654321);
        for (int i = 0; i < 10; i++) {
            final T k = zero.add(random.nextDouble());
            final T h = zero.add(random.nextDouble());
            final T a = zero.add(random.nextDouble());
            final T b = zero.add(random.nextDouble());
            final FieldGHmsjPolynomials<T> gMSJ = new FieldGHmsjPolynomials<>(k, h, a, b, 1, field);
            for (int s = -sMax; s <= sMax; s++) {
                for (int m = 2; m <= mMax; m+=2) {
                    final int j = m / 2;
                    final T[] dGHdb = getdGHdb(k, h, a, b, m, s, j, field);
                    Assertions.assertEquals(dGHdb[0].getReal(), gMSJ.getdGmsdBeta(m, s, j).getReal(), FastMath.abs(dGHdb[0].multiply(eps)).getReal());
                    Assertions.assertEquals(dGHdb[1].getReal(), gMSJ.getdHmsdBeta(m, s, j).getReal(), FastMath.abs(dGHdb[1].multiply(eps)).getReal());
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
    private static <T extends CalculusFieldElement<T>> T[] getGHmsj(final T k, final T h,
                                     final T a, final T b,
                                     final int m, final int s, final int j,
                                     final Field<T> field) {
        final FieldComplex<T> kh = new FieldComplex<>(k, h.multiply(sgn(s - j))).pow(FastMath.abs(s - j));
        FieldComplex<T> ab;
        if (FastMath.abs(s) < m) {
            ab = new FieldComplex<>(a, b).pow(m - s);
        } else {
            ab = new FieldComplex<>(a, b.multiply(sgn(s - m)).negate()).pow(FastMath.abs(s - m));
        }
        final FieldComplex<T> khab = kh.multiply(ab);

        final T[] values = MathArrays.buildArray(field, 2);
        values[0] = khab.getReal();
        values[1] = khab.getImaginary();
        return values;
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
    private static <T extends CalculusFieldElement<T>> T[] getdGHdk(final T k, final T h,
                                     final T a, final T b,
                                     final int m, final int s, final int j,
                                     final Field<T> field) {
        final FieldComplex<T> kh = new FieldComplex<>(k, h.multiply(sgn(s - j))).pow(FastMath.abs(s - j)-1).multiply(FastMath.abs(s - j));
        FieldComplex<T> ab;
        if (FastMath.abs(s) < m) {
            ab = new FieldComplex<>(a, b).pow(m - s);
        } else {
            ab = new FieldComplex<>(a, b.multiply(sgn(s - m)).negate()).pow(FastMath.abs(s - m));
        }
        final FieldComplex<T> khab = kh.multiply(ab);

        final T[] values = MathArrays.buildArray(field, 2);
        values[0] = khab.getReal();
        values[1] = khab.getImaginary();
        return values;
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
    private static <T extends CalculusFieldElement<T>> T[] getdGHdh(final T k, final T h,
                                     final T a, final T b,
                                     final int m, final int s, final int j,
                                     final Field<T> field) {
        final T zero = field.getZero();
        final FieldComplex<T> kh1 = new FieldComplex<>(k, h.multiply(sgn(s - j))).pow(FastMath.abs(s - j)-1);
        final FieldComplex<T> kh2 = new FieldComplex<>(zero, FastMath.abs(zero.add(s - j)).multiply(sgn(s - j)));
        final FieldComplex<T> kh = kh1.multiply(kh2);
        FieldComplex<T> ab;
        if (FastMath.abs(s) < m) {
            ab = new FieldComplex<>(a, b).pow(m - s);
        } else {
            ab = new FieldComplex<>(a, b.multiply(sgn(s - m)).negate()).pow(FastMath.abs(s - m));
        }
        final FieldComplex<T> khab = kh.multiply(ab);

        final T[] values = MathArrays.buildArray(field, 2);
        values[0] = khab.getReal();
        values[1] = khab.getImaginary();
        return values;
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
    private static <T extends CalculusFieldElement<T>> T[] getdGHda(final T k, final T h,
                                     final T a, final T b,
                                     final int m, final int s, final int j,
                                     final Field<T> field) {
        final FieldComplex<T> kh = new FieldComplex<>(k, h.multiply(sgn(s - j))).pow(FastMath.abs(s - j));
        FieldComplex<T> ab;
        if (FastMath.abs(s) < m) {
            ab = new FieldComplex<>(a, b).pow(m - s - 1).multiply(m - s);
        } else {
            ab = new FieldComplex<>(a, b.multiply(sgn(s - m)).negate()).pow(FastMath.abs(s - m) - 1).multiply(FastMath.abs(s - m));
        }
        final FieldComplex<T> khab = kh.multiply(ab);

        final T[] values = MathArrays.buildArray(field, 2);
        values[0] = khab.getReal();
        values[1] = khab.getImaginary();
        return values;
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
    private static <T extends CalculusFieldElement<T>> T[] getdGHdb(final T k, final T h,
                                     final T a, final T b,
                                     final int m, final int s, final int j,
                                     final Field<T> field) {
        final T zero = field.getZero();
        final FieldComplex<T> kh = new FieldComplex<>(k, h.multiply(sgn(s - j))).pow(FastMath.abs(s - j));
        FieldComplex<T> ab;
        if (FastMath.abs(s) < m) {
            FieldComplex<T> ab1 = new FieldComplex<>(a, b).pow(m - s - 1);
            FieldComplex<T> ab2 = new FieldComplex<>(zero, zero.add(m - s));
            ab = ab1.multiply(ab2);
        } else {
            FieldComplex<T> ab1 = new FieldComplex<>(a, b.multiply(sgn(s - m)).negate()).pow(FastMath.abs(s - m) - 1);
            FieldComplex<T> ab2 = new FieldComplex<>(zero, FastMath.abs(zero.add(s - m)).multiply(sgn(m - s)));
            ab = ab1.multiply(ab2);
        }
        final FieldComplex<T> khab = kh.multiply(ab);

        final T[] values = MathArrays.buildArray(field, 2);
        values[0] = khab.getReal();
        values[1] = khab.getImaginary();
        return values;
    }

    /** Get the sign of an integer.
     *  @param i number on which evaluation is done
     *  @return -1 or +1 depending on sign of i
     */
    private static int sgn(final int i) {
        return (i < 0) ? -1 : 1;
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
