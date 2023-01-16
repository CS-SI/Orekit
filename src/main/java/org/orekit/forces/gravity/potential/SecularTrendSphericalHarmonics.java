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
import org.orekit.time.AbsoluteDate;

/** Simple implementation of {@link RawSphericalHarmonicsProvider} for gravity fields with secular trend.
 * @author Luc Maisonobe
 * @since 6.0
 */
class SecularTrendSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Non-secular part of the field. */
    private final RawSphericalHarmonicsProvider provider;

    /** Reference date for the harmonics. */
    private final AbsoluteDate referenceDate;

    /** Converter from triangular to flatten array.
     * @since 11.1
     */
    private final Flattener flattener;

    /** Secular trend of the cosine coefficients. */
    private final double[] cTrend;

    /** Secular trend of the sine coefficients. */
    private final double[] sTrend;

    /** Simple constructor.
     * @param provider underlying provider for the non secular part
     * @param referenceDate reference date for the harmonics (considered to be at 12:00 TT)
     * @param flattener flattener from triangular to flatten array
     * @param cTrend secular trend of the cosine coefficients (s<sup>-1</sup>)
     * @param sTrend secular trend of the sine coefficients (s<sup>-1</sup>)
     * @since 11.1
     */
    SecularTrendSphericalHarmonics(final RawSphericalHarmonicsProvider provider, final AbsoluteDate referenceDate,
                                   final Flattener flattener, final double[] cTrend, final double[] sTrend) {
        this.provider      = provider;
        this.referenceDate = referenceDate;
        this.flattener     = flattener;
        this.cTrend        = cTrend.clone();
        this.sTrend        = sTrend.clone();
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return FastMath.max(flattener.getDegree(), provider.getMaxDegree());
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return FastMath.max(flattener.getOrder(), provider.getMaxOrder());
    }

    /** {@inheritDoc} */
    public double getMu() {
        return provider.getMu();
    }

    /** {@inheritDoc} */
    public double getAe() {
        return provider.getAe();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return provider.getTideSystem();
    }

    @Override
    public RawSphericalHarmonics onDate(final AbsoluteDate date) {
        final RawSphericalHarmonics harmonics = provider.onDate(date);
        //compute date offset from reference
        final double dateOffset = date.durationFrom(referenceDate);
        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m) {

                // retrieve the constant part of the coefficient
                double cnm = harmonics.getRawCnm(n, m);

                if (flattener.withinRange(n, m)) {
                    // add secular trend
                    cnm += dateOffset * cTrend[flattener.index(n, m)];
                }

                return cnm;

            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m) {

                // retrieve the constant part of the coefficient
                double snm = harmonics.getRawSnm(n, m);

                if (flattener.withinRange(n, m)) {
                    // add secular trend
                    snm += dateOffset * sTrend[flattener.index(n, m)];
                }

                return snm;

            }

        };
    }

}
