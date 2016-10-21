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
 * Gooding angles only initial orbit determination, assuming Keplerian motion.
 * An orbit is determined from three angular observations.
 *
 * Reference:
 *    Gooding, R.H., A New Procedure for Orbit Determination Based on Three Lines of Sight (Angles only),
 *    Technical Report 93004, April 1993
 *
 * @author Joris Olympio
 * @since 8.0
 */
public class IodGooding {

    /** Frame of the observation. */
    private final Frame frame;

    /** Gravitationnal constant. */
    private final double mu;

    /** Normalizing constant for distances. */
    private double R;

    /** Normalizing constant for velocities. */
    private double V;

    /** Normalizing constant for duration. */
    private double T;

    /** observer position 1. */
    private Vector3D vObserverPosition1;

    /** observer position 2. */
    private Vector3D vObserverPosition2;

    /** observer position 3. */
    private Vector3D vObserverPosition3;

    /** Date of the first observation. * */
    private AbsoluteDate date1;

    /** Radius of point 1 (X-R1). */
    private double R1;
    /** Radius of point 2 (X-R2). */
    private double R2;
    /** Radius of point 3 (X-R3). */
    private double R3;

    /** Range of point 1 (O1-R1). */
    private double rho1;
    /** Range of point 2 (O2-R2). */
    private double rho2;
    /** Range of point 3 (O3-R3). */
    private double rho3;

    /** working variable. */
    private double D1;

    /** working variable. */
    private double D3;

    /** factor for FD. */
    private double facFiniteDiff;

    /** Simple Lambert's problem solver. */
    private IodLambert lambert;

    /** Creator.
     *
     * @param frame Frame for the observations
     * @param mu  gravitational constant
     */
    public IodGooding(final Frame frame, final double mu) {
        this.mu = mu;
        this.frame = frame;

        this.rho1 = 0;
        this.rho2 = 0;
        this.rho3 = 0;
    }

    /** Get the range for observation (1).
     *
     * @return the range for observation (1).
     */
    public double getRange1() {
        return rho1 * R;
    }

    /** Get the range for observation (2).
     *
     * @return the range for observation (2).
     */
    public double getRange2() {
        return rho2 * R;
    }

    /** Get the range for observation (3).
     *
     * @return the range for observation (3).
     */
    public double getRange3() {
        return rho3 * R;
    }

    /** Orbit got from Observed Three Lines of Sight (angles only).
     *
     * @param O1 Observer position 1
     * @param O2 Observer position 2
     * @param O3 Observer position 3
     * @param lineOfSight1 line of sight 1
     * @param dateObs1 date of observation 1
     * @param lineOfSight2 line of sight 2
     * @param dateObs2 date of observation 1
     * @param lineOfSight3 line of sight 3
     * @param dateObs3 date of observation 1
     * @param rho1init initial guess of the range problem. range 1, in meters
     * @param rho3init initial guess of the range problem. range 3, in meters
     * @return an estimate of the Keplerian orbit
     */
    public KeplerianOrbit estimate(final Vector3D O1, final Vector3D O2, final Vector3D O3,
                                   final Vector3D lineOfSight1, final AbsoluteDate dateObs1,
                                   final Vector3D lineOfSight2, final AbsoluteDate dateObs2,
                                   final Vector3D lineOfSight3, final AbsoluteDate dateObs3,
                                   final double rho1init, final double rho3init) {

        this.date1 = dateObs1;

        // normalizing coefficients
        R = FastMath.max(rho1init, rho3init);
        V = FastMath.sqrt(mu / R);
        T = R / V;

        // Initialize Lambert's problem solver for non-dimensional units.
        lambert = new IodLambert(1.);

        this.vObserverPosition1 = O1.scalarMultiply(1. / R);
        this.vObserverPosition2 = O2.scalarMultiply(1. / R);
        this.vObserverPosition3 = O3.scalarMultiply(1. / R);

        final int maxiter = 100; // maximum iter

        // solve the range problem
        solveRangeProblem(rho1init / R, rho3init / R,
                          dateObs3.durationFrom(dateObs1) / T, dateObs2.durationFrom(dateObs1) / T,
                          0,
                          true,
                          lineOfSight1, lineOfSight2, lineOfSight3,
                          maxiter);

        // use the Gibbs problem to get the orbit now that we have three position vectors.
        final IodGibbs gibbs = new IodGibbs(mu);
        final Vector3D p1 = vObserverPosition1.add(lineOfSight1.scalarMultiply(rho1)).scalarMultiply(R);
        final Vector3D p2 = vObserverPosition2.add(lineOfSight2.scalarMultiply(rho2)).scalarMultiply(R);
        final Vector3D p3 = vObserverPosition3.add(lineOfSight3.scalarMultiply(rho3)).scalarMultiply(R);
        return gibbs.estimate(frame, p1, dateObs1, p2, dateObs2, p3, dateObs3);
    }

    /** Solve the range problem when three line of sight are given.
     *
     * @param rho1init   initial value for range R1, in meters
     * @param rho3init   initial value for range R3, in meters
     * @param T13   time of flight 1->3, in seconds
     * @param T12   time of flight 1->2, in seconds
     * @param nrev number of revolutions
     * @param direction  posigrade (true) or retrograde
     * @param lineOfSight1  line of sight 1
     * @param lineOfSight2  line of sight 2
     * @param lineOfSight3  line of sight 3
     * @param maxIterations         max iter
     * @return nothing
     */
    private boolean solveRangeProblem(final double rho1init, final double rho3init,
                                      final double T13, final double T12,
                                      final int nrev,
                                      final boolean direction,
                                      final Vector3D lineOfSight1,
                                      final Vector3D lineOfSight2,
                                      final Vector3D lineOfSight3,
                                      final int maxIterations) {
        final double ARBF = 1e-6;   // finite differences step
        boolean withHalley = true;  // use Halley's method
        final double cvtol = 1e-14; // convergence tolerance

        rho1 = rho1init;
        rho3 = rho3init;


        int iter = 0;
        double stoppingCriterion = 10 * cvtol;
        while ((iter < maxIterations) && (FastMath.abs(stoppingCriterion) > cvtol))  {
            facFiniteDiff = ARBF;

            // proposed in the original algorithm by Gooding.
            // We change the threshold to maxIterations / 2.
            if (iter >= (maxIterations / 2)) {
                withHalley = true;
            }

            // tentative position for R2
            final Vector3D P2 = getPositionOnLoS2(lineOfSight1, rho1,
                                                  lineOfSight3, rho3,
                                                  T13, T12, nrev, direction);

            if (P2 == null) {
                modifyIterate(lineOfSight1, lineOfSight2, lineOfSight3);
            } else {
                //
                R2 = P2.getNorm();

                // compute current line of sight for (2) and the associated range
                final Vector3D C = P2.subtract(vObserverPosition2);
                rho2 = C.getNorm();

                final double R10 = R1;
                final double R30 = R3;

                // indicate how close we are to the direction of sight for measurement (2)
                // for convergence test
                final double CR = lineOfSight2.dotProduct(C);

                // construct a basis and the function f and g.
                // Specifically, f is the P-coordinate and g the N-coordinate.
                // They should be zero when line of sight 2 and current direction for 2 from O2 are aligned.
                final Vector3D u = lineOfSight2.crossProduct(C);
                final Vector3D P = (u.crossProduct(lineOfSight2)).normalize();
                final Vector3D EN = (lineOfSight2.crossProduct(P)).normalize();

                // if EN is zero we have a solution!
                final double ENR = EN.getNorm();
                if (ENR == 0.) {
                    return true;
                }

                // Coordinate along 'F function'
                final double Fc = P.dotProduct(C);
                //Gc = EN.dotProduct(C);

                // Now get partials, by finite differences
                final double[] FD = new double[2];
                final double[] GD = new double[2];
                computeDerivatives(rho1, rho3,
                                   R10, R30,
                                   lineOfSight1, lineOfSight3,
                                   P, EN,
                                   Fc,
                                   T13, T12,
                                   withHalley,
                                   nrev,
                                   direction,
                                   FD, GD);

                // terms of the Jacobian
                final double fr1 = FD[0];
                final double fr3 = FD[1];
                final double gr1 = GD[0];
                final double gr3 = GD[1];
                // determinant of the Jacobian matrix
                final double detj = fr1 * gr3 - fr3 * gr1;

                // compute the Newton step
                D3 = -gr3 * Fc / detj;
                D1 = gr1 * Fc / detj;

                // update current values of ranges
                rho1 = rho1 + D3;
                rho3 = rho3 + D1;

                // convergence tests
                final double den = FastMath.max(CR, R2);
                stoppingCriterion = Fc / den;
            }

            ++iter;
        } // end while

        return true;
    }

    /** Change the current iterate if Lambert's problem solver failed to find a solution.
     *
     * @param lineOfSight1 line of sight 1
     * @param lineOfSight2 line of sight 2
     * @param lineOfSight3 line of sight 3
     */
    private void modifyIterate(final Vector3D lineOfSight1,
                               final Vector3D lineOfSight2,
                               final Vector3D lineOfSight3) {
        // This is a modifier proposed in the initial paper of Gooding.
        // Try to avoid Lambert-fail, by common-perpendicular starters
        final Vector3D R13 = vObserverPosition3.subtract(vObserverPosition1);
        D1 = R13.dotProduct(lineOfSight1);
        D3 = R13.dotProduct(lineOfSight3);
        final double D2 = lineOfSight1.dotProduct(lineOfSight3);
        final double D4 = 1. - D2 * D2;
        rho1 = FastMath.max((D1 - D3 * D2) / D4, 0.);
        rho3 = FastMath.max((D1 * D2 - D3) / D4, 0.);
    }

    /** Compute the derivatives by finite-differences for the range problem.
     * Specifically, we are trying to solve the problem:
     *      f(x,y) = 0
     *      g(x,y) = 0
     * So, in a Newton-Raphson process, we would need the derivatives:
     *  fx,fy,gx,gy
     * Enventually,
     *    dx =-f*gy / D
     *    dy = f*gx / D
     * where D is the determinant of the Jacobian matrix.
     *
     *
     * @param x    current range 1
     * @param y    current range 3
     * @param R10   current radius 1
     * @param R30   current radius 3
     * @param lineOfSight1  line of sight
     * @param lineOfSight3  line of sight
     * @param Pin   basis vector
     * @param Ein   basis vector
     * @param F     value of the f-function
     * @param T13   time of flight 1->3, in seconds
     * @param T12   time of flight 1->2, in seconds
     * @param withHailley    use Halley iterative method
     * @param nrev  number of revolutions
     * @param direction direction of motion
     * @param FD    derivatives of f wrt (rho1, rho3) by finite differences
     * @param GD    derivatives of g wrt (rho1, rho3) by finite differences
     */
    private void computeDerivatives(final double x, final double y,
                                    final double R10, final double R30,
                                    final Vector3D lineOfSight1, final Vector3D lineOfSight3,
                                    final Vector3D Pin,
                                    final Vector3D Ein,
                                    final double F,
                                    final double T13, final double T12,
                                    final boolean withHailley,
                                    final int nrev,
                                    final boolean direction,
                                    final double[] FD, final double[] GD) {

        final Vector3D P = Pin.normalize();
        final Vector3D EN = Ein.normalize();

        // Now get partials, by finite differences
        // steps for the differentiation
        final double dx = facFiniteDiff * x;
        final double dy = facFiniteDiff * y;

        final Vector3D Cm1 = getPositionOnLoS2 (lineOfSight1, x - dx,
                                                lineOfSight3, y,
                                                T13, T12, nrev, direction).subtract(vObserverPosition2);

        final double Fm1 = P.dotProduct(Cm1);
        final double Gm1 = EN.dotProduct(Cm1);

        final Vector3D Cp1 = getPositionOnLoS2 (lineOfSight1, x + dx,
                                                lineOfSight3, y,
                                                T13, T12, nrev, direction).subtract(vObserverPosition2);

        final double Fp1  = P.dotProduct(Cp1);
        final double Gp1 = EN.dotProduct(Cp1);

        // derivatives df/drho1 and dg/drho1
        final double Fx = (Fp1 - Fm1) / (2 * dx);
        final double Gx = (Gp1 - Gm1) / (2 * dx);

        final Vector3D Cm3 = getPositionOnLoS2 (lineOfSight1, x,
                                                lineOfSight3, y - dy,
                                                T13, T12, nrev, direction).subtract(vObserverPosition2);

        final double Fm3 = P.dotProduct(Cm3);
        final double Gm3 = EN.dotProduct(Cm3);

        final Vector3D Cp3 = getPositionOnLoS2 (lineOfSight1, x,
                                                lineOfSight3, y + dy,
                                                T13, T12, nrev, direction).subtract(vObserverPosition2);

        final double Fp3 = P.dotProduct(Cp3);
        final double Gp3 = EN.dotProduct(Cp3);

        // derivatives df/drho3 and dg/drho3
        final double Fy = (Fp3 - Fm3) / (2. * dy);
        final double Gy = (Gp3 - Gm3) / (2. * dy);
        final double detJac = Fx * Gy - Fy * Gx;

        // Coefficients for the classical Newton-Raphson iterative method
        FD[0] = Fx / detJac;
        FD[1] = Fy / detJac;
        GD[0] = Gx / detJac;
        GD[1] = Gy / detJac;

        // Modified Newton-Raphson process, with Halley's method to have cubic convergence.
        // This requires computing second order derivatives.
        if (withHailley) {
            //
            final double hrho1Sq = dx * dx;
            final double hrho3Sq = dy * dy;

            // Second order derivatives: d^2f / drho1^2 and d^2g / drho3^2
            final double Fxx = (Fp1 + Fm1 - 2 * F) / hrho1Sq;
            final double Gxx = (Gp1 + Gm1 - 2 * F) / hrho1Sq;
            final double Fyy = (Fp3 + Fp3 - 2 * F) / hrho3Sq;
            final double Gyy = (Gm3 + Gm3 - 2 * F) / hrho3Sq;

            final Vector3D Cp13 = getPositionOnLoS2 (lineOfSight1, x + dx,
                                                     lineOfSight3, y + dy,
                                                     T13, T12, nrev, direction).subtract(vObserverPosition2);

            // f function value at (x1+dx1, x3+dx3)
            final double Fp13 = P.dotProduct(Cp13) - F;
            // g function value at (x1+dx1, x3+dx3)
            final double Gp13 = EN.dotProduct(Cp13);

            final Vector3D Cm13 = getPositionOnLoS2 (lineOfSight1, x + dx,
                                                     lineOfSight3, y + dy,
                                                     T13, T12, nrev, direction).subtract(vObserverPosition2);

            // f function value at (x1+dx1, x3+dx3)
            final double Fm13 = P.dotProduct(Cm13) - F;
            // g function value at (x1+dx1, x3+dx3)
            final double Gm13 = EN.dotProduct(Cm13);

            // Second order derivatives: d^2f / drho1drho3 and d^2g / drho1drho3
            //double Fxy = Fp13 / (dx * dy) - (Fx / dy + Fy / dx) -
            //                0.5 * (Fxx * dx / dy + Fyy * dy / dx);
            //double Gxy = Gp13 / (dx * dy) - (Gx / dy + Gy / dx) -
            //                0.5 * (Gxx * dx / dy + Gyy * dy / dx);
            final double Fxy = (Fp13 + Fm13) / (2 * dx * dy) - 1.0 * (Fxx / 2 + Fyy / 2) - F / (dx * dy);
            final double Gxy = (Gp13 + Gm13) / (2 * dx * dy) - 1.0 * (Gxx / 2 + Gyy / 2) - F / (dx * dy);

            // delta Newton Raphson, 1st order step
            final double dx3NR = -Gy * F / detJac;
            final double dx1NR = Gx * F / detJac;

            // terms of the Jacobian, considering the development, after linearization
            // of the second order Taylor expansion around the Newton Raphson iterate:
            // (fx + 1/2 * fxx * dx* + 1/2 * fxy * dy*) * dx
            //      + (fy + 1/2 * fyy * dy* + 1/2 * fxy * dx*) * dy
            // where: dx* and dy* would be the step of the Newton raphson process.
            final double FxH = Fx + 0.5 * (Fxx * dx3NR + Fxy * dx1NR);
            final double FyH = Fy + 0.5 * (Fxy * dx3NR + Fxx * dx1NR);
            final double GxH = Gx + 0.5 * (Gxx * dx3NR + Gxy * dx1NR);
            final double GyH = Gy + 0.5 * (Gxy * dx3NR + Fxy * dx1NR);

            // New Halley's method "Jacobian"
            FD[0] = FxH;
            FD[1] = FyH;
            GD[0] = GxH;
            GD[1] = GyH;
        }
    }

    /** Calculate the position along sight-line.
     *
     * @param E1 line of sight 1
     * @param RO1 distance along E1
     * @param E3 line of sight 3
     * @param RO3 distance along E3
     * @param T12   time of flight
     * @param T13   time of flight
     * @param nRev number of revolutions
     * @param posigrade direction of motion
     * @return (R2-O2)
     */
    private Vector3D getPositionOnLoS2(final Vector3D E1, final double RO1,
                                       final Vector3D E3, final double RO3,
                                       final double T13, final double T12,
                                       final double nRev, final boolean posigrade) {
        final Vector3D P1 = vObserverPosition1.add(E1.scalarMultiply(RO1));
        R1 = P1.getNorm();

        final Vector3D P3 = vObserverPosition3.add(E3.scalarMultiply(RO3));
        R3 = P3.getNorm();

        final Vector3D P13 = P1.crossProduct(P3);

        // sweep angle
        // (Fails only if either R1 or R2 is zero)
        double TH = FastMath.atan2(P13.getNorm(), P1.dotProduct(P3));

        // compute the number of revolutions
        if (!posigrade) {
            TH = FastMath.PI - TH;
        }
        TH = TH + nRev * FastMath.PI;

        // Solve the Lambert's problem to get the velocities at endpoints
        final double[] V1 = new double[2];
        // work with non-dimensional units (MU=1)
        final boolean exitflag = lambert.solveLambertPb(R1, R3, TH, T13, 0, V1);

        if (exitflag) {
            // basis vectors
            final Vector3D Pn = P1.crossProduct(P3);
            final Vector3D Pt = Pn.crossProduct(P1);

            // tangential velocity norm
            double RT = Pt.getNorm();
            if (!posigrade) {
                RT = -RT;
            }

            // velocity vector at P1
            final Vector3D Vel1 = P1.scalarMultiply(V1[0] / R1).add(Pt.scalarMultiply(V1[1] / RT));

            // estimate the position at the second observation time
            // propagate (P1, V1) during TAU + T12 to get (P2, V2)
            final Vector3D P2 = propagatePV(P1, Vel1, T12);

            return P2;
        }

        return null;
    }

    /** Propagate a solution (Kepler).
     *
     * @param P1  initial position vector
     * @param V1  initial velocity vector
     * @param tau propagation time
     * @return final position vector
     */
    private Vector3D propagatePV(final Vector3D P1, final Vector3D V1, final double tau) {
        final PVCoordinates pv1 = new PVCoordinates(P1, V1);
        // create a Keplerian orbit. Assume MU = 1.
        final KeplerianOrbit orbit = new KeplerianOrbit(pv1, frame, date1, 1.);
        return orbit.shiftedBy(tau).getPVCoordinates().getPosition();
    }

}
