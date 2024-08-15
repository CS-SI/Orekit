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
package org.orekit.propagation.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;

class AbstractDetectorTest {

    @Test
    void testWithDetectionSettings() {
        // GIVEN
        final EventDetectionSettings settings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final TestDetector testDetector = new TestDetector(settings, null);
        final AdaptableInterval mockedInterval = Mockito.mock(AdaptableInterval.class);
        final EventDetectionSettings expectedSettings = new EventDetectionSettings(mockedInterval, 10., 100);
        // WHEN
        final TestDetector newDetector = testDetector.withDetectionSettings(expectedSettings);
        // THEN
        Assertions.assertEquals(mockedInterval, newDetector.getDetectionSettings().getMaxCheckInterval());
        Assertions.assertEquals(expectedSettings.getThreshold(), newDetector.getDetectionSettings().getThreshold());
        Assertions.assertEquals(expectedSettings.getMaxIterationCount(), newDetector.getDetectionSettings().getMaxIterationCount());
    }

    private static class TestDetector extends AbstractDetector<TestDetector> {

        protected TestDetector(EventDetectionSettings eventDetectionSettings, EventHandler handler) {
            super(eventDetectionSettings, handler);
        }

        @Override
        protected TestDetector create(AdaptableInterval newMaxCheck, double newThreshold, int newMaxIter, EventHandler newHandler) {
            return new TestDetector(new EventDetectionSettings(newMaxCheck, newThreshold, newMaxIter), newHandler);
        }

        @Override
        public double g(SpacecraftState s) {
            return 0;
        }
    }

}
