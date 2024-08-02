/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.estimation.measurements;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;


class QuadraticFieldClockModelTest {

    @Test
    void testValueField() {
        doTestValueField(Binary64Field.getInstance());
    }

    @Test
    void testRateField() {
        doTestRateField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestValueField(final Field<T> field) {
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, AbsoluteDate.GALILEO_EPOCH);
        final QuadraticFieldClockModel<T> clock =
                        new QuadraticFieldClockModel<>(t0,
                                                       field.getZero().newInstance(FastMath.scalb(1.0,  -8)),
                                                       field.getZero().newInstance(FastMath.scalb(1.0,  -9)),
                                                       field.getZero().newInstance(FastMath.scalb(1.0, -10)));
        assertEquals(1.00 / 256.0, clock.getOffset(t0).getOffset().getReal(),                1.0e-15);
        assertEquals(1.75 / 256.0, clock.getOffset(t0.shiftedBy(1.0)).getOffset().getReal(), 1.0e-15);
        assertEquals(3.00 / 256.0, clock.getOffset(t0.shiftedBy(2.0)).getOffset().getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestRateField(final Field<T> field) {
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, AbsoluteDate.GALILEO_EPOCH);
        final QuadraticFieldClockModel<T> clock =
                        new QuadraticFieldClockModel<>(t0,
                                                       field.getZero().newInstance(FastMath.scalb(1.0,  -8)),
                                                       field.getZero().newInstance(FastMath.scalb(1.0,  -9)),
                                                       field.getZero().newInstance(FastMath.scalb(1.0, -10)));
        assertEquals(1.00 / 512, clock.getOffset(t0).getRate().getReal(),                1.0e-15);
        assertEquals(2.00 / 512, clock.getOffset(t0.shiftedBy(1.0)).getRate().getReal(), 1.0e-15);
        assertEquals(3.00 / 512, clock.getOffset(t0.shiftedBy(2.0)).getRate().getReal(), 1.0e-15);
    }

}
