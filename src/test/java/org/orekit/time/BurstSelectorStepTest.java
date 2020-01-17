/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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



import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;

public class BurstSelectorStepTest {

    @Test
    public void testNoAlign() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(71);
        final List<AbsoluteDate> list = selector.selectDates(t0, t1);
        Assert.assertEquals(5, list.size());
        Assert.assertEquals( 27.0, list.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 37.0, list.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 47.0, list.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 87.0, list.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 97.0, list.get(4).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCBeforeForward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(2, 10.0, 30.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(62);
        final List<AbsoluteDate> list = selector.selectDates(t0, t1);
        Assert.assertEquals(4, list.size());
        Assert.assertEquals( 30.0, list.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 40.0, list.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 60.0, list.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 70.0, list.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCBeforeBackward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(2, 10.0, 30.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(62);
        final List<AbsoluteDate> list = selector.selectDates(t1, t0);
        Assert.assertEquals(4, list.size());
        Assert.assertEquals( 70.0, list.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 60.0, list.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 40.0, list.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 30.0, list.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCAfterForward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(2);
        final AbsoluteDate t2 = t1.shiftedBy(98);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assert.assertEquals(0, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t2);
        Assert.assertEquals(4, list2.size());
        Assert.assertEquals( 60.0, list2.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 70.0, list2.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 80.0, list2.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(120.0, list2.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testAlignUTCAfterBackward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(2);
        final AbsoluteDate t2 = t1.shiftedBy(98);
        final List<AbsoluteDate> list1 = selector.selectDates(t2, t1);
        Assert.assertEquals(4, list1.size());
        Assert.assertEquals(120.0, list1.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 80.0, list1.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 70.0, list1.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 60.0, list1.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t0);
        Assert.assertEquals(0, list2.size());
    }

    @Test
    public void testInterruptedStream() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(71);
        final AbsoluteDate t2 = t1.shiftedBy(30);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assert.assertEquals(5, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t2);
        Assert.assertEquals(1, list2.size());
        Assert.assertEquals(10.0, list2.get(0).durationFrom(list1.get(list1.size() - 1)), 1.0e-15);
    }

    @Test
    public void testMissedHighRateNoReset() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(71);
        final AbsoluteDate t2 = t1.shiftedBy(30);
        final AbsoluteDate t3 = t2.shiftedBy(19);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assert.assertEquals(5, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t2, t3);
        Assert.assertEquals(1, list2.size());
        Assert.assertEquals(50.0, list2.get(0).durationFrom(list1.get(list1.size() - 1)), 1.0e-15);
    }

    @Test
    public void testResetStream() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(71);
        final AbsoluteDate t2 = t1.shiftedBy(50);
        final AbsoluteDate t3 = t2.shiftedBy(15);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assert.assertEquals(5, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t2, t3);
        Assert.assertEquals(2, list2.size());
        Assert.assertEquals( 0.0, list2.get(0).durationFrom(t2), 1.0e-15);
        Assert.assertEquals(10.0, list2.get(1).durationFrom(t2), 1.0e-15);
    }

    @Test
    public void testShortInterval() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, null);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:27.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(71);
        final AbsoluteDate t2 = t1.shiftedBy(8);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assert.assertEquals(5, list1.size());
        final List<AbsoluteDate> list2 = selector.selectDates(t1, t2);
        Assert.assertEquals(0, list2.size());
    }

    @Test
    public void testStartMidBurstForward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:17.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(118);
        final List<AbsoluteDate> list1 = selector.selectDates(t0, t1);
        Assert.assertEquals(6, list1.size());
        Assert.assertEquals( 20.0, list1.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 60.0, list1.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 70.0, list1.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 80.0, list1.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(120.0, list1.get(4).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(130.0, list1.get(5).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Test
    public void testStartMidBurstBackward() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final DatesSelector selector = new BurstSelector(3, 10.0, 60.0, utc);
        final AbsoluteDate t0 = new AbsoluteDate("2003-02-25T00:00:17.0", utc);
        final AbsoluteDate t1 = t0.shiftedBy(118);
        final List<AbsoluteDate> list1 = selector.selectDates(t1, t0);
        Assert.assertEquals(6, list1.size());
        Assert.assertEquals(130.0, list1.get(0).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(120.0, list1.get(1).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 80.0, list1.get(2).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 70.0, list1.get(3).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 60.0, list1.get(4).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals( 20.0, list1.get(5).getComponents(utc).getTime().getSecondsInLocalDay(), 1.0e-15);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
