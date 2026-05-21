/* Copyright 2022-2026 Romain Serra
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ConstantCartesianToleranceProviderTest {

    @Test
    void testGetTolerances() {
        // GIVEN
        final double relTol = 1e-3;
        final double dP = 1.;
        final double dV = 2.;
        final double dM = 3.;
        final CartesianToleranceProvider toleranceProvider = new ConstantCartesianToleranceProvider(dP, dV, dM, relTol);
        // WHEN
        final double[][] tolerances = toleranceProvider.getTolerances(Mockito.mock(Vector3D.class),
                Mockito.mock(Vector3D.class));
        // THEN
        assertEquals(dP, tolerances[0][0]);
        assertEquals(dP, tolerances[0][1]);
        assertEquals(dP, tolerances[0][2]);
        assertEquals(dV, tolerances[0][3]);
        assertEquals(dV, tolerances[0][4]);
        assertEquals(dV, tolerances[0][5]);
        assertEquals(dM, tolerances[0][6]);
        for (int i = 0; i < tolerances[0].length; ++i) {
            assertEquals(relTol, tolerances[1][i]);
        }
    }

}
