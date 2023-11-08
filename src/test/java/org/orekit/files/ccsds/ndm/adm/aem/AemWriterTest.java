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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;

public class AemWriterTest extends AbstractWriterTest<AdmHeader, AemSegment, Aem> {

    protected AemParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildAemParser();
    }

    protected AemWriter getWriter() {
        return new WriterBuilder().buildAemWriter();
    }

    @Test
    public void testWriteExample01() {
        doTest("/ccsds/adm/aem/AEMExample01.txt");
    }

    @Test
    public void testWriteExample02() {
        doTest("/ccsds/adm/aem/AEMExample02.txt");
    }

    @Test
    public void testWriteKvnExample03() {
        doTest("/ccsds/adm/aem/AEMExample03.txt");
    }

    @Test
    public void testWriteXmlExample03() {
        doTest("/ccsds/adm/aem/AEMExample03.xml");
    }

    @Test
    public void testWriteExample04() {
        doTest("/ccsds/adm/aem/AEMExample04.txt");
    }

    @Test
    public void testWriteExample05() {
        doTest("/ccsds/adm/aem/AEMExample05.txt");
    }

    @Test
    public void testWriteExample06a() {
        doTest("/ccsds/adm/aem/AEMExample06a.txt");
    }

    @Test
    public void testWriteExample06b() {
        doTest("/ccsds/adm/aem/AEMExample06b.txt");
    }

    @Test
    public void testWriteExample07() {
        doTest("/ccsds/adm/aem/AEMExample07.txt");
    }

    @Test
    public void testWriteExample08() {
        doTest("/ccsds/adm/aem/AEMExample08.txt");
    }

    @Test
    public void testWriteExample09() {
        doTest("/ccsds/adm/aem/AEMExample09.txt");
    }

    @Test
    public void testWriteExample10() {
        doTest("/ccsds/adm/aem/AEMExample10.txt");
    }

    @Test
    public void testWriteExample11() {
        doTest("/ccsds/adm/aem/AEMExample11.xml");
    }

    @Test
    public void testWriteExample12() {
        doTest("/ccsds/adm/aem/AEMExample12.txt");
    }

    @Test
    public void testWriteExample13() {
        doTest("/ccsds/adm/aem/AEMExample13.xml");
    }

    @Test
    public void testWriteExample14() {
        doTest("/ccsds/adm/aem/AEMExample14.txt");
    }

    @Test
    public void testWriteExample15() {
        doTest("/ccsds/adm/aem/AEMExample15.txt");
    }

    @Test
    public void testWriteExample16() {
        doTest("/ccsds/adm/aem/AEMExample16.txt");
    }

    @Test
    public void testWriteExample17() {
        doTest("/ccsds/adm/aem/AEMExample17.txt");
    }

}
