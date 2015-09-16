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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.LoveNumbers;

/** Gravity field corresponding to solid tides.
 * <p>
 * This solid tides force model implementation corresponds to the method described
 * in <a href="http://www.iers.org/nn_11216/IERS/EN/Publications/TechnicalNotes/tn36.html">
 * IERS conventions (2010)</a>, chapter 6, section 6.2.
 * </p>
 * <p>
 * The computation of the spherical harmonics part is done using the algorithm
 * designed by S. A. Holmes and W. E. Featherstone from Department of Spatial Sciences,
 * Curtin University of Technology, Perth, Australia and described in their 2002 paper:
 * <a href="http://cct.gfy.ku.dk/publ_others/ccta1870.pdf">A unified approach to
 * the Clenshaw summation and the recursive computation of very high degree and
 * order normalised associated Legendre functions</a> (Journal of Geodesy (2002)
 * 76: 279–299).
 * </p>
 * <p>
 * Note that this class is <em>not</em> thread-safe, and that tides computation
 * are computer intensive if repeated. So this class is really expected to
 * be wrapped within a {@link
 * org.orekit.forces.gravity.potential.CachedNormalizedSphericalHarmonicsProvider}
 * that will both ensure thread safety and improve performances using caching.
 * </p>
 * @see SolidTides
 * @author Luc Maisonobe
 * @since 6.1
 */
class SolidTidesField implements NormalizedSphericalHarmonicsProvider {

    /** Maximum degree for normalized Legendre functions. */
    private static final int MAX_LEGENDRE_DEGREE = 4;

    /** Love numbers. */
    private final LoveNumbers love;

    /** Function computing frequency dependent terms (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂). */
    private final TimeFunction<double[]> deltaCSFunction;

    /** Permanent tide to be <em>removed</em> from ΔC₂₀ when zero-tide potentials are used. */
    private final double deltaC20PermanentTide;

    /** Function computing pole tide terms (ΔC₂₁, ΔS₂₁). */
    private final TimeFunction<double[]> poleTideFunction;

    /** Rotating body frame. */
    private final Frame centralBodyFrame;

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Tide system used in the central attraction model. */
    private final TideSystem centralTideSystem;

    /** Tide generating bodies (typically Sun and Moon). Read only after construction. */
    private final CelestialBody[] bodies;

    /** First recursion coefficients for tesseral terms. Read only after construction. */
    private final double[][] anm;

    /** Second recursion coefficients for tesseral terms. Read only after construction. */
    private final double[][] bnm;

    /** Third recursion coefficients for sectorial terms. Read only after construction. */
    private final double[] dmm;

    /** Simple constructor.
     * @param love Love numbers
     * @param deltaCSFunction function computing frequency dependent terms (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂)
     * @param deltaC20PermanentTide permanent tide to be <em>removed</em> from ΔC₂₀ when zero-tide potentials are used
     * @param poleTideFunction function computing pole tide terms (ΔC₂₁, ΔS₂₁), may be null
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param bodies tide generating bodies (typically Sun and Moon)
     */
    SolidTidesField(final LoveNumbers love, final TimeFunction<double[]> deltaCSFunction,
                           final double deltaC20PermanentTide, final TimeFunction<double[]> poleTideFunction,
                           final Frame centralBodyFrame, final double ae, final double mu,
                           final TideSystem centralTideSystem, final CelestialBody ... bodies) {

        // store mode parameters
        this.centralBodyFrame  = centralBodyFrame;
        this.ae                = ae;
        this.mu                = mu;
        this.centralTideSystem = centralTideSystem;
        this.bodies            = bodies;

        // compute recursion coefficients for Legendre functions
        this.anm               = buildTriangularArray(5, false);
        this.bnm               = buildTriangularArray(5, false);
        this.dmm               = new double[love.getSize()];
        recursionCoefficients();

        // Love numbers
        this.love = love;

        // frequency dependent terms
        this.deltaCSFunction = deltaCSFunction;

        // permanent tide
        this.deltaC20PermanentTide = deltaC20PermanentTide;

        // pole tide
        this.poleTideFunction = poleTideFunction;

    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return MAX_LEGENDRE_DEGREE;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return MAX_LEGENDRE_DEGREE;
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
        final double[][] cnm = buildTriangularArray(5, true);
        final double[][] snm = buildTriangularArray(5, true);

        // work array to hold Legendre coefficients
        final double[][] pnm = buildTriangularArray(5, true);

        // step 1: frequency independent part
        // equations 6.6 (for degrees 2 and 3) and 6.7 (for degree 4) in IERS conventions 2010
        for (final CelestialBody body : bodies) {

            // compute tide generating body state
            final Vector3D position = body.getPVCoordinates(date, centralBodyFrame).getPosition();

            // compute polar coordinates
            final double x    = position.getX();
            final double y    = position.getY();
            final double z    = position.getZ();
            final double x2   = x * x;
            final double y2   = y * y;
            final double z2   = z * z;
            final double r2   = x2 + y2 + z2;
            final double r    = FastMath.sqrt (r2);
            final double rho2 = x2 + y2;
            final double rho  = FastMath.sqrt(rho2);

            // evaluate Pnm
            evaluateLegendre(z / r, rho / r, pnm);

            // update spherical harmonic coefficients
            frequencyIndependentPart(r, body.getGM(), x / rho, y / rho, pnm, cnm, snm);

        }

        // step 2: frequency dependent corrections
        frequencyDependentPart(date, cnm, snm);

        if (centralTideSystem == TideSystem.ZERO_TIDE) {
            // step 3: remove permanent tide which is already considered
            // in the central body gravity field
            removePermanentTide(cnm);
        }

        if (poleTideFunction != null) {
            // add pole tide
            poleTide(date, cnm, snm);
        }

        return new TideHarmonics(date, cnm, snm);

    }

    /** Compute recursion coefficients. */
    private void recursionCoefficients() {

        // pre-compute the recursion coefficients
        // (see equations 11 and 12 from Holmes and Featherstone paper)
        for (int n = 0; n < anm.length; ++n) {
            for (int m = 0; m < n; ++m) {
                final double f = (n - m) * (n + m );
                anm[n][m] = FastMath.sqrt((2 * n - 1) * (2 * n + 1) / f);
                bnm[n][m] = FastMath.sqrt((2 * n + 1) * (n + m - 1) * (n - m - 1) / (f * (2 * n - 3)));
            }
        }

        // sectorial terms corresponding to equation 13 in Holmes and Featherstone paper
        dmm[0] = Double.NaN; // dummy initialization for unused term
        dmm[1] = Double.NaN; // dummy initialization for unused term
        for (int m = 2; m < dmm.length; ++m) {
            dmm[m] = FastMath.sqrt((2 * m + 1) / (2.0 * m));
        }

    }

    /** Evaluate Legendre functions.
     * @param t cos(θ), where θ is the polar angle
     * @param u sin(θ), where θ is the polar angle
     * @param pnm the computed coefficients. Modified in place.
     */
    private void evaluateLegendre(final double t, final double u, final double[][] pnm) {

        // as the degree is very low, we use the standard forward column method
        // and store everything (see equations 11 and 13 from Holmes and Featherstone paper)
        pnm[0][0] = 1;
        pnm[1][0] = anm[1][0] * t;
        pnm[1][1] = FastMath.sqrt(3) * u;
        for (int m = 2; m < dmm.length; ++m) {
            pnm[m][m - 1] = anm[m][m - 1] * t * pnm[m - 1][m - 1];
            pnm[m][m]     = dmm[m] * u * pnm[m - 1][m - 1];
        }
        for (int m = 0; m < dmm.length; ++m) {
            for (int n = m + 2; n < pnm.length; ++n) {
                pnm[n][m] = anm[n][m] * t * pnm[n - 1][m] - bnm[n][m] * pnm[n - 2][m];
            }
        }

    }

    /** Update coefficients applying frequency independent step, for one tide generating body.
     * @param r distance to tide generating body
     * @param gm tide generating body attraction coefficient
     * @param cosLambda cosine of the tide generating body longitude
     * @param sinLambda sine of the tide generating body longitude
     * @param pnm the Legendre coefficients. See {@link #evaluateLegendre(double, double, double[][])}.
     * @param cnm the computed Cnm coefficients. Modified in place.
     * @param snm the computed Snm coefficients. Modified in place.
     */
    private void frequencyIndependentPart(final double r,
                                          final double gm,
                                          final double cosLambda,
                                          final double sinLambda,
                                          final double[][] pnm,
                                          final double[][] cnm,
                                          final double[][] snm) {

        final double rRatio = ae / r;
        double fM           = gm / mu;
        double cosMLambda   = 1;
        double sinMLambda   = 0;
        for (int m = 0; m < love.getSize(); ++m) {

            double fNPlus1 = fM;
            for (int n = m; n < love.getSize(); ++n) {
                fNPlus1 *= rRatio;
                final double coeff = (fNPlus1 / (2 * n + 1)) * pnm[n][m];
                final double cCos  = coeff * cosMLambda;
                final double cSin  = coeff * sinMLambda;

                // direct effect of degree n tides on degree n coefficients
                // equation 6.6 from IERS conventions (2010)
                final double kR = love.getReal(n, m);
                final double kI = love.getImaginary(n, m);
                cnm[n][m] += kR * cCos + kI * cSin;
                snm[n][m] += kR * cSin - kI * cCos;

                if (n == 2) {
                    // indirect effect of degree 2 tides on degree 4 coefficients
                    // equation 6.7 from IERS conventions (2010)
                    final double kP = love.getPlus(n, m);
                    cnm[4][m] += kP * cCos;
                    snm[4][m] += kP * cSin;
                }

            }

            // prepare next iteration on order
            final double tmp = cosMLambda * cosLambda - sinMLambda * sinLambda;
            sinMLambda = sinMLambda * cosLambda + cosMLambda * sinLambda;
            cosMLambda = tmp;
            fM        *= rRatio;

        }

    }

    /** Update coefficients applying frequency dependent step.
     * @param date current date
     * @param cnm the Cnm coefficients. Modified in place.
     * @param snm the Snm coefficients. Modified in place.
     */
    private void frequencyDependentPart(final AbsoluteDate date,
                                        final double[][] cnm,
                                        final double[][] snm) {
        final double[] deltaCS = deltaCSFunction.value(date);
        cnm[2][0] += deltaCS[0]; // ΔC₂₀
        cnm[2][1] += deltaCS[1]; // ΔC₂₁
        snm[2][1] += deltaCS[2]; // ΔS₂₁
        cnm[2][2] += deltaCS[3]; // ΔC₂₂
        snm[2][2] += deltaCS[4]; // ΔS₂₂
    }

    /** Remove the permanent tide already counted in zero-tide central gravity fields.
     * @param cnm the Cnm coefficients. Modified in place.
     */
    private void removePermanentTide(final double[][] cnm) {
        cnm[2][0] -= deltaC20PermanentTide;
    }

    /** Update coefficients applying pole tide.
     * @param date current date
     * @param cnm the Cnm coefficients. Modified in place.
     * @param snm the Snm coefficients. Modified in place.
     */
    private void poleTide(final AbsoluteDate date,
                          final double[][] cnm,
                          final double[][] snm) {
        final double[] deltaCS = poleTideFunction.value(date);
        cnm[2][1] += deltaCS[0]; // ΔC₂₁
        snm[2][1] += deltaCS[1]; // ΔS₂₁
    }

    /** Create a triangular array.
     * @param size array size
     * @param withDiagonal if true, the array contains a[p][p] terms, otherwise each
     * row ends up at a[p][p-1]
     * @return new triangular array
     */
    private double[][] buildTriangularArray(final int size, final boolean withDiagonal) {
        final double[][] array = new double[size][];
        for (int i = 0; i < array.length; ++i) {
            array[i] = new double[withDiagonal ? i + 1 : i];
        }
        return array;
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
