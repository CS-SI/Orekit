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

import org.hamcrest.MatcherAssert;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;


public class PVCoordinatesTest {

    @Test
    public void testDefaultConstructor() {
        Assertions.assertEquals("{P(0.0, 0.0, 0.0), V(0.0, 0.0, 0.0), A(0.0, 0.0, 0.0)}", new PVCoordinates().toString());
    }

    @Test
    public void testLinearConstructors() {
        PVCoordinates pv1 = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                              new Vector3D(-1, -0.1, -10));
        PVCoordinates pv2 = new PVCoordinates(new Vector3D( 2,  0.2,  20),
                                              new Vector3D(-2, -0.2, -20));
        PVCoordinates pv3 = new PVCoordinates(new Vector3D( 3,  0.3,  30),
                                              new Vector3D(-3, -0.3, -30));
        PVCoordinates pv4 = new PVCoordinates(new Vector3D( 4,  0.4,  40),
                                              new Vector3D(-4, -0.4, -40));
        checkPV(pv4, new PVCoordinates(4, pv1), 1.0e-15);
        checkPV(pv2, new PVCoordinates(pv1, pv3), 1.0e-15);
        checkPV(pv3, new PVCoordinates(1, pv1, 1, pv2), 1.0e-15);
        checkPV(new PVCoordinates(2, pv4), new PVCoordinates(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new PVCoordinates(3, pv3), new PVCoordinates(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new PVCoordinates(5, pv4), new PVCoordinates(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVectorNeg() {
        try {
            PVCoordinates.ZERO.toDerivativeStructureVector(-1);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector3() {
        try {
            PVCoordinates.ZERO.toDerivativeStructureVector(3);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector0() {
        FieldVector3D<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(0);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(0, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        checkPV(new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  Vector3D.ZERO,
                                  Vector3D.ZERO),
                new PVCoordinates(fv), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector1() {
        FieldVector3D<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(1);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        checkPV(new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  Vector3D.ZERO),
                new PVCoordinates(fv), 1.0e-15);
    }

    @Test
    public void testUnivariateDerivative1Vector() {
        FieldVector3D<UnivariateDerivative1> fv =
                        new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toUnivariateDerivative1Vector();
        Assertions.assertEquals(1, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getDerivative(1), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getDerivative(1), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getDerivative(1), 1.0e-15);

        PVCoordinates pv = new PVCoordinates(fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);

    }

    @Test
    public void testToDerivativeStructureVector2() {
        FieldVector3D<DerivativeStructure> fv =
                new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(2);
        Assertions.assertEquals(1, fv.getX().getFreeParameters());
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getPartialDerivative(2), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getPartialDerivative(2), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getPartialDerivative(2), 1.0e-15);
        checkPV(new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                  new Vector3D(-1, -0.1, -10),
                                  new Vector3D(10, -1.0, -100)),
                new PVCoordinates(fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }

        PVCoordinates pv = new PVCoordinates(fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);
        Assertions.assertEquals(  10.0, pv.getAcceleration().getX(), 1.0e-15);
        Assertions.assertEquals(  -1.0, pv.getAcceleration().getY(), 1.0e-15);
        Assertions.assertEquals(-100.0, pv.getAcceleration().getZ(), 1.0e-15);

    }

    @Test
    public void testUnivariateDerivative2Vector() {
        FieldVector3D<UnivariateDerivative2> fv =
                        new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toUnivariateDerivative2Vector();
        Assertions.assertEquals(2, fv.getX().getOrder());
        Assertions.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assertions.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assertions.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assertions.assertEquals(  -1.0, fv.getX().getDerivative(1), 1.0e-15);
        Assertions.assertEquals(  -0.1, fv.getY().getDerivative(1), 1.0e-15);
        Assertions.assertEquals( -10.0, fv.getZ().getDerivative(1), 1.0e-15);
        Assertions.assertEquals(  10.0, fv.getX().getDerivative(2), 1.0e-15);
        Assertions.assertEquals(  -1.0, fv.getY().getDerivative(2), 1.0e-15);
        Assertions.assertEquals(-100.0, fv.getZ().getDerivative(2), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }

        PVCoordinates pv = new PVCoordinates(fv);
        Assertions.assertEquals(   1.0, pv.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals(   0.1, pv.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals(  10.0, pv.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals(  -1.0, pv.getVelocity().getX(), 1.0e-15);
        Assertions.assertEquals(  -0.1, pv.getVelocity().getY(), 1.0e-15);
        Assertions.assertEquals( -10.0, pv.getVelocity().getZ(), 1.0e-15);
        Assertions.assertEquals(  10.0, pv.getAcceleration().getX(), 1.0e-15);
        Assertions.assertEquals(  -1.0, pv.getAcceleration().getY(), 1.0e-15);
        Assertions.assertEquals(-100.0, pv.getAcceleration().getZ(), 1.0e-15);

    }

    @Test
    public void testToDerivativeStructurePVNeg() {
        try {
            PVCoordinates.ZERO.toDerivativeStructurePV(-1);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assertions.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructurePV3() {
        try {
            PVCoordinates.ZERO.toDerivativeStructurePV(3);
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
        FieldPVCoordinates<DerivativeStructure> fv =
                        new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toDerivativeStructurePV(1);
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

                Assertions.assertEquals(fv.getVelocity().getX().getReal(),     fv.getPosition().getX().getPartialDerivative(1), 1.0e-10);
                Assertions.assertEquals(fv.getVelocity().getY().getReal(),     fv.getPosition().getY().getPartialDerivative(1), 1.0e-10);
                Assertions.assertEquals(fv.getVelocity().getZ().getReal(),     fv.getPosition().getZ().getPartialDerivative(1), 1.0e-10);
                Assertions.assertEquals(fv.getAcceleration().getX().getReal(), fv.getVelocity().getX().getPartialDerivative(1), 1.0e-10);
                Assertions.assertEquals(fv.getAcceleration().getY().getReal(), fv.getVelocity().getY().getPartialDerivative(1), 1.0e-10);
                Assertions.assertEquals(fv.getAcceleration().getZ().getReal(), fv.getVelocity().getZ().getPartialDerivative(1), 1.0e-10);

    }

    @Test
    public void testToUnivariateDerivative1PV() {
        FieldPVCoordinates<UnivariateDerivative1> fv =
                        new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toUnivariateDerivative1PV();
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

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),     fv.getPosition().getX().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),     fv.getPosition().getY().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),     fv.getPosition().getZ().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(), fv.getVelocity().getX().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(), fv.getVelocity().getY().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(), fv.getVelocity().getZ().getDerivative(1), 1.0e-10);

    }

    @Test
    public void testToDerivativeStructurePV2() {
        FieldPVCoordinates<DerivativeStructure> fv =
                        new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toDerivativeStructurePV(2);
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

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),                   fv.getPosition().getX().getPartialDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),                   fv.getPosition().getY().getPartialDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),                   fv.getPosition().getZ().getPartialDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),               fv.getPosition().getX().getPartialDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),               fv.getPosition().getY().getPartialDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),               fv.getPosition().getZ().getPartialDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),               fv.getVelocity().getX().getPartialDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),               fv.getVelocity().getY().getPartialDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),               fv.getVelocity().getZ().getPartialDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getPartialDerivative(1), fv.getVelocity().getX().getPartialDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getPartialDerivative(1), fv.getVelocity().getY().getPartialDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getPartialDerivative(1), fv.getVelocity().getZ().getPartialDerivative(2), 1.0e-10);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getPosition().getX().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getPosition().getY().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getPosition().getZ().taylor(dt), 1.0e-14);
        }

    }

    @Test
    public void testToUnivariateDerivative2PV() {
        FieldPVCoordinates<UnivariateDerivative2> fv =
                        new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                          new Vector3D(-1, -0.1, -10),
                                          new Vector3D(10, -1.0, -100)).toUnivariateDerivative2PV();
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

        Assertions.assertEquals(fv.getVelocity().getX().getReal(),                   fv.getPosition().getX().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getY().getReal(),                   fv.getPosition().getY().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getVelocity().getZ().getReal(),                   fv.getPosition().getZ().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),               fv.getPosition().getX().getDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),               fv.getPosition().getY().getDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),               fv.getPosition().getZ().getDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getReal(),               fv.getVelocity().getX().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getReal(),               fv.getVelocity().getY().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getReal(),               fv.getVelocity().getZ().getDerivative(1), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getX().getDerivative(1), fv.getVelocity().getX().getDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getY().getDerivative(1), fv.getVelocity().getY().getDerivative(2), 1.0e-10);
        Assertions.assertEquals(fv.getAcceleration().getZ().getDerivative(1), fv.getVelocity().getZ().getDerivative(2), 1.0e-10);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assertions.assertEquals(p.getX(), fv.getPosition().getX().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getY(), fv.getPosition().getY().taylor(dt), 1.0e-14);
            Assertions.assertEquals(p.getZ(), fv.getPosition().getZ().taylor(dt), 1.0e-14);
        }
    }

    @Test
    public void testJerkIsVelocitySecondDerivative() {
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                                                          new Vector3D(-2079., 5291., -7842.)),
                                                        FramesFactory.getEME2000(),
                                                        AbsoluteDate.J2000_EPOCH,
                                                        Constants.EIGEN5C_EARTH_MU);
        FieldPVCoordinates<DerivativeStructure> fv = orbit.getPVCoordinates().toDerivativeStructurePV(2);
        Vector3D numericalJerk = differentiate(orbit, o -> o.getPVCoordinates().getAcceleration());
        Assertions.assertEquals(numericalJerk.getX(),
                            fv.getVelocity().getX().getPartialDerivative(2),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getY(),
                            fv.getVelocity().getY().getPartialDerivative(2),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getZ(),
                            fv.getVelocity().getZ().getPartialDerivative(2),
                            3.0e-13);

    }

    @Test
    public void testJerkIsAccelerationDerivative() {
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                                                          new Vector3D(-2079., 5291., -7842.)),
                                                        FramesFactory.getEME2000(),
                                                        AbsoluteDate.J2000_EPOCH,
                                                        Constants.EIGEN5C_EARTH_MU);

        FieldPVCoordinates<DerivativeStructure> fv1 = orbit.getPVCoordinates().toDerivativeStructurePV(1);
        Vector3D numericalJerk = differentiate(orbit, o -> o.getPVCoordinates().getAcceleration());
        Assertions.assertEquals(numericalJerk.getX(),
                            fv1.getAcceleration().getX().getPartialDerivative(1),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getY(),
                            fv1.getAcceleration().getY().getPartialDerivative(1),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getZ(),
                            fv1.getAcceleration().getZ().getPartialDerivative(1),
                            3.0e-13);

        FieldPVCoordinates<DerivativeStructure> fv2 = orbit.getPVCoordinates().toDerivativeStructurePV(2);
        Assertions.assertEquals(numericalJerk.getX(),
                            fv2.getAcceleration().getX().getPartialDerivative(1),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getY(),
                            fv2.getAcceleration().getY().getPartialDerivative(1),
                            3.0e-13);
        Assertions.assertEquals(numericalJerk.getZ(),
                            fv2.getAcceleration().getZ().getPartialDerivative(1),
                            3.0e-13);

    }

    @Test
    public void testJounceIsAccelerationSecondDerivative() {
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                                                          new Vector3D(-2079., 5291., -7842.)),
                                                        FramesFactory.getEME2000(),
                                                        AbsoluteDate.J2000_EPOCH,
                                                        Constants.EIGEN5C_EARTH_MU);
        FieldPVCoordinates<DerivativeStructure> fv = orbit.getPVCoordinates().toDerivativeStructurePV(2);
        Vector3D numericalJounce = differentiate(orbit, o -> {
            FieldVector3D<DerivativeStructure> a = o.getPVCoordinates().toDerivativeStructurePV(1).getAcceleration();
            return new Vector3D(a.getX().getPartialDerivative(1),
                                a.getY().getPartialDerivative(1),
                                a.getZ().getPartialDerivative(1));
        });
        Assertions.assertEquals(numericalJounce.getX(),
                            fv.getAcceleration().getX().getPartialDerivative(2),
                            1.0e-15);
        Assertions.assertEquals(numericalJounce.getY(),
                            fv.getAcceleration().getY().getPartialDerivative(2),
                            1.0e-15);
        Assertions.assertEquals(numericalJounce.getZ(),
                            fv.getAcceleration().getZ().getPartialDerivative(2),
                            1.0e-15);

    }

    @Test
    public void testMomentumDerivative() {
        final PVCoordinates pva =
                        new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                          new Vector3D(-2079., 5291., -7842.));
        final Vector3D p  = pva.getPosition();
        final Vector3D v  = pva.getVelocity();
        final Vector3D a  = pva.getAcceleration();
        final double   r2 = p.getNormSq();
        final double   r  = FastMath.sqrt(r2);
        final Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        final PVCoordinates velocity = new PVCoordinates(v, a, keplerianJerk);
        final Vector3D momentumRef    = pva.getMomentum();
        final Vector3D momentumDotRef = PVCoordinates.crossProduct(pva, velocity).getVelocity();

        final FieldVector3D<DerivativeStructure> momentumDot = pva.toDerivativeStructurePV(1).getMomentum();
        Assertions.assertEquals(momentumRef.getX(),    momentumDot.getX().getReal(),               1.0e-15);
        Assertions.assertEquals(momentumRef.getY(),    momentumDot.getY().getReal(),               1.0e-15);
        Assertions.assertEquals(momentumRef.getZ(),    momentumDot.getZ().getReal(),               1.0e-15);
        Assertions.assertEquals(momentumDotRef.getX(), momentumDot.getX().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(momentumDotRef.getY(), momentumDot.getY().getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(momentumDotRef.getZ(), momentumDot.getZ().getPartialDerivative(1), 1.0e-15);

    }

    @Test
    public void testShift() {
        Vector3D p1 = new Vector3D( 1,  0.1,  10);
        Vector3D p2 = new Vector3D( 2,  0.2,  20);
        Vector3D v  = new Vector3D(-1, -0.1, -10);
        checkPV(new PVCoordinates(p2, v), new PVCoordinates(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assertions.assertEquals(0.0, PVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm(), 1.0e-15);
        MatcherAssert.assertThat(
                new PVCoordinates(p1, v).positionShiftedBy(-1.0),
                OrekitMatchers.vectorCloseTo(p2, 1e-15));
    }

    @Test
    public void testToString() {
        PVCoordinates pv =
            new PVCoordinates(new Vector3D( 1,  0.1,  10), new Vector3D(-1, -0.1, -10));
        Assertions.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(0.0, 0.0, 0.0)}", pv.toString());
    }

    @Test
    public void testGetMomentum() {
        //setup
        Vector3D p = new Vector3D(1, -2, 3);
        Vector3D v = new Vector3D(-9, 8, -7);

        //action + verify
        Assertions.assertEquals(new PVCoordinates(p, v).getMomentum(), p.crossProduct(v));
        //check simple cases
        Assertions.assertEquals(
                new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_I).getMomentum(),
                Vector3D.ZERO);
        Assertions.assertEquals(
                new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J).getMomentum(),
                Vector3D.PLUS_K);
    }

    @Test
    public void testGetAngularVelocity() {
        //setup
        Vector3D p = new Vector3D(1, -2, 3);
        Vector3D v = new Vector3D(-9, 8, -7);

        //action + verify
        Assertions.assertEquals(
                new PVCoordinates(p, v).getAngularVelocity(),
                p.crossProduct(v).scalarMultiply(1.0 / p.getNormSq()));
        //check extra simple cases
        Assertions.assertEquals(
                new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_I).getAngularVelocity(),
                Vector3D.ZERO);
        Assertions.assertEquals(
                new PVCoordinates(new Vector3D(2, 0, 0), Vector3D.PLUS_J).getAngularVelocity(),
                Vector3D.PLUS_K.scalarMultiply(0.5));
    }

    @Test
    public void testNormalize() {
        DSFactory factory = new DSFactory(1, 2);
        RandomGenerator generator = new Well19937a(0xb2011ffd25412067l);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final PVCoordinates pv = randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getX();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getY();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return pv.shiftedBy(t).getPosition().normalize().getZ();
                        }
                    }).value(factory.variable(0, 0.0));
            PVCoordinates normalized = pv.normalize();
            Assertions.assertEquals(x.getValue(),              normalized.getPosition().getX(),     1.0e-16);
            Assertions.assertEquals(y.getValue(),              normalized.getPosition().getY(),     1.0e-16);
            Assertions.assertEquals(z.getValue(),              normalized.getPosition().getZ(),     1.0e-16);
            Assertions.assertEquals(x.getPartialDerivative(1), normalized.getVelocity().getX(),     3.0e-13);
            Assertions.assertEquals(y.getPartialDerivative(1), normalized.getVelocity().getY(),     3.0e-13);
            Assertions.assertEquals(z.getPartialDerivative(1), normalized.getVelocity().getZ(),     3.0e-13);
            Assertions.assertEquals(x.getPartialDerivative(2), normalized.getAcceleration().getX(), 6.0e-10);
            Assertions.assertEquals(y.getPartialDerivative(2), normalized.getAcceleration().getY(), 6.0e-10);
            Assertions.assertEquals(z.getPartialDerivative(2), normalized.getAcceleration().getZ(), 6.0e-10);
        }
    }

    @Test
    public void testCrossProduct() {
        DSFactory factory = new DSFactory(1, 2);
        RandomGenerator generator = new Well19937a(0x85c592b3be733d23l);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final PVCoordinates pv1 = randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            final PVCoordinates pv2 = randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            DerivativeStructure x =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                                                         pv2.shiftedBy(t).getPosition()).getX();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure y =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                                                         pv2.shiftedBy(t).getPosition()).getY();
                        }
                    }).value(factory.variable(0, 0.0));
            DerivativeStructure z =
                    differentiator.differentiate(new UnivariateFunction() {
                        public double value(double t) {
                            return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                                                         pv2.shiftedBy(t).getPosition()).getZ();
                        }
                    }).value(factory.variable(0, 0.0));
            PVCoordinates product = PVCoordinates.crossProduct(pv1, pv2);
            Assertions.assertEquals(x.getValue(),              product.getPosition().getX(),     1.0e-16);
            Assertions.assertEquals(y.getValue(),              product.getPosition().getY(),     1.0e-16);
            Assertions.assertEquals(z.getValue(),              product.getPosition().getZ(),     1.0e-16);
            Assertions.assertEquals(x.getPartialDerivative(1), product.getVelocity().getX(),     9.0e-10);
            Assertions.assertEquals(y.getPartialDerivative(1), product.getVelocity().getY(),     9.0e-10);
            Assertions.assertEquals(z.getPartialDerivative(1), product.getVelocity().getZ(),     9.0e-10);
            Assertions.assertEquals(x.getPartialDerivative(2), product.getAcceleration().getX(), 3.0e-9);
            Assertions.assertEquals(y.getPartialDerivative(2), product.getAcceleration().getY(), 3.0e-9);
            Assertions.assertEquals(z.getPartialDerivative(2), product.getAcceleration().getZ(), 3.0e-9);
        }
    }

    private Vector3D randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private PVCoordinates randomPVCoordinates(RandomGenerator random,
                                              double norm0, double norm1, double norm2) {
        Vector3D p0 = randomVector(random, norm0);
        Vector3D p1 = randomVector(random, norm1);
        Vector3D p2 = randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private void checkPV(PVCoordinates expected, PVCoordinates real, double epsilon) {
        Assertions.assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        Assertions.assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        Assertions.assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        Assertions.assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
    }

    private interface OrbitFunction {
        Vector3D apply(final CartesianOrbit o);
    }

    private Vector3D differentiate(CartesianOrbit orbit, OrbitFunction picker) {
        try {
            HermiteInterpolator interpolator = new HermiteInterpolator();
            final double step = 0.01;
            for (int i = -4; i < 4; ++i) {
                double dt = i * step;
                interpolator.addSamplePoint(dt, picker.apply(orbit.shiftedBy(dt)).toArray());
            }
            return new Vector3D(interpolator.derivatives(0.0, 1)[1]);
        } catch (OrekitException oe) {
            return null;
        }
     }

}
