/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.propagation.analytical.gnss.GLONASSOrbitalElements;


public class GLONASSAlmanacTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testPerfectValues() {

        final double pi = GLONASSOrbitalElements.GLONASS_PI;
        final GLONASSAlmanac almanac = new GLONASSAlmanac(0, 1, 6, 9, 2001, 27122.09375,
                                                          -0.189986229 * pi,
                                                          0.011929512 * pi,
                                                          0.440277100 * pi,
                                                          0.001482010,
                                                          -2655.76171875,
                                                          0.000549316,
                                                          0.0, 0.0, 0.0);

        final int na = almanac.getNa();
        final int n4 = almanac.getN4();

        Assert.assertEquals(615, na);
        Assert.assertEquals(2,   n4);
    }
}
