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
package org.orekit.propagation;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractMatricesHarvesterTest {

    @Test
    void testToArray() {
        // GIVEN
        final int stateDimension = 7;
        final AbstractMatricesHarvester moockedHarvester = mock();
        when(moockedHarvester.toArray(Mockito.any())).thenCallRealMethod();
        when(moockedHarvester.toSquareMatrix(Mockito.any())).thenCallRealMethod();
        when(moockedHarvester.getStateDimension()).thenReturn(stateDimension);
        final RealMatrix expectedMatrix = MatrixUtils.createRealIdentityMatrix(stateDimension);
        expectedMatrix.setEntry(0, 2, 42);
        expectedMatrix.setEntry(1, 2, 42);
        expectedMatrix.setEntry(4, 3, 42);
        // WHEN
        final double[] array = moockedHarvester.toArray(expectedMatrix.getData());
        final RealMatrix actualMatrix = moockedHarvester.toSquareMatrix(array);
        // THEN
        assertEquals(0., actualMatrix.subtract(expectedMatrix).getNorm1());
    }
}