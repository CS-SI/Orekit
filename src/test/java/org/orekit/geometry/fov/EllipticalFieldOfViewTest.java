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
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.random.UnitSphereRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.Ellipse;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.propagation.events.VisibilityTrigger;
import org.orekit.time.AbsoluteDate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EllipticalFieldOfViewTest extends AbstractSmoothFieldOfViewTest {

    @Test
    public void testPlanarProjection() {

        EllipticalFieldOfView fov = new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                              FastMath.toRadians(40.0), FastMath.toRadians(10.0),
                                                              0.0);


        // test direction
        final Vector3D d         = new Vector3D(0.4, 0.8, 0.2).normalize();

        // plane ellipse
        final Ellipse  ellipse   = new Ellipse(fov.getZ(), fov.getX(), fov.getY(),
                                               FastMath.tan(fov.getHalfApertureAlongX()),
                                               FastMath.tan(fov.getHalfApertureAlongY()),
                                               FramesFactory.getGCRF());
        final Vector3D projected   = new Vector3D(1.0 / d.getZ(), d);
        final Vector3D closestProj = ellipse.toSpace(ellipse.projectToEllipse(ellipse.toPlane(projected)));

        // the closest point to the planar project ellipse belongs to the ellipse on the sphere
        Assertions.assertEquals(0.0,
                            fov.offsetFromBoundary(closestProj, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                            2.0e-15);

        // approximate computation of closest point on the ellipse on the sphere
        Vector3D closestSphere = null;
        for (double eta = 0; eta < MathUtils.TWO_PI; eta += 0.0001) {
            Vector3D p = fov.directionAt(eta);
            if (closestSphere == null || Vector3D.angle(p, d) < Vector3D.angle(closestSphere, d)) {
                closestSphere = p;
            }
        }
        Assertions.assertEquals(0.0,
                            fov.offsetFromBoundary(closestSphere, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                            2.0e-15);

        // computing the closest point to the planar project ellipse
        // does NOT give the closest point on the ellipse on the sphere
        Assertions.assertEquals(Vector3D.angle(closestProj, d) - 0.0056958,
                            Vector3D.angle(closestSphere, d),
                            1.0e-7);

    }

    @Test
    public void testFocalPoints() {

        EllipticalFieldOfView fov = new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                              FastMath.toRadians(40.0), FastMath.toRadians(10.0),
                                                              0.0);

        // find the angular foci of the ellipse
        final Vector3D f1Sphere  = fov.getFocus1();
        final Vector3D f2Sphere  = fov.getFocus2();

        // find the planar foci in the (XY) plane
        Vector2D f1Plane = new Vector2D(f1Sphere.getX() * FastMath.cos(fov.getHalfApertureAlongY()), 0.0);
        Vector2D f2Plane = new Vector2D(-f1Plane.getX(), f1Plane.getY());

        // the focal points on plane are NOT the projection of the focal points on sphere
        Assertions.assertTrue(f1Sphere.getX() - f1Plane.getX() > +0.0095);
        Assertions.assertTrue(f2Sphere.getX() - f2Plane.getX() < -0.0095);

        // find the constant sum of the distances to foci
        final double angularDist = 2 * fov.getHalfApertureAlongX();
        final double d = 2 * FastMath.sin(fov.getHalfApertureAlongX());

        for (double angle = 0; angle < MathUtils.TWO_PI; angle += 0.001) {

            // sum of angular distances on sphere is constant
            final Vector3D pSphere = fov.directionAt(angle);
            Assertions.assertEquals(angularDist, Vector3D.angle(pSphere, f1Sphere) + Vector3D.angle(pSphere, f2Sphere), 1.0e-14);

            // sum of Cartesian distances projected on plane is constant
            final Vector2D pPlane = new Vector2D(pSphere.getX(), pSphere.getY());
            Assertions.assertEquals(d, Vector2D.distance(pPlane, f1Plane) + Vector2D.distance(pPlane, f2Plane), 5.0e-16);

        }

    }

    @Test
    public void testDirectionFromDistances()
        throws NoSuchMethodException, SecurityException, IllegalAccessException,
               IllegalArgumentException, InvocationTargetException {

        final EllipticalFieldOfView fov = new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                                    FastMath.toRadians(40.0), FastMath.toRadians(10.0),
                                                                    0.0);
        final Vector3D f1Sphere  = fov.getFocus1();
        final Vector3D f2Sphere  = fov.getFocus2();
        final double   a         = FastMath.max(fov.getHalfApertureAlongX(), fov.getHalfApertureAlongY());
        final double   delta     = Vector3D.angle(f1Sphere, f2Sphere);
        final double   dMin      = a - delta / 2;
        final double   dMax      = a + delta / 2;

        Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt",
                                                                           Double.TYPE, Double.TYPE, Double.TYPE);
        directionAt.setAccessible(true);
        for (double d1 = dMin; d1 <= dMax; d1 += 0.001) {
            final double d2 = 2 * a - d1;
            final Vector3D dPlus = (Vector3D) directionAt.invoke(fov, d1, d2, +1.0);
            Assertions.assertEquals(d1, Vector3D.angle(dPlus, f1Sphere), 2.0e-14);
            Assertions.assertEquals(d2, Vector3D.angle(dPlus, f2Sphere), 2.0e-14);
            Assertions.assertEquals(0.0,
                                fov.offsetFromBoundary(dPlus, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                1.0e-13);
            final Vector3D dMinus = (Vector3D) directionAt.invoke(fov, d1, d2, -1.0);
            Assertions.assertEquals(d1, Vector3D.angle(dMinus, f1Sphere), 2.0e-14);
            Assertions.assertEquals(d2, Vector3D.angle(dMinus, f2Sphere), 2.0e-14);
            Assertions.assertEquals(0.0,
                                fov.offsetFromBoundary(dPlus, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                1.0e-13);

        }

    }

    @Test
    public void testNadirNoMargin() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0),
                        new NadirPointing(orbit.getFrame(), earth),
                        2.0, 4.0, 83.8280, 86.9120, 120567.3, 241701.8);
    }

    @Test
    public void testNadirMargin() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.01),
                        new NadirPointing(orbit.getFrame(), earth),
                        2.0, 4.0, 83.8280, 86.9120, 120567.3, 241701.8);
    }

    @Test
    public void testRollPitchYaw() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0),
                        new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                      FastMath.toRadians(10),
                                      FastMath.toRadians(20),
                                      FastMath.toRadians(5)),
                        2.0, 4.0, 47.7675, 60.2403, 1219597.1, 1817011.0);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimb() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0),
                        new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                      FastMath.toRadians(-10),
                                      FastMath.toRadians(-39),
                                      FastMath.toRadians(-5)),
                        0.3899, 4.0, 0.0, 24.7014, 3213727.9, 5346638.0);
    }

    @Test
    public void testFOVLargerThanEarth() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(50.0), FastMath.toRadians(45.0),
                                                  0.0),
                        new NadirPointing(orbit.getFrame(), earth),
                        40.3505, 40.4655, 0.0, 0.0, 5323032.8, 5347029.8);
    }

    @Test
    public void testFOVAwayFromEarth() {
        doTestFOVAwayFromEarth(new EllipticalFieldOfView(Vector3D.MINUS_K, Vector3D.PLUS_I,
                                                         FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                         0.0),
                               new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                             FastMath.toRadians(-10),
                                             FastMath.toRadians(-39),
                                             FastMath.toRadians(-5)),
                               Vector3D.MINUS_K);
    }

    @Test
    public void testNoFootprintInside() {
        doTestNoFootprintInside(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                          FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                          0.0),
                                new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(5e6, 3e6, 2e6)));
    }

    @Test
    public void testConventionsTangentPoints()
        throws NoSuchMethodException, SecurityException, IllegalAccessException,
               IllegalArgumentException, InvocationTargetException {
        Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
        directionAt.setAccessible(true);
        final EllipticalFieldOfView ang  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                     FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                     0.0);
        final EllipticalFieldOfView cart = new EllipticalFieldOfView(ang.getCenter(), ang.getX(),
                                                                     ang.getHalfApertureAlongX(), ang.getHalfApertureAlongY(),
                                                                     ang.getMargin());
        for (int i = 0; i < 4; ++i) {
            final double theta = i * 0.5 * FastMath.PI;
            final Vector3D pAng  = (Vector3D) directionAt.invoke(ang, theta);
            final Vector3D pCart = (Vector3D) directionAt.invoke(cart, theta);
            Assertions.assertEquals(0.0, Vector3D.angle(pAng, pCart), 1.0e-15);
        }
    }

    @Test
    public void testPointsOnBoundary() {
        doTestPointsOnBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                         FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                         0.0),
                               2.0e-12);
    }

    @Test
    public void testPointsOutsideBoundary() {
        doTestPointsNearBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                           FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                           0.0),
                                 0.1, 0.0101573, 0.1, 1.0e-7);
    }

    @Test
    public void testPointsInsideBoundary() {
        doTestPointsNearBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                           FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                           0.0),
                                 -0.1, -0.1, -0.0693260, 1.0e-7);
    }

    @Test
    public void testPointsAlongPrincipalAxes() {

        final EllipticalFieldOfView fov  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                     FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                     0.0);

        // test points in the primary meridian
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(11)),
                                                              FastMath.sin(-FastMath.toRadians(11)),
                                                              0.0),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) > 0.0);
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(9)),
                                                              FastMath.sin(-FastMath.toRadians(9)),
                                                              0.0),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) < 0.0);
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(9)),
                                                              FastMath.sin(FastMath.toRadians(9)),
                                                              0.0),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) < 0.0);
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(11)),
                                                              FastMath.sin(FastMath.toRadians(11)),
                                                              0.0),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) > 0.0);

        // test points in the secondary meridian
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(41)),
                                                              0.0,
                                                              FastMath.sin(-FastMath.toRadians(41))),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) > 0.0);
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(39)),
                                                              0.0,
                                                              FastMath.sin(-FastMath.toRadians(39))),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) < 0.0);
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(39)),
                                                              0.0,
                                                              FastMath.sin(FastMath.toRadians(39))),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) < 0.0);
        Assertions.assertTrue(fov.offsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(41)),
                                                              0.0,
                                                              FastMath.sin(FastMath.toRadians(41))),
                                                 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV) > 0.0);
    }

    @Test
    public void testOffsetAngularAccuracy() {

        final EllipticalFieldOfView fov  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                     FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                     0.0);
        final Vector3D f1 = fov.getFocus1();
        final Vector3D f2 = fov.getFocus2();
        final double   a  = FastMath.max(fov.getHalfApertureAlongX(), fov.getHalfApertureAlongY());

        UnitSphereRandomVectorGenerator random =
                        new UnitSphereRandomVectorGenerator(3, new Well19937a(0xc9383d990d45a111l));
        for (int i = 0; i < 300; ++i) {
            final Vector3D los = new Vector3D(random.nextVector());
            final double   d1  = Vector3D.angle(los, f1);
            final double   d2  = Vector3D.angle(los, f2);
            double etaMin;
            double etaMax;
            if (Vector3D.dotProduct(los, fov.getY()) > 0) {
                if (Vector3D.dotProduct(los, fov.getX()) > 0) {
                    etaMin = 0;
                    etaMax = 0.5 * FastMath.PI;
                } else {
                    etaMin = 0.5 * FastMath.PI;
                    etaMax = FastMath.PI;
                }
            } else {
                if (Vector3D.dotProduct(los, fov.getX()) < 0) {
                    etaMin = FastMath.PI;
                    etaMax = 1.5 * FastMath.PI;
                } else {
                    etaMin = 1.5 * FastMath.PI;
                    etaMax = 2.0 * FastMath.PI;
                }
            }
            double minDist = Double.POSITIVE_INFINITY;
            for (double eta = etaMin; eta < etaMax; eta += 0.0001) {
                minDist = FastMath.min(minDist, Vector3D.angle(los, fov.directionAt(eta)));
            }
            minDist = FastMath.copySign(minDist, d1 + d2 - 2 * a);

            // here, we intentionally use an impossibly large radius to ensure we don't use the speed-up
            // and compute offset as an exact angle
            double hugeRadius = FastMath.PI;
            double realOffset = fov.offsetFromBoundary(los, hugeRadius, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV);
            Assertions.assertEquals(minDist + hugeRadius, realOffset, 3.0e-7);

            // here, we intentionally use a zero radius, so we may use the speed-up
            // and may underestimate the offset
            double approximateOffset = fov.offsetFromBoundary(los, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV);
            Assertions.assertTrue(approximateOffset < minDist + 3.0e-7);

        }

    }

    @Test
    public void testBoundary() {
        doTestBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                 FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                 0.01),
                       new Well19937a(0x5148b24d1bdf90cel),
                       2.0e-9);
    }

    private void doTestPointsOnBoundary(final EllipticalFieldOfView fov, double tol) {
        try {
            Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
            directionAt.setAccessible(true);
            for (double theta = 0; theta < MathUtils.TWO_PI; theta += 0.01) {
                final Vector3D direction = (Vector3D) directionAt.invoke(fov, theta);
                Assertions.assertEquals(0.0,
                                    fov.offsetFromBoundary(direction, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                    tol);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    private void doTestPointsNearBoundary(final EllipticalFieldOfView fov, final double delta,
                                          final double expectedMin, final double expectedMax, final double tol) {
        try {
            final EllipticalFieldOfView near = new EllipticalFieldOfView(fov.getCenter(), fov.getX(),
                                                                         fov.getHalfApertureAlongX() + delta,
                                                                         fov.getHalfApertureAlongY() + delta,
                                                                         fov.getMargin());
            Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
            directionAt.setAccessible(true);
            double minOffset = Double.POSITIVE_INFINITY;
            double maxOffset = Double.NEGATIVE_INFINITY;
            for (double theta = 0; theta < MathUtils.TWO_PI; theta += 0.01) {
                final Vector3D direction = (Vector3D) directionAt.invoke(near, theta);
                final double offset = fov.offsetFromBoundary(direction, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV);
                minOffset = FastMath.min(minOffset, offset);
                maxOffset = FastMath.max(maxOffset, offset);
            }
            Assertions.assertEquals(expectedMin, minOffset, tol);
            Assertions.assertEquals(expectedMax, maxOffset, tol);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

}
