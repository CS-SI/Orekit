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
package org.orekit.files.ccsds.ndm.tdm;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.section.Segment;

class TdmWriterTest extends AbstractWriterTest<TdmHeader, Segment<TdmMetadata, ObservationsBlock>, Tdm> {

    protected TdmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildTdmParser();
    }

    protected TdmWriter getWriter() {
        return new WriterBuilder().buildTdmWriter();
    }

    @Test
    void testWriteKvnExample15() {
        doTest("/ccsds/tdm/kvn/TDMExample15.txt");
    }

    @Test
    void testWriteKvnExample2() {
        doTest("/ccsds/tdm/kvn/TDMExample2.txt");
    }

    @Test
    void testWriteKvnExample4() {
        doTest("/ccsds/tdm/kvn/TDMExample4.txt");
    }

    @Test
    void testWriteKvnExample6() {
        doTest("/ccsds/tdm/kvn/TDMExample6.txt");
    }

    @Test
    void testWriteKvnExample8() {
        doTest("/ccsds/tdm/kvn/TDMExample8.txt");
    }

    @Test
    void testWriteKvnExampleAllKeywordsSequential() {
        doTest("/ccsds/tdm/kvn/TDMExampleAllKeywordsSequential.txt");
    }

    @Test
    void testWriteKvnExampleAllKeywordsSingleDiff() {
        doTest("/ccsds/tdm/kvn/TDMExampleAllKeywordsSingleDiff.txt");
    }

    @Test
    void testWriteXmlExample15() {
        doTest("/ccsds/tdm/xml/TDMExample15.xml");
    }

    @Test
    void testWriteXmlExample2() {
        doTest("/ccsds/tdm/xml/TDMExample2.xml");
    }

    @Test
    void testWriteXmlExample4() {
        doTest("/ccsds/tdm/xml/TDMExample4.xml");
    }

    @Test
    void testWriteXmlExample6() {
        doTest("/ccsds/tdm/xml/TDMExample6.xml");
    }

    @Test
    void testWriteXmlExample8() {
        doTest("/ccsds/tdm/xml/TDMExample8.xml");
    }

    @Test
    void testWriteXmlExampleAllKeywordsSequential() {
        doTest("/ccsds/tdm/xml/TDMExampleAllKeywordsSequential.xml");
    }

    @Test
    void testWriteXmlExampleAllKeywordsSingleDiff() {
        doTest("/ccsds/tdm/xml/TDMExampleAllKeywordsSingleDiff.xml");
    }

}
