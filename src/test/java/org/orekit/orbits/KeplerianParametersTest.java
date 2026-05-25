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
package org.orekit.orbits;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeplerianParametersTest {

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testBackAndForthEllipticalMean(final PositionAngleType positionAngleType) {
        templateTestBackAndForthElliptical(PositionAngleType.MEAN, positionAngleType);
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testBackAndForthEllipticalEccentric(final PositionAngleType positionAngleType) {
        templateTestBackAndForthElliptical(PositionAngleType.ECCENTRIC, positionAngleType);
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testBackAndForthEllipticalTrue(final PositionAngleType positionAngleType) {
        templateTestBackAndForthElliptical(PositionAngleType.TRUE, positionAngleType);
    }

    private void templateTestBackAndForthElliptical(final PositionAngleType initialType,
                                                    final PositionAngleType intermediateType) {
        // GIVEN
        final KeplerianParameters expectedElements = new KeplerianParameters(1., 0.5, 0.5, 0.5, 0.5, 0.5, initialType);
        // WHEN
        final KeplerianParameters elements = expectedElements.withPositionAngleType(intermediateType);
        final KeplerianParameters actualElements = elements.withPositionAngleType(initialType);
        // THEN
        assertEquals(expectedElements.a(), actualElements.a());
        assertEquals(expectedElements.e(), actualElements.e());
        assertEquals(expectedElements.i(), actualElements.i());
        assertEquals(expectedElements.pa(), actualElements.pa());
        assertEquals(expectedElements.raan(), actualElements.raan());
        assertEquals(expectedElements.positionAngleType(), actualElements.positionAngleType());
        assertEquals(expectedElements.anomaly(), actualElements.anomaly(), 1e-12);
    }
}
