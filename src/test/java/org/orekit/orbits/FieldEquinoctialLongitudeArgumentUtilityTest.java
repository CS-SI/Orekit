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

import org.hipparchus.complex.Complex;
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

class FieldEquinoctialLongitudeArgumentUtilityTest {

    private static final Complex EX = new Complex(0.1, 0.);
    private static final Complex EY = new Complex(0.66, 0.);
    private static final double TOLERANCE = 1e-10;

    @Test
    void testMeanToTrueAndBack() {
        // GIVEN
        final Complex expectedLatitudeArgument = new Complex(3., 0.);
        // WHEN
        final Complex intermediateLatitudeArgument = FieldEquinoctialLongitudeArgumentUtility.meanToTrue(
                EX, EY, expectedLatitudeArgument);
        final Complex actualLatitudeArgument = FieldEquinoctialLongitudeArgumentUtility.trueToMean(
                EX, EY, intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument.getReal(), actualLatitudeArgument.getReal(), TOLERANCE);
    }

    @Test
    void testEccentricToTrueAndBack() {
        // GIVEN
        final Complex expectedLatitudeArgument = new Complex(3., 0.);
        // WHEN
        final Complex intermediateLatitudeArgument = FieldEquinoctialLongitudeArgumentUtility
                .eccentricToTrue(EX, EY, expectedLatitudeArgument);
        final Complex actualLatitudeArgument = FieldEquinoctialLongitudeArgumentUtility
                .trueToEccentric(EX, EY, intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument.getReal(), actualLatitudeArgument.getReal(), TOLERANCE);
    }

    @Test
    void testEccentricToMeanAndBack() {
        // GIVEN
        final Complex expectedLatitudeArgument = new Complex(3., 0.);
        // WHEN
        final Complex intermediateLatitudeArgument = FieldEquinoctialLongitudeArgumentUtility
                .eccentricToMean(EX, EY, expectedLatitudeArgument);
        final Complex actualLatitudeArgument = FieldEquinoctialLongitudeArgumentUtility
                .meanToEccentric(EX, EY, intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument.getReal(), actualLatitudeArgument.getReal(), TOLERANCE);
    }

    @Test
    void testMeanToTrueVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.meanToTrue(
                EX, EY, fieldOriginalPositionAngle).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.meanToTrue(
                EX.getReal(), EY.getReal(), fieldOriginalPositionAngle.getReal());
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @Test
    void testMeanToEccentricVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.meanToEccentric(
                EX, EY, fieldOriginalPositionAngle).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.meanToEccentric(
                EX.getReal(), EY.getReal(), fieldOriginalPositionAngle.getReal());
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @Test
    void testTrueToEccentricVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.trueToEccentric(
                EX, EY, fieldOriginalPositionAngle).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.trueToEccentric(
                EX.getReal(), EY.getReal(), fieldOriginalPositionAngle.getReal());
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @Test
    void testTrueToMeanVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.trueToMean(
                EX, EY, fieldOriginalPositionAngle).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.trueToMean(
                EX.getReal(), EY.getReal(), fieldOriginalPositionAngle.getReal());
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @Test
    void testEccentricToMeanVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.eccentricToMean(
                EX, EY, fieldOriginalPositionAngle).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.eccentricToMean(
                EX.getReal(), EY.getReal(), fieldOriginalPositionAngle.getReal());
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @Test
    void testEccentricToTrueVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.eccentricToTrue(
                EX, EY, fieldOriginalPositionAngle).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.eccentricToTrue(
                EX.getReal(), EY.getReal(), fieldOriginalPositionAngle.getReal());
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @Test
    void testMeanToEccentricException() {
        // GIVEN
        final Complex fieldNaNPositionAngle = Complex.NaN;
        // WHEN & THEN
        Assertions.assertThrows(OrekitException.class, () -> FieldEquinoctialLongitudeArgumentUtility.meanToEccentric(EX,
                EY, fieldNaNPositionAngle), OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LONGITUDE_ARGUMENT.toString());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testConvertLVersusDouble(final PositionAngleType positionAngleType) {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        final PositionAngleType outputType = PositionAngleType.ECCENTRIC;
        // WHEN
        final double actualConvertedPositionAngle = FieldEquinoctialLongitudeArgumentUtility.convertL(positionAngleType,
                fieldOriginalPositionAngle, EX, EY, outputType).getReal();
        // THEN
        final double expectedPositionAngle = EquinoctialLongitudeArgumentUtility.convertL(positionAngleType,
                fieldOriginalPositionAngle.getReal(), EX.getReal(), EY.getReal(), outputType);
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e3, 5e3, 1e4, 1e5, 5e5})
    void testIssue1525(final double alphaM) {
        final double ex = 0.44940492906694396;
        final double ey = 0.56419162961687;
        final Binary64 zero = Binary64.ZERO;
        Assertions.assertDoesNotThrow(() -> FieldEquinoctialLongitudeArgumentUtility
                .meanToEccentric(zero.newInstance(ex), zero.newInstance(ey), zero.newInstance(alphaM)));
    }

}
