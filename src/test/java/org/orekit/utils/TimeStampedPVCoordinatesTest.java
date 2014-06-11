/* Copyright 2002-2014 CS Systèmes d'Information
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

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;


public class TimeStampedPVCoordinatesTest {

    @Test
    public void testShift() {
        Vector3D p1 = new Vector3D( 1,  0.1,  10);
        Vector3D p2 = new Vector3D( 2,  0.2,  20);
        Vector3D v  = new Vector3D(-1, -0.1, -10);
        checkPV(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, p2, v),
                new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(1.0), p1, v).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0, TimeStampedPVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm(), 1.0e-15);
    }

    @Test
    public void testToString() {
        TimeStampedPVCoordinates pv =
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                         new Vector3D( 1,  0.1,  10), new Vector3D(-1, -0.1, -10));
        Assert.assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0)}", pv.toString());
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

            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                Vector3D velocity = new Vector3D(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedPVCoordinates interpolated = TimeStampedPVCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
                Vector3D p = interpolated.getPosition();
                Vector3D v = interpolated.getVelocity();
                Assert.assertEquals(px.value(dt),    p.getX(), 1.0e-15 * p.getNorm());
                Assert.assertEquals(py.value(dt),    p.getY(), 1.0e-15 * p.getNorm());
                Assert.assertEquals(pz.value(dt),    p.getZ(), 1.0e-15 * p.getNorm());
                Assert.assertEquals(pxDot.value(dt), v.getX(), 1.0e-15 * v.getNorm());
                Assert.assertEquals(pyDot.value(dt), v.getY(), 1.0e-15 * v.getNorm());
                Assert.assertEquals(pzDot.value(dt), v.getZ(), 1.0e-15 * v.getNorm());
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

            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, Vector3D.ZERO));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedPVCoordinates interpolated = TimeStampedPVCoordinates.interpolate(t0.shiftedBy(dt), false, sample);
                Vector3D p = interpolated.getPosition();
                Vector3D v = interpolated.getVelocity();
                Assert.assertEquals(px.value(dt),    p.getX(), 1.0e-14 * p.getNorm());
                Assert.assertEquals(py.value(dt),    p.getY(), 1.0e-14 * p.getNorm());
                Assert.assertEquals(pz.value(dt),    p.getZ(), 1.0e-14 * p.getNorm());
                Assert.assertEquals(pxDot.value(dt), v.getX(), 1.0e-14 * v.getNorm());
                Assert.assertEquals(pyDot.value(dt), v.getY(), 1.0e-14 * v.getNorm());
                Assert.assertEquals(pzDot.value(dt), v.getZ(), 1.0e-14 * v.getNorm());
            }

        }
    }

    @Test
    public void testInterpolateNonPolynomial() {
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3D position = new Vector3D( FastMath.cos(dt), FastMath.sin(dt), 0.0);
                Vector3D velocity = new Vector3D(-FastMath.sin(dt), FastMath.cos(dt), 0.0);
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedPVCoordinates interpolated = TimeStampedPVCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
                Vector3D p = interpolated.getPosition();
                Vector3D v = interpolated.getVelocity();
                Assert.assertEquals(FastMath.cos(dt),    p.getX(), 3.0e-6 * p.getNorm());
                Assert.assertEquals(FastMath.sin(dt),    p.getY(), 3.0e-6 * p.getNorm());
                Assert.assertEquals(0,                   p.getZ(), 3.0e-6 * p.getNorm());
                Assert.assertEquals(-FastMath.sin(dt),   v.getX(), 3.0e-5 * v.getNorm());
                Assert.assertEquals( FastMath.cos(dt),   v.getY(), 3.0e-5 * v.getNorm());
                Assert.assertEquals(0,                   v.getZ(), 3.0e-5 * v.getNorm());
            }

    }

    private PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[ 1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    private void checkPV(TimeStampedPVCoordinates expected, TimeStampedPVCoordinates real, double epsilon) {
        Assert.assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        Assert.assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
    }

}
