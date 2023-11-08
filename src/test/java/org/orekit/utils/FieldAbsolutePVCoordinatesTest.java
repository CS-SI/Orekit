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
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.FieldAbsoluteDate;

public class FieldAbsolutePVCoordinatesTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void TestPVOnlyConstructor() {
        doTestPVOnlyConstructor(Binary64Field.getInstance());
    }

    @Test
    public void testPVCoordinatesCopyConstructor() {
        doTestPVCoordinatesCopyConstructor(Binary64Field.getInstance());
    }

    @Test
    public void testLinearConstructors() {
        doTestLinearConstructors(Binary64Field.getInstance());
    }

    @Test
    public void testDifferentFrames() {
        doTestDifferentFrames(Binary64Field.getInstance());
    }

    @Test
    public void testToDerivativeStructureVector1() {
        doTestToDerivativeStructureVector1(Binary64Field.getInstance());
    }

    @Test
    public void testToDerivativeStructureVector2() {
        doTestToDerivativeStructureVector2(Binary64Field.getInstance());
    }

    @Test
    public void testToUnivariateDerivative1Vector() {
        doTestToUnivariateDerivative1Vector(Binary64Field.getInstance());
    }

    @Test
    public void testToUnivariateDerivative2Vector() {
        doTestToUnivariateDerivative2Vector(Binary64Field.getInstance());
    }

    @Test
    public void testShift() {
        doTestShift(Binary64Field.getInstance());
    }

    @Test
    public void testToString() {
        doTestToString(Binary64Field.getInstance());
    }

    @Test
    public void testSamePV() {
        doTestSamePV(Binary64Field.getInstance());
    }

    @Test
    public void testTaylorProvider() {
        doTestTaylorProvider(Binary64Field.getInstance());
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
        checkPV(pv4, new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJulianEpoch(field), one.multiply(4.0), pv1), 1.0e-15);
        checkPV(pv2, new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getFiftiesEpoch(field), pv1, pv3), 1.0e-15);
        checkPV(pv3, new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getGalileoEpoch(field), one, pv1, one, pv2), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(2.0), pv4),
                new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv1, one, pv2, one, pv3),
                1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv3),
                new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv1, one, pv2, one, pv4),
                1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(5.0), pv4),
                new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(4.0), pv1, one.multiply(3.0), pv2, one.multiply(2.0), pv3, one, pv4),
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

    private <T extends CalculusFieldElement<T>> void doTestToDerivativeStructureVector1(Field<T> field) {
        final T one = field.getOne();
        FieldVector3D<FieldDerivativeStructure<T>> fv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                        new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))).toDerivativeStructureVector(1);

        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).getReal(), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                        FieldAbsoluteDate.getGalileoEpoch(field),
                        new FieldVector3D<>(one,  one.multiply(0.1), one.multiply(10.0)),
                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                        FieldVector3D.getZero(field)),
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                FieldAbsoluteDate.getGalileoEpoch(field), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getX().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getY().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getZ().taylor(dt).getReal(), 1.0e-14);
        }

        FieldAbsolutePVCoordinates<T> pv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ().getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestToDerivativeStructureVector2(Field<T> field) {
        final T one = field.getOne();
        FieldVector3D<FieldDerivativeStructure<T>> fv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                        new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))).toDerivativeStructureVector(2);

        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getPartialDerivative(2).getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getPartialDerivative(2).getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getPartialDerivative(2).getReal(), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                        FieldAbsoluteDate.getGalileoEpoch(field),
                        new FieldVector3D<>(one,  one.multiply(0.1), one.multiply(10.0)),
                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                        new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))),
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                FieldAbsoluteDate.getGalileoEpoch(field), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getX().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getY().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getZ().taylor(dt).getReal(), 1.0e-14);
        }

        FieldAbsolutePVCoordinates<T> pv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ().getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, pv.getAcceleration().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, pv.getAcceleration().getY().getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, pv.getAcceleration().getZ().getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestToUnivariateDerivative1Vector(Field<T> field) {
        final T one = field.getOne();
        FieldVector3D<FieldUnivariateDerivative1<T>> fv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                        new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))).toUnivariateDerivative1Vector();

        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).getReal(), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                        FieldAbsoluteDate.getGalileoEpoch(field),
                        new FieldVector3D<>(one,  one.multiply(0.1), one.multiply(10.0)),
                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                        FieldVector3D.getZero(field)),
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                FieldAbsoluteDate.getGalileoEpoch(field), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getX().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getY().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getZ().taylor(dt).getReal(), 1.0e-14);
        }

        FieldAbsolutePVCoordinates<T> pv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ().getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestToUnivariateDerivative2Vector(Field<T> field) {
        final T one = field.getOne();
        FieldVector3D<FieldUnivariateDerivative2<T>> fv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                        new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))).toUnivariateDerivative2Vector();

        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getPartialDerivative(2).getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getPartialDerivative(2).getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getPartialDerivative(2).getReal(), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                        FieldAbsoluteDate.getGalileoEpoch(field),
                        new FieldVector3D<>(one,  one.multiply(0.1), one.multiply(10.0)),
                        new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                        new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))),
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                FieldAbsoluteDate.getGalileoEpoch(field), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getX().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getY().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getZ().taylor(dt).getReal(), 1.0e-14);
        }

        FieldAbsolutePVCoordinates<T> pv =
                        new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                        FieldAbsoluteDate.getGalileoEpoch(field),
                                        fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ().getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, pv.getAcceleration().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, pv.getAcceleration().getY().getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, pv.getAcceleration().getZ().getReal(), 1.0e-15);

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
        Assertions.assertEquals(0.0, FieldAbsolutePVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(new Vector3D(-6, -0.6, -60)).getNorm().getReal(), 1.0e-15);
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
