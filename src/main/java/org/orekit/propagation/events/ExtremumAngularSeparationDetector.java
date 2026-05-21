/* Copyright 2022-2026 Romain Serra
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

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.ExtremumAngularSeparationFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.utils.ExtendedPositionProvider;

/** Detector of local extrema with angular separation.
 * @author Romain Serra
 * @see AngularSeparationDetector
 * @see ExtremumAngularSeparationFunction
 * @since 13.1
 */
public class ExtremumAngularSeparationDetector extends AbstractDetector<ExtremumAngularSeparationDetector> {

    /** Beacon at the center of the proximity zone. */
    private final ExtendedPositionProvider beacon;

    /** Observer for the spacecraft, that may also see the beacon at the same time if they are too close. */
    private final ExtendedPositionProvider observer;

    /** Constructor with full parameters.
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @param beacon beacon at the center of the proximity zone
     * @param observer observer for the spacecraft, that may also see
     * the beacon at the same time if they are too close to each other
     */
    public ExtremumAngularSeparationDetector(final EventDetectionSettings detectionSettings,
                                             final EventHandler handler,
                                             final ExtendedPositionProvider beacon,
                                             final ExtendedPositionProvider observer) {
        super(new ExtremumAngularSeparationFunction(beacon, observer), detectionSettings, handler);
        this.beacon         = beacon;
        this.observer       = observer;
    }

    /** Get the beacon at the center of the proximity zone.
     * @return beacon at the center of the proximity zone
     */
    public ExtendedPositionProvider getBeacon() {
        return beacon;
    }

    /** Get the observer for the spacecraft.
     * @return observer for the spacecraft
     */
    public ExtendedPositionProvider getObserver() {
        return observer;
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) {
        return getEventFunction().value(s);
    }

    /** {@inheritDoc} */
    @Override
    protected ExtremumAngularSeparationDetector create(final EventDetectionSettings detectionSettings,
                                                       final EventHandler newHandler) {
        return new ExtremumAngularSeparationDetector(detectionSettings, newHandler, beacon, observer);
    }

}
