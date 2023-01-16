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
package org.orekit.files.ccsds.ndm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.apm.Apm;
import org.orekit.files.ccsds.ndm.odm.ocm.Ocm;
import org.orekit.files.ccsds.ndm.odm.opm.Opm;

/**
 * Test class for CCSDS Navigation Data Message parsing.<p>
 * @author Luc Maisonobe
 */
public class NdmParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testNoKvn() {
        // setup
        final String name = "/ccsds/ndm/wrong-format.txt";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));

        try {
            // action
            new ParserBuilder().buildNdmParser().parseMessage(source);

            // verify
            Assertions.fail("Expected Exception");
        } catch (OrekitException oe) {
           Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
        }
    }

    @Test
    public void testEmpty() {
        final String name = "/ccsds/ndm/empty.xml";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        Assertions.assertTrue(ndm.getComments().isEmpty());
        Assertions.assertTrue(ndm.getConstituents().isEmpty());
    }

    @Test
    public void testOpm() {
        final String name = "/ccsds/ndm/NDM-opm.xml";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        Assertions.assertEquals(1, ndm.getComments().size());
        Assertions.assertEquals("NDM with only one constituent: an OPM", ndm.getComments().get(0));
        Assertions.assertEquals(1, ndm.getConstituents().size());
        Opm opm = (Opm) ndm.getConstituents().get(0);
        Assertions.assertEquals("OSPREY 5", opm.getMetadata().getObjectName());
        Assertions.assertEquals(3000.0, opm.getData().getSpacecraftParametersBlock().getMass(), 1.0e-10);
    }

    @Test
    public void testOpmApm() {
        final String name = "/ccsds/ndm/NDM-ocm-apm.xml";
        final DataSource source = new DataSource(name, () -> NdmParserTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        Assertions.assertEquals(1, ndm.getComments().size());
        Assertions.assertEquals("NDM with two constituents: an OCM and an APM", ndm.getComments().get(0));
        Assertions.assertEquals(2, ndm.getConstituents().size());
        Ocm ocm = (Ocm) ndm.getConstituents().get(0);
        Assertions.assertEquals("1998-999A", ocm.getMetadata().getInternationalDesignator());
        Assertions.assertEquals("WGS-84", ocm.getData().getUserDefinedBlock().getParameters().get("EARTH_MODEL"));
        Apm apm = (Apm) ndm.getConstituents().get(1);
        Assertions.assertEquals("MARS SPIRIT", apm.getMetadata().getObjectName());
        Assertions.assertEquals("INSTRUMENT_A", apm.getData().getQuaternionBlock().getEndpoints().getFrameA().getName());
    }

}
