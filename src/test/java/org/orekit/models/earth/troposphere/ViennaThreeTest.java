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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

public class ViennaThreeTest {

    private static double epsilon = 1e-6;

    @BeforeAll
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @BeforeEach
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testMappingFactors() {

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 0h UT
        //
        // Values: ah  = 0.00123462
        //         aw  = 0.00047101
        //         zhd = 2.1993 m
        //         zwd = 0.0690 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/5x5/VMF3/VMF3_OP/2018/VMF3_20181125.H00
        //
        // Expected mapping factors : hydrostatic -> 1.621024
        //                                    wet -> 1.623023
        //
        // Expected outputs are obtained by performing the Matlab script vmf3.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final AbsoluteDate date = new AbsoluteDate(2018, 11, 25, TimeScalesFactory.getUTC());

        final double latitude     = FastMath.toRadians(37.5);
        final double longitude    = FastMath.toRadians(277.5);
        final double height       = 824.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(0.0, FastMath.toRadians(38.0), 0.0);
        final double expectedHydro = 1.621024;
        final double expectedWet   = 1.623023;

        final ViennaThree model = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                  new ConstantAzimuthalGradientProvider(null),
                                                  new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                  TimeScalesFactory.getUTC());

        final double[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              date);

        Assertions.assertEquals(expectedHydro, computedMapping[0], epsilon);
        Assertions.assertEquals(expectedWet,   computedMapping[1], epsilon);
    }

    @Test
    public void testLowElevation() {

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 0h UT
        //
        // Values: ah  = 0.00123462
        //         aw  = 0.00047101
        //         zhd = 2.1993 m
        //         zwd = 0.0690 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/5x5/VMF3/VMF3_OP/2018/VMF3_20181125.H00
        //
        // Expected mapping factors : hydrostatic -> 10.132802
        //                                    wet -> 10.879154
        //
        // Expected outputs are obtained by performing the Matlab script vmf3.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final AbsoluteDate date = new AbsoluteDate(2018, 11, 25, TimeScalesFactory.getUTC());

        final double latitude     = FastMath.toRadians(37.5);
        final double longitude    = FastMath.toRadians(277.5);
        final double height       = 824.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(0.0, FastMath.toRadians(5.0), 0.0);
        final double expectedHydro = 10.132802;
        final double expectedWet   = 10.879154;

        final ViennaThree model = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                  new ConstantAzimuthalGradientProvider(null),
                                                  new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                  TimeScalesFactory.getUTC());

        final double[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              date);

        Assertions.assertEquals(expectedHydro, computedMapping[0], epsilon);
        Assertions.assertEquals(expectedWet,   computedMapping[1], epsilon);
    }

    @Test
    public void testHightElevation() {

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 0h UT
        //
        // Values: ah  = 0.00123462
        //         aw  = 0.00047101
        //         zhd = 2.1993 m
        //         zwd = 0.0690 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/5x5/VMF3/VMF3_OP/2018/VMF3_20181125.H00
        //
        // Expected mapping factors : hydrostatic -> 1.003810
        //                                    wet -> 1.003816
        //
        // Expected outputs are obtained by performing the Matlab script vmf3.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final AbsoluteDate date = new AbsoluteDate(2018, 11, 25, TimeScalesFactory.getUTC());

        final double latitude     = FastMath.toRadians(37.5);
        final double longitude    = FastMath.toRadians(277.5);
        final double height       = 824.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(0.0, FastMath.toRadians(85.0), 0.0);
        final double expectedHydro = 1.003810;
        final double expectedWet   = 1.003816;

        final ViennaThree model = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                  new ConstantAzimuthalGradientProvider(null),
                                                  new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                  TimeScalesFactory.getUTC());

        final double[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              date);

        Assertions.assertEquals(expectedHydro, computedMapping[0], epsilon);
        Assertions.assertEquals(expectedWet,   computedMapping[1], epsilon);
    }

    @Test
    public void testDelay() {
        final double        azimuth   = 30.0;
        final double        elevation = 10.0;
        final double        height    = 100.0;
        final AbsoluteDate  date      = new AbsoluteDate();
        final GeodeticPoint point     = new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), height);
        final ViennaThree   model     = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                        new ConstantAzimuthalGradientProvider(null),
                                                        new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                        TimeScalesFactory.getUTC());
        final TroposphericDelay delay = model.pathDelay(new TrackingCoordinates(FastMath.toRadians(azimuth), FastMath.toRadians(elevation), 0.0),
                                                        point,
                                                        TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                        model.getParameters(date), date);
        Assertions.assertEquals( 2.1993, delay.getZh(),    1.0e-4);
        Assertions.assertEquals( 0.069,  delay.getZw(),    1.0e-4);
        Assertions.assertEquals(12.2124, delay.getSh(),    1.0e-4);
        Assertions.assertEquals( 0.3916, delay.getSw(),    1.0e-4);
        Assertions.assertEquals(12.6041, delay.getDelay(), 1.0e-4);
    }

    @Test
    public void testDelayWithAzimuthalAsymmetry() {
        final double        azimuth   = 30.0;
        final double        elevation = 10.0;
        final double        height    = 100.0;
        final AbsoluteDate  date      = new AbsoluteDate();
        final GeodeticPoint point     = new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), height);
        final ViennaThree   model     = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                        new ConstantAzimuthalGradientProvider(new AzimuthalGradientCoefficients(12.0, 4.5,
                                                                                                                                0.8, 1.25)),
                                                        new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                        TimeScalesFactory.getUTC());
        final TroposphericDelay delay = model.pathDelay(new TrackingCoordinates(FastMath.toRadians(azimuth), FastMath.toRadians(elevation), 0.0),
                                                        point,
                                                        TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                        model.getParameters(date), date);
        Assertions.assertEquals( 2.1993,                      delay.getZh(),    1.0e-4);
        Assertions.assertEquals( 0.069,                       delay.getZw(),    1.0e-4);
        Assertions.assertEquals(12.2124 + 373.8241,           delay.getSh(),    1.0e-4); // second term is due to azimuthal gradient
        Assertions.assertEquals( 0.3916 +  38.9670,           delay.getSw(),    1.0e-4); // second term is due to azimuthal gradient
        Assertions.assertEquals(12.6041 + 373.8241 + 38.9670, delay.getDelay(), 1.0e-4);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 350.0);
        ViennaThree model = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                            new ConstantAzimuthalGradientProvider(null),
                                            new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                            TimeScalesFactory.getUTC());
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(elev), 0.0),
                                                 point,
                                                 TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                 model.getParameters(date), date).getDelay();
            Assertions.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

}
