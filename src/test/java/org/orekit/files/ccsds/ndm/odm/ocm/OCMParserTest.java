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
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.ndm.odm.ocm.ElementsType;
import org.orekit.files.ccsds.ndm.odm.ocm.OCMFile;
import org.orekit.files.ccsds.ndm.odm.ocm.OCMParser;
import org.orekit.files.ccsds.utils.CCSDSFrame;
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
        final String realName = getClass().getResource("/ccsds/odm/ocm/OCMExample1.txt").toURI().getPath();
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
        final String name = getClass().getResource("/ccsds/odm/ocm/OCM-missing-t0.txt").toURI().getPath();
        try {
            new OCMParser().parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(Keyword.EPOCH_TZERO, oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongDate() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/odm/ocm/OCM-wrong-date.txt").toURI().getPath();
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
        final String name = getClass().getResource("/ccsds/odm/ocm/OCM-spurious-metadata-section.txt").toURI().getPath();
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
        final String   ex  = "/ccsds/odm/ocm/OCMExample1.txt";
        final OCMFile file = new OCMParser().
                             withConventions(IERSConventions.IERS_2010).
                             parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // check the default values that are not set in this simple file
        Assert.assertEquals("CSPOC",              file.getMetadata().getCatalogName());
        Assert.assertEquals(1.0,                  file.getMetadata().getSecClockPerSISecond(), 1.0e-15);
        Assert.assertEquals(Constants.JULIAN_DAY, file.getMetadata().getSecPerDay(),           1.0e-15);
        Assert.assertEquals("LINEAR",             file.getMetadata().getInterpMethodEOP());

        // Check Header Block;
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        // OCM is the only message for which OBJECT_NAME, OBJECT_ID,
        // CENTER_NAME and REF_FRAME are not mandatory, they are not present in this minimal file
        Assert.assertNull(file.getMetadata().getObjectName());
        Assert.assertNull(file.getMetadata().getObjectID());
        Assert.assertNull(file.getMetadata().getCenterName());
        Assert.assertNull(file.getMetadata().getRefFrame());

        Assert.assertEquals("JPL", file.getOriginator());

        final AbsoluteDate t0 = new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getUTC());
        Assert.assertEquals(t0, file.getMetadata().getEpochT0());
        Assert.assertEquals(TimeScalesFactory.getUTC(), file.getMetadata().getTimeSystem().getTimeScale(null));

        // orbit data
        Assert.assertEquals(1, file.getOrbitStateTimeHistories().size());
        OCMFile.OrbitStateHistory history = file.getOrbitStateTimeHistories().get(0);
        Assert.assertEquals("intervening data records omitted between DT=20.0 and DT=500.0", history.getComment().get(0));
        Assert.assertEquals("OSCULATING", history.getOrbAveraging());
        Assert.assertEquals("EARTH", history.getCenterName());
        Assert.assertEquals(CCSDSFrame.ITRF2000, history.getOrbRefFrame());
        Assert.assertEquals(ElementsType.CARTPV, history.getOrbType());
        Assert.assertEquals(0.0, history.getOrbEpochT0().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(file.getMetadata().getTimeSystem(), history.getOrbTimeSystem());
        List<OCMFile.OrbitalState> states = history.getOrbitalStates();
        Assert.assertEquals(4, states.size());

        Assert.assertEquals(0.0, states.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(0).getElements().length);
        Assert.assertEquals( 2789600.0, states.get(0).getElements()[0], 1.0e-15);
        Assert.assertEquals( -280000.0, states.get(0).getElements()[1], 1.0e-15);
        Assert.assertEquals(-1746800.0, states.get(0).getElements()[2], 1.0e-15);
        Assert.assertEquals(    4730.0, states.get(0).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2500.0, states.get(0).getElements()[4], 1.0e-15);
        Assert.assertEquals(   -1040.0, states.get(0).getElements()[5], 1.0e-15);

        Assert.assertEquals(10.0, states.get(1).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(1).getElements().length);
        Assert.assertEquals( 2783400.0, states.get(1).getElements()[0], 1.0e-15);
        Assert.assertEquals( -308100.0, states.get(1).getElements()[1], 1.0e-15);
        Assert.assertEquals(-1877100.0, states.get(1).getElements()[2], 1.0e-15);
        Assert.assertEquals(    5190.0, states.get(1).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2420.0, states.get(1).getElements()[4], 1.0e-15);
        Assert.assertEquals(   -2000.0, states.get(1).getElements()[5], 1.0e-15);

        Assert.assertEquals(20.0, states.get(2).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(2).getElements().length);
        Assert.assertEquals( 2776000.0, states.get(2).getElements()[0], 1.0e-15);
        Assert.assertEquals( -336900.0, states.get(2).getElements()[1], 1.0e-15);
        Assert.assertEquals(-2008700.0, states.get(2).getElements()[2], 1.0e-15);
        Assert.assertEquals(    5640.0, states.get(2).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2340.0, states.get(2).getElements()[4], 1.0e-15);
        Assert.assertEquals(   -1950.0, states.get(2).getElements()[5], 1.0e-15);

        Assert.assertEquals(500.0, states.get(3).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(3).getElements().length);
        Assert.assertEquals( 2164375.0,  states.get(3).getElements()[0], 1.0e-15);
        Assert.assertEquals( 1115811.0,  states.get(3).getElements()[1], 1.0e-15);
        Assert.assertEquals( -688131.0,  states.get(3).getElements()[2], 1.0e-15);
        Assert.assertEquals(   -3533.28, states.get(3).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2884.52, states.get(3).getElements()[4], 1.0e-15);
        Assert.assertEquals(     885.35, states.get(3).getElements()[5], 1.0e-15);

    }

    // temporarily ignore the test as reference frame EFG is not available
    @Ignore
    @Test
    public void testParseOCM2() {
        final String   ex  = "/ccsds/odm/ocm/OCMExample2.txt";
        final OCMFile file = new OCMParser().parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals("COMMENT This OCM reflects the latest conditions post-maneuver A67Z\n" + 
                            "COMMENT This example shows the specification of multiple comment lines",
                            file.getMetadataComment());
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        Assert.assertEquals("JAXA",                                file.getOriginator());
        Assert.assertEquals("R. Rabbit",                           file.getMetadata().getOriginatorPOC());
        Assert.assertEquals("Flight Dynamics Mission Design Lead", file.getMetadata().getOriginatorPosition());
        Assert.assertEquals("(719)555-1234",                       file.getMetadata().getOriginatorPhone());
        Assert.assertEquals("Mr. Rodgers",                         file.getMetadata().getTechPOC());
        Assert.assertEquals("(719)555-1234",                       file.getMetadata().getTechPhone());
        Assert.assertEquals("email@email.XXX ",                    file.getMetadata().getTechAddress());
        Assert.assertEquals("GODZILLA 5",                          file.getMetadata().getObjectName());
        Assert.assertEquals("1998-999A",                           file.getMetadata().getInternationalDesignator());
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, TimeScalesFactory.getUTC()),
                            file.getMetadata().getEpochT0());
        Assert.assertEquals("UT1", file.getMetadata().getTimeSystem().getTimeScale(IERSConventions.IERS_2010).getName());
        Assert.assertEquals(36.0,                                  file.getMetadata().getTaimutcT0(), 1.0e-15);
        Assert.assertEquals(0.357,                                 file.getMetadata().getUt1mutcT0(), 1.0e-15);

        // TODO test orbit data

        // TODO test physical data

        // TODO test perturbation data

        // TODO test user data

    }

    @Test
    public void testParseOCM3() {
        final String   ex  = "/ccsds/odm/ocm/OCMExample3.txt";
        final OCMFile file = new OCMParser().
                             withConventions(IERSConventions.IERS_2010).
                             parse(getClass().getResourceAsStream(ex),
                                                   ex.substring(ex.lastIndexOf('/') + 1));

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(0, file.getMetadataComment().size());
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getUTC()),
                            file.getMetadata().getEpochT0());
        Assert.assertEquals("UTC", file.getMetadata().getTimeSystem().getTimeScale(IERSConventions.IERS_2010).getName());

        // TODO test orbit data

        // TODO test physical data

        // TODO test maneuvers data

        // TODO test perturbation data

        // TODO test orbit determination data

    }

    @Test
    public void testParseOCM4() {
        final String   ex  = "/ccsds/odm/ocm/OCMExample4.txt";
        final OCMFile file = new OCMParser().
                        withConventions(IERSConventions.IERS_2010).
                        parse(getClass().getResourceAsStream(ex), ex.substring(ex.lastIndexOf('/') + 1));

        // Check Header Block;
        Assert.assertEquals(3.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(2, file.getHeaderComment().size());
        Assert.assertEquals("This file is a dummy example with inconsistent data", file.getHeaderComment().get(0));
        Assert.assertEquals("it is used to exercise all possible keys in Key-Value Notation", file.getHeaderComment().get(1));
        Assert.assertEquals("ABC-12-34",                           file.getMessageID());

        // Check metadata
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23, 10, 29, 31.576, TimeScalesFactory.getUTC()),
                            file.getCreationDate());

        Assert.assertEquals(1,                                     file.getMetadata().getComments().size());
        Assert.assertEquals("Metadata comment",                    file.getMetadata().getComments().get(0));
        Assert.assertEquals("JPL",                                 file.getOriginator());
        Assert.assertEquals("MR. RODGERS",                         file.getMetadata().getOriginatorPOC());
        Assert.assertEquals("FLIGHT DYNAMICS MISSION DESIGN LEAD", file.getMetadata().getOriginatorPosition());
        Assert.assertEquals("+49615130312",                        file.getMetadata().getOriginatorPhone());
        Assert.assertEquals("JOHN.DOE@EXAMPLE.ORG",                file.getMetadata().getOriginatorAddress());
        Assert.assertEquals("NASA",                                file.getMetadata().getTechOrg());
        Assert.assertEquals("MAXWELL SMART",                       file.getMetadata().getTechPOC());
        Assert.assertEquals("+49615130312",                        file.getMetadata().getTechPhone());
        Assert.assertEquals("MAX@EXAMPLE.ORG",                     file.getMetadata().getTechAddress());
        Assert.assertEquals("ABC-12-33",                           file.getMetadata().getPrevMessageID());
        Assert.assertEquals(new AbsoluteDate(2001, 11, 6, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getPrevMessageEpoch());
        Assert.assertEquals("ABC-12-35",                           file.getMetadata().getNextMessageID());
        Assert.assertEquals(new AbsoluteDate(2001, 11, 7, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getNextMessageEpoch());
        Assert.assertEquals("FOUO",                                file.getMetadata().getClassification());
        Assert.assertEquals(1,                                     file.getMetadata().getAttMessageLink().size());
        Assert.assertEquals("ADM-MSG-35132.TXT",                   file.getMetadata().getAttMessageLink().get(0));
        Assert.assertEquals(2,                                     file.getMetadata().getCdmMessageLink().size());
        Assert.assertEquals("CDM-MSG-35132.TXT",                   file.getMetadata().getCdmMessageLink().get(0));
        Assert.assertEquals("CDM-MSG-35133.TXT",                   file.getMetadata().getCdmMessageLink().get(1));
        Assert.assertEquals(2,                                     file.getMetadata().getPrmMessageLink().size());
        Assert.assertEquals("PRM-MSG-35132.TXT",                   file.getMetadata().getPrmMessageLink().get(0));
        Assert.assertEquals("PRM-MSG-35133.TXT",                   file.getMetadata().getPrmMessageLink().get(1));
        Assert.assertEquals(2,                                     file.getMetadata().getRdmMessageLink().size());
        Assert.assertEquals("RDM-MSG-35132.TXT",                   file.getMetadata().getRdmMessageLink().get(0));
        Assert.assertEquals("RDM-MSG-35133.TXT",                   file.getMetadata().getRdmMessageLink().get(1));
        Assert.assertEquals(3,                                     file.getMetadata().getTdmMessageLink().size());
        Assert.assertEquals("TDM-MSG-35132.TXT",                   file.getMetadata().getTdmMessageLink().get(0));
        Assert.assertEquals("TDM-MSG-35133.TXT",                   file.getMetadata().getTdmMessageLink().get(1));
        Assert.assertEquals("TDM-MSG-35134.TXT",                   file.getMetadata().getTdmMessageLink().get(2));
        Assert.assertEquals("UNKNOWN",                             file.getMetadata().getObjectName());
        Assert.assertEquals("9999-999Z",                           file.getMetadata().getInternationalDesignator());
        Assert.assertEquals("22444",                               file.getMetadata().getObjectID());
        Assert.assertEquals("INTELSAT",                            file.getMetadata().getOperator());
        Assert.assertEquals("SIRIUS",                              file.getMetadata().getOwner());
        Assert.assertEquals("EOS",                                 file.getMetadata().getMission());
        Assert.assertEquals("SPIRE",                               file.getMetadata().getConstellation());
        Assert.assertEquals(new AbsoluteDate(2011, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getLaunchEpoch());
        Assert.assertEquals("FRANCE",                              file.getMetadata().getLaunchCountry());
        Assert.assertEquals("FRENCH GUIANA",                       file.getMetadata().getLaunchSite());
        Assert.assertEquals("ARIANESPACE",                         file.getMetadata().getLaunchProvider());
        Assert.assertEquals("ULA",                                 file.getMetadata().getLaunchIntegrator());
        Assert.assertEquals("LC-41",                               file.getMetadata().getLaunchPad());
        Assert.assertEquals("GROUND",                              file.getMetadata().getLaunchPlatform());
        Assert.assertEquals(new AbsoluteDate(2021, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getReleaseEpoch());
        Assert.assertEquals(new AbsoluteDate(2031, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getMissionStartEpoch());
        Assert.assertEquals(new AbsoluteDate(2041, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getMissionEndEpoch());
        Assert.assertEquals(new AbsoluteDate(2051, 11, 8, 11, 17, 33, TimeScalesFactory.getUTC()),
                            file.getMetadata().getReentryEpoch());
        Assert.assertEquals(22.0 * Constants.JULIAN_DAY,           file.getMetadata().getLifetime(), 1.0e-15);
        Assert.assertEquals("COMSPOC",                             file.getMetadata().getCatalogName());
        Assert.assertEquals("Payload",                             file.getMetadata().getObjectType().toString());
        Assert.assertEquals("Operational",                         file.getMetadata().getOpsStatus().toString());
        Assert.assertEquals("Extended Geostationary Orbit",        file.getMetadata().getOrbitType().toString());
        Assert.assertEquals(8,                                     file.getMetadata().getOcmDataElements().size());
        Assert.assertEquals("ORB",                                 file.getMetadata().getOcmDataElements().get(0));
        Assert.assertEquals("PHYSCHAR",                            file.getMetadata().getOcmDataElements().get(1));
        Assert.assertEquals("MNVR",                                file.getMetadata().getOcmDataElements().get(2));
        Assert.assertEquals("COV",                                 file.getMetadata().getOcmDataElements().get(3));
        Assert.assertEquals("OD",                                  file.getMetadata().getOcmDataElements().get(4));
        Assert.assertEquals("PERTS",                               file.getMetadata().getOcmDataElements().get(5));
        Assert.assertEquals("STM",                                 file.getMetadata().getOcmDataElements().get(6));
        Assert.assertEquals("USER",                                file.getMetadata().getOcmDataElements().get(7));
        Assert.assertEquals(new AbsoluteDate(2001,11, 10, 11, 17, 33.0, TimeScalesFactory.getUTC()),
                            file.getMetadata().getEpochT0());
        Assert.assertEquals("UTC", file.getMetadata().getTimeSystem().getTimeScale(null).getName());
        Assert.assertEquals(2.5,                                   file.getMetadata().getSecClockPerSISecond(), 1.0e-15);
        Assert.assertEquals(88740.0,                               file.getMetadata().getSecPerDay(), 1.0e-15);
        Assert.assertEquals(12.0,
                            file.getMetadata().getEarliestTime().durationFrom(file.getMetadata().getEpochT0()),
                            1.0e-15);
        Assert.assertEquals(new AbsoluteDate(2001,11, 13, TimeScalesFactory.getUTC()),
                            file.getMetadata().getLatestTime());
        Assert.assertEquals(20.0 * Constants.JULIAN_DAY,           file.getMetadata().getTimeSpan(), 1.0e-15);
        Assert.assertEquals(36.0,                                  file.getMetadata().getTaimutcT0(), 1.0e-15);
        Assert.assertEquals(0.357,                                 file.getMetadata().getUt1mutcT0(), 1.0e-15);
        Assert.assertEquals("CELESTRAK EOP FILE DOWNLOADED FROM HTTP://CELESTRAK.COM/SPACEDATA/EOP-LAST5YEARS.TXT AT 2001-11-08T00:00:00",
                            file.getMetadata().getEopSource());
        Assert.assertEquals("LAGRANGE ORDER 5",                    file.getMetadata().getInterpMethodEOP());

        // TODO test orbit data

        // TODO test physical data

        // TODO test perturbation data

        // TODO test user data

    }

}
