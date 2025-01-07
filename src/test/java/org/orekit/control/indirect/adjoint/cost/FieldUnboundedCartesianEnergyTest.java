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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
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
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class FieldUnboundedCartesianEnergyTest {

    @Test
    void testGetFieldThrustAccelerationVectorFieldFactor() {
        // GIVEN
        final Complex massRateFactor = Complex.ONE;
        final FieldUnboundedCartesianEnergy<Complex> unboundedCartesianEnergy = new FieldUnboundedCartesianEnergy<>("", massRateFactor);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0, 0.0);
        fieldAdjoint[4] = new Complex(2.0, 0.0);
        final Complex mass = new Complex(3., 0.);
        // WHEN
        final FieldVector3D<Complex> fieldThrustVector = unboundedCartesianEnergy.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final double[] adjoint = new double[7];
        for (int i = 0; i < adjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final Vector3D thrustVector = unboundedCartesianEnergy.toCartesianCost().getThrustAccelerationVector(adjoint, mass.getReal());
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-1, 1e10})
    void testFieldDerivatives(final double mass) {
        // GIVEN
        final String name = "a";
        final FieldUnboundedCartesianEnergy<Gradient> energy = new FieldUnboundedCartesianEnergy<>(name, GradientField.getField(1).getZero());
        final FieldCartesianAdjointDerivativesProvider<Gradient> fieldAdjointDerivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(energy);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, mass).addAdditionalState(name, 1., 2., 3., 4., 5., 6., 7.);
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(GradientField.getField(1), state);
        // WHEN
        final FieldCombinedDerivatives<Gradient>fieldCombinedDerivatives = fieldAdjointDerivativesProvider.combinedDerivatives(fieldState);
        // THEN
        final CartesianAdjointDerivativesProvider adjointDerivativesProvider = new CartesianAdjointDerivativesProvider(energy.toCartesianCost());
        final CombinedDerivatives combinedDerivatives = adjointDerivativesProvider.combinedDerivatives(state);
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(combinedDerivatives.getMainStateDerivativesIncrements()[i],
                    fieldCombinedDerivatives.getMainStateDerivativesIncrements()[i].getReal(), 1e-12);
        }
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(combinedDerivatives.getAdditionalDerivatives()[i],
                    fieldCombinedDerivatives.getAdditionalDerivatives()[i].getReal(), 1e-10);
        }
    }

    @Test
    void getFieldEventDetectorsSizeAndActionTest() {
        // GIVEN
        final Complex massFlowRateFactor = new Complex(2);
        final FieldUnboundedCartesianEnergy<Complex> unboundedCartesianEnergy = new FieldUnboundedCartesianEnergy<>("", massFlowRateFactor);
        final Field<Complex> field = ComplexField.getInstance();
        // WHEN
        final Stream<FieldEventDetector<Complex>> eventDetectorStream = unboundedCartesianEnergy.getFieldEventDetectors(field);
        // THEN
        final List<FieldEventDetector<Complex>> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, eventDetectors.size());
        Assertions.assertInstanceOf(FieldCartesianEnergyConsideringMass.FieldSingularityDetector.class, eventDetectors.get(0));
        final FieldCartesianEnergyConsideringMass<Complex>.FieldSingularityDetector singularityDetector =
                (FieldCartesianEnergyConsideringMass<Complex>.FieldSingularityDetector) eventDetectors.get(0);
        Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, false));
    }

    @Test
    void getFieldEventDetectorsTest() {
        // GIVEN
        final Complex massFlowRateFactor = new Complex(3);
        final String name = "a";
        final FieldUnboundedCartesianEnergy<Complex> unboundedCartesianEnergy = new FieldUnboundedCartesianEnergy<>(name, massFlowRateFactor);
        final Field<Complex> field = ComplexField.getInstance();
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, 10.).addAdditionalState(name, 1., 2., 3., 4., 5., 6., 7.);
        // WHEN
        final Stream<FieldEventDetector<Complex>> fieldEventDetectorStream = unboundedCartesianEnergy.getFieldEventDetectors(field);
        // THEN
        final List<FieldEventDetector<Complex>> fieldEventDetectors = fieldEventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, fieldEventDetectors.size());
        Assertions.assertInstanceOf(FieldCartesianEnergyConsideringMass.FieldSingularityDetector.class, fieldEventDetectors.get(0));
        final FieldCartesianEnergyConsideringMass<Complex>.FieldSingularityDetector fieldSingularityDetector =
                (FieldCartesianEnergyConsideringMass<Complex>.FieldSingularityDetector) fieldEventDetectors.get(0);
        final Complex gValue = fieldSingularityDetector.g(new FieldSpacecraftState<>(field, state));
        final List<EventDetector> eventDetectors = unboundedCartesianEnergy.toCartesianCost().getEventDetectors().collect(Collectors.toList());
        final CartesianEnergyConsideringMass.SingularityDetector singularityDetector =
                (CartesianEnergyConsideringMass.SingularityDetector) eventDetectors.get(0);
        final double expectedG = singularityDetector.g(state);
        Assertions.assertEquals(expectedG, gValue.getReal());
    }

    @Test
    void testToCartesianCost() {
        // GIVEN
        final Complex massRateFactor = Complex.ONE;
        final FieldUnboundedCartesianEnergy<Complex> fieldCartesianEnergy = new FieldUnboundedCartesianEnergy<>("",
                massRateFactor);
        // WHEN
        final UnboundedCartesianEnergy cartesianEnergy = fieldCartesianEnergy.toCartesianCost();
        // THEN
        Assertions.assertEquals(cartesianEnergy.getAdjointName(), fieldCartesianEnergy.getAdjointName());
    }
}
