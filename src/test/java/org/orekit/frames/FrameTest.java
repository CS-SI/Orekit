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
import org.orekit.errors.FrameAncestorException;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FrameTest extends TestCase {

    public void testSameFrameRoot() throws OrekitException {
        Random random = new Random(0x29448c7d58b95565l);
        Frame  frame  = Frame.getJ2000();
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    public void testSameFrameNoRoot() throws OrekitException {
        Random random = new Random(0xc6e88d0f53e29116l);
        Transform t   = randomTransform(random);
        Frame frame   = new Frame(Frame.getJ2000(), t, null);
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    public void testSimilarFrames() throws OrekitException {
        Random random = new Random(0x1b868f67a83666e5l);
        Transform t   = randomTransform(random);
        Frame frame1  = new Frame(Frame.getJ2000(), t, null);
        Frame frame2  = new Frame(Frame.getJ2000(), t, null);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    public void testFromParent() throws OrekitException {
        Random random = new Random(0xb92fba1183fe11b8l);
        Transform fromJ2000  = randomTransform(random);
        Frame frame = new Frame(Frame.getJ2000(), fromJ2000, null);
        Transform toJ2000 = frame.getTransformTo(Frame.getJ2000(), new AbsoluteDate());
        checkNoTransform(new Transform(fromJ2000, toJ2000), random);
    }

    public void testDecomposedTransform() throws OrekitException {
        Random random = new Random(0xb7d1a155e726da57l);
        Transform t1  = randomTransform(random);
        Transform t2  = randomTransform(random);
        Transform t3  = randomTransform(random);
        Frame frame1 =
            new Frame(Frame.getJ2000(), new Transform(new Transform(t1, t2), t3), null);
        Frame frame2 =
            new Frame(new Frame(new Frame(Frame.getJ2000(), t1, null), t2, null), t3, null);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    public void testFindCommon() throws OrekitException {

        Random random = new Random(0xb7d1a155e726da57l);
        Transform t1  = randomTransform(random);
        Transform t2  = randomTransform(random);
        Transform t3  = randomTransform(random);

        Frame R1 = new Frame(Frame.getJ2000(),t1,"R1");
        Frame R2 = new Frame(R1,t2,"R2");
        Frame R3 = new Frame(R2,t3,"R3");

        Transform T = R1.getTransformTo(R3, new AbsoluteDate());

        Transform S = new Transform(t2,t3);

        checkNoTransform(new Transform(T, S.getInverse()) , random);

    }

    public void testVeis1950() throws OrekitException {
        Transform t = Frame.getVeis1950().getTransformTo(Frame.getJ2000(), new AbsoluteDate());
        Vector3D i50    = t.transformVector(Vector3D.PLUS_I);
        Vector3D j50    = t.transformVector(Vector3D.PLUS_J);
        Vector3D k50    = t.transformVector(Vector3D.PLUS_K);
        Vector3D i50Ref = new Vector3D( 0.9999256489473456,
                                        0.011181451214217871,
                                        4.8653597990872734e-3);
        Vector3D j50Ref = new Vector3D(-0.011181255200285388,
                                       0.9999374855347822,
                                       -6.748721516262951e-5);
        Vector3D k50Ref = new Vector3D(-4.865810248725263e-3,
                                       1.3081367862337385e-5,
                                       0.9999881617896792);
        assertEquals(0, i50.subtract(i50Ref).getNorm(), 1.0e-15);
        assertEquals(0, j50.subtract(j50Ref).getNorm(), 1.0e-15);
        assertEquals(0, k50.subtract(k50Ref).getNorm(), 1.0e-15);
    }

    public void testIsChildOf() throws OrekitException{
        Random random = new Random(0xb7d1a155e726da78l);
        Frame j2000   = Frame.getJ2000();

        Frame f1 = new Frame(j2000, randomTransform(random), "f1");
        Frame f2 = new Frame(f1   , randomTransform(random), "f2");
        Frame f4 = new Frame(f2   , randomTransform(random), "f4");
        Frame f5 = new Frame(f4   , randomTransform(random), "f5");
        Frame f6 = new Frame(j2000, randomTransform(random), "f6");
        Frame f7 = new Frame(f6   , randomTransform(random), "f7");
        Frame f8 = new Frame(f6   , randomTransform(random), "f8");
        Frame f9 = new Frame(f7   , randomTransform(random), "f9");

        // check if the root frame can be an ancestor of another frame
        assertEquals(false, j2000.isChildOf(f5));

        // check if a frame which belongs to the same branch than the 2nd frame is a branch of it
        assertEquals(true, f5.isChildOf(f1));

        // check if a random frame is the child of the root frame 
        assertEquals(true, f9.isChildOf(j2000));

        // check that a frame is not its own child
        assertEquals(false, f4.isChildOf(f4));

        // check if a frame which belong to a different branch than the 2nd frame can be a child for it
        assertEquals(false, f9.isChildOf(f5));

        // check if the root frame is not a child of itself
        assertEquals(false, j2000.isChildOf(j2000));

        assertEquals(false, f9.isChildOf(f8));

    }

    public void testUpdateTransform() throws OrekitException {
        Random random     = new Random(0x2f6769c23e53e96el);
        Frame j2000       = Frame.getJ2000();
        AbsoluteDate date = new AbsoluteDate();

        Frame f1 = new Frame(j2000, randomTransform(random), "f1");
        Frame f2 = new Frame(f1   , randomTransform(random), "f2");
        Frame f3 = new Frame(f2   , randomTransform(random), "f3");
        Frame f4 = new Frame(f2   , randomTransform(random), "f4");
        Frame f5 = new Frame(f4   , randomTransform(random), "f5");
        Frame f6 = new Frame(j2000, randomTransform(random), "f6");
        Frame f7 = new Frame(f6   , randomTransform(random), "f7");
        Frame f8 = new Frame(f6   , randomTransform(random), "f8");
        Frame f9 = new Frame(f7   , randomTransform(random), "f9");

        checkFrameAncestorException(f6, f8, f9, randomTransform(random), date);
        checkFrameAncestorException(f6, f3, f5, randomTransform(random), date);
        checkFrameAncestorException(j2000, f5, f9, randomTransform(random), date);
        checkFrameAncestorException(f3, j2000, f6, randomTransform(random), date);    

        checkUpdateTransform(f1, f5, f9, date, random);
        checkUpdateTransform(f7, f6, f9, date, random);
        checkUpdateTransform(f6, j2000, f7, date, random);

        checkUpdateTransform(f6, f6.getParent(), f6, date, random);

    }

    private void checkFrameAncestorException(Frame f0, Frame f1, Frame f2,
                                             Transform transform, AbsoluteDate date) {
        doCheckFrameAncestorException(f0, f1, f2, transform, date);
        doCheckFrameAncestorException(f0, f2, f1, transform, date);
    }

    private void doCheckFrameAncestorException(Frame f0, Frame f1, Frame f2,
                                               Transform transform, AbsoluteDate date) {
        try {
            f0.updateTransform(f1, f2, transform, date);
            fail("Should raise a FrameAncestorException");
        } catch(FrameAncestorException expected){
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    private void checkUpdateTransform(Frame f0, Frame f1, Frame f2,
                                      AbsoluteDate date, Random random)
      throws OrekitException {
        Transform f1ToF2 = randomTransform(random);

        f0.updateTransform(f1, f2, f1ToF2, date);
        Transform obtained12 = f1.getTransformTo(f2, date);
        checkNoTransform(new Transform(f1ToF2, obtained12.getInverse()), random);

        f0.updateTransform(f2, f1, f1ToF2.getInverse(), date);
        Transform obtained21 = f2.getTransformTo(f1, date);
        checkNoTransform(new Transform(f1ToF2.getInverse(), obtained21.getInverse()), random);

        checkNoTransform(new Transform(obtained12, obtained21), random);

    }

    private Transform randomTransform(Random random) {
        Transform transform = Transform.IDENTITY;
        for (int i = random.nextInt(10); i > 0; --i) {
            if (random.nextBoolean()) {
                Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0);
                transform = new Transform(transform, new Transform(u));
            } else {
                double q0 = random.nextDouble() * 2 - 1;
                double q1 = random.nextDouble() * 2 - 1;
                double q2 = random.nextDouble() * 2 - 1;
                double q3 = random.nextDouble() * 2 - 1;
                double q  = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
                Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
                transform = new Transform(transform, new Transform(r));
            }
        }
        return transform;
    }

    private void checkNoTransform(Transform transform, Random random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = new Vector3D(random.nextDouble(),
                                      random.nextDouble(),
                                      random.nextDouble());
            Vector3D b = transform.transformVector(a);
            assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
            Vector3D c = transform.transformPosition(a);
            assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
        }
    }

    public static Test suite() {
        return new TestSuite(FrameTest.class);
    }

}
