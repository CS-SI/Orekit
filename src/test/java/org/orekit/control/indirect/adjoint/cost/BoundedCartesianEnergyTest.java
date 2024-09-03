/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BoundedCartesianEnergyTest {

    @Test
    void getEventDetectorsSizeAndActionTest() {
        // GIVEN
        final double maximumThrustMagnitude = 1.;
        final double massFlowRateFactor = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massFlowRateFactor,
                maximumThrustMagnitude);
        // WHEN
        final Stream<EventDetector> eventDetectorStream = boundedCartesianEnergy.getEventDetectors();
        // THEN
        final List<EventDetector> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(2, eventDetectors.size());
        for (final EventDetector eventDetector : eventDetectors) {
            Assertions.assertInstanceOf(CartesianEnergyConsideringMass.SingularityDetector.class, eventDetector);
            final CartesianEnergyConsideringMass.SingularityDetector singularityDetector =
                    (CartesianEnergyConsideringMass.SingularityDetector) eventDetector;
            Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, true));
        }
    }

    @Test
    void getFieldEventDetectorsSizeAndActionTest() {
        // GIVEN
        final double maximumThrustMagnitude = 1.;
        final double massFlowRateFactor = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massFlowRateFactor,
                maximumThrustMagnitude);
        final Field<Complex> field = ComplexField.getInstance();
        // WHEN
        final Stream<FieldEventDetector<Complex>> eventDetectorStream = boundedCartesianEnergy.getFieldEventDetectors(field);
        // THEN
        final List<FieldEventDetector<Complex>> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(2, eventDetectors.size());
        for (final FieldEventDetector<Complex> eventDetector : eventDetectors) {
            Assertions.assertInstanceOf(CartesianEnergyConsideringMass.FieldSingularityDetector.class, eventDetector);
            final CartesianEnergyConsideringMass.FieldSingularityDetector<Complex> singularityDetector =
                    (CartesianEnergyConsideringMass.FieldSingularityDetector<Complex>) eventDetector;
            Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, true));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-3, 1e-1, 1e2})
    void testGetFieldThrustAccelerationVectorFieldFactor(final double massReal) {
        // GIVEN
        final double massRateFactor = 1.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                1.);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0, 0.0);
        fieldAdjoint[4] = new Complex(2.0, 0.0);
        fieldAdjoint[5] = new Complex(3.0, 0.0);
        fieldAdjoint[6] = new Complex(4.0, 0.0);
        final Complex mass = new Complex(massReal, 0.);
        // WHEN
        final FieldVector3D<Complex> fieldThrustVector = boundedCartesianEnergy.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final double[] adjoint = new double[7];
        for (int i = 0; i < adjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final Vector3D thrustVector = boundedCartesianEnergy.getThrustAccelerationVector(adjoint, mass.getReal());
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-4, 1e2})
    void testFieldUpdateDerivatives(final double mass) {
        // GIVEN
        final double massRateFactor = 1.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                2.);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0e-3, 0.0);
        fieldAdjoint[4] = new Complex(2.0e-3, 0.0);
        fieldAdjoint[6] = Complex.ONE;
        final Complex fieldMass = new Complex(mass, 0.);
        final Complex[] fieldDerivatives = MathArrays.buildArray(field, 7);
        // WHEN
        boundedCartesianEnergy.updateFieldAdjointDerivatives(fieldAdjoint, fieldMass, fieldDerivatives);
        // THEN
        final double[] adjoint = new double[7];
        for (int i = 0; i < fieldAdjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final double[] derivatives = new double[7];
        boundedCartesianEnergy.updateAdjointDerivatives(adjoint, fieldMass.getReal(), derivatives);
        Assertions.assertEquals(derivatives[6], fieldDerivatives[6].getReal());
    }

    @Test
    void testGetFieldThrustAccelerationVectorFieldVersusUnbounded() {
        // GIVEN
        final double massRateFactor = 1.;
        final double maximumThrustMagnitude = 1.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                maximumThrustMagnitude);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0e-3, 0.0);
        fieldAdjoint[4] = new Complex(2.0e-3, 0.0);
        final Complex mass = new Complex(3.e-3, 0.);
        // WHEN
        final FieldVector3D<Complex> fieldThrustVector = boundedCartesianEnergy.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final FieldVector3D<Complex> expectedThrustVector = new UnboundedCartesianEnergy("", massRateFactor)
                .getFieldThrustAccelerationVector(fieldAdjoint, mass).scalarMultiply(maximumThrustMagnitude);
        Assertions.assertEquals(expectedThrustVector, fieldThrustVector);
    }
}
