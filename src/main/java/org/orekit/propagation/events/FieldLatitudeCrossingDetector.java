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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.LatitudeValueCrossingFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;

/** Detector for geographic latitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * latitude with respect to a central body.</p>
 * @author Luc Maisonobe
 * @author Evan Ward
 * @since 9.3
 * @param <T> type of the field elements
 */
public class FieldLatitudeCrossingDetector <T extends CalculusFieldElement<T>>
        extends FieldAbstractGeographicalDetector<FieldLatitudeCrossingDetector<T>, T> {

    /** Fixed latitude to be crossed. */
    private final double latitude;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param field the type of numbers to use.
     * @param body body on which the latitude is defined
     * @param latitude latitude to be crossed
     */
    public FieldLatitudeCrossingDetector(final Field<T> field,
                                         final BodyShape body,
                                         final double latitude) {
        this(new FieldEventDetectionSettings<>(field, EventDetectionSettings.getDefaultEventDetectionSettings()),
                new FieldStopOnIncreasing<>(), body, latitude);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the latitude is defined
     * @param latitude latitude to be crossed
     */
    public FieldLatitudeCrossingDetector(final T maxCheck,
                                         final T threshold,
                                         final BodyShape body,
                                         final double latitude) {
        this(new FieldEventDetectionSettings<>(FieldAdaptableInterval.of(maxCheck.getReal()), threshold, DEFAULT_MAX_ITER),
                new FieldStopOnIncreasing<>(), body, latitude);
    }

    /** Constructor with input detection setting and handler.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param body body on which the latitude is defined
     * @param latitude latitude to be crossed
     * @since 13.0
     */
    public FieldLatitudeCrossingDetector(
            final FieldEventDetectionSettings<T> detectionSettings,
            final FieldEventHandler<T> handler,
            final BodyShape body,
            final double latitude) {
        this(new LatitudeValueCrossingFunction(body, latitude), detectionSettings, handler, body);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param eventFunction event function
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param body body on which the latitude is defined
     * @since 14.0
     */
    protected FieldLatitudeCrossingDetector(
            final LatitudeValueCrossingFunction eventFunction,
            final FieldEventDetectionSettings<T> detectionSettings,
            final FieldEventHandler<T> handler,
            final BodyShape body) {
        super(eventFunction, detectionSettings, handler, body);
        this.latitude = eventFunction.getCriticalLatitude();
    }

    /** {@inheritDoc} */
    @Override
    protected FieldLatitudeCrossingDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                      final FieldEventHandler<T> newHandler) {
        return new FieldLatitudeCrossingDetector<>((LatitudeValueCrossingFunction) getEventFunction(),
                detectionSettings, newHandler, getBodyShape());
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
    public T g(final FieldSpacecraftState<T> s) {
        return getEventFunction().value(s);
    }

    @Override
    public LatitudeCrossingDetector toEventDetector(final EventHandler eventHandler) {
        return new LatitudeCrossingDetector((LatitudeValueCrossingFunction) getEventFunction(),
                getDetectionSettings().toEventDetectionSettings(), eventHandler, getBodyShape());
    }
}
