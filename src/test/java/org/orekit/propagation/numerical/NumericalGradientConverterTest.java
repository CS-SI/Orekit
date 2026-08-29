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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NumericalGradientConverterTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetStates(final boolean keepAdditionalData) {
        // GIVEN
        final DataDictionary dataDictionary = new DataDictionary();
        final String dataName = "data";
        final double value = 42;
        dataDictionary.put(dataName, value);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH)).withAdditionalData(dataDictionary);
        final NumericalGradientConverter converter = new NumericalGradientConverter(state, 3,  null, keepAdditionalData);
        final ParameterDriver driver = new ParameterDriver("dummy", 0., 1., 0., 1.);
        driver.setSelected(true);
        final ParameterDriversProvider provider = mock();
        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(driver);
        when(provider.getParametersDrivers()).thenReturn(drivers);
        // WHEN
        final FieldSpacecraftState<Gradient> fieldState = converter.getState(provider);
        // THEN
        assertEquals(state.getDate(), fieldState.getDate().toAbsoluteDate());
        assertEquals(state.isOrbitDefined(), fieldState.isOrbitDefined());
        assertEquals(state.getMass(), fieldState.getMass().getReal());
        if (keepAdditionalData) {
            assertEquals(value, fieldState.getAdditionalData(dataName));
        } else {
            assertEquals(0, fieldState.getAdditionalDataValues().size());
        }
    }
}
