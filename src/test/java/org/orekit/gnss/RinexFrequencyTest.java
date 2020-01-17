/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.junit.Assert;
import org.junit.Test;

public class RinexFrequencyTest {
        
    @Test
    public void testMeasurementType() {
        for (final ObservationType rf : ObservationType.values()) {
            final char c = rf.toString().charAt(0);
            switch (rf.getMeasurementType()) {
                case PSEUDO_RANGE :
                    Assert.assertTrue(c == 'C' || c == 'P');
                    break;
                case CARRIER_PHASE :
                    Assert.assertTrue(c == 'L');
                    break;
                case DOPPLER :
                    Assert.assertTrue(c == 'D');
                    break;
                case SIGNAL_STRENGTH :
                    Assert.assertTrue(c == 'S');
                    break;
                default :
                    Assert.fail("unknown " + rf.getMeasurementType());
            }
        }
    }

}
