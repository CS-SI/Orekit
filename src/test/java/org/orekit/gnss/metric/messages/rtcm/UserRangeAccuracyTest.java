/* Copyright 2002-2021 CS GROUP
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
import org.orekit.gnss.metric.messages.rtcm.ephemeris.utils.AccuracyProvider;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.utils.UserRangeAccuracy;

public class UserRangeAccuracyTest {

    private final double eps = 0.1;

    @Test
    public void testAccuracy() {
        AccuracyProvider ura;
        
        // Index = 0
        ura = new UserRangeAccuracy(0);
        Assert.assertEquals(2.0, ura.getAccuracy(), eps);
        // Index = 1
        ura = new UserRangeAccuracy(1);
        Assert.assertEquals(2.8, ura.getAccuracy(), eps);
        // Index = 2
        ura = new UserRangeAccuracy(2);
        Assert.assertEquals(4.0, ura.getAccuracy(), eps);
        // Index = 3
        ura = new UserRangeAccuracy(3);
        Assert.assertEquals(5.7, ura.getAccuracy(), eps);
        // Index = 4
        ura = new UserRangeAccuracy(4);
        Assert.assertEquals(8.0, ura.getAccuracy(), eps);
        // Index = 5
        ura = new UserRangeAccuracy(5);
        Assert.assertEquals(11.3, ura.getAccuracy(), eps);
        // Index = 6
        ura = new UserRangeAccuracy(6);
        Assert.assertEquals(16.0, ura.getAccuracy(), eps);
        // Index = 7
        ura = new UserRangeAccuracy(7);
        Assert.assertEquals(32.0, ura.getAccuracy(), eps);
        // Index = 8
        ura = new UserRangeAccuracy(8);
        Assert.assertEquals(64.0, ura.getAccuracy(), eps);
        // Index = 10
        ura = new UserRangeAccuracy(10);
        Assert.assertEquals(256.0, ura.getAccuracy(), eps);
        // Index = 16
        ura = new UserRangeAccuracy(16);
        Assert.assertEquals(8192.0, ura.getAccuracy(), eps);
    }

}
