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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class QuadraticallyPenalizedCartesianFuelTest {

    private static final String ADJOINT_NAME = "adjoint";

    @ParameterizedTest
    @ValueSource(doubles = {1, 2, 10, 100, 1000})
    void testEvaluatePenaltyFunction(final double norm) {
        // GIVEN
        final QuadraticallyPenalizedCartesianFuel penalizedCartesianFuel = new QuadraticallyPenalizedCartesianFuel(ADJOINT_NAME,
                1., 2., 0.5);
        // WHEN
        final double actualPenalty = penalizedCartesianFuel.evaluatePenaltyFunction(norm);
        // THEN
        Assertions.assertEquals(norm * norm / 2, actualPenalty);
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final QuadraticallyPenalizedCartesianFuel penalizedCartesianFuel = new QuadraticallyPenalizedCartesianFuel(ADJOINT_NAME,
                1., 2., 0.5);
        // WHEN
        final Stream<EventDetector> actual = penalizedCartesianFuel.getEventDetectors();
        // THEN
        final List<EventDetector> eventDetectorList = actual.collect(Collectors.toList());
        Assertions.assertEquals(2, eventDetectorList.size());
        final SpacecraftState state = buildState(new double[] {0, 0, 0, 1, 2, 3, 4}, 10);
        final double g1 = eventDetectorList.get(0).g(state);
        final double g2 = eventDetectorList.get(1).g(state);
        Assertions.assertEquals(penalizedCartesianFuel.getMaximumThrustMagnitude(), FastMath.abs(g2 - g1));
    }

    @Test
    void testGetThrustAccelerationVectorEpsilonCloseToZero() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        final double maximumThrustMagnitude = 10.;
        final double epsilon = 1e-6;
        final QuadraticallyPenalizedCartesianFuel penalizedCartesianFuel = new QuadraticallyPenalizedCartesianFuel(ADJOINT_NAME,
                massFlowRateFactor, maximumThrustMagnitude, epsilon);
        final double mass = 100;
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6, 7};
        // WHEN
        final Vector3D actualThrustVector = penalizedCartesianFuel.getThrustAccelerationVector(adjoint, mass);
        // THEN
        final CartesianFuelCost fuelCost = new CartesianFuelCost(ADJOINT_NAME, massFlowRateFactor, maximumThrustMagnitude);
        final Vector3D expectedThrustVector = fuelCost.getThrustAccelerationVector(adjoint, mass);
        Assertions.assertEquals(0., expectedThrustVector.subtract(actualThrustVector).getNorm(), 1e-10);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1, 1e2, 1e4})
    void testAgainstBoundedCartesianEnergy(final double mass) {
        // GIVEN
        final double massFlowRateFactor = 1.e-2;
        final double maximumThrustMagnitude = 1e-3;
        final double epsilon = 1;
        final QuadraticallyPenalizedCartesianFuel penalizedCartesianFuel = new QuadraticallyPenalizedCartesianFuel(ADJOINT_NAME,
                massFlowRateFactor, maximumThrustMagnitude, epsilon);
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6, 7};
        final SpacecraftState state = buildState(adjoint, mass);
        final AdditionalDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(penalizedCartesianFuel);
        // WHEN
        final CombinedDerivatives actualDerivatives = derivativesProvider.combinedDerivatives(state);
        // THEN
        final BoundedCartesianEnergy energy = new BoundedCartesianEnergy(ADJOINT_NAME, massFlowRateFactor, maximumThrustMagnitude);
        final CombinedDerivatives expectedDerivatives = new CartesianAdjointDerivativesProvider(energy).combinedDerivatives(state);
        Assertions.assertArrayEquals(expectedDerivatives.getAdditionalDerivatives(),
                actualDerivatives.getAdditionalDerivatives());
        Assertions.assertArrayEquals(expectedDerivatives.getMainStateDerivativesIncrements(),
                actualDerivatives.getMainStateDerivativesIncrements());
    }

    private static SpacecraftState buildState(final double[] adjoint, final double mass) {
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_I, Vector3D.MINUS_K),
                FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        return new SpacecraftState(orbit, mass).addAdditionalState(ADJOINT_NAME, adjoint);
    }
}
