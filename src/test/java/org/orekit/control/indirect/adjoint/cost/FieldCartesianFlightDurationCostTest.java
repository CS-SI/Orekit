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
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

class FieldCartesianFlightDurationCostTest {

    private static final String ADJOINT_NAME = "adjoint";

    @Test
    void testToCartesianCost() {
        // GIVEN
        final FieldCartesianFlightDurationCost<Binary64> fieldCost = new FieldCartesianFlightDurationCost<>(ADJOINT_NAME, Binary64.ONE,
                Binary64.PI);
        // WHEN
        final CartesianFlightDurationCost cost = fieldCost.toCartesianCost();
        // THEN
        Assertions.assertEquals(cost.getAdjointDimension(), fieldCost.getAdjointDimension());
        Assertions.assertEquals(cost.getAdjointName(), fieldCost.getAdjointName());
        Assertions.assertEquals(cost.getMaximumThrustMagnitude(), fieldCost.getMaximumThrustMagnitude().getReal());
        Assertions.assertEquals(cost.getMassFlowRateFactor(), fieldCost.getMassFlowRateFactor().getReal());
    }

    @Test
    void testGetHamiltonianContribution() {
        // GIVEN
        final FieldCartesianFlightDurationCost<Binary64> cost = new FieldCartesianFlightDurationCost<>(ADJOINT_NAME, Binary64.ONE,
                Binary64.PI);
        // WHEN
        final Binary64 contribution = cost.getFieldHamiltonianContribution(null, Binary64.ZERO);
        // THEN
        Assertions.assertEquals(Binary64.ONE.negate(), contribution);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testUpdateFieldAdjointDerivatives(final boolean withMass) {
        // GIVEN
        final Binary64 massFlowRateFactor = withMass ? Binary64.ONE : Binary64.ZERO;
        final FieldCartesianFlightDurationCost<Binary64> cost = new FieldCartesianFlightDurationCost<>(ADJOINT_NAME, massFlowRateFactor, Binary64.PI);
        final Binary64[] adjoint = MathArrays.buildArray(Binary64Field.getInstance(), withMass ? 7 : 6);
        final Binary64[] derivatives = adjoint.clone();
        adjoint[3] = Binary64.ONE;
        // WHEN
        cost.updateFieldAdjointDerivatives(adjoint, Binary64.ONE, derivatives);
        // THEN
        final Binary64 zero = Binary64.ZERO;
        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(zero, derivatives[i]);
        }
        if (withMass) {
            Assertions.assertNotEquals(zero, derivatives[derivatives.length - 1]);
        } else {
            Assertions.assertEquals(zero, derivatives[derivatives.length - 1]);
        }
    }

    @Test
    void testGetThrustAccelerationVector() {
        // GIVEN
        final FieldCartesianFlightDurationCost<Binary64> cost = new FieldCartesianFlightDurationCost<>(ADJOINT_NAME, Binary64.ONE, Binary64.PI);
        // WHEN
        final Binary64[] adjoint = MathArrays.buildArray(Binary64Field.getInstance(), 6);
        adjoint[3] = new Binary64(1);
        adjoint[4] = new Binary64(2);
        adjoint[5] = new Binary64(3);
        final FieldVector3D<Binary64> contribution = cost.getFieldThrustAccelerationVector(adjoint, Binary64.ZERO);
        // THEN
        final FieldVector3D<Binary64> fieldVector3D = new FieldVector3D<>(Arrays.copyOfRange(adjoint, 3, 6));
        Assertions.assertEquals(fieldVector3D.normalize().scalarMultiply(cost.getMaximumThrustMagnitude()),
                contribution);
    }
}
