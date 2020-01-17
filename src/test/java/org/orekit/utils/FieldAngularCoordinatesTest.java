/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;

public class FieldAngularCoordinatesTest {

    @Test
    public void testDerivativesStructuresNeg() {
        try {
            AngularCoordinates.IDENTITY.toDerivativeStructureRotation(-1);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }

    }

    @Test
    public void testDerivativesStructures3() {
        try {
            AngularCoordinates.IDENTITY.toDerivativeStructureRotation(3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }

    }

    @Test
    public void testDerivativesStructures0() {
        RandomGenerator random = new Well1024a(0x18a0a08fd63f047al);

        FieldRotation<Decimal64> r    = randomRotation64(random);
        FieldVector3D<Decimal64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Decimal64> oDot = randomVector64(random, 1.0e-2);
        FieldAngularCoordinates<Decimal64> ac = new FieldAngularCoordinates<>(r, o, oDot);
        FieldAngularCoordinates<Decimal64> rebuilt = new FieldAngularCoordinates<>(ac.toDerivativeStructureRotation(0));
        Assert.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, rebuilt.getRotationRate().getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures1() {
        RandomGenerator random = new Well1024a(0x8f8fc6d27bbdc46dl);

        FieldRotation<Decimal64> r    = randomRotation64(random);
        FieldVector3D<Decimal64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Decimal64> oDot = randomVector64(random, 1.0e-2);
        FieldAngularCoordinates<Decimal64> ac = new FieldAngularCoordinates<>(r, o, oDot);
        FieldAngularCoordinates<Decimal64> rebuilt = new FieldAngularCoordinates<>(ac.toDerivativeStructureRotation(1));
        Assert.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldVector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures2() {
        RandomGenerator random = new Well1024a(0x1633878dddac047dl);

        FieldRotation<Decimal64> r    = randomRotation64(random);
        FieldVector3D<Decimal64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Decimal64> oDot = randomVector64(random, 1.0e-2);
        FieldAngularCoordinates<Decimal64> ac = new FieldAngularCoordinates<>(r, o, oDot);
        FieldAngularCoordinates<Decimal64> rebuilt = new FieldAngularCoordinates<>(ac.toDerivativeStructureRotation(2));
        Assert.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldVector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldVector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()).getReal(), 1.0e-15);
    }

    @Test
    public void testZeroRate() {
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<>(createRotation(0.48, 0.64, 0.36, 0.48, false),
                                              createVector(0, 0, 0, 4),
                                              createVector(0, 0, 0, 4));
        Assert.assertEquals(createVector(0, 0, 0, 4), angularCoordinates.getRotationRate());
        double dt = 10.0;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(0.0, shifted.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, shifted.getRotationRate().getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-15);
    }

    @Test
    public void testShift() {
        double rate = 2 * FastMath.PI / (12 * 60);
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<>(createRotation(1, 0, 0, 0, false),
                                              new FieldVector3D<>(rate, createVector(0, 0, 1, 4)),
                                              createVector(0, 0, 0, 4));
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
        RandomGenerator random = new Well1024a(0xc9b4cf6c371108e0l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldVector3D<DerivativeStructure> a = randomVector(random, 1.0e-3);
            FieldAngularCoordinates<DerivativeStructure> acds = new FieldAngularCoordinates<>(r, o, a);
            AngularCoordinates ac = acds.toAngularCoordinates();
            Assert.assertEquals(0, Rotation.distance(r.toRotation(), ac.getRotation()), 1.0e-15);
            Assert.assertEquals(0, FieldVector3D.distance(o, ac.getRotationRate()).getReal(), 1.0e-15);
        }
    }

    @Test
    public void testSpin() {
        double rate = 2 * FastMath.PI / (12 * 60);
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<>(createRotation(0.48, 0.64, 0.36, 0.48, false),
                                              new FieldVector3D<>(rate, createVector(0, 0, 1, 4)),
                                              createVector(0, 0, 0, 4));
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
        RandomGenerator random = new Well1024a(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldVector3D<DerivativeStructure> a = randomVector(random, 1.0e-3);
            FieldAngularCoordinates<DerivativeStructure> ac = new FieldAngularCoordinates<>(r, o, a);
            FieldAngularCoordinates<DerivativeStructure> sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm().getReal(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        FieldAngularCoordinates<DerivativeStructure> ac1 =
                new FieldAngularCoordinates<>(createRotation(0.48,  0.64, 0.36, 0.48, false),
                                              createVector(0, 0, 0, 4),
                                              createVector(0, 0, 0, 4));
        FieldAngularCoordinates<DerivativeStructure> ac2 =
                new FieldAngularCoordinates<>(createRotation(0.36, -0.48, 0.48, 0.64, false),
                                              createVector(0, 0, 0, 4),
                                              createVector(0, 0, 0, 4));

        FieldAngularCoordinates<DerivativeStructure> add12 = ac1.addOffset(ac2);
        FieldAngularCoordinates<DerivativeStructure> add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, FieldRotation.distance(add12.getRotation(), add21.getRotation()).getReal(), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        RandomGenerator random = new Well1024a(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            FieldRotation<DerivativeStructure> r1 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o1 = randomVector(random, 1.0e-2);
            FieldVector3D<DerivativeStructure> a1 = randomVector(random, 1.0e-2);
            FieldAngularCoordinates<DerivativeStructure> ac1 = new FieldAngularCoordinates<>(r1, o1, a1);

            FieldRotation<DerivativeStructure> r2 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o2 = randomVector(random, 1.0e-2);
            FieldVector3D<DerivativeStructure> a2 = randomVector(random, 1.0e-2);
            FieldAngularCoordinates<DerivativeStructure> ac2 = new FieldAngularCoordinates<>(r2, o2, a2);

            FieldAngularCoordinates<DerivativeStructure> roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripSA.getRotation()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()).getReal(), 2.0e-17);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()).getReal(), 2.0e-17);

            FieldAngularCoordinates<DerivativeStructure> roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripAS.getRotation()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()).getReal(), 2.0e-17);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()).getReal(), 2.0e-17);
        }
    }

    @Test
    public void testResultAngularCoordinates() {
        Field<Decimal64> field = Decimal64Field.getInstance();
        Decimal64 zero = field.getZero();
        FieldVector3D<Decimal64> pos_B = new FieldVector3D<>(zero.add(-0.23723922134606962    ),
                                                             zero.add(-0.9628700341496187     ),
                                                             zero.add(0.1288365211879871      ));
        FieldVector3D<Decimal64> vel_B = new FieldVector3D<>(zero.add(2.6031808214929053E-7   ),
                                                             zero.add(-8.141147978260352E-8   ),
                                                             zero.add(-1.2908618653852553E-7  ));
        FieldVector3D<Decimal64> acc_B = new FieldVector3D<>(zero.add( -1.395403347295246E-10 ),
                                                             zero.add( -2.7451871050415643E-12),
                                                             zero.add( -2.781723303703499E-10 ));

        FieldPVCoordinates<Decimal64> B = new FieldPVCoordinates<Decimal64>(pos_B, vel_B, acc_B);


        FieldVector3D<Decimal64> pos_A = new FieldVector3D<>(zero.add(-0.44665912825286425 ),
                                                             zero.add(-0.00965737694923173 ),
                                                             zero.add(-0.894652087807798   ));
        FieldVector3D<Decimal64> vel_A = new FieldVector3D<>(zero.add(-8.897373390367405E-4),
                                                             zero.add(2.7825509772757976E-4),
                                                             zero.add(4.412017757970883E-4 ));
        FieldVector3D<Decimal64> acc_A = new FieldVector3D<>(zero.add( 4.743595125825107E-7),
                                                             zero.add( 1.01875177357042E-8 ),
                                                             zero.add( 9.520371766790574E-7));

        FieldPVCoordinates<Decimal64> A = new FieldPVCoordinates<>(pos_A, vel_A, acc_A);

        FieldPVCoordinates<Decimal64> PLUS_K = new FieldPVCoordinates<>(new FieldVector3D<>(field.getZero(), field.getZero(), field.getOne()),
                                                                        new FieldVector3D<>(field.getZero(), field.getZero(), field.getZero()),
                                                                        new FieldVector3D<>(field.getZero(), field.getZero(), field.getZero()));

        FieldPVCoordinates<Decimal64> PLUS_J = new FieldPVCoordinates<>(new FieldVector3D<>(field.getZero(), field.getOne(), field.getZero()),
                                                                        new FieldVector3D<>(field.getZero(), field.getZero(), field.getZero()),
                                                                        new FieldVector3D<>(field.getZero(), field.getZero(), field.getZero()));


        FieldAngularCoordinates<Decimal64> fac = new FieldAngularCoordinates<>(A, B, PLUS_K, PLUS_J, 1.0e-6);

        AngularCoordinates ac = new AngularCoordinates(A.toPVCoordinates(), B.toPVCoordinates(), PLUS_K.toPVCoordinates(), PLUS_J.toPVCoordinates(), 1.0e-6);

        Assert.assertTrue( fac.getRotationRate().toVector3D().equals(ac.getRotationRate()));

    }

    @Test
    public void testIdentity() {
        FieldAngularCoordinates<Decimal64> identity = FieldAngularCoordinates.getIdentity(Decimal64Field.getInstance());
        Assert.assertEquals(0.0,
                            identity.getRotation().getAngle().getReal(),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            identity.getRotationRate().getNorm().getReal(),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            identity.getRotationAcceleration().getNorm().getReal(),
                            1.0e-15);
    }

    @Test
    public void testConversionConstructor() {
        AngularCoordinates ac = new AngularCoordinates(new Rotation(Vector3D.MINUS_J, 0.15, RotationConvention.VECTOR_OPERATOR),
                                                       new Vector3D(0.001, 0.002, 0.003),
                                                       new Vector3D(-1.0e-6, -3.0e-6, 7.0e-6));
        FieldAngularCoordinates<Decimal64> ac64 = new FieldAngularCoordinates<>(Decimal64Field.getInstance(), ac);
        Assert.assertEquals(0.0,
                            Rotation.distance(ac.getRotation(), ac64.getRotation().toRotation()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(ac.getRotationRate(), ac64.getRotationRate().toVector3D()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(ac.getRotationAcceleration(), ac64.getRotationAcceleration().toVector3D()),
                            1.0e-15);
    }

    @Test
    public void testApplyTo() {

        RandomGenerator random = new Well1024a(0xbad5894f4c475905l);
        for (int i = 0; i < 1000; ++i) {
            FieldRotation<DerivativeStructure> rotation             = randomRotation(random);
            FieldVector3D<DerivativeStructure> rotationRate         = randomVector(random, 0.01);
            FieldVector3D<DerivativeStructure> rotationAcceleration = randomVector(random, 0.01);
            FieldAngularCoordinates<DerivativeStructure> ac         = new FieldAngularCoordinates<>(rotation,
                                                                                                    rotationRate,
                                                                                                    rotationAcceleration);

            FieldVector3D<DerivativeStructure> p                    = randomVector(random, 10.0);
            FieldVector3D<DerivativeStructure> v                    = randomVector(random, 10.0);
            FieldVector3D<DerivativeStructure> a                    = randomVector(random, 10.0);
            FieldPVCoordinates<DerivativeStructure> pv              = new FieldPVCoordinates<>(p, v, a);

            PVCoordinates reference = ac.toAngularCoordinates().applyTo(pv.toPVCoordinates());

            FieldPVCoordinates<DerivativeStructure> res1 = ac.applyTo(pv.toPVCoordinates());
            Assert.assertEquals(0.0, Vector3D.distance(reference.getPosition(),     res1.getPosition().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getVelocity(),     res1.getVelocity().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getAcceleration(), res1.getAcceleration().toVector3D()), 1.0e-15);

            FieldPVCoordinates<DerivativeStructure> res2 = ac.applyTo(pv);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getPosition(),     res2.getPosition().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getVelocity(),     res2.getVelocity().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getAcceleration(), res2.getAcceleration().toVector3D()), 1.0e-15);


            TimeStampedFieldPVCoordinates<DerivativeStructure> res3 =
                            ac.applyTo(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, pv));
            Assert.assertEquals(0.0, AbsoluteDate.J2000_EPOCH.durationFrom(res3.getDate().toAbsoluteDate()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getPosition(),     res3.getPosition().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getVelocity(),     res3.getVelocity().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getAcceleration(), res3.getAcceleration().toVector3D()), 1.0e-15);

            TimeStampedFieldPVCoordinates<DerivativeStructure> res4 =
                            ac.applyTo(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, pv.toPVCoordinates()));
            Assert.assertEquals(0.0, AbsoluteDate.J2000_EPOCH.durationFrom(res4.getDate().toAbsoluteDate()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getPosition(),     res4.getPosition().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getVelocity(),     res4.getVelocity().toVector3D()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(reference.getAcceleration(), res4.getAcceleration().toVector3D()), 1.0e-15);

        }

    }

    @Test
    public void testRodriguesVsDouble() {

        RandomGenerator random = new Well1024a(0x4beeee3d8d2abacal);
        for (int i = 0; i < 1000; ++i) {
            FieldRotation<DerivativeStructure> rotation             = randomRotation(random);
            FieldVector3D<DerivativeStructure> rotationRate         = randomVector(random, 0.01);
            FieldVector3D<DerivativeStructure> rotationAcceleration = randomVector(random, 0.01);
            FieldAngularCoordinates<DerivativeStructure> ac         = new FieldAngularCoordinates<>(rotation, rotationRate, rotationAcceleration);

            DerivativeStructure[][] rod = ac.getModifiedRodrigues(1.0);
            double[][] rodRef = ac.toAngularCoordinates().getModifiedRodrigues(1.0);
            Assert.assertEquals(rodRef.length, rod.length);
            for (int k = 0; k < rodRef.length; ++k) {
                Assert.assertEquals(rodRef[k].length, rod[k].length);
                for (int l = 0; l < rodRef[k].length; ++l) {
                    Assert.assertEquals(rodRef[k][l], rod[k][l].getReal(), 1.0e-15 * FastMath.abs(rodRef[k][l]));
                }
            }

            FieldAngularCoordinates<DerivativeStructure> rebuilt = FieldAngularCoordinates.createFromModifiedRodrigues(rod);
            AngularCoordinates rebuiltRef                        = AngularCoordinates.createFromModifiedRodrigues(rodRef);
            Assert.assertEquals(0.0, Rotation.distance(rebuiltRef.getRotation(), rebuilt.getRotation().toRotation()), 1.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(rebuiltRef.getRotationRate(), rebuilt.getRotationRate().toVector3D()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(rebuiltRef.getRotationAcceleration(), rebuilt.getRotationAcceleration().toVector3D()), 1.0e-15);

        }

    }

    @Test
    public void testRodriguesSymmetry() {

        // check the two-way conversion result in identity
        RandomGenerator random = new Well1024a(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            FieldRotation<DerivativeStructure> rotation             = randomRotation(random);
            FieldVector3D<DerivativeStructure> rotationRate         = randomVector(random, 0.01);
            FieldVector3D<DerivativeStructure> rotationAcceleration = randomVector(random, 0.01);
            FieldAngularCoordinates<DerivativeStructure> ac         = new FieldAngularCoordinates<>(rotation, rotationRate, rotationAcceleration);
            FieldAngularCoordinates<DerivativeStructure> rebuilt    = FieldAngularCoordinates.createFromModifiedRodrigues(ac.getModifiedRodrigues(1.0));
            Assert.assertEquals(0.0, FieldRotation.distance(rotation, rebuilt.getRotation()).getReal(), 1.0e-14);
            Assert.assertEquals(0.0, FieldVector3D.distance(rotationRate, rebuilt.getRotationRate()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(rotationAcceleration, rebuilt.getRotationAcceleration()).getReal(), 1.0e-15);
        }

    }

    @Test
    public void testRodriguesSpecialCases() {

        // identity
        DerivativeStructure[][] identity = FieldAngularCoordinates.getIdentity(new DSFactory(2, 2).getDerivativeField()).getModifiedRodrigues(1.0);
        for (DerivativeStructure[] row : identity) {
            for (DerivativeStructure element : row) {
                Assert.assertEquals(0.0, element.getReal(), Precision.SAFE_MIN);
            }
        }
        FieldAngularCoordinates<DerivativeStructure> acId = FieldAngularCoordinates.createFromModifiedRodrigues(identity);
        Assert.assertEquals(0.0, acId.getRotation().getAngle().getReal(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm().getReal(), Precision.SAFE_MIN);

        // PI angle rotation (which is singular for non-modified Rodrigues vector)
        RandomGenerator random = new Well1024a(0x4923ec495bca9fb4l);
        for (int i = 0; i < 100; ++i) {
            FieldVector3D<DerivativeStructure> axis = randomVector(random, 1.0);
            final Field<DerivativeStructure> field = axis.getX().getField();
            FieldAngularCoordinates<DerivativeStructure> original =
                            new FieldAngularCoordinates<>(new FieldRotation<>(axis, field.getZero().add(FastMath.PI), RotationConvention.VECTOR_OPERATOR),
                                                          FieldVector3D.getZero(field),
                                                          FieldVector3D.getZero(field));
            FieldAngularCoordinates<DerivativeStructure> rebuilt = FieldAngularCoordinates.createFromModifiedRodrigues(original.getModifiedRodrigues(1.0));
            Assert.assertEquals(FastMath.PI, rebuilt.getRotation().getAngle().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.angle(axis, rebuilt.getRotation().getAxis(RotationConvention.VECTOR_OPERATOR)).sin().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, rebuilt.getRotationRate().getNorm().getReal(), 1.0e-16);
        }

    }

    @Test
    public void testInverseCrossProducts()
        {
        Decimal64Field field = Decimal64Field.getInstance();
        checkInverse(FieldVector3D.getPlusK(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field));
        checkInverse(FieldVector3D.getZero(field),  FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        checkInverse(FieldVector3D.getZero(field),  FieldVector3D.getZero(field),  FieldVector3D.getPlusJ(field));
        checkInverse(FieldVector3D.getPlusK(field), FieldVector3D.getPlusK(field), FieldVector3D.getPlusJ(field));
        checkInverse(FieldVector3D.getZero(field),  FieldVector3D.getPlusK(field), FieldVector3D.getZero(field));
        checkInverse(FieldVector3D.getPlusK(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusK(field));
        checkInverse(FieldVector3D.getPlusK(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field));
        checkInverse(FieldVector3D.getPlusK(field), FieldVector3D.getPlusI(field), new FieldVector3D<Decimal64>(field, new Vector3D(1, 0, -1)).normalize());
        checkInverse(FieldVector3D.getZero(field),  FieldVector3D.getPlusI(field), FieldVector3D.getZero(field), FieldVector3D.getPlusJ(field),  FieldVector3D.getZero(field));
    }

    @Test
    public void testInverseCrossProductsFailures() {
        Decimal64Field field = Decimal64Field.getInstance();
        checkInverseFailure(FieldVector3D.getPlusK(field), FieldVector3D.getZero(field),  FieldVector3D.getPlusJ(field), FieldVector3D.getPlusI(field),  FieldVector3D.getPlusK(field));
        checkInverseFailure(FieldVector3D.getPlusK(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field),  FieldVector3D.getZero(field),    FieldVector3D.getPlusK(field));
        checkInverseFailure(FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field), FieldVector3D.getZero(field),  FieldVector3D.getMinusI(field), FieldVector3D.getPlusK(field));
        checkInverseFailure(FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field), FieldVector3D.getZero(field),  FieldVector3D.getPlusJ(field),  FieldVector3D.getPlusJ(field));
        checkInverseFailure(FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field), FieldVector3D.getPlusJ(field),  FieldVector3D.getZero(field));
        checkInverseFailure(FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field), FieldVector3D.getZero(field),    FieldVector3D.getPlusJ(field));
    }

    @Test
    public void testRandomInverseCrossProducts() {
        RandomGenerator generator = new Well1024a(0xda0ee5b245efd438l);
        for (int i = 0; i < 10000; ++i) {
            FieldVector3D<DerivativeStructure> omega = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            FieldVector3D<DerivativeStructure> v1    = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            FieldVector3D<DerivativeStructure> v2    = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            checkInverse(omega, v1, v2);
        }
    }

    @Test
    public void testRandomPVCoordinates() {
        RandomGenerator generator = new Well1024a(0xf978035a328a565bl);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r           = randomRotation(generator);
            FieldVector3D<DerivativeStructure> omega       = randomVector(generator, 10    * generator.nextDouble() + 1.0);
            FieldVector3D<DerivativeStructure> omegaDot    = randomVector(generator, 0.1   * generator.nextDouble() + 0.01);
            FieldAngularCoordinates<DerivativeStructure> ref = new FieldAngularCoordinates<>(r, omega, omegaDot);
            FieldAngularCoordinates<DerivativeStructure> inv = ref.revert();
            for (int j = 0; j < 100; ++j) {
                FieldPVCoordinates<DerivativeStructure> v1 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
                FieldPVCoordinates<DerivativeStructure> v2 = randomPVCoordinates(generator, 1000, 1.0, 0.0010);
                FieldPVCoordinates<DerivativeStructure> u1 = inv.applyTo(v1);
                FieldPVCoordinates<DerivativeStructure> u2 = inv.applyTo(v2);
                FieldAngularCoordinates<DerivativeStructure> rebuilt = new FieldAngularCoordinates<>(u1, u2, v1, v2, 1.0e-9);
                Assert.assertEquals(0.0,
                                    FieldRotation.distance(r, rebuilt.getRotation()).getReal(),
                                    6.0e-14);
                Assert.assertEquals(0.0,
                                    FieldVector3D.distance(omega, rebuilt.getRotationRate()).getReal(),
                                    3.0e-12 * omega.getNorm().getReal());
                Assert.assertEquals(0.0,
                                    FieldVector3D.distance(omegaDot, rebuilt.getRotationAcceleration()).getReal(),
                                    2.0e-6 * omegaDot.getNorm().getReal());
            }
        }
    }

    @Test
    public void testCancellingDerivatives() {
        PVCoordinates u1 = new PVCoordinates(new Vector3D(-0.4466591282528639,   -0.009657376949231283,  -0.894652087807798),
                                             new Vector3D(-8.897296517803556E-4,  2.7825250920407674E-4,  4.411979658413134E-4),
                                             new Vector3D( 4.753127475302486E-7,  1.0209400376727623E-8,  9.515403756524403E-7));
        PVCoordinates u2 = new PVCoordinates(new Vector3D( 0.23723907259910096,   0.9628700806685033,    -0.1288364474275361),
                                             new Vector3D(-7.98741002062555E-24,  2.4979687659429984E-24, 3.9607863426704016E-24),
                                             new Vector3D(-3.150541868418562E-23, 9.856329862034835E-24,  1.5648124883326986E-23));
        PVCoordinates v1 = new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);
        PVCoordinates v2 = new PVCoordinates(Vector3D.MINUS_J, Vector3D.ZERO, Vector3D.ZERO);
        AngularCoordinates ac = new AngularCoordinates(u1, u2, v1, v2, 1.0e-9);
        PVCoordinates v1Computed = ac.applyTo(u1);
        PVCoordinates v2Computed = ac.applyTo(u2);
        Assert.assertEquals(0, Vector3D.distance(v1.getPosition(),     v1Computed.getPosition()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v2.getPosition(),     v2Computed.getPosition()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v1.getVelocity(),     v1Computed.getVelocity()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v2.getVelocity(),     v2Computed.getVelocity()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v1.getAcceleration(), v1Computed.getAcceleration()), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v2.getAcceleration(), v2Computed.getAcceleration()), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void checkInverse(FieldVector3D<T> omega, FieldVector3D<T> v1, FieldVector3D<T> v2)
        {
        checkInverse(omega,
                     v1, FieldVector3D.crossProduct(omega, v1),
                     v2, FieldVector3D.crossProduct(omega, v2));
    }

    private <T extends RealFieldElement<T>> void checkInverseFailure(FieldVector3D<T> omega,
                                                                     FieldVector3D<T> v1, FieldVector3D<T> c1,
                                                                     FieldVector3D<T> v2, FieldVector3D<T> c2) {
        try {
            checkInverse(omega, v1, c1, v2, c2);
            Assert.fail("an exception should have been thrown");
        } catch (MathIllegalArgumentException miae) {
            // expected
        }
    }

    private <T extends RealFieldElement<T>> void checkInverse(FieldVector3D<T> omega,
                                                              FieldVector3D<T> v1, FieldVector3D<T> c1,
                                                              FieldVector3D<T> v2, FieldVector3D<T> c2)
        throws MathIllegalArgumentException {
        try {
            Method inverse;
            inverse = FieldAngularCoordinates.class.getDeclaredMethod("inverseCrossProducts",
                                                                      FieldVector3D.class, FieldVector3D.class,
                                                                      FieldVector3D.class, FieldVector3D.class,
                                                                      Double.TYPE);
            inverse.setAccessible(true);
            @SuppressWarnings("unchecked")
            FieldVector3D<T> rebuilt = (FieldVector3D<T>) inverse.invoke(null, v1, c1, v2, c2, 1.0e-9);
            Assert.assertEquals(0.0, FieldVector3D.distance(omega, rebuilt).getReal(), 5.0e-12 * omega.getNorm().getReal());
        } catch (NoSuchMethodException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (SecurityException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (InvocationTargetException e) {
            throw (MathIllegalArgumentException) e.getCause();
        }
    }

    private FieldVector3D<DerivativeStructure> randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new FieldVector3D<>(n, createVector(x, y, z, 4).normalize());
    }

    private FieldPVCoordinates<DerivativeStructure> randomPVCoordinates(RandomGenerator random,
                                                                        double norm0, double norm1, double norm2) {
        FieldVector3D<DerivativeStructure> p0 = randomVector(random, norm0);
        FieldVector3D<DerivativeStructure> p1 = randomVector(random, norm1);
        FieldVector3D<DerivativeStructure> p2 = randomVector(random, norm2);
        return new FieldPVCoordinates<>(p0, p1, p2);
    }

    private FieldRotation<DerivativeStructure> randomRotation(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return createRotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private FieldRotation<DerivativeStructure> createRotation(double q0, double q1, double q2, double q3,
                                                              boolean needsNormalization) {
        DSFactory factory = new DSFactory(4, 1);
        return new FieldRotation<>(factory.variable(0, q0),
                                   factory.variable(1, q1),
                                   factory.variable(2, q2),
                                   factory.variable(3, q3),
                                   needsNormalization);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        DSFactory factory = new DSFactory(params, 1);
        return new FieldVector3D<>(factory.variable(0, x),
                                   factory.variable(1, y),
                                   factory.variable(2, z));
    }

    private FieldRotation<Decimal64> randomRotation64(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new FieldRotation<>(new Decimal64(q0 / q),
                                   new Decimal64(q1 / q),
                                   new Decimal64(q2 / q),
                                   new Decimal64(q3 / q),
                                   false);
    }

    private FieldVector3D<Decimal64> randomVector64(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new FieldVector3D<>(n, new FieldVector3D<>(new Decimal64(x), new Decimal64(y), new Decimal64(z)).normalize());
    }

}
