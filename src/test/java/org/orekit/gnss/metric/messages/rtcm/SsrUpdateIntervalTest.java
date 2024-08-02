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
import org.orekit.gnss.metric.messages.common.SsrUpdateInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SsrUpdateIntervalTest {

    private double eps = 0.01;

    @Test
    void testUpdateInterval() {
        SsrUpdateInterval sui;
        
        // Index = 0
        sui = new SsrUpdateInterval(0);
        assertEquals(1.0, sui.getUpdateInterval(), eps);
        // Index = 1
        sui = new SsrUpdateInterval(1);
        assertEquals(2.0, sui.getUpdateInterval(), eps);
        // Index = 2
        sui = new SsrUpdateInterval(2);
        assertEquals(5.0, sui.getUpdateInterval(), eps);
        // Index = 3
        sui = new SsrUpdateInterval(3);
        assertEquals(10.0, sui.getUpdateInterval(), eps);
        // Index = 4
        sui = new SsrUpdateInterval(4);
        assertEquals(15.0, sui.getUpdateInterval(), eps);
        // Index = 5
        sui = new SsrUpdateInterval(5);
        assertEquals(30.0, sui.getUpdateInterval(), eps);
        // Index = 6
        sui = new SsrUpdateInterval(6);
        assertEquals(60.0, sui.getUpdateInterval(), eps);
        // Index = 7
        sui = new SsrUpdateInterval(7);
        assertEquals(120.0, sui.getUpdateInterval(), eps);
        // Index = 8
        sui = new SsrUpdateInterval(8);
        assertEquals(240.0, sui.getUpdateInterval(), eps);
        // Index = 9
        sui = new SsrUpdateInterval(9);
        assertEquals(300.0, sui.getUpdateInterval(), eps);
        // Index = 10
        sui = new SsrUpdateInterval(10);
        assertEquals(600.0, sui.getUpdateInterval(), eps);
        // Index = 11
        sui = new SsrUpdateInterval(11);
        assertEquals(900.0, sui.getUpdateInterval(), eps);
        // Index = 12
        sui = new SsrUpdateInterval(12);
        assertEquals(1800.0, sui.getUpdateInterval(), eps);
        // Index = 13
        sui = new SsrUpdateInterval(13);
        assertEquals(3600.0, sui.getUpdateInterval(), eps);
        // Index = 14
        sui = new SsrUpdateInterval(14);
        assertEquals(7200.0, sui.getUpdateInterval(), eps);
        // Index = 15
        sui = new SsrUpdateInterval(15);
        assertEquals(10800.0, sui.getUpdateInterval(), eps);
    }

}
