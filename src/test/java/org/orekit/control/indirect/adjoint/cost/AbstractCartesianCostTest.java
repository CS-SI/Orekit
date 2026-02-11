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
package org.orekit.control.indirect.adjoint.cost;

import org.junit.jupiter.api.Test;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractCartesianCostTest {

    @Test
    void testGetFieldAdjointVelocityNorm() {
        // GIVEN
        final AbstractCartesianCost cost = mock();
        when(cost.buildSwitchDetector(any(), any())).thenCallRealMethod();
        final EventDetectionSettings detectionSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final ControlSwitchFunction controlSwitchFunction = mock();
        // WHEN
        final EventDetector detector = cost.buildSwitchDetector(controlSwitchFunction, detectionSettings);
        // THEN
        assertEquals(detector.getEventFunction(), controlSwitchFunction);
        assertEquals(detector.getDetectionSettings(), detectionSettings);
        assertInstanceOf(ResetDerivativesOnEvent.class, detector.getHandler());
    }
}
