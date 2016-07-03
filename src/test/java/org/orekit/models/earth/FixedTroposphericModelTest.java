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

public class FixedTroposphericModelTest {

    private static double epsilon = 1e-6;

    private TroposphericModel model;

    @Test
    public void testModel() {
        // check with (artificial) test values from tropospheric-delay.txt
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), 0d), epsilon);
        Assert.assertEquals(20.8d, model.pathDelay(FastMath.toRadians(0d), 0d), epsilon);

        Assert.assertEquals(12.1d, model.pathDelay(FastMath.toRadians(0d), 5000d), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), 5000d), epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(FastMath.toRadians(35d), 1200d);
        Assert.assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        Assert.assertEquals(12.1d, model.pathDelay(FastMath.toRadians(-20d), 5000d), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), 100000d), epsilon);
    }

    @Test
    public void testSymmetry() {
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final double delay1 = model.pathDelay(FastMath.toRadians(elevation), 100);
            final double delay2 = model.pathDelay(FastMath.toRadians(180 - elevation), 100);

            Assert.assertEquals(delay1, delay2, epsilon);
        }
    }

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws Exception {
        model = FixedTroposphericDelay.getDefaultModel();
    }
}
