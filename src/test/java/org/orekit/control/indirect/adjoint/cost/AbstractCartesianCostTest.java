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

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AbstractCartesianCostTest {

    @Test
    void testGetFieldAdjointVelocityNorm() {
        // GIVEN
        final Complex[] fieldAdjoint = MathArrays.buildArray(ComplexField.getInstance(), 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldAdjoint[i] = Complex.ONE.multiply(i);
        }
        final double[] adjoint = new double[fieldAdjoint.length];
        for (int i = 0; i < fieldAdjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final AbstractCartesianCost cartesianEnergy = Mockito.mock(AbstractCartesianCost.class);
        Mockito.when(cartesianEnergy.getFieldAdjointVelocityNorm(fieldAdjoint)).thenCallRealMethod();
        Mockito.when(cartesianEnergy.getAdjointVelocityNorm(adjoint)).thenCallRealMethod();
        // WHEN
        final Complex fieldNorm = cartesianEnergy.getFieldAdjointVelocityNorm(fieldAdjoint);
        // THEN
        Assertions.assertEquals(cartesianEnergy.getAdjointVelocityNorm(adjoint), fieldNorm.getReal());
    }
}
