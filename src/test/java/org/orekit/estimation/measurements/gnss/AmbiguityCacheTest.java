/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.estimation.measurements.gnss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.gnss.PredefinedGnssSignal;

public class AmbiguityCacheTest {

    @Test
    public void testCache() {
        final AmbiguityCache cache  = new AmbiguityCache();
        final AmbiguityDriver
            driver01 = cache.getAmbiguity("E18", "TUKT", PredefinedGnssSignal.E01.getWavelength());
        Assertions.assertEquals("E18",  driver01.getEmitter());
        Assertions.assertEquals("TUKT", driver01.getReceiver());
        Assertions.assertEquals(PredefinedGnssSignal.E01.getWavelength(), driver01.getWavelength(), 1.0e-10);
        Assertions.assertEquals("ambiguity-E18-TUKT-154.00", driver01.getName());
        final AmbiguityDriver driver05 = cache.getAmbiguity("E18", "TUKT", PredefinedGnssSignal.E05.getWavelength());
        Assertions.assertEquals("E18",  driver05.getEmitter());
        Assertions.assertEquals("TUKT", driver05.getReceiver());
        Assertions.assertEquals(PredefinedGnssSignal.E05.getWavelength(), driver05.getWavelength(), 1.0e-10);
        Assertions.assertEquals("ambiguity-E18-TUKT-115.00", driver05.getName());
        final AmbiguityDriver driverB = cache.getAmbiguity("E19", "AGGO", PredefinedGnssSignal.E01.getWavelength());
        Assertions.assertEquals("E19",  driverB.getEmitter());
        Assertions.assertEquals("AGGO", driverB.getReceiver());
        Assertions.assertEquals(PredefinedGnssSignal.E01.getWavelength(), driverB.getWavelength(), 1.0e-10);
        Assertions.assertEquals("ambiguity-E19-AGGO-154.00", driverB.getName());
        Assertions.assertSame(driver01, cache.getAmbiguity("E18", "TUKT", PredefinedGnssSignal.E01.getWavelength()));
    }

}
