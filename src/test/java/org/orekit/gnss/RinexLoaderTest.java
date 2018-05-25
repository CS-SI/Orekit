/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.gnss;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.RinexLoader.Parser.AppliedDCBS;
import org.orekit.gnss.RinexLoader.Parser.AppliedPCVS;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class RinexLoaderTest {
    
    
    @Before
    public void setUp() throws OrekitException {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:rinex");
    }

    @Test
    public void testRinex2Header() throws OrekitException {
        
        //Tests Rinex 2 with only GPS Constellation
        RinexLoader  loader = new RinexLoader("^jnu10110\\.17o$");
        Assert.assertEquals(1, loader.getObservations().size());
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            RinexHeader header = entry.getKey();

            Assert.assertEquals(2.11, header.getRinexVersion(), 1.0e-15);
            Assert.assertEquals(SatelliteSystem.GPS,    header.getSatelliteSystem());
            Assert.assertEquals("JNU1",                 header.getMarkerName());
            Assert.assertNull(header.getMarkerNumber());
            Assert.assertEquals("Nathan Vary",          header.getObserverName());
            Assert.assertEquals("FAA",                  header.getAgencyName());
            Assert.assertEquals("82C1",                 header.getReceiverNumber());
            Assert.assertEquals("NOV WAASGII",          header.getReceiverType());
            Assert.assertEquals("",                     header.getReceiverVersion());
            Assert.assertEquals("",                     header.getAntennaNumber());
            Assert.assertEquals("MPL_WAAS_2225NW NONE", header.getAntennaType());
            Assert.assertEquals(-2354253.7610,          header.getApproxPos().getX(),      1.0e-4);
            Assert.assertEquals(-2388550.5590,          header.getApproxPos().getY(),      1.0e-4);
            Assert.assertEquals(5407042.5010,           header.getApproxPos().getZ(),      1.0e-4);
            Assert.assertEquals(0.0,                    header.getAntennaHeight(),         1.0e-4);
            Assert.assertEquals(0.0,                    header.getEccentricities().getX(), 1.0e-4);
            Assert.assertEquals(0.0,                    header.getEccentricities().getY(), 1.0e-4);
            Assert.assertEquals(30.0,                   header.getInterval(), 1.0e-15);
            Assert.assertEquals(-1,                     header.getClkOffset());
            Assert.assertEquals(18,                     header.getLeapSeconds());
            Assert.assertEquals(0.0, new AbsoluteDate(2017, 1, 11, TimeScalesFactory.getGPS()).durationFrom(header.getTFirstObs()), 1.0e-15);
            Assert.assertTrue(Double.isInfinite(header.getTLastObs().durationFrom(header.getTFirstObs())));

        }
    }

    @Test
    public void testRinex3Header() throws OrekitException {

        //Tests Rinex 3 with Multiple Constellations
        RinexLoader  loader = new RinexLoader("^aaaa0000\\.00o$");
        Assert.assertEquals(1, loader.getObservations().size());
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            RinexHeader header = entry.getKey();

            Assert.assertEquals(3.02, header.getRinexVersion(), 1.0e-15);
            Assert.assertEquals(SatelliteSystem.MIXED,  header.getSatelliteSystem());
            Assert.assertEquals("RDLT",                 header.getMarkerName());
            Assert.assertEquals("RDLT",                 header.getMarkerNumber());
            Assert.assertEquals("OBS",                  header.getObserverName());
            Assert.assertEquals("AGENCY",               header.getAgencyName());
            Assert.assertEquals("5035K69749",           header.getReceiverNumber());
            Assert.assertEquals("Trimble NetR9",        header.getReceiverType());
            Assert.assertEquals("5.03",                 header.getReceiverVersion());
            Assert.assertEquals("1912118081",           header.getAntennaNumber());
            Assert.assertEquals("TRM57971.00     NONE", header.getAntennaType());
            Assert.assertEquals(2104228.6921,           header.getApproxPos().getX(),      1.0e-4);
            Assert.assertEquals(-5642017.3992,          header.getApproxPos().getY(),      1.0e-4);
            Assert.assertEquals(2095406.0835,           header.getApproxPos().getZ(),      1.0e-4);
            Assert.assertEquals(0.0,                    header.getAntennaHeight(),         1.0e-4);
            Assert.assertEquals(0.0,                    header.getEccentricities().getX(), 1.0e-4);
            Assert.assertEquals(0.0,                    header.getEccentricities().getY(), 1.0e-4);
            Assert.assertNull(header.getAntennaReferencePoint());
            Assert.assertNull(header.getObservationCode());
            Assert.assertNull(header.getAntennaPhaseCenter());
            Assert.assertNull(header.getAntennaBSight());
            Assert.assertTrue(Double.isNaN(header.getAntennaAzimuth()));
            Assert.assertNull(header.getAntennaZeroDirection());
            Assert.assertNull(header.getCenterMass());
            Assert.assertEquals("DBHZ",                  header.getSignalStrengthUnit());
            Assert.assertEquals(15.0,                    header.getInterval(), 1.0e-15);
            Assert.assertEquals(-1,                      header.getClkOffset());
            Assert.assertEquals(0,                       header.getListAppliedDCBS().size());
            Assert.assertEquals(0,                       header.getListAppliedPCVS().size());
            Assert.assertEquals(3,                       header.getPhaseShiftCorrections().size());
            Assert.assertEquals(SatelliteSystem.GPS,     header.getPhaseShiftCorrections().get(0).getSatelliteSystem());
            Assert.assertEquals(ObservationType.L2X,      header.getPhaseShiftCorrections().get(0).getTypeObs());
            Assert.assertEquals(-0.25000,                header.getPhaseShiftCorrections().get(0).getCorrection(), 1.0e-5);
            Assert.assertEquals(SatelliteSystem.GLONASS, header.getPhaseShiftCorrections().get(1).getSatelliteSystem());
            Assert.assertEquals(ObservationType.L1P,      header.getPhaseShiftCorrections().get(1).getTypeObs());
            Assert.assertEquals(+0.25000,                header.getPhaseShiftCorrections().get(1).getCorrection(), 1.0e-5);
            Assert.assertEquals(SatelliteSystem.GLONASS, header.getPhaseShiftCorrections().get(2).getSatelliteSystem());
            Assert.assertEquals(ObservationType.L2C,      header.getPhaseShiftCorrections().get(2).getTypeObs());
            Assert.assertEquals(-0.25000,                header.getPhaseShiftCorrections().get(2).getCorrection(), 1.0e-5);
            Assert.assertEquals( 0,                      header.getLeapSeconds());
            Assert.assertEquals( 0,                      header.getLeapSecondsFuture());
            Assert.assertEquals( 0,                      header.getLeapSecondsWeekNum());
            Assert.assertEquals( 0,                      header.getLeapSecondsDayNum());
            Assert.assertEquals(0.0, new AbsoluteDate(2016, 1, 11, TimeScalesFactory.getGPS()).durationFrom(header.getTFirstObs()), 1.0e-15);
            Assert.assertTrue(Double.isInfinite(header.getTLastObs().durationFrom(header.getTFirstObs())));
        }
    }

    @Test
    public void testGPSFile() throws OrekitException {

        //Tests Rinex 2 with only GPS Constellation
        RinexLoader  loader = new RinexLoader("^jnu10110\\.17o$");
        String[] typesobs = {"L1","L2","P1","P2","C1","S1","S2"};

        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            List<ObservationDataSet> list = entry.getValue();
            Assert.assertEquals(44, list.size());

            checkObservation(list.get(0),
                             2017, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 2, -0.03,
                             typesobs, ObservationType.L1, 124458652.886, 4, 0);
            checkObservation(list.get(0),
                             2017, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 2, -0.03,
                             typesobs, ObservationType.P1, Double.NaN, 0, 0);
            checkObservation(list.get(3),
                             2017, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 6, -0.03,
                             typesobs, ObservationType.S2, 42.300, 4, 0);
            checkObservation(list.get(11),
                             2017, 1, 11, 0, 0, 30, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 2, -0.08,
                             typesobs, ObservationType.C1, 23688342.361, 4, 0);
            checkObservation(list.get(23),
                             2017, 1, 11, 0, 1, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 3, 0,
                             typesobs, ObservationType.P2, 25160656.959, 4, 0);
            checkObservation(list.get(23),
                             2017, 1, 11, 0, 1, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 3, 0,
                             typesobs, ObservationType.P1, Double.NaN, 0, 0);
            checkObservation(list.get(43),
                             2017, 1, 11, 0, 1, 30, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 30, 0,
                             typesobs, ObservationType.S1, 41.6, 4, 0);
        }

    }

    @Test
    public void testGPSGlonassFile() throws OrekitException {
        //Tests Rinex 2 with GPS and GLONASS Constellations
        RinexLoader  loader = new RinexLoader("^aiub0000\\.00o$");
        String[] typesobs2 = {"P1","L1","L2","P2"};

        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            List<ObservationDataSet> list = entry.getValue();
            Assert.assertEquals(24, list.size());
            checkObservation(list.get(0),
                             2001, 3, 24, 13, 10, 36, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 12, -.123456789,
                             typesobs2, ObservationType.P1, 23629347.915, 0, 0);
            checkObservation(list.get(1),
                             2001, 3, 24, 13, 10, 36, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 9, -.123456789,
                             typesobs2, ObservationType.L1, -0.12, 0, 9);
            checkObservation(list.get(2),
                             2001, 3, 24, 13, 10, 36, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 6, -.123456789,
                             typesobs2, ObservationType.P2, 20607605.848, 4, 4);
            checkObservation(list.get(3),
                             2001, 3, 24, 13, 10, 54, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 12, -.123456789,
                             typesobs2, ObservationType.L2, -41981.375, 0, 0);
            checkObservation(list.get(6),
                             2001, 3, 24, 13, 10, 54, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 21, -.123456789,
                             typesobs2, ObservationType.P1, 21345678.576, 0, 0);
            checkObservation(list.get(7),
                             2001, 3, 24, 13, 10, 54, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 22, -.123456789,
                             typesobs2, ObservationType.P2, Double.NaN, 0, 0);
            checkObservation(list.get(23),
                             2001, 3, 24, 13, 14, 48, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 6, -.123456234,
                             typesobs2, ObservationType.L1, 267583.678, 1, 7);
        }
        
    }

    @Test
    public void testMultipleConstellationsFile() throws OrekitException {
        //Tests Rinex 3 with Multiple Constellations
        RinexLoader  loader = new RinexLoader("^aaaa0000\\.00o$");

        String[] typesobsG = {"C1C","L1C","S1C","C2W","L2W","S2W","C2X","L2X","S2X","C5X","L5X","S5X"};
        String[] typesobsR = {"C1C","L1C","S1C","C1P","L1P","S1P","C2C","L2C","S2C","C2P","L2P","S2P"};
        String[] typesobsE = {"C1X","L1X","S1X","C5X","L5X","S5X","C7X","L7X","S7X","C8X","L8X","S8X"};
        String[] typesobsC = {"C1I","L1I","S1I","C7I","L7I","S7I","C6I","L6I","S6I"};
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            List<ObservationDataSet> list = entry.getValue();
            checkObservation(list.get(0),
                             2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 10, 0.0,
                             typesobsR, ObservationType.C1C, 23544632.969, 0, 6);
            checkObservation(list.get(1),
                             2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 27, 0.0,
                             typesobsG, ObservationType.C1C, 22399181.883, 0, 7);
            checkObservation(list.get(9),
                             2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 3, 0.0,
                             typesobsG, ObservationType.S5X,         47.600, 0, 0);
            checkObservation(list.get(10),
                             2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GALILEO, 14, 0.0,
                             typesobsE, ObservationType.L8X, 76221970.869, 0, 8);
            checkObservation(list.get(25),
                             2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.BEIDOU, 12, 0.0,
                             typesobsC, ObservationType.S7I, 31.100, 0, 0);
            checkObservation(list.get(25),
                             2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.BEIDOU, 12, 0.0,
                             typesobsC, ObservationType.S7I, 31.100, 0, 0);
            checkObservation(list.get(50),
                             2016, 1, 11, 0, 0, 15, TimeScalesFactory.getGPS(),
                             SatelliteSystem.BEIDOU, 11, 0.0,
                             typesobsC, ObservationType.C7I, 23697971.738, 0, 7);
        }

    }

    @Test
    public void testMultipleConstellationsGlonassScaleFactorFile() throws OrekitException {
        //Tests Rinex 3 with Multiple Constellations and Scale Factor for some GLONASS Observations
        RinexLoader  loader = new RinexLoader("^bbbb0000\\.00o$");
        String[] typesobsG2 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR2 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE2 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        String[] typesobsS2 = {"C1C","L1C","S1C","C5I","L5I","S5I"};
        String[] typesobsC2 = {"C2I","L2I","S2I","C7I","L7I","S7I","C6I","L6I","S6I"};
        String[] typesobsJ2 = {"C1C","L1C","S1C","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            List<ObservationDataSet> list = entry.getValue();
            checkObservation(list.get(0),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 30, 0.0,
                             typesobsG2, ObservationType.C1C, 20422534.056, 0, 8);
            checkObservation(list.get(2),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 10, 0.0,
                             typesobsR2, ObservationType.S2C, 49.250, 0, 0);
            checkObservation(list.get(2),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 10, 0.0,
                             typesobsR2, ObservationType.C1C, 19186.904493, 0, 9);
            checkObservation(list.get(7),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GALILEO, 5, 0.0,
                             typesobsE2, ObservationType.L8Q, 103747111.324, 0, 8);
            checkObservation(list.get(13),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.BEIDOU, 4, 0.0,
                             typesobsC2, ObservationType.C7I, 41010665.465, 0, 5);
            checkObservation(list.get(13),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.BEIDOU, 4, 0.0,
                             typesobsC2, ObservationType.L2I, Double.NaN, 0, 0);
            checkObservation(list.get(12),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.SBAS, 138, 0.0,
                             typesobsS2, ObservationType.C1C, 40430827.124, 0, 6);
            checkObservation(list.get(12),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.SBAS, 138, 0.0,
                             typesobsS2, ObservationType.S5I, 39.750, 0, 0);
            checkObservation(list.get(34),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.QZSS, 193, 0.0,
                             typesobsJ2, ObservationType.L2L, 168639076.823, 0, 6);
            checkObservation(list.get(32),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 2, 0.0,
                             typesobsR2, ObservationType.S1C, 0.0445, 0, 0);
        }
    }

    @Test
    public void testMultipleConstellationsGalileoScaleFactorFile() throws OrekitException {
        //Tests Rinex 3 with Multiple Constellations and Scale Factor for all GALILEO Observations
        RinexLoader  loader = new RinexLoader("^bbbb0000\\.01o$");
        String[] typesobsG4 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR4 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE4 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            List<ObservationDataSet> list = entry.getValue();
            checkObservation(list.get(0),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GPS, 30, 0.0,
                             typesobsG4, ObservationType.C1C, 20422534.056, 0, 8);
            checkObservation(list.get(2),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 10, 0.0,
                             typesobsR4, ObservationType.S2C, 49.250, 0, 0);
            checkObservation(list.get(2),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GLONASS, 10, 0.0,
                             typesobsR4, ObservationType.C1C, 19186904.493, 0, 9);
            checkObservation(list.get(7),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GALILEO, 5, 0.0,
                             typesobsE4, ObservationType.L8Q, 103747.111324, 0, 8);
            checkObservation(list.get(26),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GALILEO, 8, 0.0,
                             typesobsE4, ObservationType.C1C, 23499.584944, 0, 7);
            checkObservation(list.get(26),
                             2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                             SatelliteSystem.GALILEO, 8, 0.0,
                             typesobsE4, ObservationType.S8Q, 0.051, 0, 0);
        }

    }
    
    
    @Test
    public void testWrongLabel() {
        try {
            new RinexLoader("^unknown-label\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(22, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("THIS IS NOT A RINEX LABEL", ((String) oe.getParts()[2]).substring(60).trim());
        }
    }
    
    @Test
    public void testMissingHeaderLabel() {
        try {
            //Test with RinexV3 Missing Label inside Header
            new RinexLoader("^missing-label\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }
    
    
    @Test
    public void testUnknownSatelliteSystemHeader() throws OrekitException {
        try {
            //Test with RinexV3 Unknown Satellite System inside Header
            new RinexLoader("^unknown-satsystem\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_SATELLITE_SYSTEM, oe.getSpecifier());
            Assert.assertEquals('Z', oe.getParts()[0]);
        }
    }
    
    @Test
    public void testInconsistentNumSatellites() throws OrekitException {
        try {
            //Test with RinexV3 inconsistent number of sats in an observation w/r to max sats in header
            new RinexLoader("^inconsistent-satsnum\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS, oe.getSpecifier());
            Assert.assertEquals(25, oe.getParts()[3]); //N. max sats
            Assert.assertEquals(26, oe.getParts()[2]); //N. sats observation incoherent
        }
    }
    
    @Test
    public void testInconsistentSatSystem() throws OrekitException {
        try {
            //Test with RinexV3 inconsistent satellite system in an observation w/r to file sat system
            new RinexLoader("^inconsistent-satsystem\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM, oe.getSpecifier());
            Assert.assertEquals(SatelliteSystem.GPS, oe.getParts()[2]); //Rinex Satellite System (GPS)
            Assert.assertEquals(SatelliteSystem.GLONASS, oe.getParts()[3]); //First observation of a sat that is not GPS (GLONASS)
        }
    }
    
    @Test
    public void testUnknownFrequency() {
        try {
            new RinexLoader("^unknown-rinex-frequency\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_RINEX_FREQUENCY, oe.getSpecifier());
            Assert.assertEquals("AAA", (String) oe.getParts()[0]);
            Assert.assertEquals(14, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testDCBSApplied() throws OrekitException {
        RinexLoader  loader = new RinexLoader("^dcbs\\.00o$");
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            RinexHeader header = entry.getKey();
            List<AppliedDCBS> list = header.getListAppliedDCBS();
            Assert.assertEquals(2, list.size());
            Assert.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
            Assert.assertEquals("dcbs-program-name", list.get(0).getProgDCBS());
            Assert.assertEquals("http://example.com/GPS", list.get(0).getSourceDCBS());
            Assert.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
            Assert.assertEquals("dcbs-program-name", list.get(1).getProgDCBS());
            Assert.assertEquals("http://example.com/GLONASS", list.get(1).getSourceDCBS());
        }
    }

    @Test
    public void testPCVSApplied() throws OrekitException {
        RinexLoader  loader = new RinexLoader("^pcvs\\.00o$");
        for (Map.Entry<RinexHeader, List<ObservationDataSet>> entry : loader.getObservations().entrySet()) {
            RinexHeader header = entry.getKey();
            List<AppliedPCVS> list = header.getListAppliedPCVS();
            Assert.assertEquals(2, list.size());
            Assert.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
            Assert.assertEquals("pcvs-program-name", list.get(0).getProgPCVS());
            Assert.assertEquals("http://example.com/GPS", list.get(0).getSourcePCVS());
            Assert.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
            Assert.assertEquals("pcvs-program-name", list.get(1).getProgPCVS());
            Assert.assertEquals("http://example.com/GLONASS", list.get(1).getSourcePCVS());
        }
    }

    private void checkObservation(final ObservationDataSet obser,
                                  final int year, final int month, final int day,
                                  final int hour, final int minute, final double second,
                                  final TimeScale timescale,
                                  final SatelliteSystem system, final int prnNumber,
                                  final double rcvrClkOffset, final String[] typesObs,
                                  final ObservationType rf, final double obsValue,
                                  final int lliValue, final int sigstrength) {

          final AbsoluteDate date = new AbsoluteDate(year, month, day, hour, minute, second,
                                                     timescale);
          
          Assert.assertEquals(system,         obser.getSatelliteSystem());
          Assert.assertEquals(prnNumber,      obser.getPrnNumber());
          Assert.assertEquals(date,           obser.getDate());
          Assert.assertEquals(rcvrClkOffset,  obser.getRcvrClkOffset(), 1.E-17);
          for (int i = 0; i < typesObs.length; i++) {
              final ObservationData od = obser.getObservationData().get(i);
              Assert.assertEquals(ObservationType.valueOf(typesObs[i]), od.getObservationType());
              if (od.getObservationType() == rf) {
                  if (Double.isNaN(obsValue)) {
                      Assert.assertTrue(Double.isNaN(od.getValue()));
                  } else {
                      Assert.assertEquals(obsValue, od.getValue(), 1.E-3);
                  }
                  Assert.assertEquals(lliValue,    od.getLossOfLockIndicator());
                  Assert.assertEquals(sigstrength, od.getSignalStrength());
              }
          }

      }

}
