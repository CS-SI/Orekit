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
package org.orekit.propagation.events.functions;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.OccultationEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UmbraEventFunctionTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0., 1e2, 1e3, 1e4})
    void testValueField(final double timeShift) {
        // GIVEN
        final ExtendedPositionProvider provider = new AnalyticalSolarPositionProvider();
        final OccultationEngine engine = new OccultationEngine(provider, Constants.SUN_RADIUS,
                new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 0., FramesFactory.getGTOD(true)));
        final UmbraEventFunction eventFunction = new UmbraEventFunction(engine);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(timeShift)));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        // WHEN
        final double actual = eventFunction.value(fieldState).getReal();
        // THEN
        final double expected = eventFunction.value(state);
        assertEquals(expected, actual);
    }

}
