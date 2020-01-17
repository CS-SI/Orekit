/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.NamedData;
import org.orekit.data.UnixCompressFilter;
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
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testDefaultLoadRinex2() {
        Utils.setDataRoot("rinex");
        Assert.assertEquals(51, new RinexLoader("^aaaa0000\\.00o$").getObservationDataSets().size());
    }

    @Test
    public void testDefaultLoadRinex3() {
        Utils.setDataRoot("rinex");
        Assert.assertEquals(5, new RinexLoader("^brca083\\.06o$").getObservationDataSets().size());
    }

    @Test
    public void testReadError() {
        try {
            final InputStream failingStream = new InputStream() {
                public int read() throws IOException {
                    throw new IOException("boo!");
                }
            };
            new RinexLoader(failingStream, "read-error");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals("boo!", oe.getSpecifier().getSourceString());
        }
    }

    @Test
    public void testWrongVersion() {
        try {
            load("rinex/unknown-version.06o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongFileType() {
        try {
            load("rinex/unknown-type.06o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testShortFirstLine() {
        try {
            load("rinex/short-first-line.06o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongFirstLabel() {
        try {
            load("rinex/unknown-first-label.06o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2OptionalRecords() {
        final RinexLoader  loader = load("rinex/cccc0000.07o");
        final RinexHeader  header = loader.getObservationDataSets().get(0).getHeader();
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(1.1111, 2.2222,  3.3333),
                                              header.getAntennaReferencePoint()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(9.9999, 8.8888, 7.7777),
                                              header.getAntennaBSight()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(0.1455, -0.3421, 0.0024),
                                              header.getCenterMass()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            new AbsoluteDate(2007, 9, 29, 0, 0,  0.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTFirstObs()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            new AbsoluteDate(2007, 9, 29, 0, 0, 30.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTLastObs()),
                            1.0e-15);
    }

    @Test
    public void testRinex3OptionalRecords() {
        final RinexLoader  loader = load("rinex/dddd0000.01o");
        final RinexHeader  header = loader.getObservationDataSets().get(0).getHeader();
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(1.1111, 2.2222,  3.3333),
                                              header.getAntennaReferencePoint()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(0.1111, 0.2222, 0.3333),
                                              header.getAntennaPhaseCenter()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(9.9999, 8.8888, 7.7777),
                                              header.getAntennaBSight()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(6.6666, 5.5555, 4.4444),
                                              header.getAntennaZeroDirection()),
                            1.0e-15);
        Assert.assertEquals(0.1010,
                            header.getAntennaAzimuth(),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(0.1455, -0.3421, 0.0024),
                                              header.getCenterMass()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            new AbsoluteDate(2018, 1, 29,  0,  0,  0.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTFirstObs()),
                            1.0e-15);
        Assert.assertEquals(0.0,
                            new AbsoluteDate(2018, 1, 29, 23, 59, 45.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTLastObs()),
                            1.0e-15);
    }

    @Test
    public void testRinex2Header() {

        //Tests Rinex 2 with only GPS Constellation
        RinexLoader  loader = load("rinex/jnu10110.17o");
        Assert.assertEquals(44, loader.getObservationDataSets().size());
        for (ObservationDataSet dataSet : loader.getObservationDataSets()) {
            RinexHeader header = dataSet.getHeader();

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
    public void testRinex3Header() {

        //Tests Rinex 3 with Multiple Constellations
        RinexLoader  loader = load("rinex/aaaa0000.00o");
        for (ObservationDataSet dataSet : loader.getObservationDataSets()) {
            RinexHeader header = dataSet.getHeader();

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
            Assert.assertNull(header.getPhaseShiftCorrections().get(0).getSatsCorrected());
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
    public void testGPSFile() {

        //Tests Rinex 2 with only GPS Constellation
        RinexLoader  loader = load("rinex/jnu10110.17o");
        String[] typesobs = {"L1","L2","P1","P2","C1","S1","S2"};

        List<ObservationDataSet> list = loader.getObservationDataSets();
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

    @Test
    public void testMoreThan12Sats() {
        RinexLoader  loader = loadCompressed("rinex/bogi1210.09d.Z");
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(135, ods.size());
        AbsoluteDate lastEpoch = null;
        int[] satsPerEpoch = { 16, 15, 15, 15, 15, 15, 15, 14, 15 };
        int epochCount = 0;
        int n = 0;
        for (final ObservationDataSet ds : ods) {
            if (lastEpoch != null && ds.getDate().durationFrom(lastEpoch) > 1.0e-3) {
                Assert.assertEquals(satsPerEpoch[epochCount], n);
                ++epochCount;
                n = 0;
            }
            ++n;
            lastEpoch = ds.getDate();
        }
        Assert.assertEquals(satsPerEpoch[epochCount], n);
        Assert.assertEquals(satsPerEpoch.length, epochCount + 1);
    }

    @Test
    public void testGPSGlonassFile() {
        //Tests Rinex 2 with GPS and GLONASS Constellations
        RinexLoader  loader = load("rinex/aiub0000.00o");
        String[] typesobs2 = {"P1","L1","L2","P2"};

        List<ObservationDataSet> list = loader.getObservationDataSets();
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

    @Test
    public void testMultipleConstellationsFile() {
        //Tests Rinex 3 with Multiple Constellations
        RinexLoader  loader = load("rinex/aaaa0000.00o");

        String[] typesobsG = {"C1C","L1C","S1C","C2W","L2W","S2W","C2X","L2X","S2X","C5X","L5X","S5X"};
        String[] typesobsR = {"C1C","L1C","S1C","C1P","L1P","S1P","C2C","L2C","S2C","C2P","L2P","S2P"};
        String[] typesobsE = {"C1X","L1X","S1X","C5X","L5X","S5X","C7X","L7X","S7X","C8X","L8X","S8X"};
        String[] typesobsC = {"C1I","L1I","S1I","C7I","L7I","S7I","C6I","L6I","S6I"};
        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assert.assertEquals(51, list.size());
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

    @Test
    public void testMultipleConstellationsGlonassScaleFactorFile() {
        //Tests Rinex 3 with Multiple Constellations and Scale Factor for some GLONASS Observations
        RinexLoader  loader = load("rinex/bbbb0000.00o");
        String[] typesobsG2 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR2 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE2 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        String[] typesobsS2 = {"C1C","L1C","S1C","C5I","L5I","S5I"};
        String[] typesobsC2 = {"C2I","L2I","S2I","C7I","L7I","S7I","C6I","L6I","S6I"};
        String[] typesobsJ2 = {"C1C","L1C","S1C","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assert.assertEquals(36, list.size());

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

    @Test
    public void testMultipleConstellationsGalileoScaleFactorFile() {
        //Tests Rinex 3 with Multiple Constellations and Scale Factor for all GALILEO Observations
        RinexLoader  loader = load("rinex/bbbb0000.01o");
        String[] typesobsG4 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR4 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE4 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assert.assertEquals(36, list.size());

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
    
    
    @Test
    public void testWrongLabel() {
        try {
            load("rinex/unknown-label.00o");
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
            load("rinex/missing-label.00o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }
    
    
    @Test
    public void testUnknownSatelliteSystemHeader() {
        try {
            //Test with RinexV3 Unknown Satellite System inside Header
            load("rinex/unknown-satsystem.00o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_SATELLITE_SYSTEM, oe.getSpecifier());
            Assert.assertEquals('Z', oe.getParts()[0]);
        }
    }
    
    @Test
    public void testInconsistentNumSatellites() {
        try {
            //Test with RinexV3 inconsistent number of sats in an observation w/r to max sats in header
            load("rinex/inconsistent-satsnum.00o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS, oe.getSpecifier());
            Assert.assertEquals(25, oe.getParts()[3]); //N. max sats
            Assert.assertEquals(26, oe.getParts()[2]); //N. sats observation incoherent
        }
    }
    
    @Test
    public void testInconsistentSatSystem() {
        try {
            //Test with RinexV3 inconsistent satellite system in an observation w/r to file sat system
            load("rinex/inconsistent-satsystem.00o");
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
            load("rinex/unknown-rinex-frequency.00o");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_RINEX_FREQUENCY, oe.getSpecifier());
            Assert.assertEquals("AAA", (String) oe.getParts()[0]);
            Assert.assertEquals(14, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testDCBSApplied() {
        RinexLoader  loader = load("rinex/dcbs.00o");
        List<ObservationDataSet> l = loader.getObservationDataSets();
        Assert.assertEquals(51, l.size());
        for (ObservationDataSet dataSet : l) {
            RinexHeader header = dataSet.getHeader();
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
    public void testPCVSApplied() {
        RinexLoader loader = load("rinex/pcvs.00o");
        List<ObservationDataSet> l = loader.getObservationDataSets();
        Assert.assertEquals(51, l.size());
        for (ObservationDataSet dataSet : l) {
            RinexHeader header = dataSet.getHeader();
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

    @Test
    public void testRinex220Spaceborne() {
        RinexLoader loader = load("rinex/ice12720.07o");
        List<ObservationDataSet> l = loader.getObservationDataSets();
        Assert.assertEquals(4 * 7, l.size());
        for (int i = 0; i < l.size(); ++i) {
            ObservationDataSet dataSet = l.get(i);
            Assert.assertEquals("SPACEBORNE", dataSet.getHeader().getMarkerType());
            Assert.assertEquals(SatelliteSystem.GPS, dataSet.getHeader().getSatelliteSystem());
            switch (i % 7) {
                case 0 :
                    Assert.assertEquals( 1, dataSet.getPrnNumber());
                    break;
                case 1 :
                    Assert.assertEquals( 5, dataSet.getPrnNumber());
                    break;
                case 2 :
                    Assert.assertEquals( 9, dataSet.getPrnNumber());
                    break;
                case 3 :
                    Assert.assertEquals(12, dataSet.getPrnNumber());
                    break;
                case 4 :
                    Assert.assertEquals(14, dataSet.getPrnNumber());
                    break;
                case 5 :
                    Assert.assertEquals(22, dataSet.getPrnNumber());
                    break;
                case 6 :
                    Assert.assertEquals(30, dataSet.getPrnNumber());
                    break;
            }
            List<ObservationData> list = dataSet.getObservationData();
            Assert.assertEquals(9, list.size());
            Assert.assertEquals(ObservationType.L1, list.get(0).getObservationType());
            Assert.assertEquals(ObservationType.L2, list.get(1).getObservationType());
            Assert.assertEquals(ObservationType.P1, list.get(2).getObservationType());
            Assert.assertEquals(ObservationType.P2, list.get(3).getObservationType());
            Assert.assertEquals(ObservationType.C1, list.get(4).getObservationType());
            Assert.assertEquals(ObservationType.LA, list.get(5).getObservationType());
            Assert.assertEquals(ObservationType.S1, list.get(6).getObservationType());
            Assert.assertEquals(ObservationType.S2, list.get(7).getObservationType());
            Assert.assertEquals(ObservationType.SA, list.get(8).getObservationType());
        }
    }

    @Test
    public void testRinex220SpaceborneScaled() {
        List<ObservationDataSet> raw    = load("rinex/ice12720.07o").getObservationDataSets();
        List<ObservationDataSet> scaled = load("rinex/ice12720-scaled.07o").getObservationDataSets();
        Assert.assertEquals(4 * 7, raw.size());
        Assert.assertEquals(4 * 7, scaled.size());
        for (int i = 0; i < raw.size(); ++i) {

            ObservationDataSet rawDataSet    = raw.get(i);
            Assert.assertEquals("SPACEBORNE", rawDataSet.getHeader().getMarkerType());
            Assert.assertEquals(SatelliteSystem.GPS, rawDataSet.getHeader().getSatelliteSystem());

            ObservationDataSet scaledDataSet = scaled.get(i);
            Assert.assertEquals("SPACEBORNE", scaledDataSet.getHeader().getMarkerType());
            Assert.assertEquals(SatelliteSystem.GPS, scaledDataSet.getHeader().getSatelliteSystem());

            List<ObservationData> rawList    = rawDataSet.getObservationData();
            List<ObservationData> scaledList = scaledDataSet.getObservationData();
            Assert.assertEquals(9, rawList.size());
            Assert.assertEquals(9, scaledList.size());
            for (int j = 0; j < rawList.size(); ++j) {
                final ObservationData rawData    = rawList.get(j);
                final ObservationData scaledData = scaledList.get(j);
                Assert.assertEquals(rawData.getObservationType(), scaledData.getObservationType());
                Assert.assertEquals(rawData.getValue(), scaledData.getValue(), FastMath.ulp(rawData.getValue()));
            }
        }
    }

    @Test
    public void testIssue608() {
        //Tests Rinex 3.04 with GPS, GLONASS, Galileo and SBAS Constellations
        RinexLoader  loader = load("rinex/brca083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(5, ods.size());

        Assert.assertEquals("A 9080",                     ods.get(2).getHeader().getMarkerName());

        // Test GPS
        Assert.assertEquals(SatelliteSystem.GPS,    ods.get(1).getSatelliteSystem());
        Assert.assertEquals(9,                      ods.get(1).getPrnNumber());
        Assert.assertEquals(0.0,                    ods.get(1).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(ObservationType.C1C,    ods.get(1).getObservationData().get(0).getObservationType());
        Assert.assertEquals(20891534.648,           ods.get(1).getObservationData().get(0).getValue(), 1.0e-15);

        // Test SBAS
        Assert.assertEquals(SatelliteSystem.SBAS,   ods.get(4).getSatelliteSystem());
        Assert.assertEquals(120,                    ods.get(4).getPrnNumber());
        Assert.assertEquals(0.0,                    ods.get(4).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(ObservationType.L1C,    ods.get(4).getObservationData().get(1).getObservationType());
        Assert.assertEquals(335849.135,           ods.get(4).getObservationData().get(1).getValue(), 1.0e-15);
        
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

    private RinexLoader load(final String name) {
        return new RinexLoader(Utils.class.getClassLoader().getResourceAsStream(name), name);
     }

    private RinexLoader loadCompressed(final String name) {
        RinexLoader loader = null;
        try {
            final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                                () -> Utils.class.getClassLoader().getResourceAsStream(name));
            NamedData filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
            loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());
        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }
        return loader;
     }

}
