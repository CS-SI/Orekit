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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;

/** Provider of the Jacobi polynomials P<sub>l</sub><sup>v,w</sup>.
 * <p>
 * This class is used for {@link
 * org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral
 * tesseral contribution} computation and {@link DSSTThirdBody}.
 * </p>
 *
 * @author Nicolas Bernard
 * @since 6.1
 */
public class JacobiPolynomials {

    /** Storage map. */
    private static final Map<JacobiKey, List<PolynomialFunction>> MAP =
                    new HashMap<JacobiKey, List<PolynomialFunction>>();

    /** Private constructor as class is a utility. */
    private JacobiPolynomials() {
    }

    /** Returns the value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup> evaluated at γ.
     * <p>This method is guaranteed to be thread-safe
     * <p>It was added to improve performances of DSST propagation with tesseral gravity field or third-body perturbations.
     * <p>See issue <a href="https://gitlab.orekit.org/orekit/orekit/-/issues/1098">1098</a>.
     * <p>It appeared the "Gradient" version was degrading performances. This last was however kept for validation purposes.
     * @param l degree of the polynomial
     * @param v v value
     * @param w w value
     * @param x x value
     * @return value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup>(γ)
     * @since 11.3.3
     */
    public static double[] getValueAndDerivative(final int l, final int v, final int w, final double x) {
        // compute value and derivative
        return getValueAndDerivative(computePolynomial(l, v, w), x);
    }

    /** Get value and 1st-order of a mono-variate polynomial.
     *
     * <p> This method was added to improve performances of DSST propagation with tesseral gravity field or third-body perturbations.
     * <p> See issue <a href="https://gitlab.orekit.org/orekit/orekit/-/issues/1098">1098</a>.
     * @param polynomial polynomial to evaluate
     * @param x value to evaluate on
     * @return value and 1s-order derivative as a double array
     * @since 11.3.3
     */
    private static double[] getValueAndDerivative(final PolynomialFunction polynomial, final double x) {

        // Polynomial coefficients
        final double[] coefficients = polynomial.getCoefficients();

        // Degree of the polynomial
        final int degree = polynomial.degree();

        // Initialize value and 1st-order derivative
        double value      = coefficients[degree];
        double derivative = value * degree;
        for (int j = degree - 1; j >= 1; j--) {

            // Increment both value and derivative
            final double coef = coefficients[j];
            value        = value      * x +  coef;
            derivative   = derivative * x +  coef * j;
        }
        // If degree > 0, perform last operation
        if (degree > 0) {
            value = value * x + coefficients[0];
        }

        // Return value and 1st-order derivative as double array
        return new double[] {value, derivative};
    }

    /** Returns the value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup> evaluated at γ.
     *
     * <p>This method is guaranteed to be thread-safe
     * <p>It's not used in the code anymore, see {@link #getValueAndDerivative(int, int, int, double)}, but was kept for validation purpose.
     * @param l degree of the polynomial
     * @param v v value
     * @param w w value
     * @param gamma γ value
     * @return value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup>(γ)
     * @since 10.2
     */
    public static Gradient getValue(final int l, final int v, final int w, final Gradient gamma) {
        // compute value and derivative
        return computePolynomial(l, v, w).value(gamma);
    }

    /** Returns the value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup> evaluated at γ.
     * <p>
     * This method is guaranteed to be thread-safe
     * </p>
     * @param <T> the type of the field elements
     * @param l degree of the polynomial
     * @param v v value
     * @param w w value
     * @param gamma γ value
     * @return value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup>(γ)
     * @since 10.2
     */
    public static <T extends CalculusFieldElement<T>> FieldGradient<T> getValue(final int l, final int v, final int w,
                                                                                final FieldGradient<T> gamma) {
        // compute value and derivative
        return computePolynomial(l, v, w).value(gamma);

    }

    /** Initializes the polynomial to evalutate.
     * @param l degree of the polynomial
     * @param v v value
     * @param w w value
     * @return the polynomial to evaluate
     */
    private static PolynomialFunction computePolynomial(final int l, final int v, final int w) {
        final List<PolynomialFunction> polyList;
        synchronized (MAP) {

            final JacobiKey key = new JacobiKey(v, w);

            // Check the existence of the corresponding key in the map.
            if (!MAP.containsKey(key)) {
                MAP.put(key, new ArrayList<PolynomialFunction>());
            }

            polyList = MAP.get(key);

        }

        final PolynomialFunction polynomial;
        synchronized (polyList) {
            // If the l-th degree polynomial has not been computed yet, the polynomials
            // up to this degree are computed.
            for (int degree = polyList.size(); degree <= l; degree++) {
                polyList.add(degree, PolynomialsUtils.createJacobiPolynomial(degree, v, w));
            }
            polynomial = polyList.get(l);
        }

        return polynomial;
    }

    /** Inner class for Jacobi polynomials keys.
     * <p>
     * Please note that this class is not original content but is a copy from the
     * Hipparchus library. This library is published under the
     * Apache License, version 2.0.
     * </p>
     *
     * @see org.hipparchus.analysis.polynomials.PolynomialsUtils
     */
    private static class JacobiKey {

        /** First exponent. */
        private final int v;

        /** Second exponent. */
        private final int w;

        /** Simple constructor.
         * @param v first exponent
         * @param w second exponent
         */
        JacobiKey(final int v, final int w) {
            this.v = v;
            this.w = w;
        }

        /** Get hash code.
         * @return hash code
         */
        @Override
        public int hashCode() {
            return (v << 16) ^ w;
        }

        /** Check if the instance represent the same key as another instance.
         * @param key other key
         * @return true if the instance and the other key refer to the same polynomial
         */
        @Override
        public boolean equals(final Object key) {

            if (!(key instanceof JacobiKey)) {
                return false;
            }

            final JacobiKey otherK = (JacobiKey) key;
            return v == otherK.v && w == otherK.w;

        }
    }
}
