/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.utils.Constants;

public class SeasonalGridTest extends AbstractGridTest<SeasonalGrid> {

    @Test
    @Override
    public void testMetadata() {
        doTestMetadata(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                        "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                       -90.0, 90.0, 121, -180.0, 180.0, 241);
    }

    @Test
    @Override
    public void testMinMax() {
        doTestMinMax(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                      "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                     50496.011, 104890.869, 1.0e-3);
    }

    @Test
    @Override
    public void testValue() {
        doTestValue(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                     "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                    new GeodeticPoint(FastMath.toRadians(47.71675), FastMath.toRadians(6.12264), 300.0),
                    12.5 * Constants.JULIAN_DAY, 96983.019, 1.0e-3);
    }

    @Test
    @Override
    public void testGradient() {
        doTestGradient(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                        "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                       new GeodeticPoint(FastMath.toRadians(47.71675), FastMath.toRadians(6.12264), 300.0),
                       12.5 * Constants.JULIAN_DAY, 1.0e-12, 1.1e-5);
    }

}
