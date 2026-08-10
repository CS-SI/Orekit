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


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
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
import org.orekit.time.FieldAbsoluteDate;

class FieldAbsolutePVCoordinatesTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testPVOnlyConstructor() {
        doTestPVOnlyConstructor(Binary64Field.getInstance());
    }

    @Test
    void testPVCoordinatesCopyConstructor() {
        doTestPVCoordinatesCopyConstructor(Binary64Field.getInstance());
    }

    @Test
    void testLinearConstructors() {
        doTestLinearConstructors(Binary64Field.getInstance());
    }

    @Test
    void testDifferentFrames() {
        doTestDifferentFrames(Binary64Field.getInstance());
    }

    @Test
    void testShift() {
        doTestShift(Binary64Field.getInstance());
    }

    @Test
    void testToString() {
        doTestToString(Binary64Field.getInstance());
    }

    @Test
    void testSamePV() {
        doTestSamePV(Binary64Field.getInstance());
    }

    @Test
    void testTaylorProvider() {
        doTestTaylorProvider(Binary64Field.getInstance());
    }

    @Test
    void testIssue1557() {
        // GIVEN
        final FieldAbsolutePVCoordinates<Binary64> absPV = TestUtils.getFakeFieldAbsolutePVACoordinates();

        // WHEN
        final FieldVector3D<Binary64> velocity = absPV.getVelocity(absPV.getDate(), absPV.getFrame());

        // THEN
        final FieldPVCoordinates<Binary64> refPV = absPV.getPVCoordinates();
        Assertions.assertEquals(refPV.getVelocity(), velocity);
    }

    private <T extends CalculusFieldElement<T>> void doTestPVOnlyConstructor(Field<T> field) {
        //setup
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        final T one = field.getOne();
        FieldVector3D<T> p = new FieldVector3D<>(one, one.multiply(2.0), one.multiply(3.0));
        FieldVector3D<T> v = new FieldVector3D<>(one.multiply(4.0), one.multiply(5.0), one.multiply(6.0));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, p, v);

        //verify
        Assertions.assertEquals(date, actual.getDate());
        Assertions.assertEquals(1.0, actual.getPosition().getX().getReal(), 0.0);
        Assertions.assertEquals(2.0, actual.getPosition().getY().getReal(), 0.0);
        Assertions.assertEquals(3.0, actual.getPosition().getZ().getReal(), 0.0);
        Assertions.assertEquals(4.0, actual.getVelocity().getX().getReal(), 0.0);
        Assertions.assertEquals(5.0, actual.getVelocity().getY().getReal(), 0.0);
        Assertions.assertEquals(6.0, actual.getVelocity().getZ().getReal(), 0.0);
        Assertions.assertEquals(FieldVector3D.getZero(field), actual.getAcceleration());
    }

    private <T extends CalculusFieldElement<T>> void doTestPVCoordinatesCopyConstructor(Field<T> field) {
        //setup
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        final T one = field.getOne();
        FieldPVCoordinates<T> pv = new FieldPVCoordinates<>(new FieldVector3D<>(one, one.multiply(2), one.multiply(3)), new FieldVector3D<>(one.multiply(4), one.multiply(5), one.multiply(6)));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, pv);

        //verify
        Assertions.assertEquals(date, actual.getDate());
        Assertions.assertEquals(1.0, actual.getPosition().getX().getReal(), 0.0);
        Assertions.assertEquals(2.0, actual.getPosition().getY().getReal(), 0.0);
        Assertions.assertEquals(3.0, actual.getPosition().getZ().getReal(), 0.0);
        Assertions.assertEquals(4.0, actual.getVelocity().getX().getReal(), 0.0);
        Assertions.assertEquals(5.0, actual.getVelocity().getY().getReal(), 0.0);
        Assertions.assertEquals(6.0, actual.getVelocity().getZ().getReal(), 0.0);
        Assertions.assertEquals(FieldVector3D.getZero(field), actual.getAcceleration());
    }

    private <T extends CalculusFieldElement<T>> void doTestLinearConstructors(Field<T> field) {
        Frame frame = FramesFactory.getEME2000();
        final T one = field.getOne();
        FieldAbsolutePVCoordinates<T> pv1 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getCCSDSEpoch(field),
                                                              new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                                              new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                                              new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0)));
        FieldAbsolutePVCoordinates<T> pv2 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getFiftiesEpoch(field),
                                                              new FieldVector3D<>(one.multiply(2.0), one.multiply(0.2), one.multiply(20.0)),
                                                              new FieldVector3D<>(one.multiply(-2.0), one.multiply(-0.2), one.multiply(-20.0)),
                                                              new FieldVector3D<>(one.multiply(20.0), one.multiply(-2.0), one.multiply(-200.0)));
        FieldAbsolutePVCoordinates<T> pv3 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getGalileoEpoch(field),
                                                                new FieldVector3D<>(one.multiply(3.0), one.multiply(0.3), one.multiply(30.0)),
                                                                new FieldVector3D<>(one.multiply(-3.0), one.multiply(-0.3), one.multiply(-30.0)),
                                                                new FieldVector3D<>(one.multiply(30.0), one.multiply(-3.0), one.multiply(-300.0)));
        FieldAbsolutePVCoordinates<T> pv4 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getJulianEpoch(field),
                                                                new FieldVector3D<>(one.multiply(4.0), one.multiply(0.4), one.multiply(40.0)),
                                                                new FieldVector3D<>(one.multiply(-4.0), one.multiply(-0.4), one.multiply(-40.0)),
                                                                new FieldVector3D<>(one.multiply(40.0), one.multiply(-4.0), one.multiply(-400.0)));
        checkPV(pv4, new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJulianEpoch(field), one.multiply(4.0), pv1), 1.0e-15);
        checkPV(pv2, new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getFiftiesEpoch(field), pv1, pv3), 1.0e-15);
        checkPV(pv3, new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(field), one, pv1, one, pv2), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(2.0), pv4),
                new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv1, one, pv2, one, pv3),
                1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv3),
                new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv1, one, pv2, one, pv4),
                1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(5.0), pv4),
                new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(4.0), pv1, one.multiply(3.0), pv2, one.multiply(2.0), pv3, one, pv4),
                1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestDifferentFrames(Field<T> field) {
        final FieldVector3D<T> zero = FieldVector3D.getZero(field);
        FieldAbsolutePVCoordinates<T> apv1 = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                                                              FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                              zero, zero, zero);
        FieldAbsolutePVCoordinates<T> apv2 = new FieldAbsolutePVCoordinates<>(FramesFactory.getGCRF(),
                                                                              FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                              zero, zero, zero);
        try {
            new FieldAbsolutePVCoordinates<>(FieldAbsoluteDate.getArbitraryEpoch(field), apv1, apv2);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_FRAMES, oe.getSpecifier());
            Assertions.assertEquals(apv1.getFrame().getName(), oe.getParts()[0]);
            Assertions.assertEquals(apv2.getFrame().getName(), oe.getParts()[1]);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestShift(Field<T> field) {
        final T one = field.getOne();
        FieldVector3D<T> p1 = new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0));
        FieldVector3D<T> v1 = new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10));
        FieldVector3D<T> a1 = new FieldVector3D<>(one.multiply(10.0), one, one.multiply(100.0));
        FieldVector3D<T> p2 = new FieldVector3D<>(one.multiply(7.0), one.multiply(0.7), one.multiply(70.0));
        FieldVector3D<T> v2 = new FieldVector3D<>(one.multiply(-11.0), one.multiply(-1.1), one.multiply(-110.0));
        FieldVector3D<T> a2 = new FieldVector3D<>(one.multiply(10.0), one, one.multiply(100.0));
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), FieldAbsoluteDate.getJ2000Epoch(field), p2, v2, a2),
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(1.0), p1, v1, a1).shiftedBy(one.multiply(-1.0)), 1.0e-15);
    }

    @Test
    void testFieldConstructor() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Binary64> position = FieldVector3D.getMinusI(field);
        final FieldVector3D<Binary64> velocity = FieldVector3D.getMinusK(field);
        final FieldPVCoordinates<Binary64> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        final FieldVector3D<FieldUnivariateDerivative2<Binary64>> fieldPosition = pvCoordinates.toUnivariateDerivative2Vector();
        // WHEN
        final FieldAbsolutePVCoordinates<Binary64> fieldAbsolutePVCoordinates = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                date, fieldPosition);
        // THEN
        Assertions.assertEquals(date, fieldAbsolutePVCoordinates.getDate());
        Assertions.assertEquals(position, fieldAbsolutePVCoordinates.getPosition());
        Assertions.assertEquals(velocity, fieldAbsolutePVCoordinates.getVelocity());
    }

    @Test
    void testToAbsolutePVCoordinates() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D position = Vector3D.MINUS_I;
        final Vector3D velocity = Vector3D.MINUS_K;
        final AbsolutePVCoordinates expectedApv = new AbsolutePVCoordinates(FramesFactory.getEME2000(), date, position, velocity);
        final FieldAbsolutePVCoordinates<Binary64> fieldPV = new FieldAbsolutePVCoordinates<>(field, expectedApv);
        // WHEN
        final AbsolutePVCoordinates actualApv = fieldPV.toAbsolutePVCoordinates();
        // THEN
        Assertions.assertEquals(expectedApv.getFrame(), actualApv.getFrame());
        Assertions.assertEquals(expectedApv.getDate(), actualApv.getDate());
        Assertions.assertEquals(expectedApv.getPosition(), actualApv.getPosition());
        Assertions.assertEquals(expectedApv.getVelocity(), actualApv.getVelocity());
        Assertions.assertEquals(expectedApv.getAcceleration(), actualApv.getAcceleration());
    }

    @Test
    void testShiftNonField() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Binary64> position = FieldVector3D.getMinusI(field);
        final FieldVector3D<Binary64> velocity = FieldVector3D.getMinusK(field);
        final FieldAbsolutePVCoordinates<Binary64> apv = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                date, position, velocity);
        final double dt = 1.;
        // WHEN
        final FieldAbsolutePVCoordinates<Binary64> shiftedDouble = apv.shiftedBy(dt);
        // THEN
        final FieldAbsolutePVCoordinates<Binary64> shifted = apv.shiftedBy(new Binary64(dt));
        Assertions.assertEquals(shiftedDouble.getFrame(), shifted.getFrame());
        Assertions.assertEquals(shiftedDouble.getDate(), shifted.getDate());
        Assertions.assertEquals(shiftedDouble.getPosition(), shifted.getPosition());
        Assertions.assertEquals(shiftedDouble.getVelocity(), shifted.getVelocity());
        Assertions.assertEquals(shiftedDouble.getAcceleration(), shifted.getAcceleration());
    }

    @Test
    void testGetVelocityFrame() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Binary64> position = FieldVector3D.getMinusI(field);
        final FieldVector3D<Binary64> velocity = FieldVector3D.getMinusK(field);
        final FieldAbsolutePVCoordinates<Binary64> apv = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                date, position, velocity);
        final Frame otherFrame = FramesFactory.getGCRF();
        // WHEN
        final FieldVector3D<Binary64> velocityInOtherFrame = apv.getVelocity(otherFrame);
        // THEN
        final FieldVector3D<Binary64> expectedVelocity = apv.getPVCoordinates(otherFrame).getVelocity();
        Assertions.assertEquals(expectedVelocity, velocityInOtherFrame);
    }

    @Test
    void testGetVelocityDate() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Binary64> position = FieldVector3D.getMinusI(field);
        final FieldVector3D<Binary64> velocity = FieldVector3D.getMinusK(field);
        final FieldAbsolutePVCoordinates<Binary64> apv = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                date, position, velocity);
        final Frame otherFrame = FramesFactory.getGCRF();
        final FieldAbsoluteDate<Binary64> otherDate = date.shiftedBy(1);
        // WHEN
        final FieldVector3D<Binary64> velocityAtOtherDate = apv.getVelocity(otherDate, otherFrame);
        // THEN
        final FieldVector3D<Binary64> expectedVelocity = apv.getPVCoordinates(otherDate, otherFrame).getVelocity();
        Assertions.assertEquals(expectedVelocity, velocityAtOtherDate);
    }

    private <T extends CalculusFieldElement<T>> void doTestToString(Field<T> field) {
        final T one = field.getOne();
        FieldAbsolutePVCoordinates<T> pv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getJ2000Epoch(field),
                                        new FieldVector3D<>(one.multiply(1.0),   one.multiply(0.1),  one.multiply(10.0)),
                                        new FieldVector3D<>(one.multiply(-1.0),  one.multiply(-0.1), one.multiply(-10.0)),
                                        new FieldVector3D<>(one.multiply(10.0),  one.multiply(1.0),  one.multiply(100.0)));
        Assertions.assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    private <T extends CalculusFieldElement<T>> void doTestSamePV(Field<T> field) {
        //setup
        final T one = field.getOne();
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        Frame otherEme2000 = new Frame(frame, Transform.IDENTITY, "other-EME2000");
        FieldVector3D<T> p = new FieldVector3D<>(one.multiply(1), one.multiply(2), one.multiply(3));
        FieldVector3D<T> v = new FieldVector3D<>(one.multiply(4), one.multiply(5), one.multiply(6));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, p, v);

        //verify
        Assertions.assertSame(actual.getPosition(), actual.getPosition(frame));
        Assertions.assertNotSame(actual.getPosition(), actual.getPosition(otherEme2000));
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPosition(frame),
                                                       actual.getPosition(otherEme2000)).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPVCoordinates(frame).getPosition(),
                                                       actual.getPVCoordinates(date, frame).getPosition()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPVCoordinates(frame).getVelocity(),
                                                       actual.getPVCoordinates(date, frame).getVelocity()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPVCoordinates(frame).getAcceleration(),
                                                       actual.getPVCoordinates(date, frame).getAcceleration()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPVCoordinates(frame).getPosition(),
                                                       actual.getPVCoordinates(date, otherEme2000).getPosition()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPVCoordinates(frame).getVelocity(),
                                                       actual.getPVCoordinates(date, otherEme2000).getVelocity()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPVCoordinates(frame).getAcceleration(),
                                                       actual.getPVCoordinates(date, otherEme2000).getAcceleration()).getReal(),
                                1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestTaylorProvider(Field<T> field) {
        //setup
        final T one = field.getOne();
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        FieldVector3D<T> p = new FieldVector3D<>(one.multiply(1), one.multiply(2), one.multiply(3));
        FieldVector3D<T> v = new FieldVector3D<>(one.multiply(4), one.multiply(5), one.multiply(6));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, p, v);
        final FieldPVCoordinatesProvider<T> pv = actual.toTaylorProvider();

        //verify
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(actual.getPosition(date, frame), pv.getPosition(date, frame)).getReal(),
                                1.0e-15);
        Assertions.assertEquals(actual.getPVCoordinates(date, frame).toString(), pv.getPVCoordinates(date, frame).toString());
    }

    private <T extends CalculusFieldElement<T>> void checkPV(FieldAbsolutePVCoordinates<T> expected, FieldAbsolutePVCoordinates<T> real, double epsilon) {
        Assertions.assertEquals(expected.getDate(), real.getDate());
        Assertions.assertEquals(expected.getPosition().getX().getReal(),     real.getPosition().getX().getReal(),     epsilon);
        Assertions.assertEquals(expected.getPosition().getY().getReal(),     real.getPosition().getY().getReal(),     epsilon);
        Assertions.assertEquals(expected.getPosition().getZ().getReal(),     real.getPosition().getZ().getReal(),     epsilon);
        Assertions.assertEquals(expected.getVelocity().getX().getReal(),     real.getVelocity().getX().getReal(),     epsilon);
        Assertions.assertEquals(expected.getVelocity().getY().getReal(),     real.getVelocity().getY().getReal(),     epsilon);
        Assertions.assertEquals(expected.getVelocity().getZ().getReal(),     real.getVelocity().getZ().getReal(),     epsilon);
        Assertions.assertEquals(expected.getAcceleration().getX().getReal(), real.getAcceleration().getX().getReal(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getY().getReal(), real.getAcceleration().getY().getReal(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getZ().getReal(), real.getAcceleration().getZ().getReal(), epsilon);
    }

}
