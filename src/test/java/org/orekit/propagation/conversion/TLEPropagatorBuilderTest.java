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

import static org.orekit.Utils.assertParametersDriversValues;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;

public class TLEPropagatorBuilderTest {

    @Test
    @DisplayName("Test copy method")
    void testCopyMethod() {

        // Given
        final DataContext dataContext = Utils.setDataRoot("regular-data");
        final TLE tle = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final PositionAngleType positionAngleType = null;
        final double        positionScale = 1;

        final TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, positionAngleType, positionScale, dataContext,
                                                                      new FixedPointTleGenerationAlgorithm());

        // When
        final TLEPropagatorBuilder copyBuilder = builder.copy();

        // Then
        assertTlePropagatorBuilderIsACopy(builder, copyBuilder);

    }

    private void assertTlePropagatorBuilderIsACopy(final TLEPropagatorBuilder expected, final TLEPropagatorBuilder actual) {

        // They should not be the same instance
        Assertions.assertNotEquals(expected, actual);

        Assertions.assertArrayEquals(expected.getSelectedNormalizedParameters(),
                                     actual.getSelectedNormalizedParameters());

        assertParametersDriversValues(expected.getOrbitalParametersDrivers(),
                                      actual.getOrbitalParametersDrivers());

        Assertions.assertEquals(expected.getFrame(), actual.getFrame());
        Assertions.assertEquals(expected.getMu(), actual.getMu());
        Assertions.assertEquals(expected.getOrbitType(), actual.getOrbitType());
        Assertions.assertEquals(expected.getPositionAngleType(), actual.getPositionAngleType());
        Assertions.assertEquals(expected.getPositionScale(), actual.getPositionScale());
        Assertions.assertEquals(expected.getInitialOrbitDate(), actual.getInitialOrbitDate());
        Assertions.assertEquals(expected.getAdditionalDerivativesProviders(), actual.getAdditionalDerivativesProviders());
        Assertions.assertEquals(expected.getTemplateTLE(), actual.getTemplateTLE());

        // Attitude provider is necessarily different due to how a TLEPropagatorBuilder is defined
        //Assertions.assertEquals(expected.getAttitudeProvider(), actual.getAttitudeProvider());
    }

}
