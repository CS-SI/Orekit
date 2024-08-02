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
package org.orekit.errors;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrekitParseExceptionTest {

    @Test
    void testNullSpecifier() {
        OrekitParseException e = new OrekitParseException(null, 1, 2, 3);
        assertNull(e.getSpecifier());
        assertEquals(3, e.getParts().length);
        assertEquals("", e.getMessage());
        assertEquals("", e.getLocalizedMessage());
        assertEquals("", e.getMessage(Locale.FRENCH));
    }

    @Test
    void testNullParts() {
        OrekitParseException e =
                        new OrekitParseException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET,
                                                 (Object[]) null);
        assertEquals(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET, e.getSpecifier());
        assertEquals(0, e.getParts().length);
        assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        assertEquals("le satellite s'est écrasé sur sa cible", e.getMessage(Locale.FRENCH));
    }

    @Test
    void testMessage() {
        OrekitParseException e =
                        new OrekitParseException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        assertEquals(OrekitMessages.NON_EXISTENT_HMS_TIME, e.getSpecifier());
        assertEquals(3, e.getParts().length);
        assertEquals(97, ((Integer) e.getParts()[0]).intValue());
        assertEquals(98, ((Integer) e.getParts()[1]).intValue());
        assertEquals(99, ((Integer) e.getParts()[2]).intValue());
        assertTrue(e.getMessage().contains("98"));
        assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        assertEquals("heure inexistante 97:98:99", e.getMessage(Locale.FRENCH));
    }

}
