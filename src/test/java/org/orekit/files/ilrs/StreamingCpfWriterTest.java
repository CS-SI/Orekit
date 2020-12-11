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
package org.orekit.files.ilrs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.files.ilrs.CPFFile.CPFEphemeris;
import org.orekit.files.ilrs.StreamingCpfWriter.Segment;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;


public class StreamingCpfWriterTest {

    /** Set Orekit data. */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check reading and writing a CPF both with and without using the step handler
     * methods.
     */
    @Test
    public void testWriteCpfStepHandler() throws Exception {

        // Time scale
        TimeScale utc = TimeScalesFactory.getUTC();
        // Create a list of files for testing
        List<String> files =
                Arrays.asList("/ilrs/jason3_cpf_180613_16401.cne",
                              "/ilrs/lageos1_cpf_180613_16401.hts",
                              "/ilrs/galileo212_cpf_180613_6641.esa");
        for (String ex : files) {
            InputStream inEntry = getClass().getResourceAsStream(ex);
            CPFParser parser = new CPFParser();
            CPFFile cpfFile = parser.parse(inEntry);

            CPFEphemeris satellite =
                            cpfFile.getSatellites().values().iterator().next();
            Frame frame = satellite.getFrame();
            double step = satellite.getStop()
                            .durationFrom(satellite.getStart()) /
                            (satellite.getCoordinates().size() - 1);

            Assert.assertEquals(step, cpfFile.getHeader().getStep(), 0.1);
            StringBuilder buffer = new StringBuilder();
            StreamingCpfWriter writer = new StreamingCpfWriter(buffer, utc, cpfFile.getHeader());
            writer.writeHeader();
            Segment segment = writer.newSegment(frame);
            BoundedPropagator propagator = satellite.getPropagator();
            propagator.setMasterMode(step, segment);
            propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());

            BufferedReader reader =
                            new BufferedReader(new StringReader(buffer.toString()));
            CPFFile generatedCpfFile = parser.parse(reader, "buffer");
            CPFWriterTest.compareCpfFiles(cpfFile, generatedCpfFile);

            // check calling the methods directly
            buffer = new StringBuilder();
            writer = new StreamingCpfWriter(buffer, utc, cpfFile.getHeader());
            writer.writeHeader();
            segment = writer.newSegment(frame);
            for (TimeStampedPVCoordinates coordinate : satellite.getCoordinates()) {
                segment.writeEphemerisLine(coordinate);
            }
            writer.writeEndOfFile();

            // verify
            reader = new BufferedReader(new StringReader(buffer.toString()));
            generatedCpfFile = parser.parse(reader, "buffer");
            CPFWriterTest.compareCpfFiles(cpfFile, generatedCpfFile);

        }

    }

}
