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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;


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
    @Deprecated // to be removed when PVCoordinates.interpolate is removed
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

            List<Pair<AbsoluteDate, PVCoordinates>> sample = new ArrayList<Pair<AbsoluteDate,PVCoordinates>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3D position     = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                Vector3D velocity     = new Vector3D(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt));
                sample.add(new Pair<AbsoluteDate, PVCoordinates>(t0.shiftedBy(dt),
                                                                 new PVCoordinates(position, velocity, Vector3D.ZERO)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                PVCoordinates interpolated = PVCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
                Vector3D p = interpolated.getPosition();
                Vector3D v = interpolated.getVelocity();
                Vector3D a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX(), 1.0e-15 * p.getNorm());
                Assert.assertEquals(py.value(dt),       p.getY(), 1.0e-15 * p.getNorm());
                Assert.assertEquals(pz.value(dt),       p.getZ(), 1.0e-15 * p.getNorm());
                Assert.assertEquals(pxDot.value(dt),    v.getX(), 1.0e-15 * v.getNorm());
                Assert.assertEquals(pyDot.value(dt),    v.getY(), 1.0e-15 * v.getNorm());
                Assert.assertEquals(pzDot.value(dt),    v.getZ(), 1.0e-15 * v.getNorm());
                Assert.assertEquals(pxDotDot.value(dt), a.getX(), 1.0e-14 * a.getNorm());
                Assert.assertEquals(pyDotDot.value(dt), a.getY(), 1.0e-14 * a.getNorm());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ(), 1.0e-14 * a.getNorm());
            }

        }
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

    private PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[ 1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
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
