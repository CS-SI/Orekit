/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.bodies;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

class LazyLoadedCelestialBodiesTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data/de405-ephemerides");
    }

    @Test
    void testGetUnknownBodyThrowsOrekitException() {
        OrekitException exception = Assertions.assertThrows(OrekitException.class,
                () -> DataContext.getDefault().getCelestialBodies().getBody("MON"));
        Assertions.assertEquals(OrekitMessages.UNKNOWN_CELESTIAL_BODY, exception.getSpecifier());
        Assertions.assertTrue(exception.getMessage().contains("MON"));
    }

    @Test
    void testGetUnknownBodyLowerCase() {
        OrekitException exception = Assertions.assertThrows(OrekitException.class,
                () -> DataContext.getDefault().getCelestialBodies().getBody("unknown_body"));
        Assertions.assertEquals(OrekitMessages.UNKNOWN_CELESTIAL_BODY, exception.getSpecifier());
    }

    @Test
    void testGetKnownBodyWorks() {
        Assertions.assertNotNull(DataContext.getDefault().getCelestialBodies().getBody("Sun"));
    }

    @Test
    void testGetKnownBodyCaseInsensitive() {
        Assertions.assertNotNull(DataContext.getDefault().getCelestialBodies().getBody("sun"));
    }
}
