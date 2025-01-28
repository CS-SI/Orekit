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

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;

/**
 * Step handler recording states.
 * Automatically clears them at start of propagation.
 *
 * @author Romain Serra
 * @since 13.0
 */
public class PropagationStepRecorder implements OrekitStepHandler {

    /**
     * Recorded times.
     */
    private final List<SpacecraftState> states;

    /**
     * Constructor.
     */
    public PropagationStepRecorder() {
        this.states = new ArrayList<>();
    }

    /**
     * Copy the current saved steps.
     * @return copy of steps
     */
    public List<SpacecraftState> copyStates() {
        return new ArrayList<>(states);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        OrekitStepHandler.super.init(s0, t);
        states.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final OrekitStepInterpolator interpolator) {
        states.add(interpolator.getCurrentState());
    }
}
