/* Copyright 2022-2025 Thales Alenia Space
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
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ClockOffsetHermiteInterpolatorTest {

    @Test
    void testNoRate() {
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final ClockOffsetHermiteInterpolator interpolator = new ClockOffsetHermiteInterpolator(4);
        ClockOffset interpolated =
            interpolator.interpolate(t0.shiftedBy(2.5),
                                     Arrays.asList(new ClockOffset(t0.shiftedBy(0),  0.0, Double.NaN, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(1),  1.0, Double.NaN, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(2),  4.0, Double.NaN, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(3),  9.0, Double.NaN, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(4), 16.0, Double.NaN, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(5), 25.0, Double.NaN, Double.NaN)));
        Assertions.assertEquals(6.25, interpolated.getOffset(),       1.0e-15);
        Assertions.assertEquals(5.00, interpolated.getRate(),         1.0e-15);
        Assertions.assertEquals(2.00, interpolated.getAcceleration(), 1.0e-15);
    }

    @Test
    void testNoAcceleration() {
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final ClockOffsetHermiteInterpolator interpolator = new ClockOffsetHermiteInterpolator(4);
        ClockOffset interpolated =
            interpolator.interpolate(t0.shiftedBy(2.5),
                                     Arrays.asList(new ClockOffset(t0.shiftedBy(0),  0.0,  0.0, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(1),  1.0,  2.0, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(2),  4.0,  4.0, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(3),  9.0,  6.0, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(4), 16.0,  8.0, Double.NaN),
                                                   new ClockOffset(t0.shiftedBy(5), 25.0, 10.0, Double.NaN)));
        Assertions.assertEquals(6.25, interpolated.getOffset(),       1.0e-15);
        Assertions.assertEquals(5.00, interpolated.getRate(),         1.0e-15);
        Assertions.assertEquals(2.00, interpolated.getAcceleration(), 1.0e-15);
    }

    @Test
    void testComplete() {
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final ClockOffsetHermiteInterpolator interpolator = new ClockOffsetHermiteInterpolator(4);
        ClockOffset interpolated =
            interpolator.interpolate(t0.shiftedBy(2.5),
                                     Arrays.asList(new ClockOffset(t0.shiftedBy(0),  0.0,  0.0, 2.0),
                                                   new ClockOffset(t0.shiftedBy(1),  1.0,  2.0, 2.0),
                                                   new ClockOffset(t0.shiftedBy(2),  4.0,  4.0, 2.0),
                                                   new ClockOffset(t0.shiftedBy(3),  9.0,  6.0, 2.0),
                                                   new ClockOffset(t0.shiftedBy(4), 16.0,  8.0, 2.0),
                                                   new ClockOffset(t0.shiftedBy(5), 25.0, 10.0, 2.0)));
        Assertions.assertEquals(6.25, interpolated.getOffset(),       1.0e-15);
        Assertions.assertEquals(5.00, interpolated.getRate(),         1.0e-15);
        Assertions.assertEquals(2.00, interpolated.getAcceleration(), 1.0e-15);
    }

}
