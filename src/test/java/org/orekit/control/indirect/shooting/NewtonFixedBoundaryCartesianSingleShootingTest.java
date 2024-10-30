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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.control.indirect.adjoint.CartesianAdjointJ2Term;
import org.orekit.control.indirect.adjoint.CartesianAdjointKeplerianTerm;
import org.orekit.control.indirect.adjoint.cost.BoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.boundary.NormBasedCartesianConditionChecker;
import org.orekit.control.indirect.shooting.propagation.*;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.J2OnlyPerturbation;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.CartesianToleranceProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
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
        final double one = 1;
        Mockito.when(shooting.getScalePositionDefects()).thenReturn(one);
        Mockito.when(shooting.getScaleVelocityDefects()).thenReturn(one);
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

    @ParameterizedTest
    @ValueSource(doubles = {5e5, 8e5, 1.5e6})
    void testSolveOrbitVersusAbsolutePV(final double approximateAltitude) {
        // GIVEN
        final double tolerancePosition = 1e0;
        final double toleranceVelocity = 1e-4;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final Orbit initialOrbit = createSomeInitialOrbit(approximateAltitude);
        final double timeOfFlight = 1e4;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final FixedTimeBoundaryOrbits boundaryOrbits = new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit);
        final CartesianCost cartesianCost = new UnboundedCartesianEnergyNeglectingMass("adjoint");
        final ShootingPropagationSettings propagationSettings = createShootingSettings(initialOrbit, cartesianCost,
                new ClassicalRungeKuttaIntegrationSettings(60.));
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setScalePositionDefects(1.);
        shooting.setScaleVelocityDefects(1.);
        final double mass = 1e3;
        final double[] guess = new double[6];
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
        compareToAbsolutePV(mass, guess, propagationSettings, boundaryOrbits, conditionChecker, 0.,
                output);
    }

    private static ShootingPropagationSettings createShootingSettings(final Orbit initialOrbit,
                                                                      final CartesianCost cartesianCost,
                                                                      final ShootingIntegrationSettings integrationSettings) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final Frame J2Frame = initialOrbit.getFrame(); // approximation for speed
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(initialOrbit.getMu(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, J2Frame);
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        forceModelList.add(j2OnlyPerturbation);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(j2OnlyPerturbation.getMu(), j2OnlyPerturbation.getrEq(),
                j2OnlyPerturbation.getJ2(initialOrbit.getDate()), j2OnlyPerturbation.getFrame());
        final AdjointDynamicsProvider adjointDynamicsProvider = new CartesianAdjointDynamicsProvider(cartesianCost,
                keplerianTerm, j2Term);
        return new ShootingPropagationSettings(forceModelList, adjointDynamicsProvider, integrationSettings);
    }

    private static Orbit createSomeInitialOrbit(final double approximateAltitude) {
        return new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + approximateAltitude, 1e-4, 1., 2., 3., 4., PositionAngleType.ECCENTRIC,
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
        final String adjointName = propagationSettings.getAdjointDynamicsProvider().getAdjointName();
        Assertions.assertArrayEquals(output.getInitialState().getAdditionalState(adjointName),
                otherOutput.getInitialState().getAdditionalState(adjointName));
    }

    private static AbsolutePVCoordinates convertToAbsolutePVCoordinates(final Orbit orbit) {
        return new AbsolutePVCoordinates(orbit.getFrame(), orbit.getDate(), orbit.getPVCoordinates());
    }

    @Test
    void testSolveSequential() {
        // GIVEN
        final double tolerancePosition = 1e-0;
        final double toleranceVelocity = 1e-4;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final Orbit initialOrbit = createSomeInitialOrbit(1e6);
        final double timeOfFlight = 1e4;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final FixedTimeBoundaryOrbits boundaryOrbits = new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit);
        final double flowRateFactor = 1e-3;
        final CartesianCost cartesianCost = new UnboundedCartesianEnergy("adjoint", flowRateFactor);
        final ShootingPropagationSettings propagationSettings = createShootingSettings(initialOrbit, cartesianCost,
                new DormandPrince54IntegrationSettings(1e-1, 1e2,
                        ToleranceProvider.of(CartesianToleranceProvider.of(1e-3, 1e-6, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE))));
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        final double toleranceMassAdjoint = 1e-10;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1e3;
        final double[] guess = new double[]{-2.305656141544546E-6, -6.050107349447073E-6, -4.484389270662034E-6,
                -9.635757291472267E-4, -0.0026076008704216066, 8.621848929368622E-5, 0.};
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // THEN
        final double thrustBound = 1e5;
        final ShootingPropagationSettings propagationSettingsBoundedEnergy = createShootingSettings(initialOrbit,
                new BoundedCartesianEnergy(cartesianCost.getAdjointName(), flowRateFactor, thrustBound),
                propagationSettings.getIntegrationSettings());
        final NewtonFixedBoundaryCartesianSingleShooting shootingBoundedEnergy = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettingsBoundedEnergy,
                boundaryOrbits, conditionChecker);
        final double[] unboundedEnergyAdjoint = output.getInitialState().getAdditionalState(cartesianCost.getAdjointName());
        double[] guessBoundedEnergy = unboundedEnergyAdjoint.clone();
        final ShootingBoundaryOutput outputBoundedEnergy = shootingBoundedEnergy.solve(mass, guessBoundedEnergy);
        Assertions.assertTrue(outputBoundedEnergy.isConverged());
        Assertions.assertEquals(0, outputBoundedEnergy.getIterationCount());
    }

    @Test
    void testSolveRegression() {
        // GIVEN
        final double massFlowRateFactor = 2e-6;
        final CartesianCost cartesianCost = new UnboundedCartesianEnergy("adjoint", massFlowRateFactor);
        final Orbit initialOrbit = createSomeInitialOrbit(1e6);
        final double timeOfFlight = initialOrbit.getKeplerianPeriod() * 5;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getShootingMethod(cartesianCost,
                new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit),
                new ClassicalRungeKuttaIntegrationSettings(100.));
        final double toleranceMassAdjoint = 1e-10;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1.;
        final double[] guess = {-1.3440754763650783E-6, -6.346307866897998E-6, -4.25736594074492E-6,
                -4.54324936872417E-4, -0.0020329894350755227, -8.358161689612435E-4, 0.};
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // THEN
        Assertions.assertTrue(output.isConverged());
        final double[] initialAdjoint = output.getInitialState().getAdditionalState(cartesianCost.getAdjointName());
        final double[] expectedAdjoint = new double[] {-1.3432883741256684E-6, -6.343244627959342E-6, -4.2552646864846415E-6,
                -4.540374638007354E-4, -0.002031906384904598, -8.355018662664441E-4, -1.0320210230861449};
        final double tolerance = 1e-8;
        for (int i = 0; i < expectedAdjoint.length; i++) {
            Assertions.assertEquals(expectedAdjoint[i], initialAdjoint[i], tolerance);
        }
        Assertions.assertNotEquals(1., output.getTerminalState().getMass());
    }

    private static NewtonFixedBoundaryCartesianSingleShooting getShootingMethod(final CartesianCost cartesianCost,
                                                                                final FixedTimeBoundaryOrbits fixedTimeBoundaryOrbits,
                                                                                final ShootingIntegrationSettings integrationSettings) {
        final double tolerancePosition = 1e-0;
        final double toleranceVelocity = 1e-4;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final FixedTimeBoundaryOrbits boundaryOrbits = new FixedTimeBoundaryOrbits(fixedTimeBoundaryOrbits.getInitialOrbit(),
                fixedTimeBoundaryOrbits.getTerminalOrbit());
        final ShootingPropagationSettings propagationSettings = createShootingSettings(fixedTimeBoundaryOrbits.getInitialOrbit(),
                cartesianCost, integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setScalePositionDefects(1e3);
        shooting.setScaleVelocityDefects(1.);
        return shooting;
    }

    @Test
    void testSolveForwardBackward() {
        // GIVEN
        final CartesianCost cartesianCost = new UnboundedCartesianEnergyNeglectingMass("adjoint");
        final Orbit initialOrbit = createGeoInitialOrbit();
        final double timeOfFlight = initialOrbit.getKeplerianPeriod() * 3;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final ShootingIntegrationSettings integrationSettings = new ClassicalRungeKuttaIntegrationSettings(100.);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getShootingMethod(cartesianCost,
                new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit), integrationSettings);
        final double toleranceMassAdjoint = 1e-10;
        final double initialMass = 3e3;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double[] guess = new double[] {-1.429146468892837E-10, 4.5022870335769276E-11, -1.318194179536703E-12,
                -1.098381235039422E-6, -1.6798876678906052E-6, -3.207856651454041E-9};
        // WHEN
        final ShootingBoundaryOutput forwardOutput = shooting.solve(initialMass, guess);
        // THEN
        final SpacecraftState terminalState = forwardOutput.getTerminalState();
        final String adjointName = cartesianCost.getAdjointName();
        final double[] terminalAdjointForward = terminalState.getAdditionalState(adjointName);
        final NewtonFixedBoundaryCartesianSingleShooting backwardShooting = getShootingMethod(cartesianCost,
                new FixedTimeBoundaryOrbits(terminalOrbit, initialOrbit), integrationSettings);
        final ShootingBoundaryOutput backwardOutput = backwardShooting.solve(terminalState.getMass(), terminalAdjointForward);
        Assertions.assertTrue(backwardOutput.isConverged());
        Assertions.assertEquals(0, backwardOutput.getIterationCount());
        final double[] initialAdjointForward = forwardOutput.getInitialState().getAdditionalState(adjointName);
        final double[] terminalAdjointBackward = backwardOutput.getTerminalState().getAdditionalState(adjointName);
        for (int i = 0; i < initialAdjointForward.length; i++) {
            Assertions.assertEquals(initialAdjointForward[i], terminalAdjointBackward[i], 1e-13);
        }
    }

    private static Orbit createGeoInitialOrbit() {
        return new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 35000e3, 1e-5, 0.001, 2., 3., 4., PositionAngleType.ECCENTRIC,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-4, 1e-3, 1e-2})
    void testSolveUnboundedCartesianEnergy(final double flowRateFactor) {
        // GIVEN
        final double tolerancePosition = 1e1;
        final double toleranceVelocity = 1e-3;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final FixedTimeBoundaryOrbits boundaryOrbits = createBoundaryForKeplerianSettings();
        final CartesianCost cartesianCost = new UnboundedCartesianEnergy("adjoint", flowRateFactor);
        final DormandPrince54IntegrationSettings integrationSettings = new DormandPrince54IntegrationSettings(1e-2, 2e2,
                ToleranceProvider.of(CartesianToleranceProvider.of(1e-3, 1e-6, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE)));
        final ShootingPropagationSettings propagationSettings = createKeplerianShootingSettings(boundaryOrbits.getInitialOrbit(),
                cartesianCost, integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        final double toleranceMassAdjoint = 1e-8;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1e3;
        final double[] guess = guessWithoutMass(cartesianCost.getAdjointName(), mass, integrationSettings, boundaryOrbits,
                conditionChecker);
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // THEN
        Assertions.assertTrue(output.isConverged());
    }

    private static FixedTimeBoundaryOrbits createBoundaryForKeplerianSettings() {
        final Orbit initialOrbit = createSomeInitialOrbit(2e6);
        final PVCoordinates templatePV = initialOrbit.getPVCoordinates();
        final Orbit modifiedOrbit = new CartesianOrbit(new PVCoordinates(templatePV.getPosition().add(new Vector3D(1e3, 2e3, 3e3)), templatePV.getVelocity()),
                initialOrbit.getFrame(), initialOrbit.getDate(), initialOrbit.getMu());
        final double timeOfFlight = 1e5;
        final Orbit terminalOrbit = createTerminalBoundary(modifiedOrbit, timeOfFlight);
        return new FixedTimeBoundaryOrbits(modifiedOrbit, terminalOrbit);
    }

    private static ShootingPropagationSettings createKeplerianShootingSettings(final Orbit initialOrbit,
                                                                               final CartesianCost cartesianCost,
                                                                               final ShootingIntegrationSettings integrationSettings) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final AdjointDynamicsProvider adjointDynamicsProvider = new CartesianAdjointDynamicsProvider(cartesianCost,
                keplerianTerm);
        return new ShootingPropagationSettings(forceModelList, adjointDynamicsProvider, integrationSettings);
    }

    private static double[] guessWithoutMass(final String adjointName, final double mass,
                                             final ShootingIntegrationSettings integrationSettings,
                                             final FixedTimeBoundaryOrbits boundaryOrbits,
                                             final CartesianBoundaryConditionChecker conditionChecker) {
        final ShootingPropagationSettings propagationSettings = createKeplerianShootingSettings(boundaryOrbits.getInitialOrbit(),
                new UnboundedCartesianEnergyNeglectingMass(adjointName), integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        final ShootingBoundaryOutput output = shooting.solve(mass, new double[6]);
        final double squaredMass = mass * mass;
        final double[] adjoint = output.getInitialState().getAdditionalState(adjointName);
        final double[] adjointWithMass = new double[7];
        for (int i = 0; i < adjoint.length; i++) {
            adjointWithMass[i] = adjoint[i] * squaredMass;
        }
        adjointWithMass[adjointWithMass.length - 1] = -1.;
        return adjointWithMass;
    }

    @Test
    void testSolveHeliocentric() {
        // GIVEN
        final double tolerancePosition = 1e5;
        final double toleranceVelocity = 1e0;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final FixedTimeBoundaryOrbits boundaryOrbits = getHeliocentricBoundary();
        final DormandPrince54IntegrationSettings integrationSettings = new DormandPrince54IntegrationSettings(2e2, 1e5,
                ToleranceProvider.of(CartesianToleranceProvider.of(1e5, 1e-1, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE)));
        final EventDetectionSettings detectionSettings = new EventDetectionSettings(1e5, 1e3, EventDetectionSettings.DEFAULT_MAX_ITER);
        final CartesianCost cartesianCost = new UnboundedCartesianEnergy("adjoint", 1. / (4000. * Constants.G0_STANDARD_GRAVITY),
                detectionSettings);
        final ShootingPropagationSettings propagationSettings = createKeplerianShootingSettings(boundaryOrbits.getInitialOrbit(),
                cartesianCost, integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        final double toleranceMassAdjoint = 1e-7;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 2e3;
        final double[] guess = guessWithoutMass(cartesianCost.getAdjointName(), mass, integrationSettings, boundaryOrbits,
                conditionChecker);
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // THEN
        Assertions.assertTrue(output.isConverged());
    }

    private static FixedTimeBoundaryOrbits getHeliocentricBoundary() {
        final double mu = Constants.JPL_SSD_SUN_GM;
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2035, 1, 1, 0, 0, 0),
                TimeScalesFactory.getUTC());
        final Vector3D position = new Vector3D(269630575634.1845, -317928797663.87445, -117503661424.1842);
        final Vector3D velocity = new Vector3D(12803.992418160833, 12346.009014593829, 2789.3378661767967);
        final Orbit initialOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(date, position, velocity), frame, mu);
        final AbsoluteDate terminalDate = new AbsoluteDate(new DateTimeComponents(2038, 4, 20, 7, 48, 0),
                TimeScalesFactory.getUTC());
        final Vector3D terminalPosition = new Vector3D(-254040098474.26975, 292309940514.6629, 61765199864.609174);
        final Vector3D terminalVelocity = new Vector3D(-15342.352873059252, -10427.635262141607, -7365.033285214819);
        final Orbit terminalOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(terminalDate, terminalPosition, terminalVelocity), frame, mu);
        return new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit);
    }
}
