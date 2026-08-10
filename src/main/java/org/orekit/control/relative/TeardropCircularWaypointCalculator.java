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
package org.orekit.control.relative;

import org.hipparchus.analysis.differentiation.Derivative;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.analysis.solvers.NewtonRaphsonSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireEquations;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireMatrices;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store and compute the waypoints representing a teardrop maneuver sequence.
 * <p>
 * Note: The analytical solution of the Teardrop maneuver sequence is valid only using the Clohessy-Wiltshire equations
 * (circular orbit).
 * </p>
 *
 * @author Romain Cuvillon
 * @author Jérôme Tabeaud
 * @since 14.0
 */

public class TeardropCircularWaypointCalculator {

    /**
     * Number of orbits to consider. Must be ≥ 1.
     */
    private final int numberOfOrbits;

    /**
     * Mean motion of the target orbit.
     */
    private final double targetMeanMotion;

    /**
     * Turn-around distance of the teardrop orbit. This is the "round" end of the orbit. Note that this distance is
     * signed : negative means below the target spacecraft (in between the planet and the target), while positive means
     * above the target (target is in between the chaser and the planet).
     */
    private final double turnAroundDistance;

    /**
     * Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed
     * : negative means below the target spacecraft (in between the planet and the target), while positive means above
     * the target (target is in between the chaser and the planet).
     */
    private final double maneuverDistance;

    /**
     * Creates a new teardrop relative orbit calculator.
     *
     * @param targetMeanMotion   Target spacecraft's orbital mean motion, in rad/s.
     * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is
     *                           signed : negative means below the target spacecraft (in between the planet and the
     *                           target), while positive means above the target (target is in between the chaser and the
     *                           planet).
     * @param maneuverDistance   Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note
     *                           that this distance is signed : negative means below the target spacecraft (in between
     *                           the planet and the target), while positive means above the target (target is in between
     *                           the chaser and the planet).
     * @param numberOfTearDrops  Number of teardrop orbits to perform. Must be ≥ 1.
     */
    public TeardropCircularWaypointCalculator(final double targetMeanMotion, final double turnAroundDistance,
                                              final double maneuverDistance, final int numberOfTearDrops) {
        this.targetMeanMotion   = targetMeanMotion;
        this.turnAroundDistance = turnAroundDistance;
        this.maneuverDistance   = maneuverDistance;
        this.numberOfOrbits     = FastMath.max(1, numberOfTearDrops); // Ensure that the number of orbits is ≥ 1.
    }

    /**
     * Computes the waypoints of the teardrop relative orbit in QSW Local Orbital Frame to use them with
     * Clohessy-Wiltshire maneuvers.
     * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
     * <p>All maneuvers happen at the pointy end of the teardrop.</p>
     *
     * @param injectionDate Date of the first waypoint, which corresponds to the injection point of the teardrop orbit.
     * @return List of waypoints in time. Date, position, and velocity are non-zero.
     */
    public List<TimeStampedPVCoordinates> computeTearDropWaypoints(final AbsoluteDate injectionDate) {
        // Define start and end points of the first arc (from turn-around to maneuver point)
        final Vector3D posTurnAround = new Vector3D(turnAroundDistance, 0.0, 0.0);

        // Compute the relative orbit's period
        final double halfRelativePeriod = computeRelativeOrbitalPeriod() / 2.0;

        // Compute the norm of Y-axis aligned initial velocity v0 to reach the desired target X value at a given time using the Clohessy-Wiltshire equations
        final double v0Xtgt = targetMeanMotion * (maneuverDistance + 3 * turnAroundDistance * FastMath.cos(
                        targetMeanMotion * halfRelativePeriod) - 4 * turnAroundDistance) / (2 * (FastMath.cos(
                        targetMeanMotion * halfRelativePeriod) - 1));

        final TimeStampedPVCoordinates pvtInjection =
                        new TimeStampedPVCoordinates(injectionDate, posTurnAround, new Vector3D(0.0, -v0Xtgt, 0.0));

        // Propagate chaser motion using Clohessy-Wiltshire equations and initial conditions for t = halfRelativePeriod
        final ClohessyWiltshireMatrices cwMatrices =
                        ClohessyWiltshireEquations.computeMatrices(halfRelativePeriod, targetMeanMotion);
        final TimeStampedPVCoordinates pvtBeforeMan = cwMatrices.transform(pvtInjection);

        // Compute the PVT at maneuver point after the maneuver : same velocity along the Y axis, but reversed velocity along the X axis. The Z velocity is zero for a circular Keplerian orbit (Clohessy-Wiltshire theory).
        final TimeStampedPVCoordinates pvtAfterMan =
                        new TimeStampedPVCoordinates(pvtBeforeMan.getDate(), pvtBeforeMan.getPosition(),
                                                     new Vector3D(-pvtBeforeMan.getVelocity().getX(),
                                                                  pvtBeforeMan.getVelocity().getY(),
                                                                  pvtBeforeMan.getVelocity().getZ()));

        // Generate waypoints for each maneuver with the correct post-maneuver velocity
        final List<TimeStampedPVCoordinates> waypoints = new ArrayList<>();
        waypoints.add(pvtInjection);

        // Add one waypoint to ensure that the correct number of iterations is performed. One iteration = from a maneuver to the next maneuver.
        for (int orbitNumber = 0; orbitNumber < numberOfOrbits + 1; orbitNumber++) {
            waypoints.add(new TimeStampedPVCoordinates(
                            pvtInjection.getDate().shiftedBy(((2 * orbitNumber) + 1) * halfRelativePeriod),
                            pvtAfterMan.getPosition(), pvtAfterMan.getVelocity()));
        }

        return waypoints;
    }

    /**
     * Computes the relative orbit's period. Depends on the target's orbital pulsation and the geometry of the teardrop
     * relative orbit.
     *
     * @return Period of the relative orbit, in seconds.
     */
    public double computeRelativeOrbitalPeriod() {
        // Solve the Clohessy-Wiltshire equations to acquire the value of the time so that the Y coordinate is zero while the X coordinate is maneuverDistance
        // The function looks like a tangent function with f(0) -> -∞ and f(orbital period) -> +∞.
        // It happens that it has exactly one root in t ∈ ]0 ; orbital period[, and a mirrored root in t ∈ ]-orbital period ; 0[.
        // If the solver jumps to the negative side, the root can still be used.
        return 2.0 * FastMath.abs((new NewtonRaphsonSolver()).solve(1000,
                                                                    new yEquation(targetMeanMotion, maneuverDistance,
                                                                                  turnAroundDistance), 1e-12,
                                                                    getTargetKeplerianPeriod(), 1));
    }

    /**
     * Equation to solve to find the relative orbital period.
     *
     * @param targetMeanMotion   targetMeanMotion to compute tearDrop relative Orbital Period.
     * @param maneuverDistance   maneuverDistance to compute tearDrop relative Orbital Period.
     * @param turnAroundDistance turnAroundDistance to compute tearDrop relative Orbital Period.
     */
    private record yEquation(double targetMeanMotion, double maneuverDistance, double turnAroundDistance)
                    implements UnivariateDifferentiableFunction {

        public double value(final double t) {
            return (3 * maneuverDistance * targetMeanMotion * t -
                    3 * targetMeanMotion * t * turnAroundDistance * FastMath.cos(targetMeanMotion * t) -
                    4 * (maneuverDistance - turnAroundDistance) * FastMath.sin(targetMeanMotion * t)) /
                   (2. * (-1 + FastMath.cos(targetMeanMotion * t)));
        }

        public <T extends Derivative<T>> T value(final T t) {
            return (t.multiply(3 * maneuverDistance * targetMeanMotion)
                     .subtract(t.multiply(3 * targetMeanMotion * turnAroundDistance)
                                .multiply(t.multiply(targetMeanMotion).cos()))
                     .subtract(t.multiply(targetMeanMotion).sin()
                                .multiply(4 * (maneuverDistance - turnAroundDistance)))).divide(
                            t.multiply(targetMeanMotion).cos().add(-1).multiply(2));
        }
    }

    /**
     * Computes and returns the target's Keplerian period, in seconds.
     *
     * @return The target's Keplerian period, in seconds.
     */
    private double getTargetKeplerianPeriod() {
        return 2 * FastMath.PI / targetMeanMotion;
    }
}

