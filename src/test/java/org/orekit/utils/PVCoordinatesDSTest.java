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
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;


public class PVCoordinatesDSTest {

    @Test
    public void testLinearConstructors() {
        PVCoordinatesDS pv1 = new PVCoordinatesDS(createVector(1, 0.1, 10, 6),
                                                  createVector(-1, -0.1, -10, 6));
        PVCoordinatesDS pv2 = new PVCoordinatesDS(createVector(2, 0.2, 20, 6),
                                                  createVector(-2, -0.2, -20, 6));
        PVCoordinatesDS pv3 = new PVCoordinatesDS(createVector(3, 0.3, 30, 6),
                                                  createVector(-3, -0.3, -30, 6));
        PVCoordinatesDS pv4 = new PVCoordinatesDS(createVector(4, 0.4, 40, 6),
                                                  createVector(-4, -0.4, -40, 6));
        checkPV(pv4, new PVCoordinatesDS(4, pv1), 1.0e-15);
        checkPV(pv4, new PVCoordinatesDS(new DerivativeStructure(6, 1, 4), pv1), 1.0e-15);
        checkPV(pv4, new PVCoordinatesDS(new DerivativeStructure(6, 1, 4), pv1.toPVCoordinates()), 1.0e-15);
        checkPV(pv2, new PVCoordinatesDS(pv1, pv3), 1.0e-15);
        checkPV(pv3, new PVCoordinatesDS(1, pv1, 1, pv2), 1.0e-15);
        checkPV(pv3, new PVCoordinatesDS(new DerivativeStructure(6, 1, 1), pv1,
                                         new DerivativeStructure(6, 1, 1), pv2),
                1.0e-15);
        checkPV(pv3, new PVCoordinatesDS(new DerivativeStructure(6, 1, 1), pv1.toPVCoordinates(),
                                         new DerivativeStructure(6, 1, 1), pv2.toPVCoordinates()),
                1.0e-15);
        checkPV(new PVCoordinatesDS(2, pv4), new PVCoordinatesDS(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new PVCoordinatesDS(3, pv3), new PVCoordinatesDS(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new PVCoordinatesDS(3, pv3),
                new PVCoordinatesDS(new DerivativeStructure(6, 1, 3), pv1,
                                    new DerivativeStructure(6, 1, 1), pv2,
                                    new DerivativeStructure(6, 1, 1), pv4),
                1.0e-15);
        checkPV(new PVCoordinatesDS(3, pv3),
                new PVCoordinatesDS(new DerivativeStructure(6, 1, 3), pv1.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 1), pv2.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 1), pv4.toPVCoordinates()),
                1.0e-15);
        checkPV(new PVCoordinatesDS(5, pv4),
                new PVCoordinatesDS(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
        checkPV(new PVCoordinatesDS(5, pv4),
                new PVCoordinatesDS(new DerivativeStructure(6, 1, 4), pv1,
                                    new DerivativeStructure(6, 1, 3), pv2,
                                    new DerivativeStructure(6, 1, 2), pv3,
                                    new DerivativeStructure(6, 1, 1), pv4),
                1.0e-15);
        checkPV(new PVCoordinatesDS(5, pv4),
                new PVCoordinatesDS(new DerivativeStructure(6, 1, 4), pv1.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 3), pv2.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 2), pv3.toPVCoordinates(),
                                    new DerivativeStructure(6, 1, 1), pv4.toPVCoordinates()),
                1.0e-15);
    }

    @Test
    public void testShift() {
        Vector3DDS p1 = createVector(1, 0.1, 10, 6);
        Vector3DDS p2 = createVector(2, 0.2, 20, 6);
        Vector3DDS v  = createVector(-1, -0.1, -10, 6);
        checkPV(new PVCoordinatesDS(p2, v), new PVCoordinatesDS(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0,
                            PVCoordinatesDS.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm().getValue(),
                            1.0e-15);
    }

    @Test
    public void testGetters() {
        Vector3DDS p = createVector(1, 0.1, 10, 6);
        Vector3DDS v = createVector(-0.1, 1, 0, 6);
        PVCoordinatesDS pv = new PVCoordinatesDS(p, v);
        Assert.assertEquals(0, Vector3DDS.distance(p, pv.getPosition()).getValue(), 1.0e-15);
        Assert.assertEquals(0, Vector3DDS.distance(v, pv.getVelocity()).getValue(), 1.0e-15);
        Assert.assertEquals(0, Vector3DDS.distance(createVector(-10, -1, 1.01, 6), pv.getMomentum()).getValue(), 1.0e-15);

        PVCoordinatesDS pvn = pv.negate();
        Assert.assertEquals(0, Vector3DDS.distance(createVector(-1, -0.1, -10, 6), pvn.getPosition()).getValue(), 1.0e-15);
        Assert.assertEquals(0, Vector3DDS.distance(createVector(0.1, -1, 0, 6), pvn.getVelocity()).getValue(), 1.0e-15);
        Assert.assertEquals(0, Vector3DDS.distance(createVector(-10, -1, 1.01, 6), pvn.getMomentum()).getValue(), 1.0e-15);
    }

    @Test
    public void testToString() {
        PVCoordinatesDS pv =
            new PVCoordinatesDS(createVector(1, 0.1, 10, 6), createVector(-1, -0.1, -10, 6));
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

            List<Pair<AbsoluteDate, PVCoordinatesDS>> sample = new ArrayList<Pair<AbsoluteDate,PVCoordinatesDS>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3DDS position = new Vector3DDS(new DerivativeStructure(3, 1, 0, px.value(dt)),
                                                     new DerivativeStructure(3, 1, 1, py.value(dt)),
                                                     new DerivativeStructure(3, 1, 2, pz.value(dt)));
                Vector3DDS velocity = new Vector3DDS(new DerivativeStructure(3, 1,    pxDot.value(dt)),
                                                     new DerivativeStructure(3, 1,    pyDot.value(dt)),
                                                     new DerivativeStructure(3, 1,    pzDot.value(dt)));
                sample.add(new Pair<AbsoluteDate, PVCoordinatesDS>(t0.shiftedBy(dt), new PVCoordinatesDS(position, velocity)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                PVCoordinatesDS interpolated = PVCoordinatesDS.interpolate(t0.shiftedBy(dt), true, sample);
                Vector3DDS p = interpolated.getPosition();
                Vector3DDS v = interpolated.getVelocity();
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

            List<Pair<AbsoluteDate, PVCoordinatesDS>> sample = new ArrayList<Pair<AbsoluteDate,PVCoordinatesDS>>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                Vector3DDS position = createVector(px.value(dt), py.value(dt), pz.value(dt), 6);
                sample.add(new Pair<AbsoluteDate, PVCoordinatesDS>(t0.shiftedBy(dt),
                        new PVCoordinatesDS(position, createVector(0, 0, 0, 6))));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                PVCoordinatesDS interpolated = PVCoordinatesDS.interpolate(t0.shiftedBy(dt), false, sample);
                Vector3DDS p = interpolated.getPosition();
                Vector3DDS v = interpolated.getVelocity();
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

            List<Pair<AbsoluteDate, PVCoordinatesDS>> sample = new ArrayList<Pair<AbsoluteDate,PVCoordinatesDS>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3DDS position = createVector( FastMath.cos(dt), FastMath.sin(dt), 0.0, 6);
                Vector3DDS velocity = createVector(-FastMath.sin(dt), FastMath.cos(dt), 0.0, 6);
                sample.add(new Pair<AbsoluteDate, PVCoordinatesDS>(t0.shiftedBy(dt), new PVCoordinatesDS(position, velocity)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                PVCoordinatesDS interpolated = PVCoordinatesDS.interpolate(t0.shiftedBy(dt), true, sample);
                Vector3DDS p = interpolated.getPosition();
                Vector3DDS v = interpolated.getVelocity();
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

    private Vector3DDS createVector(double x, double y, double z, int params) {
        return new Vector3DDS(new DerivativeStructure(params, 1, 0, x),
                              new DerivativeStructure(params, 1, 1, y),
                              new DerivativeStructure(params, 1, 2, z));
    }

    private void checkPV(PVCoordinatesDS expected, PVCoordinatesDS real, double epsilon) {
        Assert.assertEquals(expected.getPosition().getX().getValue(), real.getPosition().getX().getValue(), epsilon);
        Assert.assertEquals(expected.getPosition().getY().getValue(), real.getPosition().getY().getValue(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ().getValue(), real.getPosition().getZ().getValue(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX().getValue(), real.getVelocity().getX().getValue(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY().getValue(), real.getVelocity().getY().getValue(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ().getValue(), real.getVelocity().getZ().getValue(), epsilon);
    }

}
