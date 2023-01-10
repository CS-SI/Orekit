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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

public class OrekitInternalErrorTest {

    @Test
    public void testMessage() {
        OrekitInternalError e = new OrekitInternalError(null);
        Assertions.assertEquals(OrekitMessages.INTERNAL_ERROR, e.getSpecifier());
        Assertions.assertEquals(1, e.getParts().length);
        Assertions.assertEquals("https://forum.orekit.org", e.getParts()[0]);
        Assertions.assertTrue(e.getMessage().contains("https://forum.orekit.org"));
        Assertions.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assertions.assertEquals("erreur interne, merci de signaler le problème en ouvrant une nouvelle discussion sur https://forum.orekit.org",
                            e.getMessage(Locale.FRENCH));
    }

}
