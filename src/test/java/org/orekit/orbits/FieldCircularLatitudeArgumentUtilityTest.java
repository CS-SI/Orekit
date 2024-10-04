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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

class FieldCircularLatitudeArgumentUtilityTest {

    private static final Complex EX = new Complex(0.1, 0.);
    private static final Complex EY = new Complex(0.66, 0.);
    private static final double TOLERANCE = 1e-10;

    @Test
    void testMeanToTrueAndBack() {
        // GIVEN
        final Complex expectedLatitudeArgument = new Complex(3., 0.);
        // WHEN
        final Complex intermediateLatitudeArgument = FieldCircularLatitudeArgumentUtility.meanToTrue(EX, EY,
                expectedLatitudeArgument);
        final Complex actualLatitudeArgument = FieldCircularLatitudeArgumentUtility.trueToMean(EX, EY,
                intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument.getReal(), actualLatitudeArgument.getReal(), TOLERANCE);
    }

    @Test
    void testEccentricToTrueAndBack() {
        // GIVEN
        final Complex expectedLatitudeArgument = new Complex(3., 0.);
        // WHEN
        final Complex intermediateLatitudeArgument = FieldCircularLatitudeArgumentUtility.eccentricToTrue(EX, EY,
                expectedLatitudeArgument);
        final Complex actualLatitudeArgument = FieldCircularLatitudeArgumentUtility.trueToEccentric(EX, EY,
                intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument.getReal(), actualLatitudeArgument.getReal(), TOLERANCE);
    }

    @Test
    void testEccentricToMeanAndBack() {
        // GIVEN
        final Complex expectedLatitudeArgument = new Complex(3., 0.);
        // WHEN
        final Complex intermediateLatitudeArgument = FieldCircularLatitudeArgumentUtility.eccentricToMean(EX, EY,
                expectedLatitudeArgument);
        final Complex actualLatitudeArgument = FieldCircularLatitudeArgumentUtility.meanToEccentric(EX, EY,
                intermediateLatitudeArgument);
        // THEN
        Assertions.assertEquals(expectedLatitudeArgument.getReal(), actualLatitudeArgument.getReal(), TOLERANCE);
    }

    @Test
    void testMeanToTrueVersusDouble() {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        // WHEN
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.meanToTrue(
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
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.meanToEccentric(
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
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.trueToEccentric(
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
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.trueToMean(
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
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.eccentricToMean(
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
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.eccentricToTrue(
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
        Assertions.assertThrows(OrekitException.class, () -> FieldCircularLatitudeArgumentUtility.meanToEccentric(EX,
                EY, fieldNaNPositionAngle), OrekitMessages.UNABLE_TO_COMPUTE_ECCENTRIC_LATITUDE_ARGUMENT.toString());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testConvertAlphaVersusDouble(final PositionAngleType positionAngleType) {
        // GIVEN
        final Complex fieldOriginalPositionAngle = new Complex(3., 0.);
        final PositionAngleType outputType = PositionAngleType.MEAN;
        // WHEN
        final double actualConvertedPositionAngle = FieldCircularLatitudeArgumentUtility.convertAlpha(positionAngleType,
                fieldOriginalPositionAngle, EX, EY, outputType).getReal();
        // THEN
        final double expectedPositionAngle = CircularLatitudeArgumentUtility.convertAlpha(positionAngleType,
                fieldOriginalPositionAngle.getReal(), EX.getReal(), EY.getReal(), outputType);
        Assertions.assertEquals(expectedPositionAngle, actualConvertedPositionAngle, TOLERANCE);
    }

}
