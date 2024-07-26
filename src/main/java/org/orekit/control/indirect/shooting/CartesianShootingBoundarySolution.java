/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.shooting;

import org.orekit.propagation.SpacecraftState;

/**
 * Defines two-point boundary values for indirect shooting methods with Cartesian coordinates.
 *
 * @author Romain Serra
 * @since 12.2
 * @see FixedTimeCartesianSingleShooting
 */
public class CartesianShootingBoundarySolution {

    /** Initial propagation state. */
    private final SpacecraftState initialState;

    /** Terminal propagation state. */
    private final SpacecraftState terminalState;

    /**
     * Constructor.
     * @param initialState initial state
     * @param terminalState terminal state
     */
    public CartesianShootingBoundarySolution(final SpacecraftState initialState,
                                             final SpacecraftState terminalState) {
        this.initialState = initialState;
        this.terminalState = terminalState;
    }

    /**
     * Getter for the initial state.
     * @return initial state
     */
    public SpacecraftState getInitialState() {
        return initialState;
    }

    /**
     * Getter for the terminal state.
     * @return terminal state
     */
    public SpacecraftState getTerminalState() {
        return terminalState;
    }
}
