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
package org.orekit.control.indirect.adjoint.cost;

import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;

/**
 * Abstract event detector for singularities in adjoint dynamics.
 *
 * @author Romain Serra
 * @since 13.0
 */
abstract class ControlSwitchDetector implements EventDetector {

    /** Event handler. */
    private final EventHandler handler = new ResetDerivativesOnEvent();

    /** Event detection settings. */
    private final EventDetectionSettings detectionSettings;

    /**
     * Constructor.
     * @param detectionSettings detection settings
     */
    protected ControlSwitchDetector(final EventDetectionSettings detectionSettings) {
        this.detectionSettings = detectionSettings;
    }

    @Override
    public EventDetectionSettings getDetectionSettings() {
        return detectionSettings;
    }

    @Override
    public EventHandler getHandler() {
        return handler;
    }
}
