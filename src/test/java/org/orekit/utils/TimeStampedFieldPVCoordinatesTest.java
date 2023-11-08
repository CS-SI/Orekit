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
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;


public class TimeStampedFieldPVCoordinatesTest {

    @Test
    public void testLinearConstructors() {
        DSFactory factory = new DSFactory(6, 1);
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv1 =
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.CCSDS_EPOCH,
                                                    createVector( 1,  0.1, 10, 6),
                                                    createVector(-1, -0.1, -10, 6),
                                                    createVector(10,  1.0, 100, 6));
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv2 =
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.FIFTIES_EPOCH,
                                                    createVector( 2,  0.2,  20, 6),
                                                    createVector(-2, -0.2, -20, 6),
                                                    createVector(20,  2.0, 200, 6));
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv3 =
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.GALILEO_EPOCH,
                                                    createVector( 3,  0.3,  30, 6),
                                                    createVector(-3, -0.3, -30, 6),
                                                    createVector(30,  3.0, 300, 6));
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv4 =
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.JULIAN_EPOCH,
                                                    createVector( 4,  0.4,  40, 6),
                                                    createVector(-4, -0.4, -40, 6),
                                                    createVector(40,  4.0, 400, 6));
        checkPV(pv4, new TimeStampedFieldPVCoordinates<>(AbsoluteDate.JULIAN_EPOCH, 4, pv1), 1.0e-15);
        checkPV(pv4, new TimeStampedFieldPVCoordinates<>(AbsoluteDate.JULIAN_EPOCH, factory.constant(4), pv1), 1.0e-15);
        checkPV(pv4, new TimeStampedFieldPVCoordinates<>(AbsoluteDate.JULIAN_EPOCH, factory.constant(4), pv1.toPVCoordinates()), 1.0e-15);
        checkPV(pv2, new TimeStampedFieldPVCoordinates<>(AbsoluteDate.FIFTIES_EPOCH, pv1, pv3), 1.0e-15);
        checkPV(pv3, new TimeStampedFieldPVCoordinates<>(AbsoluteDate.GALILEO_EPOCH, 1, pv1, 1, pv2), 1.0e-15);
        checkPV(pv3,
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.GALILEO_EPOCH,
                                                    factory.constant(1), pv1,
                                                    factory.constant(1), pv2),
                1.0e-15);
        checkPV(pv3,
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.GALILEO_EPOCH,
                                                    factory.constant(1), pv1.toPVCoordinates(),
                                                    factory.constant(1), pv2.toPVCoordinates()),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 2, pv4),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv3),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv4),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                    factory.constant(3), pv1,
                                                    factory.constant(1), pv2,
                                                    factory.constant(1), pv4),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 3, pv3),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                    factory.constant(3), pv1.toPVCoordinates(),
                                                    factory.constant(1), pv2.toPVCoordinates(),
                                                    factory.constant(1), pv4.toPVCoordinates()),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                    factory.constant(4), pv1,
                                                    factory.constant(3), pv2,
                                                    factory.constant(2), pv3,
                                                    factory.constant(1), pv4),
                1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, 5, pv4),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                    factory.constant(4), pv1.toPVCoordinates(),
                                                    factory.constant(3), pv2.toPVCoordinates(),
                                                    factory.constant(2), pv3.toPVCoordinates(),
                                                    factory.constant(1), pv4.toPVCoordinates()),
                1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector1() {
        FieldVector3D<FieldDerivativeStructure<Binary64>> fv =
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                toDerivativeStructureVector(1);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).doubleValue(), 1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    FieldVector3D.getZero(Binary64Field.getInstance())),
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            FieldVector3D<Binary64> p = new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                                  new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10))).
                            shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX().doubleValue(), fv.getX().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getY().doubleValue(), fv.getY().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getZ().doubleValue(), fv.getZ().taylor(dt).doubleValue(), 1.0e-14);
        }

        TimeStampedFieldPVCoordinates<Binary64> fpv =
                        new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                            fv);
        Assertions.assertEquals(   1.0, fpv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fpv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fpv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fpv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fpv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fpv.getVelocity().getZ().getReal(), 1.0e-15);

    }

    @Test
    public void testToDerivativeStructureVector2() {
        FieldVector3D<FieldDerivativeStructure<Binary64>> fv =
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                toDerivativeStructureVector(2);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getPartialDerivative(2).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getPartialDerivative(2).doubleValue(), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getPartialDerivative(2).doubleValue(), 1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))),
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            FieldVector3D<Binary64> p = new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                                  new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                                  new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                            shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX().doubleValue(), fv.getX().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getY().doubleValue(), fv.getY().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getZ().doubleValue(), fv.getZ().taylor(dt).doubleValue(), 1.0e-14);
        }

        TimeStampedFieldPVCoordinates<Binary64> fpv =
                        new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                            fv);
        Assertions.assertEquals(   1.0, fpv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fpv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fpv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fpv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fpv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fpv.getVelocity().getZ().getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, fpv.getAcceleration().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fpv.getAcceleration().getY().getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, fpv.getAcceleration().getZ().getReal(), 1.0e-15);

    }

    @Test
    public void testToUnivariateDerivative1Vector() {
        FieldVector3D<FieldUnivariateDerivative1<Binary64>> fv =
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                toUnivariateDerivative1Vector();
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).doubleValue(), 1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    FieldVector3D.getZero(Binary64Field.getInstance())),
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            FieldVector3D<Binary64> p = new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                                  new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10))).
                            shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX().doubleValue(), fv.getX().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getY().doubleValue(), fv.getY().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getZ().doubleValue(), fv.getZ().taylor(dt).doubleValue(), 1.0e-14);
        }

        TimeStampedFieldPVCoordinates<Binary64> fpv =
                        new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                            fv);
        Assertions.assertEquals(   1.0, fpv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fpv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fpv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fpv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fpv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fpv.getVelocity().getZ().getReal(), 1.0e-15);

    }

    @Test
    public void testToUnivariateDerivative2Vector() {
        FieldVector3D<FieldUnivariateDerivative2<Binary64>> fv =
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                toUnivariateDerivative2Vector();
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getPartialDerivative(2).doubleValue(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getPartialDerivative(2).doubleValue(), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getPartialDerivative(2).doubleValue(), 1.0e-15);
        checkPV(new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                    new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                    new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                    new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))),
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            FieldVector3D<Binary64> p = new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                                  new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                                  new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                            shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX().doubleValue(), fv.getX().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getY().doubleValue(), fv.getY().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getZ().doubleValue(), fv.getZ().taylor(dt).doubleValue(), 1.0e-14);
        }

        TimeStampedFieldPVCoordinates<Binary64> fpv =
                        new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getGalileoEpoch(Binary64Field.getInstance()),
                                                            fv);
        Assertions.assertEquals(   1.0, fpv.getPosition().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fpv.getPosition().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fpv.getPosition().getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fpv.getVelocity().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fpv.getVelocity().getY().getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fpv.getVelocity().getZ().getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, fpv.getAcceleration().getX().getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fpv.getAcceleration().getY().getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, fpv.getAcceleration().getZ().getReal(), 1.0e-15);

    }

    @Test
    public void testShift() {
        FieldVector3D<DerivativeStructure> p1 = createVector(  1,  0.1,   10, 4);
        FieldVector3D<DerivativeStructure> v1 = createVector( -1, -0.1,  -10, 4);
        FieldVector3D<DerivativeStructure> a1 = createVector( 10,  1.0,  100, 4);
        FieldVector3D<DerivativeStructure> p2 = createVector(  7,  0.7,   70, 4);
        FieldVector3D<DerivativeStructure> v2 = createVector(-11, -1.1, -110, 4);
        FieldVector3D<DerivativeStructure> a2 = createVector( 10,  1.0,  100, 4);
        checkPV(new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH, p2, v2, a2),
                new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH.shiftedBy(1.0), p1, v1, a1).shiftedBy(-1.0), 1.0e-15);
        Assertions.assertEquals(0.0,
                            TimeStampedFieldPVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(createVector(-6, -0.6, -60, 4)).getNorm().getReal(),
                            1.0e-15);
    }

    @Test
    public void testToString() {
        Utils.setDataRoot("regular-data");
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv =
            new TimeStampedFieldPVCoordinates<>(AbsoluteDate.J2000_EPOCH,
                                                createVector( 1,  0.1,  10, 4),
                                                createVector(-1, -0.1, -10, 4),
                                                createVector(10,  1.0, 100, 4));
        Assertions.assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    public void testIssue510() {
        DSFactory factory = new DSFactory(1, 1);
        TimeStampedFieldPVCoordinates<DerivativeStructure> pv =
                        new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(factory.getDerivativeField()),
                                                            new FieldVector3D<>(factory.constant(10.0),
                                                                                factory.constant(20.0),
                                                                                factory.constant(30.0)),
                                                            new FieldVector3D<>(factory.constant(1.0),
                                                                                factory.constant(2.0),
                                                                                factory.constant(3.0)),
                                                            FieldVector3D.getZero(factory.getDerivativeField()));
        DerivativeStructure dt = factory.variable(0, 1.0);
        TimeStampedFieldPVCoordinates<DerivativeStructure> shifted = pv.shiftedBy(dt);
        Assertions.assertEquals(1.0, shifted.getDate().durationFrom(pv.getDate()).getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(pv.getVelocity().getX().getValue(), shifted.getPosition().getX().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(pv.getVelocity().getY().getValue(), shifted.getPosition().getY().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(pv.getVelocity().getZ().getValue(), shifted.getPosition().getZ().getPartialDerivative(1), 1.0e-15);

    }

    @Test
    void testFromTimeStampedPVCoordinates() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final Vector3D position = Vector3D.MINUS_I;
        final Vector3D velocity = Vector3D.PLUS_K;
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeStampedPVCoordinates expectedPV = new TimeStampedPVCoordinates(date, position, velocity);
        // WHEN
        final TimeStampedFieldPVCoordinates<Complex> fieldPV = new TimeStampedFieldPVCoordinates<>(field, expectedPV);
        // THEN
        Assertions.assertEquals(expectedPV.getDate(), fieldPV.getDate().toAbsoluteDate());
        Assertions.assertEquals(expectedPV.getPosition(), fieldPV.getPosition().toVector3D());
        Assertions.assertEquals(expectedPV.getVelocity(), fieldPV.getVelocity().toVector3D());
        Assertions.assertEquals(expectedPV.getAcceleration(), fieldPV.getAcceleration().toVector3D());
    }

    @Test
    public void testIssue774() {
        doTestIssue774(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue774(final Field<T> field) {

        final T zero = field.getZero();

        // Epoch
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // Coordinates
        final FieldPVCoordinates<T> pv =
                        new FieldPVCoordinates<T>(new FieldVector3D<T>(zero, zero, zero),
                                                  new FieldVector3D<T>(zero, zero, zero));

        // Time stamped object
        final FieldTimeStamped<T> timeStamped =
                        new TimeStampedFieldPVCoordinates<>(date, pv);

        // Verify
        Assertions.assertEquals(0.0, date.durationFrom(timeStamped.getDate()).getReal(), Double.MIN_VALUE);
    }

    private <T extends CalculusFieldElement<T>> void checkPV(TimeStampedFieldPVCoordinates<T> expected,
                                                         TimeStampedFieldPVCoordinates<T> real, double epsilon) {
        Assertions.assertEquals(expected.getDate(), real.getDate());
        Assertions.assertEquals(expected.getPosition().getX().getReal(),     real.getPosition().getX().getReal(), epsilon);
        Assertions.assertEquals(expected.getPosition().getY().getReal(),     real.getPosition().getY().getReal(), epsilon);
        Assertions.assertEquals(expected.getPosition().getZ().getReal(),     real.getPosition().getZ().getReal(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getX().getReal(),     real.getVelocity().getX().getReal(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getY().getReal(),     real.getVelocity().getY().getReal(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getZ().getReal(),     real.getVelocity().getZ().getReal(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getX().getReal(), real.getAcceleration().getX().getReal(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getY().getReal(), real.getAcceleration().getY().getReal(), epsilon);
        Assertions.assertEquals(expected.getAcceleration().getZ().getReal(), real.getAcceleration().getZ().getReal(), epsilon);
    }

    public static FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        DSFactory factory = new DSFactory(params, 1);
        return new FieldVector3D<>(factory.variable(0, x),
                                   factory.variable(1, y),
                                   factory.variable(2, z));
    }

}
