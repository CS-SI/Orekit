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
package org.orekit.models.earth.tessellation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class AlongTrackAimingTest {

    @Test
    public void testAscending() throws OrekitException {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, true);
        for (double latitude = FastMath.toRadians(-50.21); latitude < FastMath.toRadians(50.21); latitude += 0.001) {
            final GeodeticPoint gp = new GeodeticPoint(latitude, 0.0, 0.0);
            final Vector3D aiming = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
            Assert.assertEquals(1.0, aiming.getNorm(), 1.0e-12);
            final double elevation = 0.5 * FastMath.PI - Vector3D.angle(aiming, gp.getZenith());
            final double azimuth = FastMath.atan2(Vector3D.dotProduct(aiming, gp.getEast()),
                                                  Vector3D.dotProduct(aiming, gp.getNorth()));
            Assert.assertEquals(0.0, FastMath.toDegrees(elevation), 1.0e-6);
            if (FastMath.abs(FastMath.toDegrees(latitude)) > 49.6) {
                Assert.assertTrue(FastMath.toDegrees(azimuth) > 80.0);
            }
            if (FastMath.abs(FastMath.toDegrees(latitude)) < 5.0) {
                Assert.assertTrue(FastMath.toDegrees(azimuth) < 37.0);
            }
            Assert.assertTrue(FastMath.toDegrees(azimuth) > 36.7);
        }
    }

    @Test
    public void testDescending() throws OrekitException {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, false);
        for (double latitude = FastMath.toRadians(-50.21); latitude < FastMath.toRadians(50.21); latitude += 0.001) {
            final GeodeticPoint gp = new GeodeticPoint(latitude, 0.0, 0.0);
            final Vector3D aiming = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
            Assert.assertEquals(1.0, aiming.getNorm(), 1.0e-12);
            final double elevation = 0.5 * FastMath.PI - Vector3D.angle(aiming, gp.getZenith());
            final double azimuth = MathUtils.normalizeAngle(FastMath.atan2(Vector3D.dotProduct(aiming, gp.getEast()),
                                                                           Vector3D.dotProduct(aiming, gp.getNorth())),
                                                            FastMath.PI);
            Assert.assertEquals(0.0, FastMath.toDegrees(elevation), 1.0e-6);
            if (FastMath.abs(FastMath.toDegrees(latitude)) > 49.7) {
                Assert.assertTrue(FastMath.toDegrees(azimuth) < 99.0);
            }
            if (FastMath.abs(FastMath.toDegrees(latitude)) < 5.0) {
                Assert.assertTrue(FastMath.toDegrees(azimuth) > 143);
            }
            Assert.assertTrue(FastMath.toDegrees(azimuth) < 143.3);
        }
    }

    @Test
    public void testTooNorthernLatitude() throws OrekitException {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, true);
        try {
            final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(51.0), 0.0, 0.0);
            tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_LATITUDE, oe.getSpecifier());
            Assert.assertEquals(51.0, (Double) (oe.getParts()[0]), 1.0e-10);
        }
    }

    @Test
    public void testTooSouthernLatitude() throws OrekitException {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, true);
        try {
            final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(-51.0), 0.0, 0.0);
            tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_LATITUDE, oe.getSpecifier());
            Assert.assertEquals(-51.0, (Double) (oe.getParts()[0]), 1.0e-10);
        }
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        orbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                  FastMath.toRadians(5.300), PositionAngle.MEAN,
                                  FramesFactory.getEME2000(),
                                  new AbsoluteDate(2008, 4, 7, 0, 0, 0, TimeScalesFactory.getUTC()),
                                  Constants.EIGEN5C_EARTH_MU);
        ellipsoid = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    private Orbit orbit;
    private OneAxisEllipsoid ellipsoid;

}
