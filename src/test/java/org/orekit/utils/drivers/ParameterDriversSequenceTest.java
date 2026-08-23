/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;

import java.util.List;

class ParameterDriversSequenceTest {

    @Test
    void testSingleCoefficient() {
        final ParameterDriversSequence sequence =
            new ParameterDriversSequenceBuilder("base", FastMath.scalb(1.0, -6), -12.0, 17.0).
            addReferenceValue(3.5).
            build();

        final List<ParameterDriver> drivers = sequence.getParametersDrivers();
        Assertions.assertEquals(1,                       drivers.size());
        checkDriver(drivers.getFirst(), 3.5, -12.0, 17.0, FastMath.scalb(1.0, -6), "base");

        Assertions.assertSame(drivers.getFirst(), sequence.getActiveDriver(AbsoluteDate.PAST_INFINITY));
        Assertions.assertSame(drivers.getFirst(), sequence.getActiveDriver(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertSame(drivers.getFirst(), sequence.getActiveDriver(AbsoluteDate.FUTURE_INFINITY));

        Assertions.assertEquals(0, sequence.getActiveDriverIndex(AbsoluteDate.PAST_INFINITY));
        Assertions.assertEquals(0, sequence.getActiveDriverIndex(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(0, sequence.getActiveDriverIndex(AbsoluteDate.FUTURE_INFINITY));

    }

    @Test
    void testMultipleCoefficient() {

        final AbsoluteDate t0 = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate t1 = t0.shiftedBy(TimeOffset.DAY);
        final AbsoluteDate t2 = t1.shiftedBy(TimeOffset.DAY);
        final AbsoluteDate t3 = t2.shiftedBy(TimeOffset.DAY);
        final AbsoluteDate t4 = t3.shiftedBy(TimeOffset.DAY);
        final ParameterDriversSequence sequence =
            new ParameterDriversSequenceBuilder("base", FastMath.scalb(1.0, -6), -1.0, 1.0).
            addReferenceValue(2.5, t1, t2).
            addReferenceValue(4.5, t3, t4). // intentionally added in non-chronological order for test
            addReferenceValue(3.5, t2, t3).
            build();

        final List<ParameterDriver> drivers = sequence.getParametersDrivers();
        Assertions.assertEquals(3, drivers.size());
        checkDriver(drivers.get(0), 2.5, -1.0, 1.0, FastMath.scalb(1.0, -6), "span-base-0");
        checkDriver(drivers.get(1), 3.5, -1.0, 1.0, FastMath.scalb(1.0, -6), "span-base-1");
        checkDriver(drivers.get(2), 4.5, -1.0, 1.0, FastMath.scalb(1.0, -6), "span-base-2");

        Assertions.assertNull(sequence.getActiveDriver(AbsoluteDate.PAST_INFINITY));
        Assertions.assertNull(sequence.getActiveDriver(t0));
        Assertions.assertNull(sequence.getActiveDriver(t1.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        Assertions.assertSame(drivers.getFirst(), sequence.getActiveDriver(t1.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertSame(drivers.getFirst(), sequence.getActiveDriver(t2.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        Assertions.assertSame(drivers.get(1),     sequence.getActiveDriver(t2.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertSame(drivers.get(1),     sequence.getActiveDriver(t3.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        Assertions.assertSame(drivers.get(2),     sequence.getActiveDriver(t3.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertSame(drivers.get(2),     sequence.getActiveDriver(t4.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        Assertions.assertNull(sequence.getActiveDriver(t4.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertNull(sequence.getActiveDriver(AbsoluteDate.FUTURE_INFINITY));

        checkOutOfRange(sequence, AbsoluteDate.PAST_INFINITY);
        checkOutOfRange(sequence, t0);
        checkOutOfRange(sequence, t1.shiftedBy(TimeOffset.ATTOSECOND.negate()));
        Assertions.assertEquals( 0, sequence.getActiveDriverIndex(t1.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertEquals( 0, sequence.getActiveDriverIndex(t2.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        Assertions.assertEquals( 1, sequence.getActiveDriverIndex(t2.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertEquals( 1, sequence.getActiveDriverIndex(t3.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        Assertions.assertEquals( 2, sequence.getActiveDriverIndex(t3.shiftedBy(TimeOffset.ATTOSECOND)));
        Assertions.assertEquals( 2, sequence.getActiveDriverIndex(t4.shiftedBy(TimeOffset.ATTOSECOND.negate())));
        checkOutOfRange(sequence, t4.shiftedBy(TimeOffset.ATTOSECOND));
        checkOutOfRange(sequence, AbsoluteDate.FUTURE_INFINITY);

    }

    private void checkDriver(final ParameterDriver driver,
                             final double value, final double min, final double max,
                             final double scale, final String name) {
        Assertions.assertEquals(value, driver.getValue(),    1.0e-15);
        Assertions.assertEquals(min,   driver.getMinValue(), 1.0e-15);
        Assertions.assertEquals(max,   driver.getMaxValue(), 1.0e-15);
        Assertions.assertEquals(scale, driver.getScale(),    1.0e-17);
        Assertions.assertEquals(name,  driver.getName());
    }

    private void checkOutOfRange(final ParameterDriversSequence sequence,
                                 final AbsoluteDate date) {
        try {
            sequence.getActiveDriverIndex(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DATE, oe.getSpecifier());
            Assertions.assertEquals(date, oe.getParts()[0]);
        }

    }

    @Test
    void testNoCoefficients() {
        try {
            new ParameterDriversSequenceBuilder("base", FastMath.scalb(1.0, -6), -12.0, 17.0).build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_REFERENCE_VALUES_SET, oe.getSpecifier());
        }
    }

    @Test
    void testNonContinuous() {

        try {

            final AbsoluteDate date0 = AbsoluteDate.ARBITRARY_EPOCH;
            final AbsoluteDate date1 = date0.shiftedBy(new TimeOffset(3, TimeOffset.HOUR));
            final AbsoluteDate date2 = date0.shiftedBy(new TimeOffset(6, TimeOffset.HOUR));
            final AbsoluteDate date3 = date0.shiftedBy(new TimeOffset(9, TimeOffset.HOUR));

            new ParameterDriversSequenceBuilder("base", FastMath.scalb(1.0, -6), -12.0, 17.0).
                addReferenceValue(1.0, date0, date1).
                addReferenceValue(2.0, date2, date3).
                build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.MISSING_REFERENCE_VALUE, oe.getSpecifier());
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
