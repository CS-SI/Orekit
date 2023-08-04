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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Utility for converting between (degree, order) indices and one-dimensional flatten index.
 * <p>
 * The outer loop in {@link org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel}
 * if in decreasing order and the inner loop is in increasing degree (starting
 * from the diagonal). This utility converts (degree, order) indices into a flatten index
 * in a one-dimensional array that increases as these loops are performed.
 * This means the first element of the one-dimensional array corresponds to diagonal
 * element at maximum order and the last element corresponds to order 0 and maximum degree.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 */
class Flattener {

    /** Degree of the spherical harmonics. */
    private final int degree;

    /** Order of the spherical harmonics. */
    private final int order;

    /** Number of high order cells dropped in the triangular array. */
    private final int dropped;

    /** Simple constructor.
     * @param degree degree of the spherical harmonics
     * @param order order of the spherical harmonics
      */
    Flattener(final int degree, final int order) {
        this.degree  = degree;
        this.order   = order;
        this.dropped = (degree - order + 1) * (degree - order) / 2;
    }

    /** Get the degree of the spherical harmonics.
     * @return degree of the spherical harmonics
     */
    public int getDegree() {
        return degree;
    }

    /** Get the order of the spherical harmonics.
     * @return order of the spherical harmonics
     */
    public int getOrder() {
        return order;
    }

    /** Convert (degree, order) indices to one-dimensional flatten index.
     * <p>
     * As the outer loop in {@link org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel}
     * if on decreasing order and the inner loop is in increasing degree (starting
     * from the diagonal), the flatten index increases as these loops are performed.
     * </p>
     * @param n degree index (must be within range, otherwise an exception is thrown)
     * @param m order index (must be within range, otherwise an exception is thrown)
     * @return one-dimensional flatten index
     * @see #withinRange(int, int)
     */
    public int index(final int n, final int m) {
        if (!withinRange(n, m)) {
            throw new OrekitException(OrekitMessages.WRONG_DEGREE_OR_ORDER, n, m, degree, order);
        }
        final int dm = degree - m;
        return dm * (dm + 1) / 2 + (n - m) - dropped;
    }

    /** Get the size of a flatten array sufficient to hold all coefficients.
     * @return size of a flatten array sufficient to hold all coefficients
     */
    public int arraySize() {
        return index(degree, 0) + 1;
    }

    /** Check if indices are within range.
     * @param n degree
     * @param m order
     * @return true if indices are within limits, false otherwise
     */
    public boolean withinRange(final int n, final int m) {
        return n >= 0 && n <= degree && m >= 0 && m <= FastMath.min(n, order);
    }

    /** Flatten a triangular array.
     * @param triangular triangular array to flatten
     * @return flatten array
     */
    public double[] flatten(final double[][] triangular) {
        final double[] flat = new double[arraySize()];
        for (int n = 0; n <= getDegree(); ++n) {
            for (int m = 0; m <= FastMath.min(n, getOrder()); ++m) {
                flat[index(n, m)] = triangular[n][m];
            }
        }
        return flat;
    }

}
