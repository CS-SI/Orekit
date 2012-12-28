/* Copyright 2002-2012 CS Systèmes d'Information
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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class AngularCoordinatesTest {

    @Test
    public void testZeroRate() throws OrekitException {
        AngularCoordinates AngularCoordinates =
                new AngularCoordinates(new Rotation(0.48, 0.64, 0.36, 0.48, false), Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, AngularCoordinates.getRotationRate());
        double dt = 10.0;
        AngularCoordinates shifted = AngularCoordinates.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationRate());
        Assert.assertEquals(AngularCoordinates.getRotation(), shifted.getRotation());
    }

    @Test
    public void testShift() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        AngularCoordinates AngularCoordinates =
                new AngularCoordinates(Rotation.IDENTITY,
                                       new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, AngularCoordinates.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        AngularCoordinates shifted = AngularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(AngularCoordinates.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(FastMath.cos(alpha), FastMath.sin(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-FastMath.sin(alpha), FastMath.cos(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        AngularCoordinates angularCoordinates =
                new AngularCoordinates(new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                       new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        AngularCoordinates shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(angularCoordinates.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Vector3D originalX = angularCoordinates.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D originalY = angularCoordinates.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D originalZ = angularCoordinates.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals( 1.0,                 Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        Vector3D forward = AngularCoordinates.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm(), 1.0e-10);

        Vector3D reversed = AngularCoordinates.estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-3);
            AngularCoordinates ac = new AngularCoordinates(r, o);
            AngularCoordinates sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        AngularCoordinates ac1 =
                new AngularCoordinates(new Rotation(0.48,  0.64, 0.36, 0.48, false), Vector3D.ZERO);
        AngularCoordinates ac2 =
                new AngularCoordinates(new Rotation(0.36, -0.48, 0.48, 0.64, false), Vector3D.ZERO);

        AngularCoordinates add12 = ac1.addOffset(ac2);
        AngularCoordinates add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, Rotation.distance(add12.getRotation(), add21.getRotation()), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        Random random = new Random(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            Rotation r1 = randomRotation(random);
            Vector3D o1 = randomVector(random, 1.0e-2);
            AngularCoordinates ac1 = new AngularCoordinates(r1, o1);
            Rotation r2 = randomRotation(random);
            Vector3D o2 = randomVector(random, 1.0e-2);

            AngularCoordinates ac2 = new AngularCoordinates(r2, o2);
            AngularCoordinates roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripSA.getRotation()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()), 1.0e-17);

            AngularCoordinates roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripAS.getRotation()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()), 1.0e-17);

        }
    }

    @Test
    public void testRodriguesSymmetry()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = AngularCoordinates.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                         AbsoluteDate.class, AngularCoordinates.class,
                                                                         AbsoluteDate.class, AngularCoordinates.class,
                                                                         double.class
                                                                    });
        getter.setAccessible(true);
        Method factory = AngularCoordinates.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        DerivativeStructure[].class,
                                                                        AngularCoordinates.class
                                                                    });
        factory.setAccessible(true);

        // check the two-way conversion result in identity
        Random random = new Random(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            Rotation offsetRotation    = randomRotation(random);
            Vector3D offsetRate        = randomVector(random, 0.01);
            AngularCoordinates offset  = new AngularCoordinates(offsetRotation, offsetRate);
            Rotation rotation          = randomRotation(random);
            Vector3D rotationRate      = randomVector(random, 0.01);
            AngularCoordinates ac      = new AngularCoordinates(rotation, rotationRate);
            double dt                  = 10.0 * random.nextDouble();
            DerivativeStructure[] rodrigues =
                    (DerivativeStructure[]) getter.invoke(null,
                                                          AbsoluteDate.J2000_EPOCH.shiftedBy(dt), ac,
                                                          AbsoluteDate.J2000_EPOCH, offset,
                                                          -0.9999);
            AngularCoordinates rebuilt = (AngularCoordinates) factory.invoke(null, rodrigues, offset.shiftedBy(dt));
            Assert.assertEquals(0.0, Rotation.distance(rotation, rebuilt.getRotation()), 1.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(rotationRate, rebuilt.getRotationRate()), 1.0e-15);
        }

    }

    @Test
    public void testRodriguesSpecialCases()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = AngularCoordinates.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        AbsoluteDate.class, AngularCoordinates.class,
                                                                        AbsoluteDate.class, AngularCoordinates.class,
                                                                        double.class
                                                                    });
        getter.setAccessible(true);
        Method factory = AngularCoordinates.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        DerivativeStructure[].class,
                                                                        AngularCoordinates.class
                                                                    });
        factory.setAccessible(true);

        // identity
        DerivativeStructure[] identity =
                (DerivativeStructure[]) getter.invoke(null,
                                                      AbsoluteDate.J2000_EPOCH, AngularCoordinates.IDENTITY,
                                                      AbsoluteDate.J2000_EPOCH, AngularCoordinates.IDENTITY.revert(),
                                                      -0.9999);
        AngularCoordinates acId = (AngularCoordinates) factory.invoke(null, identity, AngularCoordinates.IDENTITY);
        for (DerivativeStructure element : identity) {
            Assert.assertEquals(0.0, element.getValue(), Precision.SAFE_MIN);
            Assert.assertEquals(0.0, element.getPartialDerivative(1), Precision.SAFE_MIN);
        }
        Assert.assertEquals(0.0, acId.getRotation().getAngle(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm(), Precision.SAFE_MIN);

        // PI angle rotation (which is singular for non-modified Rodrigues vector)
        Random random = new Random(0x2158523e6accb859l);
        for (int i = 0; i < 100; ++i) {
            Vector3D axis = randomVector(random, 1.0);
            DerivativeStructure[] piRotation =
                    (DerivativeStructure[]) getter.invoke(null,
                                                          AbsoluteDate.J2000_EPOCH, 
                                                          new AngularCoordinates(new Rotation(axis, FastMath.PI),
                                                                                 Vector3D.ZERO),
                                                          AbsoluteDate.J2000_EPOCH,
                                                          AngularCoordinates.IDENTITY,
                                                          -0.9999);
            AngularCoordinates acPi = (AngularCoordinates) factory.invoke(null, piRotation, AngularCoordinates.IDENTITY);
            Assert.assertEquals(FastMath.PI, acPi.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, FastMath.sin(Vector3D.angle(axis, acPi.getRotation().getAxis())), 1.0e-15);
            Assert.assertEquals(0.0, acPi.getRotationRate().getNorm(), 1.0e-16);
        }

        // 2 PI angle rotation (which is singular for modified Rodrigues vector)
        AngularCoordinates ac = new AngularCoordinates(Rotation.IDENTITY.revert(), Vector3D.PLUS_I);
        Assert.assertNull(getter.invoke(null,
                                        AbsoluteDate.J2000_EPOCH.shiftedBy(10.0), ac,
                                        AbsoluteDate.J2000_EPOCH, AngularCoordinates.IDENTITY.revert(),
                                        -0.9999));
        Assert.assertNotNull(getter.invoke(null,
                                           AbsoluteDate.J2000_EPOCH.shiftedBy(10.0), ac,
                                           AbsoluteDate.J2000_EPOCH,
                                           new AngularCoordinates(new Rotation(Vector3D.PLUS_I, 0.1), Vector3D.ZERO),
                                           -0.9999));

    }

    @Test
    public void testInterpolationSimple() {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        AngularCoordinates reference = new AngularCoordinates(new Rotation(Vector3D.PLUS_K, alpha0),
                                                              new Vector3D(omega, Vector3D.MINUS_K));

        List<Pair<AbsoluteDate, AngularCoordinates>> sample = new ArrayList<Pair<AbsoluteDate,AngularCoordinates>>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            Rotation r = reference.shiftedBy(dt).getRotation();
            Vector3D rate = reference.shiftedBy(dt).getRotationRate();
            sample.add(new Pair<AbsoluteDate, AngularCoordinates>(date.shiftedBy(dt), new AngularCoordinates(r, rate)));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            AngularCoordinates interpolated = AngularCoordinates.interpolate(date.shiftedBy(dt), true, sample);
            Rotation r    = interpolated.getRotation();
            Vector3D rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 5.0e-15);
        }

    }

    @Test
    public void testInterpolationRotationOnly() {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        AngularCoordinates reference = new AngularCoordinates(new Rotation(Vector3D.PLUS_K, alpha0),
                                                              new Vector3D(omega, Vector3D.MINUS_K));

        List<Pair<AbsoluteDate, AngularCoordinates>> sample = new ArrayList<Pair<AbsoluteDate,AngularCoordinates>>();
        for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            Rotation r = reference.shiftedBy(dt).getRotation();
            sample.add(new Pair<AbsoluteDate, AngularCoordinates>(date.shiftedBy(dt), new AngularCoordinates(r, Vector3D.ZERO)));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            AngularCoordinates interpolated = AngularCoordinates.interpolate(date.shiftedBy(dt), false, sample);
            Rotation r    = interpolated.getRotation();
            Vector3D rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 3.0e-4);
            Assert.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 1.0e-2);
        }

    }

    @Test
    public void testInterpolationGTODIssue() {
        AbsoluteDate t0 = new AbsoluteDate("2004-04-06T19:59:28.000", TimeScalesFactory.getTAI());
        double[][] params = new double[][] {
            { 0.0, -0.3802356750911964, -0.9248896320037013, 7.292115030462892e-5 },
            { 4.0,  0.1345716955788532, -0.990903859488413,  7.292115033301528e-5 },
            { 8.0, -0.613127541102373,   0.7899839354960061, 7.292115037371062e-5 }
        };
        List<Pair<AbsoluteDate,AngularCoordinates>> sample = new ArrayList<Pair<AbsoluteDate,AngularCoordinates>>();
        for (double[] row : params) {
            AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            Rotation     r = new Rotation(row[1], 0.0, 0.0, row[2], false);
            Vector3D     o = new Vector3D(row[3], Vector3D.PLUS_K);
            sample.add(new Pair<AbsoluteDate, AngularCoordinates>(t, new AngularCoordinates(r, o)));
        }
        for (double dt = 0; dt < 29000; dt += 120) {
            AngularCoordinates shifted      = sample.get(0).getValue().shiftedBy(dt);
            AngularCoordinates interpolated = AngularCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
            Assert.assertEquals(0.0,
                                Rotation.distance(shifted.getRotation(), interpolated.getRotation()),
                                1.3e-7);
            Assert.assertEquals(0.0,
                                Vector3D.distance(shifted.getRotationRate(), interpolated.getRotationRate()),
                                1.0e-11);
        }

    }

    private Vector3D randomVector(Random random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private Rotation randomRotation(Random random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

}

