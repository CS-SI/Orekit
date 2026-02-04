/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.KeplerianPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterEstimatorTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetOrbitalParametersDrivers(final boolean estimateOnly) {
        // GIVEN
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(TestUtils.getTestOrbit(),
                PositionAngleType.MEAN, 1.);
        builder.getOrbitalParametersDrivers().getDrivers().forEach(delegatingDriver -> delegatingDriver.setSelected(false));
        builder.getOrbitalParametersDrivers().getDrivers().get(0).setSelected(true);  // select only one
        final TestEstimator testEstimator = new TestEstimator(builder);
        // WHEN
        final ParameterDriversList driversList = testEstimator.getOrbitalParametersDrivers(estimateOnly);
        // THEN
        if (estimateOnly) {
            assertEquals(driversList.getDrivers().stream().filter(ParameterDriver::isSelected).count(),
                    driversList.getDrivers().size());
        } else {
            assertEquals(6, driversList.getDrivers().size());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetPropagationParametersDrivers(final boolean estimateOnly) {
        // GIVEN
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(TestUtils.getTestOrbit(),
                PositionAngleType.MEAN, 1.);
        final TestEstimator testEstimator = new TestEstimator(builder);
        // WHEN
        final ParameterDriversList driversList = testEstimator.getPropagationParametersDrivers(estimateOnly);
        // THEN
        if (estimateOnly) {
            assertEquals(0, driversList.getDrivers().size());
        } else {
            assertEquals(1, driversList.getDrivers().size());
        }
    }

    private static class TestEstimator implements ParameterEstimator {

        private final PropagatorBuilder builder;

        TestEstimator(final PropagatorBuilder builder) {
            this.builder = builder;
        }

        @Override
        public PropagatorBuilder[] getPropagatorBuilders() {
            return new PropagatorBuilder[] {builder};
        }
    }
}
