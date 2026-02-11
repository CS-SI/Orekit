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

import org.orekit.bodies.BodyShape;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.LongitudeExtremumEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for geographic longitude extremum.
 * <p>This detector identifies when a spacecraft reaches its
 * extremum longitudes with respect to a central body.</p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class LongitudeExtremumDetector extends AbstractGeographicalDetector<LongitudeExtremumDetector> {

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the longitude is defined
     */
    public LongitudeExtremumDetector(final BodyShape body) {
        this(DEFAULT_MAX_CHECK, DEFAULT_THRESHOLD, body);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the longitude is defined
     */
    public LongitudeExtremumDetector(final double maxCheck, final double threshold, final BodyShape body) {
        this(new LongitudeExtremumEventFunction(body), new EventDetectionSettings(maxCheck, threshold, DEFAULT_MAX_ITER),
                new StopOnIncreasing());
    }

    /** Constructor with event function.
     * @param longitudeExtremumEventFunction event function
     * @since 14.0
     */
    public LongitudeExtremumDetector(final LongitudeExtremumEventFunction longitudeExtremumEventFunction) {
        this(longitudeExtremumEventFunction, EventDetectionSettings.getDefaultEventDetectionSettings(), new StopOnIncreasing());
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param longitudeExtremumEventFunction event function
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @since 14.0
     */
    protected LongitudeExtremumDetector(final LongitudeExtremumEventFunction longitudeExtremumEventFunction,
                                        final EventDetectionSettings detectionSettings, final EventHandler handler) {
        super(longitudeExtremumEventFunction, detectionSettings, handler, longitudeExtremumEventFunction.getBodyShape());
    }

    /** {@inheritDoc} */
    @Override
    protected LongitudeExtremumDetector create(final EventDetectionSettings detectionSettings,
                                               final EventHandler newHandler) {
        return new LongitudeExtremumDetector((LongitudeExtremumEventFunction) getEventFunction(), detectionSettings,
                newHandler);
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft longitude time derivative.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft longitude time derivative
     */
    public double g(final SpacecraftState s) {
        return getEventFunction().value(s);
    }

}
