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
package org.orekit.control.indirect.shooting;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.control.indirect.shooting.propagation.AdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.ClassicalRungeKuttaIntegrationSettings;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

class AbstractIndirectShootingTest {

    @Test
    void testBuildPropagatorOrbit() {
        // GIVEN
        final TestShooting testShooting = new TestShooting(createSettings());
        final SpacecraftState state = new SpacecraftState(createOrbit());
        // WHEN
        final NumericalPropagator propagator = testShooting.buildPropagator(state);
        // THEN
        Assertions.assertEquals(OrbitType.CARTESIAN, propagator.getOrbitType());
        Assertions.assertEquals(state.getDate(), propagator.getInitialState().getDate());
        Assertions.assertEquals(state.getPosition(propagator.getFrame()), propagator.getInitialState().getPosition());
    }

    @Test
    void testBuildPropagator() {
        // GIVEN
        final TestShooting testShooting = new TestShooting(createSettings());
        final CartesianOrbit orbit = createOrbit();
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(orbit.getFrame(),
                orbit.getDate(), orbit.getPVCoordinates()));
        // WHEN
        final NumericalPropagator propagator = testShooting.buildPropagator(state);
        // THEN
        Assertions.assertEquals(state.getDate(), propagator.getInitialState().getDate());
        Assertions.assertEquals(state.getPosition(propagator.getFrame()), propagator.getInitialState().getPosition());
    }

    private ShootingPropagationSettings createSettings() {
        final List<ForceModel> forceModelList = new ArrayList<>();
        final ForceModel mockedForceModel = Mockito.mock(ForceModel.class);
        forceModelList.add(mockedForceModel);
        final AdjointDynamicsProvider adjointDynamicsProvider = Mockito.mock(AdjointDynamicsProvider.class);
        Mockito.when(adjointDynamicsProvider.buildAdditionalDerivativesProvider())
                .thenReturn(Mockito.mock(AdditionalDerivativesProvider.class));
        return new ShootingPropagationSettings(forceModelList, adjointDynamicsProvider,
                new ClassicalRungeKuttaIntegrationSettings(1.));
    }

    private static CartesianOrbit createOrbit() {
        return new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates(Vector3D.MINUS_J.scalarMultiply(1e6), Vector3D.MINUS_K.scalarMultiply(10))),
                FramesFactory.getGCRF(), Constants.EGM96_EARTH_MU);
    }

    private static class TestShooting extends AbstractIndirectShooting {

        public TestShooting(ShootingPropagationSettings propagationSettings) {
            super(propagationSettings);
        }

        @Override
        public ShootingBoundaryOutput solve(double initialMass, double[] initialGuess) {
            return null;
        }

        @Override
        protected <T extends CalculusFieldElement<T>> FieldOrdinaryDifferentialEquation<T> buildFieldODE(final FieldAbsoluteDate<T> date) {
            return null;
        }
    }

}
