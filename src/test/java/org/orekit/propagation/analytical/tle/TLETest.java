/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class TLETest {

    @Test
    public void testTLEFormat() throws OrekitException {

        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        TLE tle = new TLE(line1, line2);
        Assert.assertEquals(27421, tle.getSatelliteNumber(), 0);
        Assert.assertEquals(2002, tle.getLaunchYear());
        Assert.assertEquals(21, tle.getLaunchNumber());
        Assert.assertEquals("A", tle.getLaunchPiece());
        Assert.assertEquals(-0.0089879, tle.getBStar(), 0);
        Assert.assertEquals(0, tle.getEphemerisType());
        Assert.assertEquals(98.749, FastMath.toDegrees(tle.getI()), 1e-10);
        Assert.assertEquals(199.5121, FastMath.toDegrees(tle.getRaan()), 1e-10);
        Assert.assertEquals(0.0001333, tle.getE(), 1e-10);
        Assert.assertEquals(133.9522, FastMath.toDegrees(tle.getPerigeeArgument()), 1e-10);
        Assert.assertEquals(226.1918, FastMath.toDegrees(tle.getMeanAnomaly()), 1e-10);
        Assert.assertEquals(14.26113993, tle.getMeanMotion() * Constants.JULIAN_DAY / (2 * FastMath.PI), 0);
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), 6, 0);
        Assert.assertEquals(tle.getElementNumber(), 2 ,0);

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

    @Test
    public void testIssue196() throws OrekitException {

        String line1A = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line1B = "1 27421U 02021A   02124.48976499  -.0002147  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assert.assertTrue(TLE.isFormatOK(line1A, line2));
        TLE tleA = new TLE(line1A, line2);
        Assert.assertTrue(TLE.isFormatOK(line1B, line2));
        TLE tleB = new TLE(line1B, line2);
        Assert.assertEquals(tleA.getSatelliteNumber(),         tleB.getSatelliteNumber(), 0);
        Assert.assertEquals(tleA.getLaunchYear(),              tleB.getLaunchYear());
        Assert.assertEquals(tleA.getLaunchNumber(),            tleB.getLaunchNumber());
        Assert.assertEquals(tleA.getLaunchPiece(),             tleB.getLaunchPiece());
        Assert.assertEquals(tleA.getBStar(),                   tleB.getBStar(), 0);
        Assert.assertEquals(tleA.getEphemerisType(),           tleB.getEphemerisType());
        Assert.assertEquals(tleA.getI(),                       tleB.getI(), 1e-10);
        Assert.assertEquals(tleA.getRaan(),                    tleB.getRaan(), 1e-10);
        Assert.assertEquals(tleA.getE(),                       tleB.getE(), 1e-10);
        Assert.assertEquals(tleA.getPerigeeArgument(),         tleB.getPerigeeArgument(), 1e-10);
        Assert.assertEquals(tleA.getMeanAnomaly(),             tleB.getMeanAnomaly(), 1e-10);
        Assert.assertEquals(tleA.getMeanMotion(),              tleB.getMeanMotion(), 0);
        Assert.assertEquals(tleA.getRevolutionNumberAtEpoch(), tleB.getRevolutionNumberAtEpoch(), 0);
        Assert.assertEquals(tleA.getElementNumber(),           tleB.getElementNumber(), 0);

    }

    @Test
    public void testSymmetry() throws OrekitException {
        checkSymmetry("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                      "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        checkSymmetry("1 31928U 98067BA  08269.84884916  .00114257  17652-4  13615-3 0  4412",
                      "2 31928  51.6257 175.4142 0001703  41.9031 318.2112 16.08175249 68368");
    }

    private void checkSymmetry(String line1, String line2) throws OrekitException {
        TLE tleRef = new TLE(line1, line2);
        TLE tle = new TLE(tleRef.getSatelliteNumber(), tleRef.getClassification(),
                          tleRef.getLaunchYear(), tleRef.getLaunchNumber(), tleRef.getLaunchPiece(),
                          tleRef.getEphemerisType(), tleRef.getElementNumber(), tleRef.getDate(),
                          tleRef.getMeanMotion(), tleRef.getMeanMotionFirstDerivative(),
                          tleRef.getMeanMotionSecondDerivative(), tleRef.getE(), tleRef.getI(),
                          tleRef.getPerigeeArgument(), tleRef.getRaan(), tleRef.getMeanAnomaly(),
                          tleRef.getRevolutionNumberAtEpoch(), tleRef.getBStar());
        Assert.assertEquals(line1, tle.getLine1());
        Assert.assertEquals(line2, tle.getLine2());
    }

    @Test
    public void testBug74() throws OrekitException {
        checkSymmetry("1 00001U 00001A   12026.45833333 2.94600864  39565-9  16165-7 1    12",
                      "2 00001 627.0796 454.4522 0000000 624.9662   0.4817  0.00000000    12");
    }

    @Test
    public void testBug77() throws OrekitException {
        checkSymmetry("1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                      "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
    }

    @Test
    public void testDirectConstruction() throws OrekitException {
        TLE tleA = new TLE(5555, 'U', 1971, 86, "J", 0, 908,
                           new AbsoluteDate(new DateComponents(2012, 26),
                                            new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                            TimeScalesFactory.getUTC()),
                           taylorConvert(12.26882470, 1), taylorConvert(-0.00000004, 2), taylorConvert(0.00001e-9, 3),
                           0.0075476, FastMath.toRadians(74.0161), FastMath.toRadians(328.9888),
                           FastMath.toRadians(228.9750), FastMath.toRadians(30.6709), 80454, 0.01234e-9);
        TLE tleB =  new TLE("1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                            "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
        Assert.assertEquals(tleA.getSatelliteNumber(),         tleB.getSatelliteNumber(), 0);
        Assert.assertEquals(tleA.getLaunchYear(),              tleB.getLaunchYear());
        Assert.assertEquals(tleA.getLaunchNumber(),            tleB.getLaunchNumber());
        Assert.assertEquals(tleA.getLaunchPiece(),             tleB.getLaunchPiece());
        Assert.assertEquals(tleA.getBStar(),                   tleB.getBStar(), 0);
        Assert.assertEquals(tleA.getEphemerisType(),           tleB.getEphemerisType());
        Assert.assertEquals(tleA.getI(),                       tleB.getI(), 1e-10);
        Assert.assertEquals(tleA.getRaan(),                    tleB.getRaan(), 1e-10);
        Assert.assertEquals(tleA.getE(),                       tleB.getE(), 1e-10);
        Assert.assertEquals(tleA.getPerigeeArgument(),         tleB.getPerigeeArgument(), 1e-10);
        Assert.assertEquals(tleA.getMeanAnomaly(),             tleB.getMeanAnomaly(), 1e-10);
        Assert.assertEquals(tleA.getMeanMotion(),              tleB.getMeanMotion(), 0);
        Assert.assertEquals(tleA.getRevolutionNumberAtEpoch(), tleB.getRevolutionNumberAtEpoch(), 0);
        Assert.assertEquals(tleA.getElementNumber(),           tleB.getElementNumber(), 0);
    }

    @Test
    public void testBug77TooLargeSecondDerivative() throws OrekitException {
        try {
            TLE tle = new TLE(5555, 'U', 1971, 86, "J", 0, 908,
                              new AbsoluteDate(new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              taylorConvert(12.26882470, 1), taylorConvert(-0.00000004, 2), taylorConvert(0.99999e11, 3),
                              0.0075476, FastMath.toRadians(74.0161), FastMath.toRadians(328.9888),
                              FastMath.toRadians(228.9750), FastMath.toRadians(30.6709), 80454, 0.01234e-9);
            tle.getLine1();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("meanMotionSecondDerivative", oe.getParts()[1]);
        }
    }

    @Test
    public void testBug77TooLargeBStar() throws OrekitException {
        try {
            TLE tle = new TLE(5555, 'U', 1971, 86, "J", 0, 908,
                              new AbsoluteDate(new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              taylorConvert(12.26882470, 1), taylorConvert(-0.00000004, 2), taylorConvert(0.00001e-9, 3),
                              0.0075476, FastMath.toRadians(74.0161), FastMath.toRadians(328.9888),
                              FastMath.toRadians(228.9750), FastMath.toRadians(30.6709), 80454, 0.99999e11);
            tle.getLine1();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("B*", oe.getParts()[1]);
        }
    }

    @Test
    public void testBug77TooLargeEccentricity() throws OrekitException {
        try {
            TLE tle = new TLE(5555, 'U', 1971, 86, "J", 0, 908,
                              new AbsoluteDate(new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              taylorConvert(12.26882470, 1), taylorConvert(-0.00000004, 2), taylorConvert(0.00001e-9, 3),
                              1.0075476, FastMath.toRadians(74.0161), FastMath.toRadians(328.9888),
                              FastMath.toRadians(228.9750), FastMath.toRadians(30.6709), 80454, 0.01234e-9);
            tle.getLine2();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(5555, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("eccentricity", oe.getParts()[1]);
        }
    }

    @Test
    public void testBug77TooLargeSatelliteNumber1() throws OrekitException {
        try {
            TLE tle = new TLE(1000000, 'U', 1971, 86, "J", 0, 908,
                              new AbsoluteDate(new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              taylorConvert(12.26882470, 1), taylorConvert(-0.00000004, 2), taylorConvert(0.00001e-9, 3),
                              0.0075476, FastMath.toRadians(74.0161), FastMath.toRadians(328.9888),
                              FastMath.toRadians(228.9750), FastMath.toRadians(30.6709), 80454, 0.01234e-9);
            tle.getLine1();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TLE_INVALID_PARAMETER, oe.getSpecifier());
            Assert.assertEquals(1000000, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("satelliteNumber-1", oe.getParts()[1]);
        }
    }

    @Test
    public void testBug77TooLargeSatelliteNumber2() throws OrekitException {
        try {
            TLE tle = new TLE(1000000, 'U', 1971, 86, "J", 0, 908,
                              new AbsoluteDate(new DateComponents(2012, 26),
                                               new TimeComponents(0.96078249 * Constants.JULIAN_DAY),
                                               TimeScalesFactory.getUTC()),
                              taylorConvert(12.26882470, 1), taylorConvert(-0.00000004, 2), taylorConvert(0.00001e-9, 3),
                              0.0075476, FastMath.toRadians(74.0161), FastMath.toRadians(328.9888),
                              FastMath.toRadians(228.9750), FastMath.toRadians(30.6709), 80454, 0.01234e-9);
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

    @Test(expected=OrekitException.class)
    public void testDifferentSatNumbers() throws OrekitException {
        new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                "2 27422  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    @Test
    public void testChecksumOK() throws OrekitException {
        TLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                       "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    @Test(expected=OrekitException.class)
    public void testWrongChecksum1() throws OrekitException {
        TLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                       "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    @Test(expected=OrekitException.class)
    public void testWrongChecksum2() throws OrekitException {
        TLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                       "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    61");
    }

    @Test
    public void testSatCodeCompliance() throws IOException, OrekitException, ParseException {

        BufferedReader rEntry = null;
        BufferedReader rResults = null;

        InputStream inEntry =
            TLETest.class.getResourceAsStream("/tle/extrapolationTest-data/SatCode-entry");
        rEntry = new BufferedReader(new InputStreamReader(inEntry));

        try {
            InputStream inResults =
                TLETest.class.getResourceAsStream("/tle/extrapolationTest-data/SatCode-results");
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

                        TLE tle = new TLE(line1, line2);

                        int satNum = Integer.parseInt(title[1]);
                        Assert.assertTrue(satNum==tle.getSatelliteNumber());
                        TLEPropagator ex = TLEPropagator.selectExtrapolator(tle);

                        for (rline = rResults.readLine(); (rline!=null)&&(rline.charAt(0)!='r'); rline = rResults.readLine()) {

                            String[] data = rline.split(" ");
                            double minFromStart = Double.parseDouble(data[0]);
                            double pX = 1000*Double.parseDouble(data[1]);
                            double pY = 1000*Double.parseDouble(data[2]);
                            double pZ = 1000*Double.parseDouble(data[3]);
                            double vX = 1000*Double.parseDouble(data[4]);
                            double vY = 1000*Double.parseDouble(data[5]);
                            double vZ = 1000*Double.parseDouble(data[6]);
                            Vector3D testPos = new Vector3D(pX, pY, pZ);
                            Vector3D testVel = new Vector3D(vX, vY, vZ);

                            AbsoluteDate date = tle.getDate().shiftedBy(minFromStart * 60);
                            PVCoordinates results = ex.getPVCoordinates(date);
                            double normDifPos = testPos.subtract(results.getPosition()).getNorm();
                            double normDifVel = testVel.subtract(results.getVelocity()).getNorm();

                            cumulated += normDifPos;
                            Assert.assertEquals(0, normDifPos, 2e-3);;
                            Assert.assertEquals(0, normDifVel, 1e-5);


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

    @Test
    public void testZeroInclination() throws OrekitException{
        TLE tle = new TLE("1 26451U 00043A   10130.13784012 -.00000276  00000-0  10000-3 0  3866",
                          "2 26451 000.0000 266.1044 0001893 160.7642 152.5985 01.00271160 35865");
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        PVCoordinates pv = propagator.propagate(tle.getDate().shiftedBy(100)).getPVCoordinates();
        Assert.assertEquals(42171546.979560345, pv.getPosition().getNorm(), 1.0e-3);
        Assert.assertEquals(3074.1890089357994, pv.getVelocity().getNorm(), 1.0e-6);
    }

    @Test
    public void testSymmetryAfterLeapSecondIntroduction() throws OrekitException {
        checkSymmetry("1 34602U 09013A   12187.35117436  .00002472  18981-5  42406-5 0  9995",
                      "2 34602  96.5991 210.0210 0006808 112.8142 247.3865 16.06008103193411");
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}