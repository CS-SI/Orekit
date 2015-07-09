/* Copyright 2002-2015 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Lambert initial orbit determination, assuming Keplerian motion.
 * An orbit is determined from two position vectors.
 *
 * Reference: Lambert's problem solver, in MATLAB, by Izzo.
 *
 * @author Joris Olympio
 * @since 7.1
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

    /** Estimate an keplerian orbit given two position vector and a duration.
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
        final double R = r1; // in m
        final double V = FastMath.sqrt(mu / R);  // in m/s
        final double T = R / V; // in seconds

        // sweep angle
        final Vector3D P12 = P1.crossProduct(P2);
        double dth = FastMath.atan2(P12.getNorm(), P1.dotProduct(P2));
        // compute the number of revolutions
        if (!posigrade) {
            dth = Math.PI - dth;
        }
        dth = dth + nRev * Math.PI;

        // velocity vectors in the orbital plane, in the R-T frame
        final double[] V1 = new double[2];
        final double[] V2 = new double[2];

        // call Lambert's problem solver
        final boolean exitflag = SolveLambertPb(r1 / R, r2 / R, dth,
                                          tau / T,
                                          nRev,
                                          V1, V2);

        if (exitflag) {
            // basis vectors
            final Vector3D Pn = P1.crossProduct(P2);    // normal to the orbital arc plane
            final Vector3D Pt = Pn.crossProduct(P1);    // perpendicular to the radius vector, in the orbital arc plane

            // tangential velocity norm
            double RT = Pt.getNorm();
            if (!posigrade) {
                RT = -RT;
            }

            // velocity vector at P1
            final Vector3D Vel1 = P1.scalarMultiply(V * V1[0] / r1)
                            .add(Pt.scalarMultiply(V * V1[1] / RT));

            // compile a new middle point with position, velocity
            final PVCoordinates pv = new PVCoordinates(P1, Vel1);

            // compute the equivalent keplerian orbit
            return new KeplerianOrbit(pv, frame, T1, mu);
        }

        return null;
    }

    /** Lambert's solver.
    *
    * @param r1 radius 1
    * @param r2  radius 2
    * @param dth sweep angle
    * @param tau time of flight
    * @param mRev number of revs
    * @param V1 velocity 1 in (T,N) basis
    * @param V2 velocity 2 in (T,N) basis
    * @return something
    */
    public boolean SolveLambertPb(final double r1, final double r2, final double dth, final double tau,
                                   final int mRev,
                                   final double[] V1, final double[] V2) {
       // decide whether to use the left or right branch (for multi-revolution
       // problems), and the long- or short way.
        final boolean leftbranch = Math.signum(mRev) > 0;
        int longway = 0;
        if (tau > 0) {
            longway = 1;
        }

        final int m = Math.abs(mRev);
        final double T12 = Math.abs(tau);
        double theta = dth;
        if (longway < 0) {
            theta = 2 * Math.PI - dth;
        }

        // non-dimensional chord
        final double chord = FastMath.sqrt(1 + r2 * r2 - 2 * r2 * FastMath.cos(theta));

        // non-dimensional semi-perimeter
        final double s = (1 + r2 + chord) / 2;

        // minimum energy ellipse semi major axis
        final double minSma = s / 2;

        // lambda parameter (from BATTIN's book)
        final double Lambda = FastMath.sqrt(r2) * FastMath.cos(theta / 2) / s;

        // reference tof value for the Newton solver
        final double logt = FastMath.log(T12);

        // initial values
        double inn1;
        double inn2;
        double x1;
        double x2;
        if (m == 0) {
            // one revolution, one solution
            inn1 = -0.5233;      // first initial guess
            inn2 = +0.5233;      // second initial guess
            x1   = FastMath.log(1 + inn1); // transformed first initial guess
            x2   = FastMath.log(1 + inn2); // transformed first second guess
        } else {
            // select initial values, depending on the branch
            if (!leftbranch) {
                inn1 = -0.5234; // first initial guess, left branch
                inn2 = -0.2234; // second initial guess, left branch
            } else {
                inn1 = +0.7234; // first initial guess, right branch
                inn2 = +0.5234; // second initial guess, right branch
            }
            x1 = FastMath.tan(inn1 * Math.PI / 2); // transformed first initial guess
            x2 = FastMath.tan(inn2 * Math.PI / 2); // transformed first second guess
        }

        // initial estimates for the tof
        final double[] y12 = new double[]{
                                          timeOfFlight(inn1, longway, m, minSma, s, chord),
                                          timeOfFlight(inn2, longway, m, minSma, s, chord)
        };

        // initial bounds for y
        double y1;
        double y2;
        if (m == 0) {
            y1 = Math.log(y12[0]) - logt;
            y2 = Math.log(y12[1]) - logt;
        } else {
            y1 = y12[0] - T12;
            y2 = y12[1] - T12;
        }

        // Solve for x
        // Newton-Raphson iterations
        // NOTE - the number of iterations will go to infinity in case
        // m > 0  and there is no solution.
        double err = 1e20;
        int iterations = 0;
        final double tol = 1e-10;
        double xnew = 0;
        while ((err > tol) && (iterations < 15)) {
            // increment number of iterations
            ++iterations;

            // new x
            xnew = (x1 * y2 - y1 * x2) / (y2 - y1);

            //
            double x;
            if (m == 0) {
                x = FastMath.exp(xnew) - 1;
            } else {
                x = FastMath.atan(xnew) * 2 / Math.PI;
            }

            final double tof = timeOfFlight(x, longway, m, minSma, s, chord);

            // new value of y
            double ynew;
            if (m == 0) {
                ynew = FastMath.log(tof) - logt;
            } else {
                ynew = tof - T12;
            }

            // save previous and current values for the next iteration
            x1 = x2;  x2 = xnew;
            y1 = y2;  y2 = ynew;

            // update error
            err = Math.abs(x1 - xnew);
        }

        // failure to converge
        if (err > tol) {
            return false;
        }

        // convert converged value of x
        double x;
        if (m == 0) {
            x = FastMath.exp(xnew) - 1;
        } else {
            x = FastMath.atan(xnew) * 2 / Math.PI;
        }

        // Solution for the semi-major axis
        final double a = minSma / (1 - x * x);

        double eta;
        double eta2;
        if (x < 1) {
            // ellipse
            final double beta = longway * 2 * FastMath.asin(FastMath.sqrt((s - chord) / 2 / a));
            final double alfa = 2 * Math.acos( Math.max(-1, Math.min(1, x)) );
            final double psi  = (alfa - beta) / 2;
            eta2 = 2 * a * FastMath.pow(FastMath.sin(psi), 2) / s;
            eta  = FastMath.sqrt(eta2);
        } else {
            // hyperbola
            final double beta = longway * 2 * FastMath.asinh(Math.sqrt((chord - s) / 2 / a));
            final double alfa = 2 * FastMath.acosh(x);
            final double psi  = (alfa - beta) / 2.;
            eta2 = -2 * a * FastMath.pow(FastMath.sinh(psi), 2) / s;
            eta  = FastMath.sqrt(eta2);
        }

        // radial and tangential directions for departure velocity
        final double VR1 = 1. / eta / FastMath.sqrt(minSma) * (2 * Lambda * minSma - Lambda - x * eta);
        final double VT1 = FastMath.sqrt(r2 / minSma / eta2) * FastMath.abs(Math.sin(dth / 2));
        V1[0] = VR1;
        V1[1] = VT1;

        // radial and tangential directions for arrival velocity
        final double VT2 = VT1 / r2;
        final double VR2 = (VT1 - VT2) / FastMath.tan(theta / 2) - VR1;
        V2[0] = VR2;
        V2[1] = VT2;

        return true;
    }

    /** Compute the time of flight of a given arc of orbit.
     * The time of flight is evaluated via the Lagrange expression.
     *
     * @param x          x
     * @param longway    solution number; the long way or the short war
     * @param mrev       number of revolutions of the arc of orbit
     * @param minSma     minimum possible semi-major axis
     * @param s          semi-parameter of the arc of orbit
     * @param chord      chord of the arc of orbit
     * @return the time of flight for the given arc of orbit
     */
    private double timeOfFlight(final double x, final int longway, final int mrev, final double minSma,
                                final double s, final double chord) {

        final double a = minSma / (1 - x * x);

        double tof;
        if (x < 1) {
         // elliptical orbit
            final double beta = longway * 2 * Math.asin(Math.sqrt((s - chord) / 2 / a));
            final double alfa = 2 * Math.acos( Math.max(-1, Math.min(1, x)) );

            tof = a * Math.sqrt(a) * ((alfa - Math.sin(alfa)) - (beta - Math.sin(beta)) + 2 * Math.PI * mrev);

        } else {
         // hyperbolic orbit
            final double alfa = 2 * FastMath.acosh(x);
            final double beta = longway * 2 * FastMath.asinh(Math.sqrt((s - chord) / (-2 * a)));

            tof = -a * Math.sqrt(-a) * ((Math.sinh(alfa) - alfa) - (Math.sinh(beta) - beta));
        }

        return tof;
    }
}
