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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.events.EventDetectionSettings;

class PenalizedCartesianFuelCostTest {

    @Test
    void testGetters() {
        // GIVEN
        final double expectedMagnitude = 1.;
        final double expectedEpsilon = 0.5;
        final EventDetectionSettings expectedDetectionSettings = Mockito.mock(EventDetectionSettings.class);
        // WHEN
        final TestPenalizedCost penalizedCost = new TestPenalizedCost(expectedMagnitude, expectedEpsilon, expectedDetectionSettings);
        // THEN
        Assertions.assertEquals(expectedEpsilon, penalizedCost.getEpsilon());
        Assertions.assertEquals(expectedMagnitude, penalizedCost.getMaximumThrustMagnitude());
        Assertions.assertEquals(expectedDetectionSettings, penalizedCost.getEventDetectionSettings());
    }

    @Test
    void testGetThrustDirection() {
        // GIVEN
        final double expectedMagnitude = 1.;
        final double expectedEpsilon = 0.5;
        final EventDetectionSettings expectedDetectionSettings = Mockito.mock(EventDetectionSettings.class);
        final TestPenalizedCost penalizedCost = new TestPenalizedCost(expectedMagnitude, expectedEpsilon,
                expectedDetectionSettings);
        final double[] adjoint = new double[] {0, 0, 0, 1, 2, 3};
        // WHEN
        final Vector3D thrustDirection = penalizedCost.getThrustDirection(adjoint);
        // THEN
        Assertions.assertEquals(new Vector3D(adjoint[3], adjoint[4], adjoint[5]).normalize(), thrustDirection);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-2, 2})
    void testExceptionConstructor(final double outOfBoundsEpsilon) {
        // GIVEN
        final EventDetectionSettings mockedDetectionSettings = Mockito.mock(EventDetectionSettings.class);
        // WHEN & THEN
        final OrekitException exception = Assertions.assertThrows(OrekitException.class,
                () -> new TestPenalizedCost(1, outOfBoundsEpsilon, mockedDetectionSettings));
        Assertions.assertEquals(OrekitMessages.INVALID_PARAMETER_RANGE, exception.getSpecifier());
    }

    private static class TestPenalizedCost extends PenalizedCartesianFuelCost {

        protected TestPenalizedCost(double maximumThrustMagnitude,
                                    double epsilon, EventDetectionSettings eventDetectionSettings) {
            super("", 0., maximumThrustMagnitude, epsilon, eventDetectionSettings);
        }

        @Override
        public double evaluatePenaltyFunction(double controlNorm) {
            return 0;
        }

        @Override
        public Vector3D getThrustAccelerationVector(double[] adjointVariables, double mass) {
            return null;
        }

        @Override
        public void updateAdjointDerivatives(double[] adjointVariables, double mass, double[] adjointDerivatives) {
            // not used
        }
    }
}
