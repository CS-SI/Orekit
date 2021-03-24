/* Copyright 2002-2021 CS GROUP
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
import org.junit.Assert;
import org.junit.Test;
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
        Assert.assertEquals(RecordType.STR,                        str.getRecordType());
        Assert.assertEquals("SSRA01BKG1",                          str.getMountPoint());
        Assert.assertEquals("IGS-SSR APC",                         str.getSourceIdentifier());
        Assert.assertEquals(DataFormat.RTCM_3,                     str.getFormat());
        Assert.assertEquals(5,                                     str.getFormatDetails().size());
        Assert.assertEquals("4076_021",                            str.getFormatDetails().get(0).getId());
        Assert.assertEquals(60,                                    str.getFormatDetails().get(0).getRate());
        Assert.assertEquals("4076_022",                            str.getFormatDetails().get(1).getId());
        Assert.assertEquals(5,                                     str.getFormatDetails().get(1).getRate());
        Assert.assertEquals("4076_025",                            str.getFormatDetails().get(2).getId());
        Assert.assertEquals(60,                                    str.getFormatDetails().get(2).getRate());
        Assert.assertEquals("4076_041",                            str.getFormatDetails().get(3).getId());
        Assert.assertEquals(60,                                    str.getFormatDetails().get(3).getRate());
        Assert.assertEquals("4076_042",                            str.getFormatDetails().get(4).getId());
        Assert.assertEquals(5,                                     str.getFormatDetails().get(4).getRate());
        Assert.assertEquals(CarrierPhase.NO,                       str.getCarrierPhase());
        Assert.assertEquals(2,                                     str.getNavigationSystems().size());
        Assert.assertEquals(NavigationSystem.GPS,                  str.getNavigationSystems().get(0));
        Assert.assertEquals("GPS",                                 str.getNavigationSystems().get(0).toString());
        Assert.assertEquals(NavigationSystem.GLO,                  str.getNavigationSystems().get(1));
        Assert.assertEquals("Glonass",                             str.getNavigationSystems().get(1).toString());
        Assert.assertEquals("MISC",                                str.getNetwork());
        Assert.assertEquals("DEU",                                 str.getCountry());
        Assert.assertEquals(50.09,                                 FastMath.toDegrees(str.getLatitude()),  1.0e-10);
        Assert.assertEquals(8.66,                                  FastMath.toDegrees(str.getLongitude()), 1.0e-10);
        Assert.assertEquals(false,                                 str.isNMEARequired());
        Assert.assertEquals(true,                                  str.isNetworked());
        Assert.assertEquals("RTNet",                               str.getGenerator());
        Assert.assertEquals("none",                                str.getCompressionEncryption());
        Assert.assertEquals(Authentication.BASIC,                  str.getAuthentication());
        Assert.assertEquals(false,                                 str.areFeesRequired());
        Assert.assertEquals(1000,                                  str.getBitRate());
        Assert.assertEquals("BKG IGS-SSR",                         str.getMisc());
    }

    @Test
    public void testCLK24() {
        final DataStreamRecord str = new DataStreamRecord(CLK24);
        Assert.assertEquals(RecordType.STR,       str.getRecordType());
        Assert.assertEquals("CLK24",              str.getMountPoint());
        Assert.assertEquals("BRDC_CoM_ITRF",      str.getSourceIdentifier());
        Assert.assertEquals(DataFormat.RTCM_3,    str.getFormat());
        Assert.assertEquals(1,                    str.getFormatDetails().size());
        Assert.assertEquals("1060",               str.getFormatDetails().get(0).getId());
        Assert.assertEquals(5,                    str.getFormatDetails().get(0).getRate());
        Assert.assertEquals(CarrierPhase.NO,      str.getCarrierPhase());
        Assert.assertEquals(1,                    str.getNavigationSystems().size());
        Assert.assertEquals(NavigationSystem.GPS, str.getNavigationSystems().get(0));
        Assert.assertEquals("MISC",               str.getNetwork());
        Assert.assertEquals("DEU",                str.getCountry());
        Assert.assertEquals(49.87,                Math.toDegrees(str.getLatitude()),  1.0e-15);
        Assert.assertEquals( 8.62,                Math.toDegrees(str.getLongitude()), 1.0e-15);
        Assert.assertEquals(false,                str.isNMEARequired());
        Assert.assertEquals(true,                 str.isNetworked());
        Assert.assertEquals("RETINA",             str.getGenerator());
        Assert.assertEquals("none",               str.getCompressionEncryption());
        Assert.assertEquals(Authentication.BASIC, str.getAuthentication());
        Assert.assertEquals(false,                str.areFeesRequired());
        Assert.assertEquals(1400,                 str.getBitRate());
        Assert.assertEquals("IGS Combination",    str.getMisc());
    }

    @Test
    public void testRTCM3EPH01() {
        final DataStreamRecord str = new DataStreamRecord(RTCM3EPH01);
        Assert.assertEquals(RecordType.STR,                        str.getRecordType());
        Assert.assertEquals("RTCM3EPH01",                          str.getMountPoint());
        Assert.assertEquals("Assisted-GNSS",                       str.getSourceIdentifier());
        Assert.assertEquals(DataFormat.RTCM_3,                     str.getFormat());
        Assert.assertEquals(7,                                     str.getFormatDetails().size());
        Assert.assertEquals("1019",                                str.getFormatDetails().get(0).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(0).getRate());
        Assert.assertEquals("1020",                                str.getFormatDetails().get(1).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(1).getRate());
        Assert.assertEquals("1042",                                str.getFormatDetails().get(2).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(2).getRate());
        Assert.assertEquals("1043",                                str.getFormatDetails().get(3).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(3).getRate());
        Assert.assertEquals("1044",                                str.getFormatDetails().get(4).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(4).getRate());
        Assert.assertEquals("1045",                                str.getFormatDetails().get(5).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(5).getRate());
        Assert.assertEquals("1046",                                str.getFormatDetails().get(6).getId());
        Assert.assertEquals(-1,                                    str.getFormatDetails().get(6).getRate());
        Assert.assertEquals(CarrierPhase.NO,                       str.getCarrierPhase());
        Assert.assertEquals(6,                                     str.getNavigationSystems().size());
        Assert.assertEquals(NavigationSystem.GPS,                  str.getNavigationSystems().get(0));
        Assert.assertEquals("GPS",                                 str.getNavigationSystems().get(0).toString());
        Assert.assertEquals(NavigationSystem.GLO,                  str.getNavigationSystems().get(1));
        Assert.assertEquals("Glonass",                             str.getNavigationSystems().get(1).toString());
        Assert.assertEquals(NavigationSystem.GAL,                  str.getNavigationSystems().get(2));
        Assert.assertEquals("Galileo",                             str.getNavigationSystems().get(2).toString());
        Assert.assertEquals(NavigationSystem.BDS,                  str.getNavigationSystems().get(3));
        Assert.assertEquals("Beidou",                              str.getNavigationSystems().get(3).toString());
        Assert.assertEquals(NavigationSystem.QZS,                  str.getNavigationSystems().get(4));
        Assert.assertEquals("QZNSS",                               str.getNavigationSystems().get(4).toString());
        Assert.assertEquals(NavigationSystem.SBAS,                 str.getNavigationSystems().get(5));
        Assert.assertEquals("SBAS",                                str.getNavigationSystems().get(5).toString());
        Assert.assertEquals("MISC",                                str.getNetwork());
        Assert.assertEquals("DEU",                                 str.getCountry());
        Assert.assertEquals(48.09,                                 Math.toDegrees(str.getLatitude()),  1.0e-15);
        Assert.assertEquals(11.28,                                 Math.toDegrees(str.getLongitude()), 1.0e-15);
        Assert.assertEquals(false,                                 str.isNMEARequired());
        Assert.assertEquals(true,                                  str.isNetworked());
        Assert.assertEquals("RETICLE",                             str.getGenerator());
        Assert.assertEquals("none",                                str.getCompressionEncryption());
        Assert.assertEquals(Authentication.NONE,                   str.getAuthentication());
        Assert.assertEquals(false,                                 str.areFeesRequired());
        Assert.assertEquals(13600,                                 str.getBitRate());
        Assert.assertEquals("gnss.gsoc.dlr.de:2101/BCEP0_DEU1(1)", str.getMisc());
    }

    @Test
    public void testDigestAuthentication() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";B;", ";D;"));
        Assert.assertEquals(Authentication.DIGEST, str.getAuthentication());
    }

    @Test
    public void testSingleBase() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";0;1;RETINA;", ";0;0;RETINA;"));
        Assert.assertFalse(str.isNetworked());
    }

    @Test
    public void testRequiresNMEA() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";0;1;RETINA;", ";1;1;RETINA;"));
        Assert.assertTrue(str.isNMEARequired());
    }

    @Test
    public void testRequiresFees() {
        final DataStreamRecord str = new DataStreamRecord(CLK24.replace(";B;N;", ";B;Y;"));
        Assert.assertTrue(str.areFeesRequired());
    }

    @Test
    public void testUnknownDataFormat() {
        try {
            new DataStreamRecord(CLK24.replace(";RTCM 3.1;", ";" + BAD_FIELD + ";"));
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_DATA_FORMAT, me.getSpecifier());
            Assert.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownAuthenticationMethod() {
        try {
            new DataStreamRecord(CLK24.replace(";B;", ";" + BAD_FIELD + ";"));
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_AUTHENTICATION_METHOD, me.getSpecifier());
            Assert.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownCarrierPhaseNotParsable() {
        try {
            new DataStreamRecord(CLK24.replace(";0;GPS", ";" + BAD_FIELD + ";GPS"));
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, me.getSpecifier());
            Assert.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownCarrierPhaseWrongNumber() {
        try {
            new DataStreamRecord(CLK24.replace(";0;GPS", ";17;GPS"));
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, me.getSpecifier());
            Assert.assertEquals("17", (String) me.getParts()[0]);
        }
    }

    @Test
    public void testUnknownNavigationSystem() {
        try {
            new DataStreamRecord(CLK24.replace(";GPS;", ";GPS+" + BAD_FIELD + "+GLO;"));
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_NAVIGATION_SYSTEM, me.getSpecifier());
            Assert.assertEquals(BAD_FIELD, (String) me.getParts()[0]);
        }
    }

}
