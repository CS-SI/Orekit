/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.events;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for XZ Plane crossing.
 * @author Vincent Mouraux
 * @since 10.2
 */
public class HaloXZPlaneCrossingDetector extends AbstractDetector<HaloXZPlaneCrossingDetector> {

    /**
     * Simple Constructor.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public HaloXZPlaneCrossingDetector(final double maxCheck, final double threshold) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER,
             new StopOnIncreasing());
    }

    /**
     * Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    protected HaloXZPlaneCrossingDetector(final AdaptableInterval maxCheck, final double threshold,
                                          final int maxIter,
                                          final EventHandler handler) {
        super(maxCheck, threshold, maxIter, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected HaloXZPlaneCrossingDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                                 final int newMaxIter,
                                                 final EventHandler newHandler) {
        return new HaloXZPlaneCrossingDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /** Compute the value of the detection function.
     * @param s the current state information: date, kinematics, attitude
     * @return Position on Y axis
     */
    public double g(final SpacecraftState s) {
        return s.getPosition().getY();
    }

}
