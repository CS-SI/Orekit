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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.adm.aem.AEMAttitudeType;
import org.orekit.files.ccsds.ndm.adm.aem.AEMFile;
import org.orekit.files.ccsds.ndm.adm.aem.AEMParser;
import org.orekit.files.ccsds.ndm.adm.aem.AttitudeEphemeridesBlock;
import org.orekit.files.ccsds.ndm.adm.aem.AEMParser.AEMRotationOrder;
import org.orekit.files.ccsds.ndm.odm.oem.OEMParser;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMParserTest {

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseAEM1() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        final AEMFile file = parser.parse(inEntry, "AEMExample.txt");
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment0 = file.getSegments().get(0);
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment1 = file.getSegments().get(1);
        final AbsoluteDate start = new AbsoluteDate("1996-11-28T22:08:02.5555", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, start.durationFrom(file.getSatellites().get("1996-062A").getStart()), Double.MIN_VALUE);
        final AbsoluteDate end = new AbsoluteDate("1996-12-28T21:23:00.5555", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, end.durationFrom(file.getSatellites().get("1996-062A").getStop()), Double.MIN_VALUE);
        Assert.assertEquals("1996-062A", file.getSatellites().get("1996-062A").getId());
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assert.assertEquals(new AbsoluteDate(2002, 11, 4, 17, 22, 31.0, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("NASA/JPL", file.getHeader().getOriginator());
        Assert.assertEquals(CcsdsTimeScale.UTC,     segment0.getMetadata().getTimeSystem());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenterName());
        Assert.assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        Assert.assertFalse(segment0.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment0.getMetadata().getCenterBody());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC()),
                            segment0.getData().getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 30, 1, 28, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getData().getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 28, 22, 8, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getData().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 30, 1, 18, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getData().getUseableStopTime());
        Assert.assertEquals("HERMITE", segment0.getData().getInterpolationMethod());
        Assert.assertEquals(7,         segment0.getData().getInterpolationDegree());
        Assert.assertFalse(segment0.getData().isFirst());
        Assert.assertEquals("EME2000",    segment0.getData().getRefFrameAString());
        Assert.assertEquals("SC BODY 1",  segment0.getData().getRefFrameBString());
        Assert.assertEquals("A2B",        segment0.getData().getAttitudeDirection());
        Assert.assertEquals("QUATERNION", segment0.getData().getAttitudeType());
        Assert.assertEquals(AngularDerivativesFilter.USE_R, segment0.getData().getAvailableDerivatives());
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.68427, 0.56748, 0.03146, 0.45689, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 segment0.getData().getAngularCoordinates().get(0), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 11, 28, 22, 8, 3.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.74533, 0.42319, -0.45697, 0.23784, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 segment0.getData().getAngularCoordinates().get(1), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 11, 28, 22, 8, 4.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.45652, -0.84532, 0.26974, -0.06532, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 segment0.getData().getAngularCoordinates().get(2), 1.0e-5);
        ArrayList<String> ephemeridesDataLinesComment = new ArrayList<String>();
        ephemeridesDataLinesComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        ephemeridesDataLinesComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        ephemeridesDataLinesComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(ephemeridesDataLinesComment, segment0.getMetadata().getComments());
 
        Assert.assertEquals(CcsdsTimeScale.UTC,         segment1.getMetadata().getTimeSystem());
        Assert.assertEquals(TimeScalesFactory.getUTC(), segment1.getMetadata().getTimeScale());
        Assert.assertEquals("MARS GLOBAL SURVEYOR",     segment1.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",                segment1.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",          segment1.getMetadata().getCenterName());
        Assert.assertEquals(1996,                       segment1.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                         segment1.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                        segment1.getMetadata().getLaunchPiece());
        Assert.assertFalse(segment1.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment1.getMetadata().getCenterBody());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getData().getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getData().getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getData().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getData().getUseableStopTime());
        Assert.assertFalse(segment1.getData().isFirst());
        Assert.assertEquals("EME2000",    segment1.getData().getRefFrameAString());
        Assert.assertEquals("SC BODY 1",  segment1.getData().getRefFrameBString());
        Assert.assertEquals("A2B",        segment1.getData().getAttitudeDirection());
        Assert.assertEquals("QUATERNION", segment1.getData().getAttitudeType());
        Assert.assertEquals(AngularDerivativesFilter.USE_R, segment1.getData().getAvailableDerivatives());
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.72501, -0.64585, 0.018542, -0.23854, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 segment1.getData().getAngularCoordinates().get(0), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 12, 18, 12, 10, 5.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(-0.16767, 0.87451, -0.43475, 0.13458, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 segment1.getData().getAngularCoordinates().get(1), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 12, 18, 12, 10, 10.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(-0.71418, 0.03125, -0.65874, 0.23458, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 segment1.getData().getAngularCoordinates().get(2), 1.0e-5);
        ArrayList<String> ephemeridesDataLinesComment2 = new ArrayList<String>();
        ephemeridesDataLinesComment2.add("This block begins after trajectory correction maneuver TCM-3.");
        Assert.assertEquals(ephemeridesDataLinesComment2, segment1.getMetadata().getComments());
    }

    @Test
    public void testParseAEM2() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/adm/aem/AEMExample2.txt").toURI().getPath();
        AEMParser parser = new AEMParser().
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true).
                withMu(CelestialBodyFactory.getMars().getGM()).
                withDataContext(DataContext.getDefault()).
                withMissionReferenceDate(new AbsoluteDate("1996-12-17T00:00:00.000",
                                                          TimeScalesFactory.getUTC()));

        final AEMFile file = parser.parse(name);
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment, segment0.getMetadata().getComments());
        Assert.assertEquals("EME2000",       segment0.getData().getRefFrameAString());
        Assert.assertEquals("SC BODY 1",     segment0.getData().getRefFrameBString());
        List<NDMSegment<AEMMetadata, AttitudeEphemeridesBlock>> blocks = file.getSegments();
        Assert.assertEquals(1, blocks.size());
        Assert.assertEquals(IERSConventions.IERS_2010, parser.getConventions());
        Assert.assertTrue(parser.isSimpleEOP());
        Assert.assertEquals(CelestialBodyFactory.getMars().getGM(), parser.getMu(), 1.0e-5);
        Assert.assertEquals(0.0, parser.getMissionReferenceDate().durationFrom(new AbsoluteDate(1996, 12, 17, 0, 0, 0.0, TimeScalesFactory.getUTC())), 1.0e-5);
        Assert.assertEquals(DataContext.getDefault(), parser.getDataContext());
        Assert.assertEquals((new AbsoluteDate("1996-12-17T00:00:00.000",
                                              TimeScalesFactory.getUTC())),
                            parser.getMissionReferenceDate());
    }

    @Test
    public void testParseAEM3() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample3.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        
        try {
            parser.parse(inEntry, "AEMExample3.txt");
        }  catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.getName(), oe.getParts()[0]);
        }
    }

    @Test
    public void testParseAEM4() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample4.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        final AEMFile file = parser.parse(inEntry, "AEMExample4.txt");
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment0 = file.getSegments().get(0);
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Spin KF ground solution, SPINKF rates");
        Assert.assertEquals(dataComment, segment0.getData().getAttitudeDataLinesComment());
    }

    @Test
    public void testParseAEM5() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample5.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        final AEMFile file = parser.parse(inEntry, "AEMExample5.txt");
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment,        segment0.getMetadata().getComments());
        Assert.assertEquals(CcsdsTimeScale.UTC,     segment0.getMetadata().getTimeSystem());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenterName());
        Assert.assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        Assert.assertEquals("312",                  segment0.getData().getEulerRotSeq());
        Assert.assertEquals("EME2000",              segment0.getData().getRateFrameString());
        Assert.assertFalse(segment0.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment0.getMetadata().getCenterBody());

        // Reference values
        final AbsoluteDate refDate = new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC());
        final Vector3D refRate     = new Vector3D(FastMath.toRadians(0.1045),
                                              FastMath.toRadians(0.03214),
                                              FastMath.toRadians(0.02156));
        final Vector3D refAcc      = Vector3D.ZERO;

        // Computed angular coordinates
        final TimeStampedAngularCoordinates ac = segment0.getData().getAngularCoordinates().get(0);
        final double[] angles = ac.getRotation().getAngles(AEMRotationOrder.getRotationOrder(segment0.getData().getEulerRotSeq()), RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(0.0, refDate.durationFrom(ac.getDate()),                 1.0e-5);
        Assert.assertEquals(0.0, refRate.distance(ac.getRotationRate()),             1.0e-5);
        Assert.assertEquals(0.0, refAcc.distance(ac.getRotationAcceleration()),      1.0e-5);
        Assert.assertEquals(-26.78, FastMath.toDegrees(angles[0]), 1.0e-2);
        Assert.assertEquals(46.26,  FastMath.toDegrees(angles[1]), 1.0e-2);
        Assert.assertEquals(144.10, FastMath.toDegrees(angles[2]), 1.0e-2);
    }

    @Test
    public void testParseAEM6() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample6.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        
        try {
            parser.parse(inEntry, "AEMExample6.txt");
        }  catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.getName(), oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongNDMType() {
        try {
            new AEMParser().parse(getClass().getResourceAsStream("/ccsds/odm/opm/OPMExample1.txt"), "OPMExample1.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("OPMExample1.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_OPM_VERS = 3.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/adm/aem/AEMExample.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new OEMParser().parse(wrongName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingAttitudeType() {
        try {
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(getClass().getResourceAsStream("/ccsds/adm/aem/AEM-missing-attitude-type.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_NULL_ATTITUDE_TYPE, oe.getSpecifier());
        }
    }

    @Test
    public void testInconsistentTimeSystems() {
        try {
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(getClass().getResourceAsStream("/ccsds/adm/aem/AEM-inconsistent-time-systems.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals(CcsdsTimeScale.UTC, oe.getParts()[0]);
            Assert.assertEquals(CcsdsTimeScale.TCG, oe.getParts()[1]);
        }
    }

    @Test
    public void testLowerCaseValue() {
        //setup
        String file = "/ccsds/adm/aem/aemLowerCaseValue.aem";
        InputStream in = getClass().getResourceAsStream(file);

        //action
        AEMFile actual = new AEMParser().parse(in, file);

        //verify
        Assert.assertEquals(
                CelestialBodyFactory.getEarth(),
                actual.getSegments().get(0).getMetadata().getCenterBody());
    }

    @Test
    public void testWrongFile() {
        try {
            new AEMParser().parse(getClass().getResourceAsStream("/ccsds/odm/opm/OPMExample1.txt"), "OPMExample1.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("OPMExample1.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_OPM_VERS = 3.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongKeyword()
        throws URISyntaxException {
        // simple test for AEM file, contains a wrong keyword in the metadata.
        final String name = getClass().getResource("/ccsds/adm/aem/AEM-wrong-keyword.txt").toURI().getPath();
        try {
            new AEMParser().parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(24, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testEphemerisNumberFormatErrorType() {
        try {
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).
            parse(getClass().getResourceAsStream("/ccsds/adm/aem/AEM-ephemeris-number-format-error.txt"),
                                                 "AEM-ephemeris-number-format-error.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(28, oe.getParts()[0]);
            Assert.assertEquals("AEM-ephemeris-number-format-error.txt", oe.getParts()[1]);
            Assert.assertEquals("1996-11-28T22:08:03.5555 0.42319   this-is-not-a-number  0.23784   0.74533", oe.getParts()[2]);
        }
    }


    @Test
    public void testKeywordWithinEphemeris()
        throws URISyntaxException {
        // simple test for AEM file, contains p/v entries and other mandatory data.
        final String name = getClass().getResource("/ccsds/adm/aem/AEM-keyword-within-ephemeris.txt").toURI().getPath();
        try {
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(29, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("USER_DEFINED_TEST_KEY"));
        }
    }

    @Test
    public void testWrongRotationSequence() throws URISyntaxException {
        // simple test for AEM file, contains a wrong keyword in the metadata.
        final String name = getClass().getResource("/ccsds/adm/aem/AEM-inconsistent-rotation-sequence.txt").toURI().getPath();
        try {
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_INVALID_ROTATION_SEQUENCE, oe.getSpecifier());
            Assert.assertEquals("7051995", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingConvention() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        final AEMFile file = parser.parse(inEntry, "AEMExample.txt");
        try {
            file.getConventions();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS, oe.getSpecifier());
        }
    }

    private void verifyAngularCoordinates(final TimeStampedAngularCoordinates expected,
                                          final TimeStampedAngularCoordinates actual,
                                          final double threshold) {
        // Verify date
        Assert.assertEquals(0.0, expected.getDate().durationFrom(actual.getDate()), threshold);

        // Verify Angular elements
        Assert.assertEquals(expected.getRotation().getQ0(), actual.getRotation().getQ0(), threshold);
        Assert.assertEquals(expected.getRotation().getQ1(), actual.getRotation().getQ1(), threshold);
        Assert.assertEquals(expected.getRotation().getQ2(), actual.getRotation().getQ2(), threshold);
        Assert.assertEquals(expected.getRotation().getQ3(), actual.getRotation().getQ3(), threshold);

        Assert.assertEquals(0.0, expected.getRotationRate().distance(actual.getRotationRate()), threshold);
    }

    /**
     * Check if the parser enters the correct interpolation degree
     * (the parsed one or the default if there is none)
     */
    @Test
    public void testDefaultInterpolationDegree()
        throws URISyntaxException {

        final String name = getClass().getResource("/ccsds/adm/aem/AEMExample.txt").toURI().getPath();
        AEMParser parser = new AEMParser();

        final AEMFile file = parser.parse(name);
        Assert.assertEquals(7, file.getSegments().get(0).getData().getInterpolationDegree());
        Assert.assertEquals(1, file.getSegments().get(1).getData().getInterpolationDegree());

        parser = parser.withInterpolationDegree(5);

        final AEMFile file2 = parser.parse(name);
        Assert.assertEquals(7, file2.getSegments().get(0).getData().getInterpolationDegree());
        Assert.assertEquals(5, file2.getSegments().get(1).getData().getInterpolationDegree());
    }

    @Test
    public void testIssue739() {
        final String ex = "/ccsds/adm/aem/AEMExample8.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        parser.setLocalScBodyReferenceFrameA(FramesFactory.getEME2000());
        parser.setLocalScBodyReferenceFrameB(FramesFactory.getGCRF());
        final AEMFile file = parser.parse(inEntry, "AEMExample8.txt");
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment0 = file.getSegments().get(0);
        Assert.assertEquals(FramesFactory.getEME2000(), segment0.getData().getReferenceFrame());

        final BoundedAttitudeProvider provider = segment0.getData().getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        Assert.assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        Assert.assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        Assert.assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(segment0.getData().getStart()), 0.0001);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(segment0.getData().getStop()), 0.0001);

    }

    @Test
    public void testIssue739_2() {
        final String ex = "/ccsds/adm/aem/AEMExample9.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        parser.setLocalScBodyReferenceFrameB(FramesFactory.getGCRF());
        final AEMFile file = parser.parse(inEntry, "AEMExample9.txt");
        final NDMSegment<AEMMetadata, AttitudeEphemeridesBlock> segment0 = file.getSegments().get(0);
        Assert.assertEquals(FramesFactory.getITRF(ITRFVersion.ITRF_93, IERSConventions.IERS_2010, true),
                            segment0.getData().getReferenceFrame());

        final BoundedAttitudeProvider provider = segment0.getData().getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        Assert.assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        Assert.assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        Assert.assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(segment0.getData().getStart()), 0.0001);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(segment0.getData().getStop()), 0.0001);

    }

}
