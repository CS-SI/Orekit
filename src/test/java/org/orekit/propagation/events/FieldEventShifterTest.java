/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.events;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class FieldEventShifterTest {

    @Test
    void testEventOccurred() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldDateDetector<Binary64> dateDetector = new FieldDateDetector<>(fieldDate);
        final Binary64 increasingTimeShift = Binary64.ONE;
        final Binary64 decreasingTimeShift = new Binary64(2);
        final FieldEventShifter<Binary64> fieldEventShifter = new FieldEventShifter<>(dateDetector, true,
                increasingTimeShift, decreasingTimeShift);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(fieldDate.toAbsoluteDate()));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(fieldDate.getField(), state);
        fieldEventShifter.getHandler().eventOccurred(fieldState, fieldEventShifter, true);

        // WHEN
        final FieldSpacecraftState<Binary64> resettedState = fieldEventShifter.getHandler().resetState(fieldEventShifter, fieldState);

        // THEN
        assertEquals(fieldState.getOrbit(), resettedState.getOrbit());
        assertEquals(fieldState.getMass(), resettedState.getMass());
        assertEquals(fieldState.getAttitude(), resettedState.getAttitude());
        assertEquals(fieldState.getDate(), resettedState.getDate());
        assertEquals(fieldState.getAdditionalDataValues(), resettedState.getAdditionalDataValues());
        assertEquals(fieldState.getAdditionalStatesDerivatives().size(),
                resettedState.getAdditionalStatesDerivatives().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetters(final boolean useShiftedStates) {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldDateDetector<Binary64> dateDetector = new FieldDateDetector<>(fieldDate);
        final Binary64 increasingTimeShift = Binary64.ONE;
        final Binary64 decreasingTimeShift = new Binary64(2);

        // WHEN
        final FieldEventShifter<Binary64> fieldEventShifter = new FieldEventShifter<>(dateDetector, useShiftedStates,
                increasingTimeShift, decreasingTimeShift);

        // THEN
        assertEquals(increasingTimeShift, fieldEventShifter.getIncreasingTimeShift());
        assertEquals(decreasingTimeShift, fieldEventShifter.getDecreasingTimeShift());
        assertEquals(dateDetector, fieldEventShifter.getDetector());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWithDetectionSettings() {
        // GIVEN
        final FieldDateDetector<Binary64> detector = new FieldDateDetector<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        final FieldEventShifter<Binary64> template = new FieldEventShifter<>(detector, true, Binary64.ONE, new Binary64(2));
        final FieldEventDetectionSettings<Binary64> detectionSettings = Mockito.mock();
        // WHEN
        final FieldEventShifter<Binary64> shifter = template.withDetectionSettings(detectionSettings);
        // THEN
        Assertions.assertEquals(detector, shifter.getDetector());
        Assertions.assertEquals(detectionSettings, shifter.getDetectionSettings());
        Assertions.assertEquals(template.getIncreasingTimeShift(), shifter.getIncreasingTimeShift());
        Assertions.assertEquals(template.getDecreasingTimeShift(), shifter.getDecreasingTimeShift());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDetection(final boolean useShiftedStates) {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldRecordAndContinue<Binary64> recordAndContinue = new FieldRecordAndContinue<>();
        final FieldDateDetector<Binary64> dateDetector = new FieldDateDetector<>(fieldDate).withHandler(recordAndContinue);
        final Binary64 increasingTimeShift = Binary64.ONE;
        final Binary64 decreasingTimeShift = increasingTimeShift;
        final FieldEventShifter<Binary64> fieldEventShifter = new FieldEventShifter<>(dateDetector, useShiftedStates,
                increasingTimeShift, decreasingTimeShift);
        final Binary64 dt = Binary64.ONE;
        final Orbit orbit = TestUtils.getDefaultOrbit(fieldDate.shiftedBy(dt.negate()).toAbsoluteDate());
        final FieldOrbit<Binary64> initialOrbit = new FieldCartesianOrbit<>(Binary64Field.getInstance(), orbit);
        final FieldKeplerianPropagator<Binary64> keplerianPropagator = new FieldKeplerianPropagator<>(initialOrbit);
        keplerianPropagator.addEventDetector(fieldEventShifter);

        // WHEN
        keplerianPropagator.propagate(fieldDate.shiftedBy(dt));

        // THEN
        assertEquals(1, recordAndContinue.getEvents().size());
        final double shift = useShiftedStates ? increasingTimeShift.getReal() : 0.;
        assertEquals(dateDetector.getDate().shiftedBy(shift),
                recordAndContinue.getEvents().get(0).getState().getDate());
    }
}
