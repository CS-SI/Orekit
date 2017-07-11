/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth;


import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;

public class MariniMurrayModelTest {

    private static double epsilon = 1e-6;

    private TroposphericModel model;

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws Exception {
        // ruby laser with wavelength 694.3 nm
        model = MariniMurrayModel.getStandardModel(FastMath.toRadians(45.0), 694.3);
    }

    @Test
    public void testDelay() {
        final double elevation = 10d;
        final double height = 100d;

        final double path = model.pathDelay(FastMath.toRadians(elevation), height);

        Assert.assertTrue(Precision.compareTo(path, 20d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(path, 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(FastMath.toRadians(elev), 350);
            Assert.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void compareExpectedValues() {

        double height = 0;
        double elevation = 10;
        double expectedValue = 13.919144625789874;
        double actualValue = model.pathDelay(FastMath.toRadians(elevation), height);

        Assert.assertEquals(expectedValue, actualValue, epsilon);
    }
}
