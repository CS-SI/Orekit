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
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.section.Segment;
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
    public void testParseAEM01() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile file = new ParserBuilder().buildAemParser().parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final Segment<AemMetadata, AemData> segment1 = file.getSegments().get(1);
        final AbsoluteDate start = new AbsoluteDate("1996-11-28T22:08:02.5555", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, start.durationFrom(file.getSatellites().get("1996-062A").getStart()), Double.MIN_VALUE);
        final AbsoluteDate end = new AbsoluteDate("1996-12-28T21:23:00.5555", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, end.durationFrom(file.getSatellites().get("1996-062A").getStop()), Double.MIN_VALUE);
        Assert.assertEquals("1996-062A", file.getSatellites().get("1996-062A").getId());
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assert.assertEquals(new AbsoluteDate(2002, 11, 4, 17, 22, 31.0, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("NASA/JPL", file.getHeader().getOriginator());
        Assert.assertEquals("UTC",     segment0.getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenter().getName());
        Assert.assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        Assert.assertFalse(segment0.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment0.getMetadata().getCenter().getBody());
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
        Assert.assertEquals("EME2000",       segment0.getMetadata().getEndpoints().getFrameA().getName());
        Assert.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assert.assertEquals("1",
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assert.assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        Assert.assertEquals(AttitudeType.QUATERNION, segment1.getMetadata().getAttitudeType());
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
        ephemeridesDataLinesComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        ephemeridesDataLinesComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(ephemeridesDataLinesComment, segment0.getMetadata().getComments());
 
        Assert.assertEquals("UTC",                      segment1.getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR",     segment1.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",                segment1.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",          segment1.getMetadata().getCenter().getName());
        Assert.assertEquals(1996,                       segment1.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                         segment1.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                        segment1.getMetadata().getLaunchPiece());
        Assert.assertFalse(segment1.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment1.getMetadata().getCenter().getBody());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getUseableStopTime());
        Assert.assertFalse(segment1.getMetadata().isFirst());
        Assert.assertEquals("EME2000",       segment0.getMetadata().getEndpoints().getFrameA().getName());
        Assert.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndpoints().getSpacecraftBodyFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assert.assertEquals("1",
                            segment0.getMetadata().getEndpoints().getSpacecraftBodyFrame().asSpacecraftBodyFrame().getLabel());
        Assert.assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        Assert.assertEquals(AttitudeType.QUATERNION, segment1.getMetadata().getAttitudeType());
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
    public void testParseAEM02() throws URISyntaxException {
        final String name = "/ccsds/adm/aem/AEMExample02.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        AemParser parser = new ParserBuilder().
                           withMissionReferenceDate(new AbsoluteDate("1996-12-17T00:00:00.000",
                                                                     TimeScalesFactory.getUTC())).
                           buildAemParser();

        final AemFile file = parser.parse(source); // using generic API here
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment, segment0.getMetadata().getComments());
        Assert.assertEquals("EME2000",       segment0.getMetadata().getEndpoints().getFrameA().getName());
        Assert.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assert.assertEquals("1",
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        List<AemSegment> blocks = file.getSegments();
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
    public void testParseAEM03() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample03.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        
        try {
            parser.parseMessage(source);
        }  catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AttitudeType.SPIN.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testParseAEM04() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample04.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final AemFile file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Spin KF ground solution, SPINKF rates");
        Assert.assertEquals(dataComment, segment0.getData().getComments());
    }

    @Test
    public void testParseAEM05() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample05.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final AemFile file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment,        segment0.getMetadata().getComments());
        Assert.assertEquals("UTC",                  segment0.getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenter().getName());
        Assert.assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        Assert.assertEquals(RotationOrder.ZXY,      segment0.getMetadata().getEulerRotSeq());
        Assert.assertTrue(segment0.getMetadata().rateFrameIsA());
        Assert.assertFalse(segment0.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment0.getMetadata().getCenter().getBody());

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
    public void testParseAEM06() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample06.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        
        try {
            parser.parseMessage(source);
            Assert.fail("an exception should have been thrown");
        }  catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testParseAEM07() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample07.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final AemFile file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment,        segment0.getMetadata().getComments());
        Assert.assertEquals(TimeSystem.UTC,         segment0.getMetadata().getTimeSystem());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        Assert.assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenter().getName());
        Assert.assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        Assert.assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        Assert.assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        Assert.assertFalse(segment0.getMetadata().getHasCreatableBody());
        Assert.assertNull(segment0.getMetadata().getCenter().getBody());
        Assert.assertEquals(CelestialBodyFrame.EME2000, segment0.getMetadata().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assert.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY, segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assert.assertEquals("1", segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assert.assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        Assert.assertEquals(new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate("2002-12-18T12:02:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStopTime());
        Assert.assertEquals(new AbsoluteDate("2002-12-18T12:02:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStopTime());
        Assert.assertEquals(AttitudeType.QUATERNION, segment0.getMetadata().getAttitudeType());
        Assert.assertFalse(segment0.getMetadata().isFirst());
        Assert.assertEquals("HERMITE", segment0.getMetadata().getInterpolationMethod());
        Assert.assertEquals(7, segment0.getMetadata().getInterpolationDegree());

        final AbsoluteDate refDate = new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC());

        Assert.assertEquals(3, segment0.getData().getAngularCoordinates().size());
        final TimeStampedAngularCoordinates ac0 = segment0.getData().getAngularCoordinates().get(0);
        Assert.assertEquals(0.0, ac0.getDate().durationFrom(refDate), 1.0e-5);
        Assert.assertEquals(0.0,
                            Rotation.distance(new Rotation(0.68427, 0.56748, 0.03146, 0.45689, true),
                                              ac0.getRotation()), 
                            1.0e-10);
        final TimeStampedAngularCoordinates ac1 = segment0.getData().getAngularCoordinates().get(1);
        Assert.assertEquals(60.0, ac1.getDate().durationFrom(refDate), 1.0e-5);
        Assert.assertEquals(0.0,
                            Rotation.distance(new Rotation(0.74533, 0.42319, -0.45697, 0.23784, true),
                                              ac1.getRotation()), 
                            1.0e-10);
        final TimeStampedAngularCoordinates ac2 = segment0.getData().getAngularCoordinates().get(2);
        Assert.assertEquals(120.0, ac2.getDate().durationFrom(refDate), 1.0e-5);
        Assert.assertEquals(0.0,
                            Rotation.distance(new Rotation(0.45652, -0.84532, 0.26974, -0.06532, true),
                                              ac2.getRotation()), 
                            1.0e-10);

    }

    @Test
    public void testParseAEM11() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample11.xml";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final AemFile file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("This example shows an AEM with a rotation");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        Assert.assertEquals(metadataComment, segment0.getMetadata().getComments());
        Assert.assertEquals(TimeSystem.UTC,  segment0.getMetadata().getTimeSystem());
        Assert.assertEquals("FICTITIOUS",    segment0.getMetadata().getObjectName());
        Assert.assertEquals("2020-224A",     segment0.getMetadata().getObjectID());
        Assert.assertEquals("EARTH",         segment0.getMetadata().getCenter().getName());
        Assert.assertEquals(CelestialBodyFrame.J2000, segment0.getMetadata().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assert.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY, segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assert.assertEquals("1", segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assert.assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        Assert.assertEquals(new AbsoluteDate("2020-090T05:00:00.071", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate("2020-090T05:00:00.946", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStopTime());
        Assert.assertEquals(AttitudeType.EULER_ANGLE_RATE, segment0.getMetadata().getAttitudeType());

        final AbsoluteDate refDate = new AbsoluteDate("2020-090T05:00:00.071", TimeScalesFactory.getUTC());

        Assert.assertEquals(2, segment0.getData().getAngularCoordinates().size());
        final TimeStampedAngularCoordinates ac0 = segment0.getData().getAngularCoordinates().get(0);
        Assert.assertEquals(0.0, ac0.getDate().durationFrom(refDate), 1.0e-5);
        Assert.assertEquals(0.0,
                            Rotation.distance(new Rotation(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM,
                                                           FastMath.toRadians(45),
                                                           FastMath.toRadians(0.9),
                                                           FastMath.toRadians(15)),
                                              ac0.getRotation()), 
                            1.0e-10);
        final TimeStampedAngularCoordinates ac1 = segment0.getData().getAngularCoordinates().get(1);
        Assert.assertEquals(0.875, ac1.getDate().durationFrom(refDate), 1.0e-5);
        Assert.assertEquals(0.0,
                            Rotation.distance(new Rotation(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM,
                                                           FastMath.toRadians(50),
                                                           FastMath.toRadians(1.9),
                                                           FastMath.toRadians(1.5)),
                                              ac1.getRotation()), 
                            1.0e-10);

    }

    @Test
    public void testWrongNDMType() {
        final String name = "/ccsds/odm/opm/OPMExample1.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assert.assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/adm/aem/AEMExample01.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        final DataSource source =  new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new ParserBuilder().buildAemParser().parseMessage(source);
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
            new ParserBuilder().buildAemParser().parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals(AemMetadataKey.ATTITUDE_TYPE.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentTimeSystems() {
        try {
            final String name = "/ccsds/adm/aem/AEM-inconsistent-time-systems.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals("UTC", oe.getParts()[0]);
            Assert.assertEquals("TCG", oe.getParts()[1]);
        }
    }

    @Test
    public void testLowerCaseValue() {
        //setup
        String file = "/ccsds/adm/aem/aemLowerCaseValue.aem";
        final DataSource source = new DataSource(file, () -> getClass().getResourceAsStream(file));

        //action
        AemFile actual = new ParserBuilder().buildAemParser().parseMessage(source);

        //verify
        Assert.assertEquals(
                CelestialBodyFactory.getEarth(),
                actual.getSegments().get(0).getMetadata().getCenter().getBody());
    }

    @Test
    public void testWrongFile() {
        final String name = "/ccsds/odm/opm/OPMExample1.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            Assert.fail("an exception should have been thrown");
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
            new ParserBuilder().buildAemParser().parseMessage(source);
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
            new ParserBuilder().buildAemParser().parseMessage(source);
            Assert.fail("an exception should have been thrown");
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
            new ParserBuilder().buildAemParser().parseMessage(source);
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
            new ParserBuilder().buildAemParser().parseMessage(source);
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
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile file = new ParserBuilder().buildAemParser().parseMessage(source);
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

        final String name = "/ccsds/adm/aem/AEMExample01.txt";
        ParserBuilder builder = new ParserBuilder();

        final DataSource source1 = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final AemFile file = builder.buildAemParser().parseMessage(source1);
        Assert.assertEquals(7, file.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assert.assertEquals(1, file.getSegments().get(1).getMetadata().getInterpolationDegree());

        final DataSource source2 = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final AemFile file2 = builder.withDefaultInterpolationDegree(5).buildAemParser().parseMessage(source2);
        Assert.assertEquals(7, file2.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assert.assertEquals(5, file2.getSegments().get(1).getMetadata().getInterpolationDegree());
    }

    @Test
    public void testIssue739() {
        final String ex = "/ccsds/adm/aem/AEMExample08.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile file = new ParserBuilder().buildAemParser().parseMessage(source);
        final AemSegment segment0 = file.getSegments().get(0);
        Assert.assertEquals(CelestialBodyFrame.GTOD, segment0.getMetadata().getEndpoints().getFrameB().asCelestialBodyFrame());

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
        final String ex = "/ccsds/adm/aem/AEMExample09.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile file = new ParserBuilder().buildAemParser().parseMessage(source);
        final AemSegment segment0 = file.getSegments().get(0);
        Assert.assertEquals(FramesFactory.getITRF(ITRFVersion.ITRF_1993, IERSConventions.IERS_2010, true),
                            segment0.getMetadata().getEndpoints().getFrameA().asFrame());

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
