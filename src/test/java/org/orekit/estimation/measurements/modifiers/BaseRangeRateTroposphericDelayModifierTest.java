/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObserverSatellite;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseRangeRateTroposphericDelayModifierTest {

    @Test
    void testException() {
        // GIVEN
        final BaseRangeRateTroposphericDelayModifier modifier = mock();
        final SpacecraftState state = mock();
        final ObserverSatellite observerSatellite = mock();
        when(modifier.rangeRateErrorTroposphericModel(observerSatellite, state)).thenCallRealMethod();
        // WHEN
        Assertions.assertThrows(OrekitException.class,
                () -> modifier.rangeRateErrorTroposphericModel(observerSatellite, state));
    }

    @Test
    void testFieldException() {
        // GIVEN
        final BaseRangeRateTroposphericDelayModifier modifier = mock();
        final FieldSpacecraftState<Binary64> fieldSpacecraftState = mock();
        final ObserverSatellite observerSatellite = mock();
        when(modifier.rangeRateErrorTroposphericModel(observerSatellite, fieldSpacecraftState, null)).thenCallRealMethod();
        // WHEN
        Assertions.assertThrows(OrekitException.class,
                () -> modifier.rangeRateErrorTroposphericModel(observerSatellite, fieldSpacecraftState, null));
    }
}
