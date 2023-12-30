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
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

class AbstractIntegratedPropagatorTest {

    @Test
    public void testGetResetAtEndTrue() {
        testGetResetAtEnd(true);
    }

    @Test
    public void testGetResetAtEndFalse() {
        testGetResetAtEnd(false);
    }

    private void testGetResetAtEnd(final boolean expectedResetAtEnd) {
        // GIVEN
        final TestAbstractIntegratedPropagator testAbstractIntegratedPropagator = new TestAbstractIntegratedPropagator();
        // WHEN
        testAbstractIntegratedPropagator.setResetAtEnd(expectedResetAtEnd);
        // THEN
        final boolean actualResetAtEnd = testAbstractIntegratedPropagator.getResetAtEnd();
        Assertions.assertEquals(expectedResetAtEnd, actualResetAtEnd);
    }
    
    /**
     * Test issue 1254: Wrong behavior of method "propagate(tStart, tEnd)" when used with "setResetAtEnd(false)".
     * <p>
     * Bug discovery and test are a courtesy of Christophe Le Bris.
     */
    @Test
    public void testIssue1254() {
        // GIVEN
        // GEO orbit
        final Orbit startOrbit = new EquinoctialOrbit(42165765.0, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.TRUE,
                                                      FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                      Constants.IERS2010_EARTH_MU);

        // Init numerical propagator
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(300.));
        propagator.setInitialState(new SpacecraftState(startOrbit));
        propagator.setResetAtEnd(false);

        // Produce ephemeris
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        final AbsoluteDate       minDate   = startOrbit.getDate().shiftedBy(-3600.0);
        final AbsoluteDate       maxDate   = startOrbit.getDate().shiftedBy(+3600.0);
        propagator.propagate(minDate, maxDate);

        // WHEN
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        // THEN
        Assertions.assertEquals(0., ephemeris.getMinDate().durationFrom(minDate), 0.);
        Assertions.assertEquals(0., ephemeris.getMaxDate().durationFrom(maxDate), 0.);
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