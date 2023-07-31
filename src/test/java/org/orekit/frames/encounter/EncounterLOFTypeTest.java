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
package org.orekit.frames.encounter;

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.LOF;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

class EncounterLOFTypeTest {

    @Test
    @DisplayName("Test that getFrame return the expected frames")
    void testReturnExpectedEncounterFrame() {
        // Given
        final PVCoordinates pvMock = Mockito.mock(PVCoordinates.class);

        @SuppressWarnings("unchecked")
        final FieldPVCoordinates<Binary64> pvFieldMock = Mockito.mock(FieldPVCoordinates.class);

        // When
        final LOF defaultEncounter   = EncounterLOFType.DEFAULT.getFrame(pvMock);
        final LOF valsecchiEncounter = EncounterLOFType.VALSECCHI.getFrame(pvMock);

        final LOF defaultEncounterField   = EncounterLOFType.DEFAULT.getFrame(pvFieldMock);
        final LOF valsecchiEncounterField = EncounterLOFType.VALSECCHI.getFrame(pvFieldMock);

        // Then
        Assertions.assertInstanceOf(DefaultEncounterLOF.class, defaultEncounter);
        Assertions.assertInstanceOf(ValsecchiEncounterFrame.class, valsecchiEncounter);
        Assertions.assertInstanceOf(DefaultEncounterLOF.class, defaultEncounterField);
        Assertions.assertInstanceOf(ValsecchiEncounterFrame.class, valsecchiEncounterField);
    }

    @Test
    void testReturnExpectedName() {
       // GIVEN
        final PVCoordinates pvMock = Mockito.mock(PVCoordinates.class);

        final DefaultEncounterLOF encounterLOF = new DefaultEncounterLOF(pvMock);

       // WHEN
        final String name = encounterLOF.getName();

       // THEN
        final String expectedName = "DEFAULT_ENCOUNTER_LOF";

        Assertions.assertEquals(expectedName, name);
    }

}
