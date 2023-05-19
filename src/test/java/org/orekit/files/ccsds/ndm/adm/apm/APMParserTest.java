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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame.BaseEquipment;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

public class APMParserTest {

    private static final double QUATERNION_PRECISION = 1e-5;
    private static final double ANGLE_PRECISION = 1e-4;
    private static final double SPACECRAFT_PRECISION = 0.1;
    private static final double MANEUVER_PRECISION = 1.0e-2;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseAPM1() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample01.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());

        // Check Metadata Block
        Assertions.assertEquals("TRMM",       file.getMetadata().getObjectName());
        Assertions.assertEquals("1997-009A",  file.getMetadata().getObjectID());
        Assertions.assertEquals(1997,         file.getMetadata().getLaunchYear());
        Assertions.assertEquals(9,            file.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          file.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      file.getMetadata().getCenter().getName());
        Assertions.assertTrue(file.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), file.getMetadata().getCenter().getBody());
        Assertions.assertEquals("UTC",        file.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(file.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            file.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", file.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, file.getData().getQuaternionBlock().getEndpoints().getFrameB().asCelestialBodyFrame());
        Assertions.assertTrue(file.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getData().getEpoch());
        Assertions.assertEquals(0.25678, file.getData().getQuaternionBlock().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.00005, file.getData().getQuaternionBlock().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.87543, file.getData().getQuaternionBlock().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.40949, file.getData().getQuaternionBlock().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assertions.assertFalse(file.getData().getQuaternionBlock().hasRates());
        Assertions.assertTrue(Double.isNaN(file.getData().getQuaternionBlock().getQuaternionDot().getQ1()));
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            file.getAttitude(null, null).getDate());
        Assertions.assertEquals(0.0, file.getAttitude(null, null).getSpin().getNorm(), 1.0e-15);

        Attitude attitude = file.getAttitude(null, null);
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172, TimeScalesFactory.getUTC()),
                            attitude.getDate());
        Assertions.assertEquals("ITRF-1997/CIO/2010-based ITRF simple EOP", attitude.getReferenceFrame().getName());
        Assertions.assertEquals(2 * FastMath.atan(FastMath.sqrt(0.00005 * 0.00005 + 0.87543 * 0.87543 + 0.40949 * 0.40949) / 0.25678),
                            attitude.getRotation().getAngle(), 1.0e-15);
        Assertions.assertEquals(0, attitude.getSpin().getNorm(), 1.0e-15);

    }

    @Test
    public void testParseAPM2KVN() {
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();
        final String name = "/ccsds/adm/apm/APMExample02.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        validateAPM2(parser.parseMessage(source));
    }

    @Test
    public void testParseAPM2XML() {
        final ApmParser parser = new ParserBuilder().
                        withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                  TimeScalesFactory.getUTC())).
                        buildApmParser();
        final String name = "/ccsds/adm/apm/APMExample02.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        validateAPM2(parser.parseMessage(source));
    }

    @Test
    public void testWriteApm2() throws IOException {
        final ApmParser parser = new ParserBuilder().
                        withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                  TimeScalesFactory.getUTC())).
                        buildApmParser();
        final String name = "/ccsds/adm/apm/APMExample02.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Apm original = parser.parseMessage(source);

        // write the parsed file back to a characters array
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, ApmWriter.KVN_PADDING_WIDTH, "dummy",
                                                     Constants.JULIAN_DAY, 60);
        new WriterBuilder().buildApmWriter().writeMessage(generator, original);

        // reparse the written file
        final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        final Apm    rebuilt = new ParserBuilder().buildApmParser().parseMessage(source2);
        validateAPM2(rebuilt);
    }

    private void validateAPM2(final Apm file) {

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("JPL", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("MARS SPIRIT", segment.getMetadata().getObjectName());
        Assertions.assertEquals("2004-003A",   segment.getMetadata().getObjectID());
        Assertions.assertEquals(2004,          segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(3,             segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",           segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",       segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertTrue(segment.getMetadata().getComments().isEmpty());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("UTC",         segment.getMetadata().getTimeSystem().name());

        // Check general comments
        ArrayList<String> generalComment = new ArrayList<String>();
        generalComment.add("GEOCENTRIC, CARTESIAN, EARTH FIXED");
        generalComment.add("OBJECT ID: 2004-003");
        generalComment.add("$ITIM = 2004 JAN 14 22:26:18.400000, original launch time 14:36");
        generalComment.add("Generated by JPL");
        generalComment.add("Current attitude for orbit 20 and attitude maneuver");
        generalComment.add("planning data.");
        generalComment.add("Attitude state quaternion");
        Assertions.assertEquals(generalComment, segment.getData().getComments());

        // Check data block: QUATERNION
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.INSTRUMENT,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("A", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asCelestialBodyFrame());
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());

        Assertions.assertEquals(0.47832, segment.getData().getQuaternionBlock().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.03123, segment.getData().getQuaternionBlock().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.78543, segment.getData().getQuaternionBlock().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.39158, segment.getData().getQuaternionBlock().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assertions.assertFalse(segment.getData().getQuaternionBlock().hasRates());
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ1()));

        // Check data block: EULER
        ArrayList<String> eulerComment = new ArrayList<String>();
        eulerComment.add("Attitude specified as Euler elements");
        Assertions.assertEquals(eulerComment,    segment.getData().getEulerBlock().getComments());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getEulerBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.INSTRUMENT,  segment.getData().getEulerBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("A",  segment.getData().getEulerBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertTrue(segment.getData().getEulerBlock().getEndpoints().isA2b());
        Assertions.assertTrue(segment.getData().getEulerBlock().rateFrameIsA());
        Assertions.assertEquals(RotationOrder.ZXY, segment.getData().getEulerBlock().getEulerRotSeq());

        Assertions.assertEquals(-53.3688,  FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[0]), ANGLE_PRECISION);
        Assertions.assertEquals(139.7527,  FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[1]), ANGLE_PRECISION);
        Assertions.assertEquals( 25.0658,  FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[2]), ANGLE_PRECISION);
        Assertions.assertEquals(  0.02156, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationRates()[0]),  ANGLE_PRECISION);
        Assertions.assertEquals(  0.1045,  FastMath.toDegrees(segment.getData().getEulerBlock().getRotationRates()[1]),  ANGLE_PRECISION);
        Assertions.assertEquals(  0.03214, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationRates()[2]),  ANGLE_PRECISION);

        // Check data block: SPACECRAFT PARAMETERS
        ArrayList<String> spacecraftComment = new ArrayList<String>();
        spacecraftComment.add("Spacecraft Parameters");
        Assertions.assertEquals(spacecraftComment, segment.getData().getInertiaBlock().getComments());
        Assertions.assertEquals(6080.0,            segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(0, 0), SPACECRAFT_PRECISION);
        Assertions.assertEquals(5245.5,            segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(1, 1), SPACECRAFT_PRECISION);
        Assertions.assertEquals(8067.3,            segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(2, 2), SPACECRAFT_PRECISION);
        Assertions.assertEquals(-135.9,            segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(0, 1), SPACECRAFT_PRECISION);
        Assertions.assertEquals(89.3,              segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(0, 2), SPACECRAFT_PRECISION);
        Assertions.assertEquals(-90.7,             segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(1, 2), SPACECRAFT_PRECISION);

        // Check data block: MANEUVER
        ArrayList<String> maneuverComment = new ArrayList<String>();
        maneuverComment.add("Data follows for 1 planned maneuver.");
        maneuverComment.add("First attitude maneuver for: MARS SPIRIT");
        maneuverComment.add("Impulsive, torque direction fixed in body frame");
        Assertions.assertEquals(maneuverComment, segment.getData().getManeuver(0).getComments());
        Assertions.assertTrue(segment.getData().hasManeuvers());
        Assertions.assertEquals(1, segment.getData().getNbManeuvers());
        Assertions.assertEquals(1, segment.getData().getManeuvers().size());
        Assertions.assertEquals(BaseEquipment.INSTRUMENT, segment.getData().getManeuver(0).getFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("A", segment.getData().getManeuver(0).getFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 29, 0.5098,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getManeuver(0).getEpochStart());
        Assertions.assertEquals(3,     segment.getData().getManeuver(0).getDuration(),      MANEUVER_PRECISION);
        Assertions.assertEquals(-1.25, segment.getData().getManeuver(0).getTorque().getX(), MANEUVER_PRECISION);
        Assertions.assertEquals(-0.5,  segment.getData().getManeuver(0).getTorque().getY(), MANEUVER_PRECISION);
        Assertions.assertEquals(0.5,   segment.getData().getManeuver(0).getTorque().getZ(), MANEUVER_PRECISION);

    }

    @Test
    public void testParseAPM3() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample03.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assertions.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("UTC",        segment.getMetadata().getTimeSystem().name());

        // Check data block: QUATERNION
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asCelestialBodyFrame());
        Assertions.assertFalse(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assertions.assertEquals(0.25678, segment.getData().getQuaternionBlock().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.00005, segment.getData().getQuaternionBlock().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.87543, segment.getData().getQuaternionBlock().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.40949, segment.getData().getQuaternionBlock().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assertions.assertFalse(segment.getData().getQuaternionBlock().hasRates());
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ1()));

        // Check data block: SPIN
        ArrayList<String> spinComment = new ArrayList<String>();
        spinComment.add("SPIN Parameters");
        Assertions.assertEquals(spinComment, segment.getData().getSpinStabilizedBlock().getComments());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertFalse(segment.getData().getSpinStabilizedBlock().getEndpoints().isA2b());
        Assertions.assertEquals(FastMath.toRadians(24.8),   segment.getData().getSpinStabilizedBlock().getSpinAlpha(),      ANGLE_PRECISION);
        Assertions.assertEquals(FastMath.toRadians(33.7),   segment.getData().getSpinStabilizedBlock().getSpinDelta(),      ANGLE_PRECISION);
        Assertions.assertEquals(FastMath.toRadians(42.5),   segment.getData().getSpinStabilizedBlock().getSpinAngle(),      ANGLE_PRECISION);
        Assertions.assertEquals(FastMath.toRadians(-135.9), segment.getData().getSpinStabilizedBlock().getSpinAngleVel(),   ANGLE_PRECISION);
        Assertions.assertEquals(FastMath.toRadians(89.3),   segment.getData().getSpinStabilizedBlock().getNutation(),       ANGLE_PRECISION);
        Assertions.assertEquals(FastMath.toRadians(-90.7),  segment.getData().getSpinStabilizedBlock().getNutationPhase(),  ANGLE_PRECISION);
        Assertions.assertEquals(64.0,                       segment.getData().getSpinStabilizedBlock().getNutationPeriod(), ANGLE_PRECISION);

    }

    @Test
    public void testParseAPM4() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample04.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assertions.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("UTC",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asCelestialBodyFrame());
        Assertions.assertTrue(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assertions.assertEquals(0.25678, segment.getData().getQuaternionBlock().getQuaternion().getQ0(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.00005, segment.getData().getQuaternionBlock().getQuaternion().getQ1(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.87543, segment.getData().getQuaternionBlock().getQuaternion().getQ2(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.40949, segment.getData().getQuaternionBlock().getQuaternion().getQ3(),    QUATERNION_PRECISION);
        Assertions.assertEquals(0.05678, segment.getData().getQuaternionBlock().getQuaternionDot().getQ0(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.00001, segment.getData().getQuaternionBlock().getQuaternionDot().getQ1(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.07543, segment.getData().getQuaternionBlock().getQuaternionDot().getQ2(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.00949, segment.getData().getQuaternionBlock().getQuaternionDot().getQ3(), QUATERNION_PRECISION);
        Assertions.assertEquals(new AbsoluteDate(2003, 9, 30, 14, 28, 15.1172, TimeScalesFactory.getUTC()),
                                segment.getData().getAttitude(null, null).getDate());
        Assertions.assertEquals(8.63363e-2, segment.getData().getAttitude(null, null).getSpin().getNorm(), 1.0e-7);

        Attitude attitude = file.getAttitude(null, null);
        Assertions.assertEquals(segment.getData().getEpoch(), attitude.getDate());
        Assertions.assertEquals(8.63363e-2, attitude.getSpin().getNorm(), 1.0e-7);

    }

    @Test
    public void testParseAPM5() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample05.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assertions.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("UTC",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asCelestialBodyFrame());
        Assertions.assertTrue(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assertions.assertEquals(0.47832, segment.getData().getQuaternionBlock().getQuaternion().getQ0(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.03123, segment.getData().getQuaternionBlock().getQuaternion().getQ1(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.78543, segment.getData().getQuaternionBlock().getQuaternion().getQ2(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.39158, segment.getData().getQuaternionBlock().getQuaternion().getQ3(), QUATERNION_PRECISION);
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ0()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ1()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ2()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ3()));

        Assertions.assertEquals(RotationOrder.ZXY, segment.getData().getEulerBlock().getEulerRotSeq());
        Assertions.assertFalse(segment.getData().getEulerBlock().hasAngles());
        Assertions.assertTrue(Double.isNaN(segment.getData().getEulerBlock().getRotationAngles()[0]));
        Assertions.assertTrue(Double.isNaN(segment.getData().getEulerBlock().getRotationAngles()[1]));
        Assertions.assertTrue(Double.isNaN(segment.getData().getEulerBlock().getRotationAngles()[2]));
        Assertions.assertEquals(0.02156, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationRates()[0]), ANGLE_PRECISION);
        Assertions.assertEquals(0.1045,  FastMath.toDegrees(segment.getData().getEulerBlock().getRotationRates()[1]), ANGLE_PRECISION);
        Assertions.assertEquals(0.03214, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationRates()[2]), ANGLE_PRECISION);

        Attitude attitude = file.getAttitude(null, null);
        Assertions.assertEquals(segment.getData().getEpoch(), attitude.getDate());
        Assertions.assertEquals(2.0137e-3, attitude.getSpin().getNorm(), 1.0e-7);

    }

    @Test
    public void testParseAPM6() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample06.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 19, 23, 57,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("TRMM",       segment.getMetadata().getObjectName());
        Assertions.assertEquals("1997-009A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(1997,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(9,            segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("UTC",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asCelestialBodyFrame());
        Assertions.assertTrue(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2004, 2, 14, 14, 28, 15.1172,
                                             TimeScalesFactory.getUTC()),
                            segment.getData().getEpoch());
        Assertions.assertEquals(0.47832, segment.getData().getQuaternionBlock().getQuaternion().getQ0(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.03123, segment.getData().getQuaternionBlock().getQuaternion().getQ1(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.78543, segment.getData().getQuaternionBlock().getQuaternion().getQ2(), QUATERNION_PRECISION);
        Assertions.assertEquals(0.39158, segment.getData().getQuaternionBlock().getQuaternion().getQ3(), QUATERNION_PRECISION);
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ0()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ1()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ2()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ3()));

        Assertions.assertEquals(RotationOrder.ZXY, segment.getData().getEulerBlock().getEulerRotSeq());
        Assertions.assertEquals(0.02156, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[0]), ANGLE_PRECISION);
        Assertions.assertEquals(0.1045,  FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[1]), ANGLE_PRECISION);
        Assertions.assertEquals(0.03214, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[2]), ANGLE_PRECISION);
        Assertions.assertTrue(Double.isNaN(segment.getData().getEulerBlock().getRotationRates()[0]));
        Assertions.assertTrue(Double.isNaN(segment.getData().getEulerBlock().getRotationRates()[1]));
        Assertions.assertTrue(Double.isNaN(segment.getData().getEulerBlock().getRotationRates()[2]));

        Attitude attitude = file.getAttitude(null, null);
        Assertions.assertEquals(segment.getData().getEpoch(),
                            attitude.getDate());
        Assertions.assertEquals(0.0, attitude.getSpin().getNorm(), 1.0e-15);

    }

    @Test
    public void testParseAPM7() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample07.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);
 
        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2023, 1, 21, 11, 55, 0, TimeScalesFactory.getUTC()),
                                file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());
        Assertions.assertEquals("A000001", file.getHeader().getMessageId());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("SPINNING",   segment.getMetadata().getComments().get(0));
        Assertions.assertEquals("MMS1",       segment.getMetadata().getObjectName());
        Assertions.assertEquals("2015-011A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(2015,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(11,           segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TAI",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(new AbsoluteDate(2023, 1, 1, 0, 0, 0.0,
                                                 TimeScalesFactory.getTAI()),
                                segment.getData().getEpoch());
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(CelestialBodyFrame.J2000, segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(10.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinAlpha()),     ANGLE_PRECISION);
        Assertions.assertEquals(30.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinDelta()),     ANGLE_PRECISION);
        Assertions.assertEquals( 0.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinAngle()),     ANGLE_PRECISION);
        Assertions.assertEquals(80.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getMomentumAlpha()), ANGLE_PRECISION);
        Assertions.assertEquals(10.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getMomentumDelta()), ANGLE_PRECISION);
        Assertions.assertEquals( 0.5, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getNutationVel()),   ANGLE_PRECISION);

        Attitude attitude = file.getAttitude(null, null);
        Assertions.assertEquals(segment.getData().getEpoch(), attitude.getDate());
        Assertions.assertEquals(2.22728e-2, attitude.getSpin().getNorm(), 1.0e-7);

    }

    @Test
    public void testParseAPM8() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample08.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2023, 2, 3, 12, 0, 0, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC",    file.getHeader().getOriginator());
        Assertions.assertEquals("A000002", file.getHeader().getMessageId());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("Rotation From Nadir", segment.getMetadata().getComments().get(0));
        Assertions.assertEquals("LRO",        segment.getMetadata().getObjectName());
        Assertions.assertEquals("2009-031A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(2009,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(31,           segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("MOON",       segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getMoon(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TAI",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(new AbsoluteDate(2023, 1, 1, 0, 0, 0.0, TimeScalesFactory.getTAI()),
                                segment.getData().getEpoch());
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(CelestialBodyFrame.EME2000, segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("", segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals( 0.925417, segment.getData().getQuaternionBlock().getQuaternion().getQ0(), QUATERNION_PRECISION);
        Assertions.assertEquals( 0.171010, segment.getData().getQuaternionBlock().getQuaternion().getQ1(), QUATERNION_PRECISION);
        Assertions.assertEquals(-0.030154, segment.getData().getQuaternionBlock().getQuaternion().getQ2(), QUATERNION_PRECISION);
        Assertions.assertEquals( 0.336824, segment.getData().getQuaternionBlock().getQuaternion().getQ3(), QUATERNION_PRECISION);
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ0()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ1()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ2()));
        Assertions.assertTrue(Double.isNaN(segment.getData().getQuaternionBlock().getQuaternionDot().getQ3()));

        Assertions.assertEquals(CelestialBodyFrame.EME2000, segment.getData().getAngularVelocityBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getAngularVelocityBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("", segment.getData().getAngularVelocityBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals( 0.0001,  FastMath.toDegrees(segment.getData().getAngularVelocityBlock().getAngVelX()), ANGLE_PRECISION);
        Assertions.assertEquals( 0.05,    FastMath.toDegrees(segment.getData().getAngularVelocityBlock().getAngVelY()), ANGLE_PRECISION);
        Assertions.assertEquals( 0.00003, FastMath.toDegrees(segment.getData().getAngularVelocityBlock().getAngVelZ()), ANGLE_PRECISION);

    }

    @Test
    public void testParseAPM9() {

        // File
        final String ex = "/ccsds/adm/apm/APMExample09.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2023, 2, 5, 12, 0, 0, TimeScalesFactory.getUTC()),
                                file.getHeader().getCreationDate());
        Assertions.assertEquals("GSFC", file.getHeader().getOriginator());
        Assertions.assertEquals("A000003", file.getHeader().getMessageId());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        Assertions.assertEquals("SPINNING",   segment.getMetadata().getComments().get(0));
        Assertions.assertEquals("MMS1",       segment.getMetadata().getObjectName());
        Assertions.assertEquals("2015-011A",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(2015,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(11,           segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("EARTH",      segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getEarth(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TAI",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(new AbsoluteDate(2023, 1, 1, 0, 0, 0.0,
                                                 TimeScalesFactory.getTAI()),
                                segment.getData().getEpoch());
        Assertions.assertNull(segment.getData().getQuaternionBlock());

        Assertions.assertEquals(CelestialBodyFrame.EME2000, segment.getData().getEulerBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getEulerBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("", segment.getData().getEulerBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(RotationOrder.ZXZ, segment.getData().getEulerBlock().getEulerRotSeq());
        Assertions.assertEquals(10.0, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[0]), ANGLE_PRECISION);
        Assertions.assertEquals(20.0, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[1]), ANGLE_PRECISION);
        Assertions.assertEquals( 0.0, FastMath.toDegrees(segment.getData().getEulerBlock().getRotationAngles()[2]), ANGLE_PRECISION);

        Assertions.assertEquals(CelestialBodyFrame.EME2000, segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameA().asCelestialBodyFrame());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("", segment.getData().getSpinStabilizedBlock().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(-80.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinAlpha()),      ANGLE_PRECISION);
        Assertions.assertEquals( 70.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinDelta()),      ANGLE_PRECISION);
        Assertions.assertEquals(  0.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinAngle()),      ANGLE_PRECISION);
        Assertions.assertEquals(  1.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getSpinAngleVel()),   ANGLE_PRECISION);
        Assertions.assertEquals(  0.1, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getNutation()),       ANGLE_PRECISION);
        Assertions.assertEquals(720.0, segment.getData().getSpinStabilizedBlock().getNutationPeriod(), 1.0e-10);
        Assertions.assertEquals(-85.0, FastMath.toDegrees(segment.getData().getSpinStabilizedBlock().getNutationPhase()),  ANGLE_PRECISION);

        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getInertiaBlock().getFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("", segment.getData().getInertiaBlock().getFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(1443.10, segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(0, 0), 1.0e-10);
        Assertions.assertEquals(1445.20, segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(1, 1), 1.0e-10);
        Assertions.assertEquals(1760.20, segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(2, 2), 1.0e-10);
        Assertions.assertEquals( -86.40, segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(0, 1), 1.0e-10);
        Assertions.assertEquals(   0.00, segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(0, 2), 1.0e-10);
        Assertions.assertEquals(  -0.09, segment.getData().getInertiaBlock().getInertiaMatrix().getEntry(1, 2), 1.0e-10);

        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                                segment.getData().getManeuver(0).getFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("",      segment.getData().getManeuver(0).getFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(1.0,     segment.getData().getManeuver(0).getDuration(),      1.0e-10);
        Assertions.assertEquals(1.0,     segment.getData().getManeuver(0).getTorque().getX(), 1.0e-10);
        Assertions.assertEquals(0.0,     segment.getData().getManeuver(0).getTorque().getY(), 1.0e-10);
        Assertions.assertEquals(0.0,     segment.getData().getManeuver(0).getTorque().getZ(), 1.0e-10);
        Assertions.assertEquals(-0.001,  segment.getData().getManeuver(0).getDeltaMass(),     1.0e-10);

    }

    @Test
    public void testOrbitRelativeFrameInertial() {

        // File
        final String ex = "/ccsds/adm/apm/APM-orbit-relative-frame-inertial.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2021, 2, 24, 18, 59, 43,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("CS GROUP", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("DUMMY",      segment.getMetadata().getObjectName());
        Assertions.assertEquals("9999-111Z",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(9999,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(111,          segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("Z",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("JUPITER",    segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getJupiter(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TDB",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(OrbitRelativeFrame.VNC_INERTIAL, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asOrbitRelativeFrame());
        Assertions.assertFalse(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2021, 1, 1, 0, 0, 0.0, TimeScalesFactory.getTDB()),
                            segment.getData().getEpoch());

        final PVCoordinates pv = new PVCoordinates(new Vector3D( 1.234e7, -0.567e7, 9.876e6),
                                                   new Vector3D(-0.772e4,  5.002e4, 4.892e2));
        Attitude attitude = file.getAttitude(FramesFactory.getEME2000(),
                                             (date, frame) -> new TimeStampedPVCoordinates(date, pv));
        Vector3D xSat = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D ySat = attitude.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assertions.assertEquals(FastMath.PI, Vector3D.angle(xSat, pv.getVelocity()), 1.0e-10);
        Assertions.assertEquals(0.0,         Vector3D.angle(ySat, pv.getMomentum()), 1.0e-10);
        Assertions.assertEquals(0.0,         attitude.getSpin().getX(), 1.0e-10);
        Assertions.assertEquals(0.0,         attitude.getSpin().getY(), 1.0e-10);
        Assertions.assertEquals(0.0,         attitude.getSpin().getZ(), 1.0e-10);

    }

    @Test
    public void testOrbitRelativeFrameRotating() {

        // File
        final String ex = "/ccsds/adm/apm/APM-orbit-relative-frame-rotating.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2021, 2, 24, 18, 59, 43,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("CS GROUP", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("DUMMY",      segment.getMetadata().getObjectName());
        Assertions.assertEquals("9999-111Z",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(9999,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(111,          segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("Z",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("JUPITER",    segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getJupiter(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TDB",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(OrbitRelativeFrame.VNC_ROTATING, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asOrbitRelativeFrame());
        Assertions.assertFalse(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2021, 1, 1, 0, 0, 0.0, TimeScalesFactory.getTDB()),
                            segment.getData().getEpoch());

        PVCoordinates pv = new PVCoordinates(new Vector3D( 1.234e7, -0.567e7, 9.876e6),
                                             new Vector3D(-0.772e4,  5.002e4, 4.892e2));
        Attitude attitude = file.getAttitude(FramesFactory.getEME2000(),
                                             (date, frame) -> new TimeStampedPVCoordinates(date, pv));
        Vector3D xSat = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D ySat = attitude.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assertions.assertEquals(FastMath.PI, Vector3D.angle(xSat, pv.getVelocity()), 1.0e-10);
        Assertions.assertEquals(0.0,         Vector3D.angle(ySat, pv.getMomentum()), 1.0e-10);
        Assertions.assertEquals(0.0,                               attitude.getSpin().getX(), 1.0e-10);
        Assertions.assertEquals(pv.getAngularVelocity().getNorm(), attitude.getSpin().getY(), 1.0e-10);
        Assertions.assertEquals(0.0,                               attitude.getSpin().getZ(), 1.0e-10);

    }

    @Test
    public void testUnsupportedOrbitRelativeFrame() {

        // File
        final String ex = "/ccsds/adm/apm/APM-unsupported-orbit-relative-frame.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2021, 2, 24, 18, 59, 43,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("CS GROUP", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("DUMMY",      segment.getMetadata().getObjectName());
        Assertions.assertEquals("9999-111Z",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(9999,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(111,          segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("Z",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("JUPITER",    segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getJupiter(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TDB",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY,
                            segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", segment.getData().getQuaternionBlock().getEndpoints().getFrameA().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(OrbitRelativeFrame.SEZ_INERTIAL, segment.getData().getQuaternionBlock().getEndpoints().getFrameB().asOrbitRelativeFrame());
        Assertions.assertTrue(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2021, 1, 1, 0, 0, 0.0, TimeScalesFactory.getTDB()),
                            segment.getData().getEpoch());

        final PVCoordinatesProvider prov = (date, frame) -> new TimeStampedPVCoordinates(date,
                                                                                         new PVCoordinates(new Vector3D( 1.234e7, -0.567e7, 9.876e6),
                                                                                                           new Vector3D(-0.772e4,  5.002e4, 4.892e2)));
        try {
            file.getAttitude(FramesFactory.getEME2000(), prov);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_LOCAL_ORBITAL_FRAME, oe.getSpecifier());
            Assertions.assertEquals(OrbitRelativeFrame.SEZ_INERTIAL.name(), oe.getParts()[0]);
        }

    }

    @Test
    public void testUnknownFrame() {

        // File
        final String ex = "/ccsds/adm/apm/APM-unknown-frame.txt";

        // Initialize the parser
        final ApmParser parser = new ParserBuilder().
                                 withMissionReferenceDate(new AbsoluteDate("2002-09-30T14:28:15.117",
                                                                           TimeScalesFactory.getUTC())).
                                 buildApmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated APM file
        final Apm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2021, 2, 24, 18, 59, 43,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("CS GROUP", file.getHeader().getOriginator());

        Segment<AdmMetadata, ApmData> segment = file.getSegments().get(0);

        // Check Metadata Block
        Assertions.assertEquals("DUMMY",      segment.getMetadata().getObjectName());
        Assertions.assertEquals("9999-111Z",  segment.getMetadata().getObjectID());
        Assertions.assertEquals(9999,         segment.getMetadata().getLaunchYear());
        Assertions.assertEquals(111,          segment.getMetadata().getLaunchNumber());
        Assertions.assertEquals("Z",          segment.getMetadata().getLaunchPiece());
        Assertions.assertEquals("JUPITER",    segment.getMetadata().getCenter().getName());
        Assertions.assertTrue(segment.getMetadata().getHasCreatableBody());
        Assertions.assertEquals(CelestialBodyFactory.getJupiter(), segment.getMetadata().getCenter().getBody());
        Assertions.assertEquals("TDB",        segment.getMetadata().getTimeSystem().name());

        // Check data block
        Assertions.assertFalse(segment.getData().hasManeuvers());
        FrameFacade sb = segment.getData().getQuaternionBlock().getEndpoints().getSpacecraftBodyFrame();
        Assertions.assertEquals(SpacecraftBodyFrame.BaseEquipment.SC_BODY, sb.asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", sb.asSpacecraftBodyFrame().getLabel());
        FrameFacade ext = segment.getData().getQuaternionBlock().getEndpoints().getExternalFrame();
        Assertions.assertNull(ext.asFrame());
        Assertions.assertNull(ext.asCelestialBodyFrame());
        Assertions.assertNull(ext.asOrbitRelativeFrame());
        Assertions.assertNull(ext.asSpacecraftBodyFrame());
        Assertions.assertEquals("UNKNOWN", ext.getName());
        Assertions.assertFalse(segment.getData().getQuaternionBlock().getEndpoints().isA2b());
        Assertions.assertEquals(new AbsoluteDate(2021, 1, 1, 0, 0, 0.0, TimeScalesFactory.getTDB()),
                            segment.getData().getEpoch());

        try {
            file.getAttitude(FramesFactory.getEME2000(),
                             (date, frame) -> new TimeStampedPVCoordinates(date,
                                                                           new PVCoordinates(new Vector3D( 1.234e7, -0.567e7, 9.876e6),
                                                                                             new Vector3D(-0.772e4,  5.002e4, 4.892e2))));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            Assertions.assertEquals(ext.getName(), oe.getParts()[0]);
        }

    }

    @Test
    public void testAttitudeQuaternion() {
        doTestAttitude("/ccsds/adm/apm/APM-quaternion.xml");
    }

    @Test
    public void testAtitudeQuaternionRates() {
        doTestAttitude("/ccsds/adm/apm/APM-quaternion-rates.xml");
    }

    @Test
    public void testAtitudeQuaternionAngvel() {
        doTestAttitude("/ccsds/adm/apm/APM-quaternion-angvel.xml");
    }

    @Test
    public void testAtitudeQuaternionEulerAnglesRates() {
        doTestAttitude("/ccsds/adm/apm/APM-quaternion-euler-angles-rates.xml");
    }

    @Test
    public void testAtitudeQuaternionEulerRates() {
        doTestAttitude("/ccsds/adm/apm/APM-quaternion-euler-rates.xml");
    }

    @Test
    public void testAtitudeEulerAnglesRates() {
        doTestAttitude("/ccsds/adm/apm/APM-euler-angles-rates.xml");
    }

    @Test
    public void testAtitudeEulerAnglesAngvel() {
        doTestAttitude("/ccsds/adm/apm/APM-euler-angles-angvel.xml");
    }

    @Test
    public void testAtitudeEulerAngles() {
        doTestAttitude("/ccsds/adm/apm/APM-euler-angles.xml");
    }

    @Test
    public void testAtitudeSpin() {
        doTestAttitude("/ccsds/adm/apm/APM-spin.xml");
    }

    @Test
    public void testAtitudeSpinNutation() {
        doTestAttitude("/ccsds/adm/apm/APM-spin-nutation.xml");
    }

    @Test
    public void testAtitudeSpinNutationMomentum() {
        doTestAttitude("/ccsds/adm/apm/APM-spin-nutation-momentum.xml");
    }

    private void doTestAttitude(final String name) {
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Apm apm = new ParserBuilder().
                        withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
                        buildApmParser().
                        parseMessage(source);
        final Attitude attitude = apm.getAttitude(FramesFactory.getEME2000(), null);
        Assertions.assertEquals(0.0,
                                attitude.getDate().durationFrom(new AbsoluteDate(2008, 4, 7, TimeScalesFactory.getUTC())),
                                1.0e-15);
        final double sign = FastMath.copySign(1.0, attitude.getRotation().getQ0());
        Assertions.assertEquals( 0.6184633325084984,  sign * attitude.getRotation().getQ0(), 1.0e-15);
        Assertions.assertEquals(-0.6447327809733585,  sign * attitude.getRotation().getQ1(), 1.0e-15);
        Assertions.assertEquals(-0.3463409538535587,  sign * attitude.getRotation().getQ2(), 1.0e-15);
        Assertions.assertEquals(-0.28613054916357517, sign * attitude.getRotation().getQ3(), 1.0e-15);

    }

    @Test
    public void testNoLogicalBlocks() {
        try {
            final String name = "/ccsds/adm/apm/APM-no-logical-blocks.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INCOMPLETE_DATA, oe.getSpecifier());
        }
    }

    @Test
    public void testNoAttitude() {
        final String name = "/ccsds/adm/apm/APM-no-attitude.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Apm apm = new ParserBuilder().
                        withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
                        buildApmParser().
                        parseMessage(source);
        try {
            apm.getAttitude(FramesFactory.getGCRF(), null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INCOMPLETE_DATA, oe.getSpecifier());
        }
    }

    @Test
    public void testNotImplementedTimeSystems() {
        try {
            final String name = "/ccsds/adm/apm/APM-inconsistent-time-systems.txt";
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assertions.assertEquals("BCE", oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongADMType() {
        final String name = "/ccsds/adm/aem/AEMExample01.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assertions.assertEquals(name, oe.getParts()[0]);
        }
    }

    @Test
    public void testNumberFormatErrorType() {
        final String name = "/ccsds/adm/apm/APM-number-format-error.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("Q1", oe.getParts()[0]);
            Assertions.assertEquals(22, oe.getParts()[1]);
            Assertions.assertEquals(name, oe.getParts()[2]);
        }
    }


    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/adm/apm/APMExample01.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingTwoSpacecraftFrames() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-two-spacecraft-frames.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            Assertions.assertEquals("INSTRUMENT_2", oe.getParts()[0]);
        }
    }

    @Test
    public void testNoSpacecraftFrames() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-no-spacecraft-frames.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            Assertions.assertEquals("EME2000", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingFrame() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-missing-frame.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("Q_FRAME_A", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingQuaternionComponent() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-missing-quaternion-component.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("Q{C|1|2|3}", oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongKeyword() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-wrong-keyword.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(12, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testWrongEulerSequence() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-wrong-Euler-sequence.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INVALID_ROTATION_SEQUENCE, oe.getSpecifier());
            Assertions.assertEquals("331", oe.getParts()[0]);
            Assertions.assertEquals(33, ((Integer) oe.getParts()[1]).intValue());
            Assertions.assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    public void testMissingEulerSequence() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-missing-Euler-sequence.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("EULER_ROT_SEQ", oe.getParts()[0]);
        }
    }

    @Test
    public void testRepeatedEulerAngle() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/APM-repeated-Euler-angle.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
            buildApmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("{X|Y|Z}_ANGLE", oe.getParts()[0]);
        }
    }

    @Test
    public void testSpuriousMetaDataSection() throws URISyntaxException {
        final String name = "/ccsds/adm/apm/spurious-metadata.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildApmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(15, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("metadata", oe.getParts()[2]);
        }
    }

}
