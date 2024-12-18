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

import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.propagation.SpacecraftState;

/**
 * Data container for two-point boundary output of indirect shooting methods.
 *
 * @author Romain Serra
 * @since 12.2
 * @see AbstractIndirectShooting
 */
public class ShootingBoundaryOutput {

    /** Initial propagation state. */
    private final SpacecraftState initialState;

    /** Terminal propagation state. */
    private final SpacecraftState terminalState;

    /** Propagation settings. */
    private final ShootingPropagationSettings shootingPropagationSettings;

    /** Convergence flag. */
    private final boolean converged;

    /** Iteration count. */
    private final int iterationCount;

    /**
     * Constructor.
     * @param converged convergence flag
     * @param iterationCount iteration number
     * @param initialState initial state
     * @param terminalState terminal state
     * @param shootingPropagationSettings propagation settings
     */
    public ShootingBoundaryOutput(final boolean converged, final int iterationCount,
                                  final SpacecraftState initialState,
                                  final ShootingPropagationSettings shootingPropagationSettings,
                                  final SpacecraftState terminalState) {
        this.converged = converged;
        this.iterationCount = iterationCount;
        this.initialState = initialState;
        this.terminalState = terminalState;
        this.shootingPropagationSettings = shootingPropagationSettings;
    }

    /**
     * Getter for convergence flag.
     * @return convergence flag
     */
    public boolean isConverged() {
        return converged;
    }

    /**
     * Getter for the iteration number.
     * @return count
     */
    public int getIterationCount() {
        return iterationCount;
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

    /**
     * Getter for the shooting propagation settings.
     * @return propagation settings
     */
    public ShootingPropagationSettings getShootingPropagationSettings() {
        return shootingPropagationSettings;
    }
}
