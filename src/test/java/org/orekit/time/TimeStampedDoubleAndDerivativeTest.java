/* Copyright 2002-2025 CS GROUP
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
import org.orekit.Utils;

public class TimeStampedDoubleAndDerivativeTest {
    @Test
    public void testConstructor() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedDoubleAndDerivative tsd = new TimeStampedDoubleAndDerivative(date, 1.68, 3.14);
        Assertions.assertEquals(date, tsd.getDate());
        Assertions.assertEquals(1.68, tsd.getValue());
    }

    @Test
    public void testToString() {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedDoubleAndDerivative tsd = new TimeStampedDoubleAndDerivative(date, 1.68, 3.14);
        Assertions.assertEquals("{date=2000-01-01T11:58:55.816Z, value=1.68, derivative=3.14}", tsd.toString());
    }
}
