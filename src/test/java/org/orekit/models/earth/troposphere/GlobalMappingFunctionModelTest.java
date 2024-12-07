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
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

public class GlobalMappingFunctionModelTest extends AbstractMappingFunctionTest<GlobalMappingFunctionModel> {

    protected GlobalMappingFunctionModel buildMappingFunction() {
        return new GlobalMappingFunctionModel();
    }

    @Test
    public void testMappingFactors() {
        // Site (NRAO, Green Bank, WV): latitude:  0.6708665767 radians
        //                              longitude: -1.393397187 radians
        //                              height:    844.715 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 12h UT
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)
        //
        // Expected mapping factors : hydrostatic -> 3.425246 (Ref)
        //                                    wet -> 3.449589 (Ref)
        doTestMappingFactors(AbsoluteDate.createMJDDate(55055, 43200, TimeScalesFactory.getUTC()),
                             new GeodeticPoint(0.6708665767, -1.393397187, 844.715),
                             new TrackingCoordinates(0.0, 0.5 * FastMath.PI - 1.278564131, 0.0),
                             3.425246, 3.449589);
    }

    @Test
    @Override
    public void testDerivatives() {
        doTestDerivatives(1.0e-100, 5.0e-19, 1.0e-100, 3.0e-8, 1.0e-100);
    }

}
