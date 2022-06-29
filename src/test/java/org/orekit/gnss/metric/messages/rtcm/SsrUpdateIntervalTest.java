/* Copyright 2002-2022 CS GROUP
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

import org.junit.Assert;
import org.junit.Test;
import org.orekit.gnss.metric.messages.common.SsrUpdateInterval;

public class SsrUpdateIntervalTest {

    private double eps = 0.01;

    @Test
    public void testUpdateInterval() {
        SsrUpdateInterval sui;
        
        // Index = 0
        sui = new SsrUpdateInterval(0);
        Assert.assertEquals(1.0, sui.getUpdateInterval(), eps);
        // Index = 1
        sui = new SsrUpdateInterval(1);
        Assert.assertEquals(2.0, sui.getUpdateInterval(), eps);
        // Index = 2
        sui = new SsrUpdateInterval(2);
        Assert.assertEquals(5.0, sui.getUpdateInterval(), eps);
        // Index = 3
        sui = new SsrUpdateInterval(3);
        Assert.assertEquals(10.0, sui.getUpdateInterval(), eps);
        // Index = 4
        sui = new SsrUpdateInterval(4);
        Assert.assertEquals(15.0, sui.getUpdateInterval(), eps);
        // Index = 5
        sui = new SsrUpdateInterval(5);
        Assert.assertEquals(30.0, sui.getUpdateInterval(), eps);
        // Index = 6
        sui = new SsrUpdateInterval(6);
        Assert.assertEquals(60.0, sui.getUpdateInterval(), eps);
        // Index = 7
        sui = new SsrUpdateInterval(7);
        Assert.assertEquals(120.0, sui.getUpdateInterval(), eps);
        // Index = 8
        sui = new SsrUpdateInterval(8);
        Assert.assertEquals(240.0, sui.getUpdateInterval(), eps);
        // Index = 9
        sui = new SsrUpdateInterval(9);
        Assert.assertEquals(300.0, sui.getUpdateInterval(), eps);
        // Index = 10
        sui = new SsrUpdateInterval(10);
        Assert.assertEquals(600.0, sui.getUpdateInterval(), eps);
        // Index = 11
        sui = new SsrUpdateInterval(11);
        Assert.assertEquals(900.0, sui.getUpdateInterval(), eps);
        // Index = 12
        sui = new SsrUpdateInterval(12);
        Assert.assertEquals(1800.0, sui.getUpdateInterval(), eps);
        // Index = 13
        sui = new SsrUpdateInterval(13);
        Assert.assertEquals(3600.0, sui.getUpdateInterval(), eps);
        // Index = 14
        sui = new SsrUpdateInterval(14);
        Assert.assertEquals(7200.0, sui.getUpdateInterval(), eps);
        // Index = 15
        sui = new SsrUpdateInterval(15);
        Assert.assertEquals(10800.0, sui.getUpdateInterval(), eps);
    }

}
