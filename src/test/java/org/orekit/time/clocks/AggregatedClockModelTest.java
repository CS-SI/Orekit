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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.utils.TimeSpanMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AggregatedClockModelTest {

    @Test
    void testGetters() {
        // GIVEN
        final TimeSpanMap<ClockModel> timeSpanMap = new TimeSpanMap<>(null);
        final ConstantClockModel c1 = new ConstantClockModel(1.);
        timeSpanMap.addValidAfter(c1, AbsoluteDate.PAST_INFINITY, false);
        final ConstantClockModel c2 = new ConstantClockModel(2.);
        timeSpanMap.addValidAfter(c2, AbsoluteDate.J2000_EPOCH, false);
        // WHEN
        final AggregatedClockModel clock = new AggregatedClockModel(timeSpanMap);
        final AggregatedFieldClockModel<Binary64>clock64 = clock.toField(Binary64::new);
        // THEN
        assertEquals(c1.getParametersDrivers().getFirst(), clock.getParametersDrivers().getFirst());
        assertNotEquals(c1.getParametersDrivers().getFirst(), clock.getParametersDrivers().getLast());
        assertEquals(clock.getModels(), timeSpanMap);
        assertEquals(AbsoluteDate.FUTURE_INFINITY, clock.getValidityEnd());
        assertEquals(AbsoluteDate.PAST_INFINITY, clock.getValidityStart());
        final AbsoluteDate before = AbsoluteDate.J2000_EPOCH.shiftedBy(TimeOffset.ATTOSECOND.negate());
        assertEquals(c1.getOffset(before).getBias(), clock.getOffset(before).getBias());
        final AbsoluteDate after = AbsoluteDate.J2000_EPOCH.shiftedBy(TimeOffset.ATTOSECOND);
        assertEquals(c2.getOffset(after).getBias(), clock.getOffset(after).getBias());
        final FieldAbsoluteDate<Binary64> fieldBefore = new FieldAbsoluteDate<>(Binary64Field.getInstance(), before);
        assertEquals(Binary64.ONE,          clock64.getOffset(fieldBefore).getBias());
        final FieldAbsoluteDate<Binary64> fieldAfter = new FieldAbsoluteDate<>(Binary64Field.getInstance(), after);
        assertEquals(new Binary64(2), clock64.getOffset(fieldAfter).getBias());
        assertInstanceOf(AggregatedFieldClockModel.class, clock.toGradient(0, new HashMap<>()));
    }

    @Test
    void testRoundtrip() {
        final AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        final TimeSpanMap<ClockModel> timeSpanMap = new TimeSpanMap<>(null);
        timeSpanMap.addValidAfter(new PolynomialClockModel(date, "before",  2.5,  0.125,  0.006),
                                  date, false);
        timeSpanMap.addValidAfter(new PolynomialClockModel(date, "before", -2.5, -0.125, -0.006),
                                  date.shiftedBy(10), false);
        final AggregatedClockModel                clock   = new AggregatedClockModel(timeSpanMap);
        final AggregatedFieldClockModel<Binary64> c64     = clock.toField(Binary64::new);
        final AggregatedClockModel                rebuilt = c64.toNonField();

        Assertions.assertEquals(rebuilt.getModels().getSpansNumber(), c64.getModels().getSpansNumber());
        Assertions.assertEquals(clock.getValidityStart(), c64.getValidityStart());
        Assertions.assertEquals(clock.getValidityEnd(),   c64.getValidityEnd());
        Assertions.assertEquals(clock.getValidityStart(), rebuilt.getValidityStart());
        Assertions.assertEquals(clock.getValidityEnd(),   rebuilt.getValidityEnd());

        for (double dt = -10.5; dt < 10.5; dt+= 1) {
            Assertions.assertEquals(clock.getOffset(date).getBias(),         rebuilt.getOffset(date).getBias(),         1.0e-15);
            Assertions.assertEquals(clock.getOffset(date).getRate(),         rebuilt.getOffset(date).getRate(),         1.0e-15);
            Assertions.assertEquals(clock.getOffset(date).getAcceleration(), rebuilt.getOffset(date).getAcceleration(), 1.0e-15);
        }

    }

    @Test
    void testEmptyConstructor() {
        try {
            new AggregatedClockModel(new TimeSpanMap<>(null));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, oe.getSpecifier());
            Assertions.assertEquals(OrekitMessages.NO_CACHED_ENTRIES, ((OrekitException) oe.getCause()).getSpecifier());
        }
        try {
            new AggregatedFieldClockModel<>(new TimeSpanMap<>(null));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, oe.getSpecifier());
            Assertions.assertEquals(OrekitMessages.NO_CACHED_ENTRIES, ((OrekitException) oe.getCause()).getSpecifier());
        }
    }

    @Test
    void testOutOfRange() {

        final AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        final TimeSpanMap<ClockModel> timeSpanMap = new TimeSpanMap<>(null);
        timeSpanMap.addValidBetween(new PerfectClockModel(),
                                    date.shiftedBy(TimeOffset.HOUR.negate()),
                                    date.shiftedBy(TimeOffset.HOUR));
        final AggregatedClockModel clock = new AggregatedClockModel(timeSpanMap);
        Assertions.assertEquals(0.0, clock.getOffsetValue(date), 1.0e-15);

        try {
            clock.getOffsetValue(date.shiftedBy(TimeOffset.DAY));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_DATA_GENERATED, oe.getSpecifier());
            Assertions.assertEquals(86400, ((AbsoluteDate) oe.getParts()[0]).durationFrom(date), 1.0e-10);
        }

        final AggregatedFieldClockModel<Binary64> clock64 = clock.toField(Binary64::new);
        final FieldAbsoluteDate<Binary64> date64 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        try {

            clock64.getOffsetValue(date64.shiftedBy(TimeOffset.DAY));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_DATA_GENERATED, oe.getSpecifier());
            Assertions.assertEquals(86400,
                                    ((FieldAbsoluteDate<?>) oe.getParts()[0]).durationFrom(date).getReal(),
                                    1.0e-10);
        }

    }

}
