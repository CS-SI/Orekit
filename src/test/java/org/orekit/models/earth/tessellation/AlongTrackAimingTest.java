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
package org.orekit.models.earth.tessellation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class AlongTrackAimingTest {

    @Test
    public void testAscending() {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, true);
        for (double latitude = FastMath.toRadians(-50.21); latitude < FastMath.toRadians(50.21); latitude += 0.001) {
            final GeodeticPoint gp = new GeodeticPoint(latitude, 0.0, 0.0);
            final Vector3D aiming = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
            Assertions.assertEquals(1.0, aiming.getNorm(), 1.0e-12);
            final double elevation = 0.5 * FastMath.PI - Vector3D.angle(aiming, gp.getZenith());
            final double azimuth = FastMath.atan2(Vector3D.dotProduct(aiming, gp.getEast()),
                                                  Vector3D.dotProduct(aiming, gp.getNorth()));
            Assertions.assertEquals(0.0, FastMath.toDegrees(elevation), 1.0e-6);
            if (FastMath.abs(FastMath.toDegrees(latitude)) > 49.6) {
                Assertions.assertTrue(FastMath.toDegrees(azimuth) > 80.0);
            }
            if (FastMath.abs(FastMath.toDegrees(latitude)) < 5.0) {
                Assertions.assertTrue(FastMath.toDegrees(azimuth) < 37.0);
            }
            Assertions.assertTrue(FastMath.toDegrees(azimuth) > 36.7);
        }
    }

    @Test
    public void testDescending() {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, false);
        for (double latitude = FastMath.toRadians(-50.21); latitude < FastMath.toRadians(50.21); latitude += 0.001) {
            final GeodeticPoint gp = new GeodeticPoint(latitude, 0.0, 0.0);
            final Vector3D aiming = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
            Assertions.assertEquals(1.0, aiming.getNorm(), 1.0e-12);
            final double elevation = 0.5 * FastMath.PI - Vector3D.angle(aiming, gp.getZenith());
            final double azimuth = MathUtils.normalizeAngle(FastMath.atan2(Vector3D.dotProduct(aiming, gp.getEast()),
                                                                           Vector3D.dotProduct(aiming, gp.getNorth())),
                                                            FastMath.PI);
            Assertions.assertEquals(0.0, FastMath.toDegrees(elevation), 1.0e-6);
            if (FastMath.abs(FastMath.toDegrees(latitude)) > 49.7) {
                Assertions.assertTrue(FastMath.toDegrees(azimuth) < 99.0);
            }
            if (FastMath.abs(FastMath.toDegrees(latitude)) < 5.0) {
                Assertions.assertTrue(FastMath.toDegrees(azimuth) > 143);
            }
            Assertions.assertTrue(FastMath.toDegrees(azimuth) < 143.3);
        }
    }

    @Test
    public void testTooNorthernLatitudePrograde() {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, true);
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(51.0), 0.0, 0.0);
        final Vector3D direction = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
        Assertions.assertEquals(0.0, Vector3D.angle(direction, gp.getEast()), 1.0e-15);
    }

    @Test
    public void testTooNorthernLatitudeRetrograde() {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid,
                                                                 new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(130.), FastMath.toRadians(270.),
                                                                                   FastMath.toRadians(5.300), PositionAngleType.MEAN,
                                                                                   FramesFactory.getEME2000(),
                                                                                   new AbsoluteDate(2008, 4, 7, 0, 0, 0, TimeScalesFactory.getUTC()),
                                                                                   Constants.EIGEN5C_EARTH_MU),
                                                                 true);
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(51.0), 0.0, 0.0);
        final Vector3D direction = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
        Assertions.assertEquals(0.0, Vector3D.angle(direction, gp.getWest()), 1.0e-15);
    }

    @Test
    public void testTooSouthernLatitudePrograde() {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid, orbit, true);
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(-51.0), 0.0, 0.0);
        final Vector3D direction = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
        Assertions.assertEquals(0.0, Vector3D.angle(direction, gp.getEast()), 1.0e-15);
    }

    @Test
    public void testTooSouthernLatitudeRetrrograde() {
        final AlongTrackAiming tileAiming = new AlongTrackAiming(ellipsoid,
                                                                 new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(130.), FastMath.toRadians(270.),
                                                                                   FastMath.toRadians(5.300), PositionAngleType.MEAN,
                                                                                   FramesFactory.getEME2000(),
                                                                                   new AbsoluteDate(2008, 4, 7, 0, 0, 0, TimeScalesFactory.getUTC()),
                                                                                   Constants.EIGEN5C_EARTH_MU),
                                                                 true);
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(-51.0), 0.0, 0.0);
        final Vector3D direction = tileAiming.alongTileDirection(ellipsoid.transform(gp), gp);
        Assertions.assertEquals(0.0, Vector3D.angle(direction, gp.getWest()), 1.0e-15);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        orbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                  FastMath.toRadians(5.300), PositionAngleType.MEAN,
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
