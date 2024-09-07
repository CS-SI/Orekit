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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;

class CartesianAdjointDynamicsProviderTest {

    @Test
    void testBuildAdditionalDerivativesProvider() {
        // GIVEN
        final CartesianCost mockedCost = Mockito.mock(CartesianCost.class);
        Mockito.when(mockedCost.getAdjointDimension()).thenReturn(1);
        Mockito.when(mockedCost.getAdjointName()).thenReturn("1");
        final CartesianAdjointDynamicsProvider dynamicsProvider = new CartesianAdjointDynamicsProvider(mockedCost);
        // WHEN
        final AdditionalDerivativesProvider derivativesProvider = dynamicsProvider.buildAdditionalDerivativesProvider();
        // THEN
        final FieldAdditionalDerivativesProvider<Complex> fieldDerivativesProvider = dynamicsProvider.buildFieldAdditionalDerivativesProvider(ComplexField.getInstance());
        Assertions.assertEquals(derivativesProvider.getDimension(), fieldDerivativesProvider.getDimension());
        Assertions.assertEquals(derivativesProvider.getName(), fieldDerivativesProvider.getName());
    }
}
