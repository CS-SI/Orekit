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
package org.orekit.gnss;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RinexLoaderTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testDefaultLoadRinex2() {
        Utils.setDataRoot("rinex");
        Assertions.assertEquals(51, new RinexObservationLoader("^aaaa0000\\.00o$").getObservationDataSets().size());
    }

    @Test
    public void testDefaultLoadRinex3() {
        Utils.setDataRoot("rinex");
        Assertions.assertEquals(5, new RinexObservationLoader("^brca083\\.06o$").getObservationDataSets().size());
    }

    @Test
    public void testReadError() {
        try {
            new RinexObservationLoader(new DataSource("read-error", () -> new InputStream() {
                public int read() throws IOException {
                    throw new IOException("boo!");
                }
            }));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals("boo!", oe.getSpecifier().getSourceString());
        }
    }

    @Test
    public void testWrongVersion() {
        try {
            load("rinex/unknown-version.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongFileType() {
        try {
            load("rinex/unknown-type.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testShortFirstLine() {
        try {
            load("rinex/short-first-line.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongFirstLabel() {
        try {
            load("rinex/unknown-first-label.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2OptionalRecords() {
        final RinexObservationLoader  loader = load("rinex/cccc0000.07o");
        final RinexObservationHeader  header = loader.getObservationDataSets().get(0).getHeader();
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(1.1111, 2.2222,  3.3333),
                                              header.getAntennaReferencePoint()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(9.9999, 8.8888, 7.7777),
                                              header.getAntennaBSight()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(0.1455, -0.3421, 0.0024),
                                              header.getCenterMass()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            new AbsoluteDate(2007, 9, 29, 0, 0,  0.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTFirstObs()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            new AbsoluteDate(2007, 9, 29, 0, 0, 30.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTLastObs()),
                            1.0e-15);
    }

    @Test
    public void testRinex3OptionalRecords() {
        final RinexObservationLoader  loader = load("rinex/dddd0000.01o");
        final RinexObservationHeader  header = loader.getObservationDataSets().get(0).getHeader();
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(1.1111, 2.2222,  3.3333),
                                              header.getAntennaReferencePoint()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(0.1111, 0.2222, 0.3333),
                                              header.getAntennaPhaseCenter()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(9.9999, 8.8888, 7.7777),
                                              header.getAntennaBSight()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(6.6666, 5.5555, 4.4444),
                                              header.getAntennaZeroDirection()),
                            1.0e-15);
        Assertions.assertEquals(0.1010,
                            header.getAntennaAzimuth(),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(new Vector3D(0.1455, -0.3421, 0.0024),
                                              header.getCenterMass()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            new AbsoluteDate(2018, 1, 29,  0,  0,  0.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTFirstObs()),
                            1.0e-15);
        Assertions.assertEquals(0.0,
                            new AbsoluteDate(2018, 1, 29, 23, 59, 45.0, TimeScalesFactory.getGPS()).
                            durationFrom(header.getTLastObs()),
                            1.0e-15);
    }

    @Test
    public void testRinex2Header() {

        //Tests Rinex 2 with only GPS Constellation
        RinexObservationLoader  loader = load("rinex/jnu10110.17o");
        Assertions.assertEquals(44, loader.getObservationDataSets().size());
        for (ObservationDataSet dataSet : loader.getObservationDataSets()) {
            RinexObservationHeader header = dataSet.getHeader();

            Assertions.assertEquals(2.11, header.getRinexVersion(), 1.0e-15);
            Assertions.assertEquals(SatelliteSystem.GPS,    header.getSatelliteSystem());
            Assertions.assertEquals("JNU1",                 header.getMarkerName());
            Assertions.assertNull(header.getMarkerNumber());
            Assertions.assertEquals("Nathan Vary",          header.getObserverName());
            Assertions.assertEquals("FAA",                  header.getAgencyName());
            Assertions.assertEquals("82C1",                 header.getReceiverNumber());
            Assertions.assertEquals("NOV WAASGII",          header.getReceiverType());
            Assertions.assertEquals("",                     header.getReceiverVersion());
            Assertions.assertEquals("",                     header.getAntennaNumber());
            Assertions.assertEquals("MPL_WAAS_2225NW NONE", header.getAntennaType());
            Assertions.assertEquals(-2354253.7610,          header.getApproxPos().getX(),      1.0e-4);
            Assertions.assertEquals(-2388550.5590,          header.getApproxPos().getY(),      1.0e-4);
            Assertions.assertEquals(5407042.5010,           header.getApproxPos().getZ(),      1.0e-4);
            Assertions.assertEquals(0.0,                    header.getAntennaHeight(),         1.0e-4);
            Assertions.assertEquals(0.0,                    header.getEccentricities().getX(), 1.0e-4);
            Assertions.assertEquals(0.0,                    header.getEccentricities().getY(), 1.0e-4);
            Assertions.assertEquals(30.0,                   header.getInterval(), 1.0e-15);
            Assertions.assertEquals(-1,                     header.getClkOffset());
            Assertions.assertEquals(18,                     header.getLeapSeconds());
            Assertions.assertEquals(0.0, new AbsoluteDate(2017, 1, 11, TimeScalesFactory.getGPS()).durationFrom(header.getTFirstObs()), 1.0e-15);
            Assertions.assertTrue(Double.isInfinite(header.getTLastObs().durationFrom(header.getTFirstObs())));

        }
    }

    @Test
    public void testRinex3Header() {

        //Tests Rinex 3 with Multiple Constellations
        RinexObservationLoader  loader = load("rinex/aaaa0000.00o");
        for (ObservationDataSet dataSet : loader.getObservationDataSets()) {
            RinexObservationHeader header = dataSet.getHeader();

            Assertions.assertEquals(3.02, header.getRinexVersion(), 1.0e-15);
            Assertions.assertEquals(SatelliteSystem.MIXED,  header.getSatelliteSystem());
            Assertions.assertEquals("RDLT",                 header.getMarkerName());
            Assertions.assertEquals("RDLT",                 header.getMarkerNumber());
            Assertions.assertEquals("OBS",                  header.getObserverName());
            Assertions.assertEquals("AGENCY",               header.getAgencyName());
            Assertions.assertEquals("5035K69749",           header.getReceiverNumber());
            Assertions.assertEquals("Trimble NetR9",        header.getReceiverType());
            Assertions.assertEquals("5.03",                 header.getReceiverVersion());
            Assertions.assertEquals("1912118081",           header.getAntennaNumber());
            Assertions.assertEquals("TRM57971.00     NONE", header.getAntennaType());
            Assertions.assertEquals(2104228.6921,           header.getApproxPos().getX(),      1.0e-4);
            Assertions.assertEquals(-5642017.3992,          header.getApproxPos().getY(),      1.0e-4);
            Assertions.assertEquals(2095406.0835,           header.getApproxPos().getZ(),      1.0e-4);
            Assertions.assertEquals(0.0,                    header.getAntennaHeight(),         1.0e-4);
            Assertions.assertEquals(0.0,                    header.getEccentricities().getX(), 1.0e-4);
            Assertions.assertEquals(0.0,                    header.getEccentricities().getY(), 1.0e-4);
            Assertions.assertNull(header.getAntennaReferencePoint());
            Assertions.assertNull(header.getObservationCode());
            Assertions.assertNull(header.getAntennaPhaseCenter());
            Assertions.assertNull(header.getAntennaBSight());
            Assertions.assertTrue(Double.isNaN(header.getAntennaAzimuth()));
            Assertions.assertNull(header.getAntennaZeroDirection());
            Assertions.assertNull(header.getCenterMass());
            Assertions.assertEquals("DBHZ",                  header.getSignalStrengthUnit());
            Assertions.assertEquals(15.0,                    header.getInterval(), 1.0e-15);
            Assertions.assertEquals(-1,                      header.getClkOffset());
            Assertions.assertEquals(0,                       header.getListAppliedDCBS().size());
            Assertions.assertEquals(0,                       header.getListAppliedPCVS().size());
            Assertions.assertEquals(3,                       header.getPhaseShiftCorrections().size());
            Assertions.assertEquals(SatelliteSystem.GPS,     header.getPhaseShiftCorrections().get(0).getSatelliteSystem());
            Assertions.assertEquals(ObservationType.L2X,      header.getPhaseShiftCorrections().get(0).getTypeObs());
            Assertions.assertNull(header.getPhaseShiftCorrections().get(0).getSatsCorrected());
            Assertions.assertEquals(-0.25000,                header.getPhaseShiftCorrections().get(0).getCorrection(), 1.0e-5);
            Assertions.assertEquals(SatelliteSystem.GLONASS, header.getPhaseShiftCorrections().get(1).getSatelliteSystem());
            Assertions.assertEquals(ObservationType.L1P,      header.getPhaseShiftCorrections().get(1).getTypeObs());
            Assertions.assertEquals(+0.25000,                header.getPhaseShiftCorrections().get(1).getCorrection(), 1.0e-5);
            Assertions.assertEquals(SatelliteSystem.GLONASS, header.getPhaseShiftCorrections().get(2).getSatelliteSystem());
            Assertions.assertEquals(ObservationType.L2C,      header.getPhaseShiftCorrections().get(2).getTypeObs());
            Assertions.assertEquals(-0.25000,                header.getPhaseShiftCorrections().get(2).getCorrection(), 1.0e-5);
            Assertions.assertEquals( 0,                      header.getLeapSeconds());
            Assertions.assertEquals( 0,                      header.getLeapSecondsFuture());
            Assertions.assertEquals( 0,                      header.getLeapSecondsWeekNum());
            Assertions.assertEquals( 0,                      header.getLeapSecondsDayNum());
            Assertions.assertEquals(0.0, new AbsoluteDate(2016, 1, 11, TimeScalesFactory.getGPS()).durationFrom(header.getTFirstObs()), 1.0e-15);
            Assertions.assertTrue(Double.isInfinite(header.getTLastObs().durationFrom(header.getTFirstObs())));
        }
    }

    @Test
    public void testGPSFile() {

        //Tests Rinex 2 with only GPS Constellation
        RinexObservationLoader  loader = load("rinex/jnu10110.17o");
        String[] typesobs = {"L1","L2","P1","P2","C1","S1","S2"};

        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assertions.assertEquals(44, list.size());

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
        RinexObservationLoader  loader = loadCompressed("rinex/bogi1210.09d.Z");
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assertions.assertEquals(135, ods.size());
        AbsoluteDate lastEpoch = null;
        int[] satsPerEpoch = { 16, 15, 15, 15, 15, 15, 15, 14, 15 };
        int epochCount = 0;
        int n = 0;
        for (final ObservationDataSet ds : ods) {
            if (lastEpoch != null && ds.getDate().durationFrom(lastEpoch) > 1.0e-3) {
                Assertions.assertEquals(satsPerEpoch[epochCount], n);
                ++epochCount;
                n = 0;
            }
            ++n;
            lastEpoch = ds.getDate();
        }
        Assertions.assertEquals(satsPerEpoch[epochCount], n);
        Assertions.assertEquals(satsPerEpoch.length, epochCount + 1);
    }

    @Test
    public void testGPSGlonassFile() {
        //Tests Rinex 2 with GPS and GLONASS Constellations
        RinexObservationLoader  loader = load("rinex/aiub0000.00o");
        String[] typesobs2 = {"P1","L1","L2","P2"};

        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assertions.assertEquals(24, list.size());

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
        RinexObservationLoader  loader = load("rinex/aaaa0000.00o");

        String[] typesobsG = {"C1C","L1C","S1C","C2W","L2W","S2W","C2X","L2X","S2X","C5X","L5X","S5X"};
        String[] typesobsR = {"C1C","L1C","S1C","C1P","L1P","S1P","C2C","L2C","S2C","C2P","L2P","S2P"};
        String[] typesobsE = {"C1X","L1X","S1X","C5X","L5X","S5X","C7X","L7X","S7X","C8X","L8X","S8X"};
        String[] typesobsC = {"C1I","L1I","S1I","C7I","L7I","S7I","C6I","L6I","S6I"};
        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assertions.assertEquals(51, list.size());
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
        RinexObservationLoader  loader = load("rinex/bbbb0000.00o");
        String[] typesobsG2 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR2 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE2 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        String[] typesobsS2 = {"C1C","L1C","S1C","C5I","L5I","S5I"};
        String[] typesobsC2 = {"C2I","L2I","S2I","C7I","L7I","S7I","C6I","L6I","S6I"};
        String[] typesobsJ2 = {"C1C","L1C","S1C","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assertions.assertEquals(36, list.size());

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
        RinexObservationLoader  loader = load("rinex/bbbb0000.01o");
        String[] typesobsG4 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR4 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE4 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        List<ObservationDataSet> list = loader.getObservationDataSets();
        Assertions.assertEquals(36, list.size());

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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(22, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("THIS IS NOT A RINEX LABEL", ((String) oe.getParts()[2]).substring(60).trim());
        }
    }

    @Test
    public void testMissingHeaderLabel() {
        try {
            //Test with RinexV3 Missing Label inside Header
            load("rinex/missing-label.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex3NoMarkerName() {
        try {
            //Test with RinexV3 Missing MARKER NAME Label inside Header
            load("rinex/no-markerNameV3.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoMarkerName() {
        try {
            //Test with RinexV2 Missing MARKER NAME Label inside Header
            load("rinex/no-markerNameV2.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoObserverAgency() {
        try {
            //Test with RinexV2 Missing OBSERVER / AGENCY Label inside Header
            load("rinex/no-observer-agencyV2.16o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex3NoRecType() {
        try {
            //Test with Rinex3 Missing REC # / TYPE / VERS Label inside Header
            load("rinex/no-rec-typeV3.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoRecType() {
        try {
            //Test with Rinex2 Missing REC # / TYPE / VERS Label inside Header
            load("rinex/no-rec-typeV2.16o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex3NoAntType() {
        try {
            //Test with Rinex3 Missing ANT # / TYPE Label inside Header
            load("rinex/no-ant-typeV3.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoAntType() {
        try {
            //Test with Rinex2 Missing ANT # / TYPE Label inside Header
            load("rinex/no-ant-typeV2.16o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex3ObserverAgency() {
        try {
            //Test with Rinex3 Missing OBSERVER / AGENCY Label inside Header
            load("rinex/no-rec-typeV3.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoApproxPosition() {
        try {
            //Test with Rinex2 Missing APPROX POSITION XYZ Label inside Header
            load("rinex/no-approx-positionV2.16o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoAntennaDelta() {
        try {
            //Test with Rinex2 Missing ANTENNA: DELTA H/E/N Label inside Header
            load("rinex/no-antenna-deltaV2.16o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex3NoAntennaDelta() {
        try {
            //Test with Rinex3 Missing ANTENNA: DELTA H/E/N Label inside Header
            load("rinex/no-antenna-deltaV3.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2NoTimeFirstObs() {
        try {
            //Test with Rinex2 Missing TIME OF FIRST OBS Label inside Header
            load("rinex/no-first-obsV2.16o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex3NoTimeFirstObs() {
        try {
            //Test with Rinex3 Missing TIME OF FIRST OBS Label inside Header
            load("rinex/no-first-obsV3.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testUnknownSatelliteSystemHeader() {
        try {
            //Test with RinexV3 Unknown Satellite System inside Header
            load("rinex/unknown-satsystem.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_SATELLITE_SYSTEM, oe.getSpecifier());
            Assertions.assertEquals('Z', oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentNumSatellites() {
        try {
            //Test with RinexV3 inconsistent number of sats in an observation w/r to max sats in header
            load("rinex/inconsistent-satsnum.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS, oe.getSpecifier());
            Assertions.assertEquals(25, oe.getParts()[3]); //N. max sats
            Assertions.assertEquals(26, oe.getParts()[2]); //N. sats observation incoherent
        }
    }

    @Test
    public void testInconsistentSatSystem() {
        try {
            //Test with RinexV3 inconsistent satellite system in an observation w/r to file sat system
            load("rinex/inconsistent-satsystem.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM, oe.getSpecifier());
            Assertions.assertEquals(SatelliteSystem.GPS, oe.getParts()[2]); //Rinex Satellite System (GPS)
            Assertions.assertEquals(SatelliteSystem.GLONASS, oe.getParts()[3]); //First observation of a sat that is not GPS (GLONASS)
        }
    }

    @Test
    public void testUnknownFrequency() {
        try {
            load("rinex/unknown-rinex-frequency.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_RINEX_FREQUENCY, oe.getSpecifier());
            Assertions.assertEquals("AAA", (String) oe.getParts()[0]);
            Assertions.assertEquals(14, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testDCBSApplied() {
        RinexObservationLoader  loader = load("rinex/dcbs.00o");
        List<ObservationDataSet> l = loader.getObservationDataSets();
        Assertions.assertEquals(51, l.size());
        for (ObservationDataSet dataSet : l) {
            RinexObservationHeader header = dataSet.getHeader();
            List<AppliedDCBS> list = header.getListAppliedDCBS();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
            Assertions.assertEquals("dcbs-program-name", list.get(0).getProgDCBS());
            Assertions.assertEquals("http://example.com/GPS", list.get(0).getSourceDCBS());
            Assertions.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
            Assertions.assertEquals("dcbs-program-name", list.get(1).getProgDCBS());
            Assertions.assertEquals("http://example.com/GLONASS", list.get(1).getSourceDCBS());
        }
    }

    @Test
    public void testPCVSApplied() {
        RinexObservationLoader loader = load("rinex/pcvs.00o");
        List<ObservationDataSet> l = loader.getObservationDataSets();
        Assertions.assertEquals(51, l.size());
        for (ObservationDataSet dataSet : l) {
            RinexObservationHeader header = dataSet.getHeader();
            List<AppliedPCVS> list = header.getListAppliedPCVS();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
            Assertions.assertEquals("pcvs-program-name", list.get(0).getProgPCVS());
            Assertions.assertEquals("http://example.com/GPS", list.get(0).getSourcePCVS());
            Assertions.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
            Assertions.assertEquals("pcvs-program-name", list.get(1).getProgPCVS());
            Assertions.assertEquals("http://example.com/GLONASS", list.get(1).getSourcePCVS());
        }
    }

    @Test
    public void testRinex220Spaceborne() {
        RinexObservationLoader loader = load("rinex/ice12720.07o");
        List<ObservationDataSet> l = loader.getObservationDataSets();
        Assertions.assertEquals(4 * 7, l.size());
        for (int i = 0; i < l.size(); ++i) {
            ObservationDataSet dataSet = l.get(i);
            Assertions.assertEquals("SPACEBORNE", dataSet.getHeader().getMarkerType());
            Assertions.assertEquals(SatelliteSystem.GPS, dataSet.getHeader().getSatelliteSystem());
            switch (i % 7) {
                case 0 :
                    Assertions.assertEquals( 1, dataSet.getPrnNumber());
                    break;
                case 1 :
                    Assertions.assertEquals( 5, dataSet.getPrnNumber());
                    break;
                case 2 :
                    Assertions.assertEquals( 9, dataSet.getPrnNumber());
                    break;
                case 3 :
                    Assertions.assertEquals(12, dataSet.getPrnNumber());
                    break;
                case 4 :
                    Assertions.assertEquals(14, dataSet.getPrnNumber());
                    break;
                case 5 :
                    Assertions.assertEquals(22, dataSet.getPrnNumber());
                    break;
                case 6 :
                    Assertions.assertEquals(30, dataSet.getPrnNumber());
                    break;
            }
            List<ObservationData> list = dataSet.getObservationData();
            Assertions.assertEquals(9, list.size());
            Assertions.assertEquals(ObservationType.L1, list.get(0).getObservationType());
            Assertions.assertEquals(ObservationType.L2, list.get(1).getObservationType());
            Assertions.assertEquals(ObservationType.P1, list.get(2).getObservationType());
            Assertions.assertEquals(ObservationType.P2, list.get(3).getObservationType());
            Assertions.assertEquals(ObservationType.C1, list.get(4).getObservationType());
            Assertions.assertEquals(ObservationType.LA, list.get(5).getObservationType());
            Assertions.assertEquals(ObservationType.S1, list.get(6).getObservationType());
            Assertions.assertEquals(ObservationType.S2, list.get(7).getObservationType());
            Assertions.assertEquals(ObservationType.SA, list.get(8).getObservationType());
        }
    }

    @Test
    public void testRinex220SpaceborneScaled() {
        List<ObservationDataSet> raw    = load("rinex/ice12720.07o").getObservationDataSets();
        List<ObservationDataSet> scaled = load("rinex/ice12720-scaled.07o").getObservationDataSets();
        Assertions.assertEquals(4 * 7, raw.size());
        Assertions.assertEquals(4 * 7, scaled.size());
        for (int i = 0; i < raw.size(); ++i) {

            ObservationDataSet rawDataSet    = raw.get(i);
            Assertions.assertEquals("SPACEBORNE", rawDataSet.getHeader().getMarkerType());
            Assertions.assertEquals(SatelliteSystem.GPS, rawDataSet.getHeader().getSatelliteSystem());

            ObservationDataSet scaledDataSet = scaled.get(i);
            Assertions.assertEquals("SPACEBORNE", scaledDataSet.getHeader().getMarkerType());
            Assertions.assertEquals(SatelliteSystem.GPS, scaledDataSet.getHeader().getSatelliteSystem());

            List<ObservationData> rawList    = rawDataSet.getObservationData();
            List<ObservationData> scaledList = scaledDataSet.getObservationData();
            Assertions.assertEquals(9, rawList.size());
            Assertions.assertEquals(9, scaledList.size());
            for (int j = 0; j < rawList.size(); ++j) {
                final ObservationData rawData    = rawList.get(j);
                final ObservationData scaledData = scaledList.get(j);
                Assertions.assertEquals(rawData.getObservationType(), scaledData.getObservationType());
                Assertions.assertEquals(rawData.getValue(), scaledData.getValue(), FastMath.ulp(rawData.getValue()));
            }
        }
    }

    @Test
    public void testIssue608() {
        //Tests Rinex 3.04 with GPS, GLONASS, Galileo and SBAS Constellations
        RinexObservationLoader  loader = load("rinex/brca083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assertions.assertEquals(5, ods.size());

        Assertions.assertEquals("A 9080",                     ods.get(2).getHeader().getMarkerName());

        // Test GPS
        Assertions.assertEquals(SatelliteSystem.GPS,    ods.get(1).getSatelliteSystem());
        Assertions.assertEquals(9,                      ods.get(1).getPrnNumber());
        Assertions.assertEquals(0.0,                    ods.get(1).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.C1C,    ods.get(1).getObservationData().get(0).getObservationType());
        Assertions.assertEquals(20891534.648,           ods.get(1).getObservationData().get(0).getValue(), 1.0e-15);

        // Test SBAS
        Assertions.assertEquals(SatelliteSystem.SBAS,   ods.get(4).getSatelliteSystem());
        Assertions.assertEquals(120,                    ods.get(4).getPrnNumber());
        Assertions.assertEquals(0.0,                    ods.get(4).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.L1C,    ods.get(4).getObservationData().get(1).getObservationType());
        Assertions.assertEquals(335849.135,           ods.get(4).getObservationData().get(1).getValue(), 1.0e-15);
    }

    @Test
    public void testIssue605() {
        // Test observation type C0, L0, S0 and D0
        RinexObservationLoader  loader = load("rinex/embe083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assertions.assertEquals(5, ods.size());

        // Test Glonass
        Assertions.assertEquals(SatelliteSystem.GLONASS, ods.get(3).getSatelliteSystem());
        Assertions.assertEquals(12,                      ods.get(3).getPrnNumber());
        Assertions.assertEquals(0.0,                     ods.get(3).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(20427680.259,            ods.get(3).getObservationData().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(-885349.430,             ods.get(3).getObservationData().get(1).getValue(), 1.0e-15);
        Assertions.assertEquals(22397545.647,            ods.get(3).getObservationData().get(3).getValue(), 1.0e-15);
        Assertions.assertEquals(37.594,                  ods.get(3).getObservationData().get(4).getValue(), 1.0e-15);
    }

    @Test
    public void testIssue698() {
        // Test missing Beidou observation type for Rinex 3.04
        RinexObservationLoader  loader = load("rinex/abcd083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assertions.assertEquals(2, ods.size());

        // Test Beidou
        Assertions.assertEquals(SatelliteSystem.BEIDOU, ods.get(1).getSatelliteSystem());
        Assertions.assertEquals(6,                      ods.get(1).getPrnNumber());
        Assertions.assertEquals(0.0,                    ods.get(1).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(41,                     ods.get(1).getObservationData().size());

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

        Assertions.assertEquals(system,         obser.getSatelliteSystem());
        Assertions.assertEquals(prnNumber,      obser.getPrnNumber());
        Assertions.assertEquals(date,           obser.getDate());
        Assertions.assertEquals(rcvrClkOffset,  obser.getRcvrClkOffset(), 1.E-17);
        for (int i = 0; i < typesObs.length; i++) {
            final ObservationData od = obser.getObservationData().get(i);
            Assertions.assertEquals(ObservationType.valueOf(typesObs[i]), od.getObservationType());
            if (od.getObservationType() == rf) {
                if (Double.isNaN(obsValue)) {
                    Assertions.assertTrue(Double.isNaN(od.getValue()));
                } else {
                    Assertions.assertEquals(obsValue, od.getValue(), 1.E-3);
                }
                Assertions.assertEquals(lliValue,    od.getLossOfLockIndicator());
                Assertions.assertEquals(sigstrength, od.getSignalStrength());
            }
        }

    }

    private RinexObservationLoader load(final String name) {
        return new RinexObservationLoader(new DataSource(name,
                                              () -> Utils.class.getClassLoader().getResourceAsStream(name)));
     }

    private RinexObservationLoader loadCompressed(final String name) {
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                              () -> Utils.class.getClassLoader().getResourceAsStream(name));
        DataSource filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        return new RinexObservationLoader(filtered);
     }

}
