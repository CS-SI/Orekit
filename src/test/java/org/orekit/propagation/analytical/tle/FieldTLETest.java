/* Copyright 2002-2022 CS GROUP
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


import static org.junit.Assert.fail;

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
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldTLETest {



    @Test
    public void testTLEFormat() {
        doTestTLEFormat(Decimal64Field.getInstance());
    }

    @Test
    public void TestIssue196() {
        doTestIssue196(Decimal64Field.getInstance());
    }

    @Test
    public void testSymmetry() {
        doTestSymmetry(Decimal64Field.getInstance());
    }

    @Test
    public void testBug74() {
        doTestBug74(Decimal64Field.getInstance());
    }

    @Test
    public void testBug77() {
        doTestBug77(Decimal64Field.getInstance());
    }

    @Test
    public void testDirectConstruction() {
        doTestDirectConstruction(Decimal64Field.getInstance());
    }

    @Test
    public void testGenerateAlpha5() {
        doTestGenerateAlpha5(Decimal64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeSecondDerivative() {
        doTestBug77TooLargeSecondDerivative(Decimal64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeBStar() {
        doTestBug77TooLargeBStar(Decimal64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeEccentricity() {
        doTestBug77TooLargeEccentricity(Decimal64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeSatelliteNumber1() {
        doTestBug77TooLargeSatelliteNumber1(Decimal64Field.getInstance());
    }

    @Test
    public void testBug77TooLargeSatelliteNumber2() {
        doTestBug77TooLargeSatelliteNumber2(Decimal64Field.getInstance());
    }

    @Test(expected=OrekitException.class)
    public void testDifferentSatNumbers() {
        doTestDifferentSatNumbers(Decimal64Field.getInstance());
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
        doTestSatCodeCompliance(Decimal64Field.getInstance());
    }

    @Test
    public void testZeroInclination() {
        doTestZeroInclination(Decimal64Field.getInstance());
    }

    @Test
    public void testSymmetryAfterLeapSecondIntroduction() {
        doTestSymmetryAfterLeapSecondIntroduction(Decimal64Field.getInstance());
    }

    @Test
    public void testOldTLE() {
        doTestOldTLE(Decimal64Field.getInstance());
    }

    @Test
    public void testEqualTLE() {
        doTestEqualTLE(Decimal64Field.getInstance());
    }

    @Test
    public void testNonEqualTLE() {
        doTestNonEqualTLE(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue388() {
        doTestIssue388(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue664NegativeRaanPa() {
        doTestIssue664NegativeRaanPa(Decimal64Field.getInstance());
    }

    @Test
    public void testDifferentFields() {
        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        final DSFactory factory = new DSFactory(1, 1);
        FieldTLE<DerivativeStructure> tleA = new FieldTLE<>(factory.getDerivativeField(), line1, line2);
        FieldTLE<Decimal64> tleB = new FieldTLE<>(Decimal64Field.getInstance(), line1, line2);
        try {
            tleA.equals(tleB);
            fail("an exception should have been thrown");
        } catch (Exception e) {
            // nothing to do
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestTLEFormat(Field<T> field) {

        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        FieldTLE<T> tle = new FieldTLE<T>(field, line1, line2);
        Assert.assertEquals(27421, tle.getSatelliteNumber(), 0);
        Assert.assertEquals(2002, tle.getLaunchYear());
        Assert.assertEquals(21, tle.getLaunchNumber());
        Assert.assertEquals("A", tle.getLaunchPiece());
        Assert.assertEquals(-0.0089879, tle.getBStar(), 0);
        Assert.assertEquals(0, tle.getEphemerisType());
        Assert.assertEquals(98.749, FastMath.toDegrees(tle.getI().getReal()), 1e-10);
        Assert.assertEquals(199.5121, FastMath.toDegrees(tle.getRaan().getReal()), 1e-10);
        Assert.assertEquals(0.0001333, tle.getE().getReal(), 1e-10);
        Assert.assertEquals(133.9522, FastMath.toDegrees(tle.getPerigeeArgument().getReal()), 1e-10);
        Assert.assertEquals(226.1918, FastMath.toDegrees(tle.getMeanAnomaly().getReal()), 1e-10);
        Assert.assertEquals(14.26113993, tle.getMeanMotion().getReal() * Constants.JULIAN_DAY / (2 * FastMath.PI), 0);
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), 6, 0);
        Assert.assertEquals(tle.getElementNumber(), 2 , 0);

        line1 = "1 T7421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    28";
        line2 = "2 T7421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    60";
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        tle = new FieldTLE<T>(field, line1, line2);
        Assert.assertEquals(277421, tle.getSatelliteNumber(), 0);

        line1 = "1 I7421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    28";
        line2 = "2 I7421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    60";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));
        try {
            new FieldTLE<T>(field, line1, line2);
            Assert.fail("an exception should have been thrown");
        } catch (NumberFormatException nfe) {
            // expected
        }

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14*26113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 10006113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879 2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));
    }


    public <T extends CalculusFieldElement<T>> void doTestIssue196(Field<T> field) {

        String line1A = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line1B = "1 27421U 02021A   02124.48976499  -.0002147  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assert.assertTrue(TLE.isFormatOK(line1A, line2));
        FieldTLE<T> tleA = new FieldTLE<T>(field, line1A, line2);
        Assert.assertTrue(TLE.isFormatOK(line1B, line2));
        TLE tleB = new TLE(line1B, line2);
        Assert.assertEquals(tleA.getSatelliteNumber(),           tleB.getSatelliteNumber(), 0);
        Assert.assertEquals(tleA.getLaunchYear(),                tleB.getLaunchYear());
        Assert.assertEquals(tleA.getLaunchNumber(),              tleB.getLaunchNumber());
        Assert.assertEquals(tleA.getLaunchPiece(),               tleB.getLaunchPiece());
        Assert.assertEquals(tleA.getBStar(),           tleB.getBStar(), 0);
        Assert.assertEquals(tleA.getEphemerisType(),             tleB.getEphemerisType());
        Assert.assertEquals(tleA.getI().getReal(),               tleB.getI(), 1e-10);
        Assert.assertEquals(tleA.getRaan().getReal(),            tleB.getRaan(), 1e-10);
        Assert.assertEquals(tleA.getE().getReal(),               tleB.getE(), 1e-10);
        Assert.assertEquals(tleA.getPerigeeArgument().getReal(), tleB.getPerigeeArgument(), 1e-10);
        Assert.assertEquals(tleA.getMeanAnomaly().getReal(),     tleB.getMeanAnomaly(), 1e-10);
        Assert.assertEquals(tleA.getMeanMotion().getReal(),      tleB.getMeanMotion(), 0);
        Assert.assertEquals(tleA.getRevolutionNumberAtEpoch(),   tleB.getRevolutionNumberAtEpoch(), 0);
        Assert.assertEquals(tleA.getElementNumber(),             tleB.getElementNumber(), 0);

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
        Assert.assertEquals(line1, tle.getLine1());
        Assert.assertEquals(line2, tle.getLine2());
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
        Assert.assertEquals(tleA.getSatelliteNumber(),           tleB.getSatelliteNumber(), 0);
        Assert.assertEquals(tleA.getLaunchYear(),                tleB.getLaunchYear());
        Assert.assertEquals(tleA.getLaunchNumber(),              tleB.getLaunchNumber());
        Assert.assertEquals(tleA.getLaunchPiece(),               tleB.getLaunchPiece());
        Assert.assertEquals(tleA.getBStar()          ,           tleB.getBStar(), 0);
        Assert.assertEquals(tleA.getEphemerisType(),             tleB.getEphemerisType());
        Assert.assertEquals(tleA.getI().getReal(),               tleB.getI().getReal(), 1e-10);
        Assert.assertEquals(tleA.getRaan().getReal(),            tleB.getRaan().getReal(), 1e-10);
        Assert.assertEquals(tleA.getE().getReal(),               tleB.getE().getReal(), 1e-10);
        Assert.assertEquals(tleA.getPerigeeArgument().getReal(), tleB.getPerigeeArgument().getReal(), 1e-10);
        Assert.assertEquals(tleA.getMeanAnomaly().getReal(),     tleB.getMeanAnomaly().getReal(), 1e-10);
        Assert.assertEquals(tleA.getMeanMotion().getReal(),      tleB.getMeanMotion().getReal(), 0);
        Assert.assertEquals(tleA.getRevolutionNumberAtEpoch(),   tleB.getRevolutionNumberAtEpoch(), 0);
        Assert.assertEquals(tleA.getElementNumber(),             tleB.getElementNumber(), 0);
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
        Assert.assertEquals("1 Z9999U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9088", tle.getLine1());
        Assert.assertEquals("2 Z9999  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804541", tle.getLine2());
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("meanMotionSecondDerivative", oe.getParts()[1]);
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("B*", oe.getParts()[1]);
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("eccentricity", oe.getParts()[1]);
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(1000000, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("satelliteNumber-1", oe.getParts()[1]);
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(1000000, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("satelliteNumber-2", oe.getParts()[1]);
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_CHECKSUM_ERROR, oe.getSpecifier());
            Assert.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("0", oe.getParts()[1]);
            Assert.assertEquals("1", oe.getParts()[2]);
            Assert.assertEquals("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                                oe.getParts()[3]);
        }
    }

    public void doTestWrongChecksum2() {
        try {
            FieldTLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    61");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_CHECKSUM_ERROR, oe.getSpecifier());
            Assert.assertEquals(2, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("2", oe.getParts()[1]);
            Assert.assertEquals("1", oe.getParts()[2]);
            Assert.assertEquals("2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    61",
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
                        Assert.assertTrue(TLE.isFormatOK(line1, line2));

                        FieldTLE<T> tle = new FieldTLE<T>(field, line1, line2);

                        int satNum = Integer.parseInt(title[1]);
                        Assert.assertTrue(satNum==tle.getSatelliteNumber());
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
                            Assert.assertEquals(0, normDifPos, 2e-3);
                            Assert.assertEquals(0, normDifVel, 7e-4);


                        }
                    }
                }
                Assert.assertEquals(0, cumulated, 0.026);
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
        Assert.assertEquals(42171546.979560345, pv.getPosition().getNorm().getReal(), 1.0e-3);
        Assert.assertEquals(3074.1890089357994, pv.getVelocity().getNorm().getReal(), 1.0e-6);
    }

    public <T extends CalculusFieldElement<T>> void doTestSymmetryAfterLeapSecondIntroduction(Field<T> field) {
        checkSymmetry(field, "1 34602U 09013A   12187.35117436  .00002472  18981-5  42406-5 0  9995",
                             "2 34602  96.5991 210.0210 0006808 112.8142 247.3865 16.06008103193411");
    }

    public <T extends CalculusFieldElement<T>> void doTestOldTLE(Field<T> field) {
        String line1 = "1 15427U          85091.94293084 0.00000051  00000+0  32913-4 0   179";
        String line2 = "2 15427  98.9385  46.0219 0015502 321.4354  38.5705 14.11363211 15580";
        Assert.assertTrue(TLE.isFormatOK(line1, line2));
        FieldTLE<T> tle = new FieldTLE<T>(field, line1, line2);
        Assert.assertEquals(15427, tle.getSatelliteNumber());
        Assert.assertEquals(0.00000051,
                            tle.getMeanMotionFirstDerivative().getReal() * Constants.JULIAN_DAY * Constants.JULIAN_DAY / (4 * FastMath.PI),
                            1.0e-15);
    }

    public <T extends CalculusFieldElement<T>> void doTestEqualTLE(Field<T> field) {
        FieldTLE<T> tleA = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                  "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        FieldTLE<T> tleB = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                  "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        Assert.assertTrue(tleA.equals(tleB));
    }

    public <T extends CalculusFieldElement<T>> void doTestNonEqualTLE(Field<T> field) {
        FieldTLE<T> tleA = new FieldTLE<T>(field, "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                                  "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        FieldTLE<T> tleB = new FieldTLE<T>(field, "1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                                                  "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
        Assert.assertFalse(tleA.equals(tleB));
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
        Assert.assertEquals(tleRef.getLine1(), tleOriginal.getLine1());
        Assert.assertEquals(tleRef.getLine2(), tleOriginal.getLine2());
        FieldTLE<T> changedBStar = new FieldTLE<T>(27421, 'U', 2002, 21, "A", TLE.DEFAULT, 2,
                                   new FieldAbsoluteDate<T>(field, "2002-05-04T11:45:15.695", TimeScalesFactory.getUTC()),
                                   T_zero.add(FastMath.toRadians(14.26113993 * 360 / Constants.JULIAN_DAY)),
                                   T_zero.add(FastMath.toRadians(-.00021470 * 360 * 2 / (Constants.JULIAN_DAY * Constants.JULIAN_DAY))),
                                   T_zero.add(FastMath.toRadians(0.0)),
                                   T_zero.add(1.333E-4), T_zero.add(FastMath.toRadians(98.7490)),
                                   T_zero.add(FastMath.toRadians(133.9522)), T_zero.add(FastMath.toRadians(199.5121)), T_zero.add(FastMath.toRadians(226.1918)),
                                   6, 1.0e-4);
        Assert.assertEquals(tleRef.getLine1().replace("-89879-2", " 10000-3"), changedBStar.getLine1());
        Assert.assertEquals(tleRef.getLine2(), changedBStar.getLine2());
        Assert.assertEquals(1.0e-4, new FieldTLE<T>(field, changedBStar.getLine1(), changedBStar.getLine2()).getBStar(), 1.0e-15);
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
        Assert.assertEquals(tle.getLine1(), "1 99999X 20042F   20001.04166667  .00000000  00000-0  10000-4 0  9997");
        Assert.assertEquals(tle.getLine2(), "2 99999  97.3982 239.8686 0016311 175.5448 123.6195 15.14038717    18");
    }

    @Test
    public void testStateToTLELeo() {
    	doTestStateToTLELeo(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestStateToTLELeo(final Field<T> field) {
    	final FieldTLE<T> leoTLE = new FieldTLE<>(field, "1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                                                  "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
        checkConversion(leoTLE, field);
    }

    @Test
    public void testStateToTLEGeo() {
    	doTestStateToTLEGeo(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestStateToTLEGeo(final Field<T> field) {
    	final FieldTLE<T> geoTLE = new FieldTLE<>(field, "1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                                                         "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        checkConversion(geoTLE, field);
    }

    private <T extends CalculusFieldElement<T>> void checkConversion(final FieldTLE<T> tle, final Field<T> field)
        {

        FieldPropagator<T> p = FieldTLEPropagator.selectExtrapolator(tle, tle.getParameters(field));
        final FieldTLE<T> converted = FieldTLE.stateToTLE(p.getInitialState(), tle);

        Assert.assertEquals(tle.getSatelliteNumber(),         converted.getSatelliteNumber());
        Assert.assertEquals(tle.getClassification(),          converted.getClassification());
        Assert.assertEquals(tle.getLaunchYear(),              converted.getLaunchYear());
        Assert.assertEquals(tle.getLaunchNumber(),            converted.getLaunchNumber());
        Assert.assertEquals(tle.getLaunchPiece(),             converted.getLaunchPiece());
        Assert.assertEquals(tle.getElementNumber(),           converted.getElementNumber());
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), converted.getRevolutionNumberAtEpoch());

        final double eps = 1.0e-7;
        Assert.assertEquals(tle.getMeanMotion().getReal(), converted.getMeanMotion().getReal(), eps * tle.getMeanMotion().getReal());
        Assert.assertEquals(tle.getE().getReal(), converted.getE().getReal(), eps * tle.getE().getReal());
        Assert.assertEquals(tle.getI().getReal(), converted.getI().getReal(), eps * tle.getI().getReal());
        Assert.assertEquals(tle.getPerigeeArgument().getReal(), converted.getPerigeeArgument().getReal(), eps * tle.getPerigeeArgument().getReal());
        Assert.assertEquals(tle.getRaan().getReal(), converted.getRaan().getReal(), eps * tle.getRaan().getReal());
        Assert.assertEquals(tle.getMeanAnomaly().getReal(), converted.getMeanAnomaly().getReal(), eps * tle.getMeanAnomaly().getReal());
        Assert.assertEquals(tle.getMeanAnomaly().getReal(), converted.getMeanAnomaly().getReal(), eps * tle.getMeanAnomaly().getReal());
        Assert.assertEquals(tle.getBStar(), converted.getBStar(), eps * tle.getBStar());

    }

    @Test
    public void testStateToTleISS() {
        doTestStateToTleISS(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestStateToTleISS(final Field<T> field) {

        // Initialize TLE
        final FieldTLE<T> tleISS = new FieldTLE<>(field, "1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                                         "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tleISS, tleISS.getParameters(field));

        // State at TLE epoch
        final FieldSpacecraftState<T> state = propagator.propagate(tleISS.getDate());

        // Convert to TLE
        final FieldTLE<T> rebuilt = FieldTLE.stateToTLE(state, tleISS);

        // Verify
        final double eps = 1.0e-7;
        Assert.assertEquals(tleISS.getSatelliteNumber(),           rebuilt.getSatelliteNumber());
        Assert.assertEquals(tleISS.getClassification(),            rebuilt.getClassification());
        Assert.assertEquals(tleISS.getLaunchYear(),                rebuilt.getLaunchYear());
        Assert.assertEquals(tleISS.getLaunchNumber(),              rebuilt.getLaunchNumber());
        Assert.assertEquals(tleISS.getLaunchPiece(),               rebuilt.getLaunchPiece());
        Assert.assertEquals(tleISS.getElementNumber(),             rebuilt.getElementNumber());
        Assert.assertEquals(tleISS.getRevolutionNumberAtEpoch(),   rebuilt.getRevolutionNumberAtEpoch());
        Assert.assertEquals(tleISS.getMeanMotion().getReal(),      rebuilt.getMeanMotion().getReal(),      eps * tleISS.getMeanMotion().getReal());
        Assert.assertEquals(tleISS.getE().getReal(),               rebuilt.getE().getReal(),               eps * tleISS.getE().getReal());
        Assert.assertEquals(tleISS.getI().getReal(),               rebuilt.getI().getReal(),               eps * tleISS.getI().getReal());
        Assert.assertEquals(tleISS.getPerigeeArgument().getReal(), rebuilt.getPerigeeArgument().getReal(), eps * tleISS.getPerigeeArgument().getReal());
        Assert.assertEquals(tleISS.getRaan().getReal(),            rebuilt.getRaan().getReal(),            eps * tleISS.getRaan().getReal());
        Assert.assertEquals(tleISS.getMeanAnomaly().getReal(),     rebuilt.getMeanAnomaly().getReal(),     eps * tleISS.getMeanAnomaly().getReal());
        Assert.assertEquals(tleISS.getMeanAnomaly().getReal(),     rebuilt.getMeanAnomaly().getReal(),     eps * tleISS.getMeanAnomaly().getReal());
        Assert.assertEquals(tleISS.getBStar(),                     rebuilt.getBStar(),                     eps * tleISS.getBStar());
    }

    @Test
    public void testIssue802() {
        doTestIssue802(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue802(final Field<T> field) {

        // Initialize TLE
        final FieldTLE<T> tleISS = new FieldTLE<>(field, "1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                                         "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tleISS, tleISS.getParameters(field));

        // State at TLE epoch
        final FieldSpacecraftState<T> state = propagator.propagate(tleISS.getDate());

        // Changes frame
        final Frame eme2000 = FramesFactory.getEME2000();
        final TimeStampedFieldPVCoordinates<T> pv = state.getPVCoordinates(eme2000);
        final FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(pv, eme2000, state.getMu());

        // Convert to TLE
        final FieldTLE<T> rebuilt = FieldTLE.stateToTLE(new FieldSpacecraftState<T>(orbit), tleISS);

        // Verify
        Assert.assertEquals(tleISS.getLine1(), rebuilt.getLine1());
        Assert.assertEquals(tleISS.getLine2(), rebuilt.getLine2());
    }

    @Test
    public void testToTLE() {
        doTestToTLE(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestToTLE(final Field<T> field) {
        final TLE tle = new TLE("1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");
        final FieldTLE<T> fieldTle = new FieldTLE<T>(field, tle.getLine1(), tle.getLine2());
        final TLE rebuilt = fieldTle.toTLE();
        Assert.assertTrue(rebuilt.equals(tle));
        Assert.assertEquals(tle.toString(), rebuilt.toString());
    }

    @Test
    public void testIssue781() {

        final DSFactory factory = new DSFactory(6, 3);
        final String line1 = "1 05709U 71116A   21105.62692147  .00000088  00000-0  00000-0 0  9999";
        final String line2 = "2 05709  10.8207 310.3659 0014139  71.9531 277.0561  0.99618926100056";
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        final FieldTLE<DerivativeStructure> fieldTLE = new FieldTLE<>(factory.getDerivativeField(), line1, line2);
        final FieldTLEPropagator<DerivativeStructure> tlePropagator = FieldTLEPropagator.selectExtrapolator(fieldTLE, fieldTLE.getParameters(factory.getDerivativeField()));
        final FieldTLE<DerivativeStructure> fieldTLE1 = FieldTLE.stateToTLE(tlePropagator.getInitialState(), fieldTLE);
        Assert.assertEquals(line2, fieldTLE1.getLine2());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}