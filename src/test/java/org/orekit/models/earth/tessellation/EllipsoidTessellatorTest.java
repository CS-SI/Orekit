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

import java.io.IOException;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.partitioning.RegionFactory;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class EllipsoidTessellatorTest {

    @Test
    public void testTilesAlongDescendingTrackWithoutTruncation() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 16);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0,
                                                              false, false);
        Assert.assertEquals(2,   tiles.size());
        Assert.assertEquals(109, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(4,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));

    }

    @Test
    public void testTilesAlongDescendingTrackWithTruncation() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 16);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0,
                                                              true, true);
        Assert.assertEquals(2,   tiles.size());
        Assert.assertEquals(108, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(4,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));

    }

    @Test
    public void testSampleAlongDescendingTrack() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 4);
        final List<List<GeodeticPoint>> samples = tessellator.sample(buildFrance(), 25000.0, 50000.0);
        Assert.assertEquals(2,   samples.size());
        Assert.assertEquals(452, FastMath.max(samples.get(0).size(), samples.get(1).size()));
        Assert.assertEquals(9,   FastMath.min(samples.get(0).size(), samples.get(1).size()));
    }

    @Test
    public void testTilesAlongAscendingTrack() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, true), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0,
                                                              false, false);
        Assert.assertEquals(2,   tiles.size());
        Assert.assertEquals(112, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(6,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
    }

    @Test
    public void testSampleAlongAscendingTrack() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, true), 4);
        final List<List<GeodeticPoint>> samples = tessellator.sample(buildFrance(),
                                                              25000.0, 50000.0);
        Assert.assertEquals(2,   samples.size());
        Assert.assertEquals(452, FastMath.max(samples.get(0).size(), samples.get(1).size()));
        Assert.assertEquals(10,  FastMath.min(samples.get(0).size(), samples.get(1).size()));
    }

    @Test
    public void testTilesConstantAzimuth() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120)), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, -5000.0, -5000.0,
                                                              false, false);
        Assert.assertEquals(2,  tiles.size());
        Assert.assertEquals(86, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(4,  FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
        checkTilesDontOverlap(tiles);
    }

    @Test
    public void testSampleConstantAzimuth() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120)), 4);
        final List<List<GeodeticPoint>> samples = tessellator.sample(buildFrance(), 25000.0, 50000.0);
        Assert.assertEquals(2,   samples.size());
        Assert.assertEquals(455, FastMath.max(samples.get(0).size(), samples.get(1).size()));
        Assert.assertEquals(9,   FastMath.min(samples.get(0).size(), samples.get(1).size()));
    }

    @Test
    public void testTilesIslandJoining() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120.0)), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              150000.0, 250000.0, -5000.0, -5000.0,
                                                              false, false);
        Assert.assertEquals(1,  tiles.size());
        Assert.assertEquals(28, tiles.get(0).size());
        checkTilesDontOverlap(tiles);
    }

    @Test
    public void testTilesSmallZoneWithoutTruncation() throws OrekitException, IOException {

        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(193.7));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(1.0e-10, new double[][] {
            { 43.6543, 1.4268 }, { 43.6120, 1.4179 }, { 43.6016, 1.3994 }, { 43.5682, 1.4159 },
            { 43.5707, 1.4358 }, { 43.5573, 1.4941 }, { 43.6041, 1.4866 }
        });

        final List<List<Tile>> tiles = tessellator.tessellate(small, 50000.0, 150000.0, 0, 0,
                                                              false, false);
        Assert.assertEquals(1, tiles.size());
        Assert.assertEquals(1, tiles.get(0).size());
        Tile t = tiles.get(0).get(0);

        // without truncation, the tile must match width and length specification
        // (the remaining error is due to Cartesian distance and non-developable ellipsoid)
        Assert.assertEquals(150000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[0]),
                                              ellipsoid.transform(t.getVertices()[1])),
                            140.0);
        Assert.assertEquals( 50000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[1]),
                                             ellipsoid.transform(t.getVertices()[2])),
                            0.4);
        Assert.assertEquals(150000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[2]),
                                             ellipsoid.transform(t.getVertices()[3])),
                            140.0);
        Assert.assertEquals( 50000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[3]),
                                             ellipsoid.transform(t.getVertices()[0])),
                            0.4);

    }

    @Test
    public void testTilesSmallZoneWithTruncation() throws OrekitException, IOException {

        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(193.7));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(1.0e-10, new double[][] {
            { 43.6543, 1.4268 }, { 43.6120, 1.4179 }, { 43.6016, 1.3994 }, { 43.5682, 1.4159 },
            { 43.5707, 1.4358 }, { 43.5573, 1.4941 }, { 43.6041, 1.4866 }
        });

        final List<List<Tile>> tiles = tessellator.tessellate(small, 50000.0, 150000.0, 0, 0,
                                                              true, true);
        Assert.assertEquals(1, tiles.size());
        Assert.assertEquals(1, tiles.get(0).size());
        Tile t = tiles.get(0).get(0);

        // with truncation, the tile is a fraction of the width and length specification
        Assert.assertEquals(3.0 / 16.0 * 150000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[0]),
                                              ellipsoid.transform(t.getVertices()[1])),
                            10.0);
        Assert.assertEquals(4.0 / 16.0 * 50000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[1]),
                                             ellipsoid.transform(t.getVertices()[2])),
                            0.01);
        Assert.assertEquals(3.0 / 16.0 * 150000.0,
                           Vector3D.distance(ellipsoid.transform(t.getVertices()[2]),
                                             ellipsoid.transform(t.getVertices()[3])),
                           10.0);
        Assert.assertEquals(4.0 / 16.0 * 50000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[3]),
                                             ellipsoid.transform(t.getVertices()[0])),
                            0.01);
    }

    @Test
    public void testStairedTruncatedTiles() throws OrekitException {

        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(170.0));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(1.0e-10, new double[][] {
            { 45.335,  0.457 },
            { 45.342,  0.469 },
            { 45.371,  0.424 }
        });

        final double maxWidth  = 800.0;
        final double maxLength = 10000.0;
        final List<List<Tile>> tiles = tessellator.tessellate(small, maxWidth, maxLength, 0, 0,
                                                              false, true);
        Assert.assertEquals(1, tiles.size());
        Assert.assertEquals(4, tiles.get(0).size());
        for (final Tile tile : tiles.get(0)) {
            Vector3D v0 = ellipsoid.transform(tile.getVertices()[0]);
            Vector3D v1 = ellipsoid.transform(tile.getVertices()[1]);
            Vector3D v2 = ellipsoid.transform(tile.getVertices()[2]);
            Vector3D v3 = ellipsoid.transform(tile.getVertices()[3]);
            Assert.assertTrue(Vector3D.distance(v0, v1) < (6.0002 / 16.0) * maxLength);
            Assert.assertTrue(Vector3D.distance(v2, v3) < (6.0002 / 16.0) * maxLength);
            Assert.assertEquals(maxWidth, Vector3D.distance(v1, v2), 1.0e-3);
            Assert.assertEquals(maxWidth, Vector3D.distance(v3, v0), 1.0e-3);
        }

    }

    @Test
    public void testTooThinRemainingRegion() throws OrekitException {
        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, -0.2185);
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(1.0e-10, new double[][]{
            { 32.7342, -16.9407 }, { 32.7415, -16.9422 }, { 32.7481, -16.9463 }, { 32.7531, -16.9528 },
            { 32.7561, -16.9608 }, { 32.7567, -16.9696 }, { 32.7549, -16.9781 }, { 32.7508, -16.9855 },
            { 32.7450, -16.9909 }, { 32.7379, -16.9937 }, { 32.7305, -16.9937 }, { 32.7235, -16.9909 },
            { 32.7177, -16.9855 }, { 32.7136, -16.9781 }, { 32.7118, -16.9696 }, { 32.7124, -16.9608 },
            { 32.7154, -16.9528 }, { 32.7204, -16.9463 }, { 32.7269, -16.9422 }
        });

        final double maxWidth  = 40000.0;
        final double maxLength = 40000.0;
        final List<List<Tile>> tiles = tessellator.tessellate(small, maxWidth, maxLength, 0, 0,
                                                              false, true);
        Assert.assertEquals(1, tiles.size());
        Assert.assertEquals(1, tiles.get(0).size());
        for (final Tile tile : tiles.get(0)) {
            Vector3D v0 = ellipsoid.transform(tile.getVertices()[0]);
            Vector3D v1 = ellipsoid.transform(tile.getVertices()[1]);
            Vector3D v2 = ellipsoid.transform(tile.getVertices()[2]);
            Vector3D v3 = ellipsoid.transform(tile.getVertices()[3]);
            Assert.assertTrue(Vector3D.distance(v0, v1) < 0.3 * maxLength);
            Assert.assertEquals(maxWidth, Vector3D.distance(v1, v2), 0.1);
            Assert.assertTrue(Vector3D.distance(v2, v3) < 0.3 * maxLength);
            Assert.assertEquals(maxWidth, Vector3D.distance(v3, v0), 0.1);
        }

    }

    @Test
    public void testNormalZoneTolerance() throws OrekitException {
        doTestVariableTolerance(1.0e-10);
    }

    @Test
    public void testLargeZoneTolerance() throws OrekitException {
        // this used to trigger an exception in EllipsoidTessellator.recurseMeetInside (Orekit)
        doTestVariableTolerance(1.0e-6);
    }

    @Test
    public void testHugeZoneTolerance() throws OrekitException {
        // this used to trigger an exception in Characterization.characterize (Apache Commons Math)
        // this was due to issue MATH-1266, solved in Apache Commons Math 3.6
        doTestVariableTolerance(1.0e-4);
    }

    private void doTestVariableTolerance(final double tolerance) throws OrekitException {
        final ConstantAzimuthAiming aiming = new ConstantAzimuthAiming(ellipsoid,
                                                                       FastMath.toRadians(-168.178485));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(tolerance, new double[][]{
            { -0.01048739, 0.01598931 }, { -0.00789627, 0.01555693 }, { -0.00558595, 0.01430664 },
            { -0.00380677, 0.01237394 }, { -0.00275154, 0.00996826 }, { -0.00253461, 0.00735029 },
            { -0.00317949, 0.00480374 }, { -0.00461629, 0.00260455 }, { -0.00668931, 0.00099105 },
            { -0.00917392, 0.00013808 }, { -0.01180086, 0.00013808 }, { -0.01428546, 0.00099105 },
            { -0.01635849, 0.00260455 }, { -0.01779529, 0.00480374 }, { -0.01844016, 0.00735029 },
            { -0.01822323, 0.00996826 }, { -0.01716800, 0.01237394 }, { -0.01538882, 0.01430664 },
            { -0.01307850, 0.01555693 }
        });

        final double maxWidth  = 40000.0;
        final double maxLength = 40000.0;
        final List<List<Tile>> tiles =
                tessellator.tessellate(small, maxWidth, maxLength, 0, 0, false, true);
        Assert.assertEquals(1, tiles.size());
        Assert.assertEquals(1, tiles.get(0).size());

    }

    private void checkTilesDontOverlap(final List<List<Tile>> tiles) {
        for (final List<Tile> list : tiles) {
            for (final Tile tile : list) {
                final SphericalPolygonsSet quadrilateral =
                        new SphericalPolygonsSet(1.0e-10,
                                                 toS2Point(tile.getVertices()[0]),
                                                 toS2Point(tile.getVertices()[1]),
                                                 toS2Point(tile.getVertices()[2]),
                                                 toS2Point(tile.getVertices()[3]));
                for (final List<Tile> otherList : tiles) {
                    for (final Tile otherTile : otherList) {
                        if (otherTile != tile) {
                            for (final GeodeticPoint vertex : otherTile.getVertices()) {
                                Assert.assertEquals("tiles overlap at: " + vertex,
                                                    Location.OUTSIDE, quadrilateral.checkPoint(toS2Point(vertex)));
                            }
                        }
                    }
                }
            }
        }
    }

    private S2Point toS2Point(final GeodeticPoint point) {
        return new S2Point(point.getLongitude(), 0.5 * FastMath.PI - point.getLatitude());
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        // the following orbital parameters have been computed using
        // Orekit tutorial about phasing, using the following configuration:
        //
        //  orbit.date                          = 2012-01-01T00:00:00.000
        //  phasing.orbits.number               = 143
        //  phasing.days.number                 =  10
        //  sun.synchronous.reference.latitude  = 0
        //  sun.synchronous.reference.ascending = false
        //  sun.synchronous.mean.solar.time     = 10:30:00
        //  gravity.field.degree                = 12
        //  gravity.field.order                 = 12
        AbsoluteDate date = new AbsoluteDate("2012-01-01T00:00:00.000", TimeScalesFactory.getUTC());
        Frame eme2000 = FramesFactory.getEME2000();
        orbit = new CircularOrbit(7173352.811913891,
                                  -4.029194321683225E-4, 0.0013530362644647786,
                                  FastMath.toRadians(98.63218182243709),
                                  FastMath.toRadians(77.55565567747836),
                                  FastMath.PI, PositionAngle.TRUE,
                                  eme2000, date, Constants.EIGEN5C_EARTH_MU);
        ellipsoid =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    private SphericalPolygonsSet buildFrance() {

        final SphericalPolygonsSet continental = buildSimpleZone(1.0e-10, new double[][] {
            { 51.14850,  2.51357 }, { 50.94660,  1.63900 }, { 50.12717,  1.33876 }, { 49.34737, -0.98946 },
            { 49.77634, -1.93349 }, { 48.64442, -1.61651 }, { 48.90169, -3.29581 }, { 48.68416, -4.59234 },
            { 47.95495, -4.49155 }, { 47.57032, -2.96327 }, { 46.01491, -1.19379 }, { 44.02261, -1.38422 },
            { 43.42280, -1.90135 }, { 43.03401, -1.50277 }, { 42.34338,  1.82679 }, { 42.47301,  2.98599 },
            { 43.07520,  3.10041 }, { 43.39965,  4.55696 }, { 43.12889,  6.52924 }, { 43.69384,  7.43518 },
            { 44.12790,  7.54959 }, { 45.02851,  6.74995 }, { 45.33309,  7.09665 }, { 46.42967,  6.50009 },
            { 46.27298,  6.02260 }, { 46.72577,  6.03738 }, { 47.62058,  7.46675 }, { 49.01778,  8.09927 },
            { 49.20195,  6.65822 }, { 49.44266,  5.89775 }, { 49.98537,  4.79922 }
          });

        final SphericalPolygonsSet corsica =
                EllipsoidTessellator.buildSimpleZone(1.0e-10,
                                                     new GeodeticPoint(FastMath.toRadians(42.15249),
                                                                       FastMath.toRadians(9.56001),
                                                                       0.0),
                                                     new GeodeticPoint(FastMath.toRadians(43.00998),
                                                                       FastMath.toRadians(9.39000),
                                                                       0.0),
                                                     new GeodeticPoint(FastMath.toRadians(42.62812),
                                                                       FastMath.toRadians(8.74600),
                                                                       0.0),
                                                     new GeodeticPoint(FastMath.toRadians(42.25651),
                                                                       FastMath.toRadians(8.54421),
                                                                       0.0),
                                                     new GeodeticPoint(FastMath.toRadians(41.58361),
                                                                       FastMath.toRadians(8.77572),
                                                                       0.0),
                                                     new GeodeticPoint(FastMath.toRadians(41.38000),
                                                                       FastMath.toRadians(9.22975),
                                                                       0.0));

          return (SphericalPolygonsSet) new RegionFactory<Sphere2D>().union(continental, corsica);

    }

    private SphericalPolygonsSet buildSimpleZone(double tolerance, double[][] points) {
        for (int i = 0; i < points.length; ++i) {
            points[i][0] = FastMath.toRadians(points[i][0]);
            points[i][1] = FastMath.toRadians(points[i][1]);
        }
        return EllipsoidTessellator.buildSimpleZone(tolerance, points);
    }

    private Orbit orbit;
    private OneAxisEllipsoid ellipsoid;

}
