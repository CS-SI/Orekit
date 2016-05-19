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
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;


public class FieldPVCoordinatesTest {

    @Test
    public void testLinearConstructors() {
        FieldPVCoordinates<DerivativeStructure> pv1 = new FieldPVCoordinates<DerivativeStructure>(createVector(1, 0.1, 10, 6),
                                                  createVector(-1, -0.1, -10, 6));
        FieldPVCoordinates<DerivativeStructure> pv2 = new FieldPVCoordinates<DerivativeStructure>(createVector(2, 0.2, 20, 6),
                                                  createVector(-2, -0.2, -20, 6));
        FieldPVCoordinates<DerivativeStructure> pv3 = new FieldPVCoordinates<DerivativeStructure>(createVector(3, 0.3, 30, 6),
                                                  createVector(-3, -0.3, -30, 6));
        FieldPVCoordinates<DerivativeStructure> pv4 = new FieldPVCoordinates<DerivativeStructure>(createVector(4, 0.4, 40, 6),
                                                  createVector(-4, -0.4, -40, 6));
        checkPV(pv4, new FieldPVCoordinates<DerivativeStructure>(4, pv1), 1.0e-15);
        checkPV(pv4, new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 4), pv1), 1.0e-15);
        checkPV(pv4, new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 4), pv1.toPVCoordinates()), 1.0e-15);
        checkPV(pv2, new FieldPVCoordinates<DerivativeStructure>(pv1, pv3), 1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<DerivativeStructure>(1, pv1, 1, pv2), 1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 1), pv1,
                                         new DerivativeStructure(6, 1, 1), pv2),
                1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 1), pv1.toPVCoordinates(),
                                         new DerivativeStructure(6, 1, 1), pv2.toPVCoordinates()),
                1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(2, pv4), new FieldPVCoordinates<DerivativeStructure>(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(3, pv3), new FieldPVCoordinates<DerivativeStructure>(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(3, pv3),
                new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 3), pv1,
                                    new DerivativeStructure(6, 1, 1), pv2,
                                    new DerivativeStructure(6, 1, 1), pv4),
                1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(3, pv3),
                new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 3), pv1.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 1), pv2.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 1), pv4.toPVCoordinates()),
                1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(5, pv4),
                new FieldPVCoordinates<DerivativeStructure>(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(5, pv4),
                new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 4), pv1,
                                    new DerivativeStructure(6, 1, 3), pv2,
                                    new DerivativeStructure(6, 1, 2), pv3,
                                    new DerivativeStructure(6, 1, 1), pv4),
                1.0e-15);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(5, pv4),
                new FieldPVCoordinates<DerivativeStructure>(new DerivativeStructure(6, 1, 4), pv1.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 3), pv2.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 2), pv3.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 1), pv4.toPVCoordinates()),
                1.0e-15);
    }

    @Test
    public void testGetMomentum() {
        //setup
        DerivativeStructure oneDS = new DerivativeStructure(1, 1, 1);
        DerivativeStructure zeroDS = new DerivativeStructure(1, 1, 0);
        FieldVector3D<DerivativeStructure> zero = new FieldVector3D<DerivativeStructure>(
                zeroDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> i = new FieldVector3D<DerivativeStructure>(
                oneDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> j = new FieldVector3D<DerivativeStructure>(
                zeroDS, oneDS, zeroDS);
        FieldVector3D<DerivativeStructure> k = new FieldVector3D<DerivativeStructure>(
                zeroDS, zeroDS, oneDS);
        FieldVector3D<DerivativeStructure> p = new FieldVector3D<DerivativeStructure>(
                oneDS,
                new DerivativeStructure(1, 1, -2),
                new DerivativeStructure(1, 1, 3));
        FieldVector3D<DerivativeStructure> v = new FieldVector3D<DerivativeStructure>(
                new DerivativeStructure(1, 1, -9),
                new DerivativeStructure(1, 1, 8),
                new DerivativeStructure(1, 1, -7));

        //action + verify
        Assert.assertEquals(
                new FieldPVCoordinates<DerivativeStructure>(p, v).getMomentum(),
                p.crossProduct(v));
        //check simple cases
        Assert.assertEquals(
                new FieldPVCoordinates<DerivativeStructure>(i, i.scalarMultiply(-1)).getMomentum(),
                zero);
        Assert.assertEquals(
                new FieldPVCoordinates<DerivativeStructure>(i, j).getMomentum(),
                k);
    }

    @Test
    public void testGetAngularVelocity() {
        //setup
        DerivativeStructure oneDS = new DerivativeStructure(1, 1, 1);
        DerivativeStructure zeroDS = new DerivativeStructure(1, 1, 0);
        FieldVector3D<DerivativeStructure> zero = new FieldVector3D<DerivativeStructure>(
                zeroDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> i = new FieldVector3D<DerivativeStructure>(
                oneDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> j = new FieldVector3D<DerivativeStructure>(
                zeroDS, oneDS, zeroDS);
        FieldVector3D<DerivativeStructure> k = new FieldVector3D<DerivativeStructure>(
                zeroDS, zeroDS, oneDS);
        FieldVector3D<DerivativeStructure> p = new FieldVector3D<DerivativeStructure>(
                oneDS,
                new DerivativeStructure(1, 1, -2),
                new DerivativeStructure(1, 1, 3));
        FieldVector3D<DerivativeStructure> v = new FieldVector3D<DerivativeStructure>(
                new DerivativeStructure(1, 1, -9),
                new DerivativeStructure(1, 1, 8),
                new DerivativeStructure(1, 1, -7));

        //action + verify
        Assert.assertEquals(
                new FieldPVCoordinates<DerivativeStructure>(p, v).getAngularVelocity(),
                p.crossProduct(v).scalarMultiply(p.getNormSq().reciprocal()));
        //check extra simple cases
        Assert.assertEquals(
                new FieldPVCoordinates<DerivativeStructure>(i, i.scalarMultiply(-1)).getAngularVelocity(),
                zero);
        Assert.assertEquals(
                new FieldPVCoordinates<DerivativeStructure>(i.scalarMultiply(2), j).getAngularVelocity(),
                k.scalarMultiply(0.5));
    }


    @Test
    public void testShift() {
        FieldVector3D<DerivativeStructure> p1 = createVector(1, 0.1, 10, 6);
        FieldVector3D<DerivativeStructure> p2 = createVector(2, 0.2, 20, 6);
        FieldVector3D<DerivativeStructure> v  = createVector(-1, -0.1, -10, 6);
        checkPV(new FieldPVCoordinates<DerivativeStructure>(p2, v), new FieldPVCoordinates<DerivativeStructure>(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0,
                            FieldPVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm().getValue(),
                            1.0e-15);
    }

    @Test
    public void testGetters() {
        FieldVector3D<DerivativeStructure> p = createVector(1, 0.1, 10, 6);
        FieldVector3D<DerivativeStructure> v = createVector(-0.1, 1, 0, 6);
        FieldPVCoordinates<DerivativeStructure> pv = new FieldPVCoordinates<DerivativeStructure>(p, v);
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
        FieldPVCoordinates<DerivativeStructure> pv =
            new FieldPVCoordinates<DerivativeStructure>(createVector( 1,  0.1,  10, 6),
                                                        createVector(-1, -0.1, -10, 6),
                                                        createVector(10,  1.0, 100, 6));
        Assert.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    public void testNormalize() {
        RandomGenerator generator = new Well19937a(0x7ede9376e4e1ab5al);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final FieldPVCoordinates<DerivativeStructure> pv = randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getX().getValue();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getY().getValue();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getZ().getValue();
                        }
                    }).value(new DerivativeStructure(1, 2, 0, 0.0));
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
        return new FieldPVCoordinates<DerivativeStructure>(p0, p1, p2);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        return new FieldVector3D<DerivativeStructure>(new DerivativeStructure(params, 1, 0, x),
                              new DerivativeStructure(params, 1, 1, y),
                              new DerivativeStructure(params, 1, 2, z));
    }

    private void checkPV(FieldPVCoordinates<DerivativeStructure> expected, FieldPVCoordinates<DerivativeStructure> real, double epsilon) {
        Assert.assertEquals(expected.getPosition().getX().getReal(), real.getPosition().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getPosition().getY().getReal(), real.getPosition().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ().getReal(), real.getPosition().getZ().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX().getReal(), real.getVelocity().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY().getReal(), real.getVelocity().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ().getReal(), real.getVelocity().getZ().getReal(), epsilon);
    }

}
