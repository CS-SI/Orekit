/* Copyright 2002-2024 CS GROUP
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
package org.orekit.gnss.metric.messages.rtcm;

import org.junit.jupiter.api.Test;
import org.orekit.gnss.metric.messages.common.AccuracyProvider;
import org.orekit.gnss.metric.messages.common.UserRangeAccuracy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRangeAccuracyTest {

    private final double eps = 0.1;

    @Test
    void testAccuracy() {
        AccuracyProvider ura;

        // Index = 0
        ura = new UserRangeAccuracy(0);
        assertEquals(2.0, ura.getAccuracy(), eps);
        // Index = 1
        ura = new UserRangeAccuracy(1);
        assertEquals(2.8, ura.getAccuracy(), eps);
        // Index = 2
        ura = new UserRangeAccuracy(2);
        assertEquals(4.0, ura.getAccuracy(), eps);
        // Index = 3
        ura = new UserRangeAccuracy(3);
        assertEquals(5.7, ura.getAccuracy(), eps);
        // Index = 4
        ura = new UserRangeAccuracy(4);
        assertEquals(8.0, ura.getAccuracy(), eps);
        // Index = 5
        ura = new UserRangeAccuracy(5);
        assertEquals(11.3, ura.getAccuracy(), eps);
        // Index = 6
        ura = new UserRangeAccuracy(6);
        assertEquals(16.0, ura.getAccuracy(), eps);
        // Index = 7
        ura = new UserRangeAccuracy(7);
        assertEquals(32.0, ura.getAccuracy(), eps);
        // Index = 8
        ura = new UserRangeAccuracy(8);
        assertEquals(64.0, ura.getAccuracy(), eps);
        // Index = 10
        ura = new UserRangeAccuracy(10);
        assertEquals(256.0, ura.getAccuracy(), eps);
        // Index = 16
        ura = new UserRangeAccuracy(16);
        assertEquals(8192.0, ura.getAccuracy(), eps);
    }

}
