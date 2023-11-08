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
package org.orekit.geometry.fov;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.orekit.geometry.fov.PolygonalFieldOfView.DefiningConeType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.VisibilityTrigger;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.List;

public class PolygonalFieldOfViewTest {

    @Test
    public void testRegularPolygon() {
        double delta          = 0.25;
        double margin         = 0.01;
        double maxAreaError   = 0;
        double maxOffsetError = 0;
        for (int n = 3; n < 32; ++n) {
            PolygonalFieldOfView base = new PolygonalFieldOfView(Vector3D.PLUS_K,
                                                                 DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                                                 Vector3D.PLUS_I, delta, n, margin);
            PolygonalFieldOfView fov  = new PolygonalFieldOfView(base.getZone(), margin);
            double eta = FastMath.acos(FastMath.sin(FastMath.PI / n) * FastMath.cos(delta));
            double theoreticalArea = 2 * n * eta - (n - 2) * FastMath.PI;
            double areaError = theoreticalArea - fov.getZone().getSize();
            maxAreaError = FastMath.max(FastMath.abs(areaError), maxAreaError);
            for (double lambda = -0.5 * FastMath.PI; lambda < 0.5 * FastMath.PI; lambda += 0.1) {
                Vector3D v = new Vector3D(0.0, lambda).scalarMultiply(1.0e6);
                double theoreticalOffset = 0.5 * FastMath.PI - lambda - delta - margin;
                double offset = fov.offsetFromBoundary(v, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV);
                if (theoreticalOffset > 0.01) {
                    // the offsetFromBoundary method may use the fast approximate
                    // method, so we cannot check the error accurately
                    // we know however that the fast method will underestimate the offset

                    Assertions.assertTrue(offset > 0);
                    Assertions.assertTrue(offset <= theoreticalOffset + 5e-16);
                } else {
                    double offsetError = theoreticalOffset - offset;
                    maxOffsetError = FastMath.max(FastMath.abs(offsetError), maxOffsetError);
                }
                Assertions.assertEquals(-margin,
                                    fov.offsetFromBoundary(fov.projectToBoundary(v), 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                    1.0e-12);
            }
        }
        Assertions.assertEquals(0.0, maxAreaError,   5.0e-14);
        Assertions.assertEquals(0.0, maxOffsetError, 2.0e-15);
    }

    @Test
    public void testNoFootprintInside() {
        Utils.setDataRoot("regular-data");
        PolygonalFieldOfView fov = new PolygonalFieldOfView(Vector3D.PLUS_K,
                                                            DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                                            Vector3D.PLUS_I,
                                                            FastMath.toRadians(3.0), 6, 0.0);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Transform fovToBody   = new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(5e6, 3e6, 2e6));
        try {
            fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
        }
    }

    @Test
    public void testNadirHexagonalFootprint() {
        doTest(new PolygonalFieldOfView(Vector3D.PLUS_K,
                                        DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                        Vector3D.PLUS_I,
                                        FastMath.toRadians(3.0), 6, 0.0),
               new NadirPointing(orbit.getFrame(), earth),
               210, 84.6497, 85.3729, 181052.2, 209092.8);
    }

    @Test
    public void testRollPitchYawHexagonalFootprint() {
        doTest(new PolygonalFieldOfView(Vector3D.PLUS_K,
                                        DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                        Vector3D.PLUS_I,
                                        FastMath.toRadians(3.0), 6, 0.0),
               new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                             FastMath.toRadians(10),
                             FastMath.toRadians(20),
                             FastMath.toRadians(5)),
               210, 48.0026, 60.1975, 1221543.6, 1804921.6);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimb() {
        doTest(new PolygonalFieldOfView(Vector3D.PLUS_K,
                                        DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                        Vector3D.PLUS_I,
                                        FastMath.toRadians(40.0), 6, 0.0),
               new NadirPointing(orbit.getFrame(), earth),
               2448, 0.0, 7.9089, 4583054.6, 5347029.8);
    }

    @Test
    public void testFOVLargerThanEarth() {
        doTest(new PolygonalFieldOfView(Vector3D.PLUS_K,
                                        DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                        Vector3D.PLUS_I,
                                        FastMath.toRadians(45.0), 6, 0.0),
               new NadirPointing(orbit.getFrame(), earth),
               2337, 0.0, 0.0, 5323032.8, 5347029.8);
    }

    @Test
    public void testFOVLargerThanEarthOld() {
        Utils.setDataRoot("regular-data");
        PolygonalFieldOfView fov = new PolygonalFieldOfView(Vector3D.PLUS_K,
                                                            DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                                            Vector3D.PLUS_I,
                                                            FastMath.toRadians(45.0), 6, 0.0);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(7.0e6, 1.0e6, 4.0e6),
                                                                    new Vector3D(-500.0, 8000.0, 1000.0)),
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  Constants.EIGEN5C_EARTH_MU);
        Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(new NadirPointing(orbit.getFrame(), earth));
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.0));
        Transform inertToBody = state.getFrame().getTransformTo(earth.getBodyFrame(), state.getDate());
        Transform fovToBody   = new Transform(state.getDate(),
                                              state.toTransform().getInverse(),
                                              inertToBody);
        List<List<GeodeticPoint>> footprint = fov.getFootprint(fovToBody, earth, FastMath.toRadians(1.0));
        Vector3D subSat = earth.projectToGround(state.getPosition(earth.getBodyFrame()),
                                                state.getDate(), earth.getBodyFrame());
        Assertions.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        Assertions.assertEquals(234, loop.size());
        double minEl   = Double.POSITIVE_INFINITY;
        double maxEl = 0;
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = 0;
        for (int i = 0; i < loop.size(); ++i) {
            Assertions.assertEquals(0.0, loop.get(i).getAltitude(), 3.0e-7);
            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "atLimb");
            final double elevation = topo.
                                     getTrackingCoordinates(state.getPosition(), state.getFrame(), state.getDate()).
                                     getElevation();
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);
        }
        Assertions.assertEquals(0.0,       FastMath.toDegrees(minEl), 2.0e-12);
        Assertions.assertEquals(0.0,       FastMath.toDegrees(maxEl), 1.7e-12);
        Assertions.assertEquals(5323036.6, minDist, 1.0);
        Assertions.assertEquals(5347029.8, maxDist, 1.0);
    }

    @Test
    public void testFOVAwayFromEarth() {
        PolygonalFieldOfView fov = new PolygonalFieldOfView(Vector3D.MINUS_K,
                                                            DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                                            Vector3D.PLUS_I,
                                                            FastMath.toRadians(3.0), 6, 0.0);
        Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(new NadirPointing(orbit.getFrame(), earth));
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.0));
        Transform inertToBody = state.getFrame().getTransformTo(earth.getBodyFrame(), state.getDate());
        Transform fovToBody   = new Transform(state.getDate(),
                                              state.toTransform().getInverse(),
                                              inertToBody);
        List<List<GeodeticPoint>> footprint = fov.getFootprint(fovToBody, earth, FastMath.toRadians(1.0));
        Assertions.assertEquals(0, footprint.size());
    }

    private void doTest(final PolygonalFieldOfView fov, final AttitudeProvider attitude, final int expectedPoints,
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
        Vector3D subSat = earth.projectToGround(state.getPosition(earth.getBodyFrame()),
                                                state.getDate(), earth.getBodyFrame());
        Assertions.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        Assertions.assertEquals(expectedPoints, loop.size());
        double minEl     = Double.POSITIVE_INFINITY;
        double maxEl     = 0;
        double minDist   = Double.POSITIVE_INFINITY;
        double maxDist   = 0;
        for (int i = 0; i < loop.size(); ++i) {

            Assertions.assertEquals(0.0, loop.get(i).getAltitude(), 9.0e-9);

            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "onFootprint");
            final double elevation = topo.
                                     getTrackingCoordinates(state.getPosition(), state.getFrame(), state.getDate()).
                                     getElevation();
            if (elevation > 0.001) {
                Vector3D los = fovToBody.toStaticTransform().getInverse().transformPosition(earth.transform(loop.get(i)));
                Assertions.assertEquals(-fov.getMargin(),
                                    fov.offsetFromBoundary(los, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                    4.0e-15);
                Assertions.assertEquals(0.125 - fov.getMargin(),
                                    fov.offsetFromBoundary(los, 0.125, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                    4.0e-15);
                Assertions.assertEquals(-0.125 - fov.getMargin(),
                                    fov.offsetFromBoundary(los, 0.125, VisibilityTrigger.VISIBLE_AS_SOON_AS_PARTIALLY_IN_FOV),
                                    4.0e-15);
            }
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);

        }

        Assertions.assertEquals(expectedMinElevation, FastMath.toDegrees(minEl), 0.001);
        Assertions.assertEquals(expectedMaxElevation, FastMath.toDegrees(maxEl), 0.001);
        Assertions.assertEquals(expectedMinDist,      minDist,                   1.0);
        Assertions.assertEquals(expectedMaxDist,      maxDist,                   1.0);

    }

    @BeforeEach
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

    @AfterEach
    public void tearDown() {
        earth = null;
        orbit = null;
    }

    private OneAxisEllipsoid earth;
    private Orbit            orbit;

}
