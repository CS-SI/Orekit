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
package org.orekit.files.ccsds.ndm.odm.omm;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.time.AbsoluteDate;

public class OmmWriterTest extends AbstractWriterTest<OdmHeader, Segment<OmmMetadata, OmmData>, Omm> {

    protected OmmParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).
               buildOmmParser();
    }

    protected OmmWriter getWriter() {
        return new WriterBuilder().withMissionReferenceDate(AbsoluteDate.J2000_EPOCH).buildOmmWriter();
    }

    @Test
    public void testWriteExample1() {
        doTest("/ccsds/odm/omm/OMMExample1.txt");
    }

    @Test
    public void testWriteKvnExample2() {
        doTest("/ccsds/odm/omm/OMMExample2.txt");
    }

    @Test
    public void testWriteXmlExample2() {
        doTest("/ccsds/odm/omm/OMMExample2.xml");
    }

    @Test
    public void testWriteExample3() {
        doTest("/ccsds/odm/omm/OMMExample3.txt");
    }

    @Test
    public void testWriteKvnExample4() {
        doTest("/ccsds/odm/omm/OMMExample4.txt");
    }

    @Test
    public void testWriteXmlExample4() {
        doTest("/ccsds/odm/omm/OMMExample4.xml");
    }

}
