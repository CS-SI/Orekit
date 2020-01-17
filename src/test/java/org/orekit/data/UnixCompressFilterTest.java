/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIOException;
import org.orekit.errors.OrekitMessages;

public class UnixCompressFilterTest {

    @Test
    public void testWrongHeader() throws IOException {
        try {
            tryRead("wrong-header.Z", 0xff, 0xff);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_UNIX_COMPRESSED_FILE, oe.getSpecifier());
            Assert.assertEquals("wrong-header.Z", oe.getParts()[0]);
        }
    }

    @Test
    public void testPrematureEnd() {
        try {
            tryRead("premature-end.Z", 0x1f, 0x9d, 0x90, 0x23);
            Assert.fail("an exception should have been thrown");
        } catch (IOException ioe) {
            OrekitIOException oioe = (OrekitIOException) ioe;
            Assert.assertEquals(OrekitMessages.UNEXPECTED_END_OF_FILE, oioe.getSpecifier());
            Assert.assertEquals("premature-end.Z", oioe.getParts()[0]);
        }
    }

    @Test
    public void testUninitializedRepetition() {
        try {
            tryRead("uninitialized-repetition.Z", 0x1f, 0x9d, 0x90, 0x01, 0x01, 0x00);
            Assert.fail("an exception should have been thrown");
        } catch (IOException ioe) {
            OrekitIOException oioe = (OrekitIOException) ioe;
            Assert.assertEquals(OrekitMessages.CORRUPTED_FILE, oioe.getSpecifier());
            Assert.assertEquals("uninitialized-repetition.Z", oioe.getParts()[0]);
        }
    }

    @Test
    public void testKeyPastTable() {
        try {
            tryRead("key-past-table-end.Z", 0x1f, 0x9d, 0x90, 0x31, 0x5c, 0xff);
            Assert.fail("an exception should have been thrown");
        } catch (IOException ioe) {
            OrekitIOException oioe = (OrekitIOException) ioe;
            Assert.assertEquals(OrekitMessages.CORRUPTED_FILE, oioe.getSpecifier());
            Assert.assertEquals("key-past-table-end.Z", oioe.getParts()[0]);
        }
    }

    @Test
    public void testReadPasteEnd() throws IOException {
        final byte[] array = new byte[] { (byte) 0x1f, (byte) 0x9d, (byte) 0x90, (byte) 0x0a, (byte) 0x00 };
        NamedData filtered = new UnixCompressFilter().
                        filter(new NamedData("empty-line.Z", () -> new ByteArrayInputStream(array)));
        InputStream is = filtered.getStreamOpener().openStream();
        Assert.assertEquals('\n', is.read());
        for (int i = 0; i < 1000; ++i) {
            Assert.assertEquals(-1,   is.read());
        }
    }

    @Test
    public void testSmallText() throws IOException, OrekitException {
        // for such a small text, compressed file is actually larger than initial file
        int[] uncompressed = tryRead("small-text.Z",
                                     0x1f, 0x9d, 0x90, 0x4f, 0xe4, 0x94, 0x59, 0x93, 0x86, 0x0e);
        Assert.assertEquals(6,    uncompressed.length);
        Assert.assertEquals('O',  uncompressed[0]);
        Assert.assertEquals('r',  uncompressed[1]);
        Assert.assertEquals('e',  uncompressed[2]);
        Assert.assertEquals('k',  uncompressed[3]);
        Assert.assertEquals('i',  uncompressed[4]);
        Assert.assertEquals('t',  uncompressed[5]);
    }

    @Test
    public void testRepetition() throws IOException, OrekitException {
        int[] uncompressed = tryRead("repetition.Z",
                                     0x1f, 0x9d, 0x90, 0x61, 0xc4, 0x04, 0x04);
        Assert.assertEquals(4,    uncompressed.length);
        Assert.assertEquals('a',  uncompressed[0]);
        Assert.assertEquals('b',  uncompressed[1]);
        Assert.assertEquals('a',  uncompressed[2]);
        Assert.assertEquals('b',  uncompressed[3]);
    }

    @Test
    public void testSpecialCase() throws IOException, OrekitException {
        // this is the well-known special case of a repetition entry in the
        // common sequences table being used just before being defined
        int[] uncompressed = tryRead("special-case.Z",
                                     0x1f, 0x9d, 0x90, 0x23, 0x60, 0x08, 0x04);
        Assert.assertEquals(4,    uncompressed.length);
        Assert.assertEquals('#',  uncompressed[0]);
        Assert.assertEquals('0',  uncompressed[1]);
        Assert.assertEquals('0',  uncompressed[2]);
        Assert.assertEquals('0',  uncompressed[3]);
    }

    @Test
    public void testEOPFile()  throws IOException, OrekitException {
        final String name = "compressed-data/eopc04_08.00.Z";
        NamedData filtered = new UnixCompressFilter().
                             filter(new NamedData(name,
                                                  () -> Utils.class.getClassLoader().getResourceAsStream(name)));
        try (InputStream is = filtered.getStreamOpener().openStream();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            int lines = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lines;
            }
            Assert.assertEquals(380, lines);
        }
    }

    private int[] tryRead(String name, int... bytes) throws IOException {
        final byte[] array = new byte[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            array[i] = (byte) bytes[i];
        }
        NamedData filtered = new UnixCompressFilter().
                        filter(new NamedData(name, () -> new ByteArrayInputStream(array)));
        InputStream is = filtered.getStreamOpener().openStream();
        List<Integer> output = new ArrayList<>();
        while (true) {
            boolean shouldWork = is.available() > 0;
            final int r = is.read();
            if (r < 0) {
                Assert.assertFalse(shouldWork);
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
