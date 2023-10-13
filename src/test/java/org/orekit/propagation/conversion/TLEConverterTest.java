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
package org.orekit.propagation.conversion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.utils.ParameterDriver;

public class TLEConverterTest {

    @Test
    public void testDeselectOrbitals() {

        final TLE tle = new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                                "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");

        TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, PositionAngleType.MEAN, 1.0,
                                                                new FixedPointTleGenerationAlgorithm());
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            Assertions.assertTrue(driver.isSelected());
        }
        builder.deselectDynamicParameters();
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            Assertions.assertFalse(driver.isSelected());
        }
    }

    @Test
    public void testIssue859() {

        // INTELSAT 25 TLE taken from Celestrak the 2021-11-24T07:45:00.000
        // Because the satellite eccentricity and inclination are closed to zero, this satellite
        // reach convergence issues when converting the spacecraft's state to TLE.
        final TLE tle = new TLE("1 33153U 08034A   21327.46310733 -.00000207  00000+0  00000+0 0  9990",
                                "2 33153   0.0042  20.7353 0003042 213.9370 323.2156  1.00270917 48929");

        // Verify convergence issue
        final TLEPropagatorBuilder propagatorBuilderError = new TLEPropagatorBuilder(tle, PositionAngleType.MEAN, 1.,
                                                                                     new FixedPointTleGenerationAlgorithm());
        try {
            propagatorBuilderError.buildPropagator(propagatorBuilderError.getSelectedNormalizedParameters());
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_COMPUTE_TLE, oe.getSpecifier());
        }

        // Now try using different convergence threshold
        FixedPointTleGenerationAlgorithm algorithm =
                        new FixedPointTleGenerationAlgorithm(FixedPointTleGenerationAlgorithm.EPSILON_DEFAULT,
                                                             1000, 0.5);
        final TLEPropagatorBuilder propagatorBuilder = new TLEPropagatorBuilder(tle, PositionAngleType.MEAN, 1., algorithm);
        final TLEPropagator propagator = propagatorBuilder.buildPropagator(propagatorBuilderError.getSelectedNormalizedParameters());
        final TLE newTLE = propagator.getTLE();

        // Verify
        Assertions.assertEquals(0.0, newTLE.getDate().durationFrom(tle.getDate()), Utils.epsilonTest);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}