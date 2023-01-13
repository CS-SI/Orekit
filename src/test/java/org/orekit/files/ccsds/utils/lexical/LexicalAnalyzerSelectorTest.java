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
package org.orekit.files.ccsds.utils.lexical;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.FileFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LexicalAnalyzerSelectorTest {

    @Test
    public void testUCS4BigEndianBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UCS-4-BE.xml"));
    }

    @Test
    public void testUCS4BigEndianByteOrderMarkBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UCS-4-BE-BOM.xml"));
    }

    @Test
    public void testUCS4LittleEndianBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UCS-4-LE.xml"));
    }

    @Test
    public void testUCS4LittleEndianByteOrderMarkBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UCS-4-LE-BOM.xml"));
    }

    @Test
    public void testUTF16BigEndianBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UTF-16-BE.xml"));
    }

    @Test
    public void testUTF16BigEndianCharacter() {
        checkFormat(FileFormat.XML, characterSource("/ccsds/lexical/minimalist-UTF-16-BE.xml", StandardCharsets.UTF_16BE));
    }

    @Test
    public void testUTF16BigEndianByteOrderMarkBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UTF-16-BE-BOM.xml"));
    }

    @Test
    public void testUTF16BigEndianByteOrderMarkCharacter() {
        checkFormat(FileFormat.XML, characterSource("/ccsds/lexical/minimalist-UTF-16-BE-BOM.xml", StandardCharsets.UTF_16BE));
    }

    @Test
    public void testUTF16LittleEndianBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UTF-16-LE.xml"));
    }

    @Test
    public void testUTF16LittleEndianCharacter() {
        checkFormat(FileFormat.XML, characterSource("/ccsds/lexical/minimalist-UTF-16-LE.xml", StandardCharsets.UTF_16LE));
    }

    @Test
    public void testUTF16LittleEndianByteOrderMarkBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UTF-16-LE-BOM.xml"));
    }

    @Test
    public void testUTF16LittleEndianByteOrderMarkCharacter() {
        checkFormat(FileFormat.XML, characterSource("/ccsds/lexical/minimalist-UTF-16-LE-BOM.xml", StandardCharsets.UTF_16LE));
    }

    @Test
    public void testUTF8Binary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UTF-8.xml"));
    }

    @Test
    public void testUTF8Character() {
        checkFormat(FileFormat.XML, characterSource("/ccsds/lexical/minimalist-UTF-8.xml", StandardCharsets.UTF_8));
    }

    @Test
    public void testUTF8ByteOrderMarkBinary() {
        checkFormat(FileFormat.XML, binarySource("/ccsds/lexical/minimalist-UTF-8-BOM.xml"));
    }

    @Test
    public void testUTF8ByteOrderMarkCharacter() {
        checkFormat(FileFormat.XML, characterSource("/ccsds/lexical/minimalist-UTF-8-BOM.xml", StandardCharsets.UTF_8));
    }

    @Test
    public void testKVNBinary() {
        checkFormat(FileFormat.KVN, binarySource("/ccsds/lexical/minimalist-UTF-8.kvn"));
    }

    @Test
    public void testKVNCharacter() {
        checkFormat(FileFormat.KVN, characterSource("/ccsds/lexical/minimalist-UTF-8.kvn", StandardCharsets.UTF_8));
    }

    @Test
    public void testNullBinary() throws IOException {
        try {
            LexicalAnalyzerSelector.select(new DataSource("empty", (DataSource.StreamOpener) () -> null));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals("empty", oe.getParts()[0]);
        }
    }

    @Test
    public void testNullCharacters() throws IOException {
        try {
            LexicalAnalyzerSelector.select(new DataSource("empty", (DataSource.ReaderOpener) () -> null));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals("empty", oe.getParts()[0]);
        }
    }

    @Test
    public void testTooSmallBinary() throws IOException {
        checkFormat(FileFormat.KVN, new DataSource("small", () -> new ByteArrayInputStream(new byte[] { 0x3c, 0x3f, 0x78 })));
    }

    @Test
    public void testTooSmallCharacters() throws IOException {
        checkFormat(FileFormat.KVN, new DataSource("small", () -> new StringReader("<?x")));
    }

    private DataSource binarySource(String resourceName) {
        return new DataSource(resourceName, () -> getClass().getResourceAsStream(resourceName));
    }

    private DataSource characterSource(String resourceName, Charset charset) {
        return new DataSource(resourceName, () -> new InputStreamReader(getClass().getResourceAsStream(resourceName), charset));
    }

    private void checkFormat(FileFormat expected, DataSource source) {
        try {
            final LexicalAnalyzer analyzer = LexicalAnalyzerSelector.select(source);
            Assertions.assertEquals(expected, analyzer instanceof XmlLexicalAnalyzer ? FileFormat.XML : FileFormat.KVN);
        } catch (IOException ioe) {
            Assertions.fail(ioe.getLocalizedMessage());
        }
    }
}
