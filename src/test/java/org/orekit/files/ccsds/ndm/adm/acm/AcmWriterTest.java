/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.acm;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.section.Segment;

public class AcmWriterTest extends AbstractWriterTest<AdmHeader, Segment<AcmMetadata, AcmData>, Acm> {

    protected AcmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildAcmParser();
    }

    protected AcmWriter getWriter() {
        return new WriterBuilder().buildAcmWriter();
    }

    @Test
    public void testWriteExample01() {
        doTest("/ccsds/adm/acm/ACMExample01.txt");
    }

    @Test
    public void testWriteExample02() {
        doTest("/ccsds/adm/acm/ACMExample02.txt");
    }

    @Test
    public void testWriteExample03() {
        doTest("/ccsds/adm/acm/ACMExample03.txt");
    }

    @Test
    public void testWriteExample04() {
        doTest("/ccsds/adm/acm/ACMExample04.txt");
    }

    @Test
    public void testWriteExample05() {
        doTest("/ccsds/adm/acm/ACMExample05.txt");
    }

    @Test
    public void testWriteExample06() {
        doTest("/ccsds/adm/acm/ACMExample06.txt");
    }

    @Test
    public void testWriteExample07() {
        doTest("/ccsds/adm/acm/ACMExample07.txt");
    }

    @Test
    public void testWriteExample08() {
        doTest("/ccsds/adm/acm/ACMExample08.txt");
    }

    @Test
    public void testWriteExample09() {
        doTest("/ccsds/adm/acm/ACMExample09.txt");
    }

}
