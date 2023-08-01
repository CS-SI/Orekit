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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.io.CharArrayWriter;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.utils.Constants;

public class ApmWriterTest extends AbstractWriterTest<AdmHeader, Segment<AdmMetadata, ApmData>, Apm> {

    protected ApmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildApmParser();
    }

    protected ApmWriter getWriter() {
        return new WriterBuilder().buildApmWriter();
    }

    @Test
    public void testWriteExample01() {
        doTest("/ccsds/adm/apm/APMExample01.txt");
    }

    @Test
    public void testWriteKvnExample02() {
        doTest("/ccsds/adm/apm/APMExample02.txt");
    }

    @Test
    public void testWriteXmlExample02() {
        doTest("/ccsds/adm/apm/APMExample02.xml");
    }

    @Test
    public void testWriteExample03() {
        doTest("/ccsds/adm/apm/APMExample03.txt");
    }

    @Test
    public void testWriteExample04() {
        doTest("/ccsds/adm/apm/APMExample04.txt");
    }

    @Test
    public void testWriteExample05() {
        doTest("/ccsds/adm/apm/APMExample05.txt");
    }

    @Test
    public void testWriteExample06() {
        doTest("/ccsds/adm/apm/APMExample06.txt");
    }

    @Test
    public void testWriteExample07() {
        doTest("/ccsds/adm/apm/APMExample07.txt");
    }

    @Test
    public void testWriteExample08() {
        doTest("/ccsds/adm/apm/APMExample08.txt");
    }

    @Test
    public void testWriteExample09() {
        doTest("/ccsds/adm/apm/APMExample09.txt");
    }

    @Test
    public void testWriteExample10() {
        doTest("/ccsds/adm/apm/APMExample10.txt");
    }

    @Test
    public void testWriteExample11() {
        doTest("/ccsds/adm/apm/APMExample11.txt");
    }

    @Test
    public void testWriteExample12() {
        doTest("/ccsds/adm/apm/APMExample12.txt");
    }

    @Test
    public void testWrongVersion() throws IOException {
        final String  name = "/ccsds/adm/apm/APMExample01.txt";
        final Apm file = new ParserBuilder().
                             buildApmParser().
                             parseMessage(new DataSource(name, () -> getClass().getResourceAsStream(name)));
        file.getHeader().setFormatVersion(1.0);
        file.getHeader().setMessageId("this message is only allowed in format version 2.0 and later");
        try (Generator generator = new XmlGenerator(new CharArrayWriter(), XmlGenerator.DEFAULT_INDENT, "",
                                                    Constants.JULIAN_DAY, false, null)) {
            new WriterBuilder().buildApmWriter().writeMessage(generator, file);
            Assertions.fail("an exception should heave been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION, oe.getSpecifier());
            Assertions.assertEquals(HeaderKey.MESSAGE_ID.name(), oe.getParts()[0]);
        }

    }

    @Test
    public void testClassificationForbidden() throws IOException {
        final String  name = "/ccsds/adm/apm/APMExample01.txt";
        final Apm file = new ParserBuilder().
                             buildApmParser().
                             parseMessage(new DataSource(name, () -> getClass().getResourceAsStream(name)));
        file.getHeader().setFormatVersion(1.0);
        file.getHeader().setClassification("classification is not allowed in ADM");
        try (Generator generator = new XmlGenerator(new CharArrayWriter(), XmlGenerator.DEFAULT_INDENT, "",
                                                    Constants.JULIAN_DAY, false, null)) {
            new WriterBuilder().buildApmWriter().writeMessage(generator, file);
            Assertions.fail("an exception should heave been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION, oe.getSpecifier());
            Assertions.assertEquals(HeaderKey.CLASSIFICATION.name(), oe.getParts()[0]);
        }

    }

    @Test
    public void testException() {
        try {
            new ApmData(null, null, null, null, null, null, null).validate(1.0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals(ApmQuaternionKey.Q_FRAME_A.name(), oe.getParts()[0]);
        }
    }

}
