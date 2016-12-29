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
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.random.UnitSphereRandomVectorGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
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
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class FieldOfViewTest {

    @Test
    public void testDihedralFielOfView() throws OrekitException {
        double maxError = 0;
        for (double alpha1 = 0; alpha1 < 0.5 * FastMath.PI; alpha1 += 0.1) {
            for (double alpha2 = 0; alpha2 < 0.5 * FastMath.PI; alpha2 += 0.1) {
                FieldOfView fov = new FieldOfView(Vector3D.PLUS_I,
                                                  Vector3D.PLUS_K, alpha1,
                                                  Vector3D.PLUS_J, alpha2,
                                                  0.125);
                double eta = FastMath.acos(FastMath.sin(alpha1) * FastMath.sin(alpha2));
                double theoreticalArea = MathUtils.TWO_PI - 4 * eta;
                double error = theoreticalArea - fov.getZone().getSize();
                maxError = FastMath.max(FastMath.abs(error), maxError);
                Assert.assertEquals(0.125, fov.getMargin(), 1.0e-15);
            }
        }
        Assert.assertEquals(0, maxError, 4.0e-15);
    }

    @Test
    public void testTooWideDihedralFielOfView() throws OrekitException {
        double tooLarge = 1.6;
        try {
            new FieldOfView(Vector3D.PLUS_I,
                            Vector3D.PLUS_K, 0.1,
                            Vector3D.PLUS_J, tooLarge,
                            0.125);
            Assert.fail("an exception should have been thrown");
        } catch(OrekitException oe) {
            Assert.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
            Assert.assertEquals(tooLarge,          (Double) oe.getParts()[0], 1.0e-15);
            Assert.assertEquals(0,                 (Double) oe.getParts()[1], 1.0e-15);
            Assert.assertEquals(0.5 * FastMath.PI, (Double) oe.getParts()[2], 1.0e-15);
        }
    }

    @Test
    public void testSquare() throws OrekitException {
        FieldOfView square1 = new FieldOfView(Vector3D.PLUS_K,
                                              Vector3D.PLUS_I, 0.25,
                                              Vector3D.MINUS_J, 0.25,
                                              0.0);
        FieldOfView square2 = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I, 0.25, 4, 0.0);
        Assert.assertEquals(square1.getZone().getSize(),         square2.getZone().getSize(),         1.0e-15);
        Assert.assertEquals(square1.getZone().getBoundarySize(), square2.getZone().getBoundarySize(), 1.0e-15);
        UnitSphereRandomVectorGenerator random =
                        new UnitSphereRandomVectorGenerator(3, new Well1024a(0x17df21c7598b114bl));
        for (int i = 0; i < 1000; ++i) {
            Vector3D v = new Vector3D(random.nextVector()).scalarMultiply(1.0e6);
            Assert.assertEquals(square1.offsetFromBoundary(v), square2.offsetFromBoundary(v), 1.0e-15);
        }
    }

    @Test
    public void testRegularPolygon() throws OrekitException {
        double delta          = 0.25;
        double margin         = 0.01;
        double maxAreaError   = 0;
        double maxOffsetError = 0;
        for (int n = 3; n < 32; ++n) {
            FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I, delta, n, margin);
            double eta = FastMath.acos(FastMath.sin(FastMath.PI / n) * FastMath.cos(delta));
            double theoreticalArea = 2 * n * eta - (n - 2) * FastMath.PI;
            double areaError = theoreticalArea - fov.getZone().getSize();
            maxAreaError = FastMath.max(FastMath.abs(areaError), maxAreaError);
            for (double lambda = -0.5 * FastMath.PI; lambda < 0.5 * FastMath.PI; lambda += 0.1) {
                Vector3D v = new Vector3D(0.0, lambda).scalarMultiply(1.0e6);
                double theoreticalOffset = 0.5 * FastMath.PI - lambda - delta - margin;
                double offset = fov.offsetFromBoundary(v);
                if (theoreticalOffset > 0.01) {
                    // the offsetFromBoundary method may use the fast approximate
                    // method, so we cannot check the error accurately
                    // we know however that the fast method will underestimate the offset

                    Assert.assertTrue(offset > 0);
                    Assert.assertTrue(offset <= theoreticalOffset + 5e-16);
                } else {
                    double offsetError = theoreticalOffset - offset;
                    maxOffsetError = FastMath.max(FastMath.abs(offsetError), maxOffsetError);
                }
            }
        }
        Assert.assertEquals(0.0, maxAreaError,   5.0e-14);
        Assert.assertEquals(0.0, maxOffsetError, 2.0e-15);
    }

    @Test
    public void testNoFootprintInside() throws OrekitException {
        Utils.setDataRoot("regular-data");
        FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                          FastMath.toRadians(3.0), 6, 0.0);
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

    @Test
    public void testNadirHexagonalFootprint() throws OrekitException {
        Utils.setDataRoot("regular-data");
        FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                          FastMath.toRadians(3.0), 6, 0.0);
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
        List<List<GeodeticPoint>> footprint = fov.getFootprint(fovToBody, earth, FastMath.toRadians(0.1));
        Vector3D subSat = earth.projectToGround(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                                                state.getDate(), earth.getBodyFrame());
        Assert.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        Assert.assertEquals(210, loop.size());
        double minEl   = Double.POSITIVE_INFINITY;
        double maxEl = 0;
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = 0;
        for (int i = 0; i < loop.size(); ++i) {
            Assert.assertEquals(0.0, loop.get(i).getAltitude(), 1.0e-15);
            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "atLimb");
            final double elevation = topo.getElevation(state.getPVCoordinates().getPosition(),
                                                       state.getFrame(), state.getDate());
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);
        }
        Assert.assertEquals(84.6497,  FastMath.toDegrees(minEl), 0.001);
        Assert.assertEquals(85.3729,  FastMath.toDegrees(maxEl), 0.001);
        Assert.assertEquals(181052.2, minDist, 1.0);
        Assert.assertEquals(209092.8, maxDist, 1.0);
    }

    @Test
    public void testRollPitchYawHexagonalFootprint() throws OrekitException {
        Utils.setDataRoot("regular-data");
        FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                          FastMath.toRadians(3.0), 6, 0.0);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(7.0e6, 1.0e6, 4.0e6),
                                                                    new Vector3D(-500.0, 8000.0, 1000.0)),
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  Constants.EIGEN5C_EARTH_MU);
        Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                                     FastMath.toRadians(10),
                                                     FastMath.toRadians(20),
                                                     FastMath.toRadians(5)));
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
        Assert.assertEquals(210, loop.size());
        double minEl   = Double.POSITIVE_INFINITY;
        double maxEl = 0;
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = 0;
        for (int i = 0; i < loop.size(); ++i) {
            Assert.assertEquals(0.0, loop.get(i).getAltitude(), 1.0e-15);
            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "atLimb");
            final double elevation = topo.getElevation(state.getPVCoordinates().getPosition(),
                                                       state.getFrame(), state.getDate());
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);
        }
        Assert.assertEquals(48.0026,   FastMath.toDegrees(minEl), 0.001);
        Assert.assertEquals(60.1975,   FastMath.toDegrees(maxEl), 0.001);
        Assert.assertEquals(1221543.6, minDist, 1.0);
        Assert.assertEquals(1804921.6, maxDist, 1.0);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimb() throws OrekitException {
        Utils.setDataRoot("regular-data");
        FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                          FastMath.toRadians(40.0), 6, 0.0);
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
        Vector3D subSat = earth.projectToGround(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                                                state.getDate(), earth.getBodyFrame());
        Assert.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        Assert.assertEquals(246, loop.size());
        double minEl   = Double.POSITIVE_INFINITY;
        double maxEl = 0;
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = 0;
        for (int i = 0; i < loop.size(); ++i) {
            Assert.assertEquals(0.0, loop.get(i).getAltitude(), 3.0e-7);
            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "atLimb");
            final double elevation = topo.getElevation(state.getPVCoordinates().getPosition(),
                                                       state.getFrame(), state.getDate());
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);
        }
        Assert.assertEquals(0.0,       FastMath.toDegrees(minEl), 2.0e-12);
        Assert.assertEquals(7.8897,    FastMath.toDegrees(maxEl), 0.001);
        Assert.assertEquals(4584829.6, minDist, 1.0);
        Assert.assertEquals(5347029.8, maxDist, 1.0);
    }

    @Test
    public void testFOVLargerThanEarth() throws OrekitException {
        Utils.setDataRoot("regular-data");
        FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
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
        Vector3D subSat = earth.projectToGround(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                                                state.getDate(), earth.getBodyFrame());
        Assert.assertEquals(1, footprint.size());
        List<GeodeticPoint> loop = footprint.get(0);
        Assert.assertEquals(234, loop.size());
        double minEl   = Double.POSITIVE_INFINITY;
        double maxEl = 0;
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = 0;
        for (int i = 0; i < loop.size(); ++i) {
            Assert.assertEquals(0.0, loop.get(i).getAltitude(), 3.0e-7);
            TopocentricFrame topo = new TopocentricFrame(earth, loop.get(i), "atLimb");
            final double elevation = topo.getElevation(state.getPVCoordinates().getPosition(),
                                                       state.getFrame(), state.getDate());
            minEl = FastMath.min(minEl, elevation);
            maxEl = FastMath.max(maxEl, elevation);
            final double dist = Vector3D.distance(subSat, earth.transform(loop.get(i)));
            minDist = FastMath.min(minDist, dist);
            maxDist = FastMath.max(maxDist, dist);
        }
        Assert.assertEquals(0.0,       FastMath.toDegrees(minEl), 2.0e-12);
        Assert.assertEquals(0.0,       FastMath.toDegrees(maxEl), 1.7e-12);
        Assert.assertEquals(5323036.6, minDist, 1.0);
        Assert.assertEquals(5347029.8, maxDist, 1.0);
    }

    @Test
    public void testFOVAwayFromEarth() throws OrekitException {
        Utils.setDataRoot("regular-data");
        FieldOfView fov = new FieldOfView(Vector3D.MINUS_K, Vector3D.PLUS_I,
                                          FastMath.toRadians(3.0), 6, 0.0);
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
        Assert.assertEquals(0, footprint.size());
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        FieldOfView fov = new FieldOfView(new SphericalPolygonsSet(1.0e-12,
                                                                   new S2Point(Vector3D.PLUS_I),
                                                                   new S2Point(Vector3D.PLUS_J),
                                                                   new S2Point(Vector3D.PLUS_K)),
                                          0.001);
        Assert.assertEquals(0.5 * FastMath.PI, fov.getZone().getSize(),         1.0e-15);
        Assert.assertEquals(1.5 * FastMath.PI, fov.getZone().getBoundarySize(), 1.0e-15);
        Assert.assertEquals(0.001,  fov.getMargin(), 1.0e-15);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(fov);


        Assert.assertTrue(bos.size() > 400);
        Assert.assertTrue(bos.size() < 450);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        FieldOfView deserialized  = (FieldOfView) ois.readObject();
        Assert.assertEquals(0.5 * FastMath.PI, deserialized.getZone().getSize(),         1.0e-15);
        Assert.assertEquals(1.5 * FastMath.PI, deserialized.getZone().getBoundarySize(), 1.0e-15);
        Assert.assertEquals(0.001,  deserialized.getMargin(), 1.0e-15);

    }

}
