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
import org.orekit.gnss.metric.messages.common.AccuracyProvider;
import org.orekit.gnss.metric.messages.common.GlonassUserRangeAccuracy;

public class GlonassUserRangeAccuracyTest {

    private final double eps = 0.1;

    @Test
    public void testAccuracy() {
        AccuracyProvider ura;

        // Index = 0
        ura = new GlonassUserRangeAccuracy(0);
        Assertions.assertEquals(1.0, ura.getAccuracy(), eps);
        // Index = 1
        ura = new GlonassUserRangeAccuracy(1);
        Assertions.assertEquals(2.0, ura.getAccuracy(), eps);
        // Index = 2
        ura = new GlonassUserRangeAccuracy(2);
        Assertions.assertEquals(2.5, ura.getAccuracy(), eps);
        // Index = 3
        ura = new GlonassUserRangeAccuracy(3);
        Assertions.assertEquals(4.0, ura.getAccuracy(), eps);
        // Index = 4
        ura = new GlonassUserRangeAccuracy(4);
        Assertions.assertEquals(5.0, ura.getAccuracy(), eps);
        // Index = 5
        ura = new GlonassUserRangeAccuracy(5);
        Assertions.assertEquals(7.0, ura.getAccuracy(), eps);
        // Index = 6
        ura = new GlonassUserRangeAccuracy(6);
        Assertions.assertEquals(10.0, ura.getAccuracy(), eps);
        // Index = 7
        ura = new GlonassUserRangeAccuracy(7);
        Assertions.assertEquals(12.0, ura.getAccuracy(), eps);
        // Index = 8
        ura = new GlonassUserRangeAccuracy(8);
        Assertions.assertEquals(14.0, ura.getAccuracy(), eps);
        // Index = 9
        ura = new GlonassUserRangeAccuracy(9);
        Assertions.assertEquals(16.0, ura.getAccuracy(), eps);
        // Index = 10
        ura = new GlonassUserRangeAccuracy(10);
        Assertions.assertEquals(32.0, ura.getAccuracy(), eps);
        // Index = 11
        ura = new GlonassUserRangeAccuracy(11);
        Assertions.assertEquals(64.0, ura.getAccuracy(), eps);
        // Index = 12
        ura = new GlonassUserRangeAccuracy(12);
        Assertions.assertEquals(128.0, ura.getAccuracy(), eps);
        // Index = 13
        ura = new GlonassUserRangeAccuracy(13);
        Assertions.assertEquals(256.0, ura.getAccuracy(), eps);
        // Index = 14
        ura = new GlonassUserRangeAccuracy(14);
        Assertions.assertEquals(512.0, ura.getAccuracy(), eps);
        // Index = 15
        ura = new GlonassUserRangeAccuracy(15);
        Assertions.assertEquals(1024.0, ura.getAccuracy(), eps);
    }

}
