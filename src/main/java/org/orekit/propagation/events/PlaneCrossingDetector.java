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
import org.orekit.propagation.events.functions.PlaneCrossingFunction;
import org.orekit.propagation.events.handlers.EventHandler;

/** Finder for plane crossing events.
 * @author Romain Serra
 * @since 14.0
 * @see PlaneCrossingFunction
 */
public class PlaneCrossingDetector extends AbstractDetector<PlaneCrossingDetector> {

    /** Constructor with detection settings and handler.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param planeCrossingFunction event function
     */
    public PlaneCrossingDetector(final EventDetectionSettings detectionSettings,
                                 final EventHandler handler,
                                 final PlaneCrossingFunction planeCrossingFunction) {
        super(planeCrossingFunction, detectionSettings, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected PlaneCrossingDetector create(final EventDetectionSettings detectionSettings,
                                              final EventHandler newHandler) {
        return new PlaneCrossingDetector(detectionSettings, newHandler, (PlaneCrossingFunction) getEventFunction());
    }

    @Override
    public double g(final SpacecraftState s) {
        return getEventFunction().value(s);
    }

}
