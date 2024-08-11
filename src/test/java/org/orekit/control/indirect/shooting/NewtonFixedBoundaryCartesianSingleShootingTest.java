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
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.control.indirect.adjoint.CartesianAdjointJ2Term;
import org.orekit.control.indirect.adjoint.CartesianAdjointKeplerianTerm;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergy;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.boundary.NormBasedCartesianConditionChecker;
import org.orekit.control.indirect.shooting.propagation.AdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.CartesianAdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.ClassicalRungeKuttaIntegrationSettings;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.J2OnlyPerturbation;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.*;

import java.util.ArrayList;
import java.util.List;

class NewtonFixedBoundaryCartesianSingleShootingTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testUpdateAdjointZeroDefects() {
        // GIVEN
        final double[] originalAdjoint = new double[] { 1, 2, 3, 4, 5, 6 };
        final GradientField field = GradientField.getField(6);
        final TimeStampedPVCoordinates targetPV = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates(new Vector3D(10., 1., 0.1), new Vector3D(0.001, 0.1, -0.0001)));
        final TimeStampedFieldPVCoordinates<Gradient> fieldPVCoordinates = new TimeStampedFieldPVCoordinates<>(field,
                targetPV);
        final FieldCartesianOrbit<Gradient> fieldOrbit = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(fieldPVCoordinates.getPosition().add(getFieldVector3D(field, 0, 1, 2)),
                        fieldPVCoordinates.getVelocity().add(getFieldVector3D(field, 3, 4, 5))),
                FramesFactory.getGCRF(), new FieldAbsoluteDate<>(field, targetPV.getDate()), field.getOne());
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(fieldOrbit);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = Mockito.mock(NewtonFixedBoundaryCartesianSingleShooting.class);
        Mockito.when(shooting.updateAdjoint(originalAdjoint, fieldState)).thenCallRealMethod();
        Mockito.when(shooting.getTerminalCartesianState()).thenReturn(targetPV);
        // WHEN
        final double[] adjoint = shooting.updateAdjoint(originalAdjoint, fieldState);
        // THEN
        Assertions.assertArrayEquals(originalAdjoint, adjoint);
    }

    private static FieldVector3D<Gradient> getFieldVector3D(final GradientField field, final int i1,
                                                            final int i2, final int i3) {
        final int parametersNumber = field.getOne().getFreeParameters();
        return new FieldVector3D<>(Gradient.variable(parametersNumber, i1, 0),
                Gradient.variable(parametersNumber, i2, 0), Gradient.variable(parametersNumber, i3, 0));
    }

    @Test
    void testSolveOrbitVersusAbsolutePV() {
        // GIVEN
        final double tolerancePosition = 1e-0;
        final double toleranceVelocity = 1e-4;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, 1e-4);
        final Orbit initialOrbit = createInitialOrbit();
        final double timeOfFlight = 1e4;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final FixedTimeBoundaryOrbits boundaryOrbits = new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit);
        final ShootingPropagationSettings propagationSettings = createShootingSettings(initialOrbit);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        final double toleranceMassAdjoint = 1e-10;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1e3;
        final double[] guess = new double[] {0., 0., 0., 0.005307988954045267, 0.015505564884403, 0.010685627468070606};
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // THEN
        Assertions.assertTrue(output.isConverged());
        Assertions.assertNotEquals(0, output.getIterationCount());
        final PVCoordinates expectedPV = terminalOrbit.getPVCoordinates();
        final PVCoordinates actualPV = output.getTerminalState().getPVCoordinates();
        Assertions.assertEquals(0., expectedPV.getPosition().subtract(actualPV.getPosition()).getNorm(),
                tolerancePosition);
        Assertions.assertEquals(0., expectedPV.getVelocity().subtract(actualPV.getVelocity()).getNorm(),
                toleranceVelocity);
        compareToAbsolutePV(mass, guess, propagationSettings, boundaryOrbits, conditionChecker, toleranceMassAdjoint,
                output);
    }

    private static ShootingPropagationSettings createShootingSettings(final Orbit initialOrbit) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final Frame J2Frame = initialOrbit.getFrame(); // approximation for speed
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(initialOrbit.getMu(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, J2Frame);
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        forceModelList.add(j2OnlyPerturbation);
        final CartesianCost cartesianCost = new UnboundedCartesianEnergy("adjoint", 1e-3);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(j2OnlyPerturbation.getMu(), j2OnlyPerturbation.getrEq(),
                j2OnlyPerturbation.getJ2(initialOrbit.getDate()), initialOrbit.getFrame(), j2OnlyPerturbation.getFrame());
        final AdjointDynamicsProvider adjointDynamicsProvider = new CartesianAdjointDynamicsProvider(cartesianCost,
                keplerianTerm, j2Term);
        return new ShootingPropagationSettings(forceModelList, adjointDynamicsProvider,
                new ClassicalRungeKuttaIntegrationSettings(10.));
    }

    private static Orbit createInitialOrbit() {
        return new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 1e6, 1e-4, 1., 2., 3., 4., PositionAngleType.ECCENTRIC,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

    private static Orbit createTerminalBoundary(final Orbit initialOrbit, final double timeOfFlight) {
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final AbsoluteDate epoch = initialOrbit.getDate();
        return propagator.propagate(epoch.shiftedBy(timeOfFlight)).getOrbit();
    }

    private void compareToAbsolutePV(final double mass, final double[] guess,
                                     final ShootingPropagationSettings propagationSettings,
                                     final FixedTimeBoundaryOrbits boundaryOrbits,
                                     final CartesianBoundaryConditionChecker conditionChecker,
                                     final double toleranceMassAdjoint,
                                     final ShootingBoundaryOutput output) {
        final Orbit initialOrbit = boundaryOrbits.getInitialOrbit();
        final Orbit terminalOrbit = boundaryOrbits.getTerminalOrbit();
        final FixedTimeCartesianBoundaryStates boundaryStates = new FixedTimeCartesianBoundaryStates(convertToAbsolutePVCoordinates(initialOrbit),
                convertToAbsolutePVCoordinates(terminalOrbit));
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryStates, conditionChecker);
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final ShootingBoundaryOutput otherOutput = shooting.solve(mass, guess);
        Assertions.assertEquals(otherOutput.getIterationCount(), output.getIterationCount());
        Assertions.assertEquals(otherOutput.getTerminalState().getPosition(), otherOutput.getTerminalState().getPosition());
        final String adjointName = propagationSettings.getAdjointDerivativesProvider().getAdjointName();
        Assertions.assertArrayEquals(output.getInitialState().getAdditionalState(adjointName),
                otherOutput.getInitialState().getAdditionalState(adjointName));
    }

    private static AbsolutePVCoordinates convertToAbsolutePVCoordinates(final Orbit orbit) {
        return new AbsolutePVCoordinates(orbit.getFrame(), orbit.getDate(), orbit.getPVCoordinates());
    }
}
