/* Copyright 2002-2023 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;

import java.util.List;

public class FixedStepSelectorTest {

    @Test
    public void testNoAlign() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(10.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(91);
        final List<AbsoluteDate> list = selector.selectDates(t0, t1);
        Assertions.assertEquals(10, list.size());
        Assertions.assertEquals( 27.0, list.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 37.0, list.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 47.0, list.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 57.0, list.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 67.0, list.get(4).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 77.0, list.get(5).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 87.0, list.get(6).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 97.0, list.get(7).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(107.0, list.get(8).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(117.0, list.get(9).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCBeforeForward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(5.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(17);
        final List<AbsoluteDate> list = selector.selectDates(t0, t1);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals( 30.0, list.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 35.0, list.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 40.0, list.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCBeforeBackward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(5.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(17);
        final List<AbsoluteDate> list = selector.selectDates(t1, t0);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals( 40.0, list.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 35.0, list.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 30.0, list.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCAfterForward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(10.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(2);
        final AbsoluteDate t2 = t1.shiftedBy(89);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assertions.assertEquals(0, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t2);
        Assertions.assertEquals(9, list2.size());
        Assertions.assertEquals( 30.0, list2.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 40.0, list2.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 50.0, list2.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 60.0, list2.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 70.0, list2.get(4).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 80.0, list2.get(5).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 90.0, list2.get(6).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(100.0, list2.get(7).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(110.0, list2.get(8).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCAfterBackward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(10.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(2);
        final AbsoluteDate t2 = t1.shiftedBy(89);
        final List<AbsoluteDate> list1 = selector.selectDates(t2, t1);
        Assertions.assertEquals(9, list1.size());
        Assertions.assertEquals(110.0, list1.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(100.0, list1.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 90.0, list1.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 80.0, list1.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 70.0, list1.get(4).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 60.0, list1.get(5).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 50.0, list1.get(6).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 40.0, list1.get(7).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals( 30.0, list1.get(8).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t0);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testInterruptedStream() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(10.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(91);
        final AbsoluteDate t2 = t1.shiftedBy(30);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assertions.assertEquals(10, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t2);
        Assertions.assertEquals(3, list2.size());
        Assertions.assertEquals(10.0, list2.get(0).durationFrom(list1.get(list1.size() - 1)), 1.0e-15);
    }

    @Test
    public void testResetStream() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(10.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(91);
        final AbsoluteDate t2 = t1.shiftedBy(30);
        final AbsoluteDate t3 = t2.shiftedBy(15);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assertions.assertEquals(10, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t2, t3);
        Assertions.assertEquals(2, list2.size());
        Assertions.assertEquals( 0.0, list2.get(0).durationFrom(t2), 1.0e-15);
        Assertions.assertEquals(10.0, list2.get(1).durationFrom(t2), 1.0e-15);
    }

    @Test
    public void testShortInterval() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new FixedStepSelector(10.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(91);
        final AbsoluteDate t2 = t1.shiftedBy(8);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assertions.assertEquals(10, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t2);
        Assertions.assertEquals(0, list2.size());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
