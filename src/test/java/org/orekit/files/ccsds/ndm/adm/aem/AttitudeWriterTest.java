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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AttitudeWriterTest {

    // The default format writes 5O digits after the decimal point hence the quaternion precision
    private static final double QUATERNION_PRECISION = 1e-5;
    private static final double DATE_PRECISION = 1e-3;
    
    @TempDir
    public Path temporaryFolderPath;

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testAEMWriter() {
        Assertions.assertNotNull(new WriterBuilder().buildAemWriter());
    }

    @Test
    public void testWriteAEM1() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem aem = new ParserBuilder().buildAemParser().parseMessage(source);

        AdmHeader header = new AdmHeader();
        header.setFormatVersion(aem.getHeader().getFormatVersion());
        header.setCreationDate(aem.getHeader().getCreationDate());
        header.setOriginator(aem.getHeader().getOriginator());

        final AemSegment s0 = aem.getSegments().get(0);
        AemMetadata metadata = new AemMetadata(s0.getInterpolationSamples() - 1);
        metadata.setObjectName(s0.getMetadata().getObjectName());
        metadata.setObjectID(s0.getMetadata().getObjectID());
        metadata.getEndpoints().setFrameA(s0.getMetadata().getEndpoints().getFrameA());
        metadata.getEndpoints().setFrameB(s0.getMetadata().getEndpoints().getFrameB());
        metadata.getEndpoints().setA2b(s0.getMetadata().getEndpoints().isA2b());
        metadata.setTimeSystem(s0.getMetadata().getTimeSystem());
        metadata.setStartTime(s0.getMetadata().getStart());
        metadata.setStopTime(s0.getMetadata().getStop());
        metadata.setAttitudeType(s0.getMetadata().getAttitudeType());
        metadata.setIsFirst(s0.getMetadata().isFirst());
        metadata.setCenter(s0.getMetadata().getCenter());
        metadata.setInterpolationMethod(s0.getMetadata().getInterpolationMethod());
        AemWriter writer = new WriterBuilder().
                           withConventions(IERSConventions.IERS_2010).
                           withDataContext(DataContext.getDefault()).
                           buildAemWriter();
        final CharArrayWriter caw = new CharArrayWriter();
        writer.writeMessage(new KvnGenerator(caw, 0, "", Constants.JULIAN_DAY, 60), aem);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        final Aem generatedOem = new ParserBuilder().buildAemParser().
                        parseMessage(new DataSource("", () -> new ByteArrayInputStream(bytes)));
        compareAems(aem, generatedOem);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem aem = new ParserBuilder().buildAemParser().parseMessage(source);

        AemMetadata metadata = dummyMetadata();
        metadata.setObjectID("12345");
        AttitudeWriter writer = new AttitudeWriter(new WriterBuilder().buildAemWriter(), null, metadata,
                                                   FileFormat.KVN, "", Constants.JULIAN_DAY, 60);
        try {
            writer.write(new CharArrayWriter(), aem);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            Assertions.assertEquals(metadata.getObjectID(), oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem aem = new ParserBuilder().buildAemParser().parseMessage(source);
        AttitudeWriter writer = new AttitudeWriter(new WriterBuilder().
                                                   withConventions(aem.getConventions()).
                                                   withDataContext(aem.getDataContext()).
                                                   buildAemWriter(),
                                                   aem.getHeader(),
                                                   aem.getSegments().get(0).getMetadata(),
                                                   FileFormat.KVN,
                                                   "dummy", Constants.JULIAN_DAY, 0);
        try {
            writer.write((BufferedWriter) null, aem);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            Assertions.assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        AdmHeader header = new AdmHeader();
        header.setOriginator("NASA/JPL");
        AemMetadata metadata = dummyMetadata();
        metadata.setObjectID("1996-062A");
        metadata.setObjectName("MARS GLOBAL SURVEYOR");
        AttitudeWriter writer = new AttitudeWriter(new WriterBuilder().buildAemWriter(),
                                                   header, metadata, FileFormat.KVN, "TestNullEphemeris.aem",
                                                   Constants.JULIAN_DAY, 0);
        CharArrayWriter caw = new CharArrayWriter();
        writer.write(caw, null);
        Assertions.assertEquals(0, caw.size());
    }

    @Test
    public void testUnisatelliteFileWithDefault() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem aem = new ParserBuilder().buildAemParser().parseMessage(source);

        final File temp = temporaryFolderPath.resolve("writeAEMExample01.xml").toFile();
        AttitudeWriter writer = new AttitudeWriter(new WriterBuilder().buildAemWriter(),
                                                   aem.getHeader(), aem.getSegments().get(0).getMetadata(),
                                                   FileFormat.XML, temp.getName(), Constants.JULIAN_DAY, 1);
        writer.write(temp.getAbsolutePath(), aem);
        final Aem generatedAem = new ParserBuilder().buildAemParser().parseMessage(new DataSource(temp));
        Assertions.assertEquals(aem.getSegments().get(0).getMetadata().getObjectID(),
                     generatedAem.getSegments().get(0).getMetadata().getObjectID());
    }

    @Test
    public void testMultisatelliteFile() throws IOException {

        final DataContext context = DataContext.getDefault();
        final String id1 = "1999-012A";
        final String id2 = "1999-012B";
        StandAloneEphemerisFile file = new StandAloneEphemerisFile();
        file.generate(id1, id1 + "-name", AttitudeType.QUATERNION_ANGVEL,
                      context.getFrames().getEME2000(),
                      new TimeStampedAngularCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                        Rotation.IDENTITY,
                                                        new Vector3D(0.000, 0.010, 0.000),
                                                        new Vector3D(0.000, 0.000, 0.001)),
                      900.0, 60.0);
        file.generate(id2, id2 + "-name", AttitudeType.QUATERNION_ANGVEL,
                      context.getFrames().getEME2000(),
                      new TimeStampedAngularCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                        Rotation.IDENTITY,
                                                        new Vector3D(0.000, -0.010, 0.000),
                                                        new Vector3D(0.000, 0.000, 0.003)),
                      600.0, 10.0);

        AemMetadata metadata = dummyMetadata();
        metadata.setObjectID(id2);
        AdmHeader header = new AdmHeader();
        header.setFormatVersion(1.0);
        AttitudeWriter writer = new AttitudeWriter(new WriterBuilder().buildAemWriter(),
                                                   header, metadata, FileFormat.KVN, "",
                                                   Constants.JULIAN_DAY, 60);
        final CharArrayWriter caw = new CharArrayWriter();
        writer.write(caw, file);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        int count = 0;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             InputStreamReader    isr  = new InputStreamReader(bais, StandardCharsets.UTF_8);
             BufferedReader       br   = new BufferedReader(isr)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++count;
            }
        }
        Assertions.assertEquals(81, count);

    }

    @Test
    public void testIssue723() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample02.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem aem = new ParserBuilder().buildAemParser().parseMessage(source);

        AttitudeWriter writer = new AttitudeWriter(new WriterBuilder().buildAemWriter(),
                                                   aem.getHeader(), aem.getSegments().get(0).getMetadata(),
                                                   FileFormat.KVN, "TestAEMIssue723.aem",
                                                   Constants.JULIAN_DAY, 0);
        final CharArrayWriter caw = new CharArrayWriter();
        writer.write(caw, aem);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        final Aem generatedAem = new ParserBuilder().buildAemParser().
                        parseMessage(new DataSource("", () -> new ByteArrayInputStream(bytes)));
        Assertions.assertEquals(aem.getHeader().getComments().get(0), generatedAem.getHeader().getComments().get(0));
    }

    @Test
    public void testWriteAemFormat() throws IOException {
        // setup
        String exampleFile = "/ccsds/adm/aem/AEMExample07.txt";
        final DataSource source = new DataSource(exampleFile, () -> getClass().getResourceAsStream(exampleFile));
        final Aem aem = new ParserBuilder().buildAemParser().parseMessage(source);

        AemWriter writer = new WriterBuilder().buildAemWriter();
        final CharArrayWriter caw = new CharArrayWriter();
        writer.writeMessage(new KvnGenerator(caw, 0, "", Constants.JULIAN_DAY, 60), aem);

        String[] lines2 = caw.toString().split("\n");

        Assertions.assertEquals("2002-12-18T12:00:00.331 0.5674807981623039 0.031460044248583355 0.4568906426171408 0.6842709624277855", lines2[26]);
        Assertions.assertEquals("2002-12-18T12:01:00.331 0.4231908397172568 -0.4569709067454213 0.23784047193542462 0.7453314789254544", lines2[27]);
        Assertions.assertEquals("2002-12-18T12:02:00.331 -0.8453188238242068 0.2697396246845473 -0.0653199091139417 0.4565193647993977", lines2[28]);
    }

    private static void compareAemAttitudeBlocks(AemSegment segment1, AemSegment segment2) {

        // compare metadata
        AemMetadata meta1 = segment1.getMetadata();
        AemMetadata meta2 = segment2.getMetadata();
        Assertions.assertEquals(meta1.getObjectID(),                            meta2.getObjectID());
        Assertions.assertEquals(meta1.getObjectName(),                          meta2.getObjectName());
        Assertions.assertEquals(meta1.getCenter().getName(),                    meta2.getCenter().getName());
        Assertions.assertEquals(meta1.getTimeSystem().name(), meta2.getTimeSystem().name());
        Assertions.assertEquals(meta1.getLaunchYear(),                          meta2.getLaunchYear());
        Assertions.assertEquals(meta1.getLaunchNumber(),                        meta2.getLaunchNumber());
        Assertions.assertEquals(meta1.getLaunchPiece(),                         meta2.getLaunchPiece());
        Assertions.assertEquals(meta1.getHasCreatableBody(),                    meta2.getHasCreatableBody());
        Assertions.assertEquals(meta1.getInterpolationDegree(),                 meta2.getInterpolationDegree());

        // compare data
        Assertions.assertEquals(0.0, segment1.getStart().durationFrom(segment2.getStart()), DATE_PRECISION);
        Assertions.assertEquals(0.0, segment1.getStop().durationFrom(segment2.getStop()),   DATE_PRECISION);
        Assertions.assertEquals(segment1.getInterpolationMethod(), segment2.getInterpolationMethod());
        Assertions.assertEquals(segment1.getAngularCoordinates().size(), segment2.getAngularCoordinates().size());
        for (int i = 0; i < segment1.getAngularCoordinates().size(); i++) {
            TimeStampedAngularCoordinates c1 = segment1.getAngularCoordinates().get(i);
            Rotation rot1 = c1.getRotation();
            TimeStampedAngularCoordinates c2 = segment2.getAngularCoordinates().get(i);
            Rotation rot2 = c2.getRotation();
            Assertions.assertEquals(0.0, c1.getDate().durationFrom(c2.getDate()), DATE_PRECISION);
            Assertions.assertEquals(rot1.getQ0(), rot2.getQ0(), QUATERNION_PRECISION);
            Assertions.assertEquals(rot1.getQ1(), rot2.getQ1(), QUATERNION_PRECISION);
            Assertions.assertEquals(rot1.getQ2(), rot2.getQ2(), QUATERNION_PRECISION);
            Assertions.assertEquals(rot1.getQ3(), rot2.getQ3(), QUATERNION_PRECISION);
        }
    }

    static void compareAems(Aem file1, Aem file2) {
        Assertions.assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        Assertions.assertEquals(file1.getSegments().size(), file2.getSegments().size());
        for (int i = 0; i < file1.getSegments().size(); i++) {
            compareAemAttitudeBlocks(file1.getSegments().get(i), file2.getSegments().get(i));
        }
    }

    private class StandAloneEphemerisFile
        implements AttitudeEphemerisFile<TimeStampedAngularCoordinates, AemSegment> {
        private final Map<String, AemSatelliteEphemeris> satEphem;

        public StandAloneEphemerisFile() {
            this.satEphem = new HashMap<>();
        }

        private void generate(final String objectID, final String objectName,
                              final AttitudeType type, final Frame referenceFrame,
                              final TimeStampedAngularCoordinates ac0,
                              final double duration, final double step) {

            AemMetadata metadata = dummyMetadata();
            metadata.addComment("metadata for " + objectName);
            metadata.setObjectID(objectID);
            metadata.setObjectName(objectName);
            metadata.getEndpoints().setFrameA(FrameFacade.map(referenceFrame));
            metadata.setAttitudeType(type);
            metadata.setStartTime(ac0.getDate());
            metadata.setStopTime(ac0.getDate().shiftedBy(duration));
            metadata.setUseableStartTime(metadata.getStartTime().shiftedBy(step));
            metadata.setUseableStartTime(metadata.getStopTime().shiftedBy(-step));

            AemData data = new AemData();
            data.addComment("generated data for " + objectName);
            data.addComment("duration was set to " + duration + " s");
            data.addComment("step was set to " + step + " s");
            for (double dt = 0; dt < duration; dt += step) {
                data.addData(ac0.shiftedBy(dt));
            }

            if (!satEphem.containsKey(objectID)) {
                satEphem.put(objectID, new AemSatelliteEphemeris(objectID, Collections.emptyList()));
            }

            List<AemSegment> segments = new ArrayList<>(satEphem.get(objectID).getSegments());
            segments.add(new AemSegment(metadata, data));
            satEphem.put(objectID, new AemSatelliteEphemeris(objectID, segments));

        }

        @Override
        public Map<String, AemSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

    private AemMetadata dummyMetadata() {
        AemMetadata metadata = new AemMetadata(4);
        metadata.setTimeSystem(TimeSystem.TT);
        metadata.setObjectID("9999-999ZZZ");
        metadata.setObjectName("transgalactic");
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.getEndpoints().setA2b(true);
        metadata.setStartTime(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
        metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));
        metadata.setAttitudeType(AttitudeType.QUATERNION_DERIVATIVE);
        metadata.setIsFirst(true);
        return metadata;
    }

}
