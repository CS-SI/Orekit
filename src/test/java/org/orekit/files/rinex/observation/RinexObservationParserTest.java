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
package org.orekit.files.rinex.observation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.HatanakaCompressFilter;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class RinexObservationParserTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testDefaultLoadRinex2() {
        Assertions.assertEquals(24, load("rinex/aiub0000.00o").getObservationDataSets().size());
    }

    @Test
    public void testDefaultLoadRinex3() {
        Utils.setDataRoot("regular-data:rinex");
        Assertions.assertEquals(5, load("rinex/brca083.06o").getObservationDataSets().size());
    }

    @Test
    public void testReadError() {
        try {
            new RinexObservationParser().parse(new DataSource("read-error", () -> new InputStream() {
                public int read() throws IOException {
                    throw new IOException("boo!");
                }
            }));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.SIMPLE_MESSAGE, oe.getSpecifier());
            Assertions.assertEquals("boo!", oe.getParts()[0]);
            Assertions.assertInstanceOf(IOException.class, oe.getCause());
        }
    }

    @Test
    public void testWrongVersion() {
        try {
            load("rinex/unknown-version.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT_VERSION, oe.getSpecifier());
            Assertions.assertEquals(9.99, ((Double) oe.getParts()[0]).doubleValue(), 0.001);
        }
    }

    @Test
    public void testWrongFileType() {
        try {
            load("rinex/unknown-type.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.WRONG_PARSING_TYPE, oe.getSpecifier());
        }
    }

    @Test
    public void testShortFirstLine() {
        try {
            load("rinex/short-first-line.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testWrongFirstLabel() {
        try {
            load("rinex/unknown-first-label.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testRinex2OptionalRecords() {
        final RinexObservation loaded = load("rinex/cccc0000.07o");
        final RinexObservationHeader   header = loaded.getHeader();
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
        final RinexObservation loaded = load("rinex/dddd0000.01o");
        final RinexObservationHeader   header = loaded.getHeader();
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
                            FastMath.toDegrees(header.getAntennaAzimuth()),
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
        final RinexObservation loaded = load("rinex/jnu10110.17o");
        Assertions.assertEquals(44, loaded.getObservationDataSets().size());
        final RinexObservationHeader header = loaded.getHeader();

        Assertions.assertEquals(2.11, header.getFormatVersion(), 1.0e-15);
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

    @Test
    public void testRinex3Header() {

        //Tests Rinex 3 with Multiple Constellations
        final RinexObservation loaded = load("rinex/aaaa0000.00o");
        final RinexObservationHeader header = loaded.getHeader();

        Assertions.assertEquals(3.02, header.getFormatVersion(), 1.0e-15);
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
        Assertions.assertTrue(header.getPhaseShiftCorrections().get(0).getSatsCorrected().isEmpty());
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

    @Test
    public void testGPSFile() {

        //Tests Rinex 2 with only GPS Constellation
        final List<ObservationDataSet> list = load("rinex/jnu10110.17o").getObservationDataSets();
        String[] typesobs = {"L1","L2","P1","P2","C1","S1","S2"};

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
        final List<ObservationDataSet> ods = loadCompressed("rinex/bogi1210.09d.Z").getObservationDataSets();
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
        List<ObservationDataSet> list = load("rinex/aiub0000.00o").getObservationDataSets();
        String[] typesobs2 = {"P1","L1","L2","P2"};

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
        List<ObservationDataSet> list = load("rinex/aaaa0000.00o").getObservationDataSets();

        String[] typesobsG = {"C1C","L1C","S1C","C2W","L2W","S2W","C2X","L2X","S2X","C5X","L5X","S5X"};
        String[] typesobsR = {"C1C","L1C","S1C","C1P","L1P","S1P","C2C","L2C","S2C","C2P","L2P","S2P"};
        String[] typesobsE = {"C1X","L1X","S1X","C5X","L5X","S5X","C7X","L7X","S7X","C8X","L8X","S8X"};
        String[] typesobsC = {"C1I","L1I","S1I","C7I","L7I","S7I","C6I","L6I","S6I"};
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
        final RinexObservation obs = load("rinex/bbbb0000.00o");
        Assertions.assertEquals(87, obs.getHeader().getNbSat());

        List<ObservationDataSet> list = obs.getObservationDataSets();
        String[] typesobsG2 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR2 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE2 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        String[] typesobsS2 = {"C1C","L1C","S1C","C5I","L5I","S5I"};
        String[] typesobsC2 = {"C2I","L2I","S2I","C7I","L7I","S7I","C6I","L6I","S6I"};
        String[] typesobsJ2 = {"C1C","L1C","S1C","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
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
        List<ObservationDataSet> list = load("rinex/bbbb0000.01o").getObservationDataSets();
        String[] typesobsG4 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR4 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE4 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
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
    public void testContinuationPhaseShift() {
        List<PhaseShiftCorrection> shifts = load("rinex/continuation-phase-shift.23o").getHeader().getPhaseShiftCorrections();
        Assertions.assertEquals(4, shifts.size());
        Assertions.assertEquals(SatelliteSystem.GPS,  shifts.get(0).getSatelliteSystem());
        Assertions.assertEquals(ObservationType.L1C,  shifts.get(0).getTypeObs());
        Assertions.assertEquals(                0.0,  shifts.get(0).getCorrection(), 1.0e-15);
        Assertions.assertEquals(                 30,  shifts.get(0).getSatsCorrected().size());
        Assertions.assertEquals(                 19,  shifts.get(0).getSatsCorrected().get( 0).getPRN());
        Assertions.assertEquals(                 14,  shifts.get(0).getSatsCorrected().get( 9).getPRN());
        Assertions.assertEquals(                  2,  shifts.get(0).getSatsCorrected().get(10).getPRN());
        Assertions.assertEquals(                 26,  shifts.get(0).getSatsCorrected().get(19).getPRN());
        Assertions.assertEquals(                 27,  shifts.get(0).getSatsCorrected().get(20).getPRN());
        Assertions.assertEquals(                 29,  shifts.get(0).getSatsCorrected().get(29).getPRN());
        Assertions.assertEquals(SatelliteSystem.GPS,  shifts.get(1).getSatelliteSystem());
        Assertions.assertEquals(ObservationType.L2W,  shifts.get(1).getTypeObs());
        Assertions.assertEquals(                0.0,  shifts.get(1).getCorrection(), 1.0e-15);
        Assertions.assertEquals(                 30,  shifts.get(1).getSatsCorrected().size());
        Assertions.assertEquals(                 19,  shifts.get(1).getSatsCorrected().get( 0).getPRN());
        Assertions.assertEquals(                 12,  shifts.get(1).getSatsCorrected().get( 9).getPRN());
        Assertions.assertEquals(                 28,  shifts.get(1).getSatsCorrected().get(10).getPRN());
        Assertions.assertEquals(                 26,  shifts.get(1).getSatsCorrected().get(19).getPRN());
        Assertions.assertEquals(                  9,  shifts.get(1).getSatsCorrected().get(20).getPRN());
        Assertions.assertEquals(                 29,  shifts.get(1).getSatsCorrected().get(29).getPRN());
        Assertions.assertEquals(SatelliteSystem.QZSS, shifts.get(2).getSatelliteSystem());
        Assertions.assertEquals(ObservationType.L1C,  shifts.get(2).getTypeObs());
        Assertions.assertEquals(                0.0,  shifts.get(2).getCorrection(), 1.0e-15);
        Assertions.assertEquals(                  3,  shifts.get(2).getSatsCorrected().size());
        Assertions.assertEquals(                  3,  shifts.get(2).getSatsCorrected().get( 0).getTwoDigitsRinexPRN());
        Assertions.assertEquals(                  2,  shifts.get(2).getSatsCorrected().get( 1).getTwoDigitsRinexPRN());
        Assertions.assertEquals(                  4,  shifts.get(2).getSatsCorrected().get( 2).getTwoDigitsRinexPRN());
        Assertions.assertEquals(                195,  shifts.get(2).getSatsCorrected().get( 0).getPRN());
        Assertions.assertEquals(                194,  shifts.get(2).getSatsCorrected().get( 1).getPRN());
        Assertions.assertEquals(                196,  shifts.get(2).getSatsCorrected().get( 2).getPRN());
        Assertions.assertEquals(SatelliteSystem.QZSS, shifts.get(3).getSatelliteSystem());
        Assertions.assertEquals(ObservationType.L2S,  shifts.get(3).getTypeObs());
        Assertions.assertEquals(                0.0,  shifts.get(3).getCorrection(), 1.0e-15);
        Assertions.assertEquals(                  3,  shifts.get(3).getSatsCorrected().size());
        Assertions.assertEquals(                  3,  shifts.get(3).getSatsCorrected().get( 0).getTwoDigitsRinexPRN());
        Assertions.assertEquals(                  2,  shifts.get(3).getSatsCorrected().get( 1).getTwoDigitsRinexPRN());
        Assertions.assertEquals(                  4,  shifts.get(3).getSatsCorrected().get( 2).getTwoDigitsRinexPRN());
        Assertions.assertEquals(                195,  shifts.get(3).getSatsCorrected().get( 0).getPRN());
        Assertions.assertEquals(                194,  shifts.get(3).getSatsCorrected().get( 1).getPRN());
        Assertions.assertEquals(                196,  shifts.get(3).getSatsCorrected().get( 2).getPRN());
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
    public void testNumberFormatError() {
        try {
            load("rinex/number-format-error.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(33, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("-45####.120", ((String) oe.getParts()[2]).substring(21, 33).trim());
        }
    }

    @Test
    public void testUnsupportedTimeScale() {
        try {
            load("rinex/unsupported-time-scale.06o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(18, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("XYZ", ((String) oe.getParts()[2]).substring(48, 51).trim());
        }
    }

    @Test
    public void testNoTimeScale() {
        try {
            load("rinex/no-time-scale.07o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(18, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("", ((String) oe.getParts()[2]).substring(48, 51).trim());
        }
    }

    @Test
    public void testInconsistentSatelliteSystem() {
        try {
            load("rinex/inconsistent-satellite-system.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM, oe.getSpecifier());
            Assertions.assertEquals(SatelliteSystem.GPS,     oe.getParts()[3]);
            Assertions.assertEquals(SatelliteSystem.GALILEO, oe.getParts()[2]);
        }
    }

    @Test
    public void testInconsistentNumberOfSatellites() {
        try {
            load("rinex/inconsistent-number-of-sats.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS, oe.getSpecifier());
            Assertions.assertEquals(3, ((Integer) oe.getParts()[2]).intValue());
            Assertions.assertEquals(2, ((Integer) oe.getParts()[3]).intValue());
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
        //Test with RinexV3 Unknown Satellite System inside Header
        final RinexObservation robs = load("rinex/unknown-satsystem.00o");
        Assertions.assertEquals(9, robs.getHeader().getTypeObs().get(SatelliteSystem.USER_DEFINED_Z).size());
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
    public void testUnknownEventFlag3() {
        try {
            load("rinex/unknown-event-flag-3.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(61, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testUnknownEventFlag4() {
        try {
            load("rinex/unknown-event-flag-4.00o");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(53, ((Integer) oe.getParts()[0]).intValue());
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
        RinexObservation l = load("rinex/dcbs.00o");
        Assertions.assertEquals(51, l.getObservationDataSets().size());
        RinexObservationHeader header = l.getHeader();
        List<AppliedDCBS> list = header.getListAppliedDCBS();
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
        Assertions.assertEquals("dcbs-program-name", list.get(0).getProgDCBS());
        Assertions.assertEquals("http://example.com/GPS", list.get(0).getSourceDCBS());
        Assertions.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
        Assertions.assertEquals("dcbs-program-name", list.get(1).getProgDCBS());
        Assertions.assertEquals("http://example.com/GLONASS", list.get(1).getSourceDCBS());
        Assertions.assertEquals( 4, header.getTypeObs().size());
        Assertions.assertEquals(12, header.getTypeObs().get(SatelliteSystem.GPS).size());
        Assertions.assertEquals(12, header.getTypeObs().get(SatelliteSystem.GLONASS).size());
        Assertions.assertEquals(12, header.getTypeObs().get(SatelliteSystem.GALILEO).size());
        Assertions.assertEquals( 9, header.getTypeObs().get(SatelliteSystem.BEIDOU).size());
        Assertions.assertEquals(ObservationType.C1I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(0));
        Assertions.assertEquals(ObservationType.L1I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(1));
        Assertions.assertEquals(ObservationType.S1I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(2));
        Assertions.assertEquals(ObservationType.C7I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(3));
        Assertions.assertEquals(ObservationType.L7I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(4));
        Assertions.assertEquals(ObservationType.S7I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(5));
        Assertions.assertEquals(ObservationType.C6I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(6));
        Assertions.assertEquals(ObservationType.L6I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(7));
        Assertions.assertEquals(ObservationType.S6I, header.getTypeObs().get(SatelliteSystem.BEIDOU).get(8));
    }

    @Test
    public void testPCVSApplied() {
        RinexObservation l = load("rinex/pcvs.00o");
        Assertions.assertEquals(51, l.getObservationDataSets().size());
        RinexObservationHeader header = l.getHeader();
        List<AppliedPCVS> list = header.getListAppliedPCVS();
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
        Assertions.assertEquals("pcvs-program-name", list.get(0).getProgPCVS());
        Assertions.assertEquals("http://example.com/GPS", list.get(0).getSourcePCVS());
        Assertions.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
        Assertions.assertEquals("pcvs-program-name", list.get(1).getProgPCVS());
        Assertions.assertEquals("http://example.com/GLONASS", list.get(1).getSourcePCVS());
    }

    @Test
    public void testCycleSlip() {
        RinexObservation l = load("rinex/cycle-slip.00o");
        Assertions.assertEquals(51, l.getObservationDataSets().size());
        RinexObservationHeader header = l.getHeader();
        List<AppliedPCVS> list = header.getListAppliedPCVS();
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(SatelliteSystem.GPS, list.get(0).getSatelliteSystem());
        Assertions.assertEquals("pcvs-program-name", list.get(0).getProgPCVS());
        Assertions.assertEquals("http://example.com/GPS", list.get(0).getSourcePCVS());
        Assertions.assertEquals(SatelliteSystem.GLONASS, list.get(1).getSatelliteSystem());
        Assertions.assertEquals("pcvs-program-name", list.get(1).getProgPCVS());
        Assertions.assertEquals("http://example.com/GLONASS", list.get(1).getSourcePCVS());
    }

    @Test
    public void testRinex220Spaceborne() {
        RinexObservation l = load("rinex/ice12720.07o");
        Assertions.assertEquals("SPACEBORNE", l.getHeader().getMarkerType());
        Assertions.assertEquals(SatelliteSystem.GPS, l.getHeader().getSatelliteSystem());
        Assertions.assertEquals(4 * 7, l.getObservationDataSets().size());
        for (int i = 0; i < l.getObservationDataSets().size(); ++i) {
            ObservationDataSet dataSet = l.getObservationDataSets().get(i);
            switch (i % 7) {
                case 0 :
                    Assertions.assertEquals( 1, dataSet.getSatellite().getPRN());
                    break;
                case 1 :
                    Assertions.assertEquals( 5, dataSet.getSatellite().getPRN());
                    break;
                case 2 :
                    Assertions.assertEquals( 9, dataSet.getSatellite().getPRN());
                    break;
                case 3 :
                    Assertions.assertEquals(12, dataSet.getSatellite().getPRN());
                    break;
                case 4 :
                    Assertions.assertEquals(14, dataSet.getSatellite().getPRN());
                    break;
                case 5 :
                    Assertions.assertEquals(22, dataSet.getSatellite().getPRN());
                    break;
                case 6 :
                    Assertions.assertEquals(30, dataSet.getSatellite().getPRN());
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
        RinexObservation raw   = load("rinex/ice12720.07o");
        RinexObservation scaled = load("rinex/ice12720-scaled.07o");
        Assertions.assertEquals(4 * 7, raw.getObservationDataSets().size());
        Assertions.assertEquals(4 * 7, scaled.getObservationDataSets().size());
        for (int i = 0; i < raw.getObservationDataSets().size(); ++i) {

            ObservationDataSet rawDataSet    = raw.getObservationDataSets().get(i);
            Assertions.assertEquals("SPACEBORNE", raw.getHeader().getMarkerType());
            Assertions.assertEquals(SatelliteSystem.GPS, raw.getHeader().getSatelliteSystem());

            ObservationDataSet scaledDataSet = scaled.getObservationDataSets().get(i);
            Assertions.assertEquals("SPACEBORNE", scaled.getHeader().getMarkerType());
            Assertions.assertEquals(SatelliteSystem.GPS, scaled.getHeader().getSatelliteSystem());

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
        RinexObservation ods = load("rinex/brca083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        Assertions.assertEquals(5, ods.getObservationDataSets().size());

        Assertions.assertEquals("A 9080",                     ods.getHeader().getMarkerName());

        // Test GPS
        Assertions.assertEquals(SatelliteSystem.GPS,    ods.getObservationDataSets().get(1).getSatellite().getSystem());
        Assertions.assertEquals(9,                      ods.getObservationDataSets().get(1).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                    ods.getObservationDataSets().get(1).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.C1C,    ods.getObservationDataSets().get(1).getObservationData().get(0).getObservationType());
        Assertions.assertEquals(20891534.648,           ods.getObservationDataSets().get(1).getObservationData().get(0).getValue(), 1.0e-15);

        // Test SBAS
        Assertions.assertEquals(SatelliteSystem.SBAS,   ods.getObservationDataSets().get(4).getSatellite().getSystem());
        Assertions.assertEquals(120,                    ods.getObservationDataSets().get(4).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                    ods.getObservationDataSets().get(4).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.L1C,    ods.getObservationDataSets().get(4).getObservationData().get(1).getObservationType());
        Assertions.assertEquals(335849.135,             ods.getObservationDataSets().get(4).getObservationData().get(1).getValue(), 1.0e-15);
    }

    @Test
    public void testIssue605() {
        // Test observation type C0, L0, S0 and D0
        RinexObservation ods = load("rinex/embe083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        Assertions.assertEquals(5, ods.getObservationDataSets().size());

        // Test Glonass
        Assertions.assertEquals(SatelliteSystem.GLONASS, ods.getObservationDataSets().get(3).getSatellite().getSystem());
        Assertions.assertEquals(12,                      ods.getObservationDataSets().get(3).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                     ods.getObservationDataSets().get(3).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(20427680.259,            ods.getObservationDataSets().get(3).getObservationData().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(-885349.430,             ods.getObservationDataSets().get(3).getObservationData().get(1).getValue(), 1.0e-15);
        Assertions.assertEquals(22397545.647,            ods.getObservationDataSets().get(3).getObservationData().get(3).getValue(), 1.0e-15);
        Assertions.assertEquals(37.594,                  ods.getObservationDataSets().get(3).getObservationData().get(4).getValue(), 1.0e-15);
    }

    @Test
    public void testIssue698() {
        // Test missing Beidou observation type for Rinex 3.04
        RinexObservation ods = load("rinex/abcd083.06o");
        AbsoluteDate t0 = new AbsoluteDate(2016, 3, 24, 13, 10, 36.0, TimeScalesFactory.getGPS());
        Assertions.assertEquals(2, ods.getObservationDataSets().size());

        // Test Beidou
        Assertions.assertEquals(SatelliteSystem.BEIDOU, ods.getObservationDataSets().get(1).getSatellite().getSystem());
        Assertions.assertEquals(6,                      ods.getObservationDataSets().get(1).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                    ods.getObservationDataSets().get(1).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(41,                     ods.getObservationDataSets().get(1).getObservationData().size());

    }

    @Test
    public void testGlonass() {
        RinexObservationHeader header = load("rinex/abcd083.06o").getHeader();
        List<GlonassSatelliteChannel> channels = header.getGlonassChannels();
        Assertions.assertEquals(18, channels.size());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 0).getSatellite().getSystem());
        Assertions.assertEquals( 1,                      channels.get( 0).getSatellite().getPRN());
        Assertions.assertEquals( 1,                      channels.get( 0).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 1).getSatellite().getSystem());
        Assertions.assertEquals( 2,                      channels.get( 1).getSatellite().getPRN());
        Assertions.assertEquals( 2,                      channels.get( 1).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 2).getSatellite().getSystem());
        Assertions.assertEquals( 3,                      channels.get( 2).getSatellite().getPRN());
        Assertions.assertEquals( 3,                      channels.get( 2).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 3).getSatellite().getSystem());
        Assertions.assertEquals( 4,                      channels.get( 3).getSatellite().getPRN());
        Assertions.assertEquals( 4,                      channels.get( 3).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 4).getSatellite().getSystem());
        Assertions.assertEquals( 5,                      channels.get( 4).getSatellite().getPRN());
        Assertions.assertEquals( 5,                      channels.get( 4).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 5).getSatellite().getSystem());
        Assertions.assertEquals( 6,                      channels.get( 5).getSatellite().getPRN());
        Assertions.assertEquals(-6,                      channels.get( 5).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 6).getSatellite().getSystem());
        Assertions.assertEquals( 7,                      channels.get( 6).getSatellite().getPRN());
        Assertions.assertEquals(-5,                      channels.get( 6).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 7).getSatellite().getSystem());
        Assertions.assertEquals( 8,                      channels.get( 7).getSatellite().getPRN());
        Assertions.assertEquals(-4,                      channels.get( 7).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 8).getSatellite().getSystem());
        Assertions.assertEquals( 9,                      channels.get( 8).getSatellite().getPRN());
        Assertions.assertEquals(-3,                      channels.get( 8).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get( 9).getSatellite().getSystem());
        Assertions.assertEquals(10,                      channels.get( 9).getSatellite().getPRN());
        Assertions.assertEquals(-2,                      channels.get( 9).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(10).getSatellite().getSystem());
        Assertions.assertEquals(11,                      channels.get(10).getSatellite().getPRN());
        Assertions.assertEquals(-1,                      channels.get(10).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(11).getSatellite().getSystem());
        Assertions.assertEquals(12,                      channels.get(11).getSatellite().getPRN());
        Assertions.assertEquals( 0,                      channels.get(11).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(12).getSatellite().getSystem());
        Assertions.assertEquals(13,                      channels.get(12).getSatellite().getPRN());
        Assertions.assertEquals( 1,                      channels.get(12).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(13).getSatellite().getSystem());
        Assertions.assertEquals(14,                      channels.get(13).getSatellite().getPRN());
        Assertions.assertEquals( 2,                      channels.get(13).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(14).getSatellite().getSystem());
        Assertions.assertEquals(15,                      channels.get(14).getSatellite().getPRN());
        Assertions.assertEquals( 0,                      channels.get(14).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(15).getSatellite().getSystem());
        Assertions.assertEquals(16,                      channels.get(15).getSatellite().getPRN());
        Assertions.assertEquals( 4,                      channels.get(15).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(16).getSatellite().getSystem());
        Assertions.assertEquals(17,                      channels.get(16).getSatellite().getPRN());
        Assertions.assertEquals( 5,                      channels.get(16).getK());
        Assertions.assertEquals(SatelliteSystem.GLONASS, channels.get(17).getSatellite().getSystem());
        Assertions.assertEquals(18,                      channels.get(17).getSatellite().getPRN());
        Assertions.assertEquals(-5,                      channels.get(17).getK());
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

        Assertions.assertEquals(system,         obser.getSatellite().getSystem());
        Assertions.assertEquals(prnNumber,      obser.getSatellite().getPRN());
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

    private RinexObservation load(final String name) {
        final DataSource dataSource = new DataSource(name, () -> Utils.class.getClassLoader().getResourceAsStream(name));
        return new RinexObservationParser().parse(dataSource);
     }

    private RinexObservation loadCompressed(final String name) {
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                              () -> Utils.class.getClassLoader().getResourceAsStream(name));
        DataSource filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        return new RinexObservationParser().parse(filtered);
     }

}
