/* Copyright 2002-2016 CS Systèmes d'Information
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

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class OMMParserTest {

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseOMM1()
        throws OrekitException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String ex = "/ccsds/OMMExample.txt";

        // initialize parser with purposely wrong international designator
        // (in order to check it is correctly overridden when parsing)
        final OMMParser parser =
                new OMMParser().withMu(398600e9).withInternationalDesignator(1998, 1, "a");

        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OMMFile file = parser.parse(inEntry);

        // Check Header Block;
        Assert.assertEquals(2.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2007, 03, 06, 16, 00, 00,
                                             TimeScalesFactory.getUTC()),
                                             file.getCreationDate());
        Assert.assertEquals("NOAA/USA", file.getOriginator());

        // Check Metadata Block;

        Assert.assertEquals("GOES 9", file.getMetaData().getObjectName());
        Assert.assertEquals("1995-025A", file.getMetaData().getObjectID());
        Assert.assertEquals("EARTH", file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertEquals(file.getMetaData().getCenterBody(),
                            CelestialBodyFactory.getEarth());
        Assert.assertEquals(file.getMetaData().getFrame(), FramesFactory.getTEME());
        Assert.assertEquals(file.getMetaData().getTimeSystem(), CcsdsTimeScale.UTC);
        Assert.assertEquals("SGP/SGP4", file.getMetaData().getMeanElementTheory());
        Assert.assertEquals("TEME", file.getMetaData().getFrame().toString());
        Assert.assertTrue(file.getTLERelatedParametersComment().isEmpty());

        // Check Mean Keplerian elements data block;

        Assert.assertEquals(new AbsoluteDate(2007, 03, 05, 10, 34, 41.4264,
                                             TimeScalesFactory.getUTC()), file.getEpoch());
        Assert.assertEquals(file.getMeanMotion(), 1.00273272 * FastMath.PI / 43200.0 , 1e-10);
        Assert.assertEquals(file.getE(), 0.0005013, 1e-10);
        Assert.assertEquals(file.getI(), FastMath.toRadians(3.0539), 1e-10);
        Assert.assertEquals(file.getRaan(), FastMath.toRadians(81.7939), 1e-10);
        Assert.assertEquals(file.getPa(), FastMath.toRadians(249.2363), 1e-10);
        Assert.assertEquals(file.getAnomaly(), FastMath.toRadians(150.1602), 1e-10);
        Assert.assertEquals(file.getMuParsed(), 398600.8 * 1e9, 1e-10);
        Assert.assertEquals(file.getMuSet(), 398600e9, 1e-10);
        Assert.assertEquals(file.getMuCreated(), CelestialBodyFactory.getEarth().getGM(), 1e-10);


        // Check TLE Related Parameters data block;

        Assert.assertEquals(0, file.getEphemerisType());
        Assert.assertEquals('U', file.getClassificationType());
        int[] noradIDExpected = new int[23581];
        int[] noradIDActual = new int[file.getNoradID()];
        Assert.assertEquals(noradIDExpected[0], noradIDActual[0]);
        Assert.assertEquals("0925", file.getElementSetNumber());
        int[] revAtEpochExpected = new int[4316];
        int[] revAtEpochActual = new int[file.getRevAtEpoch()];
        Assert.assertEquals(1.00273272 * FastMath.PI / 43200.0, file.getMeanMotion(), 1e-10);
        Assert.assertEquals(revAtEpochExpected[0], revAtEpochActual[0]);
        Assert.assertEquals(file.getBStar(), 0.0001, 1e-10);
        Assert.assertEquals(file.getMeanMotionDot(), -0.00000113 * FastMath.PI / 1.86624e9, 1e-12);
        Assert.assertEquals(file.getMeanMotionDotDot(), 0.0 * FastMath.PI / 5.3747712e13, 1e-10);
        Assert.assertEquals(1995, file.getMetaData().getLaunchYear());
        Assert.assertEquals(25, file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A", file.getMetaData().getLaunchPiece());
        file.generateCartesianOrbit();
        file.generateKeplerianOrbit();
        try {
            file.generateSpacecraftState();
        } catch (OrekitException orekitException) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_SPACECRAFT_MASS, orekitException.getSpecifier());
        } finally {
        }
        file.generateTLE();
    }

    @Test
    public void testParseOMM2()
        throws OrekitException, URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = getClass().getResource("/ccsds/OMMExample2.txt").toURI().getPath();
        final OMMParser parser = new OMMParser().
                                 withMissionReferenceDate(new AbsoluteDate()).
                                 withConventions(IERSConventions.IERS_1996).
                                 withSimpleEOP(true).
                                 withInternationalDesignator(1998, 1, "a");

        final OMMFile file = parser.parse(name);
        Assert.assertEquals(file.getMissionReferenceDate().shiftedBy(210840), file.getMetaData().getFrameEpoch());
        Assert.assertEquals(6800e3, file.getA(), 1e-10);
        Assert.assertEquals(300, file.getMass(), 1e-10);
        Assert.assertEquals(5, file.getSolarRadArea(), 1e-10);
        Assert.assertEquals(0.001, file.getSolarRadCoeff(), 1e-10);
        Assert.assertEquals(null, file.getCovRefFrame());
        Assert.assertEquals(LOFType.TNW, file.getCovRefLofType());
        file.getCovarianceMatrix();
        HashMap<String, String> userDefinedParameters = new HashMap<String, String>();
        userDefinedParameters.put("USER_DEFINED_EARTH_MODEL", "WGS-84");
        Assert.assertEquals(userDefinedParameters,
                            file.getUserDefinedParameters());
        Assert.assertTrue(file.hasCovarianceMatrix());
        ArrayList<String> headerComment = new ArrayList<String>();
        headerComment.add("this is a comment");
        headerComment.add("here is another one");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        ArrayList<String> metadataComment = new ArrayList<String>();
        metadataComment.add("this comment doesn't say much");
        Assert.assertEquals(metadataComment, file.getMetaData().getComment());
        ArrayList<String> epochComment = new ArrayList<String>();
        epochComment.add("the following data is what we're looking for");
        Assert.assertEquals(epochComment, file.getEpochComment());
        ArrayList<String> dataSpacecraftComment = new ArrayList<String>();
        dataSpacecraftComment.add("spacecraft data");
        Assert.assertEquals(dataSpacecraftComment, file.getSpacecraftComment());
        ArrayList<String> dataCovarianceComment = new ArrayList<String>();
        dataCovarianceComment.add("Covariance matrix");
        Assert.assertEquals(dataCovarianceComment, file.getCovarianceComment());
        Assert.assertEquals(1995, file.getMetaData().getLaunchYear());
        Assert.assertEquals(25, file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A", file.getMetaData().getLaunchPiece());
        file.generateSpacecraftState();
        file.generateKeplerianOrbit();

    }

    @Test
    public void testWrongKeyword()
        throws OrekitException, URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = getClass().getResource("/ccsds/OMM-wrong-keyword.txt").toURI().getPath();
        final OMMParser parser = new OMMParser().
                                 withMissionReferenceDate(new AbsoluteDate()).
                                 withConventions(IERSConventions.IERS_1996);
        try {
            parser.parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testOrbitFileInterface() throws OrekitException {
        // simple test for OMM file, contains p/v entries and other mandatory data.
        final String ex = "/ccsds/OMMExample.txt";

        // initialize parser with purposely wrong international designator
        // (in order to check it is correctly overridden when parsing)
        final OMMParser parser =
                new OMMParser().withMu(398600e9).withInternationalDesignator(1998, 1, "a");

        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OMMFile file = parser.parse(inEntry, "OMMExample.txt");

        final String satId = "1995-025A";
        Assert.assertEquals(satId, file.getMetaData().getObjectID());

    }

    @Test
    public void testWrongODMType() {
        try {
            new OMMParser().parse(getClass().getResourceAsStream("/ccsds/OEMExample.txt"), "OEMExample.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("OEMExample.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_OEM_VERS = 2.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testNumberFormatErrorType() {
        try {
            new OMMParser().parse(getClass().getResourceAsStream("/ccsds/OMM-number-format-error.txt"),
                                                                 "OMM-number-format-error.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(15, oe.getParts()[0]);
            Assert.assertEquals("OMM-number-format-error.txt", oe.getParts()[1]);
            Assert.assertEquals("ARG_OF_PERICENTER = this-is-not-a-number", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/OMMExample.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new OMMParser().parse(wrongName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

}
