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
package org.orekit.files.ccsds.ndm.adm.apm;

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
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.adm.ADMMetadata;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
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
        final String ex = "/ccsds/adm/apm/APMExample.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().
                                 withConventions(IERSConventions.IERS_2010).
                                 withSimpleEOP(true).
                                 withDataContext(DataContext.getDefault()).
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.oldParse(inEntry, "APMExample.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("GSFC", file.getHeader().getOriginator());

        NDMSegment<ADMMetadata, APMData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assert.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assert.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assert.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assert.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assert.assertEquals("EARTH",      segment.getMetadata().getCenterName());
        Assert.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              segment.getMetadata().getTimeSystem());

        // Check data block
        Assert.assertFalse(segment.getData().getHasManeuver());
        Assert.assertEquals("SC BODY 1", segment.getData().getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",   segment.getData().getQuaternionFrameBString());
        Assert.assertEquals("A2B",       segment.getData().getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assert.assertEquals(0.25678, segment.getData().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.00005, segment.getData().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.87543, segment.getData().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.40949, segment.getData().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ3(), QUATERNION_PRECISION);

    }

    @Test
    public void testParseAPM2() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample2.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().
                                 withConventions(IERSConventions.IERS_2010).
                                 withSimpleEOP(true).
                                 withDataContext(DataContext.getDefault()).
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.oldParse(inEntry, "APMExample2.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2004, 2, 14, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JPL", file.getHeader().getOriginator());

        NDMSegment<ADMMetadata, APMData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assert.assertEquals("MARS SPIRIT", segment.getMetadata().getObjectName());
        Assert.assertEquals("2004-003A",   segment.getMetadata().getObjectID());
        Assert.assertEquals(2004,          segment.getMetadata().getLaunchYear());
        Assert.assertEquals(3,             segment.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",           segment.getMetadata().getLaunchPiece());
        Assert.assertEquals("EARTH",       segment.getMetadata().getCenterName());
        Assert.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assert.assertTrue(segment.getMetadata().getComments().isEmpty());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              segment.getMetadata().getTimeSystem());

        // Check data block: QUATERNION
        ArrayList<String> epochComment = new ArrayList<String>();
        epochComment.add("GEOCENTRIC, CARTESIAN, EARTH FIXED");
        epochComment.add("OBJECT_ID: 2004-003");
        epochComment.add("$ITIM  = 2004 JAN 14 22:26:18.400000, $ original launch time 14:36");
        epochComment.add("Generated by JPL");
        epochComment.add("Current attitude for orbit 20 and attitude maneuver");
        epochComment.add("planning data.");
        epochComment.add("Attitude state quaternion");
        Assert.assertEquals(epochComment,   segment.getData().getEpochComment());
        Assert.assertEquals("INSTRUMENT A", segment.getData().getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",      segment.getData().getQuaternionFrameBString());
        Assert.assertEquals("A2B",          segment.getData().getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        
        Assert.assertEquals(0.47832, segment.getData().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03123, segment.getData().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.78543, segment.getData().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.39158, segment.getData().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ3(), QUATERNION_PRECISION);

        // Check data block: EULER
        ArrayList<String> eulerComment = new ArrayList<String>();
        eulerComment.add("Attitude specified as Euler elements");
        Assert.assertEquals(eulerComment,    segment.getData().getEulerComment());
        Assert.assertEquals("INSTRUMENT A",  segment.getData().getEulerFrameAString());
        Assert.assertEquals("ITRF-97",       segment.getData().getEulerFrameBString());
        Assert.assertEquals("A2B",           segment.getData().getEulerDirection());
        Assert.assertEquals("EULER FRAME A", segment.getData().getRateFrameString());
        Assert.assertEquals("312",           segment.getData().getEulerRotSeq());

        Assert.assertEquals(FastMath.toRadians(139.7527), segment.getData().getRotationAngles().getX(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(25.0658),  segment.getData().getRotationAngles().getY(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(-53.3688), segment.getData().getRotationAngles().getZ(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(0.1045),   segment.getData().getRotationRates().getX(),  ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(0.03214),  segment.getData().getRotationRates().getY(),  ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(0.02156),  segment.getData().getRotationRates().getZ(),  ANGLE_PRECISION);

        // Check data block: SPACECRAFT PARAMETERS
        ArrayList<String> spacecraftComment = new ArrayList<String>();
        spacecraftComment.add("Spacecraft Parameters");
        Assert.assertEquals(spacecraftComment, segment.getData().getSpacecraftComment());
        Assert.assertEquals("EME2000",         segment.getData().getInertiaRefFrameString());
        Assert.assertEquals(6080.0,            segment.getData().getI11(), SPACECRAFT_PRECISION);
        Assert.assertEquals(5245.5,            segment.getData().getI22(), SPACECRAFT_PRECISION);
        Assert.assertEquals(8067.3,            segment.getData().getI33(), SPACECRAFT_PRECISION);
        Assert.assertEquals(-135.9,            segment.getData().getI12(), SPACECRAFT_PRECISION);
        Assert.assertEquals(89.3,              segment.getData().getI13(), SPACECRAFT_PRECISION);
        Assert.assertEquals(-90.7,             segment.getData().getI23(), SPACECRAFT_PRECISION);

        // Check data block: MANEUVER
        ArrayList<String> maneuverComment = new ArrayList<String>();
        maneuverComment.add("Data follows for 1 planned maneuver.");
        maneuverComment.add("First attitude maneuver for: MARS SPIRIT");
        maneuverComment.add("Impulsive, torque direction fixed in body frame");
        Assert.assertEquals(maneuverComment, segment.getData().getManeuver(0).getComments());
        Assert.assertTrue(segment.getData().getHasManeuver());
        Assert.assertEquals(1, segment.getData().getNbManeuvers());
        Assert.assertEquals(1, segment.getData().getManeuvers().size());
        Assert.assertEquals("INSTRUMENT A", segment.getData().getManeuver(0).getRefFrameString());
        Assert.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 29, 0.5098,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getManeuver(0).getEpochStart());
        Assert.assertEquals(3,     segment.getData().getManeuver(0).getDuration(),      MANEUVER_PRECISION);
        Assert.assertEquals(-1.25, segment.getData().getManeuver(0).getTorque().getX(), MANEUVER_PRECISION);
        Assert.assertEquals(-0.5,  segment.getData().getManeuver(0).getTorque().getY(), MANEUVER_PRECISION);
        Assert.assertEquals(0.5,   segment.getData().getManeuver(0).getTorque().getZ(), MANEUVER_PRECISION);

    }

    @Test
    public void testParseAPM3() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample3.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().
                                 withConventions(IERSConventions.IERS_2010).
                                 withSimpleEOP(true).
                                 withDataContext(DataContext.getDefault()).
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.oldParse(inEntry, "APMExample3.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("GSFC", file.getHeader().getOriginator());

        NDMSegment<ADMMetadata, APMData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assert.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assert.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assert.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assert.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assert.assertEquals("EARTH",      segment.getMetadata().getCenterName());
        Assert.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              segment.getMetadata().getTimeSystem());

        // Check data block: QUATERNION
        Assert.assertFalse(segment.getData().getHasManeuver());
        Assert.assertEquals("SC BODY 1", segment.getData().getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",   segment.getData().getQuaternionFrameBString());
        Assert.assertEquals("A2B",       segment.getData().getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assert.assertEquals(0.25678, segment.getData().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.00005, segment.getData().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.87543, segment.getData().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.40949, segment.getData().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.0,     segment.getData().getQuaternionDot().getQ3(), QUATERNION_PRECISION);

        // Check data block: SPIN
        ArrayList<String> spinComment = new ArrayList<String>();
        spinComment.add("SPIN Parameters");
        Assert.assertEquals(spinComment, segment.getData().getSpinComment());
        Assert.assertEquals("SC BODY 1", segment.getData().getSpinFrameAString());
        Assert.assertEquals("ITRF-97",   segment.getData().getSpinFrameBString());
        Assert.assertEquals("A2B",       segment.getData().getSpinDirection());
        Assert.assertEquals(FastMath.toRadians(24.8),   segment.getData().getSpinAlpha(),      ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(33.7),   segment.getData().getSpinDelta(),      ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(42.5),   segment.getData().getSpinAngle(),      ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(-135.9), segment.getData().getSpinAngleVel(),   ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(89.3),   segment.getData().getNutation(),       ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(-90.7),  segment.getData().getNutationPhase(),  ANGLE_PRECISION);
        Assert.assertEquals(64.0,                       segment.getData().getNutationPeriod(), ANGLE_PRECISION);

    }

    @Test
    public void testParseAPM4() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample4.txt";

        // Initialize the parser
        final APMParser parser = new APMParser().
                                 withConventions(IERSConventions.IERS_2010).
                                 withSimpleEOP(true).
                                 withDataContext(DataContext.getDefault()).
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC()));

        final InputStream inEntry = getClass().getResourceAsStream(ex);

        // Generated APM file
        final APMFile file = parser.oldParse(inEntry, "APMExample4.txt");

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());
        Assert.assertEquals("2002-09-30T14:28:15.117", file.getMissionReferenceDate().toString());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("GSFC", file.getHeader().getOriginator());

        NDMSegment<ADMMetadata, APMData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assert.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assert.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assert.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assert.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assert.assertEquals("EARTH",      segment.getMetadata().getCenterName());
        Assert.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assert.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenterBody());
        Assert.assertEquals(CcsdsTimeScale.UTC,              segment.getMetadata().getTimeSystem());

        // Check data block
        Assert.assertFalse(segment.getData().getHasManeuver());
        Assert.assertEquals("SC BODY 1", segment.getData().getQuaternionFrameAString());
        Assert.assertEquals("ITRF-97",   segment.getData().getQuaternionFrameBString());
        Assert.assertEquals("A2B",       segment.getData().getAttitudeQuaternionDirection());
        Assert.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assert.assertEquals(0.25678, segment.getData().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.00005, segment.getData().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.87543, segment.getData().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.40949, segment.getData().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.05678, segment.getData().getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.00001, segment.getData().getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.07543, segment.getData().getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.00949, segment.getData().getQuaternionDot().getQ3(), QUATERNION_PRECISION);

    }

    @Test
    public void testNotImplementedTimeSystems() {
        try {
            new APMParser().parse(getClass().getResourceAsStream("/ccsds/adm/apm/APM-inconsistent-time-systems.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals("BCE", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingIERSInitialization() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/adm/apm/APMExample2.txt").toURI().getPath();
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
            new APMParser().oldParse(getClass().getResourceAsStream("/ccsds/adm/aem/AEMExample.txt"), "AEMExample.txt");
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
            parser.oldParse(getClass().getResourceAsStream("/ccsds/adm/apm/APM-number-format-error.txt"),
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
        final String realName = getClass().getResource("/ccsds/adm/apm/APMExample.txt").toURI().getPath();
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
        final String name = getClass().getResource("/ccsds/adm/apm/APM-wrong-keyword.txt").toURI().getPath();
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
