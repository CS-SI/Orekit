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
import org.orekit.gnss.metric.messages.rtcm.ephemeris.utils.AccuracyProvider;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.utils.SignalInSpaceAccuracy;

public class SignalInSpaceAccuracyTest {

    private final double eps = 0.01;

    @Test
    public void testAccuracy() {
        AccuracyProvider ura;

        // Index = 0
        ura = new SignalInSpaceAccuracy(0);
        Assert.assertEquals(0.0, ura.getAccuracy(), eps);
        // Index = 25
        ura = new SignalInSpaceAccuracy(25);
        Assert.assertEquals(0.25, ura.getAccuracy(), eps);
        // Index = 50
        ura = new SignalInSpaceAccuracy(50);
        Assert.assertEquals(0.50, ura.getAccuracy(), eps);
        // Index = 60
        ura = new SignalInSpaceAccuracy(60);
        Assert.assertEquals(0.70, ura.getAccuracy(), eps);
        // Index = 90
        ura = new SignalInSpaceAccuracy(90);
        Assert.assertEquals(1.60, ura.getAccuracy(), eps);
        // Index = 115
        ura = new SignalInSpaceAccuracy(115);
        Assert.assertEquals(4.40, ura.getAccuracy(), eps);
        // Index = 126
        ura = new SignalInSpaceAccuracy(126);
        Assert.assertEquals(-1.0, ura.getAccuracy(), eps);
    }

}
