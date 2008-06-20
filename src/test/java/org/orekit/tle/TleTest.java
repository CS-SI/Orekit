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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.tle.TLE;
import org.orekit.tle.TLEPropagator;
import org.orekit.tle.TLESeries;
import org.orekit.utils.PVCoordinates;


public class TleTest extends TestCase {

    public void testTLEFormat() throws OrekitException, ParseException {

        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        assertTrue(TLE.isFormatOK(line1, line2));

        TLE tle = new TLE(line1, line2);
        assertEquals(tle.getSatelliteNumber(), 27421, 0);
        assertTrue(tle.getInternationalDesignator().equals("02021A  "));
        assertEquals(tle.getBStar(), -0.0089879, 0);
        assertEquals(tle.getEphemerisType(), 0, 0);
        assertEquals(Math.toDegrees(tle.getI()), 98.749, 1e-10);
        assertEquals(Math.toDegrees(tle.getRaan()), 199.5121, 1e-10);
        assertEquals(tle.getE(), 0.0001333, 0);
        assertEquals(Math.toDegrees(tle.getPerigeeArgument()), 133.9522, 1e-10);
        assertEquals(Math.toDegrees(tle.getMeanAnomaly()), 226.1918, 1e-10);
        assertEquals(tle.getMeanMotion()*86400/(2*Math.PI), 14.26113993, 0);
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


    public void testTLESeriesFormat() throws IOException, OrekitException, ParseException {

        InputStream in =
            TleTest.class.getResourceAsStream("/tle/regular-data/spot-5.txt");
        TLESeries series = new TLESeries(in);
        assertEquals(0,
                     series.getFirstDate().minus(new AbsoluteDate(new ChunkedDate(2002, 05, 04),
                                                                  new ChunkedTime(11, 45, 15.695),
                                                                  UTCScale.getInstance())),
                                                                  1e-3);
        assertEquals(0,
                     series.getLastDate().minus(new AbsoluteDate(new ChunkedDate(2002, 06, 24),
                                                                 new ChunkedTime(18, 12, 44.592),
                                                                 UTCScale.getInstance())),
                                                                 1e-3);

        AbsoluteDate mid = new AbsoluteDate(new ChunkedDate(2002, 06, 02),
                                            new ChunkedTime(11, 12, 15),
                                            UTCScale.getInstance());
        assertEquals(0,
                     series.getClosestTLE(mid).getDate().minus(new AbsoluteDate(new ChunkedDate(2002, 6, 2),
                                                                                 new ChunkedTime(10, 8, 25.401),
                                                                                 UTCScale.getInstance())),
                                                                                 1e-3);
        mid = new AbsoluteDate(new ChunkedDate(2001, 06, 02),
                               new ChunkedTime(11, 12, 15),
                               UTCScale.getInstance());
        assertTrue(series.getClosestTLE(mid).getDate().equals(series.getFirstDate()));
        mid = new AbsoluteDate(new ChunkedDate(2003, 06, 02),
                               new ChunkedTime(11, 12, 15),
                               UTCScale.getInstance());
        assertTrue(series.getClosestTLE(mid).getDate().equals(series.getLastDate()));

    }


    public void testThetaG() throws OrekitException, ParseException {

//      AbsoluteDate date = AbsoluteDate.J2000Epoch;
//      double teta = SDP4.thetaG(date);

//      TIRF2000Frame ITRF = (TIRF2000Frame)Frame.getReferenceFrame(Frame.TIRF2000B, date);
//      double tetaTIRF = ITRF.getEarthRotationAngle(date);
//      assertEquals( Math.toDegrees(Utils.trimAngle(tetaTIRF, Math.PI)), Math.toDegrees(Utils.trimAngle(teta, Math.PI)), 0.003);

//      date = new AbsoluteDate(new ChunkedDate(2002, 03, 08), new ChunkedTime(01, 00, 45), UTCScale.getInstance());
//      tetaTIRF = ITRF.getEarthRotationAngle(date);
//      teta = SDP4.thetaG(date);
//      assertEquals( Math.toDegrees(Utils.trimAngle(tetaTIRF, Math.PI)), Math.toDegrees(Utils.trimAngle(teta, Math.PI)), 0.04);
    }

    public void testSatCodeCompliance() throws IOException, OrekitException, ParseException {

        boolean printResults = false;
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

                        if(printResults) {
                            System.out.println();
                            for(int i = 0; i<4; i++) {
                                if(header[i]!=null) {
                                    System.out.println(header[i]);
                                }
                            }
                            System.out.println(" Satellite number : " + satNum);
                        }

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
                                if(printResults) {
                                    System.out.println(minFromStart + "    " + normDifPos);
                                }
                                assertEquals( 0, normDifPos, 2e-3);;
                                assertEquals( 0, normDifVel, 1e-5);

                            }

                        }
                    }
                }
                if (printResults) {
                    System.out.println();
                    System.out.println(" cumul :  " + cumulated);
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


    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(TleTest.class);
    }


}