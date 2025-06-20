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

import org.hipparchus.complex.Complex;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class FieldCartesianFuelCostTest {

    @Test
    void testConstructor() {
        // GIVEN
        final EventDetectionSettings expectedDetectionSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        // WHEN
        final FieldCartesianFuelCost<Binary64> cartesianFuel = new FieldCartesianFuelCost<>("", new Binary64(1),
                new Binary64(2));
        // THEN
        final FieldEventDetectionSettings<Binary64> actualDetectionSettings = cartesianFuel.getEventDetectionSettings();
        Assertions.assertEquals(expectedDetectionSettings.getMaxIterationCount(),
                actualDetectionSettings.getMaxIterationCount());
        Assertions.assertEquals(expectedDetectionSettings.getThreshold(), actualDetectionSettings.getThreshold().getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e1, 1, 1e1})
    void testUpdateFieldAdjointDerivatives(final double adjointMass) {
        // GIVEN
        final Binary64 rateFactor = Binary64.ONE;
        final Binary64 maximumThrust = new Binary64(2);
        final FieldCartesianFuelCost<Binary64> cartesianFuel = new FieldCartesianFuelCost<>("", rateFactor, maximumThrust);
        final double[] adjoint = new double[] {0, 0, 0, 0, 1, 0, adjointMass};
        final Binary64[] fieldAdjoint = MathArrays.buildArray(Binary64Field.getInstance(), adjoint.length);
        for (int i = 0; i < adjoint.length; i++) {
            fieldAdjoint[i] = new Binary64(adjoint[i]);
        }
        final Binary64 mass = new Binary64(100);
        final Binary64[] fieldDerivatives = MathArrays.buildArray(Binary64Field.getInstance(), adjoint.length);
        final double[] derivatives = new double[adjoint.length];
        // WHEN
        cartesianFuel.updateFieldAdjointDerivatives(fieldAdjoint, mass, fieldDerivatives);
        // THEN
        cartesianFuel.toCartesianCost().updateAdjointDerivatives(adjoint, mass.getReal(), derivatives);
        Assertions.assertEquals(derivatives[6], fieldDerivatives[6].getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e1, 1, 1e1})
    void testGetFieldThrustAccelerationVector(final double adjointMass) {
        // GIVEN
        final Binary64 rateFactor = Binary64.ONE;
        final Binary64 maximumThrust = new Binary64(2);
        final FieldCartesianFuelCost<Binary64> cartesianFuel = new FieldCartesianFuelCost<>("", rateFactor, maximumThrust);
        final double[] adjoint = new double[] {0, 0, 0, 0, 1, 0, adjointMass};
        final Binary64[] fieldAdjoint = MathArrays.buildArray(Binary64Field.getInstance(), adjoint.length);
        for (int i = 0; i < adjoint.length; i++) {
            fieldAdjoint[i] = new Binary64(adjoint[i]);
        }
        final Binary64 mass = new Binary64(100);
        // WHEN
        final FieldVector3D<Binary64> actual = cartesianFuel.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        Assertions.assertEquals(cartesianFuel.toCartesianCost().getThrustAccelerationVector(adjoint, mass.getReal()), actual.toVector3D());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetFieldHamiltonianContribution() {
        // GIVEN
        final FieldCartesianFuelCost<Binary64> cartesianFuel = Mockito.mock(FieldCartesianFuelCost.class);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] adjoint = MathArrays.buildArray(field, 0);
        final Binary64 mass = Binary64.ONE;
        final FieldVector3D<Binary64> accelerationVector = new FieldVector3D<>(field, new Vector3D(1, 2, 3));
        Mockito.when(cartesianFuel.getFieldThrustAccelerationVector(adjoint, mass)).thenReturn(accelerationVector);
        Mockito.when(cartesianFuel.getFieldHamiltonianContribution(adjoint, mass)).thenCallRealMethod();
        // WHEN
        final Binary64 actual = cartesianFuel.getFieldHamiltonianContribution(adjoint, mass);
        // THEN
        Assertions.assertEquals(accelerationVector.scalarMultiply(mass).getNorm(), actual.negate());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0., 1.})
    void testGetFieldEventDetectors(final double massFlowRateFactor) {
        // GIVEN
        final String adjointName = "1";
        final FieldCartesianFuelCost<Binary64> cartesianFuel = new FieldCartesianFuelCost<>(adjointName,
                new Binary64(massFlowRateFactor), new Binary64(2));
        final Binary64Field field = Binary64Field.getInstance();
        final double mass = 3;
        // WHEN
        final Stream<FieldEventDetector<Binary64>> detectorStream = cartesianFuel.getFieldEventDetectors(field);
        // THEN
        final List<FieldEventDetector<Binary64>> fieldDetectorList = detectorStream.collect(Collectors.toList());
        final List<EventDetector> detectorList = cartesianFuel.toCartesianCost().getEventDetectors().collect(Collectors.toList());
        Assertions.assertEquals(detectorList.size(), fieldDetectorList.size());
        final EventDetector detector = detectorList.get(0);
        final FieldEventDetector<Binary64> fieldEventDetector = fieldDetectorList.get(0);
        Assertions.assertInstanceOf(FieldResetDerivativesOnEvent.class, fieldEventDetector.getHandler());
        final FieldSpacecraftState<Binary64> fieldState = buildFieldState(mass, adjointName);
        Assertions.assertEquals(detector.g(fieldState.toSpacecraftState()), fieldEventDetector.g(fieldState).getReal());
        Assertions.assertEquals(detector.getDetectionSettings().getThreshold(),
                fieldEventDetector.getDetectionSettings().getThreshold().getReal());
        Assertions.assertEquals(detector.getDetectionSettings().getMaxIterationCount(),
                fieldEventDetector.getDetectionSettings().getMaxIterationCount());
    }

    private static FieldSpacecraftState<Binary64> buildFieldState(final double mass, final String adjointName) {
        final Orbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                new Vector3D(4, 5, 6), Vector3D.MINUS_K), FramesFactory.getEME2000(), 1);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] adjoint = MathArrays.buildArray(field, 7);
        for (int i = 0; i < adjoint.length; i++) {
            adjoint[i] = new Binary64(i + 1);
        }
        return new FieldSpacecraftState<>(field, new SpacecraftState(orbit, mass))
                .addAdditionalData(adjointName, adjoint);
    }

    @Test
    void testToCartesianCost() {
        // GIVEN
        final Complex massRateFactor = Complex.ONE;
        final FieldCartesianFuelCost<Complex> fieldCartesianEnergy = new FieldCartesianFuelCost<>("",
                massRateFactor, new Complex(2));
        // WHEN
        final CartesianFuelCost cartesianEnergy = fieldCartesianEnergy.toCartesianCost();
        // THEN
        Assertions.assertEquals(cartesianEnergy.getAdjointName(), fieldCartesianEnergy.getAdjointName());
        Assertions.assertEquals(cartesianEnergy.getMaximumThrustMagnitude(),
                fieldCartesianEnergy.getMaximumThrustMagnitude().getReal());
    }
}

