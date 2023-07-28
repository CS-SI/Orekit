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
package org.orekit.gnss.metric.ntrip;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class DataStreamRecordTest {

    private static String BAD_FIELD  = "bad field";
    private static String CLK24      = "STR;CLK24;BRDC_CoM_ITRF;RTCM 3.1;1060(5);0;GPS;MISC;DEU;49.87;8.62;0;1;RETINA;none;B;N;1400;IGS Combination";
    private static String RTCM3EPH01 = "STR;RTCM3EPH01;Assisted-GNSS;RTCM 3.3;1019,1020,1042,1043,1044,1045,1046;0;GPS+GLO+GAL+BDS+QZS+SBAS;MISC;DEU;48.09;11.28;0;1;RETICLE;none;N;N;13600;gnss.gsoc.dlr.de:2101/BCEP0_DEU1(1)";
    private static String SSRA01BKG1 = "STR;SSRA01BKG1;IGS-SSR APC;RTCM 3.1;4076_021(60),4076_022(5),4076_025(60),4076_041(60),4076_042(5);0;GPS+GLO;MISC;DEU;50.09;8.66;0;1;RTNet;none;B;N;1000;BKG IGS-SSR";

    @Test
    public void testSSRA01BKG1() {
        final DataStreamRecord str = new DataStreamRecord(SSRA01BKG1);
        Assertions.assertEquals(RecordType.STR,                        str.getRecordType());
        Assertions.assertEquals("SSRA01BKG1",                          str.getMountPoint());
        Assertions.assertEquals("IGS-SSR APC",                         str.getSourceIdentifier());
        Assertions.assertEquals(DataFormat.RTCM_3,                     str.getFormat());
        Assertions.assertEquals(5,                                     str.getFormatDetails().size());
        Assertions.assertEquals("4076_021",                            str.getFormatDetails().get(0).getId());
        Assertions.assertEquals(60,                                    str.getFormatDetails().get(0).getRate());
        Assertions.assertEquals("4076_022",                            str.getFormatDetails().get(1).getId());
        Assertions.assertEquals(5,                                     str.getFormatDetails().get(1).getRate());
        Assertions.assertEquals("4076_025",                            str.getFormatDetails().get(2).getId());
        Assertions.assertEquals(60,                                    str.getFormatDetails().get(2).getRate());
        Assertions.assertEquals("4076_041",                            str.getFormatDetails().get(3).getId());
        Assertions.assertEquals(60,                                    str.getFormatDetails().get(3).getRate());
        Assertions.assertEquals("4076_042",                            str.getFormatDetails().get(4).getId());
        Assertions.assertEquals(5,                                     str.getFormatDetails().get(4).getRate());
        Assertions.assertEquals(CarrierPhase.NO,                       str.getCarrierPhase());
        Assertions.assertEquals(2,                                     str.getNavigationSystems().size());
        Assertions.assertEquals(NavigationSystem.GPS,                  str.getNavigationSystems().get(0));
        Assertions.assertEquals("GPS",                                 str.getNavigationSystems().get(0).toString());
        Assertions.assertEquals(NavigationSystem.GLO,                  str.getNavigationSystems().get(1));
        Assertions.assertEquals("Glonass",                             str.getNavigationSystems().get(1).toString());
        Assertions.assertEquals("MISC",                                str.getNetwork());
        Assertions.assertEquals("DEU",                                 str.getCountry());
        Assertions.assertEquals(50.09,                                 FastMath.toDegrees(str.getLatitude()),  1.0e-10);
        Assertions.assertEquals(8.66,                                  FastMath.toDegrees(str.getLongitude()), 1.0e-10);
        Assertions.assertEquals(false,                                 str.isNMEARequired());
        Assertions.assertEquals(true,                                  str.isNetworked());
        Assertions.assertEquals("RTNet",                               str.getGenerator());
        Assertions.assertEquals("none",                                str.getCompressionEncryption());
        Assertions.assertEquals(Authentication.BASIC,                  str.getAuthentication());
        Assertions.assertEquals(false,                                 str.areFeesRequired());
        Assertions.assertEquals(1000,                                  str.getBitRate());
        Assertions.assertEquals("BKG IGS-SSR",                         str.getMisc());
    }

    @Test
    public void testCLK24() {
        final DataStreamRecord str = new DataStreamRecord(CLK24);
        Assertions.assertEquals(RecordType.STR,       str.getRecordType());
        Assertions.assertEquals("CLK24",              str.getMountPoint());
        Assertions.assertEquals("BRDC_CoM_ITRF",      str.getSourceIdentifier());
        Assertions.assertEquals(DataFormat.RTCM_3,    str.getFormat());
        Assertions.assertEquals(1,                    str.getFormatDetails().size());
        Assertions.assertEquals("1060",               str.getFormatDetails().get(0).getId());
        Assertions.assertEquals(5,                    str.getFormatDetails().get(0).getRate());
        Assertions.assertEquals(CarrierPhase.NO,      str.getCarrierPhase());
        Assertions.assertEquals(1,                    str.getNavigationSystems().size());
        Assertions.assertEquals(NavigationSystem.GPS, str.getNavigationSystems().get(0));
        Assertions.assertEquals("MISC",               str.getNetwork());
        Assertions.assertEquals("DEU",                str.getCountry());
        Assertions.assertEquals(49.87,                FastMath.toDegrees(str.getLatitude()),  1.0e-15);
        Assertions.assertEquals( 8.62,                FastMath.toDegrees(str.getLongitude()), 1.0e-15);
        Assertions.assertEquals(false,                str.isNMEARequired());
        Assertions.assertEquals(true,                 str.isNetworked());
        Assertions.assertEquals("RETINA",             str.getGenerator());
        Assertions.assertEquals("none",               str.getCompressionEncryption());
        Assertions.assertEquals(Authentication.BASIC, str.getAuthentication());
        Assertions.assertEquals(false,                str.areFeesRequired());
        Assertions.assertEquals(1400,                 str.getBitRate());
        Assertions.assertEquals("IGS Combination",    str.getMisc());
    }

    @Test
    public void testRTCM3EPH01() {
        final DataStreamRecord str = new DataStreamRecord(RTCM3EPH01);
        Assertions.assertEquals(RecordType.STR,                        str.getRecordType());
        Assertions.assertEquals("RTCM3EPH01",                          str.getMountPoint());
        Assertions.assertEquals("Assisted-GNSS",                       str.getSourceIdentifier());
        Assertions.assertEquals(DataFormat.RTCM_3,                     str.getFormat());
        Assertions.assertEquals(7,                                     str.getFormatDetails().size());
        Assertions.assertEquals("1019",                                str.getFormatDetails().get(0).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(0).getRate());
        Assertions.assertEquals("1020",                                str.getFormatDetails().get(1).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(1).getRate());
        Assertions.assertEquals("1042",                                str.getFormatDetails().get(2).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(2).getRate());
        Assertions.assertEquals("1043",                                str.getFormatDetails().get(3).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(3).getRate());
        Assertions.assertEquals("1044",                                str.getFormatDetails().get(4).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(4).getRate());
        Assertions.assertEquals("1045",                                str.getFormatDetails().get(5).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(5).getRate());
        Assertions.assertEquals("1046",                                str.getFormatDetails().get(6).getId());
        Assertions.assertEquals(-1,                                    str.getFormatDetails().get(6).getRate());
        Assertions.assertEquals(CarrierPhase.NO,                       str.getCarrierPhase());
        Assertions.assertEquals(6,                                     str.getNavigationSystems().size());
        Assertions.assertEquals(NavigationSystem.GPS,                  str.getNavigationSystems().get(0));
        Assertions.assertEquals("GPS",                                 str.getNavigationSystems().get(0).toString());
        Assertions.assertEquals(NavigationSystem.GLO,                  str.getNavigationSystems().get(1));
        Assertions.assertEquals("Glonass",                             str.getNavigationSystems().get(1).toString());
        Assertions.assertEquals(NavigationSystem.GAL,                  str.getNavigationSystems().get(2));
        Assertions.assertEquals("Galileo",                             str.getNavigationSystems().get(2).toString());
        Assertions.assertEquals(NavigationSystem.BDS,                  str.getNavigationSystems().get(3));
        Assertions.assertEquals("Beidou",                              str.getNavigationSystems().get(3).toString());
        Assertions.assertEquals(NavigationSystem.QZS,                  str.getNavigationSystems().get(4));
        Assertions.assertEquals("QZNSS",                               str.getNavigationSystems().get(4).toString());
        Assertions.assertEquals(NavigationSystem.SBAS,                 str.getNavigationSystems().get(5));
        Assertions.assertEquals("SBAS",                                str.getNavigationSystems().get(5).toString());
        Assertions.assertEquals("MISC",                                str.getNetwork());
        Assertions.assertEquals("DEU",                                 str.getCountry());
        Assertions.assertEquals(48.09,                                 FastMath.toDegrees(str.getLatitude()),  1.0e-15);
        Assertions.assertEquals(11.28,                                 FastMath.toDegrees(str.getLongitude()), 1.0e-15);
        Assertions.assertEquals(false,                                 str.isNMEARequired());
        Assertions.assertEquals(true,                                  str.isNetworked());
        Assertions.assertEquals("RETICLE",                             str.getGenerator());
        Assertions.assertEquals("none",                                str.getCompressionEncryption());
        Assertions.assertEquals(Authentication.NONE,                   str.getAuthentication());
        Assertions.assertEquals(false,                                 str.areFeesRequired());
        Assertions.assertEquals(13600,                                 str.getBitRate());
        Assertions.assertEquals("gnss.gsoc.dlr.de:2101/BCEP0_DEU1(1)", str.getMisc());
    }

    @Test
    public void testDigestAuthentication() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";B;", ";D;"));
        Assertions.assertEquals(Authentication.DIGEST, str.getAuthentication());
    }

    @Test
    public void testSingleBase() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";0;1;RETINA;", ";0;0;RETINA;"));
        Assertions.assertFalse(str.isNetworked());
    }

    @Test
    public void testRequiresNMEA() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";0;1;RETINA;", ";1;1;RETINA;"));
        Assertions.assertTrue(str.isNMEARequired());
    }

    @Test
    public void testRequiresFees() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";B;N;", ";B;Y;"));
        Assertions.assertTrue(str.areFeesRequired());
    }

    @Test
    public void testUnknownDataFormat() {
        try {
            new DataStreamRecord(CLK24.replace(";RTCM 3.1;", ";" + BAD_FIELD + ";"));
            Assertions.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_DATA_FORMAT, me.getSpecifier());
            Assertions.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownAuthenticationMethod() {
        try {
            new DataStreamRecord(CLK24.replace(";B;", ";" + BAD_FIELD + ";"));
            Assertions.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_AUTHENTICATION_METHOD, me.getSpecifier());
            Assertions.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownCarrierPhaseNotParsable() {
        try {
            new DataStreamRecord(CLK24.replace(";0;GPS", ";" + BAD_FIELD + ";GPS"));
            Assertions.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, me.getSpecifier());
            Assertions.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownCarrierPhaseWrongNumber() {
        try {
            new DataStreamRecord(CLK24.replace(";0;GPS", ";17;GPS"));
            Assertions.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, me.getSpecifier());
            Assertions.assertEquals("17", (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownNavigationSystem() {
        try {
            new DataStreamRecord(CLK24.replace(";GPS;", ";GPS+" + BAD_FIELD + "+GLO;"));
            Assertions.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_NAVIGATION_SYSTEM, me.getSpecifier());
            Assertions.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

}
