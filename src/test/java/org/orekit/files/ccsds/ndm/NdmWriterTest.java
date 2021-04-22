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
package org.orekit.files.ccsds.ndm;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.apm.ApmFile;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmFile;
import org.orekit.files.ccsds.ndm.odm.opm.OpmFile;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;

/**
 * Test class for CCSDS Navigation Data Message writing.<p>
 * @author Luc Maisonobe
 */
public class NdmWriterTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOpm() throws IOException {
        final String name = "/ccsds/ndm/NDM-opm.xml";
        final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
        final NdmFile ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        final CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml", true)) {
            new WriterBuilder().buildNdmWriter().writeMessage(generator, ndm);
        }
        System.out.println(caw.toString());
        final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        NdmTestUtils.checkContainer(ndm, new ParserBuilder().buildNdmParser().parseMessage(source2));
    }

    @Test
    public void testOpmApm() throws IOException {
        final String name = "/ccsds/ndm/NDM-ocm-apm.xml";
        final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
        final NdmFile ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        final CharArrayWriter caw  = new CharArrayWriter();
        try (final Generator generator = new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml", true)) {
            new WriterBuilder().buildNdmWriter().writeMessage(generator, ndm);
        }
        final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        NdmTestUtils.checkContainer(ndm, new ParserBuilder().buildNdmParser().parseMessage(source2));
    }

}
