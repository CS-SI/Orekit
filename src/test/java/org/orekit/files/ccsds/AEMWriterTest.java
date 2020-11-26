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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.AEMFile.AemSatelliteEphemeris;
import org.orekit.files.ccsds.AEMFile.AttitudeEphemeridesBlock;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMWriterTest {

    // The default format writes 5 digits after the decimal point hence the quaternion precision
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
        assertNotNull(new AEMWriter());
    }

    @Test
    public void testAEMWriterInterpolationMethodStringStringString() {
        assertNotNull(new AEMWriter(null, null, null));
        assertNotNull(new AEMWriter("FakeOriginator", null, null));
        assertNotNull(new AEMWriter(null, "FakeObjectId", null));
        assertNotNull(new AEMWriter(null, null, "Fake Object Name"));
        assertNotNull(new AEMWriter("FakeOriginator", "FakeObjectId", null));
        assertNotNull(new AEMWriter("FakeOriginator", null, "Fake Object Name"));
        assertNotNull(new AEMWriter(null, "FakeObjectId", "Fake Object Name"));
        assertNotNull(new AEMWriter("FakeOriginator", "FakeObjectId", "Fake Object Name"));
    }

    @Test
    public void testWriteAEM1() throws IOException {
        final String ex = "/ccsds/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final AEMFile aemFile = parser.parse(inEntry, "AEMExample.txt");

        String originator = aemFile.getOriginator();
        String objectName = aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectName();
        String objectID   = aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectID();
        String tempAEMFilePath = tempFolder.newFile("TestWriteAEM1.aem").toString();
        AEMWriter writer = new AEMWriter(originator, objectID, objectName);
        writer.write(tempAEMFilePath, aemFile);

        final AEMFile generatedOemFile = parser.parse(tempAEMFilePath);
        compareAemFiles(aemFile, generatedOemFile);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final AEMFile aemFile = parser.parse(inEntry, "OEMExample.txt");

        String badObjectId = "12345";
        String tempOEMFilePath = tempFolder.newFile("TestAEMUnfoundSpaceId.aem").toString();
        AEMWriter writer = new AEMWriter(null, badObjectId, null);
        try {
            writer.write(tempOEMFilePath, aemFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            assertEquals(badObjectId, oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final AEMFile aemFile = parser.parse(inEntry, "AEMExample.txt");
        String originator = aemFile.getOriginator();
        String objectName = aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectName();
        String objectID = aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectID();
        AEMWriter writer = new AEMWriter(originator, objectID, objectName);
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
        AEMWriter writer = new AEMWriter("NASA/JPL", "1996-062A", "MARS GLOBAL SURVEYOR");
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
        final String ex = "/ccsds/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final AEMFile aemFile = parser.parse(inEntry, "AEMExample.txt");

        String tempAEMFilePath = tempFolder.newFile("TestOEMUnisatelliteWithDefault.oem").toString();
        AEMWriter writer = new AEMWriter();
        writer.write(tempAEMFilePath, aemFile);

        final AEMFile generatedAemFile = parser.parse(tempAEMFilePath);
        assertEquals(aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectID(),
                generatedAemFile.getAttitudeBlocks().get(0).getMetaData().getObjectID());
    }

    @Deprecated
    @Test
    public void testMultisatelliteFileDeprecated() throws IOException {
        final String id1 = "ID1";
        final String id2 = "ID2";
        AEMFile file = new StandInEphemerisFile();
        file.getSatellites().put(id1, new StandInSatelliteEphemeris(new ArrayList<>()));
        file.getSatellites().put(id2, new StandInSatelliteEphemeris(new ArrayList<>()));

        String tempAEMFilePath = tempFolder.newFile("TestAEMMultisatellite-1.aem").toString();

        AEMWriter writer1 = new AEMWriter();

        try {
            writer1.write(tempAEMFilePath, file);
            fail("Should have thrown OrekitIllegalArgumentException due to multiple satellites");
        } catch (OrekitIllegalArgumentException e) {
            assertEquals(OrekitMessages.EPHEMERIS_FILE_NO_MULTI_SUPPORT, e.getSpecifier());
        }

        tempAEMFilePath = tempFolder.newFile("TestAEMMultisatellite-2.aem").toString();
        AEMWriter writer2 = new AEMWriter(null, id1, null);
        writer2.write(tempAEMFilePath, file);
    }

    @Test
    public void testMultisatelliteFile() throws IOException {
        final String id1 = "ID1";
        final String id2 = "ID2";
        AEMFile file = new StandInEphemerisFile();
        file.getSatellites().put(id1, new StandInSatelliteEphemeris(id1, new ArrayList<>()));
        file.getSatellites().put(id2, new StandInSatelliteEphemeris(id2, new ArrayList<>()));

        String tempAEMFilePath = tempFolder.newFile("TestAEMMultisatellite-1.aem").toString();

        AEMWriter writer1 = new AEMWriter();

        try {
            writer1.write(tempAEMFilePath, file);
            fail("Should have thrown OrekitIllegalArgumentException due to multiple satellites");
        } catch (OrekitIllegalArgumentException e) {
            assertEquals(OrekitMessages.EPHEMERIS_FILE_NO_MULTI_SUPPORT, e.getSpecifier());
        }

        tempAEMFilePath = tempFolder.newFile("TestAEMMultisatellite-2.aem").toString();
        AEMWriter writer2 = new AEMWriter(null, id1, null);
        writer2.write(tempAEMFilePath, file);
    }

    @Test
    public void testIssue723() throws IOException {
        final String ex = "/ccsds/AEMExample2.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final AEMFile aemFile = parser.parse(inEntry, "AEMExample2.txt");

        String tempAEMFilePath = tempFolder.newFile("TestAEMIssue723.aem").toString();
        AEMWriter writer = new AEMWriter();
        writer.write(tempAEMFilePath, aemFile);

        final AEMFile generatedAemFile = parser.parse(tempAEMFilePath);
        assertEquals(aemFile.getHeaderComment().get(0), generatedAemFile.getHeaderComment().get(0));
    }

    /**
     * Check writing an AEM with format parameters for attitude.
     *
     * @throws IOException on error
     */
    @Test
    public void testWriteAemFormat() throws IOException {
        // setup
        String exampleFile = "/ccsds/AEMExample7.txt";
        InputStream inEntry = getClass().getResourceAsStream(exampleFile);
        AEMParser parser = new AEMParser().withConventions(IERSConventions.IERS_2010);
        AEMFile aemFile = parser.parse(inEntry, "AEMExample7.txt");
        StringBuilder buffer = new StringBuilder();

        AEMWriter writer = new AEMWriter(aemFile.getOriginator(),
                                         aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectID(),
                                         aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectName(),
                                         "%.2f");

        writer.write(buffer, aemFile);

        String[] lines = buffer.toString().split("\n");

        assertEquals(lines[21], "2002-12-18T12:00:00.331 0.57 0.03 0.46 0.68");
        assertEquals(lines[22], "2002-12-18T12:01:00.331 0.42 -0.46 0.24 0.75");
        assertEquals(lines[23], "2002-12-18T12:02:00.331 -0.85 0.27 -0.07 0.46");

        // Default format
        writer = new AEMWriter(aemFile.getOriginator(),
                               aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectID(),
                               aemFile.getAttitudeBlocks().get(0).getMetaData().getObjectName());
        buffer = new StringBuilder();
        writer.write(buffer, aemFile);

        String[] lines2 = buffer.toString().split("\n");

        assertEquals(lines2[21], "2002-12-18T12:00:00.331  0.56748  0.03146  0.45689  0.68427");
        assertEquals(lines2[22], "2002-12-18T12:01:00.331  0.42319 -0.45697  0.23784  0.74533");
        assertEquals(lines2[23], "2002-12-18T12:02:00.331 -0.84532  0.26974 -0.06532  0.45652");
    }

    private static void compareAemAttitudeBlocks(AttitudeEphemeridesBlock block1, AttitudeEphemeridesBlock block2) {
        compareAemAttitudeBlocksMetadata(block1.getMetaData(), block2.getMetaData());
        assertEquals(0.0, block1.getStart().durationFrom(block2.getStart()), DATE_PRECISION);
        assertEquals(0.0, block1.getStop().durationFrom(block2.getStop()),   DATE_PRECISION);
        assertEquals(block1.getInterpolationDegree(), block2.getInterpolationDegree());
        assertEquals(block1.getInterpolationMethod(), block2.getInterpolationMethod());
        assertEquals(block1.getAttitudeDataLines().size(), block2.getAttitudeDataLines().size());
        for (int i = 0; i < block1.getAttitudeDataLines().size(); i++) {
            TimeStampedAngularCoordinates c1 = block1.getAttitudeDataLines().get(i);
            Rotation rot1 = c1.getRotation();
            TimeStampedAngularCoordinates c2 = block2.getAttitudeDataLines().get(i);
            Rotation rot2 = c2.getRotation();
            assertEquals(0.0, c1.getDate().durationFrom(c2.getDate()), DATE_PRECISION);
            assertEquals(rot1.getQ0(), rot2.getQ0(), QUATERNION_PRECISION);
            assertEquals(rot1.getQ1(), rot2.getQ1(), QUATERNION_PRECISION);
            assertEquals(rot1.getQ2(), rot2.getQ2(), QUATERNION_PRECISION);
            assertEquals(rot1.getQ3(), rot2.getQ3(), QUATERNION_PRECISION);
        }
    }

    private static void compareAemAttitudeBlocksMetadata(ADMMetaData meta1, ADMMetaData meta2) {
        assertEquals(meta1.getObjectID(),         meta2.getObjectID());
        assertEquals(meta1.getObjectName(),       meta2.getObjectName());
        assertEquals(meta1.getCenterName(),       meta2.getCenterName());
        assertEquals(meta1.getTimeSystem(),       meta2.getTimeSystem());
        assertEquals(meta1.getLaunchYear(),       meta2.getLaunchYear());
        assertEquals(meta1.getLaunchNumber(),     meta2.getLaunchNumber());
        assertEquals(meta1.getLaunchPiece(),      meta2.getLaunchPiece());
        assertEquals(meta1.getHasCreatableBody(), meta2.getHasCreatableBody());
    }

    static void compareAemFiles(AEMFile file1, AEMFile file2) {
        assertEquals(file1.getOriginator(), file2.getOriginator());
        assertEquals(file1.getAttitudeBlocks().size(), file2.getAttitudeBlocks().size());
        for (int i = 0; i < file1.getAttitudeBlocks().size(); i++) {
            compareAemAttitudeBlocks(file1.getAttitudeBlocks().get(i), file2.getAttitudeBlocks().get(i));
        }
    }

    private class StandInSatelliteEphemeris extends AemSatelliteEphemeris {
        final List<AttitudeEphemeridesBlock> blocks;

        @Deprecated
        public StandInSatelliteEphemeris(List<AttitudeEphemeridesBlock> blocks) {
            super(blocks);
            this.blocks = blocks;
        }

        public StandInSatelliteEphemeris(String id, List<AttitudeEphemeridesBlock> blocks) {
            super(id, blocks);
            this.blocks = blocks;
        }

        @Override
        public List<AttitudeEphemeridesBlock> getSegments() {
            return blocks;
        }

        @Override
        public AbsoluteDate getStart() {
            return null;
        }

        @Override
        public AbsoluteDate getStop() {
            return null;
        }

    }

    private class StandInEphemerisFile extends AEMFile {
        private final Map<String, AemSatelliteEphemeris> satEphem;

        public StandInEphemerisFile() {
            this.satEphem = new HashMap<String, AemSatelliteEphemeris>();
        }

        @Override
        public Map<String, AemSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

}
