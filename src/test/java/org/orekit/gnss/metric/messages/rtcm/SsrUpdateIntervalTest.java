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
package org.orekit.gnss.metric.messages.rtcm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.gnss.metric.messages.common.SsrUpdateInterval;

public class SsrUpdateIntervalTest {

    private double eps = 0.01;

    @Test
    public void testUpdateInterval() {
        SsrUpdateInterval sui;
        
        // Index = 0
        sui = new SsrUpdateInterval(0);
        Assertions.assertEquals(1.0, sui.getUpdateInterval(), eps);
        // Index = 1
        sui = new SsrUpdateInterval(1);
        Assertions.assertEquals(2.0, sui.getUpdateInterval(), eps);
        // Index = 2
        sui = new SsrUpdateInterval(2);
        Assertions.assertEquals(5.0, sui.getUpdateInterval(), eps);
        // Index = 3
        sui = new SsrUpdateInterval(3);
        Assertions.assertEquals(10.0, sui.getUpdateInterval(), eps);
        // Index = 4
        sui = new SsrUpdateInterval(4);
        Assertions.assertEquals(15.0, sui.getUpdateInterval(), eps);
        // Index = 5
        sui = new SsrUpdateInterval(5);
        Assertions.assertEquals(30.0, sui.getUpdateInterval(), eps);
        // Index = 6
        sui = new SsrUpdateInterval(6);
        Assertions.assertEquals(60.0, sui.getUpdateInterval(), eps);
        // Index = 7
        sui = new SsrUpdateInterval(7);
        Assertions.assertEquals(120.0, sui.getUpdateInterval(), eps);
        // Index = 8
        sui = new SsrUpdateInterval(8);
        Assertions.assertEquals(240.0, sui.getUpdateInterval(), eps);
        // Index = 9
        sui = new SsrUpdateInterval(9);
        Assertions.assertEquals(300.0, sui.getUpdateInterval(), eps);
        // Index = 10
        sui = new SsrUpdateInterval(10);
        Assertions.assertEquals(600.0, sui.getUpdateInterval(), eps);
        // Index = 11
        sui = new SsrUpdateInterval(11);
        Assertions.assertEquals(900.0, sui.getUpdateInterval(), eps);
        // Index = 12
        sui = new SsrUpdateInterval(12);
        Assertions.assertEquals(1800.0, sui.getUpdateInterval(), eps);
        // Index = 13
        sui = new SsrUpdateInterval(13);
        Assertions.assertEquals(3600.0, sui.getUpdateInterval(), eps);
        // Index = 14
        sui = new SsrUpdateInterval(14);
        Assertions.assertEquals(7200.0, sui.getUpdateInterval(), eps);
        // Index = 15
        sui = new SsrUpdateInterval(15);
        Assertions.assertEquals(10800.0, sui.getUpdateInterval(), eps);
    }

}
