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

import org.orekit.orbits.Orbit;

/**
 * Defines two-point boundary values for indirect shooting methods with Cartesian coordinates.
 * This class represents the case where the initial and terminal times are fixed as well as the full
 * Cartesian coordinates (position and velocity vectors in some frame), using {@link org.orekit.orbits.Orbit} as data holder.
 * <br>
 * The terminal condition can be anterior in time to the initial one, it just means that the shooting method will perform backward propagation.
 * Also note that any acceleration vector passed in the {@link org.orekit.orbits.Orbit} is ignored.
 *
 * @author Romain Serra
 * @since 12.2
 * @see FixedTimeCartesianBoundaryStates
 */
public class FixedTimeBoundaryOrbits {

    /** Initial orbit (with date and frame). */
    private final Orbit initialOrbit;

    /** Terminal orbit (with date and frame). */
    private final Orbit terminalOrbit;

    /**
     * Constructor.
     * @param initialOrbit initial condition
     * @param terminalOrbit terminal condition
     */
    public FixedTimeBoundaryOrbits(final Orbit initialOrbit,
                                   final Orbit terminalOrbit) {
        this.initialOrbit = initialOrbit;
        this.terminalOrbit = terminalOrbit;
    }

    /**
     * Getter for the initial condition.
     * @return initial condition
     */
    public Orbit getInitialOrbit() {
        return initialOrbit;
    }

    /**
     * Getter for the terminal condition.
     * @return terminal condition
     */
    public Orbit getTerminalOrbit() {
        return terminalOrbit;
    }
}
