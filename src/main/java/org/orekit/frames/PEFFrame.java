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
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Pseudo Earth Fixed Frame.
 * <p> This frame handles the sidereal time according to IAU-82 model.</p>
 * <p> Its parent frame is the {@link TEMEFrame}.</p>
 * <p> The pole motion is not applied here.</p>
 * @author Pascal Parraud
 * @author Thierry Ceolin
 * @version $Revision$ $Date$
 */
class PEFFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 3979164567293107541L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Radians per second of time. */
    private static final double RADIANS_PER_SECOND = TWO_PI / Constants.JULIAN_DAY;

    /** Radians per arcsecond. */
    private static final double RADIANS_PER_ARC_SECOND = Math.PI / 648000;

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** Reference date for IAU 1982 GMST-UT1 model. */
    private static final AbsoluteDate GMST_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());

    /** First coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_0 = 24110.54841;

    /** Second coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_1 = 8640184.812866;

    /** Third coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_2 = 0.093104;

    /** Fourth coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_3 = -6.2e-6;

    // CHECKSTYLE: stop JavadocVariable check

    // Coefficients for the Mean Obliquity of the Ecliptic.
    private static final double MOE_0 = 84381.448    * RADIANS_PER_ARC_SECOND;
    private static final double MOE_1 =   -46.8150   * RADIANS_PER_ARC_SECOND;
    private static final double MOE_2 =    -0.00059  * RADIANS_PER_ARC_SECOND;
    private static final double MOE_3 =     0.001813 * RADIANS_PER_ARC_SECOND;

    // Coefficients for the Equation of the Equinoxes.
    private static final double EQE_1 =     0.00264  * RADIANS_PER_ARC_SECOND;
    private static final double EQE_2 =     0.000063 * RADIANS_PER_ARC_SECOND;

    // lunisolar nutation elements
    // Coefficients for l (Mean Anomaly of the Moon).
    private static final double F10  = Math.toRadians(134.96340251);
    private static final double F110 =    715923.2178    * RADIANS_PER_ARC_SECOND;
    private static final double F111 =      1325.0;
    private static final double F12  =        31.87908   * RADIANS_PER_ARC_SECOND;
    private static final double F13  =         0.0516348 * RADIANS_PER_ARC_SECOND;

    // Coefficients for l' (Mean Anomaly of the Sun).
    private static final double F20  = Math.toRadians(357.52752910918);
    private static final double F210 =   1292581.048     * RADIANS_PER_ARC_SECOND;
    private static final double F211 =        99.0;
    private static final double F22  =        -0.55332   * RADIANS_PER_ARC_SECOND;
    private static final double F23  =         0.0001368 * RADIANS_PER_ARC_SECOND;

    // Coefficients for F = L (Mean Longitude of the Moon) - Omega.
    private static final double F30  = Math.toRadians(93.27209062);
    private static final double F310 =    295262.8477    * RADIANS_PER_ARC_SECOND;
    private static final double F311 =      1342.0;
    private static final double F32  =       -12.7512    * RADIANS_PER_ARC_SECOND;
    private static final double F33  =        -0.0010368 * RADIANS_PER_ARC_SECOND;

    // Coefficients for D (Mean Elongation of the Moon from the Sun).
    private static final double F40  = Math.toRadians(297.85019547);
    private static final double F410 =   1105601.209     * RADIANS_PER_ARC_SECOND;
    private static final double F411 =      1236.0;
    private static final double F42  =        -6.37056   * RADIANS_PER_ARC_SECOND;
    private static final double F43  =         0.0065916 * RADIANS_PER_ARC_SECOND;

    // Coefficients for Omega (Mean Longitude of the Ascending Node of the Moon).
    private static final double F50  = Math.toRadians(125.04452222);
    private static final double F510 =   -482890.539 * RADIANS_PER_ARC_SECOND;
    private static final double F511 =        -5.0;
    private static final double F52  =         7.455 * RADIANS_PER_ARC_SECOND;
    private static final double F53  =         0.008 * RADIANS_PER_ARC_SECOND;

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


    /** "Left-central" date of the interpolation array. */
    private double tCenter;

    /** Step size of the interpolation array. */
    private final double h;

    /** Nutation in longitude current. */
    private double dpsiCurrent;

    /** Nutation in obliquity current. */
    private double depsCurrent;

    /** Nutation in longitude of reference. */
    private final double[] dpsiRef;

    /** Nutation in obliquity of reference. */
    private final double[] depsRef;

    /** Neville interpolation array for dpsi. */
    private final double[] dpsiNeville;

    /** Neville interpolation array for deps. */
    private final double[] depsNeville;

    /** Mean obliquity of the ecliptic. */
    private double moe;

    /** Cached date to avoid useless calculus. */
    private AbsoluteDate cachedDate;

    /** EOP history. */
    private final EOP1980History eopHistory;

    /** Simple constructor, applying EOP corrections (here, lod).
     * @param date the date.
     * @param name name of the frame
     * @exception OrekitException if EOP parameters cannot be read
     */
    protected PEFFrame(final AbsoluteDate date, final String name)
        throws OrekitException {
        this(true, date, name);
    }

    /** Simple constructor.
     * @param applyEOPCorr if true, EOP corrections are applied (here, lod)
     * @param date the current date
     * @param name the string representation
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected PEFFrame(final boolean applyEOPCorr,
                       final AbsoluteDate date, final String name)
        throws OrekitException {

        super(FramesFactory.getTEME(applyEOPCorr), null, name, false);

        // we need this history even if we don't apply all correction,
        // as we at least always apply the very large UT1-UTC offset
        eopHistory = FramesFactory.getEOP1980History();

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
        updateFrame(date);

    }

    /** Update the frame to the given date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // offset from J2000.0 epoch
            final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);

            // evaluate the nutation elements
            setInterpolatedNutationElements(tts);

            // offset from J2000 epoch in julian centuries
            final double ttc = tts / Constants.JULIAN_CENTURY;

            // compute the mean obliquity of the ecliptic
            moe = ((MOE_3 * ttc + MOE_2) * ttc + MOE_1) * ttc + MOE_0;

            final double eqe = getNewEquationOfEquinoxes(date, ttc);

            // offset in julian centuries from J2000 epoch (UT1 scale)
            final double dtai = date.durationFrom(GMST_REFERENCE);
            final double dutc = TimeScalesFactory.getUTC().offsetFromTAI(date);
            final double dut1 = eopHistory.getUT1MinusUTC(date);

            final double tut1 = dtai + dutc + dut1;
            final double tt   = tut1 / Constants.JULIAN_CENTURY;

            // Seconds in the day, adjusted by 12 hours because the
            // UT1 is supplied as a Julian date beginning at noon.
            final double sd = (tut1 + Constants.JULIAN_DAY / 2.) % Constants.JULIAN_DAY;

            // compute Greenwich mean sidereal time, in radians
            final double gmst = (((GMST_3 * tt + GMST_2) * tt + GMST_1) * tt + GMST_0 + sd) *
                                RADIANS_PER_SECOND;

            // compute Greenwich apparent sidereal time, in radians
            final double gast = gmst + eqe;

            // compute true angular rotation of Earth, in rad/s
            final double lod = ((TEMEFrame) getParent()).isEOPCorrectionApplied() ?
                               eopHistory.getLOD(date) : 0.0;
            final double omp = AVE * (1 - lod / Constants.JULIAN_DAY);
            final Vector3D rotationRate = new Vector3D(omp, Vector3D.PLUS_K);

            // set up the transform from parent TEME
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, -gast), rotationRate));

            cachedDate = date;

        }
    }

    /** Set the interpolated nutation elements.
     * @param t offset from J2000.0 epoch in seconds
     */
    protected void setInterpolatedNutationElements(final double t) {

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

        dpsiCurrent = dpsiNeville[n - 1];
        depsCurrent = depsNeville[n - 1];

    }

    /** Set the reference points array.
     * @param t offset from J2000.0 epoch in seconds
     */
    private void setReferencePoints(final double t) {

        final int n    = dpsiRef.length;
        final int nM12 = (n - 1) / 2;

        // evaluate new location of center interval
        final double newTCenter = h * Math.floor(t / h);

        // shift reusable reference points
        int iMin = 0;
        int iMax = n;
        final int shift = (int) Math.rint((newTCenter - tCenter) / h);
        if (!Double.isNaN(tCenter) && (Math.abs(shift) < n)) {
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
            computeNutationElements(tCenter + (i - nM12) * h);
            dpsiRef[i] = dpsiCurrent;
            depsRef[i] = depsCurrent;
        }

    }

    /** Compute nutation elements.
     * <p>This method applies the IAU-1980 theory and hence is rather slow.
     * It is called by the {@link #setInterpolatedNutationElements(double)}
     * on a small number of reference points only.</p>
     * @param t offset from J2000.0 epoch in seconds
     */
    protected void computeNutationElements(final double t) {

        // offset in julian centuries
        final double tc =  t / Constants.JULIAN_CENTURY;
        // mean anomaly of the Moon
        final double l  = ((F13 * tc + F12) * tc + F110) * tc + F10 + ((F111 * tc) % 1.0) * TWO_PI;
        // mean anomaly of the Sun
        final double lp = ((F23 * tc + F22) * tc + F210) * tc + F20 + ((F211 * tc) % 1.0) * TWO_PI;
        // L - &Omega; where L is the mean longitude of the Moon
        final double f  = ((F33 * tc + F32) * tc + F310) * tc + F30 + ((F311 * tc) % 1.0) * TWO_PI;
        // mean elongation of the Moon from the Sun
        final double d  = ((F43 * tc + F42) * tc + F410) * tc + F40 + ((F411 * tc) % 1.0) * TWO_PI;
        // mean longitude of the ascending node of the Moon
        final double om = ((F53 * tc + F52) * tc + F510) * tc + F50 + ((F511 * tc) % 1.0) * TWO_PI;

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
            if (s != 0.0) dpsi += s * Math.sin(arg);
            if (c != 0.0) deps += c * Math.cos(arg);
        }

        // Convert results from 0.1 mas units to radians. */
        dpsiCurrent = dpsi * RADIANS_PER_ARC_SECOND * 1.e-4;
        depsCurrent = deps * RADIANS_PER_ARC_SECOND * 1.e-4;

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @param tc offset from J2000.0 epoch in julian centuries
     * @return Equation of the Equinoxes at the current date in radians
     * @exception OrekitException if nutation model cannot be computed
     */
    private double getNewEquationOfEquinoxes(final AbsoluteDate date, final double tc)
        throws OrekitException {

        // See vallado, page 231, to adhere to the original definition (USNO circ 163),
        // nutation corrections are not included

        // Mean longitude of the ascending node of the Moon
        final double om = ((F53 * tc + F52) * tc + F510) * tc + F50 + ((F511 * tc) % 1.0) * TWO_PI;

        // Equation of the Equinoxes
        return dpsiCurrent * Math.cos(moe) + EQE_1 * Math.sin(om) + EQE_2 * Math.sin(om + om);

    }

}
