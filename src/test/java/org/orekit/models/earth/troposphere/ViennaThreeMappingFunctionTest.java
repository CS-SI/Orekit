/* Copyright 2002-2025 CS GROUP
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
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

public class ViennaThreeMappingFunctionTest extends AbstractMappingFunctionTest<ViennaThree> {

    protected ViennaThree buildMappingFunction() {
        return new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                               new ConstantAzimuthalGradientProvider(null),
                               new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                               TimeScalesFactory.getUTC());
    }

    @Test
    public void testMappingFactors() {

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 12h UT
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
        doTestMappingFactors(new AbsoluteDate(2018, 11, 25, 12, 0, 0, TimeScalesFactory.getUTC()),
                             new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 824.0),
                             new TrackingCoordinates(0.0, FastMath.toRadians(38.0), 0.0),
                             1.621024, 1.623023);
    }

    @Test
    public void testLowElevation() {

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 12h UT
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
        doTestMappingFactors(new AbsoluteDate(2018, 11, 25, 12, 0, 0, TimeScalesFactory.getUTC()),
                             new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 824.0),
                             new TrackingCoordinates(0.0, FastMath.toRadians(5.0), 0.0),
                             10.132802, 10.879154);
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
        doTestMappingFactors(new AbsoluteDate(2018, 11, 25, 12, 0, 0, TimeScalesFactory.getUTC()),
                             new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 824.0),
                             new TrackingCoordinates(0.0, FastMath.toRadians(85.0), 0.0),
                             1.003810, 1.003816);
    }

    @Test
    @Override
    public void testDerivatives() {
        doTestDerivatives(5.0e-16, 1.0e-18, 1.0e-100, 3.0e-8, 1.0e-100);
    }

}
