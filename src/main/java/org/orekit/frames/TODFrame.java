/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** True Equator, Mean Equinox of Date Frame.
 * <p>This frame handles nutation effects according to the IAU-80 theory.</p>
 * <p>Its parent frame is the {@link MODFrame}.</p>
 * <p>It is sometimes called True of Date (ToD) frame.<p>
 * <p>This implementation includes a caching/interpolation feature to
 * tremendously improve efficiency. The IAU-80 theory involves lots of terms
 * (106 components for Dpsi and Deps). Recomputing all these components
 * for each point is really slow. The shortest period for these components is
 * about 5.5 days (one fifth of the moon revolution period), hence the pole
 * motion is smooth at the day or week scale. This implies that these motions can
 * be computed accurately using a few reference points per day or week and interpolated
 * between these points. This implementation uses 12 points separated by 1/2 day
 * (43200 seconds) each, the resulting maximal interpolation error on the frame is about
 * 1.3&times;10<sup>-10</sup> arcseconds.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
class TODFrame extends FactoryManagedFrame {

    /** Serializable UID. */
    private static final long serialVersionUID = 6318738377160926252L;

    // CHECKSTYLE: stop JavadocVariable check

    // Coefficients for the Equation of the Equinoxes.
    private static final double EQE_1 =     0.00264  * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double EQE_2 =     0.000063 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for the Mean Obliquity of the Ecliptic.
    private static final double MOE_0 = 84381.448    * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double MOE_1 =   -46.8150   * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double MOE_2 =    -0.00059  * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double MOE_3 =     0.001813 * Constants.ARC_SECONDS_TO_RADIANS;

    // lunisolar nutation elements
    // Coefficients for l (Mean Anomaly of the Moon).
    private static final double F10  = FastMath.toRadians(134.96340251);
    private static final double F110 =    715923.2178    * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F111 =      1325.0;
    private static final double F12  =        31.87908   * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F13  =         0.0516348 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for l' (Mean Anomaly of the Sun).
    private static final double F20  = FastMath.toRadians(357.52910918);
    private static final double F210 =   1292581.048     * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F211 =        99.0;
    private static final double F22  =        -0.55332   * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F23  =         0.0001368 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for F = L (Mean Longitude of the Moon) - Omega.
    private static final double F30  = FastMath.toRadians(93.27209062);
    private static final double F310 =    295262.8477    * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F311 =      1342.0;
    private static final double F32  =       -12.7512    * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F33  =        -0.0010368 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for D (Mean Elongation of the Moon from the Sun).
    private static final double F40  = FastMath.toRadians(297.85019547);
    private static final double F410 =   1105601.209     * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F411 =      1236.0;
    private static final double F42  =        -6.37056   * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F43  =         0.0065916 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for Omega (Mean Longitude of the Ascending Node of the Moon).
    private static final double F50  = FastMath.toRadians(125.0445501);
    private static final double F510 =   -482890.2665   * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F511 =        -5.0;
    private static final double F52  =         7.4722   * Constants.ARC_SECONDS_TO_RADIANS;
    private static final double F53  =         0.007702 * Constants.ARC_SECONDS_TO_RADIANS;

    // CHECKSTYLE: resume JavadocVariable check

    /** coefficients of l, mean anomaly of the Moon. */
    private static final int[] CL = {
        +0,  0, -2,  2, -2,  1,  0,  2,  0,  0,
        +0,  0,  0,  2,  0,  0,  0,  0,  0, -2,
        +0,  2,  0,  1,  2,  0,  0,  0, -1,  0,
        +0,  1,  0,  1,  1, -1,  0,  1, -1, -1,
        +1,  0,  2,  1,  2,  0, -1, -1,  1, -1,
        +1,  0,  0,  1,  1,  2,  0,  0,  1,  0,
        +1,  2,  0,  1,  0,  1,  1,  1, -1, -2,
        +3,  0,  1, -1,  2,  1,  3,  0, -1,  1,
        -2, -1,  2,  1,  1, -2, -1,  1,  2,  2,
        +1,  0,  3,  1,  0, -1,  0,  0,  0,  1,
        +0,  1,  1,  2,  0,  0
    };

    /** coefficients of l', mean anomaly of the Sun. */
    private static final int[] CLP = {
        +0,  0,  0,  0,  0, -1, -2,  0,  0,  1,
        +1, -1,  0,  0,  0,  2,  1,  2, -1,  0,
        -1,  0,  1,  0,  1,  0,  1,  1,  0,  1,
        +0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
        +0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
        +1,  1, -1,  0,  0,  0,  0,  0,  0,  0,
        -1,  0,  1,  0,  0,  1,  0, -1, -1,  0,
        +0, -1,  1,  0,  0,  0,  0,  0,  0,  0,
        +0,  0,  0,  1,  0,  0,  0, -1,  0,  0,
        +0,  0,  0,  0,  1, -1,  0,  0,  1,  0,
        -1,  1,  0,  0,  0,  1
    };

    /** coefficients of F = L - &Omega, where L is the mean longitude of the Moon. */
    private static final int[] CF = {
        0,  0,  2, -2,  2,  0,  2, -2,  2,  0,
        2,  2,  2,  0,  2,  0,  0,  2,  0,  0,
        2,  0,  2,  0,  0, -2, -2,  0,  0,  2,
        2,  0,  2,  2,  0,  2,  0,  0,  0,  2,
        2,  2,  0,  2,  2,  2,  2,  0,  0,  2,
        0,  2,  2,  2,  0,  2,  0,  2,  2,  0,
        0,  2,  0, -2,  0,  0,  2,  2,  2,  0,
        2,  2,  2,  2,  0,  0,  0,  2,  0,  0,
        2,  2,  0,  2,  2,  2,  4,  0,  2,  2,
        0,  4,  2,  2,  2,  0, -2,  2,  0, -2,
        2,  0, -2,  0,  2,  0
    };

    /** coefficients of D, mean elongation of the Moon from the Sun. */
    private static final int[] CD = {
        +0,  0,  0,  0,  0, -1, -2,  0, -2,  0,
        -2, -2, -2, -2, -2,  0,  0, -2,  0,  2,
        -2, -2, -2, -1, -2,  2,  2,  0,  1, -2,
        +0,  0,  0,  0, -2,  0,  2,  0,  0,  2,
        +0,  2,  0, -2,  0,  0,  0,  2, -2,  2,
        -2,  0,  0,  2,  2, -2,  2,  2, -2, -2,
        +0,  0, -2,  0,  1,  0,  0,  0,  2,  0,
        +0,  2,  0, -2,  0,  0,  0,  1,  0, -4,
        +2,  4, -4, -2,  2,  4,  0, -2, -2,  2,
        +2, -2, -2, -2,  0,  2,  0, -1,  2, -2,
        +0, -2,  2,  2,  4,  1
    };

    /** coefficients of &Omega, mean longitude of the ascending node of the Moon. */
    private static final int[] COM = {
        1, 2, 1, 0, 2, 0, 1, 1, 2, 0,
        2, 2, 1, 0, 0, 0, 1, 2, 1, 1,
        1, 1, 1, 0, 0, 1, 0, 2, 1, 0,
        2, 0, 1, 2, 0, 2, 0, 1, 1, 2,
        1, 2, 0, 2, 2, 0, 1, 1, 1, 1,
        0, 2, 2, 2, 0, 2, 1, 1, 1, 1,
        0, 1, 0, 0, 0, 0, 0, 2, 2, 1,
        2, 2, 2, 1, 1, 2, 0, 2, 2, 0,
        2, 2, 0, 2, 1, 2, 2, 0, 1, 2,
        1, 2, 2, 0, 1, 1, 1, 2, 0, 0,
        1, 1, 0, 0, 2, 0
    };

    /** coefficients for nutation in longitude, const part, in 0.1milliarcsec. */
    private static final double[] SL = {
        -171996.0, 2062.0, 46.0,   11.0,  -3.0,  -3.0,  -2.0,   1.0,  -13187.0, 1426.0,
        -517.0,    217.0,  129.0,  48.0,  -22.0,  17.0, -15.0, -16.0, -12.0,   -6.0,
        -5.0,      4.0,    4.0,   -4.0,    1.0,   1.0,  -1.0,   1.0,   1.0,    -1.0,
        -2274.0,   712.0, -386.0, -301.0, -158.0, 123.0, 63.0,  63.0, -58.0,   -59.0,
        -51.0,    -38.0,   29.0,   29.0,  -31.0,  26.0,  21.0,  16.0, -13.0,   -10.0,
        -7.0,      7.0,   -7.0,   -8.0,    6.0,   6.0,  -6.0,  -7.0,   6.0,    -5.0,
        +5.0,     -5.0,   -4.0,    4.0,   -4.0,  -3.0,   3.0,  -3.0,  -3.0,    -2.0,
        -3.0,     -3.0,    2.0,   -2.0,    2.0,  -2.0,   2.0,   2.0,   1.0,    -1.0,
        +1.0,     -2.0,   -1.0,    1.0,   -1.0,  -1.0,   1.0,   1.0,   1.0,    -1.0,
        -1.0,      1.0,    1.0,   -1.0,    1.0,   1.0,  -1.0,  -1.0,  -1.0,    -1.0,
        -1.0,     -1.0,   -1.0,    1.0,   -1.0,   1.0
    };

    /** coefficients for nutation in longitude, t part, in 0.1milliarcsec. */
    private static final double[] SLT = {
        -174.2,  0.2,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0, -1.6, -3.4,
        +1.2,   -0.5,  0.1, 0.0, 0.0, -0.1, 0.0, 0.1,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        -0.2,    0.1, -0.4, 0.0, 0.0,  0.0, 0.0, 0.1, -0.1,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0,  0.0,
        +0.0,    0.0,  0.0, 0.0, 0.0,  0.0
    };

    /** coefficients for nutation in obliquity, const part, in 0.1milliarcsec. */
    private static final double[] CO = {
        +92025.0, -895.0, -24.0,  0.0,    1.0,   0.0,   1.0,   0.0,   5736.0, 54.0,
        +224.0,   -95.0,  -70.0,  1.0,    0.0,   0.0,   9.0,   7.0,   6.0,    3.0,
        +3.0,     -2.0,   -2.0,   0.0,    0.0,   0.0,   0.0,   0.0,   0.0,    0.0,
        +977.0,   -7.0,    200.0, 129.0, -1.0,  -53.0, -2.0,  -33.0,  32.0,   26.0,
        +27.0,     16.0,  -1.0,  -12.0,   13.0, -1.0,  -10.0, -8.0,   7.0,    5.0,
        +0.0,     -3.0,    3.0,   3.0,    0.0,  -3.0,   3.0,   3.0,  -3.0,    3.0,
        +0.0,      3.0,    0.0,   0.0,    0.0,   0.0,   0.0,   1.0,   1.0,    1.0,
        +1.0,      1.0,   -1.0,   1.0,   -1.0,   1.0,   0.0,  -1.0,  -1.0,    0.0,
        -1.0,      1.0,    0.0,  -1.0,    1.0,   1.0,   0.0,   0.0,  -1.0,    0.0,
        +0.0,      0.0,    0.0,   0.0,    0.0,   0.0,   0.0,   0.0,   0.0,    0.0,
        +0.0,      0.0,    0.0,   0.0,    0.0,   0.0
    };

    /** coefficients for nutation in obliquity, t part, in 0.1milliarcsec. */
    private static final double[] COT = {
        +8.9,  0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -3.1, -0.1,
        -0.6,  0.3,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        -0.5,  0.0,  0.0, -0.1,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
        +0.0,  0.0,  0.0,  0.0,  0.0,  0.0
    };

    /** Start date for applying Moon corrections to the equation of the equinoxes.
     * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
     */
    private static final AbsoluteDate NEW_EQE_MODEL_START =
        new AbsoluteDate(1997, 2, 27, 0, 0, 30, TimeScalesFactory.getTAI());

    /** "Left-central" date of the interpolation array. */
    private double tCenter;

    /** Step size of the interpolation array. */
    private final double h;

    /** Nutation in longitude of reference. */
    private final double[] dpsiRef;

    /** Nutation in obliquity of reference. */
    private final double[] depsRef;

    /** Neville interpolation array for dpsi. */
    private final double[] dpsiNeville;

    /** Neville interpolation array for deps. */
    private final double[] depsNeville;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Flag for EOP correction application. */
    private final boolean applyEOPCorrection;

    /** Simple constructor, applying EOP corrections (here, nutation).
     * @param factoryKey key of the frame within the factory
     * @exception OrekitException if EOP parameters cannot be read
     */
    protected TODFrame(final Predefined factoryKey)
        throws OrekitException {
        this(true, factoryKey);
    }

    /** Simple constructor.
     * @param applyEOPCorr if true, EOP correction is applied (here, nutation)
     * @param factoryKey key of the frame within the factory
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected TODFrame(final boolean applyEOPCorr, final Predefined factoryKey)
        throws OrekitException {

        super(FramesFactory.getMOD(applyEOPCorr), null , true, factoryKey);

        applyEOPCorrection = applyEOPCorr;

        // set up an interpolation model on 12 points with a 1/2 day step
        // this leads to an interpolation error of about 1.7e-10 arcseconds
        final int n = 12;
        h           = 43600.0;

        tCenter     = Double.NaN;
        dpsiRef     = new double[n];
        depsRef     = new double[n];
        dpsiNeville = new double[n];
        depsNeville = new double[n];

        // everything is in place, we can now synchronize the frame
        updateFrame(AbsoluteDate.J2000_EPOCH);

    }

    /** Indicate if EOP correction is applied.
     * @return true if EOP correction is applied
     */
    boolean isEOPCorrectionApplied() {
        return applyEOPCorrection;
    }

    /** Update the frame to the given date.
     * <p>The update considers the nutation effects from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // offset from J2000.0 epoch
            final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH);

            // evaluate the nutation elements
            final double[] nutation = getInterpolatedNutationElements(t);

            // compute the mean obliquity of the ecliptic
            final double moe = getMeanObliquityOfEcliptic(t);

            // get the IAU1980 corrections for the nutation parameters
            final NutationCorrection nutCorr =
                ((MODFrame) getParent()).getNutationCorrection(date);

            final double deps = nutation[1] + nutCorr.getDdeps();
            final double dpsi = nutation[0] + nutCorr.getDdpsi();

            // compute the true obliquity of the ecliptic
            final double toe = moe + deps;

            // set up the elementary rotations for nutation
            final Rotation r1 = new Rotation(Vector3D.PLUS_I,  toe);
            final Rotation r2 = new Rotation(Vector3D.PLUS_K,  dpsi);
            final Rotation r3 = new Rotation(Vector3D.PLUS_I, -moe);

            // complete nutation
            final Rotation precession = r1.applyTo(r2.applyTo(r3));

            // set up the transform from parent MOD
            setTransform(new Transform(precession));

            cachedDate = date;

        }

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @return euqation of the equinoxes
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEquationOfEquinoxes(final AbsoluteDate date)
        throws OrekitException {

        // offset from J2000 epoch in seconds
        final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH);

        // nutation in longitude
        final double dPsi = getInterpolatedNutationElements(t) [0];

        // mean obliquity of ecliptic
        final double moe = getMeanObliquityOfEcliptic(t);

        // original definition of equation of equinoxes
        double eqe = dPsi * FastMath.cos(moe);

        if (date.compareTo(NEW_EQE_MODEL_START) >= 0) {

            // IAU 1994 resolution C7 added two terms to the equation of equinoxes
            // taking effect since 1997-02-27 for continuity

            // Mean longitude of the ascending node of the Moon
            final double tc = t / Constants.JULIAN_CENTURY;
            final double om = ((F53 * tc + F52) * tc + F510) * tc + F50 + ((F511 * tc) % 1.0) * MathUtils.TWO_PI;

            // add the two correction terms
            eqe += EQE_1 * FastMath.sin(om) + EQE_2 * FastMath.sin(om + om);

        }

        return eqe;

    }

    /** Compute the mean obliquity of the ecliptic.
     * @param t offset from J2000 epoch in seconds
     * @return mean obliquity of ecliptic
     */
    private double getMeanObliquityOfEcliptic(final double t) {

        // offset from J2000 epoch in julian centuries
        final double tc = t / Constants.JULIAN_CENTURY;

        // compute the mean obliquity of the ecliptic
        return ((MOE_3 * tc + MOE_2) * tc + MOE_1) * tc + MOE_0;

    }

    /** Set the interpolated nutation elements.
     * @param t offset from J2000.0 epoch in seconds
     * @return interpolated nutation elements in a two elements array,
     * with dPsi at index 0 and dEpsilon at index 1
     */
    public double[] getInterpolatedNutationElements(final double t) {

        final int n    = dpsiRef.length;
        final int nM12 = (n - 1) / 2;
        if (Double.isNaN(tCenter) || (t < tCenter) || (t > tCenter + h)) {
            // recompute interpolation array
            setReferencePoints(t);
        }

        // interpolate nutation elements using Neville's algorithm
        System.arraycopy(dpsiRef, 0, dpsiNeville, 0, n);
        System.arraycopy(depsRef, 0, depsNeville, 0, n);
        final double theta = (t - tCenter) / h;
        for (int j = 1; j < n; ++j) {
            for (int i = n - 1; i >= j; --i) {
                final double c1 = (theta + nM12 - i + j) / j;
                final double c2 = (theta + nM12 - i) / j;
                dpsiNeville[i] = c1 * dpsiNeville[i] - c2 * dpsiNeville[i - 1];
                depsNeville[i] = c1 * depsNeville[i] - c2 * depsNeville[i - 1];
            }
        }

        return new double[] {
            dpsiNeville[n - 1], depsNeville[n - 1]
        };

    }

    /** Set the reference points array.
     * @param t offset from J2000.0 epoch in seconds
     */
    private void setReferencePoints(final double t) {

        final int n    = dpsiRef.length;
        final int nM12 = (n - 1) / 2;

        // evaluate new location of center interval
        final double newTCenter = h * FastMath.floor(t / h);

        // shift reusable reference points
        int iMin = 0;
        int iMax = n;
        final int shift = (int) FastMath.rint((newTCenter - tCenter) / h);
        if (!Double.isNaN(tCenter) && (FastMath.abs(shift) < n)) {
            if (shift >= 0) {
                System.arraycopy(dpsiRef, shift, dpsiRef, 0, n - shift);
                System.arraycopy(depsRef, shift, depsRef, 0, n - shift);
                iMin = n - shift;
            } else {
                System.arraycopy(dpsiRef, 0, dpsiRef, -shift, n + shift);
                System.arraycopy(depsRef, 0, depsRef, -shift, n + shift);
                iMax = -shift;
            }
        }

        // compute new reference points
        tCenter = newTCenter;
        for (int i = iMin; i < iMax; ++i) {
            final double[] nutation = computeNutationElements(tCenter + (i - nM12) * h);
            dpsiRef[i] = nutation[0];
            depsRef[i] = nutation[1];
        }

    }

    /** Compute nutation elements.
     * <p>This method applies the IAU-1980 theory and hence is rather slow.
     * It is called by the {@link #setInterpolatedNutationElements(double)}
     * on a small number of reference points only.</p>
     * @param t offset from J2000.0 epoch in seconds
     * @return computed nutation elements in a two elements array,
     * with dPsi at index 0 and dEpsilon at index 1
     */
    public double[] computeNutationElements(final double t) {

        // offset in julian centuries
        final double tc =  t / Constants.JULIAN_CENTURY;
        // mean anomaly of the Moon
        final double l  = ((F13 * tc + F12) * tc + F110) * tc + F10 + ((F111 * tc) % 1.0) * MathUtils.TWO_PI;
        // mean anomaly of the Sun
        final double lp = ((F23 * tc + F22) * tc + F210) * tc + F20 + ((F211 * tc) % 1.0) * MathUtils.TWO_PI;
        // L - &Omega; where L is the mean longitude of the Moon
        final double f  = ((F33 * tc + F32) * tc + F310) * tc + F30 + ((F311 * tc) % 1.0) * MathUtils.TWO_PI;
        // mean elongation of the Moon from the Sun
        final double d  = ((F43 * tc + F42) * tc + F410) * tc + F40 + ((F411 * tc) % 1.0) * MathUtils.TWO_PI;
        // mean longitude of the ascending node of the Moon
        final double om = ((F53 * tc + F52) * tc + F510) * tc + F50 + ((F511 * tc) % 1.0) * MathUtils.TWO_PI;

        // loop size
        final int n = CL.length;
        // Initialize nutation elements.
        double dpsi = 0.0;
        double deps = 0.0;

        // Sum the nutation terms from smallest to biggest.
        for (int j = n - 1; j >= 0; j--)
        {
            // Set up current argument.
            final double arg = CL[j] * l + CLP[j] * lp + CF[j] * f + CD[j] * d + COM[j] * om;

            // Accumulate current nutation term.
            final double s = SL[j] + SLT[j] * tc;
            final double c = CO[j] + COT[j] * tc;
            if (s != 0.0) dpsi += s * FastMath.sin(arg);
            if (c != 0.0) deps += c * FastMath.cos(arg);
        }

        // Convert results from 0.1 mas units to radians. */
        return new double[] {
            dpsi * Constants.ARC_SECONDS_TO_RADIANS * 1.e-4,
            deps * Constants.ARC_SECONDS_TO_RADIANS * 1.e-4
        };

    }

}
