/* Copyright 2022-2024 Romain Serra
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;

class ControlSwitchDetectorTest {

    @Test
    void testGetHandler() {
        // GIVEN
        final TestDetector detector = new TestDetector(null);
        // WHEN
        final EventHandler eventHandler = detector.getHandler();
        // THEN
        Assertions.assertInstanceOf(ResetDerivativesOnEvent.class, eventHandler);
    }

    @Test
    void testGetDetectionSettings() {
        // GIVEN
        final EventDetectionSettings mockedDetectionSettings = Mockito.mock(EventDetectionSettings.class);
        final TestDetector detector = new TestDetector(mockedDetectionSettings);
        // WHEN
        final EventDetectionSettings actualSettings = detector.getDetectionSettings();
        // THEN
        Assertions.assertEquals(mockedDetectionSettings, actualSettings);
    }

    private static class TestDetector extends ControlSwitchDetector {

        protected TestDetector(EventDetectionSettings detectionSettings) {
            super(detectionSettings);
        }

        @Override
        public double g(SpacecraftState s) {
            return 0;
        }
    }
}
