/* Copyright 2002-2024 CS GROUP
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
package org.orekit.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;

class TimeStampedPairTest {
    @Test
    @DisplayName("test error thrown when using different dates")
    void testErrorThrownWhenUsingDifferentDates() {
        // Given
        Utils.setDataRoot("regular-data");

        final TimeStamped value1 = Mockito.mock(TimeStamped.class);
        final TimeStamped value2 = Mockito.mock(TimeStamped.class);

        Mockito.when(value1.getDate()).thenReturn(new AbsoluteDate());
        Mockito.when(value2.getDate()).thenReturn(new AbsoluteDate().shiftedBy(1));

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> new TimeStampedPair<>(value1, value2));

        Assertions.assertEquals(
                "first date 2000-01-01T11:58:55.816Z does not match second date 2000-01-01T11:58:56.816Z",
                thrown.getMessage());
    }

    @Test
    @DisplayName("Test getters")
    void testGetters() {
        // Given
        final TimeStamped value1 = Mockito.mock(TimeStamped.class);
        final TimeStamped value2 = Mockito.mock(TimeStamped.class);

        final AbsoluteDate defaultDate = new AbsoluteDate();

        Mockito.when(value1.getDate()).thenReturn(defaultDate);
        Mockito.when(value2.getDate()).thenReturn(defaultDate);

        // When
        final TimeStampedPair<TimeStamped, TimeStamped> pair = new TimeStampedPair<>(value1, value2);

        // Then
        Assertions.assertEquals(defaultDate, pair.getDate());
        Assertions.assertEquals(value1, pair.getFirst());
        Assertions.assertEquals(value2, pair.getSecond());
    }
}