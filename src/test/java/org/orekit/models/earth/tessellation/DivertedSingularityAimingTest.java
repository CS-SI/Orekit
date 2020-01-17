/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.io.IOException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class DivertedSingularityAimingTest {

    @Test
    public void testSingularityOutside() {
        Assert.assertEquals(1, aiming.getSingularPoints().size());
        final GeodeticPoint singularity = aiming.getSingularPoints().get(0);
        Assert.assertEquals(Location.OUTSIDE, aoi.checkPoint(toS2Point(singularity)));
    }

    @Test
    public void testAroundSingularity() throws IOException {

        GeodeticPoint singularityGP = aiming.getSingularPoints().get(0);
        Vector3D singularity = earth.transform(singularityGP);
        Assert.assertEquals(GeodeticPoint.SOUTH_POLE.getLatitude(), singularityGP.getLatitude(), 1.0e-10);

        // in a small disk (less than 1cm radius) around singularity, aiming direction changes a lot
        double lat = GeodeticPoint.SOUTH_POLE.getLatitude() + 1.0e-9;
        GeodeticPoint gp000  = new GeodeticPoint(lat, 0.0, 0.0);
        Vector3D      p000   = earth.transform(gp000);
        Vector3D      dir000 = aiming.alongTileDirection(p000, gp000);
        GeodeticPoint gp090  = new GeodeticPoint(lat, 0.5 * FastMath.PI, 0.0);
        Vector3D      p090   = earth.transform(gp090);
        Vector3D      dir090 = aiming.alongTileDirection(p090, gp090);
        Assert.assertEquals(0.0064, Vector3D.distance(singularity, p000), 1.0e-4);
        Assert.assertEquals(0.0064, Vector3D.distance(singularity, p090), 1.0e-4);
        Assert.assertEquals(FastMath.PI, Vector3D.angle(dir000, dir090), 5.0e-7);

    }

    @Test
    public void testOppositeSingularity() throws IOException {

        GeodeticPoint singularityGP = aiming.getSingularPoints().get(0);
        Vector3D singularity = earth.transform(singularityGP);
        Vector3D opposite    = singularity.negate();
        GeodeticPoint oppositeGP = earth.transform(opposite, earth.getBodyFrame(), null);
        Assert.assertEquals(GeodeticPoint.NORTH_POLE.getLatitude(), oppositeGP.getLatitude(), 1.0e-10);

        // around opposite of singularity, aiming direction is almost constant
        // (as we use dipole field to model aiming direction, there is only one singularity)
        Vector3D refDir = aiming.alongTileDirection(opposite, oppositeGP);
        double lat = GeodeticPoint.NORTH_POLE.getLatitude() - 1.0e-9;
        for (double lon = 0; lon < 2 * FastMath.PI; lon += 0.001) {
            GeodeticPoint gp = new GeodeticPoint(lat, lon, 0.0);
            Vector3D      p  = earth.transform(gp);
            Vector3D      dir = aiming.alongTileDirection(p, gp);
            Assert.assertEquals(0.0064, Vector3D.distance(opposite, p), 1.0e-4);
            Assert.assertEquals(0.0, Vector3D.angle(refDir, dir), 1.1e-9);
        }

    }

    private S2Point toS2Point(final GeodeticPoint point) {
        return new S2Point(point.getLongitude(), 0.5 * FastMath.PI - point.getLatitude());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        aoi = new SphericalPolygonsSet(1.e-9, new S2Point[] {
            new S2Point(FastMath.toRadians(-120.0), FastMath.toRadians(5.0)),
            new S2Point(FastMath.toRadians(   0.0), FastMath.toRadians(5.0)),
            new S2Point(FastMath.toRadians( 120.0), FastMath.toRadians(5.0))
        });
        aiming = new DivertedSingularityAiming(aoi);
    }

    @After
    public void tearDown() {
        earth  = null;
        aoi    = null;
        aiming = null;
    }

    private OneAxisEllipsoid earth;
    private SphericalPolygonsSet aoi;
    private TileAiming aiming;

}
