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

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.ElevationExtremumEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for elevation extremum with respect to a ground point.
 * <p>This detector identifies when a spacecraft reaches its
 * extremum elevation with respect to a ground point.</p>
 * <p>
 * As in most cases only the elevation maximum is needed and the
 * minimum is often irrelevant, this detector is often wrapped into
 * an {@link EventSlopeFilter event slope filter} configured with
 * {@link FilterType#TRIGGER_ONLY_DECREASING_EVENTS} (i.e. when the
 * elevation derivative decreases from positive values to negative values,
 * which correspond to a maximum). Setting up this filter saves some computation
 * time as the elevation minimum occurrences are not even looked at. It is
 * however still often necessary to do an additional filtering
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class ElevationExtremumDetector extends AbstractTopocentricDetector<ElevationExtremumDetector> {

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param topo topocentric frame centered on ground point
     */
    public ElevationExtremumDetector(final TopocentricFrame topo) {
        this(DEFAULT_MAX_CHECK, DEFAULT_THRESHOLD, topo);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param topo topocentric frame centered on ground point
     */
    public ElevationExtremumDetector(final double maxCheck, final double threshold,
                                     final TopocentricFrame topo) {
        this(new EventDetectionSettings(maxCheck, threshold, DEFAULT_MAX_ITER), new StopOnIncreasing(), topo);
    }

    /** Build a detector.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param topo topocentric frame centered on ground point
     */
    public ElevationExtremumDetector(final EventDetectionSettings detectionSettings, final EventHandler handler,
                                     final TopocentricFrame topo) {
        this(new ElevationExtremumEventFunction(topo), detectionSettings, handler);
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
     * @since 14.0
     */
    protected ElevationExtremumDetector(final ElevationExtremumEventFunction eventFunction,
                                        final EventDetectionSettings detectionSettings, final EventHandler handler) {
        super(eventFunction, detectionSettings, handler, eventFunction.getTopocentricFrame());
    }

    /** {@inheritDoc} */
    @Override
    protected ElevationExtremumDetector create(final EventDetectionSettings detectionSettings,
                                               final EventHandler newHandler) {
        return new ElevationExtremumDetector((ElevationExtremumEventFunction) getEventFunction(), detectionSettings,
                newHandler);
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft elevation first time derivative.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation first time derivative
     */
    public double g(final SpacecraftState s) {
        return getEventFunction().value(s);
    }
}
