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
package org.orekit.models.earth.troposphere;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

public abstract class AbstractMappingFunctionTest {

    protected abstract TroposphereMappingFunction buildMappingFunction();

    @Test
    public abstract void testMappingFactors();

    protected void doTestMappingFactors(final double expectedHydro,
                                        final double expectedWet) {

        // Site (Le Mans, France):      latitude:  48.0°
        //                              longitude: 0.20°
        //                              height:    68 m

        final AbsoluteDate date = new AbsoluteDate(1994, 1, 1, TimeScalesFactory.getUTC());

        final double latitude    = FastMath.toRadians(48.0);
        final double longitude   = FastMath.toRadians(0.20);
        final double height      = 68.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(0.0, FastMath.toRadians(5.0), 0.0);

        final TroposphereMappingFunction model = buildMappingFunction();

        final double[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              date);
        Assertions.assertEquals(expectedHydro, computedMapping[0], 1.0e-2);
        Assertions.assertEquals(expectedWet,   computedMapping[1], 1.0e-2);

    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        TroposphereMappingFunction model = buildMappingFunction();
        double[] lastFactors = new double[] {
            Double.MAX_VALUE,
            Double.MAX_VALUE
        };
        // mapping functions shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double[] factors = model.mappingFactors(new TrackingCoordinates(0.0, FastMath.toRadians(elev), 0.0),
                                                          point,
                                                          TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                          date);
            Assertions.assertTrue(Precision.compareTo(factors[0], lastFactors[0], 1.0e-6) < 0);
            Assertions.assertTrue(Precision.compareTo(factors[1], lastFactors[1], 1.0e-6) < 0);
            lastFactors[0] = factors[0];
            lastFactors[1] = factors[1];
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
