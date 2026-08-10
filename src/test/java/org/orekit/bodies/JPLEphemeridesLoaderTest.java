/* Copyright 2002-2026 CS GROUP
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
package org.orekit.bodies;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class JPLEphemeridesLoaderTest {

    @Test
    void testConstantsJPL() {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.SUN);
        Assertions.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assertions.assertEquals(81.30056, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
        Assertions.assertTrue(Double.isNaN(loader.getLoadedConstant("not-a-constant")));
    }

    @Test
    void testConstantsInpop() {
        Utils.setDataRoot("inpop");
        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_INPOP_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.SUN);
        Assertions.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assertions.assertEquals(81.30057, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    void testGMJPL() {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.SUN);
        Assertions.assertEquals(22032.080e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
                            1.0e6);
        Assertions.assertEquals(324858.599e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
                            1.0e6);
        Assertions.assertEquals(42828.314e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
                            1.0e6);
        Assertions.assertEquals(126712767.863e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
                            6.0e7);
        Assertions.assertEquals(37940626.063e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
                            2.0e6);
        Assertions.assertEquals(5794549.007e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
                            1.0e6);
        Assertions.assertEquals(6836534.064e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
                            1.0e6);
        Assertions.assertEquals(981.601e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
                            1.0e6);
        Assertions.assertEquals(132712440017.987e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
                            1.0e6);
        Assertions.assertEquals(4902.801e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
                            1.0e6);
        Assertions.assertEquals(403503.233e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
                            1.0e6);
    }

    @Test
    void testGMInpop() {

        Utils.setDataRoot("inpop");

        JPLEphemeridesLoader loader =
                new JPLEphemeridesLoader("^inpop.*TCB.*littleendian.*\\.dat$",
                                         JPLEphemeridesLoader.EphemerisType.SUN);
        Assertions.assertEquals(22032.081e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
                            1.0e6);
        Assertions.assertEquals(324858.597e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
                            1.0e6);
        Assertions.assertEquals(42828.376e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
                            1.0e6);
        Assertions.assertEquals(126712764.535e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
                            6.0e7);
        Assertions.assertEquals(37940585.443e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
                            2.0e6);
        Assertions.assertEquals(5794549.099e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
                            1.0e6);
        Assertions.assertEquals(6836527.128e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
                            1.0e6);
        Assertions.assertEquals(971.114e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
                            1.0e6);
        Assertions.assertEquals(132712442110.032e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
                            1.0e6);
        Assertions.assertEquals(4902.800e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
                            1.0e6);
        Assertions.assertEquals(403503.250e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
                            1.0e6);
    }

    @Test
    void testDerivative405() {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                        new AbsoluteDate(1969, 6, 25, TimeScalesFactory.getTT()),
                        691200.0);
    }

    @Test
    void testDerivative406() {
        Utils.setDataRoot("regular-data:regular-data/de406-ephemerides");
        checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                        new AbsoluteDate(2964, 9, 26, TimeScalesFactory.getTT()),
                        1382400.0);
    }

    @Test
    void testDummyEarth() {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        JPLEphemeridesLoader loader =
                new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                         JPLEphemeridesLoader.EphemerisType.EARTH);
        CelestialBody body = loader.loadCelestialBody(CelestialBodyFactory.EARTH);
        AbsoluteDate date = new AbsoluteDate(1950, 1, 12, TimeScalesFactory.getTT());
        Frame eme2000 = FramesFactory.getEME2000();
        for (double h = 0; h < 86400; h += 60.0) {
            PVCoordinates pv = body.getPVCoordinates(date, eme2000);
            Assertions.assertEquals(0, pv.getPosition().getNorm(), 1.0e-15);
            Assertions.assertEquals(0, pv.getVelocity().getNorm(), 1.0e-15);
        }
    }

    @Test
    void testEndianness() {
        Utils.setDataRoot("inpop");
        JPLEphemeridesLoader.EphemerisType type = JPLEphemeridesLoader.EphemerisType.MARS;
        JPLEphemeridesLoader loaderInpopTCBBig =
                new JPLEphemeridesLoader("^inpop.*_TCB_.*_bigendian\\.dat$", type);
        CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assertions.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        JPLEphemeridesLoader loaderInpopTCBLittle =
                new JPLEphemeridesLoader("^inpop.*_TCB_.*_littleendian\\.dat$", type);
        CelestialBody bodysInpopTCBLittle = loaderInpopTCBLittle.loadCelestialBody(CelestialBodyFactory.MARS);
        Assertions.assertEquals(1.0, loaderInpopTCBLittle.getLoadedConstant("TIMESC"), 1.0e-10);
        AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4, TimeScalesFactory.getTT());
        Frame eme2000   = FramesFactory.getEME2000();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date        = t0.shiftedBy(dt);
            Vector3D pInpopTCBBig    = bodysInpopTCBBig.getPosition(date, eme2000);
            Vector3D pInpopTCBLittle = bodysInpopTCBLittle.getPosition(date, eme2000);
            Assertions.assertEquals(0.0, pInpopTCBBig.distance(pInpopTCBLittle), 1.0e-10);
        }
        for (String name : DataContext.getDefault().getDataProvidersManager().getLoadedDataNames()) {
            Assertions.assertTrue(name.contains("inpop"));
        }
    }

    @Test
    void testInpopvsJPL() {
        // EMW 2026-03-31 Modified to separate data contexts to prevent one
        // file's ICRF from being used as the basis for another file's Mars
        final LazyLoadedDataContext jpl = Utils.newDataContext("regular-data");
        jpl.getCelestialBodies().addDefaultCelestialBodyLoader("^unxp(\\d\\d\\d\\d)\\.405$");
        final LazyLoadedDataContext inpopTdb = Utils.newDataContext("inpop");
        inpopTdb.getCelestialBodies().addDefaultCelestialBodyLoader("^inpop.*_TDB_.*_bigendian\\.dat$");
        final LazyLoadedDataContext inpopTcb = Utils.newDataContext("inpop");
        inpopTcb.getCelestialBodies().addDefaultCelestialBodyLoader("^inpop.*_TCB_.*_bigendian\\.dat$");
        JPLEphemeridesLoader.EphemerisType type = JPLEphemeridesLoader.EphemerisType.MARS;
        CelestialBody bodysDE405 = jpl.getCelestialBodies().getMars();
        JPLEphemeridesLoader loaderInpopTDBBig =
                new JPLEphemeridesLoader("^inpop.*_TDB_.*_bigendian\\.dat$", type,
                        inpopTdb.getDataProvidersManager(),
                        inpopTdb.getTimeScales(),
                        inpopTdb.getFrames().getGCRF(),
                        inpopTdb.getCelestialBodies());
        CelestialBody bodysInpopTDBBig = inpopTdb.getCelestialBodies().getMars();
        Assertions.assertEquals(0.0, loaderInpopTDBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        JPLEphemeridesLoader loaderInpopTCBBig =
                new JPLEphemeridesLoader("^inpop.*_TCB_.*_bigendian\\.dat$", type,
                        inpopTcb.getDataProvidersManager(),
                        inpopTcb.getTimeScales(),
                        inpopTcb.getFrames().getGCRF(),
                        inpopTcb.getCelestialBodies());
        CelestialBody bodysInpopTCBBig = inpopTcb.getCelestialBodies().getMars();
        Assertions.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4, TimeScalesFactory.getTT());
        Frame eme2000   = FramesFactory.getEME2000();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = t0.shiftedBy(dt);
            Vector3D pDE405          = bodysDE405.getPosition(date, eme2000);
            Vector3D pInpopTDBBig    = bodysInpopTDBBig.getPosition(date, eme2000);
            Vector3D pInpopTCBBig    = bodysInpopTCBBig.getPosition(date, eme2000);
            Assertions.assertTrue(pDE405.distance(pInpopTDBBig) >  650.0);
            Assertions.assertTrue(pDE405.distance(pInpopTDBBig) < 1050.0);
            Assertions.assertTrue(pDE405.distance(pInpopTCBBig) > 1000.0);
            Assertions.assertTrue(pDE405.distance(pInpopTCBBig) < 2000.0);
        }

    }

    @Test
    void testOverlappingEphemeridesData() throws IOException {
        Utils.setDataRoot("overlapping-data/data.zip");

        // the data root contains two ephemerides files (JPL DE 405), which overlap in the period
        // (1999-12-23T23:58:55.816, 2000-01-24T23:58:55.815)
        // this test checks that the data in the overlapping and surrounding range is loaded correctly
        // from both files (see issue #113).

        // as the bug only manifests if the DataLoader first loads the ephemerides file containing earlier
        // data points, the data files are zipped to get a deterministic order when listing files

        CelestialBody moon = CelestialBodyFactory.getMoon();

        // 1999/12/31 0h00
        final AbsoluteDate initDate = new AbsoluteDate(1999, 12, 31, 00, 00, 00, TimeScalesFactory.getUTC());
        moon.getPVCoordinates(initDate, FramesFactory.getGCRF());

        // 2000/04/01 0h00
        final AbsoluteDate otherDate = new AbsoluteDate(2000, 02, 01, 00, 00, 00, TimeScalesFactory.getUTC());
        moon.getPVCoordinates(otherDate, FramesFactory.getGCRF());

        // 3 years from initDate
        AbsoluteDate currentDate = new AbsoluteDate(1999, 12, 01, 00, 00, 00, TimeScalesFactory.getTAI());
        AbsoluteDate finalDate = new AbsoluteDate(2000, 03, 14, 00, 00, 00, TimeScalesFactory.getTAI());

        while (currentDate.compareTo(finalDate) < 0)  {
            currentDate = currentDate.shiftedBy(Constants.JULIAN_DAY);
            moon.getPVCoordinates(currentDate, FramesFactory.getGCRF());
        }

    }

    /**
     * Check against subset of DE431 validation set from
     * https://ssd.jpl.nasa.gov/ftp/eph/planets/Linux/de431/testpo.431
     * @throws IOException on error.
     */
    @Test
    public void testPo431() throws IOException {
        // setup
        Utils.setDataRoot("regular-data/de431-ephemerides");
        final int nChecked = testPo("/bodies/testpo.431");
        MatcherAssert.assertThat(nChecked, Matchers.is(5));
    }

    /**
     * Check against subset of DE405 validation set from
     * https://ssd.jpl.nasa.gov/ftp/eph/planets/Linux/de405/testpo.405
     * @throws IOException on error.
     */
    @Test
    public void testPo405() throws IOException {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        final int nChecked = testPo("/bodies/testpo.405");
        MatcherAssert.assertThat(nChecked, Matchers.is(5));
    }

    /**
     * Check against subset of DE440 validation set from
     * https://ssd.jpl.nasa.gov/ftp/eph/planets/Linux/de440/testpo.440
     * @throws IOException on error.
     */
    @Test
    public void testPo440() throws IOException {
        Utils.setDataRoot("2007");
        final int nChecked = testPo("/2007/testpo.440");
        MatcherAssert.assertThat(nChecked, Matchers.is(11));
    }

    /**
     * Test parsing of 2021 format against a truncated version of de440-ephemerides.
     *
     * @param ephemerisType type of ephemeris to load.
     * @see <a href="https://gitlab.orekit.org/orekit/orekit/-/work_items/1938">Issue 1938</a>
     */
    @ParameterizedTest
    @EnumSource(JPLEphemeridesLoader.EphemerisType.class)
    public void Test2021FormatParsing(final JPLEphemeridesLoader.EphemerisType ephemerisType) {

        // GIVEN
        Utils.setDataRoot("regular-data/de440-ephemerides");

        // WHEN
        JPLEphemeridesLoader loaderWithout2021Format =
                        new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES, ephemerisType);

        JPLEphemeridesLoader loaderWith2021Format =
                        new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_2021_SUPPORTED_NAMES, ephemerisType);

        // THEN
        Assertions.assertThrows(OrekitException.class, loaderWithout2021Format::getLoadedAstronomicalUnit);
        Assertions.assertDoesNotThrow(loaderWith2021Format::getLoadedAstronomicalUnit);
    }

    @Test
    void testJPLLoadLibration() {
        Utils.setDataRoot("regular-data/de431-ephemerides");

        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.LIBRATION);
        JPLLibration libration = loader.loadLibration();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 0, 1, 4.184, TimeScalesFactory.getTDB());
        Assertions.assertEquals(-0.05408383637, libration.getPhi(date), 1.0e-11);
        Assertions.assertEquals(0.42483375049, libration.getTheta(date), 1.0e-11);
        Assertions.assertEquals(2564.14339017, libration.getPsi(date), 1.0e-8);
    }

    private int testPo(String name) throws IOException {
        JPLEphemeridesLoader loader = new JPLEphemeridesLoader(
                JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                JPLEphemeridesLoader.EphemerisType.SUN);
        final double au = loader.getLoadedAstronomicalUnit();
        final double day = Constants.JULIAN_DAY;
        final TimeScale tdb = TimeScalesFactory.getTDB();
        final double tolS = 1e-13;
        final double tolP = 1e-13 * au; // tolerance used by JPL
        final double tolV = 1e-13 * au / day; // tolerance used by JPL
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame icrf = FramesFactory.getICRF();
        final Set<Integer> nearEarth = new HashSet<>();
        nearEarth.add(3);
        nearEarth.add(10);
        nearEarth.add(13);

        int i = 0;
        // 431  1999.12.01 2451513.5  8 11  2      -23.03253618370120000000
        try(InputStream is = this.getClass().getResourceAsStream(name);
            final Scanner scanner = new Scanner(is, "UTF-8"))
        {
            scanner.useLocale(Locale.ROOT);
            while (scanner.hasNext()) {
                final int version = scanner.nextInt();
                final String dateString = scanner.next().replace('.', '-');
                final AbsoluteDate date = new AbsoluteDate(dateString, tdb);
                final double jd = scanner.nextDouble();
                final String message = version + " " + dateString;
                MatcherAssert.assertThat(
                        message,
                        date.getJD(tdb),
                        Matchers.closeTo(jd, tolS));
                final int targetInt = scanner.nextInt();
                final int centerInt = scanner.nextInt();
                final String targetName = getName(targetInt);
                final String centerName = getName(centerInt);
                if (targetName == null || centerName == null) {
                    scanner.nextLine();
                    continue;
                }
                CelestialBody target = CelestialBodyFactory.getBody(targetName);
                CelestialBody center = CelestialBodyFactory.getBody(centerName);
                Frame frame;
                if (nearEarth.contains(targetInt) || nearEarth.contains(centerInt)) {
                    frame = gcrf;
                } else {
                    frame = icrf;
                }
                PVCoordinates targetPv = target.getPVCoordinates(date, frame);
                PVCoordinates centerPv = center.getPVCoordinates(date, frame);
                final int coordinate = scanner.nextInt();
                double actual = getCoordinate(targetPv, centerPv, coordinate);
                final double expected = scanner.nextDouble();
                if (coordinate < 4) {
                    // position
                    MatcherAssert.assertThat(
                            message,
                            actual,
                            Matchers.closeTo(expected * au, tolP));
                } else {
                    // velocity
                    MatcherAssert.assertThat(
                            message,
                            actual,
                            Matchers.closeTo(expected * au / day, tolV));
                }
                i++;
                scanner.nextLine();
            }
        }
        return i;
    }

    /**
     * Get the selected coordinate
     *
     * @param targetPv   A
     * @param centerPv   B
     * @param coordinate number from JPL.
     * @return (A - B).coordinate
     */
    private double getCoordinate(PVCoordinates targetPv,
                                 PVCoordinates centerPv,
                                 int coordinate) {
        final Vector3D d;
        if (coordinate < 4) {
            d = targetPv.getPosition().subtract(centerPv.getPosition());
        } else {
            d = targetPv.getVelocity().subtract(centerPv.getVelocity());
            coordinate -= 3;
        }

        switch (coordinate) {
            case 1:
                return d.getX();
            case 2:
                return d.getY();
            case 3:
                return d.getZ();
            default:
                throw new RuntimeException("Unknown coordinate: " + coordinate);
        }
    }


    /**
     * Get the Orekit body name for a JPL body number.
     *
     * @param body number.
     * @return body name.
     */
    private static String getName(int body) {
        /*
        target number (1-Mercury, ...,3-Earth, ,,,9-Pluto, 10-Moon, 11-Sun,
                       12-Solar System Barycenter, 13-Earth-Moon Barycenter
                       14-Nutations, 15-Librations)
         */
        return switch (body) {
            case 1 -> CelestialBodyFactory.MERCURY;
            case 2 -> CelestialBodyFactory.VENUS;
            case 3 -> CelestialBodyFactory.EARTH;
            case 4 -> CelestialBodyFactory.MARS;
            case 5 -> CelestialBodyFactory.JUPITER;
            case 6 -> CelestialBodyFactory.SATURN;
            case 7 -> CelestialBodyFactory.URANUS;
            case 8 -> CelestialBodyFactory.NEPTUNE;
            case 9 -> CelestialBodyFactory.PLUTO;
            case 10 -> CelestialBodyFactory.MOON;
            case 11 -> CelestialBodyFactory.SUN;
            case 12 -> CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER;
            case 13 -> CelestialBodyFactory.EARTH_MOON;
            default -> null;
        };
    }

    private void checkDerivative(String supportedNames, AbsoluteDate date, double maxChunkDuration)
        {
        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MERCURY);
        CelestialBody body = loader.loadCelestialBody(CelestialBodyFactory.MERCURY);
        double h = 20;

        // eight points finite differences estimation of the velocity
        Frame eme2000 = FramesFactory.getEME2000();
        Vector3D pm4h = body.getPosition(date.shiftedBy(-4 * h), eme2000);
        Vector3D pm3h = body.getPosition(date.shiftedBy(-3 * h), eme2000);
        Vector3D pm2h = body.getPosition(date.shiftedBy(-2 * h), eme2000);
        Vector3D pm1h = body.getPosition(date.shiftedBy(    -h), eme2000);
        Vector3D pp1h = body.getPosition(date.shiftedBy(     h), eme2000);
        Vector3D pp2h = body.getPosition(date.shiftedBy( 2 * h), eme2000);
        Vector3D pp3h = body.getPosition(date.shiftedBy( 3 * h), eme2000);
        Vector3D pp4h = body.getPosition(date.shiftedBy( 4 * h), eme2000);
        Vector3D d4   = pp4h.subtract(pm4h);
        Vector3D d3   = pp3h.subtract(pm3h);
        Vector3D d2   = pp2h.subtract(pm2h);
        Vector3D d1   = pp1h.subtract(pm1h);
        double c = 1.0 / (840 * h);
        Vector3D estimatedV = new Vector3D(-3 * c, d4, 32 * c, d3, -168 * c, d2, 672 * c, d1);

        Vector3D loadedV = body.getPVCoordinates(date, eme2000).getVelocity();
        Assertions.assertEquals(0, loadedV.subtract(estimatedV).getNorm(), 3.5e-10 * loadedV.getNorm());
        Assertions.assertEquals(maxChunkDuration, loader.getMaxChunksDuration(), 1.0e-10);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
