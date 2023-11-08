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
package org.orekit.files.ccsds.ndm.odm.opm;

import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParameters;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class OpmParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseOPM1KVN() {
        // simple test for OPM file, contains p/v entries and other mandatory
        // data.
        final String ex = "/ccsds/odm/opm/OPMExample1.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        final OpmParser parser = new ParserBuilder().withMu(398600e9).withDefaultMass(1000.0).buildOpmParser();

        final Opm file = parser.parseMessage(source);
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());

        // Check Header Block;
        Assertions.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("JAXA", file.getHeader().getOriginator());

        // Check Metadata Block;

        Assertions.assertEquals("GODZILLA 5", file.getMetadata().getObjectName());
        Assertions.assertEquals("1998-999A", file.getMetadata().getObjectID());
        Assertions.assertEquals(1998, file.getMetadata().getLaunchYear());
        Assertions.assertEquals(999, file.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A", file.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH", file.getMetadata().getCenter().getName());
        Assertions.assertNotNull(file.getMetadata().getCenter().getBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), file.getMetadata().getCenter().getBody());
        Assertions.assertEquals(CelestialBodyFrame.ITRF2000, CelestialBodyFrame.map(file.getMetadata().getFrame()));
        Assertions.assertEquals("UTC", file.getMetadata().getTimeSystem().name());
        Assertions.assertNull(file.getData().getCovarianceBlock());

        // Check State Vector data Block;
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getDate());
        checkPVEntry(new PVCoordinates(new Vector3D(6503514.000, 1239647.000, -717490.000),
                                       new Vector3D(-873.160, 8740.420, -4191.076)),
                     file.getPVCoordinates());

        try {
            file.generateCartesianOrbit();
            Assertions.fail("an exception should have been thrown");
        } catch(OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oiae.getSpecifier());
            Assertions.assertEquals("ITRF-2000/CIO/2010-based ITRF simple EOP", oiae.getParts()[0]);
        }
        try {
            file.generateKeplerianOrbit();
            Assertions.fail("an exception should have been thrown");
        } catch(OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oiae.getSpecifier());
            Assertions.assertEquals("ITRF-2000/CIO/2010-based ITRF simple EOP", oiae.getParts()[0]);
        }
        try {
            file.generateSpacecraftState();
            Assertions.fail("an exception should have been thrown");
        } catch(OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oiae.getSpecifier());
            Assertions.assertEquals("ITRF-2000/CIO/2010-based ITRF simple EOP", oiae.getParts()[0]);
        }

    }

    @Test
    public void testParseOPM2() {
        // simple test for OPM file, contains all mandatory information plus
        // Keplerian elements, Spacecraft parameters and 2 maneuvers.
        final String ex = "/ccsds/odm/opm/OPMExample2.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        final OpmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).withDefaultMass(1000.0).buildOpmParser();
        final Opm file = parser.parseMessage(source);
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());

        // Check Header Block;
        Assertions.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        ArrayList<String> headerComment = new ArrayList<String>();
        headerComment.add("Generated by GSOC, R. Kiehling");
        headerComment.add("Current intermediate orbit IO2 and maneuver planning data");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 03, 05, 33, 00,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals(file.getHeader().getOriginator(), "GSOC");

        // Check Metadata Block;

        Assertions.assertEquals("EUTELSAT W4", file.getMetadata().getObjectName());
        Assertions.assertEquals("2000-028A", file.getMetadata().getObjectID());
        Assertions.assertEquals("EARTH", file.getMetadata().getCenter().getName());
        Assertions.assertNotNull(file.getMetadata().getCenter().getBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), file.getMetadata().getCenter().getBody());
        Assertions.assertEquals(FramesFactory.getTOD(IERSConventions.IERS_2010, true),
                            file.getMetadata().getFrame());
        Assertions.assertEquals("UTC", file.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(0, file.getMetadata().getComments().size());

        // Check Data State Vector block
        ArrayList<String> epochComment = new ArrayList<String>();
        epochComment.add("State Vector");
        Assertions.assertEquals(epochComment, file.getData().getStateVectorBlock().getComments());
        Assertions.assertEquals(new AbsoluteDate(2006, 06, 03, 00, 00, 00,
                                             TimeScalesFactory.getUTC()),
                            file.getDate());
        checkPVEntry(new PVCoordinates(new Vector3D(6655994.2, -40218575.1, -82917.7),
                                       new Vector3D(3115.48208, 470.42605, -1.01495)),
                     file.getPVCoordinates());

        // Check Data Keplerian Elements block
        KeplerianElements keplerianElements = file.getData().getKeplerianElementsBlock();
        Assertions.assertNotNull(keplerianElements);
        ArrayList<String> keplerianElementsComment = new ArrayList<String>();
        keplerianElementsComment.add("Keplerian elements");
        Assertions.assertEquals(keplerianElementsComment, keplerianElements.getComments());
        Assertions.assertEquals(41399512.3, keplerianElements.getA(), 1e-6);
        Assertions.assertEquals(0.020842611, keplerianElements.getE(), 1e-10);
        Assertions.assertEquals(FastMath.toRadians(0.117746), keplerianElements.getI(), 1e-10);
        Assertions.assertEquals(FastMath.toRadians(17.604721), keplerianElements.getRaan(), 1e-10);
        Assertions.assertEquals(FastMath.toRadians(218.242943), keplerianElements.getPa(), 1e-10);
        Assertions.assertEquals(PositionAngleType.TRUE, keplerianElements.getAnomalyType());
        Assertions.assertEquals(FastMath.toRadians(41.922339), keplerianElements.getAnomaly(), 1e-10);
        Assertions.assertEquals(398600.4415 * 1e9, keplerianElements.getMu(), 1e-10);

        // Check Data Spacecraft block
        SpacecraftParameters spacecraftParameters = file.getData().getSpacecraftParametersBlock();
        Assertions.assertNotNull(spacecraftParameters);
        ArrayList<String> spacecraftComment = new ArrayList<String>();
        spacecraftComment.add("Spacecraft parameters");
        Assertions.assertEquals(spacecraftComment, spacecraftParameters.getComments());
        Assertions.assertEquals(1913.000, spacecraftParameters.getMass(), 1e-10);
        Assertions.assertEquals(10.000, spacecraftParameters.getSolarRadArea(), 1e-10);
        Assertions.assertEquals(1.300, spacecraftParameters.getSolarRadCoeff(), 1e-10);
        Assertions.assertEquals(10.000, spacecraftParameters.getDragArea(), 1e-10);
        Assertions.assertEquals(2.300, spacecraftParameters.getDragCoeff(), 1e-10);

        // Check covariance block
        Assertions.assertNull(file.getData().getCovarianceBlock());

        // Check Data Maneuvers block
        Assertions.assertTrue(file.getData().hasManeuvers());
        Assertions.assertEquals(2, file.getNbManeuvers());
        ArrayList<String> stateManeuverComment0 = new ArrayList<String>();
        stateManeuverComment0.add("2 planned maneuvers");
        stateManeuverComment0.add("First maneuver: AMF-3");
        stateManeuverComment0.add("Non-impulsive, thrust direction fixed in inertial frame");
        Assertions.assertEquals(stateManeuverComment0, file.getManeuver(0).getComments());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 03, 9, 00, 34.1,
                                             TimeScalesFactory.getUTC()),
                            file.getManeuvers().get(0).getEpochIgnition());
        Assertions.assertEquals(132.6, file.getManeuver(0).getDuration(), 1e-10);
        Assertions.assertEquals(-18.418, file.getManeuver(0).getDeltaMass(), 1e-10);
        Assertions.assertNull(file.getManeuver(0).getReferenceFrame().asOrbitRelativeFrame());
        Assertions.assertEquals(FramesFactory.getEME2000(), file.getManeuver(0).getReferenceFrame().asFrame());
        Assertions.assertEquals(0.0,
                            new Vector3D(-23.25700, 16.83160, -8.93444).distance(file.getManeuver(0).getDV()),
                            1.0e-10);

        ArrayList<String> stateManeuverComment1 = new ArrayList<String>();
        stateManeuverComment1.add("Second maneuver: first station acquisition maneuver");
        stateManeuverComment1.add("impulsive, thrust direction fixed in RTN frame");
        Assertions.assertEquals(stateManeuverComment1, file.getManeuver(1).getComments());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 05, 18, 59, 21,
                                             TimeScalesFactory.getUTC()),
                            file.getManeuvers().get(1).getEpochIgnition());
        Assertions.assertEquals(0.0, file.getManeuver(1).getDuration(), 1e-10);
        Assertions.assertEquals(-1.469, file.getManeuver(1).getDeltaMass(), 1e-10);
        Assertions.assertEquals(LOFType.QSW, file.getManeuver(1).getReferenceFrame().asOrbitRelativeFrame().getLofType());
        Assertions.assertNull(file.getManeuver(1).getReferenceFrame().asFrame());
        Assertions.assertNull(file.getManeuver(1).getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(0.0,
                            new Vector3D(1.015, -1.873, 0.0).distance(file.getManeuver(1).getDV()),
                            1.0e-10);

        Assertions.assertNull(file.getData().getUserDefinedBlock());
        Assertions.assertNotNull(file.generateCartesianOrbit());
        Assertions.assertNotNull(file.generateKeplerianOrbit());
        Assertions.assertNotNull(file.generateSpacecraftState());

    }

    @Test
    public void testParseOPM5() {
        // simple test for OPM file, contains all mandatory information plus
        // Keplerian elements, Spacecraft parameters and 3 maneuvers.
        final String ex = "/ccsds/odm/opm/OPMExample5.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        final OpmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).withDefaultMass(1000.0).buildOpmParser();
        final Opm file = parser.parseMessage(source);

        // Check Header Block;
        Assertions.assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        ArrayList<String> headerComment = new ArrayList<String>();
        headerComment.add("Generated by GSOC, R. Kiehling");
        headerComment.add("Current intermediate orbit IO2 and maneuver planning data");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 03, 05, 33, 00,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals(file.getHeader().getOriginator(), "GSOC");

        // Check Metadata Block;
        Assertions.assertEquals("EUTELSAT W4", file.getMetadata().getObjectName());
        Assertions.assertEquals("2000-028A", file.getMetadata().getObjectID());
        Assertions.assertEquals("EARTH", file.getMetadata().getCenter().getName());
        Assertions.assertNotNull(file.getMetadata().getCenter().getBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), file.getMetadata().getCenter().getBody());
        Assertions.assertEquals(FramesFactory.getGCRF(), file.getMetadata().getFrame());
        Assertions.assertEquals("GPS", file.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(0, file.getMetadata().getComments().size());

        // Check Data State Vector block
        ArrayList<String> stateVectorComment = new ArrayList<String>();
        stateVectorComment.add("State Vector");
        Assertions.assertEquals(stateVectorComment, file.getData().getStateVectorBlock().getComments());
        Assertions.assertEquals(new AbsoluteDate(2006, 06, 03, 00, 00, 00,
                                             TimeScalesFactory.getGPS()),
                            file.getData().getStateVectorBlock().getEpoch());
        checkPVEntry(new PVCoordinates(new Vector3D(6655994.2, -40218575.1, -82917.7),
                                       new Vector3D(3115.48208, 470.42605, -1.01495)),
                     file.getPVCoordinates());

        // Check Data Keplerian Elements block
        KeplerianElements keplerianElements = file.getData().getKeplerianElementsBlock();
        Assertions.assertNotNull(keplerianElements);
        ArrayList<String> keplerianElementsComment = new ArrayList<String>();
        keplerianElementsComment.add("Keplerian elements");
        Assertions.assertEquals(keplerianElementsComment, keplerianElements.getComments());
        Assertions.assertEquals(41399512.3, keplerianElements.getA(), 1e-6);
        Assertions.assertEquals(0.020842611, keplerianElements.getE(), 1e-10);
        Assertions.assertEquals(FastMath.toRadians(0.117746), keplerianElements.getI(), 1e-10);
        Assertions.assertEquals(FastMath.toRadians(17.604721), keplerianElements.getRaan(), 1e-10);
        Assertions.assertEquals(FastMath.toRadians(218.242943), keplerianElements.getPa(), 1e-10);
        Assertions.assertEquals(PositionAngleType.TRUE, keplerianElements.getAnomalyType());
        Assertions.assertEquals(FastMath.toRadians(41.922339), keplerianElements.getAnomaly(), 1e-10);
        Assertions.assertEquals(398600.4415 * 1e9, keplerianElements.getMu(), 1e-10);

        // Check Data Spacecraft block
        SpacecraftParameters spacecraftParameters = file.getData().getSpacecraftParametersBlock();
        ArrayList<String> spacecraftComment = new ArrayList<String>();
        spacecraftComment.add("Spacecraft parameters");
        Assertions.assertEquals(spacecraftComment, spacecraftParameters.getComments());
        Assertions.assertEquals(1913.000, spacecraftParameters.getMass(), 1e-10);
        Assertions.assertEquals(10.000, spacecraftParameters.getSolarRadArea(), 1e-10);
        Assertions.assertEquals(1.300, spacecraftParameters.getSolarRadCoeff(), 1e-10);
        Assertions.assertEquals(10.000, spacecraftParameters.getDragArea(), 1e-10);
        Assertions.assertEquals(2.300, spacecraftParameters.getDragCoeff(), 1e-10);

        // Check covariance block
        Assertions.assertNull(file.getData().getCovarianceBlock());

        // Check Data Maneuvers block
        Assertions.assertTrue(file.getData().hasManeuvers());
        Assertions.assertEquals(3, file.getNbManeuvers());
        ArrayList<String> stateManeuverComment0 = new ArrayList<String>();
        stateManeuverComment0.add("2 planned maneuvers");
        stateManeuverComment0.add("First maneuver: AMF-3");
        stateManeuverComment0.add("Non-impulsive, thrust direction fixed in inertial frame");
        Assertions.assertEquals(stateManeuverComment0, file.getManeuver(0).getComments());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 03, 9, 00, 34.1,
                                             TimeScalesFactory.getGPS()),
                            file.getManeuvers().get(0).getEpochIgnition());
        Assertions.assertEquals(132.6, file.getManeuver(0).getDuration(), 1e-10);
        Assertions.assertEquals(-18.418, file.getManeuver(0).getDeltaMass(), 1e-10);
        Assertions.assertNull(file.getManeuver(0).getReferenceFrame().asOrbitRelativeFrame());
        Assertions.assertEquals(FramesFactory.getEME2000(), file.getManeuver(0).getReferenceFrame().asFrame());
        Assertions.assertEquals(0.0,
                            new Vector3D(-23.25700, 16.83160, -8.93444).distance(file.getManeuver(0).getDV()),
                            1.0e-10);

        ArrayList<String> stateManeuverComment1 = new ArrayList<String>();
        stateManeuverComment1.add("Second maneuver: first station acquisition maneuver");
        stateManeuverComment1.add("impulsive, thrust direction fixed in RTN frame");
        Assertions.assertEquals(stateManeuverComment1, file.getManeuver(1).getComments());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 05, 18, 59, 21,
                                             TimeScalesFactory.getGPS()),
                            file.getManeuvers().get(1).getEpochIgnition());
        Assertions.assertEquals(0.0, file.getManeuver(1).getDuration(), 1e-10);
        Assertions.assertEquals(-1.469, file.getManeuver(1).getDeltaMass(), 1e-10);
        Assertions.assertEquals(LOFType.QSW, file.getManeuver(1).getReferenceFrame().asOrbitRelativeFrame().getLofType());
        Assertions.assertNull(file.getManeuver(1).getReferenceFrame().asFrame());
        Assertions.assertNull(file.getManeuver(1).getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(0.0,
                            new Vector3D(1.015, -1.873, 0.0).distance(file.getManeuver(1).getDV()),
                            1.0e-10);

        Assertions.assertTrue(file.getManeuver(2).getComments().isEmpty());
        Assertions.assertEquals(new AbsoluteDate(2000, 06, 05, 18, 59, 51,
                                             TimeScalesFactory.getGPS()),
                            file.getManeuvers().get(2).getEpochIgnition());
        Assertions.assertEquals(0.0, file.getManeuver(2).getDuration(), 1e-10);
        Assertions.assertEquals(-1.469, file.getManeuver(2).getDeltaMass(), 1e-10);
        Assertions.assertEquals(LOFType.QSW, file.getManeuver(2).getReferenceFrame().asOrbitRelativeFrame().getLofType());
        Assertions.assertNull(file.getManeuver(2).getReferenceFrame().asFrame());
        Assertions.assertNull(file.getManeuver(2).getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(0.0,
                            new Vector3D(1.015, -1.873, 0.0).distance(file.getManeuver(2).getDV()),
                            1.0e-10);

        file.generateCartesianOrbit();
        file.generateKeplerianOrbit();
        file.generateSpacecraftState();

    }

    @Test
    public void testParseOPM3KVN() throws URISyntaxException {
        // simple test for OPM file, contains all mandatory information plus
        // Spacecraft parameters and the position/velocity Covariance Matrix.
        final String name = "/ccsds/odm/opm/OPMExample3.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OpmParser parser = new ParserBuilder().withDefaultMass(1000.0).buildOpmParser();
        final Opm file = parser.parseMessage(source);
        Assertions.assertEquals("OPM 201113719185", file.getHeader().getMessageId());
        Assertions.assertEquals(CelestialBodyFrame.TOD, file.getMetadata().getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getMetadata().getFrameEpoch());
        Assertions.assertEquals(1, file.getMetadata().getComments().size());
        Assertions.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", file.getMetadata().getComments().get(0));
        Assertions.assertEquals(15951238.3495, file.generateKeplerianOrbit().getA(), 0.001);
        Assertions.assertEquals(0.5914452565, file.generateKeplerianOrbit().getE(), 1.0e-10);
        // Check Data Covariance matrix Block
        CartesianCovariance covariance = file.getData().getCovarianceBlock();
        Assertions.assertNotNull(covariance);
        Assertions.assertSame(file.getMetadata().getReferenceFrame(), covariance.getReferenceFrame());

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
                Assertions.assertEquals(covMatrix.getEntry(i, j),
                                    covariance.getCovarianceMatrix().getEntry(i, j),
                                    1e-15);
            }
        }

    }

    @Test
    public void testParseOPM3XML() throws URISyntaxException {
        // simple test for OPM file, contains all mandatory information plus
        // Spacecraft parameters and the position/velocity Covariance Matrix.
        // the content of the file is slightly different from the KVN file in the covariance section
        final String name = "/ccsds/odm/opm/OPMExample3.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OpmParser parser = new ParserBuilder().withDefaultMass(1000.0).buildOpmParser();
        validateOPM3XML(parser.parseMessage(source));
    }

    @Test
    public void testWriteOPM3() throws URISyntaxException, IOException {
        // simple test for OPM file, contains all mandatory information plus
        // Spacecraft parameters and the position/velocity Covariance Matrix.
        // the content of the file is slightly different from the KVN file in the covariance section
        final String name = "/ccsds/odm/opm/OPMExample3.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OpmParser parser = new ParserBuilder().withDefaultMass(1000.0).buildOpmParser();
        final Opm original = parser.parseMessage(source);

        // write the parsed file back to a characters array
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, OpmWriter.KVN_PADDING_WIDTH, "dummy",
                                                     Constants.JULIAN_DAY, 60);
        new WriterBuilder().buildOpmWriter().writeMessage(generator, original);

        // reparse the written file
        final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        final Opm    rebuilt = new ParserBuilder().buildOpmParser().parseMessage(source2);
        validateOPM3XML(rebuilt);

    }

    private void validateOPM3XML(final Opm file) {
        Assertions.assertEquals("OPM 201113719185", file.getHeader().getMessageId());
        Assertions.assertEquals(CelestialBodyFrame.TOD, file.getMetadata().getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getMetadata().getFrameEpoch());
        Assertions.assertEquals(1, file.getMetadata().getComments().size());
        Assertions.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", file.getMetadata().getComments().get(0));
        Assertions.assertEquals(15951238.3495, file.generateKeplerianOrbit().getA(), 0.001);
        Assertions.assertEquals(0.5914452565, file.generateKeplerianOrbit().getE(), 1.0e-10);
        // Check Data Covariance matrix Block
        CartesianCovariance covariance = file.getData().getCovarianceBlock();
        Assertions.assertNotNull(covariance);
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, covariance.getReferenceFrame().asCelestialBodyFrame());

        Array2DRowRealMatrix covMatrix = new Array2DRowRealMatrix(6, 6);
        double[] column1 = {
            316000.0, 722000.0, 202000.0, 912000.0, 562000.0, 245000.0
        };
        double[] column2 = {
            722000.0, 518000.0, 715000.0, 306000.0, 899000.0, 965000.0
        };
        double[] column3 = {
            202000.0, 715000.0, 002000.0, 276000.0, 022000.0, 950000.0
        };
        double[] column4 = {
            912000.0, 306000.0, 276000.0, 797000.0, 079000.0, 435000.0
        };
        double[] column5 = {
            562000.0, 899000.0, 022000.0, 079000.0, 415000.0, 621000.0
        };
        double[] column6 = {
            245000.0, 965000.0, 950000.0, 435000.0, 621000.0, 991000.0
        };
        covMatrix.setColumn(0, column1);
        covMatrix.setColumn(1, column2);
        covMatrix.setColumn(2, column3);
        covMatrix.setColumn(3, column4);
        covMatrix.setColumn(4, column5);
        covMatrix.setColumn(5, column6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assertions.assertEquals(covMatrix.getEntry(i, j),
                                    covariance.getCovarianceMatrix().getEntry(i, j),
                                    1e-15);
            }
        }

    }

    @Test
    public void testParseOPM3NoDesignator() throws URISyntaxException {
        final String ex = "/ccsds/odm/opm/OPM-no-designator.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        OpmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).withDefaultMass(1000.0).buildOpmParser();
        final Opm file = parser.parseMessage(source);
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                                 TimeScalesFactory.getGMST(IERSConventions.IERS_2010, false)),
                            file.getMetadata().getFrameEpoch());
        try {
            file.getMetadata().getLaunchYear();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_VALID_INTERNATIONAL_DESIGNATOR, oe.getSpecifier());
            Assertions.assertEquals("REDACTED FOR TEST PURPOSES", (String) oe.getParts()[0]);
        }
        try {
            file.getMetadata().getLaunchNumber();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_VALID_INTERNATIONAL_DESIGNATOR, oe.getSpecifier());
            Assertions.assertEquals("REDACTED FOR TEST PURPOSES", (String) oe.getParts()[0]);
        }
        try {
            file.getMetadata().getLaunchPiece();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_VALID_INTERNATIONAL_DESIGNATOR, oe.getSpecifier());
            Assertions.assertEquals("REDACTED FOR TEST PURPOSES", (String) oe.getParts()[0]);
        }
    }

    @Test
    public void testParseOPM4() {
        //
        final String ex = "/ccsds/odm/opm/OPMExample4.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Opm file = new ParserBuilder().
                             withMu(Constants.EIGEN5C_EARTH_MU).
                             withSimpleEOP(false).
                             withDefaultMass(1000.0).
                             buildOpmParser().
                             parseMessage(source);
        Assertions.assertEquals("TOD/2010 accurate EOP", file.getMetadata().getFrame().toString());
        Assertions.assertEquals("2000-028A", file.getMetadata().getObjectID());
        Assertions.assertEquals(new AbsoluteDate(2006, 6, 3, TimeScalesFactory.getUTC()), file.getDate());
        final TimeStampedPVCoordinates pva = file.getPVCoordinates();
        Assertions.assertEquals(  6655994.2, pva.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals(-40218575.1, pva.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals(   -82917.7, pva.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals( 3115.48208, pva.getVelocity().getX(), 1.0e-10);
        Assertions.assertEquals( 0470.42605, pva.getVelocity().getY(), 1.0e-10);
        Assertions.assertEquals(-0001.01495, pva.getVelocity().getZ(), 1.0e-10);
    }

    @Test
    public void testParseOPM6() throws URISyntaxException {
        // simple test for OPM file, contains all mandatory information plus
        // Spacecraft parameters and the position/velocity Covariance Matrix.
        final String name = "/ccsds/odm/opm/OPMExample6.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OpmParser parser = new ParserBuilder().buildOpmParser();
        validate6(parser.parseMessage(source));
    }

    @Test
    public void testParseOPM7() {
        final String ex = "/ccsds/odm/opm/OPMExample7.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Opm file = new ParserBuilder().buildOpmParser().parseMessage(source);
        Frame frame = file.getMetadata().getFrame();
        Assertions.assertSame(CelestialBodyFactory.getMars().getInertiallyOrientedFrame(), frame);
    }

    @Test
    public void testParseOPM8() {
        final String ex = "/ccsds/odm/opm/OPMExample8.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Opm file = new ParserBuilder().buildOpmParser().parseMessage(source);
        Frame frame = file.getMetadata().getFrame();
        Assertions.assertSame(CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame(), frame);
    }

    @Test
    public void testParseNonStandardUnits() throws URISyntaxException {
        // this file is similar to OPMExample6.txt but uses non-standard units
        // it is therefore NOT a regular CCSDS OPM, but is correctly parsed by Orekit
        final String name = "/ccsds/odm/opm/OPM-non-standard-units.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OpmParser parser = new ParserBuilder().
                           withParsedUnitsBehavior(ParsedUnitsBehavior.CONVERT_COMPATIBLE).
                           buildOpmParser();
        validate6(parser.parseMessage(source));
    }

    @Test
    public void testRefuseNonStandardUnits() throws URISyntaxException {
        // this file is similar to OPMExample6.txt but uses non-standard units
        // it is therefore NOT a regular CCSDS OPM, but is correctly parsed by Orekit
        final String name = "/ccsds/odm/opm/OPM-non-standard-units.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assertions.assertEquals("m",  oe.getParts()[0]);
            Assertions.assertEquals("km", oe.getParts()[1]);
        }
    }

    private void validate6(final Opm file) {
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172,
                                             TimeScalesFactory.getGMST(IERSConventions.IERS_2010, false)),
                            file.getMetadata().getFrameEpoch());
        Assertions.assertEquals(1, file.getMetadata().getComments().size());
        Assertions.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", file.getMetadata().getComments().get(0));
        Assertions.assertEquals("OREKIT-4D00FC96-AC64-11E9-BF71-001FD054093C", file.getHeader().getMessageId());

        Assertions.assertEquals(15951238.3495, file.generateKeplerianOrbit().getA(), 0.001);
        Assertions.assertEquals(0.5914452565, file.generateKeplerianOrbit().getE(), 1.0e-10);
        // Check Data Covariance matrix Block
        CartesianCovariance covariance = file.getData().getCovarianceBlock();
        Assertions.assertNotNull(covariance);
        ArrayList<String> dataCovMatrixComment = new ArrayList<String>();
        dataCovMatrixComment.add("covariance comment 1");
        dataCovMatrixComment.add("covariance comment 2");
        Assertions.assertEquals(dataCovMatrixComment, covariance.getComments());
        Assertions.assertEquals(FramesFactory.getTEME(), covariance.getReferenceFrame().asFrame());

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
                Assertions.assertEquals(covMatrix.getEntry(i, j),
                                    file.getData().getCovarianceBlock().getCovarianceMatrix().getEntry(i, j), 1e-15);
            }
        }

        // Check User defined Parameters Block
        HashMap<String, String> userDefinedParameters = new HashMap<String, String>();
        userDefinedParameters.put("EARTH_MODEL", "WGS-84");
        Assertions.assertEquals(userDefinedParameters, file.getData().getUserDefinedBlock().getParameters());

    }

    @Test
    public void testCentersAndTimeScales() {

        final OpmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).withDefaultMass(1000.0).buildOpmParser();

        final String name1 = "/ccsds/odm/opm/OPM-dummy-solar-system-barycenter.txt";
        final DataSource source1 = new DataSource(name1, () -> getClass().getResourceAsStream(name1));
        Opm file1 = parser.parseMessage(source1);
        Assertions.assertEquals("TDB", file1.getMetadata().getTimeSystem().name());
        Assertions.assertEquals("solar system barycenter", file1.getMetadata().getCenter().getBody().getName());

        final String name2 = "/ccsds/odm/opm/OPM-dummy-ssb.txt";
        final DataSource source2 = new DataSource(name2, () -> getClass().getResourceAsStream(name2));
        Opm file2 = parser.parseMessage(source2);
        Assertions.assertEquals("TCB", file2.getMetadata().getTimeSystem().name());
        Assertions.assertEquals("solar system barycenter", file2.getMetadata().getCenter().getBody().getName());

        final String name3 = "/ccsds/odm/opm/OPM-dummy-earth-barycenter.txt";
        final DataSource source3 = new DataSource(name3, () -> getClass().getResourceAsStream(name3));
        Opm file3 = parser.parseMessage(source3);
        Assertions.assertEquals("TDB", file3.getMetadata().getTimeSystem().name());
        Assertions.assertEquals("Earth-Moon barycenter", file3.getMetadata().getCenter().getBody().getName());

        final String name4 = "/ccsds/odm/opm/OPM-dummy-earth-dash-moon-barycenter.txt";
        final DataSource source4 = new DataSource(name4, () -> getClass().getResourceAsStream(name4));
        Opm file4 = parser.parseMessage(source4);
        Assertions.assertEquals("TDB", file4.getMetadata().getTimeSystem().name());
        Assertions.assertEquals("Earth-Moon barycenter", file4.getMetadata().getCenter().getBody().getName());

        final String name5 = "/ccsds/odm/opm/OPM-dummy-earth-moon-barycenter.txt";
        final DataSource source5 = new DataSource(name5, () -> getClass().getResourceAsStream(name5));
        Opm file5 = parser.parseMessage(source5);
        Assertions.assertEquals("UT1", file5.getMetadata().getTimeSystem().name());
        Assertions.assertEquals("Earth-Moon barycenter", file5.getMetadata().getCenter().getBody().getName());

        final String name6 = "/ccsds/odm/opm/OPM-dummy-emb.txt";
        final DataSource source6 = new DataSource(name6, () -> getClass().getResourceAsStream(name6));
        Opm file6 = parser.parseMessage(source6);
        Assertions.assertEquals("TT", file6.getMetadata().getTimeSystem().name());
        Assertions.assertEquals("Earth-Moon barycenter", file6.getMetadata().getCenter().getBody().getName());

    }

    @Test
    public void testOrbitFileInterface() {
        final String ex = "/ccsds/odm/opm/OPMExample4.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        final OpmParser parser = new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).withDefaultMass(1000.0).buildOpmParser();

        final Opm file = parser.parseMessage(source);

        final String satId = "2000-028A";
        Assertions.assertEquals(satId, file.getMetadata().getObjectID());

        checkPVEntry(file.getPVCoordinates(), file.getPVCoordinates());

    }

    private void checkPVEntry(final PVCoordinates expected,
                              final PVCoordinates actual) {
        final Vector3D expectedPos = expected.getPosition();
        final Vector3D expectedVel = expected.getVelocity();

        final Vector3D actualPos = actual.getPosition();
        final Vector3D actualVel = actual.getVelocity();

        final double eps = 1e-12;

        Assertions.assertEquals(expectedPos.getX(), actualPos.getX(), eps);
        Assertions.assertEquals(expectedPos.getY(), actualPos.getY(), eps);
        Assertions.assertEquals(expectedPos.getZ(), actualPos.getZ(), eps);

        Assertions.assertEquals(expectedVel.getX(), actualVel.getX(), eps);
        Assertions.assertEquals(expectedVel.getY(), actualVel.getY(), eps);
        Assertions.assertEquals(expectedVel.getZ(), actualVel.getZ(), eps);
    }

    @Test
    public void testWrongODMType() {
        final String name = "/ccsds/odm/omm/OMMExample1.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            withDefaultMass(1000.0).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assertions.assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    public void testNumberFormatErrorType() {
        final String name = "/ccsds/odm/opm/OPM-number-format-error.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            withMissionReferenceDate(new AbsoluteDate()).
            withDefaultMass(1000.0).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("SEMI_MAJOR_AXIS", oe.getParts()[0]);
            Assertions.assertEquals(17, oe.getParts()[1]);
            Assertions.assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    public void testUnknownCenter() throws URISyntaxException {
        final String name = "/ccsds/odm/opm/OPM-unknown-center.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Opm opm = new ParserBuilder().
                            withMu(Constants.EIGEN5C_EARTH_MU).
                            withDefaultMass(1000.0).
                            buildOpmParser().
                            parseMessage(source);
        Assertions.assertEquals("UNKNOWN-CENTER", opm.getMetadata().getCenter().getName());
        Assertions.assertNull(opm.getMetadata().getCenter().getBody());
        try {
            opm.getMetadata().getFrame();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, oe.getSpecifier());
            Assertions.assertEquals("UNKNOWN-CENTER", oe.getParts()[0]);
        }
    }

    @Test
    public void testUnknownFrame() throws URISyntaxException {
        final String name = "/ccsds/odm/opm/OPM-unknown-frame.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Opm opm = new ParserBuilder().
                            withMu(Constants.EIGEN5C_EARTH_MU).
                            withDefaultMass(1000.0).
                            buildOpmParser().
                            parseMessage(source);
        Assertions.assertEquals("ZZRF", opm.getMetadata().getReferenceFrame().getName());
        try {
            opm.getMetadata().getFrame();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            Assertions.assertEquals("ZZRF", oe.getParts()[0]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/odm/opm/OPMExample1.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new ParserBuilder().withMu(Constants.EIGEN5C_EARTH_MU).withDefaultMass(1000.0).buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongKeyword() throws URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory data.
        final String name = "/ccsds/odm/opm/OPM-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            withDefaultMass(1000.0).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(11, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testSpuriousMetaDataSection() throws URISyntaxException {
        final String name = "/ccsds/odm/opm/spurious-metadata.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(23, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("metadata", oe.getParts()[2]);
        }
    }

    @Test
    public void testIncompatibleUnits1() throws URISyntaxException {
        final String name = "/ccsds/odm/opm/OPM-incompatible-units.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withParsedUnitsBehavior(ParsedUnitsBehavior.CONVERT_COMPATIBLE).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assertions.assertEquals("s",  oe.getParts()[0]);
            Assertions.assertEquals("m²", oe.getParts()[1]);
        }
    }

    @Test
    public void testIncompatibleUnits2() throws URISyntaxException {
        final String name = "/ccsds/odm/opm/OPM-incompatible-units.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
            buildOpmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assertions.assertEquals("s",  oe.getParts()[0]);
            Assertions.assertEquals("m²", oe.getParts()[1]);
        }
    }

    @Test
    public void testIgnoredIncompatibleUnits() throws URISyntaxException {
        final String name = "/ccsds/odm/opm/OPM-incompatible-units.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Opm file = new ParserBuilder().
                             withParsedUnitsBehavior(ParsedUnitsBehavior.IGNORE_PARSED).
                             buildOpmParser().
                             parseMessage(source);
        Assertions.assertEquals(18.77, file.getData().getSpacecraftParametersBlock().getSolarRadArea(), 1.0e-10);
    }

    @Test
    public void testIssue619() {
        // test for issue 619 - moon centered transformation
        // Verify that moon is at the center of the new frame
        CelestialBody moon = CelestialBodyFactory.getMoon();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 12, 0, 00, TimeScalesFactory.getUTC());
        final String ex = "/ccsds/odm/opm/OPM-dummy-moon-EME2000.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OpmParser parser = new ParserBuilder().
                                 withMu(CelestialBodyFactory.getEarth().getGM()).
                                 withDefaultMass(1000.0).
                                 buildOpmParser();
        final Opm file = parser.parseMessage(source);
        final Frame actualFrame = file.getMetadata().getFrame();
        MatcherAssert.assertThat(moon.getPVCoordinates(date, actualFrame),
                                 OrekitMatchers.pvCloseTo(PVCoordinates.ZERO, 1e-3));
    }

}
