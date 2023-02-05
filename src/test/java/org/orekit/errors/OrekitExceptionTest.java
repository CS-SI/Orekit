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

package org.orekit.errors;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.exception.DummyLocalizable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Unit tests for {@link OrekitException}.
 *
 * @author Evan Ward
 */
public class OrekitExceptionTest {

    /** Check {@link OrekitException#getMessage()} does not throw a NPE. */
    @Test
    public void testNullString() {
        // action
        OrekitException exception = new OrekitException(new DummyLocalizable(null));

        // verify
        Assertions.assertEquals(exception.getMessage(), "");
    }

    /**
     * Check that if the message formatting in {@link OrekitException#getLocalizedMessage()}
     * throws an exception a useful message and stack trace is still printed.
     */
    @Test
    public void testBadMessage() {
        // setup
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        OrekitMessages message = OrekitMessages.CORRUPTED_FILE;
        Object part = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("bad message");
            }
        };

        // action, message expects a parameter, but none is given
        new OrekitException(message, part).printStackTrace(printer);

        // verify
        String actual = writer.toString();
        MatcherAssert.assertThat(actual,
                CoreMatchers.containsString(message.getSourceString()));
        MatcherAssert.assertThat(actual,
                CoreMatchers.containsString("IllegalStateException: bad message"));
    }

}
