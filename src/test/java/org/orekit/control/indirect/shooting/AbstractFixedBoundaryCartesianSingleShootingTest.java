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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.boundary.NormBasedCartesianConditionChecker;
import org.orekit.control.indirect.shooting.propagation.CartesianAdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.control.indirect.shooting.propagation.ClassicalRungeKuttaIntegrationSettings;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.stream.Stream;

class AbstractFixedBoundaryCartesianSingleShootingTest {

    @Test
    void testSolveZeroTimeOfFlight() {
        // GIVEN
        final String adjointName = "adjoint";
        final AbsolutePVCoordinates initialCondition = new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Vector3D.PLUS_I, Vector3D.MINUS_J);
        final FixedTimeCartesianBoundaryStates boundaryStates = new FixedTimeCartesianBoundaryStates(initialCondition,
                initialCondition);
        final CartesianCost cartesianCost = new UnboundedCartesianEnergyNeglectingMass(adjointName);
        final TestSingleShooting testSingleShooting = new TestSingleShooting(buildPropagationSettings(cartesianCost), boundaryStates,
                new NormBasedCartesianConditionChecker(10, 1, 1));
        final double[] initialGuess = new double[6];
        // WHEN
        final ShootingBoundaryOutput boundarySolution = testSingleShooting.solve(1., initialGuess);
        // THEN
        Assertions.assertArrayEquals(initialGuess, boundarySolution.getInitialState().getAdditionalState(adjointName));
    }

    private static ShootingPropagationSettings buildPropagationSettings(final CartesianCost cost) {
        final ClassicalRungeKuttaIntegrationSettings integrationSettings = new ClassicalRungeKuttaIntegrationSettings(10.);
        final CartesianAdjointDynamicsProvider derivativesProvider = new CartesianAdjointDynamicsProvider(cost);
        return new ShootingPropagationSettings(new ArrayList<>(), derivativesProvider, integrationSettings);
    }

    @Test
    void testBuildFieldPropagator() {
        // GIVEN
        final Field<Gradient> field = GradientField.getField(1);
        final FixedTimeBoundaryOrbits boundaryOrbits = createBoundaries();
        final CartesianCost mockedCost = mockCost(field);
        final TestSingleShooting testShooting = new TestSingleShooting(buildPropagationSettings(mockedCost),
                boundaryOrbits, null);
        final SpacecraftState state = new SpacecraftState(boundaryOrbits.getInitialOrbit());
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final FieldNumericalPropagator<Gradient> fieldPropagator = testShooting.buildFieldPropagator(fieldState);
        // THEN
        final NumericalPropagator propagator = testShooting.buildPropagator(state);
        final int actualSize = fieldPropagator.getEventsDetectors().size();
        Assertions.assertNotEquals(0, actualSize);
        Assertions.assertEquals(propagator.getEventsDetectors().size(), actualSize);
    }

    @SuppressWarnings("unchecked")
    private static CartesianCost mockCost(final Field<Gradient> field) {
        final CartesianCost mockedCost = Mockito.mock(CartesianCost.class);
        Mockito.when(mockedCost.getAdjointName()).thenReturn("");
        Mockito.when(mockedCost.getEventDetectors()).thenReturn(Stream.of(Mockito.mock(EventDetector.class)));
        Mockito.when(mockedCost.getFieldEventDetectors(field)).thenReturn(Stream.of(Mockito.mock(FieldEventDetector.class)));
        return mockedCost;
    }

    @Test
    void testSolveImpossibleTolerance() {
        // GIVEN
        final String adjointName = "adjoint";
        final double impossibleTolerance = 0.;
        final CartesianCost cartesianCost = new UnboundedCartesianEnergyNeglectingMass(adjointName);
        final TestSingleShooting testSingleShooting = new TestSingleShooting(buildPropagationSettings(cartesianCost),
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
