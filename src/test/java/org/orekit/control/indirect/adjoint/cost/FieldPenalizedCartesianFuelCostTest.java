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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

class FieldPenalizedCartesianFuelCostTest {

    @ParameterizedTest
    @ValueSource(doubles = {-2, 2})
    void testExceptionConstructor(final double epsilon) {
        // GIVEN
        final Binary64 magnitude = Binary64.ONE;        // WHEN & THEN
        final Binary64 outOfBoundsEpsilon = magnitude.newInstance(epsilon);
        final OrekitException exception = Assertions.assertThrows(OrekitException.class,
                () -> new TestPenalizedCost(magnitude, outOfBoundsEpsilon));
        Assertions.assertEquals(OrekitMessages.INVALID_PARAMETER_RANGE, exception.getSpecifier());
    }

    @Test
    void testGetters() {
        // GIVEN
        final Binary64 expectedMagnitude = Binary64.ONE;
        final Binary64 expectedEpsilon = Binary64.ZERO;
        // WHEN
        final TestPenalizedCost penalizedCost = new TestPenalizedCost(expectedMagnitude, expectedEpsilon);
        // THEN
        Assertions.assertEquals(expectedEpsilon, penalizedCost.getEpsilon());
        Assertions.assertEquals(expectedMagnitude, penalizedCost.getMaximumThrustMagnitude());
    }

    private static class TestPenalizedCost extends FieldPenalizedCartesianFuelCost<Binary64> {

        protected TestPenalizedCost(Binary64 maximumThrustMagnitude, Binary64 epsilon) {
            super("", Binary64.ZERO, maximumThrustMagnitude, epsilon);
        }

        @Override
        public Binary64 evaluateFieldPenaltyFunction(Binary64 controlNorm) {
            return Binary64.ZERO;
        }

        @Override
        public FieldVector3D<Binary64> getFieldThrustAccelerationVector(Binary64[] adjointVariables, Binary64 mass) {
            return null;
        }

        @Override
        public void updateFieldAdjointDerivatives(Binary64[] adjointVariables, Binary64 mass, Binary64[] adjointDerivatives) {
            // not used
        }

        @Override
        public CartesianCost toCartesianCost() {
            return null;
        }
    }
}
