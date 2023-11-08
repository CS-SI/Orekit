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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;

/** Detector for geographic longitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * longitude with respect to a central body.</p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class LongitudeCrossingDetector extends AbstractDetector<LongitudeCrossingDetector> {

    /** Body on which the longitude is defined. */
    private OneAxisEllipsoid body;

    /** Fixed longitude to be crossed. */
    private final double longitude;

    /** Filtering detector. */
    private final EventEnablingPredicateFilter filtering;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the longitude is defined
     * @param longitude longitude to be crossed
     */
    public LongitudeCrossingDetector(final OneAxisEllipsoid body, final double longitude) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, body, longitude);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the longitude is defined
     * @param longitude longitude to be crossed
     */
    public LongitudeCrossingDetector(final double maxCheck, final double threshold,
                                    final OneAxisEllipsoid body, final double longitude) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing(),
             body, longitude);
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
     * @param body body on which the longitude is defined
     * @param longitude longitude to be crossed
     */
    protected LongitudeCrossingDetector(final AdaptableInterval maxCheck, final double threshold,
                                        final int maxIter, final EventHandler handler,
                                        final OneAxisEllipsoid body, final double longitude) {

        super(maxCheck, threshold, maxIter, handler);

        this.body      = body;
        this.longitude = longitude;

        // we filter out spurious longitude crossings occurring at the antimeridian
        final RawLongitudeCrossingDetector raw = new RawLongitudeCrossingDetector(maxCheck, threshold, maxIter,
                                                                                  new ContinueOnEvent());
        final EnablingPredicate predicate =
            (state, detector, g) -> FastMath.abs(g) < 0.5 * FastMath.PI;
        this.filtering = new EventEnablingPredicateFilter(raw, predicate);

    }

    /** {@inheritDoc} */
    @Override
    protected LongitudeCrossingDetector create(final AdaptableInterval newMaxCheck, final double newThreshold, final int newMaxIter,
                                               final EventHandler newHandler) {
        return new LongitudeCrossingDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                             body, longitude);
    }

    /** Get the body on which the geographic zone is defined.
     * @return body on which the geographic zone is defined
     */
    public OneAxisEllipsoid getBody() {
        return body;
    }

    /** Get the fixed longitude to be crossed (radians).
     * @return fixed longitude to be crossed (radians)
     */
    public double getLongitude() {
        return longitude;
    }

    /**  {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        filtering.init(s0, t);
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
        return filtering.g(s);
    }

    private class RawLongitudeCrossingDetector extends AbstractDetector<RawLongitudeCrossingDetector> {

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
         */
        protected RawLongitudeCrossingDetector(final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                               final EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected RawLongitudeCrossingDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                                      final int newMaxIter,
                                                      final EventHandler newHandler) {
            return new RawLongitudeCrossingDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** Compute the value of the detection function.
         * <p>
         * The value is the longitude difference between the spacecraft and the fixed
         * longitude to be crossed, and it <em>does</em> change sign twice around
         * the central body: once at expected longitude and once at antimeridian.
         * The second sign change is a spurious one and is filtered out by the
         * outer class.
         * </p>
         * @param s the current state information: date, kinematics, attitude
         * @return longitude difference between the spacecraft and the fixed
         * longitude
         */
        public double g(final SpacecraftState s) {

            // convert state to geodetic coordinates
            final GeodeticPoint gp = body.transform(s.getPosition(),
                                                    s.getFrame(), s.getDate());

            // longitude difference
            return MathUtils.normalizeAngle(gp.getLongitude() - longitude, 0.0);

        }

    }

}
