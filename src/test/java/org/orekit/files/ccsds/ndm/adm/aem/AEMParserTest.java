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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
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
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.CCSDSBodyFrame;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.lexical.KVNLexicalAnalyzer;
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
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1);
        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser);
        final Segment<AEMMetadata, AEMData> segment0 = file.getSegments().get(0);
        final Segment<AEMMetadata, AEMData> segment1 = file.getSegments().get(1);
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
                            segment0.getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 30, 1, 28, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 28, 22, 8, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 11, 30, 1, 18, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStopTime());
        Assert.assertEquals("HERMITE", segment0.getMetadata().getInterpolationMethod());
        Assert.assertEquals(7,         segment0.getMetadata().getInterpolationDegree());
        Assert.assertFalse(segment0.getMetadata().isFirst());
        Assert.assertEquals("EME2000",       segment0.getMetadata().getEndPoints().getExternalFrame().name());
        Assert.assertEquals(CCSDSBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndPoints().getLocalFrame().getBaseEquipment());
        Assert.assertEquals("1",
                            segment0.getMetadata().getEndPoints().getLocalFrame().getLabel());
        Assert.assertTrue(segment0.getMetadata().getEndPoints().isExternal2Local());
        Assert.assertEquals(AEMAttitudeType.QUATERNION, segment1.getMetadata().getAttitudeType());
        Assert.assertEquals(AngularDerivativesFilter.USE_R, segment0.getMetadata().getAttitudeType().getAngularDerivativesFilter());
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
        Assert.assertEquals("MARS GLOBAL SURVEYOR",     segment1.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",                segment1.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",          segment1.getMetadata().getCenterName());
        Assert.assertEquals(1996,                       segment1.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                         segment1.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                        segment1.getMetadata().getLaunchPiece());
        Assert.assertFalse(segment1.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment1.getMetadata().getCenterBody());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getUseableStopTime());
        Assert.assertFalse(segment1.getMetadata().isFirst());
        Assert.assertEquals("EME2000",       segment0.getMetadata().getEndPoints().getExternalFrame().name());
        Assert.assertEquals(CCSDSBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndPoints().getLocalFrame().getBaseEquipment());
        Assert.assertEquals("1",
                            segment0.getMetadata().getEndPoints().getLocalFrame().getLabel());
        Assert.assertTrue(segment0.getMetadata().getEndPoints().isExternal2Local());
        Assert.assertEquals(AEMAttitudeType.QUATERNION, segment1.getMetadata().getAttitudeType());
        Assert.assertEquals(AngularDerivativesFilter.USE_R, segment0.getMetadata().getAttitudeType().getAngularDerivativesFilter());
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
        final String name = "/ccsds/adm/aem/AEMExample2.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                         new AbsoluteDate("1996-12-17T00:00:00.000", TimeScalesFactory.getUTC()),
                                         1);

        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser);
        final Segment<AEMMetadata, AEMData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only.  The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment, segment0.getMetadata().getComments());
        Assert.assertEquals("EME2000",       segment0.getMetadata().getEndPoints().getExternalFrame().name());
        Assert.assertEquals(CCSDSBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndPoints().getLocalFrame().getBaseEquipment());
        Assert.assertEquals("1",
                            segment0.getMetadata().getEndPoints().getLocalFrame().getLabel());
        List<AEMSegment> blocks = file.getSegments();
        Assert.assertEquals(1, blocks.size());
        Assert.assertEquals(IERSConventions.IERS_2010, parser.getConventions());
        Assert.assertTrue(parser.isSimpleEOP());
        Assert.assertEquals(0.0, parser.getMissionReferenceDate().durationFrom(new AbsoluteDate(1996, 12, 17, 0, 0, 0.0, TimeScalesFactory.getUTC())), 1.0e-5);
        Assert.assertEquals(DataContext.getDefault(), parser.getDataContext());
        Assert.assertEquals((new AbsoluteDate("1996-12-17T00:00:00.000",
                                              TimeScalesFactory.getUTC())),
                            parser.getMissionReferenceDate());
    }

    @Test
    public void testParseAEM3() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample3.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        
        try {
            new KVNLexicalAnalyzer(source).accept(parser);
        }  catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testParseAEM4() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample4.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser);
        final Segment<AEMMetadata, AEMData> segment0 = file.getSegments().get(0);
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Spin KF ground solution, SPINKF rates");
        Assert.assertEquals(dataComment, segment0.getData().getComments());
    }

    @Test
    public void testParseAEM5() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample5.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser);
        final Segment<AEMMetadata, AEMData> segment0 = file.getSegments().get(0);
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
        Assert.assertEquals(RotationOrder.ZXY,      segment0.getMetadata().getEulerRotSeq());
        Assert.assertFalse(segment0.getMetadata().localRates());
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
        final double[] angles = ac.getRotation().getAngles(segment0.getMetadata().getEulerRotSeq(),
                                                           RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(0.0, refDate.durationFrom(ac.getDate()),                                      1.0e-5);
        Assert.assertEquals(0.0, refRate.distance(ac.getRotation().applyInverseTo(ac.getRotationRate())), 1.0e-5);
        Assert.assertEquals(0.0, refAcc.distance(ac.getRotationAcceleration()),                           1.0e-5);
        Assert.assertEquals(-26.78, FastMath.toDegrees(angles[0]), 1.0e-2);
        Assert.assertEquals(46.26,  FastMath.toDegrees(angles[1]), 1.0e-2);
        Assert.assertEquals(144.10, FastMath.toDegrees(angles[2]), 1.0e-2);
    }

    @Test
    public void testParseAEM6() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample6.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        
        try {
            new KVNLexicalAnalyzer(source).accept(parser);
        }  catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongNDMType() {
        final String name = "/ccsds/odm/opm/OPMExample1.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assert.assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/adm/aem/AEMExample.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        final DataSource source =  new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingAttitudeType() {
        try {
            final String name = "/ccsds/adm/aem/AEM-missing-attitude-type.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals(AEMMetadataKey.ATTITUDE_TYPE.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentTimeSystems() {
        try {
            final String name = "/ccsds/adm/aem/AEM-inconsistent-time-systems.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
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
        final DataSource source = new DataSource(file, () -> getClass().getResourceAsStream(file));

        //action
        AEMFile actual = new KVNLexicalAnalyzer(source).
                         accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));

        //verify
        Assert.assertEquals(
                CelestialBodyFactory.getEarth(),
                actual.getSegments().get(0).getMetadata().getCenterBody());
    }

    @Test
    public void testWrongFile() {
        final String name = "/ccsds/odm/opm/OPMExample1.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assert.assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongKeyword()
        throws URISyntaxException {
        // simple test for AEM file, contains a wrong keyword in the metadata.
        final String name = "/ccsds/adm/aem/AEM-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(24, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testEphemerisNumberFormatErrorType() {
        final String name = "/ccsds/adm/aem/AEM-ephemeris-number-format-error.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(28, oe.getParts()[0]);
            Assert.assertEquals(name, oe.getParts()[1]);
            Assert.assertEquals("1996-11-28T22:08:03.5555 0.42319   this-is-not-a-number  0.23784   0.74533", oe.getParts()[2]);
        }
    }

    @Test
    public void testKeywordWithinEphemeris()
        throws URISyntaxException {
        // simple test for AEM file, contains p/v entries and other mandatory data.
        final String name = "/ccsds/adm/aem/AEM-keyword-within-ephemeris.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
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
        final String name = "/ccsds/adm/aem/AEM-inconsistent-rotation-sequence.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new KVNLexicalAnalyzer(source).
            accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_INVALID_ROTATION_SEQUENCE, oe.getSpecifier());
            Assert.assertEquals("7051995", oe.getParts()[0]);
            Assert.assertEquals(22, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    public void testMissingConvention() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMFile file = new KVNLexicalAnalyzer(source).
                             accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
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

        final String name = "/ccsds/adm/aem/AEMExample.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        AEMParser parser1 = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1);

        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser1);
        Assert.assertEquals(7, file.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assert.assertEquals(1, file.getSegments().get(1).getMetadata().getInterpolationDegree());

        AEMParser parser2 = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 5);
        final AEMFile file2 = new KVNLexicalAnalyzer(source).accept(parser2);
        Assert.assertEquals(7, file2.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assert.assertEquals(5, file2.getSegments().get(1).getMetadata().getInterpolationDegree());
    }

    @Test
    public void testIssue739() {
        final String ex = "/ccsds/adm/aem/AEMExample8.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1);
        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser);
        final AEMSegment segment0 = file.getSegments().get(0);
        Assert.assertEquals(FramesFactory.getGTOD(IERSConventions.IERS_2010, true),
                            segment0.getMetadata().getEndPoints().getExternalFrame().getFrame(IERSConventions.IERS_2010, true));

        final BoundedAttitudeProvider provider = segment0.getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        Assert.assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        Assert.assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        Assert.assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(segment0.getMetadata().getStart()), 0.0001);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(segment0.getMetadata().getStop()), 0.0001);

    }

    @Test
    public void testIssue739_2() {
        final String ex = "/ccsds/adm/aem/AEMExample9.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile file = new KVNLexicalAnalyzer(source).accept(parser);
        final AEMSegment segment0 = file.getSegments().get(0);
        Assert.assertEquals(FramesFactory.getITRF(ITRFVersion.ITRF_93, IERSConventions.IERS_2010, true),
                            segment0.getMetadata().getEndPoints().getExternalFrame().getFrame(IERSConventions.IERS_2010, true));

        final BoundedAttitudeProvider provider = segment0.getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        Assert.assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        Assert.assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        Assert.assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(segment0.getMetadata().getStart()), 0.0001);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(segment0.getMetadata().getStop()), 0.0001);

    }

}
