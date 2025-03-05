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
package org.orekit.propagation.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.intervals.AdaptableInterval;

class EventDetectionSettingsTest {

    @Test
    void testGetDefaultEventDetectionSettings() {
        // GIVEN

        // WHEN
        final EventDetectionSettings defaultSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        // THEN
        Assertions.assertEquals(FieldEventDetectionSettings.DEFAULT_MAX_ITER, defaultSettings.getMaxIterationCount());
        Assertions.assertEquals(FieldEventDetectionSettings.DEFAULT_THRESHOLD, defaultSettings.getThreshold());
        Assertions.assertEquals(FieldEventDetectionSettings.DEFAULT_MAX_CHECK, defaultSettings.getMaxCheckInterval()
                .currentInterval(Mockito.mock(SpacecraftState.class), true));
    }

    @Test
    void testWithThreshold() {
        // GIVEN
        final EventDetectionSettings defaultSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final double expectedThreshold = 123;
        // WHEN
        final EventDetectionSettings detectionSettings = defaultSettings.withThreshold(expectedThreshold);
        // THEN
        Assertions.assertEquals(expectedThreshold, detectionSettings.getThreshold());
    }

    @Test
    void testWithMaxIterationCount() {
        // GIVEN
        final EventDetectionSettings defaultSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final int expectedCount = 123;
        // WHEN
        final EventDetectionSettings detectionSettings = defaultSettings.withMaxIterationCount(expectedCount);
        // THEN
        Assertions.assertEquals(expectedCount, detectionSettings.getMaxIterationCount());
    }

    @Test
    void testWithMaxCheckInterval() {
        // GIVEN
        final EventDetectionSettings defaultSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final AdaptableInterval expectedInterval = Mockito.mock(AdaptableInterval.class);
        // WHEN
        final EventDetectionSettings detectionSettings = defaultSettings.withMaxCheckInterval(expectedInterval);
        // THEN
        Assertions.assertEquals(expectedInterval, detectionSettings.getMaxCheckInterval());
    }
}
