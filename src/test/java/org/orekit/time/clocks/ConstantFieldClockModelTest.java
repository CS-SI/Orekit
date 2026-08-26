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

import org.hipparchus.analysis.function.Abs;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.time.TimeScalesFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantFieldClockModelTest {

    @Test
    void testGetOffset() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> t0 = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final ConstantFieldClockModel<Binary64> constantFieldClockModel =
            new ConstantFieldClockModel<>(Binary64.ONE);
        // WHEN
        final FieldClockOffset<Binary64> offset = constantFieldClockModel.getOffset(t0);
        // THEN
        assertEquals(1., offset.getBias().getReal(),                           1.0e-15);
        assertEquals(0., offset.getRate().getReal(),                           1.0e-15);
        assertEquals(0., offset.getAcceleration().getReal(),                   1.0e-15);
        assertEquals(1., constantFieldClockModel.getOffsetValue(t0).getReal(), 1.0e-15);
    }

    @Test
    void testValidity() {

        final AbsoluteDate t0      = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final ConstantClockModel clockModel = new ConstantClockModel(1.0);
        clockModel.getParametersDrivers().getFirst().setValidity(TimeInterval.of(t0, 10.0));
        final ConstantFieldClockModel<Binary64> fieldClockModel = clockModel.toField(Binary64::new);

        // beware, the result of this test will change when field parameter drivers are available!
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY,   fieldClockModel.getValidityStart());
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, fieldClockModel.getValidityEnd());

    }

}
