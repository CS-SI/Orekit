/* Copyright 2022-2025 Romain Serra
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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergyNeglectingMass;
import org.orekit.control.indirect.adjoint.cost.TestCost;
import org.orekit.control.indirect.adjoint.cost.TestFieldCost;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class FieldCartesianAdjointDerivativesProviderTest {

    @Test
    @SuppressWarnings("unchecked")
    void testInitException() {
        // GIVEN
        final String name = "name";
        final double mu = Constants.EGM96_EARTH_MU;
        final FieldCartesianAdjointDerivativesProvider<Binary64> derivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(
                new FieldUnboundedCartesianEnergyNeglectingMass<>(name, Binary64Field.getInstance()), new CartesianAdjointKeplerianTerm(mu));
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.isOrbitDefined()).thenReturn(true);
        final FieldOrbit<Binary64> mockedOrbit = Mockito.mock(FieldOrbit.class);
        Mockito.when(mockedOrbit.getType()).thenReturn(OrbitType.EQUINOCTIAL);
        Mockito.when(mockedState.getOrbit()).thenReturn(mockedOrbit);
        // WHEN
        Assertions.assertThrows(OrekitException.class, () -> derivativesProvider.init(mockedState, null));
    }

    @Test
    void testIntegration() {
        // GIVEN
        final String name = "name";
        final double mu = Constants.EGM96_EARTH_MU;
        final Binary64Field field = Binary64Field.getInstance();
        final FieldCartesianAdjointDerivativesProvider<Binary64> derivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(
                new FieldUnboundedCartesianEnergyNeglectingMass<>(name, field), new CartesianAdjointKeplerianTerm(mu));
        final ClassicalRungeKuttaFieldIntegrator<Binary64> integrator = new ClassicalRungeKuttaFieldIntegrator<>(field,
                Binary64.ONE.multiply(100.));
        final FieldNumericalPropagator<Binary64> propagator = new FieldNumericalPropagator<>(field, integrator);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, mu);
        final FieldSpacecraftState<Binary64> initialState = new FieldSpacecraftState<>(field, new SpacecraftState(orbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setInitialState(initialState.addAdditionalState(name, MathArrays.buildArray(field, 6)));
        propagator.addAdditionalDerivativesProvider(derivativesProvider);
        // WHEN
        final FieldSpacecraftState<Binary64> terminalState = propagator.propagate(initialState.getDate().shiftedBy(1000.));
        // THEN
        Assertions.assertTrue(propagator.isAdditionalStateManaged(name));
        final Binary64[] adjoint = terminalState.getAdditionalState(name);
        Assertions.assertEquals(0., adjoint[0].getReal());
        Assertions.assertEquals(0., adjoint[1].getReal());
        Assertions.assertEquals(0., adjoint[2].getReal());
        Assertions.assertEquals(0., adjoint[3].getReal());
        Assertions.assertEquals(0., adjoint[4].getReal());
        Assertions.assertEquals(0., adjoint[5].getReal());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testEvaluateHamiltonian(final boolean withMassAdjoint) {
        // GIVEN
        final TestFieldCost cost = new TestFieldCost();
        final double mu = 1e-3;
        final FieldCartesianAdjointDerivativesProvider<Binary64> derivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(cost,
                new CartesianAdjointKeplerianTerm(mu));
        final FieldSpacecraftState<Binary64> state = getState(derivativesProvider.getName(), withMassAdjoint);
        // WHEN
        final Binary64 hamiltonian = derivativesProvider.evaluateHamiltonian(state);
        // THEN
        final FieldVector3D<Binary64> velocity = state.getPVCoordinates().getVelocity();
        final FieldVector3D<Binary64> vector = new FieldVector3D<>(Binary64.ONE, Binary64.ONE, Binary64.ONE);
        Assertions.assertEquals(velocity.dotProduct(vector).add(mu), hamiltonian);
    }

    @Test
    void testCombinedDerivatives() {
        // GIVEN
        final TestFieldCost cost = new TestFieldCost();
        final FieldCartesianAdjointDerivativesProvider<Binary64> derivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(
                cost);
        final FieldSpacecraftState<Binary64> state = getState(derivativesProvider.getName(), false);
        // WHEN
        final FieldCombinedDerivatives<Binary64> combinedDerivatives = derivativesProvider.combinedDerivatives(state);
        // THEN
        final Binary64[] increment = combinedDerivatives.getMainStateDerivativesIncrements();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(0., increment[i].getReal());
        }
        Assertions.assertEquals(1., increment[3].getReal());
        Assertions.assertEquals(2., increment[4].getReal());
        Assertions.assertEquals(3., increment[5].getReal());
        Assertions.assertEquals(-10. * state.getMass().getReal() * new Vector3D(1., 2., 3).getNorm(),
                increment[6].getReal(), 1e-10);
    }

    private static FieldSpacecraftState<Binary64> getState(final String name, final boolean withMassAdjoint) {
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_K),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        final FieldSpacecraftState<Binary64> stateWithoutAdditional = new FieldSpacecraftState<>(new FieldCartesianOrbit<>(Binary64Field.getInstance(), orbit));
        final Binary64[] adjoint = MathArrays.buildArray(stateWithoutAdditional.getDate().getField(),
                withMassAdjoint ? 7 : 6);
        for (int i = 0; i < 6; i++) {
            adjoint[i] = Binary64.ONE;
        }
        return stateWithoutAdditional.addAdditionalState(name, adjoint);
    }
}
