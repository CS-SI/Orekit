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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;


public class StreamingAemWriterTest {

    private static final double QUATERNION_PRECISION = 1e-5;
    private static final double DATE_PRECISION = 1e-3;

    /** Set Orekit data. */
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check reading and writing an AEM both with and without using the step handler
     * methods.
     */
    @Test
    public void testWriteAemStepHandler() throws Exception {

        // Create a list of files
        List<String> files = Collections.singletonList("/ccsds/adm/aem/AEMExample07.txt");
        for (final String ex : files) {

            // Reference AEM file
            final DataSource source0 = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            AemParser parser = new ParserBuilder().buildAemParser();
            Aem aem  = parser.parseMessage(source0);

            // Satellite attitude ephemeris as read from the reference file
            AemSegment ephemerisBlock = aem.getSegments().get(0);

            // Meta data are extracted from the reference file
            String            originator   = aem.getHeader().getOriginator();
            String            objectName   = ephemerisBlock.getMetadata().getObjectName();
            String            objectID     = ephemerisBlock.getMetadata().getObjectID();
            String            headerCmt    = aem.getHeader().getComments().get(0);
            FrameFacade       frameA       = ephemerisBlock.getMetadata().getEndpoints().getFrameA();
            FrameFacade       frameB       = ephemerisBlock.getMetadata().getEndpoints().getFrameB();
            boolean           a2b          = ephemerisBlock.getMetadata().getEndpoints().isA2b();
            AttitudeType      attitudeType = ephemerisBlock.getMetadata().getAttitudeType();
            boolean           isFirst      = ephemerisBlock.getMetadata().isFirst();

            // Initialize the header and metadata
            // Here, we use only one data segment.
            AdmHeader header = new AdmHeader();
            header.setOriginator(originator);
            header.addComment(headerCmt);

            AemMetadata metadata = new AemMetadata(1);
            metadata.setTimeSystem(TimeSystem.UTC);
            metadata.setObjectID(objectID);
            metadata.setObjectName(objectName);
            metadata.setAttitudeType(attitudeType);
            metadata.setIsFirst(isFirst);
            metadata.getEndpoints().setFrameA(frameA);
            metadata.getEndpoints().setFrameB(frameB);
            metadata.getEndpoints().setA2b(a2b);
            metadata.setStartTime(AbsoluteDate.PAST_INFINITY);  // will be overwritten at propagation start
            metadata.setStopTime(AbsoluteDate.FUTURE_INFINITY); // will be overwritten at propagation start

            StringBuilder buffer = new StringBuilder();
            StreamingAemWriter writer =
                            new StreamingAemWriter(new KvnGenerator(buffer, AemWriter.KVN_PADDING_WIDTH, ex + "-new",
                                                                    Constants.JULIAN_DAY, 60),
                                                   new WriterBuilder(). buildAemWriter(),
                                                   header, metadata);

            // Initialize a Keplerian propagator with an Inertial attitude provider
            // It is expected that all attitude data lines will have the same value
            StreamingAemWriter.SegmentWriter segment = writer.newSegment();
            KeplerianPropagator propagator =
                            createPropagator(ephemerisBlock.getStart(),
                                             new FrameAlignedProvider(ephemerisBlock.getAngularCoordinates().get(0).getRotation(),
                                                                      FramesFactory.getEME2000()));

            // We propagate 60 seconds after the start date with a step equals to 10.0 seconds
            // It is expected to have an attitude data block containing 7 data lines
            double step = 10.0;
            propagator.setStepHandler(step, segment);
            propagator.propagate(ephemerisBlock.getStart().shiftedBy(60.0));
            writer.close();

            // Generated AEM file
            final DataSource source1 = new DataSource("buffer",
                                                   () -> new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8)));
            Aem generatedAem = parser.parseMessage(source1);

            // There is only one attitude ephemeris block
            Assertions.assertEquals(1, generatedAem.getSegments().size());
            AemSegment attitudeBlocks = generatedAem.getSegments().get(0);
            // There are 7 data lines in the attitude ephemeris block
            List<? extends TimeStampedAngularCoordinates> ac  = attitudeBlocks.getAngularCoordinates();
            Assertions.assertEquals(7, ac.size());

            // Verify
            for (int i = 0; i < 7; i++) {
                Assertions.assertEquals(step * i, ac.get(i).getDate().durationFrom(ephemerisBlock.getStart()), DATE_PRECISION);
                Rotation rot = ac.get(i).getRotation();
                Assertions.assertEquals(0.68427, rot.getQ0(), QUATERNION_PRECISION);
                Assertions.assertEquals(0.56748, rot.getQ1(), QUATERNION_PRECISION);
                Assertions.assertEquals(0.03146, rot.getQ2(), QUATERNION_PRECISION);
                Assertions.assertEquals(0.45689, rot.getQ3(), QUATERNION_PRECISION);
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

}
