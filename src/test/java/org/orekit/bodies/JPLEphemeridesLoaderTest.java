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
package org.orekit.bodies;


import java.io.IOException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class JPLEphemeridesLoaderTest {

    @Test
    public void testConstantsJPL() throws OrekitException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30056, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
        Assert.assertTrue(Double.isNaN(loader.getLoadedConstant("not-a-constant")));
    }

    @Test
    public void testConstantsInpop() throws OrekitException {
        Utils.setDataRoot("inpop");
        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_INPOP_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30057, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    public void testGMJPL() throws OrekitException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                     JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(22032.080e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
                            1.0e6);
        Assert.assertEquals(324858.599e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
                            1.0e6);
        Assert.assertEquals(42828.314e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
                            1.0e6);
        Assert.assertEquals(126712767.863e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
                            6.0e7);
        Assert.assertEquals(37940626.063e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
                            2.0e6);
        Assert.assertEquals(5794549.007e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
                            1.0e6);
        Assert.assertEquals(6836534.064e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
                            1.0e6);
        Assert.assertEquals(981.601e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
                            1.0e6);
        Assert.assertEquals(132712440017.987e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
                            1.0e6);
        Assert.assertEquals(4902.801e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
                            1.0e6);
        Assert.assertEquals(403503.233e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
                            1.0e6);
    }

    @Test
    public void testGMInpop() throws OrekitException {

        Utils.setDataRoot("inpop");

        JPLEphemeridesLoader loader =
                new JPLEphemeridesLoader("^inpop.*TCB.*littleendian.*\\.dat$",
                                         JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(22032.081e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
                            1.0e6);
        Assert.assertEquals(324858.597e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
                            1.0e6);
        Assert.assertEquals(42828.376e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
                            1.0e6);
        Assert.assertEquals(126712764.535e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
                            6.0e7);
        Assert.assertEquals(37940585.443e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
                            2.0e6);
        Assert.assertEquals(5794549.099e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
                            1.0e6);
        Assert.assertEquals(6836527.128e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
                            1.0e6);
        Assert.assertEquals(971.114e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
                            1.0e6);
        Assert.assertEquals(132712442110.032e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
                            1.0e6);
        Assert.assertEquals(4902.800e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
                            1.0e6);
        Assert.assertEquals(403503.250e9,
                            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
                            1.0e6);
    }

    @Test
    public void testDerivative405() throws OrekitException {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                        new AbsoluteDate(1969, 6, 25, TimeScalesFactory.getTT()),
                        691200.0);
    }

    @Test
    public void testDerivative406() throws OrekitException {
        Utils.setDataRoot("regular-data:regular-data/de406-ephemerides");
        checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                        new AbsoluteDate(2964, 9, 26, TimeScalesFactory.getTT()),
                        1382400.0);
    }

    @Test
    public void testDummyEarth() throws OrekitException {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        JPLEphemeridesLoader loader =
                new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                                         JPLEphemeridesLoader.EphemerisType.EARTH);
        CelestialBody body = loader.loadCelestialBody(CelestialBodyFactory.EARTH);
        AbsoluteDate date = new AbsoluteDate(1950, 1, 12, TimeScalesFactory.getTT());
        Frame eme2000 = FramesFactory.getEME2000();
        for (double h = 0; h < 86400; h += 60.0) {
            PVCoordinates pv = body.getPVCoordinates(date, eme2000);
            Assert.assertEquals(0, pv.getPosition().getNorm(), 1.0e-15);
            Assert.assertEquals(0, pv.getVelocity().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testEndianness() throws OrekitException {
        Utils.setDataRoot("inpop");
        JPLEphemeridesLoader.EphemerisType type = JPLEphemeridesLoader.EphemerisType.MARS;
        JPLEphemeridesLoader loaderInpopTCBBig =
                new JPLEphemeridesLoader("^inpop.*_TCB_.*_bigendian\\.dat$", type);
        CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        JPLEphemeridesLoader loaderInpopTCBLittle =
                new JPLEphemeridesLoader("^inpop.*_TCB_.*_littleendian\\.dat$", type);
        CelestialBody bodysInpopTCBLittle = loaderInpopTCBLittle.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBLittle.getLoadedConstant("TIMESC"), 1.0e-10);
        AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4, TimeScalesFactory.getTT());
        Frame eme2000   = FramesFactory.getEME2000();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date        = t0.shiftedBy(dt);
            Vector3D pInpopTCBBig    = bodysInpopTCBBig.getPVCoordinates(date, eme2000).getPosition();
            Vector3D pInpopTCBLittle = bodysInpopTCBLittle.getPVCoordinates(date, eme2000).getPosition();
            Assert.assertEquals(0.0, pInpopTCBBig.distance(pInpopTCBLittle), 1.0e-10);
        }
        for (String name : DataProvidersManager.getInstance().getLoadedDataNames()) {
            Assert.assertTrue(name.contains("inpop"));
        }
    }

    @Test
    public void testInpopvsJPL() throws OrekitException {
        Utils.setDataRoot("regular-data:inpop");
        JPLEphemeridesLoader.EphemerisType type = JPLEphemeridesLoader.EphemerisType.MARS;
        JPLEphemeridesLoader loaderDE405 =
                new JPLEphemeridesLoader("^unxp(\\d\\d\\d\\d)\\.405$", type);
        CelestialBody bodysDE405 = loaderDE405.loadCelestialBody(CelestialBodyFactory.MARS);
        JPLEphemeridesLoader loaderInpopTDBBig =
                new JPLEphemeridesLoader("^inpop.*_TDB_.*_bigendian\\.dat$", type);
        CelestialBody bodysInpopTDBBig = loaderInpopTDBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(0.0, loaderInpopTDBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        JPLEphemeridesLoader loaderInpopTCBBig =
                new JPLEphemeridesLoader("^inpop.*_TCB_.*_bigendian\\.dat$", type);
        CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4, TimeScalesFactory.getTT());
        Frame eme2000   = FramesFactory.getEME2000();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = t0.shiftedBy(dt);
            Vector3D pDE405          = bodysDE405.getPVCoordinates(date, eme2000).getPosition();
            Vector3D pInpopTDBBig    = bodysInpopTDBBig.getPVCoordinates(date, eme2000).getPosition();
            Vector3D pInpopTCBBig    = bodysInpopTCBBig.getPVCoordinates(date, eme2000).getPosition();
            Assert.assertTrue(pDE405.distance(pInpopTDBBig) >  650.0);
            Assert.assertTrue(pDE405.distance(pInpopTDBBig) < 1050.0);
            Assert.assertTrue(pDE405.distance(pInpopTCBBig) > 1000.0);
            Assert.assertTrue(pDE405.distance(pInpopTCBBig) < 2000.0);
        }

    }

    @Test
    public void testOverlappingEphemeridesData() throws OrekitException, IOException {
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

    private void checkDerivative(String supportedNames, AbsoluteDate date, double maxChunkDuration)
        throws OrekitException {
        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MERCURY);
        CelestialBody body = loader.loadCelestialBody(CelestialBodyFactory.MERCURY);
        double h = 20;

        // eight points finite differences estimation of the velocity
        Frame eme2000 = FramesFactory.getEME2000();
        Vector3D pm4h = body.getPVCoordinates(date.shiftedBy(-4 * h), eme2000).getPosition();
        Vector3D pm3h = body.getPVCoordinates(date.shiftedBy(-3 * h), eme2000).getPosition();
        Vector3D pm2h = body.getPVCoordinates(date.shiftedBy(-2 * h), eme2000).getPosition();
        Vector3D pm1h = body.getPVCoordinates(date.shiftedBy(    -h), eme2000).getPosition();
        Vector3D pp1h = body.getPVCoordinates(date.shiftedBy(     h), eme2000).getPosition();
        Vector3D pp2h = body.getPVCoordinates(date.shiftedBy( 2 * h), eme2000).getPosition();
        Vector3D pp3h = body.getPVCoordinates(date.shiftedBy( 3 * h), eme2000).getPosition();
        Vector3D pp4h = body.getPVCoordinates(date.shiftedBy( 4 * h), eme2000).getPosition();
        Vector3D d4   = pp4h.subtract(pm4h);
        Vector3D d3   = pp3h.subtract(pm3h);
        Vector3D d2   = pp2h.subtract(pm2h);
        Vector3D d1   = pp1h.subtract(pm1h);
        double c = 1.0 / (840 * h);
        Vector3D estimatedV = new Vector3D(-3 * c, d4, 32 * c, d3, -168 * c, d2, 672 * c, d1);

        Vector3D loadedV = body.getPVCoordinates(date, eme2000).getVelocity();
        Assert.assertEquals(0, loadedV.subtract(estimatedV).getNorm(), 3.5e-10 * loadedV.getNorm());
        Assert.assertEquals(maxChunkDuration, loader.getMaxChunksDuration(), 1.0e-10);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
