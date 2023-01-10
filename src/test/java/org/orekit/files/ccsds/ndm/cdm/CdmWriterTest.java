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
package org.orekit.files.ccsds.ndm.cdm;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;

public class CdmWriterTest extends AbstractWriterTest<CdmHeader, CdmSegment, Cdm> {

    protected CdmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildCdmParser();
    }

    protected CdmWriter getWriter() {
        return new WriterBuilder().buildCdmWriter();
    }

    @Test
    public void testWriteExample1() {
        doTest("/ccsds/cdm/CDMExample1.txt");
    }

    @Test
    public void testWriteExample1XML() {
        doTest("/ccsds/cdm/CDMExample1.xml");
    }

    @Test
    public void testWriteExample2() {
        doTest("/ccsds/cdm/CDMExample2.txt");
    }

    @Test
    public void testWriteExample3() {
        doTest("/ccsds/cdm/CDMExample3.txt");
    }

    @Test
    public void testWriteExample4() {
        doTest("/ccsds/cdm/CDMExample4.txt");
    }
    
    @Test
    public void testWrite_issue_942_KVN() {
        doTest("/ccsds/cdm/CDMExample_issue942.txt");
    }
    
    @Test
    public void testWrite_issue_942_XML() {
        doTest("/ccsds/cdm/CDMExample_issue942.xml");
    }

    @Test
    public void testWrite_issue_988_KVN_YES() {
        doTest("/ccsds/cdm/CDMExample_issue988.txt");
    }

    @Test
    public void testWrite_issue_988_KVN_NO() {
        doTest("/ccsds/cdm/CDMExample_issue988_2.txt");
    }

    @Test
    public void testWrite_issue_988_KVN_NONE() {
        doTest("/ccsds/cdm/CDMExample_issue988_3.txt");
    }

    @Test
    public void testWrite_issue_988_XML_YES() {
        doTest("/ccsds/cdm/CDMExample_issue988.xml");
    }


    @Test
    public void testWrite_issue_988_XML_NO() {
        doTest("/ccsds/cdm/CDMExample_issue988_2.xml");
    }

    @Test
    public void testWrite_issue_988_XML_NONE() {
        doTest("/ccsds/cdm/CDMExample_issue988_3.txt");
    }

}

