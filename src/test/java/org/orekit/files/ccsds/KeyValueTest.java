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
package org.orekit.files.ccsds;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.KeyValue;


public class KeyValueTest {

    @Test
    public void testUppercase() {
        for (final Keyword keyword : Keyword.values()) {
            if (keyword.toString().toUpperCase().equals(keyword.toString())) {
                KeyValue entry = new KeyValue("  " + keyword + " = " + "aaa", 12, "dummy");
                Assert.assertEquals(keyword, entry.getKeyword());
                Assert.assertEquals(keyword.toString(), entry.getKey());
                if (keyword == Keyword.COMMENT) {
                    Assert.assertEquals("aaa", entry.getValue());
                } else {
                    Assert.assertEquals("AAA", entry.getValue());
                }
                try {
                    entry.getDoubleValue();
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
                    Assert.assertEquals(12, ((Integer) oe.getParts()[0]).intValue());
                    Assert.assertEquals("dummy", ((String) oe.getParts()[1]));
                }
            }
        }
    }

    @Test
    public void testUserDefined() {
        KeyValue entry = new KeyValue("USER_DEFINED_TEST_KEY = Orekit", 3, "dummy");
        Assert.assertEquals(Keyword.USER_DEFINED_X, entry.getKeyword());
        Assert.assertEquals("USER_DEFINED_TEST_KEY", entry.getKey());
        Assert.assertEquals("OREKIT", entry.getValue());
    }

    @Test
    public void testUnknownKey() {
        KeyValue entry = new KeyValue("INEXISTENT_KEY = 1.0e-5", 5, "dummy");
        Assert.assertNull(entry.getKeyword());
        Assert.assertEquals("INEXISTENT_KEY", entry.getKey());
        Assert.assertEquals("1.0E-5", entry.getValue());
        Assert.assertEquals(1.0e-5, entry.getDoubleValue(), 1.0e-15);
    }

    @Test
    public void testBadSyntax() {
        KeyValue entry = new KeyValue("there are no equal sign in this line", 17, "dummy");
        Assert.assertNull(entry.getKeyword());
        Assert.assertEquals("", entry.getKey());
        Assert.assertEquals("", entry.getValue());
    }

    @Test
    public void testUnderscoreEqualsBlank() {
        KeyValue entry = new KeyValue("CENTER_NAME = EARTH_BARYCENTER", 1, "dummy");
        Assert.assertEquals(Keyword.CENTER_NAME, entry.getKeyword());
        Assert.assertEquals("EARTH BARYCENTER", entry.getValue());
    }

    @Test
    public void testCoalescedBlanks() {
        KeyValue entry = new KeyValue("CENTER_NAME = EARTH    ___     BARYCENTER    ", 1, "dummy");
        Assert.assertEquals(Keyword.CENTER_NAME, entry.getKeyword());
        Assert.assertEquals("EARTH BARYCENTER", entry.getValue());
    }

}
