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

import java.io.IOException;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class DivertedSingularityAimingTest {

    @Test
    public void testSingularityOutside() {
        Assertions.assertEquals(1, aiming.getSingularPoints().size());
        final GeodeticPoint singularity = aiming.getSingularPoints().get(0);
        Assertions.assertEquals(Location.OUTSIDE, aoi.checkPoint(toS2Point(singularity)));
    }

    @Test
    public void testAroundSingularity() throws IOException {

        GeodeticPoint singularityGP = aiming.getSingularPoints().get(0);
        Vector3D singularity = earth.transform(singularityGP);
        Assertions.assertEquals(GeodeticPoint.SOUTH_POLE.getLatitude(), singularityGP.getLatitude(), 1.0e-10);

        // in a small disk (less than 1cm radius) around singularity, aiming direction changes a lot
        double lat = GeodeticPoint.SOUTH_POLE.getLatitude() + 1.0e-9;
        GeodeticPoint gp000  = new GeodeticPoint(lat, 0.0, 0.0);
        Vector3D      p000   = earth.transform(gp000);
        Vector3D      dir000 = aiming.alongTileDirection(p000, gp000);
        GeodeticPoint gp090  = new GeodeticPoint(lat, 0.5 * FastMath.PI, 0.0);
        Vector3D      p090   = earth.transform(gp090);
        Vector3D      dir090 = aiming.alongTileDirection(p090, gp090);
        Assertions.assertEquals(0.0064, Vector3D.distance(singularity, p000), 1.0e-4);
        Assertions.assertEquals(0.0064, Vector3D.distance(singularity, p090), 1.0e-4);
        Assertions.assertEquals(FastMath.PI, Vector3D.angle(dir000, dir090), 5.0e-7);

    }

    @Test
    public void testOppositeSingularity() throws IOException {

        GeodeticPoint singularityGP = aiming.getSingularPoints().get(0);
        Vector3D singularity = earth.transform(singularityGP);
        Vector3D opposite    = singularity.negate();
        GeodeticPoint oppositeGP = earth.transform(opposite, earth.getBodyFrame(), null);
        Assertions.assertEquals(GeodeticPoint.NORTH_POLE.getLatitude(), oppositeGP.getLatitude(), 1.0e-10);

        // around opposite of singularity, aiming direction is almost constant
        // (as we use dipole field to model aiming direction, there is only one singularity)
        Vector3D refDir = aiming.alongTileDirection(opposite, oppositeGP);
        double lat = GeodeticPoint.NORTH_POLE.getLatitude() - 1.0e-9;
        for (double lon = 0; lon < 2 * FastMath.PI; lon += 0.001) {
            GeodeticPoint gp = new GeodeticPoint(lat, lon, 0.0);
            Vector3D      p  = earth.transform(gp);
            Vector3D      dir = aiming.alongTileDirection(p, gp);
            Assertions.assertEquals(0.0064, Vector3D.distance(opposite, p), 1.0e-4);
            Assertions.assertEquals(0.0, Vector3D.angle(refDir, dir), 1.1e-9);
        }

    }

    /** Test issue 969 on computation of singularity point.
     * Issue was due to an Hipparchus bug (see https://github.com/Hipparchus-Math/hipparchus/issues/208).
     * It was fixed by upgrading to Hipparchus 2.3. 
     */
    @Test
    public void testIssue969() throws IOException {

        // Given
        // -----
        
        // Zone on Earth
        final GeodeticPoint northWest = new GeodeticPoint(FastMath.toRadians(30.), FastMath.toRadians(-30.), 10.);
        final GeodeticPoint southWest = new GeodeticPoint(FastMath.toRadians(-10.), FastMath.toRadians(-30.), 3000.);
        final GeodeticPoint southEast = new GeodeticPoint(FastMath.toRadians(-10.), FastMath.toRadians(20.), -2000.);
        final GeodeticPoint northEast = new GeodeticPoint(FastMath.toRadians(30.), FastMath.toRadians(20.), -30.);

        // When
        // ----
        
        // Counter clockwise zone definition
        final SphericalPolygonsSet targetZone = EllipsoidTessellator.buildSimpleZone(1.0e-10, northWest, southWest,
                                                                                     southEast, northEast);
        // Build DivertedSingularityAiming
        final TileAiming tileAiming = new DivertedSingularityAiming(targetZone);

        // Then
        // ----
        
        // Check center and singularity
        final S2Point centerZone = targetZone.getEnclosingCap().getCenter();
        final GeodeticPoint centerGP = new GeodeticPoint(0.5 * FastMath.PI - centerZone.getPhi(), centerZone.getTheta(), 0.0);       

        // Get singularity point (there should be just one)
        List<GeodeticPoint> singularGPs = tileAiming.getSingularPoints();
        final GeodeticPoint singularGP = singularGPs.get(0);

        // Singular list size
        Assertions.assertEquals(1, singularGPs.size());

        // Check center
        Assertions.assertEquals(9.0794674733, FastMath.toDegrees(centerGP.getLatitude()), 1.0e-10);
        Assertions.assertEquals(-5., FastMath.toDegrees(centerGP.getLongitude()), 1.0e-14);
        Assertions.assertEquals(0., FastMath.toDegrees(centerGP.getAltitude()), 0.);

        // Check singularity (should be at the antipodes of center)
        Assertions.assertEquals(-9.0794674733, FastMath.toDegrees(singularGP.getLatitude()), 1.0e-10);
        Assertions.assertEquals(175., FastMath.toDegrees(singularGP.getLongitude()), 1.0e-13);
        Assertions.assertEquals(0., FastMath.toDegrees(singularGP.getAltitude()), 0.);

    }

    private S2Point toS2Point(final GeodeticPoint point) {
        return new S2Point(point.getLongitude(), 0.5 * FastMath.PI - point.getLatitude());
    }

    @BeforeEach
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

    @AfterEach
    public void tearDown() {
        earth  = null;
        aoi    = null;
        aiming = null;
    }

    private OneAxisEllipsoid earth;
    private SphericalPolygonsSet aoi;
    private TileAiming aiming;

}
