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

import org.junit.jupiter.api.Test;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BooleanEventFunctionTest {

    @Test
    void testDependsOnlyOnTimeSingle() {
        // GIVEN
        final EventFunction dateEventFunction = new SingleDateEventFunction(AbsoluteDate.ARBITRARY_EPOCH);
        final BooleanEventFunction booleanEventFunction = BooleanEventFunction.orCombine(Collections.singletonList(dateEventFunction));
        // WHEN
        final boolean actual = booleanEventFunction.dependsOnTimeOnly();
        // THEN
        assertTrue(actual);
    }

    @Test
    void testDependsOnlyOnTimeMultiple() {
        // GIVEN
        final EventFunction dateEventFunction = new SingleDateEventFunction(AbsoluteDate.ARBITRARY_EPOCH);
        final List<EventFunction> eventFunctions = new ArrayList<>();
        eventFunctions.add(dateEventFunction);
        eventFunctions.add(new ApsideEventFunction());
        final BooleanEventFunction booleanEventFunction = BooleanEventFunction.andCombine(eventFunctions);
        // WHEN
        final boolean actual = booleanEventFunction.dependsOnTimeOnly();
        // THEN
        assertFalse(actual);
    }

    @Test
    void testDependsOnMainVariablesOnly() {
        // GIVEN
        final EventFunction eventFunction = new EventFunction() {
            @Override
            public double value(SpacecraftState state) {
                return 0;
            }

            @Override
            public boolean dependsOnMainVariablesOnly() {
                return false;
            }
        };
        final BooleanEventFunction booleanEventFunction = BooleanEventFunction.orCombine(Collections.singletonList(eventFunction));
        // WHEN
        final boolean actual = booleanEventFunction.dependsOnMainVariablesOnly();
        // THEN
        assertFalse(actual);
    }
}
