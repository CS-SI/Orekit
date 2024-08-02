/* Copyright 2002-2024 CS GROUP
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
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DataStreamRecordTest {

    private static String BAD_FIELD  = "bad field";
    private static String CLK24      = "STR;CLK24;BRDC_CoM_ITRF;RTCM 3.1;1060(5);0;GPS;MISC;DEU;49.87;8.62;0;1;RETINA;none;B;N;1400;IGS Combination";
    private static String RTCM3EPH01 = "STR;RTCM3EPH01;Assisted-GNSS;RTCM 3.3;1019,1020,1042,1043,1044,1045,1046;0;GPS+GLO+GAL+BDS+QZS+SBAS;MISC;DEU;48.09;11.28;0;1;RETICLE;none;N;N;13600;gnss.gsoc.dlr.de:2101/BCEP0_DEU1(1)";
    private static String SSRA01BKG1 = "STR;SSRA01BKG1;IGS-SSR APC;RTCM 3.1;4076_021(60),4076_022(5),4076_025(60),4076_041(60),4076_042(5);0;GPS+GLO;MISC;DEU;50.09;8.66;0;1;RTNet;none;B;N;1000;BKG IGS-SSR";

    @Test
    void testSSRA01BKG1() {
        final DataStreamRecord str = new DataStreamRecord(SSRA01BKG1);
        assertEquals(RecordType.STR,                        str.getRecordType());
        assertEquals("SSRA01BKG1",                          str.getMountPoint());
        assertEquals("IGS-SSR APC",                         str.getSourceIdentifier());
        assertEquals(DataFormat.RTCM_3,                     str.getFormat());
        assertEquals(5,                                     str.getFormatDetails().size());
        assertEquals("4076_021",                            str.getFormatDetails().get(0).getId());
        assertEquals(60,                                    str.getFormatDetails().get(0).getRate());
        assertEquals("4076_022",                            str.getFormatDetails().get(1).getId());
        assertEquals(5,                                     str.getFormatDetails().get(1).getRate());
        assertEquals("4076_025",                            str.getFormatDetails().get(2).getId());
        assertEquals(60,                                    str.getFormatDetails().get(2).getRate());
        assertEquals("4076_041",                            str.getFormatDetails().get(3).getId());
        assertEquals(60,                                    str.getFormatDetails().get(3).getRate());
        assertEquals("4076_042",                            str.getFormatDetails().get(4).getId());
        assertEquals(5,                                     str.getFormatDetails().get(4).getRate());
        assertEquals(CarrierPhase.NO,                       str.getCarrierPhase());
        assertEquals(2,                                     str.getNavigationSystems().size());
        assertEquals(NavigationSystem.GPS,                  str.getNavigationSystems().get(0));
        assertEquals("GPS",                                 str.getNavigationSystems().get(0).toString());
        assertEquals(NavigationSystem.GLO,                  str.getNavigationSystems().get(1));
        assertEquals("Glonass",                             str.getNavigationSystems().get(1).toString());
        assertEquals("MISC",                                str.getNetwork());
        assertEquals("DEU",                                 str.getCountry());
        assertEquals(50.09,                                 FastMath.toDegrees(str.getLatitude()),  1.0e-10);
        assertEquals(8.66,                                  FastMath.toDegrees(str.getLongitude()), 1.0e-10);
        assertFalse(str.isNMEARequired());
        assertTrue(str.isNetworked());
        assertEquals("RTNet",                               str.getGenerator());
        assertEquals("none",                                str.getCompressionEncryption());
        assertEquals(Authentication.BASIC,                  str.getAuthentication());
        assertFalse(str.areFeesRequired());
        assertEquals(1000,                                  str.getBitRate());
        assertEquals("BKG IGS-SSR",                         str.getMisc());
    }

    @Test
    void testCLK24() {
        final DataStreamRecord str = new DataStreamRecord(CLK24);
        assertEquals(RecordType.STR,       str.getRecordType());
        assertEquals("CLK24",              str.getMountPoint());
        assertEquals("BRDC_CoM_ITRF",      str.getSourceIdentifier());
        assertEquals(DataFormat.RTCM_3,    str.getFormat());
        assertEquals(1,                    str.getFormatDetails().size());
        assertEquals("1060",               str.getFormatDetails().get(0).getId());
        assertEquals(5,                    str.getFormatDetails().get(0).getRate());
        assertEquals(CarrierPhase.NO,      str.getCarrierPhase());
        assertEquals(1,                    str.getNavigationSystems().size());
        assertEquals(NavigationSystem.GPS, str.getNavigationSystems().get(0));
        assertEquals("MISC",               str.getNetwork());
        assertEquals("DEU",                str.getCountry());
        assertEquals(49.87,                FastMath.toDegrees(str.getLatitude()),  1.0e-15);
        assertEquals( 8.62,                FastMath.toDegrees(str.getLongitude()), 1.0e-15);
        assertFalse(str.isNMEARequired());
        assertTrue(str.isNetworked());
        assertEquals("RETINA",             str.getGenerator());
        assertEquals("none",               str.getCompressionEncryption());
        assertEquals(Authentication.BASIC, str.getAuthentication());
        assertFalse(str.areFeesRequired());
        assertEquals(1400,                 str.getBitRate());
        assertEquals("IGS Combination",    str.getMisc());
    }

    @Test
    void testRTCM3EPH01() {
        final DataStreamRecord str = new DataStreamRecord(RTCM3EPH01);
        assertEquals(RecordType.STR,                        str.getRecordType());
        assertEquals("RTCM3EPH01",                          str.getMountPoint());
        assertEquals("Assisted-GNSS",                       str.getSourceIdentifier());
        assertEquals(DataFormat.RTCM_3,                     str.getFormat());
        assertEquals(7,                                     str.getFormatDetails().size());
        assertEquals("1019",                                str.getFormatDetails().get(0).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(0).getRate());
        assertEquals("1020",                                str.getFormatDetails().get(1).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(1).getRate());
        assertEquals("1042",                                str.getFormatDetails().get(2).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(2).getRate());
        assertEquals("1043",                                str.getFormatDetails().get(3).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(3).getRate());
        assertEquals("1044",                                str.getFormatDetails().get(4).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(4).getRate());
        assertEquals("1045",                                str.getFormatDetails().get(5).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(5).getRate());
        assertEquals("1046",                                str.getFormatDetails().get(6).getId());
        assertEquals(-1,                                    str.getFormatDetails().get(6).getRate());
        assertEquals(CarrierPhase.NO,                       str.getCarrierPhase());
        assertEquals(6,                                     str.getNavigationSystems().size());
        assertEquals(NavigationSystem.GPS,                  str.getNavigationSystems().get(0));
        assertEquals("GPS",                                 str.getNavigationSystems().get(0).toString());
        assertEquals(NavigationSystem.GLO,                  str.getNavigationSystems().get(1));
        assertEquals("Glonass",                             str.getNavigationSystems().get(1).toString());
        assertEquals(NavigationSystem.GAL,                  str.getNavigationSystems().get(2));
        assertEquals("Galileo",                             str.getNavigationSystems().get(2).toString());
        assertEquals(NavigationSystem.BDS,                  str.getNavigationSystems().get(3));
        assertEquals("Beidou",                              str.getNavigationSystems().get(3).toString());
        assertEquals(NavigationSystem.QZS,                  str.getNavigationSystems().get(4));
        assertEquals("QZNSS",                               str.getNavigationSystems().get(4).toString());
        assertEquals(NavigationSystem.SBAS,                 str.getNavigationSystems().get(5));
        assertEquals("SBAS",                                str.getNavigationSystems().get(5).toString());
        assertEquals("MISC",                                str.getNetwork());
        assertEquals("DEU",                                 str.getCountry());
        assertEquals(48.09,                                 FastMath.toDegrees(str.getLatitude()),  1.0e-15);
        assertEquals(11.28,                                 FastMath.toDegrees(str.getLongitude()), 1.0e-15);
        assertFalse(str.isNMEARequired());
        assertTrue(str.isNetworked());
        assertEquals("RETICLE",                             str.getGenerator());
        assertEquals("none",                                str.getCompressionEncryption());
        assertEquals(Authentication.NONE,                   str.getAuthentication());
        assertFalse(str.areFeesRequired());
        assertEquals(13600,                                 str.getBitRate());
        assertEquals("gnss.gsoc.dlr.de:2101/BCEP0_DEU1(1)", str.getMisc());
    }

    @Test
    void testDigestAuthentication() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";B;", ";D;"));
        assertEquals(Authentication.DIGEST, str.getAuthentication());
    }

    @Test
    void testSingleBase() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";0;1;RETINA;", ";0;0;RETINA;"));
        assertFalse(str.isNetworked());
    }

    @Test
    void testRequiresNMEA() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";0;1;RETINA;", ";1;1;RETINA;"));
        assertTrue(str.isNMEARequired());
    }

    @Test
    void testRequiresFees() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";B;N;", ";B;Y;"));
        assertTrue(str.areFeesRequired());
    }

    @Test
    void testUnknownDataFormat() {
        try {
            new DataStreamRecord(CLK24.replace(";RTCM 3.1;", ";" + BAD_FIELD + ";"));
            fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNKNOWN_DATA_FORMAT, me.getSpecifier());
            assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    void testUnknownAuthenticationMethod() {
        try {
            new DataStreamRecord(CLK24.replace(";B;", ";" + BAD_FIELD + ";"));
            fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNKNOWN_AUTHENTICATION_METHOD, me.getSpecifier());
            assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    void testUnknownCarrierPhaseNotParsable() {
        try {
            new DataStreamRecord(CLK24.replace(";0;GPS", ";" + BAD_FIELD + ";GPS"));
            fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, me.getSpecifier());
            assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    void testUnknownCarrierPhaseWrongNumber() {
        try {
            new DataStreamRecord(CLK24.replace(";0;GPS", ";17;GPS"));
            fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, me.getSpecifier());
            assertEquals("17", (String) me.getParts()[0]);
        }
    }

    @Test
    void testUnknownNavigationSystem() {
        try {
            new DataStreamRecord(CLK24.replace(";GPS;", ";GPS+" + BAD_FIELD + "+GLO;"));
            fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNKNOWN_NAVIGATION_SYSTEM, me.getSpecifier());
            assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

}
