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

import org.hipparchus.CalculusFieldElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

public class TLEPropagatorBuilderTest {

    @Test
    void testClone() {

        // Given
        final DataContext dataContext = Utils.setDataRoot("regular-data");
        final TLE tle = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, PositionAngleType.MEAN, 1.0, dataContext,
                new FixedPointTleGenerationAlgorithm());

        // When
        final TLEPropagatorBuilder copyBuilder = builder.clone();

        // Then
        assertPropagatorBuilderIsACopy(builder, copyBuilder);
        Assertions.assertEquals(builder.getTemplateTLE(), copyBuilder.getTemplateTLE());

    }

    /** Test for issue #1741.
     * <p>This test checks that orbital drivers in cloned propagator builders
     * ain't at the same physical address, i.e. that they're not linked anymore.</p>
     */
    @Test
    void testIssue1741() {

        // Given
        final DataContext dataContext = Utils.setDataRoot("regular-data");
        final TLE tle = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, PositionAngleType.MEAN, 1.0, dataContext,
                                                                      new FixedPointTleGenerationAlgorithm());
        final Orbit orbit = builder.createInitialOrbit();

        // When
        final TLEPropagatorBuilder copyBuilder = builder.clone();


        // Change orbit of the copied builder
        final TimeStampedPVCoordinates modifiedPv = orbit.shiftedBy(3600.).getPVCoordinates();
        copyBuilder.resetOrbit(new CartesianOrbit(modifiedPv, orbit.getFrame(), orbit.getDate(), orbit.getMu()));

        // Then
        // Original builder should still have original orbit
        final PVCoordinates originalPv = orbit.getPVCoordinates();
        final PVCoordinates initialPv = builder.createInitialOrbit().getPVCoordinates();
        final double dP = originalPv.getPosition().distance(initialPv.getPosition());
        final double dV = originalPv.getVelocity().distance(initialPv.getVelocity());
        final double dA = originalPv.getAcceleration().distance(initialPv.getAcceleration());
        Assertions.assertEquals(0., dP, 0.);
        Assertions.assertEquals(0., dV, 0.);
        Assertions.assertEquals(0., dA, 0.);
    }

    @Test
    void testConfiguredGenerationAlgorithmUsedForPropagatorReset() {

        // Given
        final DataContext dataContext = Utils.setDataRoot("regular-data");
        final TLE tle = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final CountingTleGenerationAlgorithm countingAlgorithm =
                new CountingTleGenerationAlgorithm(new FixedPointTleGenerationAlgorithm());
        final TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, PositionAngleType.MEAN, 1.0, dataContext,
                                                                       countingAlgorithm);

        // When
        final TLEPropagator propagator = builder.buildPropagator();
        final int callsAfterBuild = countingAlgorithm.getStateCalls();
        propagator.resetInitialState(propagator.getInitialState());

        // Then
        Assertions.assertTrue(callsAfterBuild > 0);
        Assertions.assertTrue(countingAlgorithm.getStateCalls() > callsAfterBuild);
    }

    /** Counting wrapper around a TLE generation algorithm. */
    private static class CountingTleGenerationAlgorithm implements TleGenerationAlgorithm {

        /** Delegate. */
        private final TleGenerationAlgorithm delegate;

        /** Number of state generations. */
        private int stateCalls;

        /** Number of field state generations. */
        private int fieldCalls;

        /** Constructor.
         * @param delegate delegate algorithm
         */
        CountingTleGenerationAlgorithm(final TleGenerationAlgorithm delegate) {
            this.delegate = delegate;
        }

        /** {@inheritDoc} */
        @Override
        public TLE generate(final SpacecraftState state, final TLE templateTLE) {
            stateCalls++;
            return delegate.generate(state, templateTLE);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldTLE<T> generate(final FieldSpacecraftState<T> state,
                                                                         final FieldTLE<T> templateTLE) {
            fieldCalls++;
            return delegate.generate(state, templateTLE);
        }

        /** Get the number of calls with regular states.
         * @return number of calls with regular states
         */
        int getStateCalls() {
            return stateCalls;
        }

        /** Get the number of calls with field states.
         * @return number of calls with field states
         */
        int getFieldCalls() {
            return fieldCalls;
        }
    }
}
