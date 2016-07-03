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
package org.orekit.propagation.events;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.RegionFactory;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class GeographicZoneDetectorTest {

    @Test
    public void testFrance() throws OrekitException {

        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        GeographicZoneDetector d =
                new GeographicZoneDetector(20.0, 1.e-3, earth, buildFrance(), FastMath.toRadians(0.5)).
                withHandler(new ContinueOnEvent<GeographicZoneDetector>());

        Assert.assertEquals(20.0, d.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-3, d.getThreshold(), 1.0e-15);
        Assert.assertEquals(0.5, FastMath.toDegrees(d.getMargin()), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date,
                                                 Constants.EIGEN5C_EARTH_MU);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit,
                                          Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                          Constants.EIGEN5C_EARTH_MU,
                                          Constants.EIGEN5C_EARTH_C20,
                                          Constants.EIGEN5C_EARTH_C30,
                                          Constants.EIGEN5C_EARTH_C40,
                                          Constants.EIGEN5C_EARTH_C50,
                                          Constants.EIGEN5C_EARTH_C60);

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(date.shiftedBy(10 * Constants.JULIAN_DAY));
        Assert.assertEquals(26, logger.getLoggedEvents().size());

    }

    @Test
    public void testSerialization()
      throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, OrekitException {

        final double r = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final BodyShape earth = new OneAxisEllipsoid(r, Constants.WGS84_EARTH_FLATTENING,
                                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        GeographicZoneDetector d =
                new GeographicZoneDetector(20.0, 1.e-3, earth, buildFrance(), FastMath.toRadians(0.5)).
                withMargin(FastMath.toRadians(0.75)).
                withHandler(new ContinueOnEvent<GeographicZoneDetector>());

        Assert.assertEquals(r, ((OneAxisEllipsoid) d.getBody()).getEquatorialRadius(), 1.0e-12);
        Assert.assertEquals(0.75, FastMath.toDegrees(d.getMargin()), 1.0e-12);
        Assert.assertEquals(5.6807e11, d.getZone().getSize() * r * r, 1.0e9);
        Assert.assertEquals(4.0289e6,  d.getZone().getBoundarySize() * r, 1.0e3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(d);

        Assert.assertTrue(bos.size() > 2100);
        Assert.assertTrue(bos.size() < 2200);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        GeographicZoneDetector deserialized  = (GeographicZoneDetector) ois.readObject();

        Assert.assertEquals(d.getZone().getSize(),         deserialized.getZone().getSize(),         1.0e-3);
        Assert.assertEquals(d.getZone().getBoundarySize(), deserialized.getZone().getBoundarySize(), 1.0e-3);
        Assert.assertEquals(d.getZone().getTolerance(),    deserialized.getZone().getTolerance(),    1.0e-15);
        Assert.assertEquals(d.getMaxCheckInterval(),       deserialized.getMaxCheckInterval(),       1.0e-15);
        Assert.assertEquals(d.getThreshold(),              deserialized.getThreshold(),              1.0e-15);
        Assert.assertEquals(d.getMaxIterationCount(),      deserialized.getMaxIterationCount());

        Assert.assertTrue(new RegionFactory<Sphere2D>().difference(d.getZone(), deserialized.getZone()).isEmpty());

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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

