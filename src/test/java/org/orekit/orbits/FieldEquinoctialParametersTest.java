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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldEquinoctialParametersTest {

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
        final EquinoctialParameters elements = new EquinoctialParameters(1., 0.5, 0.5, 0.5, 0.5, 0.5, initialType);
        final FieldEquinoctialParameters<Binary64> expectedElements = new FieldEquinoctialParameters<>(Binary64Field.getInstance(), elements);
        // WHEN
        final FieldEquinoctialParameters<Binary64> converted = expectedElements.withPositionAngleType(intermediateType);
        final FieldEquinoctialParameters<Binary64> actualElements = converted.withPositionAngleType(initialType);
        // THEN
        assertEquals(expectedElements.a(), actualElements.a());
        assertEquals(expectedElements.ex(), actualElements.ex());
        assertEquals(expectedElements.ey(), actualElements.ey());
        assertEquals(expectedElements.hx(), actualElements.hx());
        assertEquals(expectedElements.hy(), actualElements.hy());
        assertEquals(expectedElements.positionAngleType(), actualElements.positionAngleType());
        assertEquals(expectedElements.longitudeArgument().getReal(), actualElements.longitudeArgument().getReal(), 1e-12);
    }

    @Test
    void testToEquinoctialElements() {
        // GIVEN
        final EquinoctialParameters expectedElements = new EquinoctialParameters(1., 0.5, 0.5, 0.5, 0.5, 0.5,
                PositionAngleType.TRUE);
        // WHEN
        final FieldEquinoctialParameters<Binary64> fieldElements = new FieldEquinoctialParameters<>(Binary64Field.getInstance(),
                expectedElements);
        final EquinoctialParameters actualElements = fieldElements.toEquinoctialElements();
        // THEN
        assertEquals(expectedElements, actualElements);
    }
}
