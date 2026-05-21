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
 * Class modelling impulsive maneuvers to make the orbit circular i.e. setting the osculating eccentricity to zero.
 * The impulse vector is computed in the same frame as the orbit.
 * The resulting osculating semi-major axis depends on the execution location. The instantaneous orbital plane is left unchanged.
 * A constraint on the maximum magnitude can be optionally set.
 * @see AbstractInPlaneImpulseProvider
 * @author Romain Serra
 * @since 14.0
 */
public class CircularizingImpulseProvider extends AbstractInPlaneImpulseProvider {

    /**
     * Constructor with default maximum magnitude set to positive infinity (unconstrained).
     */
    public CircularizingImpulseProvider() {
        this(Double.POSITIVE_INFINITY);
    }

    /**
     * Constructor.
     * @param maximumMagnitude maximum magnitude
     */
    public CircularizingImpulseProvider(final double maximumMagnitude) {
        super(maximumMagnitude);
    }

    @Override
    public Vector3D getUnconstrainedImpulse(final SpacecraftState state, final boolean isForward) {
        final Orbit orbit = state.getOrbit();
        final Vector3D position = orbit.getPosition();
        final Vector3D momentum = orbit.getPVCoordinates().getMomentum();
        final double circularSpeed = FastMath.sqrt(orbit.getMu() / position.getNorm2());
        final Vector3D circularVelocity = Vector3D.crossProduct(momentum, position).normalize().scalarMultiply(circularSpeed);
        return circularVelocity.subtract(orbit.getVelocity());
    }
}
