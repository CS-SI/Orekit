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
package org.orekit.orbits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

class CircularLatitudeArgumentUtilityTest {

    private static final double EX = 0.1;
    private static final double EY = 0.66;
    private static final double TOLERANCE = 1e-10;

    @Test
    void testMeanToTrueAndBack() {
        // GIVEN
        final double expectedLatitudeArgument = 3.;
        // WHEN
        final double intermediateLatitudeArgument = CircularLatitudeArgumentUtility.meanToTrue(EX, EY,
                expectedLatitudeArgument);
        final double actualLatitudeArgument = CircularLatitudeArgumentUtility.trueToMean(EX, EY,
                intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument, actualLatitudeArgument, TOLERANCE);
    }

    @Test
    void testEccentricToTrueAndBack() {
        // GIVEN
        final double expectedLatitudeArgument = 3.;
        // WHEN
        final double intermediateLatitudeArgument = CircularLatitudeArgumentUtility.eccentricToTrue(EX, EY,
                expectedLatitudeArgument);
        final double actualLatitudeArgument = CircularLatitudeArgumentUtility.trueToEccentric(EX, EY,
                intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument, actualLatitudeArgument, TOLERANCE);
    }

    @Test
    void testEccentricToMeanAndBack() {
        // GIVEN
        final double expectedLatitudeArgument = 3.;
        // WHEN
        final double intermediateLatitudeArgument = CircularLatitudeArgumentUtility.eccentricToMean(EX, EY,
                expectedLatitudeArgument);
        final double actualLatitudeArgument = CircularLatitudeArgumentUtility.meanToEccentric(EX, EY,
                intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument, actualLatitudeArgument, TOLERANCE);
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testConvertAlpha(final PositionAngleType inputType) {
        // GIVEN
        final double expectedLatitudeArgument = 3.;
        final PositionAngleType intermediateType = PositionAngleType.MEAN;
        // WHEN
        final double intermediateLatitudeArgument = CircularLatitudeArgumentUtility.convertAlpha(inputType,
                expectedLatitudeArgument, EX, EY, intermediateType);
        final double actualLatitudeArgument = CircularLatitudeArgumentUtility.convertAlpha(intermediateType,
                intermediateLatitudeArgument, EX, EY, inputType);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument, actualLatitudeArgument, TOLERANCE);
    }

    @Test
    void testMeanToEccentricException() {
        // GIVEN
        final double nanLatitudeArgument = Double.NaN;
        // WHEN & THEN
        Assertions.assertThrows(OrekitException.class, () -> CircularLatitudeArgumentUtility.meanToEccentric(EX, EY,
                nanLatitudeArgument), OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LATITUDE_ARGUMENT.toString());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e3, 5e3, 1e4, 1e5, 5e5})
    void testIssue1525(final double alphaM) {
        final double ex = 0.44940492906694396;
        final double ey = 0.56419162961687;
        Assertions.assertDoesNotThrow(() -> CircularLatitudeArgumentUtility.meanToEccentric(ex, ey, alphaM));
    }

}
