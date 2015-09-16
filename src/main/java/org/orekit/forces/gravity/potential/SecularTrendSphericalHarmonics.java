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
package org.orekit.forces.gravity.potential;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

/** Simple implementation of {@link RawSphericalHarmonicsProvider} for gravity fields with secular trend.
 * @author Luc Maisonobe
 * @since 6.0
 */
class SecularTrendSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Non-secular part of the field. */
    private final RawSphericalHarmonicsProvider provider;

    /** Reference date for the harmonics. */
    private final AbsoluteDate referenceDate;

    /** Secular trend of the cosine coefficients. */
    private final double[][] cTrend;

    /** Secular trend of the sine coefficients. */
    private final double[][] sTrend;

    /** Simple constructor.
     * @param provider underlying provider for the non secular part
     * @param referenceDate reference date for the harmonics (considered to be at 12:00 TT)
     * @param cTrend secular trend of the cosine coefficients (s<sup>-1</sup>)
     * @param sTrend secular trend of the sine coefficients (s<sup>-1</sup>)
     */
    SecularTrendSphericalHarmonics(final RawSphericalHarmonicsProvider provider,
                                          final DateComponents referenceDate,
                                          final double[][] cTrend, final double[][] sTrend) {
        this.provider      = provider;
        this.referenceDate = new AbsoluteDate(referenceDate, TimeComponents.H12, TimeScalesFactory.getTT());
        this.cTrend        = cTrend;
        this.sTrend        = sTrend;
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return provider.getMaxDegree();
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return provider.getMaxOrder();
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
    public double getOffset(final AbsoluteDate date) {
        return date.durationFrom(referenceDate);
    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return provider.getTideSystem();
    }

    @Override
    public RawSphericalHarmonics onDate(final AbsoluteDate date) throws OrekitException {
        final RawSphericalHarmonics harmonics = provider.onDate(date);
        //compute date offset from reference
        final double dateOffset = getOffset(date);
        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m)
                throws OrekitException {

                // retrieve the constant part of the coefficient
                double cnm = harmonics.getRawCnm(n, m);

                if (n < cTrend.length && m < cTrend[n].length) {
                    // add secular trend
                    cnm += dateOffset * cTrend[n][m];
                }

                return cnm;

            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m)
                throws OrekitException {

                // retrieve the constant part of the coefficient
                double snm = harmonics.getRawSnm(n, m);

                if (n < sTrend.length && m < sTrend[n].length) {
                    // add secular trend
                    snm += dateOffset * sTrend[n][m];
                }

                return snm;

            }

        };
    }

}
