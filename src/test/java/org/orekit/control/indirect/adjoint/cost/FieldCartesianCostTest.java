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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.time.AbsoluteDate;

class FieldCartesianCostTest {

    @Test
    void getFieldEventDetectorsTest() {
        // GIVEN
        final TestFieldCost fieldCost = new TestFieldCost();
        // WHEN & THEN
        Assertions.assertEquals(0., fieldCost.getFieldEventDetectors(Binary64Field.getInstance()).count());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SuppressWarnings("unchecked")
    void getCostDerivativeProviderTest(final boolean yields) {
        // GIVEN
        final TestFieldCost fieldCost = new TestFieldCost();
        final String expectedName = "a";
        final FieldSpacecraftState mockedState = Mockito.mock();
        final String adjointName = fieldCost.getAdjointName();
        Mockito.when(mockedState.hasAdditionalState(adjointName)).thenReturn(yields);
        // WHEN
        final FieldAdditionalDerivativesProvider<Binary64> fieldCostDerivative = fieldCost.getCostDerivativeProvider(expectedName);
        // THEN
        Assertions.assertEquals(expectedName, fieldCostDerivative.getName());
        Assertions.assertEquals(1, fieldCostDerivative.getDimension());
        Assertions.assertNotEquals(yields, fieldCostDerivative.yields(mockedState));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCostDerivativeProviderCombinedDerivativesTest() {
        // GIVEN
        final FieldCartesianCost<Binary64> mockedFieldCost = Mockito.mock();
        final String expectedName = "a";
        final String adjointName = "adjoint";
        Mockito.when(mockedFieldCost.getAdjointName()).thenReturn(adjointName);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] adjoint = MathArrays.buildArray(field, 6);
        for (int i = 0; i < adjoint.length; ++i) {
            adjoint[i] = new Binary64(i);
        }
        final FieldSpacecraftState<Binary64> state = new FieldSpacecraftState<>(field,
                new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH)))
                .addAdditionalState(adjointName, adjoint);
        final Binary64 expectedDerivative = Binary64.ONE;
        Mockito.when(mockedFieldCost.getFieldHamiltonianContribution(adjoint, state.getMass()))
                .thenReturn(expectedDerivative.negate());
        Mockito.when(mockedFieldCost.getCostDerivativeProvider(expectedName)).thenCallRealMethod();
        // WHEN
        final FieldAdditionalDerivativesProvider<Binary64> fieldCostDerivative = mockedFieldCost.getCostDerivativeProvider(expectedName);
        // THEN
        final FieldCombinedDerivatives<Binary64> fieldCombinedDerivatives = fieldCostDerivative.combinedDerivatives(state);
        Assertions.assertNull(fieldCombinedDerivatives.getMainStateDerivativesIncrements());
        Assertions.assertEquals(expectedDerivative, fieldCombinedDerivatives.getAdditionalDerivatives()[0]);
    }
}
