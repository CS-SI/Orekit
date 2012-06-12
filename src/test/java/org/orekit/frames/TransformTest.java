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
package org.orekit.frames;


import java.util.Random;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class TransformTest {

    @Test
    public void testIdentityTranslation() {
        checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(0, 0, 0)),
                         new Random(0xfd118eac6b5ec136l));
    }

    @Test
    public void testIdentityRotation() {
        checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(1, 0, 0, 0, false)),
                         new Random(0xfd118eac6b5ec136l));
    }

    @Test
    public void testSimpleComposition() {
        Transform transform =
            new Transform(AbsoluteDate.J2000_EPOCH,
                          new Transform(AbsoluteDate.J2000_EPOCH,
                                        new Rotation(Vector3D.PLUS_K, 0.5 * FastMath.PI)),
                          new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I));
        Vector3D u = transform.transformPosition(new Vector3D(1.0, 1.0, 1.0));
        Vector3D v = new Vector3D(0.0, 1.0, 1.0);
        Assert.assertEquals(0, u.subtract(v).getNorm(), 1.0e-15);
    }

    @Test
    public void testRandomComposition() {

        Random random = new Random(0x171c79e323a1123l);
        for (int i = 0; i < 20; ++i) {

            // build a complex transform by compositing primitive ones
            int n = random.nextInt(20);
            Transform[] transforms = new Transform[n];
            Transform combined = Transform.IDENTITY;
            for (int k = 0; k < n; ++k) {
                transforms[k] = random.nextBoolean()
                ? new Transform(AbsoluteDate.J2000_EPOCH, randomVector(random))
                : new Transform(AbsoluteDate.J2000_EPOCH, randomRotation(random));
                combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, transforms[k]);
            }

            // check the composition
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(random.nextDouble(),
                                          random.nextDouble(),
                                          random.nextDouble());
                Vector3D bRef = a;
                Vector3D cRef = a;
                for (int k = 0; k < n; ++k) {
                    bRef = transforms[k].transformVector(bRef);
                    cRef = transforms[k].transformPosition(cRef);
                }

                Vector3D bCombined = combined.transformVector(a);
                Vector3D cCombined = combined.transformPosition(a);
                Assert.assertEquals(0, bCombined.subtract(bRef).getNorm(), 1.0e-11);
                Assert.assertEquals(0, cCombined.subtract(cRef).getNorm(), 1.0e-10);

            }
        }

    }

    @Test
    public void testReverse() {
        Random random = new Random(0x9f82ba2b2c98dac5l);
        for (int i = 0; i < 20; ++i) {
            Transform combined = randomTransform(random);

            checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, combined, combined.getInverse()), random);

        }

    }

    @Test
    public void testTranslation() {
        Random rnd = new Random(0x7e9d737ba4147787l);
        for (int i = 0; i < 10; ++i) {
            Vector3D delta = randomVector(rnd);
            Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, delta);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                Vector3D b = transform.transformVector(a);
                Assert.assertEquals(0, b.subtract(a).getNorm(), 1.0e-10);
                Vector3D c = transform.transformPosition(a);
                Assert.assertEquals(0,
                             c.subtract(a).subtract(delta).getNorm(),
                             1.0e-13);
            }
        }
    }

    @Test
    public void testRoughTransPV() {

        PVCoordinates pointP1 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_I);

        // translation transform test
        PVCoordinates pointP2 = new PVCoordinates(new Vector3D(0, 0, 0), new Vector3D(0, 0, 0));
        Transform R1toR2 = new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.MINUS_I, Vector3D.MINUS_I);
        PVCoordinates result1 = R1toR2.transformPVCoordinates(pointP1);
        checkVectors(pointP2.getPosition(), result1.getPosition());
        checkVectors(pointP2.getVelocity(), result1.getVelocity());

        // test inverse translation
        Transform R2toR1 = R1toR2.getInverse();
        PVCoordinates invResult1 = R2toR1.transformPVCoordinates(pointP2);
        checkVectors(pointP1.getPosition(), invResult1.getPosition());
        checkVectors(pointP1.getVelocity(), invResult1.getVelocity());

        // rotation transform test
        PVCoordinates pointP3 = new PVCoordinates(Vector3D.PLUS_J, new Vector3D(-2, 1, 0));
        Rotation R = new Rotation(Vector3D.PLUS_K, FastMath.PI/2);
        Transform R1toR3 = new Transform(AbsoluteDate.J2000_EPOCH, R, new Vector3D(0, 0, -2));
        PVCoordinates result2 = R1toR3.transformPVCoordinates(pointP1);
        checkVectors(pointP3.getPosition(), result2.getPosition());
        checkVectors(pointP3.getVelocity(), result2.getVelocity());

        // test inverse rotation
        Transform R3toR1 = R1toR3.getInverse();
        PVCoordinates invResult2 = R3toR1.transformPVCoordinates(pointP3);
        checkVectors(pointP1.getPosition(), invResult2.getPosition());
        checkVectors(pointP1.getVelocity(), invResult2.getVelocity());

        // combine 2 velocity transform
        Transform R1toR4 = new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(-2,0,0),new Vector3D(-2,0,0));
        PVCoordinates pointP4 = new PVCoordinates(new Vector3D(-1,0,0),new Vector3D(-1,0,0));
        Transform R2toR4 = new Transform(AbsoluteDate.J2000_EPOCH, R2toR1, R1toR4);
        PVCoordinates compResult = R2toR4.transformPVCoordinates(pointP2);
        checkVectors(pointP4.getPosition() , compResult.getPosition());
        checkVectors(pointP4.getVelocity() , compResult.getVelocity());

        // combine 2 rotation tranform
        PVCoordinates pointP5 = new PVCoordinates(new Vector3D(-1,0,0),new Vector3D(-1 , 0 , 3));
        Rotation R2 = new Rotation( new Vector3D(0,0,1), FastMath.PI );
        Transform R1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R2 , new Vector3D(0, -3, 0));
        Transform R3toR5 = new Transform (AbsoluteDate.J2000_EPOCH, R3toR1, R1toR5);
        PVCoordinates combResult = R3toR5.transformPVCoordinates(pointP3);
        checkVectors(pointP5.getPosition() , combResult.getPosition());
        checkVectors(pointP5.getVelocity() , combResult.getVelocity());

        // combine translation and rotation
        Transform R2toR3 = new Transform (AbsoluteDate.J2000_EPOCH, R2toR1,R1toR3);
        PVCoordinates Result = R2toR3.transformPVCoordinates(pointP2);
        checkVectors(pointP3.getPosition() , Result.getPosition());
        checkVectors(pointP3.getVelocity() , Result.getVelocity());

        Transform R3toR2 = new Transform (AbsoluteDate.J2000_EPOCH, R3toR1, R1toR2);
        Result = R3toR2.transformPVCoordinates(pointP3);
        checkVectors(pointP2.getPosition() , Result.getPosition());
        checkVectors(pointP2.getVelocity() , Result.getVelocity());

        Transform newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR2, R2toR3);
        newR1toR5 = new   Transform(AbsoluteDate.J2000_EPOCH, newR1toR5,R3toR5);
        Result = newR1toR5.transformPVCoordinates(pointP1);
        checkVectors(pointP5.getPosition() , Result.getPosition());
        checkVectors(pointP5.getVelocity() , Result.getVelocity());

        // more tests

        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR2, R2toR3);
        Transform R3toR4 = new Transform(AbsoluteDate.J2000_EPOCH, R3toR1, R1toR4);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R3toR4);
        Transform R4toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR4.getInverse(), R1toR5);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R4toR5);
        Result = newR1toR5.transformPVCoordinates(pointP1);
        checkVectors(pointP5.getPosition() , Result.getPosition());
        checkVectors(pointP5.getVelocity() , Result.getVelocity());

    }

    @Test
    public void testRotPV() {

        Random rnd = new Random(0x73d5554d99427af0l);

        // Instant Rotation only

        for (int i = 0; i < 10; ++i) {

            // Random instant rotation

            Rotation instantRot    = randomRotation(rnd);
            Vector3D normAxis = instantRot.getAxis();
            double w  = FastMath.abs(instantRot.getAngle())/Constants.JULIAN_DAY;

            // random rotation
            Rotation rot    = randomRotation(rnd);

            // so we have a transform
            Transform tr = new Transform(AbsoluteDate.J2000_EPOCH, rot , new Vector3D(w, normAxis));

            // random position and velocity
            Vector3D pos = randomVector(rnd);
            Vector3D vel = randomVector(rnd);

            PVCoordinates pvOne = new PVCoordinates(pos,vel);

            // we obtain

            PVCoordinates pvTwo = tr.transformPVCoordinates(pvOne);

            // test inverse

            Vector3D resultvel = tr.getInverse().transformPVCoordinates(pvTwo).getVelocity();

            checkVectors(resultvel , vel);

        }

    }

    @Test
    public void testTransPV() {

        Random rnd = new Random(0x73d5554d99427af0l);

        // translation velocity only :

        for (int i = 0; i < 10; ++i) {

            // random position and velocity
            Vector3D pos = randomVector(rnd);
            Vector3D vel = randomVector(rnd);
            PVCoordinates pvOne = new PVCoordinates(pos , vel);

            // random transform
            Vector3D trans = randomVector(rnd);
            Vector3D transVel = randomVector(rnd);
            Transform tr = new Transform(AbsoluteDate.J2000_EPOCH, trans , transVel);

            double dt = 1;

            // we should obtain :

            Vector3D good =(tr.transformPosition(
                                                 pos.add(new Vector3D( dt , vel))).add(new Vector3D(dt, transVel)));

            // we have :

            PVCoordinates pvTwo = tr.transformPVCoordinates(pvOne);
            Vector3D result  = (pvTwo.getPosition().add(
                                                        new Vector3D(dt ,pvTwo.getVelocity())));
            checkVectors( good , result);

            // test inverse

            Vector3D resultvel = tr.getInverse().
            transformPVCoordinates(pvTwo).getVelocity();
            checkVectors(resultvel , vel);

        }

    }

    @Test
    public void testRotation() {
        Random rnd = new Random(0x73d5554d99427af0l);
        for (int i = 0; i < 10; ++i) {

            Rotation r    = randomRotation(rnd);
            Vector3D axis = r.getAxis();
            double angle  = r.getAngle();

            Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, r);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                Vector3D b = transform.transformVector(a);
                Assert.assertEquals(Vector3D.angle(axis, a), Vector3D.angle(axis, b), 1.0e-13);
                Vector3D aOrtho = Vector3D.crossProduct(axis, a);
                Vector3D bOrtho = Vector3D.crossProduct(axis, b);
                Assert.assertEquals(angle, Vector3D.angle(aOrtho, bOrtho), 1.0e-13);
                Vector3D c = transform.transformPosition(a);
                Assert.assertEquals(0, c.subtract(b).getNorm(), 1.0e-13);
            }

        }
    }

    @Test
    public void testJacobian() {

        // base directions for finite differences
        PVCoordinates[] directions = new PVCoordinates[] {
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_I),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_J),
            new PVCoordinates(Vector3D.ZERO,   Vector3D.PLUS_K)
        };
        double h = 0.01;

        Random random = new Random(0xce2bfddfbb9796bel);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            Transform combined = randomTransform(random);

            // compute Jacobian
            double[][] jacobian = new double[6][6];
            combined.getJacobian(jacobian);

            for (int j = 0; j < 100; ++j) {

                PVCoordinates pv0 = new PVCoordinates(randomVector(random), randomVector(random));
                double epsilonP = 1.0e-11 * pv0.getPosition().getNorm();
                double epsilonV = 1.0e-7  * pv0.getVelocity().getNorm();

                for (int l = 0; l < directions.length; ++l) {

                    // eight points finite differences estimation of a Jacobian column
                    PVCoordinates pvm4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -4 * h, directions[l]));
                    PVCoordinates pvm3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -3 * h, directions[l]));
                    PVCoordinates pvm2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -2 * h, directions[l]));
                    PVCoordinates pvm1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -1 * h, directions[l]));
                    PVCoordinates pvp1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +1 * h, directions[l]));
                    PVCoordinates pvp2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +2 * h, directions[l]));
                    PVCoordinates pvp3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +3 * h, directions[l]));
                    PVCoordinates pvp4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +4 * h, directions[l]));
                    PVCoordinates d4   = new PVCoordinates(pvm4h, pvp4h);
                    PVCoordinates d3   = new PVCoordinates(pvm3h, pvp3h);
                    PVCoordinates d2   = new PVCoordinates(pvm2h, pvp2h);
                    PVCoordinates d1   = new PVCoordinates(pvm1h, pvp1h);
                    double c = 1.0 / (840 * h);
                    PVCoordinates estimatedColumn = new PVCoordinates(-3 * c, d4, 32 * c, d3, -168 * c, d2, 672 * c, d1);

                    // check finite analytical Jacobian against finite difference reference
                    Assert.assertEquals(estimatedColumn.getPosition().getX(), jacobian[0][l], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getY(), jacobian[1][l], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getZ(), jacobian[2][l], epsilonP);
                    Assert.assertEquals(estimatedColumn.getVelocity().getX(), jacobian[3][l], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getY(), jacobian[4][l], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getZ(), jacobian[5][l], epsilonV);

                }

            }
        }

    }

    @Test
    public void testLine() {
        Random random = new Random(0x4a5ff67426c5731fl);
        for (int i = 0; i < 100; ++i) {
            Transform transform = randomTransform(random);
            for (int j = 0; j < 20; ++j) {
                Vector3D p0 = randomVector(random);
                Vector3D p1 = randomVector(random);
                Line l = new Line(p0, p1);
                Line transformed = transform.transformLine(l);
                for (int k = 0; k < 10; ++k) {
                    Vector3D p = l.pointAt(random.nextDouble() * 1.0e6);
                    Assert.assertEquals(0.0, transformed.distance(transform.transformPosition(p)), 1.0e-6);
                }
            }
        }
    }

    @Test
    public void testLinear() {

        Random random = new Random(0x14f6411217b148d8l);
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
                                1.0e-9 * linearA.getNorm());

            for (int i = 0; i < 100; ++i) {
                Vector3D p  = randomVector(random);
                Vector3D q  = t.transformPosition(p);

                double[] qA = linearA.operate(new double[] { p.getX(), p.getY(), p.getZ(), 1.0 });
                Assert.assertEquals(q.getX(), qA[0], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getY(), qA[1], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getZ(), qA[2], 1.0e-9 * p.getNorm());

                double[] qB = linearB.operate(new double[] { p.getX(), p.getY(), p.getZ(), 1.0 });
                Assert.assertEquals(q.getX(), qB[0], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getY(), qB[1], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getZ(), qB[2], 1.0e-9 * p.getNorm());

            }

        }

    }

    @Test
    public void testShift() {

        // the following transform corresponds to a frame moving along the line x=1 and rotating around its -z axis
        // the linear motion velocity is (0, +1, 0), the angular rate is PI/2
        // at t = -1 the frame origin is at (1, -1, 0), its X axis is equal to  Xref and its Y axis is equal to  Yref
        // at t =  0 the frame origin is at (1,  0, 0), its X axis is equal to  Yref and its Y axis is equal to  Xref
        // at t = +1 the frame origin is at (1, +1, 0), its X axis is equal to -Xref and its Y axis is equal to -Yref
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        Transform t   = new Transform(date,
                                      new Transform(date, Vector3D.MINUS_I, Vector3D.MINUS_J),
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_K, alpha0),
                                                    new Vector3D(omega, Vector3D.MINUS_K)));

        for (double dt = -10.0; dt < 10.0; dt += 0.125) {

            Transform shifted = t.shiftedBy(dt);

            // the following point should always remain at moving frame origin
            PVCoordinates expectedFixedPoint =
                    shifted.transformPVCoordinates(new PVCoordinates(new Vector3D(1, dt, 0), Vector3D.PLUS_J));
            checkVectors(expectedFixedPoint.getPosition(), Vector3D.ZERO);
            checkVectors(expectedFixedPoint.getVelocity(), Vector3D.ZERO);

            // fixed frame origin apparent motion in moving frame
            PVCoordinates expectedApparentMotion = shifted.transformPVCoordinates(PVCoordinates.ZERO);
            double c = FastMath.cos(alpha0 + omega * dt);
            double s = FastMath.sin(alpha0 + omega * dt);
            Vector3D referencePosition = new Vector3D(-c + dt * s, -s - dt * c, 0);
            Vector3D referenceVelocity =
                    new Vector3D( (1 + omega) * s + dt * omega * c, -(1 + omega) * c + dt * omega * s, 0);
            checkVectors(expectedApparentMotion.getPosition(), referencePosition);
            checkVectors(expectedApparentMotion.getVelocity(), referenceVelocity);

        }

    }

    private Transform randomTransform(Random random) {
        // generate a random transform
        Transform combined = Transform.IDENTITY;
        for (int k = 0; k < 20; ++k) {
            Transform t = random.nextBoolean() ?
                          new Transform(AbsoluteDate.J2000_EPOCH, randomVector(random), randomVector(random)) :
                          new Transform(AbsoluteDate.J2000_EPOCH, randomRotation(random), randomVector(random));
            combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, t);
        }
        return combined;
    }

    private void checkVectors(Vector3D v1 , Vector3D v2) {

        Vector3D d = v1.subtract(v2);

        Assert.assertEquals(0,d.getX(),1.0e-8);
        Assert.assertEquals(0,d.getY(),1.0e-8);
        Assert.assertEquals(0,d.getZ(),1.0e-8);

        Assert.assertEquals(0,d.getNorm(),1.0e-8);

        if ((v1.getNorm() > 1.0e-10) && (v2.getNorm() > 1.0e-10)) {
            Rotation r = new Rotation(v1, v2);
            Assert.assertEquals(0,r.getAngle(),1.0e-8);


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

    private void checkNoTransform(Transform transform, Random random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = randomVector(random);
            Vector3D tA = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            Vector3D b = randomVector(random);
            Vector3D tB = transform.transformPosition(b);
            Assert.assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * a.getNorm());
            PVCoordinates pv  = new PVCoordinates(randomVector(random), randomVector(random));
            PVCoordinates tPv = transform.transformPVCoordinates(pv);
            Assert.assertEquals(0, pv.getPosition().subtract(tPv.getPosition()).getNorm(),
                         1.0e-10 * pv.getPosition().getNorm());
            Assert.assertEquals(0, pv.getVelocity().subtract(tPv.getVelocity()).getNorm(),
                         1.0e-9 * pv.getVelocity().getNorm());
        }
    }

}
