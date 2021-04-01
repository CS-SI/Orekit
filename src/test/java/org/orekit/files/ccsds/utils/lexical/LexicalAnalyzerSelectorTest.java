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
package org.orekit.files.ccsds.utils.lexical;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.utils.FileFormat;

public class LexicalAnalyzerSelectorTest {

    @Test
    public void testUCS4BigEndian() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UCS-4-BE.xml"));
    }

    @Test
    public void testUCS4BigEndianByteOrderMark() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UCS-4-BE-BOM.xml"));
    }

    @Test
    public void testUCS4LittleEndian() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UCS-4-LE.xml"));
    }

    @Test
    public void testUCS4LittleEndianByteOrderMark() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UCS-4-LE-BOM.xml"));
    }

    @Test
    public void testUTF16BigEndian() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UTF-16-BE.xml"));
    }

    @Test
    public void testUTF16BigEndianByteOrderMark() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UTF-16-BE-BOM.xml"));
    }

    @Test
    public void testUTF16LittleEndian() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UTF-16-LE.xml"));
    }

    @Test
    public void testUTF16LittleEndianByteOrderMark() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UTF-16-LE-BOM.xml"));
    }

    @Test
    public void testUTF8() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UTF-8.xml"));
    }

    @Test
    public void testUTF8ByteOrderMark() {
        Assert.assertEquals(FileFormat.XML, selectedFormat("/ccsds/lexical/minimalist-UTF-8-BOM.xml"));
    }

    @Test
    public void testKVN() {
        Assert.assertEquals(FileFormat.KVN, selectedFormat("/ccsds/lexical/minimalist-UTF-8.kvn"));
    }

    private FileFormat selectedFormat(String resourceName) {
        try {
            final DataSource source = new DataSource(resourceName, () -> getClass().getResourceAsStream(resourceName));
            final LexicalAnalyzer analyzer = LexicalAnalyzerSelector.select(source);
            return analyzer instanceof XmlLexicalAnalyzer ? FileFormat.XML : FileFormat.KVN;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.fail(ioe.getLocalizedMessage());
            return null;
        }
    }
}
