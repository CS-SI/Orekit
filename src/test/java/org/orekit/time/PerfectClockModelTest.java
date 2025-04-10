/* Copyright 2022-2025 Thales Alenia Space
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;

public class PerfectClockModelTest {

    @Test
    public void testZero() {
        final AbsoluteDate       t0      = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final PerfectClockModel clockModel = new PerfectClockModel();
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final ClockOffset co = clockModel.getOffset(t0.shiftedBy(dt));
            Assertions.assertEquals(dt, co.getDate().durationFrom(t0), 1.0e-15);
            Assertions.assertEquals(0,  co.getOffset(),                1.0e-15);
            Assertions.assertEquals(0,  co.getRate(),                  1.0e-15);
            Assertions.assertEquals(0,  co.getAcceleration(),          1.0e-15);
        }

    }

    @Test
    public void testZeroField() {
        doTestZero(Binary64Field.getInstance());
    }

    public <T extends CalculusFieldElement<T>> void doTestZero(final Field<T> field) {
        final AbsoluteDate         t0  = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<T> t0F = new FieldAbsoluteDate<>(field, t0);
        final PerfectClockModel clockModel = new PerfectClockModel();
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final T dtF = field.getZero().newInstance(dt);
            final FieldClockOffset<T> co = clockModel.getOffset(t0F.shiftedBy(dtF));
            Assertions.assertEquals(dt, co.getDate().durationFrom(t0).getReal(), 1.0e-15);
            Assertions.assertEquals(0,  co.getOffset().getReal(),                1.0e-15);
            Assertions.assertEquals(0,  co.getRate().getReal(),                  1.0e-15);
            Assertions.assertEquals(0,  co.getAcceleration().getReal(),          1.0e-15);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
