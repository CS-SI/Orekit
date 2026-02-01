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
package org.orekit.propagation.analytical;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.FieldSpacecraftStateInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldEphemerisTest {

    private static final Binary64Field FIELD = Binary64Field.getInstance();

    @Test
    void testFieldEphemerisException() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        // WHEN & THEN
        assertThrows(OrekitIllegalArgumentException.class, () -> new FieldEphemeris<>(spacecraftStates, 2));
    }

    @Test
    void testResetInitialState() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        // WHEN
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        // THEN
        assertThrows(OrekitException.class, () -> fieldEphemeris.resetInitialState(null));
    }

    @Test
    void testResetIntermediateState() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        // WHEN
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        // THEN
        assertThrows(OrekitException.class, () -> fieldEphemeris.resetIntermediateState(null, true));
    }

    @Test
    void testGetters() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        spacecraftStates.add(state.shiftedBy(10.));
        // WHEN
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        // THEN
        assertEquals(state.getFrame(), fieldEphemeris.getFrame());
        assertTrue(fieldEphemeris.getParametersDrivers().isEmpty());
        assertEquals(spacecraftStates.get(0).getDate(), fieldEphemeris.getMinDate());
        assertEquals(spacecraftStates.get(1).getDate(), fieldEphemeris.getMaxDate());
        assertInstanceOf(FieldSpacecraftStateInterpolator.class, fieldEphemeris.getStateInterpolator());
    }

    @Test
    void testGetMass() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        spacecraftStates.add(state.shiftedBy(10.));
        // WHEN
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        // THEN
        assertEquals(state.getMass(), fieldEphemeris.getMass(state.getDate()));
    }

    @Test
    void testPropagate() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        spacecraftStates.add(state.shiftedBy(10.));
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        // WHEN
        final FieldAbsoluteDate<Binary64> interpolationDate = state.getDate().shiftedBy(5);
        final FieldSpacecraftState<Binary64> interpolated = fieldEphemeris.propagate(interpolationDate);
        // THEN
        assertEquals(interpolated.getDate(), interpolationDate.getDate());
        final FieldSpacecraftState<Binary64> expected = fieldEphemeris.getStateInterpolator().interpolate(interpolationDate,
                spacecraftStates);
        assertEquals(expected.getPosition(), interpolated.getPosition());
        assertEquals(expected.getVelocity(), interpolated.getVelocity());
        assertEquals(expected.getMass(), interpolated.getMass());
    }

    @Test
    void testPropagateOrbit() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        spacecraftStates.add(state.shiftedBy(10.));
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        final FieldAbsoluteDate<Binary64> interpolationDate = state.getDate().shiftedBy(5);
        // WHEN
        final FieldOrbit<Binary64> interpolated = fieldEphemeris.propagateOrbit(interpolationDate, null);
        // THEN
        final FieldOrbit<Binary64> expected = fieldEphemeris.basicPropagate(interpolationDate).getOrbit();
        assertEquals(expected.getDate(), interpolated.getDate());
        assertEquals(expected.getFrame(), interpolated.getFrame());
        assertEquals(expected.getPosition(), interpolated.getPosition());
        assertEquals(expected.getVelocity(), interpolated.getVelocity());
    }

    @Test
    void testMixedStatesException() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        spacecraftStates.add(state);
        spacecraftStates.add(getFieldState(state.getDate().toAbsoluteDate().shiftedBy(1), false));
        // WHEN & THEN
        assertThrows(OrekitIllegalArgumentException.class, () -> new FieldEphemeris<>(spacecraftStates, 2));
    }

    @Test
    void testAdditional() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = getFieldState(AbsoluteDate.ARBITRARY_EPOCH, false);
        final List<FieldSpacecraftState<Binary64>> spacecraftStates = new ArrayList<>();
        final String name = "42";
        spacecraftStates.add(state.addAdditionalData(name, new Binary64(2)));
        // WHEN
        final FieldEphemeris<Binary64> fieldEphemeris = new FieldEphemeris<>(spacecraftStates, 1);
        // THEN
        assertTrue(fieldEphemeris.isAdditionalDataManaged(name));
        assertFalse(fieldEphemeris.isAdditionalDataManaged("41"));
        assertNotEquals(0, fieldEphemeris.getManagedAdditionalData().length);
        final FieldSpacecraftState<Binary64> interpolated = fieldEphemeris.propagate(state.getDate());
        assertNotNull(interpolated.getAdditionalData(name));
    }

    private static FieldSpacecraftState<Binary64> getFieldState(final AbsoluteDate date) {
        return getFieldState(date, true);
    }

    private static FieldSpacecraftState<Binary64> getFieldState(final AbsoluteDate date, final boolean isOrbit) {
        final Orbit orbit = TestUtils.getDefaultOrbit(date);
        if (isOrbit) {
            return new FieldSpacecraftState<>(new FieldCartesianOrbit<>(FIELD, orbit));
        } else {
            return new FieldSpacecraftState<>(new FieldAbsolutePVCoordinates<>(FIELD, new AbsolutePVCoordinates(orbit.getFrame(),
                    orbit.getDate(), orbit.getPVCoordinates())));
        }
    }
}
