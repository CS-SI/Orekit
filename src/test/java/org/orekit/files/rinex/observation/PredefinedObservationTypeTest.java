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
package org.orekit.files.rinex.observation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;

public class PredefinedObservationTypeTest {

    @Test
    public void testMeasurementType() {
        for (final ObservationType rf : PredefinedObservationType.values()) {
            final char c = rf.toString().charAt(0);
            switch (rf.getMeasurementType()) {
                case PSEUDO_RANGE :
                    Assertions.assertTrue(c == 'C' || c == 'P');
                    break;
                case CARRIER_PHASE :
                    Assertions.assertEquals('L', c);
                    break;
                case DOPPLER :
                    Assertions.assertEquals('D', c);
                    break;
                case SIGNAL_STRENGTH :
                    Assertions.assertEquals('S', c);
                    break;
                default :
                    Assertions.fail("unknown " + rf.getMeasurementType());
            }
        }
    }

}
