/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;

/** Provider of the Jacobi polynomials P<sub>l</sub><sup>v,w</sup>.
 * employed in the {@link TesseralContribution TesseralContribution}
 *
 * @author Nicolas Bernard
 * @since 6.1
 */
public class JacobiPolynomials {

    /** Storage map. */
    private Map<JacobiKey, List<PolynomialFunction>> map;

    /** Simple constructor.
     */
    public JacobiPolynomials() {
        map = new HashMap<JacobiPolynomials.JacobiKey, List<PolynomialFunction>>();
    }

    /** Returns the value of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup> evaluated at &gamma;.
     * @param l l value
     * @param v v value
     * @param w w value
     * @param gamma &gamma; value
     * @return Value of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup>(&gamma;)
     */
    public double getValue(final int l, final int v, final int w, final double gamma) {
        final JacobiKey key = new JacobiKey(v, w);

        // Check the existence of the corresponding key in the map.
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<PolynomialFunction>());
        }

        // If the l-th degree polynomial has not been computed yet, the polynomials
        // up to this degree are computed.
        final int maxComputedDegree = map.get(key).size() - 1;
        if (maxComputedDegree < l) {
            computeUpToDegree(l, v, w);
        }

        return map.get(key).get(l).value(gamma);

    }

    /** Returns the value of the derivative of the Jacobi polynomial.
     * dP<sub>l</sub><sup>v,w</sup> / d&gamma; evaluated at &gamma;
     * @param l l value
     * @param v v value
     * @param w w value
     * @param gamma &gamma; value
     * @return Value of the Jacobi polynomial P<sub>l</sub><sup>v,w</sup>(&gamma;)
     */
    public double getDerivative(final int l, final int v, final int w, final double gamma) {
        final JacobiKey key = new JacobiKey(v, w);

        // Check the existence of the corresponding key in the map.
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<PolynomialFunction>());
        }

        // If the l-th degree polynomial has not been computed yet, the polynomials
        // up to this degree are computed.
        final int maxComputedDegree = map.get(key).size() - 1;
        if (maxComputedDegree < l) {
            computeUpToDegree(l, v, w);
        }

        return map.get(key).get(l).derivative().value(gamma);
    }


    /** Compute for a tuple (v,w) the polynomials up to a given degree.
     * <p>
     * The already computed degrees
     * are skipped. The computation is done through the use of
     * {@link org.apache.commons.math3.analysis.polynomials.PolynomialsUtils PolynomialsUtils}
     * </p>
     *
     * @param degree maximum degree of polynomial to compute
     * @param v v value
     * @param w w value
     *
     * @see org.apache.commons.math3.analysis.polynomials.PolynomialsUtils
     */
    private void computeUpToDegree(final int degree, final int v, final int w) {
        final JacobiKey key = new JacobiKey(v, w);
        final List<PolynomialFunction> polyList = map.get(key);

        for (int l = polyList.size(); l <= degree; l++) {
            polyList.add(l, PolynomialsUtils.createJacobiPolynomial(l, v, w));
        }
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
        public JacobiKey(final int v, final int w) {
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
