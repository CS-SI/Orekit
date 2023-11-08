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

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for geographic latitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * latitude with respect to a central body.</p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class LatitudeCrossingDetector extends AbstractDetector<LatitudeCrossingDetector> {

    /** Body on which the latitude is defined. */
    private OneAxisEllipsoid body;

    /** Fixed latitude to be crossed. */
    private final double latitude;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the latitude is defined
     * @param latitude latitude to be crossed
     */
    public LatitudeCrossingDetector(final OneAxisEllipsoid body, final double latitude) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, body, latitude);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the latitude is defined
     * @param latitude latitude to be crossed
     */
    public LatitudeCrossingDetector(final double maxCheck, final double threshold,
                                    final OneAxisEllipsoid body, final double latitude) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing(),
             body, latitude);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param body body on which the latitude is defined
     * @param latitude latitude to be crossed
     */
    protected LatitudeCrossingDetector(final AdaptableInterval maxCheck, final double threshold,
                                       final int maxIter, final EventHandler handler,
                                       final OneAxisEllipsoid body, final double latitude) {
        super(maxCheck, threshold, maxIter, handler);
        this.body     = body;
        this.latitude = latitude;
    }

    /** {@inheritDoc} */
    @Override
    protected LatitudeCrossingDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                              final int newMaxIter,
                                              final EventHandler newHandler) {
        return new LatitudeCrossingDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                          body, latitude);
    }

    /** Get the body on which the geographic zone is defined.
     * @return body on which the geographic zone is defined
     */
    public OneAxisEllipsoid getBody() {
        return body;
    }

    /** Get the fixed latitude to be crossed (radians).
     * @return fixed latitude to be crossed (radians)
     */
    public double getLatitude() {
        return latitude;
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft latitude minus the fixed latitude to be crossed.
     * It is positive if the spacecraft is northward and negative if it is southward
     * with respect to the fixed latitude.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft latitude minus the fixed latitude to be crossed
     */
    public double g(final SpacecraftState s) {

        // convert state to geodetic coordinates
        final GeodeticPoint gp = body.transform(s.getPosition(),
                                                s.getFrame(), s.getDate());

        // latitude difference
        return gp.getLatitude() - latitude;

    }

}
