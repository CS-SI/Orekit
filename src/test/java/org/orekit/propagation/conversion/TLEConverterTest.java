/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.conversion;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.utils.ParameterDriver;

public class TLEConverterTest {

    @Test
    public void testDeselectOrbitals() {

        Utils.setDataRoot("regular-data");
        final TLE tle = new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                                "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        
        TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, PositionAngle.MEAN, 1.0);
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            Assert.assertTrue(driver.isSelected());
        }
        builder.deselectDynamicParameters();
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            Assert.assertFalse(driver.isSelected());
        }
    }
}