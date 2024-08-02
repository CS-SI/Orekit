/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldAbstractIntegratedPropagatorTest {

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
        final TestFieldAbstractIntegratedPropagator testAbstractIntegratedPropagator = new TestFieldAbstractIntegratedPropagator();
        // WHEN
        testAbstractIntegratedPropagator.setResetAtEnd(expectedResetAtEnd);
        // THEN
        final boolean actualResetAtEnd = testAbstractIntegratedPropagator.getResetAtEnd();
        assertEquals(expectedResetAtEnd, actualResetAtEnd);
    }
    
    /** Test issue 1461.
     * <p>Test for the new generic method AbstractIntegratedPropagator.reset(SpacecraftState, PropagationType)
     */
    @Test
    void testIssue1461() {
        doTestIssue1461(Binary64Field.getInstance());
    }
    
    /** Method for running test for issue 1461. */
    private <T extends CalculusFieldElement<T>> void doTestIssue1461(Field<T> field) {
        // GIVEN
        
        final T zero = field.getZero();
        // GEO orbit
        final FieldOrbit<T> startOrbit = new FieldEquinoctialOrbit<>(field,
                        new EquinoctialOrbit(42165765.0, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.TRUE,
                                             FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                             Constants.IERS2010_EARTH_MU));

        // Init numerical propagator
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(startOrbit);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field,
                        new ClassicalRungeKuttaFieldIntegrator<T>(field, zero.newInstance(300.)));
        propagator.setInitialState(state);

        // WHEN
        propagator.resetInitialState(state, PropagationType.OSCULATING);
        final FieldSpacecraftState<T> oscState = propagator.getInitialState();
        
        propagator.resetInitialState(state, PropagationType.MEAN);
        final FieldSpacecraftState<T> meanState = propagator.getInitialState();

        // THEN
        
        // Check that all three states are identical
        final double dpOsc = oscState.getPosition().distance(state.getPosition()).getReal();
        final double dvOsc = oscState.getPVCoordinates().getVelocity().distance(state.getPVCoordinates().getVelocity()).getReal();
        
        final double dpMean = meanState.getPosition().distance(state.getPosition()).getReal();
        final double dvMean = meanState.getPVCoordinates().getVelocity().distance(state.getPVCoordinates().getVelocity()).getReal();
        
        assertEquals(0., dpOsc, 0.);
        assertEquals(0., dvOsc, 0.);
        assertEquals(0., dpMean, 0.);
        assertEquals(0., dvMean, 0.);
    }

    private static class TestFieldAbstractIntegratedPropagator extends FieldAbstractIntegratedPropagator<Complex> {

        @SuppressWarnings("unchecked")
        protected TestFieldAbstractIntegratedPropagator() {
            super(ComplexField.getInstance(), Mockito.mock(FieldODEIntegrator.class), PropagationType.OSCULATING);
        }

        @Override
        protected FieldStateMapper<Complex> createMapper(FieldAbsoluteDate<Complex> referenceDate, Complex mu,
                                                         OrbitType orbitType, PositionAngleType positionAngleType,
                                                         AttitudeProvider attitudeProvider, Frame frame) {
            return null;
        }

        @Override
        protected MainStateEquations<Complex> getMainStateEquations(FieldODEIntegrator<Complex> integ) {
            return null;
        }
    }

}
