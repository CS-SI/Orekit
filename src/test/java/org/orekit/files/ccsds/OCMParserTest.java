/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.files.ccsds;

import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class OCMParserTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/OCMExample1.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new OCMParser().parse(wrongName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingT0() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/OCM-missing-t0.txt").toURI().getPath();
        try {
            new OCMParser().parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(Keyword.DEF_EPOCH_TZERO, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingTimeSystem() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/OCM-missing-time-system.txt").toURI().getPath();
        try {
            new OCMParser().parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(Keyword.DEF_TIME_SYSTEM, oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongDate() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/OCM-wrong-date.txt").toURI().getPath();
        try {
            new OCMParser().parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(11, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("EARLIEST_TIME             = WRONG=123", oe.getParts()[2]);
        }
    }

    @Test
    public void testSpuriousMetaDataSection() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/OCM-spurious-metadata-section.txt").toURI().getPath();
        try {
            new OCMParser().parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(15, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("META_START", oe.getParts()[2]);
        }
    }

    @Test
    public void testParseOCM1() {
        final String   ex  = "/ccsds/OCMExample1.txt";
        final OCMFile file = new OCMParser().parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // check the default values that are not set in this simple file
        Assert.assertEquals("CSPOC",              file.getMetaData().getCatalogName());
        Assert.assertEquals(1.0,                  file.getMetaData().getSecClockPerSISecond(), 1.0e-15);
        Assert.assertEquals(Constants.JULIAN_DAY, file.getMetaData().getSecPerDay(),           1.0e-15);
        Assert.assertEquals("LINEAR",             file.getMetaData().getInterpMethodEOP());

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        // OCM is the only message for which ORIGINATOR, OBJECT_NAME, OBJECT_ID,
        // CENTER_NAME and REF_FRAME are not mandatory, they are not present in this minimal file
        Assert.assertNull(file.getOriginator());
        Assert.assertNull(file.getMetaData().getObjectName());
        Assert.assertNull(file.getMetaData().getObjectID());
        Assert.assertNull(file.getMetaData().getCenterName());
        Assert.assertNull(file.getMetaData().getRefFrame());

        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getUTC()),
                            file.getMetaData().getDefEpochT0());
        Assert.assertEquals(TimeScalesFactory.getUTC(), file.getMetaData().getDefTimeSystem().getTimeScale(null));

        // TODO test orbit data

    }

    @Test
    public void testParseOCM2() {
        final String   ex  = "/ccsds/OCMExample2.txt";
        final OCMFile file = new OCMParser().parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals("COMMENT This OCM reflects the latest conditions post-maneuver A67Z\n" + 
                            "COMMENT This example shows the specification of multiple comment lines",
                            file.getMetaDataComment());
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        Assert.assertEquals("JAXA",                                file.getOriginator());
        Assert.assertEquals("R. Rabbit",                           file.getMetaData().getOriginatorPOC());
        Assert.assertEquals("Flight Dynamics Mission Design Lead", file.getMetaData().getOriginatorPosition());
        Assert.assertEquals("(719)555-1234",                       file.getMetaData().getOriginatorPhone());
        Assert.assertEquals("Mr. Rodgers",                         file.getMetaData().getTechPOC());
        Assert.assertEquals("(719)555-1234",                       file.getMetaData().getTechPhone());
        Assert.assertEquals("email@email.XXX ",                    file.getMetaData().getTechAddress());
        Assert.assertEquals("GODZILLA 5",                          file.getMetaData().getObjectName());
        Assert.assertEquals("1998-999A",                           file.getMetaData().getInternationalDesignator());
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, TimeScalesFactory.getUTC()),
                            file.getMetaData().getDefEpochT0());
        Assert.assertEquals("UT1", file.getMetaData().getDefTimeSystem().getTimeScale(IERSConventions.IERS_2010).getName());
        Assert.assertEquals(36.0,                                  file.getMetaData().getTaimutcT0(), 1.0e-15);
        Assert.assertEquals(0.357,                                 file.getMetaData().getUt1mutcT0(), 1.0e-15);

        // TODO test orbit data

        // TODO test physical data

        // TODO test perturbation data

        // TODO test user data

    }

    @Test
    public void testParseOCM3() {
        final String   ex  = "/ccsds/OCMExample3.txt";
        final OCMFile file = new OCMParser().parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertNull(file.getMetaDataComment());
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getUTC()),
                            file.getMetaData().getDefEpochT0());
        Assert.assertEquals("UTC", file.getMetaData().getDefTimeSystem().getTimeScale(IERSConventions.IERS_2010).getName());

        // TODO test orbit data

        // TODO test physical data

        // TODO test maneuvers data

        // TODO test perturbation data

        // TODO test orbit determination data

    }

    @Test
    public void testParseOCM4() {
        final String   ex  = "/ccsds/OCMExample4.txt";
        final OCMFile file = new OCMParser().parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals("COMMENT  This file is a dummy example with inconsistent data\n" + 
                            "COMENT   it is used to exercise all possible keys in Key-Value Notation",
                            file.getMetaDataComment());
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23, 10, 29, 31.576, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        Assert.assertEquals("JPL",                                 file.getOriginator());
        Assert.assertEquals("Mr. Rodgers",                         file.getMetaData().getOriginatorPOC());
        Assert.assertEquals("Flight Dynamics Mission Design Lead", file.getMetaData().getOriginatorPosition());
        Assert.assertEquals("+49615130312",                        file.getMetaData().getOriginatorPhone());
        Assert.assertEquals("JOHN.DOE@EXAMPLE.ORG",                file.getMetaData().getOriginatorAddress());
        Assert.assertEquals("NASA",                                file.getMetaData().getTechOrg());
        Assert.assertEquals("Maxwell Smart",                       file.getMetaData().getTechPOC());
        Assert.assertEquals("+49615130312",                        file.getMetaData().getTechPhone());
        Assert.assertEquals("MAX@EXAMPLE.ORG",                     file.getMetaData().getTechAddress());
        Assert.assertEquals("ABC-12_34",                           file.getMessageID());
        Assert.assertEquals("ABC-12_33",                           file.getMetaData().getPrevMessageID());
        Assert.assertEquals(new AbsoluteDate(2001, 11, 6, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getPrevMessageEpoch());
        Assert.assertEquals("ABC-12_35",                           file.getMetaData().getNextMessageID());
        Assert.assertEquals(new AbsoluteDate(2001, 11, 7, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getNextMessageEpoch());
        Assert.assertEquals("FOUO",                                file.getMessageClassification());
        Assert.assertEquals(1,                                     file.getMetaData().getAttMessageLink().size());
        Assert.assertEquals("ADM_MSG_35132.txt",                   file.getMetaData().getAttMessageLink().get(0));
        Assert.assertEquals(2,                                     file.getMetaData().getCdmMessageLink().size());
        Assert.assertEquals("CDM_MSG_35132.txt",                   file.getMetaData().getCdmMessageLink().get(0));
        Assert.assertEquals("CDM_MSG_35133.txt",                   file.getMetaData().getCdmMessageLink().get(1));
        Assert.assertEquals(2,                                     file.getMetaData().getPrmMessageLink().size());
        Assert.assertEquals("PRM_MSG_35132.txt",                   file.getMetaData().getPrmMessageLink().get(0));
        Assert.assertEquals("PRM_MSG_35133.txt",                   file.getMetaData().getPrmMessageLink().get(1));
        Assert.assertEquals(2,                                     file.getMetaData().getPrmMessageLink().size());
        Assert.assertEquals("RDM_MSG_35132.txt",                   file.getMetaData().getRdmMessageLink().get(0));
        Assert.assertEquals("RDM_MSG_35133.txt",                   file.getMetaData().getRdmMessageLink().get(1));
        Assert.assertEquals(3,                                     file.getMetaData().getRdmMessageLink().size());
        Assert.assertEquals("TDM_MSG_35132.txt",                   file.getMetaData().getTdmMessageLink().get(0));
        Assert.assertEquals("TDM_MSG_35133.txt",                   file.getMetaData().getTdmMessageLink().get(1));
        Assert.assertEquals("TDM_MSG_35134.txt",                   file.getMetaData().getTdmMessageLink().get(2));
        Assert.assertEquals("UNKNOWN",                             file.getMetaData().getObjectName());
        Assert.assertEquals("9999-999Z",                           file.getMetaData().getInternationalDesignator());
        Assert.assertEquals("22444",                               file.getMetaData().getObjectID());
        Assert.assertEquals("INTELSAT",                            file.getMetaData().getOperator());
        Assert.assertEquals("SIRIUS",                              file.getMetaData().getOwner());
        Assert.assertEquals("EOS",                                 file.getMetaData().getMission());
        Assert.assertEquals("SPIRE",                               file.getMetaData().getConstellation());
        Assert.assertEquals(new AbsoluteDate(2011, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getLaunchEpoch());
        Assert.assertEquals("FRANCE",                              file.getMetaData().getLaunchCountry());
        Assert.assertEquals("FRENCH GUIANA",                       file.getMetaData().getLaunchSite());
        Assert.assertEquals("ARIANESPACE",                         file.getMetaData().getLaunchProvider());
        Assert.assertEquals("ULA",                                 file.getMetaData().getLaunchIntegrator());
        Assert.assertEquals("LC-41",                               file.getMetaData().getLaunchPad());
        Assert.assertEquals("GROUND",                              file.getMetaData().getLaunchPlatform());
        Assert.assertEquals(new AbsoluteDate(2021, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getReleaseEpoch());
        Assert.assertEquals(new AbsoluteDate(2031, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getMissionStartEpoch());
        Assert.assertEquals(new AbsoluteDate(2041, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getMissionEndEpoch());
        Assert.assertEquals(new AbsoluteDate(2051, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetaData().getReentryEpoch());
        Assert.assertEquals(22.0 * Constants.JULIAN_DAY,           file.getMetaData().getLifetime(), 1.0e-15);
        Assert.assertEquals("COMSPOC",                             file.getMetaData().getCatalogName());
        Assert.assertEquals("Payload",                             file.getMetaData().getObjectType().toString());
        Assert.assertEquals("Operational",                         file.getMetaData().getOpsStatus().toString());
        Assert.assertEquals("Extended Geostationary Orbit",        file.getMetaData().getOrbitType().toString());
        Assert.assertEquals(8,                                     file.getMetaData().getOcmDataElements().size());
        Assert.assertEquals("ORB",                                 file.getMetaData().getOcmDataElements().get(0));
        Assert.assertEquals("PHYSCHAR",                            file.getMetaData().getOcmDataElements().get(1));
        Assert.assertEquals("MNVR",                                file.getMetaData().getOcmDataElements().get(2));
        Assert.assertEquals("COV",                                 file.getMetaData().getOcmDataElements().get(3));
        Assert.assertEquals("OD",                                  file.getMetaData().getOcmDataElements().get(4));
        Assert.assertEquals("PERTS",                               file.getMetaData().getOcmDataElements().get(5));
        Assert.assertEquals("STM",                                 file.getMetaData().getOcmDataElements().get(6));
        Assert.assertEquals("USER",                                file.getMetaData().getOcmDataElements().get(7));
        Assert.assertEquals(new AbsoluteDate(2001,11, 10, TimeScalesFactory.getUTC()),
                            file.getMetaData().getDefEpochT0());
        Assert.assertEquals("UTC", file.getMetaData().getDefTimeSystem().getTimeScale(null).getName());
        Assert.assertEquals(2.5,                                   file.getMetaData().getSecClockPerSISecond(), 1.0e-15);
        Assert.assertEquals(88740.0,                               file.getMetaData().getSecPerDay(), 1.0e-15);
        Assert.assertEquals(12.0,
                            file.getMetaData().getEarliestTime().durationFrom(file.getMetaData().getDefEpochT0()),
                            1.0e-15);
        Assert.assertEquals(new AbsoluteDate(2001,11, 13, TimeScalesFactory.getUTC()),
                            file.getMetaData().getLatestTime());
        Assert.assertEquals(20.0 * Constants.JULIAN_DAY,           file.getMetaData().getTimeSpan(), 1.0e-15);
        Assert.assertEquals(36.0,                                  file.getMetaData().getTaimutcT0(), 1.0e-15);
        Assert.assertEquals(0.357,                                 file.getMetaData().getUt1mutcT0(), 1.0e-15);
        Assert.assertEquals("CelesTrak EOP file downloaded from http://celestrak.com/SpaceData/EOP-Last5Years.txt at 2001-11-08T00:00:00",
                            file.getMetaData().getEopSource());
        Assert.assertEquals("LAGRANGE_ORDER_5",                    file.getMetaData().getInterpMethodEOP());

        // TODO test orbit data

        // TODO test physical data

        // TODO test perturbation data

        // TODO test user data

    }

}
