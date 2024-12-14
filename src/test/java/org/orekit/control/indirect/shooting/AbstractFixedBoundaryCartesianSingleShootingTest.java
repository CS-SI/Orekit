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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.boundary.NormBasedCartesianConditionChecker;
import org.orekit.control.indirect.shooting.propagation.AdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.CartesianAdjointDynamicsProviderFactory;
import org.orekit.control.indirect.shooting.propagation.ShootingIntegrationSettings;
import org.orekit.control.indirect.shooting.propagation.ShootingIntegrationSettingsFactory;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

class AbstractFixedBoundaryCartesianSingleShootingTest {

    @Test
    void testSolveZeroTimeOfFlight() {
        // GIVEN
        final String adjointName = "adjoint";
        final AbsolutePVCoordinates initialCondition = new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Vector3D.PLUS_I, Vector3D.MINUS_J);
        final FixedTimeCartesianBoundaryStates boundaryStates = new FixedTimeCartesianBoundaryStates(initialCondition,
                initialCondition);
        final TestSingleShooting testSingleShooting = new TestSingleShooting(buildPropagationSettings(), boundaryStates,
                new NormBasedCartesianConditionChecker(10, 1, 1));
        final double[] initialGuess = new double[6];
        // WHEN
        final ShootingBoundaryOutput boundarySolution = testSingleShooting.solve(1., initialGuess);
        // THEN
        Assertions.assertArrayEquals(initialGuess, boundarySolution.getInitialState().getAdditionalState(adjointName));
    }

    private static ShootingPropagationSettings buildPropagationSettings() {
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(10.);
        final AdjointDynamicsProvider derivativesProvider = CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProvider("adjoint",
                0.);
        return new ShootingPropagationSettings(new ArrayList<>(), derivativesProvider, integrationSettings);
    }

    @Test
    void testSolveImpossibleTolerance() {
        // GIVEN
        final double impossibleTolerance = 0.;
        final TestSingleShooting testSingleShooting = new TestSingleShooting(buildPropagationSettings(),
                createBoundaries(), new NormBasedCartesianConditionChecker(10,  impossibleTolerance, impossibleTolerance));
        final double[] initialGuess = new double[6];
        // WHEN
        final ShootingBoundaryOutput output = testSingleShooting.solve(1., initialGuess);
        // THEN
        Assertions.assertFalse(output.isConverged());
        Assertions.assertEquals(testSingleShooting.getPropagationSettings(), output.getShootingPropagationSettings());
    }

    private static FixedTimeBoundaryOrbits createBoundaries() {
        final Orbit initialCondition = new CartesianOrbit(new PVCoordinates(Vector3D.PLUS_I,
                Vector3D.MINUS_J), FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        return new FixedTimeBoundaryOrbits(initialCondition, initialCondition);
    }

    private static class TestSingleShooting extends AbstractFixedBoundaryCartesianSingleShooting {

        protected TestSingleShooting(ShootingPropagationSettings propagationSettings, FixedTimeCartesianBoundaryStates boundaryConditions, CartesianBoundaryConditionChecker convergenceChecker) {
            super(propagationSettings, boundaryConditions, convergenceChecker);
        }

        protected TestSingleShooting(ShootingPropagationSettings propagationSettings, FixedTimeBoundaryOrbits boundaryConditions, CartesianBoundaryConditionChecker convergenceChecker) {
            super(propagationSettings, boundaryConditions, convergenceChecker);
        }

        @Override
        protected double[] updateAdjoint(double[] originalInitialAdjoint, FieldSpacecraftState<Gradient> fieldTerminalState) {
            return originalInitialAdjoint;
        }
    }

}
