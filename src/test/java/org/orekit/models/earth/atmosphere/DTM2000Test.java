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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.SolarInputs97to05;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.TimeZone;

public class DTM2000Test {

    @Test
    public void testWithOriginalTestsCases() {

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth);
        double roTestCase;
        double myRo;

        // Inputs :
//      alt=800.
//      lat=40.
//      day=185.
//      hl=16.
//      xlon=0.
//      fm(1)=150.
//      f(1) =fm(1)
//      fm(2)=0.
//      f(2)=0.
//      akp(1)=0.
//      akp(2)=0.
//      akp(3)=0.
//      akp(4)=0.

        // Outputs :
        roTestCase = 1.8710001353820e-17 * 1000;

        // Computation and results
        myRo = atm.getDensity(185, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assertions.assertEquals(roTestCase, myRo , roTestCase * 1e-14);

//      IDEM., day=275

        roTestCase=    2.8524195214905e-17* 1000;

        myRo = atm.getDensity(275, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assertions.assertEquals(roTestCase, myRo , roTestCase * 1e-14);

//      IDEM., day=355

        roTestCase=    1.7343324462212e-17* 1000;

        myRo = atm.getDensity(355, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assertions.assertEquals(roTestCase, myRo , roTestCase * 2e-14);
//      IDEM., day=85

        roTestCase=    2.9983740796297e-17* 1000;

        myRo = atm.getDensity(85, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assertions.assertEquals(roTestCase, myRo , roTestCase * 1e-14);


//      alt=500.
//      lat=-70.      NB: the subroutine requires latitude in rad
//      day=15.
//      hl=16.        NB: the subroutine requires local time in rad (0hr=0 rad)
//      xlon=0.
//      fm(1)=70.
//      f(1) =fm(1)
//      fm(2)=0.
//      f(2)=0.
//      akp(1)=0.
//      akp(2)=0.
//      akp(3)=0.
//      akp(4)=0.
//      ro=    1.3150282384722D-16
//      tz=    793.65487014559
//      tinf=    793.65549802348
        // note that the values above are the ones present in the original fortran source comments
        // however, running this original source (converted to double precision) does
        // not yield the results in the comments, but instead gives the following results
        // as all the other tests cases do behave correctly, we assume the comments are wrong
        // and prefer to ensure we get the same result as the original CODE
        // as we don't have access to any other tests cases, we can't really decide if this is
        // the best approach. Indeed, we are able to get the same results as original fortran
        roTestCase =    1.5699108952425600E-016* 1000;

        myRo = atm.getDensity(15, 500*1000, 0, FastMath.toRadians(-70), 16*FastMath.PI/12, 70, 70, 0, 0);
        Assertions.assertEquals(roTestCase, myRo , roTestCase * 1e-14);

//      IDEM., alt=800.
//      ro=    1.9556768571305D-18
//      tz=    793.65549797919
//      tinf=    793.65549802348
        // note that the values above are the ones present in the original fortran source comments
        // however, running this original source (converted to double precision) does
        // not yield the results in the comments, but instead gives the following results
        // as all the other tests cases do behave correctly, we assume the comments are wrong
        // and prefer to ensure we get the same result as the original CODE
        // as we don't have access to any other tests cases, we can't really decide if this is
        // the best approach. Indeed, we are able to get the same results as original fortran
        roTestCase =    2.4123751406975562E-018* 1000;
        myRo = atm.getDensity(15, 800*1000, 0, FastMath.toRadians(-70), 16*FastMath.PI/12, 70, 70, 0, 0);
        Assertions.assertEquals(roTestCase, myRo , roTestCase * 1e-14);

    }

    @Test
    public void testNonEarthRotationAxisAlignedFrame() {
        //setup
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Rotation rotation = new Rotation(Vector3D.PLUS_I, FastMath.PI / 2, RotationConvention.VECTOR_OPERATOR);
        Frame frame = new Frame(ecef, new Transform(date, rotation), "other");
        Vector3D pEcef = new Vector3D(6378137 + 300e3, 0, 0);
        Vector3D pFrame = ecef.getStaticTransformTo(frame, date)
                .transformPosition(pEcef);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                6378136.460, 1.0 / 298.257222101, ecef);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth);

        //action
        final double actual = atm.getDensity(date, pFrame, frame);

        //verify
        Assertions.assertEquals(atm.getDensity(date, pEcef, ecef), actual, 0.0);
    }

    @Test
    public void testField() {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth);

        // Computation and results
        for (double alti = 400; alti < 1000; alti += 50) {
            for (double lon = 0; lon < 6; lon += 0.5) {
                for (double lat = -1.5; lat < 1.5; lat += 0.5) {
                    for (double hl = 0; hl < 6; hl += 0.5) {
                        double rhoD = atm.getDensity(185, alti*1000, lon, lat, hl, 50, 150, 0, 0);
                        Binary64 rho64 = atm.getDensity(185, new Binary64(alti*1000),
                                                         new Binary64(lon), new Binary64(lat),
                                                         new Binary64(hl), 50, 150, 0, 0);
                        Assertions.assertEquals(rhoD, rho64.getReal(), rhoD * 1e-14);
                    }
                }
            }
        }

    }

    /** Test issue 539. Density computation should be independent of user's default time zone.
     * See <a href="https://gitlab.orekit.org/orekit/orekit/issues/539"> issue 539 on Orekit forge.</a>
     */
    @Test
    public void testTimeZoneIndependantIssue539() {

        // Prepare input: Choose a date in summer time for "GMT+1" time zone.
        // So that after 22h in GMT we are in the next day in local time
        TimeScale utc     = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate("2000-04-01T22:30:00.000", utc);

        // LEO random position, in GCRF
        Vector3D position = new Vector3D(-1038893.194, -4654348.144, 5021579.14);
        Frame gcrf = FramesFactory.getGCRF();

        // Get ITRF and Earth ellipsoid
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.GRIM5C1_EARTH_FLATTENING, itrf);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth);

        // Store user time zone
        TimeZone defaultTZ = TimeZone.getDefault();

        // Set default time zone to UTC & get density
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
        double rhoUtc = atm.getDensity(date, position, gcrf);

        // Set default time zone to GMT+1 & get density
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
        double rhoParis = atm.getDensity(date, position, gcrf);

        // Check that the 2 densities are equal
        Assertions.assertEquals(0., rhoUtc - rhoParis, 0.);

        // Set back default time zone to what it was before the test, to avoid any interference with another routine
        TimeZone.setDefault(defaultTZ);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
