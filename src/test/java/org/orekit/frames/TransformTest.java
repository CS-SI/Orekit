/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.frames;


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class TransformTest {

    @Test
    public void testIdentityTranslation() {
        checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(0, 0, 0)),
                         new Well19937a(0xfd118eac6b5ec136l));
    }

    @Test
    public void testIdentityRotation() {
        checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(1, 0, 0, 0, false)),
                         new Well19937a(0xfd118eac6b5ec136l));
    }

    @Test
    public void testSimpleComposition() {
        Transform transform =
            new Transform(AbsoluteDate.J2000_EPOCH,
                          new Transform(AbsoluteDate.J2000_EPOCH,
                                        new Rotation(Vector3D.PLUS_K, 0.5 * FastMath.PI,
                                                     RotationConvention.VECTOR_OPERATOR)),
                          new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I));
        Vector3D u = transform.transformPosition(new Vector3D(1.0, 1.0, 1.0));
        Vector3D v = new Vector3D(0.0, 1.0, 1.0);
        Assert.assertEquals(0, u.subtract(v).getNorm(), 1.0e-15);
    }

    @Test
    public void testAcceleration() {

        PVCoordinates initPV = new PVCoordinates(new Vector3D(9, 8, 7), new Vector3D(6, 5, 4), new Vector3D(3, 2, 1));
        for (double dt = 0; dt < 1; dt += 0.01) {
            PVCoordinates basePV        = initPV.shiftedBy(dt);
            PVCoordinates transformedPV = evolvingTransform(AbsoluteDate.J2000_EPOCH, dt).transformPVCoordinates(basePV);

            // rebuild transformed acceleration, relying only on transformed position and velocity
            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            double h = 1.0e-2;
            for (int i = -3; i < 4; ++i) {
                Transform t = evolvingTransform(AbsoluteDate.J2000_EPOCH, dt + i * h);
                PVCoordinates pv = t.transformPVCoordinates(initPV.shiftedBy(dt + i * h));
                sample.add(new TimeStampedPVCoordinates(t.getDate(), pv.getPosition(), pv.getVelocity(), Vector3D.ZERO));
            }
            PVCoordinates rebuiltPV = TimeStampedPVCoordinates.interpolate(AbsoluteDate.J2000_EPOCH.shiftedBy(dt),
                                                                           CartesianDerivativesFilter.USE_PV,
                                                                           sample);

            checkVector(rebuiltPV.getPosition(),     transformedPV.getPosition(),     4.0e-16);
            checkVector(rebuiltPV.getVelocity(),     transformedPV.getVelocity(),     2.0e-16);
            checkVector(rebuiltPV.getAcceleration(), transformedPV.getAcceleration(), 9.0e-11);

        }

    }

    @Test
    public void testAccelerationComposition() {
        RandomGenerator random = new Well19937a(0x41fdd07d6c9e9f65l);

        Vector3D  p1 = randomVector(1.0e3,  random);
        Vector3D  v1 = randomVector(1.0,    random);
        Vector3D  a1 = randomVector(1.0e-3, random);
        Rotation  r1 = randomRotation(random);
        Vector3D  o1 = randomVector(0.1, random);

        Vector3D  p2 = randomVector(1.0e3,  random);
        Vector3D  v2 = randomVector(1.0,    random);
        Vector3D  a2 = randomVector(1.0e-3, random);
        Rotation  r2 = randomRotation(random);
        Vector3D  o2 = randomVector(0.1, random);

        Transform t1  = new Transform(AbsoluteDate.J2000_EPOCH,
                                      new Transform(AbsoluteDate.J2000_EPOCH, p1, v1, a1),
                                      new Transform(AbsoluteDate.J2000_EPOCH, r1, o1));
        Transform t2  = new Transform(AbsoluteDate.J2000_EPOCH,
                                      new Transform(AbsoluteDate.J2000_EPOCH, p2, v2, a2),
                                      new Transform(AbsoluteDate.J2000_EPOCH, r2, o2));
        Transform t12 = new Transform(AbsoluteDate.J2000_EPOCH, t1, t2);

        Vector3D q       = randomVector(1.0e3,  random);
        Vector3D qDot    = randomVector(1.0,    random);
        Vector3D qDotDot = randomVector(1.0e-3, random);

        PVCoordinates pva0 = new PVCoordinates(q, qDot, qDotDot);
        PVCoordinates pva1 = t1.transformPVCoordinates(pva0);
        PVCoordinates pva2 = t2.transformPVCoordinates(pva1);
        PVCoordinates pvac = t12.transformPVCoordinates(pva0);

        checkVector(pva2.getPosition(),     pvac.getPosition(),     1.0e-15);
        checkVector(pva2.getVelocity(),     pvac.getVelocity(),     1.0e-15);
        checkVector(pva2.getAcceleration(), pvac.getAcceleration(), 1.0e-15);

        // despite neither raw transforms have angular acceleration,
        // the combination does have an angular acceleration,
        // it is due to the cross product Ω₁ ⨯ Ω₂
        Assert.assertEquals(0.0, t1.getAngular().getRotationAcceleration().getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, t2.getAngular().getRotationAcceleration().getNorm(), 1.0e-15);
        Assert.assertTrue(t12.getAngular().getRotationAcceleration().getNorm() > 0.01);

    }

    @Test
    public void testRandomComposition() {

        RandomGenerator random = new Well19937a(0x171c79e323a1123l);
        for (int i = 0; i < 20; ++i) {

            // build a complex transform by composing primitive ones
            int n = random.nextInt(20);
            Transform[] transforms = new Transform[n];
            Transform combined = Transform.IDENTITY;
            for (int k = 0; k < n; ++k) {
                transforms[k] = random.nextBoolean()
                ? new Transform(AbsoluteDate.J2000_EPOCH, randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random))
                : new Transform(AbsoluteDate.J2000_EPOCH, randomRotation(random), randomVector(0.01, random), randomVector(1.0e-4, random));
                combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, transforms[k]);
            }

            // check the composition
            for (int j = 0; j < 10; ++j) {
                Vector3D a = randomVector(1.0, random);
                FieldVector3D<Decimal64> aF = new FieldVector3D<Decimal64>(Decimal64.ONE, a);
                Vector3D b = randomVector(1.0e3, random);
                PVCoordinates c = new PVCoordinates(randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
                Vector3D                 aRef  = a;
                FieldVector3D<Decimal64> aFRef = aF;
                Vector3D                 bRef  = b;
                PVCoordinates            cRef  = c;
                for (int k = 0; k < n; ++k) {
                    aRef  = transforms[k].transformVector(aRef);
                    aFRef = transforms[k].transformVector(aFRef);
                    bRef  = transforms[k].transformPosition(bRef);
                    cRef  = transforms[k].transformPVCoordinates(cRef);
                }

                Vector3D aCombined = combined.transformVector(a);
                FieldVector3D<Decimal64> aFCombined = combined.transformVector(aF);
                Vector3D bCombined = combined.transformPosition(b);
                PVCoordinates cCombined = combined.transformPVCoordinates(c);
                checkVector(aRef, aCombined, 3.0e-15);
                checkVector(aFRef.toVector3D(), aFCombined.toVector3D(), 3.0e-15);
                checkVector(bRef, bCombined, 5.0e-15);
                checkVector(cRef.getPosition(),     cCombined.getPosition(),     1.0e-14);
                checkVector(cRef.getVelocity(),     cCombined.getVelocity(),     1.0e-14);
                checkVector(cRef.getAcceleration(), cCombined.getAcceleration(), 1.0e-14);

            }
        }

    }

    @Test
    public void testReverse() {
        RandomGenerator random = new Well19937a(0x9f82ba2b2c98dac5l);
        for (int i = 0; i < 20; ++i) {
            Transform combined = randomTransform(random);

            checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, combined, combined.getInverse()), random);

        }

    }

    @Test
    public void testDecomposeAndRebuild() {
        RandomGenerator random = new Well19937a(0xb8ee9da1b05198c9l);
        for (int i = 0; i < 20; ++i) {
            Transform combined = randomTransform(random);
            Transform rebuilt  = new Transform(combined.getDate(),
                                               new Transform(combined.getDate(), combined.getTranslation(),
                                                             combined.getVelocity(), combined.getAcceleration()),
                                               new Transform(combined.getDate(), combined.getRotation(),
                                                             combined.getRotationRate(), combined.getRotationAcceleration()));

            checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, combined, rebuilt.getInverse()), random);

        }

    }

    @Test
    public void testTranslation() {
        RandomGenerator rnd = new Well19937a(0x7e9d737ba4147787l);
        for (int i = 0; i < 10; ++i) {
            Vector3D delta = randomVector(1.0e3, rnd);
            Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, delta);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                Vector3D b = transform.transformVector(a);
                Assert.assertEquals(0, b.subtract(a).getNorm(), 1.0e-15);
                Vector3D c = transform.transformPosition(a);
                Assert.assertEquals(0,
                             c.subtract(a).subtract(delta).getNorm(),
                             1.0e-14);
            }
        }
    }

    @Test
    public void testRoughTransPV() {

        PVCoordinates pointP1 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_I);

        // translation transform test
        PVCoordinates pointP2 = new PVCoordinates(new Vector3D(0, 0, 0), new Vector3D(0, 0, 0));
        Transform R1toR2 = new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.MINUS_I, Vector3D.MINUS_I, Vector3D.MINUS_I);
        PVCoordinates result1 = R1toR2.transformPVCoordinates(pointP1);
        checkVector(pointP2.getPosition(),     result1.getPosition(),     1.0e-15);
        checkVector(pointP2.getVelocity(),     result1.getVelocity(),     1.0e-15);
        checkVector(pointP2.getAcceleration(), result1.getAcceleration(), 1.0e-15);

        // test inverse translation
        Transform R2toR1 = R1toR2.getInverse();
        PVCoordinates invResult1 = R2toR1.transformPVCoordinates(pointP2);
        checkVector(pointP1.getPosition(),     invResult1.getPosition(),     1.0e-15);
        checkVector(pointP1.getVelocity(),     invResult1.getVelocity(),     1.0e-15);
        checkVector(pointP1.getAcceleration(), invResult1.getAcceleration(), 1.0e-15);

        // rotation transform test
        PVCoordinates pointP3 = new PVCoordinates(Vector3D.PLUS_J, new Vector3D(-2, 1, 0), new Vector3D(-4, -3, -1));
        Rotation R = new Rotation(Vector3D.PLUS_K, FastMath.PI/2, RotationConvention.VECTOR_OPERATOR);
        Transform R1toR3 = new Transform(AbsoluteDate.J2000_EPOCH, R, new Vector3D(0, 0, -2), new Vector3D(1, 0, 0));
        PVCoordinates result2 = R1toR3.transformPVCoordinates(pointP1);
        checkVector(pointP3.getPosition(),     result2.getPosition(),     1.0e-15);
        checkVector(pointP3.getVelocity(),     result2.getVelocity(),     1.0e-15);
        checkVector(pointP3.getAcceleration(), result2.getAcceleration(), 1.0e-15);

        // test inverse rotation
        Transform R3toR1 = R1toR3.getInverse();
        PVCoordinates invResult2 = R3toR1.transformPVCoordinates(pointP3);
        checkVector(pointP1.getPosition(),     invResult2.getPosition(),     1.0e-15);
        checkVector(pointP1.getVelocity(),     invResult2.getVelocity(),     1.0e-15);
        checkVector(pointP1.getAcceleration(), invResult2.getAcceleration(), 1.0e-15);

        // combine 2 velocity transform
        Transform R1toR4 = new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(-2, 0, 0), new Vector3D(-2, 0, 0), new Vector3D(-2, 0, 0));
        PVCoordinates pointP4 = new PVCoordinates(new Vector3D(-1, 0, 0), new Vector3D(-1, 0, 0), new Vector3D(-1, 0, 0));
        Transform R2toR4 = new Transform(AbsoluteDate.J2000_EPOCH, R2toR1, R1toR4);
        PVCoordinates compResult = R2toR4.transformPVCoordinates(pointP2);
        checkVector(pointP4.getPosition(),     compResult.getPosition(),     1.0e-15);
        checkVector(pointP4.getVelocity(),     compResult.getVelocity(),     1.0e-15);
        checkVector(pointP4.getAcceleration(), compResult.getAcceleration(), 1.0e-15);

        // combine 2 rotation tranform
        PVCoordinates pointP5 = new PVCoordinates(new Vector3D(-1, 0, 0), new Vector3D(-1, 0, 3), new Vector3D(8, 0, 6));
        Rotation R2 = new Rotation( new Vector3D(0,0,1), FastMath.PI, RotationConvention.VECTOR_OPERATOR);
        Transform R1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R2, new Vector3D(0, -3, 0));
        Transform R3toR5 = new Transform (AbsoluteDate.J2000_EPOCH, R3toR1, R1toR5);
        PVCoordinates combResult = R3toR5.transformPVCoordinates(pointP3);
        checkVector(pointP5.getPosition(),     combResult.getPosition(),     1.0e-15);
        checkVector(pointP5.getVelocity(),     combResult.getVelocity(),     1.0e-15);
        checkVector(pointP5.getAcceleration(), combResult.getAcceleration(), 1.0e-15);

        // combine translation and rotation
        Transform R2toR3 = new Transform (AbsoluteDate.J2000_EPOCH, R2toR1,R1toR3);
        PVCoordinates result = R2toR3.transformPVCoordinates(pointP2);
        checkVector(pointP3.getPosition(),     result.getPosition(),     1.0e-15);
        checkVector(pointP3.getVelocity(),     result.getVelocity(),     1.0e-15);
        checkVector(pointP3.getAcceleration(), result.getAcceleration(), 1.0e-15);

        Transform R3toR2 = new Transform (AbsoluteDate.J2000_EPOCH, R3toR1, R1toR2);
        result = R3toR2.transformPVCoordinates(pointP3);
        checkVector(pointP2.getPosition(),     result.getPosition(),     1.0e-15);
        checkVector(pointP2.getVelocity(),     result.getVelocity(),     1.0e-15);
        checkVector(pointP2.getAcceleration(), result.getAcceleration(), 1.0e-15);

        Transform newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR2, R2toR3);
        newR1toR5 = new   Transform(AbsoluteDate.J2000_EPOCH, newR1toR5,R3toR5);
        result = newR1toR5.transformPVCoordinates(pointP1);
        checkVector(pointP5.getPosition(),     result.getPosition(),     1.0e-15);
        checkVector(pointP5.getVelocity(),     result.getVelocity(),     1.0e-15);
        checkVector(pointP5.getAcceleration(), result.getAcceleration(), 1.0e-15);

        // more tests
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR2, R2toR3);
        Transform R3toR4 = new Transform(AbsoluteDate.J2000_EPOCH, R3toR1, R1toR4);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R3toR4);
        Transform R4toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR4.getInverse(), R1toR5);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R4toR5);
        result = newR1toR5.transformPVCoordinates(pointP1);
        checkVector(pointP5.getPosition(),     result.getPosition(), 1.0e-15);
        checkVector(pointP5.getVelocity(),     result.getVelocity(), 1.0e-15);
        checkVector(pointP5.getAcceleration(), result.getAcceleration(), 1.0e-15);

    }

    @Test
    public void testRotPV() {

        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);

        // Instant Rotation only

        for (int i = 0; i < 10; ++i) {

            // Random instant rotation

            Rotation instantRot    = randomRotation(rnd);
            Vector3D normAxis = instantRot.getAxis(RotationConvention.VECTOR_OPERATOR);
            double w  = FastMath.abs(instantRot.getAngle())/Constants.JULIAN_DAY;

            // random rotation
            Rotation rot    = randomRotation(rnd);

            // so we have a transform
            Transform tr = new Transform(AbsoluteDate.J2000_EPOCH, rot, new Vector3D(w, normAxis));

            // random position, velocity, acceleration
            Vector3D pos = randomVector(1.0e3, rnd);
            Vector3D vel = randomVector(1.0, rnd);
            Vector3D acc = randomVector(1.0e-3, rnd);

            PVCoordinates pvOne = new PVCoordinates(pos, vel, acc);

            // we obtain

            PVCoordinates pvTwo = tr.transformPVCoordinates(pvOne);

            // test inverse

            Vector3D resultvel = tr.getInverse().transformPVCoordinates(pvTwo).getVelocity();

            checkVector(resultvel, vel, 1.0e-15);

        }

    }

    @Test
    public void testTransPV() {

        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);

        // translation velocity only :

        for (int i = 0; i < 10; ++i) {

            // random position, velocity and acceleration
            Vector3D pos = randomVector(1.0e3,  rnd);
            Vector3D vel = randomVector(1.0,    rnd);
            Vector3D acc = randomVector(1.0e-3, rnd);
            PVCoordinates pvOne = new PVCoordinates(pos, vel, acc);

            // random transform
            Vector3D transPos = randomVector(1.0e3,  rnd);
            Vector3D transVel = randomVector(1.0,    rnd);
            Vector3D transAcc = randomVector(1.0e-3, rnd);
            Transform tr = new Transform(AbsoluteDate.J2000_EPOCH, transPos, transVel, transAcc);

            double dt = 1;

            // we should obtain
            Vector3D good = tr.transformPosition(pos.add(new Vector3D(dt, vel))).add(new Vector3D(dt, transVel));

            // we have
            PVCoordinates pvTwo = tr.transformPVCoordinates(pvOne);
            Vector3D result  = pvTwo.getPosition().add(new Vector3D(dt, pvTwo.getVelocity()));
            checkVector(good, result, 1.0e-15);

            FieldPVCoordinates<Decimal64> fieldPVOne =
                            new FieldPVCoordinates<Decimal64>(new FieldVector3D<Decimal64>(Decimal64.ONE, pvOne.getPosition()),
                                                              new FieldVector3D<Decimal64>(Decimal64.ONE, pvOne.getVelocity()),
                                                              new FieldVector3D<Decimal64>(Decimal64.ONE, pvOne.getAcceleration()));
            FieldPVCoordinates<Decimal64> fieldPVTwo = tr.transformPVCoordinates(fieldPVOne);
            FieldVector3D<Decimal64> fieldResult  =
                            fieldPVTwo.getPosition().add(new FieldVector3D<Decimal64>(dt, fieldPVTwo.getVelocity()));
            checkVector(good, fieldResult.toVector3D(), 1.0e-15);

            TimeStampedFieldPVCoordinates<Decimal64> fieldTPVOne =
                            new TimeStampedFieldPVCoordinates<Decimal64>(tr.getDate(),
                                            new FieldVector3D<Decimal64>(Decimal64.ONE, pvOne.getPosition()),
                                            new FieldVector3D<Decimal64>(Decimal64.ONE, pvOne.getVelocity()),
                                            new FieldVector3D<Decimal64>(Decimal64.ONE, pvOne.getAcceleration()));
            TimeStampedFieldPVCoordinates<Decimal64> fieldTPVTwo = tr.transformPVCoordinates(fieldTPVOne);
            FieldVector3D<Decimal64> fieldTResult  =
                            fieldTPVTwo.getPosition().add(new FieldVector3D<Decimal64>(dt, fieldTPVTwo.getVelocity()));
            checkVector(good, fieldTResult.toVector3D(), 1.0e-15);

            // test inverse
            Vector3D resultvel = tr.getInverse().
            transformPVCoordinates(pvTwo).getVelocity();
            checkVector(resultvel, vel, 1.0e-15);

        }

    }

    @Test
    public void testRotation() {
        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);
        for (int i = 0; i < 10; ++i) {

            Rotation r    = randomRotation(rnd);
            Vector3D axis = r.getAxis(RotationConvention.VECTOR_OPERATOR);
            double angle  = r.getAngle();

            Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, r);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                Vector3D b = transform.transformVector(a);
                Assert.assertEquals(Vector3D.angle(axis, a), Vector3D.angle(axis, b), 1.0e-14);
                Vector3D aOrtho = Vector3D.crossProduct(axis, a);
                Vector3D bOrtho = Vector3D.crossProduct(axis, b);
                Assert.assertEquals(angle, Vector3D.angle(aOrtho, bOrtho), 1.0e-14);
                Vector3D c = transform.transformPosition(a);
                Assert.assertEquals(0, c.subtract(b).getNorm(), 1.0e-14);
            }

        }
    }

    @Test
    public void testJacobianP() {

        // base directions for finite differences
        PVCoordinates[] directions = new PVCoordinates[] {
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO),
        };
        double h = 0.01;

        RandomGenerator random = new Well19937a(0x47fd0d6809f4b173l);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            Transform combined = randomTransform(random);

            // compute Jacobian
            double[][] jacobian = new double[9][9];
            for (int l = 0; l < jacobian.length; ++l) {
                for (int c = 0; c < jacobian[l].length; ++c) {
                    jacobian[l][c] = l + 0.1 * c;
                }
            }
            combined.getJacobian(CartesianDerivativesFilter.USE_P, jacobian);

            for (int j = 0; j < 100; ++j) {

                PVCoordinates pv0 = new PVCoordinates(randomVector(1e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
                double epsilonP = 2.0e-12 * pv0.getPosition().getNorm();

                for (int c = 0; c < directions.length; ++c) {

                    // eight points finite differences estimation of a Jacobian column
                    PVCoordinates pvm4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -4 * h, directions[c]));
                    PVCoordinates pvm3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -3 * h, directions[c]));
                    PVCoordinates pvm2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -2 * h, directions[c]));
                    PVCoordinates pvm1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -1 * h, directions[c]));
                    PVCoordinates pvp1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +1 * h, directions[c]));
                    PVCoordinates pvp2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +2 * h, directions[c]));
                    PVCoordinates pvp3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +3 * h, directions[c]));
                    PVCoordinates pvp4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +4 * h, directions[c]));
                    PVCoordinates d4   = new PVCoordinates(pvm4h, pvp4h);
                    PVCoordinates d3   = new PVCoordinates(pvm3h, pvp3h);
                    PVCoordinates d2   = new PVCoordinates(pvm2h, pvp2h);
                    PVCoordinates d1   = new PVCoordinates(pvm1h, pvp1h);
                    double d = 1.0 / (840 * h);
                    PVCoordinates estimatedColumn = new PVCoordinates(-3 * d, d4, 32 * d, d3, -168 * d, d2, 672 * d, d1);

                    // check analytical Jacobian against finite difference reference
                    Assert.assertEquals(estimatedColumn.getPosition().getX(), jacobian[0][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getY(), jacobian[1][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getZ(), jacobian[2][c], epsilonP);

                    // check the rest of the matrix remains untouched
                    for (int l = 3; l < jacobian.length; ++l) {
                        Assert.assertEquals(l + 0.1 * c, jacobian[l][c], 1.0e-15);
                    }

                }

                // check the rest of the matrix remains untouched
                for (int c = directions.length; c < jacobian[0].length; ++c) {
                    for (int l = 0; l < jacobian.length; ++l) {
                        Assert.assertEquals(l + 0.1 * c, jacobian[l][c], 1.0e-15);
                    }
                }

            }
        }

    }

    @Test
    public void testJacobianPV() {

        // base directions for finite differences
        PVCoordinates[] directions = new PVCoordinates[] {
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO,   Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO,   Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO,   Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_I, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_J, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_K, Vector3D.ZERO)
        };
        double h = 0.01;

        RandomGenerator random = new Well19937a(0xce2bfddfbb9796bel);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            Transform combined = randomTransform(random);

            // compute Jacobian
            double[][] jacobian = new double[9][9];
            for (int l = 0; l < jacobian.length; ++l) {
                for (int c = 0; c < jacobian[l].length; ++c) {
                    jacobian[l][c] = l + 0.1 * c;
                }
            }
            combined.getJacobian(CartesianDerivativesFilter.USE_PV, jacobian);

            for (int j = 0; j < 100; ++j) {

                PVCoordinates pv0 = new PVCoordinates(randomVector(1e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
                double epsilonP = 2.0e-12 * pv0.getPosition().getNorm();
                double epsilonV = 6.0e-11  * pv0.getVelocity().getNorm();

                for (int c = 0; c < directions.length; ++c) {

                    // eight points finite differences estimation of a Jacobian column
                    PVCoordinates pvm4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -4 * h, directions[c]));
                    PVCoordinates pvm3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -3 * h, directions[c]));
                    PVCoordinates pvm2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -2 * h, directions[c]));
                    PVCoordinates pvm1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -1 * h, directions[c]));
                    PVCoordinates pvp1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +1 * h, directions[c]));
                    PVCoordinates pvp2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +2 * h, directions[c]));
                    PVCoordinates pvp3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +3 * h, directions[c]));
                    PVCoordinates pvp4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +4 * h, directions[c]));
                    PVCoordinates d4   = new PVCoordinates(pvm4h, pvp4h);
                    PVCoordinates d3   = new PVCoordinates(pvm3h, pvp3h);
                    PVCoordinates d2   = new PVCoordinates(pvm2h, pvp2h);
                    PVCoordinates d1   = new PVCoordinates(pvm1h, pvp1h);
                    double d = 1.0 / (840 * h);
                    PVCoordinates estimatedColumn = new PVCoordinates(-3 * d, d4, 32 * d, d3, -168 * d, d2, 672 * d, d1);

                    // check analytical Jacobian against finite difference reference
                    Assert.assertEquals(estimatedColumn.getPosition().getX(), jacobian[0][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getY(), jacobian[1][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getZ(), jacobian[2][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getVelocity().getX(), jacobian[3][c], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getY(), jacobian[4][c], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getZ(), jacobian[5][c], epsilonV);

                    // check the rest of the matrix remains untouched
                    for (int l = 6; l < jacobian.length; ++l) {
                        Assert.assertEquals(l + 0.1 * c, jacobian[l][c], 1.0e-15);
                    }

                }

                // check the rest of the matrix remains untouched
                for (int c = directions.length; c < jacobian[0].length; ++c) {
                    for (int l = 0; l < jacobian.length; ++l) {
                        Assert.assertEquals(l + 0.1 * c, jacobian[l][c], 1.0e-15);
                    }
                }

            }
        }

    }

    @Test
    public void testJacobianPVA() {

        // base directions for finite differences
        PVCoordinates[] directions = new PVCoordinates[] {
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO,   Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO,   Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO,   Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_I, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_J, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_K, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.ZERO,   Vector3D.PLUS_I),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.ZERO,   Vector3D.PLUS_J),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.ZERO,   Vector3D.PLUS_K)
        };
        double h = 0.01;

        RandomGenerator random = new Well19937a(0xd223e88b6232198fl);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            Transform combined = randomTransform(random);

            // compute Jacobian
            double[][] jacobian = new double[9][9];
            for (int l = 0; l < jacobian.length; ++l) {
                for (int c = 0; c < jacobian[l].length; ++c) {
                    jacobian[l][c] = l + 0.1 * c;
                }
            }
            combined.getJacobian(CartesianDerivativesFilter.USE_PVA, jacobian);

            for (int j = 0; j < 100; ++j) {

                PVCoordinates pv0 = new PVCoordinates(randomVector(1e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
                double epsilonP = 2.0e-12 * pv0.getPosition().getNorm();
                double epsilonV = 6.0e-11 * pv0.getVelocity().getNorm();
                double epsilonA = 2.0e-9  * pv0.getAcceleration().getNorm();

                for (int c = 0; c < directions.length; ++c) {

                    // eight points finite differences estimation of a Jacobian column
                    PVCoordinates pvm4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -4 * h, directions[c]));
                    PVCoordinates pvm3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -3 * h, directions[c]));
                    PVCoordinates pvm2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -2 * h, directions[c]));
                    PVCoordinates pvm1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -1 * h, directions[c]));
                    PVCoordinates pvp1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +1 * h, directions[c]));
                    PVCoordinates pvp2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +2 * h, directions[c]));
                    PVCoordinates pvp3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +3 * h, directions[c]));
                    PVCoordinates pvp4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +4 * h, directions[c]));
                    PVCoordinates d4   = new PVCoordinates(pvm4h, pvp4h);
                    PVCoordinates d3   = new PVCoordinates(pvm3h, pvp3h);
                    PVCoordinates d2   = new PVCoordinates(pvm2h, pvp2h);
                    PVCoordinates d1   = new PVCoordinates(pvm1h, pvp1h);
                    double d = 1.0 / (840 * h);
                    PVCoordinates estimatedColumn = new PVCoordinates(-3 * d, d4, 32 * d, d3, -168 * d, d2, 672 * d, d1);

                    // check analytical Jacobian against finite difference reference
                    Assert.assertEquals(estimatedColumn.getPosition().getX(),     jacobian[0][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getY(),     jacobian[1][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getZ(),     jacobian[2][c], epsilonP);
                    Assert.assertEquals(estimatedColumn.getVelocity().getX(),     jacobian[3][c], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getY(),     jacobian[4][c], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getZ(),     jacobian[5][c], epsilonV);
                    Assert.assertEquals(estimatedColumn.getAcceleration().getX(), jacobian[6][c], epsilonA);
                    Assert.assertEquals(estimatedColumn.getAcceleration().getY(), jacobian[7][c], epsilonA);
                    Assert.assertEquals(estimatedColumn.getAcceleration().getZ(), jacobian[8][c], epsilonA);

                }

            }
        }

    }

    @Test
    public void testLine() {
        RandomGenerator random = new Well19937a(0x4a5ff67426c5731fl);
        for (int i = 0; i < 100; ++i) {
            Transform transform = randomTransform(random);
            for (int j = 0; j < 20; ++j) {
                Vector3D p0 = randomVector(1.0e3, random);
                Vector3D p1 = randomVector(1.0e3, random);
                Line l = new Line(p0, p1, 1.0e-10);
                Line transformed = transform.transformLine(l);
                for (int k = 0; k < 10; ++k) {
                    Vector3D p = l.pointAt(random.nextDouble() * 1.0e6);
                    Assert.assertEquals(0.0, transformed.distance(transform.transformPosition(p)), 1.0e-9);
                }
            }
        }
    }

    @Test
    public void testLinear() {

        RandomGenerator random = new Well19937a(0x14f6411217b148d8l);
        for (int n = 0; n < 100; ++n) {
            Transform t = randomTransform(random);

            // build an equivalent linear transform by extracting raw translation/rotation
            RealMatrix linearA = MatrixUtils.createRealMatrix(3, 4);
            linearA.setSubMatrix(t.getRotation().getMatrix(), 0, 0);
            Vector3D rt = t.getRotation().applyTo(t.getTranslation());
            linearA.setEntry(0, 3, rt.getX());
            linearA.setEntry(1, 3, rt.getY());
            linearA.setEntry(2, 3, rt.getZ());

            // build an equivalent linear transform by observing transformed points
            RealMatrix linearB = MatrixUtils.createRealMatrix(3, 4);
            Vector3D p0 = t.transformPosition(Vector3D.ZERO);
            Vector3D pI = t.transformPosition(Vector3D.PLUS_I).subtract(p0);
            Vector3D pJ = t.transformPosition(Vector3D.PLUS_J).subtract(p0);
            Vector3D pK = t.transformPosition(Vector3D.PLUS_K).subtract(p0);
            linearB.setColumn(0, new double[] { pI.getX(), pI.getY(), pI.getZ() });
            linearB.setColumn(1, new double[] { pJ.getX(), pJ.getY(), pJ.getZ() });
            linearB.setColumn(2, new double[] { pK.getX(), pK.getY(), pK.getZ() });
            linearB.setColumn(3, new double[] { p0.getX(), p0.getY(), p0.getZ() });

            // both linear transforms should be equal
            Assert.assertEquals(0.0, linearB.subtract(linearA).getNorm(),
                                1.0e-15 * linearA.getNorm());

            for (int i = 0; i < 100; ++i) {
                Vector3D p  = randomVector(1.0e3, random);
                Vector3D q  = t.transformPosition(p);

                double[] qA = linearA.operate(new double[] { p.getX(), p.getY(), p.getZ(), 1.0 });
                Assert.assertEquals(q.getX(), qA[0], 1.0e-13 * p.getNorm());
                Assert.assertEquals(q.getY(), qA[1], 1.0e-13 * p.getNorm());
                Assert.assertEquals(q.getZ(), qA[2], 1.0e-13 * p.getNorm());

                double[] qB = linearB.operate(new double[] { p.getX(), p.getY(), p.getZ(), 1.0 });
                Assert.assertEquals(q.getX(), qB[0], 1.0e-10 * p.getNorm());
                Assert.assertEquals(q.getY(), qB[1], 1.0e-10 * p.getNorm());
                Assert.assertEquals(q.getZ(), qB[2], 1.0e-10 * p.getNorm());

            }

        }

    }

    @Test
    public void testShift() {

        // the following transform corresponds to a frame moving along the line x=1 and rotating around its -z axis
        // the linear motion velocity is (0, +1, 0), the angular rate is PI/2
        // at t = -1 the frame origin is at (1, -1, 0), its X axis is equal to  Xref and its Y axis is equal to  Yref
        // at t =  0 the frame origin is at (1,  0, 0), its X axis is equal to -Yref and its Y axis is equal to  Xref
        // at t = +1 the frame origin is at (1, +1, 0), its X axis is equal to -Xref and its Y axis is equal to -Yref
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        Transform t   = new Transform(date,
                                      new Transform(date, Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.ZERO),
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_K, alpha0,
                                                                 RotationConvention.VECTOR_OPERATOR),
                                                    new Vector3D(omega, Vector3D.MINUS_K)));

        for (double dt = -10.0; dt < 10.0; dt += 0.125) {

            Transform shifted = t.shiftedBy(dt);

            // the following point should always remain at moving frame origin
            PVCoordinates expectedFixedPoint =
                    shifted.transformPVCoordinates(new PVCoordinates(new Vector3D(1, dt, 0), Vector3D.PLUS_J, Vector3D.ZERO));
            checkVector(expectedFixedPoint.getPosition(),     Vector3D.ZERO, 1.0e-14);
            checkVector(expectedFixedPoint.getVelocity(),     Vector3D.ZERO, 1.0e-14);
            checkVector(expectedFixedPoint.getAcceleration(), Vector3D.ZERO, 1.0e-14);

            // fixed frame origin apparent motion in moving frame
            PVCoordinates expectedApparentMotion = shifted.transformPVCoordinates(PVCoordinates.ZERO);
            double c = FastMath.cos(alpha0 + omega * dt);
            double s = FastMath.sin(alpha0 + omega * dt);
            Vector3D referencePosition = new Vector3D(-c + dt * s,
                                                      -s - dt * c,
                                                      0);
            Vector3D referenceVelocity = new Vector3D( (1 + omega) * s + dt * omega * c,
                                                      -(1 + omega) * c + dt * omega * s,
                                                      0);
            Vector3D referenceAcceleration = new Vector3D(omega * (2 + omega) * c - dt * omega * omega * s,
                                                          omega * (2 + omega) * s + dt * omega * omega * c,
                                                          0);
            checkVector(expectedApparentMotion.getPosition(),     referencePosition,     1.0e-14);
            checkVector(expectedApparentMotion.getVelocity(),     referenceVelocity,     1.0e-14);
            checkVector(expectedApparentMotion.getAcceleration(), referenceAcceleration, 1.0e-14);

        }

    }

    @Test
    public void testShiftDerivatives() {

        RandomGenerator random = new Well19937a(0x5acda4f605aadce7l);
        for (int i = 0; i < 10; ++i) {
            Transform t = randomTransform(random);

            for (double dt = -10.0; dt < 10.0; dt += 0.125) {

                Transform t0    = t.shiftedBy(dt);
                double v        = t0.getVelocity().getNorm();
                double a        = t0.getAcceleration().getNorm();
                double omega    = t0.getRotationRate().getNorm();
                double omegaDot = t0.getRotationAcceleration().getNorm();

                // numerical derivatives
                double h = 0.01 / omega;
                Transform tm4h = t.shiftedBy(dt - 4 * h);
                Transform tm3h = t.shiftedBy(dt - 3 * h);
                Transform tm2h = t.shiftedBy(dt - 2 * h);
                Transform tm1h = t.shiftedBy(dt - 1 * h);
                Transform tp1h = t.shiftedBy(dt + 1 * h);
                Transform tp2h = t.shiftedBy(dt + 2 * h);
                Transform tp3h = t.shiftedBy(dt + 3 * h);
                Transform tp4h = t.shiftedBy(dt + 4 * h);
                double numXDot = derivative(h,
                                            tm4h.getTranslation().getX(), tm3h.getTranslation().getX(),
                                            tm2h.getTranslation().getX(), tm1h.getTranslation().getX(),
                                            tp1h.getTranslation().getX(), tp2h.getTranslation().getX(),
                                            tp3h.getTranslation().getX(), tp4h.getTranslation().getX());
                double numYDot = derivative(h,
                                            tm4h.getTranslation().getY(), tm3h.getTranslation().getY(),
                                            tm2h.getTranslation().getY(), tm1h.getTranslation().getY(),
                                            tp1h.getTranslation().getY(), tp2h.getTranslation().getY(),
                                            tp3h.getTranslation().getY(), tp4h.getTranslation().getY());
                double numZDot = derivative(h,
                                            tm4h.getTranslation().getZ(), tm3h.getTranslation().getZ(),
                                            tm2h.getTranslation().getZ(), tm1h.getTranslation().getZ(),
                                            tp1h.getTranslation().getZ(), tp2h.getTranslation().getZ(),
                                            tp3h.getTranslation().getZ(), tp4h.getTranslation().getZ());
                double numXDot2 = derivative(h,
                                            tm4h.getVelocity().getX(), tm3h.getVelocity().getX(),
                                            tm2h.getVelocity().getX(), tm1h.getVelocity().getX(),
                                            tp1h.getVelocity().getX(), tp2h.getVelocity().getX(),
                                            tp3h.getVelocity().getX(), tp4h.getVelocity().getX());
                double numYDot2 = derivative(h,
                                            tm4h.getVelocity().getY(), tm3h.getVelocity().getY(),
                                            tm2h.getVelocity().getY(), tm1h.getVelocity().getY(),
                                            tp1h.getVelocity().getY(), tp2h.getVelocity().getY(),
                                            tp3h.getVelocity().getY(), tp4h.getVelocity().getY());
                double numZDot2 = derivative(h,
                                            tm4h.getVelocity().getZ(), tm3h.getVelocity().getZ(),
                                            tm2h.getVelocity().getZ(), tm1h.getVelocity().getZ(),
                                            tp1h.getVelocity().getZ(), tp2h.getVelocity().getZ(),
                                            tp3h.getVelocity().getZ(), tp4h.getVelocity().getZ());
                double numQ0Dot = derivative(h,
                                             tm4h.getRotation().getQ0(), tm3h.getRotation().getQ0(),
                                             tm2h.getRotation().getQ0(), tm1h.getRotation().getQ0(),
                                             tp1h.getRotation().getQ0(), tp2h.getRotation().getQ0(),
                                             tp3h.getRotation().getQ0(), tp4h.getRotation().getQ0());
                double numQ1Dot = derivative(h,
                                             tm4h.getRotation().getQ1(), tm3h.getRotation().getQ1(),
                                             tm2h.getRotation().getQ1(), tm1h.getRotation().getQ1(),
                                             tp1h.getRotation().getQ1(), tp2h.getRotation().getQ1(),
                                             tp3h.getRotation().getQ1(), tp4h.getRotation().getQ1());
                double numQ2Dot = derivative(h,
                                             tm4h.getRotation().getQ2(), tm3h.getRotation().getQ2(),
                                             tm2h.getRotation().getQ2(), tm1h.getRotation().getQ2(),
                                             tp1h.getRotation().getQ2(), tp2h.getRotation().getQ2(),
                                             tp3h.getRotation().getQ2(), tp4h.getRotation().getQ2());
                double numQ3Dot = derivative(h,
                                             tm4h.getRotation().getQ3(), tm3h.getRotation().getQ3(),
                                             tm2h.getRotation().getQ3(), tm1h.getRotation().getQ3(),
                                             tp1h.getRotation().getQ3(), tp2h.getRotation().getQ3(),
                                             tp3h.getRotation().getQ3(), tp4h.getRotation().getQ3());
                double numOxDot = derivative(h,
                                            tm4h.getRotationRate().getX(), tm3h.getRotationRate().getX(),
                                            tm2h.getRotationRate().getX(), tm1h.getRotationRate().getX(),
                                            tp1h.getRotationRate().getX(), tp2h.getRotationRate().getX(),
                                            tp3h.getRotationRate().getX(), tp4h.getRotationRate().getX());
                double numOyDot = derivative(h,
                                            tm4h.getRotationRate().getY(), tm3h.getRotationRate().getY(),
                                            tm2h.getRotationRate().getY(), tm1h.getRotationRate().getY(),
                                            tp1h.getRotationRate().getY(), tp2h.getRotationRate().getY(),
                                            tp3h.getRotationRate().getY(), tp4h.getRotationRate().getY());
                double numOzDot = derivative(h,
                                            tm4h.getRotationRate().getZ(), tm3h.getRotationRate().getZ(),
                                            tm2h.getRotationRate().getZ(), tm1h.getRotationRate().getZ(),
                                            tp1h.getRotationRate().getZ(), tp2h.getRotationRate().getZ(),
                                            tp3h.getRotationRate().getZ(), tp4h.getRotationRate().getZ());

                // theoretical derivatives
                double theXDot  = t0.getVelocity().getX();
                double theYDot  = t0.getVelocity().getY();
                double theZDot  = t0.getVelocity().getZ();
                double theXDot2 = t0.getAcceleration().getX();
                double theYDot2 = t0.getAcceleration().getY();
                double theZDot2 = t0.getAcceleration().getZ();
                Rotation  r0 = t0.getRotation();
                Vector3D  w  = t0.getRotationRate();
                Vector3D  q  = new Vector3D(r0.getQ1(), r0.getQ2(), r0.getQ3());
                Vector3D  qw = Vector3D.crossProduct(q, w);
                double theQ0Dot = -0.5 * Vector3D.dotProduct(q, w);
                double theQ1Dot =  0.5 * (r0.getQ0() * w.getX() + qw.getX());
                double theQ2Dot =  0.5 * (r0.getQ0() * w.getY() + qw.getY());
                double theQ3Dot =  0.5 * (r0.getQ0() * w.getZ() + qw.getZ());
                double theOxDot2 = t0.getRotationAcceleration().getX();
                double theOyDot2 = t0.getRotationAcceleration().getY();
                double theOzDot2 = t0.getRotationAcceleration().getZ();

                // check consistency
                Assert.assertEquals(theXDot, numXDot, 1.0e-13 * v);
                Assert.assertEquals(theYDot, numYDot, 1.0e-13 * v);
                Assert.assertEquals(theZDot, numZDot, 1.0e-13 * v);

                Assert.assertEquals(theXDot2, numXDot2, 1.0e-13 * a);
                Assert.assertEquals(theYDot2, numYDot2, 1.0e-13 * a);
                Assert.assertEquals(theZDot2, numZDot2, 1.0e-13 * a);

                Assert.assertEquals(theQ0Dot, numQ0Dot, 1.0e-13 * omega);
                Assert.assertEquals(theQ1Dot, numQ1Dot, 1.0e-13 * omega);
                Assert.assertEquals(theQ2Dot, numQ2Dot, 1.0e-13 * omega);
                Assert.assertEquals(theQ3Dot, numQ3Dot, 1.0e-13 * omega);


                Assert.assertEquals(theOxDot2, numOxDot, 1.0e-12 * omegaDot);
                Assert.assertEquals(theOyDot2, numOyDot, 1.0e-12 * omegaDot);
                Assert.assertEquals(theOzDot2, numOzDot, 1.0e-12 * omegaDot);

            }
        }
    }

    @Test
    public void testInterpolation() throws OrekitException {

        AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        List<Transform> sample = new ArrayList<Transform>();
        for (int i = 0; i < 5; ++i) {
            sample.add(evolvingTransform(t0, i * 0.8));
        }

        for (double dt = 0.1; dt <= 3.1; dt += 0.01) {
            Transform reference = evolvingTransform(t0, dt);
            Transform interpolated = sample.get(0).interpolate(reference.getDate(), sample);
            Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse());
            Assert.assertEquals(0.0, error.getCartesian().getPosition().getNorm(),           2.0e-15);
            Assert.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(),           6.0e-15);
            Assert.assertEquals(0.0, error.getCartesian().getAcceleration().getNorm(),       4.0e-14);
            Assert.assertEquals(0.0, error.getAngular().getRotation().getAngle(),            2.0e-15);
            Assert.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(),         6.0e-15);
            Assert.assertEquals(0.0, error.getAngular().getRotationAcceleration().getNorm(), 4.0e-14);

        }

    }

    private Transform evolvingTransform(final AbsoluteDate t0, final double dt) {
        // the following transform corresponds to a frame moving along the circle r = 1
        // with its x axis always pointing to the reference frame center
        final double omega = 0.2;
        final AbsoluteDate date = t0.shiftedBy(dt);
        final double cos = FastMath.cos(omega * dt);
        final double sin = FastMath.sin(omega * dt);
        return new Transform(date,
                             new Transform(date,
                                           new Vector3D(-cos, -sin, 0),
                                           new Vector3D(omega * sin, -omega * cos, 0),
                                           new Vector3D(omega * omega * cos, omega * omega * sin, 0)),
                             new Transform(date,
                                           new Rotation(Vector3D.PLUS_K, FastMath.PI - omega * dt,
                                                        RotationConvention.VECTOR_OPERATOR),
                                           new Vector3D(omega, Vector3D.PLUS_K)));
    }

    private double derivative(double h,
                              double ym4h, double ym3h, double ym2h, double ym1h,
                              double yp1h, double yp2h, double yp3h, double yp4h) {
        return (-3 * (yp4h - ym4h) + 32 * (yp3h - ym3h) - 168 * (yp2h - ym2h) + 672 * (yp1h - ym1h)) /
               (840 * h);
    }

    private Transform randomTransform(RandomGenerator random) {
        // generate a random transform
        Transform combined = Transform.IDENTITY;
        for (int k = 0; k < 20; ++k) {
            Transform t = random.nextBoolean() ?
                          new Transform(AbsoluteDate.J2000_EPOCH, randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random)) :
                          new Transform(AbsoluteDate.J2000_EPOCH, randomRotation(random), randomVector(0.01, random), randomVector(1.0e-4, random));
            combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, t);
        }
        return combined;
    }

    private Vector3D randomVector(double scale, RandomGenerator random) {
        return new Vector3D(random.nextDouble() * scale,
                            random.nextDouble() * scale,
                            random.nextDouble() * scale);
    }

    private Rotation randomRotation(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private void checkNoTransform(Transform transform, RandomGenerator random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = randomVector(1.0e3, random);
            Vector3D tA = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            Vector3D b = randomVector(1.0e3, random);
            Vector3D tB = transform.transformPosition(b);
            Assert.assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * a.getNorm());
            PVCoordinates pv  = new PVCoordinates(randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
            PVCoordinates tPv = transform.transformPVCoordinates(pv);
            checkVector(pv.getPosition(),     tPv.getPosition(), 1.0e-10);
            checkVector(pv.getVelocity(),     tPv.getVelocity(), 3.0e-9);
            checkVector(pv.getAcceleration(), tPv.getAcceleration(), 3.0e-9);
        }
    }

    private void checkVector(Vector3D reference, Vector3D result, double relativeTolerance) {
        double refNorm = reference.getNorm();
        double resNorm = result.getNorm();
        double tolerance = relativeTolerance * (1 + FastMath.max(refNorm, resNorm));
        Assert.assertEquals("ref = " + reference + ", res = " + result + " -> " +
                            (Vector3D.distance(reference, result) / (1 + FastMath.max(refNorm, resNorm))),
                            0, Vector3D.distance(reference, result), tolerance);
    }

}
