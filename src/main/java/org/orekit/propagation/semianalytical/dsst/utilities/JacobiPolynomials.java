/* Copyright 2002-2015 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;

/** Provider of the Jacobi polynomials P<sub>l</sub><sup>v,w</sup>.
 * <p>
 * This class is used for {@link
 * org.orekit.propagation.semianalytical.dsst.forces.TesseralContribution
 * tesseral contribution} computation.
 * </p>
 *
 * @author Nicolas Bernard
 * @since 6.1
 */
public class JacobiPolynomials {

    /** Storage map. */
    private static final Map<JacobiKey, List<PolynomialFunction>> MAP =
            new HashMap<JacobiPolynomials.JacobiKey, List<PolynomialFunction>>();

    /** Private constructor as class is a utility. */
    private JacobiPolynomials() {
    }

    /** Returns the value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup> evaluated at γ.
     * <p>
     * This method is guaranteed to be thread-safe
     * </p>
     * @param l degree of the polynomial
     * @param v v value
     * @param w w value
     * @param gamma γ value
     * @return value and derivatives of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup>(γ)
     */
    public static DerivativeStructure getValue(final int l, final int v, final int w, final DerivativeStructure gamma) {

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

        // compute value and derivative
        return polynomial.value(gamma);

    }


    /** Inner class for Jacobi polynomials keys.
     * <p>
     * Please note that this class is not original content but is a copy from the
     * Apache commons-math3 library. This library is published under the
     * Apache License, version 2.0.
     * </p>
     *
     * @see org.apache.commons.math3.analysis.polynomials.PolynomialsUtils
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

            if ((key == null) || !(key instanceof JacobiKey)) {
                return false;
            }

            final JacobiKey otherK = (JacobiKey) key;
            return (v == otherK.v) && (w == otherK.w);

        }
    }

}
