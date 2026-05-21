/* Copyright 2022-2026 Romain Serra
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ObserverTest {

    @Test
    void testGetParameterIndicesEmpty() {
        // GIVEN
        final List<ParameterDriver> drivers = new ArrayList<>();
        // WHEN
        final Map<?, ?> indices = Observer.getParameterIndices(new SpacecraftState[] {}, drivers);
        // THEN
        assertTrue(indices.isEmpty());
    }

    @Test
    void testGetParameterIndices() {
        // GIVEN
        final List<ParameterDriver> drivers = new ArrayList<>();
        final ParameterDriver driver = new ParameterDriver("", 0, 1, 0,1);
        driver.setSelected(true);
        drivers.add(driver);
        final SpacecraftState[] states = new SpacecraftState[] {mock(SpacecraftState.class)};
        // WHEN
        final Map<?, Integer> indices = Observer.getParameterIndices(states, drivers);
        // THEN
        assertEquals(1, indices.size());
        assertEquals(states.length * 6, indices.values().iterator().next());
    }
}
