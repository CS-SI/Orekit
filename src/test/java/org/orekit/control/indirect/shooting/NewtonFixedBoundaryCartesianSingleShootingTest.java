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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.CartesianAdjointEquationTerm;
import org.orekit.control.indirect.adjoint.CartesianAdjointJ2Term;
import org.orekit.control.indirect.adjoint.CartesianAdjointKeplerianTerm;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianCost;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergyNeglectingMass;
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
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class NewtonFixedBoundaryCartesianSingleShootingTest {

    private static final double THRESHOLD_LU_DECOMPOSITION = 1e-11;
    private static final String ADJOINT_NAME = "adjoint";

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
        Mockito.when(shooting.getScales()).thenReturn(new double[] {1, 1, 1, 1, 1, 1});
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
        final ShootingPropagationSettings propagationSettings = createShootingSettings(initialOrbit, 0., Double.POSITIVE_INFINITY,
                ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(60.));
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
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

    private static ShootingPropagationSettings createShootingSettings(final Orbit initialOrbit, final double massFlowRate,
                                                                      final double maximumThrustMagnitude,
                                                                      final ShootingIntegrationSettings integrationSettings) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final Frame j2Frame = initialOrbit.getFrame(); // approximation for speed
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(initialOrbit.getMu(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, j2Frame);
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        forceModelList.add(j2OnlyPerturbation);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(j2OnlyPerturbation.getMu(), j2OnlyPerturbation.getrEq(),
                j2OnlyPerturbation.getJ2(initialOrbit.getDate()), j2OnlyPerturbation.getFrame());
        return new ShootingPropagationSettings(forceModelList, getAdjointDynamicsProvider(massFlowRate,
                maximumThrustMagnitude, keplerianTerm, j2Term), integrationSettings);
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
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
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
        final ShootingPropagationSettings propagationSettings = createShootingSettings(initialOrbit, flowRateFactor, Double.POSITIVE_INFINITY,
                ShootingIntegrationSettingsFactory.getDormandPrince54IntegratorSettings(1e-1, 1e2,
                        ToleranceProvider.of(CartesianToleranceProvider.of(1e-3, 1e-6, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE))));
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
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
                flowRateFactor, thrustBound, propagationSettings.getIntegrationSettings());
        final NewtonFixedBoundaryCartesianSingleShooting shootingBoundedEnergy = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettingsBoundedEnergy,
                boundaryOrbits, conditionChecker);
        shootingBoundedEnergy.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double[] unboundedEnergyAdjoint = output.getInitialState().getAdditionalState(ADJOINT_NAME);
        double[] guessBoundedEnergy = unboundedEnergyAdjoint.clone();
        final ShootingBoundaryOutput outputBoundedEnergy = shootingBoundedEnergy.solve(mass, guessBoundedEnergy);
        Assertions.assertTrue(outputBoundedEnergy.isConverged());
        Assertions.assertEquals(0, outputBoundedEnergy.getIterationCount());
    }

    @Test
    void testSolveRegression() {
        // GIVEN
        final double massFlowRateFactor = 2e-6;
        final Orbit initialOrbit = createSomeInitialOrbit(1e6);
        final double timeOfFlight = initialOrbit.getKeplerianPeriod() * 5;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getShootingMethod(massFlowRateFactor,
                new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit),
                ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(100.));
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double toleranceMassAdjoint = 1e-10;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1.;
        final double[] guess = {-1.3440754763650783E-6, -6.346307866897998E-6, -4.25736594074492E-6,
                -4.54324936872417E-4, -0.0020329894350755227, -8.358161689612435E-4, 0.};
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // THEN
        Assertions.assertTrue(output.isConverged());
        final double[] initialAdjoint = output.getInitialState().getAdditionalState(shooting.getPropagationSettings()
                .getAdjointDynamicsProvider().getAdjointName());
        final double[] expectedAdjoint = new double[] {-1.3432883741256684E-6, -6.343244627959342E-6, -4.2552646864846415E-6,
                -4.540374638007354E-4, -0.002031906384904598, -8.355018662664441E-4, -1.0320210230861449};
        final double tolerance = 1e-8;
        for (int i = 0; i < expectedAdjoint.length; i++) {
            Assertions.assertEquals(expectedAdjoint[i], initialAdjoint[i], tolerance);
        }
        Assertions.assertNotEquals(1., output.getTerminalState().getMass());
    }

    private static NewtonFixedBoundaryCartesianSingleShooting getShootingMethod(final double massFlowRateFactor,
                                                                                final FixedTimeBoundaryOrbits fixedTimeBoundaryOrbits,
                                                                                final ShootingIntegrationSettings integrationSettings) {
        final double tolerancePosition = 1e-0;
        final double toleranceVelocity = 1e-4;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final FixedTimeBoundaryOrbits boundaryOrbits = new FixedTimeBoundaryOrbits(fixedTimeBoundaryOrbits.getInitialOrbit(),
                fixedTimeBoundaryOrbits.getTerminalOrbit());
        final ShootingPropagationSettings propagationSettings = createShootingSettings(fixedTimeBoundaryOrbits.getInitialOrbit(),
                massFlowRateFactor, Double.POSITIVE_INFINITY, integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        shooting.setScalePositionDefects(1e3);
        shooting.setScaleVelocityDefects(1.);
        return shooting;
    }

    @Test
    void testSolveForwardBackward() {
        // GIVEN
        final Orbit initialOrbit = createGeoInitialOrbit();
        final double timeOfFlight = initialOrbit.getKeplerianPeriod() * 3;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getClassicalRungeKuttaIntegratorSettings(100.);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getShootingMethod(0.,
                new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit), integrationSettings);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double toleranceMassAdjoint = 1e-10;
        final double initialMass = 3e3;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double[] guess = new double[] {-1.429146468892837E-10, 4.5022870335769276E-11, -1.318194179536703E-12,
                -1.098381235039422E-6, -1.6798876678906052E-6, -3.207856651454041E-9};
        // WHEN
        final ShootingBoundaryOutput forwardOutput = shooting.solve(initialMass, guess);
        // THEN
        final SpacecraftState terminalState = forwardOutput.getTerminalState();
        final String adjointName = ADJOINT_NAME;
        final double[] terminalAdjointForward = terminalState.getAdditionalState(adjointName);
        final NewtonFixedBoundaryCartesianSingleShooting backwardShooting = getShootingMethod(0.,
                new FixedTimeBoundaryOrbits(terminalOrbit, initialOrbit), integrationSettings);
        backwardShooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
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
    @ValueSource(doubles = {1, 10, 100, 1000})
    void testSolveScales(final double scale) {
        // GIVEN
        final double tolerancePosition = 1e1;
        final double toleranceVelocity = 1e-3;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final FixedTimeBoundaryOrbits boundaryOrbits = createBoundaryForKeplerianSettings();
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getDormandPrince54IntegratorSettings(1e-2, 2e2,
                ToleranceProvider.of(CartesianToleranceProvider.of(1e-3, 1e-6, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE)));
        final ShootingPropagationSettings propagationSettings = createKeplerianShootingSettings(boundaryOrbits.getInitialOrbit(),
                0, integrationSettings);
        // WHEN
        final ShootingBoundaryOutput output = getShootingBoundaryOutput(propagationSettings, boundaryOrbits, conditionChecker, scale);
        // THEN
        final ShootingBoundaryOutput expectedOutput = getShootingBoundaryOutput(propagationSettings, boundaryOrbits, conditionChecker, 1);
        Assertions.assertEquals(expectedOutput.getIterationCount(), output.getIterationCount());
        Assertions.assertArrayEquals(expectedOutput.getInitialState().getAdditionalState(ADJOINT_NAME),
                output.getInitialState().getAdditionalState(ADJOINT_NAME), 1e-20);
    }

    private static ShootingBoundaryOutput getShootingBoundaryOutput(final ShootingPropagationSettings propagationSettings,
                                                                    final FixedTimeBoundaryOrbits boundaryOrbits,
                                                                    final CartesianBoundaryConditionChecker conditionChecker,
                                                                    final double scale) {
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double toleranceMassAdjoint = 1e-8;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1e3;
        final double[] guess = new double[6];
        final double[] scales = guess.clone();
        Arrays.fill(scales, scale);
        return shooting.solve(mass, guess, scales);
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
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getDormandPrince54IntegratorSettings(1e-2, 2e2,
                ToleranceProvider.of(CartesianToleranceProvider.of(1e-3, 1e-6, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE)));
        final ShootingPropagationSettings propagationSettings = createKeplerianShootingSettings(boundaryOrbits.getInitialOrbit(),
                flowRateFactor, integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double toleranceMassAdjoint = 1e-8;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 1e3;
        final double[] guess = guessWithoutMass(propagationSettings.getAdjointDynamicsProvider().getAdjointName(), mass, integrationSettings, boundaryOrbits,
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
                                                                               final double massFlowRateFactor,
                                                                               final ShootingIntegrationSettings integrationSettings) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final AdjointDynamicsProvider adjointDynamicsProvider = getAdjointDynamicsProvider(massFlowRateFactor, Double.POSITIVE_INFINITY,
                keplerianTerm);
        return new ShootingPropagationSettings(forceModelList, adjointDynamicsProvider, integrationSettings);
    }

    private static double[] guessWithoutMass(final String adjointName, final double mass,
                                             final ShootingIntegrationSettings integrationSettings,
                                             final FixedTimeBoundaryOrbits boundaryOrbits,
                                             final CartesianBoundaryConditionChecker conditionChecker) {
        final ShootingPropagationSettings propagationSettings = createKeplerianShootingSettings(boundaryOrbits.getInitialOrbit(),
                0., integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
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

    private static AdjointDynamicsProvider getAdjointDynamicsProvider(final double massFlowRateFactor,
                                                                      final double maximumThrustMagnitude,
                                                                      final CartesianAdjointEquationTerm... terms) {
        final String adjointName = ADJOINT_NAME;
        if (maximumThrustMagnitude == Double.POSITIVE_INFINITY) {
            if (massFlowRateFactor == 0) {
                return CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProviderNeglectingMass(adjointName,
                        terms);
            }
            return CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProvider(adjointName, massFlowRateFactor,
                    EventDetectionSettings.getDefaultEventDetectionSettings(), terms);
        } else {
            return CartesianAdjointDynamicsProviderFactory.buildBoundedEnergyProvider(adjointName, massFlowRateFactor,
                    maximumThrustMagnitude, EventDetectionSettings.getDefaultEventDetectionSettings(), terms);
        }
    }

    private NewtonFixedBoundaryCartesianSingleShooting getHeliocentricShootingMethod(final double massFlowRateFactor,
                                                                                     final FixedTimeBoundaryOrbits boundaryOrbits) {
        // GIVEN
        final double tolerancePosition = 1e5;
        final double toleranceVelocity = 1e0;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory.getDormandPrince54IntegratorSettings(2e2, 1e5,
                ToleranceProvider.of(CartesianToleranceProvider.of(1e5, 1e-1, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE)));
        final EventDetectionSettings detectionSettings = new EventDetectionSettings(1e5, 1e3, EventDetectionSettings.DEFAULT_MAX_ITER);
        final ShootingPropagationSettings propagationSettings = createHeliocentricShootingSettings(boundaryOrbits.getInitialOrbit(),
                massFlowRateFactor, detectionSettings, integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double toleranceMassAdjoint = 1e-7;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        return shooting;
    }

    @Test
    void testGetSingularityThreshold() {
        // GIVEN
        final FixedTimeBoundaryOrbits boundaryOrbits = getHeliocentricBoundary();
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getHeliocentricShootingMethod(1. / (4000. * Constants.G0_STANDARD_GRAVITY),
                boundaryOrbits);
        final double expectedThreshold = 1.;
        shooting.setSingularityThreshold(expectedThreshold);
        // WHEN
        final double actualThreshold = shooting.getSingularityThreshold();
        // THEN
        Assertions.assertEquals(expectedThreshold, actualThreshold);
    }

    @Test
    void testSolveHeliocentric() {
        // GIVEN
        final FixedTimeBoundaryOrbits boundaryOrbits = getHeliocentricBoundary();
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getHeliocentricShootingMethod(1. / (4000. * Constants.G0_STANDARD_GRAVITY),
                boundaryOrbits);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double mass = 2e3;
        final double[] guess = guessWithoutMass(ADJOINT_NAME, mass, shooting.getPropagationSettings().getIntegrationSettings(),
                boundaryOrbits, shooting.getConditionChecker());
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

    private static ShootingPropagationSettings createHeliocentricShootingSettings(final Orbit initialOrbit,
                                                                               final double massFlowRateFactor,
                                                                               final EventDetectionSettings eventDetectionSettings,
                                                                               final ShootingIntegrationSettings integrationSettings) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final int dimension = (massFlowRateFactor == 0) ? 6 : 7;
        final CartesianAdjointDynamicsProvider adjointDynamicsProvider = new CartesianAdjointDynamicsProvider(ADJOINT_NAME, dimension) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                final CartesianCost cost;
                if (massFlowRateFactor == 0) {
                    cost = new UnboundedCartesianEnergyNeglectingMass(getAdjointName());
                } else {
                    cost = new UnboundedCartesianEnergy(getAdjointName(), massFlowRateFactor, eventDetectionSettings);
                }
                return new CartesianAdjointDerivativesProvider(cost, keplerianTerm);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(Field<T> field) {
                final FieldCartesianCost<T> cost;
                if (massFlowRateFactor == 0) {
                    cost = new FieldUnboundedCartesianEnergyNeglectingMass<>(getAdjointName(), field);
                } else {
                    cost = new FieldUnboundedCartesianEnergy<>(getAdjointName(), field.getZero().newInstance(massFlowRateFactor),
                            new FieldEventDetectionSettings<>(field, eventDetectionSettings));
                }
                return new FieldCartesianAdjointDerivativesProvider<>(cost, keplerianTerm);
            }
        };
        return new ShootingPropagationSettings(forceModelList, adjointDynamicsProvider, integrationSettings);
    }

    @Test
    void testSolveHeliocentricWithoutMass() {
        // GIVEN
        final FixedTimeBoundaryOrbits boundaryOrbits = getHeliocentricBoundary();
        final NewtonFixedBoundaryCartesianSingleShooting shooting = getHeliocentricShootingMethod(0.,
                boundaryOrbits);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double mass = 3e3;
        // WHEN
        final ShootingBoundaryOutput output = shooting.solve(mass, new double[6]);
        // THEN
        Assertions.assertTrue(output.isConverged());
        final SpacecraftState repropagatedState = repropagate(shooting.getPropagationSettings(), output.getInitialState(),
                boundaryOrbits.getTerminalOrbit().getDate());
        final Vector3D relativePosition = repropagatedState.getPosition().subtract(boundaryOrbits.getTerminalOrbit().getPosition());
        Assertions.assertEquals(0, relativePosition.getNorm(), 1e2);
    }

    private SpacecraftState repropagate(final ShootingPropagationSettings propagationSettings,
                                        final SpacecraftState initialState, final AbsoluteDate terminalDate) {
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final ODEIntegrator integrator = propagationSettings.getIntegrationSettings().getIntegratorBuilder()
                .buildIntegrator(initialState.getOrbit(), orbitType);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
        propagator.setOrbitType(orbitType);
        propagator.addAdditionalDerivativesProvider(propagationSettings.getAdjointDynamicsProvider().buildAdditionalDerivativesProvider());
        return propagator.propagate(terminalDate);
    }

    @Test
    void testQuadraticContinuation() {
        // GIVEN
        final double tolerancePosition = 1e-0;
        final double toleranceVelocity = 1e-4;
        final CartesianBoundaryConditionChecker conditionChecker = new NormBasedCartesianConditionChecker(10,
                tolerancePosition, toleranceVelocity);
        final Orbit initialOrbit = createSomeInitialOrbit(2e6);
        final double timeOfFlight = 1e4;
        final Orbit terminalOrbit = createTerminalBoundary(initialOrbit, timeOfFlight);
        final FixedTimeBoundaryOrbits boundaryOrbits = new FixedTimeBoundaryOrbits(initialOrbit, terminalOrbit);
        final double flowRateFactor = 1e-2;
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getDormandPrince54IntegratorSettings(1e-1, 1e2,  ToleranceProvider.of(CartesianToleranceProvider.of(1e-3, 1e-6, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE)));
        final double maximumThrust = 1e1;
        final ShootingPropagationSettings propagationSettings = createShootingSettingsForQuadraticPenalty(initialOrbit, flowRateFactor,
                maximumThrust, 1., integrationSettings);
        final NewtonFixedBoundaryCartesianSingleShooting shooting = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettings,
                boundaryOrbits, conditionChecker);
        shooting.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
        final double toleranceMassAdjoint = 1e-10;
        shooting.setToleranceMassAdjoint(toleranceMassAdjoint);
        final double mass = 2e3;
        final double[] guess = new double[]{-1.3144902474363005, -7.770298698677809, -5.328110916176676,
                -275.6031033370172, -3049.432734131893, -1560.1101732794737, -172.14906242868724};
        ShootingBoundaryOutput output = shooting.solve(mass, guess);
        // WHEN & THEN
        for (double epsilon = 0.9; epsilon > 0.7; epsilon -= 0.05) {
            double[] previousAdjoint = output.getInitialState().getAdditionalState(ADJOINT_NAME);
            final ShootingPropagationSettings propagationSettingsWithNewEpsilon = createShootingSettingsForQuadraticPenalty(initialOrbit,
                    flowRateFactor, maximumThrust, epsilon, propagationSettings.getIntegrationSettings());
            final NewtonFixedBoundaryCartesianSingleShooting shootingWithNewEpsilon = new NewtonFixedBoundaryCartesianSingleShooting(propagationSettingsWithNewEpsilon,
                    boundaryOrbits, conditionChecker);
            shootingWithNewEpsilon.setSingularityThreshold(THRESHOLD_LU_DECOMPOSITION);
            output = shootingWithNewEpsilon.solve(mass, previousAdjoint);
            Assertions.assertTrue(output.isConverged());
        }
    }

    private static ShootingPropagationSettings createShootingSettingsForQuadraticPenalty(final Orbit initialOrbit,
                                                                                         final double massFlowRate,
                                                                                         final double maximumThrustMagnitude,
                                                                                         final double epsilon,
                                                                                         final ShootingIntegrationSettings integrationSettings) {
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(initialOrbit.getMu());
        final Frame j2Frame = initialOrbit.getFrame(); // approximation for speed
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(initialOrbit.getMu(),
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, j2Frame);
        final List<ForceModel> forceModelList = new ArrayList<>();
        forceModelList.add(newtonianAttraction);
        forceModelList.add(j2OnlyPerturbation);
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(initialOrbit.getMu());
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(j2OnlyPerturbation.getMu(), j2OnlyPerturbation.getrEq(),
                j2OnlyPerturbation.getJ2(initialOrbit.getDate()), j2OnlyPerturbation.getFrame());
        return new ShootingPropagationSettings(forceModelList,
                CartesianAdjointDynamicsProviderFactory.buildQuadraticPenaltyFuelCostProvider(ADJOINT_NAME,
                        massFlowRate, maximumThrustMagnitude, epsilon, EventDetectionSettings.getDefaultEventDetectionSettings(),
                        keplerianTerm, j2Term), integrationSettings);
    }
}
