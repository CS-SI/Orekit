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
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.time.FieldAbsoluteDate;


public class FieldPVCoordinatesTest {

    @Test
    public void testLinearConstructors() {
        DSFactory factory = new DSFactory(6, 1);
        FieldPVCoordinates<DerivativeStructure> pv1 = new FieldPVCoordinates<>(createVector(1, 0.1, 10, 6),
                                                                               createVector(-1, -0.1, -10, 6));
        FieldPVCoordinates<DerivativeStructure> pv2 = new FieldPVCoordinates<>(createVector(2, 0.2, 20, 6),
                                                                               createVector(-2, -0.2, -20, 6));
        FieldPVCoordinates<DerivativeStructure> pv3 = new FieldPVCoordinates<>(createVector(3, 0.3, 30, 6),
                                                                               createVector(-3, -0.3, -30, 6));
        FieldPVCoordinates<DerivativeStructure> pv4 = new FieldPVCoordinates<>(createVector(4, 0.4, 40, 6),
                                                                               createVector(-4, -0.4, -40, 6));
        checkPV(pv4, new FieldPVCoordinates<>(4, pv1), 1.0e-15);
        checkPV(pv4, new FieldPVCoordinates<>(factory.constant(4), pv1), 1.0e-15);
        checkPV(pv4, new FieldPVCoordinates<>(factory.constant(4), pv1.toPVCoordinates()), 1.0e-15);
        checkPV(pv2, new FieldPVCoordinates<>(pv1, pv3), 1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<>(1, pv1, 1, pv2), 1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<>(factory.constant(1), pv1,
                                              factory.constant(1), pv2),
                1.0e-15);
        checkPV(pv3, new FieldPVCoordinates<>(factory.constant(1), pv1.toPVCoordinates(),
                                              factory.constant(1), pv2.toPVCoordinates()),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(2, pv4), new FieldPVCoordinates<>(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(3, pv3), new FieldPVCoordinates<>(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(3, pv3),
                new FieldPVCoordinates<>(factory.constant(3), pv1,
                                         factory.constant(1), pv2,
                                         factory.constant(1), pv4),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(3, pv3),
                new FieldPVCoordinates<>(factory.constant(3), pv1.toPVCoordinates(),
                                         factory.constant(1), pv2.toPVCoordinates(),
                                         factory.constant(1), pv4.toPVCoordinates()),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(5, pv4),
                new FieldPVCoordinates<>(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(5, pv4),
                new FieldPVCoordinates<>(factory.constant(4), pv1,
                                         factory.constant(3), pv2,
                                         factory.constant(2), pv3,
                                         factory.constant(1), pv4),
                1.0e-15);
        checkPV(new FieldPVCoordinates<>(5, pv4),
                new FieldPVCoordinates<>(factory.constant(4), pv1.toPVCoordinates(),
                                         factory.constant(3), pv2.toPVCoordinates(),
                                         factory.constant(2), pv3.toPVCoordinates(),
                                         factory.constant(1), pv4.toPVCoordinates()),
                1.0e-15);
    }

    @Test
    public void testConversionConstructor() {
        PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6), new Vector3D(7, 8, 9));
        FieldPVCoordinates<Binary64> pv64 = new FieldPVCoordinates<>(Binary64Field.getInstance(), pv);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(pv.getPosition(), pv64.getPosition().toVector3D()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(pv.getVelocity(), pv64.getVelocity().toVector3D()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(pv.getAcceleration(), pv64.getAcceleration().toVector3D()),
                            1.0e-15);
    }

    @Test
    public void testZero() {
        Assertions.assertEquals(0.0,
                            FieldPVCoordinates.getZero(Binary64Field.getInstance()).getPosition().getNorm().getReal(),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            FieldPVCoordinates.getZero(Binary64Field.getInstance()).getVelocity().getNorm().getReal(),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            FieldPVCoordinates.getZero(Binary64Field.getInstance()).getAcceleration().getNorm().getReal(),
                            1.0e-15);
    }

    @Test
    public void testGetMomentum() {
        //setup
        DSFactory factory = new DSFactory(1, 1);
        DerivativeStructure oneDS = factory.getDerivativeField().getOne();
        DerivativeStructure zeroDS = factory.getDerivativeField().getZero();
        FieldVector3D<DerivativeStructure> zero = new FieldVector3D<>(zeroDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> i = new FieldVector3D<>(oneDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> j = new FieldVector3D<>(zeroDS, oneDS, zeroDS);
        FieldVector3D<DerivativeStructure> k = new FieldVector3D<>(zeroDS, zeroDS, oneDS);
        FieldVector3D<DerivativeStructure> p = new FieldVector3D<>(oneDS,
                                                                   factory.constant(-2),
                                                                   factory.constant(3));
        FieldVector3D<DerivativeStructure> v = new FieldVector3D<>(factory.constant(-9),
                                                                   factory.constant(8),
                                                                   factory.constant(-7));

        //action + verify
        Assertions.assertEquals(
                new FieldPVCoordinates<>(p, v).getMomentum(),
                p.crossProduct(v));
        //check simple cases
        Assertions.assertEquals(
                new FieldPVCoordinates<>(i, i.scalarMultiply(-1)).getMomentum(),
                zero);
        Assertions.assertEquals(
                new FieldPVCoordinates<>(i, j).getMomentum(),
                k);
    }

    @Test
    public void testGetAngularVelocity() {
        //setup
        DSFactory factory = new DSFactory(1, 1);
        DerivativeStructure oneDS = factory.getDerivativeField().getOne();
        DerivativeStructure zeroDS = factory.getDerivativeField().getZero();
        FieldVector3D<DerivativeStructure> zero = new FieldVector3D<>(zeroDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> i = new FieldVector3D<>(oneDS, zeroDS, zeroDS);
        FieldVector3D<DerivativeStructure> j = new FieldVector3D<>(zeroDS, oneDS, zeroDS);
        FieldVector3D<DerivativeStructure> k = new FieldVector3D<>(zeroDS, zeroDS, oneDS);
        FieldVector3D<DerivativeStructure> p = new FieldVector3D<>(oneDS,
                                                                   factory.constant(-2),
                                                                   factory.constant(3));
        FieldVector3D<DerivativeStructure> v = new FieldVector3D<>(factory.constant(-9),
                                                                   factory.constant(8),
                                                                   factory.constant(-7));

        //action + verify
        Assertions.assertEquals(
                new FieldPVCoordinates<>(p, v).getAngularVelocity(),
                p.crossProduct(v).scalarMultiply(p.getNormSq().reciprocal()));
        //check extra simple cases
        Assertions.assertEquals(
                new FieldPVCoordinates<>(i, i.scalarMultiply(-1)).getAngularVelocity(),
                zero);
        Assertions.assertEquals(
                new FieldPVCoordinates<>(i.scalarMultiply(2), j).getAngularVelocity(),
                k.scalarMultiply(0.5));
    }

    @Test
    public void testToDerivativeStructureVectorNeg() {
        try {
            FieldPVCoordinates.getZero(Binary64Field.getInstance()).toDerivativeStructureVector(-1);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector3() {
        try {
            FieldPVCoordinates.getZero(Binary64Field.getInstance()).toDerivativeStructureVector(3);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector0() {
        FieldVector3D<FieldDerivativeStructure<Binary64>> fv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                         new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                         new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                         toDerivativeStructureVector(0);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(0, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        checkPV(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(1), new Binary64(0.1), new Binary64(10)),
                                         FieldVector3D.getZero(Binary64Field.getInstance()),
                                         FieldVector3D.getZero(Binary64Field.getInstance())),
                new FieldPVCoordinates<>(fv), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector1() {
        FieldVector3D<FieldDerivativeStructure<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toDerivativeStructureVector(1);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).getReal(), 1.0e-15);
        checkPV(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(1), new Binary64(0.1), new Binary64(10)),
                                         new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                         FieldVector3D.getZero(Binary64Field.getInstance())),
                new FieldPVCoordinates<>(fv), 1.0e-15);
    }

    @Test
    public void testUnivariateDerivative1Vector() {
        FieldVector3D<FieldUnivariateDerivative1<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toUnivariateDerivative1Vector();
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getDerivative(1).getReal(), 1.0e-15);

        FieldPVCoordinates<Binary64> fpv = new FieldPVCoordinates<>(fv);
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
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toDerivativeStructureVector(2);
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
        checkPV(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(1), new Binary64(0.1), new Binary64(10)),
                                         new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                         new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))),
                new FieldPVCoordinates<>(fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            FieldVector3D<Binary64> p = new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                                  new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                                  new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                         shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX().doubleValue(), fv.getX().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getY().doubleValue(), fv.getY().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getZ().doubleValue(), fv.getZ().taylor(dt).doubleValue(), 1.0e-14);
        }

        FieldPVCoordinates<Binary64> fpv = new FieldPVCoordinates<>(fv);
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
    public void testUnivariateDerivative2Vector() {
        FieldVector3D<FieldUnivariateDerivative2<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1),  new Binary64(0.1),  new Binary64(10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).toUnivariateDerivative2Vector();
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getDerivative(2).getReal(), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getDerivative(2).getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getDerivative(2).getReal(), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            FieldVector3D<Binary64> p = new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1),  new Binary64(0.1),  new Binary64(10)),
                                                                  new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                                  new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX().doubleValue(), fv.getX().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getY().doubleValue(), fv.getY().taylor(dt).doubleValue(), 1.0e-14);
            Assertions.assertEquals(p.getZ().doubleValue(), fv.getZ().taylor(dt).doubleValue(), 1.0e-14);
        }

        FieldPVCoordinates<Binary64> fpv = new FieldPVCoordinates<>(fv);
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
    public void testToDerivativeStructurePVNeg() {
        try {
            FieldPVCoordinates.getZero(Binary64Field.getInstance()).toDerivativeStructurePV(-1);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructurePV3() {
        try {
            FieldPVCoordinates.getZero(Binary64Field.getInstance()).toDerivativeStructurePV(3);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructurePV0() {
        FieldPVCoordinates<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructurePV(0);
        Assertions.assertEquals(1, fv.getPosition().getX().getFreeParameters());
        Assertions.assertEquals(0, fv.getPosition().getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getPosition().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(   0.1, fv.getPosition().getY().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getPosition().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getVelocity().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(  -0.1, fv.getVelocity().getY().getReal(),     1.0e-10);
        Assertions.assertEquals( -10.0, fv.getVelocity().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getAcceleration().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getAcceleration().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(-100.0, fv.getAcceleration().getZ().getReal(), 1.0e-10);
    }

    @Test
    public void testToDerivativeStructurePV1() {
        FieldPVCoordinates<FieldDerivativeStructure<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toDerivativeStructurePV(1);
        Assertions.assertEquals(1, fv.getPosition().getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getPosition().getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getPosition().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(   0.1, fv.getPosition().getY().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getPosition().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getVelocity().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(  -0.1, fv.getVelocity().getY().getReal(),     1.0e-10);
        Assertions.assertEquals( -10.0, fv.getVelocity().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getAcceleration().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getAcceleration().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(-100.0, fv.getAcceleration().getZ().getReal(), 1.0e-10);

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),     fv.getPosition().getX().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),     fv.getPosition().getY().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),     fv.getPosition().getZ().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(), fv.getVelocity().getX().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(), fv.getVelocity().getY().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(), fv.getVelocity().getZ().getPartialDerivative(1).getReal(), 1.0e-10);

    }


    @Test
    public void testToUnivariateDerivative1PV() {
        FieldPVCoordinates<FieldUnivariateDerivative1<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toUnivariateDerivative1PV();
        Assertions.assertEquals(1, fv.getPosition().getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getPosition().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(   0.1, fv.getPosition().getY().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getPosition().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getVelocity().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(  -0.1, fv.getVelocity().getY().getReal(),     1.0e-10);
        Assertions.assertEquals( -10.0, fv.getVelocity().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getAcceleration().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getAcceleration().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(-100.0, fv.getAcceleration().getZ().getReal(), 1.0e-10);

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),     fv.getPosition().getX().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),     fv.getPosition().getY().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),     fv.getPosition().getZ().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(), fv.getVelocity().getX().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(), fv.getVelocity().getY().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(), fv.getVelocity().getZ().getDerivative(1).getReal(), 1.0e-10);
    }

    @Test
    public void testToDerivativeStructurePV2() {
        FieldPVCoordinates<FieldDerivativeStructure<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toDerivativeStructurePV(2);
        Assertions.assertEquals(1, fv.getPosition().getX().getFreeParameters());
        Assertions.assertEquals(2, fv.getPosition().getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getPosition().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(   0.1, fv.getPosition().getY().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getPosition().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getVelocity().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(  -0.1, fv.getVelocity().getY().getReal(),     1.0e-10);
        Assertions.assertEquals( -10.0, fv.getVelocity().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getAcceleration().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getAcceleration().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(-100.0, fv.getAcceleration().getZ().getReal(), 1.0e-10);

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),                             fv.getPosition().getX().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),                             fv.getPosition().getY().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),                             fv.getPosition().getZ().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),                         fv.getPosition().getX().getPartialDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),                         fv.getPosition().getY().getPartialDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),                         fv.getPosition().getZ().getPartialDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),                         fv.getVelocity().getX().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),                         fv.getVelocity().getY().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),                         fv.getVelocity().getZ().getPartialDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getPartialDerivative(1).getReal(), fv.getVelocity().getX().getPartialDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getPartialDerivative(1).getReal(), fv.getVelocity().getY().getPartialDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getPartialDerivative(1).getReal(), fv.getVelocity().getZ().getPartialDerivative(2).getReal(), 1.0e-10);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getPosition().getX().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getPosition().getY().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getPosition().getZ().taylor(dt).getReal(), 1.0e-14);
        }

    }

    @Test
    public void testToUnivariateDerivative2PV() {
        FieldPVCoordinates<FieldUnivariateDerivative2<Binary64>> fv =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64( 1), new Binary64( 0.1), new Binary64( 10)),
                                                 new FieldVector3D<>(new Binary64(-1), new Binary64(-0.1), new Binary64(-10)),
                                                 new FieldVector3D<>(new Binary64(10), new Binary64(-1.0), new Binary64(-100))).
                                                 toUnivariateDerivative2PV();
        Assertions.assertEquals(2, fv.getPosition().getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getPosition().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(   0.1, fv.getPosition().getY().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getPosition().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getVelocity().getX().getReal(),     1.0e-10);
        Assertions.assertEquals(  -0.1, fv.getVelocity().getY().getReal(),     1.0e-10);
        Assertions.assertEquals( -10.0, fv.getVelocity().getZ().getReal(),     1.0e-10);
        Assertions.assertEquals(  10.0, fv.getAcceleration().getX().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getAcceleration().getY().getReal(), 1.0e-10);
        Assertions.assertEquals(-100.0, fv.getAcceleration().getZ().getReal(), 1.0e-10);

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),                      fv.getPosition().getX().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),                      fv.getPosition().getY().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),                      fv.getPosition().getZ().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),                  fv.getPosition().getX().getDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),                  fv.getPosition().getY().getDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),                  fv.getPosition().getZ().getDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),                  fv.getVelocity().getX().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),                  fv.getVelocity().getY().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),                  fv.getVelocity().getZ().getDerivative(1).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getDerivative(1).getReal(), fv.getVelocity().getX().getDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getDerivative(1).getReal(), fv.getVelocity().getY().getDerivative(2).getReal(), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getDerivative(1).getReal(), fv.getVelocity().getZ().getDerivative(2).getReal(), 1.0e-10);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getPosition().getX().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getPosition().getY().taylor(dt).getReal(), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getPosition().getZ().taylor(dt).getReal(), 1.0e-14);
        }
    }

    @Test
    public void testJerkIsVelocitySecondDerivative() {
        final FieldCartesianOrbit<Binary64> orbit =
                        new FieldCartesianOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(-4947831.), new Binary64(-3765382.), new Binary64(-3708221.)),
                                                                           new FieldVector3D<>(new Binary64(-2079.), new Binary64(5291.), new Binary64(-7842.))),
                                                        FramesFactory.getEME2000(),
                                                        FieldAbsoluteDate.getJ2000Epoch(Binary64Field.getInstance()),
                                                        new Binary64(Constants.EIGEN5C_EARTH_MU));
        FieldPVCoordinates<FieldDerivativeStructure<Binary64>> fv = orbit.getPVCoordinates().toDerivativeStructurePV(2);
        FieldVector3D<Binary64> numericalJerk = differentiate(orbit, o -> o.getPVCoordinates().getAcceleration());
        Assertions.assertEquals(numericalJerk.getX().getReal(),
                            fv.getVelocity().getX().getPartialDerivative(2).getReal(),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getY().getReal(),
                            fv.getVelocity().getY().getPartialDerivative(2).getReal(),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getZ().getReal(),
                            fv.getVelocity().getZ().getPartialDerivative(2).getReal(),
                            3.0e-13);

    }

    @Test
    public void testJerkIsAccelerationDerivative() {
        final FieldCartesianOrbit<Binary64> orbit =
                        new FieldCartesianOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(-4947831.), new Binary64(-3765382.), new Binary64(-3708221.)),
                                                                           new FieldVector3D<>(new Binary64(-2079.), new Binary64(5291.), new Binary64(-7842.))),
                                                        FramesFactory.getEME2000(),
                                                        FieldAbsoluteDate.getJ2000Epoch(Binary64Field.getInstance()),
                                                        new Binary64(Constants.EIGEN5C_EARTH_MU));
        FieldPVCoordinates<FieldDerivativeStructure<Binary64>> fv1 = orbit.getPVCoordinates().toDerivativeStructurePV(1);
        FieldVector3D<Binary64> numericalJerk = differentiate(orbit, o -> o.getPVCoordinates().getAcceleration());
        Assertions.assertEquals(numericalJerk.getX().getReal(),
                            fv1.getAcceleration().getX().getPartialDerivative(1).getReal(),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getY().getReal(),
                            fv1.getAcceleration().getY().getPartialDerivative(1).getReal(),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getZ().getReal(),
                            fv1.getAcceleration().getZ().getPartialDerivative(1).getReal(),
                            3.0e-13);

        FieldPVCoordinates<FieldDerivativeStructure<Binary64>> fv2 = orbit.getPVCoordinates().toDerivativeStructurePV(2);
        Assertions.assertEquals(numericalJerk.getX().getReal(),
                            fv2.getAcceleration().getX().getPartialDerivative(1).getReal(),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getY().getReal(),
                            fv2.getAcceleration().getY().getPartialDerivative(1).getReal(),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getZ().getReal(),
                            fv2.getAcceleration().getZ().getPartialDerivative(1).getReal(),
                            3.0e-13);

    }

    @Test
    public void testJounceIsAccelerationSecondDerivative() {
        final FieldCartesianOrbit<Binary64> orbit =
                        new FieldCartesianOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(-4947831.), new Binary64(-3765382.), new Binary64(-3708221.)),
                                                                           new FieldVector3D<>(new Binary64(-2079.), new Binary64(5291.), new Binary64(-7842.))),
                                                        FramesFactory.getEME2000(),
                                                        FieldAbsoluteDate.getJ2000Epoch(Binary64Field.getInstance()),
                                                        new Binary64(Constants.EIGEN5C_EARTH_MU));
        FieldPVCoordinates<FieldDerivativeStructure<Binary64>> fv = orbit.getPVCoordinates().toDerivativeStructurePV(2);
        FieldVector3D<Binary64> numericalJounce = differentiate(orbit, o -> {
        FieldVector3D<FieldDerivativeStructure<Binary64>> a = o.getPVCoordinates().toDerivativeStructurePV(1).getAcceleration();
            return new FieldVector3D<>(a.getX().getPartialDerivative(1),
                                       a.getY().getPartialDerivative(1),
                                       a.getZ().getPartialDerivative(1));
        });
        Assertions.assertEquals(numericalJounce.getX().getReal(),
                            fv.getAcceleration().getX().getPartialDerivative(2).getReal(),
                            1.0e-15);
        Assertions.assertEquals(numericalJounce.getY().getReal(),
                            fv.getAcceleration().getY().getPartialDerivative(2).getReal(),
                            1.0e-15);
        Assertions.assertEquals(numericalJounce.getZ().getReal(),
                            fv.getAcceleration().getZ().getPartialDerivative(2).getReal(),
                            1.0e-15);

    }

    @Test
    public void testMomentumDerivative() {
        final FieldPVCoordinates<Binary64> pva =
                        new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(-4947831.), new Binary64(-3765382.), new Binary64(-3708221.)),
                                                 new FieldVector3D<>(new Binary64(-2079.), new Binary64(5291.), new Binary64(-7842.)));
        final FieldVector3D<Binary64> p  = pva.getPosition();
        final FieldVector3D<Binary64>  v  = pva.getVelocity();
        final FieldVector3D<Binary64>  a  = pva.getAcceleration();
        final Binary64   r2 = p.getNormSq();
        final Binary64   r  = r2.sqrt();
        final FieldVector3D<Binary64>  keplerianJerk = new FieldVector3D<>(FieldVector3D.dotProduct(p, v).multiply(-2).divide(r2), a,
                                                                            a.getNorm().negate().divide(r), v);
        final FieldPVCoordinates<Binary64> velocity = new FieldPVCoordinates<>(v, a, keplerianJerk);
        final FieldVector3D<Binary64>  momentumRef    = pva.getMomentum();
        final FieldVector3D<Binary64>  momentumDotRef = pva.crossProduct(velocity).getVelocity();

        final FieldVector3D<FieldDerivativeStructure<Binary64>> momentumDot = pva.toDerivativeStructurePV(1).getMomentum();
        Assertions.assertEquals(momentumRef.getX().getReal(),    momentumDot.getX().getReal(),                         1.0e-15);
        Assertions.assertEquals(momentumRef.getY().getReal(),    momentumDot.getY().getReal(),                         1.0e-15);
        Assertions.assertEquals(momentumRef.getZ().getReal(),    momentumDot.getZ().getReal(),                         1.0e-15);
        Assertions.assertEquals(momentumDotRef.getX().getReal(), momentumDot.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(momentumDotRef.getY().getReal(), momentumDot.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assertions.assertEquals(momentumDotRef.getZ().getReal(), momentumDot.getZ().getPartialDerivative(1).getReal(), 1.0e-15);

    }

    @Test
    public void testShift() {
        FieldVector3D<DerivativeStructure> p1 = createVector(1, 0.1, 10, 6);
        FieldVector3D<DerivativeStructure> p2 = createVector(2, 0.2, 20, 6);
        FieldVector3D<DerivativeStructure> v  = createVector(-1, -0.1, -10, 6);
        checkPV(new FieldPVCoordinates<>(p2, v), new FieldPVCoordinates<>(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assertions.assertEquals(0.0,
                            FieldPVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm().getValue(),
                            1.0e-15);
    }

    @Test
    public void testGetters() {
        FieldVector3D<DerivativeStructure> p = createVector(1, 0.1, 10, 6);
        FieldVector3D<DerivativeStructure> v = createVector(-0.1, 1, 0, 6);
        FieldPVCoordinates<DerivativeStructure> pv = new FieldPVCoordinates<>(p, v);
        Assertions.assertEquals(0, FieldVector3D.distance(p, pv.getPosition()).getValue(), 1.0e-15);
        Assertions.assertEquals(0, FieldVector3D.distance(v, pv.getVelocity()).getValue(), 1.0e-15);
        Assertions.assertEquals(0, FieldVector3D.distance(createVector(-10, -1, 1.01, 6), pv.getMomentum()).getValue(), 1.0e-15);

        FieldPVCoordinates<DerivativeStructure> pvn = pv.negate();
        Assertions.assertEquals(0, FieldVector3D.distance(createVector(-1, -0.1, -10, 6), pvn.getPosition()).getValue(), 1.0e-15);
        Assertions.assertEquals(0, FieldVector3D.distance(createVector(0.1, -1, 0, 6), pvn.getVelocity()).getValue(), 1.0e-15);
        Assertions.assertEquals(0, FieldVector3D.distance(createVector(-10, -1, 1.01, 6), pvn.getMomentum()).getValue(), 1.0e-15);
    }

    @Test
    public void testToString() {
        FieldPVCoordinates<DerivativeStructure> pv = new FieldPVCoordinates<>(createVector( 1,  0.1,  10, 6),
                                                                              createVector(-1, -0.1, -10, 6),
                                                                              createVector(10,  1.0, 100, 6));
        Assertions.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    @Test
    public void testNormalize() {
        DSFactory factory = new DSFactory(1, 2);
        RandomGenerator generator = new Well19937a(0x7ede9376e4e1ab5al);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final FieldPVCoordinates<DerivativeStructure> pv = randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getX().getValue();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getY().getValue();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getZ().getValue();
                        }
                    }).value(factory.variable(0, 0.0));
            FieldPVCoordinates<DerivativeStructure> normalized = pv.normalize();
            Assertions.assertEquals(x.getValue(),              normalized.getPosition().getX().getValue(),     1.0e-16);
            Assertions.assertEquals(y.getValue(),              normalized.getPosition().getY().getValue(),     1.0e-16);
            Assertions.assertEquals(z.getValue(),              normalized.getPosition().getZ().getValue(),     1.0e-16);
            Assertions.assertEquals(x.getPartialDerivative(1), normalized.getVelocity().getX().getValue(),     3.0e-13);
            Assertions.assertEquals(y.getPartialDerivative(1), normalized.getVelocity().getY().getValue(),     3.0e-13);
            Assertions.assertEquals(z.getPartialDerivative(1), normalized.getVelocity().getZ().getValue(),     3.0e-13);
            Assertions.assertEquals(x.getPartialDerivative(2), normalized.getAcceleration().getX().getValue(), 6.0e-10);
            Assertions.assertEquals(y.getPartialDerivative(2), normalized.getAcceleration().getY().getValue(), 6.0e-10);
            Assertions.assertEquals(z.getPartialDerivative(2), normalized.getAcceleration().getZ().getValue(), 6.0e-10);
        }
    }

    private FieldVector3D<DerivativeStructure> randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        n = n / FastMath.sqrt(x * x + y * y + z * z);
        return createVector(n * x, n * y, n * z, 3);
    }

    private FieldPVCoordinates<DerivativeStructure> randomPVCoordinates(RandomGenerator random,
                                                                        double norm0, double norm1, double norm2) {
        FieldVector3D<DerivativeStructure> p0 = randomVector(random, norm0);
        FieldVector3D<DerivativeStructure> p1 = randomVector(random, norm1);
        FieldVector3D<DerivativeStructure> p2 = randomVector(random, norm2);
        return new FieldPVCoordinates<>(p0, p1, p2);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        DSFactory factory = new DSFactory(params, 1);
        return new FieldVector3D<>(factory.variable(0, x),
                                   factory.variable(1, y),
                                   factory.variable(2, z));
    }

    private <T extends CalculusFieldElement<T>> void checkPV(FieldPVCoordinates<T> expected, FieldPVCoordinates<T> real, double epsilon) {
        Assertions.assertEquals(expected.getPosition().getX().getReal(), real.getPosition().getX().getReal(), epsilon);
        Assertions.assertEquals(expected.getPosition().getY().getReal(), real.getPosition().getY().getReal(), epsilon);
        Assertions.assertEquals(expected.getPosition().getZ().getReal(), real.getPosition().getZ().getReal(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getX().getReal(), real.getVelocity().getX().getReal(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getY().getReal(), real.getVelocity().getY().getReal(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getZ().getReal(), real.getVelocity().getZ().getReal(), epsilon);
    }

    private interface OrbitFunction<T extends CalculusFieldElement<T>>  {
        FieldVector3D<T> apply(final FieldCartesianOrbit<T> o);
    }

    private <T extends CalculusFieldElement<T>> FieldVector3D<T> differentiate(FieldCartesianOrbit<T> orbit,
                                                                           OrbitFunction<T> picker) {
        try {
            FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();
            final T step = orbit.getDate().getField().getZero().add(0.01);
            for (int i = -4; i < 4; ++i) {
                T dt = step.multiply(i);
                interpolator.addSamplePoint(dt, picker.apply(orbit.shiftedBy(dt)).toArray());
            }
            return new FieldVector3D<>(interpolator.derivatives(orbit.getDate().getField().getZero(), 1)[1]);
        } catch (OrekitException oe) {
            return null;
        }
     }

}
