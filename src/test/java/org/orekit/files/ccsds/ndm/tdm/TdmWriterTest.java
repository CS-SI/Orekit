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
package org.orekit.files.ccsds.ndm.tdm;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.section.Segment;

public class TdmWriterTest extends AbstractWriterTest<TdmHeader, Segment<TdmMetadata, ObservationsBlock>, Tdm> {

    protected TdmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildTdmParser();
    }

    protected TdmWriter getWriter() {
        return new WriterBuilder().buildTdmWriter();
    }

    @Test
    public void testWriteKvnExample15() {
        doTest("/ccsds/tdm/kvn/TDMExample15.txt");
    }

    @Test
    public void testWriteKvnExample2() {
        doTest("/ccsds/tdm/kvn/TDMExample2.txt");
    }

    @Test
    public void testWriteKvnExample4() {
        doTest("/ccsds/tdm/kvn/TDMExample4.txt");
    }

    @Test
    public void testWriteKvnExample6() {
        doTest("/ccsds/tdm/kvn/TDMExample6.txt");
    }

    @Test
    public void testWriteKvnExample8() {
        doTest("/ccsds/tdm/kvn/TDMExample8.txt");
    }

    @Test
    public void testWriteKvnExampleAllKeywordsSequential() {
        doTest("/ccsds/tdm/kvn/TDMExampleAllKeywordsSequential.txt");
    }

    @Test
    public void testWriteKvnExampleAllKeywordsSingleDiff() {
        doTest("/ccsds/tdm/kvn/TDMExampleAllKeywordsSingleDiff.txt");
    }

    @Test
    public void testWriteXmlExample15() {
        doTest("/ccsds/tdm/xml/TDMExample15.xml");
    }

    @Test
    public void testWriteXmlExample2() {
        doTest("/ccsds/tdm/xml/TDMExample2.xml");
    }

    @Test
    public void testWriteXmlExample4() {
        doTest("/ccsds/tdm/xml/TDMExample4.xml");
    }

    @Test
    public void testWriteXmlExample6() {
        doTest("/ccsds/tdm/xml/TDMExample6.xml");
    }

    @Test
    public void testWriteXmlExample8() {
        doTest("/ccsds/tdm/xml/TDMExample8.xml");
    }

    @Test
    public void testWriteXmlExampleAllKeywordsSequential() {
        doTest("/ccsds/tdm/xml/TDMExampleAllKeywordsSequential.xml");
    }

    @Test
    public void testWriteXmlExampleAllKeywordsSingleDiff() {
        doTest("/ccsds/tdm/xml/TDMExampleAllKeywordsSingleDiff.xml");
    }

}
