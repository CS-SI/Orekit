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
package org.orekit.propagation.sampling;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldPropagationStepRecorderTest {

    @Test
    void copyStatesAtConstructionTest() {
        // GIVEN
        final FieldPropagationStepRecorder<Binary64> recorder = new FieldPropagationStepRecorder<>();
        // WHEN
        final List<FieldSpacecraftState<Binary64>> states = recorder.copyStates();
        // THEN
        assertEquals(0, states.size());
    }

    @Test
    void copyStatesTest() {
        // GIVEN
        final FieldPropagationStepRecorder<Binary64> recorder = new FieldPropagationStepRecorder<>();
        final FieldOrekitStepInterpolator<Binary64> interpolator = createInterpolator();
        recorder.handleStep(interpolator);
        // WHEN
        recorder.init(createState(), FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        // THEN
        final List<FieldSpacecraftState<Binary64>> states = recorder.copyStates();
        assertEquals(0, states.size());
    }

    @Test
    void handleStepTest() {
        // GIVEN
        final FieldPropagationStepRecorder<Binary64> recorder = new FieldPropagationStepRecorder<>();
        final FieldOrekitStepInterpolator<Binary64> interpolator = createInterpolator();
        final int expectedSize = 10;
        // WHEN
        for (int i = 0; i < expectedSize; ++i) {
            recorder.handleStep(interpolator);
        }
        // WHEN
        final List<FieldSpacecraftState<Binary64>> states = recorder.copyStates();
        assertEquals(expectedSize, states.size());
    }

    private static FieldOrekitStepInterpolator<Binary64> createInterpolator() {
        return new FieldOrekitStepInterpolator<Binary64>() {
            @Override
            public FieldSpacecraftState<Binary64> getPreviousState() {
                return null;
            }

            @Override
            public FieldSpacecraftState<Binary64> getCurrentState() {
                return createState();
            }

            @Override
            public FieldSpacecraftState<Binary64> getInterpolatedState(FieldAbsoluteDate<Binary64> date) {
                return null;
            }

            @Override
            public boolean isForward() {
                return false;
            }

            @Override
            public FieldOrekitStepInterpolator<Binary64> restrictStep(FieldSpacecraftState<Binary64> newPreviousState, FieldSpacecraftState<Binary64> newCurrentState) {
                return null;
            }
        };
    }

    private static FieldSpacecraftState<Binary64> createState() {
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        return new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
    }
}
