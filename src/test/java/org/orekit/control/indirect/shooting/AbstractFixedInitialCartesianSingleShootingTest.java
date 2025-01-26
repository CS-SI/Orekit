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
package org.orekit.control.indirect.shooting;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.CartesianAdjointKeplerianTerm;
import org.orekit.control.indirect.shooting.propagation.CartesianAdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.CartesianAdjointDynamicsProviderFactory;
import org.orekit.control.indirect.shooting.propagation.ShootingIntegrationSettings;
import org.orekit.control.indirect.shooting.propagation.ShootingIntegrationSettingsFactory;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

import java.util.ArrayList;
import java.util.List;

class AbstractFixedInitialCartesianSingleShootingTest {

    @Test
    void createFieldInitialStateWithMassAndAdjointTest() {
        // GIVEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(1);
        final ShootingPropagationSettings propagationSettings = new ShootingPropagationSettings(new ArrayList<>(),
                CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProviderNeglectingMass("adjoint"), integrationSettings);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final TestSingleShooting testSingleShooting = new TestSingleShooting(propagationSettings, state);
        final double expectedMass = 1.;
        final double[] expectedAdjoint = new double[1];
        // WHEN
        final FieldSpacecraftState<Gradient> fieldState = testSingleShooting.createFieldInitialStateWithMassAndAdjoint(expectedMass,
                expectedAdjoint);
        // THEN
        Assertions.assertEquals(state.getDate(), fieldState.getDate().toAbsoluteDate());
        Assertions.assertEquals(expectedMass, fieldState.getMass().getReal());
        final FieldVector3D<Gradient> fieldPosition = fieldState.getPosition();
        final FieldVector3D<Gradient> fieldVelocity = fieldState.getPVCoordinates().getVelocity();
        Assertions.assertEquals(state.getPosition(), fieldPosition.toVector3D());
        Assertions.assertEquals(state.getPVCoordinates().getVelocity(), fieldVelocity.toVector3D());
        Assertions.assertEquals(1., fieldState.getAdditionalState("adjoint")[0].getGradient()[0]);
    }

    @Test
    void buildPropagatorTest() {
        // GIVEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(1);
        final ShootingPropagationSettings propagationSettings = new ShootingPropagationSettings(new ArrayList<>(),
                CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProviderNeglectingMass("adjoint"), integrationSettings);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final TestSingleShooting testSingleShooting = new TestSingleShooting(propagationSettings, state);
        // WHEN
        final NumericalPropagator propagator = testSingleShooting.buildPropagator(state);
        // THEN
        Assertions.assertEquals(1, propagator.getMultiplexer().getHandlers().size());
    }

    @Test
    void buildFieldODETest() {
        // GIVEN
        final double mu = Constants.EGM96_EARTH_MU;
        final NewtonianAttraction attraction = new NewtonianAttraction(mu);
        final List<ForceModel> forces = new ArrayList<>();
        forces.add(attraction);
        final CartesianAdjointKeplerianTerm adjointKeplerianTerm = new CartesianAdjointKeplerianTerm(mu);
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(1);
        final String adjointName = "adjoint";
        final CartesianAdjointDynamicsProvider adjointDynamicsProvider = CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProviderNeglectingMass(adjointName,
                adjointKeplerianTerm);
        final ShootingPropagationSettings propagationSettings = new ShootingPropagationSettings(forces,
                adjointDynamicsProvider, integrationSettings);
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6};
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH))
                .addAdditionalState(adjointName, adjoint);
        final TestSingleShooting testSingleShooting = new TestSingleShooting(propagationSettings, state);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), state.getDate());
        // WHEN
        final FieldOrdinaryDifferentialEquation<Binary64> fieldODE = testSingleShooting.buildFieldODE(fieldDate);
        // THEN
        Assertions.assertEquals(7 + propagationSettings.getAdjointDynamicsProvider().getDimension(),
                fieldODE.getDimension());
        checkDerivatives(fieldODE, state, adjoint, attraction, adjointDynamicsProvider);
    }

    private static void checkDerivatives(final FieldOrdinaryDifferentialEquation<Binary64> fieldODE,
                                         final SpacecraftState state, final double[] adjoint,
                                         final NewtonianAttraction attraction,
                                         final CartesianAdjointDynamicsProvider adjointDynamicsProvider) {
        final Binary64[] fullState = MathArrays.buildArray(Binary64Field.getInstance(), fieldODE.getDimension());
        fullState[0] = new Binary64(state.getPosition().getX());
        fullState[1] = new Binary64(state.getPosition().getY());
        fullState[2] = new Binary64(state.getPosition().getZ());
        fullState[3] = new Binary64(state.getPVCoordinates().getVelocity().getX());
        fullState[4] = new Binary64(state.getPVCoordinates().getVelocity().getY());
        fullState[5] = new Binary64(state.getPVCoordinates().getVelocity().getZ());
        fullState[6] = new Binary64(state.getMass());
        for (int i = 0; i < adjoint.length; i++) {
            fullState[7 + i] = new Binary64(adjoint[i]);
        }
        final Binary64[] derivatives = fieldODE.computeDerivatives(Binary64.ZERO, fullState);
        Assertions.assertEquals(derivatives[0], fullState[3]);
        Assertions.assertEquals(derivatives[1], fullState[4]);
        Assertions.assertEquals(derivatives[2], fullState[5]);
        final double mu = attraction.getMu(AbsoluteDate.ARBITRARY_EPOCH);
        final Vector3D newtonianAcceleration = attraction.acceleration(state, new double[] {mu});
        final CartesianAdjointDerivativesProvider derivativesProvider = adjointDynamicsProvider.buildAdditionalDerivativesProvider();
        final CombinedDerivatives combinedDerivatives = derivativesProvider.combinedDerivatives(state);
        final double[] mainDerivativesIncrement = combinedDerivatives.getMainStateDerivativesIncrements();
        Assertions.assertEquals(derivatives[3].getReal(), newtonianAcceleration.getX() + mainDerivativesIncrement[3]);
        Assertions.assertEquals(derivatives[4].getReal(), newtonianAcceleration.getY() + mainDerivativesIncrement[4]);
        Assertions.assertEquals(derivatives[5].getReal(), newtonianAcceleration.getZ() + mainDerivativesIncrement[5]);
        Assertions.assertEquals(derivatives[6].getReal(), mainDerivativesIncrement[6], 1e-20);
        final double[] adjointDerivatives = combinedDerivatives.getAdditionalDerivatives();
        for (int i = 0; i < adjoint.length; i++) {
            Assertions.assertEquals(adjointDerivatives[i], derivatives[i + 7].getReal());
        }
    }

    private static class TestSingleShooting extends AbstractFixedInitialCartesianSingleShooting {

        protected TestSingleShooting(ShootingPropagationSettings propagationSettings,
                                     SpacecraftState initialSpacecraftStateTemplate) {
            super(propagationSettings, initialSpacecraftStateTemplate);
        }

        @Override
        public int getMaximumIterationCount() {
            return 0;
        }

        @Override
        public ShootingBoundaryOutput computeCandidateSolution(SpacecraftState initialState, int iterationCount) {
            return null;
        }

        @Override
        protected double[] updateShootingVariables(double[] originalShootingVariables, FieldSpacecraftState<Gradient> fieldTerminalState) {
            return new double[0];
        }

        @Override
        protected double[] getScales() {
            return new double[] {1., 1., 1., 1., 1., 1.};
        }
    }
}