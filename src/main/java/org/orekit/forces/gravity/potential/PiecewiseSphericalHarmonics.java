/* Copyright 2002-2022 CS GROUP
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
import org.hipparchus.util.SinCos;
import org.orekit.forces.gravity.potential.RawSphericalHarmonicsProvider.RawSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Part of a piecewise gravity fields valid for one time interval.
 * @author Luc Maisonobe
 * @since 11.1
 */
class PiecewiseSphericalHarmonics {

    /** Constant part of the field. */
    private final ConstantSphericalHarmonics constant;

    /** Reference dates. */
    private final AbsoluteDate[] references;

    /** Pulsations (rad/s). */
    private final double[] pulsations;

    /** Harmonics. */
    private final TimeSpanMap<TimeDependentHarmonic[]> harmonics;

    /** Simple constructor.
     * @param constant constant part of the field
     * @param references references dates
     * @param pulsations pulsations (rad/s)
     * @param harmonics map fo harmonics components
     */
    PiecewiseSphericalHarmonics(final ConstantSphericalHarmonics constant,
                                final AbsoluteDate[] references, final double[] pulsations,
                                final TimeSpanMap<TimeDependentHarmonic[]> harmonics) {
        this.constant   = constant;
        this.references = references.clone();
        this.pulsations = pulsations.clone();
        this.harmonics  = harmonics;
    }

    /** Get the constant part of the field.
     * @return constant part of the field
     */
    public ConstantSphericalHarmonics getConstant() {
        return constant;
    }

    /** Convert (degree, order) indices to one-dimensional flatten index.
     * <p>
     * As the outer loop in {@link org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel}
     * if on decreasing order and the inner loop is in increasing degree (starting
     * from the diagonal), the flatten index increases as these loops are performed.
     * </p>
     * @param n degree index
     * @param m order index
     * @return one-dimensional flatten index
     */
    private int index(final int n, final int m) {
        // TODO
    }

    /** Get the raw spherical harmonic coefficients on a specific date.
     * @param date to evaluate the spherical harmonics
     * @return the raw spherical harmonics on {@code date}.
     */
    public RawSphericalHarmonics onDate(final AbsoluteDate date) {

        // raw (constant) harmonics
        final RawSphericalHarmonics raw = constant.onDate(date);

        // select part of the piecewise model that is active at specified date
        final TimeDependentHarmonic[] active = harmonics.get(date);

        // pre-compute canonical functions
        final double[]   offsets = new double[references.length];
        final SinCos[][] sinCos  = new SinCos[references.length][pulsations.length];
        for (int i = 0; i < references.length; ++i) {
            final double offset = date.durationFrom(references[i]);
            offsets[i] = offset;
            for (int j = 0; j < pulsations.length; ++j) {
                sinCos[i][j] = FastMath.sinCos(offset * pulsations[j]);
            }
        }

        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m) {
                return active[index(n, m)].getRawCnm(raw.getRawCnm(n, m), offsets, sinCos);
            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m) {
                return active[index(n, m)].getRawSnm(raw.getRawSnm(n, m), offsets, sinCos);
            }

        };

    }

}
