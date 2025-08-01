/* Copyright 2022-2025 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

class KeplerianMotionCartesianUtilityTest {

    private static final double TOLERANCE_DISTANCE = 1e-13;
    private static final double TOLERANCE_SPEED = 1e-14;

    @Test
    void testPredictPositionVelocityElliptic() {
        // GIVEN
        final double mu = 1.;
        final Vector3D position = new Vector3D(10., 1., 0.);
        final Vector3D velocity = new Vector3D(0., 0.1, 0.2);
        // WHEN & THEN
        for (double dt = -10.; dt <= 10.; dt += 0.1) {
            final PVCoordinates actualPV = KeplerianMotionCartesianUtility.predictPositionVelocity(dt, position,
                    velocity, mu);
            final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(position, velocity), FramesFactory.getGCRF(),
                    AbsoluteDate.ARBITRARY_EPOCH, mu);
            final EquinoctialOrbit equinoctialOrbit = new EquinoctialOrbit(orbit);
            final PVCoordinates expectedPV = equinoctialOrbit.shiftedBy(dt).getPVCoordinates();
            comparePV(expectedPV, actualPV);
        }
    }

    @Test
    void testPredictPositionVelocityHyperbolic() {
        // GIVEN
        final double mu = 1.;
        final Vector3D position = new Vector3D(10., 1., 0.);
        final Vector3D velocity = new Vector3D(0., 0.1, 1.);
        // WHEN & THEN
        for (double dt = -10.; dt <= 10.; dt += 0.1) {
            final PVCoordinates actualPV = KeplerianMotionCartesianUtility.predictPositionVelocity(dt, position,
                    velocity, mu);
            final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(position, velocity), FramesFactory.getGCRF(),
                    AbsoluteDate.ARBITRARY_EPOCH, mu);
            final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(orbit);
            final PVCoordinates expectedPV = keplerianOrbit.shiftedBy(dt).getPVCoordinates();
            comparePV(expectedPV, actualPV);
        }
    }

    private void comparePV(final PVCoordinates pvCoordinates, final PVCoordinates otherPVCoordinates) {
        comparePV(pvCoordinates, otherPVCoordinates, TOLERANCE_DISTANCE, TOLERANCE_SPEED);
    }

    private void comparePV(final PVCoordinates pvCoordinates, final PVCoordinates otherPVCoordinates,
                           final double toleranceDistance, final double toleranceSpeed) {
        final double expectedValue = 0.;
        final PVCoordinates relativePV = new PVCoordinates(pvCoordinates, otherPVCoordinates);
        Assertions.assertEquals(expectedValue, relativePV.getPosition().getNorm(), toleranceDistance);
        Assertions.assertEquals(expectedValue, relativePV.getVelocity().getNorm(), toleranceSpeed);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0., 1e-20, 1e-15, 1e-10, 1e-8, 1e-5, 1e-2, 1e-1})
    void testPredictPositionVelocityCircularField(final double speedShift) {
        // GIVEN
        final double mu = 1.5;
        final Vector3D position = new Vector3D(4., 0., 0.);
        final Vector3D velocity = new Vector3D(0., FastMath.sqrt(mu / position.getNorm()) + speedShift, 0.);
        final GradientField field = GradientField.getField(6);
        final Gradient fieldMu = new Gradient(mu, new double[6]);
        final FieldOrbit<Gradient> fieldOrbit = DerivativeStateUtils.buildOrbitGradient(field,
                new CartesianOrbit(new PVCoordinates(position, velocity), FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, mu));
        final FieldVector3D<Gradient> fieldPosition = fieldOrbit.getPosition();
        final FieldVector3D<Gradient> fieldVelocity = fieldOrbit.getVelocity();
        // WHEN & THEN
        for (double dt = -10.; dt <= 10.; dt += 0.1) {
            final Gradient fieldDt = new Gradient(dt, new double[6]);
            final FieldPVCoordinates<Gradient> actualPV = KeplerianMotionCartesianUtility.predictPositionVelocity(fieldDt,
                    fieldPosition, fieldVelocity, fieldMu);
            final PVCoordinates expectedPV = KeplerianMotionCartesianUtility.predictPositionVelocity(dt, position,
                    velocity, mu);
            comparePV(expectedPV, actualPV.toPVCoordinates());
            final FieldOrbit<Gradient> fieldEquinoctialOrbit = OrbitType.EQUINOCTIAL.convertType(fieldOrbit);
            final FieldOrbit<Gradient> shiftedOrbit = fieldEquinoctialOrbit.shiftedBy(fieldDt);
            compareDerivatives(shiftedOrbit.getPVCoordinates(), actualPV);
        }
    }

    private static void compareDerivatives(final FieldPVCoordinates<Gradient> expectedPV,
                                           final FieldPVCoordinates<Gradient> actualPV) {

        final double toleranceGradientPosition = 1e-6;
        final double toleranceGradientVelocity = 1e-7;
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(expectedPV.getPosition().getX().getPartialDerivative(i),
                    actualPV.getPosition().getX().getPartialDerivative(i), toleranceGradientPosition);
            Assertions.assertEquals(expectedPV.getPosition().getY().getPartialDerivative(i),
                    actualPV.getPosition().getY().getPartialDerivative(i), toleranceGradientPosition);
            Assertions.assertEquals(expectedPV.getPosition().getZ().getPartialDerivative(i),
                    actualPV.getPosition().getZ().getPartialDerivative(i), toleranceGradientPosition);
            Assertions.assertEquals(expectedPV.getVelocity().getX().getPartialDerivative(i),
                    actualPV.getVelocity().getX().getPartialDerivative(i), toleranceGradientVelocity);
            Assertions.assertEquals(expectedPV.getVelocity().getY().getPartialDerivative(i),
                    actualPV.getVelocity().getY().getPartialDerivative(i), toleranceGradientVelocity);
            Assertions.assertEquals(expectedPV.getVelocity().getZ().getPartialDerivative(i),
                    actualPV.getVelocity().getZ().getPartialDerivative(i), toleranceGradientVelocity);
        }
    }

    @Test
    void testPredictPositionVelocityEllipticField() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final double mu = 1.;
        final Vector3D position = new Vector3D(6., 0., 1.);
        final Vector3D velocity = new Vector3D(0.01, 0.1, 0.);
        final Complex fieldMu = new Complex(mu);
        final FieldVector3D<Complex> fieldPosition = new FieldVector3D<>(field, position);
        final FieldVector3D<Complex> fieldVelocity = new FieldVector3D<>(field, velocity);
        // WHEN & THEN
        for (double dt = -10.; dt <= 10.; dt += 0.1) {
            final FieldPVCoordinates<Complex> actualPV = KeplerianMotionCartesianUtility.predictPositionVelocity(new Complex(dt),
                    fieldPosition, fieldVelocity, fieldMu);
            final PVCoordinates expectedPV = KeplerianMotionCartesianUtility.predictPositionVelocity(dt, position,
                    velocity, mu);
            comparePV(expectedPV, actualPV.toPVCoordinates());
        }
    }

    @Test
    void testPredictPositionVelocityHyperbolicField() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final double mu = 1.;
        final Vector3D position = new Vector3D(6., 0., 2.);
        final Vector3D velocity = new Vector3D(0.01, 1, 0.);
        final Complex fieldMu = new Complex(mu);
        final FieldVector3D<Complex> fieldPosition = new FieldVector3D<>(field, position);
        final FieldVector3D<Complex> fieldVelocity = new FieldVector3D<>(field, velocity);
        // WHEN & THEN
        for (double dt = -10.; dt <= 10.; dt += 0.1) {
            final FieldPVCoordinates<Complex> actualPV = KeplerianMotionCartesianUtility.predictPositionVelocity(new Complex(dt),
                    fieldPosition, fieldVelocity, fieldMu);
            final PVCoordinates expectedPV = KeplerianMotionCartesianUtility.predictPositionVelocity(dt, position,
                    velocity, mu);
            comparePV(expectedPV, actualPV.toPVCoordinates());
        }
    }

}
