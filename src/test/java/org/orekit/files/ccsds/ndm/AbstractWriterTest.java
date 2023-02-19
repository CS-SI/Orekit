/* Copyright 2002-2023 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.files.ccsds.ndm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.MessageWriter;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.utils.Constants;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public abstract class AbstractWriterTest<H extends Header, S extends Segment<?, ?>, F extends NdmConstituent<H, S>> {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    protected abstract MessageParser<F>       getParser();
    protected abstract MessageWriter<H, S, F> getWriter();

    protected  void doTest(final String name) {
        doTest(name, FileFormat.KVN, 60);
        doTest(name, FileFormat.KVN,  0);
        doTest(name, FileFormat.XML, 60);
        doTest(name, FileFormat.XML,  0);
    }

    protected  void doTest(final String name, final FileFormat format, final int unitsColumn) {
        try {
            final DataSource source1  = new DataSource(name, () -> getClass().getResourceAsStream(name));
            final F          original = getParser().parseMessage(source1);

            // write the parsed file back to a characters array
            final MessageWriter<H, S, F> writer = getWriter();
            final CharArrayWriter caw = new CharArrayWriter();
            try (Generator generator = format == FileFormat.KVN ?
                                       new KvnGenerator(caw, 25, "dummy.kvn", Constants.JULIAN_DAY, unitsColumn) :
                                       new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml",
                                                        Constants.JULIAN_DAY, unitsColumn > 0,
                                                        XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION)) {
                writer.writeMessage(generator, original);
            }

            // reparse the written file
            final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
            final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
            final F          rebuilt = getParser().parseMessage(source2);

            NdmTestUtils.checkEquals(original, rebuilt);

            if (format == FileFormat.XML) {
                // check schema
               try (InputStream       is  = new ByteArrayInputStream(bytes);
                    InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                    BufferedReader    br  = new BufferedReader(isr)) {
                    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", br.readLine());
                    Assertions.assertEquals("<" + writer.getRoot() +
                                            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                                            " xsi:noNamespaceSchemaLocation=\"" + XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION +
                                            "\" id=\"" + writer.getFormatVersionKey() +
                                            "\" version=\"" + String.format(Locale.US, "%.1f", writer.getVersion()) +
                                            "\">",
                                            br.readLine());
                }
            }

        } catch (IOException ioe) {
            Assertions.fail(ioe.getLocalizedMessage());
        }
    }

}
