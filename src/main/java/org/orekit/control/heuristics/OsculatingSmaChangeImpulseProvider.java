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
package org.orekit.control.heuristics;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;

/**
 * Class modelling impulsive maneuvers to set the osculating semi-major axis to a given value.
 * The impulse vector is tangential and computed in the same frame as the orbit.
 * The resulting osculating eccentricity depends on the execution location. The instantaneous orbital plane is left unchanged.
 * A constraint on the maximum magnitude can be optionally set.
 * @see AbstractInPlaneImpulseProvider
 * @author Romain Serra
 * @since 14.0
 */
public class OsculatingSmaChangeImpulseProvider extends AbstractInPlaneImpulseProvider {

    /** Target osculating semi-major axis. */
    private final double targetSemiMajorAxis;

    /**
     * Constructor with default maximum magnitude set to positive infinity (unconstrained).
     * @param targetSemiMajorAxis osculating value to achieve
     */
    public OsculatingSmaChangeImpulseProvider(final double targetSemiMajorAxis) {
        this(Double.POSITIVE_INFINITY, targetSemiMajorAxis);
    }

    /**
     * Constructor.
     * @param maximumMagnitude maximum magnitude
     * @param targetSemiMajorAxis osculating value to achieve
     */
    public OsculatingSmaChangeImpulseProvider(final double maximumMagnitude, final double targetSemiMajorAxis) {
        super(maximumMagnitude);
        this.targetSemiMajorAxis = targetSemiMajorAxis;
    }

    @Override
    public Vector3D getUnconstrainedImpulse(final SpacecraftState state, final boolean isForward) {
        final Orbit orbit = state.getOrbit();
        final Vector3D position = orbit.getPosition();
        final Vector3D velocity = orbit.getVelocity();
        final double mu = orbit.getMu();
        final double targetEnergy = -mu / (2. * targetSemiMajorAxis);
        final double targetSpeed = FastMath.sqrt(2. * (targetEnergy + mu / position.getNorm2()));
        return velocity.normalize().scalarMultiply(targetSpeed - velocity.getNorm2());
    }
}
