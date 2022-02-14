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
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.time.AbsoluteDate;

/** Simple implementation of {@link RawSphericalHarmonicsProvider} for pulsating gravity fields.
 * @author Luc Maisonobe
 * @since 6.0
 * @deprecated as of 11.1, replaced by {@link PiecewiseSphericalHarmonics}
 */
@Deprecated
class PulsatingSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Underlying part of the field. */
    private final RawSphericalHarmonicsProvider provider;

    /** Pulsation (rad/s). */
    private final double pulsation;

    /** Converter from triangular to flatten array.
     * @since 11.1
     */
    private final Flattener flattener;

    /** Cosine component of the cosine coefficients. */
    private final double[] cosC;

    /** Sine component of the cosine coefficients. */
    private final double[] sinC;

    /** Cosine component of the sine coefficients. */
    private final double[] cosS;

    /** Sine component of the sine coefficients. */
    private final double[] sinS;

    /** Simple constructor.
     * @param provider underlying part of the field
     * @param period period of the pulsation (s)
     * @param cosC cosine component of the cosine coefficients
     * @param sinC sine component of the cosine coefficients
     * @param cosS cosine component of the sine coefficients
     * @param sinS sine component of the sine coefficients
     * @deprecated as of 11.1, replaced by {@link #PulsatingSphericalHarmonics(RawSphericalHarmonicsProvider,
     * double, Flattener, double[], double[], double[], double[])}
     */
    @Deprecated
    PulsatingSphericalHarmonics(final RawSphericalHarmonicsProvider provider,
                                final double period,
                                final double[][] cosC, final double[][] sinC,
                                final double[][] cosS, final double[][] sinS) {
        this(provider, period, buildFlattener(cosC),
             buildFlattener(cosC).flatten(cosC), buildFlattener(sinC).flatten(sinC),
             buildFlattener(cosS).flatten(cosS), buildFlattener(sinS).flatten(sinS));
    }

    /** Simple constructor.
     * @param provider underlying part of the field
     * @param period period of the pulsation (s)
     * @param flattener flattener from triangular to flatten array
     * @param cosC cosine component of the cosine coefficients
     * @param sinC sine component of the cosine coefficients
     * @param cosS cosine component of the sine coefficients
     * @param sinS sine component of the sine coefficients
     * @since 11.1
     */
    PulsatingSphericalHarmonics(final RawSphericalHarmonicsProvider provider,
                                final double period, final Flattener flattener,
                                final double[] cosC, final double[] sinC,
                                final double[] cosS, final double[] sinS) {
        this.provider  = provider;
        this.pulsation = MathUtils.TWO_PI / period;
        this.flattener = flattener;
        this.cosC      = cosC.clone();
        this.sinC      = sinC.clone();
        this.cosS      = cosS.clone();
        this.sinS      = sinS.clone();
    }

    /** Get a flattener for a triangular array.
     * @param triangular triangular array to flatten
     * @return flattener suited for triangular array dimensions
     * @since 11.1
     */
    private static Flattener buildFlattener(final double[][] triangular) {
        return new Flattener(triangular.length - 1, triangular[triangular.length - 1].length - 1);
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
        return provider.getReferenceDate();
    }

    /** {@inheritDoc} */
    @Deprecated
    public double getOffset(final AbsoluteDate date) {
        return provider.getOffset(date);
    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return provider.getTideSystem();
    }

    @Override
    @Deprecated
    public RawSphericalHarmonics onDate(final AbsoluteDate date) {
        //raw (constant) harmonics
        final RawSphericalHarmonics raw = provider.onDate(date);
        //phase angle, will loose precision for large offsets
        final double alpha = pulsation * provider.getOffset(date);
        //pre-compute transcendental functions
        final SinCos scAlpha = FastMath.sinCos(alpha);
        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m) {

                // retrieve the underlying part of the coefficient
                double cnm = raw.getRawCnm(n, m);

                if (flattener.withinRange(n, m)) {
                    // add pulsation
                    cnm += cosC[flattener.index(n, m)] * scAlpha.cos() + sinC[flattener.index(n, m)] * scAlpha.sin();
                }

                return cnm;
            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m) {

                // retrieve the constant part of the coefficient
                double snm = raw.getRawSnm(n, m);

                if (flattener.withinRange(n, m)) {
                    // add pulsation
                    snm += cosS[flattener.index(n, m)] * scAlpha.cos() + sinS[flattener.index(n, m)] * scAlpha.sin();
                }

                return snm;
            }

        };
    }

}
