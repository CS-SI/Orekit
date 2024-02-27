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
package org.orekit.gnss;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;


public class QuadraticClockModelTest {

    @Test
    public void testValue() {
        Assertions.assertEquals(1.00 / 256.0, clock.getOffset(t0),                1.0e-15);
        Assertions.assertEquals(1.75 / 256.0, clock.getOffset(t0.shiftedBy(1.0)), 1.0e-15);
        Assertions.assertEquals(3.00 / 256.0, clock.getOffset(t0.shiftedBy(2.0)), 1.0e-15);
    }

    @Test
    public void testRate() {
        Assertions.assertEquals(1.00 / 512, clock.getRate(t0),                   1.0e-15);
        Assertions.assertEquals(2.00 / 512, clock.getRate(t0.shiftedBy(1.0)),    1.0e-15);
        Assertions.assertEquals(3.00 / 512, clock.getRate(t0.shiftedBy(2.0)),    1.0e-15);
    }

    @Test
    public void testGetters() {
        Assertions.assertEquals(0.0, clock.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
        Assertions.assertEquals(1.00 /  256, clock.getA0(), 1.0e-15);
        Assertions.assertEquals(1.00 /  512, clock.getA1(), 1.0e-15);
        Assertions.assertEquals(1.00 / 1024, clock.getA2(), 1.0e-15);
    }

    @BeforeEach
    public void setUp() {
        t0    = AbsoluteDate.GALILEO_EPOCH;
        clock = new QuadraticClockModel(t0,
                                        FastMath.scalb(1.0, -8),
                                        FastMath.scalb(1.0, -9),
                                        FastMath.scalb(1.0, -10));
    }

    @AfterEach
    public void tearDown() {
        t0    = null;
        clock = null;
    }

    private AbsoluteDate        t0;
    private QuadraticClockModel clock;

}
