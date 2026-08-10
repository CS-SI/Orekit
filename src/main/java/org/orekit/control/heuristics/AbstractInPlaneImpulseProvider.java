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
import org.orekit.forces.maneuvers.ImpulseProvider;
import org.orekit.propagation.SpacecraftState;

/**
 * Abstract class modelling impulsive, in-plane maneuvers.
 * The impulse vector is computed in the same frame as the orbit. The instantaneous orbital plane is left unchanged.
 * A constraint on the maximum magnitude can be optionally set.
 * @see ImpulseProvider
 * @see org.orekit.forces.maneuvers.ImpulseManeuver
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractInPlaneImpulseProvider implements ImpulseProvider {

    /** Maximum impulse magnitude. */
    private final double maximumMagnitude;

    /**
     * Constructor.
     * @param maximumMagnitude maximum magnitude
     */
    protected AbstractInPlaneImpulseProvider(final double maximumMagnitude) {
        this.maximumMagnitude = FastMath.abs(maximumMagnitude);
    }

    /**
     * Getter for the maximum impulse's magnitude.
     * @return maximum magnitude
     */
    public double getMaximumMagnitude() {
        return maximumMagnitude;
    }

    @Override
    public Vector3D getImpulse(final SpacecraftState state, final boolean isForward) {
        final Vector3D impulse = getUnconstrainedImpulse(state, isForward);
        if (impulse.getNorm2() > maximumMagnitude) {
            return impulse.normalize().scalarMultiply(maximumMagnitude);
        }
        return impulse;
    }

    /**
     * Compute the impulse without magnitude constraint.
     * @param state state immediately before (or after in backward time) the maneuver
     * @param isForward flag on propagation direction
     * @return impulse vector
     */
    protected abstract Vector3D getUnconstrainedImpulse(SpacecraftState state, boolean isForward);
}
