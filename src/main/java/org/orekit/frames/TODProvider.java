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
package org.orekit.frames;

import java.io.InputStream;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.data.DelaunayArguments;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Provider for True of Date (ToD) frame.
 * <p>This frame handles nutation effects according to the IAU-80 theory.</p>
 * <p>Transform is computed with reference to the frame is the {@link MODProvider Mean of Date}.</p>
 * @author Pascal Parraud
 */
class TODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130606L;

    /** First Moon correction term for the Equation of the Equinoxes. */
    private static final double EQE_1 =     0.00264  * Constants.ARC_SECONDS_TO_RADIANS;

    /** Second Moon correction term for the Equation of the Equinoxes. */
    private static final double EQE_2 =     0.000063 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Degree 0 coefficient for the Mean Obliquity of the Ecliptic. */
    private static final double MOE_0 = 84381.448    * Constants.ARC_SECONDS_TO_RADIANS;

    /** Degree 1 coefficient for the Mean Obliquity of the Ecliptic. */
    private static final double MOE_1 =   -46.8150   * Constants.ARC_SECONDS_TO_RADIANS;

    /** Degree 2 coefficient for the Mean Obliquity of the Ecliptic. */
    private static final double MOE_2 =    -0.00059  * Constants.ARC_SECONDS_TO_RADIANS;

    /** Degree 3 coefficient for the Mean Obliquity of the Ecliptic. */
    private static final double MOE_3 =     0.001813 * Constants.ARC_SECONDS_TO_RADIANS;

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

    /** EOP history. */
    private final EOP1980History eopHistory;

    /** Generator for fundamental nutation arguments. */
    private final FundamentalNutationArguments nutationArguments;

    /** Simple constructor.
     * @param applyEOPCorr if true, EOP correction is applied (here, nutation)
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    public TODProvider(final boolean applyEOPCorr)
        throws OrekitException {

        eopHistory  = applyEOPCorr ? FramesFactory.getEOP1980History() : null;

        // get the Delaunay arguments table data, from IERS 2010 conventions
        // the TOD itself is not defined in the 2010 conventions. It was superseded in 2003
        // when IERS switched from equinox-based frame to non-rotating origin paradigm
        // So we don't set up a constructor argument allowing the user to choose
        // IERS conventions here, as it would be misleading. However, we do need the
        // raw Delaunay arguments, so we retrieve them from a hard-coded set of conventions.
        final String name        = IERSConventions.IERS_2010.getNutationArguments();
        final InputStream stream = TODProvider.class.getResourceAsStream(name);
        nutationArguments        = new FundamentalNutationArguments(stream, name);

    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    double getLOD(final AbsoluteDate date) {
        return (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        return (eopHistory == null) ? PoleCorrection.NULL_CORRECTION : eopHistory.getPoleCorrection(date);
    }

    /** Get the transform from Mean Of Date at specified date.
     * <p>The update considers the nutation effects from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute Delaunay arguments
        final DelaunayArguments arguments = nutationArguments.evaluateDelaunay(date);

        // evaluate the nutation elements
        final double[] nutation = computeNutationElements(arguments);

        // compute the mean obliquity of the ecliptic
        final double moe = getMeanObliquityOfEcliptic(arguments);

        // get the IAU1980 corrections for the nutation parameters
        final NutationCorrection nutCorr = (eopHistory == null) ?
                                           NutationCorrection.NULL_CORRECTION :
                                           eopHistory.getNutationCorrection(date);

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
        return new Transform(date, precession);

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @return equation of the equinoxes
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEquationOfEquinoxes(final AbsoluteDate date)
        throws OrekitException {

        // compute Delaunay arguments
        final DelaunayArguments elements = nutationArguments.evaluateDelaunay(date);

        // nutation in longitude
        final double dPsi = computeNutationElements(elements) [0];

        // mean obliquity of ecliptic
        final double moe = getMeanObliquityOfEcliptic(elements);

        // original definition of equation of equinoxes
        double eqe = dPsi * FastMath.cos(moe);

        if (date.compareTo(NEW_EQE_MODEL_START) >= 0) {

            // IAU 1994 resolution C7 added two terms to the equation of equinoxes
            // taking effect since 1997-02-27 for continuity

            // Mean longitude of the ascending node of the Moon
            final double om = elements.getOmega();

            // add the two correction terms
            eqe += EQE_1 * FastMath.sin(om) + EQE_2 * FastMath.sin(om + om);

        }

        return eqe;

    }

    /** Compute the mean obliquity of the ecliptic.
     * @param arguments Delaunay arguments
     * @return mean obliquity of ecliptic
     */
    private double getMeanObliquityOfEcliptic(final DelaunayArguments arguments) {

        // offset from J2000 epoch in Julian centuries
        final double tc = arguments.getTC();

        // compute the mean obliquity of the ecliptic
        return ((MOE_3 * tc + MOE_2) * tc + MOE_1) * tc + MOE_0;

    }

    /** Compute nutation elements.
     * <p>This method applies the IAU-1980 theory and hence is rather slow.
     * It is indirectly called by the {@link #getInterpolatedNutationElements(double)}
     * on a small number of reference points only.</p>
     * @param arguments Delaunay arguments
     * @return computed nutation elements in a two elements array,
     * with dPsi at index 0 and dEpsilon at index 1
     */
    private double[] computeNutationElements(final DelaunayArguments arguments) {

        // offset in julian centuries
        final double tc =  arguments.getTC();
        // mean anomaly of the Moon
        final double l  = arguments.getL();
        // mean anomaly of the Sun
        final double lp = arguments.getLPrime();
        // L - &Omega; where L is the mean longitude of the Moon
        final double f  = arguments.getF();
        // mean elongation of the Moon from the Sun
        final double d  = arguments.getD();
        // mean longitude of the ascending node of the Moon
        final double om = arguments.getOmega();

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
