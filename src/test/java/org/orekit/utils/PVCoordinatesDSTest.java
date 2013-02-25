/* Copyright 2002-2013 CS Systèmes d'Information
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


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;


public class PVCoordinatesDSTest {

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
        Assert.assertEquals(0, p.distance(pv.getPosition()).getValue(), 1.0e-15);
        Assert.assertEquals(0, v.distance(pv.getVelocity()).getValue(), 1.0e-15);
        Assert.assertEquals(0, createVector(-10, -1, 1.01, 6).distance(pv.getMomentum()).getValue(), 1.0e-15);

        FieldPVCoordinates<DerivativeStructure> pvn = pv.negate();
        Assert.assertEquals(0, createVector(-1, -0.1, -10, 6).distance(pvn.getPosition()).getValue(), 1.0e-15);
        Assert.assertEquals(0, createVector(0.1, -1, 0, 6).distance(pvn.getVelocity()).getValue(), 1.0e-15);
        Assert.assertEquals(0, createVector(-10, -1, 1.01, 6).distance(pvn.getMomentum()).getValue(), 1.0e-15);
    }

    @Test
    public void testToString() {
        FieldPVCoordinates<DerivativeStructure> pv =
            new FieldPVCoordinates<DerivativeStructure>(createVector(1, 0.1, 10, 6), createVector(-1, -0.1, -10, 6));
        Assert.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0)}", pv.toString());
    }

    @Test
    public void testInterpolatePolynomialPV() {
        Random random = new Random(0xae7771c9933407bdl);
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px    = randomPolynomial(5, random);
            PolynomialFunction py    = randomPolynomial(5, random);
            PolynomialFunction pz    = randomPolynomial(5, random);
            PolynomialFunction pxDot = px.polynomialDerivative();
            PolynomialFunction pyDot = py.polynomialDerivative();
            PolynomialFunction pzDot = pz.polynomialDerivative();

            List<Pair<AbsoluteDate, FieldPVCoordinates<DerivativeStructure>>> sample =
                    new ArrayList<Pair<AbsoluteDate,FieldPVCoordinates<DerivativeStructure>>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<DerivativeStructure> position = new FieldVector3D<DerivativeStructure>(new DerivativeStructure(3, 1, 0, px.value(dt)),
                                                     new DerivativeStructure(3, 1, 1, py.value(dt)),
                                                     new DerivativeStructure(3, 1, 2, pz.value(dt)));
                FieldVector3D<DerivativeStructure> velocity = new FieldVector3D<DerivativeStructure>(new DerivativeStructure(3, 1,    pxDot.value(dt)),
                                                     new DerivativeStructure(3, 1,    pyDot.value(dt)),
                                                     new DerivativeStructure(3, 1,    pzDot.value(dt)));
                sample.add(new Pair<AbsoluteDate, FieldPVCoordinates<DerivativeStructure>>(t0.shiftedBy(dt), new FieldPVCoordinates<DerivativeStructure>(position, velocity)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldPVCoordinates<DerivativeStructure> interpolated = FieldPVCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                Assert.assertEquals(px.value(dt),    p.getX().getValue(), 1.0e-15 * p.getNorm().getValue());
                Assert.assertEquals(1,               p.getX().getPartialDerivative(1, 0, 0), 1.0e-15);
                Assert.assertEquals(0,               p.getX().getPartialDerivative(0, 1, 0), 1.0e-15);
                Assert.assertEquals(0,               p.getX().getPartialDerivative(0, 0, 1), 1.0e-15);
                Assert.assertEquals(py.value(dt),    p.getY().getValue(), 1.0e-15 * p.getNorm().getValue());
                Assert.assertEquals(0,               p.getY().getPartialDerivative(1, 0, 0), 1.0e-15);
                Assert.assertEquals(1,               p.getY().getPartialDerivative(0, 1, 0), 1.0e-15);
                Assert.assertEquals(0,               p.getY().getPartialDerivative(0, 0, 1), 1.0e-15);
                Assert.assertEquals(pz.value(dt),    p.getZ().getValue(), 1.0e-15 * p.getNorm().getValue());
                Assert.assertEquals(0,               p.getZ().getPartialDerivative(1, 0, 0), 1.0e-15);
                Assert.assertEquals(0,               p.getZ().getPartialDerivative(0, 1, 0), 1.0e-15);
                Assert.assertEquals(1,               p.getZ().getPartialDerivative(0, 0, 1), 1.0e-15);
                Assert.assertEquals(pxDot.value(dt), v.getX().getValue(), 1.0e-15 * v.getNorm().getValue());
                Assert.assertEquals(pyDot.value(dt), v.getY().getValue(), 1.0e-15 * v.getNorm().getValue());
                Assert.assertEquals(pzDot.value(dt), v.getZ().getValue(), 1.0e-15 * v.getNorm().getValue());
            }

        }
    }

    @Test
    public void testInterpolatePolynomialPositionOnly() {
        Random random = new Random(0x88740a12e4299003l);
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px    = randomPolynomial(5, random);
            PolynomialFunction py    = randomPolynomial(5, random);
            PolynomialFunction pz    = randomPolynomial(5, random);
            PolynomialFunction pxDot = px.polynomialDerivative();
            PolynomialFunction pyDot = py.polynomialDerivative();
            PolynomialFunction pzDot = pz.polynomialDerivative();

            List<Pair<AbsoluteDate, FieldPVCoordinates<DerivativeStructure>>> sample =
                    new ArrayList<Pair<AbsoluteDate,FieldPVCoordinates<DerivativeStructure>>>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                FieldVector3D<DerivativeStructure> position = createVector(px.value(dt), py.value(dt), pz.value(dt), 6);
                sample.add(new Pair<AbsoluteDate, FieldPVCoordinates<DerivativeStructure>>(t0.shiftedBy(dt),
                        new FieldPVCoordinates<DerivativeStructure>(position, createVector(0, 0, 0, 6))));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldPVCoordinates<DerivativeStructure> interpolated = FieldPVCoordinates.interpolate(t0.shiftedBy(dt), false, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                Assert.assertEquals(px.value(dt),    p.getX().getValue(), 1.0e-14 * p.getNorm().getValue());
                Assert.assertEquals(py.value(dt),    p.getY().getValue(), 1.0e-14 * p.getNorm().getValue());
                Assert.assertEquals(pz.value(dt),    p.getZ().getValue(), 1.0e-14 * p.getNorm().getValue());
                Assert.assertEquals(pxDot.value(dt), v.getX().getValue(), 1.0e-14 * v.getNorm().getValue());
                Assert.assertEquals(pyDot.value(dt), v.getY().getValue(), 1.0e-14 * v.getNorm().getValue());
                Assert.assertEquals(pzDot.value(dt), v.getZ().getValue(), 1.0e-14 * v.getNorm().getValue());
            }

        }
    }

    @Test
    public void testInterpolateNonPolynomial() {
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

            List<Pair<AbsoluteDate, FieldPVCoordinates<DerivativeStructure>>> sample =
                    new ArrayList<Pair<AbsoluteDate,FieldPVCoordinates<DerivativeStructure>>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<DerivativeStructure> position = createVector( FastMath.cos(dt), FastMath.sin(dt), 0.0, 6);
                FieldVector3D<DerivativeStructure> velocity = createVector(-FastMath.sin(dt), FastMath.cos(dt), 0.0, 6);
                sample.add(new Pair<AbsoluteDate, FieldPVCoordinates<DerivativeStructure>>(t0.shiftedBy(dt), new FieldPVCoordinates<DerivativeStructure>(position, velocity)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldPVCoordinates<DerivativeStructure> interpolated = FieldPVCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                Assert.assertEquals(FastMath.cos(dt),    p.getX().getValue(), 3.0e-6 * p.getNorm().getValue());
                Assert.assertEquals(FastMath.sin(dt),    p.getY().getValue(), 3.0e-6 * p.getNorm().getValue());
                Assert.assertEquals(0,                   p.getZ().getValue(), 3.0e-6 * p.getNorm().getValue());
                Assert.assertEquals(-FastMath.sin(dt),   v.getX().getValue(), 3.0e-5 * v.getNorm().getValue());
                Assert.assertEquals( FastMath.cos(dt),   v.getY().getValue(), 3.0e-5 * v.getNorm().getValue());
                Assert.assertEquals(0,                   v.getZ().getValue(), 3.0e-5 * v.getNorm().getValue());
            }

    }

    private PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[ 1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
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
