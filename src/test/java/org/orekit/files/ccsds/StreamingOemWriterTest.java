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
package org.orekit.files.ccsds;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.files.ccsds.OEMFile.EphemeridesBlock;
import org.orekit.files.ccsds.OEMFile.OemSatelliteEphemeris;
import org.orekit.files.ccsds.StreamingOemWriter.Segment;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
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
     * Check guessing the CCSDS frame name for some frames.
     */
    @Test
    public void testGuessFrame() {
        // action + verify
        // check all non-LOF frames created by OEMParser
        for (CCSDSFrame ccsdsFrame : CCSDSFrame.values()) {
            if (!ccsdsFrame.isLof()) {
                Frame frame = ccsdsFrame.getFrame(IERSConventions.IERS_2010, true);
                String actual = StreamingOemWriter.guessFrame(frame);
                MatcherAssert.assertThat(actual, CoreMatchers.is(ccsdsFrame.name()));
            }
        }

        // check common Orekit frames from FramesFactory
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getGCRF()),
                CoreMatchers.is("GCRF"));
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getEME2000()),
                CoreMatchers.is("EME2000"));
        MatcherAssert.assertThat(
                StreamingOemWriter.guessFrame(
                        FramesFactory.getITRFEquinox(IERSConventions.IERS_2010, true)),
                CoreMatchers.is("GRC"));
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getICRF()),
                CoreMatchers.is("ICRF"));
        MatcherAssert.assertThat(
                   StreamingOemWriter.guessFrame(
                           FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                   CoreMatchers.is("ITRF2014"));
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getGTOD(true)),
                CoreMatchers.is("TDR"));
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getTEME()),
                CoreMatchers.is("TEME"));
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getTOD(true)),
                CoreMatchers.is("TOD"));

        // check that guessed name loses the IERS conventions and simpleEOP flag
        for (ITRFVersion version : ITRFVersion.values()) {
            final String name = version.getName().replaceAll("-", "");
            for (final IERSConventions conventions : IERSConventions.values()) {
                MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getITRF(version, conventions, true)),
                           CoreMatchers.is(name));
                MatcherAssert.assertThat(StreamingOemWriter.guessFrame(FramesFactory.getITRF(version, conventions, false)),
                           CoreMatchers.is(name));
            }
        }

        // check other names in Annex A
        MatcherAssert.assertThat(
                StreamingOemWriter.guessFrame(
                        CelestialBodyFactory.getMars().getInertiallyOrientedFrame()),
                CoreMatchers.is("MCI"));
        MatcherAssert.assertThat(
                StreamingOemWriter.guessFrame(
                        CelestialBodyFactory.getSolarSystemBarycenter()
                                .getInertiallyOrientedFrame()),
                CoreMatchers.is("ICRF"));
        // check some special CCSDS frames
        CcsdsModifiedFrame frame = new CcsdsModifiedFrame(
                FramesFactory.getEME2000(), "EME2000",
                CelestialBodyFactory.getMars(), "MARS");
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(frame), CoreMatchers.is("EME2000"));
        Vector3D v = frame.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getTranslation();
        FieldVector3D<Decimal64> v64 = frame.getTransformProvider().getTransform(FieldAbsoluteDate.getJ2000Epoch(Decimal64Field.getInstance())).getTranslation();
        Assert.assertEquals(0.0, FieldVector3D.distance(v64, v).getReal(), 1.0e-10);

        // check unknown frame
        Frame topo = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                               Constants.WGS84_EARTH_FLATTENING,
                                                               FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                          new GeodeticPoint(1.2, 2.3, 45.6),
                                          "dummy");
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(topo), CoreMatchers.is("dummy"));

        // check a fake ICRF
        Frame fakeICRF = new Frame(FramesFactory.getGCRF(), Transform.IDENTITY,
                                      CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER + "/inertial");
        MatcherAssert.assertThat(StreamingOemWriter.guessFrame(fakeICRF), CoreMatchers.is("ICRF"));
    }

    /**
     * Check guessing the frame center for some frames.
     */
    @Test
    public void testGuessCenter() {
        // action + verify
        // check all CCSDS common center names
        List<CenterName> centerNames =
                new ArrayList<>(Arrays.asList(CenterName.values()));
        centerNames.remove(CenterName.EARTH_MOON);
        for (CenterName centerName : centerNames) {
            CelestialBody body = centerName.getCelestialBody();
            String name = centerName.name().replace('_', ' ');
            MatcherAssert.assertThat(StreamingOemWriter.guessCenter(body.getInertiallyOrientedFrame()),
                    CoreMatchers.is(name));
            MatcherAssert.assertThat(StreamingOemWriter.guessCenter(body.getBodyOrientedFrame()),
                    CoreMatchers.is(name));
        }
        // Earth-Moon Barycenter is special
        CelestialBody emb = CenterName.EARTH_MOON.getCelestialBody();
        MatcherAssert.assertThat(StreamingOemWriter.guessCenter(emb.getInertiallyOrientedFrame()),
                CoreMatchers.is("EARTH-MOON BARYCENTER"));
        MatcherAssert.assertThat(StreamingOemWriter.guessCenter(emb.getBodyOrientedFrame()),
                CoreMatchers.is("EARTH-MOON BARYCENTER"));
        // check some special CCSDS frames
        CcsdsModifiedFrame frame = new CcsdsModifiedFrame(
                FramesFactory.getEME2000(), "EME2000",
                CelestialBodyFactory.getMars(), "MARS");
        MatcherAssert.assertThat(StreamingOemWriter.guessCenter(frame), CoreMatchers.is("MARS"));

        // check unknown frame
        Frame topo = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                               Constants.WGS84_EARTH_FLATTENING,
                                                               FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                          new GeodeticPoint(1.2, 2.3, 45.6),
                                          "dummy");
        MatcherAssert.assertThat(StreamingOemWriter.guessCenter(topo), CoreMatchers.is("UNKNOWN"));
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
        TimeScale utc = TimeScalesFactory.getUTC();
        List<String> files =
                Arrays.asList("/ccsds/OEMExample5.txt", "/ccsds/OEMExample4.txt");
        for (String ex : files) {
            InputStream inEntry = getClass().getResourceAsStream(ex);
            OEMParser parser = new OEMParser()
                    .withMu(CelestialBodyFactory.getEarth().getGM())
                    .withConventions(IERSConventions.IERS_2010);
            OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");

            OemSatelliteEphemeris satellite =
                    oemFile.getSatellites().values().iterator().next();
            EphemeridesBlock ephemerisBlock = satellite.getSegments().get(0);
            Frame frame = ephemerisBlock.getFrame();
            double step = ephemerisBlock.getStopTime()
                    .durationFrom(ephemerisBlock.getStartTime()) /
                    (ephemerisBlock.getCoordinates().size() - 1);
            String originator = oemFile.getOriginator();
            EphemeridesBlock block = oemFile.getEphemeridesBlocks().get(0);
            String objectName = block.getMetaData().getObjectName();
            String objectID = block.getMetaData().getObjectID();

            Map<Keyword, String> metadata = new LinkedHashMap<>();
            metadata.put(Keyword.ORIGINATOR, originator);
            metadata.put(Keyword.OBJECT_NAME, "will be overwritten");
            metadata.put(Keyword.OBJECT_ID, objectID);
            Map<Keyword, String> segmentData = new LinkedHashMap<>();
            segmentData.put(Keyword.OBJECT_NAME, objectName);

            // check using the Propagator / StepHandler interface
            StringBuilder buffer = new StringBuilder();
            StreamingOemWriter writer = new StreamingOemWriter(buffer, utc, metadata);
            writer.writeHeader();
            Segment segment = writer.newSegment(frame, segmentData);
            BoundedPropagator propagator = satellite.getPropagator();
            propagator.setMasterMode(step, segment);
            propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());

            // verify
            BufferedReader reader =
                    new BufferedReader(new StringReader(buffer.toString()));
            OEMFile generatedOemFile = parser.parse(reader, "buffer");
            compareOemFiles(oemFile, generatedOemFile, POSITION_PRECISION, VELOCITY_PRECISION);

            // check calling the methods directly
            buffer = new StringBuilder();
            writer = new StreamingOemWriter(buffer, utc, metadata);
            writer.writeHeader();
            // set start and stop date manually
            segmentData.put(Keyword.START_TIME,
                    StreamingOemWriter.dateToString(block.getStart().getComponents(utc)));
            segmentData.put(Keyword.STOP_TIME,
                    StreamingOemWriter.dateToString(block.getStop().getComponents(utc)));
            segment = writer.newSegment(frame, segmentData);
            segment.writeMetadata();
            for (TimeStampedPVCoordinates coordinate : block.getCoordinates()) {
                segment.writeEphemerisLine(coordinate);
            }

            // verify
            reader = new BufferedReader(new StringReader(buffer.toString()));
            generatedOemFile = parser.parse(reader, "buffer");
            compareOemFiles(oemFile, generatedOemFile, POSITION_PRECISION, VELOCITY_PRECISION);

        }

    }

    private static void compareOemEphemerisBlocks(EphemeridesBlock block1,
                                                  EphemeridesBlock block2,
                                                  double p_tol,
                                                  double v_tol) {
        compareOemEphemerisBlocksMetadata(block1.getMetaData(), block2.getMetaData());
        assertEquals(block1.getStart(), block2.getStart());
        assertEquals(block1.getStop(), block2.getStop());
        assertEquals(block1.getEphemeridesDataLines().size(), block2.getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getEphemeridesDataLines().get(i);
            assertEquals(c1.getDate(), c2.getDate());
            assertEquals(c1.getPosition() + " -> " + c2.getPosition(), 0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), p_tol);
            assertEquals(c1.getVelocity() + " -> " + c2.getVelocity(), 0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), v_tol);
        }

    }

    private static void compareOemEphemerisBlocksMetadata(ODMMetaData meta1, ODMMetaData meta2) {
        assertEquals(meta1.getObjectID(), meta2.getObjectID());
        assertEquals(meta1.getObjectName(), meta2.getObjectName());
        assertEquals(meta1.getCenterName(), meta2.getCenterName());
        assertEquals(meta1.getFrameString(), meta2.getFrameString());
        assertEquals(meta1.getTimeSystem(), meta2.getTimeSystem());
    }

    static void compareOemFiles(OEMFile file1,
                                OEMFile file2,
                                double p_tol,
                                double v_tol) {
        assertEquals(file1.getOriginator(), file2.getOriginator());
        assertEquals(file1.getEphemeridesBlocks().size(), file2.getEphemeridesBlocks().size());
        for (int i = 0; i < file1.getEphemeridesBlocks().size(); i++) {
            compareOemEphemerisBlocks(
                    file1.getEphemeridesBlocks().get(i),
                    file2.getEphemeridesBlocks().get(i),
                    p_tol,
                    v_tol);
        }
    }

    /**
     * Check writing an OEM with format parameters for position and velocity.
     *
     * @throws IOException on error
     */
    @Test
    public void testWriteOemFormat() throws IOException {
        // setup
        String exampleFile = "/ccsds/OEMExample4.txt";
        InputStream inEntry = getClass().getResourceAsStream(exampleFile);
        OEMParser parser = new OEMParser();
        OEMFile oemFile = parser.parse(inEntry, "OEMExample4.txt");

        EphemeridesBlock block = oemFile.getEphemeridesBlocks().get(0);
        Frame frame = block.getFrame();

        TimeScale utc = TimeScalesFactory.getUTC();
        Map<Keyword, String> metadata = new LinkedHashMap<>();
        Map<Keyword, String> segmentData = new LinkedHashMap<>();

        StringBuilder buffer = new StringBuilder();
        StreamingOemWriter writer = new StreamingOemWriter(buffer, utc, metadata, "%.2f", "%.3f");
        Segment segment = writer.newSegment(frame, segmentData);

        for (TimeStampedPVCoordinates coordinate : block.getCoordinates()) {
            segment.writeEphemerisLine(coordinate);
        }

        String expected = "2002-12-18T12:00:00.331 2789.62 -280.05 -1746.76 4.734 -2.496 -1.042\n"
                        + "2002-12-18T12:01:00.331 2783.42 -308.14 -1877.07 5.186 -2.421 -1.996\n"
                        + "2002-12-18T12:02:00.331 2776.03 -336.86 -2008.68 5.637 -2.340 -1.947\n";

        assertEquals(buffer.toString(), expected);

        buffer = new StringBuilder();
        writer = new StreamingOemWriter(buffer, utc, metadata);
        segment = writer.newSegment(frame, segmentData);

        for (TimeStampedPVCoordinates coordinate : block.getCoordinates()) {
            segment.writeEphemerisLine(coordinate);
        }

        expected = "2002-12-18T12:00:00.331  2789.619 -280.045 -1746.755  4.73372 -2.49586 -1.04195\n"
                 + "2002-12-18T12:01:00.331  2783.419 -308.143 -1877.071  5.18604 -2.42124 -1.99608\n"
                 + "2002-12-18T12:02:00.331  2776.033 -336.859 -2008.682  5.63678 -2.33951 -1.94687\n";
;

        assertEquals(buffer.toString(), expected);

    }

}
