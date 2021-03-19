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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMWriterTest {

    // The default format writes 5O digits after the decimal point hence the quaternion precision
    private static final double QUATERNION_PRECISION = 1e-5;
    private static final double DATE_PRECISION = 1e-3;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testAEMWriter() {
        assertNotNull(new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null, dummyMetadata()));
    }

    @Test
    public void testWriteAEM1() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile aemFile = new ParserBuilder().buildAemParser().parseMessage(source);

        Header header = new Header();
        header.setFormatVersion(aemFile.getHeader().getFormatVersion());
        header.setCreationDate(aemFile.getHeader().getCreationDate());
        header.setOriginator(aemFile.getHeader().getOriginator());

        final AemSegment s0 = aemFile.getSegments().get(0);
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
        String tempAEMFilePath = tempFolder.newFile("TestWriteAEM1.aem").toString();
        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), header, metadata);
        writer.write(tempAEMFilePath, aemFile);

        final AemFile generatedOemFile = new AemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1).
                                         parseMessage(new DataSource(tempAEMFilePath));
        compareAemFiles(aemFile, generatedOemFile);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile aemFile = new ParserBuilder().buildAemParser().parseMessage(source);

        AemMetadata metadata = dummyMetadata();
        metadata.setObjectID("12345");
        String tempOEMFilePath = tempFolder.newFile("TestAEMUnfoundSpaceId.aem").toString();
        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null, metadata);
        try {
            writer.write(tempOEMFilePath, aemFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            assertEquals(metadata.getObjectID(), oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile aemFile = new ParserBuilder().buildAemParser().parseMessage(source);
        AemWriter writer = new AemWriter(aemFile.getConventions(), aemFile.getDataContext(),
                                         aemFile.getHeader(), aemFile.getSegments().get(0).getMetadata());
        try {
            writer.write((BufferedWriter) null, aemFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        File tempAEMFile = tempFolder.newFile("TestNullEphemeris.aem");
        Header header = new Header();
        header.setOriginator("NASA/JPL");
        AemMetadata metadata = dummyMetadata();
        metadata.setObjectID("1996-062A");
        metadata.setObjectName("MARS GLOBAL SURVEYOR");
        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), header, metadata);
        writer.write(tempAEMFile.toString(), null);
        assertTrue(tempAEMFile.exists());
        try (FileInputStream   fis = new FileInputStream(tempAEMFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            int count = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++count;
            }
            assertEquals(0, count);
        }
    }

    @Test
    public void testUnisatelliteFileWithDefault() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample01.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile aemFile = new ParserBuilder().buildAemParser().parseMessage(source);

        String tempAEMFilePath = tempFolder.newFile("TestOEMUnisatelliteWithDefault.oem").toString();
        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         null, aemFile.getSegments().get(0).getMetadata());
        writer.write(tempAEMFilePath, aemFile);

        final AemFile generatedAemFile = new AemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1).
                                         parseMessage(new DataSource(tempAEMFilePath));
        assertEquals(aemFile.getSegments().get(0).getMetadata().getObjectID(),
                     generatedAemFile.getSegments().get(0).getMetadata().getObjectID());
    }

    @Test
    public void testMultisatelliteFile() throws IOException {

        final DataContext context = DataContext.getDefault();
        final String id1 = "1999-012A";
        final String id2 = "1999-012B";
        StandAloneEphemerisFile file = new StandAloneEphemerisFile();
        file.generate(id1, id1 + "-name", AttitudeType.QUATERNION_RATE,
                      context.getFrames().getEME2000(),
                      new TimeStampedAngularCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                        Rotation.IDENTITY,
                                                        new Vector3D(0.000, 0.010, 0.000),
                                                        new Vector3D(0.000, 0.000, 0.001)),
                      900.0, 60.0);
        file.generate(id2, id2 + "-name", AttitudeType.QUATERNION_RATE,
                      context.getFrames().getEME2000(),
                      new TimeStampedAngularCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                        Rotation.IDENTITY,
                                                        new Vector3D(0.000, -0.010, 0.000),
                                                        new Vector3D(0.000, 0.000, 0.003)),
                      600.0, 10.0);

       File written = tempFolder.newFile("TestAEMMultisatellite.aem");

        AemMetadata metadata = dummyMetadata();
        metadata.setObjectID(id2);
        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, context, null, metadata);
        writer.write(written.getAbsolutePath(), file);

        int count = 0;
        try (FileInputStream   fis = new FileInputStream(written);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++count;
            }
        }
        assertEquals(82, count);

    }

    @Test
    public void testIssue723() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample02.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AemFile aemFile = new ParserBuilder().buildAemParser().parseMessage(source);

        String tempAEMFilePath = tempFolder.newFile("TestAEMIssue723.aem").toString();
        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         aemFile.getHeader(), aemFile.getSegments().get(0).getMetadata());
        writer.write(tempAEMFilePath, aemFile);

        final AemFile generatedAemFile = new AemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1).
                                         parseMessage(new DataSource(tempAEMFilePath));
        assertEquals(aemFile.getHeader().getComments().get(0), generatedAemFile.getHeader().getComments().get(0));
    }

    /**
     * Check writing an AEM with format parameters for attitude.
     *
     * @throws IOException on error
     */
    @Test
    public void testWriteAemFormat() throws IOException {
        // setup
        String exampleFile = "/ccsds/adm/aem/AEMExample07.txt";
        final DataSource source = new DataSource(exampleFile, () -> getClass().getResourceAsStream(exampleFile));
        final AemFile aemFile = new ParserBuilder().buildAemParser().parseMessage(source);
        StringBuilder buffer = new StringBuilder();

        AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null,
                                         aemFile.getSegments().get(0).getMetadata(), "some-name",
                                         "%.2f");

        writer.write(buffer, aemFile);

        String[] lines = buffer.toString().split("\n");

        assertEquals(lines[23], "2002-12-18T12:00:00.331 0.57 0.03 0.46 0.68");
        assertEquals(lines[24], "2002-12-18T12:01:00.331 0.42 -0.46 0.24 0.75");
        assertEquals(lines[25], "2002-12-18T12:02:00.331 -0.85 0.27 -0.07 0.46");

        // Default format
        writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                               null, aemFile.getSegments().get(0).getMetadata());
        buffer = new StringBuilder();
        writer.write(buffer, aemFile);

        String[] lines2 = buffer.toString().split("\n");

        assertEquals(lines2[23], "2002-12-18T12:00:00.331  0.567480798  0.031460044  0.456890643  0.684270962");
        assertEquals(lines2[24], "2002-12-18T12:01:00.331  0.423190840 -0.456970907  0.237840472  0.745331479");
        assertEquals(lines2[25], "2002-12-18T12:02:00.331 -0.845318824  0.269739625 -0.065319909  0.456519365");
    }

    private static void compareAemAttitudeBlocks(AemSegment segment1, AemSegment segment2) {

        // compare metadata
        AemMetadata meta1 = segment1.getMetadata();
        AemMetadata meta2 = segment2.getMetadata();
        assertEquals(meta1.getObjectID(),                            meta2.getObjectID());
        assertEquals(meta1.getObjectName(),                          meta2.getObjectName());
        assertEquals(meta1.getCenter().getName(),                    meta2.getCenter().getName());
        assertEquals(meta1.getTimeSystem().name(), meta2.getTimeSystem().name());
        assertEquals(meta1.getLaunchYear(),                          meta2.getLaunchYear());
        assertEquals(meta1.getLaunchNumber(),                        meta2.getLaunchNumber());
        assertEquals(meta1.getLaunchPiece(),                         meta2.getLaunchPiece());
        assertEquals(meta1.getHasCreatableBody(),                    meta2.getHasCreatableBody());
        assertEquals(meta1.getInterpolationDegree(),                 meta2.getInterpolationDegree());

        // compare data
        assertEquals(0.0, segment1.getStart().durationFrom(segment2.getStart()), DATE_PRECISION);
        assertEquals(0.0, segment1.getStop().durationFrom(segment2.getStop()),   DATE_PRECISION);
        assertEquals(segment1.getInterpolationMethod(), segment2.getInterpolationMethod());
        assertEquals(segment1.getAngularCoordinates().size(), segment2.getAngularCoordinates().size());
        for (int i = 0; i < segment1.getAngularCoordinates().size(); i++) {
            TimeStampedAngularCoordinates c1 = segment1.getAngularCoordinates().get(i);
            Rotation rot1 = c1.getRotation();
            TimeStampedAngularCoordinates c2 = segment2.getAngularCoordinates().get(i);
            Rotation rot2 = c2.getRotation();
            assertEquals(0.0, c1.getDate().durationFrom(c2.getDate()), DATE_PRECISION);
            assertEquals(rot1.getQ0(), rot2.getQ0(), QUATERNION_PRECISION);
            assertEquals(rot1.getQ1(), rot2.getQ1(), QUATERNION_PRECISION);
            assertEquals(rot1.getQ2(), rot2.getQ2(), QUATERNION_PRECISION);
            assertEquals(rot1.getQ3(), rot2.getQ3(), QUATERNION_PRECISION);
        }
    }

    static void compareAemFiles(AemFile file1, AemFile file2) {
        assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        assertEquals(file1.getSegments().size(), file2.getSegments().size());
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
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO, "1"),
                                                          "GYRO 1"));
        metadata.getEndpoints().setA2b(true);
        metadata.setStartTime(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
        metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));
        metadata.setAttitudeType(AttitudeType.QUATERNION_DERIVATIVE);
        metadata.setIsFirst(true);
        return metadata;
    }

}
