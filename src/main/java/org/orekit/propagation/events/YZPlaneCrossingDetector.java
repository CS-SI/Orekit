/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.propagation.events.handlers.StopOnDecreasing;

/** Detector for YZ Planes crossing.
 * @author Vincent Mouraux
 */
public class YZPlaneCrossingDetector
    extends
    AbstractDetector<YZPlaneCrossingDetector> {

    /** Offset from X=0 plane. */
    private final double delta;

    /**
     * Simple Constructor.
     * @param delta Offset from X=0 plane;
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public YZPlaneCrossingDetector(final double delta, final double maxCheck, final double threshold) {
        this(delta, maxCheck, threshold, DEFAULT_MAX_ITER,
             new StopOnDecreasing<YZPlaneCrossingDetector>());
    }

    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     * @param delta Offset from 0
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    private YZPlaneCrossingDetector(final double delta, final double maxCheck, final double threshold,
                             final int maxIter,
                             final EventHandler<? super YZPlaneCrossingDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.delta = delta;
    }

    /** {@inheritDoc} */
    @Override
    protected YZPlaneCrossingDetector
        create(final double newMaxCheck, final double newThreshold,
               final int newMaxIter,
               final EventHandler<? super YZPlaneCrossingDetector> newHandler) {
        return new YZPlaneCrossingDetector(delta, newMaxCheck, newThreshold, newMaxIter,
                                    newHandler);
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the longitude difference between the spacecraft and the fixed
     * longitude to be crossed, with some sign tweaks to ensure continuity.
     * These tweaks imply the {@code increasing} flag in events detection becomes
     * irrelevant here! As an example, the longitude of a prograde spacecraft
     * will always increase, but this g function will increase and decrease so it
     * will cross the zero value once per orbit, in increasing and decreasing
     * directions on alternate orbits. If eastwards and westwards crossing have to
     * be distinguished, the velocity direction has to be checked instead of looking
     * at the {@code increasing} flag.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return longitude difference between the spacecraft and the fixed
     * longitude, with some sign tweaks to ensure continuity
     */
    public double g(final SpacecraftState s) {
        return s.getPVCoordinates().getPosition().getX() * 1E6 - delta * 1E6;

    }
}
