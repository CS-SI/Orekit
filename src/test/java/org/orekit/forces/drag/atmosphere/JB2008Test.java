/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.forces.drag.atmosphere;


import java.text.ParseException;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

public class JB2008Test {

    private static final TimeScale TT = TimeScalesFactory.getTT();

    @Test
    public void testLegacy() throws OrekitException {
        final boolean print = false;
        // Build the model
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        final JB2008 atm = new JB2008(null, sun, earth);

        // Reference input data
        final double[] D1950  = {20035.00454861111, 20035.50362, 20036.00468, 20036.50375,
                                 20037.00000, 20037.50556, 20038.00088, 20038.50644,
                                 20039.00543, 20039.50450, 20040.00000, 20040.50556};
        final double[] SUNRA  = {3.8826, 3.8914, 3.9001, 3.9089, 3.9176, 3.9265,
                                 3.9352, 3.9441, 3.9529, 3.9618, 3.9706, 3.9795};
        final double[] SUNDEC = {-0.2847, -0.2873, -0.2898, -0.2923, -0.2948, -0.2973,
                                 -0.2998, -0.3022, -0.3046, -0.3070, -0.3094, -0.3117};
        final double[] SATLON = {73.46, 218.68, 34.55, 46.25, 216.56, 32.00,
                                 38.83, 213.67, 29.37, 38.12, 211.78, 26.64};
        final double[] SATLAT = {-85.24, -18.65, 37.71, 74.36,  -8.85, -39.64,
                                 -51.93, -21.25, 46.43, 65.97, -21.31, -51.87};
        final double[] SATALT = {398.91, 376.75, 373.45, 380.61, 374.03, 385.05,
                                 389.83, 376.98, 374.56, 378.97, 377.76, 390.09};
        final double[] F10    = {128.80, 128.80, 129.60, 129.60, 124.10, 124.10,
                                 140.90, 140.90, 104.60, 104.60,  94.90,  94.90};
        final double[] F10B   = {105.60, 105.60, 105.60, 105.60, 105.60, 105.60,
                                 105.60, 105.60, 105.70, 105.70, 105.90, 105.90};
        final double[] S10    = {103.50, 103.50, 110.20, 110.20, 109.40, 109.40,
                                 108.60, 108.60, 107.40, 107.40, 110.90, 110.90};
        final double[] S10B   = {103.80, 103.80, 103.80, 103.80, 103.80, 103.80,
                                 103.70, 103.70, 103.70, 103.70, 103.70, 103.70};
        final double[] XM10   = {110.90, 110.90, 115.60, 115.60, 110.00, 110.00,
                                 110.00, 110.00, 106.90, 106.90, 102.20, 102.20};
        final double[] XM10B  = {106.90, 106.90, 106.90, 106.90, 106.90, 106.90,
                                 107.00, 107.00, 107.10, 107.10, 107.10, 107.10};
        final double[] Y10    = {127.90, 127.90, 125.90, 125.90, 127.70, 127.70,
                                 125.60, 125.60, 126.60, 126.60, 126.80, 126.80};
        final double[] Y10B   = {112.90, 112.90, 112.90, 112.90, 113.00, 113.00,
                                 113.20, 113.20, 113.20, 113.20, 113.30, 113.30};
        final double[] DSTDTC = {  3.,  80., 240., 307., 132.,  40.,
                                 327., 327., 118.,  25.,  85., 251.};

        // Loop over cases
        for (int i = 0; i < 12; i++) {
            final double rho = atm.getDensity(MJD(D1950[i]), SUNRA[i], SUNDEC[i],
                                              RAP(D1950[i], SATLON[i]),
                                              FastMath.toRadians(SATLAT[i]),
                                              SATALT[i] * 1000.,
                                              F10[i], F10B[i], S10[i], S10B[i],
                                              XM10[i], XM10B[i], Y10[i], Y10B[i], DSTDTC[i]);
            checkLegacy(i, rho, atm.getExosphericTemp(), atm.getLocalTemp(), print);
        }

    }


    @Test
    public void testAltitude() throws OrekitException {
        final boolean print = false;
        // Build the iput params provider
        final InputParams ip = new InputParams();
        // Get Sun
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // Get Earth body shape
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        earth.setAngularThreshold(1e-10);
        // Build the model
        final JB2008 atm = new JB2008(ip, sun, earth);

        // Reference locations as {lat, lon, alt}
        final double[][] loc = {{-85.24,  73.46,   91.0e+3},
                                {-18.65, 218.68,  110.0e+3},
                                {-68.05, 145.28,  122.0e+3},
                                { 37.71,  34.55,  150.0e+3},
                                { 74.36,  46.25,  220.0e+3},
                                { -8.85, 216.56,  270.0e+3},
                                {-39.64,  32.00,  400.0e+3},
                                {-51.93,  38.83,  550.0e+3},
                                {-21.25, 213.67,  700.0e+3},
                                { 46.43,  29.37,  900.0e+3},
                                { 65.97,  38.12, 1200.0e+3},
                                {-21.31, 211.78, 1700.0e+3},
                                {-51.87,  26.64, 2300.0e+3}};

        // Loop over cases
        for (int i = 0; i < 13; i++) {
            // Build the point
            final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(loc[i][0]),
                                                          FastMath.toRadians(loc[i][1]),
                                                          loc[i][2]);
            // Run
            final double rho = atm.getDensity(InputParams.TC[i], earth.transform(point), atm.getFrame());

            // Check
            checkAltitude(i, rho, atm.getExosphericTemp(), atm.getLocalTemp(), print);
        }
   }

    @Test
    public void testException() throws OrekitException, ParseException {

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);

        final JB2008 atm = new JB2008(new InputParams(), CelestialBodyFactory.getSun(), earth);

        // alt = 89.999km
        try {
            atm.getDensity(0., 0., 0., 0., 0., 89999.0, 0., 0., 0., 0., 0., 0., 0., 0., 0.);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, oe.getSpecifier());
            Assert.assertEquals(89999.0, (Double) oe.getParts()[0], 1.0e-15);
            Assert.assertEquals(90000.0, (Double) oe.getParts()[1], 1.0e-15);
        }

    }

    /** Convert duration from fifties epoch to mjd epoch.
     * @param d1950 duration from fifties epoch
     * @return duration from mjd epoch
     */
    private double MJD(final double d1950) {
        return d1950 + 33281.0;
    }

    /** Convert longitude of position to right ascension of position.
     * @param d1950 duration from fifties epoch
     * @param satLon longitude of position (°)
     * @return right ascension of position (rad)
     */
    private double RAP(final double d1950, final double satLon) {
        double theta;
        final double nbday = FastMath.floor(d1950);
        if (nbday < 7305.) {
            theta = 1.7294446614 + 1.72027915246e-2 * nbday + 6.3003880926 * (d1950 - nbday);
        } else {
            final double ts70 = d1950 - 7305.;
            final double ids70 = FastMath.floor(ts70);
            final double tfrac = ts70 - ids70;
            theta = 1.73213438565 + 1.720279169407e-2 * ids70 +
                    (1.720279169407e-2 + MathUtils.TWO_PI) * tfrac +
                    5.0755141943e-15 * ts70 * ts70;
        }
        theta = MathUtils.normalizeAngle(theta, FastMath.PI);

        return MathUtils.normalizeAngle(theta + FastMath.toRadians(satLon), 0.);
    }

    /** Check legacy results.
     * @param id legacy test number
     * @param rho computed density
     * @param tInf computed exospheric temperature
     * @param tAlt computed local temperature
     * @param print if true print else check
     */
    private void checkLegacy(final int id, final double rho, final double tInf, final double tAlt, final boolean print) {
        final double[] rhoRef  = {0.18730056e-11, 0.25650339e-11, 0.57428913e-11,
                                  0.83266893e-11, 0.82238726e-11, 0.48686457e-11,
                                  0.67210914e-11, 0.74215571e-11, 0.31821075e-11,
                                  0.29553578e-11, 0.64122627e-11, 0.79559727e-11};
        final double[] tInfRef = { 867.4,  833.7, 1002.1, 1159.4,
                                  1115.7, 1015.3, 1140.6, 1098.5,
                                   867.9,  876.6, 1062.3, 1204.7};
        final double[] tAltRef = { 857.5,  831.1,  995.0, 1148.3,
                                  1105.7, 1008.8, 1128.9, 1089.9,
                                   864.7,  868.3, 1050.7, 1191.3};
        final double dRho = 2.e-5;
        final double dTmp = 1.e-4;
        final int nb = id + 1;
        if (print) {
            System.out.printf("Case #%d\n", nb);
            System.out.printf("Rho:  %12.5e  %12.5e\n", rhoRef[id], rho);
            System.out.printf("Tinf: %6.1f  %6.1f\n", tInfRef[id], tInf);
            System.out.printf("Talt: %6.1f  %6.1f\n\n", tAltRef[id], tAlt);
        } else {
            Assert.assertEquals(rhoRef[id],  rho,  rhoRef[id]  * dRho);
            Assert.assertEquals(tInfRef[id], tInf, tInfRef[id] * dTmp);
            Assert.assertEquals(tAltRef[id], tAlt, tAltRef[id] * dTmp);
        }

    }

    /** Check altiude results.
     * @param id test number
     * @param rho computed density
     * @param tInf computed exospheric temperature
     * @param tAlt computed local temperature
     * @param print if true print else check
     */
    private void checkAltitude(final int id, final double rho, final double tInf, final double tAlt, final boolean print) {
        final double[] rhoRef  = {0.27945654e-05, 0.94115202e-07, 0.15025977e-07, 0.21128330e-08,
                                  0.15227435e-09, 0.54609767e-10, 0.45899746e-11, 0.14922800e-12,
                                  0.17392987e-13, 0.35250121e-14, 0.13482414e-14, 0.77684879e-15,
                                  0.19900569e-15};
        final double[] tInfRef = { 955.24,  825.33,  941.87,  860.71,  948.95, 1104.19,
                                  1094.84,  913.21,  913.36,  902.48,  961.74, 1130.35, 1079.56};
        final double[] tAltRef = { 183.07,  245.51,  376.87,  639.08,  877.73, 1050.80,
                                  1085.20,  908.07,  908.41,  902.35,  961.69, 1130.33, 1079.55};
        final double dRho = 3.e-4;
        final double dTmp = 2.e-4;
        final int nb = id + 1;
        if (print) {
          System.out.printf("Case #%d\n", nb);
          System.out.printf("Rho:  %12.5e  %12.5e\n", rhoRef[id], rho);
          System.out.printf("Tinf: %6.1f  %6.1f\n", tInfRef[id], tInf);
          System.out.printf("Talt: %6.1f  %6.1f\n\n", tAltRef[id], tAlt);
        } else {
            Assert.assertEquals(rhoRef[id],  rho,  rhoRef[id]  * dRho);
            Assert.assertEquals(tInfRef[id], tInf, tInfRef[id] * dTmp);
            Assert.assertEquals(tAltRef[id], tAlt, tAltRef[id] * dTmp);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private static class InputParams implements JB2008InputParameters {

        /** Serializable UID. */
        private static final long serialVersionUID = 5091441542522297257L;

        /** Dates. */
        public static final AbsoluteDate[] TC = new AbsoluteDate[] {
            new AbsoluteDate(new DateComponents(2003, 312), new TimeComponents( 0,  6, 33.0), TT),
            new AbsoluteDate(new DateComponents(2003, 312), new TimeComponents(12,  5, 13.0), TT),
            new AbsoluteDate(new DateComponents(2003, 312), new TimeComponents(18, 45,  3.0), TT),
            new AbsoluteDate(new DateComponents(2003, 313), new TimeComponents( 0,  6, 44.0), TT),
            new AbsoluteDate(new DateComponents(2003, 313), new TimeComponents(12,  5, 24.0), TT),
            new AbsoluteDate(new DateComponents(2003, 314), new TimeComponents( 0,  0,  0.0), TT),
            new AbsoluteDate(new DateComponents(2003, 314), new TimeComponents(12,  8,  0.0), TT),
            new AbsoluteDate(new DateComponents(2003, 315), new TimeComponents( 0,  1, 16.0), TT),
            new AbsoluteDate(new DateComponents(2003, 315), new TimeComponents(12,  9, 16.0), TT),
            new AbsoluteDate(new DateComponents(2003, 316), new TimeComponents( 0,  7, 49.0), TT),
            new AbsoluteDate(new DateComponents(2003, 316), new TimeComponents(12,  6, 29.0), TT),
            new AbsoluteDate(new DateComponents(2003, 317), new TimeComponents( 0,  0,  0.0), TT),
            new AbsoluteDate(new DateComponents(2003, 317), new TimeComponents(12,  8,  0.0), TT)
        };

        /** F10 data. */
        private static final double[] F10 = new double[] {
            91.00, 91.00, 91.00, 92.70, 92.70, 93.00,
            93.00, 94.60, 94.60, 95.60, 95.60, 98.70, 98.70
        };

        /** F10B data. */
        private static final double[] F10B = new double[] {
            137.10, 137.10, 137.10, 136.90, 136.90, 136.80,
            136.80, 136.70, 136.70, 136.70, 136.70, 136.90, 136.90
        };

        /** S10 data. */
        private static final double[] S10 = new double[] {
            108.80, 108.80, 108.80, 104.20, 104.20, 102.60,
            102.60, 100.30, 100.30,  99.50,  99.50, 101.20, 101.20
        };

        /** S10B data. */
        private static final double[] S10B = new double[] {
            123.80, 123.80, 123.80, 123.70, 123.70, 123.60,
            123.60, 123.50, 123.50, 123.50, 123.50, 123.60, 123.60
        };

        /** XM10 data. */
        private static final double[] XM10 = new double[] {
            116.70, 116.70, 116.70, 109.60, 109.60, 100.20,
            100.20,  97.00,  97.00,  95.40,  95.40,  95.40,  95.40
        };

        /** XM10B data. */
        private static final double[] XM10B = new double[] {
            128.50, 128.50, 128.50, 128.00, 128.00, 127.70,
            127.70, 127.60, 127.60, 127.60, 127.60, 127.70, 127.70
        };

        /** Y10 data. */
        private static final double[] Y10 = new double[] {
            168.00, 168.00, 168.00, 147.90, 147.90, 131.60,
            131.60, 122.60, 122.60, 114.30, 114.30, 112.70, 112.70
        };

        /** Y10B data. */
        private static final double[] Y10B = new double[] {
            138.60, 138.60, 138.60, 138.40, 138.40, 138.10,
            138.10, 137.90, 137.90, 137.90, 137.90, 137.80, 137.80
        };

        /** DSTDTC data. */
        private static final double[] DSTDTC = new double[] {
             43.00,  30.00,  67.00,  90.00,  87.00, 115.00,
            114.00, 106.00, 148.00, 148.00, 105.00, 146.00, 119.00
        };

        /** Constructor. */
        public InputParams() {
        }

        @Override
        public AbsoluteDate getMinDate() {
            return TC[0];
        }

        @Override
        public AbsoluteDate getMaxDate() {
            return TC[TC.length - 1];
        }

        @Override
        public double getF10(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return F10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getF10B(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return F10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getS10(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return S10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getS10B(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return S10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getXM10(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return XM10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getXM10B(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return XM10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getY10(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return Y10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getY10B(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return Y10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getDSTDTC(AbsoluteDate date)
            throws OrekitException {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return DSTDTC[i];
                }
            }
            return Double.NaN;
        }

    }

}
