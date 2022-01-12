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
package org.orekit.files.ccsds.ndm.odm.omm;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParameters;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class OmmParserTest {

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseOMM1() {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String ex = "/ccsds/odm/omm/OMMExample1.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // initialize parser
        final OmmParser parser = new ParserBuilder().withMu(398600e9).withDefaultMass(1000.0).buildOmmParser();
        final Omm   file   = parser.parseMessage(source);

        // Check Header Block;
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2007, 03, 06, 16, 00, 00,
                                             TimeScalesFactory.getUTC()),
                                             file.getHeader().getCreationDate());
        Assert.assertEquals("NOAA/USA", file.getHeader().getOriginator());
        Assert.assertNull(file.getHeader().getMessageId());

        // Check Metadata Block;

        Assert.assertEquals("GOES 9", file.getMetadata().getObjectName());
        Assert.assertEquals("1995-025A", file.getMetadata().getObjectID());
        Assert.assertEquals("EARTH", file.getMetadata().getCenter().getName());
        Assert.assertNotNull(file.getMetadata().getCenter().getBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetadata().getCenter().getBody());
        Assert.assertEquals(FramesFactory.getTEME(), file.getMetadata().getFrame());
        Assert.assertEquals("UTC",      file.getMetadata().getTimeSystem().name());
        Assert.assertEquals("SGP/SGP4", file.getMetadata().getMeanElementTheory());
        Assert.assertEquals("TEME", file.getMetadata().getFrame().toString());
        Assert.assertTrue(file.getData().getTLEBlock().getComments().isEmpty());

        // Check Mean Keplerian elements data block;
        KeplerianElements kep = file.getData().getKeplerianElementsBlock();
        Assert.assertEquals(new AbsoluteDate(2007, 03, 05, 10, 34, 41.4264,
                                             TimeScalesFactory.getUTC()),
                            file.getDate());
        Assert.assertEquals(1.00273272 * FastMath.PI / 43200.0, kep.getMeanMotion(), 1e-10);
        Assert.assertEquals(0.0005013, kep.getE(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(3.0539), kep.getI(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(81.7939), kep.getRaan(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(249.2363), kep.getPa(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(150.1602), kep.getAnomaly(), 1e-10);
        Assert.assertEquals(398600.8 * 1e9, kep.getMu(), 1e-10);


        // Check TLE Related Parameters data block;
        OmmTle tle = file.getData().getTLEBlock();
        Assert.assertEquals(0, tle.getEphemerisType());
        Assert.assertEquals('U', tle.getClassificationType());
        int[] noradIDExpected = new int[23581];
        int[] noradIDActual = new int[tle.getNoradID()];
        Assert.assertEquals(noradIDExpected[0], noradIDActual[0]);
        Assert.assertEquals(925, tle.getElementSetNumber());
        int[] revAtEpochExpected = new int[4316];
        int[] revAtEpochActual = new int[tle.getRevAtEpoch()];
        Assert.assertEquals(revAtEpochExpected[0], revAtEpochActual[0]);
        Assert.assertEquals(0.0001, tle.getBStar(), 1e-10);
        Assert.assertEquals(-0.00000113 * FastMath.PI / 1.86624e9, tle.getMeanMotionDot(), 1e-12);
        Assert.assertEquals(0.0 * FastMath.PI / 5.3747712e13, tle.getMeanMotionDotDot(), 1e-10);
        Assert.assertEquals(1995, file.getMetadata().getLaunchYear());
        Assert.assertEquals(25, file.getMetadata().getLaunchNumber());
        Assert.assertEquals("A", file.getMetadata().getLaunchPiece());
        file.generateKeplerianOrbit();
        try {
            file.generateSpacecraftState();
        } catch (OrekitException orekitException) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_SPACECRAFT_MASS, orekitException.getSpecifier());
        }
        TLE generated = file.generateTLE();
        Assert.assertEquals("1 23581U 95025A   07064.44075725 -.00000113  00000-0  10000-3 0  9250", generated.getLine1());
        Assert.assertEquals("2 23581   3.0539  81.7939 0005013 249.2363 150.1602  1.00273272 43169", generated.getLine2());
    }

    @Test
    public void testParseOMM2KVN() throws URISyntaxException {
        String name = "/ccsds/odm/omm/OMMExample2.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OmmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).buildOmmParser();

        validateOMM2(parser.parseMessage(source));
    }

    @Test
    public void testParseOMM2XML() throws URISyntaxException {
        String name = "/ccsds/odm/omm/OMMExample2.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OmmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).buildOmmParser();

        validateOMM2(parser.parseMessage(source));
    }

    @Test
    public void testWriteOMM3() throws URISyntaxException, IOException {
        final String name = "/ccsds/odm/omm/OMMExample2.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OmmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).buildOmmParser();
        final Omm original = parser.parseMessage(source);

        // write the parsed file back to a characters array
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, OmmWriter.KVN_PADDING_WIDTH, "dummy", 60);
        new WriterBuilder().buildOmmWriter().writeMessage(generator, original);

        // reparse the written file
        final byte[]     bytes = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        final Omm    rebuilt = new ParserBuilder().buildOmmParser().parseMessage(source2);
        validateOMM2(rebuilt);

    }

    private void validateOMM2(final Omm file) throws URISyntaxException {
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals("SGP/SGP4", file.getMetadata().getMeanElementTheory());
        final KeplerianElements kep = file.getData().getKeplerianElementsBlock();
        Assert.assertEquals(1.00273272, Constants.JULIAN_DAY * kep.getMeanMotion() / MathUtils.TWO_PI, 1e-10);
        Assert.assertTrue(Double.isNaN(file.getData().getMass()));
        CartesianCovariance covariance = file.getData().getCovarianceBlock();
        Assert.assertEquals(FramesFactory.getTEME(), covariance.getReferenceFrame().asFrame());
        Assert.assertEquals(6, covariance.getCovarianceMatrix().getRowDimension());
        Assert.assertEquals(6, covariance.getCovarianceMatrix().getColumnDimension());
        Assert.assertEquals(1995, file.getMetadata().getLaunchYear());
        Assert.assertEquals(25, file.getMetadata().getLaunchNumber());
        Assert.assertEquals("A", file.getMetadata().getLaunchPiece());
        file.generateKeplerianOrbit();

        Array2DRowRealMatrix covMatrix = new Array2DRowRealMatrix(6, 6);
        double[] column1 = {
            333.1349476038534, 461.8927349220216,
            -307.0007847730449, -0.3349365033922630,
            -0.2211832501084875, -0.3041346050686871
        };
        double[] column2 = {
            461.8927349220216, 678.2421679971363,
            -422.1234189514228, -0.4686084221046758,
            -0.2864186892102733, -0.4989496988610662
        };
        double[] column3 = {
            -307.0007847730449, -422.1234189514228,
            323.1931992380369, 0.2484949578400095,
            0.1798098699846038, 0.3540310904497689
        };
        double[] column4 = {
            -0.3349365033922630, -0.4686084221046758,
            0.2484949578400095, 0.0004296022805587290,
            0.0002608899201686016, 0.0001869263192954590
        };
        double[] column5 = {
            -0.2211832501084875, -0.2864186892102733,
            0.1798098699846038, 0.0002608899201686016,
            0.0001767514756338532, 0.0001008862586240695
        };
        double[] column6 = {
            -0.3041346050686871, -0.4989496988610662,
            0.3540310904497689, 0.0001869263192954590,
            0.0001008862586240695, 0.0006224444338635500
        };
        covMatrix.setColumn(0, column1);
        covMatrix.setColumn(1, column2);
        covMatrix.setColumn(2, column3);
        covMatrix.setColumn(3, column4);
        covMatrix.setColumn(4, column5);
        covMatrix.setColumn(5, column6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(covMatrix.getEntry(i, j),
                                    covariance.getCovarianceMatrix().getEntry(i, j),
                                    1e-15);
            }
        }

    }

    @Test
    public void testParseOMM3() throws URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = "/ccsds/odm/omm/OMMExample3.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final AbsoluteDate missionReferenceDate = new AbsoluteDate(2000, 1, 1, DataContext.getDefault().getTimeScales().getUTC());
        final OmmParser parser = new ParserBuilder().
                                 withMu(Constants.EIGEN5C_EARTH_MU).
                                 withMissionReferenceDate(missionReferenceDate).
                                 withDefaultMass(1000.0).
                                 buildOmmParser();

        final Omm file = parser.parseMessage(source);
        final KeplerianElements kep = file.getData().getKeplerianElementsBlock();
        Assert.assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(missionReferenceDate.shiftedBy(210840), file.getMetadata().getFrameEpoch());
        Assert.assertEquals(6800e3, kep.getA(), 1e-10);

        final SpacecraftParameters sp = file.getData().getSpacecraftParametersBlock();
        Assert.assertEquals(300, sp.getMass(), 1e-10);
        Assert.assertEquals(5, sp.getSolarRadArea(), 1e-10);
        Assert.assertEquals(0.001, sp.getSolarRadCoeff(), 1e-10);

        CartesianCovariance covariance = file.getData().getCovarianceBlock();
        Assert.assertEquals(null, covariance.getReferenceFrame().asFrame());
        Assert.assertEquals(null, covariance.getReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(LOFType.TNW, covariance.getReferenceFrame().asOrbitRelativeFrame().getLofType());

        UserDefined ud = file.getData().getUserDefinedBlock();
        HashMap<String, String> userDefinedParameters = new HashMap<String, String>();
        userDefinedParameters.put("EARTH_MODEL", "WGS-84");
        Assert.assertEquals(userDefinedParameters, ud.getParameters());
        Assert.assertEquals(Arrays.asList("this is a comment", "here is another one"),
                            file.getHeader().getComments());
        Assert.assertEquals(Collections.singletonList("this comment doesn't say much"),
                            file.getMetadata().getComments());
        Assert.assertEquals(Collections.singletonList("the following data is what we're looking for"),
                            file.getData().getKeplerianElementsBlock().getComments());
        Assert.assertEquals(Collections.singletonList("spacecraft data"),
                            file.getData().getSpacecraftParametersBlock().getComments());
        Assert.assertEquals(Collections.singletonList("Covariance matrix"),
                            file.getData().getCovarianceBlock().getComments());
        Assert.assertEquals(1995, file.getMetadata().getLaunchYear());
        Assert.assertEquals(25, file.getMetadata().getLaunchNumber());
        Assert.assertEquals("A", file.getMetadata().getLaunchPiece());
        file.generateSpacecraftState();
        file.generateKeplerianOrbit();

    }

    @Test
    public void testWrongKeyword() throws URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = "/ccsds/odm/omm/OMM-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OmmParser parser = new ParserBuilder().
                                 withMu(Constants.EIGEN5C_EARTH_MU).
                                 withMissionReferenceDate(new AbsoluteDate()).
                                 withDefaultMass(1000.0).
                                 buildOmmParser();
        try {
            parser.parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testOrbitFileInterface() {
        // simple test for OMM file, contains p/v entries and other mandatory data.
        final String name = "/ccsds/odm/omm/OMMExample1.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));

        // initialize parser
        final OmmParser parser = new ParserBuilder().
                        withMu(398600e9).
                        withMissionReferenceDate(new AbsoluteDate()).
                        withDefaultMass(1000.0).
                        buildOmmParser();

        final Omm file = parser.parseMessage(source);

        final String satId = "1995-025A";
        Assert.assertEquals(satId, file.getMetadata().getObjectID());

    }

    @Test
    public void testWrongODMType() {
        final String name = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withConventions(IERSConventions.IERS_1996).
            withMu(Constants.EIGEN5C_EARTH_MU).
            withMissionReferenceDate(new AbsoluteDate()).
            withDefaultMass(1000.0).
            buildOmmParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assert.assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    public void testNumberFormatErrorType() {
        final String name = "/ccsds/odm/omm/OMM-number-format-error.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withConventions(IERSConventions.IERS_1996).
            withMu(Constants.EIGEN5C_EARTH_MU).
            withMissionReferenceDate(new AbsoluteDate()).
            withDefaultMass(1000.0).
            buildOmmParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("ARG_OF_PERICENTER", oe.getParts()[0]);
            Assert.assertEquals(15, oe.getParts()[1]);
            Assert.assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = "/ccsds/odm/omm/OMMExample1.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new ParserBuilder().
            withConventions(IERSConventions.IERS_1996).
            withMu(Constants.EIGEN5C_EARTH_MU).
            withMissionReferenceDate(new AbsoluteDate()).
            withDefaultMass(1000.0).
            buildOmmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

}
