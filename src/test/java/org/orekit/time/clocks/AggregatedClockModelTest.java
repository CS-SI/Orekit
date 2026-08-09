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
package org.orekit.time.clocks;

import java.util.HashMap;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AggregatedClockModelTest {

    @Test
    void testGetters() {
        // GIVEN
        final TimeSpanMap<ClockModel> timeSpanMap = new TimeSpanMap<>(null);
        final ConstantClockModel perfectClockModel = new ConstantClockModel(1.);
        timeSpanMap.addValidAfter(perfectClockModel, AbsoluteDate.PAST_INFINITY, false);
        // WHEN
        final AggregatedClockModel clock = new AggregatedClockModel(timeSpanMap);
        // THEN
        assertEquals(perfectClockModel.getParametersDrivers().getFirst(), clock.getParametersDrivers().getFirst());
        assertEquals(clock.getModels(), timeSpanMap);
        assertEquals(AbsoluteDate.FUTURE_INFINITY, clock.getValidityEnd());
        assertEquals(AbsoluteDate.PAST_INFINITY, clock.getValidityStart());
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        assertEquals(perfectClockModel.getOffset(date).getBias(), clock.getOffset(date).getBias());
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        assertEquals(Binary64.ONE, clock.getFieldOffset(fieldDate).getBias());
        assertInstanceOf(ConstantFieldClockModel.class, clock.getFieldModel(0, new HashMap<>(), date));
    }
}