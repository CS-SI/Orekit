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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

public class AngularCoordinatesTest {

    @Test
    public void testZeroRate() throws OrekitException {
        AngularCoordinates AngularCoordinates =
                new AngularCoordinates(new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                       Vector3D.ZERO);
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
    public void testRodriguesSymmetry()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = AngularCoordinates.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        Rotation.class, double.class,
                                                                        AngularCoordinates.class
                                                                    });
        getter.setAccessible(true);
        Method factory = AngularCoordinates.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        Rotation.class, double[].class, double[].class
                                                                    });
        factory.setAccessible(true);

        // check the two-way conversion result in identity
        Random random = new Random(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            Rotation offset       = randomRotation(random);
            Rotation rotation     = randomRotation(random);
            Vector3D rotationRate = randomVector(random);
            double[][] rodrigues  = (double[][]) getter.invoke(null, offset.revert(), -0.9999,
                                                               new AngularCoordinates(rotation, rotationRate));
            AngularCoordinates ac = (AngularCoordinates) factory.invoke(null, offset, rodrigues[0], rodrigues[1]);
            Assert.assertEquals(0.0, Rotation.distance(rotation, ac.getRotation()), 2.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(rotationRate, ac.getRotationRate()), 2.0e-9);
        }

    }

    @Test
    public void testRodriguesSpecialCases()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = AngularCoordinates.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        Rotation.class, double.class,
                                                                        AngularCoordinates.class
                                                                    });
        getter.setAccessible(true);
        Method factory = AngularCoordinates.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        Rotation.class, double[].class, double[].class
                                                                    });
        factory.setAccessible(true);

        // identity
        double[][] identity = (double[][]) getter.invoke(null, Rotation.IDENTITY, -0.9999,
                                                         new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO));
        AngularCoordinates acId = (AngularCoordinates) factory.invoke(null, Rotation.IDENTITY, identity[0], identity[1]);
        for (double[] row : identity) {
            for (double element : row) {
                Assert.assertEquals(0.0, element, 1.0e-15);
            }
        }
        Assert.assertEquals(0.0, acId.getRotation().getAngle(), 1.0e-15);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm(), 1.0e-15);

        // PI angle rotation (which is singular for non-modified Rodrigues vector)
        Random random = new Random(0x2158523e6accb859l);
        for (int i = 0; i < 100; ++i) {
            Vector3D axis = randomVector(random);
            double[][] piRotation = (double[][]) getter.invoke(null, Rotation.IDENTITY, -0.9999,
                                                               new AngularCoordinates(new Rotation(axis, FastMath.PI),
                                                                                      Vector3D.ZERO));
            AngularCoordinates acPi = (AngularCoordinates) factory.invoke(null, Rotation.IDENTITY, piRotation[0], piRotation[1]);
            Assert.assertEquals(FastMath.PI, acPi.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, FastMath.sin(Vector3D.angle(axis, acPi.getRotation().getAxis())), 1.0e-15);
            Assert.assertEquals(0.0, acPi.getRotationRate().getNorm(), 1.0e-16);
        }

        // 2 PI angle rotation (which is singular for modified Rodrigues vector)
        for (int i = 0; i < 100; ++i) {
            AngularCoordinates ac =
                    new AngularCoordinates(new Rotation(randomVector(random), 2 * FastMath.PI), Vector3D.ZERO);
            Assert.assertNull(getter.invoke(null, Rotation.IDENTITY, -0.9999, ac));
            Assert.assertNotNull(getter.invoke(null, new Rotation(Vector3D.PLUS_I, 0.1), -0.9999, ac));
        }

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
            Assert.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 2.4e-4);
            Assert.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 1.8e-3);
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
            Assert.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 4.0e-3);
            Assert.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 0.15);
        }

    }

    private Vector3D randomVector(Random random) {
        return new Vector3D(random.nextDouble() * 10000.0,
                            random.nextDouble() * 10000.0,
                            random.nextDouble() * 10000.0);
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

