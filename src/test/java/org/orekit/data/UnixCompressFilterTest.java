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
package org.orekit.data;

import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIOException;
import org.orekit.errors.OrekitMessages;

import java.io.BufferedReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class UnixCompressFilterTest {

    @Test
    void testWrongHeader() throws IOException {
        try {
            tryRead("wrong-header.Z", 0xff, 0xff);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NOT_A_SUPPORTED_UNIX_COMPRESSED_FILE, oe.getSpecifier());
            assertEquals("wrong-header.Z", oe.getParts()[0]);
        }
    }

    @Test
    void testPrematureEnd() {
        try {
            tryRead("premature-end.Z", 0x1f, 0x9d, 0x90, 0x23);
            fail("an exception should have been thrown");
        } catch (IOException ioe) {
            OrekitIOException oioe = (OrekitIOException) ioe;
            assertEquals(OrekitMessages.UNEXPECTED_END_OF_FILE, oioe.getSpecifier());
            assertEquals("premature-end.Z", oioe.getParts()[0]);
        }
    }

    @Test
    void testUninitializedRepetition() {
        try {
            tryRead("uninitialized-repetition.Z", 0x1f, 0x9d, 0x90, 0x01, 0x01, 0x00);
            fail("an exception should have been thrown");
        } catch (IOException ioe) {
            OrekitIOException oioe = (OrekitIOException) ioe;
            assertEquals(OrekitMessages.CORRUPTED_FILE, oioe.getSpecifier());
            assertEquals("uninitialized-repetition.Z", oioe.getParts()[0]);
        }
    }

    @Test
    void testKeyPastTable() {
        try {
            tryRead("key-past-table-end.Z", 0x1f, 0x9d, 0x90, 0x31, 0x5c, 0xff);
            fail("an exception should have been thrown");
        } catch (IOException ioe) {
            OrekitIOException oioe = (OrekitIOException) ioe;
            assertEquals(OrekitMessages.CORRUPTED_FILE, oioe.getSpecifier());
            assertEquals("key-past-table-end.Z", oioe.getParts()[0]);
        }
    }

    @Test
    void testReadPasteEnd() throws IOException {
        final byte[] array = new byte[] { (byte) 0x1f, (byte) 0x9d, (byte) 0x90, (byte) 0x0a, (byte) 0x00 };
        DataSource filtered = new UnixCompressFilter().
                        filter(new DataSource("empty-line.Z", () -> new ByteArrayInputStream(array)));
        InputStream is = filtered.getOpener().openStreamOnce();
        assertEquals('\n', is.read());
        for (int i = 0; i < 1000; ++i) {
            assertEquals(-1,   is.read());
        }
    }

    @Test
    void testSmallText() throws IOException, OrekitException {
        // for such a small text, compressed file is actually larger than initial file
        int[] uncompressed = tryRead("small-text.Z",
                                     0x1f, 0x9d, 0x90, 0x4f, 0xe4, 0x94, 0x59, 0x93, 0x86, 0x0e);
        assertEquals(6,    uncompressed.length);
        assertEquals('O',  uncompressed[0]);
        assertEquals('r',  uncompressed[1]);
        assertEquals('e',  uncompressed[2]);
        assertEquals('k',  uncompressed[3]);
        assertEquals('i',  uncompressed[4]);
        assertEquals('t',  uncompressed[5]);
    }

    @Test
    void testRepetition() throws IOException, OrekitException {
        int[] uncompressed = tryRead("repetition.Z",
                                     0x1f, 0x9d, 0x90, 0x61, 0xc4, 0x04, 0x04);
        assertEquals(4,    uncompressed.length);
        assertEquals('a',  uncompressed[0]);
        assertEquals('b',  uncompressed[1]);
        assertEquals('a',  uncompressed[2]);
        assertEquals('b',  uncompressed[3]);
    }

    @Test
    void testSpecialCase() throws IOException, OrekitException {
        // this is the well-known special case of a repetition entry in the
        // common sequences table being used just before being defined
        int[] uncompressed = tryRead("special-case.Z",
                                     0x1f, 0x9d, 0x90, 0x23, 0x60, 0x08, 0x04);
        assertEquals(4,    uncompressed.length);
        assertEquals('#',  uncompressed[0]);
        assertEquals('0',  uncompressed[1]);
        assertEquals('0',  uncompressed[2]);
        assertEquals('0',  uncompressed[3]);
    }

    @Test
    void testEOPFile()  throws IOException, OrekitException {
        final String name = "compressed-data/eopc04_08.00.Z";
        DataSource filtered = new UnixCompressFilter().
                             filter(new DataSource(name,
                                                  () -> Utils.class.getClassLoader().getResourceAsStream(name)));
        try (BufferedReader br = new BufferedReader(filtered.getOpener().openReaderOnce())) {
            int lines = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lines;
            }
            assertEquals(380, lines);
        }
    }

    private int[] tryRead(String name, int... bytes) throws IOException {
        final byte[] array = new byte[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            array[i] = (byte) bytes[i];
        }
        DataSource filtered = new UnixCompressFilter().
                              filter(new DataSource(name, () -> new ByteArrayInputStream(array)));
        InputStream is = filtered.getOpener().openStreamOnce();
        List<Integer> output = new ArrayList<>();
        while (true) {
            boolean shouldWork = is.available() > 0;
            final int r = is.read();
            if (r < 0) {
                assertFalse(shouldWork);
                int[] result = new int[output.size()];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = output.get(i);
                }
                return result;
            } else {
                output.add(r);
            }
        }
    }

}
