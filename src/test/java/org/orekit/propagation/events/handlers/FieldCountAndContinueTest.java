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
package org.orekit.propagation.events.handlers;

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FieldCountAndContinueTest {

    @Test
    void testConstructor() {
        // GIVEN
        final int expectedCount = 2;
        // WHEN
        final FieldCountAndContinue<Binary64> handler = new FieldCountAndContinue<>(expectedCount);
        // THEN
        Assertions.assertEquals(expectedCount, handler.getCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDoesCount(final boolean isIncreasing) {
        // GIVEN
        final FieldCountAndContinue<Binary64> handler = new FieldCountAndContinue<>(0);
        // WHEN
        final boolean value = handler.doesCount(null, null, isIncreasing);
        // THEN
        Assertions.assertTrue(value);
    }
}
