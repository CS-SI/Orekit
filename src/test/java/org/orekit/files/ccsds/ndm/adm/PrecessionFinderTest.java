/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UnitSphereRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Test class for precession finder.<p>
 * @author Luc Maisonobe
 */
public class PrecessionFinderTest {

    @Test
    public void testNoSpin() {
        final FieldVector3D<UnivariateDerivative2> spin =
                        FieldVector3D.getZero(UnivariateDerivative2Field.getInstance());
        PrecessionFinder pf = new PrecessionFinder(spin);
        Assertions.assertEquals(0.0, Vector3D.PLUS_K.distance(pf.getAxis()), 1.0e-15);
        Assertions.assertEquals(0.0, pf.getPrecessionAngle(), 1.0e-15);
        Assertions.assertEquals(0.0, pf.getAngularVelocity(), 1.0e-15);
    }

    @Test
    public void testFixedSpin() {
        final FieldVector3D<UnivariateDerivative2> spin =
                        new FieldVector3D<>(new UnivariateDerivative2( 1.25, 0.0, 0.0),
                                            new UnivariateDerivative2(-0.50, 0.0, 0.0),
                                            new UnivariateDerivative2( 2.00, 0.0, 0.0));
        PrecessionFinder pf = new PrecessionFinder(spin);
        Assertions.assertEquals(0.0, new Vector3D(1.25, -0.50, 2.00).normalize().distance(pf.getAxis()), 1.0e-15);
        Assertions.assertEquals(0.0, pf.getPrecessionAngle(), 1.0e-15);
        Assertions.assertEquals(0.0, pf.getAngularVelocity(), 1.0e-15);
    }

    @Test
    public void testMissingSecondDerivative() {
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final FieldRotation<UnivariateDerivative2> r =
                        new FieldRotation<>(FieldVector3D.getPlusK(field),
                                            new UnivariateDerivative2(0.0, -0.5, 0.0),
                                            RotationConvention.FRAME_TRANSFORM);
        final FieldVector3D<UnivariateDerivative2> spin = r.applyTo(new Vector3D(3, 0, 4));
        final FieldVector3D<UnivariateDerivative2> spinWithoutSecondDerivative =
                        new FieldVector3D<>(new UnivariateDerivative2(spin.getX().getValue(), spin.getX().getFirstDerivative(), 0.0),
                                            new UnivariateDerivative2(spin.getY().getValue(), spin.getY().getFirstDerivative(), 0.0),
                                            new UnivariateDerivative2(spin.getZ().getValue(), spin.getZ().getFirstDerivative(), 0.0));
        try {
            new PrecessionFinder(spinWithoutSecondDerivative);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CANNOT_ESTIMATE_PRECESSION_WITHOUT_PROPER_DERIVATIVES,
                                    oe.getSpecifier());
        }
    }

    @Test
    public void testCanonicalZ() {
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final FieldRotation<UnivariateDerivative2> r =
                        new FieldRotation<>(FieldVector3D.getPlusK(field),
                                            new UnivariateDerivative2(0.0, -0.5, 0.0),
                                            RotationConvention.FRAME_TRANSFORM);
        final FieldVector3D<UnivariateDerivative2> spin = r.applyTo(new Vector3D(3, 0, 4));
        PrecessionFinder pf = new PrecessionFinder(spin);
        Assertions.assertEquals(0.0, Vector3D.PLUS_K.distance(pf.getAxis()), 1.0e-15);
        Assertions.assertEquals(Vector3D.angle(new Vector3D(3, 0, 4), Vector3D.PLUS_K), pf.getPrecessionAngle(), 1.0e-15);
        Assertions.assertEquals(0.5, pf.getAngularVelocity(), 1.0e-15);
    }

    @Test
    public void testRandom() {
        RandomGenerator random = new Well19937a(0x7a1ac34897e52f1fl);
        UnitSphereRandomVectorGenerator us = new UnitSphereRandomVectorGenerator(3, random);
        double maxAngleError     = 0;
        double maxRateError      = 0;
        double maxAlignmentError = 0;
        for (int i = 0; i < 1000; ++i) {
            final Rotation base = new Rotation(new Vector3D(us.nextVector()),
                                               FastMath.PI * random.nextDouble(),
                                               RotationConvention.FRAME_TRANSFORM);
            final double phi0    = MathUtils.TWO_PI * random.nextDouble();
            final double phi0Dot = MathUtils.TWO_PI * random.nextDouble() * 0.001;
            final double theta   = FastMath.PI      * random.nextDouble();
            final double psi0    = MathUtils.TWO_PI * random.nextDouble();
            final double psi0Dot = MathUtils.TWO_PI * random.nextDouble() * 0.01;
            final FieldRotation<UnivariateDerivative2> r =
                            new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                new UnivariateDerivative2(phi0,  phi0Dot, 0.0),
                                                new UnivariateDerivative2(theta, 0.0,     0.0),
                                                new UnivariateDerivative2(psi0,  psi0Dot, 0.0)).
                            applyTo(base);
            // beware! here we follow CCSDS model were spin is always exactly Z axis
            // (it corresponds to the third rotation above) it is NOT the instantaneous rotation rate
            final FieldVector3D<UnivariateDerivative2> spin = r.applyInverseTo(Vector3D.PLUS_K);
            final PrecessionFinder pf = new PrecessionFinder(spin);
            maxAngleError     = FastMath.max(maxAngleError,     FastMath.abs(pf.getPrecessionAngle() - theta));
            maxRateError      = FastMath.max(maxRateError,      FastMath.abs(pf.getAngularVelocity() - phi0Dot));
            maxAlignmentError = FastMath.max(maxAlignmentError, Vector3D.angle(pf.getAxis(), base.applyInverseTo(Vector3D.PLUS_K)));
        }
        Assertions.assertEquals(0.0, maxAngleError,     1.7e-9);
        Assertions.assertEquals(0.0, maxRateError,      2.3e-13);
        Assertions.assertEquals(0.0, maxAlignmentError, 1.7e-9);
    }

}
