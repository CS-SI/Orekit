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
package org.orekit.control.indirect.shooting.boundary;

import org.orekit.utils.AbsolutePVCoordinates;

/**
 * Defines two-point boundary values for indirect shooting methods with Cartesian coordinates.
 * This class represents the case where the initial and terminal times are fixed as well as the full
 * Cartesian coordinates (position and velocity vectors in some frame), using {@link AbsolutePVCoordinates} as data holder.
 * <br>
 * The terminal condition can be anterior in time to the initial one, it just means that the shooting method will perform backward propagation.
 * Also note that any acceleration vector passed in the {@link AbsolutePVCoordinates} is ignored.
 *
 * @author Romain Serra
 * @since 12.2
 * @see FixedTimeBoundaryOrbits
 */
public class FixedTimeCartesianBoundaryStates {

    /** Initial Cartesian coordinates with date and frame. */
    private final AbsolutePVCoordinates initialCartesianState;

    /** Terminal Cartesian coordinates with date and frame. */
    private final AbsolutePVCoordinates terminalCartesianState;

    /**
     * Constructor.
     * @param initialCartesianState initial condition
     * @param terminalCartesianState terminal condition
     */
    public FixedTimeCartesianBoundaryStates(final AbsolutePVCoordinates initialCartesianState,
                                            final AbsolutePVCoordinates terminalCartesianState) {
        this.initialCartesianState = initialCartesianState;
        this.terminalCartesianState = terminalCartesianState;
    }

    /**
     * Getter for the initial Cartesian condition.
     * @return initial condition
     */
    public AbsolutePVCoordinates getInitialCartesianState() {
        return initialCartesianState;
    }

    /**
     * Getter for the terminal Cartesian condition.
     * @return terminal condition
     */
    public AbsolutePVCoordinates getTerminalCartesianState() {
        return terminalCartesianState;
    }
}
