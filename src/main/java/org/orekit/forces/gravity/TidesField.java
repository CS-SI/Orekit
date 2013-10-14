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
package org.orekit.forces.gravity;

import java.util.Arrays;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.PoissonSeries;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.LoveNumbers;

/** Gravity field corresponding to tides.
 * <p>
 * This solid tides force model implementation corresponds to the method described
 * in <a href="http://www.iers.org/nn_11216/IERS/EN/Publications/TechnicalNotes/tn36.html">
 * IERS conventions (2010)</a>, chapter 6, section 6.2.
 * </p>
 * <p>
 * The computation of the spherical harmonics part is done using the algorithm implemented
 * designed by S. A. Holmes and W. E. Featherstone from Department of Spatial Sciences,
 * Curtin University of Technology, Perth, Australia and described in their 2002 paper:
 * <a href="http://cct.gfy.ku.dk/publ_others/ccta1870.pdf">A unified approach to
 * the Clenshaw summation and the recursive computation of very high degree and
 * order normalised associated Legendre functions</a> (Journal of Geodesy (2002)
 * 76: 279–299).
 * </p>
 * @see SolidTides
 * @author Luc Maisonobe
 */
class TidesField implements NormalizedSphericalHarmonicsProvider {

    /** Maximum degree for normalized Legendre functions. */
    private static final int MAX_LEGENDRE_DEGREE = 4;

    /** Love numbers. */
    private final LoveNumbers love;

    /** Poisson series for frequency dependent terms (k₂₀, k₂₁, k₂₂). */
    private final PoissonSeries<DerivativeStructure>[] kSeries;

    /** Rotating body frame. */
    private final Frame centralBodyFrame;

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Tide system used in the central attraction model. */
    private final TideSystem centralTideSystem;

    /** Tide generating bodies (typically Sun and Moon). */
    private final CelestialBody[] bodies;

    /** Date offset of cached coefficients. */
    private double cachedOffset;

    /** Cached cnm. */
    private final double[][] cachedCnm;

    /** Cached snm. */
    private final double[][] cachedSnm;

    /** Tesseral terms. */
    private final double[][] pnm;

    /** First recursion coefficients for tesseral terms. */
    private final double[][] anm;

    /** Second recursion coefficients for tesseral terms. */
    private final double[][] bnm;

    /** Third recursion coefficients for sectorial terms. */
    private final double[] dmm;

    /** Simple constructor.
     * @param love Love numbers
     * @param kSeries Poisson series for frequency dependent terms (k₂₀, k₂₁, k₂₂)
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param bodies tide generating bodies (typically Sun and Moon)
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    public TidesField(final LoveNumbers love,
                      final PoissonSeries<DerivativeStructure>[] kSeries,
                      final Frame centralBodyFrame,
                      final double ae, final double mu, final TideSystem centralTideSystem,
                      final CelestialBody ... bodies)
        throws OrekitException {

        // store mode parameters
        this.centralBodyFrame  = centralBodyFrame;
        this.ae                = ae;
        this.mu                = mu;
        this.centralTideSystem = centralTideSystem;
        this.bodies            = bodies;

        // compute recursion coefficients for Legendre functions
        this.pnm               = buildTriangularArray(love.getSize(), true);
        this.anm               = buildTriangularArray(love.getSize(), false);
        this.bnm               = buildTriangularArray(love.getSize(), false);
        this.dmm               = new double[love.getSize()];
        recursionCoefficients();

        // prepare coefficients caching
        this.cachedOffset      = Double.NaN;
        this.cachedCnm         = buildTriangularArray(love.getSize(), true);
        this.cachedSnm         = buildTriangularArray(love.getSize(), true);

        // Love numbers
        this.love = love;

        // Frequency dependent terms
        this.kSeries = kSeries.clone();

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
    public double getNormalizedCnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        if (!Precision.equals(dateOffset, cachedOffset, 1)) {
            fillCache(dateOffset);
        }
        return cachedCnm[n][m];
    }

    /** {@inheritDoc} */
    @Override
    public double getNormalizedSnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        if (!Precision.equals(dateOffset, cachedOffset, 1)) {
            fillCache(dateOffset);
        }
        return cachedSnm[n][m];
    }

    /** Fill the cache.
     * @param offset date offset from J2000.0
     * @exception OrekitException if coefficients cannot be computed
     */
    private void fillCache(final double offset) throws OrekitException {

        cachedOffset = offset;
        for (int i = 0; i < cachedCnm.length; ++i) {
            Arrays.fill(cachedCnm[i], 0.0);
            Arrays.fill(cachedSnm[i], 0.0);
        }

        final AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offset);

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
            evaluateLegendre(z / r, rho / r);

            // update spherical harmonic coefficients
            frequencyIndependentPart(r, body.getGM(), x / rho, y / rho);

        }

        // step 2: frequency dependent corrections
        frequencyDependentPart();

        if (centralTideSystem == TideSystem.ZERO_TIDE) {
            // step 3: remove permanent tide which is already considered
            // in the central body gravity field
            removePermanentTide();
        }

    }

    /** Compute recursion coefficients.
     */
    private void recursionCoefficients() {

        // pre-compute the recursion coefficients
        // (see equations 11 and 12 from Holmes and Featherstone paper)
        for (int n = 0; n < anm.length; ++n) {
            for (int m = 0; m < n; ++m) {
                anm[n][m] = FastMath.sqrt((2 * n - 1) * (2 * n + 1) /
                                          ((n - m) * (n + m)));
                bnm[n][m] = FastMath.sqrt((2 * n + 1) * (n + m - 1) * (n - m - 1) /
                                          ((n - m) * (n + m) * (2 * n - 3)));
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
     * @param t cos(&theta;), where &theta; is the polar angle
     * @param u sin(&theta;), where &theta; is the polar angle
     */
    private void evaluateLegendre(final double t, final double u) {

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
     */
    private void frequencyIndependentPart(final double r, final double gm,
                                          final double cosLambda, final double sinLambda) {

        final double rRatio = ae / r;
        double fM           = gm / mu;
        double cosMLambda   = 1;
        double sinMLambda   = 0;
        for (int m = 0; m < love.getSize(); ++m) {

            double fNPlus1 = fM;
            for (int n = m; n < love.getSize(); ++n) {
                fNPlus1 *= rRatio;
                final double coeff = (fNPlus1 / (2 * n + 1)) * pnm[n][m];

                // direct effect of degree n tides on degree n coefficients
                // equation 6.6 from IERS conventions (2010)
                cachedCnm[n][m] += coeff * (love.getReal(n, m) * cosMLambda + love.getImaginary(n, m) * sinMLambda);
                cachedSnm[n][m] += coeff * (love.getReal(n, m) * sinMLambda - love.getImaginary(n, m) * cosMLambda);

                if (n == 2) {
                    // indirect effect of degree 2 tides on degree 4 coefficients
                    // equation 6.7 from IERS conventions (2010)
                    cachedCnm[4][m] += coeff * love.getPlus(n, m) * cosMLambda;
                    cachedSnm[4][m] += coeff * love.getPlus(n, m) * sinMLambda;
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
     */
    private void frequencyDependentPart() {
        // TODO implement equations 6.8a and 6.8b in IERS conventions 2010
    }

    /** Remove the permanent tide already counted in zero-tide central gravity fields.
     */
    private void removePermanentTide() {
        // TODO implement equations 6.13 and 6.14 in IERS conventions 2010
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

}
