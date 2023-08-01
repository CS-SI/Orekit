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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
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
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
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
    @BeforeEach
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
            Oem oem = parser.parseMessage(source0);

            OemSatelliteEphemeris satellite = oem.getSatellites().values().iterator().next();
            OemSegment ephemerisBlock = satellite.getSegments().get(0);
            double step = ephemerisBlock.
                          getMetadata().
                          getStopTime().
                          durationFrom(ephemerisBlock.getMetadata().getStartTime()) /
                          (ephemerisBlock.getCoordinates().size() - 1);
            String originator = oem.getHeader().getOriginator();
            OemSegment block = oem.getSegments().get(0);
            String objectName = block.getMetadata().getObjectName();
            String objectID = block.getMetadata().getObjectID();
            Frame frame = satellite.getSegments().get(0).getInertialFrame();

            OdmHeader header = new OdmHeader();
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
            StreamingOemWriter writer = new StreamingOemWriter(new KvnGenerator(buffer1, OemWriter.KVN_PADDING_WIDTH, "some-name",
                                                                                Constants.JULIAN_DAY, 60),
                                                               new WriterBuilder().buildOemWriter(),
                                                               header, metadata);
            BoundedPropagator propagator = satellite.getPropagator(new FrameAlignedProvider(frame));
            propagator.setStepHandler(step, writer.newSegment());
            propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());
            writer.close();

            // verify
            final DataSource source1 = new DataSource("buffer",
                                                    () -> new ByteArrayInputStream(buffer1.toString().getBytes(StandardCharsets.UTF_8)));
            Oem generatedOem = new ParserBuilder().
                                       withConventions(IERSConventions.IERS_2010).
                                       withSimpleEOP(true).
                                       withDataContext(DataContext.getDefault()).
                                       withMu(CelestialBodyFactory.getEarth().getGM()).
                                       withDefaultInterpolationDegree(1).
                                       buildOemParser().
                                       parseMessage(source1);
            compareOems(oem, generatedOem, POSITION_PRECISION, VELOCITY_PRECISION);

            // check calling the methods directly
            final StringBuilder buffer2 = new StringBuilder();
            OemWriter oemWriter = new WriterBuilder().buildOemWriter();
            try (Generator generator = new KvnGenerator(buffer2, OemWriter.KVN_PADDING_WIDTH, "another-name",
                                                        Constants.JULIAN_DAY, 60)) {
                oemWriter.writeHeader(generator, header);
                metadata.setObjectName(objectName);
                metadata.setStartTime(block.getStart());
                metadata.setStopTime(block.getStop());
                metadata.setReferenceFrame(FrameFacade.map(frame));
                oemWriter.writeMetadata(generator, metadata);
                for (TimeStampedPVCoordinates coordinate : block.getCoordinates()) {
                    oemWriter.writeOrbitEphemerisLine(generator, metadata, coordinate, true);
                }
            }

            // verify
            final DataSource source2 = new DataSource("buffer",
                                                    () -> new ByteArrayInputStream(buffer2.toString().getBytes(StandardCharsets.UTF_8)));
            generatedOem = new ParserBuilder().
                               withConventions(IERSConventions.IERS_2010).
                               withSimpleEOP(true).
                               withDataContext(DataContext.getDefault()).
                               withMu(CelestialBodyFactory.getEarth().getGM()).
                               withDefaultInterpolationDegree(1).
                               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
                               buildOemParser().
                               parseMessage(source2);
            compareOems(oem, generatedOem, POSITION_PRECISION, VELOCITY_PRECISION);

        }

    }

    @Test
    public void testWriteOemEcfNoInterpolation() {
        // setup
        String path = "/ccsds/odm/oem/OEMExample5.txt";
        DataSource source = new DataSource(path, () -> getClass().getResourceAsStream(path));
        final OemParser oemParser = new ParserBuilder().buildOemParser();
        final Oem original = oemParser.parse(source);
        final OemSatelliteEphemeris originalEphem =
                original.getSatellites().values().iterator().next();
        final Frame frame = originalEphem.getSegments().get(0).getInertialFrame();
        final BoundedPropagator propagator = originalEphem.getPropagator(new FrameAlignedProvider(frame));
        StringBuilder buffer = new StringBuilder();
        OdmHeader header = original.getHeader();
        OemMetadata metadata = original.getSegments().get(0).getMetadata();
        metadata.setTimeSystem(TimeSystem.UTC);
        metadata.setReferenceFrame(FrameFacade.map(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        metadata.setInterpolationMethod(null);
        metadata.setInterpolationDegree(-1);

        // action
        StreamingOemWriter writer = new StreamingOemWriter(
                new KvnGenerator(buffer, OemWriter.KVN_PADDING_WIDTH, "out",
                                 Constants.JULIAN_DAY, 0),
                new WriterBuilder().buildOemWriter(),
                header,
                metadata,
                false,
                false);
        propagator.setStepHandler(30 * 60, writer.newSegment());
        propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());

        // verify
        String actualText = buffer.toString();
        String expectedPath = "/ccsds/odm/oem/OEMExample5ITRF.txt";
        Oem expected = oemParser.parse(
                new DataSource(expectedPath, () -> getClass().getResourceAsStream(expectedPath)));
        Oem actual = oemParser.parse(
                new DataSource("mem", () -> new StringReader(actualText)));

        compareOems(expected, actual, 1.9e-5, 2.2e-8);
        MatcherAssert.assertThat(
                actualText,
                CoreMatchers.not(CoreMatchers.containsString("INTERPOLATION_DEGREE")));
        // check no acceleration
        MatcherAssert.assertThat(
                actualText,
                CoreMatchers.containsString(
                        "\n2017-04-11T22:31:43.121856 -2757.3016318855234 -4173.47960139054 4566.018498013474 6.625901653955907 -1.0118172088819106 3.0698336591485442\n"));
    }

    private static void compareOemEphemerisBlocks(OemSegment block1,
                                                  OemSegment block2,
                                                  double p_tol,
                                                  double v_tol) {
        compareOemEphemerisBlocksMetadata(block1.getMetadata(), block2.getMetadata());
        Assertions.assertEquals(block1.getStart(), block2.getStart());
        Assertions.assertEquals(block1.getStop(), block2.getStop());
        Assertions.assertEquals(block1.getData().getEphemeridesDataLines().size(), block2.getData().getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getData().getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getData().getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getData().getEphemeridesDataLines().get(i);
            Assertions.assertEquals(c1.getDate(), c2.getDate(),"" + i);
            Assertions.assertEquals(0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), p_tol,c1.getPosition() + " -> " + c2.getPosition());
            Assertions.assertEquals(0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), v_tol,c1.getVelocity() + " -> " + c2.getVelocity());
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

    static void compareOems(Oem file1, Oem file2, double p_tol, double v_tol) {
        Assertions.assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        Assertions.assertEquals(file1.getSegments().size(), file2.getSegments().size());
        for (int i = 0; i < file1.getSegments().size(); i++) {
            compareOemEphemerisBlocks(file1.getSegments().get(i), file2.getSegments().get(i), p_tol, v_tol);
        }
    }

}
