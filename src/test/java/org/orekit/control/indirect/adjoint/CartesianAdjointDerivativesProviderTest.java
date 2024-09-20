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
package org.orekit.control.indirect.adjoint;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.TestCost;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class CartesianAdjointDerivativesProviderTest {

    @Test
    void testInitException() {
        // GIVEN
        final String name = "name";
        final double mu = Constants.EGM96_EARTH_MU;
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(
                new UnboundedCartesianEnergyNeglectingMass(name), new CartesianAdjointKeplerianTerm(mu));
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.isOrbitDefined()).thenReturn(true);
        final Orbit mockedOrbit = Mockito.mock(Orbit.class);
        Mockito.when(mockedOrbit.getType()).thenReturn(OrbitType.EQUINOCTIAL);
        Mockito.when(mockedState.getOrbit()).thenReturn(mockedOrbit);
        // WHEN
        final Exception exception = Assertions.assertThrows(OrekitException.class,
                () -> derivativesProvider.init(mockedState, null));
        Assertions.assertEquals(OrekitMessages.WRONG_COORDINATES_FOR_ADJOINT_EQUATION.getSourceString(),
                exception.getMessage());
    }

    @Test
    void testIntegration() {
        // GIVEN
        final String name = "name";
        final double mu = Constants.EGM96_EARTH_MU;
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(
                new UnboundedCartesianEnergyNeglectingMass(name), new CartesianAdjointKeplerianTerm(mu));
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(100.));
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, mu);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setInitialState(new SpacecraftState(orbit).addAdditionalState(name, new double[6]));
        propagator.addAdditionalDerivativesProvider(derivativesProvider);
        // WHEN
        final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.));
        // THEN
        Assertions.assertTrue(propagator.isAdditionalStateManaged(name));
        final double[] finalAdjoint = state.getAdditionalState(name);
        Assertions.assertEquals(0, finalAdjoint[0]);
        Assertions.assertEquals(0, finalAdjoint[1]);
        Assertions.assertEquals(0, finalAdjoint[2]);
        Assertions.assertEquals(0, finalAdjoint[3]);
        Assertions.assertEquals(0, finalAdjoint[4]);
        Assertions.assertEquals(0, finalAdjoint[5]);
    }

    @Test
    void testGetCost() {
        // GIVEN
        final CartesianCost expectedCost = Mockito.mock(CartesianCost.class);
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(expectedCost);
        // WHEN
        final CartesianCost actualCost = derivativesProvider.getCost();
        // THEN
        Assertions.assertEquals(expectedCost, actualCost);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testEvaluateHamiltonian(final boolean withMassAdjoint) {
        // GIVEN
        final CartesianCost cost = new TestCost();
        final SpacecraftState state = getState(cost.getAdjointName(), withMassAdjoint);
        final CartesianAdjointEquationTerm mockedTerm = Mockito.mock(CartesianAdjointEquationTerm.class);
        final double[] cartesian = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(state.getOrbit(), null, cartesian, null);
        Mockito.when(mockedTerm.getHamiltonianContribution(state.getDate(), cartesian,
                        state.getAdditionalState(cost.getAdjointName()), state.getFrame())).thenReturn(0.);
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(cost,
                mockedTerm);
        // WHEN
        final double hamiltonian = derivativesProvider.evaluateHamiltonian(state);
        // THEN
        final Vector3D velocity = state.getPVCoordinates().getVelocity();
        Assertions.assertEquals(velocity.dotProduct(new Vector3D(1, 1, 1)), hamiltonian);
    }

    @Test
    void testCombinedDerivatives() {
        // GIVEN
        final CartesianCost cost = new TestCost();
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(cost);
        final SpacecraftState state = getState(derivativesProvider.getName(), false);
        // WHEN
        final CombinedDerivatives combinedDerivatives = derivativesProvider.combinedDerivatives(state);
        // THEN
        final double[] increment = combinedDerivatives.getMainStateDerivativesIncrements();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(0., increment[i]);
        }
        Assertions.assertEquals(1., increment[3]);
        Assertions.assertEquals(2., increment[4]);
        Assertions.assertEquals(3., increment[5]);
        Assertions.assertEquals(-10. * state.getMass() * new Vector3D(1., 2., 3).getNorm(), increment[6], 1e-10);
    }

    @Test
    void testCombinedDerivativesWithEquationTerm() {
        // GIVEN
        final CartesianCost cost = new TestCost();
        final CartesianAdjointEquationTerm equationTerm = new TestAdjointTerm();
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(cost, equationTerm);
        final SpacecraftState state = getState(derivativesProvider.getName(), false);
        // WHEN
        final CombinedDerivatives combinedDerivatives = derivativesProvider.combinedDerivatives(state);
        // THEN
        final double[] adjointDerivatives = combinedDerivatives.getAdditionalDerivatives();
        Assertions.assertEquals(1., adjointDerivatives[0]);
        Assertions.assertEquals(10., adjointDerivatives[1]);
        Assertions.assertEquals(100., adjointDerivatives[2]);
        Assertions.assertEquals(-1, adjointDerivatives[3]);
        Assertions.assertEquals(-1, adjointDerivatives[4]);
        Assertions.assertEquals(-1, adjointDerivatives[5]);
    }

    private static SpacecraftState getState(final String name, final boolean withMassAdjoint) {
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_K),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        final SpacecraftState stateWithoutAdditional = new SpacecraftState(orbit);
        final double[] adjoint = withMassAdjoint ? new double[7] : new double[6];
        for (int i = 0; i < 6; i++) {
            adjoint[i] = 1;
        }
        return stateWithoutAdditional.addAdditionalState(name, adjoint);
    }

    private static class TestAdjointTerm implements CartesianAdjointEquationTerm {

        @Override
        public double[] getRatesContribution(AbsoluteDate date, double[] stateVariables, double[] adjointVariables, Frame frame) {
            return new double[] { 1., 10., 100., 0., 0., 0. };
        }

        @Override
        public <T extends CalculusFieldElement<T>> T[] getFieldRatesContribution(FieldAbsoluteDate<T> date, T[] stateVariables, T[] adjointVariables, Frame frame) {
            return null;
        }

        @Override
        public double getHamiltonianContribution(AbsoluteDate date, double[] stateVariables, double[] adjointVariables, Frame frame) {
            return 0;
        }

        @Override
        public <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(FieldAbsoluteDate<T> date, T[] stateVariables, T[] adjointVariables, Frame frame) {
            return date.getField().getZero();
        }
    }

}
