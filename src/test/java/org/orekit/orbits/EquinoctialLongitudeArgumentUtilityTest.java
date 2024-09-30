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
package org.orekit.orbits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

class EquinoctialLongitudeArgumentUtilityTest {

    private static final double EX = 0.1;
    private static final double EY = 0.66;
    private static final double TOLERANCE = 1e-10;
    
    @Test
    void testMeanToTrueAndBack() {
        // GIVEN
        final double expectedLongitudeArgument = 3.;
        // WHEN
        final double intermediateLongitudeArgument = EquinoctialLongitudeArgumentUtility.meanToTrue(EX, EY,
                expectedLongitudeArgument);
        final double actualLongitudeArgument = EquinoctialLongitudeArgumentUtility.trueToMean(EX, EY,
                intermediateLongitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLongitudeArgument, actualLongitudeArgument, TOLERANCE);
    }

    @Test
    void testEccentricToTrueAndBack() {
        // GIVEN
        final double expectedLongitudeArgument = 3.;
        // WHEN
        final double intermediateLongitudeArgument = EquinoctialLongitudeArgumentUtility.eccentricToTrue(EX, EY,
                expectedLongitudeArgument);
        final double actualLongitudeArgument = EquinoctialLongitudeArgumentUtility.trueToEccentric(EX, EY,
                intermediateLongitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLongitudeArgument, actualLongitudeArgument, TOLERANCE);
    }

    @Test
    void testEccentricToMeanAndBack() {
        // GIVEN
        final double expectedLongitudeArgument = 3.;
        // WHEN
        final double intermediateLongitudeArgument = EquinoctialLongitudeArgumentUtility.eccentricToMean(EX, EY,
                expectedLongitudeArgument);
        final double actualLongitudeArgument = EquinoctialLongitudeArgumentUtility.meanToEccentric(EX, EY,
                intermediateLongitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLongitudeArgument, actualLongitudeArgument, TOLERANCE);
    }

    @Test
    void testMeanToEccentricException() {
        // GIVEN
        final double nanLongitudeArgument = Double.NaN;
        // WHEN & THEN
        Assertions.assertThrows(OrekitException.class, () -> EquinoctialLongitudeArgumentUtility.meanToEccentric(EX, EY,
                nanLongitudeArgument), OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LONGITUDE_ARGUMENT.toString());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testConvertL(final PositionAngleType inputType) {
        // GIVEN
        final double expectedLongitudeArgument = 3.;
        final PositionAngleType intermediateType = PositionAngleType.ECCENTRIC;
        // WHEN
        final double intermediateLongitudeArgument = EquinoctialLongitudeArgumentUtility.convertL(inputType,
                expectedLongitudeArgument, EX, EY, intermediateType);
        final double actualLongitudeArgument = EquinoctialLongitudeArgumentUtility.convertL(intermediateType,
                intermediateLongitudeArgument, EX, EY, inputType);
        // THEN
        Assertions.assertEquals(expectedLongitudeArgument, actualLongitudeArgument, TOLERANCE);
    }
}
