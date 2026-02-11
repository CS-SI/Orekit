/* Copyright 2002-2026 CS GROUP
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.TimeStampedGeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.KinematicTransform;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class LambertSolverTest {

    @Test
    void testSolveFailure() {
        // GIVEN
        final LambertSolver solver = new LambertSolver(1.);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(date,
                Vector3D.PLUS_I, date.shiftedBy(1), Vector3D.PLUS_J, FramesFactory.getEME2000());
        // WHEN and THEN
        final OrekitException exception = assertThrows(OrekitException.class, () -> {
            solver.solve(true, 10, boundaryConditions);
        });
        assertEquals(OrekitMessages.LAMBERT_INVALID_NUMBER_OF_REVOLUTIONS, exception.getSpecifier());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e4, 1e5, 0.8e6, 1e7})
    void testSolveGeoTimes(final double timeOfFlight) {
        testSolveGeo(timeOfFlight, 0.1, true);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.2, 0.3})
    void testSolveGeoLongitude(final double longitude) {
        testSolveGeo(Constants.JULIAN_DAY * 1.4, longitude, true);
    }

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void testSolveGeoDirection(final boolean posigrade) {
        testSolveGeo(1e8, 0.1, posigrade);
    }

    private void testSolveGeo(final double timeOfFlight, final double longitude2, final boolean posigrade) {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final double latitude = 0.;
        final double altitude = 42157e6 - 6378136.;
        final OneAxisEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(false));
        final TimeStampedGeodeticPoint point1 = new TimeStampedGeodeticPoint(date, new GeodeticPoint(latitude, 0., altitude));
        final Frame frame = FramesFactory.getGCRF();
        final Orbit orbit1 = buildOrbitFromGeodetic(ellipsoid, point1, frame);
        final TimeStampedGeodeticPoint point2 = new TimeStampedGeodeticPoint(date.shiftedBy(timeOfFlight),
                new GeodeticPoint(latitude, longitude2, altitude));
        final Orbit orbit2 = buildOrbitFromGeodetic(ellipsoid, point2, frame);
        final LambertSolver solver = new LambertSolver(orbit2.getMu());
        final int nRev = (int) FastMath.floor(timeOfFlight / orbit1.getKeplerianPeriod());
        // WHEN
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(orbit1.getDate(),
                orbit1.getPosition(), orbit2.getDate(), orbit2.getPosition(), orbit2.getFrame());
        final List<LambertSolution> solution = solver.solve(posigrade, nRev, boundaryConditions);
        // THEN
        final Orbit lambertOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(orbit1.getDate(), orbit1.getPosition(), solution.get(0).getBoundaryVelocities().getInitialVelocity()),
                orbit1.getFrame(), orbit1.getMu());
        final KeplerianPropagator propagator = new  KeplerianPropagator(lambertOrbit);
        final Orbit propagatedOrbit = propagator.propagateOrbit(orbit2.getDate());
        assertArrayEquals(orbit2.getPosition().toArray(), propagatedOrbit.getPosition().toArray(), 1.);
    }

    private static Orbit buildOrbitFromGeodetic(final OneAxisEllipsoid ellipsoid,
                                                final TimeStampedGeodeticPoint geodeticPoint,
                                                final Frame frame) {
        final Vector3D position = ellipsoid.transform(geodeticPoint);
        final KinematicTransform transform = ellipsoid.getBodyFrame().getKinematicTransformTo(frame,
                geodeticPoint.getDate());
        final PVCoordinates pvCoordinates = transform.transformOnlyPV(new PVCoordinates(position));
        return new CartesianOrbit(pvCoordinates, frame, geodeticPoint.getDate(), Constants.EGM96_EARTH_MU);
    }

    @Test
    void testSolveJacobian() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit orbit1 = new CartesianOrbit(new TimeStampedPVCoordinates(date, new Vector3D(7.557249611265823E6, -4.1474095848830185E7, -958.8453227159644),
                new Vector3D(3024.3391189172766, 551.0833948309845, 0.09684004762824829)), frame, mu);
        final double timeOfFlight = Constants.JULIAN_DAY * 1.5;
        final Orbit orbit2 = new CartesianOrbit(new TimeStampedPVCoordinates(date.shiftedBy(timeOfFlight),
                new Vector3D(-1.6651012438926659E7, 3.8729271015852745E7, 650.8862988349741),
                new Vector3D(-2824.183311624355, -1214.2111159693908, -0.10881575040466626)), frame, mu);
        final LambertSolver solver = new LambertSolver(orbit2.getMu());
        final int nRev = (int) FastMath.floor(timeOfFlight / orbit1.getKeplerianPeriod());
        final boolean posigrade = true;
        // WHEN
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(orbit1.getDate(),
                orbit1.getPosition(), orbit2.getDate(), orbit2.getPosition(), orbit2.getFrame());
        final LambertSolution solution = solver.solve(posigrade, nRev, boundaryConditions).get(0);
        final LambertBoundaryVelocities velocities = solution.getBoundaryVelocities();
        final RealMatrix jacobian = solver.computeJacobian(boundaryConditions, velocities);
        // THEN
        assertFalse(Double.isNaN(jacobian.getEntry(0, 0)));
        checkJacobianWithFiniteDifferences(solver, posigrade, nRev, boundaryConditions, jacobian);
    }

    @Test
    void testLambert() {
        final double mu = 398600435507000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(15945340.0, 0.0, 0.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 76.0 * 60.0);
        final Vector3D position2 = new Vector3D(12214838.99, 10249467.31, 0.0);
        final Frame inertialFrame = FramesFactory.getEME2000();
        final LambertBoundaryConditions boundaryConditions =
                new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final List<LambertSolution> solutions = iod.solve(
                true,
                boundaryConditions);

        // Check the number of solutions
        Assertions.assertEquals(1, solutions.size());

        // Check the solution
        final LambertSolution solution = solutions.get(0);
        final Vector3D expectedVelocity1 = new Vector3D(2058.9497203633254685, 2915.9389575906252503, 0.0);
        final Vector3D expectedVelocity2 = new Vector3D(-3451.5763801709435938, 910.2714190993910961, 0.0);
        checkLambertSolution(solution, expectedVelocity1, expectedVelocity2, 0.1, 1e-3);

        // Test LambertSolution constructor from LambertBoundaryVelocities
        final LambertSolution solutionCopy = new LambertSolution(
                solution.getNRev(),
                solution.getPathType(),
                solution.getOrbitType(),
                solution.getPosigrade(),
                solution.getBoundaryConditions(),
                solution.getBoundaryVelocities());
        checkLambertSolution(solutionCopy, expectedVelocity1, expectedVelocity2, 0.1, 1e-3);
    }

    // Der examples are extracted from section Lambert Numerical Examples of https://amostech.com/TechnicalPapers/2011/Poster/DER.pdf

    @Test
    void testLambertDerExample1() {
        final double mu = 398600435507000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(22592.145603, -1599.915239, -19783.950506).scalarMultiply(1000.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 36000.0);
        final Vector3D position2 = new Vector3D(1922.067697, 4054.157051, -8925.727465).scalarMultiply(1000.0);
        final Frame inertialFrame = FramesFactory.getEME2000();
        final LambertBoundaryConditions boundaryConditions =
                new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final List<LambertSolution> solutionsPosigrade = iod.solve(
                true,
                boundaryConditions);

        // Check the number of solutions
        Assertions.assertEquals(3, solutionsPosigrade.size());

        // Check the solutions
        final Vector3D solutionPosigrade1ExpectedVelocity1 = new Vector3D(2.000652697, 0.387688615, -2.666947760).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade1ExpectedVelocity2 = new Vector3D(-3.79246619, -1.77707641, 6.856814395).scalarMultiply(1000.0);

        final Vector3D solutionPosigrade2ExpectedVelocity1 = new Vector3D(-2.45759553, 1.16945801, 0.43161258).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade2ExpectedVelocity2 = new Vector3D(-5.53841370, 0.01822220, 5.49641054).scalarMultiply(1000.0);

        final Vector3D solutionPosigrade3ExpectedVelocity1 = new Vector3D(0.50335770, 0.61869408, -1.57176904).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade3ExpectedVelocity2 = new Vector3D(-4.18334626, -1.13262727, 6.13307091).scalarMultiply(1000.0);

        final ArrayList<Vector3D> solutionsPosigradeV1 = new ArrayList<>();
        solutionsPosigradeV1.add(solutionPosigrade1ExpectedVelocity1);
        solutionsPosigradeV1.add(solutionPosigrade2ExpectedVelocity1);
        solutionsPosigradeV1.add(solutionPosigrade3ExpectedVelocity1);

        final ArrayList<Vector3D> solutionsPosigradeV2 = new ArrayList<>();
        solutionsPosigradeV2.add(solutionPosigrade1ExpectedVelocity2);
        solutionsPosigradeV2.add(solutionPosigrade2ExpectedVelocity2);
        solutionsPosigradeV2.add(solutionPosigrade3ExpectedVelocity2);

        for (int i = 0; i < solutionsPosigrade.size(); i++) {
            final LambertSolution solution = solutionsPosigrade.get(i);
            final Vector3D expectedVelocity1 = solutionsPosigradeV1.get(i);
            final Vector3D expectedVelocity2 = solutionsPosigradeV2.get(i);
            checkLambertSolution(solution, expectedVelocity1, expectedVelocity2, 0.1, 1e-3);
        }

        final List<LambertSolution> solutionsRetrograde = iod.solve(
            false,
            boundaryConditions);

        // Check the number of retrograde solutions
        Assertions.assertEquals(3, solutionsRetrograde.size());

        // Check the solutions
        final Vector3D solutionRetrograde1ExpectedVelocity1 = new Vector3D(2.96616042, -1.27577231, -0.75545632).scalarMultiply(1000.0);
        final Vector3D solutionRetrograde1ExpectedVelocity2 = new Vector3D(5.84375455, -0.20047673, -5.48615883).scalarMultiply(1000.0);

        final Vector3D solutionRetrograde2ExpectedVelocity1 = new Vector3D(-1.38861608, -0.47836611, 2.21280154).scalarMultiply(1000.0);
        final Vector3D solutionRetrograde2ExpectedVelocity2 = new Vector3D(3.92901545, 1.50871943, -6.52926969).scalarMultiply(1000.0);

        final Vector3D solutionRetrograde3ExpectedVelocity1 = new Vector3D(1.33645655, -0.94654565, 0.30211211).scalarMultiply(1000.0);
        final Vector3D solutionRetrograde3ExpectedVelocity2 = new Vector3D(4.93628678, 0.39863416, -5.61593092).scalarMultiply(1000.0);

        final ArrayList<Vector3D> solutionsRetrogradeV1 = new ArrayList<>();
        solutionsRetrogradeV1.add(solutionRetrograde1ExpectedVelocity1);
        solutionsRetrogradeV1.add(solutionRetrograde2ExpectedVelocity1);
        solutionsRetrogradeV1.add(solutionRetrograde3ExpectedVelocity1);

        final ArrayList<Vector3D> solutionsRetrogradeV2 = new ArrayList<>();
        solutionsRetrogradeV2.add(solutionRetrograde1ExpectedVelocity2);
        solutionsRetrogradeV2.add(solutionRetrograde2ExpectedVelocity2);
        solutionsRetrogradeV2.add(solutionRetrograde3ExpectedVelocity2);

        for (int i = 0; i < solutionsRetrograde.size(); i++) {
            final LambertSolution solution = solutionsRetrograde.get(i);
            final Vector3D expectedVelocity1 = solutionsRetrogradeV1.get(i);
            final Vector3D expectedVelocity2 = solutionsRetrogradeV2.get(i);
            checkLambertSolution(solution, expectedVelocity1, expectedVelocity2, 0.1, 1e-3);
        }
    }

    @Test
    void testLambertDerExample2() {
        final double mu = 398600435507000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(7231.58074563487, 218.02523761425, 11.79251215952).scalarMultiply(1000.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 12300.0);
        final Vector3D position2 = new Vector3D(7357.06485698842, 253.55724281562, 38.81222241557).scalarMultiply(1000.0);
        final Frame inertialFrame = FramesFactory.getEME2000();
        final LambertBoundaryConditions boundaryConditions =
                new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final List<LambertSolution> solutionsPosigrade = iod.solve(
                true,
                boundaryConditions);

        // Check the number of solutions
        Assertions.assertEquals(11, solutionsPosigrade.size());

        // Check the first 5 solutions (for 0, 1 and 2 complete revolutions)
        final Vector3D solutionPosigrade1ExpectedVelocity1 = new Vector3D(8.79257809, 0.27867677, 0.02581527).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade1ExpectedVelocity2 = new Vector3D(-8.68383320, -0.28592643, -0.03453010).scalarMultiply(1000.0);

        final Vector3D solutionPosigrade2ExpectedVelocity1 = new Vector3D(8.19519089, 2.30595215, 1.75229388).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade2ExpectedVelocity2 = new Vector3D(8.07984345, 2.30222567, 1.75189559).scalarMultiply(1000.0);

        final Vector3D solutionPosigrade3ExpectedVelocity1 = new Vector3D(7.63353091, 0.24582764, 0.02569470).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade3ExpectedVelocity2 = new Vector3D(-7.50840227, -0.24335652, -0.02658981).scalarMultiply(1000.0);

        final Vector3D solutionPosigrade4ExpectedVelocity1 = new Vector3D(7.00660748, 1.96687296, 1.49423471).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade4ExpectedVelocity2 = new Vector3D(6.87133644, 1.96250281, 1.49376762).scalarMultiply(1000.0);

        final Vector3D solutionPosigrade5ExpectedVelocity1 = new Vector3D(6.51890385, 0.21496104, 0.02618989).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade5ExpectedVelocity2 = new Vector3D(-6.37230007, -0.20150975, -0.01832295).scalarMultiply(1000.0);

        final ArrayList<Vector3D> solutionsPosigradeV1 = new ArrayList<>();
        solutionsPosigradeV1.add(solutionPosigrade1ExpectedVelocity1);
        solutionsPosigradeV1.add(solutionPosigrade2ExpectedVelocity1);
        solutionsPosigradeV1.add(solutionPosigrade3ExpectedVelocity1);
        solutionsPosigradeV1.add(solutionPosigrade4ExpectedVelocity1);
        solutionsPosigradeV1.add(solutionPosigrade5ExpectedVelocity1);

        final ArrayList<Vector3D> solutionsPosigradeV2 = new ArrayList<>();
        solutionsPosigradeV2.add(solutionPosigrade1ExpectedVelocity2);
        solutionsPosigradeV2.add(solutionPosigrade2ExpectedVelocity2);
        solutionsPosigradeV2.add(solutionPosigrade3ExpectedVelocity2);
        solutionsPosigradeV2.add(solutionPosigrade4ExpectedVelocity2);
        solutionsPosigradeV2.add(solutionPosigrade5ExpectedVelocity2);

        for (int i = 0; i < 5; i++) {
            final LambertSolution solution = solutionsPosigrade.get(i);
            final Vector3D expectedVelocity1 = solutionsPosigradeV1.get(i);
            final Vector3D expectedVelocity2 = solutionsPosigradeV2.get(i);
            checkLambertSolution(solution, expectedVelocity1, expectedVelocity2, 0.1, 1e-3);
        }
    }

    // Example 7.5 from Fundamentals of Astrodynamics and Applications by David Vallado

    @Test
    void testLambertValladoExample75() {
        final double mu = 398600435507000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(15945.34, 0.0, 0.0).scalarMultiply(1000.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 76.0 * 60);
        final Vector3D position2 = new Vector3D(12214.83399, 10249.46731, 0.0).scalarMultiply(1000.0);
        final Frame inertialFrame = FramesFactory.getEME2000();
        final LambertBoundaryConditions boundaryConditions =
                new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final List<LambertSolution> solutionsPosigrade = iod.solve(
                true,
                boundaryConditions);

        // Check the number of solutions
        Assertions.assertEquals(1, solutionsPosigrade.size());

        // Check the single posigrade solutions (no complete revolutions)
        final Vector3D solutionPosigrade1ExpectedVelocity1 = new Vector3D(2.058913, 2.915965, 0.0).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade1ExpectedVelocity2 = new Vector3D(-3.451565, 0.910315, 0.0).scalarMultiply(1000.0);

        checkLambertSolution(solutionsPosigrade.get(0), solutionPosigrade1ExpectedVelocity1, solutionPosigrade1ExpectedVelocity2, 0.1, 1e-3);
    }

    // Example 5.2 (page 621) from Orbital Mechanics for Engineering Students by Howard Curtis

    @Test
    void testLambertCurtisExample52() {
        final double mu = 398600000000000.0;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(5000.0, 10000.0, 2100.0).scalarMultiply(1000.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 3600.0);
        final Vector3D position2 = new Vector3D(-14600.0, 2500.0, 7000.0).scalarMultiply(1000.0);
        final Frame inertialFrame = FramesFactory.getEME2000();
        final LambertBoundaryConditions boundaryConditions =
                new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final List<LambertSolution> solutionsPosigrade = iod.solve(
                true,
                boundaryConditions);

        // Check the number of solutions
        Assertions.assertEquals(1, solutionsPosigrade.size());

        // Check the single posigrade solutions (no complete revolutions)
        final Vector3D solutionPosigrade1ExpectedVelocity1 = new Vector3D(-5.99249, 1.92536, 3.24564).scalarMultiply(1000.0);
        final Vector3D solutionPosigrade1ExpectedVelocity2 = new Vector3D(-3.31246, -4.19662, -0.385288).scalarMultiply(1000.0);

        checkLambertSolution(solutionsPosigrade.get(0), solutionPosigrade1ExpectedVelocity1, solutionPosigrade1ExpectedVelocity2, 0.1, 1e-3);
    }

    // Example 7-12 (page 341) from An Introduction to the Mathematics and Methods of Astrodynamics (Revised Edition) by Richard H. Battin

    @Test
    void testLambertBattinExample712() {
        final double mu = 1.32712440018e20;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date1 = new AbsoluteDate(2005, 6, 2, 10, 0, 0.0, utc);
        final Vector3D position1 = new Vector3D(0.159321004, 0.579266185, 0.052359607).scalarMultiply(149597870691.0);
        final AbsoluteDate date2 = new AbsoluteDate(date1, 0.010794065 * 365.25 * 86400.0);
        final Vector3D position2 = new Vector3D(0.0575943377, 0.605750797, 0.068345246).scalarMultiply(149597870691.0);
        final Frame inertialFrame = FramesFactory.getICRF();
        final LambertBoundaryConditions boundaryConditions =
                new LambertBoundaryConditions(date1, position1, date2, position2, inertialFrame);

        final LambertSolver iod = new LambertSolver(mu);

        final List<LambertSolution> solutionsPosigrade = iod.solve(
                true,
                boundaryConditions);

        // Check the number of solutions
        Assertions.assertEquals(1, solutionsPosigrade.size());

        // Check the single posigrade solutions (no complete revolutions)
        final Vector3D solutionPosigrade1ExpectedVelocity1 = new Vector3D(-9.303603251, 3.018641330, 1.5363621434).scalarMultiply(149597870691.0 / 31557600.0);
        // since Battin does not give expected v2, match it against results with other implementations
        final Vector3D solutionPosigrade1ExpectedVelocity2 = new Vector3D(-45087.49723745, 8953.99053394, 6738.00090557);

        checkLambertSolution(solutionsPosigrade.get(0), solutionPosigrade1ExpectedVelocity1, solutionPosigrade1ExpectedVelocity2, 1, 1e-3);
    }

    private static void checkLambertSolution(final LambertSolution solution,
                                     final Vector3D expectedVelocity1,
                                     final Vector3D expectedVelocity2,
                                     final double absoluteTolerance,
                                     final double relativeTolerance) {
        Assertions.assertEquals(expectedVelocity1.getX(), solution.getBoundaryVelocities().getInitialVelocity().getX(), FastMath.max(absoluteTolerance, relativeTolerance * FastMath.abs(expectedVelocity1.getX())));
        Assertions.assertEquals(expectedVelocity1.getY(), solution.getBoundaryVelocities().getInitialVelocity().getY(), FastMath.max(absoluteTolerance, relativeTolerance * FastMath.abs(expectedVelocity1.getY())));
        Assertions.assertEquals(expectedVelocity1.getZ(), solution.getBoundaryVelocities().getInitialVelocity().getZ(), FastMath.max(absoluteTolerance, relativeTolerance * FastMath.abs(expectedVelocity1.getZ())));
        Assertions.assertEquals(expectedVelocity2.getX(), solution.getBoundaryVelocities().getTerminalVelocity().getX(), FastMath.max(absoluteTolerance, relativeTolerance * FastMath.abs(expectedVelocity2.getX())));
        Assertions.assertEquals(expectedVelocity2.getY(), solution.getBoundaryVelocities().getTerminalVelocity().getY(), FastMath.max(absoluteTolerance, relativeTolerance * FastMath.abs(expectedVelocity2.getY())));
        Assertions.assertEquals(expectedVelocity2.getZ(), solution.getBoundaryVelocities().getTerminalVelocity().getZ(), FastMath.max(absoluteTolerance, relativeTolerance * FastMath.abs(expectedVelocity2.getZ())));
    }

    private static void checkJacobianWithFiniteDifferences(final LambertSolver solver, final boolean posigrade,
                                                           final int nRev,
                                                           final LambertBoundaryConditions boundaryConditions,
                                                           final RealMatrix actualJacobian) {
        checkPartialDerivativesWrtTimes(solver, posigrade, nRev, boundaryConditions, actualJacobian);
        final double dV = 1e-1;
        final double toleranceForVelocity = 1e-7;
        for (int i = 1; i < 4; i++) {
            final double[] shift = new double[8];
            shift[i] = -dV/2.;
            final LambertBoundaryVelocities solutionBefore = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift)).get(0).getBoundaryVelocities();
            shift[i] = dV/2.;
            final LambertBoundaryVelocities solutionAfter = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift)).get(0).getBoundaryVelocities();
            checkColumn(solutionBefore, solutionAfter, dV, actualJacobian.getColumn(i), toleranceForVelocity);
        }
        for (int i = 5; i < 8; i++) {
            final double[] shift = new double[8];
            shift[i] = -dV/2.;
            final LambertBoundaryVelocities solutionBefore = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift)).get(0).getBoundaryVelocities();
            shift[i] = dV/2.;
            final LambertBoundaryVelocities solutionAfter = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift)).get(0).getBoundaryVelocities();
            checkColumn(solutionBefore, solutionAfter, dV, actualJacobian.getColumn(i), toleranceForVelocity);
        }
    }

    private static void checkPartialDerivativesWrtTimes(final LambertSolver solver, final boolean posigrade,
                                                        final int nRev,
                                                        final LambertBoundaryConditions boundaryConditions,
                                                        final RealMatrix actualJacobian) {
        final double dt = 1.;
        final double toleranceForTime = 1e-6;
        final LambertBoundaryVelocities solution0Before = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {-dt/2., 0., 0., 0., 0., 0., 0., 0.})).get(0).getBoundaryVelocities();
        final LambertBoundaryVelocities solution0After = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {dt/2., 0., 0., 0., 0., 0., 0., 0.})).get(0).getBoundaryVelocities();
        checkColumn(solution0Before, solution0After, dt, actualJacobian.getColumn(0), toleranceForTime);
        final LambertBoundaryVelocities solution7Before = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {0., 0., 0., 0., -dt/2., 0., 0., 0.})).get(0).getBoundaryVelocities();
        final LambertBoundaryVelocities solution7After = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {0., 0., 0., 0., dt/2.,0., 0., 0.})).get(0).getBoundaryVelocities();
        checkColumn(solution7Before, solution7After, dt, actualJacobian.getColumn(4), toleranceForTime);
    }

    private static void checkColumn(final LambertBoundaryVelocities solutionBefore,
                                    final LambertBoundaryVelocities solutionAfter,
                                    final double dP, final double[] actualColumn, final double tolerance) {
        final double[] difference = new double[actualColumn.length];
        final Vector3D differenceInitial = solutionAfter.getInitialVelocity().subtract(solutionBefore.getInitialVelocity());
        final Vector3D differenceTerminal = solutionAfter.getTerminalVelocity().subtract(solutionBefore.getTerminalVelocity());
        difference[0] = differenceInitial.getX() / dP;
        difference[1] = differenceInitial.getY() / dP;
        difference[2] = differenceInitial.getZ() / dP;
        difference[3] = differenceTerminal.getX() /dP;
        difference[4] = differenceTerminal.getY() / dP;
        difference[5] = differenceTerminal.getZ() / dP;
        assertArrayEquals(difference, actualColumn, tolerance);
    }

    private static LambertBoundaryConditions perturbConditions(final LambertBoundaryConditions conditions,
                                                               final double[] shift) {
        return new LambertBoundaryConditions(conditions.getInitialDate().shiftedBy(shift[0]),
                conditions.getInitialPosition().add(new Vector3D(shift[1], shift[2], shift[3])),
                conditions.getTerminalDate().shiftedBy(shift[4]),
                conditions.getTerminalPosition().add(new Vector3D(shift[5], shift[6], shift[7])),
                conditions.getReferenceFrame());
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
