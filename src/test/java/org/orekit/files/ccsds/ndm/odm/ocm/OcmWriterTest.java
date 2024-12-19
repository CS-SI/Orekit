/* Copyright 2002-2024 CS GROUP
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.utils.Constants;

public class OcmWriterTest extends AbstractWriterTest<OdmHeader, Segment<OcmMetadata, OcmData>, Ocm> {

    protected OcmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               withEquatorialRadius(Constants.WGS84_EARTH_EQUATORIAL_RADIUS).
               withFlattening(Constants.WGS84_EARTH_FLATTENING).
               withMu(Constants.EIGEN5C_EARTH_MU).
               buildOcmParser();
    }

    protected OcmWriter getWriter() {
        return new WriterBuilder().
               withEquatorialRadius(Constants.WGS84_EARTH_EQUATORIAL_RADIUS).
               withFlattening(Constants.WGS84_EARTH_FLATTENING).
               buildOcmWriter();
    }

    @Test
    public void testWriteExample1() {
        doTest("/ccsds/odm/ocm/OCMExample1.txt");
    }

    @Test
    public void testWriteKvnExample2() {
        doTest("/ccsds/odm/ocm/OCMExample2.txt");
    }

    @Test
    public void testWriteXmlExample2() {
        doTest("/ccsds/odm/ocm/OCMExample2.xml");
    }

    @Test
    public void testWriteExample3() {
        doTest("/ccsds/odm/ocm/OCMExample3.txt");
    }

    @Test
    public void testWriteExample4() {
        doTest("/ccsds/odm/ocm/OCMExample4.txt");
    }

    @Test
    public void testWriteExample5ITRF() {
        doTest("/ccsds/odm/ocm/OCMExample5ITRF.txt");
    }

    @Test
    public void testWriteExample5Geodetic() {
        doTest("/ccsds/odm/ocm/OCMExample5Geodetic.txt");
    }

    /**
     * Check that reading an OCM and writing it out doesn't add a bunch of optional fields
     * without default values.
     *
     * @throws IOException on error.
     */
    @Test
    public void testWriteMinimal1623() throws IOException {
        // setup
        // this file has every OCM section with all required values
        String name = "/ccsds/odm/ocm/OCMMinimal.txt";
        // this file also has the default values defined
        String expectedName = "/ccsds/odm/ocm/OCMMinimalExpected.txt";
        Ocm parsed = getParser().parse(
                new DataSource(name, () -> this.getClass().getResourceAsStream(name)));
        StringWriter buffer = new StringWriter();
        Generator generator = new KvnGenerator(buffer, 0, "memory", 0, 0);

        // action
        getWriter().writeMessage(generator, parsed);

        // verify
        String expected = fixture(expectedName);
        String actual = buffer.toString();
        assertThat(actual, is(expected));
    }

    /**
     * Read all characters from a class path resource idendified by
     * {@code name}. Assumes UTF8. Not particularly efficient.
     *
     * @param name to read from. See {@link Class#getResourceAsStream(String)}.
     * @return contents of the file.
     * @throws IOException on error.
     */
    public static String fixture(String name) throws IOException {
        try (InputStream is = OcmWriterTest.class.getResourceAsStream(name);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            CharBuffer buffer = CharBuffer.allocate(1024);
            while (reader.read(buffer) != -1) {
                if (!buffer.hasRemaining()) {
                    // needs larger buffer, double in size
                    CharBuffer b = CharBuffer.allocate(2 * buffer.capacity());
                    buffer.flip();
                    b.put(buffer);
                    buffer = b;
                }
            }

            buffer.flip();
            return buffer.toString();
        }
    }

}
