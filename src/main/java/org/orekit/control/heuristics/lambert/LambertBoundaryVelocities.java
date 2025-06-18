/* Copyright 2020-2025 Exotrail
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
package org.orekit.control.heuristics.lambert;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/**
 * Class holding the two velocity vectors of a Lambert arc.
 *
 * @author Romain Serra
 * @since 13.1
 */
public class LambertBoundaryVelocities {

    /** Initial velocity vector. */
    private final Vector3D initialVelocity;

    /** Terminal velocity vector. */
    private final Vector3D terminalVelocity;

    /**
     * Constructor.
     * @param initialVelocity initial velocity
     * @param terminalVelocity terminal velocity
     */
    public LambertBoundaryVelocities(final Vector3D initialVelocity, final Vector3D terminalVelocity) {
        this.initialVelocity = initialVelocity;
        this.terminalVelocity = terminalVelocity;
    }

    /**
     * Getter for the initial velocity vector.
     * @return initial velocity
     */
    public Vector3D getInitialVelocity() {
        return initialVelocity;
    }

    /**
     * Getter for the terminal velocity vector.
     * @return terminal velocity
     */
    public Vector3D getTerminalVelocity() {
        return terminalVelocity;
    }
}
