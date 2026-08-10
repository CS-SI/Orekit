/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.AbstractOrbitFactory;
import org.orekit.orbits.AbstractOrbitalParameterFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CartesianOrbitFactory;
import org.orekit.orbits.OrbitalParameters;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

import static org.orekit.Utils.assertParametersDriversValues;

public class AbstractPropagatorBuilderTest {

    /** Test method resetOrbit. */
    @Test
    void testResetOrbit() {
        // Load a context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Use a Cartesian orbit so the parameters are changed sufficiently when shifting the orbit of a minute
        final CartesianOrbit initialOrbit = new CartesianOrbit(context.initialOrbit);

        final AbstractPropagatorBuilder<KeplerianPropagator, CartesianOrbit, CartesianOrbitFactory> propagatorBuilder =
            new AbstractPropagatorBuilder<>
                ((CartesianOrbitFactory) initialOrbit.factory(PositionAngleType.TRUE, 10.), true) {

            @Override
            public KeplerianPropagator buildPropagator(double[] normalizedParameters) {
                // Dummy function "buildPropagator", copied from KeplerianPropagatorBuilder
                setParameters(normalizedParameters);
                return new KeplerianPropagator(getOrbitalParameterFactory().createFromDrivers());
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
        final CartesianOrbit newOrbit = initialOrbit.shiftedBy(60.).inFrame(FramesFactory.getTOD(true));
        propagatorBuilder.resetOrbit(newOrbit);

        // Check that the new orbit was properly set in the builder and
        Assertions.assertEquals(0.,
                                propagatorBuilder.getOrbitalParameterFactory().getDate().durationFrom(newOrbit.getDate()),
                                0.);
        final double[] stateVector = new double[6];
        initialOrbit.getType().mapOrbitToArray(newOrbit.inFrame(context.initialOrbit.getFrame()),
                                               PositionAngleType.TRUE, stateVector, null);
        int i = 0;
        for (DelegatingDriver driver :
            propagatorBuilder.getOrbitalParameterFactory().getOrbitalParametersDrivers().getDrivers()) {
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
     * @param <T> type of the propagator
     * @param <O> type of the orbital parameters
     * @param <F> type of the orbital parameters factory
     */
    public static <T extends AbstractPropagator,
                   O extends OrbitalParameters,
                   F extends AbstractOrbitalParameterFactory<O>,
                   B extends AbstractPropagatorBuilder<T, O, F>>
    void assertPropagatorBuilderIsACopy(final B expected, final B actual) {

        // They should not be the same instance
        Assertions.assertNotEquals(expected, actual);

        Assertions.assertArrayEquals(expected.getSelectedNormalizedParameters(),
                                     actual.getSelectedNormalizedParameters());

        final F expectedF = expected.getOrbitalParameterFactory();
        final F actualF   = actual.getOrbitalParameterFactory();
        assertParametersDriversValues(expectedF.getOrbitalParametersDrivers(),
                                      actualF.getOrbitalParametersDrivers());

        Assertions.assertEquals(expectedF.getFrame(), actualF.getFrame());
        Assertions.assertEquals(expectedF.getMu(), actualF.getMu());
        Assertions.assertEquals(expected.getAttitudeProvider(), actual.getAttitudeProvider());
        if (expectedF instanceof AbstractOrbitFactory<?>) {
            Assertions.assertEquals(expectedF.getOrbitType(), actualF.getOrbitType());
        }
        Assertions.assertEquals(expectedF.getPositionAngleType(), actualF.getPositionAngleType());
        Assertions.assertEquals(expectedF.getDate(), actualF.getDate());
        Assertions.assertEquals(expected.getAdditionalDerivativesProviders(), actual.getAdditionalDerivativesProviders());

        // Verify that the propagations give the same results
        AbsoluteDate targetEpoch = expected.getOrbitalParameterFactory().getDate().shiftedBy(7200.0);
        TimeStampedPVCoordinates expectedCoordinates = expected.buildPropagator().propagate(targetEpoch).getPVCoordinates();
        TimeStampedPVCoordinates actualCoordinates   = actual.buildPropagator().propagate(targetEpoch).getPVCoordinates();
        Assertions.assertEquals(0.0, Vector3D.distance(expectedCoordinates.getPosition(), actualCoordinates.getPosition()));
        Assertions.assertEquals(0.0, Vector3D.distance(expectedCoordinates.getVelocity(), actualCoordinates.getVelocity()));

    }
}

