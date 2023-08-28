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
package org.orekit.files.rinex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.GzipFilter;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.files.rinex.observation.RinexObservation;
import org.orekit.files.rinex.observation.RinexObservationParser;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class HatanakaCompressFilterTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testNotFiltered() throws IOException {

        final String name = "rinex/aaaa0000.00o";
        final DataSource raw = new DataSource(name,
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        final DataSource filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        Assertions.assertSame(raw, filtered);
    }

    @Test
    public void testWrongVersion() throws IOException {
        doTestWrong("rinex/vers9990.01d", OrekitMessages.UNSUPPORTED_FILE_FORMAT);
    }

    @Test
    public void testWrongFirstLabel() throws IOException {
        doTestWrong("rinex/labl8880.01d", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testWrongSecondLabel() throws IOException {
        doTestWrong("rinex/labl9990.01d", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testTruncatedAtReceiverClockLine() throws IOException {
        doTestWrong("rinex/truncateA_U_20190320000_15M_30S_MO.crx", OrekitMessages.UNEXPECTED_END_OF_FILE);
    }

    @Test
    public void testTruncatedBeforeDifferencedValue() throws IOException {
        doTestWrong("rinex/truncateB_U_20190320000_15M_30S_MO.crx", OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE);
    }

    @Test
    public void testTruncatedCompactHeader() throws IOException {
        doTestWrong("rinex/truncateC_U_20190320000_15M_30S_MO.crx", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testEmpty() throws IOException {
        doTestWrong("rinex/emptyFile_U_20190320000_15M_30S_MO.crx", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testBadClockReset() throws IOException {
        doTestWrong("rinex/badclkrst_U_20190320000_10M_10M_MO.crx", OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE);
    }

    private void doTestWrong(final String name, final OrekitMessages expectedError)
        throws IOException {
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        try {
            try (Reader            r   = new HatanakaCompressFilter().filter(raw).getOpener().openReaderOnce();
                 BufferedReader    br  = new BufferedReader(r)) {
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    // nothing to do here
                }
                Assertions.fail("an exception should have been thrown");
            }
        } catch (OrekitException oe) {
            Assertions.assertEquals(expectedError, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2MoreThan12Satellites() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/bogi1210.09d.Z";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw)),
                                         "7b1556a1f582b4e037b6bae7e31672370fbea23ebb9289ceebcefefa898afe9c");
        RinexObservationParser parser = new RinexObservationParser();

        List<ObservationDataSet> ods = parser.parse(digester.getDigestedSource()).getObservationDataSets();
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

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testHatanakaRinex2() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/arol0090.01d.Z";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw)),
                                         "2ec64a396f19c09e70d0748e01ebb0f96d4961fdfd3ba8f023011de40b52c2b4");
        RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2001, 1, 9, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(921, ods.size());

        Assertions.assertEquals("AROL",              rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.GPS, ods.get(0).getSatellite().getSystem());
        Assertions.assertEquals(24,                  ods.get(0).getSatellite().getPRN());
        Assertions.assertEquals(90.0,                ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(7,                   ods.get(0).getObservationData().size());
        Assertions.assertEquals(-3351623.823,        ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(-2502276.763,        ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(21472157.836,        ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(21472163.602,        ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);
        Assertions.assertTrue(Double.isNaN(ods.get(0).getObservationData().get(4).getValue()));
        Assertions.assertEquals(18.7504,             ods.get(0).getObservationData().get(5).getValue(), 1.0e-3);
        Assertions.assertEquals(19.7504,             ods.get(0).getObservationData().get(6).getValue(), 1.0e-3);

        Assertions.assertEquals(SatelliteSystem.GPS, ods.get(447).getSatellite().getSystem());
        Assertions.assertEquals(10,                  ods.get(447).getSatellite().getPRN());
        Assertions.assertEquals(2310.0,              ods.get(447).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(7,                   ods.get(447).getObservationData().size());
        Assertions.assertEquals(-8892260.422,        ods.get(447).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(-6823186.119,        ods.get(447).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(22280029.148,        ods.get(447).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(22280035.160,        ods.get(447).getObservationData().get(3).getValue(), 1.0e-3);
        Assertions.assertTrue(Double.isNaN(ods.get(447).getObservationData().get(4).getValue()));
        Assertions.assertEquals(14.2504,             ods.get(447).getObservationData().get(5).getValue(), 1.0e-3);
        Assertions.assertEquals(13.2504,             ods.get(447).getObservationData().get(6).getValue(), 1.0e-3);

        Assertions.assertEquals(SatelliteSystem.GPS, ods.get(920).getSatellite().getSystem());
        Assertions.assertEquals(31,                  ods.get(920).getSatellite().getPRN());
        Assertions.assertEquals(71430.0,             ods.get(920).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(7,                   ods.get(920).getObservationData().size());
        Assertions.assertEquals(-3993480.91843,      ods.get(920).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(-3363000.11542,      ods.get(920).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(24246301.1804,       ods.get(920).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(24246308.9304,       ods.get(920).getObservationData().get(3).getValue(), 1.0e-3);
        Assertions.assertTrue(Double.isNaN(ods.get(920).getObservationData().get(4).getValue()));
        Assertions.assertEquals(6.2504,              ods.get(920).getObservationData().get(5).getValue(), 1.0e-3);
        Assertions.assertEquals(2.2504,              ods.get(920).getObservationData().get(6).getValue(), 1.0e-3);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testCompressedRinex3() throws IOException, NoSuchAlgorithmException {

        //Tests Rinex 3 with Hatanaka compression
        final String name = "rinex/GANP00SVK_R_20151890000_01H_10M_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                         "680cf9145f416d92458afc82aabd9cef3460c27f33837686fa0535195df379fe");
        RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2015, 7, 8, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(188, ods.size());

        Assertions.assertEquals("GANP",                  rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,  ods.get(0).getSatellite().getSystem());
        Assertions.assertEquals(2,                       ods.get(0).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                     ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(6,                       ods.get(0).getObservationData().size());
        Assertions.assertEquals(40517356.773,            ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(40517351.688,            ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(210984654.306,           ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(163146718.773,           ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);
        Assertions.assertEquals(35.400,                  ods.get(0).getObservationData().get(4).getValue(), 1.0e-3);
        Assertions.assertEquals(37.900,                  ods.get(0).getObservationData().get(5).getValue(), 1.0e-3);

        Assertions.assertEquals(SatelliteSystem.GLONASS, ods.get(96).getSatellite().getSystem());
        Assertions.assertEquals(20,                      ods.get(96).getSatellite().getPRN());
        Assertions.assertEquals(1200.0,                  ods.get(96).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(12,                      ods.get(96).getObservationData().size());
        Assertions.assertEquals(21579038.953,            ods.get(96).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(21579038.254,            ods.get(96).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(21579044.469,            ods.get(96).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(21579043.914,            ods.get(96).getObservationData().get(3).getValue(), 1.0e-3);
        Assertions.assertEquals(115392840.925,           ods.get(96).getObservationData().get(4).getValue(), 1.0e-3);
        Assertions.assertEquals(115393074.174,           ods.get(96).getObservationData().get(5).getValue(), 1.0e-3);
        Assertions.assertEquals(89750072.711,            ods.get(96).getObservationData().get(6).getValue(), 1.0e-3);
        Assertions.assertEquals(89750023.963,            ods.get(96).getObservationData().get(7).getValue(), 1.0e-3);
        Assertions.assertEquals(43.800,                  ods.get(96).getObservationData().get(8).getValue(), 1.0e-3);
        Assertions.assertEquals(42.500,                  ods.get(96).getObservationData().get(9).getValue(), 1.0e-3);
        Assertions.assertEquals(44.000,                  ods.get(96).getObservationData().get(10).getValue(), 1.0e-3);
        Assertions.assertEquals(44.000,                  ods.get(96).getObservationData().get(11).getValue(), 1.0e-3);

        Assertions.assertEquals(SatelliteSystem.SBAS,    ods.get(187).getSatellite().getSystem());
        Assertions.assertEquals(126,                     ods.get(187).getSatellite().getPRN());
        Assertions.assertEquals(3000.0,                  ods.get(187).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(3,                       ods.get(187).getObservationData().size());
        Assertions.assertEquals(38446689.984,            ods.get(187).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(202027899.813,           ods.get(187).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(40.200,                  ods.get(187).getObservationData().get(2).getValue(), 1.0e-3);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testClockReset() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/clckReset_U_20190320000_10M_10M_MO.crx";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(raw),
                                         "b54f4ec3fb860a032f93f569199224e247b35ceba309bc96478779ff7120455a");
        RinexObservationParser parser = new RinexObservationParser();

        List<ObservationDataSet> ods = parser.parse(digester.getDigestedSource()).getObservationDataSets();
        Assertions.assertEquals(23, ods.size());
        final AbsoluteDate t0 = ods.get(0).getDate();
        for (final ObservationDataSet dataSet : ods) {
            if (dataSet.getDate().durationFrom(t0) < 0.001) {
                Assertions.assertEquals(0.123456789012, dataSet.getRcvrClkOffset(), 1.0e-15);
            } else {
                Assertions.assertEquals(0.999999999999, dataSet.getRcvrClkOffset(), 1.0e-15);
            }
        }

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testWith5thOrderDifferencesClockOffsetReinitialization() throws IOException, NoSuchAlgorithmException {

        // the following file has several specific features with respect to Hatanaka compression
        //  - we created it using 5th order differences instead of standard 3rd order
        //  - epoch lines do contain a clock offset (which is a dummy value manually edited from original IGS file)
        //  - differences are reinitialized every 20 epochs
        final String name = "rinex/ZIMM00CHE_R_20190320000_15M_30S_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                         "6ac7c9160d2131581156b2ea88c0c5844b88e1369c3a03956cb77f919c5cca3e");
        RinexObservationParser parser = new RinexObservationParser();

        List<ObservationDataSet> ods = parser.parse(digester.getDigestedSource()).getObservationDataSets();
        Assertions.assertEquals(349, ods.size());
        for (final ObservationDataSet dataSet : ods) {
            Assertions.assertEquals(0.123456789012, dataSet.getRcvrClkOffset(), 1.0e-15);
        }
        ObservationDataSet last = ods.get(ods.size() - 1);
        Assertions.assertEquals( 24815572.703, last.getObservationData().get(0).getValue(), 1.0e-4);
        Assertions.assertEquals(130406727.683, last.getObservationData().get(1).getValue(), 1.0e-4);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testSplice() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/aber0440.16d.Z";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw)),
                                         "b2bc4c32c144f8e6fdda15c9a041c17cbd6b48653c0866dd121f9ad5663f3895");
        RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2016, 2, 13, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(114, ods.size());

        Assertions.assertEquals("ABER",              rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.GPS, ods.get(0).getSatellite().getSystem());
        Assertions.assertEquals(18,                  ods.get(0).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                 ods.get(0).getDate().durationFrom(t0), 1.0e-15);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testVerySmallValue() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/abmf0440.16d.Z";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw)),
                                         "8eee596cd333bb784ece5a7afd94ab1674f27af58ede842873d952414a39998f");
        RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2016, 2, 13, 0, 10, 0.0, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(77, ods.size());

        Assertions.assertEquals("ABMF",              rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.GPS, ods.get(59).getSatellite().getSystem());
        Assertions.assertEquals(23,                  ods.get(59).getSatellite().getPRN());
        Assertions.assertEquals(60.0,                ods.get(59).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.D2,  ods.get(59).getObservationData().get(7).getObservationType());
        Assertions.assertEquals(-0.096,              ods.get(59).getObservationData().get(7).getValue(), 1.0e-15);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testMultipleOf5Observations() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/arev0440.16d.Z";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw)),
                                         "4e5d77c4f4b21f9c995da88b4e1efd75d0e808e3e531ec815f95c4a8652fba8f");
        RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2016, 2, 13, 0, 0, 0.0, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(32, ods.size());

        Assertions.assertEquals("AREV",                  rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.GLONASS, ods.get(16).getSatellite().getSystem());
        Assertions.assertEquals(22,                      ods.get(16).getSatellite().getPRN());
        Assertions.assertEquals(30.0,                    ods.get(16).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.L2,      ods.get(16).getObservationData().get(1).getObservationType());
        Assertions.assertEquals(79103696.341,            ods.get(16).getObservationData().get(1).getValue(), 1.0e-15);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testSingleByteReads() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/arev0440.16d.Z";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        DataSource filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        try (InputStream is = filtered.getOpener().openStreamOnce()) {
            int count = 0;
            while (is.read() >= 0) {
                ++count;
            }
            Assertions.assertEquals(7060, count);
        }

    }

    @Test
    public void testDifferential3rdOrder() {
        doTestDifferential(15, 3, 3,
                           new long[] {
                               40517356773l, -991203l, -38437l,
                               3506l, -630l, 2560l
                           }, new String[] {
                               "   40517356.773", "   40516365.570", "   40515335.930",
                               "   40514271.359", "   40513171.227", "   40512038.094"
                           });
    }

    @Test
    public void testDifferential5thOrder() {
        doTestDifferential(12, 5, 5,
                           new long[] {
                               23439008766l, -19297641l, 30704l, 3623l, -8215l,
                               14517l, -6644l, -2073l, 4164l, -2513l
                           }, new String[] {
                               "234390.08766", "234197.11125", "234004.44188", "233812.11578", "233620.08703",
                               "233428.37273", "233236.98656", "233045.91805", "232855.17422", "232664.75445"
                           });
    }

    private void doTestDifferential(final int fieldLength, final int decimalPlaces, final int order,
                                    final long[] compressed, final String[] uncompressed) {
        try {
            Class<?> differentialClass = null;
            for (final Class<?> c : HatanakaCompressFilter.class.getDeclaredClasses()) {
                if (c.getName().endsWith("NumericDifferential")) {
                    differentialClass = c;
                }
            }
            final Constructor<?> cstr = differentialClass.getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
            cstr.setAccessible(true);
            final Object differential = cstr.newInstance(fieldLength, decimalPlaces, order);
            final Method acceptMethod = differentialClass.getDeclaredMethod("accept", CharSequence.class);
            final Method getUncompressedMethod = differentialClass.getDeclaredMethod("getUncompressed");

            for (int i = 0; i < compressed.length; ++i) {
                acceptMethod.invoke(differential, Long.toString(compressed[i]));
                Assertions.assertEquals(uncompressed[i], getUncompressedMethod.invoke(differential).toString());
            }

        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testTextDates() {
        doTestText(35,
                   new String[] {
                       "------@> 2015 07 08 00 00 00.0000000  0 34@----------",
                       "------@                1@",
                       "------@                2                 2@----------",
                       "------@                3                 1@----------",
                       "------@                4                 0@----------",
                       "------@                5                27@----------"
                   }, new String[] {
                       "> 2015 07 08 00 00 00.0000000  0 34",
                       "> 2015 07 08 00 10 00.0000000  0 34",
                       "> 2015 07 08 00 20 00.0000000  0 32",
                       "> 2015 07 08 00 30 00.0000000  0 31",
                       "> 2015 07 08 00 40 00.0000000  0 30",
                       "> 2015 07 08 00 50 00.0000000  0 27"
                   });
    }

    @Test
    public void testTextSats() {
        doTestText(108,
                   new String[] {
                       "> 2015 07 08 00 00 00.0000000  0 34@      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G17G23G26G31G32R03R04R05R12R13R14R15R19R20R21S20S26@",
                       "                1@@",
                       "                2                 2@                                                          23  6 31  2R03  4  5 13  4  5  9 20 21S  S 6&&&&&&@",
                       "                3                 1@                  E 1  2  9 20G01  2  3  6  7  9 10  6 23  6 31  2R03  4  5 13  4  5  9 20  1S 0  6&&&@",
                       "                4                 0@                                2  3  6  7  9 10  6 23  6 31  2R03  4  5 13  4  5  9 20  1S 0  6&&&@",
                       "                5                27@                                                         R03R04  5 13 14  5 20 21S20S 6&&&&&&&&&@"
                   }, new String[] {
                       "      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G17G23G26G31G32R03R04R05R12R13R14R15R19R20R21S20S26",
                       "      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G17G23G26G31G32R03R04R05R12R13R14R15R19R20R21S20S26",
                       "      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G23G26G31G32R03R04R05R13R14R15R19R20R21S20S26      ",
                       "      C02C05C07C10E11E12E19E20G01G02G03G06G07G09G10G16G23G26G31G32R03R04R05R13R14R15R19R20R21S20S26         ",
                       "      C02C05C07C10E11E12E19E20G02G03G06G07G09G10G16G23G26G31G32R03R04R05R13R14R15R19R20R21S20S26            ",
                       "      C02C05C07C10E11E12E19E20G02G03G06G07G09G10G16G23G26R03R04R05R13R14R15R20R21S20S26                     "
                   });
    }

    private void doTestText(final int fieldLength, final String[] compressed, final String[] uncompressed) {
        try {
            Class<?> textClass = null;
            for (final Class<?> c : HatanakaCompressFilter.class.getDeclaredClasses()) {
                if (c.getName().endsWith("TextDifferential")) {
                    textClass = c;
                }
            }
            final Constructor<?> cstr = textClass.getDeclaredConstructor(Integer.TYPE);
            cstr.setAccessible(true);
            final Object differentialClass = cstr.newInstance(fieldLength);
            final Method acceptMethod = textClass.getDeclaredMethod("accept", CharSequence.class);
            final Method getUncompressedMethod = textClass.getDeclaredMethod("getUncompressed");

            for (int i = 0; i < compressed.length; ++i) {
                acceptMethod.invoke(differentialClass,
                                    compressed[i].subSequence(compressed[i].indexOf('@') + 1,
                                                              compressed[i].lastIndexOf('@')));
                Assertions.assertEquals(uncompressed[i], getUncompressedMethod.invoke(differentialClass).toString());
            }

        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testManyObservations() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/THTG00PYF_R_20160440000_60S_30S_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                              () -> Utils.class.getClassLoader().getResourceAsStream(name));
          Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                           "f07c83dcd4dfa02e517ebb8bed6ac7caa0a8ba6f5809cb9d367d7e757741afab");
          RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2016, 2, 13, 0, 0, 0.0, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(87, ods.size());

        Assertions.assertEquals("THTG",                 rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.BEIDOU, ods.get(24).getSatellite().getSystem());
        Assertions.assertEquals(12,                     ods.get(24).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                    ods.get(24).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.L1I,    ods.get(24).getObservationData().get(1).getObservationType());
        Assertions.assertEquals(129123198.213,          ods.get(24).getObservationData().get(1).getValue(), 1.0e-15);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testSeptentrioMissingType() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/TLSG00FRA_R_20160440000_30S_30S_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                              () -> Utils.class.getClassLoader().getResourceAsStream(name));
          Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                           "79b524e36869055b41238ee5919b13f0ca2923b95f8516fe0e09a7ac968a62d6");
          RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2016, 2, 13, 0, 0, 0.0, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(69, ods.size());

        Assertions.assertEquals("TLSG",                 rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.SBAS,   ods.get(37).getSatellite().getSystem());
        Assertions.assertEquals(123,                    ods.get(37).getSatellite().getPRN());
        Assertions.assertEquals(30.0,                   ods.get(37).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.D1C,    ods.get(37).getObservationData().get(2).getObservationType());
        Assertions.assertEquals(2.648,                  ods.get(37).getObservationData().get(2).getValue(), 1.0e-15);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testSeptentrioPhaseShiftWithS() throws IOException, NoSuchAlgorithmException {

        final String name = "rinex/VILL00ESP_R_20160440000_01D_30S_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                              () -> Utils.class.getClassLoader().getResourceAsStream(name));
          Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                           "a7f7136b71923d1fbd44638843300298872bb35a732325782ebe13cc299c0d46");
          RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2016, 2, 13, 0, 0, 0.0, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(56, ods.size());

        Assertions.assertEquals("VILL",                  rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.GLONASS, ods.get(35).getSatellite().getSystem());
        Assertions.assertEquals(17,                      ods.get(35).getSatellite().getPRN());
        Assertions.assertEquals(30.0,                    ods.get(35).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(ObservationType.L1C,     ods.get(35).getObservationData().get(1).getObservationType());
        Assertions.assertEquals(111836179.674,           ods.get(35).getObservationData().get(1).getValue(), 1.0e-15);

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testIssue892() throws IOException, NoSuchAlgorithmException {

        //Tests Rinex 3 with Hatanaka compression
        final String name = "rinex/DJIG00DJI_R_20191820000_01D_30S_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                         "3813aa1ac94733dd56c289998a34e7ec0cac8d5d03692038a966124994b52343");
        RinexObservationParser parser = new RinexObservationParser();

        AbsoluteDate t0 = new AbsoluteDate(2019, 7, 1, TimeScalesFactory.getGPS());
        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(133040, ods.size());

        Assertions.assertEquals("DJIG",                  rinexObservation.getHeader().getMarkerName());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,  ods.get(0).getSatellite().getSystem());
        Assertions.assertEquals(2,                       ods.get(0).getSatellite().getPRN());
        Assertions.assertEquals(0.0,                     ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(12,                      ods.get(0).getObservationData().size());
        Assertions.assertEquals(37732983.563,            ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(37732971.582,            ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(37732976.137,            ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(-2.043,                  ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);

        Assertions.assertEquals(SatelliteSystem.BEIDOU,  ods.get(96).getSatellite().getSystem());
        Assertions.assertEquals(12,                      ods.get(96).getSatellite().getPRN());
        Assertions.assertEquals(60.0,                    ods.get(96).getDate().durationFrom(t0), 1.0e-15);
        Assertions.assertEquals(12,                      ods.get(96).getObservationData().size());
        Assertions.assertEquals(25161908.281,            ods.get(96).getObservationData().get(0).getValue(), 1.0e-3);
        Assertions.assertEquals(25161895.332,            ods.get(96).getObservationData().get(1).getValue(), 1.0e-3);
        Assertions.assertEquals(25161903.730,            ods.get(96).getObservationData().get(2).getValue(), 1.0e-3);
        Assertions.assertEquals(1488.887,                ods.get(96).getObservationData().get(3).getValue(), 1.0e-3);
        Assertions.assertEquals(1207.371,                ods.get(96).getObservationData().get(4).getValue(), 1.0e-3);

        final SatInSystem c07 = new SatInSystem(SatelliteSystem.BEIDOU, 7);
        final Map<ObservationType, Integer> map = rinexObservation.getHeader().getNbObsPerSat().get(c07);
        Assertions.assertEquals(9, map.size());
        Assertions.assertEquals(1395, map.get(ObservationType.C2I));
        Assertions.assertEquals(1395, map.get(ObservationType.C6I));
        Assertions.assertEquals(1395, map.get(ObservationType.C7I));
        Assertions.assertEquals(1395, map.get(ObservationType.D2I));
        Assertions.assertEquals(1388, map.get(ObservationType.D6I));
        Assertions.assertEquals(1387, map.get(ObservationType.D7I));
        Assertions.assertEquals(1384, map.get(ObservationType.L2I));
        Assertions.assertEquals(1382, map.get(ObservationType.L6I));
        Assertions.assertEquals(1382, map.get(ObservationType.L7I));

        // the reference digest was computed externally using CRX2RNX and sha256sum on a Linux computer
        digester.checkDigest();

    }

    @Test
    public void testPrnNbObs() throws IOException, NoSuchAlgorithmException {

        //Tests Rinex 3 with Hatanaka compression
        final String name = "rinex/YEBE00ESP_R_20230891800_01H_30S_MO.crx.gz";
        final DataSource raw = new DataSource(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        Digester digester = new Digester(new HatanakaCompressFilter().filter(new GzipFilter().filter(raw)),
                                         "bef4d59c47cb908a41e4efae0d7add7ce2bbcbb57ad4fa51f63f315cdfe1cfeb");
        RinexObservationParser parser = new RinexObservationParser();

        final RinexObservation rinexObservation = parser.parse(digester.getDigestedSource());
        List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        Assertions.assertEquals(4767, ods.size());

        final SatInSystem g08 = new SatInSystem(SatelliteSystem.GPS, 8);
        final Map<ObservationType, Integer> mapG08 = rinexObservation.getHeader().getNbObsPerSat().get(g08);
        Assertions.assertEquals(12, mapG08.size());
        Assertions.assertEquals(120, mapG08.get(ObservationType.C1C));
        Assertions.assertNull(mapG08.get(ObservationType.C1L));
        Assertions.assertEquals(120, mapG08.get(ObservationType.C2S));
        Assertions.assertEquals(120, mapG08.get(ObservationType.C2W));
        Assertions.assertEquals(120, mapG08.get(ObservationType.C5Q));
        Assertions.assertEquals(120, mapG08.get(ObservationType.L1C));
        Assertions.assertNull(mapG08.get(ObservationType.L1L));
        Assertions.assertEquals(120, mapG08.get(ObservationType.L2S));
        Assertions.assertEquals(120, mapG08.get(ObservationType.L2W));
        Assertions.assertEquals(120, mapG08.get(ObservationType.L5Q));
        Assertions.assertEquals(120, mapG08.get(ObservationType.S1C));
        Assertions.assertNull(mapG08.get(ObservationType.S1L));
        Assertions.assertEquals(120, mapG08.get(ObservationType.S2S));
        Assertions.assertEquals(120, mapG08.get(ObservationType.S2W));
        Assertions.assertEquals(120, mapG08.get(ObservationType.S5Q));

        final SatInSystem e11 = new SatInSystem(SatelliteSystem.GALILEO, 11);
        final Map<ObservationType, Integer> mapE11 = rinexObservation.getHeader().getNbObsPerSat().get(e11);
        Assertions.assertEquals(15, mapE11.size());
        Assertions.assertEquals(37, mapE11.get(ObservationType.C1C));
        Assertions.assertEquals(83, mapE11.get(ObservationType.C5Q));
        Assertions.assertEquals(74, mapE11.get(ObservationType.C6C));
        Assertions.assertEquals(84, mapE11.get(ObservationType.C7Q));
        Assertions.assertEquals(89, mapE11.get(ObservationType.C8Q));
        Assertions.assertEquals(37, mapE11.get(ObservationType.L1C));
        Assertions.assertEquals(83, mapE11.get(ObservationType.L5Q));
        Assertions.assertEquals(74, mapE11.get(ObservationType.L6C));
        Assertions.assertEquals(84, mapE11.get(ObservationType.L7Q));
        Assertions.assertEquals(89, mapE11.get(ObservationType.L8Q));
        Assertions.assertEquals(37, mapE11.get(ObservationType.S1C));
        Assertions.assertEquals(83, mapE11.get(ObservationType.S5Q));
        Assertions.assertEquals(74, mapE11.get(ObservationType.S6C));
        Assertions.assertEquals(84, mapE11.get(ObservationType.S7Q));
        Assertions.assertEquals(89, mapE11.get(ObservationType.S8Q));

        digester.checkDigest();

    }

    private static class Digester {

        final DataSource    source;
        final MessageDigest md;
        final String        expected256;

        Digester(final DataSource source, final String expected256) throws NoSuchAlgorithmException {
            this.source      = source;
            this.md          = MessageDigest.getInstance("SHA-256");
            this.expected256 = expected256;
        }

        DataSource getDigestedSource() {
            return new DataSource(source.getName(),
                                  () -> new DigestInputStream(source.getOpener().openStreamOnce(), md));
        }

        void checkDigest() {
            StringBuilder builder = new StringBuilder();
            for (final byte b : md.digest()) {
                final int ib = Byte.toUnsignedInt(b);
                if (ib < 0x10) {
                    builder.append('0');
                }
                builder.append(Integer.toHexString(ib));
            }
            Assertions.assertEquals(expected256, builder.toString());
        }
    }

}
