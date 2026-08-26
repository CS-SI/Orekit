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
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldClockOffsetTest {

    @Test
    void testConstructorException() {
        Assertions.assertThrows(OrekitException.class, () ->
                new FieldClockOffset<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()), new Binary64[4]));
    }

    @Test
    void testConstructorWithoutBias() {
        final FieldClockOffset<Binary64> fieldClockOffset = new FieldClockOffset<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()),
                new Binary64[]{});
        assertEquals( 0.0, fieldClockOffset.getBias().getReal(),         1.0e-15);
        assertEquals( 0.0, fieldClockOffset.getRate().getReal(),         1.0e-15);
        assertEquals( 0.0, fieldClockOffset.getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testConstructorWithoutDrift() {
        final FieldClockOffset<Binary64> fieldClockOffset = new FieldClockOffset<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()),
                new Binary64[]{new Binary64(3)});
        assertEquals( 3.0, fieldClockOffset.getBias().getReal(),         1.0e-15);
        assertEquals( 0.0, fieldClockOffset.getRate().getReal(),         1.0e-15);
        assertEquals( 0.0, fieldClockOffset.getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testConstructorWithoutAcceleration() {
        final FieldClockOffset<Binary64> fieldClockOffset = new FieldClockOffset<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()),
                new Binary64[]{new Binary64(3), new Binary64(5)});
        assertEquals( 3.0, fieldClockOffset.getBias().getReal(),         1.0e-15);
        assertEquals( 5.0, fieldClockOffset.getRate().getReal(),         1.0e-15);
        assertEquals( 0.0, fieldClockOffset.getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testConstructor() {
        // GIVEN
        final FieldClockOffset<Binary64> fieldClockOffset = new FieldClockOffset<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()),
                new Binary64[]{new Binary64(3), new Binary64(2), Binary64.ONE});
        // WHEN
        final FieldClockOffset<Binary64> copy = new FieldClockOffset<>(fieldClockOffset.getDate(), fieldClockOffset.getBias(),
                fieldClockOffset.getRate(), fieldClockOffset.getAcceleration());
        // THEN
        assertEquals(copy.getBias(), fieldClockOffset.getBias());
        assertEquals(copy.getRate(), fieldClockOffset.getRate());
        assertEquals(copy.getAcceleration(), fieldClockOffset.getAcceleration());
    }

    @Test
    void testNegate() {
        // GIVEN
        final FieldClockOffset<Binary64> fieldClockOffset = new FieldClockOffset<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()),
                new Binary64(3), new Binary64(2), Binary64.ONE);
        // WHEN
        final FieldClockOffset<Binary64> negated = fieldClockOffset.negate();
        // THEN
        assertEquals(fieldClockOffset.getBias().negate(), negated.getBias());
        assertEquals(fieldClockOffset.getRate().negate(), negated.getRate());
        assertEquals(fieldClockOffset.getAcceleration().negate(), negated.getAcceleration());
    }

    @Test
    void testGetters() {
        doTestGetters(Binary64Field.getInstance());
    }

    @Test
    void testAdd() {
        doTestAdd(Binary64Field.getInstance());
    }

    @Test
    void testSubtract() {
        doTestSubtract(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetters(final Field<T> field) {
        final FieldClockOffset<T> clockOffset = new FieldClockOffset<> (FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                        field.getZero().newInstance(1.0),
                                                                        field.getZero().newInstance(-2.0),
                                                                        field.getZero().newInstance(3.0));
        assertEquals(AbsoluteDate.ARBITRARY_EPOCH, clockOffset.getDate().toAbsoluteDate());
        assertEquals( 1.0, clockOffset.getBias().getReal(),         1.0e-15);
        assertEquals(-2.0, clockOffset.getRate().getReal(),         1.0e-15);
        assertEquals( 3.0, clockOffset.getAcceleration().getReal(), 1.0e-15);
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
        assertEquals(AbsoluteDate.ARBITRARY_EPOCH, sum.getDate().toAbsoluteDate());
        assertEquals( 4.0, sum.getBias().getReal(),         1.0e-15);
        assertEquals(15.0, sum.getRate().getReal(),         1.0e-15);
        assertEquals(15.0, sum.getAcceleration().getReal(), 1.0e-15);
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
        assertEquals(AbsoluteDate.ARBITRARY_EPOCH, difference.getDate().toAbsoluteDate());
        assertEquals( -2.0, difference.getBias().getReal(),         1.0e-15);
        assertEquals(-19.0, difference.getRate().getReal(),         1.0e-15);
        assertEquals( -9.0, difference.getAcceleration().getReal(), 1.0e-15);
    }

}
