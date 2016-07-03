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


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;


public class TimeStampedFieldPVCoordinatesTest {

    @Test
    public void testLinearConstructors() {
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv1 =
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.CCSDS_EPOCH,
                                                                       createVector( 1,  0.1, 10, 6),
                                                                       createVector(-1, -0.1, -10, 6),
                                                                       createVector(10,  1.0, 100, 6));
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv2 =
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.FIFTIES_EPOCH,
                                                                       createVector( 2,  0.2,  20, 6),
                                                                       createVector(-2, -0.2, -20, 6),
                                                                       createVector(20,  2.0, 200, 6));
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv3 =
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.GALILEO_EPOCH,
                                                                       createVector( 3,  0.3,  30, 6),
                                                                       createVector(-3, -0.3, -30, 6),
                                                                       createVector(30,  3.0, 300, 6));
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv4 =
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.JULIAN_EPOCH,
                                                                       createVector( 4,  0.4,  40, 6),
                                                                       createVector(-4, -0.4, -40, 6),
                                                                       createVector(40,  4.0, 400, 6));
        checkPV(pv4, new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.JULIAN_EPOCH, 4, pv1), 1.0e-15);
        checkPV(pv4, new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.JULIAN_EPOCH, new DerivativeStructure(6, 1, 4), pv1), 1.0e-15);
        checkPV(pv4, new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.JULIAN_EPOCH, new DerivativeStructure(6, 1, 4), pv1.toPVCoordinates()), 1.0e-15);
        checkPV(pv2, new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.FIFTIES_EPOCH, pv1, pv3), 1.0e-15);
        checkPV(pv3, new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.GALILEO_EPOCH, 1, pv1, 1, pv2), 1.0e-15);
        checkPV(pv3,
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.GALILEO_EPOCH,
                                                                       new DerivativeStructure(6, 1, 1), pv1,
                                                                       new DerivativeStructure(6, 1, 1), pv2),
                1.0e-15);
        checkPV(pv3,
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.GALILEO_EPOCH,
                                                                       new DerivativeStructure(6, 1, 1), pv1.toPVCoordinates(),
                                                                       new DerivativeStructure(6, 1, 1), pv2.toPVCoordinates()),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 2, pv4),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv3),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv4),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH,
                                                                       new DerivativeStructure(6, 1, 3), pv1,
                                                                       new DerivativeStructure(6, 1, 1), pv2,
                                                                       new DerivativeStructure(6, 1, 1), pv4),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH,
                                                                       new DerivativeStructure(6, 1, 3), pv1.toPVCoordinates(),
                                                                       new DerivativeStructure(6, 1, 1), pv2.toPVCoordinates(),
                                                                       new DerivativeStructure(6, 1, 1), pv4.toPVCoordinates()),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH,
                                                                       new DerivativeStructure(6, 1, 4), pv1,
                                                                       new DerivativeStructure(6, 1, 3), pv2,
                                                                       new DerivativeStructure(6, 1, 2), pv3,
                                                                       new DerivativeStructure(6, 1, 1), pv4),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH,
                                                                       new DerivativeStructure(6, 1, 4), pv1.toPVCoordinates(),
                                                                       new DerivativeStructure(6, 1, 3), pv2.toPVCoordinates(),
                                                                       new DerivativeStructure(6, 1, 2), pv3.toPVCoordinates(),
                                                                       new DerivativeStructure(6, 1, 1), pv4.toPVCoordinates()),
                1.0e-15);
    }

    @Test
    public void testShift() {
        FieldVector3D<DerivativeStructure> p1 = createVector(  1,  0.1,   10, 4);
        FieldVector3D<DerivativeStructure> v1 = createVector( -1, -0.1,  -10, 4);
        FieldVector3D<DerivativeStructure> a1 = createVector( 10,  1.0,  100, 4);
        FieldVector3D<DerivativeStructure> p2 = createVector(  7,  0.7,   70, 4);
        FieldVector3D<DerivativeStructure> v2 = createVector(-11, -1.1, -110, 4);
        FieldVector3D<DerivativeStructure> a2 = createVector( 10,  1.0,  100, 4);
        checkPV(new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH, p2, v2, a2),
                new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH.shiftedBy(1.0), p1, v1, a1).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0,
                            TimeStampedFieldPVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(createVector(-6, -0.6,-60, 4)).getNorm().getReal(),
                            1.0e-15);
    }

    @Test
    public void testToString() {
        Utils.setDataRoot("regular-data");
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv =
            new TimeStampedFieldPVCoordinates<DerivativeStructure>(AbsoluteDate.J2000_EPOCH,
                                                                   createVector( 1,  0.1,  10, 4),
                                                                   createVector(-1, -0.1, -10, 4),
                                                                   createVector(10,  1.0, 100, 4));
        Assert.assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    public void testInterpolatePolynomialPVA() {
        Random random = new Random(0xfe3945fcb8bf47cel);
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample =
                    new ArrayList<TimeStampedFieldPVCoordinates<DerivativeStructure>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<DerivativeStructure> position     = createVector(px.value(dt), py.value(dt), pz.value(dt), 4);
                FieldVector3D<DerivativeStructure> velocity     = createVector(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt), 4);
                FieldVector3D<DerivativeStructure> acceleration = createVector(pxDotDot.value(dt), pyDotDot.value(dt), pzDotDot.value(dt), 4);
                sample.add(new TimeStampedFieldPVCoordinates<DerivativeStructure>(t0.shiftedBy(dt), position, velocity, acceleration));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated =
                        TimeStampedFieldPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PVA, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                FieldVector3D<DerivativeStructure> a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(py.value(dt),       p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pz.value(dt),       p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pxDot.value(dt),    v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pyDot.value(dt),    v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pzDot.value(dt),    v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 6.0e-15 * a.getNorm().getReal());
                Assert.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 6.0e-15 * a.getNorm().getReal());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 6.0e-15 * a.getNorm().getReal());
            }

        }

    }

    @Test
    public void testInterpolatePolynomialPV() {
        Random random = new Random(0xae7771c9933407bdl);
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample =
                    new ArrayList<TimeStampedFieldPVCoordinates<DerivativeStructure>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<DerivativeStructure> position = createVector(px.value(dt), py.value(dt), pz.value(dt), 4);
                FieldVector3D<DerivativeStructure> velocity = createVector(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt), 4);
                sample.add(new TimeStampedFieldPVCoordinates<DerivativeStructure>(t0.shiftedBy(dt), position, velocity, createVector(0, 0, 0, 4)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated =
                        TimeStampedFieldPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PV, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                FieldVector3D<DerivativeStructure> a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(py.value(dt),       p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pz.value(dt),       p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pxDot.value(dt),    v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pyDot.value(dt),    v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pzDot.value(dt),    v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 1.0e-14 * a.getNorm().getReal());
                Assert.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 1.0e-14 * a.getNorm().getReal());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 1.0e-14 * a.getNorm().getReal());
            }

        }

    }

    @Test
    public void testInterpolatePolynomialPositionOnly() {
        Random random = new Random(0x88740a12e4299003l);
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample =
                    new ArrayList<TimeStampedFieldPVCoordinates<DerivativeStructure>>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                FieldVector3D<DerivativeStructure> position = createVector(px.value(dt), py.value(dt), pz.value(dt), 4);
                sample.add(new TimeStampedFieldPVCoordinates<DerivativeStructure>(t0.shiftedBy(dt),
                                                                                  position,
                                                                                  createVector(0, 0, 0, 4),
                                                                                  createVector(0, 0, 0, 4)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated =
                        TimeStampedFieldPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_P, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                FieldVector3D<DerivativeStructure> a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(py.value(dt),       p.getY().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pz.value(dt),       p.getZ().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pxDot.value(dt),    v.getX().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assert.assertEquals(pyDot.value(dt),    v.getY().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assert.assertEquals(pzDot.value(dt),    v.getZ().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assert.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 2.0e-13 * a.getNorm().getReal());
                Assert.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 2.0e-13 * a.getNorm().getReal());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 2.0e-13 * a.getNorm().getReal());
            }

        }
    }

    @Test
    public void testInterpolateNonPolynomial() {
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample =
                new ArrayList<TimeStampedFieldPVCoordinates<DerivativeStructure>>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            FieldVector3D<DerivativeStructure> position     = createVector( FastMath.cos(dt),  FastMath.sin(dt), 0.0, 4);
            FieldVector3D<DerivativeStructure> velocity     = createVector(-FastMath.sin(dt),  FastMath.cos(dt), 0.0, 4);
            FieldVector3D<DerivativeStructure> acceleration = createVector(-FastMath.cos(dt), -FastMath.sin(dt), 0.0, 4);
            sample.add(new TimeStampedFieldPVCoordinates<DerivativeStructure>(t0.shiftedBy(dt), position, velocity, acceleration));
        }

        for (double dt = 0; dt < 1.0; dt += 0.01) {
            TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated =
                    TimeStampedFieldPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PVA, sample);
            FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
            FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
            FieldVector3D<DerivativeStructure> a = interpolated.getAcceleration();
            Assert.assertEquals( FastMath.cos(dt),   p.getX().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assert.assertEquals( FastMath.sin(dt),   p.getY().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assert.assertEquals(0,                   p.getZ().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assert.assertEquals(-FastMath.sin(dt),   v.getX().getReal(), 3.0e-9  * v.getNorm().getReal());
            Assert.assertEquals( FastMath.cos(dt),   v.getY().getReal(), 3.0e-9  * v.getNorm().getReal());
            Assert.assertEquals(0,                   v.getZ().getReal(), 3.0e-9  * v.getNorm().getReal());
            Assert.assertEquals(-FastMath.cos(dt),   a.getX().getReal(), 4.0e-8  * a.getNorm().getReal());
            Assert.assertEquals(-FastMath.sin(dt),   a.getY().getReal(), 4.0e-8  * a.getNorm().getReal());
            Assert.assertEquals(0,                   a.getZ().getReal(), 4.0e-8  * a.getNorm().getReal());
        }

    }

    private PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[ 1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    private void checkPV(TimeStampedFieldPVCoordinates<DerivativeStructure> expected,
                         TimeStampedFieldPVCoordinates<DerivativeStructure> real, double epsilon) {
        Assert.assertEquals(expected.getDate(), real.getDate());
        Assert.assertEquals(expected.getPosition().getX().getReal(),     real.getPosition().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getPosition().getY().getReal(),     real.getPosition().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ().getReal(),     real.getPosition().getZ().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX().getReal(),     real.getVelocity().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY().getReal(),     real.getVelocity().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ().getReal(),     real.getVelocity().getZ().getReal(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getX().getReal(), real.getAcceleration().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getY().getReal(), real.getAcceleration().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getZ().getReal(), real.getAcceleration().getZ().getReal(), epsilon);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        return new FieldVector3D<DerivativeStructure>(new DerivativeStructure(params, 1, 0, x),
                                                      new DerivativeStructure(params, 1, 1, y),
                                                      new DerivativeStructure(params, 1, 2, z));
    }

}
