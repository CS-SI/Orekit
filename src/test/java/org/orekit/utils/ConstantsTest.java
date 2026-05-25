/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConstantsTest {

    public static final double TOLERANCE = 1.0;

    @Test
    public void testIAU2015NominalSolarRadius() {
        Assertions.assertEquals(6.957e8, Constants.IAU_2015_NOMINAL_SOLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalEarthRadii() {
        Assertions.assertEquals(6.3781e6, Constants.IAU_2015_NOMINAL_EARTH_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(6.3568e6, Constants.IAU_2015_NOMINAL_EARTH_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalJupiterRadii() {
        Assertions.assertEquals(7.1492e7, Constants.IAU_2015_NOMINAL_JUPITER_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(6.6854e7, Constants.IAU_2015_NOMINAL_JUPITER_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalMercuryRadii() {
        Assertions.assertEquals(2.44053e6, Constants.IAU_2015_NOMINAL_MERCURY_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(2.43826e6, Constants.IAU_2015_NOMINAL_MERCURY_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalVenusRadii() {
        Assertions.assertEquals(6.0518e6, Constants.IAU_2015_NOMINAL_VENUS_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(6.0518e6, Constants.IAU_2015_NOMINAL_VENUS_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalMarsRadii() {
        Assertions.assertEquals(3.39619e6, Constants.IAU_2015_NOMINAL_MARS_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(3.37620e6, Constants.IAU_2015_NOMINAL_MARS_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalSaturnRadii() {
        Assertions.assertEquals(6.0268e7, Constants.IAU_2015_NOMINAL_SATURN_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(5.4364e7, Constants.IAU_2015_NOMINAL_SATURN_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalUranusRadii() {
        Assertions.assertEquals(2.5559e7, Constants.IAU_2015_NOMINAL_URANUS_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(2.4973e7, Constants.IAU_2015_NOMINAL_URANUS_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testIAU2015NominalNeptuneRadii() {
        Assertions.assertEquals(2.4764e7, Constants.IAU_2015_NOMINAL_NEPTUNE_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(2.4341e7, Constants.IAU_2015_NOMINAL_NEPTUNE_POLAR_RADIUS, TOLERANCE);
    }

    @Test
    public void testLegacyConstants() {
        Assertions.assertEquals(1737400.0, Constants.MOON_EQUATORIAL_RADIUS, TOLERANCE);
        Assertions.assertEquals(6.957e8, Constants.SUN_RADIUS, TOLERANCE);
    }

}
