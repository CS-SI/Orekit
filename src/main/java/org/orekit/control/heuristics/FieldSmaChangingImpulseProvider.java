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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;

/**
 * Class modelling impulsive maneuvers to set the osculating semi-major axis to a given value.
 * The impulse vector is tangential and computed in the same frame as the orbit.
 * The resulting osculating eccentricity depends on the execution location. The instantaneous orbital plane is left unchanged.
 * A constraint on the maximum magnitude can be optionally set.
 * @see FieldAbstractInPlaneImpulseProvider
 * @see OsculatingSmaChangeImpulseProvider
 * @author Romain Serra
 * @since 14.0
 */
public class FieldSmaChangingImpulseProvider<T extends CalculusFieldElement<T>> extends FieldAbstractInPlaneImpulseProvider<T> {

    /** Target osculating semi-major axis. */
    private final T targetSemiMajorAxis;

    /**
     * Constructor with default maximum magnitude set to positive infinity (unconstrained).
     * @param targetSemiMajorAxis osculating value to achieve
     */
    public FieldSmaChangingImpulseProvider(final T targetSemiMajorAxis) {
        this(targetSemiMajorAxis.getField().getZero().newInstance(Double.POSITIVE_INFINITY), targetSemiMajorAxis);
    }

    /**
     * Constructor.
     * @param maximumMagnitude maximum magnitude
     * @param targetSemiMajorAxis osculating value to achieve
     */
    public FieldSmaChangingImpulseProvider(final T maximumMagnitude, final T targetSemiMajorAxis) {
        super(maximumMagnitude);
        this.targetSemiMajorAxis = targetSemiMajorAxis;
    }

    @Override
    public FieldVector3D<T> getUnconstrainedImpulse(final FieldSpacecraftState<T> state, final boolean isForward) {
        final FieldOrbit<T> orbit = state.getOrbit();
        final FieldVector3D<T> position = orbit.getPosition();
        final FieldVector3D<T> velocity = orbit.getVelocity();
        final T mu = orbit.getMu();
        final T targetEnergy = mu.negate().divide(targetSemiMajorAxis.multiply(2));
        final T targetSpeed = FastMath.sqrt((targetEnergy.add(mu.divide(position.getNorm2()))).multiply(2));
        return velocity.normalize().scalarMultiply(targetSpeed.subtract(velocity.getNorm2()));
    }
}
