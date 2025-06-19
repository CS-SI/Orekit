/* Copyright 2002-2025 CS GROUP
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
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.TimeStampedGeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.KinematicTransform;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.junit.jupiter.api.Assertions.*;

class LambertSolverTest {

    @Test
    void testSolveFailure() {
        // GIVEN
        final LambertSolver solver = new LambertSolver(1.);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(date,
                Vector3D.PLUS_I, date.shiftedBy(1), Vector3D.PLUS_J, FramesFactory.getEME2000());
        // WHEN
        final LambertBoundaryVelocities solution = solver.solve(true, 10, boundaryConditions);
        final RealMatrix jacobian = solver.computeJacobian(true, 10, boundaryConditions);
        // THEN
        assertNull(solution);
        assertTrue(Double.isNaN(jacobian.getEntry(0,0)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e4, 1e5, 1e6, 1e7})
    void testSolveGeoTimes(final double timeOfFlight) {
        testSolveGeo(timeOfFlight, 0.1, true);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.2, 0.3})
    void testSolveGeoLongitude(final double longitude) {
        testSolveGeo(Constants.JULIAN_DAY * 1.5, longitude, true);
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
        final LambertBoundaryVelocities solution = solver.solve(posigrade, nRev, boundaryConditions);
        // THEN
        final Orbit lambertOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(orbit1.getDate(), orbit1.getPosition(), solution.getInitialVelocity()),
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
        final Orbit orbit1 = new CartesianOrbit(new TimeStampedPVCoordinates(date, new Vector3D(7.557249611265823E9, -4.1474095848830185E10, -958845.3227159644),
                new Vector3D(3024339.1189172766, 551083.3948309845, 96.84004762824829)), frame, mu);
        final double timeOfFlight = Constants.JULIAN_DAY * 1.5;
        final Orbit orbit2 = new CartesianOrbit(new TimeStampedPVCoordinates(date.shiftedBy(timeOfFlight),
                new Vector3D(-1.6651012438926659E10, 3.8729271015852745E10, 650886.2988349741),
                new Vector3D(-2824183.311624355, -1214211.1159693908, -108.81575040466626)), frame, mu);
        final LambertSolver solver = new LambertSolver(orbit2.getMu());
        final int nRev = (int) FastMath.floor(timeOfFlight / orbit1.getKeplerianPeriod());
        final boolean posigrade = true;
        // WHEN
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(orbit1.getDate(),
                orbit1.getPosition(), orbit2.getDate(), orbit2.getPosition(), orbit2.getFrame());
        final RealMatrix jacobian = solver.computeJacobian(posigrade, nRev, boundaryConditions);
        // THEN
        assertFalse(Double.isNaN(jacobian.getEntry(0, 0)));
        checkJacobianWithFiniteDifferences(solver, posigrade, nRev, boundaryConditions, jacobian);
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
            final LambertBoundaryVelocities solutionBefore = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift));
            shift[i] = dV/2.;
            final LambertBoundaryVelocities solutionAfter = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift));
            checkColumn(solutionBefore, solutionAfter, dV, actualJacobian.getColumn(i), toleranceForVelocity);
        }
        for (int i = 5; i < 8; i++) {
            final double[] shift = new double[8];
            shift[i] = -dV/2.;
            final LambertBoundaryVelocities solutionBefore = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift));
            shift[i] = dV/2.;
            final LambertBoundaryVelocities solutionAfter = solver.solve(posigrade, nRev, perturbConditions(boundaryConditions, shift));
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
                perturbConditions(boundaryConditions, new double[] {-dt/2., 0., 0., 0., 0., 0., 0., 0.}));
        final LambertBoundaryVelocities solution0After = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {dt/2., 0., 0., 0., 0., 0., 0., 0.}));
        checkColumn(solution0Before, solution0After, dt, actualJacobian.getColumn(0), toleranceForTime);
        final LambertBoundaryVelocities solution7Before = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {0., 0., 0., 0., -dt/2., 0., 0., 0.}));
        final LambertBoundaryVelocities solution7After = solver.solve(posigrade, nRev,
                perturbConditions(boundaryConditions, new double[] {0., 0., 0., 0., dt/2.,0., 0., 0.}));
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
