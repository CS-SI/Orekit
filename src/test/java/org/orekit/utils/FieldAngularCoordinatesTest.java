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

import org.apache.commons.math3.RealFieldElement;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class FieldAngularCoordinatesTest {

    @Test
    public void testZeroRate() throws OrekitException {
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.48, 0.64, 0.36, 0.48, false), createVector(0, 0, 0, 4));
        Assert.assertEquals(createVector(0, 0, 0, 4), angularCoordinates.getRotationRate());
        double dt = 10.0;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(createVector(0, 0, 0, 4), shifted.getRotationRate());
        Assert.assertEquals(angularCoordinates.getRotation(), shifted.getRotation());
    }

    @Test
    public void testShift() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(1, 0, 0, 0, false),
                                       new FieldVector3D<DerivativeStructure>(rate, createVector(0, 0, 1, 4)));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getReal(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm().getReal(), 1.0e-10);
        Assert.assertEquals(alpha, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> xSat = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        Assert.assertEquals(0.0, xSat.subtract(createVector(FastMath.cos(alpha), FastMath.sin(alpha), 0, 4)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<DerivativeStructure> ySat = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        Assert.assertEquals(0.0, ySat.subtract(createVector(-FastMath.sin(alpha), FastMath.cos(alpha), 0, 4)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<DerivativeStructure> zSat = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assert.assertEquals(0.0, zSat.subtract(createVector(0, 0, 1, 4)).getNorm().getReal(), 1.0e-10);

    }

    @Test
    public void testToAC() {
        Random random = new Random(0xc9b4cf6c371108e0l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldAngularCoordinates<DerivativeStructure> acds = new FieldAngularCoordinates<DerivativeStructure>(r, o);
            AngularCoordinates ac = acds.toAngularCoordinates();
            Assert.assertEquals(0, Rotation.distance(r.toRotation(), ac.getRotation()), 1.0e-15);
            Assert.assertEquals(0, FieldVector3D.distance(o, ac.getRotationRate()).getReal(), 1.0e-15);
        }
    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.48, 0.64, 0.36, 0.48, false),
                                       new FieldVector3D<DerivativeStructure>(rate, createVector(0, 0, 1, 4)));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getReal(), 1.0e-10);
        double dt = 10.0;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm().getReal(), 1.0e-10);
        Assert.assertEquals(rate * dt, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> shiftedX  = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        FieldVector3D<DerivativeStructure> shiftedY  = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        FieldVector3D<DerivativeStructure> shiftedZ  = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        FieldVector3D<DerivativeStructure> originalX = angularCoordinates.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        FieldVector3D<DerivativeStructure> originalY = angularCoordinates.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        FieldVector3D<DerivativeStructure> originalZ = angularCoordinates.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assert.assertEquals( FastMath.cos(rate * dt), FieldVector3D.dotProduct(shiftedX, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), FieldVector3D.dotProduct(shiftedX, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedX, originalZ).getReal(), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), FieldVector3D.dotProduct(shiftedY, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), FieldVector3D.dotProduct(shiftedY, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedY, originalZ).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 1.0,                 FieldVector3D.dotProduct(shiftedZ, originalZ).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> forward = FieldAngularCoordinates.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm().getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> reversed = FieldAngularCoordinates.estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm().getReal(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldAngularCoordinates<DerivativeStructure> ac = new FieldAngularCoordinates<DerivativeStructure>(r, o);
            FieldAngularCoordinates<DerivativeStructure> sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm().getReal(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        FieldAngularCoordinates<DerivativeStructure> ac1 =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.48,  0.64, 0.36, 0.48, false), createVector(0, 0, 0, 4));
        FieldAngularCoordinates<DerivativeStructure> ac2 =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.36, -0.48, 0.48, 0.64, false), createVector(0, 0, 0, 4));

        FieldAngularCoordinates<DerivativeStructure> add12 = ac1.addOffset(ac2);
        FieldAngularCoordinates<DerivativeStructure> add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, FieldRotation.distance(add12.getRotation(), add21.getRotation()).getReal(), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        Random random = new Random(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            FieldRotation<DerivativeStructure> r1 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o1 = randomVector(random, 1.0e-2);
            FieldAngularCoordinates<DerivativeStructure> ac1 = new FieldAngularCoordinates<DerivativeStructure>(r1, o1);
            FieldRotation<DerivativeStructure> r2 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o2 = randomVector(random, 1.0e-2);

            FieldAngularCoordinates<DerivativeStructure> ac2 = new FieldAngularCoordinates<DerivativeStructure>(r2, o2);
            FieldAngularCoordinates<DerivativeStructure> roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripSA.getRotation()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()).getReal(), 1.0e-17);

            FieldAngularCoordinates<DerivativeStructure> roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripAS.getRotation()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()).getReal(), 1.0e-17);

        }
    }

    @Test
    public void testRodriguesSymmetry()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = FieldAngularCoordinates.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                         AbsoluteDate.class, FieldAngularCoordinates.class,
                                                                         AbsoluteDate.class, FieldAngularCoordinates.class,
                                                                         double.class
                                                                    });
        getter.setAccessible(true);
        Method factory = FieldAngularCoordinates.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        RealFieldElement[][].class,
                                                                        FieldAngularCoordinates.class
                                                                    });
        factory.setAccessible(true);

        // check the two-way conversion result in identity
        Random random = new Random(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            FieldRotation<DerivativeStructure> offsetRotation    = randomRotation(random);
            FieldVector3D<DerivativeStructure> offsetRate        = randomVector(random, 0.01);
            FieldAngularCoordinates<DerivativeStructure> offset  = new FieldAngularCoordinates<DerivativeStructure>(offsetRotation, offsetRate);
            FieldRotation<DerivativeStructure> rotation          = randomRotation(random);
            FieldVector3D<DerivativeStructure> rotationRate      = randomVector(random, 0.01);
            FieldAngularCoordinates<DerivativeStructure> ac      = new FieldAngularCoordinates<DerivativeStructure>(rotation, rotationRate);
            double dt                  = 10.0 * random.nextDouble();
            DerivativeStructure[][] rodrigues =
                    (DerivativeStructure[][]) getter.invoke(null,
                                                            AbsoluteDate.J2000_EPOCH.shiftedBy(dt), ac,
                                                            AbsoluteDate.J2000_EPOCH, offset,
                                                            -0.9999);
            @SuppressWarnings("unchecked")
            FieldAngularCoordinates<DerivativeStructure> rebuilt =
            (FieldAngularCoordinates<DerivativeStructure>) factory.invoke(null, rodrigues, offset.shiftedBy(dt));
            Assert.assertEquals(0.0, FieldRotation.distance(rotation, rebuilt.getRotation()).getReal(), 1.0e-14);
            Assert.assertEquals(0.0, FieldVector3D.distance(rotationRate, rebuilt.getRotationRate()).getReal(), 1.0e-15);
        }

    }

    @Test
    public void testRodriguesSpecialCases()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // use reflection to test the private static methods
        Method getter  = FieldAngularCoordinates.class.getDeclaredMethod("getModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        AbsoluteDate.class, FieldAngularCoordinates.class,
                                                                        AbsoluteDate.class, FieldAngularCoordinates.class,
                                                                        double.class
                                                                    });
        getter.setAccessible(true);
        Method factory = FieldAngularCoordinates.class.getDeclaredMethod("createFromModifiedRodrigues",
                                                                    new Class<?>[] {
                                                                        RealFieldElement[][].class,
                                                                        FieldAngularCoordinates.class
                                                                    });
        factory.setAccessible(true);

        // identity
        DerivativeStructure[][] identity =
                (DerivativeStructure[][]) getter.invoke(null,
                                                        AbsoluteDate.J2000_EPOCH, identity(),
                                                        AbsoluteDate.J2000_EPOCH, identity().revert(),
                                                        -0.9999);
        @SuppressWarnings("unchecked")
        FieldAngularCoordinates<DerivativeStructure> acId =
                (FieldAngularCoordinates<DerivativeStructure>) factory.invoke(null, identity, identity());
        for (DerivativeStructure element : identity[0]) {
            Assert.assertEquals(0.0, element.getReal(), Precision.SAFE_MIN);
        }
        for (DerivativeStructure element : identity[1]) {
            Assert.assertEquals(0.0, element.getReal(), Precision.SAFE_MIN);
        }
        Assert.assertEquals(0.0, acId.getRotation().getAngle().getReal(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm().getReal(), Precision.SAFE_MIN);

        // PI angle FieldRotation<DerivativeStructure> (which is singular for non-modified Rodrigues vector)
        Random random = new Random(0x2158523e6accb859l);
        for (int i = 0; i < 100; ++i) {
            FieldVector3D<DerivativeStructure> axis = randomVector(random, 1.0);
            DerivativeStructure[][] piRotation =
                    (DerivativeStructure[][]) getter.invoke(null,
                                                            AbsoluteDate.J2000_EPOCH, 
                                                            new FieldAngularCoordinates<DerivativeStructure>(createRotation(axis, FastMath.PI),
                                                                                                             createVector(0, 0, 0, 4)),
                                                            AbsoluteDate.J2000_EPOCH,
                                                            identity(),
                                                            -0.9999);
            @SuppressWarnings("unchecked")
            FieldAngularCoordinates<DerivativeStructure> acPi =
                    (FieldAngularCoordinates<DerivativeStructure>) factory.invoke(null, piRotation, identity());
            Assert.assertEquals(FastMath.PI, acPi.getRotation().getAngle().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.angle(axis, acPi.getRotation().getAxis()).sin().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, acPi.getRotationRate().getNorm().getReal(), 1.0e-16);
        }

        // 2 PI angle FieldRotation<DerivativeStructure> (which is singular for modified Rodrigues vector)
        FieldAngularCoordinates<DerivativeStructure> ac = new FieldAngularCoordinates<DerivativeStructure>(createRotation(1, 0, 0, 0, false).revert(), createVector(1, 0, 0, 4));
        Assert.assertNull(getter.invoke(null,
                                        AbsoluteDate.J2000_EPOCH.shiftedBy(10.0), ac,
                                        AbsoluteDate.J2000_EPOCH, identity().revert(),
                                        -0.9999));
        Assert.assertNotNull(getter.invoke(null,
                                           AbsoluteDate.J2000_EPOCH.shiftedBy(10.0), ac,
                                           AbsoluteDate.J2000_EPOCH,
                                           new FieldAngularCoordinates<DerivativeStructure>(createRotation(createVector(1, 0, 0, 4), 0.1), createVector(0, 0, 0, 4)),
                                           -0.9999));

    }

    @Test
    public void testInterpolationSimple() {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        FieldAngularCoordinates<DerivativeStructure> reference = new FieldAngularCoordinates<DerivativeStructure>(createRotation(createVector(0, 0, 1, 4), alpha0),
                                                              new FieldVector3D<DerivativeStructure>(omega, createVector(0, 0, -1, 4)));

        List<Pair<AbsoluteDate, FieldAngularCoordinates<DerivativeStructure>>> sample =
                new ArrayList<Pair<AbsoluteDate,FieldAngularCoordinates<DerivativeStructure>>>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            FieldRotation<DerivativeStructure> r = reference.shiftedBy(dt).getRotation();
            FieldVector3D<DerivativeStructure> rate = reference.shiftedBy(dt).getRotationRate();
            sample.add(new Pair<AbsoluteDate, FieldAngularCoordinates<DerivativeStructure>>(date.shiftedBy(dt), new FieldAngularCoordinates<DerivativeStructure>(r, rate)));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            FieldAngularCoordinates<DerivativeStructure> interpolated = FieldAngularCoordinates.interpolate(date.shiftedBy(dt), true, sample);
            FieldRotation<DerivativeStructure> r    = interpolated.getRotation();
            FieldVector3D<DerivativeStructure> rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, FieldRotation.distance(reference.shiftedBy(dt).getRotation(), r).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate).getReal(), 5.0e-15);
        }

    }

    @Test
    public void testInterpolationRotationOnly() {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        FieldAngularCoordinates<DerivativeStructure> reference = new FieldAngularCoordinates<DerivativeStructure>(createRotation(createVector(0, 0, 1, 4), alpha0),
                                                              new FieldVector3D<DerivativeStructure>(omega, createVector(0, 0, -1, 4)));

        List<Pair<AbsoluteDate, FieldAngularCoordinates<DerivativeStructure>>> sample =
                new ArrayList<Pair<AbsoluteDate,FieldAngularCoordinates<DerivativeStructure>>>();
        for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            FieldRotation<DerivativeStructure> r = reference.shiftedBy(dt).getRotation();
            sample.add(new Pair<AbsoluteDate, FieldAngularCoordinates<DerivativeStructure>>(date.shiftedBy(dt), new FieldAngularCoordinates<DerivativeStructure>(r, createVector(0, 0, 0, 4))));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            FieldAngularCoordinates<DerivativeStructure> interpolated = FieldAngularCoordinates.interpolate(date.shiftedBy(dt), false, sample);
            FieldRotation<DerivativeStructure> r    = interpolated.getRotation();
            FieldVector3D<DerivativeStructure> rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, FieldRotation.distance(reference.shiftedBy(dt).getRotation(), r).getReal(), 3.0e-4);
            Assert.assertEquals(0.0, FieldVector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate).getReal(), 1.0e-2);
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
        List<Pair<AbsoluteDate,FieldAngularCoordinates<DerivativeStructure>>> sample =
                new ArrayList<Pair<AbsoluteDate,FieldAngularCoordinates<DerivativeStructure>>>();
        for (double[] row : params) {
            AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            FieldRotation<DerivativeStructure>     r = createRotation(row[1], 0.0, 0.0, row[2], false);
            FieldVector3D<DerivativeStructure>     o = new FieldVector3D<DerivativeStructure>(row[3], createVector(0, 0, 1, 4));
            sample.add(new Pair<AbsoluteDate, FieldAngularCoordinates<DerivativeStructure>>(t, new FieldAngularCoordinates<DerivativeStructure>(r, o)));
        }
        for (double dt = 0; dt < 29000; dt += 120) {
            FieldAngularCoordinates<DerivativeStructure> shifted      = sample.get(0).getValue().shiftedBy(dt);
            FieldAngularCoordinates<DerivativeStructure> interpolated = FieldAngularCoordinates.interpolate(t0.shiftedBy(dt), true, sample);
            Assert.assertEquals(0.0,
                                FieldRotation.distance(shifted.getRotation(), interpolated.getRotation()).getReal(),
                                1.3e-7);
            Assert.assertEquals(0.0,
                                FieldVector3D.distance(shifted.getRotationRate(), interpolated.getRotationRate()).getReal(),
                                1.0e-11);
        }

    }

    private FieldVector3D<DerivativeStructure> randomVector(Random random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new FieldVector3D<DerivativeStructure>(n, createVector(x, y, z, 4).normalize());
    }

    private FieldRotation<DerivativeStructure> randomRotation(Random random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return createRotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private FieldAngularCoordinates<DerivativeStructure> identity() {
        return new FieldAngularCoordinates<DerivativeStructure>(createRotation(1, 0, 0, 0, false),
                                        createVector(0, 0, 0, 4));
    }

    private FieldRotation<DerivativeStructure> createRotation(FieldVector3D<DerivativeStructure> axis, double angle) {
        return new FieldRotation<DerivativeStructure>(axis, new DerivativeStructure(4, 1, angle));
    }

    private FieldRotation<DerivativeStructure> createRotation(double q0, double q1, double q2, double q3,
                                      boolean needsNormalization) {
        return new FieldRotation<DerivativeStructure>(new DerivativeStructure(4, 1, 0, q0),
                              new DerivativeStructure(4, 1, 1, q1),
                              new DerivativeStructure(4, 1, 2, q2),
                              new DerivativeStructure(4, 1, 3, q3),
                              needsNormalization);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        return new FieldVector3D<DerivativeStructure>(new DerivativeStructure(params, 1, 0, x),
                              new DerivativeStructure(params, 1, 1, y),
                              new DerivativeStructure(params, 1, 2, z));
    }

}

