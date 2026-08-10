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

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.AbsoluteDate;

class FieldAbstractDetectorTest {

    @Test
    void testGetEventFunction() {
        // GIVEN
        final FieldEventDetectionSettings<Complex> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(ComplexField.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final TestFieldDetector fieldEventDetector = new TestFieldDetector(fieldEventDetectionSettings, new FieldStopOnEvent<>());
        // WHEN
        final EventFunction eventFunction = fieldEventDetector.getEventFunction();
        // THEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);
        final FieldSpacecraftState<Complex> fieldState = new FieldSpacecraftState<>(ComplexField.getInstance(), state);
        Assertions.assertEquals(fieldEventDetector.g(fieldState), eventFunction.value(fieldState));
        final double expectedG = fieldEventDetector.g(fieldState).getReal();
        Assertions.assertEquals(expectedG, eventFunction.value(state));
        final FieldSpacecraftState<Binary64> binary64State = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        Assertions.assertEquals(expectedG, eventFunction.value(binary64State).getReal());
    }

    @Test
    void testToEventDetector() {
        // GIVEN
        final FieldEventDetectionSettings<Complex> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(ComplexField.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final EventHandler handler = new ContinueOnEvent();
        final TestFieldDetector fieldEventDetector = new TestFieldDetector(fieldEventDetectionSettings, new FieldStopOnEvent<>());
        // WHEN
        final EventDetector detector = fieldEventDetector.toEventDetector(handler);
        // THEN
        Assertions.assertEquals(handler, detector.getHandler());
        Assertions.assertEquals(fieldEventDetector.getEventFunction(), detector.getEventFunction());
    }

    @Test
    void testWithDetectionSettings() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldEventDetectionSettings<Complex> settings = new FieldEventDetectionSettings<>(field,
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final TestFieldDetector testDetector = new TestFieldDetector(settings, null);
        final FieldAdaptableInterval<Complex> mockedInterval = FieldAdaptableInterval.of(1.);
        final FieldEventDetectionSettings<Complex> expectedSettings = new FieldEventDetectionSettings<>(mockedInterval,
                Complex.ONE, 100);
        // WHEN
        final TestFieldDetector newDetector = testDetector.withDetectionSettings(expectedSettings);
        // THEN
        Assertions.assertEquals(mockedInterval, newDetector.getDetectionSettings().getMaxCheckInterval());
        Assertions.assertEquals(expectedSettings.getThreshold(), newDetector.getDetectionSettings().getThreshold());
        Assertions.assertEquals(expectedSettings.getMaxIterationCount(), newDetector.getDetectionSettings().getMaxIterationCount());
    }

    private static class TestFieldDetector extends FieldAbstractDetector<TestFieldDetector, Complex> {

        protected TestFieldDetector(FieldEventDetectionSettings<Complex> detectionSettings, FieldEventHandler<Complex> handler) {
            super(detectionSettings, handler);
        }

        @Override
        protected TestFieldDetector create(FieldEventDetectionSettings<Complex> detectionSettings, FieldEventHandler<Complex> newHandler) {
            return new TestFieldDetector(detectionSettings, newHandler);
        }

        @Override
        public Complex g(FieldSpacecraftState<Complex> s) {
            return Complex.ONE;
        }
    }
}
