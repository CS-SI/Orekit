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
import org.orekit.files.ccsds.AEMFile.AttitudeEphemeridesBlock;
import org.orekit.files.ccsds.AEMParser.AEMRotationOrder;
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
        final String ex = "/ccsds/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        final AEMFile file = parser.parse(inEntry, "AEMExample.txt");
        final AbsoluteDate start = new AbsoluteDate("1996-11-28T22:08:02.5555", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, start.durationFrom(file.getSatellites().get("1996-062A").getStart()), Double.MIN_VALUE);
        final AbsoluteDate end = new AbsoluteDate("1996-12-28T21:23:00.5555", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, end.durationFrom(file.getSatellites().get("1996-062A").getStop()), Double.MIN_VALUE);
        Assert.assertEquals("1996-062A", file.getSatellites().get("1996-062A").getId());
        Assert.assertEquals(1.0, file.getFormatVersion(), Double.MIN_VALUE);
        Assert.assertEquals(CelestialBodyFactory.getEarth().getGM(), file.getMu(), 1.0e-5);
        Assert.assertEquals(new AbsoluteDate(2002, 11, 4, 17, 22, 31.0, TimeScalesFactory.getUTC()),
                            file.getCreationDate());
        Assert.assertEquals("NASA/JPL", file.getOriginator());
        Assert.assertEquals(CcsdsTimeScale.UTC,     file.getAttitudeBlocks().get(0).getMetaData().getTimeSystem());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getAttitudeBlocks().get(0).getMetaData().getObjectName());
        Assert.assertEquals("1996-062A",            file.getAttitudeBlocks().get(0).getMetaData().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      file.getAttitudeBlocks().get(0).getMetaData().getCenterName());
        Assert.assertEquals(1996,                   file.getAttitudeBlocks().get(0).getMetaData().getLaunchYear());
        Assert.assertEquals(62,                     file.getAttitudeBlocks().get(0).getMetaData().getLaunchNumber());
        Assert.assertEquals("A",                    file.getAttitudeBlocks().get(0).getMetaData().getLaunchPiece());
        Assert.assertFalse(file.getAttitudeBlocks().get(0).getMetaData().getHasCreatableBody());
        Assert.assertNull(file.getAttitudeBlocks().get(0).getMetaData().getCenterBody());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(0).getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 30, 1, 28, 2.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(0).getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 28, 22, 8, 2.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(0).getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 30, 1, 18, 2.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(0).getUseableStopTime());
        Assert.assertEquals("HERMITE", file.getAttitudeBlocks().get(0).getInterpolationMethod());
        Assert.assertEquals(7,         file.getAttitudeBlocks().get(0).getInterpolationDegree());
        Assert.assertFalse(file.getAttitudeBlocks().get(0).isFirst());
        Assert.assertEquals("EME2000",    file.getAttitudeBlocks().get(0).getRefFrameAString());
        Assert.assertEquals("SC BODY 1",  file.getAttitudeBlocks().get(0).getRefFrameBString());
        Assert.assertEquals("A2B",        file.getAttitudeBlocks().get(0).getAttitudeDirection());
        Assert.assertEquals("QUATERNION", file.getAttitudeBlocks().get(0).getAttitudeType());
        Assert.assertEquals(AngularDerivativesFilter.USE_R, file.getAttitudeBlocks().get(0).getAvailableDerivatives());
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.68427, 0.56748, 0.03146, 0.45689, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 file.getAttitudeBlocks().get(0).getAngularCoordinates().get(0), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 11, 28, 22, 8, 3.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.74533, 0.42319, -0.45697, 0.23784, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 file.getAttitudeBlocks().get(0).getAngularCoordinates().get(1), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 11, 28, 22, 8, 4.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.45652, -0.84532, 0.26974, -0.06532, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 file.getAttitudeBlocks().get(0).getAngularCoordinates().get(2), 1.0e-5);
        ArrayList<String> ephemeridesDataLinesComment = new ArrayList<String>();
        ephemeridesDataLinesComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        ephemeridesDataLinesComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        ephemeridesDataLinesComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(ephemeridesDataLinesComment, file.getAttitudeBlocks().get(0).getMetaData().getComment());
 
        Assert.assertEquals(CcsdsTimeScale.UTC,         file.getAttitudeBlocks().get(1).getMetaData().getTimeSystem());
        Assert.assertEquals(TimeScalesFactory.getUTC(), file.getAttitudeBlocks().get(1).getMetaData().getTimeScale());
        Assert.assertEquals("MARS GLOBAL SURVEYOR",     file.getAttitudeBlocks().get(1).getMetaData().getObjectName());
        Assert.assertEquals("1996-062A",                file.getAttitudeBlocks().get(1).getMetaData().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",          file.getAttitudeBlocks().get(1).getMetaData().getCenterName());
        Assert.assertEquals(1996,                       file.getAttitudeBlocks().get(1).getMetaData().getLaunchYear());
        Assert.assertEquals(62,                         file.getAttitudeBlocks().get(1).getMetaData().getLaunchNumber());
        Assert.assertEquals("A",                        file.getAttitudeBlocks().get(1).getMetaData().getLaunchPiece());
        Assert.assertFalse(file.getAttitudeBlocks().get(1).getMetaData().getHasCreatableBody());
        Assert.assertNull(file.getAttitudeBlocks().get(1).getMetaData().getCenterBody());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(1).getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(1).getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(1).getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.5555, TimeScalesFactory.getUTC()),
                            file.getAttitudeBlocks().get(1).getUseableStopTime());
        Assert.assertFalse(file.getAttitudeBlocks().get(1).isFirst());
        Assert.assertEquals("EME2000",    file.getAttitudeBlocks().get(1).getRefFrameAString());
        Assert.assertEquals("SC BODY 1",  file.getAttitudeBlocks().get(1).getRefFrameBString());
        Assert.assertEquals("A2B",        file.getAttitudeBlocks().get(1).getAttitudeDirection());
        Assert.assertEquals("QUATERNION", file.getAttitudeBlocks().get(1).getAttitudeType());
        Assert.assertEquals(AngularDerivativesFilter.USE_R, file.getAttitudeBlocks().get(1).getAvailableDerivatives());
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(0.72501, -0.64585, 0.018542, -0.23854, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 file.getAttitudeBlocks().get(1).getAngularCoordinates().get(0), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 12, 18, 12, 10, 5.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(-0.16767, 0.87451, -0.43475, 0.13458, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 file.getAttitudeBlocks().get(1).getAngularCoordinates().get(1), 1.0e-5);
        verifyAngularCoordinates(new TimeStampedAngularCoordinates(new AbsoluteDate(1996, 12, 18, 12, 10, 10.5555, TimeScalesFactory.getUTC()),
                                                                   new Rotation(-0.71418, 0.03125, -0.65874, 0.23458, false),
                                                                   Vector3D.ZERO,
                                                                   Vector3D.ZERO),
                                 file.getAttitudeBlocks().get(1).getAngularCoordinates().get(2), 1.0e-5);
        ArrayList<String> ephemeridesDataLinesComment2 = new ArrayList<String>();
        ephemeridesDataLinesComment2.add("This block begins after trajectory correction maneuver TCM-3.");
        Assert.assertEquals(ephemeridesDataLinesComment2, file.getAttitudeBlocks().get(1).getMetaData().getComment());
    }

    @Test
    public void testParseAEM2() throws URISyntaxException {
        final String name = getClass().getResource("/ccsds/AEMExample2.txt").toURI().getPath();
        AEMParser parser = new AEMParser().
                withConventions(IERSConventions.IERS_2010).
                withSimpleEOP(true).
                withMu(CelestialBodyFactory.getMars().getGM()).
                withDataContext(DataContext.getDefault()).
                withInternationalDesignator(1996, 2, "A").
                withMissionReferenceDate(new AbsoluteDate("1996-12-17T00:00:00.000",
                                                          TimeScalesFactory.getUTC()));

        final AEMFile file = parser.parse(name);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment, file.getAttitudeBlocks().get(0).getMetaData().getComment());
        Assert.assertEquals("EME2000",       file.getAttitudeBlocks().get(0).getRefFrameAString());
        Assert.assertEquals("SC BODY 1",     file.getAttitudeBlocks().get(0).getRefFrameBString());
        List<AttitudeEphemeridesBlock> blocks = file.getAttitudeBlocks();
        Assert.assertEquals(1, blocks.size());
        Assert.assertEquals(IERSConventions.IERS_2010, parser.getConventions());
        Assert.assertTrue(parser.isSimpleEOP());
        Assert.assertEquals(CelestialBodyFactory.getMars().getGM(), parser.getMu(), 1.0e-5);
        Assert.assertEquals(0.0, parser.getMissionReferenceDate().durationFrom(new AbsoluteDate(1996, 12, 17, 0, 0, 0.0, TimeScalesFactory.getUTC())), 1.0e-5);
        Assert.assertEquals(1996, parser.getLaunchYear());
        Assert.assertEquals(2, parser.getLaunchNumber());
        Assert.assertEquals("A", parser.getLaunchPiece());
        Assert.assertEquals(DataContext.getDefault(), parser.getDataContext());
        Assert.assertEquals((new AbsoluteDate("1996-12-17T00:00:00.000",
                                                          TimeScalesFactory.getUTC())), file.getMissionReferenceDate());
    }

    @Test
    public void testParseAEM3() throws URISyntaxException {
        final String ex = "/ccsds/AEMExample3.txt";
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
        final String ex = "/ccsds/AEMExample4.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        final AEMFile file = parser.parse(inEntry, "AEMExample4.txt");
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Spin KF ground solution, SPINKF rates");
        Assert.assertEquals(dataComment, file.getAttitudeBlocks().get(0).getAttitudeDataLinesComment());
    }

    @Test
    public void testParseAEM5() throws URISyntaxException {
        final String ex = "/ccsds/AEMExample5.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        final AEMFile file = parser.parse(inEntry, "AEMExample5.txt");
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment,        file.getAttitudeBlocks().get(0).getMetaData().getComment());
        Assert.assertEquals(CcsdsTimeScale.UTC,     file.getAttitudeBlocks().get(0).getMetaData().getTimeSystem());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getAttitudeBlocks().get(0).getMetaData().getObjectName());
        Assert.assertEquals("1996-062A",            file.getAttitudeBlocks().get(0).getMetaData().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      file.getAttitudeBlocks().get(0).getMetaData().getCenterName());
        Assert.assertEquals(1996,                   file.getAttitudeBlocks().get(0).getMetaData().getLaunchYear());
        Assert.assertEquals(62,                     file.getAttitudeBlocks().get(0).getMetaData().getLaunchNumber());
        Assert.assertEquals("A",                    file.getAttitudeBlocks().get(0).getMetaData().getLaunchPiece());
        Assert.assertEquals("312",                  file.getAttitudeBlocks().get(0).getEulerRotSeq());
        Assert.assertEquals("EME2000",              file.getAttitudeBlocks().get(0).getRateFrameString());
        Assert.assertFalse(file.getAttitudeBlocks().get(0).getMetaData().getHasCreatableBody());
        Assert.assertNull(file.getAttitudeBlocks().get(0).getMetaData().getCenterBody());

        // Reference values
        final AbsoluteDate refDate = new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC());
        final Vector3D refRate     = new Vector3D(FastMath.toRadians(0.1045),
                                              FastMath.toRadians(0.03214),
                                              FastMath.toRadians(0.02156));
        final Vector3D refAcc      = Vector3D.ZERO;

        // Computed angular coordinates
        final TimeStampedAngularCoordinates ac = file.getAttitudeBlocks().get(0).getAngularCoordinates().get(0);
        final double[] angles = ac.getRotation().getAngles(AEMRotationOrder.getRotationOrder(file.getAttitudeBlocks().get(0).getEulerRotSeq()), RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(0.0, refDate.durationFrom(ac.getDate()),                 1.0e-5);
        Assert.assertEquals(0.0, refRate.distance(ac.getRotationRate()),             1.0e-5);
        Assert.assertEquals(0.0, refAcc.distance(ac.getRotationAcceleration()),      1.0e-5);
        Assert.assertEquals(-26.78, FastMath.toDegrees(angles[0]), 1.0e-2);
        Assert.assertEquals(46.26,  FastMath.toDegrees(angles[1]), 1.0e-2);
        Assert.assertEquals(144.10, FastMath.toDegrees(angles[2]), 1.0e-2);
    }

    @Test
    public void testParseAEM6() throws URISyntaxException {
        final String ex = "/ccsds/AEMExample6.txt";
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
            new AEMParser().parse(getClass().getResourceAsStream("/ccsds/OPMExample.txt"), "OPMExample.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("OPMExample.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_OPM_VERS = 2.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/AEMExample.txt").toURI().getPath();
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
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(getClass().getResourceAsStream("/ccsds/AEM-missing-attitude-type.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_NULL_ATTITUDE_TYPE, oe.getSpecifier());
        }
    }

    @Test
    public void testInconsistentTimeSystems() {
        try {
            new AEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(getClass().getResourceAsStream("/ccsds/AEM-inconsistent-time-systems.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals(CcsdsTimeScale.UTC, oe.getParts()[0]);
            Assert.assertEquals(CcsdsTimeScale.TCG, oe.getParts()[1]);
        }
    }

    @Test
    public void testLowerCaseValue() {
        //setup
        String file = "/ccsds/aemLowerCaseValue.aem";
        InputStream in = getClass().getResourceAsStream(file);

        //action
        AEMFile actual = new AEMParser().parse(in, file);

        //verify
        Assert.assertEquals(
                CelestialBodyFactory.getEarth(),
                actual.getAttitudeBlocks().get(0).getMetaData().getCenterBody());
    }

    @Test
    public void testWrongFile() {
        try {
            new AEMParser().parse(getClass().getResourceAsStream("/ccsds/OPMExample.txt"), "OPMExample.txt");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("OPMExample.txt", oe.getParts()[1]);
            Assert.assertEquals("CCSDS_OPM_VERS = 2.0", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongKeyword()
        throws URISyntaxException {
        // simple test for AEM file, contains a wrong keyword in the metadata.
        final String name = getClass().getResource("/ccsds/AEM-wrong-keyword.txt").toURI().getPath();
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
            parse(getClass().getResourceAsStream("/ccsds/AEM-ephemeris-number-format-error.txt"),
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
        final String name = getClass().getResource("/ccsds/AEM-keyword-within-ephemeris.txt").toURI().getPath();
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
        final String name = getClass().getResource("/ccsds/AEM-inconsistent-rotation-sequence.txt").toURI().getPath();
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
        final String ex = "/ccsds/AEMExample.txt";
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

        final String name = getClass().getResource("/ccsds/AEMExample.txt").toURI().getPath();
        AEMParser parser = new AEMParser();

        final AEMFile file = parser.parse(name);
        Assert.assertEquals(7, file.getAttitudeBlocks().get(0).getInterpolationDegree());
        Assert.assertEquals(1, file.getAttitudeBlocks().get(1).getInterpolationDegree());

        parser = parser.withInterpolationDegree(5);

        final AEMFile file2 = parser.parse(name);
        Assert.assertEquals(7, file2.getAttitudeBlocks().get(0).getInterpolationDegree());
        Assert.assertEquals(5, file2.getAttitudeBlocks().get(1).getInterpolationDegree());
    }

    @Test
    public void testIssue739() {
        final String ex = "/ccsds/AEMExample8.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        parser.setLocalScBodyReferenceFrameA(FramesFactory.getEME2000());
        parser.setLocalScBodyReferenceFrameB(FramesFactory.getGCRF());
        final AEMFile file = parser.parse(inEntry, "AEMExample8.txt");
        Assert.assertEquals(FramesFactory.getEME2000(), file.getAttitudeBlocks().get(0).getReferenceFrame());

        final BoundedAttitudeProvider provider = file.getAttitudeBlocks().get(0).getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        Assert.assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        Assert.assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        Assert.assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(file.getAttitudeBlocks().get(0).getStart()), 0.0001);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(file.getAttitudeBlocks().get(0).getStop()), 0.0001);

    }

    @Test
    public void testIssue739_2() {
        final String ex = "/ccsds/AEMExample9.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        parser.setLocalScBodyReferenceFrameB(FramesFactory.getGCRF());
        final AEMFile file = parser.parse(inEntry, "AEMExample9.txt");
        Assert.assertEquals(FramesFactory.getITRF(ITRFVersion.ITRF_93, IERSConventions.IERS_2010, true), file.getAttitudeBlocks().get(0).getReferenceFrame());

        final BoundedAttitudeProvider provider = file.getAttitudeBlocks().get(0).getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        Assert.assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        Assert.assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        Assert.assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(file.getAttitudeBlocks().get(0).getStart()), 0.0001);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(file.getAttitudeBlocks().get(0).getStop()), 0.0001);

    }

}
