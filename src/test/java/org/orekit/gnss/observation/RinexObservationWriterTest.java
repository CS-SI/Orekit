/* Copyright 2023 Thales Alenia Space
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
package org.orekit.gnss.observation;

import java.io.CharArrayWriter;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class RinexObservationWriterTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testWriteHeaderTwice() throws IOException {
        final RinexObservation       robs   = load("rinex/aiub0000.00o");
        final CharArrayWriter        caw    = new CharArrayWriter();
        final RinexObservationWriter writer = new RinexObservationWriter(caw, "dummy");
        writer.writeHeader(robs.getHeader());
        try {
            writer.writeHeader(robs.getHeader());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.HEADER_ALREADY_WRITTEN, oe.getSpecifier());
            Assertions.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    @Test
    public void testNiWriteHeader() throws IOException {
        final RinexObservation       robs   = load("rinex/aiub0000.00o");
        final CharArrayWriter        caw    = new CharArrayWriter();
        final RinexObservationWriter writer = new RinexObservationWriter(caw, "dummy");
        try {
            writer.writeObservationDataSet(robs.getObservationDataSets().get(0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.HEADER_NOT_WRITTEN, oe.getSpecifier());
            Assertions.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    private RinexObservation load(final String name) {
        final DataSource dataSource = new DataSource(name, () -> Utils.class.getClassLoader().getResourceAsStream(name));
        return new RinexObservationParser().parse(dataSource);
     }

}
