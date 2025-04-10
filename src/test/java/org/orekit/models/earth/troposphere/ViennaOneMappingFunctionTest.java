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

public class ViennaOneMappingFunctionTest extends AbstractMappingFunctionTest<ViennaOne> {

    protected ViennaOne buildMappingFunction() {
        return new ViennaOne(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
                             new ConstantAzimuthalGradientProvider(null),
                             new ConstantTroposphericModel(new TroposphericDelay(2.0966, 0.2140, 0, 0)),
                             TimeScalesFactory.getUTC());
    }

    @Test
    public void testMappingFactors() {
        // Site (NRAO, Green Bank, WV): latitude:  38°
        //                              longitude: 280°
        //                              height:    824.17 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref for the inputs:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //                        IERS Technical Note No. 36, BKG (2010)
        //
        // Values: ah  = 0.00127683
        //         aw  = 0.00060955
        //         zhd = 2.0966 m
        //         zwd = 0.2140 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/2.5x2/VMF1/VMF1_OP/2009/VMFG_20090812.H00
        //
        // Expected mapping factors : hydrostatic -> 3.425088
        //                                    wet -> 3.448300
        //
        // Expected outputs are obtained by performing the Matlab script vmf1_ht.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //
        doTestMappingFactors(AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC()),
                             new GeodeticPoint(FastMath.toRadians(38.0), FastMath.toRadians(280.0), 824.17),
                             new TrackingCoordinates(0.0, 0.5 * FastMath.PI - 1.278564131, 0.0),
                             3.425088, 3.448300);
    }

    @Test
    @Override
    public void testDerivatives() {
        doTestDerivatives(5.0e-16, 2.0e-19, 1.0e-100, 3.0e-8, 1.0e-100);
    }

}
