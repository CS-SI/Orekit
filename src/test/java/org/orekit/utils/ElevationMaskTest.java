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
package org.orekit.utils;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ElevationMaskTest {

    @BeforeEach
    public void setUp()
        throws Exception {
    }

    @AfterEach
    public void tearDown()
        throws Exception {
    }

    @Test
    public void testElevationMask() {
        double [][] masqueData = {{FastMath.toRadians(  0), FastMath.toRadians(5)},
                                  {FastMath.toRadians(180), FastMath.toRadians(3)},
                                  {FastMath.toRadians(-90), FastMath.toRadians(4)}};

        ElevationMask mask = new ElevationMask(masqueData);

        Assertions.assertNotNull(mask);
    }

    @Test
    public void testMaskException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            double [][] masque = {{FastMath.toRadians(   0), FastMath.toRadians(5)},
                    {FastMath.toRadians( 360), FastMath.toRadians(4)}};

            new ElevationMask(masque);
        });
    }

    @Test
    public void testGetElevation() {
        double [][] masqueData = {{FastMath.toRadians(  0), FastMath.toRadians(5)},
                              {FastMath.toRadians(180), FastMath.toRadians(3)},
                              {FastMath.toRadians(-90), FastMath.toRadians(4)}};
        ElevationMask mask = new ElevationMask(masqueData);

        double azimuth = FastMath.toRadians(90);
        double elevation = mask.getElevation(azimuth);
        Assertions.assertEquals(FastMath.toRadians(4), elevation, 1.0e-15);
    }

}
