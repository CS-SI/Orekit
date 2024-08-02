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
package org.orekit.files.ccsds.definitions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyFacadeTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testIssue898Earth() {

        // Create the body facade
        final BodyFacade body = BodyFacade.create(CenterName.EARTH);

        // Verify
        assertEquals("EARTH", body.getName());
        assertEquals("Earth", body.getBody().getName());

    }

    @Test
    void testIssue898Sun() {

        // Create the body facade
        final BodyFacade body = BodyFacade.create(CenterName.SUN);

        // Verify
        assertEquals("SUN", body.getName());
        assertEquals("Sun", body.getBody().getName());

    }

    @Test
    void testIssue1137() {

        // Create the body facade (SUN)
        final BodyFacade sun = BodyFacade.create(CenterName.SUN, DataContext.getDefault());

        // Verify
        assertEquals("SUN", sun.getName());
        assertEquals("Sun", sun.getBody().getName());

        // Create the body facade (EARTH)
        final BodyFacade earth = BodyFacade.create(CenterName.EARTH, DataContext.getDefault().getCelestialBodies());

        // Verify
        assertEquals("EARTH", earth.getName());
        assertEquals("Earth", earth.getBody().getName());

    }

}
