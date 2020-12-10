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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.files.ccsds.AEMFile.AemSatelliteEphemeris;
import org.orekit.files.ccsds.AEMFile.AttitudeEphemeridesBlock;
import org.orekit.files.ccsds.StreamingAemWriter.AEMSegment;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;


public class StreamingAemWriterTest {

    private static final double QUATERNION_PRECISION = 1e-5;
    private static final double DATE_PRECISION = 1e-3;

    /** Set Orekit data. */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check reading and writing an AEM both with and without using the step handler
     * methods.
     */
    @Test
    public void testWriteAemStepHandler() throws Exception {

        // Time scale
        TimeScale utc = TimeScalesFactory.getUTC();
        // Create a list of files
        List<String> files =
                Arrays.asList("/ccsds/AEMExample7.txt");
        for (String ex : files) {

            // Reference AEM file
            InputStream inEntry = getClass().getResourceAsStream(ex);
            AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).withConventions(IERSConventions.IERS_2010);
            AEMFile aemFile = parser.parse(inEntry, "AEMExample.txt");

            // Satellite attitude ephemeris as read from the reference file
            AemSatelliteEphemeris satellite =
                            aemFile.getSatellites().values().iterator().next();
            AttitudeEphemeridesBlock ephemerisBlock = satellite.getSegments().get(0);

            // Meta data are extracted from the reference file
            String originator   = aemFile.getOriginator();
            String objectName   = ephemerisBlock.getMetaData().getObjectName();
            String objectID     = ephemerisBlock.getMetaData().getObjectID();
            String headerCmt    = aemFile.getHeaderComment().get(0);
            String attitudeDir  = ephemerisBlock.getAttitudeDirection();
            String refFrameA    = ephemerisBlock.getRefFrameAString();
            String refFrameB    = ephemerisBlock.getRefFrameBString();
            String attitudeType = ephemerisBlock.getAttitudeType();
            String isFirst      = "LAST";

            // Initialize the map of Meta data
            // Here, we use only one data segment.
            Map<Keyword, String> metadata = new LinkedHashMap<>();
            metadata.put(Keyword.ORIGINATOR,  originator);
            metadata.put(Keyword.OBJECT_NAME, "will be overwritten");
            metadata.put(Keyword.OBJECT_ID,   objectID);
            metadata.put(Keyword.COMMENT,     headerCmt);
            Map<Keyword, String> segmentData = new LinkedHashMap<>();
            segmentData.put(Keyword.OBJECT_NAME,     objectName);
            segmentData.put(Keyword.ATTITUDE_DIR,    attitudeDir);
            segmentData.put(Keyword.QUATERNION_TYPE, isFirst);
            segmentData.put(Keyword.ATTITUDE_TYPE,   attitudeType);
            segmentData.put(Keyword.REF_FRAME_A,     refFrameA);
            segmentData.put(Keyword.REF_FRAME_B,     refFrameB.replace(' ', '_'));

            // Initialize a Keplerian propagator with an Inertial attitude provider
            // It is expected that all attitude data lines will have the same value
            StringBuilder buffer = new StringBuilder();
            StreamingAemWriter writer = new StreamingAemWriter(buffer, utc, metadata);
            writer.writeHeader();
            AEMSegment segment = writer.newSegment(segmentData);
            
            KeplerianPropagator propagator = createPropagator(ephemerisBlock.getStart(),
                                                              new InertialProvider(ephemerisBlock.getAngularCoordinates().get(0).getRotation(),
                                                                                   FramesFactory.getEME2000()));

            // We propagate 60 seconds after the start date with a step equals to 10.0 seconds
            // It is expected to have an attitude data block containing 7 data lines
            double step = 10.0;
            propagator.setMasterMode(step, segment);
            propagator.propagate(ephemerisBlock.getStart().shiftedBy(60.0));

            // Generated AEM file
            BufferedReader reader =
                    new BufferedReader(new StringReader(buffer.toString()));
            AEMFile generatedAemFile = parser.parse(reader, "buffer");

            // There is only one attitude ephemeris block
            Assert.assertEquals(1, generatedAemFile.getAttitudeBlocks().size());
            AttitudeEphemeridesBlock attitudeBlocks = generatedAemFile.getAttitudeBlocks().get(0);
            // There are 7 data lines in the attitude ephemeris block
            List<TimeStampedAngularCoordinates> ac  = attitudeBlocks.getAngularCoordinates();
            Assert.assertEquals(7, ac.size());

            // Verify
            for (int i = 0; i < 7; i++) {
                Assert.assertEquals(step * i, ac.get(i).getDate().durationFrom(ephemerisBlock.getStart()), DATE_PRECISION);
                Rotation rot = ac.get(i).getRotation();
                Assert.assertEquals(0.68427, rot.getQ0(), QUATERNION_PRECISION);
                Assert.assertEquals(0.56748, rot.getQ1(), QUATERNION_PRECISION);
                Assert.assertEquals(0.03146, rot.getQ2(), QUATERNION_PRECISION);
                Assert.assertEquals(0.45689, rot.getQ3(), QUATERNION_PRECISION);
            }

        }

    }

    /**
     * Create a Keplerian propagator.
     * @param date reference date
     * @param attitudeProv attitude provider
     * @return a Keplerian propagator
     */
    private KeplerianPropagator createPropagator(AbsoluteDate date,
                                                 AttitudeProvider attitudeProv) {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        return new KeplerianPropagator(p, attitudeProv);
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
        AEMParser parser = new AEMParser();
        AEMFile aemFile = parser.parse(inEntry, "AEMExample7.txt");

        AttitudeEphemeridesBlock block = aemFile.getAttitudeBlocks().get(0);

        TimeScale utc = TimeScalesFactory.getUTC();
        Map<Keyword, String> metadata = new LinkedHashMap<>();
        Map<Keyword, String> segmentData = new LinkedHashMap<>();

        StringBuilder buffer = new StringBuilder();
        StreamingAemWriter writer = new StreamingAemWriter(buffer, utc, metadata, "%.2f");
        AEMSegment segment = writer.newSegment(segmentData);

        for (TimeStampedAngularCoordinates coordinate : block.getAngularCoordinates()) {
            segment.writeAttitudeEphemerisLine(coordinate, block.isFirst(), block.getAttitudeType(), block.getRotationOrder());
        }

        String expected = "2002-12-18T12:00:00.331 0.57 0.03 0.46 0.68\n" +
                          "2002-12-18T12:01:00.331 0.42 -0.46 0.24 0.75\n" +
                          "2002-12-18T12:02:00.331 -0.85 0.27 -0.07 0.46\n";

        assertEquals(buffer.toString(), expected);

        buffer = new StringBuilder();
        // Default format
        writer = new StreamingAemWriter(buffer, utc, metadata);
        segment = writer.newSegment(segmentData);

        for (TimeStampedAngularCoordinates coordinate : block.getAngularCoordinates()) {
            segment.writeAttitudeEphemerisLine(coordinate, block.isFirst(), block.getAttitudeType(), block.getRotationOrder());
        }

        expected = "2002-12-18T12:00:00.331  0.56748  0.03146  0.45689  0.68427\n" +
                   "2002-12-18T12:01:00.331  0.42319 -0.45697  0.23784  0.74533\n" +
                   "2002-12-18T12:02:00.331 -0.84532  0.26974 -0.06532  0.45652\n";

        assertEquals(buffer.toString(), expected);

    }

}
