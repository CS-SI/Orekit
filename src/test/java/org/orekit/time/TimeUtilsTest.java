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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.orekit.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeUtilsTest {

    @Test
    public void testEarliest() {
        final AbsoluteDate t1 = new AbsoluteDate(2003, 4, 5, 16, 34, 22.5, TimeScalesFactory.getUTC());
        final AbsoluteDate t2 = t1.shiftedBy(TimeOffset.ATTOSECOND);
        Assertions.assertSame(t1, TimeUtils.earliest(t1, t2));
        Assertions.assertSame(t1, TimeUtils.earliest(t2, t1));
    }

    @Test
    public void testLatest() {
        final AbsoluteDate t1 = new AbsoluteDate(2003, 4, 5, 16, 34, 22.5, TimeScalesFactory.getUTC());
        final AbsoluteDate t2 = t1.shiftedBy(TimeOffset.ATTOSECOND);
        Assertions.assertSame(t2, TimeUtils.latest(t1, t2));
        Assertions.assertSame(t2, TimeUtils.latest(t2, t1));
    }

    @Test
    public void testFieldEarliest() {
        doTestEarliest(Binary64Field.getInstance());
    }

    @Test
    public void testFieldLatest() {
        doTestLatest(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEarliest(final Field<T> field) {
        final FieldAbsoluteDate<T> t1 = new FieldAbsoluteDate<>(field,
                                                                new AbsoluteDate(2003, 4, 5, 16, 34, 22.5,
                                                                                 TimeScalesFactory.getUTC()));
        final FieldAbsoluteDate<T> t2 = t1.shiftedBy(TimeOffset.ATTOSECOND);
        Assertions.assertSame(t1, TimeUtils.earliest(t1, t2));
        Assertions.assertSame(t1, TimeUtils.earliest(t2, t1));
    }

    private <T extends CalculusFieldElement<T>> void doTestLatest(final Field<T> field) {
        final FieldAbsoluteDate<T> t1 = new FieldAbsoluteDate<>(field,
                                                                new AbsoluteDate(2003, 4, 5, 16, 34, 22.5,
                                                                                 TimeScalesFactory.getUTC()));
        final FieldAbsoluteDate<T> t2 = t1.shiftedBy(TimeOffset.ATTOSECOND);
        Assertions.assertSame(t2, TimeUtils.latest(t1, t2));
        Assertions.assertSame(t2, TimeUtils.latest(t2, t1));
    }

    @ParameterizedTest(name = "input={0}s → expected=\"{1}\"")
    @CsvSource({
            // Zero
            "0.0,                                      '0.0 s'",

            // Seconds only
            "30.0,                                     '30.0 s'",
            "59.0,                                     '59.0 s'",

            // Minutes (+ seconds)
            "60.0,                                     '1 min 0.0 s'",
            "90.0,                                     '1 min 30.0 s'",
            "3599.0,                                   '59 min 59.0 s'",

            // Hours (+ minutes + seconds)
            "3600.0,                                   '1 h 0.0 s'",
            "3661.0,                                   '1 h 1 min 1.0 s'",
            "86399.0,                                  '23 h 59 min 59.0 s'",

            // Days
            "86400.0,                                  '1 d 0.0 s'",
            "90061.0,                                  '1 d 1 h 1 min 1.0 s'",
            "172800.0,                                 '2 d 0.0 s'",
            "31536000.0,                               '365 d 0.0 s'",

            // Negative values
            "-30.0,                                    '-30.0 s'",
            "-90.0,                                    '-1 min 30.0 s'",
            "-90061.0,                                 '-1 d 1 h 1 min 1.0 s'"
    })
    public void secondsToDHMSTest(double input, String expected) {
        assertEquals(expected, TimeUtils.secondsToDHMS(input));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
