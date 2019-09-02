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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.NadirPointing;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

public class EllipticalFieldOfViewTest extends AbstractSmoothFieldOfViewTest {

    @Test
    public void testNadirNoMarginAngular() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                        new NadirPointing(orbit.getFrame(), earth),
                        2.0, 4.0, 83.8280, 86.9120, 120567.3, 241701.8);
    }

    @Test
    public void testNadirNoMarginCartesian() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                        new NadirPointing(orbit.getFrame(), earth),
                        2.0, 4.0, 83.8280, 86.9120, 120567.3, 241701.8);
    }

    @Test
    public void testNadirMarginAngular() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.01, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                        new NadirPointing(orbit.getFrame(), earth),
                        2.0, 4.0, 83.8280, 86.9120, 120567.3, 241701.8);
    }

    @Test
    public void testNadirMarginCartesian() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.01, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                        new NadirPointing(orbit.getFrame(), earth),
                        2.0, 4.0, 83.8280, 86.9120, 120567.3, 241701.8);
    }

    @Test
    public void testRollPitchYawAngular() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                        new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                      FastMath.toRadians(10),
                                      FastMath.toRadians(20),
                                      FastMath.toRadians(5)),
                        2.0, 4.0, 47.7675, 60.2391, 1219653.0, 1816963.9);
    }

    @Test
    public void testRollPitchYawCartesian() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                        new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                      FastMath.toRadians(10),
                                      FastMath.toRadians(20),
                                      FastMath.toRadians(5)),
                        2.0, 4.0, 47.7675, 60.2403, 1219597.1, 1817011.0);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimbAngular() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                        new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                      FastMath.toRadians(-10),
                                      FastMath.toRadians(-39),
                                      FastMath.toRadians(-5)),
                        0.3899, 4.0, 0.0, 24.7014, 3213744.5, 5346638.0);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimbCartesian() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                        new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                      FastMath.toRadians(-10),
                                      FastMath.toRadians(-39),
                                      FastMath.toRadians(-5)),
                        0.3899, 4.0, 0.0, 24.7014, 3213727.9, 5346638.0);
    }

    @Test
    public void testFOVLargerThanEarthAngular() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(50.0), FastMath.toRadians(45.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                        new NadirPointing(orbit.getFrame(), earth),
                        40.3505, 40.4655, 0.0, 0.0, 5323032.8, 5347029.8);
    }

    @Test
    public void testFOVLargerThanEarthCartesian() {
        doTestFootprint(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                  FastMath.toRadians(50.0), FastMath.toRadians(45.0),
                                                  0.0, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                        new NadirPointing(orbit.getFrame(), earth),
                        40.3505, 40.4655, 0.0, 0.0, 5323032.8, 5347029.8);
    }

    @Test
    public void testFOVAwayFromEarthAngular() {
        doTestFOVAwayFromEarth(new EllipticalFieldOfView(Vector3D.MINUS_K, Vector3D.PLUS_I,
                                                         FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                         0.0, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                               new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                             FastMath.toRadians(-10),
                                             FastMath.toRadians(-39),
                                             FastMath.toRadians(-5)),
                               Vector3D.MINUS_K);
    }

    @Test
    public void testFOVAwayFromEarthCartesian() {
        doTestFOVAwayFromEarth(new EllipticalFieldOfView(Vector3D.MINUS_K, Vector3D.PLUS_I,
                                                         FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                         0.0, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                               new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ,
                                             FastMath.toRadians(-10),
                                             FastMath.toRadians(-39),
                                             FastMath.toRadians(-5)),
                               Vector3D.MINUS_K);
    }

    @Test
    public void testNoFootprintInsideAngular() {
        doTestNoFootprintInside(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                          FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                          0.0, EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                                new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(5e6, 3e6, 2e6)));
    }

    @Test
    public void testNoFootprintInsideCartesian() {
        doTestNoFootprintInside(new EllipticalFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                          FastMath.toRadians(4.0), FastMath.toRadians(2.0),
                                                          0.0, EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
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
                                                                     0.0,
                                                                     EllipticalFieldOfView.EllipticalConstraint.ANGULAR);
        final EllipticalFieldOfView cart = new EllipticalFieldOfView(ang.getCenter(), ang.getX(),
                                                                     ang.getHalfApertureAlongX(), ang.getHalfApertureAlongY(),
                                                                     ang.getMargin(),
                                                                     EllipticalFieldOfView.EllipticalConstraint.CARTESIAN);
        for (int i = 0; i < 4; ++i) {
            final double theta = i * 0.5 * FastMath.PI;
            final Vector3D pAng  = (Vector3D) directionAt.invoke(ang, theta);
            final Vector3D pCart = (Vector3D) directionAt.invoke(cart, theta);
            Assert.assertEquals(0.0, Vector3D.angle(pAng, pCart), 1.0e-15);
        }
    }

    @Test
    public void testPointsOnBoundaryAngular() {
        doTestPointsOnBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                         FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                         0.0,
                                                         EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                               1.0e-15);
    }

    @Test
    public void testPointsOnBoundaryCartesian() {
        doTestPointsOnBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                         FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                         0.0,
                                                         EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                               1.0e-15);
    }

    @Test
    public void testPointsOutsideBoundaryAngular() {
        doTestPointsNearBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                           FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                           0.0,
                                                           EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                                 0.1, 0.306997, 1.474196, 1.0e-6);
    }

    @Test
    public void testPointsOutsideBoundaryCartesian() {
        doTestPointsNearBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                           FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                           0.0,
                                                           EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                                 0.1, 0.240954, 1.437307, 1.0e-6);
    }

    @Test
    public void testPointsInsideBoundaryAngular() {
        doTestPointsNearBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                           FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                           0.0,
                                                           EllipticalFieldOfView.EllipticalConstraint.ANGULAR),
                                 -0.1, -0.817635, -0.265962, 1.0e-6);
    }

    @Test
    public void testPointsInsideBoundaryCartesian() {
        doTestPointsNearBoundary(new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                           FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                           0.0,
                                                           EllipticalFieldOfView.EllipticalConstraint.CARTESIAN),
                                 -0.1, -0.816113, -0.232576, 1.0e-6);
    }

    @Test
    public void testConventionsDifferences()
        throws NoSuchMethodException, SecurityException, IllegalAccessException,
               IllegalArgumentException, InvocationTargetException {
        Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
        directionAt.setAccessible(true);
        final EllipticalFieldOfView ang  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                     FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                     0.0,
                                                                     EllipticalFieldOfView.EllipticalConstraint.ANGULAR);
        final EllipticalFieldOfView cart = new EllipticalFieldOfView(ang.getCenter(), ang.getX(),
                                                                     ang.getHalfApertureAlongX(), ang.getHalfApertureAlongY(),
                                                                     ang.getMargin(),
                                                                     EllipticalFieldOfView.EllipticalConstraint.CARTESIAN);
        double maxCartVsAng = 0.0;
        double minCartVsAng = Double.POSITIVE_INFINITY;
        double maxAngVsCart = 0.0;
        double minAngVsCart = Double.POSITIVE_INFINITY;
        for (double theta = 0; theta < 0.25 * MathUtils.TWO_PI; theta += 0.001) {
            double cartVsAng = ang.rawOffsetFromBoundary(new Vector3D(1e0, (Vector3D) directionAt.invoke(cart, theta)));
            double angVsCart = cart.rawOffsetFromBoundary(new Vector3D(1e0, (Vector3D) directionAt.invoke(ang, theta)));
            maxCartVsAng = FastMath.max(maxCartVsAng, cartVsAng);
            minCartVsAng = FastMath.min(minCartVsAng, cartVsAng);
            maxAngVsCart = FastMath.max(maxAngVsCart, angVsCart);
            minAngVsCart = FastMath.min(minAngVsCart, angVsCart);
        }

        // Cartesian defined ellipse is outside of angular defined ellipse
        Assert.assertEquals( 0.094338, maxCartVsAng, 1.0e-6);
        Assert.assertEquals( 0.0,      minCartVsAng, 1.0e-15);

        // Angular defined ellipse is inside Cartesian defined ellipse
        Assert.assertEquals( 0.0,      maxAngVsCart, 1.0e-15);
        Assert.assertEquals(-0.072146, minAngVsCart, 1.0e-6);

    }

    @Test
    public void testInBetweenPoint()
        throws NoSuchMethodException, SecurityException, IllegalAccessException,
               IllegalArgumentException, InvocationTargetException {
        final EllipticalFieldOfView ang  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                     FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                     0.0,
                                                                     EllipticalFieldOfView.EllipticalConstraint.ANGULAR);
        final EllipticalFieldOfView cart = new EllipticalFieldOfView(ang.getCenter(), ang.getX(),
                                                                     ang.getHalfApertureAlongX(), ang.getHalfApertureAlongY(),
                                                                     ang.getMargin(),
                                                                     EllipticalFieldOfView.EllipticalConstraint.CARTESIAN);

        // the following point is inside the Cartesian ellipse (negative offset)
        // and outside the angular ellipse (positive offset)
        final Vector3D inBetween = new Vector3D(847.623917, 97.014898, 518.588517);
        Assert.assertEquals(-0.0339, cart.rawOffsetFromBoundary(inBetween), 1.0e-4);
        Assert.assertEquals(+0.0449, ang.rawOffsetFromBoundary(inBetween),  1.0e-4);

        Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
        directionAt.setAccessible(true);
        double minDistAng  = Double.POSITIVE_INFINITY;
        double minDistCart = Double.POSITIVE_INFINITY;
        for (double theta = FastMath.toRadians(46); theta < FastMath.toRadians(52); theta += 1.0e-7) {
            minDistAng  = FastMath.min(minDistAng,  Vector3D.angle(inBetween, (Vector3D) directionAt.invoke(ang,  theta)));
            minDistCart = FastMath.min(minDistCart, Vector3D.angle(inBetween, (Vector3D) directionAt.invoke(cart, theta)));
        }
        Assert.assertEquals(0.28708775, FastMath.toDegrees(minDistAng),  1.0e-8);
        Assert.assertEquals(0.28617985, FastMath.toDegrees(minDistCart), 1.0e-8);
        
    }

    @Test
    public void testDistance() {
        final EllipticalFieldOfView ang  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                     FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                     0.0,
                                                                     EllipticalFieldOfView.EllipticalConstraint.ANGULAR);
        final EllipticalFieldOfView cart = new EllipticalFieldOfView(ang.getCenter(), ang.getX(),
                                                                     ang.getHalfApertureAlongX(), ang.getHalfApertureAlongY(),
                                                                     ang.getMargin(),
                                                                     EllipticalFieldOfView.EllipticalConstraint.CARTESIAN);
        final double thetaAng  = FastMath.toRadians(51.65046);
        final double thetaCart = FastMath.toRadians(47.02751);
        final double distance  = FastMath.toRadians(0.60040);
        doTestDistance(ang, cart, thetaAng, thetaCart, distance);
        doTestDistance(cart, ang, thetaCart, thetaAng, distance);
    }

    @Test
    public void testPointsAlongPrincipalAxes() {

        for (final EllipticalFieldOfView.EllipticalConstraint convention : EllipticalFieldOfView.EllipticalConstraint.values()) {
            final EllipticalFieldOfView fov  = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                         FastMath.toRadians(10.0), FastMath.toRadians(40.0),
                                                                         0.0,
                                                                         convention);

            // test points in the primary meridian
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(11)),
                                                                     FastMath.sin(-FastMath.toRadians(11)),
                                                                     0.0)) > 0.0);
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(9)),
                                                                     FastMath.sin(-FastMath.toRadians(9)),
                                                                     0.0)) < 0.0);
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(9)),
                                                                     FastMath.sin(FastMath.toRadians(9)),
                                                                     0.0)) < 0.0);
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(11)),
                                                                     FastMath.sin(FastMath.toRadians(11)),
                                                                     0.0)) > 0.0);

            // test points in the secondary meridian
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(41)),
                                                                     0.0,
                                                                     FastMath.sin(-FastMath.toRadians(41)))) > 0.0);
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(39)),
                                                                     0.0,
                                                                     FastMath.sin(-FastMath.toRadians(39)))) < 0.0);
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(39)),
                                                                     0.0,
                                                                     FastMath.sin(FastMath.toRadians(39)))) < 0.0);
            Assert.assertTrue(fov.rawOffsetFromBoundary(new Vector3D(FastMath.cos(FastMath.toRadians(41)),
                                                                     0.0,
                                                                     FastMath.sin(FastMath.toRadians(41)))) > 0.0);
        }

    }

    private void doTestPointsOnBoundary(final EllipticalFieldOfView fov, double tol) {
        try {
            Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
            directionAt.setAccessible(true);
            for (double theta = 0; theta < MathUtils.TWO_PI; theta += 0.01) {
                final Vector3D direction = (Vector3D) directionAt.invoke(fov, theta);
                Assert.assertEquals(0.0, fov.rawOffsetFromBoundary(direction), tol);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

    private void doTestPointsNearBoundary(final EllipticalFieldOfView fov, final double delta,
                                          final double expectedMin, final double expectedMax, final double tol) {
        try {
            final EllipticalFieldOfView near = new EllipticalFieldOfView(fov.getCenter(), fov.getX(),
                                                                         fov.getHalfApertureAlongX() + delta,
                                                                         fov.getHalfApertureAlongY() + delta,
                                                                         fov.getMargin(),
                                                                         fov.getConstraint());
            Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
            directionAt.setAccessible(true);
            double minOffset = Double.POSITIVE_INFINITY;
            double maxOffset = Double.NEGATIVE_INFINITY;
            for (double theta = 0; theta < MathUtils.TWO_PI; theta += 0.01) {
                final Vector3D direction = (Vector3D) directionAt.invoke(near, theta);
                final double offset = fov.rawOffsetFromBoundary(direction);
                minOffset = FastMath.min(minOffset, offset);
                maxOffset = FastMath.max(maxOffset, offset);
            }
            Assert.assertEquals(expectedMin, minOffset, tol);
            Assert.assertEquals(expectedMax, maxOffset, tol);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

    private void doTestDistance(EllipticalFieldOfView fov1, EllipticalFieldOfView fov2, double theta,
                                double expectedEtaMin, double expectedDistMin) {
        try {
            Method directionAt = EllipticalFieldOfView.class.getDeclaredMethod("directionAt", Double.TYPE);
            directionAt.setAccessible(true);
            Vector3D p = (Vector3D) directionAt.invoke(fov1,  theta);
            double minDist = Double.POSITIVE_INFINITY;
            double minEta  = 0;
            for (double eta = expectedEtaMin - 0.01; eta < expectedEtaMin + 0.01; eta += 1.0e-7) {
                double d = Vector3D.distance(p, (Vector3D) directionAt.invoke(fov2, eta));
                if (d < minDist) {
                    minEta  = eta;
                    minDist = d;
                }
            }
            Assert.assertEquals(expectedEtaMin,  minEta,  2.0e-7);
            Assert.assertEquals(expectedDistMin, minDist, 2.0e-7);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException e) {
            Assert.fail(e.getLocalizedMessage());
        }

    }

}
