/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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


import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class OrekitParseExceptionTest {

    @Test
    public void testNullSpecifier() {
        OrekitParseException e = new OrekitParseException(null, 1, 2, 3);
        Assert.assertNull(e.getSpecifier());
        Assert.assertEquals(3, e.getParts().length);
        Assert.assertEquals("", e.getMessage());
        Assert.assertEquals("", e.getLocalizedMessage());
        Assert.assertEquals("", e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testNullParts() {
        OrekitParseException e =
                        new OrekitParseException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET,
                                                 (Object[]) null);
        Assert.assertEquals(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET, e.getSpecifier());
        Assert.assertEquals(0, e.getParts().length);
        Assert.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assert.assertEquals("le satellite s'est écrasé sur sa cible", e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testMessage() {
        OrekitParseException e =
                        new OrekitParseException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        Assert.assertEquals(OrekitMessages.NON_EXISTENT_HMS_TIME, e.getSpecifier());
        Assert.assertEquals(3, e.getParts().length);
        Assert.assertEquals(97, ((Integer) e.getParts()[0]).intValue());
        Assert.assertEquals(98, ((Integer) e.getParts()[1]).intValue());
        Assert.assertEquals(99, ((Integer) e.getParts()[2]).intValue());
        Assert.assertTrue(e.getMessage().contains("98"));
        Assert.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assert.assertEquals("heure inexistante 97:98:99", e.getMessage(Locale.FRENCH));
    }

}
