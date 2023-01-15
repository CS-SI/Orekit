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
package org.orekit.bodies;

import java.io.IOException;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.models.earth.ReferenceEllipsoid;


public class EllipsoidTest {

    @Test
    public void testGetters() {

        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);

        Assertions.assertEquals(Predefined.EME2000,
                            ((FactoryManagedFrame) ellipsoid.getFrame()).getFactoryKey());
        Assertions.assertEquals(1.0, ellipsoid.getA(), 1.0e-15);
        Assertions.assertEquals(2.0, ellipsoid.getB(), 1.0e-15);
        Assertions.assertEquals(3.0, ellipsoid.getC(), 1.0e-15);

    }

    @Test
    public void testPrincipalPlanesIntersections() {

        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);

        final Ellipse xy = ellipsoid.getPlaneSection(Vector3D.ZERO, Vector3D.PLUS_K);
        Assertions.assertEquals(0, Vector3D.distance(Vector3D.ZERO, xy.getCenter()), 1.0e-15);
        checkPrincipalAxes(xy, Vector3D.PLUS_J, Vector3D.MINUS_I);
        Assertions.assertEquals(2.0, xy.getA(), 1.0e-15);
        Assertions.assertEquals(1.0, xy.getB(), 1.0e-15);
        Assertions.assertTrue(xy.getFrame() == ellipsoid.getFrame());
        Assertions.assertEquals(0.0, errorOnEllipsoid(xy, ellipsoid), 1.0e-15);
        Assertions.assertEquals(0.0, errorOnPlane(xy, Vector3D.ZERO, Vector3D.PLUS_K), 1.0e-15);

        final Ellipse yz = ellipsoid.getPlaneSection(Vector3D.ZERO, Vector3D.PLUS_I);
        Assertions.assertEquals(0, Vector3D.distance(Vector3D.ZERO, yz.getCenter()), 1.0e-15);
        checkPrincipalAxes(yz, Vector3D.PLUS_K, Vector3D.MINUS_J);
        Assertions.assertEquals(3.0, yz.getA(), 1.0e-15);
        Assertions.assertEquals(2.0, yz.getB(), 1.0e-15);
        Assertions.assertTrue(yz.getFrame() == ellipsoid.getFrame());
        Assertions.assertEquals(0.0, errorOnEllipsoid(yz, ellipsoid), 1.0e-15);
        Assertions.assertEquals(0.0, errorOnPlane(yz, Vector3D.ZERO, Vector3D.PLUS_I), 1.0e-15);

        final Ellipse zx = ellipsoid.getPlaneSection(Vector3D.ZERO, Vector3D.PLUS_J);
        Assertions.assertEquals(0, Vector3D.distance(Vector3D.ZERO, zx.getCenter()), 1.0e-15);
        checkPrincipalAxes(zx, Vector3D.PLUS_K, Vector3D.PLUS_I);
        Assertions.assertEquals(3.0, zx.getA(), 1.0e-15);
        Assertions.assertEquals(1.0, zx.getB(), 1.0e-15);
        Assertions.assertTrue(zx.getFrame() == ellipsoid.getFrame());
        Assertions.assertEquals(0.0, errorOnEllipsoid(zx, ellipsoid), 1.0e-15);
        Assertions.assertEquals(0.0, errorOnPlane(zx, Vector3D.ZERO, Vector3D.PLUS_J), 1.0e-15);

    }

    @Test
    public void testFieldPrincipalPlanesIntersections() {
        doTestFieldPrincipalPlanesIntersections(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldPrincipalPlanesIntersections(final Field<T> field) {

        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);

        final FieldVector3D<T> zero   = FieldVector3D.getZero(field);
        final FieldVector3D<T> plusI  = FieldVector3D.getPlusI(field);
        final FieldVector3D<T> plusJ  = FieldVector3D.getPlusJ(field);
        final FieldVector3D<T> plusK  = FieldVector3D.getPlusK(field);
        final FieldVector3D<T> minusI = FieldVector3D.getMinusI(field);
        final FieldVector3D<T> minusJ = FieldVector3D.getMinusJ(field);

        final FieldEllipse<T> xy = ellipsoid.getPlaneSection(zero, plusK);
        Assertions.assertEquals(0, FieldVector3D.distance(zero, xy.getCenter()).getReal(), 1.0e-15);
        checkPrincipalAxes(xy, plusJ, minusI);
        Assertions.assertEquals(2.0, xy.getA().getReal(), 1.0e-15);
        Assertions.assertEquals(1.0, xy.getB().getReal(), 1.0e-15);
        Assertions.assertTrue(xy.getFrame() == ellipsoid.getFrame());
        Assertions.assertEquals(0.0, errorOnEllipsoid(xy, ellipsoid).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, errorOnPlane(xy, zero, plusK).getReal(), 1.0e-15);

        final FieldEllipse<T> yz = ellipsoid.getPlaneSection(zero, plusI);
        Assertions.assertEquals(0, FieldVector3D.distance(zero, yz.getCenter()).getReal(), 1.0e-15);
        checkPrincipalAxes(yz, plusK, minusJ);
        Assertions.assertEquals(3.0, yz.getA().getReal(), 1.0e-15);
        Assertions.assertEquals(2.0, yz.getB().getReal(), 1.0e-15);
        Assertions.assertTrue(yz.getFrame() == ellipsoid.getFrame());
        Assertions.assertEquals(0.0, errorOnEllipsoid(yz, ellipsoid).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, errorOnPlane(yz, zero, plusI).getReal(), 1.0e-15);

        final FieldEllipse<T> zx = ellipsoid.getPlaneSection(zero, plusJ);
        Assertions.assertEquals(0, FieldVector3D.distance(zero, zx.getCenter()).getReal(), 1.0e-15);
        checkPrincipalAxes(zx, plusK, plusI);
        Assertions.assertEquals(3.0, zx.getA().getReal(), 1.0e-15);
        Assertions.assertEquals(1.0, zx.getB().getReal(), 1.0e-15);
        Assertions.assertTrue(zx.getFrame() == ellipsoid.getFrame());
        Assertions.assertEquals(0.0, errorOnEllipsoid(zx, ellipsoid).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, errorOnPlane(zx, zero, plusJ).getReal(), 1.0e-15);

    }

    @Test
    public void testNoIntersections() {
        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        final Ellipse ps = ellipsoid.getPlaneSection(new Vector3D(0, 0, 4), Vector3D.PLUS_K);
        Assertions.assertNull(ps);
    }

    @Test
    public void testFieldNoIntersections() {
        doTestFieldNoIntersections(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldNoIntersections(final Field<T> field) {
        final T zero = field.getZero();
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        final FieldEllipse<T> ps = ellipsoid.getPlaneSection(new FieldVector3D<>(zero, zero, zero.newInstance(4)),
                                                             FieldVector3D.getPlusK(field));
        Assertions.assertNull(ps);
    }

    @Test
    public void testSinglePoint() throws IOException {
        final Ellipsoid ellipsoid =
                new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        final Ellipse ps = ellipsoid.getPlaneSection(new Vector3D(0, 0, 3), Vector3D.PLUS_K);
        Assertions.assertEquals(0, Vector3D.distance(new Vector3D(0, 0, 3), ps.getCenter()), 1.0e-15);
        Assertions.assertEquals(0.0, ps.getA(), 1.0e-15);
        Assertions.assertEquals(0.0, ps.getB(), 1.0e-15);
    }

    @Test
    public void testFieldSinglePoint() throws IOException {
        doTestFieldSinglePoint(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSinglePoint(final Field<T> field) {
        final T zero = field.getZero();
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        final FieldEllipse<T> ps = ellipsoid.getPlaneSection(new FieldVector3D<>(zero, zero, zero.newInstance(3)),
                                                             FieldVector3D.getPlusK(field));
        Assertions.assertEquals(0,
                                FieldVector3D.distance(new FieldVector3D<>(zero, zero, zero.newInstance(3)), ps.getCenter()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0, ps.getA().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, ps.getB().getReal(), 1.0e-15);
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
                Assertions.assertEquals(0.0, errorOnEllipsoid(ps, ellipsoid), 1.0e-12 * size);
                Assertions.assertEquals(0.0, errorOnPlane(ps, surfacePoint, tAz), 1.0e-10 * size);
                double cos = Vector3D.dotProduct(surfacePoint.subtract(ps.getCenter()), ps.getU()) / ps.getA();
                double sin = Vector3D.dotProduct(surfacePoint.subtract(ps.getCenter()), ps.getV()) / ps.getB();
                final Vector3D rebuilt = ps.pointAt(FastMath.atan2(sin, cos));
                Assertions.assertEquals(0, Vector3D.distance(surfacePoint, rebuilt), 1.0e-11 * size);
            }
        }
    }

    @Test
    public void testFieldRandomNormalSections() throws IOException {
        doTestFieldRandomNormalSections(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldRandomNormalSections(final Field<T> field) {
        final T zero = field.getZero();
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
                FieldVector3D<T> surfacePoint = new FieldVector3D<>(zero.newInstance(ellipsoid.getA() * cPhi * cLambda),
                                                                    zero.newInstance(ellipsoid.getB() * cPhi * sLambda),
                                                                    zero.newInstance(ellipsoid.getC() * sPhi));
                FieldVector3D<T> t1 = new FieldVector3D<>(zero.newInstance(-ellipsoid.getA() * cPhi * sLambda),
                                                          zero.newInstance(ellipsoid.getB() * cPhi * cLambda),
                                                           zero).normalize();
                FieldVector3D<T> t2 = new FieldVector3D<>(zero.newInstance(-ellipsoid.getA() * sPhi * cLambda),
                                                          zero.newInstance(-ellipsoid.getB() * sPhi * sLambda),
                                                          zero.newInstance(ellipsoid.getC() * cPhi)).normalize();
                final double azimuth = 2 * FastMath.PI * random.nextDouble();
                double cAzimuth = FastMath.cos(azimuth);
                double sAzimuth = FastMath.sin(azimuth);
                FieldVector3D<T> tAz = new FieldVector3D<>(cAzimuth, t1, sAzimuth, t2);

                final FieldEllipse<T> ps = ellipsoid.getPlaneSection(surfacePoint, tAz);
                Assertions.assertEquals(0.0, errorOnEllipsoid(ps, ellipsoid).getReal(), 1.0e-12 * size);
                Assertions.assertEquals(0.0, errorOnPlane(ps, surfacePoint, tAz).getReal(), 1.0e-10 * size);
                T cos = FieldVector3D.dotProduct(surfacePoint.subtract(ps.getCenter()), ps.getU()).divide(ps.getA());
                T sin = FieldVector3D.dotProduct(surfacePoint.subtract(ps.getCenter()), ps.getV()).divide(ps.getB());
                final FieldVector3D<T> rebuilt = ps.pointAt(FastMath.atan2(sin, cos));
                Assertions.assertEquals(0, FieldVector3D.distance(surfacePoint, rebuilt).getReal(), 1.0e-11 * size);
            }
        }
    }

    @Test
    public void testInside() {
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        for (double f = -2.0; f <= 2.0; f += 1.0 / 128.0) {
            final boolean inside = FastMath.abs(f) <= 1.0;
            Assertions.assertEquals(inside, ellipsoid.isInside(new Vector3D(f * ellipsoid.getA(), 0, 0)));
            Assertions.assertEquals(inside, ellipsoid.isInside(new Vector3D(0, f * ellipsoid.getB(), 0)));
            Assertions.assertEquals(inside, ellipsoid.isInside(new Vector3D(0, 0, f * ellipsoid.getC())));
        }
    }

    @Test
    public void testFieldInside() {
        doTestFieldInside(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldInside(final Field<T> field) {
        final T zero = field.getZero();
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        for (double f = -2.0; f <= 2.0; f += 1.0 / 128.0) {
            final boolean inside = FastMath.abs(f) <= 1.0;
            Assertions.assertEquals(inside, ellipsoid.isInside(new FieldVector3D<>(zero.newInstance(f * ellipsoid.getA()), zero, zero)));
            Assertions.assertEquals(inside, ellipsoid.isInside(new FieldVector3D<>(zero, zero.newInstance(f * ellipsoid.getB()), zero)));
            Assertions.assertEquals(inside, ellipsoid.isInside(new FieldVector3D<>(zero, zero, zero.newInstance(f * ellipsoid.getC()))));
        }
    }

    @Test
    public void testLimb() {
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
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
                }
            } else {
                checkOnLimb(ellipsoid, observer, outside);
            }
        }
    }

    @Test
    public void testFieldLimb() {
        doTestFieldLimb(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldLimb(final Field<T> field) {
        final T zero = field.getZero();
        final Ellipsoid ellipsoid =
                        new Ellipsoid(FramesFactory.getEME2000(), 1, 2, 3);
        RandomGenerator random = new Well1024a(0xa69c430a67475af7l);
        for (int i = 0; i < 5000; ++i) {
            FieldVector3D<T> observer = new FieldVector3D<>(zero.newInstance((random.nextDouble() - 0.5) * 5),
                                                            zero.newInstance((random.nextDouble() - 0.5) * 5),
                                                            zero.newInstance((random.nextDouble() - 0.5) * 5));
            FieldVector3D<T> outside  = new FieldVector3D<>(zero.newInstance((random.nextDouble() - 0.5) * 5),
                                                            zero.newInstance((random.nextDouble() - 0.5) * 5),
                                                            zero.newInstance((random.nextDouble() - 0.5) * 5));
            if (ellipsoid.isInside(observer)) {
                try {
                    ellipsoid.pointOnLimb(observer, outside);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
                }
            } else {
                checkOnLimb(ellipsoid, observer, outside);
            }
        }
    }

    /** A test case where x >> y and y << 1, which is stressing numerically. */
    @Test
    public void testIssue639() {
        // setup
        Vector3D observer = new Vector3D(
                5621586.021199942, -4496118.751975084, 0.000000008);
        Vector3D outside = new Vector3D(
                69159195202.69193, 123014642034.89732, -44866184753.460625);
        Ellipsoid oae = ReferenceEllipsoid.getWgs84(null);

        // action
        Vector3D actual = oae.pointOnLimb(observer, outside);

        // verify
        checkOnLimb(oae, observer, outside);
        Assertions.assertFalse(actual.isNaN(),"" + actual);
    }

    @Test
    public void testFieldIssue639() {
        doTestFieldIssue639(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldIssue639(final Field<T> field) {
        final T zero = field.getZero();
        // setup
        FieldVector3D<T> observer = new FieldVector3D<>(zero.newInstance(5621586.021199942),
                                                        zero.newInstance(-4496118.751975084),
                                                        zero.newInstance(0.000000008));
        FieldVector3D<T>  outside = new FieldVector3D<>(zero.newInstance(69159195202.69193),
                                                        zero.newInstance(123014642034.89732),
                                                        zero.newInstance(-44866184753.460625));
        Ellipsoid oae = ReferenceEllipsoid.getWgs84(null);

        // action
        FieldVector3D<T>  actual = oae.pointOnLimb(observer, outside);

        // verify
        checkOnLimb(oae, observer, outside);
        Assertions.assertFalse(actual.isNaN(),"" + actual);
    }

    private void checkPrincipalAxes(Ellipse ps, Vector3D expectedU, Vector3D expectedV) {
        if (Vector3D.dotProduct(expectedU, ps.getU()) >= 0) {
            Assertions.assertEquals(0, Vector3D.distance(expectedU,  ps.getU()), 1.0e-15);
            Assertions.assertEquals(0, Vector3D.distance(expectedV,  ps.getV()), 1.0e-15);
        } else {
            Assertions.assertEquals(0, Vector3D.distance(expectedU.negate(), ps.getU()), 1.0e-15);
            Assertions.assertEquals(0, Vector3D.distance(expectedV.negate(), ps.getV()), 1.0e-15);
        }
    }

    private <T extends CalculusFieldElement<T>> void checkPrincipalAxes(FieldEllipse<T> ps, FieldVector3D<T> expectedU, FieldVector3D<T> expectedV) {
        if (FieldVector3D.dotProduct(expectedU, ps.getU()).getReal() >= 0) {
            Assertions.assertEquals(0, FieldVector3D.distance(expectedU,  ps.getU()).getReal(), 1.0e-15);
            Assertions.assertEquals(0, FieldVector3D.distance(expectedV,  ps.getV()).getReal(), 1.0e-15);
        } else {
            Assertions.assertEquals(0, FieldVector3D.distance(expectedU.negate(), ps.getU()).getReal(), 1.0e-15);
            Assertions.assertEquals(0, FieldVector3D.distance(expectedV.negate(), ps.getV()).getReal(), 1.0e-15);
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

    private <T extends CalculusFieldElement<T>> T errorOnEllipsoid(FieldEllipse<T> ps, Ellipsoid ellipsoid) {
        T one = ps.getA().getField().getOne();
        T max = ps.getA().getField().getZero();
        for (double theta = 0; theta < 2 * FastMath.PI; theta += 0.1) {
            FieldVector3D<T> p = ps.pointAt(max.newInstance(theta));
            T xOa = p.getX().divide(ellipsoid.getA());
            T yOb = p.getY().divide(ellipsoid.getB());
            T zOc = p.getZ().divide(ellipsoid.getC());
            max = FastMath.max(max, FastMath.abs(max.linearCombination(xOa, xOa, yOb, yOb, zOc, zOc, one, one.negate())));
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

    private <T extends CalculusFieldElement<T>> T errorOnPlane(FieldEllipse<T> ps, FieldVector3D<T> planePoint, FieldVector3D<T> planeNormal) {
        T max = ps.getA().getField().getZero();
        for (double theta = 0; theta < 2 * FastMath.PI; theta += 0.1) {
            FieldVector3D<T> p = ps.pointAt(max.newInstance(theta));
            max = FastMath.max(max, FastMath.abs(FieldVector3D.dotProduct(p.subtract(planePoint).normalize(), planeNormal)));
        }
        return max;
    }

    private void checkOnLimb(Ellipsoid ellipsoid, Vector3D observer, Vector3D outside) {
        final Vector3D onLimb = ellipsoid.pointOnLimb(observer, outside);
        Assertions.assertEquals(0,
                            FastMath.sin(Vector3D.angle(Vector3D.crossProduct(observer, outside),
                                                        Vector3D.crossProduct(observer, onLimb))),
                            2e-14);
        final double scaledX = onLimb.getX() / ellipsoid.getA();
        final double scaledY = onLimb.getY() / ellipsoid.getB();
        final double scaledZ = onLimb.getZ() / ellipsoid.getC();
        Assertions.assertEquals(1.0, scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ, 9e-11);
        final Vector3D normal = new Vector3D(scaledX / ellipsoid.getA(),
                                             scaledY / ellipsoid.getB(),
                                             scaledZ / ellipsoid.getC()).normalize();
        final Vector3D lineOfSight = onLimb.subtract(observer).normalize();
        Assertions.assertEquals(0.0, Vector3D.dotProduct(normal, lineOfSight), 5e-10);
    }

    private <T extends CalculusFieldElement<T>> void checkOnLimb(Ellipsoid ellipsoid, FieldVector3D<T> observer, FieldVector3D<T> outside) {
        final FieldVector3D<T> onLimb = ellipsoid.pointOnLimb(observer, outside);
        Assertions.assertEquals(0,
                                FastMath.sin(FieldVector3D.angle(FieldVector3D.crossProduct(observer, outside),
                                                                 FieldVector3D.crossProduct(observer, onLimb))).getReal(),
                                2e-14);
        final T scaledX = onLimb.getX().divide(ellipsoid.getA());
        final T scaledY = onLimb.getY().divide(ellipsoid.getB());
        final T scaledZ = onLimb.getZ().divide(ellipsoid.getC());
        Assertions.assertEquals(1.0,
                                scaledX.multiply(scaledX).add(scaledY.multiply(scaledY)).add(scaledZ.multiply(scaledZ)).getReal(),
                                9e-11);
        final FieldVector3D<T> normal = new FieldVector3D<>(scaledX.divide(ellipsoid.getA()),
                                                            scaledY.divide(ellipsoid.getB()),
                                                            scaledZ.divide(ellipsoid.getC())).normalize();
        final FieldVector3D<T> lineOfSight = onLimb.subtract(observer).normalize();
        Assertions.assertEquals(0.0, FieldVector3D.dotProduct(normal, lineOfSight).getReal(), 5e-10);
    }

}

