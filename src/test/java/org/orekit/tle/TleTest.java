/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.tle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.apache.commons.math.geometry.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class TleTest {

    @Test
    public void testTLEFormat() throws OrekitException {

        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        assertTrue(TLE.isFormatOK(line1, line2));

        TLE tle = new TLE(line1, line2);
        assertEquals(27421, tle.getSatelliteNumber(), 0);
        assertEquals(2002, tle.getLaunchYear());
        assertEquals(21, tle.getLaunchNumber());
        assertEquals("A", tle.getLaunchPiece());
        assertEquals(-0.0089879, tle.getBStar(), 0);
        assertEquals(0, tle.getEphemerisType());
        assertEquals(98.749, Math.toDegrees(tle.getI()), 1e-10);
        assertEquals(199.5121, Math.toDegrees(tle.getRaan()), 1e-10);
        assertEquals(0.0001333, tle.getE(), 1e-10);
        assertEquals(133.9522, Math.toDegrees(tle.getPerigeeArgument()), 1e-10);
        assertEquals(226.1918, Math.toDegrees(tle.getMeanAnomaly()), 1e-10);
        assertEquals(14.26113993, tle.getMeanMotion() * 86400 / (2 * Math.PI), 0);
        assertEquals(tle.getRevolutionNumberAtEpoch(), 6, 0);
        assertEquals(tle.getElementNumber(), 2 ,0);

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14*26113993    62";
        assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 10006113993    62";
        assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879 2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        assertFalse(TLE.isFormatOK(line1, line2));
    }

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
        assertEquals(line1, tle.getLine1());
        assertEquals(line2, tle.getLine2());
    }

    @Test
    public void testTLESeriesFormat() throws IOException, OrekitException {

        InputStream in =
            TleTest.class.getResourceAsStream("/tle/regular-data/spot-5.txt");
        TLESeries series = new TLESeries(in);
        assertEquals(0,
                     series.getFirstDate().durationFrom(new AbsoluteDate(new DateComponents(2002, 05, 04),
                                                                  new TimeComponents(11, 45, 15.695),
                                                                  TimeScalesFactory.getUTC())),
                                                                  1e-3);
        assertEquals(0,
                     series.getLastDate().durationFrom(new AbsoluteDate(new DateComponents(2002, 06, 24),
                                                                 new TimeComponents(18, 12, 44.592),
                                                                 TimeScalesFactory.getUTC())),
                                                                 1e-3);

        AbsoluteDate mid = new AbsoluteDate(new DateComponents(2002, 06, 02),
                                            new TimeComponents(11, 12, 15),
                                            TimeScalesFactory.getUTC());
        assertEquals(0,
                     series.getClosestTLE(mid).getDate().durationFrom(new AbsoluteDate(new DateComponents(2002, 6, 2),
                                                                                 new TimeComponents(10, 8, 25.401),
                                                                                 TimeScalesFactory.getUTC())),
                                                                                 1e-3);
        mid = new AbsoluteDate(new DateComponents(2001, 06, 02),
                               new TimeComponents(11, 12, 15),
                               TimeScalesFactory.getUTC());
        assertTrue(series.getClosestTLE(mid).getDate().equals(series.getFirstDate()));
        mid = new AbsoluteDate(new DateComponents(2003, 06, 02),
                               new TimeComponents(11, 12, 15),
                               TimeScalesFactory.getUTC());
        assertTrue(series.getClosestTLE(mid).getDate().equals(series.getLastDate()));

    }

    @Test
    public void testSatCodeCompliance() throws IOException, OrekitException, ParseException {

        BufferedReader rEntry = null;
        BufferedReader rResults = null;

        InputStream inEntry =
            TleTest.class.getResourceAsStream("/tle/extrapolationTest-data/SatCode-entry");
        rEntry = new BufferedReader(new InputStreamReader(inEntry));

        try {
            InputStream inResults =
                TleTest.class.getResourceAsStream("/tle/extrapolationTest-data/SatCode-results");
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
                        assertTrue(TLE.isFormatOK(line1, line2));

                        TLE tle = new TLE(line1, line2);

                        int satNum = Integer.parseInt(title[1]);
                        assertTrue(satNum==tle.getSatelliteNumber());
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

                            AbsoluteDate date = new AbsoluteDate(tle.getDate(), minFromStart*60);
                            PVCoordinates results = null;
                            try {
                                results = ex.getPVCoordinates(date);
                            }
                            catch(OrekitException e)  {
                                if(satNum==28872  || satNum==23333 || satNum==29141 ) {
                                    // expected behaviour
                                }
                                else {
                                    fail("exception not expected"+e.getMessage());
                                }
                            }
                            if (results != null) {
                                double normDifPos = testPos.subtract(results.getPosition()).getNorm();
                                double normDifVel = testVel.subtract(results.getVelocity()).getNorm();

                                cumulated += normDifPos;
                                assertEquals( 0, normDifPos, 2e-3);;
                                assertEquals( 0, normDifVel, 1e-5);

                            }

                        }
                    }
                }
                assertEquals(0, cumulated, 0.024);
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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}