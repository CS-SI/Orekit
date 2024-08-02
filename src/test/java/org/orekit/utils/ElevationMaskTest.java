/* Copyright 2002-2024 CS GROUP
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ElevationMaskTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testElevationMask() {
        double [][] masqueData = {{FastMath.toRadians(  0), FastMath.toRadians(5)},
                                  {FastMath.toRadians(180), FastMath.toRadians(3)},
                                  {FastMath.toRadians(-90), FastMath.toRadians(4)}};

        ElevationMask mask = new ElevationMask(masqueData);

        assertNotNull(mask);
    }

    @Test
    void testMaskException() {
        assertThrows(IllegalArgumentException.class, () -> {
            double [][] masque = {{FastMath.toRadians(   0), FastMath.toRadians(5)},
                    {FastMath.toRadians( 360), FastMath.toRadians(4)}};

            new ElevationMask(masque);
        });
    }

    @Test
    void testGetElevation() {
        double [][] masqueData = {{FastMath.toRadians(  0), FastMath.toRadians(5)},
                              {FastMath.toRadians(180), FastMath.toRadians(3)},
                              {FastMath.toRadians(-90), FastMath.toRadians(4)}};
        ElevationMask mask = new ElevationMask(masqueData);

        double azimuth = FastMath.toRadians(90);
        double elevation = mask.getElevation(azimuth);
        assertEquals(FastMath.toRadians(4), elevation, 1.0e-15);
    }

}
