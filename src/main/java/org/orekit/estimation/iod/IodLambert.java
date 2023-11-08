/* Copyright 2002-2023 CS GROUP
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
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Lambert position-based Initial Orbit Determination (IOD) algorithm, assuming Keplerian motion.
 * <p>
 * An orbit is determined from two position vectors.
 *
 * References:
 *  Battin, R.H., An Introduction to the Mathematics and Methods of Astrodynamics, AIAA Education, 1999.
 *  Lancaster, E.R. and Blanchard, R.C., A Unified Form of Lambert’s Theorem, Goddard Space Flight Center, 1968.
 * </p>
 * @author Joris Olympio
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

        final double r1 = p1.getNorm();
        final double r2 = p2.getNorm();
        final double tau = t2.durationFrom(t1); // in seconds

        // Exception if t2 < t1
        if (tau < 0.0) {
            throw new OrekitException(OrekitMessages.NON_CHRONOLOGICAL_DATES_FOR_OBSERVATIONS, t1, t2, -tau);
        }

        // normalizing constants
        final double R = FastMath.max(r1, r2); // in m
        final double V = FastMath.sqrt(mu / R);  // in m/s
        final double T = R / V; // in seconds

        // sweep angle
        double dth = Vector3D.angle(p1, p2);
        // compute the number of revolutions
        if (!posigrade) {
            dth = 2 * FastMath.PI - dth;
        }

        // velocity vectors in the orbital plane, in the R-T frame
        final double[] Vdep = new double[2];

        // call Lambert's problem solver
        final boolean exitflag = solveLambertPb(r1 / R, r2 / R, dth, tau / T, nRev, Vdep);

        if (exitflag) {
            // basis vectors
            // normal to the orbital arc plane
            final Vector3D Pn = p1.crossProduct(p2);
            // perpendicular to the radius vector, in the orbital arc plane
            final Vector3D Pt = Pn.crossProduct(p1);

            // tangential velocity norm
            double RT = Pt.getNorm();
            if (!posigrade) {
                RT = -RT;
            }

            // velocity vector at P1
            final Vector3D Vel1 = new Vector3D(V * Vdep[0] / r1, p1,
                                               V * Vdep[1] / RT, Pt);

            // compute the equivalent Cartesian orbit
            return new CartesianOrbit(new PVCoordinates(p1, Vel1), frame, t1, mu);
        }

        return null;
    }

    /** Lambert's solver.
     * Assume mu=1.
     *
     * @param r1 radius 1
     * @param r2  radius 2
     * @param dth sweep angle
     * @param tau time of flight
     * @param mRev number of revs
     * @param V1 velocity at departure in (T, N) basis
     * @return something
     */
    boolean solveLambertPb(final double r1, final double r2, final double dth, final double tau,
                           final int mRev, final double[] V1) {
        // decide whether to use the left or right branch (for multi-revolution
        // problems), and the long- or short way.
        final boolean leftbranch = dth < FastMath.PI;
        int longway = 1;
        if (dth > FastMath.PI) {
            longway = -1;
        }

        final int m = FastMath.abs(mRev);
        final double rtof = FastMath.abs(tau);

        // non-dimensional chord ||r2-r1||
        final double chord = FastMath.sqrt(r1 * r1 + r2 * r2 - 2 * r1 * r2 * FastMath.cos(dth));

        // non-dimensional semi-perimeter of the triangle
        final double speri = 0.5 * (r1 + r2 + chord);

        // minimum energy ellipse semi-major axis
        final double minSma = speri / 2.;

        // lambda parameter (Eq 7.6)
        final double lambda = longway * FastMath.sqrt(1 - chord / speri);

        // reference tof value for the Newton solver
        final double logt = FastMath.log(rtof);

        // initialisation of the iterative root finding process (secant method)
        // initial values
        //  -1 < x < 1  =>  Elliptical orbits
        //  x = 1           Parabolic orbit
        //  x > 1           Hyperbolic orbits
        final double in1;
        final double in2;
        double x1;
        double x2;
        if (m == 0) {
            // one revolution, one solution. Define the left and right asymptotes.
            in1 = -0.6523333;
            in2 = 0.6523333;
            x1   = FastMath.log(1 + in1);
            x2   = FastMath.log(1 + in2);
        } else {
            // select initial values, depending on the branch
            if (!leftbranch) {
                in1 = -0.523334;
                in2 = -0.223334;
            } else {
                in1 = 0.723334;
                in2 = 0.523334;
            }
            x1 = FastMath.tan(in1 * 0.5 * FastMath.PI);
            x2 = FastMath.tan(in2 * 0.5 * FastMath.PI);
        }

        // initial estimates for the tof
        final double tof1 = timeOfFlight(in1, longway, m, minSma, speri, chord);
        final double tof2 = timeOfFlight(in2, longway, m, minSma, speri, chord);

        // initial bounds for y
        double y1;
        double y2;
        if (m == 0) {
            y1 = FastMath.log(tof1) - logt;
            y2 = FastMath.log(tof2) - logt;
        } else {
            y1 = tof1 - rtof;
            y2 = tof2 - rtof;
        }

        // Solve for x with the secant method
        double err = 1e20;
        int iterations = 0;
        final double tol = 1e-13;
        final int maxiter = 50;
        double xnew = 0;
        while (err > tol && iterations < maxiter) {
            // new x
            xnew = (x1 * y2 - y1 * x2) / (y2 - y1);

            // evaluate new time of flight
            final double x;
            if (m == 0) {
                x = FastMath.exp(xnew) - 1;
            } else {
                x = FastMath.atan(xnew) * 2 / FastMath.PI;
            }

            final double tof = timeOfFlight(x, longway, m, minSma, speri, chord);

            // new value of y
            final double ynew;
            if (m == 0) {
                ynew = FastMath.log(tof) - logt;
            } else {
                ynew = tof - rtof;
            }

            // save previous and current values for the next iteration
            x1 = x2;
            x2 = xnew;
            y1 = y2;
            y2 = ynew;

            // update error
            err = FastMath.abs(x1 - xnew);

            // increment number of iterations
            ++iterations;
        }

        // failure to converge
        if (err > tol) {
            return false;
        }

        // convert converged value of x
        final double x;
        if (m == 0) {
            x = FastMath.exp(xnew) - 1;
        } else {
            x = FastMath.atan(xnew) * 2 / FastMath.PI;
        }

        // Solution for the semi-major axis (Eq. 7.20)
        final double sma = minSma / (1 - x * x);

        // compute velocities
        final double eta;
        if (x < 1) {
            // ellipse, Eqs. 7.7, 7.17
            final double alfa = 2 * FastMath.acos(x);
            final double beta = longway * 2 * FastMath.asin(FastMath.sqrt((speri - chord) / (2. * sma)));
            final double psi  = (alfa - beta) / 2;
            // Eq. 7.21
            final double sinPsi = FastMath.sin(psi);
            final double etaSq = 2 * sma * sinPsi * sinPsi / speri;
            eta  = FastMath.sqrt(etaSq);
        } else {
            // hyperbola
            final double gamma = 2 * FastMath.acosh(x);
            final double delta = longway * 2 * FastMath.asinh(FastMath.sqrt((chord - speri) / (2 * sma)));
            //
            final double psi  = (gamma - delta) / 2.;
            final double sinhPsi = FastMath.sinh(psi);
            final double etaSq = -2 * sma * sinhPsi * sinhPsi / speri;
            eta  = FastMath.sqrt(etaSq);
        }

        // radial and tangential directions for departure velocity (Eq. 7.36)
        final double VR1 = (1. / eta) * FastMath.sqrt(1. / minSma) * (2 * lambda * minSma / r1 - (lambda + x * eta));
        final double VT1 = (1. / eta) * FastMath.sqrt(1. / minSma) * FastMath.sqrt(r2 / r1) * FastMath.sin(dth / 2);
        V1[0] = VR1;
        V1[1] = VT1;

        return true;
    }

    /** Compute the time of flight of a given arc of orbit.
     * The time of flight is evaluated via the Lagrange expression.
     *
     * @param x          x
     * @param longway    solution number; the long way or the short war
     * @param mrev       number of revolutions of the arc of orbit
     * @param minSma     minimum possible semi-major axis
     * @param speri      semi-parameter of the arc of orbit
     * @param chord      chord of the arc of orbit
     * @return the time of flight for the given arc of orbit
     */
    private double timeOfFlight(final double x, final int longway, final int mrev, final double minSma,
                                final double speri, final double chord) {

        final double a = minSma / (1 - x * x);

        final double tof;
        if (FastMath.abs(x) < 1) {
            // Lagrange form of the time of flight equation Eq. (7.9)
            // elliptical orbit (note: mu = 1)
            final double beta = longway * 2 * FastMath.asin(FastMath.sqrt((speri - chord) / (2. * a)));
            final double alfa = 2 * FastMath.acos(x);
            tof = a * FastMath.sqrt(a) * ((alfa - FastMath.sin(alfa)) - (beta - FastMath.sin(beta)) + 2 * FastMath.PI * mrev);
        } else {
            // hyperbolic orbit
            final double alfa = 2 * FastMath.acosh(x);
            final double beta = longway * 2 * FastMath.asinh(FastMath.sqrt((speri - chord) / (-2. * a)));
            tof = -a * FastMath.sqrt(-a) * ((FastMath.sinh(alfa) - alfa) - (FastMath.sinh(beta) - beta));
        }

        return tof;
    }
}
