/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.geometry.partitioning.Region;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.partitioning.RegionFactory;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.Vertex;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class EllipsoidTessellatorTest {

    @Test
    public void testTilesAlongDescendingTrackWithoutTruncation() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 16);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0,
                                                              false, false);
        Assertions.assertEquals(2,   tiles.size());
        Assertions.assertEquals(108, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assertions.assertEquals(4,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));

    }

    @Test
    public void testTilesAlongDescendingTrackWithTruncation() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 16);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, 5000.0, 5000.0,
                                                              true, true);
        Assertions.assertEquals(2,   tiles.size());
        Assertions.assertEquals(108, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assertions.assertEquals(4,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));

    }

    @Test
    public void testSampleAlongDescendingTrack() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, false), 4);
        final List<List<GeodeticPoint>> samples = tessellator.sample(buildFrance(), 25000.0, 50000.0);
        Assertions.assertEquals(2,   samples.size());
        Assertions.assertEquals(455, FastMath.max(samples.get(0).size(), samples.get(1).size()));
        Assertions.assertEquals(9,   FastMath.min(samples.get(0).size(), samples.get(1).size()));
    }

    @Test
    public void testTilesAlongAscendingTrack() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, true), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 140000.0, 5000.0, 5000.0,
                                                              false, false);
        Assertions.assertEquals(2,   tiles.size());
        Assertions.assertEquals(121, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assertions.assertEquals(6,   FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
    }

    @Test
    public void testSampleAlongAscendingTrack() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new AlongTrackAiming(ellipsoid, orbit, true), 4);
        final List<List<GeodeticPoint>> samples = tessellator.sample(buildFrance(),
                                                              25000.0, 50000.0);
        Assertions.assertEquals(2,   samples.size());
        Assertions.assertEquals(454, FastMath.max(samples.get(0).size(), samples.get(1).size()));
        Assertions.assertEquals(10,  FastMath.min(samples.get(0).size(), samples.get(1).size()));
    }

    @Test
    public void testTilesConstantAzimuth() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120)), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              50000.0, 150000.0, -5000.0, -5000.0,
                                                              false, false);
        Assertions.assertEquals(2,  tiles.size());
        Assertions.assertEquals(86, FastMath.max(tiles.get(0).size(), tiles.get(1).size()));
        Assertions.assertEquals(4,  FastMath.min(tiles.get(0).size(), tiles.get(1).size()));
        checkTilesDontOverlap(tiles);
    }

    @Test
    public void testSampleConstantAzimuth() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120)), 4);
        final List<List<GeodeticPoint>> samples = tessellator.sample(buildFrance(), 25000.0, 50000.0);
        Assertions.assertEquals(2,   samples.size());
        Assertions.assertEquals(452, FastMath.max(samples.get(0).size(), samples.get(1).size()));
        Assertions.assertEquals(9,   FastMath.min(samples.get(0).size(), samples.get(1).size()));
    }

    @Test
    public void testTilesIslandJoining() {
        final EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(120.0)), 4);
        final List<List<Tile>> tiles = tessellator.tessellate(buildFrance(),
                                                              150000.0, 250000.0, -5000.0, -5000.0,
                                                              false, false);
        Assertions.assertEquals(1,  tiles.size());
        Assertions.assertEquals(30, tiles.get(0).size());
        checkTilesDontOverlap(tiles);
    }

    @Test
    public void testTilesSmallZoneWithoutTruncation() {

        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(193.7));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(1.0e-10, new double[][] {
            { 43.6543, 1.4268 }, { 43.6120, 1.4179 }, { 43.6016, 1.3994 }, { 43.5682, 1.4159 },
            { 43.5707, 1.4358 }, { 43.5573, 1.4941 }, { 43.6041, 1.4866 }
        });

        final List<List<Tile>> tiles = tessellator.tessellate(small, 50000.0, 150000.0, 0, 0,
                                                              false, false);
        Assertions.assertEquals(1, tiles.size());
        Assertions.assertEquals(1, tiles.get(0).size());
        Tile t = tiles.get(0).get(0);

        // without truncation, the tile must match width and length specification
        // (the remaining error is due to Cartesian distance and non-developable ellipsoid)
        Assertions.assertEquals(150000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[0]),
                                              ellipsoid.transform(t.getVertices()[1])),
                            140.0);
        Assertions.assertEquals( 50000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[1]),
                                             ellipsoid.transform(t.getVertices()[2])),
                            0.4);
        Assertions.assertEquals(150000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[2]),
                                             ellipsoid.transform(t.getVertices()[3])),
                            140.0);
        Assertions.assertEquals( 50000.0,
                            Vector3D.distance(ellipsoid.transform(t.getVertices()[3]),
                                             ellipsoid.transform(t.getVertices()[0])),
                            0.4);

    }

    @Test
    public void testTilesSmallZoneWithTruncation() {

        TileAiming aiming = new ConstantAzimuthAiming(ellipsoid, FastMath.toRadians(193.7));
        EllipsoidTessellator tessellator =
                new EllipsoidTessellator(ellipsoid, aiming, 16);

        SphericalPolygonsSet small = buildSimpleZone(1.0e-10, new double[][] {
            { 43.6543, 1.4268 }, { 43.6120, 1.4179 }, { 43.6016, 1.3994 }, { 43.5682, 1.4159 },
            { 43.5707, 1.4358 }, { 43.5573, 1.4941 }, { 43.6041, 1.4866 }
        });

        final List<List<Tile>> tiles = tessellator.tessellate(small, 50000.0, 150000.0, 0, 0,
                                                              true, true);

        Assertions.assertEquals(1, tiles.size());
        Assertions.assertEquals(1, tiles.get(0).size());
        Tile t = tiles.get(0).get(0);

        // with truncation, the tile is a fraction of the width and length specification
        Assertions.assertEquals(2.0 / 16.0 * 150000.0,
                                Vector3D.distance(ellipsoid.transform(t.getVertices()[0]),
                                                  ellipsoid.transform(t.getVertices()[1])),
                                10.0);
        Assertions.assertEquals(4.0 / 16.0 * 50000.0,
                                Vector3D.distance(ellipsoid.transform(t.getVertices()[1]),
                                                  ellipsoid.transform(t.getVertices()[2])),
                                0.01);
        Assertions.assertEquals(2.0 / 16.0 * 150000.0,
                                Vector3D.distance(ellipsoid.transform(t.getVertices()[2]),
                                                  ellipsoid.transform(t.getVertices()[3])),
                                10.0);
        Assertions.assertEquals(4.0 / 16.0 * 50000.0,
                                Vector3D.distance(ellipsoid.transform(t.getVertices()[3]),
                                                  ellipsoid.transform(t.getVertices()[0])),
                                0.01);
    }

    @Test
    public void testStairedTruncatedTiles() {

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
        Assertions.assertEquals(1, tiles.size());
        Assertions.assertEquals(4, tiles.get(0).size());
        for (final Tile tile : tiles.get(0)) {
            Vector3D v0 = ellipsoid.transform(tile.getVertices()[0]);
            Vector3D v1 = ellipsoid.transform(tile.getVertices()[1]);
            Vector3D v2 = ellipsoid.transform(tile.getVertices()[2]);
            Vector3D v3 = ellipsoid.transform(tile.getVertices()[3]);
            Assertions.assertTrue(Vector3D.distance(v0, v1) < (6.0002 / 16.0) * maxLength);
            Assertions.assertTrue(Vector3D.distance(v2, v3) < (6.0002 / 16.0) * maxLength);
            Assertions.assertEquals(maxWidth, Vector3D.distance(v1, v2), 1.0e-3);
            Assertions.assertEquals(maxWidth, Vector3D.distance(v3, v0), 1.0e-3);
        }

    }

    @Test
    public void testTooThinRemainingRegion() {
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
        Assertions.assertEquals(1, tiles.size());
        Assertions.assertEquals(1, tiles.get(0).size());
        for (final Tile tile : tiles.get(0)) {
            Vector3D v0 = ellipsoid.transform(tile.getVertices()[0]);
            Vector3D v1 = ellipsoid.transform(tile.getVertices()[1]);
            Vector3D v2 = ellipsoid.transform(tile.getVertices()[2]);
            Vector3D v3 = ellipsoid.transform(tile.getVertices()[3]);
            Assertions.assertTrue(Vector3D.distance(v0, v1) < 0.3 * maxLength);
            Assertions.assertEquals(maxWidth, Vector3D.distance(v1, v2), 0.1);
            Assertions.assertTrue(Vector3D.distance(v2, v3) < 0.3 * maxLength);
            Assertions.assertEquals(maxWidth, Vector3D.distance(v3, v0), 0.1);
        }

    }

    @Test
    public void testNormalZoneTolerance() {
        doTestVariableTolerance(1.0e-10);
    }

    @Test
    public void testLargeZoneTolerance() {
        // this used to trigger an exception in EllipsoidTessellator.recurseMeetInside (Orekit)
        doTestVariableTolerance(1.0e-6);
    }

    @Test
    public void testHugeZoneTolerance() {
        // this used to trigger an exception in Characterization.characterize (Apache Commons Math)
        // this was due to issue MATH-1266, solved in Apache Commons Math 3.6
        doTestVariableTolerance(1.0e-4);
    }

    private void doTestVariableTolerance(final double tolerance) {
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
        Assertions.assertEquals(1, tiles.size());
        Assertions.assertEquals(1, tiles.get(0).size());

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
                                Assertions.assertEquals(Location.OUTSIDE, quadrilateral.checkPoint(toS2Point(vertex)),"tiles overlap at: " + vertex);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testSampleAroundPoleConstantAzimuth() {
        SphericalPolygonsSet aoi = new SphericalPolygonsSet(1.e-9,
                                                            new S2Point(FastMath.toRadians(-120.0), FastMath.toRadians(5.0)),
                                                            new S2Point(FastMath.toRadians(   0.0), FastMath.toRadians(5.0)),
                                                            new S2Point(FastMath.toRadians( 120.0), FastMath.toRadians(5.0)));
        doTestSampleAroundPole(aoi, new ConstantAzimuthAiming(ellipsoid, 0.0), -1);
    }

    @Test
    public void testSampleAroundPoleDivertedSingularity() {
        SphericalPolygonsSet aoi = new SphericalPolygonsSet(1.e-9,
                                                            new S2Point(FastMath.toRadians(-120.0), FastMath.toRadians(5.0)),
                                                            new S2Point(FastMath.toRadians(   0.0), FastMath.toRadians(5.0)),
                                                            new S2Point(FastMath.toRadians( 120.0), FastMath.toRadians(5.0)));
        doTestSampleAroundPole(aoi, new DivertedSingularityAiming(aoi), 993);
    }

    @Test
    public void testIssue1388() throws IOException {

        // Step 1 : define the coordinates :
        //      a. format is [lat, lon]
        //      b. order is counter-clockWise
        //      c. the first coordinates != the last one

        double[][] shapeCoordinates1 = {
            { 18.52684751402596,  -76.97880893719434 },
            { 18.451108862175584, -76.99484778988442 },
            { 18.375369256045143, -77.01087679277504 },
            { 18.299628701801268, -77.02689599675749 },
            { 18.223887203723567, -77.04290545732495 },
            { 18.148144771769385, -77.05890521550272 },
            { 18.072401410187016, -77.0748953260324  },
            { 17.99665712514784,  -77.09087584010511 },
            { 17.92091192468999,  -77.10684680260262 },
            { 17.84516581113023,  -77.12280827309398 },
            { 17.769418792522664, -77.13876029654094 },
            { 17.747659863099422, -77.14334087084347 },
            { 17.67571798336192,  -77.15846791369165 },
            { 17.624293265977183, -77.16928381433733 },
            { 17.5485398681768,   -77.18520934447962 },
            { 17.526779103104783, -77.1897823275402  },
            { 17.49650619905315,  -77.0342031192472  },
            { 17.588661518962343, -77.01473903648854 },
            { 17.728574326965138, -76.98517769352242 },
            { 17.80416324015021,  -76.96919708557023 },
            { 17.87969526622326,  -76.95321858415689 },
            { 17.955280973332677, -76.93721874766547 },
            { 18.030855567607098, -76.92121123297645 },
            { 18.106414929680927, -76.90519686376611 },
            { 18.182031502555215, -76.88916022728444 },
            { 18.257597934434987, -76.87312403715188 },
            { 18.3331742522667,   -76.85707550881591 },
            { 18.408750874895002, -76.84101662269072 },
            { 18.57249082100609,  -76.80620195239239 },
            { 18.602585205425896, -76.96276018365842 }
        };

        double[][] shapeCoordinates2 = {
            { 18.338614038907608, -78.37885677406668 },
            { 18.195574802144037, -78.24425107003432 },
            { 18.20775293886321,  -78.0711865934217  },
            { 18.07679345301507,  -77.95901517339438 },
            { 18.006705181057598, -77.85325354879791 },
            { 17.857293838883137, -77.73787723105598 },
            { 17.854243316622103, -77.57442744758828 },
            { 17.875595873376014, -77.38213358468467 },
            { 17.72607423578937,  -77.23470828979222 },
            { 17.71386286451302,  -77.12253686976543 },
            { 17.790170276013725, -77.14817605148616 },
            { 17.869495404611797, -77.14497115377101 },
            { 17.854243309397717, -76.9302429967729  },
            { 17.954882874700132, -76.84371075846688 },
            { 17.94268718313505,  -76.6898756681441  },
            { 17.869495397388064, -76.54886016868198 },
            { 17.863394719203555, -76.35015651034861 },
            { 17.93049065091843,  -76.23478019260665 },
            { 18.155989976776553, -76.32451732862788 },
            { 18.22601854027039,  -76.63218750927341 },
            { 18.33861403170316,  -76.85653034932697 },
            { 18.405527980074993, -76.97831646249921 },
            { 18.4541763474828,   -77.28598664314421 },
            { 18.496732365966466, -77.705828243816   },
            { 18.451136227912485, -78.00708862903122 },
            { 18.405527980074993, -78.25707065080552 }
        };

        double[][] shapeCoordinates3 = {
            { 18.52684751402596,  -76.97880893719434 },
            { 18.451108862175584, -76.99484778988442 },
            { 18.375369256045143, -77.01087679277504 },
            { 18.299628701801268, -77.02689599675749 },
            { 18.223887203723567, -77.04290545732495 },
            { 18.148144771769385, -77.05890521550272 },
            { 18.072401410187016, -77.0748953260324  },
            { 17.99665712514784,  -77.09087584010511 },
            { 17.92091192468999,  -77.10684680260262 },
            { 17.84516581113023,  -77.12280827309398 },
            { 17.769418792522664, -77.13876029654094 },
            { 17.747659863099422, -77.14334087084347 },
            { 17.67571798336192,  -77.15846791369165 },
            { 17.624293265977183, -77.16928381433733 },
            { 17.5485398681768,   -77.18520934447962 },
            { 17.526779103104783, -77.1897823275402  },
            { 17.49650619905315,  -77.0342031192472  },
            { 17.588661518962343, -77.01473903648854 },
            { 17.728574326965138, -76.98517769352242 },
            { 17.80416324015021,  -76.96919708557023 },
            { 17.87969526622326,  -76.95321858415689 },
            { 17.955280973332677, -76.93721874766547 },
            { 18.030855567607098, -76.92121123297645 },
            { 18.106414929680927, -76.90519686376611 },
            { 18.182031502555215, -76.88916022728444 },
            { 18.257597934434987, -76.87312403715188 },
            { 18.3331742522667,   -76.85707550881591 },
            { 18.408750874895002, -76.84101662269072 },
            { 18.57249082100609,  -76.80620195239239 },
            { 18.602585205425896, -76.96276018365842 }
        };

        double[][] shapeCoordinates4 = {
            { 18.338614038907608, -78.37885677406668 },
            { 18.195574802144037, -78.24425107003432 },
            { 18.20775293886321,  -78.0711865934217  },
            { 18.07679345301507,  -77.95901517339438 },
            { 18.006705181057598, -77.85325354879791 },
            { 17.857293838883137, -77.73787723105598 },
            { 17.854243316622103, -77.57442744758828 },
            { 17.875595873376014, -77.38213358468467 },
            { 17.72607423578937,  -77.23470828979222 },
            { 17.71386286451302,  -77.12253686976543 },
            { 17.790170276013725, -77.14817605148616 },
            { 17.869495404611797, -77.14497115377101 },
            { 17.854243309397717, -76.9302429967729  },
            { 17.954882874700132, -76.84371075846688 },
            { 17.94268718313505,  -76.6898756681441  },
            { 17.869495397388064, -76.54886016868198 },
            { 17.863394719203555, -76.35015651034861 },
            { 17.93049065091843,  -76.23478019260665 },
            { 18.155989976776553, -76.32451732862788 },
            { 18.22601854027039,  -76.63218750927341 },
            { 18.33861403170316,  -76.85653034932697 },
            { 18.405527980074993, -76.97831646249921 },
            { 18.4541763474828,   -77.28598664314421 },
            { 18.496732365966466, -77.705828243816   },
            { 18.451136227912485, -78.00708862903122 },
            { 18.405527980074993, -78.25707065080552 }
        };

        // Step 3 : Build the SphericalPolygonsSet
        SphericalPolygonsSet shape1 = EllipsoidTessellator.buildSimpleZone(1e-10, coordinatesToRadians(shapeCoordinates1));
        SphericalPolygonsSet shape2 = EllipsoidTessellator.buildSimpleZone(1e-10, coordinatesToRadians(shapeCoordinates2));
        SphericalPolygonsSet shape3 = EllipsoidTessellator.buildSimpleZone(1e-10, coordinatesToRadians(shapeCoordinates3));
        SphericalPolygonsSet shape4 = EllipsoidTessellator.buildSimpleZone(1e-10, coordinatesToRadians(shapeCoordinates4));

        // Step 4 : Make the intersection
        Region<Sphere2D> intersection = new RegionFactory<Sphere2D>().intersection(shape2, shape1);
        Region<Sphere2D> intersection2 = new RegionFactory<Sphere2D>().intersection(shape3, shape4);

        // Step 5 : Plot the intersection between shape1 and shape2 to shows the issue
        final ProcessBuilder pb = new ProcessBuilder("gnuplot").
                                  redirectOutput(ProcessBuilder.Redirect.INHERIT).
                                  redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.environment().remove("XDG_SESSION_TYPE");
        final String output = null;// = "/tmp";
        final Process gnuplot = pb.start();
        try (PrintStream out = new PrintStream(gnuplot.getOutputStream(), false, StandardCharsets.UTF_8.name())) {
            final File outputFile;
            if (output == null) {
                out.format(Locale.US, "set terminal qt size %d, %d title 'issue 1388'%n", 1000, 1000);
                outputFile = null;
            } else {
                out.format(Locale.US, "set terminal pngcairo size %d, %d%n", 1000, 1000);
                outputFile = new File(output, "issue-1388.png");
                out.format(Locale.US, "set output '%s'%n", outputFile.getAbsolutePath());
            }
            out.format(Locale.US, "set xlabel 'longitude'%n");
            out.format(Locale.US, "set ylabel 'latitude'%n");
            print(out, "$shape1", shape3);
            print(out, "$shape2", shape4);
            print(out, "$intersection", (SphericalPolygonsSet) intersection);
            out.format(Locale.US, "plot $shape1 using 1:2 with lines title 'shape1',\\%n");
            out.format(Locale.US, "     $shape2 using 1:2 with lines title 'shape2',\\%n");
            out.format(Locale.US, "     $intersection using 1:2 with linespoints lw 2 title 'intersection'%n");
            if (output == null) {
                out.format(Locale.US, "pause mouse close%n");
            } else {
                System.out.println(outputFile + " written");
            }
        }

    }
    private void print(final PrintStream out, final String dataName, final SphericalPolygonsSet shape) {
        out.format(Locale.US, "%s <<EOD%n", dataName);
        List<Vertex> loops = shape.getBoundaryLoops();
        boolean following = false;
        for (Vertex first : shape.getBoundaryLoops()) {
            if (following) {
                out.format(Locale.US, "%n%n");
            }
            int count = 0;
            for (Vertex v = first; count == 0 || v != first;
                 v = v.getOutgoing().getEnd()) {
                ++count;
                Edge e = v.getIncoming();
                out.format(Locale.US, "%.3f %.3f%n",
                           FastMath.toDegrees(v.getLocation().getTheta()),
                           90.0 - FastMath.toDegrees(v.getLocation().getPhi()));
            }
            out.format(Locale.US, "%.3f %.3f%n",
                       FastMath.toDegrees(first.getLocation().getTheta()),
                       90.0 - FastMath.toDegrees(first.getLocation().getPhi()));
            following = true;
        }
        out.format(Locale.US, "EOD%n");
    }

            /**
             * Converts coordinates from degrees to radians.
             *
             * @param coordinates The coordinates in degrees.
             * @return The coordinates in radians.
             */
            private static double[][] coordinatesToRadians(double[][] coordinates) {
                // Convert coordinates to radians
                double[][] coordinatesRadian = new double[coordinates.length][2];
                for (int i = 0; i < coordinates.length; i++) {
                    coordinatesRadian[i][0] = FastMath.toRadians(coordinates[i][0]);
                    coordinatesRadian[i][1] = FastMath.toRadians(coordinates[i][1]);
                }
                return coordinatesRadian;
            }

    private void doTestSampleAroundPole(final SphericalPolygonsSet aoi, final TileAiming aiming, final int expectedNodes) {
        EllipsoidTessellator tessellator = new EllipsoidTessellator(ellipsoid, aiming, 1);
        try {
            List<List<GeodeticPoint>> sampledZone = tessellator.sample(aoi, 20000.0, 20000.0);
            if (expectedNodes < 0) {
                Assertions.fail("an exception should have been thrown");
            } else {
                Assertions.assertEquals(1,             sampledZone.size());
                Assertions.assertEquals(expectedNodes, sampledZone.get(0).size());
            }
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CANNOT_COMPUTE_AIMING_AT_SINGULAR_POINT, oe.getSpecifier());
        }

    }

    private S2Point toS2Point(final GeodeticPoint point) {
        return new S2Point(point.getLongitude(), 0.5 * FastMath.PI - point.getLatitude());
    }

    @BeforeEach
    public void setUp() {
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
                                  FastMath.PI, PositionAngleType.TRUE,
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
