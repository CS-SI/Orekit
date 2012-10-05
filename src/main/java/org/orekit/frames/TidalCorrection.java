/* Copyright 2002-2012 CS Systèmes d'Information
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;


/** Compute tidal correction to the pole motion.
 * <p>This class computes the diurnal and semidiurnal variations in the
 * Earth orientation. It is a java translation of the fortran subroutine
 * found at ftp://tai.bipm.org/iers/conv2003/chapter8/ortho_eop.f.</p>
 * @author Pascal Parraud
 * @author Evan Ward
 */
public class TidalCorrection implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 9143236723147294697L;

    /** pi;/2. */
    private static final double HALF_PI = FastMath.PI / 2.0;

    /** Angular units conversion factor. */
    private static final double MICRO_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;

    /** Time units conversion factor. */
    private static final double MICRO_SECONDS_TO_SECONDS = 1.0e-6;

    /** Number of interpolation points to use. */
    private static final int INTERPOLATION_POINTS = 8;

    /** Step size in days of the interpolation data points. */
    private static final double STEP_SIZE = 3.0 / 32.0;

    /** Difference in days between the modified julian day epoch and the year 1960. */
    private static final double MJD_TO_1960 = 37076.5;

    /** HS parameter. */
    private static final double[] HS = {
        -001.94, -001.25, -006.64, -001.51, -008.02,
        -009.47, -050.20, -001.80, -009.54, +001.52,
        -049.45, -262.21, +001.70, +003.43, +001.94,
        +001.37, +007.41, +020.62, +004.14, +003.94,
        -007.14, +001.37, -122.03, +001.02, +002.89,
        -007.30, +368.78, +050.01, -001.08, +002.93,
        +005.25, +003.95, +020.62, +004.09, +003.42,
        +001.69, +011.29, +007.23, +001.51, +002.16,
        +001.38, +001.80, +004.67, +016.01, +019.32,
        +001.30, -001.02, -004.51, +120.99, +001.13,
        +022.98, +001.06, -001.90, -002.18, -023.58,
        +631.92, +001.92, -004.66, -017.86, +004.47,
        +001.97, +017.20, +294.00, -002.46, -001.02,
        +079.96, +023.83, +002.59, +004.47, +001.95,
        +001.17
    };

    /** PHASE parameter. */
    private static final double[] PHASE = {
        +09.0899831 - HALF_PI, +08.8234208 - HALF_PI, +12.1189598 - HALF_PI,
        +01.4425700 - HALF_PI, +04.7381090 - HALF_PI, +04.4715466 - HALF_PI,
        +07.7670857 - HALF_PI, -02.9093042 - HALF_PI, +00.3862349 - HALF_PI,
        -03.1758666 - HALF_PI, +00.1196725 - HALF_PI, +03.4152116 - HALF_PI,
        +12.8946194 - HALF_PI, +05.5137686 - HALF_PI, +06.4441883 - HALF_PI,
        -04.2322016 - HALF_PI, -00.9366625 - HALF_PI, +08.5427453 - HALF_PI,
        +11.8382843 - HALF_PI, +01.1618945 - HALF_PI, +05.9693878 - HALF_PI,
        -01.2032249 - HALF_PI, +02.0923141 - HALF_PI, -01.7847596 - HALF_PI,
        +08.0679449 - HALF_PI, +00.8953321 - HALF_PI, +04.1908712 - HALF_PI,
        +07.4864102 - HALF_PI, +10.7819493 - HALF_PI, +00.3137975 - HALF_PI,
        +06.2894282 - HALF_PI, +07.2198478 - HALF_PI, -00.1610030 - HALF_PI,
        +03.1345361 - HALF_PI, +02.8679737 - HALF_PI, -04.5128771 - HALF_PI,
        +04.9665307 - HALF_PI, +08.2620698 - HALF_PI, +11.5576089 - HALF_PI,
        +00.6146566 - HALF_PI, +03.9101957 - HALF_PI,
        +20.6617051, +13.2808543, +16.3098310, +08.9289802, +05.0519065,
        +15.8350306, +08.6624178, +11.9579569, +08.0808832, +04.5771061,
        +00.7000324, +14.9869335, +11.4831564, +04.3105437, +07.6060827,
        +03.7290090, +10.6350594, +03.2542086, +12.7336164, +16.0291555,
        +10.1602590, +06.2831853, +02.4061116, +05.0862033, +08.3817423,
        +11.6772814, +14.9728205, +04.0298682, +07.3254073, +09.1574019
    };

    /** FREQUENCY parameter. */
    private static final double[] FREQUENCY = {
        05.18688050, 05.38346657, 05.38439079, 05.41398343, 05.41490765,
        05.61149372, 05.61241794, 05.64201057, 05.64293479, 05.83859664,
        05.83952086, 05.84044508, 05.84433381, 05.87485066, 06.03795537,
        06.06754801, 06.06847223, 06.07236095, 06.07328517, 06.10287781,
        06.24878055, 06.26505830, 06.26598252, 06.28318449, 06.28318613,
        06.29946388, 06.30038810, 06.30131232, 06.30223654, 06.31759007,
        06.33479368, 06.49789839, 06.52841524, 06.52933946, 06.72592553,
        06.75644239, 06.76033111, 06.76125533, 06.76217955, 06.98835826,
        06.98928248, 11.45675174, 11.48726860, 11.68477889, 11.71529575,
        11.73249771, 11.89560406, 11.91188181, 11.91280603, 11.93000800,
        11.94332289, 11.96052486, 12.11031632, 12.12363121, 12.13990896,
        12.14083318, 12.15803515, 12.33834347, 12.36886033, 12.37274905,
        12.37367327, 12.54916865, 12.56637061, 12.58357258, 12.59985198,
        12.60077620, 12.60170041, 12.60262463, 12.82880334, 12.82972756,
        13.06071921
    };

    /** Orthotide weight factors. */
    private static final double[] SP = {
        0.0298,
        0.1408,
        0.0805,
        0.6002,
        0.3025,
        0.1517,
        0.0200,
        0.0905,
        0.0638,
        0.3476,
        0.1645,
        0.0923
    };

    /** Orthoweights for X polar motion. */
    private static final double[] ORTHOWX = {
        -06.77832 * MICRO_ARC_SECONDS_TO_RADIANS,
        -14.86323 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.47884 * MICRO_ARC_SECONDS_TO_RADIANS,
        -01.45303 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.16406 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.42030 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.09398 * MICRO_ARC_SECONDS_TO_RADIANS,
        +25.73054 * MICRO_ARC_SECONDS_TO_RADIANS,
        -04.77974 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.28080 * MICRO_ARC_SECONDS_TO_RADIANS,
        +01.94539 * MICRO_ARC_SECONDS_TO_RADIANS,
        -00.73089 * MICRO_ARC_SECONDS_TO_RADIANS
    };

    /** Orthoweights for Y polar motion. */
    private static final double[] ORTHOWY = {
        +14.86283 * MICRO_ARC_SECONDS_TO_RADIANS,
        -06.77846 * MICRO_ARC_SECONDS_TO_RADIANS,
        +01.45234 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.47888 * MICRO_ARC_SECONDS_TO_RADIANS,
        -00.42056 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.16469 * MICRO_ARC_SECONDS_TO_RADIANS,
        +15.30276 * MICRO_ARC_SECONDS_TO_RADIANS,
        -04.30615 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.07564 * MICRO_ARC_SECONDS_TO_RADIANS,
        +02.28321 * MICRO_ARC_SECONDS_TO_RADIANS,
        -00.45717 * MICRO_ARC_SECONDS_TO_RADIANS,
        -01.62010 * MICRO_ARC_SECONDS_TO_RADIANS
    };

    /** Orthoweights for UT1. */
    private static final double[] ORTHOWT = {
        -1.76335 *  MICRO_SECONDS_TO_SECONDS,
        +1.03364 *  MICRO_SECONDS_TO_SECONDS,
        -0.27553 *  MICRO_SECONDS_TO_SECONDS,
        +0.34569 *  MICRO_SECONDS_TO_SECONDS,
        -0.12343 *  MICRO_SECONDS_TO_SECONDS,
        -0.10146 *  MICRO_SECONDS_TO_SECONDS,
        -0.47119 *  MICRO_SECONDS_TO_SECONDS,
        +1.28997 *  MICRO_SECONDS_TO_SECONDS,
        -0.19336 *  MICRO_SECONDS_TO_SECONDS,
        +0.02724 *  MICRO_SECONDS_TO_SECONDS,
        +0.08955 *  MICRO_SECONDS_TO_SECONDS,
        +0.04726 *  MICRO_SECONDS_TO_SECONDS
    };

    /** Cache of computed {@link Correction tidal corrections}. */
    private final TimeStampedCache<CorrectionData> cache;

    /** Simple constructor.
     */
    public TidalCorrection() {

        // create cache
        cache = new TimeStampedCache<CorrectionData>(INTERPOLATION_POINTS,
                                                     OrekitConfiguration.getCacheSlotsNumber(),
                                                     Constants.JULIAN_YEAR, 7 * Constants.JULIAN_DAY,
                                                     new Generator(), CorrectionData.class);

    }

    /** Get the dUT1 value.
     * @param date date at which the value is desired
     * @return dUT1 in seconds
     */
    public double getDUT1(final AbsoluteDate date) {

        try {
            final double t       = toDay(date);
            final double tCenter = toDayQuantum(t);

            final int n    = INTERPOLATION_POINTS;
            final int nM12 = (n - 1) / 2;

            final CorrectionData[] corrections = cache.getNeighbors(date);

            // copy points to a temporary array
            final double[] dtNeville = new double[n];
            for (int i = 0; i < n; i++) {
                dtNeville[i] = corrections[i].dt;
            }

            // interpolate corrections using Neville's algorithm
            final double theta = (t - tCenter) / STEP_SIZE;
            for (int j = 1; j < n; ++j) {
                for (int i = n - 1; i >= j; --i) {
                    final double c1 = (theta + nM12 - i + j) / j;
                    final double c2 = (theta + nM12 - i) / j;
                    dtNeville[i] = c1 * dtNeville[i] - c2 * dtNeville[i - 1];
                }
            }

            return dtNeville[n - 1];
        } catch (TimeStampedCacheException tce) {
            // this should never happen as the generator is not bounded
            throw OrekitException.createInternalError(tce);
        }

    }

    /** Get the pole IERS Reference Pole correction.
     * @param date date at which the correction is desired
     * @return pole correction
     */
    public  PoleCorrection getPoleCorrection(final AbsoluteDate date) {

        try {
            final double t       = toDay(date);
            final double tCenter = toDayQuantum(t);

            final int n    = INTERPOLATION_POINTS;
            final int nM12 = (n - 1) / 2;

            final CorrectionData[] corrections = this.cache.getNeighbors(date);

            // copy points to a temporary array
            final double[] dxNeville = new double[n];
            final double[] dyNeville = new double[n];
            for (int i = 0; i < n; i++) {
                dxNeville[i] = corrections[i].dx;
                dyNeville[i] = corrections[i].dy;
            }

            // interpolate corrections using Neville's algorithm
            final double theta = (t - tCenter) / STEP_SIZE;
            for (int j = 1; j < n; ++j) {
                for (int i = n - 1; i >= j; --i) {
                    final double c1 = (theta + nM12 - i + j) / j;
                    final double c2 = (theta + nM12 - i) / j;
                    dxNeville[i] = c1 * dxNeville[i] - c2 * dxNeville[i - 1];
                    dyNeville[i] = c1 * dyNeville[i] - c2 * dyNeville[i - 1];
                }
            }

            return new PoleCorrection(dxNeville[n - 1], dyNeville[n - 1]);
        } catch (TimeStampedCacheException tce) {
            // this should never happen as the generator is not bounded
            throw OrekitException.createInternalError(tce);
        }

    }

    /** Convert an {@link AbsoluteDate} to days past the epoch.
     * @param date the date to convert
     * @return days past the epoch, including the fractional part
     */
    private static double toDay(final AbsoluteDate date) {
        return date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY - MJD_TO_1960;
    }

    /** Convert days to an {@link AbsoluteDate}.
     * @param t the time in days
     * @return the date corresponding to {@code t}
     */
    private static AbsoluteDate toDate(final double t) {
        return AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(Constants.JULIAN_DAY * (t + MJD_TO_1960));
    }

    /** Convert a day to the closest quantum in terms of {@link #STEP_SIZE}.
     * @param t the day to convert
     * @return the closest quantum before t, still in days
     */
    private static double toDayQuantum(final double t) {
        return STEP_SIZE * FastMath.floor(t / STEP_SIZE);
    }

    /** Compute the partials of the tidal variations to the orthoweights.
     * @param t offset from reference epoch in days
     * @return the pole and UT1 correction
     */
    private static CorrectionData computeCorrections(final double t) {

        // compute the time dependent potential matrix
        final double d60A = t + 2;
        final double d60B = t;
        final double d60C = t - 2;

        double anm00 = 0;
        double anm01 = 0;
        double anm02 = 0;
        double bnm00 = 0;
        double bnm01 = 0;
        double bnm02 = 0;
        for (int j = 0; j < 41; j++) {

            final double hsj = HS[j];
            final double pj  = PHASE[j];
            final double fj  = FREQUENCY[j];

            final double alphaA = pj + fj * d60A;
            anm00 += hsj * FastMath.cos(alphaA);
            bnm00 -= hsj * FastMath.sin(alphaA);

            final double alphaB = pj + fj * d60B;
            anm01 += hsj * FastMath.cos(alphaB);
            bnm01 -= hsj * FastMath.sin(alphaB);

            final double alphaC = pj + fj * d60C;
            anm02 += hsj * FastMath.cos(alphaC);
            bnm02 -= hsj * FastMath.sin(alphaC);

        }

        double anm10 = 0;
        double anm11 = 0;
        double anm12 = 0;
        double bnm10 = 0;
        double bnm11 = 0;
        double bnm12 = 0;
        for (int j = 41; j < HS.length; j++) {

            final double hsj = HS[j];
            final double pj  = PHASE[j];
            final double fj  = FREQUENCY[j];

            final double alphaA = pj + fj * d60A;
            anm10 += hsj * FastMath.cos(alphaA);
            bnm10 -= hsj * FastMath.sin(alphaA);

            final double alphaB = pj + fj * d60B;
            anm11 += hsj * FastMath.cos(alphaB);
            bnm11 -= hsj * FastMath.sin(alphaB);

            final double alphaC = pj + fj * d60C;
            anm12 += hsj * FastMath.cos(alphaC);
            bnm12 -= hsj * FastMath.sin(alphaC);

        }

        // orthogonalize the response terms ...
        final double ap0 = anm02 + anm00;
        final double am0 = anm02 - anm00;
        final double bp0 = bnm02 + bnm00;
        final double bm0 = bnm02 - bnm00;
        final double ap1 = anm12 + anm10;
        final double am1 = anm12 - anm10;
        final double bp1 = bnm12 + bnm10;
        final double bm1 = bnm12 - bnm10;

        // ... and fill partials vector
        final double partials0  = SP[0] * anm01;
        final double partials1  = SP[0] * bnm01;
        final double partials2  = SP[1] * anm01 - SP[2] * ap0;
        final double partials3  = SP[1] * bnm01 - SP[2] * bp0;
        final double partials4  = SP[3] * anm01 - SP[4] * ap0 + SP[5] * bm0;
        final double partials5  = SP[3] * bnm01 - SP[4] * bp0 - SP[5] * am0;
        final double partials6  = SP[6] * anm11;
        final double partials7  = SP[6] * bnm11;
        final double partials8  = SP[7] * anm11 - SP[8] * ap1;
        final double partials9  = SP[7] * bnm11 - SP[8] * bp1;
        final double partials10 = SP[9] * anm11 - SP[10] * ap1 + SP[11] * bm1;
        final double partials11 = SP[9] * bnm11 - SP[10] * bp1 - SP[11] * am1;

        // combine partials to set up corrections
        final double dx =
                    partials0 * ORTHOWX[0] + partials1  * ORTHOWX[1]  + partials2  * ORTHOWX[2] +
                    partials3 * ORTHOWX[3] + partials4  * ORTHOWX[4]  + partials5  * ORTHOWX[5] +
                    partials6 * ORTHOWX[6] + partials7  * ORTHOWX[7]  + partials8  * ORTHOWX[8] +
                    partials9 * ORTHOWX[9] + partials10 * ORTHOWX[10] + partials11 * ORTHOWX[11];
        final double dy =
                    partials0 * ORTHOWY[0] + partials1  * ORTHOWY[1]  + partials2  * ORTHOWY[2] +
                    partials3 * ORTHOWY[3] + partials4  * ORTHOWY[4]  + partials5  * ORTHOWY[5] +
                    partials6 * ORTHOWY[6] + partials7  * ORTHOWY[7]  + partials8  * ORTHOWY[8] +
                    partials9 * ORTHOWY[9] + partials10 * ORTHOWY[10] + partials11 * ORTHOWY[11];
        final double dt =
                    partials0 * ORTHOWT[0] + partials1  * ORTHOWT[1]  + partials2  * ORTHOWT[2] +
                    partials3 * ORTHOWT[3] + partials4  * ORTHOWT[4]  + partials5  * ORTHOWT[5] +
                    partials6 * ORTHOWT[6] + partials7  * ORTHOWT[7]  + partials8  * ORTHOWT[8] +
                    partials9 * ORTHOWT[9] + partials10 * ORTHOWT[10] + partials11 * ORTHOWT[11];

        return new CorrectionData(toDate(t), dx, dy, dt);

    }

    /** A data cache container for tidal correction data.
     */
    private static class CorrectionData implements TimeStamped {

        /** date the correction is valid. */
        private final AbsoluteDate date;

        /** x component of the pole correction. */
        private final double dx;

        /** y component of the pole correction. */
        private final double dy;

        /** time component of the correction. */
        private final double dt;

        /** Create a new correction with the given data.
         * @param date date the correction is valid
         * @param dx x component of the pole correction
         * @param dy y component of the pole correction
         * @param dt time component of the correction
         */
        public CorrectionData(final AbsoluteDate date, final double dx, final double dy, final double dt) {
            this.date = date;
            this.dx   = dx;
            this.dy   = dy;
            this.dt   = dt;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getDate() {
            return date;
        }
    }

    /** Generates {@link Correction}s for a {@link TimeStampedCache}.
     */
    private static class Generator implements TimeStampedGenerator<CorrectionData> {

        /**
         * {@inheritDoc}
         * <p>
         * <b>Note:</b> this {@link Generator} generates the minimum points necessary to
         * cover the range (existing, date].
         */
        public List<CorrectionData> generate(final CorrectionData existing, final AbsoluteDate date)
            throws TimeStampedCacheException {

            // date in days
            double tStart;
            double tEnd;

            if (existing == null) {
                // set tStart and tEnd so that n points are generated
                final int nM12 = (INTERPOLATION_POINTS - 1) / 2;
                // days to subtract from tStart
                final double extraBefore = STEP_SIZE * nM12;
                // days to add to tEnd
                final double extraAfter = STEP_SIZE * (INTERPOLATION_POINTS - nM12);
                tStart = toDayQuantum(toDay(date)) - extraBefore;
                tEnd   = toDayQuantum(toDay(date) + extraAfter);
            } else if (existing.getDate().compareTo(date) > 0) {
                // existing is after date
                tStart = toDayQuantum(toDay(date));
                tEnd   = toDayQuantum(toDay(existing.getDate()));
            } else {
                // existing is before or same as date
                tStart = toDayQuantum(toDay(existing.getDate())) + STEP_SIZE;
                tEnd   = toDayQuantum(toDay(date)) + STEP_SIZE;
            }

            // n is number of points to generate. (tEnd - tStart) / STEP_SIZE should
            // already be *very* close to an integer
            final int n = (int) FastMath.round((tEnd - tStart) / STEP_SIZE);

            // list of generated points
            final List<CorrectionData> generated = new ArrayList<CorrectionData>(n);

            // compute new reference points in [tStart, tEnd)
            for (int i = 0; i < n; ++i) {
                generated.add(computeCorrections(tStart + i * STEP_SIZE));
            }

            return generated;
        }
    }
}
