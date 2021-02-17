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
package org.orekit.files.ccsds.ndm.odm.oem;

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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.Covariance;
import org.orekit.files.ccsds.utils.CcsdsFrame;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OEMWriterTest {
    // As the default format for position is 3 digits after decimal point in km the max precision in m is 1
    private static final double POSITION_PRECISION = 1; // in m
    // As the default format for velocity is 5 digits after decimal point in km/s the max precision in m/s is 1e-2
    private static final double VELOCITY_PRECISION = 1e-2; //in m/s

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOEMWriter() {
        assertNotNull(new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null, dummyMetadata()));
    }

    @Test
    public void testWriteOEM1() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final OemFile oemFile = parser.parseMessage(source);

        String tempOEMFilePath = tempFolder.newFile("TestWriteOEM1.oem").toString();
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         oemFile.getHeader(), oemFile.getSegments().get(0).getMetadata());
        writer.write(tempOEMFilePath, oemFile);

        final OemFile generatedOemFile = new OemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                       null, CelestialBodyFactory.getMars().getGM(), 1).
                                         parseMessage(new DataSource(tempOEMFilePath));
        compareOemFiles(oemFile, generatedOemFile);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final OemFile oemFile = parser.parseMessage(source);

        String tempOEMFilePath = tempFolder.newFile("TestOEMUnfoundSpaceId.oem").toString();
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         oemFile.getHeader(), dummyMetadata());
        try {
            writer.write(tempOEMFilePath, oemFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            assertEquals(dummyMetadata().getObjectID(), oiae.getParts()[0]);
        }

    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final OemFile oemFile = parser.parseMessage(source);
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         oemFile.getHeader(), oemFile.getSegments().get(0).getMetadata());
        try {
            writer.write((BufferedWriter) null, oemFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        File tempOEMFile = tempFolder.newFile("TestNullEphemeris.oem");
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         null, dummyMetadata());
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
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final OemFile oemFile = parser.parseMessage(source);

        String tempOEMFilePath = tempFolder.newFile("TestOEMUnisatelliteWithDefault.oem").toString();
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         oemFile.getHeader(), oemFile.getSegments().get(0).getMetadata());
        writer.write(tempOEMFilePath, oemFile);

        final OemFile generatedOemFile = new OemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                       null, CelestialBodyFactory.getEarth().getGM(), 1).
                                         parseMessage(new DataSource(tempOEMFilePath));
        assertEquals(oemFile.getSegments().get(0).getMetadata().getObjectID(),
                generatedOemFile.getSegments().get(0).getMetadata().getObjectID());
    }

    @Test
    public void testIssue723() throws IOException {
        final String ex = "/ccsds/odm/oem/OEMExampleWithHeaderComment.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
        final OemFile oemFile = parser.parseMessage(source);

        String tempOEMFilePath = tempFolder.newFile("TestOEMIssue723.aem").toString();
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         oemFile.getHeader(), oemFile.getSegments().get(0).getMetadata());
        writer.write(tempOEMFilePath, oemFile);

        final OemFile generatedOemFile = new OemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                       null, CelestialBodyFactory.getEarth().getGM(), 1).
                                         parseMessage(new DataSource(tempOEMFilePath));
        assertEquals(oemFile.getHeader().getComments().get(0), generatedOemFile.getHeader().getComments().get(0));
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
        OemFile oemFile = parser.parseMessage(source);
        StringBuilder buffer = new StringBuilder();

        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                                         oemFile.getHeader(), oemFile.getSegments().get(0).getMetadata(),
                                         "", "%.2f", "%.3f", "%.4f", "%.6e");

        writer.write(buffer, oemFile);

        String[] lines = buffer.toString().split("\n");

        assertEquals(lines[19], "2002-12-18T12:00:00.331000000 2789.62 -280.05 -1746.76 4.734 -2.496 -1.042");
        assertEquals(lines[20], "2002-12-18T12:01:00.331000000 2783.42 -308.14 -1877.07 5.186 -2.421 -1.996");
        assertEquals(lines[21], "2002-12-18T12:02:00.331000000 2776.03 -336.86 -2008.68 5.637 -2.340 -1.947");

        // Default format
        
        writer = new OemWriter(IERSConventions.IERS_2010, DataContext.getDefault(),
                               oemFile.getHeader(), oemFile.getSegments().get(0).getMetadata());
        buffer = new StringBuilder();
        writer.write(buffer, oemFile);

        String[] lines2 = buffer.toString().split("\n");

        assertEquals(lines2[19], "2002-12-18T12:00:00.331000000  2789.619000 -280.045000 -1746.755000  4.733720000 -2.495860000 -1.041950000");
        assertEquals(lines2[20], "2002-12-18T12:01:00.331000000  2783.419000 -308.143000 -1877.071000  5.186040000 -2.421240000 -1.996080000");
        assertEquals(lines2[21], "2002-12-18T12:02:00.331000000  2776.033000 -336.859000 -2008.682000  5.636780000 -2.339510000 -1.946870000");
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

       File written = tempFolder.newFile("TestAEMMultisatellite.aem");

        OemMetadata metadata = dummyMetadata();
        metadata.setObjectID(id2);
        OemWriter writer = new OemWriter(IERSConventions.IERS_2010, context, null, metadata);
        writer.write(written.getAbsolutePath(), file);

        int count = 0;
        try (FileInputStream   fis = new FileInputStream(written);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++count;
            }
        }
        assertEquals(79, count);

    }

    private static void compareOemEphemerisBlocks(OemSegment block1, OemSegment block2) {
        compareOemEphemerisBlocksMetadata(block1.getMetadata(), block2.getMetadata());
        assertEquals(block1.getStart(), block2.getStart());
        assertEquals(block1.getStop(), block2.getStop());
        assertEquals(block1.getMetadata().getInterpolationDegree(), block2.getMetadata().getInterpolationDegree());
        assertEquals(block1.getMetadata().getInterpolationMethod(), block2.getMetadata().getInterpolationMethod());
        assertEquals(block1.getData().getEphemeridesDataLines().size(), block2.getData().getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getData().getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getData().getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getData().getEphemeridesDataLines().get(i);
            assertEquals(c1.getDate(), c2.getDate());
            assertEquals(c1.getPosition() + " -> " + c2.getPosition(), 0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), POSITION_PRECISION);
            assertEquals(c1.getVelocity() + " -> " + c2.getVelocity(), 0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), VELOCITY_PRECISION);
        }
        assertEquals(block1.getCovarianceMatrices().size(), block2.getCovarianceMatrices().size());
        for (int j = 0; j < block1.getCovarianceMatrices().size(); j++) {
        	Covariance covMat1 = block1.getCovarianceMatrices().get(j);
        	Covariance covMat2 = block2.getCovarianceMatrices().get(j);
        	assertEquals(covMat1.getEpoch(), covMat2.getEpoch());
        	assertEquals(covMat1.getRefCCSDSFrame(), covMat2.getRefCCSDSFrame());
        	assertEquals(covMat1.getCovarianceMatrix(),covMat2.getCovarianceMatrix());       	
        }
    }

    private static void compareOemEphemerisBlocksMetadata(OemMetadata meta1, OemMetadata meta2) {
        assertEquals(meta1.getObjectID(),   meta2.getObjectID());
        assertEquals(meta1.getObjectName(), meta2.getObjectName());
        assertEquals(meta1.getCenterName(), meta2.getCenterName());
        assertEquals(meta1.getRefFrame(),   meta2.getRefFrame());
        assertEquals(meta1.getTimeSystem(), meta2.getTimeSystem());
    }

    static void compareOemFiles(OemFile file1, OemFile file2) {
        assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        assertEquals(file1.getSegments().size(), file2.getSegments().size());
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
        metadata.setTimeSystem(CcsdsTimeScale.TT);
        metadata.setObjectID("9999-999ZZZ");
        metadata.setObjectName("transgalactic");
        metadata.setCenterName("EARTH", CelestialBodyFactory.getCelestialBodies());
        metadata.setRefFrame(FramesFactory.getEME2000(), CcsdsFrame.EME2000);
        metadata.setStartTime(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
        metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));
        return metadata;
    }

}
