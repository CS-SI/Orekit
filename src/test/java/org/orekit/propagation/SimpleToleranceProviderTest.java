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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;

import static org.junit.jupiter.api.Assertions.*;

class SimpleToleranceProviderTest {

    @Test
    void testOfConstantsCartesian() {
        // GIVEN
        final double expectedAbsolute = 1.;
        final double expectedRelative = 2.;
        // WHEN
        final ToleranceProvider toleranceProvider = new SimpleToleranceProvider(expectedAbsolute, expectedRelative);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Vector3D.ZERO, Vector3D.ZERO);
        // THEN
        assertEquals(2, actualTolerances.length);
        assertEquals(7, actualTolerances[0].length);
        assertEquals(actualTolerances[1].length, actualTolerances[0].length);
        for (int i = 0; i < 7; i++) {
            assertEquals(expectedAbsolute, actualTolerances[0][i]);
            assertEquals(expectedRelative, actualTolerances[1][i]);
        }
    }

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testOfConstantsOrbit(final OrbitType orbitType) {
        // GIVEN
        final double expectedAbsolute = 1.;
        final double expectedRelative = 2.;
        // WHEN
        final ToleranceProvider toleranceProvider = new SimpleToleranceProvider(expectedAbsolute, expectedRelative);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Mockito.mock(Orbit.class), orbitType,
                PositionAngleType.MEAN);
        // THEN
        assertEquals(2, actualTolerances.length);
        assertEquals(7, actualTolerances[0].length);
        assertEquals(actualTolerances[1].length, actualTolerances[0].length);
        for (int i = 0; i < 7; i++) {
            assertEquals(expectedAbsolute, actualTolerances[0][i]);
            assertEquals(expectedRelative, actualTolerances[1][i]);
        }
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testOfConstantsOrbit(final PositionAngleType positionAngleType) {
        // GIVEN
        final double expectedAbsolute = 1.;
        final double expectedRelative = 2.;
        // WHEN
        final ToleranceProvider toleranceProvider = new SimpleToleranceProvider(expectedAbsolute, expectedRelative);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Mockito.mock(Orbit.class), OrbitType.EQUINOCTIAL,
                positionAngleType);
        // THEN
        assertEquals(2, actualTolerances.length);
        assertEquals(7, actualTolerances[0].length);
        assertEquals(actualTolerances[1].length, actualTolerances[0].length);
        for (int i = 0; i < 7; i++) {
            assertEquals(expectedAbsolute, actualTolerances[0][i]);
            assertEquals(expectedRelative, actualTolerances[1][i]);
        }
    }
}
