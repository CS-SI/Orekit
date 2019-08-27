/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.geometry.fov;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class CircularFieldOfViewTest {

    @Test
    public void testNadirNoMargin() {
        doTest(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
               new NadirPointing(orbit.getFrame(), earth),
               189, 3.0, 3.0, 85.3650, 85.3745, 181027.5, 181028.5);
    }

    @Test
    public void testNadirMargin() {
        doTest(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.01),
               new NadirPointing(orbit.getFrame(), earth),
               189, 3.0, 3.0, 85.3650, 85.3745, 181027.5, 181028.5);
    }

    @Test
    public void testRollPitchYaw() {
        doTest(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
               new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                             FastMath.toRadians(10),
                             FastMath.toRadians(20),
                             FastMath.toRadians(5)),
               189, 3.0, 3.0, 48.8582, 59.4238, 1256415.4, 1761324.7);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimb() {
        doTest(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
               new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                             FastMath.toRadians(-10),
                             FastMath.toRadians(-39),
                             FastMath.toRadians(-5)),
               189, 0.3912, 3.0, 0.0, 21.7315, 3431348.7, 5346735.9);
    }

    @Test
    public void testFOVLargerThanEarth() {
        doTest(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(45.0), 0.0),
               new NadirPointing(orbit.getFrame(), earth),
               2546, 40.3505, 40.4655, 0.0, 0.0, 5323032.8, 5347029.8);
    }

    @Test
    public void testFOVAwayFromEarth() {
        CircularFieldOfView fov = new CircularFieldOfView(Vector3D.MINUS_K, FastMath.toRadians(3.0), 0.0);
        Assert.assertEquals(0.0, Vector3D.distance(Vector3D.MINUS_K, fov.getCenter()), 1.0e-15);
        Assert.assertEquals(FastMath.toRadians(3.0), fov.getHalfAperture(), 1.0e-15);
        AttitudeProvider attitude = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                                  FastMath.toRadians(-10),
                                                  FastMath.toRadians(-39),
                                                  FastMath.toRadians(-5));
        Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(attitude);
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.0));
        Transform inertToBody = state.getFrame().getTransformTo(earth.getBodyFrame(), state.getDate());
        Transform fovToBody   = new Transform(state.getDate(),
                                              state.toTransform().getInverse(),
                                              inertToBody);
        List<List<GeodeticPoint>> footprint = fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
        Assert.assertTrue(footprint.isEmpty());
    }

    @Test
    public void testNoFootprintInside() {
        CircularFieldOfView fov = new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Transform fovToBody   = new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(5e6, 3e6, 2e6));
        try {
            fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
        }
    }

    private void doTest(final CircularFieldOfView fov, final AttitudeProvider attitude, final int expectedPoints,
                        final double expectedMinOffset, final double expectedMaxOffset,
                        final double expectedMinElevation, final double expectedMaxElevation,
                        final double expectedMinDist, final double expectedMaxDist) {

        Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(attitude);
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.0));
        Transform inertToBody = state.getFrame().getTransformTo(earth.getBodyFrame(), state.getDate());
        Transform fovToBody   = new Transform(state.getDate(),
                                              state.toTransform().getInverse(),
                                              inertToBody);
        List<List<GeodeticPoint>> footprint = fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
        Vector3D subSat = earth.projectToGround(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                                                state.getDate(), earth.getBodyFrame());
        Assert.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        Assert.assertEquals(expectedPoints, loop.size());
        double minOffset = Double.POSITIVE_INFINITY;
        double maxOffset = 0;
        double minEl     = Double.POSITIVE_INFINITY;
        double maxEl     = 0;
        double minDist   = Double.POSITIVE_INFINITY;
        double maxDist   = 0;
        for (int i = 0; i < loop.size(); ++i) {

            Assert.assertEquals(0.0, loop.get(i).getAltitude(), 9.0e-9);

            Vector3D los = fovToBody.getInverse().transformPosition(earth.transform(loop.get(i)));
            double offset = Vector3D.angle(fov.getCenter(), los);
            minOffset = FastMath.min(minOffset, offset);
            maxOffset = FastMath.max(maxOffset, offset);

            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "onFootprint");
            final double elevation = topo.getElevation(state.getPVCoordinates().getPosition(),
                                                       state.getFrame(), state.getDate());
            if (elevation > 0.001) {
                Assert.assertEquals(-fov.getMargin(), fov.offsetFromBoundary(los), 4.0e-15);
            }
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);

        }

        Assert.assertEquals(expectedMinOffset,     FastMath.toDegrees(minOffset), 0.001);
        Assert.assertEquals(expectedMaxOffset,     FastMath.toDegrees(maxOffset), 0.001);
        Assert.assertEquals(expectedMinElevation,  FastMath.toDegrees(minEl),     0.001);
        Assert.assertEquals(expectedMaxElevation,  FastMath.toDegrees(maxEl),     0.001);
        Assert.assertEquals(expectedMinDist,       minDist,                       1.0);
        Assert.assertEquals(expectedMaxDist,       maxDist,                       1.0);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(7.0e6, 1.0e6, 4.0e6),
                                                     new Vector3D(-500.0, 8000.0, 1000.0)),
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);
    }

    @After
    public void tearDown() {
        earth = null;
        orbit = null;
    }

    private OneAxisEllipsoid earth;
    private Orbit            orbit;
    
}
