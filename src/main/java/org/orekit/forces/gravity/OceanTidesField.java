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
package org.orekit.forces.gravity;

import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanTidesWave;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;

/** Gravity field corresponding to ocean tides.
 * <p>
 * This ocean tides force model implementation corresponds to the method described
 * in <a href="http://www.iers.org/nn_11216/IERS/EN/Publications/TechnicalNotes/tn36.html">
 * IERS conventions (2010)</a>, chapter 6, section 6.3.
 * </p>
 * <p>
 * Note that this class is <em>not</em> thread-safe, and that tides computation
 * are computer intensive if repeated. So this class is really expected to
 * be wrapped within a {@link
 * org.orekit.forces.gravity.potential.CachedNormalizedSphericalHarmonicsProvider}
 * that will both ensure thread safety and improve performances using caching.
 * </p>
 * @see OceanTides
 * @author Luc Maisonobe
 * @since 6.1
 */
class OceanTidesField implements NormalizedSphericalHarmonicsProvider {

    /** Maximum degree. */
    private final int degree;

    /** Maximum order. */
    private final int order;

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Tides model. */
    private final List<OceanTidesWave> waves;

    /** Object computing the fundamental arguments. */
    private final FundamentalNutationArguments arguments;

    /** Function computing pole tide terms (ΔC₂₁, ΔS₂₁). */
    private final TimeFunction<double[]> poleTideFunction;

    /** Simple constructor.
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param waves ocean tides waves
     * @param arguments object computing the fundamental arguments
     * @param poleTideFunction function computing ocean pole tide terms (ΔC₂₁, ΔS₂₁), may be null
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    OceanTidesField(final double ae, final double mu,
                           final List<OceanTidesWave> waves,
                           final FundamentalNutationArguments arguments,
                           final TimeFunction<double[]> poleTideFunction)
        throws OrekitException {

        // store mode parameters
        this.ae  = ae;
        this.mu  = mu;

        // waves
        this.waves = waves;
        int m = 0;
        int n = 0;
        for (final OceanTidesWave wave : waves) {
            m = FastMath.max(m, wave.getMaxDegree());
            n = FastMath.max(n, wave.getMaxOrder());
        }
        degree = m;
        order  = n;

        this.arguments = arguments;

        // pole tide
        this.poleTideFunction = poleTideFunction;

    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return degree;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return ae;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getReferenceDate() {
        return AbsoluteDate.J2000_EPOCH;
    }

    /** {@inheritDoc} */
    @Override
    public double getOffset(final AbsoluteDate date) {
        return date.durationFrom(AbsoluteDate.J2000_EPOCH);
    }

    /** {@inheritDoc} */
    @Override
    public TideSystem getTideSystem() {
        // not really used here, but for consistency we can state that either
        // we add the permanent tide or it was already in the central attraction
        return TideSystem.ZERO_TIDE;
    }

    /** {@inheritDoc} */
    @Override
    public NormalizedSphericalHarmonics onDate(final AbsoluteDate date) throws OrekitException {

        // computed Cnm and Snm coefficients
        final int        rows = degree + 1;
        final double[][] cnm  = new double[rows][];
        final double[][] snm  = new double[rows][];
        for (int i = 0; i <= degree; ++i) {
            final int m = FastMath.min(i, order) + 1;
            cnm[i] = new double[m];
            snm[i] = new double[m];
        }

        final BodiesElements bodiesElements = arguments.evaluateAll(date);
        for (final OceanTidesWave wave : waves) {
            wave.addContribution(bodiesElements, cnm, snm);
        }

        if (poleTideFunction != null && degree > 1 && order > 0) {
            // add pole tide
            poleTide(date, cnm, snm);
        }

        return new TideHarmonics(date, cnm, snm);

    }

    /** Update coefficients applying pole tide.
     * @param date current date
     * @param cnm the Cnm coefficients. Modified in place.
     * @param snm the Snm coefficients. Modified in place.
     */
    private void poleTide(final AbsoluteDate date, final double[][] cnm, final double[][] snm) {
        final double[] deltaCS = poleTideFunction.value(date);
        cnm[2][1] += deltaCS[0]; // ΔC₂₁
        snm[2][1] += deltaCS[1]; // ΔS₂₁
    }

    /** The Tidal geopotential evaluated on a specific date. */
    private static class TideHarmonics implements NormalizedSphericalHarmonics {

        /** evaluation date. */
        private final AbsoluteDate date;

        /** Cached cnm. */
        private final double[][] cnm;

        /** Cached snm. */
        private final double[][] snm;

        /** Construct the tidal harmonics on the given date.
         *
         * @param date of evaluation
         * @param cnm the Cnm coefficients. Not copied.
         * @param snm the Snm coeffiecients. Not copied.
         */
        private TideHarmonics(final AbsoluteDate date,
                              final double[][] cnm,
                              final double[][] snm) {
            this.date = date;
            this.cnm = cnm;
            this.snm = snm;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** {@inheritDoc} */
        @Override
        public double getNormalizedCnm(final int n, final int m)
            throws OrekitException {
            return cnm[n][m];
        }

        /** {@inheritDoc} */
        @Override
        public double getNormalizedSnm(final int n, final int m)
            throws OrekitException {
            return snm[n][m];
        }

    }

}
