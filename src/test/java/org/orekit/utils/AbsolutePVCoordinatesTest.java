/* Copyright 2002-2024 CS GROUP
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        assertEquals(date, actual.getDate());
        assertEquals(1, actual.getPosition().getX(), 0);
        assertEquals(2, actual.getPosition().getY(), 0);
        assertEquals(3, actual.getPosition().getZ(), 0);
        assertEquals(4, actual.getVelocity().getX(), 0);
        assertEquals(5, actual.getVelocity().getY(), 0);
        assertEquals(6, actual.getVelocity().getZ(), 0);
        assertEquals(Vector3D.ZERO, actual.getAcceleration());
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
        assertEquals(date, actual.getDate());
        assertEquals(1, actual.getPosition().getX(), 0);
        assertEquals(2, actual.getPosition().getY(), 0);
        assertEquals(3, actual.getPosition().getZ(), 0);
        assertEquals(4, actual.getVelocity().getX(), 0);
        assertEquals(5, actual.getVelocity().getY(), 0);
        assertEquals(6, actual.getVelocity().getZ(), 0);
        assertEquals(Vector3D.ZERO, actual.getAcceleration());
    }

    @Test
    void testLinearConstructors() {
        Frame frame = FramesFactory.getEME2000();
        AbsolutePVCoordinates pv1 = new AbsolutePVCoordinates(frame,
                                                              AbsoluteDate.CCSDS_EPOCH,
                                                              new Vector3D( 1,  0.1,   10),
                                                              new Vector3D(-1, -0.1,  -10),
                                                              new Vector3D(10, -1.0, -100));
        AbsolutePVCoordinates pv2 = new AbsolutePVCoordinates(frame,
                                                              AbsoluteDate.FIFTIES_EPOCH,
                                                              new Vector3D( 2,  0.2,   20),
                                                              new Vector3D(-2, -0.2,  -20),
                                                              new Vector3D(20, -2.0, -200));
        AbsolutePVCoordinates pv3 = new AbsolutePVCoordinates(frame,
                                                              AbsoluteDate.GALILEO_EPOCH,
                                                              new Vector3D( 3,  0.3,   30),
                                                              new Vector3D(-3, -0.3,  -30),
                                                              new Vector3D(30, -3.0, -300));
        AbsolutePVCoordinates pv4 = new AbsolutePVCoordinates(frame,
                                                              AbsoluteDate.JULIAN_EPOCH,
                                                              new Vector3D( 4,  0.4,   40),
                                                              new Vector3D(-4, -0.4,  -40),
                                                              new Vector3D(40, -4.0, -400));
        assertPV(pv4, new AbsolutePVCoordinates(AbsoluteDate.JULIAN_EPOCH, 4, pv1), 1.0e-15);
        assertPV(pv2, new AbsolutePVCoordinates(AbsoluteDate.FIFTIES_EPOCH, pv1, pv3), 1.0e-15);
        assertPV(pv3, new AbsolutePVCoordinates(AbsoluteDate.GALILEO_EPOCH, 1, pv1, 1, pv2), 1.0e-15);
        assertPV(new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 2, pv4),
                 new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv3),
                 1.0e-15);
        assertPV(new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv3),
                 new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv4),
                 1.0e-15);
        assertPV(new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 5, pv4),
                 new AbsolutePVCoordinates(AbsoluteDate.J2000_EPOCH, 4, pv1, 3, pv2, 2, pv3, 1, pv4),
                 1.0e-15);
    }

    @Test
    void testDifferentFrames() {
        final AbsolutePVCoordinates apv1 = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                                                     AbsoluteDate.ARBITRARY_EPOCH,
                                                                     Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
        final AbsolutePVCoordinates apv2 = new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                                                                     AbsoluteDate.ARBITRARY_EPOCH,
                                                                     Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
        try {
            new AbsolutePVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, apv1, apv2);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            assertEquals(OrekitMessages.INCOMPATIBLE_FRAMES, oe.getSpecifier());
            assertEquals(apv1.getFrame().getName(), oe.getParts()[0]);
            assertEquals(apv2.getFrame().getName(), oe.getParts()[1]);
        }
    }

    @Test
    void testToDerivativeStructureVector1() {
        FieldVector3D<DerivativeStructure> fv =
                new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH,
                                          new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(1);
        assertEquals(1, fv.getX().getFreeParameters());
        assertEquals(1, fv.getX().getOrder());
        assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        assertPV(new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                           AbsoluteDate.GALILEO_EPOCH,
                                           new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           Vector3D.ZERO),
                 new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH, fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10)).shiftedBy(dt).getPosition();
            assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }

        AbsolutePVCoordinates pv = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                                             AbsoluteDate.GALILEO_EPOCH,
                                                             fv);
        assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);

    }

    @Test
    void testToDerivativeStructureVector2() {
        FieldVector3D<DerivativeStructure> fv =
                new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH,
                                          new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(2);
        assertEquals(1, fv.getX().getFreeParameters());
        assertEquals(2, fv.getX().getOrder());
        assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        assertEquals(  10.0, fv.getX().getPartialDerivative(2), 1.0e-15);
        assertEquals(  -1.0, fv.getY().getPartialDerivative(2), 1.0e-15);
        assertEquals(-100.0, fv.getZ().getPartialDerivative(2), 1.0e-15);
        assertPV(new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                           AbsoluteDate.GALILEO_EPOCH,
                                           new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)),
                 new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH, fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }

        AbsolutePVCoordinates pv = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                                             AbsoluteDate.GALILEO_EPOCH,
                                                             fv);
        assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);
        assertEquals(  10.0, pv.getAcceleration().getX(), 1.0e-15);
        assertEquals(  -1.0, pv.getAcceleration().getY(), 1.0e-15);
        assertEquals(-100.0, pv.getAcceleration().getZ(), 1.0e-15);

    }

    @Test
    void testToUnivariateDerivative1Vector() {
        FieldVector3D<UnivariateDerivative1> fv =
                new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH,
                                          new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toUnivariateDerivative1Vector();
        assertEquals(1, fv.getX().getFreeParameters());
        assertEquals(1, fv.getX().getOrder());
        assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        assertPV(new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                           AbsoluteDate.GALILEO_EPOCH,
                                           new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           Vector3D.ZERO),
                 new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH, fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10)).shiftedBy(dt).getPosition();
            assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }

        AbsolutePVCoordinates pv = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                                             AbsoluteDate.GALILEO_EPOCH,
                                                             fv);
        assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);

    }

    @Test
    void testToUnivariateDerivative2Vector() {
        FieldVector3D<UnivariateDerivative2> fv =
                new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH,
                                          new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toUnivariateDerivative2Vector();
        assertEquals(1, fv.getX().getFreeParameters());
        assertEquals(2, fv.getX().getOrder());
        assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        assertEquals(  10.0, fv.getX().getPartialDerivative(2), 1.0e-15);
        assertEquals(  -1.0, fv.getY().getPartialDerivative(2), 1.0e-15);
        assertEquals(-100.0, fv.getZ().getPartialDerivative(2), 1.0e-15);
        assertPV(new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                           AbsoluteDate.GALILEO_EPOCH,
                                           new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)),
                 new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                          AbsoluteDate.GALILEO_EPOCH, fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }

        AbsolutePVCoordinates pv = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                                             AbsoluteDate.GALILEO_EPOCH,
                                                             fv);
        assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);
        assertEquals(  10.0, pv.getAcceleration().getX(), 1.0e-15);
        assertEquals(  -1.0, pv.getAcceleration().getY(), 1.0e-15);
        assertEquals(-100.0, pv.getAcceleration().getZ(), 1.0e-15);

    }

    @Test
    void testShift() {
        Vector3D p1 = new Vector3D(  1,  0.1,   10);
        Vector3D v1 = new Vector3D( -1, -0.1,  -10);
        Vector3D a1 = new Vector3D( 10,  1.0,  100);
        Vector3D p2 = new Vector3D(  7,  0.7,   70);
        Vector3D v2 = new Vector3D(-11, -1.1, -110);
        Vector3D a2 = new Vector3D( 10,  1.0,  100);
        assertPV(new AbsolutePVCoordinates(FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, p2, v2, a2),
                 new AbsolutePVCoordinates(FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH.shiftedBy(1.0), p1, v1, a1).shiftedBy(-1.0), 1.0e-15);
        assertEquals(0.0, AbsolutePVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(new Vector3D(-6, -0.6, -60)).getNorm(), 1.0e-15);
    }

    @Test
    void testToString() {
        AbsolutePVCoordinates pv =
            new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                      AbsoluteDate.J2000_EPOCH,
                                      new Vector3D( 1,   0.1,  10),
                                      new Vector3D(-1,  -0.1, -10),
                                      new Vector3D(10,   1.0, 100));
        assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        AbsolutePVCoordinates pv = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                                                             AbsoluteDate.GALILEO_EPOCH,
                                                             new Vector3D(1, 2, 3),
                                                             new Vector3D(4, 5, 6),
                                                             new Vector3D(7, 8, 9));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(pv);

        assertTrue(bos.size() > 320);
        assertTrue(bos.size() < 340);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        AbsolutePVCoordinates deserialized  = (AbsolutePVCoordinates) ois.readObject();
        assertEquals(0.0, deserialized.getDate().durationFrom(pv.getDate()), 1.0e-15);
        assertEquals(0.0, Vector3D.distance(deserialized.getPosition(),     pv.getPosition()),     1.0e-15);
        assertEquals(0.0, Vector3D.distance(deserialized.getVelocity(),     pv.getVelocity()),     1.0e-15);
        assertEquals(0.0, Vector3D.distance(deserialized.getAcceleration(), pv.getAcceleration()), 1.0e-15);

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
        assertSame(actual.getPosition(), actual.getPosition(frame));
        assertNotSame(actual.getPosition(), actual.getPosition(otherEme2000));
        assertEquals(0.0,
                                Vector3D.distance(actual.getPosition(frame),
                                                  actual.getPosition(otherEme2000)),
                                1.0e-15);
        assertEquals(0.0,
                                Vector3D.distance(actual.getPVCoordinates(frame).getPosition(),
                                                  actual.getPVCoordinates(date, frame).getPosition()),
                                1.0e-15);
        assertEquals(0.0,
                                Vector3D.distance(actual.getPVCoordinates(frame).getVelocity(),
                                                  actual.getPVCoordinates(date, frame).getVelocity()),
                                1.0e-15);
        assertEquals(0.0,
                                Vector3D.distance(actual.getPVCoordinates(frame).getAcceleration(),
                                                  actual.getPVCoordinates(date, frame).getAcceleration()),
                                1.0e-15);
        assertEquals(0.0,
                                Vector3D.distance(actual.getPVCoordinates(frame).getPosition(),
                                                  actual.getPVCoordinates(date, otherEme2000).getPosition()),
                                1.0e-15);
        assertEquals(0.0,
                                Vector3D.distance(actual.getPVCoordinates(frame).getVelocity(),
                                                  actual.getPVCoordinates(date, otherEme2000).getVelocity()),
                                1.0e-15);
        assertEquals(0.0,
                                Vector3D.distance(actual.getPVCoordinates(frame).getAcceleration(),
                                                  actual.getPVCoordinates(date, otherEme2000).getAcceleration()),
                                1.0e-15);
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
        assertEquals(0.0,
                                Vector3D.distance(actual.getPosition(date, frame), pv.getPosition(date, frame)),
                                1.0e-15);
        assertEquals(actual.getPVCoordinates(date, frame).toString(), pv.getPVCoordinates(date, frame).toString());

    }

    public static void assertPV(TimeStampedPVCoordinates expected, TimeStampedPVCoordinates real, double epsilon) {
        assertTrue(expected.getDate().isCloseTo(real.getDate(), epsilon));
        assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
        assertEquals(expected.getAcceleration().getX(), real.getAcceleration().getX(), epsilon);
        assertEquals(expected.getAcceleration().getY(), real.getAcceleration().getY(), epsilon);
        assertEquals(expected.getAcceleration().getZ(), real.getAcceleration().getZ(), epsilon);
    }

}
