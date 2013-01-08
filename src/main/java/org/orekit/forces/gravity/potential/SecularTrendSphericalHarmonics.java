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
package org.orekit.forces.gravity.potential;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

/** Simple implementation of {@link SphericalHarmonicsProvider} for gravity fields with secular trend.
 * @author Luc Maisonobe
 * @since 6.0
 */
public class SecularTrendSphericalHarmonics implements SphericalHarmonicsProvider {

    /** Non-secular part of the field. */
    private final SphericalHarmonicsProvider provider;

    /** Reference date for the harmonics. */
    private final AbsoluteDate referenceDate;

    /** Secular trend of the cosine coefficients. */
    private final double[][] cTrend;

    /** Secular trend of the sine coefficients. */
    private final double[][] sTrend;

    /** Simple constructor.
     * @param provider underlying provider for the non secular part
     * @param referenceDate reference date for the harmonics (considered to be at 12:00 TT)
     * @param cTrend un-normalized trend of the cosine coefficients (s<sup>-1</sup>)
     * @param sTrend un-normalized trend of the sine coefficients (s<sup>-1</sup>)
     */
    public SecularTrendSphericalHarmonics(final SphericalHarmonicsProvider provider,
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
    public double getUnnormalizedCnm(final double dateOffset, final int n, final int m)
            throws OrekitException {

        // retrieve the constant part of the coefficient
        double cnm = provider.getUnnormalizedCnm(dateOffset, n, m);

        if (n < cTrend.length && m < cTrend[n].length) {
            // add secular trend
            cnm += dateOffset * cTrend[n][m];
        }

        return cnm;

    }

    /** {@inheritDoc} */
    public double getUnnormalizedSnm(final double dateOffset, final int n, final int m)
            throws OrekitException {

        // retrieve the constant part of the coefficient
        double snm = provider.getUnnormalizedSnm(dateOffset, n, m);

        if (n < sTrend.length && m < sTrend[n].length) {
            // add secular trend
            snm += dateOffset * sTrend[n][m];
        }

        return snm;

    }

}
