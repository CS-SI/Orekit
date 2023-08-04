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

import org.hipparchus.util.SinCos;

/** Part of a {@link PiecewiseSphericalHarmonics piecewise gravity fields} valid for one time interval.
 * @author Luc Maisonobe
 * @since 11.1
 */
class PiecewisePart {

    /** Converter between (degree, order) indices and flatten array. */
    private final Flattener flattener;

    /** Components of the spherical harmonics. */
    private final TimeDependentHarmonic[] components;

    /** Simple constructor.
     * @param flattener converter between (degree, order) indices and flatten array
     * @param components components of the spherical harmonics
     */
    PiecewisePart(final Flattener flattener, final TimeDependentHarmonic[] components) {
        this.flattener  = flattener;
        this.components = components.clone();
    }

    /** Get the maximum supported degree.
     * @return maximal supported degree
     */
    public int getMaxDegree() {
        return flattener.getDegree();
    }

    /** Get the maximal supported order.
     * @return maximal supported order
     */
    public int getMaxOrder() {
        return flattener.getOrder();
    }

    /** Compute the time-dependent part of a spherical harmonic cosine coefficient.
     * @param n degree of the coefficient
     * @param m order of the coefficient
     * @param offsets offsets to reference dates in the gravity field
     * @param pulsations angular pulsations in the gravity field
     * @return raw coefficient Cnm
     */
    public double computeCnm(final int n, final int m,
                             final double[] offsets, final SinCos[][] pulsations) {
        final TimeDependentHarmonic harmonic = components[flattener.index(n, m)];
        return harmonic == null ? 0.0 : harmonic.computeCnm(offsets, pulsations);
    }

    /** Compute the time-dependent part of a spherical harmonic sine coefficient.
     * @param n degree of the coefficient
     * @param m order of the coefficient
     * @param offsets offsets to reference dates in the gravity field
     * @param pulsations angular pulsations in the gravity field
     * @return raw coefficient Snm
     */
    public double computeSnm(final int n, final int m,
                             final double[] offsets, final SinCos[][] pulsations) {
        final TimeDependentHarmonic harmonic = components[flattener.index(n, m)];
        return harmonic == null ? 0.0 : harmonic.computeSnm(offsets, pulsations);
    }

}
