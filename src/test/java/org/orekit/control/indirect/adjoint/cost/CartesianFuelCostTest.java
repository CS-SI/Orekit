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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CartesianFuelCostTest {

    @Test
    void testConstructor() {
        // GIVEN
        final EventDetectionSettings expectedDetectionSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        // WHEN
        final CartesianFuelCost cartesianFuel = new CartesianFuelCost("", 1., 2.);
        // THEN
        final EventDetectionSettings actualDetectionSettings = cartesianFuel.getEventDetectionSettings();
        Assertions.assertEquals(expectedDetectionSettings.getMaxIterationCount(),
                actualDetectionSettings.getMaxIterationCount());
        Assertions.assertEquals(expectedDetectionSettings.getThreshold(), actualDetectionSettings.getThreshold());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e1, 1, 1e1})
    void testUpdateAdjointDerivatives(final double adjointMass) {
        // GIVEN
        final double rateFactor = 1.;
        final double maximumThrust = 2.;
        final CartesianFuelCost cartesianFuel = new CartesianFuelCost("", rateFactor, maximumThrust);
        final double[] adjoint = new double[] {0, 0, 0, 1, 0, 0, adjointMass};
        final double mass = 100;
        final double[] derivatives = new double[7];
        // WHEN
        cartesianFuel.updateAdjointDerivatives(adjoint, mass, derivatives);
        // THEN
        if (derivatives[6] != 0) {
            Assertions.assertEquals(maximumThrust / mass / mass, derivatives[6]);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e1, 1, 1e1})
    void testGetThrustAccelerationVector(final double adjointMass) {
        // GIVEN
        final double rateFactor = 1.;
        final double maximumThrust = 2.;
        final CartesianFuelCost cartesianFuel = new CartesianFuelCost("", rateFactor, maximumThrust);
        final double[] adjoint = new double[] {0, 0, 0, 1, 0, 0, adjointMass};
        final double mass = 100;
        // WHEN
        final Vector3D actual = cartesianFuel.getThrustAccelerationVector(adjoint, mass);
        // THEN
        if (actual.getNorm() != 0) {
            Assertions.assertEquals(Vector3D.PLUS_I.scalarMultiply(maximumThrust / mass), actual);
        }
    }

    @Test
    void testGetHamiltonianContribution() {
        // GIVEN
        final CartesianFuelCost cartesianFuel = Mockito.mock(CartesianFuelCost.class);
        final double[] adjoint = new double[0];
        final double mass = 1;
        final Vector3D accelerationVector = new Vector3D(1, 2, 3);
        Mockito.when(cartesianFuel.getThrustAccelerationVector(adjoint, mass)).thenReturn(accelerationVector);
        Mockito.when(cartesianFuel.getHamiltonianContribution(adjoint, mass)).thenCallRealMethod();
        // WHEN
        final double actual = cartesianFuel.getHamiltonianContribution(adjoint, mass);
        // THEN
        Assertions.assertEquals(accelerationVector.scalarMultiply(mass).getNorm(), -actual);
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final EventDetectionSettings mockedDetectionSettings = Mockito.mock(EventDetectionSettings.class);
        final CartesianFuelCost cartesianFuel = new CartesianFuelCost("", 1., 2.,
                mockedDetectionSettings);
        // WHEN
        final Stream<EventDetector> detectorStream = cartesianFuel.getEventDetectors();
        // THEN
        final List<EventDetector> detectorList = detectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, detectorList.size());
        Assertions.assertEquals(mockedDetectionSettings, detectorList.get(0).getDetectionSettings());
        Assertions.assertInstanceOf(ResetDerivativesOnEvent.class, detectorList.get(0).getHandler());
    }

}

