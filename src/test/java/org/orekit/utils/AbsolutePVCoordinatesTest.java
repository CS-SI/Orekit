/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

public class AbsolutePVCoordinatesTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testPVOnlyConstructor() {
        //setup
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getEME2000();
        Vector3D p = new Vector3D(1, 2, 3);
        Vector3D v = new Vector3D(4, 5, 6);

        //action
        AbsolutePVCoordinates actual = new AbsolutePVCoordinates(frame, date, p, v);

        //verify
        Assertions.assertEquals(date, actual.getDate());
        Assertions.assertEquals(1, actual.getPosition().getX(), 0);
        Assertions.assertEquals(2, actual.getPosition().getY(), 0);
        Assertions.assertEquals(3, actual.getPosition().getZ(), 0);
        Assertions.assertEquals(4, actual.getVelocity().getX(), 0);
        Assertions.assertEquals(5, actual.getVelocity().getY(), 0);
        Assertions.assertEquals(6, actual.getVelocity().getZ(), 0);
        Assertions.assertEquals(Vector3D.ZERO, actual.getAcceleration());
    }

    @Test
    void testPVCoordinatesCopyConstructor() {
        //setup
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getEME2000();
        PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));

        //action
        AbsolutePVCoordinates actual = new AbsolutePVCoordinates(frame, date, pv);

        //verify
        Assertions.assertEquals(date, actual.getDate());
        Assertions.assertEquals(1, actual.getPosition().getX(), 0);
        Assertions.assertEquals(2, actual.getPosition().getY(), 0);
        Assertions.assertEquals(3, actual.getPosition().getZ(), 0);
        Assertions.assertEquals(4, actual.getVelocity().getX(), 0);
        Assertions.assertEquals(5, actual.getVelocity().getY(), 0);
        Assertions.assertEquals(6, actual.getVelocity().getZ(), 0);
        Assertions.assertEquals(Vector3D.ZERO, actual.getAcceleration());
    }

    @Test
    void testFieldConstructor() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final Vector3D position = new Vector3D(1, 2, 3);
        final Vector3D velocity = new Vector3D(4, 5, 6);
        final Vector3D acceleration = new Vector3D(7, 8, 9);
        final FieldVector3D<UnivariateDerivative2> fieldPosition = new FieldVector3D<>(field, position)
                .add(new FieldVector3D<>(new UnivariateDerivative2(0., velocity.getX(), acceleration.getX()),
                        new UnivariateDerivative2(0., velocity.getY(), acceleration.getY()),
                        new UnivariateDerivative2(0., velocity.getZ(), acceleration.getZ())));
        // WHEN
        final AbsolutePVCoordinates apv = new AbsolutePVCoordinates(FramesFactory.getEME2000(), date, fieldPosition);
        // THEN
        Assertions.assertEquals(position, apv.getPosition());
        Assertions.assertEquals(velocity, apv.getVelocity());
        Assertions.assertEquals(acceleration, apv.getAcceleration());
    }

    @Test
    void testLinearConstructors() {
        Frame frame = FramesFactory.getEME2000();
        AbsolutePVCoordinates pv1 = new AbsolutePVCoordinates(frame,
                AbsoluteDate.CCSDS_EPOCH,
                new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100));
        AbsolutePVCoordinates pv2 = new AbsolutePVCoordinates(frame,
                AbsoluteDate.FIFTIES_EPOCH,
                new Vector3D(2, 0.2, 20),
                new Vector3D(-2, -0.2, -20),
                new Vector3D(20, -2.0, -200));
        AbsolutePVCoordinates pv3 = new AbsolutePVCoordinates(frame,
                AbsoluteDate.GALILEO_EPOCH,
                new Vector3D(3, 0.3, 30),
                new Vector3D(-3, -0.3, -30),
                new Vector3D(30, -3.0, -300));
        AbsolutePVCoordinates pv4 = new AbsolutePVCoordinates(frame,
                AbsoluteDate.JULIAN_EPOCH,
                new Vector3D(4, 0.4, 40),
                new Vector3D(-4, -0.4, -40),
                new Vector3D(40, -4.0, -400));
        assertAbsPV(pv4, new AbsolutePVCoordinates(AbsoluteDate.JULIAN_EPOCH, 4, pv1), 1.0e-15);
        assertAbsPV(pv2, new AbsolutePVCoordinates(AbsoluteDate.FIFTIES_EPOCH, pv1, pv3), 1.0e-15);
        assertAbsPV(pv3, new AbsolutePVCoordinates(AbsoluteDate.GALILEO_EPOCH, 1, pv1, 1, pv2), 1.0e-15);
        assertAbsPV(new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 2, pv4),
                new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv3),
                1.0e-15);
        assertAbsPV(new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv4),
                1.0e-15);
        assertAbsPV(new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 4, pv1, 3, pv2, 2, pv3, 1, pv4),
                1.0e-15);
    }

    @Test
    void testDifferentFrames() {
        final AbsolutePVCoordinates apv1 = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH,
                Vector3D.ZERO, Vector3D.ZERO);
        final AbsolutePVCoordinates apv2 = new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH,
                Vector3D.ZERO, Vector3D.ZERO);
        try {
            new AbsolutePVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, apv1, apv2);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_FRAMES, oe.getSpecifier());
            Assertions.assertEquals(apv1.getFrame().getName(), oe.getParts()[0]);
            Assertions.assertEquals(apv2.getFrame().getName(), oe.getParts()[1]);
        }
    }

    @Test
    void testShift() {
        Vector3D p1 = new Vector3D(1, 0.1, 10);
        Vector3D v1 = new Vector3D(-1, -0.1, -10);
        Vector3D a1 = new Vector3D(10, 1.0, 100);
        Vector3D p2 = new Vector3D(7, 0.7, 70);
        Vector3D v2 = new Vector3D(-11, -1.1, -110);
        Vector3D a2 = new Vector3D(10, 1.0, 100);
        assertAbsPV(new AbsolutePVCoordinates(FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, p2, v2, a2),
                new AbsolutePVCoordinates(FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH.shiftedBy(1.0), p1, v1, a1).shiftedBy(-1.0), 1.0e-15);
    }

    @Test
    void testToString() {
        AbsolutePVCoordinates pv =
                new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                        AbsoluteDate.J2000_EPOCH,
                        new Vector3D(1, 0.1, 10),
                        new Vector3D(-1, -0.1, -10),
                        new Vector3D(10, 1.0, 100));
        Assertions.assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    void testSamePV() {
        //setup
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getEME2000();
        Vector3D p = new Vector3D(1, 2, 3);
        Vector3D v = new Vector3D(4, 5, 6);
        Frame otherEme2000 = new Frame(frame, Transform.IDENTITY, "other-EME2000");

        //action
        AbsolutePVCoordinates actual = new AbsolutePVCoordinates(frame, date, p, v);

        //verify
        Assertions.assertSame(actual.getPosition(), actual.getPosition(frame));
        Assertions.assertNotSame(actual.getPosition(), actual.getPosition(otherEme2000));
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPosition(frame),
                        actual.getPosition(otherEme2000)),
                1.0e-15);
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPVCoordinates(frame).getPosition(),
                        actual.getPVCoordinates(date, frame).getPosition()),
                1.0e-15);
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPVCoordinates(frame).getVelocity(),
                        actual.getPVCoordinates(date, frame).getVelocity()),
                1.0e-15);
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPVCoordinates(frame).getAcceleration(),
                        actual.getPVCoordinates(date, frame).getAcceleration()),
                1.0e-15);
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPVCoordinates(frame).getPosition(),
                        actual.getPVCoordinates(date, otherEme2000).getPosition()),
                1.0e-15);
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPVCoordinates(frame).getVelocity(),
                        actual.getPVCoordinates(date, otherEme2000).getVelocity()),
                1.0e-15);
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPVCoordinates(frame).getAcceleration(),
                        actual.getPVCoordinates(date, otherEme2000).getAcceleration()),
                1.0e-15);
    }

    @Test
    void testIssue1557() {
        // GIVEN
        final AbsolutePVCoordinates absPV = TestUtils.getFakeAbsolutePVCoordinates();

        // WHEN
        final Vector3D velocity = absPV.getVelocity(absPV.getDate(), absPV.getFrame());

        // THEN
        final PVCoordinates refPV = absPV.getPVCoordinates();
        Assertions.assertEquals(refPV.getVelocity(), velocity);
    }

    @Test
    void testTaylorProvider() {
        //setup
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getEME2000();
        Vector3D p = new Vector3D(1, 2, 3);
        Vector3D v = new Vector3D(4, 5, 6);

        //action
        AbsolutePVCoordinates actual = new AbsolutePVCoordinates(frame, date, p, v);
        final PVCoordinatesProvider pv = actual.toTaylorProvider();

        //verify 
        Assertions.assertEquals(0.0,
                Vector3D.distance(actual.getPosition(date, frame), pv.getPosition(date, frame)),
                1.0e-15);
        Assertions.assertEquals(actual.getPVCoordinates(date, frame).toString(), pv.getPVCoordinates(date, frame).toString());

    }

    public static void assertPV(TimeStampedPVCoordinates expected, TimeStampedPVCoordinates real, double epsilon) {
        Assertions.assertTrue(expected.getDate().isCloseTo(real.getDate(), epsilon));
        Assertions.assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        Assertions.assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        Assertions.assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getX(), real.getAcceleration().getX(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getY(), real.getAcceleration().getY(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getZ(), real.getAcceleration().getZ(), epsilon);
    }

    public static void assertAbsPV(AbsolutePVCoordinates expected, AbsolutePVCoordinates real, double epsilon) {
        assertPV(expected.getPVCoordinates(), real.getPVCoordinates(), epsilon);
    }
}
