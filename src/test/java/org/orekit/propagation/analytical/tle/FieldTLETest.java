/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.analytical.tle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

public class FieldTLETest {



    @Test
    public void testTLEFormat() {
        doTestTLEFormat(Binary64Field.getInstance());
    }

    @Test
    public void TestIssue196() {
        doTestIssue196(Binary64Field.getInstance());
    }

    @Test
    public void testSymmetry() {
        doTestSymmetry(Binary64Field.getInstance());
    }

    @Test
    public void testBug74() {
        doTestBug74(Binary64Field.getInstance());
    }

    @Test
    public void testBug77() {
        doTestBug77(Binary64Field.getInstance());
    }

    @Test
    public void testDirectConstruction() {
        doTestDirectConstruction(Binary64Field.getInstance());
    }

    @Test
    public void testGenerateAlpha5() {
        doTestGenerateAlpha5(Binary64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeSecondDerivative() {
        doTestBug77TooLargeSecondDerivative(Binary64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeBStar() {
        doTestBug77TooLargeBStar(Binary64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeEccentricity() {
        doTestBug77TooLargeEccentricity(Binary64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeSatelliteNumber1() {
        doTestBug77TooLargeSatelliteNumber1(Binary64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeSatelliteNumber2() {
        doTestBug77TooLargeSatelliteNumber2(Binary64Field.getInstance());
    }

    @Test
    public void testDifferentSatNumbers() {
        Assertions.assertThrows(OrekitException.class, () -> {
            doTestDifferentSatNumbers(Binary64Field.getInstance());
        });
    }

    @Test
    public void testChecksumOK() {
        doTestChecksumOK();
    }

    @Test
    public void testWrongChecksum1() {
        doTestWrongChecksum1();
    }

    @Test
    public void testWrongChecksum2() {
        doTestWrongChecksum2();
    }

    @Test
    public void testSatCodeCompliance() throws IOException, OrekitException, ParseException {
        doTestSatCodeCompliance(Binary64Field.getInstance());
    }

    @Test
    public void testZeroInclination() {
        doTestZeroInclination(Binary64Field.getInstance());
    }

    @Test
    public void testSymmetryAfterLeapSecondIntroduction() {
        doTestSymmetryAfterLeapSecondIntroduction(Binary64Field.getInstance());
    }

    @Test
    public void testOldTLE() {
        doTestOldTLE(Binary64Field.getInstance());
    }

    @Test
    public void testEqualTLE() {
        doTestEqualTLE(Binary64Field.getInstance());
    }

    @Test
    public void testNonEqualTLE() {
        doTestNonEqualTLE(Binary64Field.getInstance());
    }

    @Test
    public void testIssue388() {
        doTestIssue388(Binary64Field.getInstance());
    }

    @Test
    public void testIssue664NegativeRaanPa() {
        doTestIssue664NegativeRaanPa(Binary64Field.getInstance());
    }

    @Test
    public void testDifferentFields() {
        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        final DSFactory factory = new DSFactory(1, 1);
        FieldTLE<DerivativeStructure> tleA = new FieldTLE<>(factory.getDerivativeField(), line1, line2);
        FieldTLE<Binary64> tleB = new FieldTLE<>(Binary64Field.getInstance(), line1, line2);
        try {
            tleA.equals(tleB);
            Assertions.fail("an exception should have been thrown");
        } catch (Exception e) {
            // nothing to do
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestTLEFormat(Field<T> field) {

        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assertions.assertTrue(TLE.isFormatOK(line1, line2));

        FieldTLE<T> tle = new FieldTLE<T>(field, line1, line2);
        Assertions.assertEquals(27421, tle.getSatelliteNumber(), 0);
        Assertions.assertEquals(2002, tle.getLaunchYear());
        Assertions.assertEquals(21, tle.getLaunchNumber());
        Assertions.assertEquals("A", tle.getLaunchPiece());
        Assertions.assertEquals(-0.0089879, tle.getBStar(), 0);
        Assertions.assertEquals(0, tle.getEphemerisType());
        Assertions.assertEquals(98.749, FastMath.toDegrees(tle.getI().getReal()), 1e-10);
        Assertions.assertEquals(199.5121, FastMath.toDegrees(tle.getRaan().getReal()), 1e-10);
        Assertions.assertEquals(0.0001333, tle.getE().getReal(), 1e-10);
        Assertions.assertEquals(133.9522, FastMath.toDegrees(tle.getPerigeeArgument().getReal()), 1e-10);
        Assertions.assertEquals(226.1918, FastMath.toDegrees(tle.getMeanAnomaly().getReal()), 1e-10);
        Assertions.assertEquals(14.26113993, tle.getMeanMotion().getReal() * Constants.JULIAN_DAY / (2 * FastMath.PI), 0);
        Assertions.assertEquals(tle.getRevolutionNumberAtEpoch(), 6, 0);
        Assertions.assertEquals(tle.getElementNumber(), 2 , 0);

        line1 = "1 T7421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    28";
        line2 = "2 T7421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    60";
        Assertions.assertTrue(TLE.isFormatOK(line1, line2));

        tle = new FieldTLE<T>(field, line1, line2);
        Assertions.assertEquals(277421, tle.getSatelliteNumber(), 0);

        line1 = "1 I7421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    28";
        line2 = "2 I7421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    60";
        Assertions.assertFalse(TLE.isFormatOK(line1, line2));
        try {
            new FieldTLE<T>(field, line1, line2);
            Assertions.fail("an exception should have been thrown");
        } catch (NumberFormatException nfe) {
            // expected
        }

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14*26113993    62";
        Assertions.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assertions.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 10006113993    62";
        Assertions.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879 2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assertions.assertFalse(TLE.isFormatOK(line1, line2));
    }


    public <T extends CalculusFieldElement<T>> void doTestIssue196(Field<T> field) {

        String line1A = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line1B = "1 27421U 02021A   02124.48976499  -.0002147  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assertions.assertTrue(TLE.isFormatOK(line1A, line2));
        FieldTLE<T> tleA = new FieldTLE<T>(field, line1A, line2);
        Assertions.assertTrue(TLE.isFormatOK(line1B, line2));
        TLE tleB = new TLE(line1B, line2);
        Assertions.assertEquals(tleA.getSatelliteNumber(),           tleB.getSatelliteNumber(), 0);
        Assertions.assertEquals(tleA.getLaunchYear(),                tleB.getLaunchYear());
        Assertions.assertEquals(tleA.getLaunchNumber(),              tleB.getLaunchNumber());
        Assertions.assertEquals(tleA.getLaunchPiece(),               tleB.getLaunchPiece());
        Assertions.assertEquals(tleA.getBStar(),           tleB.getBStar(), 0);
        Assertions.assertEquals(tleA.getEphemerisType(),             tleB.getEphemerisType());
        Assertions.assertEquals(tleA.getI().getReal(),               tleB.getI(), 1e-10);
        Assertions.assertEquals(tleA.getRaan().getReal(),            tleB.getRaan(), 1e-10);
        Assertions.assertEquals(tleA.getE().getReal(),               tleB.getE(), 1e-10);
        Assertions.assertEquals(tleA.getPerigeeArgument().getReal(), tleB.getPerigeeArgument(), 1e-10);
        Assertions.assertEquals(tleA.getMeanAnomaly().getReal(),     tleB.getMeanAnomaly(), 1e-10);
        Assertions.assertEquals(tleA.getMeanMotion().getReal(),      tleB.getMeanMotion(), 0);
        Assertions.assertEquals(tleA.getRevolutionNumberAtEpoch(),   tleB.getRevolutionNumberAtEpoch(), 0);
        Assertions.assertEquals(tleA.getElementNumber(),             tleB.getElementNumber(), 0);

    }

    public <T extends CalculusFieldElement<T>>void doTestSymmetry(Field<T> field) {
        checkSymmetry(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                      "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        checkSymmetry(field, "1 31928U 98067BA  08269.84884916  .00114257  17652-4  13615-3 0  4412",
                      "2 31928  51.6257 175.4142 0001703  41.9031 318.2112 16.08175249 68368");
        checkSymmetry(field, "1 T7421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    28",
                      "2 T7421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    60");
    }

    private <T extends CalculusFieldElement<T>> void checkSymmetry(Field<T> field, String line1, String line2) {
        FieldTLE<T> tleRef = new FieldTLE<T>(field, line1, line2);
        FieldTLE<T> tle = new FieldTLE<T>(tleRef.getSatelliteNumber(), tleRef.getClassification(),
                          tleRef.getLaunchYear(), tleRef.getLaunchNumber(), tleRef.getLaunchPiece(),
                          tleRef.getEphemerisType(), tleRef.getElementNumber(), tleRef.getDate(),
                          tleRef.getMeanMotion(), tleRef.getMeanMotionFirstDerivative(),
                          tleRef.getMeanMotionSecondDerivative(), tleRef.getE(), tleRef.getI(),
                          tleRef.getPerigeeArgument(), tleRef.getRaan(), tleRef.getMeanAnomaly(),
                          tleRef.getRevolutionNumberAtEpoch(), tleRef.getBStar());
        Assertions.assertEquals(line1, tle.getLine1());
        Assertions.assertEquals(line2, tle.getLine2());
    }

    public <T extends CalculusFieldElement<T>> void doTestBug74(Field<T> field) {
        checkSymmetry(field, "1 00001U 00001A   12026.45833333 2.94600864  39565-9  16165-7 1    12",
                      "2 00001 127.0796 254.4522 0000000 224.9662   0.4817  0.00000000    11");
    }

    public <T extends CalculusFieldElement<T> >void doTestBug77(Field<T> field) {
        checkSymmetry(field, "1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                      "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
    }

    public <T extends CalculusFieldElement<T>> void doTestDirectConstruction(Field<T> field) {
        final T T_zero = field.getZero();
        FieldTLE<T> tleA = new FieldTLE<T>(5555, 'U', 1971, 86, "J", 0, 908,
                           new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                            new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                            TimeScalesFactory.getUTC()),
                           T_zero.add(taylorConvert(12.26882470, 1)), T_zero.add(taylorConvert(-0.00000004, 2)), T_zero.add(taylorConvert(0.00001e-9, 3)),
                           T_zero.add(0.0075476), T_zero.add(FastMath.toRadians(74.0161)), T_zero.add(FastMath.toRadians(328.9888)),
                           T_zero.add(FastMath.toRadians(228.9750)), T_zero.add(FastMath.toRadians(30.6709)), 80454, 0.01234e-9);
        FieldTLE<T> tleB =  new FieldTLE<T>(field, "1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                            "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
        Assertions.assertEquals(tleA.getSatelliteNumber(),           tleB.getSatelliteNumber(), 0);
        Assertions.assertEquals(tleA.getLaunchYear(),                tleB.getLaunchYear());
        Assertions.assertEquals(tleA.getLaunchNumber(),              tleB.getLaunchNumber());
        Assertions.assertEquals(tleA.getLaunchPiece(),               tleB.getLaunchPiece());
        Assertions.assertEquals(tleA.getBStar()          ,           tleB.getBStar(), 0);
        Assertions.assertEquals(tleA.getEphemerisType(),             tleB.getEphemerisType());
        Assertions.assertEquals(tleA.getI().getReal(),               tleB.getI().getReal(), 1e-10);
        Assertions.assertEquals(tleA.getRaan().getReal(),            tleB.getRaan().getReal(), 1e-10);
        Assertions.assertEquals(tleA.getE().getReal(),               tleB.getE().getReal(), 1e-10);
        Assertions.assertEquals(tleA.getPerigeeArgument().getReal(), tleB.getPerigeeArgument().getReal(), 1e-10);
        Assertions.assertEquals(tleA.getMeanAnomaly().getReal(),     tleB.getMeanAnomaly().getReal(), 1e-10);
        Assertions.assertEquals(tleA.getMeanMotion().getReal(),      tleB.getMeanMotion().getReal(), 0);
        Assertions.assertEquals(tleA.getRevolutionNumberAtEpoch(),   tleB.getRevolutionNumberAtEpoch(), 0);
        Assertions.assertEquals(tleA.getElementNumber(),             tleB.getElementNumber(), 0);
    }

    public <T extends CalculusFieldElement<T>> void doTestGenerateAlpha5(Field<T> field) {
        final T T_zero = field.getZero();
        FieldTLE<T> tle = new FieldTLE<T>(339999, 'U', 1971, 86, "J", 0, 908,
                          new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                                   new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                                   TimeScalesFactory.getUTC()),
                          T_zero.add(taylorConvert(12.26882470, 1)),
                          T_zero.add(taylorConvert(-0.00000004, 2)),
                          T_zero.add(taylorConvert(0.00001e-9, 3)),
                          T_zero.add(0.0075476), T_zero.add(FastMath.toRadians(74.0161)),
                          T_zero.add(FastMath.toRadians(328.9888)),
                          T_zero.add(FastMath.toRadians(228.9750)),
                          T_zero.add(FastMath.toRadians(30.6709)),
                          80454, 0.01234e-9);
        Assertions.assertEquals("1 Z9999U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9088", tle.getLine1());
        Assertions.assertEquals("2 Z9999  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804541", tle.getLine2());
    }

    public <T extends CalculusFieldElement<T>> void doTestBug77TooLargeSecondDerivative(Field<T> field) {
        try {
            final T T_zero = field.getZero();
            FieldTLE<T> tle = new FieldTLE<T>(5555, 'U', 1971, 86, "J", 0, 908,
                              new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              T_zero.add(taylorConvert(12.26882470, 1)), T_zero.add(taylorConvert(-0.00000004, 2)), T_zero.add(taylorConvert(0.99999e11, 3)),
                              T_zero.add(0.0075476), T_zero.add(FastMath.toRadians(74.0161)), T_zero.add(FastMath.toRadians(328.9888)),
                              T_zero.add(FastMath.toRadians(228.9750)), T_zero.add(FastMath.toRadians(30.6709)), 80454, 0.01234e-9);
            tle.getLine1();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("meanMotionSecondDerivative", oe.getParts()[1]);
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestBug77TooLargeBStar(Field<T> field) {
        try {
            final T T_zero = field.getZero();
            FieldTLE<T> tle = new FieldTLE<T>(5555, 'U', 1971, 86, "J", 0, 908,
                              new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              T_zero.add(taylorConvert(12.26882470, 1)), T_zero.add(taylorConvert(-0.00000004, 2)), T_zero.add(taylorConvert(0.00001e-9, 3)),
                              T_zero.add(0.0075476), T_zero.add(FastMath.toRadians(74.0161)), T_zero.add(FastMath.toRadians(328.9888)),
                              T_zero.add(FastMath.toRadians(228.9750)), T_zero.add(FastMath.toRadians(30.6709)), 80454, 0.99999e11);
            tle.getLine1();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("B*", oe.getParts()[1]);
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestBug77TooLargeEccentricity(Field<T> field) {
        try {
            final T T_zero = field.getZero();
            FieldTLE<T> tle = new FieldTLE<T>(5555, 'U', 1971, 86, "J", 0, 908,
                              new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              T_zero.add(taylorConvert(12.26882470, 1)), T_zero.add(taylorConvert(-0.00000004, 2)), T_zero.add(taylorConvert(0.00001e-9, 3)),
                              T_zero.add(1.0075476), T_zero.add(FastMath.toRadians(74.0161)), T_zero.add(FastMath.toRadians(328.9888)),
                              T_zero.add(FastMath.toRadians(228.9750)), T_zero.add(FastMath.toRadians(30.6709)), 80454, 0.01234e-9);
            tle.getLine2();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("eccentricity", oe.getParts()[1]);
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestBug77TooLargeSatelliteNumber1(Field<T> field) {
        try {
            final T T_zero = field.getZero();
            FieldTLE<T> tle = new FieldTLE<T>(1000000, 'U', 1971, 86, "J", 0, 908,
                              new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              T_zero.add(taylorConvert(12.26882470, 1)),  T_zero.add(taylorConvert(-0.00000004, 2)),  T_zero.add(taylorConvert(0.00001e-9, 3)),
                              T_zero.add(0.0075476),  T_zero.add(FastMath.toRadians(74.0161)),  T_zero.add(FastMath.toRadians(328.9888)),
                              T_zero.add(FastMath.toRadians(228.9750)),  T_zero.add(FastMath.toRadians(30.6709)), 80454, 0.01234e-9);
            tle.getLine1();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(1000000, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("satelliteNumber-1", oe.getParts()[1]);
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestBug77TooLargeSatelliteNumber2(Field<T> field) {
        try {
            final T T_zero = field.getZero();
            FieldTLE<T> tle = new FieldTLE<T>(1000000, 'U', 1971, 86, "J", 0, 908,
                              new FieldAbsoluteDate<T>(field, new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              T_zero.add(taylorConvert(12.26882470, 1)), T_zero.add(taylorConvert(-0.00000004, 2)), T_zero.add(taylorConvert(0.00001e-9, 3)),
                              T_zero.add(0.0075476), T_zero.add(FastMath.toRadians(74.0161)), T_zero.add(FastMath.toRadians(328.9888)),
                              T_zero.add(FastMath.toRadians(228.9750)), T_zero.add(FastMath.toRadians(30.6709)), 80454, 0.01234e-9);
            tle.getLine2();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(1000000, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("satelliteNumber-2", oe.getParts()[1]);
        }
    }

    final double taylorConvert(final double m, final int n) {
        // convert one term of TLE mean motion Taylor series
        return  m * 2 * FastMath.PI * CombinatoricsUtils.factorial(n) / FastMath.pow(Constants.JULIAN_DAY, n);
    }

    public <T extends CalculusFieldElement<T>> void doTestDifferentSatNumbers(Field<T> field) {
        new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                               "2 27422  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    public void doTestChecksumOK() {
        FieldTLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                            "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    public void doTestWrongChecksum1() {
        try {
            FieldTLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_CHECKSUM_ERROR, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("0", oe.getParts()[1]);
            Assertions.assertEquals("1", oe.getParts()[2]);
            Assertions.assertEquals("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                                oe.getParts()[3]);
        }
    }

    public void doTestWrongChecksum2() {
        try {
            FieldTLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    61");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TLE_CHECKSUM_ERROR, oe.getSpecifier());
            Assertions.assertEquals(2, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("2", oe.getParts()[1]);
            Assertions.assertEquals("1", oe.getParts()[2]);
            Assertions.assertEquals("2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    61",
                                oe.getParts()[3]);
        }
    }

    public <T extends CalculusFieldElement<T>>void doTestSatCodeCompliance(Field<T> field) throws IOException, OrekitException, ParseException {

        BufferedReader rEntry = null;
        BufferedReader rResults = null;
        final T T_zero = field.getZero();

        InputStream inEntry =
            FieldTLETest.class.getResourceAsStream("/tle/extrapolationTest-data/SatCode-entry");
        rEntry = new BufferedReader(new InputStreamReader(inEntry));

        try {
            InputStream inResults =
                FieldTLETest.class.getResourceAsStream("/tle/extrapolationTest-data/SatCode-results");
            rResults = new BufferedReader(new InputStreamReader(inResults));

            try {
                double cumulated = 0; // sum of all differences between test cases and OREKIT results
                boolean stop = false;

                String rline = rResults.readLine();

                while (!stop) {
                    if (rline == null) break;

                    String[] title = rline.split(" ");

                    if (title[0].matches("r")) {

                        String eline;
                        int count = 0;
                        String[] header = new String[4];
                        for (eline = rEntry.readLine(); (eline != null) && (eline.charAt(0)=='#'); eline = rEntry.readLine()) {
                            header[count++] = eline;
                        }
                        String line1 = eline;
                        String line2 = rEntry.readLine();
                        Assertions.assertTrue(TLE.isFormatOK(line1, line2));

                        FieldTLE<T> tle = new FieldTLE<T>(field, line1, line2);

                        int satNum = Integer.parseInt(title[1]);
                        Assertions.assertTrue(satNum==tle.getSatelliteNumber());
                        final T[] parameters;
                        parameters = MathArrays.buildArray(field, 1);
                        parameters[0] = field.getZero().add(tle.getBStar());
                        FieldTLEPropagator<T> ex = FieldTLEPropagator.selectExtrapolator(tle, parameters);
                        for (rline = rResults.readLine(); (rline!=null)&&(rline.charAt(0)!='r'); rline = rResults.readLine()) {

                            String[] data = rline.split(" ");
                            double minFromStart = Double.parseDouble(data[0]);
                            T pX = T_zero.add(1000*Double.parseDouble(data[1]));
                            T pY = T_zero.add(1000*Double.parseDouble(data[2]));
                            T pZ = T_zero.add(1000*Double.parseDouble(data[3]));
                            T vX = T_zero.add(1000*Double.parseDouble(data[4]));
                            T vY = T_zero.add(1000*Double.parseDouble(data[5]));
                            T vZ = T_zero.add(1000*Double.parseDouble(data[6]));
                            FieldVector3D<T> testPos = new FieldVector3D<T>(pX, pY, pZ);
                            FieldVector3D<T> testVel = new FieldVector3D<T>(vX, vY, vZ);

                            FieldAbsoluteDate<T> date = tle.getDate().shiftedBy(minFromStart * 60);
                            FieldPVCoordinates<T> results = ex.getPVCoordinates(date, parameters);
                            double normDifPos = testPos.subtract(results.getPosition()).getNorm().getReal();
                            double normDifVel = testVel.subtract(results.getVelocity()).getNorm().getReal();

                            cumulated += normDifPos;
                            Assertions.assertEquals(0, normDifPos, 2e-3);
                            Assertions.assertEquals(0, normDifVel, 7e-4);


                        }
                    }
                }
                Assertions.assertEquals(0, cumulated, 0.026);
            } finally {
                if (rResults != null) {
                    rResults.close();
                }
            }
        } finally {
            if (rEntry != null) {
                rEntry.close();
            }
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestZeroInclination(Field<T> field) {
        FieldTLE<T> tle = new FieldTLE<T>(field,"1 26451U 00043A   10130.13784012 -.00000276  00000-0  10000-3 0  3866",
                                                "2 26451 000.0000 266.1044 0001893 160.7642 152.5985 01.00271160 35865");
        final T[] parameters;
        parameters = MathArrays.buildArray(field, 1);
        parameters[0].add(tle.getBStar());
        FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tle, parameters);
        FieldPVCoordinates<T> pv = propagator.propagate(tle.getDate().shiftedBy(100)).getPVCoordinates();
        Assertions.assertEquals(42171546.979560345, pv.getPosition().getNorm().getReal(), 1.0e-3);
        Assertions.assertEquals(3074.1890089357994, pv.getVelocity().getNorm().getReal(), 1.0e-6);
    }

    public <T extends CalculusFieldElement<T>> void doTestSymmetryAfterLeapSecondIntroduction(Field<T> field) {
        checkSymmetry(field, "1 34602U 09013A   12187.35117436  .00002472  18981-5  42406-5 0  9995",
                             "2 34602  96.5991 210.0210 0006808 112.8142 247.3865 16.06008103193411");
    }

    public <T extends CalculusFieldElement<T>> void doTestOldTLE(Field<T> field) {
        String line1 = "1 15427U          85091.94293084 0.00000051  00000+0  32913-4 0   179";
        String line2 = "2 15427  98.9385  46.0219 0015502 321.4354  38.5705 14.11363211 15580";
        Assertions.assertTrue(TLE.isFormatOK(line1, line2));
        FieldTLE<T> tle = new FieldTLE<T>(field, line1, line2);
        Assertions.assertEquals(15427, tle.getSatelliteNumber());
        Assertions.assertEquals(0.00000051,
                            tle.getMeanMotionFirstDerivative().getReal() * Constants.JULIAN_DAY * Constants.JULIAN_DAY / (4 * FastMath.PI),
                            1.0e-15);
    }

    public <T extends CalculusFieldElement<T>> void doTestEqualTLE(Field<T> field) {
        FieldTLE<T> tleA = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                  "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        FieldTLE<T> tleB = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                  "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        Assertions.assertTrue(tleA.equals(tleB));
    }

    public <T extends CalculusFieldElement<T>> void doTestNonEqualTLE(Field<T> field) {
        FieldTLE<T> tleA = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                  "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        FieldTLE<T> tleB = new FieldTLE<T>(field, "1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                                                  "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
        Assertions.assertFalse(tleA.equals(tleB));
    }

    public <T extends CalculusFieldElement<T>> void doTestIssue388(Field<T> field) {
        final T T_zero = field.getZero();
        FieldTLE<T> tleRef = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        FieldTLE<T> tleOriginal = new FieldTLE<T>(27421, 'U', 2002, 21, "A", TLE.DEFAULT, 2,
                                  new FieldAbsoluteDate<T>(field, "2002-05-04T11:45:15.695", TimeScalesFactory.getUTC()),
                                  T_zero.add(FastMath.toRadians(14.26113993 * 360 / Constants.JULIAN_DAY)),
                                  T_zero.add(FastMath.toRadians(-.00021470 * 360 * 2 / (Constants.JULIAN_DAY * Constants.JULIAN_DAY))),
                                  T_zero.add(FastMath.toRadians(0.0)),
                                  T_zero.add(1.333E-4), T_zero.add(FastMath.toRadians(98.7490)),
                                  T_zero.add(FastMath.toRadians(133.9522)), T_zero.add(FastMath.toRadians(199.5121)), T_zero.add(FastMath.toRadians(226.1918)),
                                  6, -0.0089879);
        Assertions.assertEquals(tleRef.getLine1(), tleOriginal.getLine1());
        Assertions.assertEquals(tleRef.getLine2(), tleOriginal.getLine2());
        FieldTLE<T> changedBStar = new FieldTLE<T>(27421, 'U', 2002, 21, "A", TLE.DEFAULT, 2,
                                   new FieldAbsoluteDate<T>(field, "2002-05-04T11:45:15.695", TimeScalesFactory.getUTC()),
                                   T_zero.add(FastMath.toRadians(14.26113993 * 360 / Constants.JULIAN_DAY)),
                                   T_zero.add(FastMath.toRadians(-.00021470 * 360 * 2 / (Constants.JULIAN_DAY * Constants.JULIAN_DAY))),
                                   T_zero.add(FastMath.toRadians(0.0)),
                                   T_zero.add(1.333E-4), T_zero.add(FastMath.toRadians(98.7490)),
                                   T_zero.add(FastMath.toRadians(133.9522)), T_zero.add(FastMath.toRadians(199.5121)), T_zero.add(FastMath.toRadians(226.1918)),
                                   6, 1.0e-4);
        Assertions.assertEquals(tleRef.getLine1().replace("-89879-2", " 10000-3"), changedBStar.getLine1());
        Assertions.assertEquals(tleRef.getLine2(), changedBStar.getLine2());
        Assertions.assertEquals(1.0e-4, new FieldTLE<T>(field, changedBStar.getLine1(), changedBStar.getLine2()).getBStar(), 1.0e-15);
    }

    public <T extends CalculusFieldElement<T>> void doTestIssue664NegativeRaanPa(Field<T> field) {
        final T T_zero = field.getZero();
        FieldTLE<T> tle = new FieldTLE<T>(99999, 'X', 2020, 42, "F", 0, 999,
                new FieldAbsoluteDate<T>(field, "2020-01-01T01:00:00.000", TimeScalesFactory.getUTC()), T_zero.add(0.0011010400252833312), T_zero.add(0.0),
                T_zero.add(0.0), T_zero.add(0.0016310523359516962), T_zero.add(1.6999188604164899),
                T_zero.add(-3.219351286726724), T_zero.add(-2.096689019811356),
                T_zero.add(2.157567545975006), 1, 1e-05);
        // Comparing with TLE strings generated in Orekit Python after forcing the RAAN
        // and PA to the [0, 2*Pi] range
        Assertions.assertEquals(tle.getLine1(), "1 99999X 20042F   20001.04166667  .00000000  00000-0  10000-4 0  9997");
        Assertions.assertEquals(tle.getLine2(), "2 99999  97.3982 239.8686 0016311 175.5448 123.6195 15.14038717    18");
    }

    @Test
    public void testStateToTleISS() {
        doTestStateToTleISS(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestStateToTleISS(final Field<T> field) {

        // Initialize TLE
        final FieldTLE<T> tleISS = new FieldTLE<>(field, "1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                                         "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tleISS, tleISS.getParameters(field));

        // State at TLE epoch
        final FieldSpacecraftState<T> state = propagator.propagate(tleISS.getDate());

        // TLE generation algorithm
        final TleGenerationAlgorithm algorithm = new FixedPointTleGenerationAlgorithm();

        // Convert to TLE
        final FieldTLE<T> rebuilt = FieldTLE.stateToTLE(state, tleISS, algorithm);

        // Verify
        final double eps = 1.0e-7;
        Assertions.assertEquals(tleISS.getSatelliteNumber(),           rebuilt.getSatelliteNumber());
        Assertions.assertEquals(tleISS.getClassification(),            rebuilt.getClassification());
        Assertions.assertEquals(tleISS.getLaunchYear(),                rebuilt.getLaunchYear());
        Assertions.assertEquals(tleISS.getLaunchNumber(),              rebuilt.getLaunchNumber());
        Assertions.assertEquals(tleISS.getLaunchPiece(),               rebuilt.getLaunchPiece());
        Assertions.assertEquals(tleISS.getElementNumber(),             rebuilt.getElementNumber());
        Assertions.assertEquals(tleISS.getRevolutionNumberAtEpoch(),   rebuilt.getRevolutionNumberAtEpoch());
        Assertions.assertEquals(tleISS.getMeanMotion().getReal(),      rebuilt.getMeanMotion().getReal(),      eps * tleISS.getMeanMotion().getReal());
        Assertions.assertEquals(tleISS.getE().getReal(),               rebuilt.getE().getReal(),               eps * tleISS.getE().getReal());
        Assertions.assertEquals(tleISS.getI().getReal(),               rebuilt.getI().getReal(),               eps * tleISS.getI().getReal());
        Assertions.assertEquals(tleISS.getPerigeeArgument().getReal(), rebuilt.getPerigeeArgument().getReal(), eps * tleISS.getPerigeeArgument().getReal());
        Assertions.assertEquals(tleISS.getRaan().getReal(),            rebuilt.getRaan().getReal(),            eps * tleISS.getRaan().getReal());
        Assertions.assertEquals(tleISS.getMeanAnomaly().getReal(),     rebuilt.getMeanAnomaly().getReal(),     eps * tleISS.getMeanAnomaly().getReal());
        Assertions.assertEquals(tleISS.getMeanAnomaly().getReal(),     rebuilt.getMeanAnomaly().getReal(),     eps * tleISS.getMeanAnomaly().getReal());
        Assertions.assertEquals(tleISS.getBStar(),                     rebuilt.getBStar(),                     eps * tleISS.getBStar());
    }

    @Test
    public void testToTLE() {
        doTestToTLE(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestToTLE(final Field<T> field) {
        final TLE tle = new TLE("1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");
        final FieldTLE<T> fieldTle = new FieldTLE<T>(field, tle.getLine1(), tle.getLine2());
        final TLE rebuilt = fieldTle.toTLE();
        Assertions.assertTrue(rebuilt.equals(tle));
        Assertions.assertEquals(tle.toString(), rebuilt.toString());
    }

    @Test
    void roundToNextDayError() {
        //Given
        final Field<Binary64> field = Binary64Field.getInstance();
        final Binary64        zero  = field.getZero();

        final FieldAbsoluteDate<Binary64> tleDate =
                new FieldAbsoluteDate<>(field, new AbsoluteDate("2022-01-01T23:59:59.99999999", TimeScalesFactory.getUTC()));

        final FieldTLE<Binary64> tle =
                new FieldTLE<>(99999, 'U', 2022, 999, "A", 0, 1, tleDate, zero, zero, zero, zero, zero, zero, zero, zero, 99,
                               11606 * 1e-4, TimeScalesFactory.getUTC());

        //When
        final FieldAbsoluteDate<Binary64> returnedDate = tle.getDate();

        //Then
        // Assert that TLE class did not round the date to the next day
        Assertions.assertEquals(tleDate, returnedDate);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}