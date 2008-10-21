/* Copyright 2002-2008 CS Communication & Systèmes
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

import java.io.Serializable;

import org.orekit.time.AbsoluteDate;


/** Compute tidal correction to the pole motion.
 * <p>This class computes the diurnal and semidiurnal variations in the
 * Earth orientation. It is a java translation of the fortran subroutine
 * found at ftp://tai.bipm.org/iers/conv2003/chapter8/ortho_eop.f.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class TidalCorrection implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -8006605442311656729L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Angular units conversion factor. */
    private static final double MICRO_ARC_SECONDS_TO_RADIANS = TWO_PI / 1296000.e+6;

    /** Time units conversion factor. */
    private static final double MICRO_SECONDS_TO_SECONDS = 1.e-6;

    /** MJ parameter. */
    private static final int[] MJ = {
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 2,
        2, 2, 2, 2, 2,
        2, 2, 2, 2, 2,
        2, 2, 2, 2, 2,
        2, 2, 2, 2, 2,
        2, 2, 2, 2, 2,
        2
    };

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
        +09.0899831, +08.8234208, +12.1189598, +01.4425700, +04.7381090,
        +04.4715466, +07.7670857, -02.9093042, +00.3862349, -03.1758666,
        +00.1196725, +03.4152116, +12.8946194, +05.5137686, +06.4441883,
        -04.2322016, -00.9366625, +08.5427453, +11.8382843, +01.1618945,
        +05.9693878, -01.2032249, +02.0923141, -01.7847596, +08.0679449,
        +00.8953321, +04.1908712, +07.4864102, +10.7819493, +00.3137975,
        +06.2894282, +07.2198478, -00.1610030, +03.1345361, +02.8679737,
        -04.5128771, +04.9665307, +08.2620698, +11.5576089, +00.6146566,
        +03.9101957, +20.6617051, +13.2808543, +16.3098310, +08.9289802,
        +05.0519065, +15.8350306, +08.6624178, +11.9579569, +08.0808832,
        +04.5771061, +00.7000324, +14.9869335, +11.4831564, +04.3105437,
        +07.6060827, +03.7290090, +10.6350594, +03.2542086, +12.7336164,
        +16.0291555, +10.1602590, +06.2831853, +02.4061116, +05.0862033,
        +08.3817423, +11.6772814, +14.9728205, +04.0298682, +07.3254073,
        +09.1574019
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
    private static double[] ORTHOWT = {
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

    /** Vector of length 12 with partials of the tidal
     * variation with respect to the orthoweights. */
    private double[] partials;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Private constructor for the singleton.
     */
    private TidalCorrection() {
        partials = new double[12];
    }

    /** Compute the partials of the tidal variations to the orthoweights.
     * @param date date at which the partials are desired
     */
    private void cnmtx(final AbsoluteDate date) {

        final double[][] anm = new double[2][3];
        final double[][] bnm = new double[2][3];

        final double dt60 =
            date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / 86400.0 - 37076.5;

        // compute the time dependent potential matrix
        for (int k = 0; k < 3; k++) {
            final double d60 = dt60 - (k - 1) * 2.;
            for (int j = 0; j < MJ.length; j++) {
                final int m  = MJ[j] - 1;
                final int mm = MJ[j] % 2;
                final double pinm = mm * TWO_PI / 4.;
                final double alpha = PHASE[j] + FREQUENCY[j] * d60 - pinm;
                anm[m][k] += HS[j] * Math.cos(alpha);
                bnm[m][k] -= HS[j] * Math.sin(alpha);
            }
        }

        // orthogonalize the response terms ...
        final double anm00 = anm[0][0];
        final double anm01 = anm[0][1];
        final double anm02 = anm[0][2];
        final double anm10 = anm[1][0];
        final double anm11 = anm[1][1];
        final double anm12 = anm[1][2];
        final double bnm00 = bnm[0][0];
        final double bnm01 = bnm[0][1];
        final double bnm02 = bnm[0][2];
        final double bnm10 = bnm[1][0];
        final double bnm11 = bnm[1][1];
        final double bnm12 = bnm[1][2];
        final double ap0 = anm02 + anm00;
        final double am0 = anm02 - anm00;
        final double bp0 = bnm02 + bnm00;
        final double bm0 = bnm02 - bnm00;
        final double ap1 = anm12 + anm10;
        final double am1 = anm12 - anm10;
        final double bp1 = bnm12 + bnm10;
        final double bm1 = bnm12 - bnm10;

        // ... and fill partials vector
        partials[0]  = SP[0] * anm01;
        partials[1]  = SP[0] * bnm01;
        partials[2]  = SP[1] * anm01 - SP[2] * ap0;
        partials[3]  = SP[1] * bnm01 - SP[2] * bp0;
        partials[4]  = SP[3] * anm01 - SP[4] * ap0 + SP[5] * bm0;
        partials[5]  = SP[3] * bnm01 - SP[4] * bp0 - SP[5] * am0;
        partials[6]  = SP[6] * anm11;
        partials[7]  = SP[6] * bnm11;
        partials[8]  = SP[7] * anm11 - SP[8] * ap1;
        partials[9]  = SP[7] * bnm11 - SP[8] * bp1;
        partials[10] = SP[9] * anm11 - SP[10] * ap1 + SP[11] * bm1;
        partials[11] = SP[9] * bnm11 - SP[10] * bp1 - SP[11] * am1;

    }

    /** Get the unique instance of this class.
     * @return the unique instance
     */
    public static TidalCorrection getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** Get the dUT1 value.
     * @param date date at which the value is desired
     * @return dUT1 in seconds
     */
    protected double getDUT1(final AbsoluteDate date) {

        double dUT1 = 0;

        if ((cachedDate == null) || !(cachedDate == date)) {

            // compute the partials of the tidal variations to the orthoweights
            cnmtx(date);

            cachedDate = date;
        }

        // compute UT1 change
        for (int j = 0; j < ORTHOWT.length; j++) {
            dUT1 += partials[j] * ORTHOWT[j];
        }

        return dUT1;

    }

    /** Get the pole IERS Reference Pole correction.
     * @param date date at which the correction is desired
     * @return pole correction
     */
    protected PoleCorrection getPoleCorrection(final AbsoluteDate date) {

        double dX = 0;
        double dY = 0;

        if ((cachedDate == null) || !(cachedDate == date)) {

            // compute the partials of the tidal variations to the orthoweights
            cnmtx(date);

            cachedDate = date;
        }

        // compute X and Y change in pole motion
        for (int j = 0; j < ORTHOWX.length; j++) {
            dX += partials[j] * ORTHOWX[j];
            dY += partials[j] * ORTHOWY[j];
        }

        return new PoleCorrection(dX, dY);

    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class LazyHolder {

        /** Unique instance. */
        private static final TidalCorrection INSTANCE = new TidalCorrection();

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyHolder() {
        }

    }

}
