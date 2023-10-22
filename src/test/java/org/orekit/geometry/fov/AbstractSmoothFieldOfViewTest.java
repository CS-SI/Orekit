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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UnitSphereRandomVectorGenerator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
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

public abstract class AbstractSmoothFieldOfViewTest {

    protected void doTestFOVAwayFromEarth(final SmoothFieldOfView fov, final AttitudeProvider attitude,
                                          final Vector3D expectedCenter) {
        Assertions.assertEquals(0.0, Vector3D.distance(expectedCenter, fov.getCenter()), 1.0e-15);
        Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(attitude);
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.0));
        Transform inertToBody = state.getFrame().getTransformTo(earth.getBodyFrame(), state.getDate());
        Transform fovToBody   = new Transform(state.getDate(),
                                              state.toTransform().getInverse(),
                                              inertToBody);
        List<List<GeodeticPoint>> footprint = fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
        Assertions.assertTrue(footprint.isEmpty());
    }

    protected void doTestNoFootprintInside(final SmoothFieldOfView fov, final Transform fovToBody) {
        try {
            fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
        }
    }

    protected void doTestFootprint(final SmoothFieldOfView fov, final AttitudeProvider attitude,
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
        Vector3D subSat = earth.projectToGround(state.getPosition(earth.getBodyFrame()),
                                                state.getDate(), earth.getBodyFrame());
        Assertions.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        double minOffset = Double.POSITIVE_INFINITY;
        double maxOffset = 0;
        double minEl     = Double.POSITIVE_INFINITY;
        double maxEl     = 0;
        double minDist   = Double.POSITIVE_INFINITY;
        double maxDist   = 0;
        for (int i = 0; i < loop.size(); ++i) {

            Assertions.assertEquals(0.0, loop.get(i).getAltitude(), 9.0e-9);

            Vector3D los = fovToBody.toStaticTransform().getInverse().transformPosition(earth.transform(loop.get(i)));
            double offset = Vector3D.angle(fov.getCenter(), los);
            minOffset = FastMath.min(minOffset, offset);
            maxOffset = FastMath.max(maxOffset, offset);

            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "onFootprint");
            final double elevation = topo.getTrackingCoordinates(state.getPosition(),
                                                                 state.getFrame(),
                                                                 state.getDate()).
                                     getElevation();
            if (elevation > 0.001) {
                Assertions.assertEquals(-fov.getMargin(),
                                    fov.offsetFromBoundary(los, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                    2.0e-10);
                Assertions.assertEquals(0.1 - fov.getMargin(),
                                    fov.offsetFromBoundary(los, 0.1, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                    2.0e-10);
                Assertions.assertEquals(-0.1 - fov.getMargin(),
                                    fov.offsetFromBoundary(los, 0.1, VisibilityTrigger.VISIBLE_AS_SOON_AS_PARTIALLY_IN_FOV),
                                    2.0e-10);
            }
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);

        }

        Assertions.assertEquals(expectedMinOffset,     FastMath.toDegrees(minOffset), 0.001);
        Assertions.assertEquals(expectedMaxOffset,     FastMath.toDegrees(maxOffset), 0.001);
        Assertions.assertEquals(expectedMinElevation,  FastMath.toDegrees(minEl),     0.001);
        Assertions.assertEquals(expectedMaxElevation,  FastMath.toDegrees(maxEl),     0.001);
        Assertions.assertEquals(expectedMinDist,       minDist,                       1.0);
        Assertions.assertEquals(expectedMaxDist,       maxDist,                       1.0);

    }

    protected void doTestBoundary(final SmoothFieldOfView fov, final RandomGenerator random,
                                  final double tol) {
        UnitSphereRandomVectorGenerator spGenerator = new UnitSphereRandomVectorGenerator(3, random);
        for (int i = 0; i < 1000; ++i) {
            Vector3D queryLOS = new Vector3D(spGenerator.nextVector());
            Vector3D closest = fov.projectToBoundary(queryLOS);
            Assertions.assertEquals(-fov.getMargin(),
                                fov.offsetFromBoundary(closest, 0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                tol);
        }
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

    protected OneAxisEllipsoid earth;
    protected Orbit            orbit;

}
