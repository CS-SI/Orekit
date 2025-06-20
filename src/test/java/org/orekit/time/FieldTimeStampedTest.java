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
package org.orekit.time;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldTimeStampedTest {

    @Test
    void testGetDurationFromTimeStamped() {
        // GIVEN
        final FieldTimeStamped<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final TimeStamped timeStamped = () -> fieldDate.getDate().toAbsoluteDate().shiftedBy(1);
        // WHEN
        final Binary64 duration = fieldDate.durationFrom(timeStamped);
        // THEN
        final Binary64 expected = fieldDate.getDate().durationFrom(timeStamped.getDate());
        Assertions.assertEquals(expected, duration);
    }
}
