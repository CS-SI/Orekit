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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EphemerisExtendedPositionProviderTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getTestOrbit());
        final List<SpacecraftState> states = new ArrayList<>();
        states.add(state);
        states.add(state.shiftedBy(1));
        final Ephemeris ephemeris = new Ephemeris(states, 1);
        final EphemerisExtendedPositionProvider provider = new EphemerisExtendedPositionProvider(ephemeris);
        final AbsoluteDate middleDate = state.getDate().shiftedBy(0.5);
        // WHEN
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = new FieldAbsoluteDate<>(ComplexField.getInstance(),
                middleDate).shiftedBy(Complex.I);
        final FieldVector3D<Complex> fieldPosition = provider.getPosition(fieldAbsoluteDate, state.getFrame());
        // THEN
        assertNotEquals(0., fieldPosition.getNorm2().getImaginary());
    }

    @Test
    void testGetFieldProvider() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getTestOrbit());
        final List<SpacecraftState> states = new ArrayList<>();
        states.add(state);
        states.add(state.shiftedBy(1));
        final Ephemeris ephemeris = new Ephemeris(states, 1);
        final EphemerisExtendedPositionProvider provider = new EphemerisExtendedPositionProvider(ephemeris);
        // WHEN
        final FieldEphemeris<Binary64> fieldEphemeris = provider.getFieldProvider(Binary64Field.getInstance());
        // THEN
        assertEquals(ephemeris.getFrame(), fieldEphemeris.getFrame());
        assertEquals(ephemeris.getStates().size(), fieldEphemeris.getStates().size());
        assertEquals(ephemeris.getMaxDate(), fieldEphemeris.getMaxDate().toAbsoluteDate());
        assertEquals(ephemeris.getMinDate(), fieldEphemeris.getMinDate().toAbsoluteDate());
    }
}
