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

import java.util.Random;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class FrameTest {

    @Test
    public void testSameFrameRoot() throws OrekitException {
        Random random = new Random(0x29448c7d58b95565l);
        Frame  frame  = FramesFactory.getEME2000();
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
        Assert.assertTrue(frame.getDepth() > 0);
        Assert.assertEquals(frame.getParent().getDepth() + 1, frame.getDepth());
    }

    @Test
    public void testSameFrameNoRoot() throws OrekitException {
        Random random = new Random(0xc6e88d0f53e29116l);
        Transform t   = randomTransform(random);
        Frame frame   = new Frame(FramesFactory.getEME2000(), t, null, true);
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    @Test
    public void testSimilarFrames() throws OrekitException {
        Random random = new Random(0x1b868f67a83666e5l);
        Transform t   = randomTransform(random);
        Frame frame1  = new Frame(FramesFactory.getEME2000(), t, null, true);
        Frame frame2  = new Frame(FramesFactory.getEME2000(), t, null, false);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testFromParent() throws OrekitException {
        Random random = new Random(0xb92fba1183fe11b8l);
        Transform fromEME2000  = randomTransform(random);
        Frame frame = new Frame(FramesFactory.getEME2000(), fromEME2000, null);
        Transform toEME2000 = frame.getTransformTo(FramesFactory.getEME2000(), new AbsoluteDate());
        checkNoTransform(new Transform(fromEME2000.getDate(), fromEME2000, toEME2000), random);
    }

    @Test
    public void testDecomposedTransform() throws OrekitException {
        Random random = new Random(0xb7d1a155e726da57l);
        Transform t1  = randomTransform(random);
        Transform t2  = randomTransform(random);
        Transform t3  = randomTransform(random);
        Frame frame1 =
            new Frame(FramesFactory.getEME2000(),
                      new Transform(t1.getDate(), new Transform(t1.getDate(), t1, t2), t3),
                      null);
        Frame frame2 =
            new Frame(new Frame(new Frame(FramesFactory.getEME2000(), t1, null), t2, null), t3, null);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testFindCommon() throws OrekitException {

        Random random = new Random(0xb7d1a155e726da57l);
        Transform t1  = randomTransform(random);
        Transform t2  = randomTransform(random);
        Transform t3  = randomTransform(random);

        Frame R1 = new Frame(FramesFactory.getEME2000(),t1,"R1");
        Frame R2 = new Frame(R1,t2,"R2");
        Frame R3 = new Frame(R2,t3,"R3");
        Assert.assertTrue(R1.getDepth() > 0);
        Assert.assertEquals(R1.getDepth() + 1, R2.getDepth());
        Assert.assertEquals(R2.getDepth() + 1, R3.getDepth());

        Transform T = R1.getTransformTo(R3, new AbsoluteDate());

        Transform S = new Transform(t2.getDate(), t2,t3);

        checkNoTransform(new Transform(T.getDate(), T, S.getInverse()) , random);

    }

    @Test
    public void testDepthAndAncestor() throws OrekitException{
        Random random = new Random(0x01f8d3b944123044l);
        Frame root = Frame.getRoot();

        Frame f1 = new Frame(root, randomTransform(random), "f1");
        Frame f2 = new Frame(f1,   randomTransform(random), "f2");
        Frame f3 = new Frame(f1,   randomTransform(random), "f3");
        Frame f4 = new Frame(f2,   randomTransform(random), "f4");
        Frame f5 = new Frame(f3,   randomTransform(random), "f5");
        Frame f6 = new Frame(f5,   randomTransform(random), "f6");

        Assert.assertEquals(0, root.getDepth());
        Assert.assertEquals(1, f1.getDepth());
        Assert.assertEquals(2, f2.getDepth());
        Assert.assertEquals(2, f3.getDepth());
        Assert.assertEquals(3, f4.getDepth());
        Assert.assertEquals(3, f5.getDepth());
        Assert.assertEquals(4, f6.getDepth());

        Assert.assertTrue(root == f1.getAncestor(1));
        Assert.assertTrue(root == f6.getAncestor(4));
        Assert.assertTrue(f1   == f6.getAncestor(3));
        Assert.assertTrue(f3   == f6.getAncestor(2));
        Assert.assertTrue(f5   == f6.getAncestor(1));
        Assert.assertTrue(f6   == f6.getAncestor(0));

        try {
            f6.getAncestor(5);
            Assert.fail("an exception should have been triggered");
        } catch (IllegalArgumentException iae) {
            // expected behavior
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getClass().getName());
        }

    }

    @Test
    public void testIsChildOf() throws OrekitException{
        Random random = new Random(0xb7d1a155e726da78l);
        Frame eme2000 = FramesFactory.getEME2000();

        Frame f1 = new Frame(eme2000, randomTransform(random), "f1");
        Frame f2 = new Frame(f1     , randomTransform(random), "f2");
        Frame f4 = new Frame(f2     , randomTransform(random), "f4");
        Frame f5 = new Frame(f4     , randomTransform(random), "f5");
        Frame f6 = new Frame(eme2000, randomTransform(random), "f6");
        Frame f7 = new Frame(f6     , randomTransform(random), "f7");
        Frame f8 = new Frame(f6     , randomTransform(random), "f8");
        Frame f9 = new Frame(f7     , randomTransform(random), "f9");

        // check if the root frame can be an ancestor of another frame
        Assert.assertEquals(false, eme2000.isChildOf(f5));

        // check if a frame which belongs to the same branch than the 2nd frame is a branch of it
        Assert.assertEquals(true, f5.isChildOf(f1));

        // check if a random frame is the child of the root frame
        Assert.assertEquals(true, f9.isChildOf(eme2000));

        // check that a frame is not its own child
        Assert.assertEquals(false, f4.isChildOf(f4));

        // check if a frame which belongs to a different branch than the 2nd frame can be a child for it
        Assert.assertEquals(false, f9.isChildOf(f5));

        // check if the root frame is not a child of itself
        Assert.assertEquals(false, eme2000.isChildOf(eme2000));

        Assert.assertEquals(false, f9.isChildOf(f8));

    }

    @Test
    public void testH0m9() throws OrekitException {
        AbsoluteDate h0         = new AbsoluteDate("2010-07-01T10:42:09", TimeScalesFactory.getUTC());
        Frame itrf              = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame rotatingPadFrame  = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                            Constants.WGS84_EARTH_FLATTENING,
                                                                            itrf),
                                                       new GeodeticPoint(FastMath.toRadians(5.0),
                                                                                              FastMath.toRadians(-100.0),
                                                                                              0.0),
                                                       "launch pad");

        // create a new inertially oriented frame that is aligned with ITRF at h0 - 9 seconds
        AbsoluteDate h0M9       = h0.shiftedBy(-9.0);
        Frame eme2000           = FramesFactory.getEME2000();
        Frame frozenLaunchFrame = rotatingPadFrame.getFrozenFrame(eme2000, h0M9, "launch frame");

        // check velocity module is unchanged
        Vector3D pEme2000 = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D vEme2000 = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvEme2000 = new PVCoordinates(pEme2000, vEme2000);
        PVCoordinates pvH0m9 = eme2000.getTransformTo(frozenLaunchFrame, h0M9).transformPVCoordinates(pvEme2000);
        Assert.assertEquals(vEme2000.getNorm(), pvH0m9.getVelocity().getNorm(), 1.0e-6);

        // this frame is fixed with respect to EME2000 but rotates with respect to the non-frozen one
        // the following loop should have a fixed angle a1 and an evolving angle a2
        double minA1 = Double.POSITIVE_INFINITY;
        double maxA1 = Double.NEGATIVE_INFINITY;
        double minA2 = Double.POSITIVE_INFINITY;
        double maxA2 = Double.NEGATIVE_INFINITY;
        double dt;
        for (dt = 0; dt < 86164; dt += 300.0) {
            AbsoluteDate date = h0M9.shiftedBy(dt);
            double a1 = frozenLaunchFrame.getTransformTo(eme2000,          date).getRotation().getAngle();
            double a2 = frozenLaunchFrame.getTransformTo(rotatingPadFrame, date).getRotation().getAngle();
            minA1 = FastMath.min(minA1, a1);
            maxA1 = FastMath.max(maxA1, a1);
            minA2 = FastMath.min(minA2, a2);
            maxA2 = FastMath.max(maxA2, a2);
        }
        Assert.assertEquals(0, maxA1 - minA1, 1.0e-12);
        Assert.assertEquals(FastMath.PI, maxA2 - minA2, 0.01);

    }

    private Transform randomTransform(Random random) {
        Transform transform = Transform.IDENTITY;
        for (int i = random.nextInt(10); i > 0; --i) {
            if (random.nextBoolean()) {
                Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), u));
            } else {
                double q0 = random.nextDouble() * 2 - 1;
                double q1 = random.nextDouble() * 2 - 1;
                double q2 = random.nextDouble() * 2 - 1;
                double q3 = random.nextDouble() * 2 - 1;
                double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
                Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), r));
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
            Assert.assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
            Vector3D c = transform.transformPosition(a);
            Assert.assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
