/* Copyright 2002-2023 CS GROUP
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
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class NRLMSISE00Test {

    @Test
    public void testLegacy() throws
                                    NoSuchMethodException, SecurityException, InstantiationException,
                                    IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        // Build the model
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING, itrf);
        NRLMSISE00 atm = new NRLMSISE00(null, null, earth);

        // Common data for all cases
        final int doy = 172;
        final double sec   = 29000.;
        final double alt   = 400.;
        final double lat   =  60.;
        final double lon   = -70.;
        final double hl    =  16.;
        final double f107a = 150.;
        final double f107  = 150.;
        double[] ap  = {4., 100., 100., 100., 100., 100., 100.};
        final boolean print = false;

        Method gtd7 = getOutputClass().getDeclaredMethod("gtd7", Double.TYPE);
        gtd7.setAccessible(true);

        // Case #1
        final Object out1 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out1, alt);
        checkLegacy(1, out1, print);

        // Case #2
        final int doy2 = 81;
        final Object out2 = createOutput(atm, doy2, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out2, alt);
        checkLegacy(2, out2, print);

        // Case #3
        final double sec3 = 75000.;
        final double alt3 = 1000.;
        final Object out3 = createOutput(atm, doy, sec3, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out3, alt3);
        checkLegacy(3, out3, print);

        // Case #4
        final double alt4 = 100.;
        final Object out4 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out4, alt4);
        checkLegacy(4, out4, print);

        // Case #5
        final double lat5 = 0.;
        final Object out5 = createOutput(atm, doy, sec, lat5, lon, hl, f107a, f107, ap);
        gtd7.invoke(out5, alt);
        checkLegacy(5, out5, print);

        // Case #6
        final double lon6 = 0.;
        final Object out6 = createOutput(atm, doy, sec, lat, lon6, hl, f107a, f107, ap);
        gtd7.invoke(out6, alt);
        checkLegacy(6, out6, print);

        // Case #7
        final double hl7 = 4.;
        final Object out7 = createOutput(atm, doy, sec, lat, lon, hl7, f107a, f107, ap);
        gtd7.invoke(out7, alt);
        checkLegacy(7, out7, print);

        // Case #8
        final double f107a8 = 70.;
        final Object out8 = createOutput(atm, doy, sec, lat, lon, hl, f107a8, f107, ap);
        gtd7.invoke(out8, alt);
        checkLegacy(8, out8, print);

        // Case #9
        final double f1079 = 180.;
        final Object out9 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f1079, ap);
        gtd7.invoke(out9, alt);
        checkLegacy(9, out9, print);

        // Case #10
        ap[0] = 40.;
        final Object out10 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out10, alt);
        checkLegacy(10, out10, print);
        ap[0] = 4.;

        // Case #11
        final double alt11 =  0.;
        final Object out11 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out11, alt11);
        checkLegacy(11, out11, print);

        // Case #12
        final double alt12 = 10.;
        final Object out12 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out12, alt12);
        checkLegacy(12, out12, print);

        // Case #13
        final double alt13 = 30.;
        final Object out13 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out13, alt13);
        checkLegacy(13, out13, print);

        // Case #14
        final double alt14 = 50.;
        final Object out14 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out14, alt14);
        checkLegacy(14, out14, print);

        // Case #15
        final double alt15 = 70.;
        final Object out15 = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out15, alt15);
        checkLegacy(15, out15, print);

        // Case #16
        NRLMSISE00 otherAtm = atm.withSwitch(9, -1);
        final Object out16 = createOutput(otherAtm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out16, alt);
        checkLegacy(16, out16, print);

        // Case #17
        final double alt17 = 100.;
        final Object out17 = createOutput(otherAtm, doy, sec, lat, lon, hl, f107a, f107, ap);
        gtd7.invoke(out17, alt17);
        checkLegacy(17, out17, print);

    }

    @Test
    public void testDensity() throws
                              InstantiationException, IllegalAccessException,
                              IllegalArgumentException, InvocationTargetException,
                              NoSuchMethodException, SecurityException {
        // Build the input params provider
        final InputParams ip = new InputParams();
        // Get Sun
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // Get Earth body shape
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        // Build the model
        final NRLMSISE00 atm = new NRLMSISE00(ip, sun, earth).withSwitch(9, -1);
        // Build the date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 172),
                                                   new TimeComponents(29000.),
                                                   TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true));
        // Build the position
        final double alt = 400.;
        final double lat =  60.;
        final double lon = -70.;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(lat),
                                                      FastMath.toRadians(lon),
                                                      alt * 1000.);
        final Vector3D pos = earth.transform(point);

        // Run
        try {
            atm.getDensity(date.shiftedBy(2 * Constants.JULIAN_YEAR), pos, itrf);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE, oe.getSpecifier());
        }

        final double rho = atm.getDensity(date, pos, itrf);
        final double lst = 29000. / 3600. - 70. / 15.;
        final double[] ap  = {4., 100., 100., 100., 100., 100., 100.};

        Class<?> outputClass = getOutputClass();
        Constructor<?> cons = outputClass.getDeclaredConstructor(NRLMSISE00.class,
                                                                 Integer.TYPE,
                                                                 Double.TYPE,
                                                                 Double.TYPE,
                                                                 Double.TYPE,
                                                                 Double.TYPE,
                                                                 Double.TYPE,
                                                                 Double.TYPE,
                                                                 double[].class);
        cons.setAccessible(true);
        Method gtd7d = outputClass.getDeclaredMethod("gtd7d", Double.TYPE);
        gtd7d.setAccessible(true);
        Method getDensity = outputClass.getDeclaredMethod("getDensity", Integer.TYPE);
        getDensity.setAccessible(true);

        final Object out = createOutput(atm, 172, 29000., 60., -70, lst, 150., 150., ap);
        gtd7d.invoke(out, 400.0);
        Assertions.assertEquals(rho, ((Double) getDensity.invoke(out, 5)).doubleValue(), rho * 1.e-3);

    }

    @Test
    public void testDensityField() {
        // Build the input params provider
        final InputParams ip = new InputParams();
        // Get Sun
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // Get Earth body shape
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        // Build the model
        final NRLMSISE00 atm = new NRLMSISE00(ip, sun, earth);
        // Build the date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 172),
                                                   new TimeComponents(29000.),
                                                   TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true));
        // Build the position
        final double alt = 400.;
        final double lat =  60.;
        final double lon = -70.;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(lat),
                                                      FastMath.toRadians(lon),
                                                      alt * 1000.);
        final Vector3D pos = earth.transform(point);
        Field<Binary64> field = Binary64Field.getInstance();

        // Run
        try {
            atm.getDensity(new FieldAbsoluteDate<>(field, date).shiftedBy(2 * Constants.JULIAN_YEAR),
                           new FieldVector3D<>(field.getOne(), pos),
                           itrf);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE, oe.getSpecifier());
        }

        final double    rho = atm.getDensity(date, pos, itrf);
        final Binary64 rho64 = atm.getDensity(new FieldAbsoluteDate<>(field, date),
                                               new FieldVector3D<>(field.getOne(), pos),
                                               itrf);

        Assertions.assertEquals(rho, rho64.getReal(), rho * 2.0e-13);

    }

    @Test
    public void testDensityGradient() {
        // Build the input params provider
        final InputParams ip = new InputParams();
        // Get Sun
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // Get Earth body shape
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        // Build the model
        final NRLMSISE00 atm = new NRLMSISE00(ip, sun, earth);
        // Build the date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 172),
                                                   new TimeComponents(29000.),
                                                   TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true));
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

        Assertions.assertEquals(rhoX.getValue(), rhoDS.getReal(), rhoX.getValue() * 2.0e-13);
        Assertions.assertEquals(rhoY.getValue(), rhoDS.getReal(), rhoY.getValue() * 2.0e-13);
        Assertions.assertEquals(rhoZ.getValue(), rhoDS.getReal(), rhoZ.getValue() * 2.0e-13);
        Assertions.assertEquals(rhoX.getPartialDerivative(1),
                            rhoDS.getPartialDerivative(1, 0, 0),
                            FastMath.abs(2.0e-10 * rhoX.getPartialDerivative(1)));
        Assertions.assertEquals(rhoY.getPartialDerivative(1),
                            rhoDS.getPartialDerivative(0, 1, 0),
                            FastMath.abs(2.0e-10 * rhoY.getPartialDerivative(1)));
        Assertions.assertEquals(rhoZ.getPartialDerivative(1),
                            rhoDS.getPartialDerivative(0, 0, 1),
                            FastMath.abs(2.0e-10 * rhoY.getPartialDerivative(1)));

    }

    @Test
    public void testWrongNumberLow() {
        try {
            new NRLMSISE00(null, null, null).withSwitch(0, 17);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
            Assertions.assertEquals( 0, oe.getParts()[0]);
            Assertions.assertEquals( 1, oe.getParts()[1]);
            Assertions.assertEquals(23, oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongNumberHigh() {
        try {
            new NRLMSISE00(null, null, null).withSwitch(24, 17);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
            Assertions.assertEquals(24, oe.getParts()[0]);
            Assertions.assertEquals( 1, oe.getParts()[1]);
            Assertions.assertEquals(23, oe.getParts()[2]);
        }
    }

    @Test
    public void testGlobe7SwitchesOn() {
        RandomGenerator random = new Well19937a(0xb9d06451353d23cbl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 1);
        }
        doTestDoubleMethod(atm, random, "globe7", 2.0e-14, 2.2e-16);
    }

    @Test
    public void testGlobe7SwitchesOff() {
        RandomGenerator random = new Well19937a(0x778b486a40464b8fl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 0);
        }
        doTestDoubleMethod(atm, random, "globe7", 1.0e-50, 1.0e-50);
    }

    @Test
    public void testGlobe7SwitchesRandom() {
        RandomGenerator random = new Well19937a(0xe20a69235cc9583dl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, random.nextInt(3) - 2);
        }
        doTestDoubleMethod(atm, random, "globe7", 3.0e-14, 4.0e-16);
    }

    @Test
    public void testGlob7sSwitchesOn() {
        RandomGenerator random = new Well19937a(0xc7c218fabec5e98cl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 1);
        }
        doTestDoubleMethod(atm, random, "glob7s", 4.0e-15, 9.0e-16);
    }

    @Test
    public void testGlob7sSwitchesOff() {
        RandomGenerator random = new Well19937a(0x141f7aa933299a83l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 0);
        }
        doTestDoubleMethod(atm, random, "glob7s", 1.0e-50, 1.0e-50);
    }

    @Test
    public void testGlob7sSwitchesRandom() {
        RandomGenerator random = new Well19937a(0x3671893ce741fc5cl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, random.nextInt(3) - 2);
        }
        doTestDoubleMethod(atm, random, "glob7s", 1.0e-50, 1.0e-50);
    }

    @Test
    public void testgts7SwitchesOn() {
        RandomGenerator random = new Well19937a(0xb6dcf73ed5e5d985l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 1);
        }
        doTestVoidMethod(atm, random, "gts7", 1.0e-50, 6.0e-14);
    }

    @Test
    public void testgts7SwitchesOff() {
        RandomGenerator random = new Well19937a(0x0c953641bea0f6d2l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 0);
        }
        doTestVoidMethod(atm, random, "gts7", 1.0e-50, 6.0e-14);
    }

    @Test
    public void testgts7SwitchesRandom() {
        RandomGenerator random = new Well19937a(0x7347cacb946cb93bl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, random.nextInt(3) - 2);
        }
        doTestVoidMethod(atm, random, "gts7", 1.0e-50, 6.0e-14);
    }

    @Test
    public void testgtd7SwitchesOn() {
        RandomGenerator random = new Well19937a(0x3439206bdd4dff5dl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 1);
        }
        doTestVoidMethod(atm, random, "gtd7", 5.0e-16, 3.0e-14);
    }

    @Test
    public void testgtd7SwitchesOff() {
        RandomGenerator random = new Well19937a(0x3dc1f824e1033d1bl);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 0);
        }
        doTestVoidMethod(atm, random, "gtd7", 1.0e-50, 3.0e-14);
    }

    @Test
    public void testgtd7SwitchesRandom() {
        RandomGenerator random = new Well19937a(0xa12175ef0b689b04l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, random.nextInt(3) - 2);
        }
        doTestVoidMethod(atm, random, "gtd7", 1.0e-50, 3.0e-14);
    }

    @Test
    public void testgtd7dSwitchesOn() {
        RandomGenerator random = new Well19937a(0x4bbb424422a1b909l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 1);
        }
        doTestVoidMethod(atm, random, "gtd7d", 3.0e-16, 3.0e-14);
    }

    @Test
    public void testgtd7dSwitchesOff() {
        RandomGenerator random = new Well19937a(0x7f6da37655e30103l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, 0);
        }
        doTestVoidMethod(atm, random, "gtd7d", 1.0e-50, 2.0e-14);
    }

    @Test
    public void testgtd7dSwitchesRandom() {
        RandomGenerator random = new Well19937a(0x4a75e29ddf23ccd7l);
        NRLMSISE00 atm = new NRLMSISE00(null, null, null);
        for (int i = 1; i <= 23; ++i) {
            atm = atm.withSwitch(i, random.nextInt(3) - 2);
        }
        doTestVoidMethod(atm, random, "gtd7d", 1.0e-50, 3.0e-14);
    }

    private void doTestDoubleMethod(NRLMSISE00 atm, RandomGenerator random, String methodName,
                                    double absTolerance, double relTolerance)
        {
        try {
            // Common data for all cases
            final int doy = 172;
            final double sec   = 29000.;
            final double lat   =  60.;
            final double lon   = -70.;
            final double hl    =  16.;
            final double f107a = 149.;
            final double f107  = 150.;
            double[] ap  = {4., 100., 100., 100., 100., 100., 100.};

            Method methodD = getOutputClass().getDeclaredMethod(methodName, double[].class);
            methodD.setAccessible(true);

            Method methodF = getFieldOutputClass().getDeclaredMethod(methodName, double[].class);
            methodF.setAccessible(true);

            double[] p = new double[150];

            Object output = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
            Object fieldOutput = createFieldOutput(Binary64Field.getInstance(),
                                                   atm, doy, sec, lat, lon, hl, f107a, f107, ap);
            double maxAbsoluteError = 0;
            double maxRelativeError = 0;
            for (int i = 0; i < 100; ++i) {
                for (int k = 0; k < p.length; ++k) {
                    p[k] = random.nextDouble();
                }
                double resDouble = ((Double) methodD.invoke(output, p)).doubleValue();
                double resField  = ((CalculusFieldElement<?>) methodF.invoke(fieldOutput, p)).getReal();
                maxAbsoluteError = FastMath.max(maxAbsoluteError, FastMath.abs(resDouble - resField));
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((resDouble - resField) / resDouble));
            }
            Assertions.assertEquals(0.0, maxAbsoluteError, absTolerance);
            if (maxAbsoluteError != 0.0) {
                Assertions.assertEquals(0.0, maxRelativeError, relTolerance);
            }

        } catch (NoSuchMethodException | SecurityException |
                        IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    private void doTestVoidMethod(NRLMSISE00 atm, RandomGenerator random, String methodName,
                                  double temperatureRelativeTolerance, double densityRelativeTolerance)
        {
        try {

            // Common data for all cases
            final int doy = 172;
            final double sec   = 29000.;
            final double lat   =  60.;
            final double lon   = -70.;
            final double hl    =  16.;
            final double f107a = 149.;
            final double f107  = 150.;
            double[] ap  = {4., 100., 100., 100., 100., 100., 100.};

            Method methodD = getOutputClass().getDeclaredMethod(methodName, Double.TYPE);
            methodD.setAccessible(true);

            Method methodF = getFieldOutputClass().getDeclaredMethod(methodName, CalculusFieldElement.class);
            methodF.setAccessible(true);

            Object output = createOutput(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
            Object fieldOutput = createFieldOutput(Binary64Field.getInstance(),
                                                   atm, doy, sec, lat, lon, hl, f107a, f107, ap);
            double maxTemperatureError = 0;
            double maxDensityError     = 0;
            for (int i = 0; i < 100; ++i) {
                double alt = 500.0 * random.nextDouble();
                methodD.invoke(output, alt);
                methodF.invoke(fieldOutput, new Binary64(alt));
                for (int index = 0; index < 2; ++index) {
                    double tD = getOutputTemperature(output, index);
                    double tF = getFieldOutputTemperature(fieldOutput, index);
                    maxTemperatureError = FastMath.max(maxTemperatureError, FastMath.abs((tD - tF) / tF));
                }
                for (int index = 0; index < 9; ++index) {
                    double dD = getOutputDensity(output, index);
                    double dF = getFieldOutputDensity(fieldOutput, index);
                    if (Double.isNaN(dD)) {
                        // when switches are off, some altitudes generate NaNs
                        // for example when switch 15 is 0, DM28 is not set and remains equals to 0
                        // so a division later on generate NaNs
                        Assertions.assertTrue(Double.isNaN(dF));
                    } else if (dD == 0) {
                        // some densities are forced to zero depending on altitude
                        Assertions.assertEquals(dD, dF, Precision.SAFE_MIN);
                    } else {
                        maxDensityError = FastMath.max(maxDensityError, FastMath.abs((dD - dF) / dD));
                    }
                }
            }
            Assertions.assertEquals(0.0, maxTemperatureError, temperatureRelativeTolerance);
            Assertions.assertEquals(0.0, maxDensityError,     densityRelativeTolerance);

        } catch (NoSuchMethodException | SecurityException |
                        IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    private Class<?> getOutputClass() {
        for (final Class<?> c : NRLMSISE00.class.getDeclaredClasses()) {
            if (c.getName().endsWith("$Output")) {
                return c;
            }
        }
        return null;
    }

    private Object createOutput(final NRLMSISE00 atm,
                                final int doy, final double sec,
                                final double lat, final double lon, final double hl,
                                final double f107a, final double f107, final double[] ap) {
        try {
            Class<?> outputClass = getOutputClass();
            Constructor<?> cons = outputClass.getDeclaredConstructor(NRLMSISE00.class,
                                                                     Integer.TYPE,
                                                                     Double.TYPE,
                                                                     Double.TYPE,
                                                                     Double.TYPE,
                                                                     Double.TYPE,
                                                                     Double.TYPE,
                                                                     Double.TYPE,
                                                                     double[].class);
            cons.setAccessible(true);

            return cons.newInstance(atm, doy, sec, lat, lon, hl, f107a, f107, ap);
        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }

    }

    private double getOutputDensity(Object o, int index) {
        try {
            Method getDensity = getOutputClass().
                                    getDeclaredMethod("getDensity", Integer.TYPE);
            getDensity.setAccessible(true);
            return ((Double) getDensity.invoke(o, index)).doubleValue();
        } catch (NoSuchMethodException | SecurityException |
                 IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
            return Double.NaN;
        }
    }

    private double getOutputTemperature(Object o, int index) {
        try {
            java.lang.reflect.Field temperaturesField = getOutputClass().getDeclaredField("temperatures");
            temperaturesField.setAccessible(true);
            return ((double[]) temperaturesField.get(o))[index];
         } catch (NoSuchFieldException | SecurityException |
                 IllegalAccessException | IllegalArgumentException e) {
            Assertions.fail(e.getLocalizedMessage());
            return Double.NaN;
        }

    }

    private Class<?> getFieldOutputClass() {
        for (final Class<?> c : NRLMSISE00.class.getDeclaredClasses()) {
            if (c.getName().endsWith("$FieldOutput")) {
                return c;
            }
        }
        return null;
    }

    private <T extends CalculusFieldElement<T>> Object createFieldOutput(Field<T>field,
                                                                     final NRLMSISE00 atm,
                                                                     final int doy, final double sec,
                                                                     final double lat, final double lon,
                                                                     final double hl,
                                                                     final double f107a, final double f107,
                                                                     final double[] ap) {
        try {
            Class<?> fieldOutputClass = getFieldOutputClass();
            Constructor<?> cons = fieldOutputClass.getDeclaredConstructor(NRLMSISE00.class,
                                                                          Integer.TYPE,
                                                                          CalculusFieldElement.class,
                                                                          CalculusFieldElement.class,
                                                                          CalculusFieldElement.class,
                                                                          CalculusFieldElement.class,
                                                                          Double.TYPE,
                                                                          Double.TYPE,
                                                                          double[].class);
            cons.setAccessible(true);
            return cons.newInstance(atm, doy,
                                    field.getZero().add(sec),
                                    field.getZero().add(lat),
                                    field.getZero().add(lon),
                                    field.getZero().add(hl),
                                    f107a, f107, ap);
        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }

    }

    private double getFieldOutputDensity(Object o, int index) {
        try {
            Method getDensity = getFieldOutputClass().
                                    getDeclaredMethod("getDensity", Integer.TYPE);
            getDensity.setAccessible(true);
            return ((CalculusFieldElement<?>) getDensity.invoke(o, index)).getReal();
        } catch (NoSuchMethodException | SecurityException |
                 IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
            return Double.NaN;
        }
    }

    private double getFieldOutputTemperature(Object o, int index) {
        try {
            java.lang.reflect.Field temperaturesField = getFieldOutputClass().getDeclaredField("temperatures");
            temperaturesField.setAccessible(true);
            return ((CalculusFieldElement[]) temperaturesField.get(o))[index].getReal();
        } catch (NoSuchFieldException | SecurityException |
                 IllegalAccessException | IllegalArgumentException e) {
            Assertions.fail(e.getLocalizedMessage());
            return Double.NaN;
        }

    }

    private void checkLegacy(final int nb, final Object out, final boolean print)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final double[] tInfRef = {1.250540E+03, 1.166754E+03, 1.239892E+03, 1.027318E+03,
                                  1.212396E+03, 1.220146E+03, 1.116385E+03, 1.031247E+03,
                                  1.306052E+03, 1.361868E+03, 1.027318E+03, 1.027318E+03,
                                  1.027318E+03, 1.027318E+03, 1.027318E+03, 1.426412E+03,
                                  1.027318E+03};
        final double[] tAltRef = {1.241416E+03, 1.161710E+03, 1.239891E+03, 2.068878E+02,
                                  1.208135E+03, 1.212712E+03, 1.112999E+03, 1.024848E+03,
                                  1.293374E+03, 1.347389E+03, 2.814648E+02, 2.274180E+02,
                                  2.374389E+02, 2.795551E+02, 2.190732E+02, 1.408608E+03,
                                  1.934071E+02};
        final double[] dHeRef  = {6.665177E+05, 3.407293E+06, 1.123767E+05, 5.411554E+07,
                                  1.851122E+06, 8.673095E+05, 5.776251E+05, 3.740304E+05,
                                  6.748339E+05, 5.528601E+05, 1.375488E+14, 4.427443E+13,
                                  2.127829E+12, 1.412184E+11, 1.254884E+10, 5.196477E+05,
                                  4.260860E+07};
        final double[] dORef   = {1.138806E+08, 1.586333E+08, 6.934130E+04, 1.918893E+11,
                                  1.476555E+08, 1.278862E+08, 6.979139E+07, 4.782720E+07,
                                  1.245315E+08, 1.198041E+08, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 1.274494E+08,
                                  1.241342E+11};
        final double[] dN2Ref  = {1.998211E+07, 1.391117E+07, 4.247105E+01, 6.115826E+12,
                                  1.579356E+07, 1.822577E+07, 1.236814E+07, 5.240380E+06,
                                  2.369010E+07, 3.495798E+07, 2.049687E+19, 6.597567E+18,
                                  3.170791E+17, 2.104370E+16, 1.874533E+15, 4.850450E+07,
                                  4.929562E+12};
        final double[] dO2Ref  = {4.022764E+05, 3.262560E+05, 1.322750E-01, 1.225201E+12,
                                  2.633795E+05, 2.922214E+05, 2.492868E+05, 1.759875E+05,
                                  4.911583E+05, 9.339618E+05, 5.498695E+18, 1.769929E+18,
                                  8.506280E+16, 5.645392E+15, 4.923051E+14, 1.720838E+06,
                                  1.048407E+12};
        final double[] dARRef  = {3.557465E+03, 1.559618E+03, 2.618848E-05, 6.023212E+10,
                                  1.588781E+03, 2.402962E+03, 1.405739E+03, 5.501649E+02,
                                  4.578781E+03, 1.096255E+04, 2.451733E+17, 7.891680E+16,
                                  3.792741E+15, 2.517142E+14, 2.239685E+13, 2.354487E+04,
                                  4.993465E+10};
        final double[] dHRef   = {3.475312E+04, 4.854208E+04, 2.016750E+04, 1.059880E+07,
                                  5.816167E+04, 3.686389E+04, 5.291986E+04, 8.896776E+04,
                                  3.244595E+04, 2.686428E+04, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 2.500078E+04,
                                  8.831229E+06};
        final double[] dNRef   = {4.095913E+06, 4.380967E+06, 5.741256E+03, 2.615737E+05,
                                  5.478984E+06, 3.897276E+06, 1.069814E+06, 1.979741E+06,
                                  5.370833E+06, 4.889974E+06, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 6.279210E+06,
                                  2.252516E+05};
        final double[] dAnORef = {2.667273E+04, 6.956682E+03, 2.374394E+04, 2.819879E-42,
                                  1.264446E+03, 2.667273E+04, 2.667273E+04, 9.121815E+03,
                                  2.667273E+04, 2.805445E+04, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 2.667273E+04,
                                  2.415246E-42};
        final double[] rhoRef  = {4.074714E-15, 5.001846E-15, 2.756772E-18, 3.584426E-10,
                                  4.809630E-15, 4.355866E-15, 2.470651E-15, 1.571889E-15,
                                  4.564420E-15, 4.974543E-15, 1.261066E-03, 4.059139E-04,
                                  1.950822E-05, 1.294709E-06, 1.147668E-07, 5.881940E-15,
                                  2.914304E-10};
        final double deltaT = 1.e-2;
        final double deltaD = 5.e-7;
        final int id = nb - 1;
        if (print) {
            System.out.printf("Case #%d\n", nb);
            System.out.printf("Tinf: %E  %E\n", tInfRef[id],  getOutputTemperature(out, 0));
            System.out.printf("Talt: %E  %E\n", tAltRef[id],  getOutputTemperature(out, 1));
            System.out.printf("He:   %E  %E\n", dHeRef[id],   getOutputDensity(out, 0) * 1e-6);
            System.out.printf("O:    %E  %E\n", dORef[id],    getOutputDensity(out, 1) * 1e-6);
            System.out.printf("N2:   %E  %E\n", dN2Ref[id],   getOutputDensity(out, 2) * 1e-6);
            System.out.printf("O2:   %E  %E\n", dO2Ref[id],   getOutputDensity(out, 3) * 1e-6);
            System.out.printf("Ar:   %E  %E\n", dARRef[id],   getOutputDensity(out, 4) * 1e-6);
            System.out.printf("H:    %E  %E\n", dHRef[id],    getOutputDensity(out, 6) * 1e-6);
            System.out.printf("N:    %E  %E\n", dNRef[id],    getOutputDensity(out, 7) * 1e-6);
            System.out.printf("AnO:  %E  %E\n", dAnORef[id],  getOutputDensity(out, 8) * 1e-6);
            System.out.printf("Rho:  %E  %E\n\n", rhoRef[id], getOutputDensity(out, 5) * 1e-3);
        } else {
            Assertions.assertEquals(tInfRef[id], getOutputTemperature(out, 0), deltaT);
            Assertions.assertEquals(tAltRef[id], getOutputTemperature(out, 1), deltaT);
            Assertions.assertEquals(dHeRef[id],  getOutputDensity(out, 0) * 1e-6, dHeRef[id]  * deltaD);
            Assertions.assertEquals(dORef[id],   getOutputDensity(out, 1) * 1e-6, dORef[id]   * deltaD);
            Assertions.assertEquals(dN2Ref[id],  getOutputDensity(out, 2) * 1e-6, dN2Ref[id]  * deltaD);
            Assertions.assertEquals(dO2Ref[id],  getOutputDensity(out, 3) * 1e-6, dO2Ref[id]  * deltaD);
            Assertions.assertEquals(dARRef[id],  getOutputDensity(out, 4) * 1e-6, dARRef[id]  * deltaD);
            Assertions.assertEquals(dHRef[id],   getOutputDensity(out, 6) * 1e-6, dHRef[id]   * deltaD);
            Assertions.assertEquals(dNRef[id],   getOutputDensity(out, 7) * 1e-6, dNRef[id]   * deltaD);
            Assertions.assertEquals(dAnORef[id], getOutputDensity(out, 8) * 1e-6, dAnORef[id] * deltaD);
            Assertions.assertEquals(rhoRef[id],  getOutputDensity(out, 5) * 1e-3, rhoRef[id]  * deltaD);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private static class InputParams implements NRLMSISE00InputParameters {

        /** Serializable UID. */
        private static final long serialVersionUID = 1L;

        /** Constructor. */
        public InputParams() {

        }

        @Override
        public AbsoluteDate getMinDate() {
            return new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC());
        }

        @Override
        public AbsoluteDate getMaxDate() {
            return new AbsoluteDate(2003, 12, 31, TimeScalesFactory.getUTC());
        }

        @Override
        public double getDailyFlux(AbsoluteDate date) {
            return 150.;
        }

        @Override
        public double getAverageFlux(AbsoluteDate date) {
            return 150.;
        }

        @Override
        public double[] getAp(AbsoluteDate date) {
            return new double[] {4., 100., 100., 100., 100., 100., 100.};
        }
    }
}
