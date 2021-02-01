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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.KVNLexicalAnalyzer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
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
        assertNotNull(new AEMWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null, null));
    }

    @Test
    public void testWriteAEM1() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1);
        final AEMFile aemFile = new KVNLexicalAnalyzer(inEntry, "AEMExample.txt").accept(parser);

        Header header = new Header();
        header.setFormatVersion(aemFile.getHeader().getFormatVersion());
        header.setCreationDate(aemFile.getHeader().getCreationDate());
        header.setOriginator(aemFile.getHeader().getOriginator());

        final AEMSegment s0 = aemFile.getSegments().get(0);
        AEMMetadata metadata = new AEMMetadata(s0.getInterpolationSamples() - 1);
        metadata.setObjectName(s0.getMetadata().getObjectName());
        metadata.setObjectID(s0.getMetadata().getObjectID());
        metadata.getEndPoints().setFrameA(s0.getMetadata().getEndPoints().getExternalFrame().name());
        metadata.getEndPoints().setFrameB(s0.getMetadata().getEndPoints().getLocalFrame().toString());
        metadata.getEndPoints().setDirection("A2B");
        metadata.setTimeSystem(s0.getMetadata().getTimeSystem());
        metadata.setStartTime(s0.getMetadata().getStart());
        metadata.setStopTime(s0.getMetadata().getStop());
        metadata.setAttitudeType(s0.getMetadata().getAttitudeType());
        metadata.setIsFirst(s0.getMetadata().isFirst());
        metadata.setCenterName(s0.getMetadata().getCenterName(), DataContext.getDefault().getCelestialBodies());
        metadata.setInterpolationMethod(s0.getMetadata().getInterpolationMethod());
        String tempAEMFilePath = tempFolder.newFile("TestWriteAEM1.aem").toString();
        AEMWriter writer = new AEMWriter(IERSConventions.IERS_2010, DataContext.getDefault(), header, metadata);
        writer.write(tempAEMFilePath, aemFile);

        final AEMFile generatedOemFile = new KVNLexicalAnalyzer(tempAEMFilePath).
                        accept(new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(), null, 1));
        compareAemFiles(aemFile, generatedOemFile);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile aemFile = new KVNLexicalAnalyzer(inEntry, "OEMExample.txt").accept(parser);

        AEMMetadata metadata = new AEMMetadata(1);
        metadata.setObjectID("12345");
        String tempOEMFilePath = tempFolder.newFile("TestAEMUnfoundSpaceId.aem").toString();
        AEMWriter writer = new AEMWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null, metadata);
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
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile aemFile = new KVNLexicalAnalyzer(inEntry, "AEMExample.txt").accept(parser);
        AEMWriter writer = new AEMWriter(aemFile.getConventions(), aemFile.getDataContext(),
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
        final String ex = "/ccsds/adm/aem/AEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile aemFile = new KVNLexicalAnalyzer(inEntry, "AEMExample.txt").accept(parser);

        String tempAEMFilePath = tempFolder.newFile("TestOEMUnisatelliteWithDefault.oem").toString();
        AEMWriter writer = new AEMWriter();
        writer.write(tempAEMFilePath, aemFile);

        final AEMFile generatedAemFile = parser.parseType(tempAEMFilePath);
        assertEquals(aemFile.getSegments().get(0).getMetadata().getObjectID(),
                     generatedAemFile.getSegments().get(0).getMetadata().getObjectID());
    }

    @Test
    public void testMultisatelliteFile() throws IOException {
        final String id1 = "ID1";
        final String id2 = "ID2";
        AEMFile file = new StandInEphemerisFile(IERSConventions.IERS_2010, DataContext.getDefault(), null);
        file.getSatellites().put(id1, new AEMSatelliteEphemeris(id1, new ArrayList<>()));
        file.getSatellites().put(id2, new AEMSatelliteEphemeris(id2, new ArrayList<>()));

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
        final String ex = "/ccsds/adm/aem/AEMExample2.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               null, 1);
        final AEMFile aemFile = new KVNLexicalAnalyzer(inEntry, "AEMExample2.txt").accept(parser);

        String tempAEMFilePath = tempFolder.newFile("TestAEMIssue723.aem").toString();
        AEMWriter writer = new AEMWriter();
        writer.write(tempAEMFilePath, aemFile);

        final AEMFile generatedAemFile = parser.parseType(tempAEMFilePath);
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
        String exampleFile = "/ccsds/adm/aem/AEMExample7.txt";
        InputStream inEntry = getClass().getResourceAsStream(exampleFile);
        AEMParser parser = new AEMParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                         null, 1);
        AEMFile aemFile = new KVNLexicalAnalyzer(inEntry, "AEMExample7.txt").accept(parser);
        StringBuilder buffer = new StringBuilder();

        AEMWriter writer = new AEMWriter(aemFile.getHeader().getOriginator(),
                                         aemFile.getSegments().get(0).getMetadata().getObjectID(),
                                         aemFile.getSegments().get(0).getMetadata().getObjectName(),
                                         "%.2f");

        writer.write(buffer, aemFile);

        String[] lines = buffer.toString().split("\n");

        assertEquals(lines[21], "2002-12-18T12:00:00.331 0.57 0.03 0.46 0.68");
        assertEquals(lines[22], "2002-12-18T12:01:00.331 0.42 -0.46 0.24 0.75");
        assertEquals(lines[23], "2002-12-18T12:02:00.331 -0.85 0.27 -0.07 0.46");

        // Default format
        writer = new AEMWriter(aemFile.getHeader().getOriginator(),
                               aemFile.getSegments().get(0).getMetadata().getObjectID(),
                               aemFile.getSegments().get(0).getMetadata().getObjectName());
        buffer = new StringBuilder();
        writer.write(buffer, aemFile);

        String[] lines2 = buffer.toString().split("\n");

        assertEquals(lines2[21], "2002-12-18T12:00:00.331  0.56748  0.03146  0.45689  0.68427");
        assertEquals(lines2[22], "2002-12-18T12:01:00.331  0.42319 -0.45697  0.23784  0.74533");
        assertEquals(lines2[23], "2002-12-18T12:02:00.331 -0.84532  0.26974 -0.06532  0.45652");
    }

    private static void compareAemAttitudeBlocks(AEMSegment segment1, AEMSegment segment2) {

        // compare metadata
        AEMMetadata meta1 = segment1.getMetadata();
        AEMMetadata meta2 = segment2.getMetadata();
        assertEquals(meta1.getObjectID(),            meta2.getObjectID());
        assertEquals(meta1.getObjectName(),          meta2.getObjectName());
        assertEquals(meta1.getCenterName(),          meta2.getCenterName());
        assertEquals(meta1.getTimeSystem(),          meta2.getTimeSystem());
        assertEquals(meta1.getLaunchYear(),          meta2.getLaunchYear());
        assertEquals(meta1.getLaunchNumber(),        meta2.getLaunchNumber());
        assertEquals(meta1.getLaunchPiece(),         meta2.getLaunchPiece());
        assertEquals(meta1.getHasCreatableBody(),    meta2.getHasCreatableBody());
        assertEquals(meta1.getInterpolationDegree(), meta2.getInterpolationDegree());

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

    static void compareAemFiles(AEMFile file1, AEMFile file2) {
        assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        assertEquals(file1.getSegments().size(), file2.getSegments().size());
        for (int i = 0; i < file1.getSegments().size(); i++) {
            compareAemAttitudeBlocks(file1.getSegments().get(i), file2.getSegments().get(i));
        }
    }

    private class StandInEphemerisFile extends AEMFile {
        private final Map<String, AEMSatelliteEphemeris> satEphem;

        /** Simple constructor.
         * @param conventions IERS conventions
         * @param dataContext used for creating frames, time scales, etc.
         * @param missionReferenceDate reference date for Mission Elapsed Time and Mission Relative Time time systems.
         */
        public StandInEphemerisFile(final IERSConventions conventions, final DataContext dataContext,
                                    final AbsoluteDate missionReferenceDate) {
            super(conventions, dataContext, missionReferenceDate);
            this.satEphem = new HashMap<String, AEMSatelliteEphemeris>();
        }

        @Override
        public Map<String, AEMSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

}
