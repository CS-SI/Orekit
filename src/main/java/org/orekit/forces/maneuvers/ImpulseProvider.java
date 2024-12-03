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
package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.SpacecraftState;

/** Interface providing velocity increment vectors to impulsive maneuvers.
 *
 * @author Romain Serra
 * @see ImpulseManeuver
 * @since 13.0
 */
@FunctionalInterface
public interface ImpulseProvider {

    /**
     * Method returning the impulse to be applied.
     * @param state state before the maneuver is applied if {@code isForward} is true, after otherwise
     * @param isForward flag on propagation direction
     * @param attitudeOverride maneuver attitude override, can be null
     * @return impulse in satellite's frame
     */
    Vector3D getImpulse(SpacecraftState state, boolean isForward, AttitudeProvider attitudeOverride);

    /**
     * Get a provider returning a given vector for forward propagation and its opposite for backward.
     * @param forwardImpulse forward impulse vector
     * @return constant provider
     */
    static ImpulseProvider of(Vector3D forwardImpulse) {
        return (state, isForward, attitudeOverride) -> isForward ? forwardImpulse : forwardImpulse.negate();
    }
}
