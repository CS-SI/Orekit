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
package org.orekit.utils;


import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


public class PVCoordinatesTest {

    @Test
    public void testDefaultConstructor() {
        Assert.assertEquals("{P(0.0, 0.0, 0.0), V(0.0, 0.0, 0.0), A(0.0, 0.0, 0.0)}", new PVCoordinates().toString());
    }

    @Test
    public void testLinearConstructors() {
        PVCoordinates pv1 = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                              new Vector3D(-1, -0.1, -10));
        PVCoordinates pv2 = new PVCoordinates(new Vector3D( 2,  0.2,  20),
                                              new Vector3D(-2, -0.2, -20));
        PVCoordinates pv3 = new PVCoordinates(new Vector3D( 3,  0.3,  30),
                                              new Vector3D(-3, -0.3, -30));
        PVCoordinates pv4 = new PVCoordinates(new Vector3D( 4,  0.4,  40),
                                              new Vector3D(-4, -0.4, -40));
        checkPV(pv4, new PVCoordinates(4, pv1), 1.0e-15);
        checkPV(pv2, new PVCoordinates(pv1, pv3), 1.0e-15);
        checkPV(pv3, new PVCoordinates(1, pv1, 1, pv2), 1.0e-15);
        checkPV(new PVCoordinates(2, pv4), new PVCoordinates(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new PVCoordinates(3, pv3), new PVCoordinates(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new PVCoordinates(5, pv4), new PVCoordinates(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVectorNeg() throws OrekitException {
        try {
            PVCoordinates.ZERO.toDerivativeStructureVector(-1);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector3() throws OrekitException {
        try {
            PVCoordinates.ZERO.toDerivativeStructureVector(3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector0() throws OrekitException {
        FieldVector3D<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(0);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(0, fv.getX().getOrder());
        Assert.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        checkPV(new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  Vector3D.ZERO,
                                  Vector3D.ZERO),
                new PVCoordinates(fv), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector1() throws OrekitException {
        FieldVector3D<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(1);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(1, fv.getX().getOrder());
        Assert.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        checkPV(new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  Vector3D.ZERO),
                new PVCoordinates(fv), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector2() throws OrekitException {
        FieldVector3D<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(2);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(2, fv.getX().getOrder());
        Assert.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(  10.0, fv.getX().getPartialDerivative(2), 1.0e-15);
        Assert.assertEquals(  -1.0, fv.getY().getPartialDerivative(2), 1.0e-15);
        Assert.assertEquals(-100.0, fv.getZ().getPartialDerivative(2), 1.0e-15);
        checkPV(new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)),
                new PVCoordinates(fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assert.assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            Assert.assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            Assert.assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }
    }

    @Test
    public void testShift() {
        Vector3D p1 = new Vector3D( 1,  0.1,  10);
        Vector3D p2 = new Vector3D( 2,  0.2,  20);
        Vector3D v  = new Vector3D(-1, -0.1, -10);
        checkPV(new PVCoordinates(p2, v), new PVCoordinates(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0, PVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm(), 1.0e-15);
    }

    @Test
    public void testToString() {
        PVCoordinates pv =
            new PVCoordinates(new Vector3D( 1,  0.1,  10), new Vector3D(-1, -0.1, -10));
        Assert.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(0.0, 0.0, 0.0)}", pv.toString());
    }

    @Test
    public void testGetMomentum() {
        //setup
        Vector3D p = new Vector3D(1, -2, 3);
        Vector3D v = new Vector3D(-9, 8, -7);

        //action + verify
        Assert.assertEquals(new PVCoordinates(p, v).getMomentum(), p.crossProduct(v));
        //check simple cases
        Assert.assertEquals(
                new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_I).getMomentum(),
                Vector3D.ZERO);
        Assert.assertEquals(
                new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J).getMomentum(),
                Vector3D.PLUS_K);
    }

    @Test
    public void testGetAngularVelocity() {
        //setup
        Vector3D p = new Vector3D(1, -2, 3);
        Vector3D v = new Vector3D(-9, 8, -7);

        //action + verify
        Assert.assertEquals(
                new PVCoordinates(p, v).getAngularVelocity(),
                p.crossProduct(v).scalarMultiply(1.0 / p.getNormSq()));
        //check extra simple cases
        Assert.assertEquals(
                new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_I).getAngularVelocity(),
                Vector3D.ZERO);
        Assert.assertEquals(
                new PVCoordinates(new Vector3D(2, 0, 0), Vector3D.PLUS_J).getAngularVelocity(),
                Vector3D.PLUS_K.scalarMultiply(0.5));
    }

    @Test
    public void testNormalize() {
        RandomGenerator generator = new Well19937a(0xb2011ffd25412067l);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final PVCoordinates pv = randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getX();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getY();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getZ();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            PVCoordinates normalized = pv.normalize();
            Assert.assertEquals(x.getValue(),              normalized.getPosition().getX(),     1.0e-16);
            Assert.assertEquals(y.getValue(),              normalized.getPosition().getY(),     1.0e-16);
            Assert.assertEquals(z.getValue(),              normalized.getPosition().getZ(),     1.0e-16);
            Assert.assertEquals(x.getPartialDerivative(1), normalized.getVelocity().getX(),     3.0e-13);
            Assert.assertEquals(y.getPartialDerivative(1), normalized.getVelocity().getY(),     3.0e-13);
            Assert.assertEquals(z.getPartialDerivative(1), normalized.getVelocity().getZ(),     3.0e-13);
            Assert.assertEquals(x.getPartialDerivative(2), normalized.getAcceleration().getX(), 6.0e-10);
            Assert.assertEquals(y.getPartialDerivative(2), normalized.getAcceleration().getY(), 6.0e-10);
            Assert.assertEquals(z.getPartialDerivative(2), normalized.getAcceleration().getZ(), 6.0e-10);
        }
    }

    @Test
    public void testCrossProduct() {
        RandomGenerator generator = new Well19937a(0x85c592b3be733d23l);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final PVCoordinates pv1 = randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            final PVCoordinates pv2 = randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                                                         pv2.shiftedBy(t).getPosition()).getX();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                                                         pv2.shiftedBy(t).getPosition()).getY();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                                                         pv2.shiftedBy(t).getPosition()).getZ();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            PVCoordinates product = PVCoordinates.crossProduct(pv1, pv2);
            Assert.assertEquals(x.getValue(),              product.getPosition().getX(),     1.0e-16);
            Assert.assertEquals(y.getValue(),              product.getPosition().getY(),     1.0e-16);
            Assert.assertEquals(z.getValue(),              product.getPosition().getZ(),     1.0e-16);
            Assert.assertEquals(x.getPartialDerivative(1), product.getVelocity().getX(),     9.0e-10);
            Assert.assertEquals(y.getPartialDerivative(1), product.getVelocity().getY(),     9.0e-10);
            Assert.assertEquals(z.getPartialDerivative(1), product.getVelocity().getZ(),     9.0e-10);
            Assert.assertEquals(x.getPartialDerivative(2), product.getAcceleration().getX(), 3.0e-9);
            Assert.assertEquals(y.getPartialDerivative(2), product.getAcceleration().getY(), 3.0e-9);
            Assert.assertEquals(z.getPartialDerivative(2), product.getAcceleration().getZ(), 3.0e-9);
        }
    }

    private Vector3D randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private PVCoordinates randomPVCoordinates(RandomGenerator random,
                                              double norm0, double norm1, double norm2) {
        Vector3D p0 = randomVector(random, norm0);
        Vector3D p1 = randomVector(random, norm1);
        Vector3D p2 = randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private void checkPV(PVCoordinates expected, PVCoordinates real, double epsilon) {
        Assert.assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        Assert.assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
    }

}
