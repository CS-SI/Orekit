/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile.CovarianceMatrix;
import org.orekit.files.ccsds.OEMFile.EphemeridesBlock;
import org.orekit.files.ccsds.OEMWriter.InterpolationMethod;
import org.orekit.files.general.EphemerisFile;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OEMWriterTest {
    private static final double POSITION_PRECISION = 1e-9;
    private static final double VELOCITY_PRECISION = 1e-9;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOEMWriter() {
        assertNotNull(new OEMWriter());
    }

    @Test
    public void testOEMWriterInterpolationMethodStringStringString() {
        assertNotNull(new OEMWriter(OEMWriter.DEFAULT_INTERPOLATION_METHOD, null, null, null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", null, null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, null, "FakeObjectId", null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, null, null, "Fake Object Name"));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", "FakeObjectId", null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", null, "Fake Object Name"));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, null, "FakeObjectId", "Fake Object Name"));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", "FakeObjectId", "Fake Object Name"));
    }

    @Test
    public void testWriteOEM1() throws IOException {
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;

        String originator = oemFile.getOriginator();
        String objectName = oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectName();
        String objectID = oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID();
        String interpolationMethodString = oemFile.getEphemeridesBlocks().get(0).getInterpolationMethod();
        InterpolationMethod interpolationMethod = Enum.valueOf(InterpolationMethod.class, interpolationMethodString);
        String tempOEMFilePath = tempFolder.newFile("TestWriteOEM1.oem").toString();
        OEMWriter writer = new OEMWriter(interpolationMethod, originator, objectID, objectName);
        writer.write(tempOEMFilePath, ephemerisFile);

        final OEMFile generatedOemFile = parser.parse(tempOEMFilePath);
        compareOemFiles(oemFile, generatedOemFile);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;

        String badObjectId = "12345";
        String interpolationMethodString = oemFile.getEphemeridesBlocks().get(0).getInterpolationMethod();
        InterpolationMethod interpolationMethod = Enum.valueOf(InterpolationMethod.class, interpolationMethodString);
        String tempOEMFilePath = tempFolder.newFile("TestOEMUnfoundSpaceId.oem").toString();
        OEMWriter writer = new OEMWriter(interpolationMethod, null, badObjectId, null);
        try {
            writer.write(tempOEMFilePath, ephemerisFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            assertEquals(badObjectId, oiae.getParts()[0]);
        }

    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;
        String originator = oemFile.getOriginator();
        String objectName = oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectName();
        String objectID = oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID();
        String interpolationMethodString = oemFile.getEphemeridesBlocks().get(0).getInterpolationMethod();
        InterpolationMethod interpolationMethod = Enum.valueOf(InterpolationMethod.class, interpolationMethodString);
        OEMWriter writer = new OEMWriter(interpolationMethod, originator, objectID, objectName);
        try {
            writer.write((BufferedWriter) null, ephemerisFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        File tempOEMFile = tempFolder.newFile("TestNullEphemeris.oem");
        OEMWriter writer = new OEMWriter(InterpolationMethod.HERMITE,
                                         "NASA/JPL", "1996-062A", "MARS GLOBAL SURVEYOR");
        writer.write(tempOEMFile.toString(), null);
        assertTrue(tempOEMFile.exists());
        try (FileInputStream   fis = new FileInputStream(tempOEMFile);
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
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;

        String tempOEMFilePath = tempFolder.newFile("TestOEMUnisatelliteWithDefault.oem").toString();
        OEMWriter writer = new OEMWriter();
        writer.write(tempOEMFilePath, ephemerisFile);

        final OEMFile generatedOemFile = parser.parse(tempOEMFilePath);
        assertEquals(oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID(),
                generatedOemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID());
    }

    @Test
    public void testMultisatelliteFile() throws IOException {
        final String id1 = "ID1";
        final String id2 = "ID2";
        StandInEphemerisFile file = new StandInEphemerisFile();
        file.getSatellites().put(id1, new StandInSatelliteEphemeris(id1));
        file.getSatellites().put(id2, new StandInSatelliteEphemeris(id2));

        EphemerisFile ephemerisFile = (EphemerisFile) file;
        String tempOEMFilePath = tempFolder.newFile("TestOEMMultisatellite-1.oem").toString();

        OEMWriter writer1 = new OEMWriter();

        try {
            writer1.write(tempOEMFilePath, ephemerisFile);
            fail("Should have thrown OrekitIllegalArgumentException due to multiple satellites");
        } catch (OrekitIllegalArgumentException e) {
            assertEquals(OrekitMessages.EPHEMERIS_FILE_NO_MULTI_SUPPORT, e.getSpecifier());
        }

        tempOEMFilePath = tempFolder.newFile("TestOEMMultisatellite-2.oem").toString();
        OEMWriter writer2 = new OEMWriter(OEMWriter.DEFAULT_INTERPOLATION_METHOD, null, id1, null);
        writer2.write(tempOEMFilePath, ephemerisFile);
    }

    private static void compareOemEphemerisBlocks(EphemeridesBlock block1, EphemeridesBlock block2) {
        compareOemEphemerisBlocksMetadata(block1.getMetaData(), block2.getMetaData());
        assertEquals(block1.getStart(), block2.getStart());
        assertEquals(block1.getStop(), block2.getStop());
        assertEquals(block1.getInterpolationDegree(), block2.getInterpolationDegree());
        assertEquals(block1.getInterpolationMethod(), block2.getInterpolationMethod());
        assertEquals(block1.getEphemeridesDataLines().size(), block2.getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getEphemeridesDataLines().get(i);
            assertEquals(c1.getDate(), c2.getDate());
            assertEquals(c1.getPosition() + " -> " + c2.getPosition(), 0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), POSITION_PRECISION);
            assertEquals(c1.getVelocity() + " -> " + c2.getVelocity(), 0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), VELOCITY_PRECISION);
        }
        assertEquals(block1.getCovarianceMatrices().size(), block2.getCovarianceMatrices().size());
        for (int j = 0; j < block1.getCovarianceMatrices().size(); j++) {
        	CovarianceMatrix covMat1 = block1.getCovarianceMatrices().get(j);
        	CovarianceMatrix covMat2 = block2.getCovarianceMatrices().get(j);
        	assertEquals(covMat1.getEpoch(), covMat2.getEpoch());
        	assertEquals(covMat1.getFrame(), covMat2.getFrame());
        	assertEquals(covMat1.getLofType(), covMat2.getLofType());
        	assertEquals(covMat1.getMatrix(),covMat2.getMatrix());       	
        }
    }

    private static void compareOemEphemerisBlocksMetadata(ODMMetaData meta1, ODMMetaData meta2) {
        assertEquals(meta1.getObjectID(), meta2.getObjectID());
        assertEquals(meta1.getObjectName(), meta2.getObjectName());
        assertEquals(meta1.getCenterName(), meta2.getCenterName());
        assertEquals(meta1.getFrameString(), meta2.getFrameString());
        assertEquals(meta1.getTimeSystem(), meta2.getTimeSystem());
    }

    static void compareOemFiles(OEMFile file1, OEMFile file2) {
        assertEquals(file1.getOriginator(), file2.getOriginator());
        assertEquals(file1.getEphemeridesBlocks().size(), file2.getEphemeridesBlocks().size());
        for (int i = 0; i < file1.getEphemeridesBlocks().size(); i++) {
            compareOemEphemerisBlocks(file1.getEphemeridesBlocks().get(i), file2.getEphemeridesBlocks().get(i));
        }
    }

    private class StandInSatelliteEphemeris implements EphemerisFile.SatelliteEphemeris {
        final String id;

        public StandInSatelliteEphemeris(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public double getMu() {
            // TODO Auto-generated method stub
            return 0;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public List<? extends EphemerisFile.EphemerisSegment> getSegments() {
            return new ArrayList();
        }

        @Override
        public AbsoluteDate getStart() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public AbsoluteDate getStop() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class StandInEphemerisFile implements EphemerisFile {
        private final Map<String, StandInSatelliteEphemeris> satEphem;

        public StandInEphemerisFile() {
            this.satEphem = new HashMap<String, StandInSatelliteEphemeris>();
        }

        @Override
        public Map<String, StandInSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

}
