/* Copyright 2020-2026 Exotrail
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
package org.orekit.control.heuristics.lambert;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Precision;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.mock;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.J2OnlyPerturbation;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class LambertDifferentialCorrectorTest {

    @Test
    void testDefaultValues() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        // WHEN & THEN
        assertEquals(1000., corrector.getInitialMass());
        assertEquals(10, corrector.getMaxIter());
        assertEquals(1e-4, corrector.getPositionTolerance());
        assertEquals(0, corrector.getCurrentIter());
        assertEquals("stm", corrector.getStmName());
        assertTrue(corrector.getThresholdMatrixSolver() <= 1e-15);
        assertNull(corrector.getMatricesHarvester());
    }

    @Test
    void testSetStmName() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        final String expectedName = "42";
        // WHEN
        corrector.setStmName(expectedName);
        // THEN
        assertEquals(expectedName, corrector.getStmName());
    }

    @Test
    void testSetMaxIter() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        final int expectedMaxIter = 20;
        // WHEN
        corrector.setMaxIter(expectedMaxIter);
        // THEN
        assertEquals(expectedMaxIter, corrector.getMaxIter());
    }

    @Test
    void testSetPositionTolerance() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        final double expectedTolerance = 1.;
        // WHEN
        corrector.setPositionTolerance(expectedTolerance);
        // THEN
        assertEquals(expectedTolerance, corrector.getPositionTolerance());
    }

    @Test
    void testSetInitialMass() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        final double expectedInitialMass = 1.;
        // WHEN
        corrector.setInitialMass(expectedInitialMass);
        // THEN
        assertEquals(expectedInitialMass, corrector.getInitialMass());
    }

    @Test
    void testSetThresholdMatrixSolver() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        final double expectedThreshold = 1.;
        // THEN
        corrector.setThresholdMatrixSolver(expectedThreshold);
        // WHEN
        assertEquals(expectedThreshold, corrector.getThresholdMatrixSolver());
    }

    @Test
    void testGetLambertBoundaryConditions() {
        // GIVEN
        final LambertBoundaryConditions mockedConditions = mock();
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(mockedConditions);
        // WHEN & THEN
        assertEquals(mockedConditions, corrector.getLambertBoundaryConditions());
    }

    @Test
    void testSolveNoConvergence() {
        // GIVEN
        final Orbit initialOrbit = getOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(1e5);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(initialOrbit.getDate(),
                initialOrbit.getPosition(), terminalOrbit.getDate(), terminalOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        final Vector3D terminalVelocity = terminalOrbit.getVelocity();
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(terminalVelocity, terminalVelocity);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertNull(velocities);
        assertEquals(corrector.getMaxIter(), corrector.getCurrentIter());
    }

    @Test
    void testSolveStopping() {
        // GIVEN
        final Orbit initialOrbit = getOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(1e5);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(initialOrbit.getDate(),
                initialOrbit.getPosition(), terminalOrbit.getDate(), terminalOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(initialOrbit.getVelocity(),
                terminalOrbit.getVelocity());
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final AbsoluteDate stoppingDate = initialOrbit.getDate().shiftedBy(1e2);
        propagator.addEventDetector(new DateDetector(stoppingDate).withHandler(new StopOnEvent()));
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertNull(velocities);
    }

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testSolveTrivialAnalytical(final OrbitType orbitType) {
        // GIVEN
        final Orbit initialOrbit = getOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(1e5);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(initialOrbit.getDate(),
                initialOrbit.getPosition(), terminalOrbit.getDate(), terminalOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(initialOrbit.getVelocity(),
                terminalOrbit.getVelocity());
        final KeplerianPropagator propagator = new KeplerianPropagator(orbitType.convertType(initialOrbit));
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertEquals(0, corrector.getCurrentIter());
        compareVelocities(guess, velocities, 1e-9);
    }

    @Test
    void testSolveBackward() {
        // GIVEN
        final Orbit initialOrbit = getOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(1e5);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(terminalOrbit.getDate(),
                terminalOrbit.getPosition(), initialOrbit.getDate(), initialOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(terminalOrbit.getVelocity(),
                initialOrbit.getVelocity());
        final KeplerianPropagator propagator = new KeplerianPropagator(terminalOrbit);
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertEquals(0, corrector.getCurrentIter());
        compareVelocities(guess, velocities, 1e-9);
    }

    @Test
    void testSolveTrivialNumerical() {
        // GIVEN
        final Orbit initialOrbit = getOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(1e3);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(initialOrbit.getDate(),
                initialOrbit.getPosition(), terminalOrbit.getDate(), terminalOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        corrector.setPositionTolerance(1e-3);
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(initialOrbit.getVelocity(),
                terminalOrbit.getVelocity());
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.));
        propagator.setOrbitType(null);
        propagator.addForceModel(new NewtonianAttraction(initialOrbit.getMu()));
        propagator.setInitialState(new SpacecraftState(new AbsolutePVCoordinates(initialOrbit.getFrame(),
                initialOrbit.getDate(), initialOrbit.getPVCoordinates())));
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertEquals(0, corrector.getCurrentIter());
        compareVelocities(guess, velocities, 1e-5);
    }

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testSolveNumericalJ2(final OrbitType orbitType) {
        // GIVEN
        final Orbit initialOrbit = getOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(1e4);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(initialOrbit.getDate(),
                initialOrbit.getPosition(), terminalOrbit.getDate(), terminalOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(initialOrbit.getVelocity(),
                terminalOrbit.getVelocity());
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.));
        propagator.setOrbitType(orbitType);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.addForceModel(buildJ2Force());
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertNotEquals(0, corrector.getCurrentIter());
        assertNotNull(velocities);
        final PVCoordinates pvCoordinates = new PVCoordinates(conditions.getInitialPosition(),
                velocities.getInitialVelocity());
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, initialOrbit.getFrame(),
                initialOrbit.getDate(), initialOrbit.getMu());
        propagator.resetInitialState(new SpacecraftState(orbit).withMass(corrector.getInitialMass()));
        final SpacecraftState state = propagator.propagate(conditions.getTerminalDate());
        assertArrayEquals(conditions.getTerminalPosition().toArray(), state.getPosition().toArray(),
                corrector.getPositionTolerance());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e2, 1e3, 1e4, 1e5})
    void testSolveNumericalGeo(final double timeOfFlight) {
        // GIVEN
        final Orbit initialOrbit = new KeplerianOrbit(42000e3, 0.0001, 0.01, 1, 2, 3, PositionAngleType.ECCENTRIC,
                FramesFactory.getTEME(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final Orbit terminalOrbit = initialOrbit.shiftedBy(timeOfFlight);
        final LambertBoundaryConditions conditions = new LambertBoundaryConditions(initialOrbit.getDate(),
                initialOrbit.getPosition(), terminalOrbit.getDate(), terminalOrbit.getPosition(), initialOrbit.getFrame());
        final LambertDifferentialCorrector corrector = buildCorrector(conditions);
        final LambertBoundaryVelocities guess = new LambertBoundaryVelocities(initialOrbit.getVelocity(),
                terminalOrbit.getVelocity());
        final DormandPrince54IntegratorBuilder integratorBuilder = new DormandPrince54IntegratorBuilder(1e-2,
                1e2, 1e-3);
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
        final NumericalPropagator propagator = new NumericalPropagator(integratorBuilder.buildIntegrator(initialOrbit, orbitType));
        propagator.setOrbitType(orbitType);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.addForceModel(buildJ2Force());
        // WHEN
        final LambertBoundaryVelocities velocities = corrector.solve(propagator, guess.getInitialVelocity());
        // THEN
        assertNotEquals(0, corrector.getCurrentIter());
        assertNotNull(velocities);
    }

    @Test
    void testRefinedLambertDerExample1() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = 398600435507000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(22592.145603, -1599.915239, -19783.950506).scalarMultiply(1000.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 36000.0);
        final Vector3D position2 = new Vector3D(1922.067697, 4054.157051, -8925.727465).scalarMultiply(1000.0);
        final Frame inertialFrame = FramesFactory.getEME2000();

        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final LambertSolution solutionToRefine = iod.solve(
            true,
            1,
            boundaryConditions).get(1);

        final Vector3D unrefinedSolutionExpectedVelocity1 = new Vector3D(0.50335770, 0.61869408, -1.57176904).scalarMultiply(1000.0);
        final Vector3D unrefinedSolutionExpectedVelocity2 = new Vector3D(-4.18334626, -1.13262727, 6.13307091).scalarMultiply(1000.0);

        compareVelocities(unrefinedSolutionExpectedVelocity1, solutionToRefine.getBoundaryVelocities().getInitialVelocity(), 0.1);
        compareVelocities(unrefinedSolutionExpectedVelocity2, solutionToRefine.getBoundaryVelocities().getTerminalVelocity(), 0.1);

        final LambertDifferentialCorrector corrector = buildCorrector(boundaryConditions);
        final double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(1.0).getTolerances(position1, unrefinedSolutionExpectedVelocity1);
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(60.0);
        final NumericalPropagator perturbedPropagator = new NumericalPropagator(integrator);
        perturbedPropagator.setOrbitType(OrbitType.CARTESIAN);
        perturbedPropagator.removeForceModels();
        final NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(4, 0);
        final ForceModel holmesFeatherstone = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        perturbedPropagator.addForceModel(holmesFeatherstone);
        final TimeStampedPVCoordinates initialPV = new TimeStampedPVCoordinates(date1, position1, solutionToRefine.getBoundaryVelocities().getInitialVelocity());
        final Orbit initialOrbit = new CartesianOrbit(initialPV, inertialFrame, mu);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        perturbedPropagator.setInitialState(initialState);

        final LambertBoundaryVelocities refinedVelocities = corrector.solve(perturbedPropagator, solutionToRefine.getBoundaryVelocities().getInitialVelocity());

        assertNotNull(refinedVelocities);

        final Vector3D refinedSolutionExpectedVelocity1 = new Vector3D(0.48947268, 0.62699255, -1.57594642).scalarMultiply(1000.0);
        final Vector3D refinedSolutionExpectedVelocity2 = new Vector3D(-4.18527250, -1.05070711, 6.14512956).scalarMultiply(1000.0);

        compareVelocities(refinedSolutionExpectedVelocity1, refinedVelocities.getInitialVelocity(), 1e-3 * refinedSolutionExpectedVelocity1.getNorm());
        compareVelocities(refinedSolutionExpectedVelocity2, refinedVelocities.getTerminalVelocity(), 1e-3 * refinedSolutionExpectedVelocity2.getNorm());
    }

    @Test
    void testRefinedLambertDerExample2() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = 398600435507000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(7231.58074563487, 218.02523761425, 11.79251215952).scalarMultiply(1000.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 12300.0);
        final Vector3D position2 = new Vector3D(7357.06485698842, 253.55724281562, 38.81222241557).scalarMultiply(1000.0);
        final Frame inertialFrame = FramesFactory.getEME2000();

        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final LambertSolution solutionToRefine = iod.solve(
            true,
            0,
            boundaryConditions).get(0);

        final Vector3D unrefinedSolutionExpectedVelocity1 = new Vector3D(8.79257809, 0.27867677, 0.02581527).scalarMultiply(1000.0);
        final Vector3D unrefinedSolutionExpectedVelocity2 = new Vector3D(-8.68383320, -0.28592643, -0.03453010).scalarMultiply(1000.0);

        compareVelocities(unrefinedSolutionExpectedVelocity1, solutionToRefine.getBoundaryVelocities().getInitialVelocity(), 1e-3);
        compareVelocities(unrefinedSolutionExpectedVelocity2, solutionToRefine.getBoundaryVelocities().getTerminalVelocity(), 1e-3);

        final LambertDifferentialCorrector corrector = buildCorrector(boundaryConditions);
        final double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(1.0).getTolerances(position1, unrefinedSolutionExpectedVelocity1);
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(60.0);
        final NumericalPropagator perturbedPropagator = new NumericalPropagator(integrator);
        perturbedPropagator.setOrbitType(OrbitType.CARTESIAN);
        perturbedPropagator.removeForceModels();
        final NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(4, 0);
        final ForceModel holmesFeatherstone = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        perturbedPropagator.addForceModel(holmesFeatherstone);
        final TimeStampedPVCoordinates initialPV = new TimeStampedPVCoordinates(date1, position1, solutionToRefine.getBoundaryVelocities().getInitialVelocity());
        final Orbit initialOrbit = new CartesianOrbit(initialPV, inertialFrame, mu);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        perturbedPropagator.setInitialState(initialState);

        final LambertBoundaryVelocities refinedVelocities = corrector.solve(perturbedPropagator, solutionToRefine.getBoundaryVelocities().getInitialVelocity());

        assertNotNull(refinedVelocities);

        final Vector3D refinedSolutionExpectedVelocity1 = new Vector3D(8.7925788197, 0.2786767791, 0.0258152755).scalarMultiply(1000.0);
        final Vector3D refinedSolutionExpectedVelocity2 = new Vector3D(-8.6838339145, -0.2859264505, -0.0345301070).scalarMultiply(1000.0);

        compareVelocities(refinedSolutionExpectedVelocity1, refinedVelocities.getInitialVelocity(), 1e-3 * refinedSolutionExpectedVelocity1.getNorm());
        compareVelocities(refinedSolutionExpectedVelocity2, refinedVelocities.getTerminalVelocity(), 1e-3 * refinedSolutionExpectedVelocity2.getNorm());
    }

    private J2OnlyPerturbation buildJ2Force() {
        return new J2OnlyPerturbation(Constants.EGM96_EARTH_MU, Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                -Constants.EGM96_EARTH_C20, FramesFactory.getTOD(false));
    }

    private static LambertDifferentialCorrector buildCorrector(final LambertBoundaryConditions conditions) {
        final LambertDifferentialCorrector corrector = new LambertDifferentialCorrector(conditions);
        corrector.setPositionTolerance(1e-4);
        corrector.setMaxIter(10);
        corrector.setThresholdMatrixSolver(Precision.EPSILON);
        return corrector;
    }

    private static void compareVelocities(final LambertBoundaryVelocities expected,
                                          final LambertBoundaryVelocities actual, final double tolerance) {
        compareVelocities(expected.getInitialVelocity(), actual.getInitialVelocity(), tolerance);
        compareVelocities(expected.getTerminalVelocity(), actual.getTerminalVelocity(), tolerance);
    }

    private static void compareVelocities(final Vector3D expected, final Vector3D actual, final double tolerance) {
        assertArrayEquals(expected.toArray(), actual.toArray(), tolerance);
    }

    private static Orbit getOrbit(final AbsoluteDate date) {
        return new EquinoctialOrbit(7e6, 0.01, -0.001, 1., 2., 3., PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                date, Constants.EGM96_EARTH_MU);
    }
}
