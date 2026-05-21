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

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class FieldAltitudeDetectorTest {

    @Test
    void testGetter() {
        // GIVEN
        final Binary64 expectedAltitude = Binary64.ONE;
        final FieldAltitudeDetector<Binary64> fieldDetector = new FieldAltitudeDetector<>(expectedAltitude, mock(BodyShape.class));
        // WHEN
        final Binary64 actualAltitude = fieldDetector.getAltitude();
        // THEN
        assertEquals(expectedAltitude, actualAltitude);
    }

    @Test
    void testToEventDetector() {
        // GIVEN
        final FieldAltitudeDetector<Binary64> fieldDetector = new FieldAltitudeDetector<>(Binary64.ONE, mock(BodyShape.class));
        final EventHandler expectedHandler = new ContinueOnEvent();
        // WHEN
        final AltitudeDetector detector = fieldDetector.toEventDetector(expectedHandler);
        // THEN
        assertEquals(expectedHandler, detector.getHandler());
        assertEquals(fieldDetector.getBodyShape(), detector.getBodyShape());
        assertEquals(fieldDetector.getAltitude().getReal(), detector.getAltitude());
    }
}
