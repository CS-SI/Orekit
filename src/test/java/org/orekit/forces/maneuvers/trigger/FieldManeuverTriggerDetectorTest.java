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

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.events.FieldDetectorModifier;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FieldManeuverTriggerDetectorTest {

    @Test
    @SuppressWarnings("unchecked")
    void testGetHandler() {
        // GIVEN
        final FieldEventDetector<Binary64> mockedDetector = Mockito.mock();
        final FieldEventHandler<Binary64> mockedHandler = Mockito.mock();
        final FieldManeuverTriggerDetector<Binary64, ?> triggerDetector = new FieldManeuverTriggerDetector<>(mockedDetector,
                mockedHandler);
        // WHEN
        final FieldEventHandler<Binary64> handler = triggerDetector.getHandler();
        // THEN
        assertEquals(mockedHandler, handler);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetDetector() {
        // GIVEN
        final FieldEventDetector<Binary64> mockedDetector = Mockito.mock();
        final FieldEventHandler<Binary64> mockedHandler = Mockito.mock();
        // WHEN
        final FieldManeuverTriggerDetector<Binary64, ?> triggerDetector = new FieldManeuverTriggerDetector<>(mockedDetector,
                mockedHandler);
        final FieldEventDetector<Binary64> detector = triggerDetector.getDetector();
        // THEN
        assertInstanceOf(FieldDetectorModifier.class, triggerDetector);
        assertEquals(mockedDetector, detector);
    }
}
