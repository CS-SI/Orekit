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
package org.orekit.files.ccsds.ndm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.apm.Apm;
import org.orekit.files.ccsds.ndm.odm.ocm.Ocm;
import org.orekit.files.ccsds.ndm.odm.opm.Opm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for CCSDS Navigation Data Message parsing.<p>
 * @author Luc Maisonobe
 */
class NdmParserTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testNoKvn() {
        // setup
        final String name = "/ccsds/ndm/wrong-format.txt";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));

        try {
            // action
            new ParserBuilder().buildNdmParser().parseMessage(source);

            // verify
            fail("Expected Exception");
        } catch (OrekitException oe) {
           assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    void testEmpty() {
        final String name = "/ccsds/ndm/empty.xml";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        assertTrue(ndm.getComments().isEmpty());
        assertTrue(ndm.getConstituents().isEmpty());
    }

    @Test
    void testOpm() {
        final String name = "/ccsds/ndm/NDM-opm.xml";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        assertEquals(1, ndm.getComments().size());
        assertEquals("NDM with only one constituent: an OPM", ndm.getComments().get(0));
        assertEquals(1, ndm.getConstituents().size());
        Opm opm = (Opm) ndm.getConstituents().get(0);
        assertEquals("OSPREY 5", opm.getMetadata().getObjectName());
        assertEquals(3000.0, opm.getData().getSpacecraftParametersBlock().getMass(), 1.0e-10);
    }

    @Test
    void testOpmApm() {
        final String name = "/ccsds/ndm/NDM-ocm-apm.xml";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        assertEquals(1, ndm.getComments().size());
        assertEquals("NDM with two constituents: an OCM and an APM", ndm.getComments().get(0));
        assertEquals(2, ndm.getConstituents().size());
        Ocm ocm = (Ocm) ndm.getConstituents().get(0);
        assertEquals("1998-999A", ocm.getMetadata().getInternationalDesignator());
        assertEquals("WGS-84", ocm.getData().getUserDefinedBlock().getParameters().get("EARTH_MODEL"));
        Apm apm = (Apm) ndm.getConstituents().get(1);
        assertEquals("MARS SPIRIT", apm.getMetadata().getObjectName());
        assertEquals("INSTRUMENT_A", apm.getData().getQuaternionBlock().getEndpoints().getFrameA().getName());
    }

}
