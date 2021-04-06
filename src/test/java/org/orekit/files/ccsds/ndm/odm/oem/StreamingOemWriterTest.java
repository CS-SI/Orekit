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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.CenterName;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.ModifiedFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Check {@link StreamingOemWriter}.
 *
 * @author Evan Ward
 */
public class StreamingOemWriterTest {
    // As the default format for position is 3 digits after decimal point in km the max precision in m is 1
    private static final double POSITION_PRECISION = 1; // in m
    // As the default format for velocity is 5 digits after decimal point in km/s the max precision in m/s is 1e-2
    private static final double VELOCITY_PRECISION = 1e-2; //in m/s

    /** Set Orekit data. */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check guessing the frame center for some frames.
     */
    @Test
    public void testGuessCenter() {
        // action + verify
        // check all CCSDS common center names
        List<CenterName> centerNames = new ArrayList<>(Arrays.asList(CenterName.values()));
        centerNames.remove(CenterName.EARTH_MOON);
        for (CenterName centerName : centerNames) {
            CelestialBody body = centerName.getCelestialBody();
            String name = centerName.name().replace('_', ' ');
            MatcherAssert.assertThat(CenterName.guessCenter(body.getInertiallyOrientedFrame()),
                                     CoreMatchers.is(name));
            MatcherAssert.assertThat(CenterName.guessCenter(body.getBodyOrientedFrame()),
                                     CoreMatchers.is(name));
        }
        // Earth-Moon Barycenter is special
        CelestialBody emb = CenterName.EARTH_MOON.getCelestialBody();
        MatcherAssert.assertThat(CenterName.guessCenter(emb.getInertiallyOrientedFrame()),
                                 CoreMatchers.is("EARTH-MOON BARYCENTER"));
        MatcherAssert.assertThat(CenterName.guessCenter(emb.getBodyOrientedFrame()),
                                 CoreMatchers.is("EARTH-MOON BARYCENTER"));
        // check some special CCSDS frames
        ModifiedFrame frame = new ModifiedFrame(FramesFactory.getEME2000(),
                                                          CelestialBodyFrame.EME2000,
                                                          CelestialBodyFactory.getMars(), "MARS");
        MatcherAssert.assertThat(CenterName.guessCenter(frame), CoreMatchers.is("MARS"));

        // check unknown frame
        Frame topo = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                               Constants.WGS84_EARTH_FLATTENING,
                                                               FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                          new GeodeticPoint(1.2, 2.3, 45.6),
                                          "dummy");
        MatcherAssert.assertThat(CenterName.guessCenter(topo), CoreMatchers.is("UNKNOWN"));
    }


    /**
     * Check reading and writing an OEM both with and without using the step handler
     * methods.
     *
     * @throws Exception on error.
     */
    @Test
    public void testWriteOemStepHandler() throws Exception {
        // setup
        List<String> files =
                Arrays.asList("/ccsds/odm/oem/OEMExample5.txt", "/ccsds/odm/oem/OEMExample4.txt");
        for (final String ex : files) {
            final DataSource source0 =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getEarth().getGM()).buildOemParser();
            OemFile oemFile = parser.parseMessage(source0);

            OemSatelliteEphemeris satellite = oemFile.getSatellites().values().iterator().next();
            OemSegment ephemerisBlock = satellite.getSegments().get(0);
            double step = ephemerisBlock.
                          getMetadata().
                          getStopTime().
                          durationFrom(ephemerisBlock.getMetadata().getStartTime()) /
                          (ephemerisBlock.getCoordinates().size() - 1);
            String originator = oemFile.getHeader().getOriginator();
            OemSegment block = oemFile.getSegments().get(0);
            String objectName = block.getMetadata().getObjectName();
            String objectID = block.getMetadata().getObjectID();

            Header header = new Header();
            header.setOriginator(originator);
            OemMetadata metadata = new OemMetadata(1);
            metadata.setObjectName(objectName);
            metadata.setObjectID(objectID);
            metadata.setTimeSystem(TimeSystem.UTC);
            metadata.setCenter(ephemerisBlock.getMetadata().getCenter());
            metadata.setReferenceFrame(FrameFacade.map(FramesFactory.getEME2000())); // will be overwritten
            metadata.setStartTime(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
            metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));


            // check using the Propagator / StepHandler interface
            final StringBuilder buffer1 = new StringBuilder();
            StreamingOemWriter writer = new StreamingOemWriter(new KvnGenerator(buffer1, OemWriter.KVN_PADDING_WIDTH, "some-name"),
                                                               new WriterBuilder().buildOemWriter(),
                                                               header, metadata);
            BoundedPropagator propagator = satellite.getPropagator();
            propagator.setMasterMode(step, writer.newSegment());
            propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());

            // verify
            final DataSource source1 = new DataSource("buffer",
                                                    () -> new ByteArrayInputStream(buffer1.toString().getBytes(StandardCharsets.UTF_8)));
            OemFile generatedOemFile = new OemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                     null, CelestialBodyFactory.getEarth().getGM(), 1).
                                       parseMessage(source1);
            compareOemFiles(oemFile, generatedOemFile, POSITION_PRECISION, VELOCITY_PRECISION);

            // check calling the methods directly
            final StringBuilder buffer2 = new StringBuilder();
            OemWriter oemWriter = new WriterBuilder().buildOemWriter();
            try (Generator generator = new KvnGenerator(buffer2, OemWriter.KVN_PADDING_WIDTH, "another-name")) {
                oemWriter.writeHeader(generator, header);
                metadata.setObjectName(objectName);
                metadata.setStartTime(block.getStart());
                metadata.setStopTime(block.getStop());
                final Frame      stateFrame = satellite.getPropagator().getFrame();
                metadata.setReferenceFrame(FrameFacade.map(stateFrame));
                oemWriter.writeMetadata(generator, metadata);
                for (TimeStampedPVCoordinates coordinate : block.getCoordinates()) {
                    oemWriter.writeOrbitEphemerisLine(generator, metadata, coordinate, true);
                }
            }

            // verify
            final DataSource source2 = new DataSource("buffer",
                                                    () -> new ByteArrayInputStream(buffer2.toString().getBytes(StandardCharsets.UTF_8)));
            generatedOemFile = new OemParser(IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                             null, CelestialBodyFactory.getEarth().getGM(), 1).
                               parseMessage(source2);
            compareOemFiles(oemFile, generatedOemFile, POSITION_PRECISION, VELOCITY_PRECISION);

        }

    }

    private static void compareOemEphemerisBlocks(OemSegment block1,
                                                  OemSegment block2,
                                                  double p_tol,
                                                  double v_tol) {
        compareOemEphemerisBlocksMetadata(block1.getMetadata(), block2.getMetadata());
        assertEquals(block1.getStart(), block2.getStart());
        assertEquals(block1.getStop(), block2.getStop());
        assertEquals(block1.getData().getEphemeridesDataLines().size(), block2.getData().getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getData().getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getData().getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getData().getEphemeridesDataLines().get(i);
            assertEquals(c1.getDate(), c2.getDate());
            assertEquals(c1.getPosition() + " -> " + c2.getPosition(), 0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), p_tol);
            assertEquals(c1.getVelocity() + " -> " + c2.getVelocity(), 0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), v_tol);
        }

    }

    private static void compareOemEphemerisBlocksMetadata(OemMetadata meta1, OemMetadata meta2) {
        assertEquals(meta1.getObjectID(),                               meta2.getObjectID());
        assertEquals(meta1.getObjectName(),                             meta2.getObjectName());
        assertEquals(meta1.getCenter().getName(),                       meta2.getCenter().getName());
        assertEquals(meta1.getReferenceFrame().asFrame(),               meta2.getReferenceFrame().asFrame());
        assertEquals(meta1.getReferenceFrame().asCelestialBodyFrame(),  meta2.getReferenceFrame().asCelestialBodyFrame());
        assertEquals(meta1.getReferenceFrame().asOrbitRelativeFrame(),  meta2.getReferenceFrame().asOrbitRelativeFrame());
        assertEquals(meta1.getReferenceFrame().asSpacecraftBodyFrame(), meta2.getReferenceFrame().asSpacecraftBodyFrame());
        assertEquals(meta1.getTimeSystem().name(),    meta2.getTimeSystem().name());
    }

    static void compareOemFiles(OemFile file1, OemFile file2, double p_tol, double v_tol) {
        assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        assertEquals(file1.getSegments().size(), file2.getSegments().size());
        for (int i = 0; i < file1.getSegments().size(); i++) {
            compareOemEphemerisBlocks(file1.getSegments().get(i), file2.getSegments().get(i), p_tol, v_tol);
        }
    }

}
