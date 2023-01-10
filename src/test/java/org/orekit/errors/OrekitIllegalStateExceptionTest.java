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

public class OrekitIllegalStateExceptionTest {

    @Test
    public void testNullSpecifier() {
        OrekitIllegalStateException e = new OrekitIllegalStateException(null, 1, 2, 3);
        Assertions.assertNull(e.getSpecifier());
        Assertions.assertEquals(3, e.getParts().length);
        Assertions.assertEquals("", e.getMessage());
        Assertions.assertEquals("", e.getLocalizedMessage());
        Assertions.assertEquals("", e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testNullParts() {
        OrekitIllegalStateException e =
                        new OrekitIllegalStateException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET,
                                                        (Object[]) null);
        Assertions.assertEquals(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET, e.getSpecifier());
        Assertions.assertEquals(0, e.getParts().length);
        Assertions.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assertions.assertEquals("le satellite s'est écrasé sur sa cible", e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testMessage() {
        OrekitIllegalStateException e =
                        new OrekitIllegalStateException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        Assertions.assertEquals(OrekitMessages.NON_EXISTENT_HMS_TIME, e.getSpecifier());
        Assertions.assertEquals(3, e.getParts().length);
        Assertions.assertEquals(97, ((Integer) e.getParts()[0]).intValue());
        Assertions.assertEquals(98, ((Integer) e.getParts()[1]).intValue());
        Assertions.assertEquals(99, ((Integer) e.getParts()[2]).intValue());
        Assertions.assertTrue(e.getMessage().contains("98"));
        Assertions.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assertions.assertEquals("heure inexistante 97:98:99", e.getMessage(Locale.FRENCH));
    }

}
