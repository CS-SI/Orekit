/* Copyright 2002-2025 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.models.earth.atmosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.data.JB2008SpaceEnvironmentData;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.text.ParseException;

class JB2008Test {

    private static final TimeScale TT = TimeScalesFactory.getTT();

    @Test
    void testLegacy() {
        final boolean print = false;
        // Build the model
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        final JB2008 atm = new JB2008(new LegacyInputParams(), sun, earth);

        // Loop over cases
        for (int i = 0; i < 12; i++) {
            final double rho = atm.computeDensity(LegacyInputParams.TC[i], LegacyInputParams.SUNRA[i], LegacyInputParams.SUNDEC[i],
                                                  RAP(LegacyInputParams.TC[i], LegacyInputParams.SATLON[i]),
                                                  FastMath.toRadians(LegacyInputParams.SATLAT[i]),
                                                  LegacyInputParams.SATALT[i] * 1000.);
            checkLegacy(i, rho, print);
        }

    }

    @Test
    void testDensityWithLocalSolarActivityData() {
        // First case of "testAltitude"
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final JB2008 model = new JB2008(null, sun, earth);
        double referenceDensity = 0.27945654e-05;
        double computedDensity = model.getDensity(52951.003805740744, 3.046653643566772, -0.285987757544287,
                                                  1.28211886851503, -1.4877186543999,
                                                  91.0e+3, 91.00, 137.10,
                                                  108.80, 123.80, 116.70, 128.50,
                                                  168.00, 138.60, 43.);
        Assertions.assertEquals(referenceDensity, computedDensity, referenceDensity * 2.e-5);
    }

    @Test
    void testDensityWithLocalSolarActivityDataField() {
        doTestDensityWithLocalSolarActivityData(Binary64Field.getInstance());
    }

    <T extends CalculusFieldElement<T>> void doTestDensityWithLocalSolarActivityData(Field<T> field) {
        // First case of "testAltitude"
        T zero = field.getZero();
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final JB2008 model = new JB2008(null, sun, earth);
        double referenceDensity = 0.27945654e-05;
        double computedDensity = model.getDensity(zero.add(52951.003805740744), zero.add(3.046653643566772), zero.add(-0.285987757544287),
                                                  zero.add(1.28211886851503), zero.add(-1.4877186543999),
                                                  zero.add(91.0e+3), 91.00, 137.10,
                                                  108.80, 123.80, 116.70, 128.50,
                                                  168.00, 138.60, 43.).getReal();
        Assertions.assertEquals(referenceDensity, computedDensity, referenceDensity * 2.e-5);
    }

    @Test
    void testAltitude() {
        final boolean print = false;
        // Build the iput params provider
        final InputParams ip = new InputParams();
        // Get Sun
        final CelestialBody sun = CelestialBodyFactory.getSun();
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
            checkAltitude(i, rho, print);
        }
   }

    @Test
    void testException() throws ParseException {

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);

        final JB2008 atm = new JB2008(new InputParams(), CelestialBodyFactory.getSun(), earth);

        // alt = 89.999km
        try {
            atm.computeDensity(AbsoluteDate.ARBITRARY_EPOCH, 0., 0., 0., 0., 89999.0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, oe.getSpecifier());
            Assertions.assertEquals(89999.0, (Double) oe.getParts()[0], 1.0e-15);
            Assertions.assertEquals(90000.0, (Double) oe.getParts()[1], 1.0e-15);
        }

    }

    @Test
    void testDensityField() {

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);

        final JB2008 atm = new JB2008(new InputParams(), CelestialBodyFactory.getSun(), earth);

        final AbsoluteDate date = InputParams.TC[4];

        for (double alt = 100; alt < 1000; alt += 50) {
            for (double lat = -1.2; lat < 1.2; lat += 0.4) {
                for (double lon = 0; lon < 6.28; lon += 0.8) {

                    final GeodeticPoint point = new GeodeticPoint(lat, lon, alt * 1000.);
                    final Vector3D pos = earth.transform(point);
                    Field<Binary64> field = Binary64Field.getInstance();

                    // Run
                    final double    rho = atm.getDensity(date, pos, itrf);
                    final Binary64 rho64 = atm.getDensity(new FieldAbsoluteDate<>(field, date),
                                                           new FieldVector3D<>(field, pos),
                                                           itrf);

                    Assertions.assertEquals(rho, rho64.getReal(), rho * 4.0e-10);

                }
            }
        }

    }

    @Test
    void testDensityGradient() {

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);

        final JB2008 atm = new JB2008(new InputParams(), CelestialBodyFactory.getSun(), earth);

        final AbsoluteDate date = InputParams.TC[6];

        // Build the position
        final double alt = 400.;
        final double lat =  60.;
        final double lon = -70.;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(lat),
                                                      FastMath.toRadians(lon),
                                                      alt * 1000.);
        final Vector3D pos = earth.transform(point);

        // Run
        DerivativeStructure zero = new DSFactory(1, 1).variable(0, 0.0);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 10.0);
        DerivativeStructure  rhoX = differentiator.
                        differentiate((double x) -> {
                            try {
                                return atm.getDensity(date, new Vector3D(1, pos, x, Vector3D.PLUS_I), itrf);
                            } catch (OrekitException oe) {
                                return Double.NaN;
                            }
                        }). value(zero);
        DerivativeStructure  rhoY = differentiator.
                        differentiate((double y) -> {
                            try {
                                return atm.getDensity(date, new Vector3D(1, pos, y, Vector3D.PLUS_J), itrf);
                            } catch (OrekitException oe) {
                                return Double.NaN;
                            }
                        }). value(zero);
        DerivativeStructure  rhoZ = differentiator.
                        differentiate((double z) -> {
                            try {
                                return atm.getDensity(date, new Vector3D(1, pos, z, Vector3D.PLUS_K), itrf);
                            } catch (OrekitException oe) {
                                return Double.NaN;
                            }
                        }). value(zero);

        DSFactory factory3 = new DSFactory(3, 1);
        Field<DerivativeStructure> field = factory3.getDerivativeField();
        final DerivativeStructure rhoDS = atm.getDensity(new FieldAbsoluteDate<>(field, date),
                                                         new FieldVector3D<>(factory3.variable(0, pos.getX()),
                                                                             factory3.variable(1, pos.getY()),
                                                                             factory3.variable(2, pos.getZ())),
                                                         itrf);

        Assertions.assertEquals(rhoX.getValue(), rhoDS.getReal(), rhoX.getValue() * 2.0e-14);
        Assertions.assertEquals(rhoY.getValue(), rhoDS.getReal(), rhoY.getValue() * 2.0e-14);
        Assertions.assertEquals(rhoZ.getValue(), rhoDS.getReal(), rhoZ.getValue() * 2.0e-14);
        Assertions.assertEquals(rhoX.getPartialDerivative(1),
                            rhoDS.getPartialDerivative(1, 0, 0),
                            FastMath.abs(6.0e-10 * rhoX.getPartialDerivative(1)));
        Assertions.assertEquals(rhoY.getPartialDerivative(1),
                            rhoDS.getPartialDerivative(0, 1, 0),
                            FastMath.abs(6.0e-10 * rhoY.getPartialDerivative(1)));
        Assertions.assertEquals(rhoZ.getPartialDerivative(1),
                            rhoDS.getPartialDerivative(0, 0, 1),
                            FastMath.abs(6.0e-10 * rhoY.getPartialDerivative(1)));

    }

    @Test
    void testComparisonWithReference() {

        // The objective of this test is to compare Orekit results with
        // the reference JB2008 Code Files provided by Space Environment
        // (i.e., JB2008.for and JB08DRVY2K.for)

        // Earth
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);

        // Input
        final int    year  = 2004;
        final int    month = 1;
        final int    day   = 2;
        final int    hour  = 12;
        final int    min   = 0;
        final double sec   = 0.0;
        final double lat   = FastMath.toRadians(45.0);
        final double lon   = FastMath.toRadians(45.0);
        final double alt   = 250.0e3;

        // Initialize JB2008 model
        final JB2008SpaceEnvironmentData JBData = new JB2008SpaceEnvironmentData("SOLFSMY_trunc.txt", "DTCFILE_trunc.TXT");
        final JB2008 atm = new JB2008(JBData, CelestialBodyFactory.getSun(), earth);

        // Compute density
        final GeodeticPoint point = new GeodeticPoint(lat, lon, alt);
        final Vector3D pos = earth.transform(point);
        final AbsoluteDate date = new AbsoluteDate(year, month, day, hour, min, sec, TimeScalesFactory.getUTC());
        final double density = atm.getDensity(date, pos, itrf);

        // Verify
        final double ref = 6.6862e-11;
        Assertions.assertEquals(ref, density, 1.0e-15);

    }

    /** Convert longitude of position to right ascension of position.
     * @param d1950 duration from fifties epoch
     * @param satLon longitude of position (°)
     * @return right ascension of position (rad)
     */
    private double RAP(final AbsoluteDate date, final double satLon) {
        double d1950 = date.getMJD() - 33281.;
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
     * @param print if true print else check
     */
    private void checkLegacy(final int id, final double rho, final boolean print) {
        final double[] rhoRef  = {0.18730056e-11, 0.25650339e-11, 0.57428913e-11,
                                  0.83266893e-11, 0.82238726e-11, 0.48686457e-11,
                                  0.67210914e-11, 0.74215571e-11, 0.31821075e-11,
                                  0.29553578e-11, 0.64122627e-11, 0.79559727e-11};
        final double dRho = 2.e-5;
        final int nb = id + 1;
        if (print) {
            System.out.printf("Case #%d\n", nb);
            System.out.printf("Rho:  %12.5e  %12.5e\n", rhoRef[id], rho);
        } else {
            Assertions.assertEquals(rhoRef[id],  rho,  rhoRef[id]  * dRho);
        }

    }

    /** Check altiude results.
     * @param id test number
     * @param rho computed density
     * @param print if true print else check
     */
    private void checkAltitude(final int id, final double rho, final boolean print) {
        final double[] rhoRef  = {0.27945654e-05, 0.94115202e-07, 0.15025977e-07, 0.21128330e-08,
                                  0.15227435e-09, 0.54609767e-10, 0.45899746e-11, 0.14922800e-12,
                                  0.17392987e-13, 0.35250121e-14, 0.13482414e-14, 0.77684879e-15,
                                  0.19900569e-15};
        final double dRho = 3.e-4;
        final int nb = id + 1;
        if (print) {
          System.out.printf("Case #%d\n", nb);
          System.out.printf("Rho:  %12.5e  %12.5e\n", rhoRef[id], rho);
        } else {
            Assertions.assertEquals(rhoRef[id],  rho,  rhoRef[id]  * dRho);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
    }

    private static class LegacyInputParams implements JB2008InputParameters {

        private static final AbsoluteDate[] TC  = {AbsoluteDate.createMJDDate(MJD(20035), 0.00454861111 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20035), 0.50362 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20036), 0.00468 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20036), 0.50375 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20037), 0. * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20037), 0.50556 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20038), 0.00088 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20038), 0.50644 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20039), 0.00543 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20039), 0.50450* Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20040), 0. * Constants.JULIAN_DAY, TimeScalesFactory.getUTC()),
                                                   AbsoluteDate.createMJDDate(MJD(20040), 0.50556 * Constants.JULIAN_DAY, TimeScalesFactory.getUTC())};

        private static final double[] SUNRA  = {3.8826, 3.8914, 3.9001, 3.9089, 3.9176, 3.9265,
                                                3.9352, 3.9441, 3.9529, 3.9618, 3.9706, 3.9795};

        private static final double[] SUNDEC = {-0.2847, -0.2873, -0.2898, -0.2923, -0.2948, -0.2973,
                                                -0.2998, -0.3022, -0.3046, -0.3070, -0.3094, -0.3117};

        private static final double[] SATLON = {73.46, 218.68, 34.55, 46.25, 216.56, 32.00,
                                                38.83, 213.67, 29.37, 38.12, 211.78, 26.64};

        private static final double[] SATLAT = {-85.24, -18.65, 37.71, 74.36,  -8.85, -39.64,
                                                -51.93, -21.25, 46.43, 65.97, -21.31, -51.87};

        private static final double[] SATALT = {398.91, 376.75, 373.45, 380.61, 374.03, 385.05,
                                                389.83, 376.98, 374.56, 378.97, 377.76, 390.09};

        private static final double[] F10    = {128.80, 128.80, 129.60, 129.60, 124.10, 124.10,
                                                140.90, 140.90, 104.60, 104.60,  94.90,  94.90};

        private static final double[] F10B   = {105.60, 105.60, 105.60, 105.60, 105.60, 105.60,
                                                105.60, 105.60, 105.70, 105.70, 105.90, 105.90};

        private static final double[] S10    = {103.50, 103.50, 110.20, 110.20, 109.40, 109.40,
                                                108.60, 108.60, 107.40, 107.40, 110.90, 110.90};

        private static final double[] S10B   = {103.80, 103.80, 103.80, 103.80, 103.80, 103.80,
                                                103.70, 103.70, 103.70, 103.70, 103.70, 103.70};

        private static final double[] XM10   = {110.90, 110.90, 115.60, 115.60, 110.00, 110.00,
                                                110.00, 110.00, 106.90, 106.90, 102.20, 102.20};

        private static final double[] XM10B  = {106.90, 106.90, 106.90, 106.90, 106.90, 106.90,
                                                107.00, 107.00, 107.10, 107.10, 107.10, 107.10};

        private static final double[] Y10    = {127.90, 127.90, 125.90, 125.90, 127.70, 127.70,
                                                125.60, 125.60, 126.60, 126.60, 126.80, 126.80};

        private static final double[] Y10B   = {112.90, 112.90, 112.90, 112.90, 113.00, 113.00,
                                                113.20, 113.20, 113.20, 113.20, 113.30, 113.30};

        private static final double[] DSTDTC = {  3.,  80., 240., 307., 132.,  40.,
                                                327., 327., 118.,  25.,  85., 251.};

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
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return F10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getF10B(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return F10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getS10(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return S10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getS10B(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return S10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getXM10(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return XM10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getXM10B(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return XM10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getY10(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return Y10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getY10B(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return Y10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getDSTDTC(AbsoluteDate date)
        {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return DSTDTC[i];
                }
            }
            return Double.NaN;
        }


        private static int MJD(final int d1950) {
            return d1950 + 33281;
        }

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
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return F10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getF10B(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return F10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getS10(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return S10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getS10B(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return S10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getXM10(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return XM10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getXM10B(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return XM10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getY10(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return Y10[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getY10B(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return Y10B[i];
                }
            }
            return Double.NaN;
        }

        @Override
        public double getDSTDTC(AbsoluteDate date)
            {
            for (int i = 0; i < TC.length; i++) {
                if (date.equals(TC[i])) {
                    return DSTDTC[i];
                }
            }
            return Double.NaN;
        }

    }

}
