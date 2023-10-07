/* Copyright 2022-2023 Romain Serra
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
package org.orekit.propagation.integration;

import org.hipparchus.ode.ODEIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.time.AbsoluteDate;

class AbstractIntegratedPropagatorTest {

    @Test
    void testGetResetAtEndTrue() {
        testGetResetAtEnd(true);
    }

    @Test
    void testGetResetAtEndFalse() {
        testGetResetAtEnd(false);
    }

    void testGetResetAtEnd(final boolean expectedResetAtEnd) {
        // GIVEN
        final TestAbstractIntegratedPropagator testAbstractIntegratedPropagator = new TestAbstractIntegratedPropagator();
        // WHEN
        testAbstractIntegratedPropagator.setResetAtEnd(expectedResetAtEnd);
        // THEN
        final boolean actualResetAtEnd = testAbstractIntegratedPropagator.getResetAtEnd();
        Assertions.assertEquals(expectedResetAtEnd, actualResetAtEnd);
    }

    private static class TestAbstractIntegratedPropagator extends AbstractIntegratedPropagator {

        protected TestAbstractIntegratedPropagator() {
            super(Mockito.mock(ODEIntegrator.class), PropagationType.OSCULATING);
        }

        @Override
        protected StateMapper createMapper(AbsoluteDate referenceDate, double mu, OrbitType orbitType, PositionAngleType positionAngleType, AttitudeProvider attitudeProvider, Frame frame) {
            return null;
        }

        @Override
        protected MainStateEquations getMainStateEquations(ODEIntegrator integ) {
            return null;
        }
    }

}