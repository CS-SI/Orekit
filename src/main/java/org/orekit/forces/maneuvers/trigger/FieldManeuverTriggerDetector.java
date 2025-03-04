/* Copyright 2022-2025 Romain Serra
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

package org.orekit.forces.maneuvers.trigger;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.events.FieldDetectorModifier;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;

/**
 * Wrapper for event detection triggering maneuvers (Field version).
 *
 * @see AbstractManeuverTriggers
 * @see ManeuverTriggerDetector
 * @author Romain Serra
 * @since 13.0
 */
public class FieldManeuverTriggerDetector<W extends CalculusFieldElement<W>, T extends FieldEventDetector<W>>
        implements FieldDetectorModifier<W> {

    /** Prototype detector. */
    private final T detector;

    /** Event handler. */
    private final FieldEventHandler<W> handler;

    /**
     * Constructor.
     * @param detector prototype detector
     * @param handler event handler
     */
    public FieldManeuverTriggerDetector(final T detector, final FieldEventHandler<W> handler) {
        this.detector = detector;
        this.handler = handler;
    }

    @Override
    public T getDetector() {
        return detector;
    }

    @Override
    public FieldEventHandler<W> getHandler() {
        return handler;
    }
}
