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
    private static final long serialVersionUID = -5006388224733240851L;

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
        -1.94,  -1.25,  -6.64,  -1.51,  -8.02,
        -9.47, -50.20,  -1.80,  -9.54,   1.52,
       -49.45,-262.21,   1.70,   3.43,   1.94,
         1.37,   7.41,  20.62,   4.14,   3.94,
        -7.14,   1.37,-122.03,   1.02,   2.89,
        -7.30, 368.78,  50.01,  -1.08,   2.93,
         5.25,   3.95,  20.62,   4.09,   3.42,
         1.69,  11.29,   7.23,   1.51,   2.16,
         1.38,   1.80,   4.67,  16.01,  19.32,
         1.30,  -1.02,  -4.51, 120.99,   1.13,
        22.98,   1.06,  -1.90,  -2.18, -23.58,
        31.92,   1.92,  -4.66, -17.86,   4.47,
         1.97,  17.20, 294.00,  -2.46,  -1.02,
        79.96,  23.83,   2.59,   4.47,   1.95,
         1.17
    };

    /** PHASE parameter. */
    private static final double[] PHASE = {
        9.0899831, 8.8234208, 12.1189598, 1.4425700, 4.7381090,
        4.4715466, 7.7670857, -2.9093042, 0.3862349,-3.1758666,
        0.1196725, 3.4152116, 12.8946194, 5.5137686, 6.4441883,
       -4.2322016,-0.9366625,  8.5427453,11.8382843, 1.1618945,
        5.9693878,-1.2032249,  2.0923141,-1.7847596, 8.0679449,
        0.8953321, 4.1908712,  7.4864102,10.7819493, 0.3137975,
        6.2894282, 7.2198478, -0.1610030, 3.1345361, 2.8679737,
       -4.5128771, 4.9665307,  8.2620698,11.5576089, 0.6146566,
        3.9101957,20.6617051, 13.2808543,16.3098310, 8.9289802,
        5.0519065,15.8350306,  8.6624178,11.9579569, 8.0808832,
        4.5771061, 0.7000324, 14.9869335,11.4831564, 4.3105437,
        7.6060827, 3.7290090, 10.6350594, 3.2542086,12.7336164,
       16.0291555,10.1602590,  6.2831853, 2.4061116, 5.0862033,
        8.3817423,11.6772814, 14.9728205, 4.0298682, 7.3254073,
        9.1574019
    };

    /** FREQUENCY parameter. */
    private static final double[] FREQUENCY = {
        5.18688050,  5.38346657,  5.38439079,  5.41398343,  5.41490765,
        5.61149372,  5.61241794,  5.64201057,  5.64293479,  5.83859664,
        5.83952086,  5.84044508,  5.84433381,  5.87485066,  6.03795537,
        6.06754801,  6.06847223,  6.07236095,  6.07328517,  6.10287781,
        6.24878055,  6.26505830,  6.26598252,  6.28318449,  6.28318613,
        6.29946388,  6.30038810,  6.30131232,  6.30223654,  6.31759007,
        6.33479368,  6.49789839,  6.52841524,  6.52933946,  6.72592553,
        6.75644239,  6.76033111,  6.76125533,  6.76217955,  6.98835826,
        6.98928248, 11.45675174, 11.48726860, 11.68477889, 11.71529575,
       11.73249771, 11.89560406, 11.91188181, 11.91280603, 11.93000800,
       11.94332289, 11.96052486, 12.11031632, 12.12363121, 12.13990896,
       12.14083318, 12.15803515, 12.33834347, 12.36886033, 12.37274905,
       12.37367327, 12.54916865, 12.56637061, 12.58357258, 12.59985198,
       12.60077620, 12.60170041, 12.60262463, 12.82880334, 12.82972756,
       13.06071921
    };

    /** Orthotide weight factors. */
    private static final double[][] SP = {
        { 0.0298, 0.0200 },
        { 0.1408, 0.0905 },
        { 0.0805, 0.0638 },
        { 0.6002, 0.3476 },
        { 0.3025, 0.1645 },
        { 0.1517, 0.0923 }
    };

    /** Orthoweights for X polar motion. */
    private static final double[] ORTHOWX = {
         -6.77832 * MICRO_ARC_SECONDS_TO_RADIANS,
        -14.86323 * MICRO_ARC_SECONDS_TO_RADIANS,
          0.47884 * MICRO_ARC_SECONDS_TO_RADIANS,
         -1.45303 * MICRO_ARC_SECONDS_TO_RADIANS,
          0.16406 * MICRO_ARC_SECONDS_TO_RADIANS,
          0.42030 * MICRO_ARC_SECONDS_TO_RADIANS,
          0.09398 * MICRO_ARC_SECONDS_TO_RADIANS,
         25.73054 * MICRO_ARC_SECONDS_TO_RADIANS,
         -4.77974 * MICRO_ARC_SECONDS_TO_RADIANS,
          0.28080 * MICRO_ARC_SECONDS_TO_RADIANS,
          1.94539 * MICRO_ARC_SECONDS_TO_RADIANS,
         -0.73089 * MICRO_ARC_SECONDS_TO_RADIANS
    };

    /** Orthoweights for Y polar motion. */
    private static final double[] ORTHOWY = {
        14.86283 * MICRO_ARC_SECONDS_TO_RADIANS,
        -6.77846 * MICRO_ARC_SECONDS_TO_RADIANS,
         1.45234 * MICRO_ARC_SECONDS_TO_RADIANS,
         0.47888 * MICRO_ARC_SECONDS_TO_RADIANS,
        -0.42056 * MICRO_ARC_SECONDS_TO_RADIANS,
         0.16469 * MICRO_ARC_SECONDS_TO_RADIANS,
        15.30276 * MICRO_ARC_SECONDS_TO_RADIANS,
        -4.30615 * MICRO_ARC_SECONDS_TO_RADIANS,
         0.07564 * MICRO_ARC_SECONDS_TO_RADIANS,
         2.28321 * MICRO_ARC_SECONDS_TO_RADIANS,
        -0.45717 * MICRO_ARC_SECONDS_TO_RADIANS,
        -1.62010 * MICRO_ARC_SECONDS_TO_RADIANS
    };

    /** Orthoweights for UT1. */
    private static double[] ORTHOWT = {
        -1.76335 *  MICRO_SECONDS_TO_SECONDS,
         1.03364 *  MICRO_SECONDS_TO_SECONDS,
        -0.27553 *  MICRO_SECONDS_TO_SECONDS,
         0.34569 *  MICRO_SECONDS_TO_SECONDS,
        -0.12343 *  MICRO_SECONDS_TO_SECONDS,
        -0.10146 *  MICRO_SECONDS_TO_SECONDS,
        -0.47119 *  MICRO_SECONDS_TO_SECONDS,
         1.28997 *  MICRO_SECONDS_TO_SECONDS,
        -0.19336 *  MICRO_SECONDS_TO_SECONDS,
         0.02724 *  MICRO_SECONDS_TO_SECONDS,
         0.08955 *  MICRO_SECONDS_TO_SECONDS,
         0.04726 *  MICRO_SECONDS_TO_SECONDS
    };
    
    /** Private constructor for the singleton.
     */
    private TidalCorrection() {
    }

    /** Compute the partials of the tidal variations to the orthoweights.
     * @param date date at which the correction is desired
     * @return partials of the tidal variations
     */
    private double[] CNMTX(final AbsoluteDate date) {

        double[]   h   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
        double[][] anm = {{0.,0.,0.}, {0.,0.,0.}};
        double[][] bnm = {{0.,0.,0.}, {0.,0.,0.}};
        double[][] p   = {{0.,0.}, {0.,0.}, {0.,0.}};
        double[][] q   = {{0.,0.}, {0.,0.}, {0.,0.}};
        
        double dt60 = date.durationFrom(new AbsoluteDate(AbsoluteDate.MODIFIED_JULIAN_EPOCH, 37076.5 * 86400));
        dt60 /= 86400.;

        // compute the time dependent potential matrix
        for (int k = 0; k < 3; k++) {
            dt60 -= (k - 1) * 2.;
            for (int j = 0; j < MJ.length; j++) {
                int m  = MJ[j] - 1;
                int mm = MJ[j] % 2;
                double pinm = mm * TWO_PI / 4.;
                double alpha = PHASE[j] + FREQUENCY[j] * dt60 - pinm;
                alpha %= TWO_PI;
                anm[m][k] += HS[j] * Math.cos(alpha);
                bnm[m][k] -= HS[j] * Math.sin(alpha);
            }
        }

        // orthogonalize the response terms
        for (int m = 0; m < 2; m++) {
            double ap = anm[m][2] + anm[m][0];
            double am = anm[m][2] - anm[m][0];
            double bp = bnm[m][2] + bnm[m][0];
            double bm = bnm[m][2] - bnm[m][0];
            p[0][m]   = SP[0][m] * anm[m][1];
            p[1][m]   = SP[1][m] * anm[m][1] - SP[2][m] * ap;
            p[2][m]   = SP[3][m] * anm[m][1] - SP[4][m] * ap + SP[5][m] * bm;
            q[0][m]   = SP[0][m] * bnm[m][1];
            q[1][m]   = SP[1][m] * bnm[m][1] - SP[2][m] * bp;
            q[2][m]   = SP[3][m] * bnm[m][1] - SP[4][m] * bp - SP[5][m] * am;
            anm[m][0] = p[0][m];
            anm[m][1] = p[1][m];
            anm[m][2] = p[2][m];
            bnm[m][0] = q[0][m];
            bnm[m][1] = q[1][m];
            bnm[m][2] = q[2][m];
        }

        // fill partials vector
        int j = 0;
        for (int m = 0; m < 2; m++) {
            for (int k = 0; k < 3; k++) {
                h[j]   = anm[m][k];
                h[j+1] = bnm[m][k];
                j += 2;
            }
        }

        return h;

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
        
        // compute the partials of the tidal variations to the orthoweights
        double[] h = CNMTX(date);

        // compute UT1 change
        for (int j = 0; j < ORTHOWT.length; j++) {
            dUT1 += h[j] * ORTHOWT[j];
        }

//      System.out.println("dUT1 = " + (dUT1 / MICRO_SECONDS_TO_SECONDS) + " microsecondes");

        return dUT1;

    }

    /** Get the pole IERS Reference Pole correction.
     * @param date date at which the correction is desired
     * @return pole correction
     */
    protected PoleCorrection getPoleCorrection(final AbsoluteDate date) {

        double dX = 0;
        double dY = 0;
        
        // compute the partials of the tidal variations to the orthoweights
        double[] h = CNMTX(date);

        // compute X and Y change in pole motion
        for (int j = 0; j < ORTHOWX.length; j++) {
            dX += h[j] * ORTHOWX[j];
            dY += h[j] * ORTHOWY[j];
        }

//      System.out.println("dX = " + (dX / MICRO_ARC_SECONDS_TO_RADIANS) + " microarcsec ; " +
//      		             "dY = " + (dY / MICRO_ARC_SECONDS_TO_RADIANS) + " microarcsec");
        
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
