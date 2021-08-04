/* Copyright 2002-2021 CS GROUP
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

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParserBuilder;

public class XmlLexicalAnalyzerTest {

    @Test
    public void testNullBinary() {
        XmlLexicalAnalyzer la = new XmlLexicalAnalyzer(new DataSource("empty", (DataSource.StreamOpener) () -> null));
        try {
            la.accept(new ParserBuilder().buildOcmParser());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals("empty", oe.getParts()[0]);
        }
    }

    @Test
    public void testNullCharacter() {
        XmlLexicalAnalyzer la = new XmlLexicalAnalyzer(new DataSource("empty", (DataSource.ReaderOpener) () -> null));
        try {
            la.accept(new ParserBuilder().buildOcmParser());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals("empty", oe.getParts()[0]);
        }
    }

}
