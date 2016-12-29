/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Lambert initial orbit determination, assuming Keplerian motion.
 * An orbit is determined from two position vectors.
 *
 * References:
 *  Battin, R.H., An Introduction to the Mathematics and Methods of Astrodynamics, AIAA Education, 1999.
 *  Lancaster, E.R. and Blanchard, R.C., A Unified Form of Lambert’s Theorem, Goddard Space Flight Center, 1968.
 *
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

    /** Estimate a keplerian orbit given two position vectors and a duration.
     *
     * @param frame     frame
     * @param posigrade flag indicating the direction of motion
     * @param nRev      number of revolutions
     * @param P1        position vector 1
     * @param T1        date of observation 1
     * @param P2        position vector 2
     * @param T2        date of observation 2
     * @return  an initial keplerian orbit estimate
     */
    public KeplerianOrbit estimate(final Frame frame, final boolean posigrade,
                                   final int nRev,
                                   final Vector3D P1, final AbsoluteDate T1,
                                   final Vector3D P2, final AbsoluteDate T2) {

        final double r1 = P1.getNorm();
        final double r2 = P2.getNorm();
        final double tau = T2.durationFrom(T1); // in seconds

        // normalizing constants
        final double R = FastMath.max(r1, r2); // in m
        final double V = FastMath.sqrt(mu / R);  // in m/s
        final double T = R / V; // in seconds

        // sweep angle
        final Vector3D P12 = P1.crossProduct(P2);
        double dth = FastMath.atan2(P12.getNorm(), P1.dotProduct(P2));
        // compute the number of revolutions
        if (!posigrade) {
            dth = FastMath.PI - dth;
        }
        dth = dth + nRev * FastMath.PI;

        // velocity vectors in the orbital plane, in the R-T frame
        final double[] Vdep = new double[2];

        // call Lambert's problem solver
        final boolean exitflag = solveLambertPb(r1 / R, r2 / R, dth, tau / T, nRev, Vdep);

        if (exitflag) {
            // basis vectors
            // normal to the orbital arc plane
            final Vector3D Pn = P1.crossProduct(P2);
            // perpendicular to the radius vector, in the orbital arc plane
            final Vector3D Pt = Pn.crossProduct(P1);

            // tangential velocity norm
            double RT = Pt.getNorm();
            if (!posigrade) {
                RT = -RT;
            }

            // velocity vector at P1
            final Vector3D Vel1 = P1.scalarMultiply(V * Vdep[0] / r1).add(Pt.scalarMultiply(V * Vdep[1] / RT));

            // compile a new middle point with position, velocity
            final PVCoordinates pv = new PVCoordinates(P1, Vel1);

            // compute the equivalent keplerian orbit
            return new KeplerianOrbit(pv, frame, T1, mu);
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
     * @param V1 velocity at departure in (T,N) basis
     * @return something
     */
    public boolean solveLambertPb(final double r1, final double r2, final double dth, final double tau,
                                  final int mRev, final double[] V1) {
        // decide whether to use the left or right branch (for multi-revolution
        // problems), and the long- or short way.
        final boolean leftbranch = FastMath.signum(mRev) > 0;
        int longway = 0;
        if (tau > 0) {
            longway = 1;
        }

        final int m = FastMath.abs(mRev);
        final double rtof = FastMath.abs(tau);
        double theta = dth;
        if (longway < 0) {
            theta = 2 * FastMath.PI - dth;
        }

        // non-dimensional chord ||r2-r1||
        final double chord = FastMath.sqrt(r1 * r1 + r2 * r2 - 2 * r1 * r2 * FastMath.cos(theta));

        // non-dimensional semi-perimeter of the triangle
        final double speri = 0.5 * (r1 + r2 + chord);

        // minimum energy ellipse semi-major axis
        final double minSma = speri / 2.;

        // lambda parameter (Eq 7.6)
        final double lambda = FastMath.sqrt(1 - chord / speri);

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
        while ((err > tol) && (iterations < maxiter)) {
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
            final double etaSq = 2 * sma * FastMath.pow(FastMath.sin(psi), 2) / speri;
            eta  = FastMath.sqrt(etaSq);
        } else {
            // hyperbola
            final double gamma = 2 * FastMath.acosh(x);
            final double delta = longway * 2 * FastMath.asinh(FastMath.sqrt((chord - speri) / (2 * sma)));
            //
            final double psi  = (gamma - delta) / 2.;
            final double etaSq = -2 * sma * FastMath.pow(FastMath.sinh(psi), 2) / speri;
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
