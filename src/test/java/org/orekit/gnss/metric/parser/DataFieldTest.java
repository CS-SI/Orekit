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
package org.orekit.gnss.metric.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitInternalError;

public class DataFieldTest {

    @Test
    public void testDefaultMethods() {
        MockDataField mdf = new MockDataField();

        // Boolean
        try {
            mdf.booleanValue(null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitInternalError oie) {
            Assertions.assertNull(oie.getCause());
        }

        // Integer
        try {
            mdf.intValue(null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitInternalError oie) {
            Assertions.assertNull(oie.getCause());
        }

        // Double
        try {
            mdf.doubleValue(null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitInternalError oie) {
            Assertions.assertNull(oie.getCause());
        }

        // String
        try {
            mdf.stringValue(null, 0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitInternalError oie) {
            Assertions.assertNull(oie.getCause());
        }

    }

    private static class MockDataField implements DataField {
        // Noting to do ...
    }

}
