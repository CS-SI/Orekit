/* Copyright 2023 Thales Alenia Space
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
package org.orekit.models.earth.weather.water;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.models.earth.troposphere.TropoUnit;

public abstract class AbstractWaterVaporPressureProviderTest {

    protected abstract WaterVaporPressureProvider buildProvider();

    @Test
    public abstract void testReferenceWaterVaporPressure();

    protected void doTestReferenceWaterVaporPressure(final double tolerance) {
        // the reference value is from NBS/NRC steam table
        final WaterVaporPressureProvider provider = buildProvider();
        Assertions.assertEquals(TropoUnit.HECTO_PASCAL.toSI(10.55154),
                                provider.waterVaporPressure(TropoUnit.HECTO_PASCAL.toSI(1013.25), 273.5 + 18, 0.5),
                                tolerance);
    }

    @Test
    public void testRelativeHumidity() {
        final WaterVaporPressureProvider provider = buildProvider();
        for (double pPa = 700; pPa < 1100; pPa += 0.5) {
            final double p = TropoUnit.HECTO_PASCAL.toSI(pPa);
            for (double tC = 0.01; tC < 99; tC += 0.25) {
                final double t = 273.15 + tC;
                for (double rH = 0.0; rH < 1.0; rH += 0.02) {
                    final double e = provider.waterVaporPressure(p, t, rH);
                    Assertions.assertEquals(rH, provider.relativeHumidity(p, t, e), 1.0e-10);
                }
            }
        }
    }

}
