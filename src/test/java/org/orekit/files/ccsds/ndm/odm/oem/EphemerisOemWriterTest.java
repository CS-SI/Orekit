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
package org.orekit.files.ccsds.ndm.odm.oem;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EphemerisOemWriterTest {

    // As the default format for position is 3 digits after decimal point in km the max precision in m is 1
    private static final double POSITION_PRECISION = 1; // in m
    // As the default format for velocity is 5 digits after decimal point in km/s the max precision in m/s is 1e-2
    private static final double VELOCITY_PRECISION = 1e-2; //in m/s

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOEMWriter() {
        Assertions.assertNotNull(new WriterBuilder().buildOemWriter());
    }

    @Test
    public void testWriteOEM1Kvn() throws IOException {
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, 0, "", Constants.JULIAN_DAY, 60);
        doTestWriteOEM1(caw, generator);
    }

    @Test
    public void testWriteOEM1Xml() throws IOException {
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new XmlGenerator(caw, 2, "", Constants.JULIAN_DAY, true, XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION);
        doTestWriteOEM1(caw, generator);
    }

    private void doTestWriteOEM1(final CharArrayWriter caw, Generator generator) throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem oem = parser.parseMessage(source);

        OemWriter writer = new WriterBuilder().
                        withConventions(IERSConventions.IERS_2010).
                        withDataContext(DataContext.getDefault()).
                        buildOemWriter();
        writer.writeMessage(generator, oem);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        final Oem generatedOem = new ParserBuilder().
                                         withConventions(IERSConventions.IERS_2010).
                                         withSimpleEOP(true).
                                         withDataContext(DataContext.getDefault()).
                                         withMu(CelestialBodyFactory.getMars().getGM()).
                                         withDefaultInterpolationDegree(1).
                                         buildOemParser().
                                         parseMessage(new DataSource("", () -> new ByteArrayInputStream(bytes)));
        compareOems(oem, generatedOem);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final Oem oem = parser.parseMessage(source);

        EphemerisOemWriter writer = new EphemerisOemWriter(new WriterBuilder().buildOemWriter(),
                                                     oem.getHeader(), dummyMetadata(), FileFormat.KVN, "",
                                                     Constants.JULIAN_DAY, 0);
        try {
            writer.write(new CharArrayWriter(), oem);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            Assertions.assertEquals(dummyMetadata().getObjectID(), oiae.getParts()[0]);
        }

    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final Oem oem = parser.parseMessage(source);
        EphemerisOemWriter writer = new EphemerisOemWriter(new WriterBuilder().buildOemWriter(),
                                                     oem.getHeader(),
                                                     oem.getSegments().get(0).getMetadata(),
                                                     FileFormat.KVN, "dummy",
                                                     Constants.JULIAN_DAY, 0);
        try {
            writer.write((BufferedWriter) null, oem);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            Assertions.assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        EphemerisOemWriter writer = new EphemerisOemWriter(new WriterBuilder().buildOemWriter(),
                                                     null, dummyMetadata(), FileFormat.KVN, "nullEphemeris",
                                                     Constants.JULIAN_DAY, 60);
        CharArrayWriter caw = new CharArrayWriter();
        writer.write(caw, null);
        Assertions.assertEquals(0, caw.size());
    }

    @Test
    public void testUnisatelliteFileWithDefault() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final Oem oem = parser.parseMessage(source);

        OemWriter writer = new WriterBuilder().buildOemWriter();
        final CharArrayWriter caw = new CharArrayWriter();
        writer.writeMessage(new KvnGenerator(caw, 0, "", Constants.JULIAN_DAY, 60), oem);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        final Oem generatedOem = new ParserBuilder().
                                         withMu(CelestialBodyFactory.getEarth().getGM()).
                                         buildOemParser().
                                         parseMessage(new DataSource("", () -> new ByteArrayInputStream(bytes)));
        Assertions.assertEquals(oem.getSegments().get(0).getMetadata().getObjectID(),
                generatedOem.getSegments().get(0).getMetadata().getObjectID());
    }

    @Test
    public void testIssue723() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExampleWithHeaderComment.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final Oem oem = parser.parseMessage(source);

        EphemerisOemWriter writer = new EphemerisOemWriter(new WriterBuilder().buildOemWriter(),
                                                     oem.getHeader(),
                                                     oem.getSegments().get(0).getMetadata(),
                                                     FileFormat.KVN, "TestOEMIssue723.aem",
                                                     Constants.JULIAN_DAY, 0);
        final CharArrayWriter caw = new CharArrayWriter();
        writer.write(caw, oem);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        final Oem generatedOem = new ParserBuilder().
                                         withMu(CelestialBodyFactory.getEarth().getGM()).
                                         buildOemParser().
                                         parseMessage(new DataSource("", () -> new ByteArrayInputStream(bytes)));
        Assertions.assertEquals(oem.getHeader().getComments().get(0), generatedOem.getHeader().getComments().get(0));
    }

    /**
     * Check writing an OEM with format parameters for orbit.
     *
     * @throws IOException on error
     */
    @Test
    public void testWriteOemFormat() throws IOException {
        // setup
        String exampleFile = "/ccsds/odm/oem/OEMExample4.txt";
        final DataSource source =  new DataSource(exampleFile, () -> getClass().getResourceAsStream(exampleFile));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        Oem oem = parser.parseMessage(source);

        OemWriter writer = new WriterBuilder().buildOemWriter();
        final CharArrayWriter caw = new CharArrayWriter();
        writer.writeMessage(new KvnGenerator(caw, 0, "", Constants.JULIAN_DAY, 60), oem);

        String[] lines2 = caw.toString().split("\n");
        Assertions.assertEquals("2002-12-18T12:00:00.331 2789.619 -280.045 -1746.755 4.73372 -2.49586 -1.0419499999999997", lines2[21]);
        Assertions.assertEquals("2002-12-18T12:01:00.331 2783.419 -308.143 -1877.071 5.18604 -2.42124 -1.99608", lines2[22]);
        Assertions.assertEquals("2002-12-18T12:02:00.331 2776.033 -336.859 -2008.682 5.63678 -2.33951 -1.94687", lines2[23]);

    }

    @Test
    public void testMultisatelliteFile() throws IOException {

        final DataContext context = DataContext.getDefault();
        final String id1 = "1999-012A";
        final String id2 = "1999-012B";
        StandAloneEphemerisFile file = new StandAloneEphemerisFile();
        file.generate(id1, id1 + "-name", context.getFrames().getEME2000(),
                      new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                   new Vector3D(1.0e6, 2.0e6, 3.0e6),
                                                   new Vector3D(-300, -200, -100)),
                      900.0, 60.0);
        file.generate(id2, id2 + "-name", context.getFrames().getEME2000(),
                      new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                   new Vector3D(3.0e6, 2.0e6, -1.0e6),
                                                   new Vector3D(-17, -20, 150)),
                      600.0, 10.0);


        OemMetadata metadata = dummyMetadata();
        metadata.setObjectID(id2);
        EphemerisOemWriter writer = new EphemerisOemWriter(new WriterBuilder().withDataContext(context).buildOemWriter(),
                                                     null, metadata, FileFormat.KVN, "",
                                                     Constants.JULIAN_DAY, -1);
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
        Assertions.assertEquals(80, count);

    }

    private static void compareOemEphemerisBlocks(OemSegment block1, OemSegment block2) {
        compareOemEphemerisBlocksMetadata(block1.getMetadata(), block2.getMetadata());
        Assertions.assertEquals(block1.getStart(), block2.getStart());
        Assertions.assertEquals(block1.getStop(), block2.getStop());
        Assertions.assertEquals(block1.getMetadata().getInterpolationDegree(), block2.getMetadata().getInterpolationDegree());
        Assertions.assertEquals(block1.getMetadata().getInterpolationMethod(), block2.getMetadata().getInterpolationMethod());
        Assertions.assertEquals(block1.getData().getEphemeridesDataLines().size(), block2.getData().getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getData().getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getData().getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getData().getEphemeridesDataLines().get(i);
            Assertions.assertEquals(c1.getDate(), c2.getDate());
            Assertions.assertEquals( 0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), POSITION_PRECISION,c1.getPosition() + " -> " + c2.getPosition());
            Assertions.assertEquals( 0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), VELOCITY_PRECISION,c1.getVelocity() + " -> " + c2.getVelocity());
        }
        Assertions.assertEquals(block1.getCovarianceMatrices().size(), block2.getCovarianceMatrices().size());
        for (int j = 0; j < block1.getCovarianceMatrices().size(); j++) {
            CartesianCovariance covMat1 = block1.getCovarianceMatrices().get(j);
            CartesianCovariance covMat2 = block2.getCovarianceMatrices().get(j);
            Assertions.assertEquals(covMat1.getEpoch(), covMat2.getEpoch());
            Assertions.assertEquals(covMat1.getReferenceFrame().asFrame(),               covMat2.getReferenceFrame().asFrame());
            Assertions.assertEquals(covMat1.getReferenceFrame().asCelestialBodyFrame(),  covMat2.getReferenceFrame().asCelestialBodyFrame());
            Assertions.assertEquals(covMat1.getReferenceFrame().asOrbitRelativeFrame(),  covMat2.getReferenceFrame().asOrbitRelativeFrame());
            Assertions.assertEquals(covMat1.getReferenceFrame().asSpacecraftBodyFrame(), covMat2.getReferenceFrame().asSpacecraftBodyFrame());
            Assertions.assertEquals(covMat1.getCovarianceMatrix(),covMat2.getCovarianceMatrix());
        }
    }

    private static void compareOemEphemerisBlocksMetadata(OemMetadata meta1, OemMetadata meta2) {
        Assertions.assertEquals(meta1.getObjectID(),                               meta2.getObjectID());
        Assertions.assertEquals(meta1.getObjectName(),                             meta2.getObjectName());
        Assertions.assertEquals(meta1.getCenter().getName(),                       meta2.getCenter().getName());
        Assertions.assertEquals(meta1.getReferenceFrame().asFrame(),               meta2.getReferenceFrame().asFrame());
        Assertions.assertEquals(meta1.getReferenceFrame().asCelestialBodyFrame(),  meta2.getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(meta1.getReferenceFrame().asOrbitRelativeFrame(),  meta2.getReferenceFrame().asOrbitRelativeFrame());
        Assertions.assertEquals(meta1.getReferenceFrame().asSpacecraftBodyFrame(), meta2.getReferenceFrame().asSpacecraftBodyFrame());
        Assertions.assertEquals(meta1.getTimeSystem().name(),    meta2.getTimeSystem().name());
    }

    static void compareOems(Oem file1, Oem file2) {
        Assertions.assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        Assertions.assertEquals(file1.getSegments().size(), file2.getSegments().size());
        for (int i = 0; i < file1.getSegments().size(); i++) {
            compareOemEphemerisBlocks(file1.getSegments().get(i), file2.getSegments().get(i));
        }
    }

    private class StandAloneEphemerisFile
        implements EphemerisFile<TimeStampedPVCoordinates, OemSegment> {
        private final Map<String, OemSatelliteEphemeris> satEphem;

        /** Simple constructor.
         */
        public StandAloneEphemerisFile() {
            this.satEphem    = new HashMap<String, OemSatelliteEphemeris>();
        }

        private void generate(final String objectID, final String objectName,
                              final Frame referenceFrame, final TimeStampedPVCoordinates pv0,
                              final double duration, final double step) {

            OemMetadata metadata = dummyMetadata();
            metadata.addComment("metadata for " + objectName);
            metadata.setObjectID(objectID);
            metadata.setObjectName(objectName);
            metadata.setStartTime(pv0.getDate());
            metadata.setStopTime(pv0.getDate().shiftedBy(duration));
            metadata.setUseableStartTime(metadata.getStartTime().shiftedBy(step));
            metadata.setUseableStartTime(metadata.getStopTime().shiftedBy(-step));

            OemData data = new OemData();
            data.addComment("generated data for " + objectName);
            data.addComment("duration was set to " + duration + " s");
            data.addComment("step was set to " + step + " s");
            for (double dt = 0; dt < duration; dt += step) {
                data.addData(pv0.shiftedBy(dt), false);
            }

            if (!satEphem.containsKey(objectID)) {
                satEphem.put(objectID,
                             new OemSatelliteEphemeris(objectID, Constants.EIGEN5C_EARTH_MU, Collections.emptyList()));
            }

            List<OemSegment> segments =
                            new ArrayList<>(satEphem.get(objectID).getSegments());
            segments.add(new OemSegment(metadata, data, Constants.EIGEN5C_EARTH_MU));
            satEphem.put(objectID, new OemSatelliteEphemeris(objectID, Constants.EIGEN5C_EARTH_MU, segments));

        }

        @Override
        public Map<String, OemSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

    private OemMetadata dummyMetadata() {
        OemMetadata metadata = new OemMetadata(4);
        metadata.addComment("dummy comment");
        metadata.setTimeSystem(TimeSystem.TT);
        metadata.setObjectID("9999-999ZZZ");
        metadata.setObjectName("transgalactic");
        metadata.setCenter(new BodyFacade("EARTH", CelestialBodyFactory.getCelestialBodies().getEarth()));
        metadata.setReferenceFrame(FrameFacade.map(FramesFactory.getEME2000()));
        metadata.setStartTime(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
        metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));
        return metadata;
    }

}
