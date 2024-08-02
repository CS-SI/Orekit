/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AEMParserTest {

    @BeforeEach
    void setUp()
            throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testParseAEM01() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().buildAemParser().parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final Segment<AemMetadata, AemData> segment1 = file.getSegments().get(1);
        final AbsoluteDate start = new AbsoluteDate("1996-11-28T22:08:02.5555", TimeScalesFactory.getUTC());
        assertEquals(0.0, start.durationFrom(file.getSatellites().get("1996-062A").getStart()), Double.MIN_VALUE);
        final AbsoluteDate end = new AbsoluteDate("1996-12-28T21:23:00.5555", TimeScalesFactory.getUTC());
        assertEquals(0.0, end.durationFrom(file.getSatellites().get("1996-062A").getStop()), Double.MIN_VALUE);
        assertEquals("1996-062A", file.getSatellites().get("1996-062A").getId());
        assertEquals(1.0, file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        assertEquals(new AbsoluteDate(2002, 11, 4, 17, 22, 31.0, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        assertEquals("NASA/JPL", file.getHeader().getOriginator());
        assertEquals("UTC",     segment0.getMetadata().getTimeSystem().name());
        assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenter().getName());
        assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        assertFalse(segment0.getMetadata().getHasCreatableBody());
        assertNull(segment0.getMetadata().getCenter().getBody());
        assertEquals(new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStartTime());
        assertEquals(new AbsoluteDate(1996, 11, 30, 1, 28, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStopTime());
        assertEquals(new AbsoluteDate(1996, 11, 28, 22, 8, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStartTime());
        assertEquals(new AbsoluteDate(1996, 11, 30, 1, 18, 2.5555, TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStopTime());
        assertEquals("HERMITE", segment0.getMetadata().getInterpolationMethod());
        assertEquals(7,         segment0.getMetadata().getInterpolationDegree());
        assertFalse(segment0.getMetadata().isFirst());
        assertEquals("EME2000",       segment0.getMetadata().getEndpoints().getFrameA().getName());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1",
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(AttitudeType.QUATERNION, segment1.getMetadata().getAttitudeType());
        assertEquals(AngularDerivativesFilter.USE_R, segment0.getMetadata().getAttitudeType().getAngularDerivativesFilter());
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
        assertEquals(ephemeridesDataLinesComment, segment0.getMetadata().getComments());

        assertEquals("UTC",                      segment1.getMetadata().getTimeSystem().name());
        assertEquals("MARS GLOBAL SURVEYOR",     segment1.getMetadata().getObjectName());
        assertEquals("1996-062A",                segment1.getMetadata().getObjectID());
        assertEquals("MARS BARYCENTER",          segment1.getMetadata().getCenter().getName());
        assertEquals(1996,                       segment1.getMetadata().getLaunchYear());
        assertEquals(62,                         segment1.getMetadata().getLaunchNumber());
        assertEquals("A",                        segment1.getMetadata().getLaunchPiece());
        assertFalse(segment1.getMetadata().getHasCreatableBody());
        assertNull(segment1.getMetadata().getCenter().getBody());
        assertEquals(new AbsoluteDate(1996, 12, 18, 12, 5, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getStartTime());
        assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getStopTime());
        assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getUseableStartTime());
        assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.5555, TimeScalesFactory.getUTC()),
                            segment1.getMetadata().getUseableStopTime());
        assertFalse(segment1.getMetadata().isFirst());
        assertEquals("EME2000",       segment0.getMetadata().getEndpoints().getFrameA().getName());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndpoints().getSpacecraftBodyFrame().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1",
                            segment0.getMetadata().getEndpoints().getSpacecraftBodyFrame().asSpacecraftBodyFrame().getLabel());
        assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(AttitudeType.QUATERNION, segment1.getMetadata().getAttitudeType());
        assertEquals(AngularDerivativesFilter.USE_R, segment0.getMetadata().getAttitudeType().getAngularDerivativesFilter());
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
        assertEquals(ephemeridesDataLinesComment2, segment1.getMetadata().getComments());
    }

    @Test
    void testParseAEM02() throws URISyntaxException {
        final String name = "/ccsds/adm/aem/AEMExample02.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        AemParser parser = new ParserBuilder().
                           withMissionReferenceDate(new AbsoluteDate("1996-12-17T00:00:00.000",
                                                                     TimeScalesFactory.getUTC())).
                           buildAemParser();

        final Aem file = parser.parse(source); // using generic API here
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        assertEquals(metadataComment, segment0.getMetadata().getComments());
        assertEquals("EME2000",       segment0.getMetadata().getEndpoints().getFrameA().getName());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1",
                            segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        List<AemSegment> blocks = file.getSegments();
        assertEquals(1, blocks.size());
        assertEquals(IERSConventions.IERS_2010, parser.getConventions());
        assertTrue(parser.isSimpleEOP());
        assertEquals(0.0, parser.getMissionReferenceDate().durationFrom(new AbsoluteDate(1996, 12, 17, 0, 0, 0.0, TimeScalesFactory.getUTC())), 1.0e-5);
        assertEquals(DataContext.getDefault(), parser.getDataContext());
        assertEquals((new AbsoluteDate("1996-12-17T00:00:00.000",
                                              TimeScalesFactory.getUTC())),
                            parser.getMissionReferenceDate());
    }

    @Test
    void testParseKvnAEM03() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample03.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        validateAEM03(new ParserBuilder().buildAemParser().parseMessage(source));
    }

    @Test
    void testParseXmlAEM03() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample03.xml";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        validateAEM03(parser.parse(source));
    }

    private void validateAEM03(final Aem file) {

        final TimeScale utc = TimeScalesFactory.getUTC();
        assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-15);
        assertEquals(0,
                            file.getHeader().getCreationDate().durationFrom(new AbsoluteDate("2008-071T17:09:49", utc)),
                            1.0e-12);
        assertEquals("GSFC FDF", file.getHeader().getOriginator());
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        assertEquals("ST5-224", segment0.getMetadata().getObjectName());
        assertEquals("2006224", segment0.getMetadata().getObjectID());
        assertEquals("J2000",   segment0.getMetadata().getEndpoints().getFrameA().getName());
        assertEquals("SC_BODY_1", segment0.getMetadata().getEndpoints().getFrameB().getName());
        assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(TimeSystem.UTC, segment0.getMetadata().getTimeSystem());
        assertEquals(0,
                            segment0.getMetadata().getStartTime().durationFrom(new AbsoluteDate("2006-090T05:00:00.071", utc)),
                            1.0e-12);
        assertEquals(0,
                            segment0.getMetadata().getUseableStartTime().durationFrom(new AbsoluteDate("2006-090T05:00:00.071", utc)),
                            1.0e-12);
        assertEquals(0,
                            segment0.getMetadata().getUseableStopTime().durationFrom(new AbsoluteDate("2006-090T05:00:00.946", utc)),
                            1.0e-12);
        assertEquals(0,
                            segment0.getMetadata().getStopTime().durationFrom(new AbsoluteDate("2006-090T05:00:00.946", utc)),
                            1.0e-12);
        assertEquals(AttitudeType.SPIN, segment0.getMetadata().getAttitudeType());
        assertEquals(1, segment0.getData().getComments().size());
        assertEquals("Spin KF ground solution, SPINKF rates", segment0.getData().getComments().get(0));
        assertEquals(8, segment0.getData().getAngularCoordinates().size());
        TimeStampedAngularCoordinates prev = null;
        for (TimeStampedAngularCoordinates tac : segment0.getData().getAngularCoordinates()) {
            if (prev != null) {
                double dt = tac.getDate().durationFrom(prev.getDate());
                double dR = Rotation.distance(tac.getRotation(), prev.getRotation());
                double meanRate = 0.5 * (prev.getRotationRate().getNorm() + tac.getRotationRate().getNorm());
                assertEquals(dR, dt * meanRate, 1.3e-3);
            }
            prev = tac;
        }

    }

    @Test
    void testParseAEM04() throws URISyntaxException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final String ex = "/ccsds/adm/aem/AEMExample04.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final Aem file = parser.parseMessage(source);
        assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-15);
        assertEquals(0,
                            file.getHeader().getCreationDate().durationFrom(new AbsoluteDate("2021-04-13T08:41:42", utc)),
                            1.0e-12);
        assertEquals("CS GROUP", file.getHeader().getOriginator());
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        assertEquals("COPIHUE",   segment0.getMetadata().getObjectName());
        assertEquals("2100-017F", segment0.getMetadata().getObjectID());
        assertEquals(2100,        segment0.getMetadata().getLaunchYear());
        assertEquals(17,          segment0.getMetadata().getLaunchNumber());
        assertEquals("F",         segment0.getMetadata().getLaunchPiece());
        assertEquals("EME2000",   segment0.getMetadata().getEndpoints().getFrameA().getName());
        assertEquals("SC_BODY_1", segment0.getMetadata().getEndpoints().getFrameB().getName());
        assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(TimeSystem.UTC, segment0.getMetadata().getTimeSystem());
        assertEquals(0,
                            segment0.getMetadata().getStartTime().durationFrom(new AbsoluteDate("2021-12-31T00:00:00.000", utc)),
                            1.0e-12);
        assertEquals(0,
                            segment0.getMetadata().getUseableStartTime().durationFrom(new AbsoluteDate("2021-12-31T00:00:00.500", utc)),
                            1.0e-12);
        assertEquals(0,
                            segment0.getMetadata().getUseableStopTime().durationFrom(new AbsoluteDate("2021-12-31T00:00:05.500", utc)),
                            1.0e-12);
        assertEquals(0,
                            segment0.getMetadata().getStopTime().durationFrom(new AbsoluteDate("2021-12-31T00:00:06.000", utc)),
                            1.0e-12);
        assertEquals(AttitudeType.QUATERNION_DERIVATIVE, segment0.getMetadata().getAttitudeType());
        assertEquals("HERMITE", segment0.getMetadata().getInterpolationMethod());
        assertEquals(3, segment0.getMetadata().getInterpolationDegree());
        assertEquals(1, segment0.getData().getComments().size());
        assertEquals(13, segment0.getData().getAngularCoordinates().size());
        TimeStampedAngularCoordinates prev = null;
        for (TimeStampedAngularCoordinates tac : segment0.getData().getAngularCoordinates()) {
            if (prev != null) {
                double dt = tac.getDate().durationFrom(prev.getDate());
                double dR = Rotation.distance(tac.getRotation(), prev.getRotation());
                double meanRate = 0.5 * (prev.getRotationRate().getNorm() + tac.getRotationRate().getNorm());
                assertEquals(dR, dt * meanRate, 1.5e-6);
            }
            prev = tac;
        }

    }

    @Test
    void testParseAEM05() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample05.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final Aem file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        assertEquals(metadataComment,        segment0.getMetadata().getComments());
        assertEquals("UTC",                  segment0.getMetadata().getTimeSystem().name());
        assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenter().getName());
        assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        assertEquals(RotationOrder.ZXY,      segment0.getMetadata().getEulerRotSeq());
        assertTrue(segment0.getMetadata().rateFrameIsA());
        assertFalse(segment0.getMetadata().getHasCreatableBody());
        assertNull(segment0.getMetadata().getCenter().getBody());

        // Reference values
        final AbsoluteDate refDate = new AbsoluteDate(1996, 11, 28, 21, 29, 7.2555, TimeScalesFactory.getUTC());

        // Computed angular coordinates
        final TimeStampedAngularCoordinates ac = segment0.getData().getAngularCoordinates().get(0);
        final FieldRotation<UnivariateDerivative1> r = ac.toUnivariateDerivative1Rotation();
        final UnivariateDerivative1[] angles = r.getAngles(segment0.getMetadata().getEulerRotSeq(),
                                                           RotationConvention.FRAME_TRANSFORM);
        assertEquals(0.0,     refDate.durationFrom(ac.getDate()),                 1.0e-5);
        assertEquals(0.0,     ac.getRotationAcceleration().getNorm(),             1.0e-5);
        assertEquals(-26.78,  FastMath.toDegrees(angles[0].getValue()),           1.0e-2);
        assertEquals(46.26,   FastMath.toDegrees(angles[1].getValue()),           1.0e-2);
        assertEquals(144.10,  FastMath.toDegrees(angles[2].getValue()),           1.0e-2);
        assertEquals(0.10450, FastMath.toDegrees(angles[0].getFirstDerivative()), 1.0e-5);
        assertEquals(0.03214, FastMath.toDegrees(angles[1].getFirstDerivative()), 1.0e-5);
        assertEquals(0.02156, FastMath.toDegrees(angles[2].getFirstDerivative()), 1.0e-5);
    }

    @Test
    void testParseAEM06a() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample06a.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();

        final Aem file = parser.parseMessage(source);
        final TimeStampedAngularCoordinates ac = file.getSegments().get(0).getAngularCoordinates().get(7);
        final Vector3D lastSpin = ac.getRotation().applyInverseTo(Vector3D.PLUS_K);
        assertEquals(268.45119, FastMath.toDegrees(MathUtils.normalizeAngle(lastSpin.getAlpha(), FastMath.PI)), 1.0e-5);
        assertEquals(68.317275, FastMath.toDegrees(lastSpin.getDelta()), 1.0e-5);
    }

    @Test
    void testParseAEM06b() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample06b.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();

        final Aem file = parser.parseMessage(source);
        final TimeStampedAngularCoordinates ac = file.getSegments().get(0).getAngularCoordinates().get(7);
        final Vector3D lastSpin = ac.getRotation().applyInverseTo(Vector3D.PLUS_K);
        assertEquals(268.45119, FastMath.toDegrees(MathUtils.normalizeAngle(lastSpin.getAlpha(), FastMath.PI)), 1.0e-5);
        assertEquals(68.317275, FastMath.toDegrees(lastSpin.getDelta()), 1.0e-5);
    }

    @Test
    void testParseAEM07() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample07.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final Aem file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 2002 OCT 04.");
        metadataComment.add("It is to be used for attitude reconstruction only. The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        assertEquals(metadataComment,        segment0.getMetadata().getComments());
        assertEquals(TimeSystem.UTC,         segment0.getMetadata().getTimeSystem());
        assertEquals("MARS GLOBAL SURVEYOR", segment0.getMetadata().getObjectName());
        assertEquals("1996-062A",            segment0.getMetadata().getObjectID());
        assertEquals("MARS BARYCENTER",      segment0.getMetadata().getCenter().getName());
        assertEquals(1996,                   segment0.getMetadata().getLaunchYear());
        assertEquals(62,                     segment0.getMetadata().getLaunchNumber());
        assertEquals("A",                    segment0.getMetadata().getLaunchPiece());
        assertFalse(segment0.getMetadata().getHasCreatableBody());
        assertNull(segment0.getMetadata().getCenter().getBody());
        assertEquals(CelestialBodyFrame.EME2000, segment0.getMetadata().getEndpoints().getFrameA().asCelestialBodyFrame());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY, segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1", segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStartTime());
        assertEquals(new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStartTime());
        assertEquals(new AbsoluteDate("2002-12-18T12:02:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getUseableStopTime());
        assertEquals(new AbsoluteDate("2002-12-18T12:02:00.331", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStopTime());
        assertEquals(AttitudeType.QUATERNION, segment0.getMetadata().getAttitudeType());
        assertFalse(segment0.getMetadata().isFirst());
        assertEquals("HERMITE", segment0.getMetadata().getInterpolationMethod());
        assertEquals(7, segment0.getMetadata().getInterpolationDegree());

        final AbsoluteDate refDate = new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC());

        assertEquals(3, segment0.getData().getAngularCoordinates().size());
        final TimeStampedAngularCoordinates ac0 = segment0.getData().getAngularCoordinates().get(0);
        assertEquals(0.0, ac0.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(0.68427, 0.56748, 0.03146, 0.45689, true),
                                              ac0.getRotation()),
                            1.0e-10);
        final TimeStampedAngularCoordinates ac1 = segment0.getData().getAngularCoordinates().get(1);
        assertEquals(60.0, ac1.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(0.74533, 0.42319, -0.45697, 0.23784, true),
                                              ac1.getRotation()),
                            1.0e-10);
        final TimeStampedAngularCoordinates ac2 = segment0.getData().getAngularCoordinates().get(2);
        assertEquals(120.0, ac2.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(0.45652, -0.84532, 0.26974, -0.06532, true),
                                              ac2.getRotation()),
                            1.0e-10);

    }

    @Test
    void testParseAEM11() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample11.xml";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final Aem file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("This example shows an AEM with a rotation");
        assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("The relative accuracy of these");
        metadataComment.add("attitudes is 0.1 degrees per axis.");
        assertEquals(metadataComment, segment0.getMetadata().getComments());
        assertEquals(TimeSystem.UTC,  segment0.getMetadata().getTimeSystem());
        assertEquals("FICTITIOUS",    segment0.getMetadata().getObjectName());
        assertEquals("2020-224A",     segment0.getMetadata().getObjectID());
        assertEquals("EARTH",         segment0.getMetadata().getCenter().getName());
        assertEquals(CelestialBodyFrame.J2000, segment0.getMetadata().getEndpoints().getFrameA().asCelestialBodyFrame());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY, segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1", segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        assertTrue(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(new AbsoluteDate("2020-090T05:00:00.071", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStartTime());
        assertEquals(new AbsoluteDate("2020-090T05:00:00.946", TimeScalesFactory.getUTC()),
                            segment0.getMetadata().getStopTime());
        assertEquals(AttitudeType.EULER_ANGLE_DERIVATIVE, segment0.getMetadata().getAttitudeType());

        final AbsoluteDate refDate = new AbsoluteDate("2020-090T05:00:00.071", TimeScalesFactory.getUTC());

        assertEquals(2, segment0.getData().getAngularCoordinates().size());
        final TimeStampedAngularCoordinates ac0 = segment0.getData().getAngularCoordinates().get(0);
        assertEquals(0.0, ac0.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM,
                                                           FastMath.toRadians(45),
                                                           FastMath.toRadians(0.9),
                                                           FastMath.toRadians(15)),
                                              ac0.getRotation()),
                            1.0e-10);
        final TimeStampedAngularCoordinates ac1 = segment0.getData().getAngularCoordinates().get(1);
        assertEquals(0.875, ac1.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM,
                                                           FastMath.toRadians(50),
                                                           FastMath.toRadians(1.9),
                                                           FastMath.toRadians(1.5)),
                                              ac1.getRotation()),
                            1.0e-10);

    }

    @Test
    void testParseAEM13() throws URISyntaxException {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final String ex = "/ccsds/adm/aem/AEMExample13.xml";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final Aem file = parser.parseMessage(source);
        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        assertEquals(TimeSystem.TAI,          segment0.getMetadata().getTimeSystem());
        assertEquals("OREKIT SAT",            segment0.getMetadata().getObjectName());
        assertEquals("2020-012A",             segment0.getMetadata().getObjectID());
        assertEquals(OrbitRelativeFrame.LVLH, segment0.getMetadata().getEndpoints().getFrameA().asOrbitRelativeFrame());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.IMU_FRAME, segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1", segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        assertFalse(segment0.getMetadata().getEndpoints().isA2b());
        assertEquals(new AbsoluteDate("2021-04-15T13:31:20.000", tai), segment0.getMetadata().getStartTime());
        assertEquals(new AbsoluteDate("2021-04-15T13:31:23.000", tai), segment0.getMetadata().getStopTime());
        assertEquals(AttitudeType.QUATERNION_DERIVATIVE, segment0.getMetadata().getAttitudeType());
        assertTrue(segment0.getMetadata().isFirst());

        final AbsoluteDate refDate = new AbsoluteDate("2021-04-15T13:31:20.000", tai);

        assertEquals(7, segment0.getData().getAngularCoordinates().size());
        final TimeStampedAngularCoordinates ac0 = segment0.getData().getAngularCoordinates().get(0);
        assertEquals(0.0, ac0.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(-0.488615, -0.402157,  0.581628,  0.511111, true),
                                              ac0.getRotation()),
                            1.0e-10);
        final TimeStampedAngularCoordinates ac1 = segment0.getData().getAngularCoordinates().get(1);
        assertEquals(0.5, ac1.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(-0.488765, -0.402027,  0.581486,  0.511231, true),
                                              ac1.getRotation()),
                            1.0e-10);
        final TimeStampedAngularCoordinates ac2 = segment0.getData().getAngularCoordinates().get(2);
        assertEquals(1.0, ac2.getDate().durationFrom(refDate), 1.0e-5);
        assertEquals(0.0,
                            Rotation.distance(new Rotation(-0.488916, -0.401898,  0.581344,  0.511350, true),
                                              ac2.getRotation()),
                            1.0e-10);

        final CircularOrbit o = new CircularOrbit(6992992, -5e-04, 1.2e-03,
                                                  FastMath.toRadians(97.83), FastMath.toRadians(80.95),
                                                  FastMath.toRadians(179.86), PositionAngleType.MEAN,
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate("2021-04-15T13:31:22.000", tai),
                                                  Constants.EIGEN5C_EARTH_MU);
        final FieldCircularOrbit<Binary64> fo =
                        new FieldCircularOrbit<>(new Binary64(o.getA()),
                                                 new Binary64(o.getCircularEx()), new Binary64(o.getCircularEy()),
                                                 new Binary64(o.getI()), new Binary64(o.getRightAscensionOfAscendingNode()),
                                                 new Binary64(o.getAlphaM()), PositionAngleType.MEAN,
                                                 o.getFrame(), new FieldAbsoluteDate<>(Binary64Field.getInstance(), o.getDate()),
                                                 new Binary64(o.getMu()));
        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("2020-012A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();
        Attitude                  a = provider.getAttitude(o, o.getDate(), o.getFrame());
        FieldAttitude<Binary64> fa = provider.getAttitude(fo, fo.getDate(), fo.getFrame());
        assertEquals(a.getRotation().getQ0(), fa.getRotation().getQ0().getReal(), 0.00001);
        assertEquals(a.getRotation().getQ1(), fa.getRotation().getQ1().getReal(), 0.00001);
        assertEquals(a.getRotation().getQ2(), fa.getRotation().getQ2().getReal(), 0.00001);
        assertEquals(a.getRotation().getQ3(), fa.getRotation().getQ3().getReal(), 0.00001);

    }

    @Test
    void testParseAEM14() throws URISyntaxException {
        final TimeScale tai = TimeScalesFactory.getTAI();
        final String ex = "/ccsds/adm/aem/AEMExample14.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemParser parser  = new ParserBuilder().buildAemParser();
        final Aem file = parser.parseMessage(source);

        final Segment<AemMetadata, AemData> segment0 = file.getSegments().get(0);
        assertEquals(TimeSystem.TAI,          segment0.getMetadata().getTimeSystem());
        assertEquals("MMS",                   segment0.getMetadata().getObjectName());
        assertEquals("2015-011A",             segment0.getMetadata().getObjectID());
        assertEquals("EME2000",               segment0.getMetadata().getEndpoints().getFrameA().getName());
        assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY, segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        assertEquals("1", segment0.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        assertEquals(new AbsoluteDate("2023-01-01T00:00:00.000", tai), segment0.getMetadata().getStartTime());
        assertEquals(new AbsoluteDate("2023-01-01T00:04:30.000", tai), segment0.getMetadata().getStopTime());
        assertEquals(AttitudeType.EULER_ANGLE_DERIVATIVE, segment0.getMetadata().getAttitudeType());
        assertEquals(RotationOrder.ZXZ, segment0.getMetadata().getEulerRotSeq());
        assertEquals(10, segment0.getData().getAngularCoordinates().size());

        assertEquals(AttitudeType.SPIN_NUTATION_MOMENTUM,
                                file.getSegments().get(1).getMetadata().getAttitudeType());
        assertEquals(AttitudeType.QUATERNION,
                                file.getSegments().get(2).getMetadata().getAttitudeType());
        assertEquals(AttitudeType.QUATERNION_ANGVEL,
                                file.getSegments().get(3).getMetadata().getAttitudeType());
        assertEquals(AttitudeType.EULER_ANGLE_ANGVEL,
                                file.getSegments().get(4).getMetadata().getAttitudeType());

    }

    @Test
    void testWrongNDMType() {
        final String name = "/ccsds/odm/opm/OPMExample1.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/adm/aem/AEMExample01.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        final DataSource source =  new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    void testMissingAttitudeType() {
        try {
            final String name = "/ccsds/adm/aem/AEM-missing-attitude-type.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            assertEquals(AemMetadataKey.ATTITUDE_TYPE.name(), oe.getParts()[0]);
        }
    }

    @Test
    void testInconsistentDirection() {
        try {
            final String name = "/ccsds/adm/aem/AEM-inconsistent-direction.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION, oe.getSpecifier());
            assertEquals(AemMetadataKey.ATTITUDE_DIR, oe.getParts()[0]);
            assertEquals(2.0, ((Double) oe.getParts()[1]).doubleValue(), 1.0e-15);
        }
    }

    @Test
    void testInconsistentQuaternionType() {
        try {
            final String name = "/ccsds/adm/aem/AEM-inconsistent-quaternion-type.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION, oe.getSpecifier());
            assertEquals(AemMetadataKey.QUATERNION_TYPE, oe.getParts()[0]);
            assertEquals(2.0, ((Double) oe.getParts()[1]).doubleValue(), 1.0e-15);
        }
    }

    @Test
    void testLowerCaseValue() {
        //setup
        String file = "/ccsds/adm/aem/aemLowerCaseValue.aem";
        final DataSource source = new DataSource(file, () -> getClass().getResourceAsStream(file));

        //action
        Aem actual = new ParserBuilder().buildAemParser().parseMessage(source);

        //verify
        assertEquals(
                CelestialBodyFactory.getEarth(),
                actual.getSegments().get(0).getMetadata().getCenter().getBody());
    }

    @Test
    void testWrongFile() {
        final String name = "/ccsds/odm/opm/OPMExample1.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    void testWrongKeyword()
            throws URISyntaxException {
        // simple test for AEM file, contains a wrong keyword in the metadata.
        final String name = "/ccsds/adm/aem/AEM-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(24, ((Integer) oe.getParts()[0]).intValue());
            assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    void testEphemerisNumberFormatErrorType() {
        final String name = "/ccsds/adm/aem/AEM-ephemeris-number-format-error.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            assertEquals(28, oe.getParts()[0]);
            assertEquals(name, oe.getParts()[1]);
            assertEquals("1996-11-28T22:08:03.5555 0.42319   this-is-not-a-number  0.23784   0.74533", oe.getParts()[2]);
        }
    }

    @Test
    void testKeywordWithinEphemeris()
            throws URISyntaxException {
        // simple test for AEM file, contains p/v entries and other mandatory data.
        final String name = "/ccsds/adm/aem/AEM-keyword-within-ephemeris.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(29, ((Integer) oe.getParts()[0]).intValue());
            assertTrue(((String) oe.getParts()[2]).startsWith("USER_DEFINED_TEST_KEY"));
        }
    }

    @Test
    void testWrongRotationSequence() throws URISyntaxException {
        // simple test for AEM file, contains a wrong keyword in the metadata.
        final String name = "/ccsds/adm/aem/AEM-inconsistent-rotation-sequence.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_INVALID_ROTATION_SEQUENCE, oe.getSpecifier());
            assertEquals("7051995", oe.getParts()[0]);
            assertEquals(22, ((Integer) oe.getParts()[1]).intValue());
            assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    void testSpuriousMetaDataSection() throws URISyntaxException {
        final String name = "/ccsds/adm/aem/spurious-metadata.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAemParser().parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(26, ((Integer) oe.getParts()[0]).intValue());
            assertEquals("META", oe.getParts()[2]);
        }
    }

    @Test
    void testMissingConvention() throws URISyntaxException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().buildAemParser().parseMessage(source);
        try {
            file.getConventions();
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS, oe.getSpecifier());
        }
    }

    private void verifyAngularCoordinates(final TimeStampedAngularCoordinates expected,
                                          final TimeStampedAngularCoordinates actual,
                                          final double threshold) {
        // Verify date
        assertEquals(0.0, expected.getDate().durationFrom(actual.getDate()), threshold);

        // Verify Angular elements
        assertEquals(expected.getRotation().getQ0(), actual.getRotation().getQ0(), threshold);
        assertEquals(expected.getRotation().getQ1(), actual.getRotation().getQ1(), threshold);
        assertEquals(expected.getRotation().getQ2(), actual.getRotation().getQ2(), threshold);
        assertEquals(expected.getRotation().getQ3(), actual.getRotation().getQ3(), threshold);

        assertEquals(0.0, expected.getRotationRate().distance(actual.getRotationRate()), threshold);
    }

    /**
     * Check if the parser enters the correct interpolation degree
     * (the parsed one or the default if there is none)
     */
    @Test
    void testDefaultInterpolationDegree()
            throws URISyntaxException {

        final String name = "/ccsds/adm/aem/AEMExample01.txt";
        ParserBuilder builder = new ParserBuilder();

        final DataSource source1 = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Aem file = builder.buildAemParser().parseMessage(source1);
        assertEquals(7, file.getSegments().get(0).getMetadata().getInterpolationDegree());
        assertEquals(1, file.getSegments().get(1).getMetadata().getInterpolationDegree());

        final DataSource source2 = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Aem file2 = builder.withDefaultInterpolationDegree(5).buildAemParser().parseMessage(source2);
        assertEquals(7, file2.getSegments().get(0).getMetadata().getInterpolationDegree());
        assertEquals(5, file2.getSegments().get(1).getMetadata().getInterpolationDegree());
    }

    @Test
    void testIssue739() {
        final String ex = "/ccsds/adm/aem/AEMExample08.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().buildAemParser().parseMessage(source);
        final AemSegment segment0 = file.getSegments().get(0);
        assertEquals(CelestialBodyFrame.GTOD, segment0.getMetadata().getEndpoints().getFrameB().asCelestialBodyFrame());

        final BoundedAttitudeProvider provider = segment0.getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        assertEquals(0.0, provider.getMinDate().durationFrom(segment0.getMetadata().getStart()), 0.0001);
        assertEquals(0.0, provider.getMaxDate().durationFrom(segment0.getMetadata().getStop()), 0.0001);

    }

    @Test
    void testIssue739_2() {
        final String ex = "/ccsds/adm/aem/AEMExample09.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().buildAemParser().parseMessage(source);
        final AemSegment segment0 = file.getSegments().get(0);
        assertEquals(FramesFactory.getITRF(ITRFVersion.ITRF_1993, IERSConventions.IERS_2010, true),
                            segment0.getMetadata().getEndpoints().getFrameA().asFrame());

        final BoundedAttitudeProvider provider = segment0.getAttitudeProvider();
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:03.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        assertEquals(0.42319,  rotation.getQ1(), 0.0001);
        assertEquals(-0.45697, rotation.getQ2(), 0.0001);
        assertEquals(0.23784,  rotation.getQ3(), 0.0001);
        assertEquals(0.74533,  rotation.getQ0(), 0.0001);
        assertEquals(0.0, provider.getMinDate().durationFrom(segment0.getMetadata().getStart()), 0.0001);
        assertEquals(0.0, provider.getMaxDate().durationFrom(segment0.getMetadata().getStop()), 0.0001);

    }

}
