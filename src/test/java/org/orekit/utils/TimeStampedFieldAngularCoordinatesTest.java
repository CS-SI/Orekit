/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;

import java.util.Random;

public class TimeStampedFieldAngularCoordinatesTest {

    @Test
    public void testZeroRate() {
        TimeStampedFieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                         createRotation(0.48, 0.64, 0.36, 0.48, false),
                                                         createVector(0, 0, 0, 4),
                                                         createVector(0, 0, 0, 4));
        Assertions.assertEquals(createVector(0, 0, 0, 4), angularCoordinates.getRotationRate());
        double dt = 10.0;
        TimeStampedFieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assertions.assertEquals(0.0, shifted.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, shifted.getRotationRate().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-15);
    }

    @Test
    public void testShift() {
        double rate = 2 * FastMath.PI / (12 * 60);
        TimeStampedFieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                         createRotation(1, 0, 0, 0, false),
                                                         new FieldVector3D<>(rate, createVector(0, 0, 1, 4)),
                                                         createVector(0, 0, 0, 4));
        Assertions.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getReal(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        TimeStampedFieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assertions.assertEquals(rate, shifted.getRotationRate().getNorm().getReal(), 1.0e-10);
        Assertions.assertEquals(alpha, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> xSat = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        Assertions.assertEquals(0.0, xSat.subtract(createVector(FastMath.cos(alpha), FastMath.sin(alpha), 0, 4)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<DerivativeStructure> ySat = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        Assertions.assertEquals(0.0, ySat.subtract(createVector(-FastMath.sin(alpha), FastMath.cos(alpha), 0, 4)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<DerivativeStructure> zSat = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assertions.assertEquals(0.0, zSat.subtract(createVector(0, 0, 1, 4)).getNorm().getReal(), 1.0e-10);

    }

    @Test
    public void testToAC() {
        Random random = new Random(0xc9b4cf6c371108e0l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldVector3D<DerivativeStructure> a = randomVector(random, 1.0e-3);
            TimeStampedFieldAngularCoordinates<DerivativeStructure> acds =
        new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH, r, o, a);
            AngularCoordinates ac = acds.toAngularCoordinates();
            Assertions.assertEquals(0, Rotation.distance(r.toRotation(), ac.getRotation()), 1.0e-15);
            Assertions.assertEquals(0, FieldVector3D.distance(o, ac.getRotationRate()).getReal(), 1.0e-15);
            Assertions.assertEquals(0, FieldVector3D.distance(a, ac.getRotationAcceleration()).getReal(), 1.0e-15);
        }
    }

    @Test
    public void testSpin() {
        double rate = 2 * FastMath.PI / (12 * 60);
        TimeStampedFieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                         createRotation(0.48, 0.64, 0.36, 0.48, false),
                                                         new FieldVector3D<>(rate, createVector(0, 0, 1, 4)),
                                        createVector(0, 0, 0, 4));
        Assertions.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getReal(), 1.0e-10);
        double dt = 10.0;
        TimeStampedFieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assertions.assertEquals(rate, shifted.getRotationRate().getNorm().getReal(), 1.0e-10);
        Assertions.assertEquals(rate * dt, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> shiftedX  = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        FieldVector3D<DerivativeStructure> shiftedY  = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        FieldVector3D<DerivativeStructure> shiftedZ  = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        FieldVector3D<DerivativeStructure> originalX = angularCoordinates.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        FieldVector3D<DerivativeStructure> originalY = angularCoordinates.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        FieldVector3D<DerivativeStructure> originalZ = angularCoordinates.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assertions.assertEquals( FastMath.cos(rate * dt), FieldVector3D.dotProduct(shiftedX, originalX).getReal(), 1.0e-10);
        Assertions.assertEquals( FastMath.sin(rate * dt), FieldVector3D.dotProduct(shiftedX, originalY).getReal(), 1.0e-10);
        Assertions.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedX, originalZ).getReal(), 1.0e-10);
        Assertions.assertEquals(-FastMath.sin(rate * dt), FieldVector3D.dotProduct(shiftedY, originalX).getReal(), 1.0e-10);
        Assertions.assertEquals( FastMath.cos(rate * dt), FieldVector3D.dotProduct(shiftedY, originalY).getReal(), 1.0e-10);
        Assertions.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedY, originalZ).getReal(), 1.0e-10);
        Assertions.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalX).getReal(), 1.0e-10);
        Assertions.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalY).getReal(), 1.0e-10);
        Assertions.assertEquals( 1.0,                 FieldVector3D.dotProduct(shiftedZ, originalZ).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> forward = FieldAngularCoordinates.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assertions.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm().getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> reversed = FieldAngularCoordinates.estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assertions.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm().getReal(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldVector3D<DerivativeStructure> a = randomVector(random, 1.0e-3);
            TimeStampedFieldAngularCoordinates<DerivativeStructure> ac =
        new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH, r, o, a);
            TimeStampedFieldAngularCoordinates<DerivativeStructure> sum = ac.addOffset(ac.revert());
            Assertions.assertEquals(0.0, sum.getRotation().getAngle().getReal(), 1.0e-15);
            Assertions.assertEquals(0.0, sum.getRotationRate().getNorm().getReal(), 1.0e-15);
            Assertions.assertEquals(0.0, sum.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        TimeStampedFieldAngularCoordinates<DerivativeStructure> ac1 =
                new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                         createRotation(0.48,  0.64, 0.36, 0.48, false),
                                                         createVector(0, 0, 0, 4),
                                                         createVector(0, 0, 0, 4));
        TimeStampedFieldAngularCoordinates<DerivativeStructure> ac2 =
                new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                         createRotation(0.36, -0.48, 0.48, 0.64, false),
                                                         createVector(0, 0, 0, 4),
                                                         createVector(0, 0, 0, 4));

        TimeStampedFieldAngularCoordinates<DerivativeStructure> add12 = ac1.addOffset(ac2);
        TimeStampedFieldAngularCoordinates<DerivativeStructure> add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assertions.assertEquals(2.574, FieldRotation.distance(add12.getRotation(), add21.getRotation()).getReal(), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        Random random = new Random(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            FieldRotation<DerivativeStructure> r1 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o1 = randomVector(random, 1.0e-2);
            FieldVector3D<DerivativeStructure> a1 = randomVector(random, 1.0e-2);
            TimeStampedFieldAngularCoordinates<DerivativeStructure> ac1 =
        new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH, r1, o1, a1);
            FieldRotation<DerivativeStructure> r2 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o2 = randomVector(random, 1.0e-2);
            FieldVector3D<DerivativeStructure> a2 = randomVector(random, 1.0e-2);

            TimeStampedFieldAngularCoordinates<DerivativeStructure> ac2 =
        new TimeStampedFieldAngularCoordinates<>(AbsoluteDate.J2000_EPOCH, r2, o2, a2);
            TimeStampedFieldAngularCoordinates<DerivativeStructure> roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assertions.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripSA.getRotation()).getReal(), 4.0e-16);
            Assertions.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()).getReal(), 2.0e-17);
            Assertions.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()).getReal(), 1.0e-17);

            TimeStampedFieldAngularCoordinates<DerivativeStructure> roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assertions.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripAS.getRotation()).getReal(), 6.0e-16);
            Assertions.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()).getReal(), 2.0e-17);
            Assertions.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()).getReal(), 2.0e-17);

        }
    }

    @Test
    public void testDerivativesStructures0() {
        RandomGenerator random = new Well1024a(0x18a0a08fd63f047al);

        FieldRotation<Binary64> r    = randomRotation64(random);
        FieldVector3D<Binary64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Binary64> oDot = randomVector64(random, 1.0e-2);
        TimeStampedFieldAngularCoordinates<Binary64> ac =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 r, o, oDot);
        TimeStampedFieldAngularCoordinates<Binary64> rebuilt =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 ac.toDerivativeStructureRotation(0));
        Assertions.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, rebuilt.getRotationRate().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures1() {
        RandomGenerator random = new Well1024a(0x8f8fc6d27bbdc46dl);

        FieldRotation<Binary64> r    = randomRotation64(random);
        FieldVector3D<Binary64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Binary64> oDot = randomVector64(random, 1.0e-2);
        TimeStampedFieldAngularCoordinates<Binary64> ac =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 r, o, oDot);
        TimeStampedFieldAngularCoordinates<Binary64> rebuilt =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 ac.toDerivativeStructureRotation(1));
        Assertions.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures2() {
        RandomGenerator random = new Well1024a(0x1633878dddac047dl);

        FieldRotation<Binary64> r    = randomRotation64(random);
        FieldVector3D<Binary64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Binary64> oDot = randomVector64(random, 1.0e-2);
        TimeStampedFieldAngularCoordinates<Binary64> ac =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 r, o, oDot);
        TimeStampedFieldAngularCoordinates<Binary64> rebuilt =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 ac.toDerivativeStructureRotation(2));
        Assertions.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()).getReal(), 1.0e-15);

    }

    @Test
    public void testUnivariateDerivative1() {
        RandomGenerator random = new Well1024a(0x6de8cce747539904l);

        FieldRotation<Binary64> r    = randomRotation64(random);
        FieldVector3D<Binary64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Binary64> oDot = randomVector64(random, 1.0e-2);
        TimeStampedFieldAngularCoordinates<Binary64> ac =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 r, o, oDot);
        FieldRotation<FieldUnivariateDerivative1<Binary64>> rotationUD = ac.toUnivariateDerivative1Rotation();
        FieldRotation<FieldDerivativeStructure<Binary64>>   rotationDS = ac.toDerivativeStructureRotation(1);
        Assertions.assertEquals(rotationDS.getQ0().getReal(), rotationUD.getQ0().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getReal(), rotationUD.getQ1().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getReal(), rotationUD.getQ2().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getReal(), rotationUD.getQ3().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ0().getPartialDerivative(1).getReal(), rotationUD.getQ0().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getPartialDerivative(1).getReal(), rotationUD.getQ1().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getPartialDerivative(1).getReal(), rotationUD.getQ2().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getPartialDerivative(1).getReal(), rotationUD.getQ3().getFirstDerivative().getReal(), 1.0e-15);

        TimeStampedFieldAngularCoordinates<Binary64> rebuilt =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 rotationUD);
        Assertions.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()).getReal(), 1.0e-15);

    }

    @Test
    public void testUnivariateDerivative2() {
        RandomGenerator random = new Well1024a(0x255710c8fa2247ecl);

        FieldRotation<Binary64> r    = randomRotation64(random);
        FieldVector3D<Binary64> o    = randomVector64(random, 1.0e-2);
        FieldVector3D<Binary64> oDot = randomVector64(random, 1.0e-2);
        TimeStampedFieldAngularCoordinates<Binary64> ac =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 r, o, oDot);
        FieldRotation<FieldUnivariateDerivative2<Binary64>> rotationUD = ac.toUnivariateDerivative2Rotation();
        FieldRotation<FieldDerivativeStructure<Binary64>>   rotationDS = ac.toDerivativeStructureRotation(2);
        Assertions.assertEquals(rotationDS.getQ0().getReal(), rotationUD.getQ0().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getReal(), rotationUD.getQ1().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getReal(), rotationUD.getQ2().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getReal(), rotationUD.getQ3().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ0().getPartialDerivative(1).getReal(), rotationUD.getQ0().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getPartialDerivative(1).getReal(), rotationUD.getQ1().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getPartialDerivative(1).getReal(), rotationUD.getQ2().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getPartialDerivative(1).getReal(), rotationUD.getQ3().getFirstDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ0().getPartialDerivative(2).getReal(), rotationUD.getQ0().getSecondDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getPartialDerivative(2).getReal(), rotationUD.getQ1().getSecondDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getPartialDerivative(2).getReal(), rotationUD.getQ2().getSecondDerivative().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getPartialDerivative(2).getReal(), rotationUD.getQ3().getSecondDerivative().getReal(), 1.0e-15);

        TimeStampedFieldAngularCoordinates<Binary64> rebuilt =
                        new TimeStampedFieldAngularCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                                 rotationUD);
        Assertions.assertEquals(0.0, FieldRotation.distance(ac.getRotation(), rebuilt.getRotation()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()).getReal(), 1.0e-15);

    }

    @Test
    public void testIssue773() {
        doTestIssue773(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue773(final Field<T> field) {
        // Epoch
        final AbsoluteDate date = new AbsoluteDate();

        // Coordinates
        final TimeStampedAngularCoordinates angular =
                        new TimeStampedAngularCoordinates(date,
                                                          new Rotation(0., 0., 0., 0., false),
                                                          Vector3D.ZERO,
                                                          Vector3D.ZERO);

        // Time stamped object
        final FieldTimeStamped<T> timeStamped =
                        new TimeStampedFieldAngularCoordinates<>(field, angular);

        // Verify
        Assertions.assertEquals(0.0, date.durationFrom(timeStamped.getDate().toAbsoluteDate()), Double.MIN_VALUE);
    }

    private FieldVector3D<DerivativeStructure> randomVector(Random random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new FieldVector3D<>(n, createVector(x, y, z, 4).normalize());
    }

    private FieldRotation<DerivativeStructure> randomRotation(Random random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return createRotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    public static FieldRotation<DerivativeStructure> createRotation(FieldVector3D<DerivativeStructure> axis, double angle) {
        return new FieldRotation<>(axis,
                                   new DSFactory(4, 1).constant(angle),
                                   RotationConvention.VECTOR_OPERATOR);
    }

    public static FieldRotation<DerivativeStructure> createRotation(double q0, double q1, double q2, double q3,
                                                                    boolean needsNormalization) {
        DSFactory factory = new DSFactory(4, 1);
        return new FieldRotation<>(factory.variable(0, q0),
                                   factory.variable(1, q1),
                                   factory.variable(2, q2),
                                   factory.variable(3, q3),
                                   needsNormalization);
    }

    public static FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        DSFactory factory = new DSFactory(params, 1);
        return new FieldVector3D<>(factory.variable(0, x),
                                   factory.variable(1, y),
                                   factory.variable(2, z));
    }

    private FieldRotation<Binary64> randomRotation64(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new FieldRotation<>(new Binary64(q0 / q),
                                   new Binary64(q1 / q),
                                   new Binary64(q2 / q),
                                   new Binary64(q3 / q),
                                   false);
    }

    private FieldVector3D<Binary64> randomVector64(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new FieldVector3D<>(n, new FieldVector3D<>(new Binary64(x), new Binary64(y), new Binary64(z)).normalize());
    }

}

