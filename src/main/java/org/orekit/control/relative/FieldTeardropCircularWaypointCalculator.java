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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Derivative;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.analysis.solvers.NewtonRaphsonSolver;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireEquations;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireMatrices;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store and compute the waypoints representing a teardrop maneuver sequence.
 * <p>
 * Note: The analytical solution of the Teardrop maneuver sequence is valid only using the Clohessy-Wiltshire equations
 * (circular orbit).
 * </p>
 *
 * @param <T> Any scalar field
 * @author Romain Cuvillon
 * @since 14.0
 */
public class FieldTeardropCircularWaypointCalculator<T extends CalculusFieldElement<T>> {

    /**
     * Number of orbits to consider. Must be ≥ 1.
     */
    private final int numberOfOrbits;

    /**
     * Mean motion of the target orbit.
     */
    private final T targetMeanMotion;

    /**
     * Turn-around distance of the teardrop orbit. This is the "round" end of the orbit. Note that this distance is
     * signed : negative means below the target spacecraft (in between the planet and the target), while positive means
     * above the target (target is in between the chaser and the planet).
     */
    private final T turnAroundDistance;

    /**
     * Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed
     * : negative means below the target spacecraft (in between the planet and the target), while positive means above
     * the target (target is in between the chaser and the planet).
     */
    private final T maneuverDistance;

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
     * @param numberOfOrbits     Number of teardrop orbits to perform. Must be ≥ 1.
     */
    public FieldTeardropCircularWaypointCalculator(final T targetMeanMotion, final T turnAroundDistance,
                                                   final T maneuverDistance, final int numberOfOrbits) {
        this.targetMeanMotion   = targetMeanMotion;
        this.turnAroundDistance = turnAroundDistance;
        this.maneuverDistance   = maneuverDistance;
        this.numberOfOrbits     = FastMath.max(1, numberOfOrbits); // Ensure that the number of orbits is ≥ 1.
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
    public List<TimeStampedFieldPVCoordinates<T>> computeTearDropWaypoints(final FieldAbsoluteDate<T> injectionDate) {
        final Field<T> field = injectionDate.getField();
        // Define start and end points of the first arc (from turn-around to maneuver point)
        final FieldVector3D<T> posTurnAround =
                        new FieldVector3D<>(turnAroundDistance, field.getZero(), field.getZero());

        // Compute the relative orbit's period
        final T halfRelativePeriod = computeRelativeOrbitalPeriod().divide(2.0);

        // Compute the norm of Y-axis aligned initial velocity v0 to reach the desired target X value at a given time using the Clohessy-Wiltshire equations
        final T v0Xtgt = targetMeanMotion.multiply(maneuverDistance.add(
                                                                                   turnAroundDistance.multiply(3).multiply(targetMeanMotion.multiply(halfRelativePeriod).cos()))
                                                                   .subtract(turnAroundDistance.multiply(4)))
                                         .divide(targetMeanMotion.multiply(halfRelativePeriod).cos().subtract(1)
                                                                 .multiply(2));

        final TimeStampedFieldPVCoordinates<T> pvtInjection = new TimeStampedFieldPVCoordinates<>(injectionDate,
                                                                                                  new FieldPVCoordinates<>(
                                                                                                                  posTurnAround,
                                                                                                                  new FieldVector3D<>(
                                                                                                                                  field.getZero(),
                                                                                                                                  v0Xtgt.negate(),
                                                                                                                                  field.getZero())));

        // Propagate chaser motion using Clohessy-Wiltshire equations and initial conditions for t = halfRelativePeriod
        final FieldClohessyWiltshireMatrices<T> cwMatrices =
                        (new FieldClohessyWiltshireEquations<T>()).computeMatrices(halfRelativePeriod,
                                                                                   targetMeanMotion);
        final TimeStampedFieldPVCoordinates<T> pvtBeforeMan = cwMatrices.transform(pvtInjection);

        // Compute the PVT at maneuver point after the maneuver : same velocity along the Y axis, but reversed velocity along the X axis. The Z velocity is zero for a circular Keplerian orbit (Clohessy-Wiltshire theory).
        final TimeStampedFieldPVCoordinates<T> pvtAfterMan = new TimeStampedFieldPVCoordinates<>(pvtBeforeMan.getDate(),
                                                                                                 new FieldPVCoordinates<>(
                                                                                                                 pvtBeforeMan.getPosition(),
                                                                                                                 new FieldVector3D<>(
                                                                                                                                 pvtBeforeMan.getVelocity()
                                                                                                                                             .getX()
                                                                                                                                             .negate(),
                                                                                                                                 pvtBeforeMan.getVelocity()
                                                                                                                                             .getY(),
                                                                                                                                 pvtBeforeMan.getVelocity()
                                                                                                                                             .getZ())));

        // Generate waypoints for each maneuver with the correct post-maneuver velocity
        final List<TimeStampedFieldPVCoordinates<T>> waypoints = new ArrayList<>();
        waypoints.add(pvtInjection);

        // Add one waypoint to ensure that the correct number of iterations is performed. One iteration = from a maneuver to the next maneuver.
        for (int orbitNumber = 0; orbitNumber < numberOfOrbits + 1; orbitNumber++) {
            waypoints.add(new TimeStampedFieldPVCoordinates<>(
                            pvtInjection.getDate().shiftedBy(halfRelativePeriod.multiply((2 * orbitNumber) + 1)),
                            new FieldPVCoordinates<>(pvtAfterMan.getPosition(), pvtAfterMan.getVelocity())));
        }

        return waypoints;
    }

    /**
     * Computes the relative orbit's period. Depends on the target's orbital pulsation and the geometry of the teardrop
     * relative orbit.
     *
     * @return Period of the relative orbit, in seconds.
     */
    public T computeRelativeOrbitalPeriod() {
        // Solve the Clohessy-Wiltshire equations to acquire the value of the time so that the Y coordinate is zero while the X coordinate is maneuverDistance
        // The function looks like a tangent function with f(0) -> -∞ and f(orbital period) -> +∞.
        // It happens that it has exactly one root in t ∈ ]0 ; orbital period[, and a mirrored root in t ∈ ]-orbital period ; 0[.
        // If the solver jumps to the negative side, the root can still be used.
        return targetMeanMotion.getField().getOne().multiply(2.0 * FastMath.abs((new NewtonRaphsonSolver()).solve(1000,
                                                                                                                  new yEquation(targetMeanMotion,
                                                                                                                                maneuverDistance,
                                                                                                                                turnAroundDistance),
                                                                                                                  1e-12,
                                                                                                                  getTargetKeplerianPeriod().getReal(),
                                                                                                                  1)));
    }

    /**
     * Equation to solve to find the relative orbital period.
     * <p>
     * Here the "value(double)" method may lose the "fielded" part of the computation. This is done so as Hipparchus
     * does not have fielded version of UnivariateDifferentiableFunction.
     * </p>
     */
    private class yEquation implements UnivariateDifferentiableFunction {

        /**
         * targetMeanMotion to compute tearDrop relative Orbital Period.
         */
        private final T targetMeanMotion;

        /**
         * maneuverDistance to compute tearDrop relative Orbital Period.
         */
        private final T maneuverDistance;

        /**
         * turnAroundDistance to compute tearDrop relative Orbital Period.
         */
        private final T turnAroundDistance;

        private yEquation(final T targetMeanMotion, final T maneuverDistance, final T turnAroundDistance) {
            this.targetMeanMotion   = targetMeanMotion;
            this.maneuverDistance   = maneuverDistance;
            this.turnAroundDistance = turnAroundDistance;
        }

        public double value(final double t) {
            final double maneuverDistanceReal = maneuverDistance.getReal();
            final double targetMeanMotionReal = targetMeanMotion.getReal();
            final double turnAroundDistanceReal = turnAroundDistance.getReal();
            return (3 * maneuverDistanceReal * targetMeanMotionReal * t -
                    3 * targetMeanMotionReal * t * turnAroundDistanceReal * FastMath.cos(targetMeanMotionReal * t) -
                    4 * (maneuverDistanceReal - turnAroundDistanceReal) * FastMath.sin(targetMeanMotionReal * t)) /
                   (2. * (-1 + FastMath.cos(targetMeanMotionReal * t)));
        }

        public <M extends Derivative<M>> M value(final M t) {
            final double maneuverDistanceReal = maneuverDistance.getReal();
            final double targetMeanMotionReal = targetMeanMotion.getReal();
            final double turnAroundDistanceReal = turnAroundDistance.getReal();
            return (t.multiply(3 * maneuverDistanceReal * targetMeanMotionReal)
                     .subtract(t.multiply(3 * targetMeanMotionReal * turnAroundDistanceReal)
                                .multiply(t.multiply(targetMeanMotionReal).cos()))
                     .subtract(t.multiply(targetMeanMotionReal).sin()
                                .multiply(4 * (maneuverDistanceReal - turnAroundDistanceReal)))).divide(
                            t.multiply(targetMeanMotionReal).cos().add(-1).multiply(2));

        }
    }

    /**
     * Computes and returns the target's Keplerian period, in seconds.
     *
     * @return The target's Keplerian period, in seconds.
     */
    private T getTargetKeplerianPeriod() {
        final T pi = targetMeanMotion.getPi();
        return pi.multiply(2).divide(targetMeanMotion);
    }
}
