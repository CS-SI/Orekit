/* Copyright 2022-2026 Thales Alenia Space
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

public class FieldClockOffsetTest {

    @Test
    public void testGetters() {
        doTestGetters(Binary64Field.getInstance());
    }

    @Test
    public void testAdd() {
        doTestAdd(Binary64Field.getInstance());
    }

    @Test
    public void testSubtract() {
        doTestSubtract(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetters(final Field<T> field) {
        final FieldClockOffset<T> clockOffset = new FieldClockOffset<> (FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                        field.getZero().newInstance(1.0),
                                                                        field.getZero().newInstance(-2.0),
                                                                        field.getZero().newInstance(3.0));
        Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, clockOffset.getDate().toAbsoluteDate());
        Assertions.assertEquals( 1.0, clockOffset.getOffset().getReal(), 1.0e-15);
        Assertions.assertEquals(-2.0, clockOffset.getRate().getReal(), 1.0e-15);
        Assertions.assertEquals( 3.0, clockOffset.getAcceleration().getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestAdd(final Field<T> field) {
        final FieldClockOffset<T> clockOffset1 = new FieldClockOffset<> (FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                         field.getZero().newInstance(1.0),
                                                                         field.getZero().newInstance(-2.0),
                                                                         field.getZero().newInstance(3.0));
        final FieldClockOffset<T> clockOffset2 = new FieldClockOffset<> (FieldAbsoluteDate.getJulianEpoch(field),
                                                                         field.getZero().newInstance(3.0),
                                                                         field.getZero().newInstance(17.0),
                                                                         field.getZero().newInstance(12.0));
        final FieldClockOffset<T> sum          = clockOffset1.add(clockOffset2);
        Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, sum.getDate().toAbsoluteDate());
        Assertions.assertEquals( 4.0, sum.getOffset().getReal(), 1.0e-15);
        Assertions.assertEquals(15.0, sum.getRate().getReal(), 1.0e-15);
        Assertions.assertEquals(15.0, sum.getAcceleration().getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestSubtract(final Field<T> field) {
        final FieldClockOffset<T> clockOffset1 = new FieldClockOffset<> (FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                         field.getZero().newInstance(1.0),
                                                                         field.getZero().newInstance(-2.0),
                                                                         field.getZero().newInstance(3.0));
        final FieldClockOffset<T> clockOffset2 = new FieldClockOffset<> (FieldAbsoluteDate.getJulianEpoch(field),
                                                                         field.getZero().newInstance(3.0),
                                                                         field.getZero().newInstance(17.0),
                                                                         field.getZero().newInstance(12.0));
        final FieldClockOffset<T> difference   = clockOffset1.subtract(clockOffset2);
        Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, difference.getDate().toAbsoluteDate());
        Assertions.assertEquals( -2.0, difference.getOffset().getReal(), 1.0e-15);
        Assertions.assertEquals(-19.0, difference.getRate().getReal(), 1.0e-15);
        Assertions.assertEquals( -9.0, difference.getAcceleration().getReal(), 1.0e-15);
    }

}
