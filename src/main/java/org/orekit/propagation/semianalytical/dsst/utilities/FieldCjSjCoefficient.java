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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.complex.Complex;
import org.hipparchus.exception.NullArgumentException;

/** Compute the S<sub>j</sub>(k, h) and the C<sub>j</sub>(k, h) series
 *  and their partial derivatives with respect to k and h.
 *  <p>
 *  Those series are given in Danielson paper by expression 2.5.3-(5):
 *
 *  <p> C<sub>j</sub>(k, h) + i S<sub>j</sub>(k, h) = (k+ih)<sup>j</sup>
 *
 *  <p>
 *  The C<sub>j</sub>(k, h) and the S<sub>j</sub>(k, h) elements are store as an
 *  {@link ArrayList} of {@link Complex} number, the C<sub>j</sub>(k, h) being
 *  represented by the real and the S<sub>j</sub>(k, h) by the imaginary part.
 * @param <T> type of the field elements
 */
public class FieldCjSjCoefficient <T extends CalculusFieldElement<T>> {

    /** Zero for initialization. /*/
    private final T zero;

    /** Last computed order j. */
    private int jLast;

    /** Complex base (k + ih) of the C<sub>j</sub>, S<sub>j</sub> series. */
    private final FieldComplex<T> kih;

    /** List of computed elements. */
    private final List<FieldComplex<T>> cjsj;

    /** C<sub>j</sub>(k, h) and S<sub>j</sub>(k, h) constructor.
     * @param k k value
     * @param h h value
     * @param field field for fieldElements
     */
    public FieldCjSjCoefficient(final T k, final T h, final Field<T> field) {
        zero = field.getZero();
        kih  = new FieldComplex<>(k, h);
        cjsj = new ArrayList<FieldComplex<T>>();
        cjsj.add(new FieldComplex<>(zero.add(1.), zero));
        cjsj.add(kih);
        jLast = 1;
    }

    /** Get the C<sub>j</sub> coefficient.
     * @param j order
     * @return C<sub>j</sub>
     */
    public T getCj(final int j) {
        if (j > jLast) {
            // Update to order j
            updateCjSj(j);
        }
        return cjsj.get(j).getReal();
    }

    /** Get the S<sub>j</sub> coefficient.
     * @param j order
     * @return S<sub>j</sub>
     */
    public T getSj(final int j) {
        if (j > jLast) {
            // Update to order j
            updateCjSj(j);
        }
        return cjsj.get(j).getImaginary();
    }

    /** Get the dC<sub>j</sub> / dk coefficient.
     * @param j order
     * @return dC<sub>j</sub> / d<sub>k</sub>
     */
    public T getDcjDk(final int j) {
        return j == 0 ? zero : getCj(j - 1).multiply(j);
    }

    /** Get the dS<sub>j</sub> / dk coefficient.
     * @param j order
     * @return dS<sub>j</sub> / d<sub>k</sub>
     */
    public T getDsjDk(final int j) {
        return j == 0 ? zero : getSj(j - 1).multiply(j);
    }

    /** Get the dC<sub>j</sub> / dh coefficient.
     * @param j order
     * @return dC<sub>i</sub> / d<sub>k</sub>
     */
    public T getDcjDh(final int j) {
        return j == 0 ? zero : getSj(j - 1).multiply(-j);
    }

    /** Get the dS<sub>j</sub> / dh coefficient.
     * @param j order
     * @return dS<sub>j</sub> / d<sub>h</sub>
     */
    public T getDsjDh(final int j) {
        return j == 0 ? zero : getCj(j - 1).multiply(j);
    }

    /** Update the cjsj up to order j.
     * @param j order
     */
    private void updateCjSj(final int j) {
        FieldComplex<T> last = cjsj.get(cjsj.size() - 1);
        for (int i = jLast; i < j; i++) {
            final FieldComplex<T> next = last.multiply(kih);
            cjsj.add(next);
            last = next;
        }
        jLast = j;
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
    }
}
