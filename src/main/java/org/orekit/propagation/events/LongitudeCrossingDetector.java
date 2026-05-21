/* Copyright 2002-2026 CS GROUP
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
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.functions.LongitudeValueCrossingFunction;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;

/** Detector for geographic longitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * longitude with respect to a central body.</p>
 * <p>
 * The g value is the longitude difference between the spacecraft and the fixed
 * longitude to be crossed, with some sign tweaks to ensure continuity.
 * These tweaks imply the {@code increasing} flag in events detection becomes
 * irrelevant here! As an example, the longitude of a prograde spacecraft
 * will always increase, but this g function will increase and decrease so it
 * will cross the zero value once per orbit, in increasing and decreasing
 * directions on alternate orbits. If eastwards and westwards crossing have to
 * be distinguished, the velocity direction has to be checked instead of looking
 * at the {@code increasing} flag.
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class LongitudeCrossingDetector extends AbstractGeographicalDetector<LongitudeCrossingDetector>
        implements DetectorModifier {

    /** Fixed longitude to be crossed. */
    private final double longitude;

    /** Filtering detector. */
    private final EventEnablingPredicateFilter filtering;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the longitude is defined
     * @param longitude longitude to be crossed
     */
    public LongitudeCrossingDetector(final BodyShape body, final double longitude) {
        this(DEFAULT_MAX_CHECK, DEFAULT_THRESHOLD, body, longitude);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the longitude is defined
     * @param longitude longitude to be crossed
     */
    public LongitudeCrossingDetector(final double maxCheck, final double threshold,
                                     final BodyShape body, final double longitude) {
        this(new EventDetectionSettings(maxCheck, threshold, DEFAULT_MAX_ITER), new StopOnIncreasing(),
             body, longitude);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param body body on which the longitude is defined
     * @param longitude longitude to be crossed
     * @since 13.0
     */
    protected LongitudeCrossingDetector(final EventDetectionSettings detectionSettings, final EventHandler handler,
                                        final BodyShape body, final double longitude) {
        super(detectionSettings, handler, body);

        this.longitude = longitude;

        // The value is the longitude difference between the spacecraft and the fixed
        // longitude to be crossed, and it <em>does</em> change sign twice around
        // the central body: once at expected longitude and once at antimeridian.
        // The second sign change is a spurious one and is filtered out by the
        // outer class
        final EventFunction eventFunction = new LongitudeValueCrossingFunction(getBodyShape(), longitude);
        final EventDetector raw = EventDetector.of(eventFunction, new ContinueOnEvent(), getDetectionSettings());
        final EnablingPredicate predicate = new EnablingPredicate() {
            @Override
            public boolean eventIsEnabled(final SpacecraftState state, final EventDetector detector, final double g) {
                return FastMath.abs(g) < 0.5 * FastMath.PI;
            }

            @Override
            public boolean dependsOnMainVariablesOnly() {
                return true;
            }
        };
        this.filtering = new EventEnablingPredicateFilter(raw, predicate);

    }

    /** {@inheritDoc} */
    @Override
    protected LongitudeCrossingDetector create(final EventDetectionSettings detectionSettings,
                                               final EventHandler newHandler) {
        return new LongitudeCrossingDetector(detectionSettings, newHandler, getBodyShape(), longitude);
    }

    @Override
    public EventFunction getEventFunction() {
        return filtering.getEventFunction();
    }

    /** Get the fixed longitude to be crossed (radians).
     * @return fixed longitude to be crossed (radians)
     */
    public double getLongitude() {
        return longitude;
    }

    @Override
    public EventDetector getDetector() {
        return filtering;
    }

    /**  {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        super.init(s0, t);
        filtering.init(s0, t);
    }

}
