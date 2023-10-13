/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.odm.ocm;

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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
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
import org.orekit.files.ccsds.definitions.CenterName;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class EphemerisOcmWriterTest {

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOCMWriter() {
        Assertions.assertNotNull(new WriterBuilder().buildOcmWriter());
    }

    @Test
    public void testWriteOCM3Kvn() throws IOException {
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, 0, "", Constants.JULIAN_DAY, 60);
        doTestWriteOCM3(caw, generator);
    }

    @Test
    public void testWriteOCM3Xml() throws IOException {
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new XmlGenerator(caw, 2, "", Constants.JULIAN_DAY, true, XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION);
        doTestWriteOCM3(caw, generator);
    }

    private void doTestWriteOCM3(final CharArrayWriter caw, Generator generator) throws IOException {
        final String ex = "/ccsds/odm/ocm/OCMExample3.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OcmParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOcmParser();
        final Ocm ocm = parser.parseMessage(source);

        OcmWriter writer = new WriterBuilder().
                        withConventions(IERSConventions.IERS_2010).
                        withDataContext(DataContext.getDefault()).
                        buildOcmWriter();
        writer.writeMessage(generator, ocm);
        final byte[] bytes = caw.toString().getBytes(StandardCharsets.UTF_8);

        final Ocm generatedOcm = new ParserBuilder().
                                         withConventions(IERSConventions.IERS_2010).
                                         withSimpleEOP(true).
                                         withDataContext(DataContext.getDefault()).
                                         withMu(Constants.EIGEN5C_EARTH_MU).
                                         withDefaultInterpolationDegree(1).
                                         buildOcmParser().
                                         parseMessage(new DataSource("", () -> new ByteArrayInputStream(bytes)));
        compareOcms(ocm, generatedOcm);
    }

    @Test
    public void testUnfoundSpaceId() throws IOException {
        final String ex = "/ccsds/odm/ocm/OCMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OcmParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOcmParser();
        final Ocm ocm = parser.parseMessage(source);

        EphemerisOcmWriter writer = new EphemerisOcmWriter(new WriterBuilder().buildOcmWriter(),
                                                           ocm.getHeader(), dummyMetadata(),
                                                           dummyTrajectoryMetadata(), FileFormat.KVN, "",
                                                           Constants.JULIAN_DAY, 0);
        try {
            writer.write(new CharArrayWriter(), ocm);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.VALUE_NOT_FOUND, oiae.getSpecifier());
            Assertions.assertEquals(dummyMetadata().getInternationalDesignator(), oiae.getParts()[0]);
        }

    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ccsds/odm/ocm/OCMExample1.txt";
        final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OcmParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOcmParser();
        final Ocm ocm = parser.parseMessage(source);
        EphemerisOcmWriter writer = new EphemerisOcmWriter(new WriterBuilder().buildOcmWriter(),
                                                           ocm.getHeader(),
                                                           ocm.getSegments().get(0).getMetadata(),
                                                           ocm.getSegments().get(0).getData().getTrajectoryBlocks().get(0).getMetadata(),
                                                           FileFormat.KVN, "dummy",
                                                           Constants.JULIAN_DAY, 0);
        try {
            writer.write((BufferedWriter) null, ocm);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            Assertions.assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        EphemerisOcmWriter writer = new EphemerisOcmWriter(new WriterBuilder().buildOcmWriter(),
                                                           null, dummyMetadata(), dummyTrajectoryMetadata(),
                                                           FileFormat.KVN, "nullEphemeris",
                                                           Constants.JULIAN_DAY, 60);
        CharArrayWriter caw = new CharArrayWriter();
        writer.write(caw, null);
        Assertions.assertEquals(0, caw.size());
    }

    @Test
    public void testGenerateKVN() throws IOException {
        doTestGenerate(FileFormat.KVN, 45);
    }

    @Test
    public void testGenerateXML() throws IOException {
        doTestGenerate(FileFormat.XML, 55);
    }

    private void doTestGenerate(FileFormat format, int expectedLines) throws IOException {

        final DataContext context = DataContext.getDefault();
        final String id = "1999-012A";
        StandAloneEphemerisFile file = new StandAloneEphemerisFile();
        file.generate(id, OrbitElementsType.CARTPV, context.getFrames().getEME2000(),
                      new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH,
                                                   new Vector3D(1.0e6, 2.0e6, 3.0e6),
                                                   new Vector3D(-300, -200, -100)),
                      900.0, 60.0);


        OcmMetadata metadata = dummyMetadata();
        metadata.setEpochT0(AbsoluteDate.GALILEO_EPOCH);
        metadata.setInternationalDesignator(id);
        EphemerisOcmWriter writer = new EphemerisOcmWriter(new WriterBuilder().withDataContext(context).buildOcmWriter(),
                                                           null, metadata, dummyTrajectoryMetadata(), format, "",
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
        Assertions.assertEquals(expectedLines, count);

    }

    private static void compareOcmEphemerisBlocks(TrajectoryStateHistory block1, TrajectoryStateHistory block2) {
        compareOcmEphemerisBlocksMetadata(block1.getMetadata(), block2.getMetadata());
        Assertions.assertEquals(0.0, block1.getStart().durationFrom(block2.getStart()), 1.0e-12);
        Assertions.assertEquals(0.0, block1.getStop().durationFrom(block2.getStop()), 1.0e-12);
        Assertions.assertEquals(block1.getMetadata().getInterpolationDegree(), block2.getMetadata().getInterpolationDegree());
        Assertions.assertEquals(block1.getMetadata().getInterpolationMethod(), block2.getMetadata().getInterpolationMethod());
        Assertions.assertEquals(block1.getTrajectoryStates().size(), block2.getTrajectoryStates().size());
        for (int i = 0; i < block1.getTrajectoryStates().size(); i++) {
            TrajectoryState c1 = block1.getTrajectoryStates().get(i);
            TrajectoryState c2 = block2.getTrajectoryStates().get(i);
            Assertions.assertEquals(0.0, c1.getDate().durationFrom(c2.getDate()), 1.0e-12);
            Assertions.assertEquals(c1.getType(), c2.getType());
            Assertions.assertEquals(c1.getElements().length, c2.getElements().length);
            for (int j = 0; j < c1.getElements().length; ++j) {
            Assertions.assertTrue(Precision.equals(c1.getElements()[j], c2.getElements()[j], 2));
            }
        }
    }

    private static void compareOcmEphemerisBlocksMetadata(TrajectoryStateHistoryMetadata meta1, TrajectoryStateHistoryMetadata meta2) {
        Assertions.assertEquals(meta1.getTrajID(),                                 meta2.getTrajID());
        Assertions.assertEquals(meta1.getTrajPrevID(),                             meta2.getTrajPrevID());
        Assertions.assertEquals(meta1.getTrajNextID(),                             meta2.getTrajNextID());
        Assertions.assertEquals(meta1.getTrajBasis(),                              meta2.getTrajBasis());
        Assertions.assertEquals(meta1.getTrajBasisID(),                            meta2.getTrajBasisID());
        Assertions.assertEquals(meta1.getInterpolationMethod(),                    meta2.getInterpolationMethod());
        Assertions.assertEquals(meta1.getInterpolationDegree(),                    meta2.getInterpolationDegree());
        Assertions.assertEquals(meta1.getPropagator(),                             meta2.getPropagator());
        Assertions.assertEquals(meta1.getCenter().getName(),                       meta2.getCenter().getName());
        Assertions.assertEquals(meta1.getTrajReferenceFrame().getName(),           meta2.getTrajReferenceFrame().getName());
        Assertions.assertEquals(meta1.getTrajFrameEpoch(),                         meta2.getTrajFrameEpoch());
        Assertions.assertEquals(meta1.getUseableStartTime(),                       meta2.getUseableStartTime());
        Assertions.assertEquals(meta1.getUseableStopTime(),                        meta2.getUseableStopTime());
        Assertions.assertEquals(meta1.getOrbRevNum(),                              meta2.getOrbRevNum());
        Assertions.assertEquals(meta1.getOrbRevNumBasis(),                         meta2.getOrbRevNumBasis());
        Assertions.assertEquals(meta1.getOrbAveraging(),                           meta2.getOrbAveraging());
        Assertions.assertEquals(meta1.getTrajType(),                               meta2.getTrajType());
        Assertions.assertEquals(meta1.getTrajUnits().size(),                       meta2.getTrajUnits().size());
        for (int i = 0; i < meta1.getTrajUnits().size(); ++i) {
            Assertions.assertEquals(meta1.getTrajUnits().get(i), meta2.getTrajUnits().get(i));
        }
    }

    static void compareOcms(Ocm file1, Ocm file2) {
        Assertions.assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        Assertions.assertEquals(file1.getSegments().get(0).getData().getTrajectoryBlocks().size(),
                                file2.getSegments().get(0).getData().getTrajectoryBlocks().size());
        for (int i = 0; i < file1.getSegments().get(0).getData().getTrajectoryBlocks().size(); i++) {
            compareOcmEphemerisBlocks(file1.getSegments().get(0).getData().getTrajectoryBlocks().get(i),
                                      file2.getSegments().get(0).getData().getTrajectoryBlocks().get(i));
        }
    }

    private class StandAloneEphemerisFile
        implements EphemerisFile<TimeStampedPVCoordinates, TrajectoryStateHistory> {
        private final Map<String, OcmSatelliteEphemeris> satEphem;

        /** Simple constructor.
         */
        public StandAloneEphemerisFile() {
            this.satEphem = new HashMap<>();
        }

        private void generate(final String internationalDesignator, final OrbitElementsType type,
                              final Frame referenceFrame, final TimeStampedPVCoordinates pv0,
                              final double duration, final double step) {

            TrajectoryStateHistoryMetadata metadata = dummyTrajectoryMetadata();
            metadata.addComment("generated for " + internationalDesignator);
            metadata.setUseableStartTime(pv0.getDate());
            metadata.setUseableStopTime(pv0.getDate().shiftedBy(duration));

            List<TrajectoryState> states = new ArrayList<>();
            for (double dt = 0; dt < duration; dt += step) {
                TimeStampedPVCoordinates pv = pv0.shiftedBy(dt);
                double[] elements = type.toRawElements(pv, referenceFrame, null, Constants.EIGEN5C_EARTH_MU);
                states.add(new TrajectoryState(type, pv.getDate(), elements));
            }

            if (!satEphem.containsKey(internationalDesignator)) {
                satEphem.put(internationalDesignator,
                             new OcmSatelliteEphemeris(internationalDesignator, Constants.EIGEN5C_EARTH_MU, Collections.emptyList()));
            }

            List<TrajectoryStateHistory> history = new ArrayList<>(satEphem.get(internationalDesignator).getSegments());
            history.add(new TrajectoryStateHistory(metadata, states, null, Constants.EIGEN5C_EARTH_MU));
            satEphem.put(internationalDesignator, new OcmSatelliteEphemeris(internationalDesignator, Constants.EIGEN5C_EARTH_MU, history));

        }

        @Override
        public Map<String, OcmSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

    private OcmMetadata dummyMetadata() {
        OcmMetadata metadata = new OcmMetadata(DataContext.getDefault());
        metadata.addComment("dummy metadata comment");
        metadata.setTimeSystem(TimeSystem.TT);
        metadata.setInternationalDesignator("9999-999ZZZ");
        metadata.setObjectName("transgalactic");
        metadata.setEpochT0(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
        metadata.setStartTime(metadata.getEpochT0());
        metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));
        return metadata;
    }

    private TrajectoryStateHistoryMetadata dummyTrajectoryMetadata() {
        final AbsoluteDate t0 = new AbsoluteDate(2003, 5, 7, 19, 43, 56.75, TimeScalesFactory.getUTC());
        TrajectoryStateHistoryMetadata metadata = new TrajectoryStateHistoryMetadata(t0, DataContext.getDefault());
        metadata.addComment("dummy trajectory comment");
        metadata.setTrajID("traj 17");
        metadata.setTrajBasis("PREDICTED");
        metadata.setTrajBasisID("simulation 22");
        metadata.setInterpolationMethod(InterpolationMethod.HERMITE);
        metadata.setInterpolationDegree(4);
        metadata.setPropagator("Orekit");
        metadata.setCenter(BodyFacade.create(CenterName.EARTH));
        metadata.setTrajReferenceFrame(FrameFacade.map(FramesFactory.getTOD(IERSConventions.IERS_2010, false)));
        metadata.setTrajFrameEpoch(new AbsoluteDate(2003, 5, 7, TimeScalesFactory.getUTC()));
        metadata.setUseableStartTime(t0);
        metadata.setUseableStopTime(t0.shiftedBy(3600.0));
        metadata.setOrbRevNum(12);
        metadata.setOrbRevNumBasis(0);
        metadata.setOrbAveraging("OSCULATING");
        metadata.setTrajType(OrbitElementsType.KEPLERIAN);
        metadata.setTrajUnits(metadata.getTrajType().getUnits());
        return metadata;
    }

}
