/* Copyright 2002-2017 CS Systèmes d'Information
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


import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;


public class FieldPVCoordinatesTest {

    @Test
    public void testLinearConstructors() {
        DSFactory factory = new DSFactory(6, 1);
        FieldPVCoordinates<DerivativeStructure> pv1 = new FieldPVCoordinates<>(createVector(1, 0.1, 10, 6),
                                                                               createVector(-1, -0.1, -10, 6));
        FieldPVCoordinates<DerivativeStructure> pv2 = new FieldPVCoordinates<>(createVector(2, 0.2, 20, 6),
                                                                               createVector(-2, -0.2, -20, 6));
        FieldPVCoordinates<DerivativeStructure> pv3 = new FieldPVCoordinates<>(createVector(3, 0.3, 30, 6),
                                                                               createVector(-3, -0.3, -30, 6));
        FieldPVCoordinates<DerivativeStructure> pv4 = new FieldPVCoordinates<>(createVector(4, 0.4, 40, 6),
                                                                               createVector(-4, -0.4, -40, 6));
        checkPV(pv4, new FieldPVCoordinates<>(4, pv1), 1.0e-15);
        checkPV(pv4, new FieldPVCoordinates<>(factory.constant(4), pv1), 1.0e-15);
        checkPV(pv4, new FieldPVCoordinates<>(factory.constant(4), pv1.toPVCoordinates()), 1.0e-15);
        checkPV(pv2, new FieldPVCoordinates<>(pv1, pv3), 1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<>(1, pv1, 1, pv2), 1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<>(factory.constant(1), pv1,
                                              factory.constant(1), pv2),
                1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<>(factory.constant(1), pv1.toPVCoordinates(),
                                              factory.constant(1), pv2.toPVCoordinates()),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(2, pv4), new FieldPVCoordinates<>(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(3, pv3), new FieldPVCoordinates<>(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(3, pv3),
                new FieldPVCoordinates<>(factory.constant(3), pv1,
                                         factory.constant(1), pv2,
                                         factory.constant(1), pv4),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(3, pv3),
                new FieldPVCoordinates<>(factory.constant(3), pv1.toPVCoordinates(),
                                         factory.constant(1), pv2.toPVCoordinates(),
                                         factory.constant(1), pv4.toPVCoordinates()),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(5, pv4),
                new FieldPVCoordinates<>(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(5, pv4),
                new FieldPVCoordinates<>(factory.constant(4), pv1,
                                         factory.constant(3), pv2,
                                         factory.constant(2), pv3,
                                         factory.constant(1), pv4),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(5, pv4),
                new FieldPVCoordinates<>(factory.constant(4), pv1.toPVCoordinates(),
                                         factory.constant(3), pv2.toPVCoordinates(),
                                         factory.constant(2), pv3.toPVCoordinates(),
                                         factory.constant(1), pv4.toPVCoordinates()),
                1.0e-15);
    }

    @Test
    public void testConversionConstructor() {
        PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6), new Vector3D(7, 8, 9));
        FieldPVCoordinates<Decimal64> pv64 = new FieldPVCoordinates<>(Decimal64Field.getInstance(), pv);
        Assert.assertEquals(0.0,
                            Vector3D.distance(pv.getPosition(), pv64.getPosition().toVector3D()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(pv.getVelocity(), pv64.getVelocity().toVector3D()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(pv.getAcceleration(), pv64.getAcceleration().toVector3D()),
                            1.0e-15);
    }

    @Test
    public void testZero() {
        Assert.assertEquals(0.0,
                            FieldPVCoordinates.getZero(Decimal64Field.getInstance()).getPosition().getNorm().getReal(),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            FieldPVCoordinates.getZero(Decimal64Field.getInstance()).getVelocity().getNorm().getReal(),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            FieldPVCoordinates.getZero(Decimal64Field.getInstance()).getAcceleration().getNorm().getReal(),
                            1.0e-15);
    }

    @Test
    public void testGetMomentum() {
        //setup
        DSFactory factory = new DSFactory(1, 1);
        DerivativeStructure oneDS = factory.getDerivativeField().getOne();
        DerivativeStructure zeroDS = factory.getDerivativeField().getZero();
        FieldVector3D<DerivativeStructure> zero = new FieldVector3D<>(zeroDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> i = new FieldVector3D<>(oneDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> j = new FieldVector3D<>(zeroDS, oneDS, zeroDS);
        FieldVector3D<DerivativeStructure> k = new FieldVector3D<>(zeroDS, zeroDS, oneDS);
        FieldVector3D<DerivativeStructure> p = new FieldVector3D<>(oneDS,
                                                                   factory.constant(-2),
                                                                   factory.constant(3));
        FieldVector3D<DerivativeStructure> v = new FieldVector3D<>(factory.constant(-9),
                                                                   factory.constant(8),
                                                                   factory.constant(-7));

        //action + verify
        Assert.assertEquals(
                new FieldPVCoordinates<>(p, v).getMomentum(),
                p.crossProduct(v));
        //check simple cases
        Assert.assertEquals(
                new FieldPVCoordinates<>(i, i.scalarMultiply(-1)).getMomentum(),
                zero);
        Assert.assertEquals(
                new FieldPVCoordinates<>(i, j).getMomentum(),
                k);
    }

    @Test
    public void testGetAngularVelocity() {
        //setup
        DSFactory factory = new DSFactory(1, 1);
        DerivativeStructure oneDS = factory.getDerivativeField().getOne();
        DerivativeStructure zeroDS = factory.getDerivativeField().getZero();
        FieldVector3D<DerivativeStructure> zero = new FieldVector3D<>(zeroDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> i = new FieldVector3D<>(oneDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> j = new FieldVector3D<>(zeroDS, oneDS, zeroDS);
        FieldVector3D<DerivativeStructure> k = new FieldVector3D<>(zeroDS, zeroDS, oneDS);
        FieldVector3D<DerivativeStructure> p = new FieldVector3D<>(oneDS,
                                                                   factory.constant(-2),
                                                                   factory.constant(3));
        FieldVector3D<DerivativeStructure> v = new FieldVector3D<>(factory.constant(-9),
                                                                   factory.constant(8),
                                                                   factory.constant(-7));

        //action + verify
        Assert.assertEquals(
                new FieldPVCoordinates<>(p, v).getAngularVelocity(),
                p.crossProduct(v).scalarMultiply(p.getNormSq().reciprocal()));
        //check extra simple cases
        Assert.assertEquals(
                new FieldPVCoordinates<>(i, i.scalarMultiply(-1)).getAngularVelocity(),
                zero);
        Assert.assertEquals(
                new FieldPVCoordinates<>(i.scalarMultiply(2), j).getAngularVelocity(),
                k.scalarMultiply(0.5));
    }


    @Test
    public void testShift() {
        FieldVector3D<DerivativeStructure> p1 = createVector(1, 0.1, 10, 6);
        FieldVector3D<DerivativeStructure> p2 = createVector(2, 0.2, 20, 6);
        FieldVector3D<DerivativeStructure> v  = createVector(-1, -0.1, -10, 6);
        checkPV(new FieldPVCoordinates<>(p2, v), new FieldPVCoordinates<>(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0,
                            FieldPVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm().getValue(),
                            1.0e-15);
    }

    @Test
    public void testGetters() {
        FieldVector3D<DerivativeStructure> p = createVector(1, 0.1, 10, 6);
        FieldVector3D<DerivativeStructure> v = createVector(-0.1, 1, 0, 6);
        FieldPVCoordinates<DerivativeStructure> pv = new FieldPVCoordinates<>(p, v);
        Assert.assertEquals(0, FieldVector3D.distance(p, pv.getPosition()).getValue(), 1.0e-15);
        Assert.assertEquals(0, FieldVector3D.distance(v, pv.getVelocity()).getValue(), 1.0e-15);
        Assert.assertEquals(0, FieldVector3D.distance(createVector(-10, -1, 1.01, 6), pv.getMomentum()).getValue(), 1.0e-15);

        FieldPVCoordinates<DerivativeStructure> pvn = pv.negate();
        Assert.assertEquals(0, FieldVector3D.distance(createVector(-1, -0.1, -10, 6), pvn.getPosition()).getValue(), 1.0e-15);
        Assert.assertEquals(0, FieldVector3D.distance(createVector(0.1, -1, 0, 6), pvn.getVelocity()).getValue(), 1.0e-15);
        Assert.assertEquals(0, FieldVector3D.distance(createVector(-10, -1, 1.01, 6), pvn.getMomentum()).getValue(), 1.0e-15);
    }

    @Test
    public void testToString() {
        FieldPVCoordinates<DerivativeStructure> pv = new FieldPVCoordinates<>(createVector( 1,  0.1,  10, 6),
                                                                              createVector(-1, -0.1, -10, 6),
                                                                              createVector(10,  1.0, 100, 6));
        Assert.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    public void testNormalize() {
        DSFactory factory = new DSFactory(1, 2);
        RandomGenerator generator = new Well19937a(0x7ede9376e4e1ab5al);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final FieldPVCoordinates<DerivativeStructure> pv = randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getX().getValue();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getY().getValue();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getZ().getValue();
                        }
                    }).value(factory.variable(0, 0.0));
            FieldPVCoordinates<DerivativeStructure> normalized = pv.normalize();
            Assert.assertEquals(x.getValue(),              normalized.getPosition().getX().getValue(),     1.0e-16);
            Assert.assertEquals(y.getValue(),              normalized.getPosition().getY().getValue(),     1.0e-16);
            Assert.assertEquals(z.getValue(),              normalized.getPosition().getZ().getValue(),     1.0e-16);
            Assert.assertEquals(x.getPartialDerivative(1), normalized.getVelocity().getX().getValue(),     3.0e-13);
            Assert.assertEquals(y.getPartialDerivative(1), normalized.getVelocity().getY().getValue(),     3.0e-13);
            Assert.assertEquals(z.getPartialDerivative(1), normalized.getVelocity().getZ().getValue(),     3.0e-13);
            Assert.assertEquals(x.getPartialDerivative(2), normalized.getAcceleration().getX().getValue(), 6.0e-10);
            Assert.assertEquals(y.getPartialDerivative(2), normalized.getAcceleration().getY().getValue(), 6.0e-10);
            Assert.assertEquals(z.getPartialDerivative(2), normalized.getAcceleration().getZ().getValue(), 6.0e-10);
        }
    }

    private FieldVector3D<DerivativeStructure> randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        n = n / FastMath.sqrt(x * x + y * y + z * z);
        return createVector(n * x, n * y, n * z, 3);
    }

    private FieldPVCoordinates<DerivativeStructure> randomPVCoordinates(RandomGenerator random,
                                                                        double norm0, double norm1, double norm2) {
        FieldVector3D<DerivativeStructure> p0 = randomVector(random, norm0);
        FieldVector3D<DerivativeStructure> p1 = randomVector(random, norm1);
        FieldVector3D<DerivativeStructure> p2 = randomVector(random, norm2);
        return new FieldPVCoordinates<>(p0, p1, p2);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        DSFactory factory = new DSFactory(params, 1);
        return new FieldVector3D<>(factory.variable(0, x),
                                   factory.variable(1, y),
                                   factory.variable(2, z));
    }

    private <T extends RealFieldElement<T>> void checkPV(FieldPVCoordinates<T> expected, FieldPVCoordinates<T> real, double epsilon) {
        Assert.assertEquals(expected.getPosition().getX().getReal(), real.getPosition().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getPosition().getY().getReal(), real.getPosition().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ().getReal(), real.getPosition().getZ().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX().getReal(), real.getVelocity().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY().getReal(), real.getVelocity().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ().getReal(), real.getVelocity().getZ().getReal(), epsilon);
    }

}
