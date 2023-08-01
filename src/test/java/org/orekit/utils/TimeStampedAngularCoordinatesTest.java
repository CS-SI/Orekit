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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;

public class TimeStampedAngularCoordinatesTest {

    @Test
    public void testZeroRate() {
        TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                  Vector3D.ZERO, Vector3D.ZERO);
        Assertions.assertEquals(Vector3D.ZERO, ac.getRotationRate());
        double dt = 10.0;
        TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assertions.assertEquals(Vector3D.ZERO, shifted.getRotationAcceleration());
        Assertions.assertEquals(Vector3D.ZERO, shifted.getRotationRate());
        Assertions.assertEquals(0.0, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-15);
    }

    @Test
    public void testOnePair() throws java.io.IOException {
        RandomGenerator random = new Well1024a(0xed7dd911a44c5197l);

        for (int i = 0; i < 20; ++i) {

            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-2);
            Vector3D a = randomVector(random, 1.0e-2);
            TimeStampedAngularCoordinates reference = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, a);

            PVCoordinates u = randomPVCoordinates(random, 1000, 1.0, 0.001);
            PVCoordinates v = reference.applyTo(u);
            TimeStampedAngularCoordinates ac =
                    new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, u, v);

            Assertions.assertEquals(0, Vector3D.distance(v.getPosition().normalize(), ac.applyTo(u).getPosition().normalize()), 1.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v.getVelocity().normalize(), ac.applyTo(u).getVelocity().normalize()), 4.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v.getAcceleration().normalize(), ac.applyTo(u).getAcceleration().normalize()), 1.0e-14);

        }

    }

    @Test
    public void testTwoPairs() throws java.io.IOException {
        RandomGenerator random = new Well1024a(0x976ad943966c9f00l);

        for (int i = 0; i < 20; ++i) {

            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-2);
            Vector3D a = randomVector(random, 1.0e-2);
            TimeStampedAngularCoordinates reference = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, a);

            PVCoordinates u1 = randomPVCoordinates(random, 1000, 1.0, 0.001);
            PVCoordinates u2 = randomPVCoordinates(random, 1000, 1.0, 0.001);
            PVCoordinates v1 = reference.applyTo(u1);
            PVCoordinates v2 = reference.applyTo(u2);
            TimeStampedAngularCoordinates ac =
                    new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, u1, u2, v1, v2, 1.0e-9);

            Assertions.assertEquals(0, Vector3D.distance(v1.getPosition().normalize(), ac.applyTo(u1).getPosition().normalize()), 1.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v1.getVelocity().normalize(), ac.applyTo(u1).getVelocity().normalize()), 1.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v1.getAcceleration().normalize(), ac.applyTo(u1).getAcceleration().normalize()), 1.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v2.getPosition().normalize(), ac.applyTo(u2).getPosition().normalize()), 1.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v2.getVelocity().normalize(), ac.applyTo(u2).getVelocity().normalize()), 1.0e-14);
            Assertions.assertEquals(0, Vector3D.distance(v2.getAcceleration().normalize(), ac.applyTo(u2).getAcceleration().normalize()), 1.0e-14);

        }

    }

    @Test
    public void testDerivativesStructures0() {
        RandomGenerator random = new Well1024a(0x18a0a08fd63f047al);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, oDot);
        TimeStampedAngularCoordinates rebuilt = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                  ac.toDerivativeStructureRotation(0));
        Assertions.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, rebuilt.getRotationRate().getNorm(), 1.0e-15);
        Assertions.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures1() {
        RandomGenerator random = new Well1024a(0x8f8fc6d27bbdc46dl);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, oDot);
        TimeStampedAngularCoordinates rebuilt = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                  ac.toDerivativeStructureRotation(1));
        Assertions.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);
        Assertions.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures2() {
        RandomGenerator random = new Well1024a(0x1633878dddac047dl);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, oDot);
        TimeStampedAngularCoordinates rebuilt = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                  ac.toDerivativeStructureRotation(2));
        Assertions.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()), 1.0e-15);
    }

    @Test
    public void testUnivariateDerivative1() {
        RandomGenerator random = new Well1024a(0x6de8cce747539904l);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, oDot);
        FieldRotation<UnivariateDerivative1> rotationUD = ac.toUnivariateDerivative1Rotation();
        FieldRotation<DerivativeStructure>   rotationDS = ac.toDerivativeStructureRotation(1);
        Assertions.assertEquals(rotationDS.getQ0().getReal(), rotationUD.getQ0().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getReal(), rotationUD.getQ1().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getReal(), rotationUD.getQ2().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getReal(), rotationUD.getQ3().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ0().getPartialDerivative(1), rotationUD.getQ0().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getPartialDerivative(1), rotationUD.getQ1().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getPartialDerivative(1), rotationUD.getQ2().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getPartialDerivative(1), rotationUD.getQ3().getFirstDerivative(), 1.0e-15);

        TimeStampedAngularCoordinates rebuilt = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, rotationUD);
        Assertions.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);

    }

    @Test
    public void testUnivariateDerivative2() {
        RandomGenerator random = new Well1024a(0x255710c8fa2247ecl);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, oDot);
        FieldRotation<UnivariateDerivative2> rotationUD = ac.toUnivariateDerivative2Rotation();
        FieldRotation<DerivativeStructure>   rotationDS = ac.toDerivativeStructureRotation(2);
        Assertions.assertEquals(rotationDS.getQ0().getReal(), rotationUD.getQ0().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getReal(), rotationUD.getQ1().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getReal(), rotationUD.getQ2().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getReal(), rotationUD.getQ3().getReal(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ0().getPartialDerivative(1), rotationUD.getQ0().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getPartialDerivative(1), rotationUD.getQ1().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getPartialDerivative(1), rotationUD.getQ2().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getPartialDerivative(1), rotationUD.getQ3().getFirstDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ0().getPartialDerivative(2), rotationUD.getQ0().getSecondDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ1().getPartialDerivative(2), rotationUD.getQ1().getSecondDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ2().getPartialDerivative(2), rotationUD.getQ2().getSecondDerivative(), 1.0e-15);
        Assertions.assertEquals(rotationDS.getQ3().getPartialDerivative(2), rotationUD.getQ3().getSecondDerivative(), 1.0e-15);

        TimeStampedAngularCoordinates rebuilt = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, rotationUD);
        Assertions.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()), 1.0e-15);

    }

    @Test
    public void testShift() {
        double rate = 2 * FastMath.PI / (12 * 60);
        TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  Rotation.IDENTITY,
                                                  new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assertions.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assertions.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assertions.assertEquals(alpha, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assertions.assertEquals(0.0, xSat.subtract(new Vector3D(FastMath.cos(alpha), FastMath.sin(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assertions.assertEquals(0.0, ySat.subtract(new Vector3D(-FastMath.sin(alpha), FastMath.cos(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assertions.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    @Test
    public void testSpin() {
        double rate = 2 * FastMath.PI / (12 * 60);
        TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                  new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assertions.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assertions.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assertions.assertEquals(rate * dt, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Vector3D originalX = ac.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D originalY = ac.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D originalZ = ac.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assertions.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assertions.assertEquals( FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assertions.assertEquals(-FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assertions.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assertions.assertEquals( 1.0,                 Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        Vector3D forward = TimeStampedAngularCoordinates.estimateRate(ac.getRotation(), shifted.getRotation(), dt);
        Assertions.assertEquals(0.0, forward.subtract(ac.getRotationRate()).getNorm(), 1.0e-10);

        Vector3D reversed = TimeStampedAngularCoordinates.estimateRate(shifted.getRotation(), ac.getRotation(), dt);
        Assertions.assertEquals(0.0, reversed.add(ac.getRotationRate()).getNorm(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        RandomGenerator random = new Well1024a(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-3);
            Vector3D a = randomVector(random, 1.0e-3);
            TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, a);
            TimeStampedAngularCoordinates sum = ac.addOffset(ac.revert());
            Assertions.assertEquals(0.0, sum.getRotation().getAngle(), 1.0e-15);
            Assertions.assertEquals(0.0, sum.getRotationRate().getNorm(), 1.0e-15);
            Assertions.assertEquals(0.0, sum.getRotationAcceleration().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        TimeStampedAngularCoordinates ac1 =
        new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, new Rotation(0.48,  0.64, 0.36, 0.48, false), Vector3D.ZERO, Vector3D.ZERO);
        TimeStampedAngularCoordinates ac2 =
        new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, new Rotation(0.36, -0.48, 0.48, 0.64, false), Vector3D.ZERO, Vector3D.ZERO);

        TimeStampedAngularCoordinates add12 = ac1.addOffset(ac2);
        TimeStampedAngularCoordinates add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assertions.assertEquals(2.574, Rotation.distance(add12.getRotation(), add21.getRotation()), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        RandomGenerator random = new Well1024a(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            Rotation r1 = randomRotation(random);
            Vector3D o1 = randomVector(random, 1.0e-2);
            Vector3D a1 = randomVector(random, 1.0e-2);
            TimeStampedAngularCoordinates ac1 = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r1, o1, a1);
            Rotation r2 = randomRotation(random);
            Vector3D o2 = randomVector(random, 1.0e-2);
            Vector3D a2 = randomVector(random, 1.0e-2);

            TimeStampedAngularCoordinates ac2 = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r2, o2, a2);
            TimeStampedAngularCoordinates roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assertions.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripSA.getRotation()), 4.0e-16);
            Assertions.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()), 2.0e-17);
            Assertions.assertEquals(0.0, Vector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()), 1.0e-17);

            TimeStampedAngularCoordinates roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assertions.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripAS.getRotation()), 6.0e-16);
            Assertions.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()), 2.0e-17);
            Assertions.assertEquals(0.0, Vector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()), 2.0e-17);

        }
    }

    private Vector3D randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = 2 * random.nextDouble() - 1;
        double y = 2 * random.nextDouble() - 1;
        double z = 2 * random.nextDouble() - 1;
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private PVCoordinates randomPVCoordinates(RandomGenerator random,
                                              double norm0, double norm1, double norm2) {
        Vector3D p0 = randomVector(random, norm0);
        Vector3D p1 = randomVector(random, norm1);
        Vector3D p2 = randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private Rotation randomRotation(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

}

