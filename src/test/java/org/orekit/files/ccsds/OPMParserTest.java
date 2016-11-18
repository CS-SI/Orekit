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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class OPMParserTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseOPM1()
        throws OrekitException {
        // simple test for OPM file, contains p/v entries and other mandatory
        // data.
        final String ex = "/ccsds/OPMExample.txt";

        final OPMParser parser = new OPMParser().withMu(398600e9).
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true);

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        final OPMFile file = parser.parse(inEntry, "OPMExample.txt");
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());

        // Check Header Block;
        Assert.assertEquals(2.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57,
                                             TimeScalesFactory.getUTC()), file
            .getCreationDate());
        Assert.assertEquals("JAXA", file.getOriginator());

        // Check Metadata Block;

        Assert.assertEquals("GODZILLA 5", file.getMetaData().getObjectName());
        Assert.assertEquals("1998-057A", file.getMetaData().getObjectID());
        Assert.assertEquals(1998, file.getMetaData().getLaunchYear());
        Assert.assertEquals(57, file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A", file.getMetaData().getLaunchPiece());
        Assert.assertEquals("EARTH", file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetaData().getCenterBody());
        Assert.assertEquals(CCSDSFrame.ITRF97.toString(), file.getMetaData().getFrame().getName());
        Assert.assertEquals(CcsdsTimeScale.TAI, file.getMetaData().getTimeSystem());
        Assert.assertFalse(file.hasCovarianceMatrix());

        // Check State Vector data Block;
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                             TimeScalesFactory.getTAI()), file.getEpoch());
        checkPVEntry(new PVCoordinates(new Vector3D(6503514.000, 1239647.000,
                                                    -717490.000),
                                       new Vector3D(-873.160, 8740.420,
                                                    -4191.076)),
                     file.getPVCoordinates());

        try {
            file.generateCartesianOrbit();
            Assert.fail("an exception should have been thrown");
        } catch(OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oiae.getSpecifier());
            Assert.assertEquals("ITRF97", oiae.getParts()[0]);
        }
        try {
            file.generateKeplerianOrbit();
            Assert.fail("an exception should have been thrown");
        } catch(OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oiae.getSpecifier());
            Assert.assertEquals("ITRF97", oiae.getParts()[0]);
        }
        try {
            file.generateSpacecraftState();
            Assert.fail("an exception should have been thrown");
        } catch(OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oiae.getSpecifier());
            Assert.assertEquals("ITRF97", oiae.getParts()[0]);
        }

    }

    @Test
    public void testParseOPM2()
        throws OrekitException {
        // simple test for OPM file, contains all mandatory information plus
        // Keplerian elements, Spacecraft parameters and 2 maneuvers.
        final String ex = "/ccsds/OPMExample2.txt";

        final OPMParser parser = new OPMParser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);

        final OPMFile file = parser.parse(inEntry, "OPMExample2.txt");
        try {
            file.getConventions();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS, oe.getSpecifier());
        }

        // Check Header Block;
        Assert.assertEquals(2.0, file.getFormatVersion(), 1.0e-10);
        ArrayList<String> headerComment = new ArrayList<String>();
        headerComment.add("Generated by GSOC, R. Kiehling");
        headerComment.add("Current intermediate orbit IO2 and maneuver planning data");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        Assert.assertEquals(new AbsoluteDate(2000, 06, 03, 05, 33, 00,
                                             TimeScalesFactory.getUTC()),
                            file.getCreationDate());
        Assert.assertEquals(file.getOriginator(), "GSOC");

        // Check Metadata Block;

        Assert.assertEquals("EUTELSAT W4", file.getMetaData().getObjectName());
        Assert.assertEquals("2000-028A", file.getMetaData().getObjectID());
        Assert.assertEquals("EARTH", file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetaData().getCenterBody());
        Assert.assertEquals(FramesFactory.getGCRF(), file.getMetaData().getFrame());
        Assert.assertEquals(CcsdsTimeScale.GPS, file.getMetaData().getTimeSystem());
        Assert.assertEquals(0, file.getMetaDataComment().size());

        // Check Data State Vector block
        ArrayList<String> epochComment = new ArrayList<String>();
        epochComment.add("State Vector");
        Assert.assertEquals(epochComment, file.getEpochComment());
        Assert.assertEquals(new AbsoluteDate(2006, 06, 03, 00, 00, 00,
                                             TimeScalesFactory.getGPS()),
                            file.getEpoch());
        checkPVEntry(new PVCoordinates(new Vector3D(6655994.2, -40218575.1, -82917.7),
                                       new Vector3D(3115.48208, 470.42605, -1.01495)),
                     file.getPVCoordinates());

        // Check Data Keplerian Elements block
        Assert.assertTrue(file.hasKeplerianElements());
        ArrayList<String> keplerianElementsComment = new ArrayList<String>();
        keplerianElementsComment.add("Keplerian elements");
        Assert.assertEquals(keplerianElementsComment, file.getKeplerianElementsComment());
        Assert.assertEquals(41399512.3, file.getA(), 1e-6);
        Assert.assertEquals(0.020842611, file.getE(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(0.117746), file.getI(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(17.604721), file.getRaan(), 1e-10);
        Assert.assertEquals(FastMath.toRadians(218.242943), file.getPa(), 1e-10);
        Assert.assertEquals(PositionAngle.TRUE, file.getAnomalyType());
        Assert.assertEquals(FastMath.toRadians(41.922339), file.getAnomaly(), 1e-10);
        Assert.assertEquals(398600.4415 * 1e9, file.getMuParsed(), 1e-10);

        // Check Data Spacecraft block
        ArrayList<String> spacecraftComment = new ArrayList<String>();
        spacecraftComment.add("Spacecraft parameters");
        Assert.assertEquals(spacecraftComment, file.getSpacecraftComment());
        Assert.assertEquals(1913.000, file.getMass(), 1e-10);
        Assert.assertEquals(10.000, file.getSolarRadArea(), 1e-10);
        Assert.assertEquals(1.300, file.getSolarRadCoeff(), 1e-10);
        Assert.assertEquals(10.000, file.getDragArea(), 1e-10);
        Assert.assertEquals(2.300, file.getDragCoeff(), 1e-10);

        // Check Data Maneuvers block
        Assert.assertTrue(file.getHasManeuver());
        Assert.assertEquals(3, file.getNbManeuvers());
        ArrayList<String> stateManeuverComment0 = new ArrayList<String>();
        stateManeuverComment0.add("2 planned maneuvers");
        stateManeuverComment0.add("First maneuver: AMF-3");
        stateManeuverComment0.add("Non-impulsive, thrust direction fixed in inertial frame");
        Assert.assertEquals(stateManeuverComment0, file.getManeuver(0).getComment());
        Assert.assertEquals(new AbsoluteDate(2000, 06, 03, 9, 00, 34.1,
                                             TimeScalesFactory.getGPS()),
                            file.getManeuvers().get(0).getEpochIgnition());
        Assert.assertEquals(132.6, file.getManeuver(0).getDuration(),1e-10);
        Assert.assertEquals(-18.418, file.getManeuver(0).getDeltaMass(), 1e-10);
        Assert.assertNull(file.getManeuver(0).getRefLofType());
        Assert.assertEquals(FramesFactory.getEME2000(), file.getManeuver(0).getRefFrame());
        Assert.assertEquals(0.0,
                            new Vector3D(-23.25700, 16.83160, -8.93444).distance(file.getManeuver(0).getDV()),
                            1.0e-10);

        ArrayList<String> stateManeuverComment1 = new ArrayList<String>();
        stateManeuverComment1.add("Second maneuver: first station acquisition maneuver");
        stateManeuverComment1.add("impulsive, thrust direction fixed in RTN frame");
        Assert.assertEquals(stateManeuverComment1, file.getManeuver(1).getComment());
        Assert.assertEquals(new AbsoluteDate(2000, 06, 05, 18, 59, 21,
                                             TimeScalesFactory.getGPS()),
                            file.getManeuvers().get(1).getEpochIgnition());
        Assert.assertEquals(0.0, file.getManeuver(1).getDuration(), 1e-10);
        Assert.assertEquals(-1.469, file.getManeuver(1).getDeltaMass(), 1e-10);
        Assert.assertEquals(LOFType.QSW, file.getManeuver(1).getRefLofType());
        Assert.assertNull(file.getManeuver(1).getRefFrame());
        Assert.assertEquals(0.0,
                            new Vector3D(1.015, -1.873, 0.0).distance(file.getManeuver(1).getDV()),
                            1.0e-10);

        Assert.assertTrue(file.getManeuver(2).getComment().isEmpty());
        Assert.assertEquals(new AbsoluteDate(2000, 06, 05, 18, 59, 51,
                                             TimeScalesFactory.getGPS()),
                            file.getManeuvers().get(2).getEpochIgnition());
        Assert.assertEquals(0.0, file.getManeuver(2).getDuration(), 1e-10);
        Assert.assertEquals(-1.469, file.getManeuver(2).getDeltaMass(), 1e-10);
        Assert.assertEquals(LOFType.QSW, file.getManeuver(2).getRefLofType());
        Assert.assertNull(file.getManeuver(2).getRefFrame());
        Assert.assertEquals(0.0,
                            new Vector3D(1.015, -1.873, 0.0).distance(file.getManeuver(2).getDV()),
                            1.0e-10);

        file.generateCartesianOrbit();
        file.generateKeplerianOrbit();
        file.generateSpacecraftState();

    }

    @Test
    public void testMissingIERSInitialization()
            throws OrekitException, URISyntaxException {
        final String name = getClass().getResource("/ccsds/OPMExample3.txt").toURI().getPath();
        OPMParser parser = new OPMParser();
        try {
            // we explicitly forget to call parser.setConventions here
            parser.parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS, oe.getSpecifier());
        }
    }

    @Test
    public void testMissingMu()
            throws OrekitException, URISyntaxException {
        final String name = getClass().getResource("/ccsds/OPM-unknown-mu.txt").toURI().getPath();
        OPMFile opm = new OPMParser().withConventions(IERSConventions.IERS_2010).parse(name);
        try {
            opm.generateCartesianOrbit();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_GM, oe.getSpecifier());
        }
    }

    @Test
    public void testParseOPM3()
            throws OrekitException, URISyntaxException {
        // simple test for OPM file, contains all mandatory information plus
        // Spacecraft parameters and the position/velocity Covariance Matrix.
        final String name = getClass().getResource("/ccsds/OPMExample3.txt").toURI().getPath();
        OPMParser parser = new OPMParser().withConventions(IERSConventions.IERS_2010);
        final OPMFile file = parser.parse(name);
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                             TimeScalesFactory.getGMST(IERSConventions.IERS_2010, false)),
                            file.getMetaData().getFrameEpoch());
        Assert.assertEquals(2, file.getMetaDataComment().size());
        Assert.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", file.getMetaDataComment().get(0));
        Assert.assertEquals("titi", file.getMetaDataComment().get(1));
        Assert.assertEquals(15951238.3495, file.generateKeplerianOrbit().getA(), 0.001);
        Assert.assertEquals(0.5914452565, file.generateKeplerianOrbit().getE(), 1.0e-10);
        // Check Data Covariance matrix Block
        ArrayList<String> dataCovMatrixComment = new ArrayList<String>();
        dataCovMatrixComment.add("toto");
        dataCovMatrixComment.add("tata");
        Assert.assertEquals(dataCovMatrixComment, file.getCovarianceComment());
        Assert.assertTrue(file.hasCovarianceMatrix());
        Assert.assertEquals(file.getCovRefFrame(), FramesFactory.getTEME());

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
                Assert.assertEquals(covMatrix.getEntry(i, j), file
                    .getCovarianceMatrix().getEntry(i, j), 1e-15);
            }
        }

        // Check User defined Parameters Block
        HashMap<String, String> userDefinedParameters = new HashMap<String, String>();
        userDefinedParameters.put("USER_DEFINED_EARTH_MODEL", "WGS-84");
        userDefinedParameters.put("USER_DEFINED_TOTO", "TITI");
        Assert.assertEquals(userDefinedParameters,
                            file.getUserDefinedParameters());

    }

    @Test
    public void testParseOPM3NoDesignator()
            throws OrekitException, URISyntaxException {
        final String name = getClass().getResource("/ccsds/OPM-no-designator.txt").toURI().getPath();
        OPMParser parser =
                new OPMParser().withConventions(IERSConventions.IERS_2010).withInternationalDesignator(2060, 666, "XYZ");
        final OPMFile file = parser.parse(name);
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                                 TimeScalesFactory.getGMST(IERSConventions.IERS_2010, false)),
                            file.getMetaData().getFrameEpoch());
        Assert.assertEquals(2060, file.getMetaData().getLaunchYear());
        Assert.assertEquals(666, file.getMetaData().getLaunchNumber());
        Assert.assertEquals("XYZ", file.getMetaData().getLaunchPiece());
    }

    @Test
    public void testParseOPM4()
        throws OrekitException {
        //
        final String ex = "/ccsds/OPMExample4.txt";
        OPMParser parser =
                new OPMParser().
                withMissionReferenceDate(new AbsoluteDate()).
                withConventions(IERSConventions.IERS_2010);
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OPMFile file = parser.parse(inEntry, "OPMExample4.txt");
        file.getMetaData().getFrame().toString();
        file.getMetaData().getObjectID();
        file.getEpoch();
        file.getPVCoordinates();
        file.getMetaData().getFrame();
    }

    @Test
    public void testCentersAndTimeScales() throws OrekitException {

        final OPMParser parser = new OPMParser().withMissionReferenceDate(new AbsoluteDate())
                                                .withConventions(IERSConventions.IERS_2010);

        OPMFile file =
                parser.parse(getClass().getResourceAsStream("/ccsds/OPM-dummy-solar-system-barycenter.txt"));
        Assert.assertEquals(CcsdsTimeScale.TDB, file.getMetaData().getTimeSystem());
        Assert.assertEquals("solar system barycenter", file.getMetaData().getCenterBody().getName());

        file = parser.parse(getClass().getResourceAsStream("/ccsds/OPM-dummy-ssb.txt"));
        Assert.assertEquals(CcsdsTimeScale.TCB, file.getMetaData().getTimeSystem());
        Assert.assertEquals("solar system barycenter", file.getMetaData().getCenterBody().getName());

        file = parser.parse(getClass().getResourceAsStream("/ccsds/OPM-dummy-earth-barycenter.txt"));
        Assert.assertEquals(CcsdsTimeScale.TDB, file.getMetaData().getTimeSystem());
        Assert.assertEquals("Earth-Moon barycenter", file.getMetaData().getCenterBody().getName());

        file = parser.parse(getClass().getResourceAsStream("/ccsds/OPM-dummy-earth-dash-moon-barycenter.txt"));
        Assert.assertEquals(CcsdsTimeScale.TDB, file.getMetaData().getTimeSystem());
        Assert.assertEquals("Earth-Moon barycenter", file.getMetaData().getCenterBody().getName());

        file = parser.parse(getClass().getResourceAsStream("/ccsds/OPM-dummy-earth-moon-barycenter.txt"));
        Assert.assertEquals(CcsdsTimeScale.UT1, file.getMetaData().getTimeSystem());
        Assert.assertEquals("Earth-Moon barycenter", file.getMetaData().getCenterBody().getName());

        file = parser.parse(getClass().getResourceAsStream("/ccsds/OPM-dummy-emb.txt"));
        Assert.assertEquals(CcsdsTimeScale.TT, file.getMetaData().getTimeSystem());
        Assert.assertEquals("Earth-Moon barycenter", file.getMetaData().getCenterBody().getName());

    }

    @Test
    public void testOrbitFileInterface() throws OrekitException {
        final String ex = "/ccsds/OPMExample4.txt";

        final OPMParser parser = new OPMParser().withMissionReferenceDate(new AbsoluteDate())
                                                .withConventions(IERSConventions.IERS_2010);

        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OPMFile file = parser.parse(inEntry, "OPMExample4.txt");

        final String satId = "1998-057A";
        Assert.assertEquals(satId, file.getMetaData().getObjectID());

        checkPVEntry(file.getPVCoordinates(), file.getPVCoordinates());
        Assert.assertEquals(file.getEpoch(), file.getEpoch());

    }

    private void checkPVEntry(final PVCoordinates expected,
                              final PVCoordinates actual) {
        final Vector3D expectedPos = expected.getPosition();
        final Vector3D expectedVel = expected.getVelocity();

        final Vector3D actualPos = actual.getPosition();
        final Vector3D actualVel = actual.getVelocity();

        final double eps = 1e-12;

        Assert.assertEquals(expectedPos.getX(), actualPos.getX(), eps);
        Assert.assertEquals(expectedPos.getY(), actualPos.getY(), eps);
        Assert.assertEquals(expectedPos.getZ(), actualPos.getZ(), eps);

        Assert.assertEquals(expectedVel.getX(), actualVel.getX(), eps);
        Assert.assertEquals(expectedVel.getY(), actualVel.getY(), eps);
        Assert.assertEquals(expectedVel.getZ(), actualVel.getZ(), eps);
    }

    @Test
    public void testWrongODMType() {
        try {
            new OPMParser().parse(getClass().getResourceAsStream("/ccsds/OMMExample.txt"), "OMMExample.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("OMMExample.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_OMM_VERS = 2.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testNumberFormatErrorType() {
        try {
            OPMParser parser = new OPMParser().withConventions(IERSConventions.IERS_2010);
            parser.parse(getClass().getResourceAsStream("/ccsds/OPM-number-format-error.txt"),
                         "OPM-number-format-error.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(17, oe.getParts()[0]);
            Assert.assertEquals("OPM-number-format-error.txt", oe.getParts()[1]);
            Assert.assertEquals("SEMI_MAJOR_AXIS = this-is-not-a-number [km]", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/OPMExample.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new OPMParser().parse(wrongName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongKeyword()
        throws OrekitException, URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = getClass().getResource("/ccsds/OPM-wrong-keyword.txt").toURI().getPath();
        try {
            new OPMParser().withConventions(IERSConventions.IERS_2010).parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(11, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

}
