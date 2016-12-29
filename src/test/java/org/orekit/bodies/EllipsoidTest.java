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
package org.orekit.bodies;


import java.io.IOException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;


public class EllipsoidTest {

    @Test
    public void testGetters() {

        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);

        Assert.assertEquals(Predefined.EME2000,
                            ((FactoryManagedFrame) ellipsoid.getFrame()).getFactoryKey());
        Assert.assertEquals(1.0, ellipsoid.getA(), 1.0e-15);
        Assert.assertEquals(2.0, ellipsoid.getB(), 1.0e-15);
        Assert.assertEquals(3.0, ellipsoid.getC(), 1.0e-15);

    }

    @Test
    public void testPrincipalPlanesIntersections() {

        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);

        final Ellipse xy = ellipsoid.getPlaneSection(Vector3D.ZERO, Vector3D.PLUS_K);
        Assert.assertEquals(0, Vector3D.distance(Vector3D.ZERO, xy.getCenter()), 1.0e-15);
        checkPrincipalAxes(xy, Vector3D.PLUS_J, Vector3D.MINUS_I);
        Assert.assertEquals(2.0, xy.getA(), 1.0e-15);
        Assert.assertEquals(1.0, xy.getB(), 1.0e-15);
        Assert.assertTrue(xy.getFrame() == ellipsoid.getFrame());
        Assert.assertEquals(0.0, errorOnEllipsoid(xy, ellipsoid), 1.0e-15);
        Assert.assertEquals(0.0, errorOnPlane(xy, Vector3D.ZERO, Vector3D.PLUS_K), 1.0e-15);

        final Ellipse yz = ellipsoid.getPlaneSection(Vector3D.ZERO, Vector3D.PLUS_I);
        Assert.assertEquals(0, Vector3D.distance(Vector3D.ZERO, yz.getCenter()), 1.0e-15);
        checkPrincipalAxes(yz, Vector3D.PLUS_K, Vector3D.MINUS_J);
        Assert.assertEquals(3.0, yz.getA(), 1.0e-15);
        Assert.assertEquals(2.0, yz.getB(), 1.0e-15);
        Assert.assertTrue(yz.getFrame() == ellipsoid.getFrame());
        Assert.assertEquals(0.0, errorOnEllipsoid(yz, ellipsoid), 1.0e-15);
        Assert.assertEquals(0.0, errorOnPlane(yz, Vector3D.ZERO, Vector3D.PLUS_I), 1.0e-15);

        final Ellipse zx = ellipsoid.getPlaneSection(Vector3D.ZERO, Vector3D.PLUS_J);
        Assert.assertEquals(0, Vector3D.distance(Vector3D.ZERO, zx.getCenter()), 1.0e-15);
        checkPrincipalAxes(zx, Vector3D.PLUS_K, Vector3D.PLUS_I);
        Assert.assertEquals(3.0, zx.getA(), 1.0e-15);
        Assert.assertEquals(1.0, zx.getB(), 1.0e-15);
        Assert.assertTrue(zx.getFrame() == ellipsoid.getFrame());
        Assert.assertEquals(0.0, errorOnEllipsoid(zx, ellipsoid), 1.0e-15);
        Assert.assertEquals(0.0, errorOnPlane(zx, Vector3D.ZERO, Vector3D.PLUS_J), 1.0e-15);

    }

    @Test
    public void testNoIntersections() {
        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        final Ellipse ps = ellipsoid.getPlaneSection(new Vector3D(0, 0, 4), Vector3D.PLUS_K);
        Assert.assertNull(ps);
    }

    @Test
    public void testSinglePoint() throws IOException {
        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        final Ellipse ps = ellipsoid.getPlaneSection(new Vector3D(0, 0, 3), Vector3D.PLUS_K);
        Assert.assertEquals(0, Vector3D.distance(new Vector3D(0, 0, 3), ps.getCenter()), 1.0e-15);
        Assert.assertEquals(0.0, ps.getA(), 1.0e-15);
        Assert.assertEquals(0.0, ps.getB(), 1.0e-15);
    }

    @Test
    public void testRandomNormalSections() throws IOException {
        RandomGenerator random = new Well19937a(0x573c54d152aeafe4l);
        for (int i = 0; i < 100; ++i) {
            double a = 10 * random.nextDouble();
            double b = 10 * random.nextDouble();
            double c = 10 * random.nextDouble();
            double size = FastMath.max(FastMath.max(a, b), c);
            final Ellipsoid ellipsoid = new Ellipsoid(FramesFactory.getEME2000(), a, b, c);
            for (int j = 0; j < 50; ++j) {
                double phi     = FastMath.PI * (random.nextDouble() - 0.5);
                double lambda  = 2 * FastMath.PI * random.nextDouble();
                double cPhi    = FastMath.cos(phi);
                double sPhi    = FastMath.sin(phi);
                double cLambda = FastMath.cos(lambda);
                double sLambda = FastMath.sin(lambda);
                Vector3D surfacePoint = new Vector3D(ellipsoid.getA() * cPhi * cLambda,
                                                     ellipsoid.getB() * cPhi * sLambda,
                                                     ellipsoid.getC() * sPhi);
                Vector3D t1 = new Vector3D(-ellipsoid.getA() * cPhi * sLambda,
                                            ellipsoid.getB() * cPhi * cLambda,
                                            0).normalize();
                Vector3D t2 = new Vector3D(-ellipsoid.getA() * sPhi * cLambda,
                                           -ellipsoid.getB() * sPhi * sLambda,
                                            ellipsoid.getC() * cPhi).normalize();
                final double azimuth = 2 * FastMath.PI * random.nextDouble();
                double cAzimuth = FastMath.cos(azimuth);
                double sAzimuth = FastMath.sin(azimuth);
                Vector3D tAz = new Vector3D(cAzimuth, t1, sAzimuth, t2);

                final Ellipse ps = ellipsoid.getPlaneSection(surfacePoint, tAz);
                Assert.assertEquals(0.0, errorOnEllipsoid(ps, ellipsoid), 1.0e-12 * size);
                Assert.assertEquals(0.0, errorOnPlane(ps, surfacePoint, tAz), 1.0e-10 * size);
                double cos = Vector3D.dotProduct(surfacePoint.subtract(ps.getCenter()), ps.getU()) / ps.getA();
                double sin = Vector3D.dotProduct(surfacePoint.subtract(ps.getCenter()), ps.getV()) / ps.getB();
                final Vector3D rebuilt = ps.pointAt(FastMath.atan2(sin, cos));
                Assert.assertEquals(0, Vector3D.distance(surfacePoint, rebuilt), 1.0e-11 * size);
            }
        }
    }

    @Test
    public void testInside() {
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        for (double f = -2.0; f <= 2.0; f += 1.0 / 128.0) {
            final boolean inside = FastMath.abs(f) <= 1.0;
            Assert.assertEquals(inside, ellipsoid.isInside(new Vector3D(f * ellipsoid.getA(), 0, 0)));
            Assert.assertEquals(inside, ellipsoid.isInside(new Vector3D(0, f * ellipsoid.getB(), 0)));
            Assert.assertEquals(inside, ellipsoid.isInside(new Vector3D(0, 0, f * ellipsoid.getC())));
        }
    }

    @Test
    public void testLimb() throws OrekitException {
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        RandomGenerator random = new Well1024a(0xa69c430a67475af7l);
        for (int i = 0; i < 5000; ++i) {
            Vector3D observer = new Vector3D((random.nextDouble() - 0.5) * 5,
                                             (random.nextDouble() - 0.5) * 5,
                                             (random.nextDouble() - 0.5) * 5);
            Vector3D outside  = new Vector3D((random.nextDouble() - 0.5) * 5,
                                             (random.nextDouble() - 0.5) * 5,
                                             (random.nextDouble() - 0.5) * 5);
            if (ellipsoid.isInside(observer)) {
                try {
                    ellipsoid.pointOnLimb(observer, outside);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assert.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
                }
            } else {
                final Vector3D onLimb = ellipsoid.pointOnLimb(observer, outside);
                Assert.assertEquals(0,
                                    FastMath.sin(Vector3D.angle(Vector3D.crossProduct(observer, outside),
                                                                Vector3D.crossProduct(observer, onLimb))),
                                    2e-14);
                final double scaledX = onLimb.getX() / ellipsoid.getA();
                final double scaledY = onLimb.getY() / ellipsoid.getB();
                final double scaledZ = onLimb.getZ() / ellipsoid.getC();
                Assert.assertEquals(1.0, scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ, 9e-11);
                final Vector3D normal = new Vector3D(scaledX / ellipsoid.getA(),
                                                     scaledY / ellipsoid.getB(),
                                                     scaledZ / ellipsoid.getC()).normalize();
                final Vector3D lineOfSight = onLimb.subtract(observer).normalize();
                Assert.assertEquals(0.0, Vector3D.dotProduct(normal, lineOfSight), 5e-10);
            }
        }
    }

    private void checkPrincipalAxes(Ellipse ps, Vector3D expectedU, Vector3D expectedV) {
        if (Vector3D.dotProduct(expectedU, ps.getU()) >= 0) {
            Assert.assertEquals(0, Vector3D.distance(expectedU,  ps.getU()), 1.0e-15);
            Assert.assertEquals(0, Vector3D.distance(expectedV,  ps.getV()), 1.0e-15);
        } else {
            Assert.assertEquals(0, Vector3D.distance(expectedU.negate(), ps.getU()), 1.0e-15);
            Assert.assertEquals(0, Vector3D.distance(expectedV.negate(), ps.getV()), 1.0e-15);
        }
    }

    private double errorOnEllipsoid(Ellipse ps, Ellipsoid ellipsoid) {
        double max = 0;
        for (double theta = 0; theta < 2 * FastMath.PI; theta += 0.1) {
            Vector3D p = ps.pointAt(theta);
            double xOa = p.getX() / ellipsoid.getA();
            double yOb = p.getY() / ellipsoid.getB();
            double zOc = p.getZ() / ellipsoid.getC();
            max = FastMath.max(max, FastMath.abs(MathArrays.linearCombination(xOa, xOa, yOb, yOb, zOc, zOc, 1, -1)));
        }
        return max;
    }

    private double errorOnPlane(Ellipse ps, Vector3D planePoint, Vector3D planeNormal) {
        double max = 0;
        for (double theta = 0; theta < 2 * FastMath.PI; theta += 0.1) {
            Vector3D p = ps.pointAt(theta);
            max = FastMath.max(max, FastMath.abs(Vector3D.dotProduct(p.subtract(planePoint).normalize(), planeNormal)));
        }
        return max;
    }

}

