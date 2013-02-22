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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class AngularCoordinatesDSTest {

    @Test
    public void testZeroRate() throws OrekitException {
        AngularCoordinatesDS AngularCoordinatesDS =
                new AngularCoordinatesDS(createRotation(0.48, 0.64, 0.36, 0.48, false), createVector(0, 0, 0, 4));
        Assert.assertEquals(createVector(0, 0, 0, 4), AngularCoordinatesDS.getRotationRate());
        double dt = 10.0;
        AngularCoordinatesDS shifted = AngularCoordinatesDS.shiftedBy(dt);
        Assert.assertEquals(createVector(0, 0, 0, 4), shifted.getRotationRate());
        Assert.assertEquals(AngularCoordinatesDS.getRotation(), shifted.getRotation());
    }

    @Test
    public void testShift() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        AngularCoordinatesDS AngularCoordinatesDS =
                new AngularCoordinatesDS(createRotation(1, 0, 0, 0, false),
                                       new Vector3DDS(rate, createVector(0, 0, 1, 4)));
        Assert.assertEquals(rate, AngularCoordinatesDS.getRotationRate().getNorm().getValue(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        AngularCoordinatesDS shifted = AngularCoordinatesDS.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm().getValue(), 1.0e-10);
        Assert.assertEquals(alpha, RotationDS.distance(AngularCoordinatesDS.getRotation(), shifted.getRotation()).getValue(), 1.0e-10);

        Vector3DDS xSat = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        Assert.assertEquals(0.0, xSat.subtract(createVector(FastMath.cos(alpha), FastMath.sin(alpha), 0, 4)).getNorm().getValue(), 1.0e-10);
        Vector3DDS ySat = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        Assert.assertEquals(0.0, ySat.subtract(createVector(-FastMath.sin(alpha), FastMath.cos(alpha), 0, 4)).getNorm().getValue(), 1.0e-10);
        Vector3DDS zSat = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assert.assertEquals(0.0, zSat.subtract(createVector(0, 0, 1, 4)).getNorm().getValue(), 1.0e-10);

    }

    @Test
    public void testToAC() {
        Random random = new Random(0xc9b4cf6c371108e0l);
        for (int i = 0; i < 100; ++i) {
            RotationDS r = randomRotation(random);
            Vector3DDS o = randomVector(random, 1.0e-3);
            AngularCoordinatesDS acds = new AngularCoordinatesDS(r, o);
            AngularCoordinates ac = acds.toAngularCoordinates();
            Assert.assertEquals(0, Rotation.distance(r.toRotation(), ac.getRotation()), 1.0e-15);
            Assert.assertEquals(0, Vector3DDS.distance(o, ac.getRotationRate()).getValue(), 1.0e-15);
        }
    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        AngularCoordinatesDS angularCoordinates =
                new AngularCoordinatesDS(createRotation(0.48, 0.64, 0.36, 0.48, false),
                                       new Vector3DDS(rate, createVector(0, 0, 1, 4)));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getValue(), 1.0e-10);
        double dt = 10.0;
        AngularCoordinatesDS shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm().getValue(), 1.0e-10);
        Assert.assertEquals(rate * dt, RotationDS.distance(angularCoordinates.getRotation(), shifted.getRotation()).getValue(), 1.0e-10);

        Vector3DDS shiftedX  = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        Vector3DDS shiftedY  = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        Vector3DDS shiftedZ  = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Vector3DDS originalX = angularCoordinates.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        Vector3DDS originalY = angularCoordinates.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        Vector3DDS originalZ = angularCoordinates.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3DDS.dotProduct(shiftedX, originalX).getValue(), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), Vector3DDS.dotProduct(shiftedX, originalY).getValue(), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3DDS.dotProduct(shiftedX, originalZ).getValue(), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), Vector3DDS.dotProduct(shiftedY, originalX).getValue(), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3DDS.dotProduct(shiftedY, originalY).getValue(), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3DDS.dotProduct(shiftedY, originalZ).getValue(), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3DDS.dotProduct(shiftedZ, originalX).getValue(), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3DDS.dotProduct(shiftedZ, originalY).getValue(), 1.0e-10);
        Assert.assertEquals( 1.0,                 Vector3DDS.dotProduct(shiftedZ, originalZ).getValue(), 1.0e-10);

        Vector3DDS forward = AngularCoordinatesDS.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm().getValue(), 1.0e-10);

        Vector3DDS reversed = AngularCoordinatesDS.estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm().getValue(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            RotationDS r = randomRotation(random);
            Vector3DDS o = randomVector(random, 1.0e-3);
            AngularCoordinatesDS ac = new AngularCoordinatesDS(r, o);
            AngularCoordinatesDS sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle().getValue(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm().getValue(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        AngularCoordinatesDS ac1 =
                new AngularCoordinatesDS(createRotation(0.48,  0.64, 0.36, 0.48, false), createVector(0, 0, 0, 4));
        AngularCoordinatesDS ac2 =
                new AngularCoordinatesDS(createRotation(0.36, -0.48, 0.48, 0.64, false), createVector(0, 0, 0, 4));

        AngularCoordinatesDS add12 = ac1.addOffset(ac2);
        AngularCoordinatesDS add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, RotationDS.distance(add12.getRotation(), add21.getRotation()).getValue(), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        Random random = new Random(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            RotationDS r1 = randomRotation(random);
            Vector3DDS o1 = randomVector(random, 1.0e-2);
            AngularCoordinatesDS ac1 = new AngularCoordinatesDS(r1, o1);
            RotationDS r2 = randomRotation(random);
            Vector3DDS o2 = randomVector(random, 1.0e-2);

            AngularCoordinatesDS ac2 = new AngularCoordinatesDS(r2, o2);
            AngularCoordinatesDS roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, RotationDS.distance(ac1.getRotation(), roundTripSA.getRotation()).getValue(), 1.0e-15);
            Assert.assertEquals(0.0, Vector3DDS.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()).getValue(), 1.0e-17);

            AngularCoordinatesDS roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, RotationDS.distance(ac1.getRotation(), roundTripAS.getRotation()).getValue(), 1.0e-15);
            Assert.assertEquals(0.0, Vector3DDS.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()).getValue(), 1.0e-17);

        }
    }

    @Test
    public void testRodriguesSymmetry()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = AngularCoordinatesDS.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                         AbsoluteDate.class, AngularCoordinatesDS.class,
                                                                         AbsoluteDate.class, AngularCoordinatesDS.class,
                                                                         double.class
                                                                    });
        getter.setAccessible(true);
        Method factory = AngularCoordinatesDS.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        DerivativeStructure[].class,
                                                                        AngularCoordinatesDS.class
                                                                    });
        factory.setAccessible(true);

        // check the two-way conversion result in identity
        Random random = new Random(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            RotationDS offsetRotation    = randomRotation(random);
            Vector3DDS offsetRate        = randomVector(random, 0.01);
            AngularCoordinatesDS offset  = new AngularCoordinatesDS(offsetRotation, offsetRate);
            RotationDS rotation          = randomRotation(random);
            Vector3DDS rotationRate      = randomVector(random, 0.01);
            AngularCoordinatesDS ac      = new AngularCoordinatesDS(rotation, rotationRate);
            double dt                  = 10.0 * random.nextDouble();
            DerivativeStructure[] rodrigues =
                    (DerivativeStructure[]) getter.invoke(null,
                                                          AbsoluteDate.J2000_EPOCH.shiftedBy(dt), ac,
                                                          AbsoluteDate.J2000_EPOCH, offset,
                                                          -0.9999);
            AngularCoordinatesDS rebuilt = (AngularCoordinatesDS) factory.invoke(null, rodrigues, offset.shiftedBy(dt));
            Assert.assertEquals(0.0, RotationDS.distance(rotation, rebuilt.getRotation()).getValue(), 1.0e-14);
            Assert.assertEquals(0.0, Vector3DDS.distance(rotationRate, rebuilt.getRotationRate()).getValue(), 1.0e-15);
        }

    }

    @Test
    public void testRodriguesSpecialCases()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = AngularCoordinatesDS.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        AbsoluteDate.class, AngularCoordinatesDS.class,
                                                                        AbsoluteDate.class, AngularCoordinatesDS.class,
                                                                        double.class
                                                                    });
        getter.setAccessible(true);
        Method factory = AngularCoordinatesDS.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        DerivativeStructure[].class,
                                                                        AngularCoordinatesDS.class
                                                                    });
        factory.setAccessible(true);

        // identity
        DerivativeStructure[] identity =
                (DerivativeStructure[]) getter.invoke(null,
                                                      AbsoluteDate.J2000_EPOCH, identity(),
                                                      AbsoluteDate.J2000_EPOCH, identity().revert(),
                                                      -0.9999);
        AngularCoordinatesDS acId = (AngularCoordinatesDS) factory.invoke(null, identity, identity());
        for (DerivativeStructure element : identity) {
            Assert.assertEquals(0.0, element.getValue(), Precision.SAFE_MIN);
        }
        Assert.assertEquals(0.0, acId.getRotation().getAngle().getValue(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm().getValue(), Precision.SAFE_MIN);

        // PI angle RotationDS (which is singular for non-modified Rodrigues vector)
        Random random = new Random(0x2158523e6accb859l);
        for (int i = 0; i < 100; ++i) {
            Vector3DDS axis = randomVector(random, 1.0);
            DerivativeStructure[] piRotation =
                    (DerivativeStructure[]) getter.invoke(null,
                                                          AbsoluteDate.J2000_EPOCH, 
                                                          new AngularCoordinatesDS(createRotation(axis, FastMath.PI),
                                                                                 createVector(0, 0, 0, 4)),
                                                          AbsoluteDate.J2000_EPOCH,
                                                          identity(),
                                                          -0.9999);
            AngularCoordinatesDS acPi = (AngularCoordinatesDS) factory.invoke(null, piRotation, identity());
            Assert.assertEquals(FastMath.PI, acPi.getRotation().getAngle().getValue(), 1.0e-15);
            Assert.assertEquals(0.0, Vector3DDS.angle(axis, acPi.getRotation().getAxis()).sin().getValue(), 1.0e-15);
            Assert.assertEquals(0.0, acPi.getRotationRate().getNorm().getValue(), 1.0e-16);
        }

        // 2 PI angle RotationDS (which is singular for modified Rodrigues vector)
        AngularCoordinatesDS ac = new AngularCoordinatesDS(createRotation(1, 0, 0, 0, false).revert(), createVector(1, 0, 0, 4));
        Assert.assertNull(getter.invoke(null,
                                        AbsoluteDate.J2000_EPOCH.shiftedBy(10.0), ac,
                                        AbsoluteDate.J2000_EPOCH, identity().revert(),
                                        -0.9999));
        Assert.assertNotNull(getter.invoke(null,
                                           AbsoluteDate.J2000_EPOCH.shiftedBy(10.0), ac,
                                           AbsoluteDate.J2000_EPOCH,
                                           new AngularCoordinatesDS(createRotation(createVector(1, 0, 0, 4), 0.1), createVector(0, 0, 0, 4)),
                                           -0.9999));

    }

    @Test
    public void testInterpolationSimple() {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        AngularCoordinatesDS reference = new AngularCoordinatesDS(createRotation(createVector(0, 0, 1, 4), alpha0),
                                                              new Vector3DDS(omega, createVector(0, 0, -1, 4)));

        List<Pair<AbsoluteDate, AngularCoordinatesDS>> sample = new ArrayList<Pair<AbsoluteDate,AngularCoordinatesDS>>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            RotationDS r = reference.shiftedBy(dt).getRotation();
            Vector3DDS rate = reference.shiftedBy(dt).getRotationRate();
            sample.add(new Pair<AbsoluteDate, AngularCoordinatesDS>(date.shiftedBy(dt), new AngularCoordinatesDS(r, rate)));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            AngularCoordinatesDS interpolated = AngularCoordinatesDS.interpolate(date.shiftedBy(dt), true, sample);
            RotationDS r    = interpolated.getRotation();
            Vector3DDS rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, RotationDS.distance(reference.shiftedBy(dt).getRotation(), r).getValue(), 1.0e-15);
            Assert.assertEquals(0.0, Vector3DDS.distance(reference.shiftedBy(dt).getRotationRate(), rate).getValue(), 5.0e-15);
        }

    }

    @Test
    public void testInterpolationRotationOnly() {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        AngularCoordinatesDS reference = new AngularCoordinatesDS(createRotation(createVector(0, 0, 1, 4), alpha0),
                                                              new Vector3DDS(omega, createVector(0, 0, -1, 4)));

        List<Pair<AbsoluteDate, AngularCoordinatesDS>> sample = new ArrayList<Pair<AbsoluteDate,AngularCoordinatesDS>>();
        for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            RotationDS r = reference.shiftedBy(dt).getRotation();
            sample.add(new Pair<AbsoluteDate, AngularCoordinatesDS>(date.shiftedBy(dt), new AngularCoordinatesDS(r, createVector(0, 0, 0, 4))));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            AngularCoordinatesDS interpolated = AngularCoordinatesDS.interpolate(date.shiftedBy(dt), false, sample);
            RotationDS r    = interpolated.getRotation();
            Vector3DDS rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, RotationDS.distance(reference.shiftedBy(dt).getRotation(), r).getValue(), 3.0e-4);
            Assert.assertEquals(0.0, Vector3DDS.distance(reference.shiftedBy(dt).getRotationRate(), rate).getValue(), 1.0e-2);
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
        List<Pair<AbsoluteDate,AngularCoordinatesDS>> sample = new ArrayList<Pair<AbsoluteDate,AngularCoordinatesDS>>();
        for (double[] row : params) {
            AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            RotationDS     r = createRotation(row[1], 0.0, 0.0, row[2], false);
            Vector3DDS     o = new Vector3DDS(row[3], createVector(0, 0, 1, 4));
            sample.add(new Pair<AbsoluteDate, AngularCoordinatesDS>(t, new AngularCoordinatesDS(r, o)));
        }
        for (double dt = 0; dt < 29000; dt += 120) {
            AngularCoordinatesDS shifted      = sample.get(0).getValue().shiftedBy(dt);
            AngularCoordinatesDS interpolated = AngularCoordinatesDS.interpolate(t0.shiftedBy(dt), true, sample);
            Assert.assertEquals(0.0,
                                RotationDS.distance(shifted.getRotation(), interpolated.getRotation()).getValue(),
                                1.3e-7);
            Assert.assertEquals(0.0,
                                Vector3DDS.distance(shifted.getRotationRate(), interpolated.getRotationRate()).getValue(),
                                1.0e-11);
        }

    }

    private Vector3DDS randomVector(Random random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new Vector3DDS(n, createVector(x, y, z, 4).normalize());
    }

    private RotationDS randomRotation(Random random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return createRotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private AngularCoordinatesDS identity() {
        return new AngularCoordinatesDS(createRotation(1, 0, 0, 0, false),
                                        createVector(0, 0, 0, 4));
    }

    private RotationDS createRotation(Vector3DDS axis, double angle) {
        return new RotationDS(axis, new DerivativeStructure(4, 1, angle));
    }

    private RotationDS createRotation(double q0, double q1, double q2, double q3,
                                      boolean needsNormalization) {
        return new RotationDS(new DerivativeStructure(4, 1, 0, q0),
                              new DerivativeStructure(4, 1, 1, q1),
                              new DerivativeStructure(4, 1, 2, q2),
                              new DerivativeStructure(4, 1, 3, q3),
                              needsNormalization);
    }

    private Vector3DDS createVector(double x, double y, double z, int params) {
        return new Vector3DDS(new DerivativeStructure(params, 1, 0, x),
                              new DerivativeStructure(params, 1, 1, y),
                              new DerivativeStructure(params, 1, 2, z));
    }

}

