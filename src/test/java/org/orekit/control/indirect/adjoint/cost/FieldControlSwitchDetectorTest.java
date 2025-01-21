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


import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;

class FieldControlSwitchDetectorTest {

    @Test
    void testGetHandler() {
        // GIVEN
        final TestDetector detector = new TestDetector(null);
        // WHEN
        final FieldEventHandler<Binary64> eventHandler = detector.getHandler();
        // THEN
        Assertions.assertInstanceOf(FieldResetDerivativesOnEvent.class, eventHandler);
    }

    @Test
    void testGetDetectionSettings() {
        // GIVEN
        final FieldEventDetectionSettings<Binary64> detectionSettings = new FieldEventDetectionSettings<>(Binary64Field.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final TestDetector detector = new TestDetector(detectionSettings);
        // WHEN
        final FieldEventDetectionSettings<Binary64> actualSettings = detector.getDetectionSettings();
        // THEN
        Assertions.assertEquals(detectionSettings, actualSettings);
    }

    private static class TestDetector extends FieldControlSwitchDetector<Binary64> {

        protected TestDetector(FieldEventDetectionSettings<Binary64> detectionSettings) {
            super(detectionSettings);
        }

        @Override
        public Binary64 g(FieldSpacecraftState<Binary64> s) {
            return Binary64.ZERO;
        }
    }
}
