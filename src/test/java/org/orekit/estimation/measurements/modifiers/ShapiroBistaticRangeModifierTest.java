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

import org.junit.jupiter.api.Test;
import org.orekit.utils.Constants;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapiroBistaticRangeModifierTest {

    @Test
    void testGetEffectName() {
        // GIVEN
        final double mu = Constants.EGM96_EARTH_MU;
        final ShapiroBistaticRangeModifier modifier = new ShapiroBistaticRangeModifier(mu);
        // WHEN
        final String name = modifier.getEffectName();
        // THEN
        assertEquals("Shapiro", name);
    }
}
