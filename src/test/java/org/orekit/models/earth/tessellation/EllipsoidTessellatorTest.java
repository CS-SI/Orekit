/* Copyright 2002-2015 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.partitioning.Region.Location;
import org.apache.commons.math3.geometry.partitioning.RegionFactory;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.Sphere2D;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.util.FastMath;
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
    public void testAlongDescendingTrack() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0);
        Assert.assertEquals(2,   tiles.size());
        Assert.assertEquals(116, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(5,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
    }

    @Test
    public void testAlongAscendingTrack() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, true), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0);
        Assert.assertEquals(2,   tiles.size());
        Assert.assertEquals(113, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(6,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
    }

    @Test
    public void testConstantAzimuth() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120)), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, -5000.0, -5000.0);
        Assert.assertEquals(2,  tiles.size());
        Assert.assertEquals(90, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assert.assertEquals(4,  FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
        checkTilesDontOverlap(tiles);
    }

    @Test
    public void testIslandJoining() throws OrekitException {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120.0)), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              150000.0, 250000.0, -5000.0, -5000.0);
        Assert.assertEquals(1,  tiles.size());
        Assert.assertEquals(27, tiles.get(0).size());
        checkTilesDontOverlap(tiles);
    }

    @Test
    public void testSmallzone() throws OrekitException, IOException {

        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(193.7));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 4);

        SphericalPolygonsSet small = buildSimpleZone(new double[][] {
            { 43.6543, 1.4268 }, { 43.6120, 1.4179 }, { 43.6016, 1.3994 }, { 43.5682, 1.4159 },
            { 43.5707, 1.4358 }, { 43.5573, 1.4941 }, { 43.6041, 1.4866 }
        });

        final List<List<Tile>> tiles = tessellator.tessellate(small, 50000.0, 150000.0, 0, 0);
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

        final SphericalPolygonsSet continental = buildSimpleZone(new double[][] {
            { 51.14850,  2.51357 }, { 50.94660,  1.63900 }, { 50.12717,  1.33876 }, { 49.34737, -0.98946 },
            { 49.77634, -1.93349 }, { 48.64442, -1.61651 }, { 48.90169, -3.29581 }, { 48.68416, -4.59234 },
            { 47.95495, -4.49155 }, { 47.57032, -2.96327 }, { 46.01491, -1.19379 }, { 44.02261, -1.38422 },
            { 43.42280, -1.90135 }, { 43.03401, -1.50277 }, { 42.34338,  1.82679 }, { 42.47301,  2.98599 },
            { 43.07520,  3.10041 }, { 43.39965,  4.55696 }, { 43.12889,  6.52924 }, { 43.69384,  7.43518 },
            { 44.12790,  7.54959 }, { 45.02851,  6.74995 }, { 45.33309,  7.09665 }, { 46.42967,  6.50009 },
            { 46.27298,  6.02260 }, { 46.72577,  6.03738 }, { 47.62058,  7.46675 }, { 49.01778,  8.09927 },
            { 49.20195,  6.65822 }, { 49.44266,  5.89775 }, { 49.98537,  4.79922 }
          });

        final SphericalPolygonsSet corsica = buildSimpleZone(new double[][] {
            { 42.15249,  9.56001 }, { 43.00998,  9.39000 }, { 42.62812,  8.74600 }, { 42.25651,  8.54421 },
            { 41.58361,  8.77572 }, { 41.38000,  9.22975 }
          });

          return (SphericalPolygonsSet) new RegionFactory<Sphere2D>().union(continental, corsica);

    }

    private SphericalPolygonsSet buildSimpleZone(double[][] points) {
        final S2Point[] vertices = new S2Point[points.length];
        for (int i = 0; i < points.length; ++i) {
            vertices[i] = new S2Point(FastMath.toRadians(points[i][1]),         // points[i][1] is longitude
                                      FastMath.toRadians(90.0 - points[i][0])); // points[i][0] is latitude
        }
        return new SphericalPolygonsSet(1.0e-10, vertices);
    }

    private Orbit orbit;
    private OneAxisEllipsoid ellipsoid;

}
