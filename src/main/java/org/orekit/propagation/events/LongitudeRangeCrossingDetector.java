/* Copyright 2023-2024 Alberto Ferrero
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Alberto Ferrero licenses this file to You under the Apache License, Version 2.0
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
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;


/** Detector for geographic longitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * longitude range with respect to a central body.</p>
 * @author Alberto Ferrero
 * @since 12.0
 */
public class LongitudeRangeCrossingDetector extends AbstractDetector<LongitudeRangeCrossingDetector> {

    /** Body on which the longitude is defined. */
    private final OneAxisEllipsoid body;

    /** Fixed longitude to be crossed, lower boundary in radians. */
    private final double fromLongitude;

    /** Fixed longitude to be crossed, upper boundary in radians. */
    private final double toLongitude;

    /**
     * Sign, to get reversed inclusion longitude range (lower > upper).
     */
    private final double sign;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the longitude is defined
     * @param fromLongitude longitude to be crossed, lower range boundary
     * @param toLongitude longitude to be crossed, upper range boundary
     */
    public LongitudeRangeCrossingDetector(final OneAxisEllipsoid body, final double fromLongitude, final double toLongitude) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, body, fromLongitude, toLongitude);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the longitude is defined
     * @param fromLongitude longitude to be crossed, lower range boundary
     * @param toLongitude longitude to be crossed, upper range boundary
     */
    public LongitudeRangeCrossingDetector(final double maxCheck, final double threshold,
                                          final OneAxisEllipsoid body, final double fromLongitude, final double toLongitude) {
        this(AdaptableInterval.of(maxCheck), threshold, DEFAULT_MAX_ITER, new StopOnDecreasing(),
             body, fromLongitude, toLongitude);
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param body body on which the longitude is defined
     * @param fromLongitude longitude to be crossed, lower range boundary
     * @param toLongitude longitude to be crossed, upper range boundary
     */
    protected LongitudeRangeCrossingDetector(final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                             final EventHandler handler,
                                             final OneAxisEllipsoid body, final double fromLongitude, final double toLongitude) {
        super(new EventDetectionSettings(maxCheck, threshold, maxIter), handler);
        this.body     = body;
        this.fromLongitude = ensureLongitudePositiveContinuity(fromLongitude);
        this.toLongitude = ensureLongitudePositiveContinuity(toLongitude);
        this.sign = FastMath.signum(this.toLongitude - this.fromLongitude);
    }

    /** {@inheritDoc} */
    @Override
    protected LongitudeRangeCrossingDetector create(final AdaptableInterval newMaxCheck,
                                                    final double newThreshold,
                                                    final int newMaxIter,
                                                    final EventHandler newHandler) {
        return new LongitudeRangeCrossingDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                          body, fromLongitude, toLongitude);
    }

    /** Get the body on which the geographic zone is defined.
     * @return body on which the geographic zone is defined
     */
    public OneAxisEllipsoid getBody() {
        return body;
    }

    /** Get the fixed longitude range to be crossed (radians), lower boundary.
     * @return fixed lower boundary longitude range to be crossed (radians)
     */
    public double getFromLongitude() {
        return getLongitudeOverOriginalRange(fromLongitude);
    }

    /** Get the fixed longitude range to be crossed (radians), upper boundary.
     * @return fixed upper boundary longitude range to be crossed (radians)
     */
    public double getToLongitude() {
        return getLongitudeOverOriginalRange(toLongitude);
    }

    /**
     * Ensure continuity for negative angles, as longitude defined as [-PI, PI], transform negative to positive.
     * New longitude angle definition from [0, 2 PI].
     * @param longitude original longitude value
     * @return positive range longitude
     */
    private double ensureLongitudePositiveContinuity(final double longitude) {
        return longitude < 0 ? longitude + 2 * FastMath.PI : longitude;
    }

    /**
     * Get longitude shifted over the original range [-PI, PI].
     * @param longitude longitude value to convert
     * @return original range longitude
     */
    private double getLongitudeOverOriginalRange(final double longitude) {
        return longitude > FastMath.PI ? longitude - 2 * FastMath.PI : longitude;
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is positive if the spacecraft longitude is inside the longitude range.
     * The longitude value is reflected from [-PI, +PI] to [0, 2 PI] to ensure continuity.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return positive if spacecraft inside the range
     */
    public double g(final SpacecraftState s) {

        // convert state to geodetic coordinates
        final GeodeticPoint gp = body.transform(s.getPVCoordinates().getPosition(),
            s.getFrame(), s.getDate());

        // point longitude
        final double longitude = ensureLongitudePositiveContinuity(gp.getLongitude());

        // inside or outside longitude range
        return sign * (longitude - fromLongitude) * (toLongitude - longitude);

    }

}
