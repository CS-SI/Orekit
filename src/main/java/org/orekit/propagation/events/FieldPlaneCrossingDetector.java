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

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.PlaneCrossingFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;

/** Finder for plane crossing events.
 * @author Romain Serra
 * @since 14.0
 * @see PlaneCrossingDetector
 */
public class FieldPlaneCrossingDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldPlaneCrossingDetector<T>, T> {

    /** Constructor with detection settings and handler.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param planeCrossingFunction event function
     */
    public FieldPlaneCrossingDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                      final FieldEventHandler<T> handler,
                                      final PlaneCrossingFunction planeCrossingFunction) {
        super(planeCrossingFunction, detectionSettings, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldPlaneCrossingDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                   final FieldEventHandler<T> newHandler) {
        return new FieldPlaneCrossingDetector<>(detectionSettings, newHandler, (PlaneCrossingFunction) getEventFunction());
    }

    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return getEventFunction().value(s);
    }

    @Override
    public PlaneCrossingDetector toEventDetector(final EventHandler eventHandler) {
        return new PlaneCrossingDetector(getDetectionSettings().toEventDetectionSettings(), eventHandler,
                (PlaneCrossingFunction) getEventFunction());
    }
}
