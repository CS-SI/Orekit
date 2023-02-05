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
package org.orekit.files.ccsds.utils.lexical;

import java.net.MalformedURLException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmParser;

public class XmlLexicalAnalyzerTest {

    @Test
    public void testNullBinary() {
        XmlLexicalAnalyzer la = new XmlLexicalAnalyzer(new DataSource("empty", (DataSource.StreamOpener) () -> null));
        try {
            la.accept(new ParserBuilder().buildOcmParser());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals("empty", oe.getParts()[0]);
        }
    }

    @Test
    public void testNullCharacter() {
        XmlLexicalAnalyzer la = new XmlLexicalAnalyzer(new DataSource("empty", (DataSource.ReaderOpener) () -> null));
        try {
            la.accept(new ParserBuilder().buildOcmParser());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals("empty", oe.getParts()[0]);
        }
    }

    /**
     * Check the XML parser is configured to ignore XML entities to avoid
     * security risks.
     */
    @Test
    public void testExternalResourcesAreIgnored() {
        // setup
        XmlLexicalAnalyzer la = new XmlLexicalAnalyzer(new DataSource(
                "entity",
                () -> this.getClass().getResourceAsStream("/ccsds/ndm/NDM-opm-entity.xml")));
        OcmParser parser = new ParserBuilder().buildOcmParser();

        // action
        try {
            la.accept(parser);
            // verify
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            // Malformed URL exception indicates external resource was disabled
            // file not found exception indicates parser tried to load the resource
            MatcherAssert.assertThat(e.getCause(),
                    CoreMatchers.instanceOf(MalformedURLException.class));
        }
    }

}
