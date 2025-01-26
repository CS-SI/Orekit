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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.time.AbsoluteDate;

class CartesianCostTest {

    @Test
    void getEventDetectorsTest() {
        // GIVEN
        final TestCost cost = new TestCost();
        // WHEN & THEN
        Assertions.assertEquals(0., cost.getEventDetectors().count());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getCostDerivativeProviderTest(final boolean yields) {
        // GIVEN
        final TestCost cost = new TestCost();
        final String expectedName = "a";
        final SpacecraftState mockedState = Mockito.mock();
        final String adjointName = cost.getAdjointName();
        Mockito.when(mockedState.hasAdditionalState(adjointName)).thenReturn(yields);
        // WHEN
        final AdditionalDerivativesProvider costDerivative = cost.getCostDerivativeProvider(expectedName);
        // THEN
        Assertions.assertEquals(expectedName, costDerivative.getName());
        Assertions.assertEquals(1, costDerivative.getDimension());
        Assertions.assertNotEquals(yields, costDerivative.yields(mockedState));
    }

    @Test
    void getCostDerivativeProviderCombinedDerivativesTest() {
        // GIVEN
        final CartesianCost mockedCost = Mockito.mock();
        final String adjointName = "adjoint";
        Mockito.when(mockedCost.getAdjointName()).thenReturn(adjointName);
        final String name = "a";
        Mockito.when(mockedCost.getCostDerivativeProvider(name)).thenCallRealMethod();
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6};
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH))
                .addAdditionalState(mockedCost.getAdjointName(), adjoint);
        final double expectedDerivative = 1.;
        Mockito.when(mockedCost.getHamiltonianContribution(adjoint, state.getMass())).thenReturn(-expectedDerivative);
        // WHEN
        final AdditionalDerivativesProvider costDerivative = mockedCost.getCostDerivativeProvider(name);
        // THEN
        final CombinedDerivatives combinedDerivatives = costDerivative.combinedDerivatives(state);
        Assertions.assertNull(combinedDerivatives.getMainStateDerivativesIncrements());
        Assertions.assertEquals(expectedDerivative, combinedDerivatives.getAdditionalDerivatives()[0]);
    }
}
