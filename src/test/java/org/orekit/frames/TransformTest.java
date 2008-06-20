/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.frames.Transform;
import org.orekit.utils.PVCoordinates;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TransformTest extends TestCase {

    public void testIdentityTranslation() {
        checkNoTransform(new Transform(new Vector3D(0, 0, 0)),
                         new Random(0xfd118eac6b5ec136l));
    }

    public void testIdentityRotation() {
        checkNoTransform(new Transform(new Rotation(1, 0, 0, 0, false)),
                         new Random(0xfd118eac6b5ec136l));
    }

    public void testSimpleComposition() {
        Transform transform =
            new Transform(new Transform(new Rotation(Vector3D.PLUS_K, 0.5 * Math.PI)),
                          new Transform(Vector3D.PLUS_I));
        Vector3D u = transform.transformPosition(new Vector3D(1.0, 1.0, 1.0));
        Vector3D v = new Vector3D(0.0, 1.0, 1.0);
        assertEquals(0, u.subtract(v).getNorm(), 1.0e-15);
    }

    public void testRandomComposition() {

        Random random = new Random(0x171c79e323a1123l);
        for (int i = 0; i < 20; ++i) {

            // build a complex transform by compositing primitive ones
            int n = random.nextInt(20);
            Transform[] transforms = new Transform[n];
            Transform combined = Transform.IDENTITY;
            for (int k = 0; k < n; ++k) {
                transforms[k] = random.nextBoolean()
                ? new Transform(randomVector(random))
                : new Transform(randomRotation(random));
                combined = new Transform(combined, transforms[k]);
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
                assertEquals(0, bCombined.subtract(bRef).getNorm(), 1.0e-11);
                assertEquals(0, cCombined.subtract(cRef).getNorm(), 1.0e-10);

            }
        }

    }

    public void testReverse() {
        Random random = new Random(0x9f82ba2b2c98dac5l);
        for (int i = 0; i < 20; ++i) {
            int n = random.nextInt(20);
            Transform combined = Transform.IDENTITY;
            for (int k = 0; k < n; ++k) {
                Transform t = random.nextBoolean()
                ? new Transform(randomVector(random), randomVector(random))
                : new Transform(randomRotation(random), randomVector(random));
                combined = new Transform(combined, t);
            }

            checkNoTransform(new Transform(combined, combined.getInverse()), random);

        }

    }

    public void testTranslation() {
        Random rnd = new Random(0x7e9d737ba4147787l);
        for (int i = 0; i < 10; ++i) {
            Vector3D delta = randomVector(rnd);
            Transform transform = new Transform(delta);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                Vector3D b = transform.transformVector(a);
                assertEquals(0, b.subtract(a).getNorm(), 1.0e-10);
                Vector3D c = transform.transformPosition(a);
                assertEquals(0,
                             c.subtract(a).subtract(delta).getNorm(),
                             1.0e-13);
            }
        }
    }

    public void testRoughTransPV() {

        PVCoordinates pointP1 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_I);

        // translation transform test
        PVCoordinates pointP2 = new PVCoordinates(new Vector3D(0, 0, 0), new Vector3D(0, 0, 0));
        Transform R1toR2 = new Transform(Vector3D.MINUS_I, Vector3D.MINUS_I);
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
        Rotation R = new Rotation(Vector3D.PLUS_K, Math.PI/2);
        Transform R1toR3 = new Transform(R, new Vector3D(0, 0, -2));
        PVCoordinates result2 = R1toR3.transformPVCoordinates(pointP1);
        checkVectors(pointP3.getPosition(), result2.getPosition());
        checkVectors(pointP3.getVelocity(), result2.getVelocity());

        // test inverse rotation
        Transform R3toR1 = R1toR3.getInverse();
        PVCoordinates invResult2 = R3toR1.transformPVCoordinates(pointP3);
        checkVectors(pointP1.getPosition(), invResult2.getPosition());
        checkVectors(pointP1.getVelocity(), invResult2.getVelocity());

        // combine 2 velocity transform
        Transform R1toR4 = new Transform(new Vector3D(-2,0,0),new Vector3D(-2,0,0));
        PVCoordinates pointP4 = new PVCoordinates(new Vector3D(-1,0,0),new Vector3D(-1,0,0));
        Transform R2toR4 = new Transform(R2toR1, R1toR4);
        PVCoordinates compResult = R2toR4.transformPVCoordinates(pointP2);
        checkVectors(pointP4.getPosition() , compResult.getPosition());
        checkVectors(pointP4.getVelocity() , compResult.getVelocity());

        // combine 2 rotation tranform
        PVCoordinates pointP5 = new PVCoordinates(new Vector3D(-1,0,0),new Vector3D(-1 , 0 , 3));
        Rotation R2 = new Rotation( new Vector3D(0,0,1), Math.PI );
        Transform R1toR5 = new Transform(R2 , new Vector3D(0, -3, 0));
        Transform R3toR5 = new Transform (R3toR1, R1toR5);
        PVCoordinates combResult = R3toR5.transformPVCoordinates(pointP3);
        checkVectors(pointP5.getPosition() , combResult.getPosition());
        checkVectors(pointP5.getVelocity() , combResult.getVelocity());

        // combine translation and rotation
        Transform R2toR3 = new Transform (R2toR1,R1toR3);
        PVCoordinates Result = R2toR3.transformPVCoordinates(pointP2);
        checkVectors(pointP3.getPosition() , Result.getPosition());
        checkVectors(pointP3.getVelocity() , Result.getVelocity());

        Transform R3toR2 = new Transform (R3toR1, R1toR2);
        Result = R3toR2.transformPVCoordinates(pointP3);
        checkVectors(pointP2.getPosition() , Result.getPosition());
        checkVectors(pointP2.getVelocity() , Result.getVelocity());

        Transform newR1toR5 = new Transform(R1toR2, R2toR3);
        newR1toR5 = new   Transform(newR1toR5,R3toR5);
        Result = newR1toR5.transformPVCoordinates(pointP1);
        checkVectors(pointP5.getPosition() , Result.getPosition());
        checkVectors(pointP5.getVelocity() , Result.getVelocity());

        // more tests

        newR1toR5 = new Transform(R1toR2, R2toR3);
        Transform R3toR4 = new Transform(R3toR1, R1toR4);
        newR1toR5 = new Transform(newR1toR5, R3toR4);
        Transform R4toR5 = new Transform(R1toR4.getInverse(), R1toR5);
        newR1toR5 = new Transform(newR1toR5, R4toR5);
        Result = newR1toR5.transformPVCoordinates(pointP1);
        checkVectors(pointP5.getPosition() , Result.getPosition());
        checkVectors(pointP5.getVelocity() , Result.getVelocity());

    }

    public void testRotPV() {

        Random rnd = new Random(0x73d5554d99427af0l);

        // Instant Rotation only

        for (int i = 0; i < 10; ++i) {

            // Random instant rotation

            Rotation instantRot    = randomRotation(rnd);
            Vector3D normAxis = instantRot.getAxis();
            double w  = Math.abs(instantRot.getAngle())/86400;

            // random rotation
            Rotation rot    = randomRotation(rnd);

            // so we have a transform
            Transform tr = new Transform(rot , new Vector3D(w, normAxis));

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
            Transform tr = new Transform(trans , transVel);

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

    public void testRotation() {
        Random rnd = new Random(0x73d5554d99427af0l);
        for (int i = 0; i < 10; ++i) {

            Rotation r    = randomRotation(rnd);
            Vector3D axis = r.getAxis();
            double angle  = r.getAngle();

            Transform transform = new Transform(r);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                Vector3D b = transform.transformVector(a);
                assertEquals(Vector3D.angle(axis, a), Vector3D.angle(axis, b), 1.0e-13);
                Vector3D aOrtho = Vector3D.crossProduct(axis, a);
                Vector3D bOrtho = Vector3D.crossProduct(axis, b);
                assertEquals(angle, Vector3D.angle(aOrtho, bOrtho), 1.0e-13);
                Vector3D c = transform.transformPosition(a);
                assertEquals(0, c.subtract(b).getNorm(), 1.0e-13);
            }

        }
    }

    private void checkVectors(Vector3D v1 , Vector3D v2) {

        Vector3D d = v1.subtract(v2);

        assertEquals(0,d.getX(),1.0e-8);
        assertEquals(0,d.getY(),1.0e-8);
        assertEquals(0,d.getZ(),1.0e-8);

        assertEquals(0,d.getNorm(),1.0e-8);

        if ((v1.getNorm() > 1.0e-10) && (v2.getNorm() > 1.0e-10)) {
            Rotation r = new Rotation(v1, v2);
            assertEquals(0,r.getAngle(),1.0e-8);


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
        double q  = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private void checkNoTransform(Transform transform, Random random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = randomVector(random);
            Vector3D tA = transform.transformVector(a);
            assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            Vector3D b = randomVector(random);
            Vector3D tB = transform.transformPosition(b);
            assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * a.getNorm());
            PVCoordinates pv  = new PVCoordinates(randomVector(random), randomVector(random));
            PVCoordinates tPv = transform.transformPVCoordinates(pv);
            assertEquals(0, pv.getPosition().subtract(tPv.getPosition()).getNorm(),
                         1.0e-10 * pv.getPosition().getNorm());
            assertEquals(0, pv.getVelocity().subtract(tPv.getVelocity()).getNorm(),
                         1.0e-9 * pv.getVelocity().getNorm());
        }
    }

    public static Test suite() {
        return new TestSuite(TransformTest.class);
    }

}
