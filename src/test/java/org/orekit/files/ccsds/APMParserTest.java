/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class APMParserTest {

    private static final double QUATERNION_PRECISION = 1e-5;
    private static final double ANGLE_PRECISION = 1e-4;
    private static final double SPACECRAFT_PRECISION = 0.1;
    private static final double MANEUVER_PRECISION = 1.0e-2;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseAPM1() {

        // File
        final String ex = "/ccsds/APMExample.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().withMu(398600e9).
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true).
                withDataContext(DataContext.getDefault()).
                withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                          TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.parse(inEntry, "APMExample.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getCreationDate());
        Assert.assertEquals("GSFC", file.getOriginator());

        // Check Metadata Block
        Assert.assertEquals("TRMM",       file.getMetaData().getObjectName());
        Assert.assertEquals("1997-009A",  file.getMetaData().getObjectID());
        Assert.assertEquals(1997,         file.getMetaData().getLaunchYear());
        Assert.assertEquals(9,            file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A",          file.getMetaData().getLaunchPiece());
        Assert.assertEquals("EARTH",      file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetaData().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              file.getMetaData().getTimeSystem());

        // Check data block
        Assert.assertFalse(file.getHasManeuver());
        Assert.assertEquals("SC BODY 1", file.getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",   file.getQuaternionFrameBString());
        Assert.assertEquals("A2B",       file.getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getEpoch());
        Assert.assertEquals(0.25678, file.getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.00005, file.getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.87543, file.getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.40949, file.getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ3(), QUATERNION_PRECISION);

    }

    @Test
    public void testParseAPM2() {

        // File
        final String ex = "/ccsds/APMExample2.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().withMu(398600e9).
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true).
                withDataContext(DataContext.getDefault()).
                withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                          TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.parse(inEntry, "APMExample2.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2004, 2, 14, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getCreationDate());
        Assert.assertEquals("JPL", file.getOriginator());

        // Check Metadata Block
        Assert.assertEquals("MARS SPIRIT", file.getMetaData().getObjectName());
        Assert.assertEquals("2004-003A",   file.getMetaData().getObjectID());
        Assert.assertEquals(2004,          file.getMetaData().getLaunchYear());
        Assert.assertEquals(3,             file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A",           file.getMetaData().getLaunchPiece());
        Assert.assertEquals("EARTH",       file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertTrue(file.getMetaDataComment().isEmpty());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetaData().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              file.getMetaData().getTimeSystem());

        // Check data block: QUATERNION
        ArrayList<String> epochComment = new ArrayList<String>();
        epochComment.add("GEOCENTRIC, CARTESIAN, EARTH FIXED");
        epochComment.add("OBJECT_ID: 2004-003");
        epochComment.add("$ITIM  = 2004 JAN 14 22:26:18.400000, $ original launch time 14:36");
        epochComment.add("Generated by JPL");
        epochComment.add("Current attitude for orbit 20 and attitude maneuver");
        epochComment.add("planning data.");
        epochComment.add("Attitude state quaternion");
        Assert.assertEquals(epochComment,   file.getEpochComment());
        Assert.assertEquals("INSTRUMENT A", file.getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",      file.getQuaternionFrameBString());
        Assert.assertEquals("A2B",          file.getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getEpoch());
        
        Assert.assertEquals(0.47832, file.getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03123, file.getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.78543, file.getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.39158, file.getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ3(), QUATERNION_PRECISION);

        // Check data block: EULER
        ArrayList<String> eulerComment = new ArrayList<String>();
        eulerComment.add("Attitude specified as Euler elements");
        Assert.assertEquals(eulerComment,    file.getEulerComment());
        Assert.assertEquals("INSTRUMENT A",  file.getEulerFrameAString());
        Assert.assertEquals("ITRF-97",       file.getEulerFrameBString());
        Assert.assertEquals("A2B",           file.getEulerDirection());
        Assert.assertEquals("EULER FRAME A", file.getRateFrameString());
        Assert.assertEquals("312",           file.getEulerRotSeq());

        Assert.assertEquals(FastMath.toRadians(139.7527), file.getRotationAngles().getX(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(25.0658),  file.getRotationAngles().getY(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(-53.3688), file.getRotationAngles().getZ(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(0.1045),   file.getRotationRates().getX(),  ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(0.03214),  file.getRotationRates().getY(),  ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(0.02156),  file.getRotationRates().getZ(),  ANGLE_PRECISION);

        // Check data block: SPACECRAFT PARAMETERS
        ArrayList<String> spacecraftComment = new ArrayList<String>();
        spacecraftComment.add("Spacecraft Parameters");
        Assert.assertEquals(spacecraftComment, file.getSpacecraftComment());
        Assert.assertEquals("EME2000",         file.getInertiaRefFrameString());
        Assert.assertEquals(6080.0,            file.getI11(), SPACECRAFT_PRECISION);
        Assert.assertEquals(5245.5,            file.getI22(), SPACECRAFT_PRECISION);
        Assert.assertEquals(8067.3,            file.getI33(), SPACECRAFT_PRECISION);
        Assert.assertEquals(-135.9,            file.getI12(), SPACECRAFT_PRECISION);
        Assert.assertEquals(89.3,              file.getI13(), SPACECRAFT_PRECISION);
        Assert.assertEquals(-90.7,             file.getI23(), SPACECRAFT_PRECISION);

        // Check data block: MANEUVER
        ArrayList<String> maneuverComment = new ArrayList<String>();
        maneuverComment.add("Data follows for 1 planned maneuver.");
        maneuverComment.add("First attitude maneuver for: MARS SPIRIT");
        maneuverComment.add("Impulsive, torque direction fixed in body frame");
        Assert.assertEquals(maneuverComment, file.getManeuver(0).getComment());
        Assert.assertTrue(file.getHasManeuver());
        Assert.assertEquals(1, file.getNbManeuvers());
        Assert.assertEquals(1, file.getManeuvers().size());
        Assert.assertEquals("INSTRUMENT A", file.getManeuver(0).getRefFrameString());
        Assert.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 29, 0.5098,
                                             TimeScalesFactory.getUTC()),
                            file.getManeuver(0).getEpochStart());
        Assert.assertEquals(3,     file.getManeuver(0).getDuration(),      MANEUVER_PRECISION);
        Assert.assertEquals(-1.25, file.getManeuver(0).getTorque().getX(), MANEUVER_PRECISION);
        Assert.assertEquals(-0.5,  file.getManeuver(0).getTorque().getY(), MANEUVER_PRECISION);
        Assert.assertEquals(0.5,   file.getManeuver(0).getTorque().getZ(), MANEUVER_PRECISION);

    }

    @Test
    public void testParseAPM3() {

        // File
        final String ex = "/ccsds/APMExample3.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().withMu(398600e9).
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true).
                withDataContext(DataContext.getDefault()).
                withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                          TimeScalesFactory.getUTC())).
                withInternationalDesignator(1997, 9, "A");

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.parse(inEntry, "APMExample3.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getCreationDate());
        Assert.assertEquals("GSFC", file.getOriginator());

        // Check Metadata Block
        Assert.assertEquals("TRMM",       file.getMetaData().getObjectName());
        Assert.assertEquals("1997-009A",  file.getMetaData().getObjectID());
        Assert.assertEquals(1997,         file.getMetaData().getLaunchYear());
        Assert.assertEquals(9,            file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A",          file.getMetaData().getLaunchPiece());
        Assert.assertEquals("EARTH",      file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetaData().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              file.getMetaData().getTimeSystem());

        // Check data block: QUATERNION
        Assert.assertFalse(file.getHasManeuver());
        Assert.assertEquals("SC BODY 1", file.getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",   file.getQuaternionFrameBString());
        Assert.assertEquals("A2B",       file.getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getEpoch());
        Assert.assertEquals(0.25678, file.getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.00005, file.getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.87543, file.getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.40949, file.getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     file.getQuaternionDot().getQ3(), QUATERNION_PRECISION);

        // Check data block: SPIN
        ArrayList<String> spinComment = new ArrayList<String>();
        spinComment.add("SPIN Parameters");
        Assert.assertEquals(spinComment, file.getSpinComment());
        Assert.assertEquals("SC BODY 1", file.getSpinFrameAString());
        Assert.assertEquals("ITRF-97",   file.getSpinFrameBString());
        Assert.assertEquals("A2B",       file.getSpinDirection());
        Assert.assertEquals(FastMath.toRadians(24.8),   file.getSpinAlpha(),      ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(33.7),   file.getSpinDelta(),      ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(42.5),   file.getSpinAngle(),      ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(-135.9), file.getSpinAngleVel(),   ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(89.3),   file.getNutation(),       ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(-90.7),  file.getNutationPhase(),  ANGLE_PRECISION);
        Assert.assertEquals(64.0,                       file.getNutationPeriod(), ANGLE_PRECISION);

    }

    @Test
    public void testParseAPM4() {

        // File
        final String ex = "/ccsds/APMExample4.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().withMu(398600e9).
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true).
                withDataContext(DataContext.getDefault()).
                withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                          TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.parse(inEntry, "APMExample4.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getCreationDate());
        Assert.assertEquals("GSFC", file.getOriginator());

        // Check Metadata Block
        Assert.assertEquals("TRMM",       file.getMetaData().getObjectName());
        Assert.assertEquals("1997-009A",  file.getMetaData().getObjectID());
        Assert.assertEquals(1997,         file.getMetaData().getLaunchYear());
        Assert.assertEquals(9,            file.getMetaData().getLaunchNumber());
        Assert.assertEquals("A",          file.getMetaData().getLaunchPiece());
        Assert.assertEquals("EARTH",      file.getMetaData().getCenterName());
        Assert.assertTrue(file.getMetaData().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), file.getMetaData().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              file.getMetaData().getTimeSystem());

        // Check data block
        Assert.assertFalse(file.getHasManeuver());
        Assert.assertEquals("SC BODY 1", file.getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",   file.getQuaternionFrameBString());
        Assert.assertEquals("A2B",       file.getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getEpoch());
        Assert.assertEquals(0.25678, file.getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.00005, file.getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.87543, file.getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.40949, file.getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.05678, file.getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.00001, file.getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.07543, file.getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.00949, file.getQuaternionDot().getQ3(), QUATERNION_PRECISION);

    }

    @Test
    public void testNotImplementedTimeSystems() {
        try {
            new APMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(getClass().getResourceAsStream("/ccsds/APM-inconsistent-time-systems.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals("BCE", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingIERSInitialization() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/APMExample2.txt").toURI().getPath();
        APMParser parser = new APMParser();
        try {
            // we explicitly forget to call parser.setConventions here
            parser.parse(name);
            parser.getConventions();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongADMType() {
        try {
            new APMParser().parse(getClass().getResourceAsStream("/ccsds/AEMExample.txt"), "AEMExample.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("AEMExample.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_AEM_VERS = 1.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testNumberFormatErrorType() {
        try {
            APMParser parser = new APMParser().withConventions(IERSConventions.IERS_2010);
            parser.parse(getClass().getResourceAsStream("/ccsds/APM-number-format-error.txt"),
                         "APM-number-format-error.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(20, oe.getParts()[0]);
            Assert.assertEquals("APM-number-format-error.txt", oe.getParts()[1]);
            Assert.assertEquals("Q1             = this-is-not-a-number", oe.getParts()[2]);
        }
    }


    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/APMExample.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new APMParser().parse(wrongName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongKeyword() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/APM-wrong-keyword.txt").toURI().getPath();
        try {
            new APMParser().withConventions(IERSConventions.IERS_2010).parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(11, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }
}
