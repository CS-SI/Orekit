/* Copyright 2022-2025 Luc Maisonobe
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

package org.orekit.propagation.events.intervals;

import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;

/** This interface represents an event checking interval that depends on state.
*
* @see EventDetector
* @author Luc Maisonobe
* @since 12.0
*
*/
@FunctionalInterface
public interface AdaptableInterval {

    /**
     * Get the current value of maximal time interval between events handler checks.
     *
     * @param state     current state
     * @param isForward direction of propagation
     * @return current value of maximal time interval between events handler checks
     */
    double currentInterval(SpacecraftState state, boolean isForward);

    /**
     * Method creating a constant interval provider.
     * @param constantInterval value of constant interval
     * @return adaptable interval ready to be added to an event detector
     * @since 12.1
     */
    static AdaptableInterval of(final double constantInterval) {
        return (state, isForward) -> constantInterval;
    }

    /**
     * Method creating an interval taking the minimum value of all candidates.
     * @param defaultMaxCheck default value if no intervals is given as input
     * @param adaptableIntervals intervals
     * @return adaptable interval ready to be added to an event detector
     * @since 13.0
     */
    static AdaptableInterval of(final double defaultMaxCheck, final AdaptableInterval... adaptableIntervals) {
        return (state, isForward) -> {
            double maxCheck = defaultMaxCheck;
            for (final AdaptableInterval interval : adaptableIntervals) {
                maxCheck = FastMath.min(maxCheck, interval.currentInterval(state, isForward));
            }
            return maxCheck;
        };
    }
}
