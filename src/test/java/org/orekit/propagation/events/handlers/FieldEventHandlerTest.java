/* Copyright 2022-2024 Romain Serra
 * Licensed to CS Group under one or more
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
package org.orekit.propagation.events.handlers;


import org.hipparchus.complex.Complex;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;

class FieldEventHandlerTest {

    @Test
    @SuppressWarnings("unchecked")
    void testFinish() {
        // GIVEN
        final TestFieldHandler handler = new TestFieldHandler();
        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> handler.finish(Mockito.mock(FieldSpacecraftState.class),
                Mockito.mock(FieldEventDetector.class)));
    }

    private static class TestFieldHandler implements FieldEventHandler<Complex> {

        @Override
        public Action eventOccurred(FieldSpacecraftState<Complex> s, FieldEventDetector<Complex> detector, boolean increasing) {
            return null;
        }
    }
}
