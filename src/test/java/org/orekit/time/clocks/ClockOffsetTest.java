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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;

public class ClockOffsetTest {

    @Test
    public void testGetters() {
        final ClockOffset clockOffset = new ClockOffset(AbsoluteDate.ARBITRARY_EPOCH, 1.0, -2.0, 3.0);
        Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, clockOffset.getDate());
        Assertions.assertEquals( 1.0, clockOffset.getOffset(), 1.0e-15);
        Assertions.assertEquals(-2.0, clockOffset.getRate(), 1.0e-15);
        Assertions.assertEquals( 3.0, clockOffset.getAcceleration(), 1.0e-15);
    }

    @Test
    public void testAdd() {
        final ClockOffset clockOffset1 = new ClockOffset(AbsoluteDate.ARBITRARY_EPOCH, 1.0, -2.0,  3.0);
        final ClockOffset clockOffset2 = new ClockOffset(AbsoluteDate.JULIAN_EPOCH, 3.0, 17.0, 12.0);
        final ClockOffset sum          = clockOffset1.add(clockOffset2);
        Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, sum.getDate());
        Assertions.assertEquals( 4.0, sum.getOffset(), 1.0e-15);
        Assertions.assertEquals(15.0, sum.getRate(), 1.0e-15);
        Assertions.assertEquals(15.0, sum.getAcceleration(), 1.0e-15);
    }

    @Test
    public void testSubtract() {
        final ClockOffset clockOffset1 = new ClockOffset(AbsoluteDate.ARBITRARY_EPOCH, 1.0, -2.0,  3.0);
        final ClockOffset clockOffset2 = new ClockOffset(AbsoluteDate.JULIAN_EPOCH, 3.0, 17.0, 12.0);
        final ClockOffset difference   = clockOffset1.subtract(clockOffset2);
        Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, difference.getDate());
        Assertions.assertEquals( -2.0, difference.getOffset(), 1.0e-15);
        Assertions.assertEquals(-19.0, difference.getRate(), 1.0e-15);
        Assertions.assertEquals( -9.0, difference.getAcceleration(), 1.0e-15);
    }

}
