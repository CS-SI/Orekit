/* Copyright 2024-2026 Rafael Ayala
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
 * Class holding a solution to the Lambert problem.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class LambertSolution {

    /** number of complete revolutions. */
    private final int nRev;

    /** path (high or low). */
    private final LambertPathType pathType;

    /** orbit type (elliptic, parabolic, etc). */
    private final LambertOrbitType orbitType;

    /** posigrade flag (true for prograde orbits, false for retrograde orbits). */
    private final boolean posigrade;

    /** LambertBoundaryConditions with the boundary conditions for the Lambert problem. */
    private final LambertBoundaryConditions boundaryConditions;

    /** LambertBoundaryVelocities holding the initial and terminal velocities. */
    private final LambertBoundaryVelocities boundaryVelocities;

    /**
     * Basic constructor with initial and terminal velocities.
     * @param nRev number of complete revolutions
     * @param pathType path (high or low)
     * @param orbitType orbit type (elliptic, parabolic, etc)
     * @param posigrade posigrade flag (true for prograde orbits, false for retrograde orbits)
     * @param boundaryConditions LambertBoundaryConditions with the boundary conditions for the Lambert problem
     * @param v1 velocity at t1 (initial velocity)
     * @param v2 velocity at t2 (terminal velocity)
     */
    public LambertSolution(final int nRev, final LambertPathType pathType, final LambertOrbitType orbitType, final boolean posigrade,
                        final LambertBoundaryConditions boundaryConditions, final Vector3D v1, final Vector3D v2) {
        this.nRev = nRev;
        this.pathType = pathType;
        this.orbitType = orbitType;
        this.posigrade = posigrade;
        this.boundaryConditions = boundaryConditions;
        this.boundaryVelocities = new LambertBoundaryVelocities(v1, v2);
    }

    /**
     * Basic constructor with LambertBoundaryVelocities directly.
     * @param nRev number of complete revolutions
     * @param pathType path (high or low)
     * @param orbitType orbit type (elliptic, parabolic, etc)
     * @param posigrade posigrade flag (true for prograde orbits, false for retrograde orbits)
     * @param boundaryConditions LambertBoundaryConditions with the boundary conditions for the Lambert problem
     * @param boundaryVelocities LambertBoundaryVelocities with initial and terminal velocities
     */
    public LambertSolution(final int nRev, final LambertPathType pathType, final LambertOrbitType orbitType, final boolean posigrade,
                        final LambertBoundaryConditions boundaryConditions, final LambertBoundaryVelocities boundaryVelocities) {
        this.nRev = nRev;
        this.pathType = pathType;
        this.orbitType = orbitType;
        this.posigrade = posigrade;
        this.boundaryConditions = boundaryConditions;
        this.boundaryVelocities = boundaryVelocities;
    }

    /**
     * Get the number of complete revolutions.
     * @return number of complete revolutions
     */
    public int getNRev() {
        return nRev;
    }

    /**
     * Get the path type (high or low).
     * @return path type
     */
    public LambertPathType getPathType() {
        return pathType;
    }

    /**
     * Get the orbit type (elliptic, parabolic, hyperbolic).
     * @return orbit type
     */
    public LambertOrbitType getOrbitType() {
        return orbitType;
    }

    /**
     * Get the posigrade flag.
     * @return posigrade flag
     */
    public boolean getPosigrade() {
        return posigrade;
    }

    /**
     * Get the boundary conditions.
     * @return boundary conditions
     */
    public LambertBoundaryConditions getBoundaryConditions() {
        return boundaryConditions;
    }

    /**
     * Get the boundary velocities.
     * @return boundary velocities
     */
    public LambertBoundaryVelocities getBoundaryVelocities() {
        return boundaryVelocities;
    }
}
