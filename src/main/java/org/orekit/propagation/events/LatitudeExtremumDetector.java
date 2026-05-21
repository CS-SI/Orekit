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
import org.orekit.propagation.events.functions.LatitudeExtremumEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for geographic latitude extremum.
 * <p>This detector identifies when a spacecraft reaches its
 * extremum latitudes with respect to a central body.</p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class LatitudeExtremumDetector extends AbstractGeographicalDetector<LatitudeExtremumDetector> {

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the latitude is defined
     */
    public LatitudeExtremumDetector(final BodyShape body) {
        this(DEFAULT_MAX_CHECK, DEFAULT_THRESHOLD, body);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the latitude is defined
     */
    public LatitudeExtremumDetector(final double maxCheck, final double threshold,
                                    final BodyShape body) {
        this(new LatitudeExtremumEventFunction(body), new EventDetectionSettings(maxCheck, threshold, DEFAULT_MAX_ITER),
                new StopOnIncreasing());
    }

    /** Constructor with event function.
     * @param latitudeExtremumEventFunction event function
     * @since 14.0
     */
    public LatitudeExtremumDetector(final LatitudeExtremumEventFunction latitudeExtremumEventFunction) {
        this(latitudeExtremumEventFunction, EventDetectionSettings.getDefaultEventDetectionSettings(), new StopOnIncreasing());
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param latitudeExtremumEventFunction event function
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     */
    protected LatitudeExtremumDetector(final LatitudeExtremumEventFunction latitudeExtremumEventFunction,
                                       final EventDetectionSettings detectionSettings, final EventHandler handler) {
        super(latitudeExtremumEventFunction, detectionSettings, handler, latitudeExtremumEventFunction.getBodyShape());
    }

    /** {@inheritDoc} */
    @Override
    protected LatitudeExtremumDetector create(final EventDetectionSettings detectionSettings,
                                              final EventHandler newHandler) {
        return new LatitudeExtremumDetector((LatitudeExtremumEventFunction) getEventFunction(), detectionSettings,
                newHandler);
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft latitude time derivative.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft latitude time derivative
     */
    public double g(final SpacecraftState s) {
        return getEventFunction().value(s);
    }

}
