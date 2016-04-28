/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for geographic longitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * longitude with respect to a central body.</p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class LongitudeCrossingDetector extends AbstractDetector<LongitudeCrossingDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150824L;

    /** Body on which the longitude is defined. */
    private OneAxisEllipsoid body;

    /** Fixed longitude to be crossed. */
    private final double longitude;

    /** Sign to apply for longitude difference. */
    private double sign;

    /** Previous longitude difference. */
    private double previousDelta;

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
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing<LongitudeCrossingDetector>(),
             body, longitude);
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
     * @param longitude longitude to be crossed
     */
    private LongitudeCrossingDetector(final double maxCheck, final double threshold,
                                     final int maxIter, final EventHandler<? super LongitudeCrossingDetector> handler,
                                     final OneAxisEllipsoid body, final double longitude) {
        super(maxCheck, threshold, maxIter, handler);
        this.body          = body;
        this.longitude     = longitude;
        this.sign          = +1.0;
        this.previousDelta = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    protected LongitudeCrossingDetector create(final double newMaxCheck, final double newThreshold,
                                              final int newMaxIter,
                                              final EventHandler<? super LongitudeCrossingDetector> newHandler) {
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
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {

        // convert state to geodetic coordinates
        final GeodeticPoint gp = body.transform(s.getPVCoordinates().getPosition(),
                                                s.getFrame(), s.getDate());

        // longitude difference
        double delta = MathUtils.normalizeAngle(sign * (gp.getLongitude() - longitude), 0.0);

        // ensure continuity
        if (FastMath.abs(delta - previousDelta) > FastMath.PI) {
            sign  = -sign;
            delta = MathUtils.normalizeAngle(sign * (gp.getLongitude() - longitude), 0.0);
        }
        previousDelta = delta;

        return delta;

    }

}
