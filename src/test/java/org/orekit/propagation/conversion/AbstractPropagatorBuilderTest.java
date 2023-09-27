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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import static org.orekit.Utils.assertParametersDriversValues;

public class AbstractPropagatorBuilderTest {

    /** Test method resetOrbit. */
    @Test
    public void testResetOrbit() {
        // Load a context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Use a Cartesian orbit so the parameters are changed sufficiently when shifting the orbit of a minute
        final Orbit initialOrbit = new CartesianOrbit(context.initialOrbit);

        final AbstractPropagatorBuilder propagatorBuilder = new AbstractPropagatorBuilder(initialOrbit, PositionAngleType.TRUE, 10., true) {

            @Override
            public PropagatorBuilder copy() {
                return null;
            }

            @Override
            public Propagator buildPropagator(double[] normalizedParameters) {
                // Dummy function "buildPropagator", copied from KeplerianPropagatorBuilder
                setParameters(normalizedParameters);
                return new KeplerianPropagator(createInitialOrbit());
            }

            @Override
            public AbstractBatchLSModel buildLeastSquaresModel(PropagatorBuilder[] builders,
                                                               List<ObservedMeasurement<?>> measurements,
                                                               ParameterDriversList estimatedMeasurementsParameters,
                                                               ModelObserver observer) {
                // The test don't use orbit determination. So, the method can return null
                return null;
            }
        };

        // Shift the orbit of a minute
        // Reset the builder and check the orbits value
        final Orbit newOrbit = initialOrbit.shiftedBy(60.);
        propagatorBuilder.resetOrbit(newOrbit);

        // Check that the new orbit was properly set in the builder and
        Assertions.assertEquals(0., propagatorBuilder.getInitialOrbitDate().durationFrom(newOrbit.getDate()), 0.);
        final double[] stateVector = new double[6];
        propagatorBuilder.getOrbitType().mapOrbitToArray(newOrbit, PositionAngleType.TRUE, stateVector, null);
        int i = 0;
        for (DelegatingDriver driver : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            final double expectedValue = stateVector[i++];
            Assertions.assertEquals(expectedValue, driver.getValue(), 0.);
            Assertions.assertEquals(expectedValue, driver.getReferenceValue(), 0.);
        }
    }

    /**
     * Assert that actual {@link PropagatorBuilder} instance et is a copy of expected instance.
     *
     * @param expected expected instance to compare to
     * @param actual actual instance to be compared
     * @param <B> type of the propagator builder
     */
    public static <B extends AbstractPropagatorBuilder> void assertPropagatorBuilderIsACopy(final B expected, final B actual){

        // They should not be the same instance
        Assertions.assertNotEquals(expected, actual);

        Assertions.assertArrayEquals(expected.getSelectedNormalizedParameters(),
                                     actual.getSelectedNormalizedParameters());

        assertParametersDriversValues(expected.getOrbitalParametersDrivers(),
                                       actual.getOrbitalParametersDrivers());

        Assertions.assertEquals(expected.getFrame(), actual.getFrame());
        Assertions.assertEquals(expected.getMu(), actual.getMu());
        Assertions.assertEquals(expected.getAttitudeProvider(), actual.getAttitudeProvider());
        Assertions.assertEquals(expected.getOrbitType(), actual.getOrbitType());
        Assertions.assertEquals(expected.getPositionAngleType(), actual.getPositionAngleType());
        Assertions.assertEquals(expected.getPositionScale(), actual.getPositionScale());
        Assertions.assertEquals(expected.getInitialOrbitDate(), actual.getInitialOrbitDate());
        Assertions.assertEquals(expected.getAdditionalDerivativesProviders(), actual.getAdditionalDerivativesProviders());
    }
}

