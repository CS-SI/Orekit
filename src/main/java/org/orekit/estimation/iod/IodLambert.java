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
package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.orekit.control.heuristics.lambert.LambertBoundaryConditions;
import org.orekit.control.heuristics.lambert.LambertBoundaryVelocities;
import org.orekit.control.heuristics.lambert.LambertSolver;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Lambert position-based Initial Orbit Determination (IOD) algorithm, assuming Keplerian motion.
 * <p>
 * An orbit is determined from two position vectors.
 * @author Joris Olympio
 * @see LambertSolver
 * @since 8.0
 */
public class IodLambert {

    /** gravitational constant. */
    private final double mu;

    /** Creator.
     *
     * @param mu gravitational constant
     */
    public IodLambert(final double mu) {
        this.mu = mu;
    }

    /** Estimate an initial orbit from two position measurements.
     * <p>
     * The logic for setting {@code posigrade} and {@code nRev} is that the
     * sweep angle Δυ travelled by the object between {@code t1} and {@code t2} is
     * 2π {@code nRev +1} - α if {@code posigrade} is false and 2π {@code nRev} + α
     * if {@code posigrade} is true, where α is the separation angle between
     * {@code p1} and {@code p2}, which is always computed between 0 and π
     * (because in 3D without a normal reference, vector angles cannot go past π).
     * </p>
     * <p>
     * This implies that {@code posigrade} should be set to true if {@code p2} is
     * located in the half orbit starting at {@code p1} and it should be set to
     * false if {@code p2} is located in the half orbit ending at {@code p1},
     * regardless of the number of periods between {@code t1} and {@code t2},
     * and {@code nRev} should be set accordingly.
     * </p>
     * <p>
     * As an example, if {@code t2} is less than half a period after {@code t1},
     * then {@code posigrade} should be {@code true} and {@code nRev} should be 0.
     * If {@code t2} is more than half a period after {@code t1} but less than
     * one period after {@code t1}, {@code posigrade} should be {@code false} and
     * {@code nRev} should be 0.
     * </p>
     * @param frame     measurements frame
     * @param posigrade flag indicating the direction of motion
     * @param nRev      number of revolutions
     * @param p1        first position measurement
     * @param p2        second position measurement
     * @return an initial Keplerian orbit estimation at the first observation date t1
     * @since 11.0
     */
    public Orbit estimate(final Frame frame, final boolean posigrade,
                          final int nRev, final Position p1,  final Position p2) {
        return estimate(frame, posigrade, nRev,
                        p1.getPosition(), p1.getDate(), p2.getPosition(), p2.getDate());
    }

    /** Estimate an initial orbit from two PV measurements.
     * <p>
     * The logic for setting {@code posigrade} and {@code nRev} is that the
     * sweep angle Δυ travelled by the object between {@code t1} and {@code t2} is
     * 2π {@code nRev +1} - α if {@code posigrade} is false and 2π {@code nRev} + α
     * if {@code posigrade} is true, where α is the separation angle between
     * {@code p1} and {@code p2}, which is always computed between 0 and π
     * (because in 3D without a normal reference, vector angles cannot go past π).
     * </p>
     * <p>
     * This implies that {@code posigrade} should be set to true if {@code p2} is
     * located in the half orbit starting at {@code p1} and it should be set to
     * false if {@code p2} is located in the half orbit ending at {@code p1},
     * regardless of the number of periods between {@code t1} and {@code t2},
     * and {@code nRev} should be set accordingly.
     * </p>
     * <p>
     * As an example, if {@code t2} is less than half a period after {@code t1},
     * then {@code posigrade} should be {@code true} and {@code nRev} should be 0.
     * If {@code t2} is more than half a period after {@code t1} but less than
     * one period after {@code t1}, {@code posigrade} should be {@code false} and
     * {@code nRev} should be 0.
     * </p>
     * @param frame     measurements frame
     * @param posigrade flag indicating the direction of motion
     * @param nRev      number of revolutions
     * @param pv1       first PV measurement
     * @param pv2       second PV measurement
     * @return an initial Keplerian orbit estimation at the first observation date t1
     * @since 12.0
     */
    public Orbit estimate(final Frame frame, final boolean posigrade,
                          final int nRev, final PV pv1,  final PV pv2) {
        return estimate(frame, posigrade, nRev,
                        pv1.getPosition(), pv1.getDate(), pv2.getPosition(), pv2.getDate());
    }

    /** Estimate a Keplerian orbit given two position vectors and a duration.
     * <p>
     * The logic for setting {@code posigrade} and {@code nRev} is that the
     * sweep angle Δυ travelled by the object between {@code t1} and {@code t2} is
     * 2π {@code nRev +1} - α if {@code posigrade} is false and 2π {@code nRev} + α
     * if {@code posigrade} is true, where α is the separation angle between
     * {@code p1} and {@code p2}, which is always computed between 0 and π
     * (because in 3D without a normal reference, vector angles cannot go past π).
     * </p>
     * <p>
     * This implies that {@code posigrade} should be set to true if {@code p2} is
     * located in the half orbit starting at {@code p1} and it should be set to
     * false if {@code p2} is located in the half orbit ending at {@code p1},
     * regardless of the number of periods between {@code t1} and {@code t2},
     * and {@code nRev} should be set accordingly.
     * </p>
     * <p>
     * As an example, if {@code t2} is less than half a period after {@code t1},
     * then {@code posigrade} should be {@code true} and {@code nRev} should be 0.
     * If {@code t2} is more than half a period after {@code t1} but less than
     * one period after {@code t1}, {@code posigrade} should be {@code false} and
     * {@code nRev} should be 0.
     * </p>
     * @param frame     frame
     * @param posigrade flag indicating the direction of motion
     * @param nRev      number of revolutions
     * @param p1        position vector 1
     * @param t1        date of observation 1
     * @param p2        position vector 2
     * @param t2        date of observation 2
     * @return  an initial Keplerian orbit estimate at the first observation date t1
     */
    public Orbit estimate(final Frame frame, final boolean posigrade,
                          final int nRev,
                          final Vector3D p1, final AbsoluteDate t1,
                          final Vector3D p2, final AbsoluteDate t2) {
        // Exception if t2 < t1
        final double tau = t2.durationFrom(t1); // in seconds
        if (tau < 0.) {
            throw new OrekitException(OrekitMessages.NON_CHRONOLOGICAL_DATES_FOR_OBSERVATIONS, t1, t2, -tau);
        }
        final LambertSolver solver = new LambertSolver(mu);
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(t1, p1, t2, p2,
                frame);
        final LambertBoundaryVelocities velocities = solver.solve(posigrade, nRev, boundaryConditions);
        if (velocities == null) {
            return null;
        } else {
            return new CartesianOrbit(new TimeStampedPVCoordinates(t1, p1, velocities.getInitialVelocity()),
                    frame, mu);
        }
    }

    /** Lambert's solver for the historical, planar problem.
     * Assume mu=1.
     *
     * @param r1 radius 1
     * @param r2  radius 2
     * @param dth sweep angle
     * @param tau time of flight
     * @param mRev number of revs
     * @param V1 velocity at departure in (T, N) basis
     * @return exit flag
     * @deprecated as of 13.1, use {@link LambertSolver}
     */
    boolean solveLambertPb(final double r1, final double r2, final double dth, final double tau,
                           final int mRev, final double[] V1) {
        final LambertSolver solver = new LambertSolver(mu);
        final Vector2D solution = solver.solveNormalized2D(r1, r2, dth, tau, mRev);
        if (solution == Vector2D.NaN) {
            return false;
        } else {
            V1[0] = solution.getX();
            V1[1] = solution.getY();
            return true;
        }
    }

}
