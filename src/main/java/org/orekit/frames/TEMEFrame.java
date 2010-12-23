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
import org.orekit.utils.Constants;

/** True Equator Mean Equinox Frame.
 * <p>This frame is used for the SGP4 model in TLE propagation. This frame has <em>no</em>
 * official definition and there are some ambiguities about whether it should be used
 * as "of date" or "of epoch". This frame should therefore be used <em>only</em> for
 * TLE propagation and not for anything else, as recommended by the CCSDS Orbit Data Message
 * blue book.</p>
 * <p>This implementation is compliant to the one suggested in Vallado's "Fundamentals of
 * Astrodynamics and Applications" and uses the {@link TODFrame} as the parent of TEME with
 * the equation of the equinoxes with ten terms of nutation, no simplification of
 * trigonometric terms and ignoring the post 1997 kinematic correction.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
class TEMEFrame extends FactoryManagedFrame {

    /** Serializable UID. */
    private static final long serialVersionUID = 4804891311476040157L;

    // CHECKSTYLE: stop JavadocVariable check

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
        0,  0,  0,  0,  0,  1,   0,  0,  1,  0
    };

    /** coefficients of l', mean anomaly of the Sun. */
    private static final int[] CLP = {
        0,  0,  0,  0,  1,  0,   1,  0,  0, -1
    };

    /** coefficients of F = L - &Omega, where L is the mean longitude of the Moon. */
    private static final int[] CF = {
        0,  2,  2,  0,  0,  0,   2,  2,  2,  2
    };

    /** coefficients of D, mean elongation of the Moon from the Sun. */
    private static final int[] CD = {
        0, -2,  0,  0,  0,  0,  -2,  0,  0, -2
    };

    /** coefficients of &Omega, mean longitude of the ascending node of the Moon. */
    private static final int[] COM = {
        1,  2,  2,  2,  0,  0,   2,  1,  2,  2
    };

    /** coefficients for nutation in longitude, const part, in 0.1milliarcsec. */
    private static final double[] SL = {
        -171996.0, -13187.0, -2274.0, 2062.0, 1426.0, 712.0, -517.0, -386.0, -301.0, 217.0
    };

    /** coefficients for nutation in longitude, t part, in 0.1milliarcsec. */
    private static final double[] SLT = {
        -174.2, -1.6, -0.2, 0.2, -3.4, 0.1, 1.2, -0.4, 0.0, -0.5
    };

    /** Nutation in longitude current. */
    private double dpsiCurrent;

    /** Mean obliquity of the ecliptic. */
    private double moe;

    /** Cached date to avoid useless calculus. */
    private AbsoluteDate cachedDate;

    /** Simple constructor.
     * @param factoryKey key of the frame within the factory
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected TEMEFrame(final Predefined factoryKey)
        throws OrekitException {

        super(FramesFactory.getTOD(false), null, false, factoryKey);

        // everything is in place, we can now synchronize the frame
        updateFrame(AbsoluteDate.J2000_EPOCH);

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
            computeNutationElements(tts);

            // offset from J2000 epoch in julian centuries
            final double ttc = tts / Constants.JULIAN_CENTURY;

            // compute the mean obliquity of the ecliptic
            moe = ((MOE_3 * ttc + MOE_2) * ttc + MOE_1) * ttc + MOE_0;

            final double eqe = getEquationOfEquinoxes(date, ttc);

            // set up the transform from parent TOD
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, -eqe)));

            cachedDate = date;

        }
    }

    /** Compute nutation elements.
     * <p>This method applies the IAU-1980 theory and hence is rather slow.
     * It is called by the {@link #setInterpolatedNutationElements(double)}
     * on a small number of reference points only.</p>
     * @param t offset from J2000.0 epoch in seconds
     */
    private void computeNutationElements(final double t) {

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

        // Sum the nutation terms from smallest to biggest.
        for (int j = n - 1; j >= 0; j--) {
            // Set up current argument.
            final double arg = CL[j] * l + CLP[j] * lp + CF[j] * f + CD[j] * d + COM[j] * om;

            // Accumulate current nutation term.
            final double s = SL[j] + SLT[j] * tc;
            if (s != 0.0) {
                dpsi += s * FastMath.sin(arg);
            }
        }

        // Convert results from 0.1 mas units to radians. */
        dpsiCurrent = dpsi * Constants.ARC_SECONDS_TO_RADIANS * 1.e-4;

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @param tc offset from J2000.0 epoch in julian centuries
     * @return Equation of the Equinoxes at the current date in radians
     * @exception OrekitException if nutation model cannot be computed
     */
    private double getEquationOfEquinoxes(final AbsoluteDate date, final double tc)
        throws OrekitException {

        return dpsiCurrent * FastMath.cos(moe);

    }
}
