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
 * Gooding angles only initial orbit determination, assuming Keplerian motion.
 * An orbit is determined from three angular observations.
 *
 * Reference:
 *    Gooding, R.H., A New Procedure for Orbit Determination Based on Three Lines of Sight (Angles only),
 *    Technical Report 93004, April 1993
 *
 * @author Joris Olympio
 * @since 7.1
 */
public class IodGooding {

    /** Frame of the observation. */
    private final Frame frame;

    /** Gravitationnal constant. */
    private final double mu;

    /** observer position 1. */
    private final Vector3D O1;

    /** observer position 2. */
    private final Vector3D O2;

    /** observer position 3. */
    private final Vector3D O3;

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
    private final IodLambert lambert;

    /** Creator.
     *
     * @param frame Frame for the observations
     * @param mu  gravitational constant
     * @param O1 Observer position 1
     * @param O2 Observer position 2
     * @param O3 Observer position 3
     */
    public IodGooding(final Frame frame, final double mu, final Vector3D O1, final Vector3D O2, final Vector3D O3) {
        this.mu = mu;
        this.frame = frame;
        lambert = new IodLambert(mu);

        this.O1 = O1;
        this.O2 = O2;
        this.O3 = O3;
        this.rho1 = 0;
        this.rho2 = 0;
        this.rho3 = 0;
    }

    /** Get the range for observation (1).
     *
     * @return the range for observation (1).
     */
    public double getRange1() {
        return rho1;
    }

    /** Get the range for observation (2).
     *
     * @return the range for observation (2).
     */
    public double getRange2() {
        return rho2;
    }

    /** Get the range for observation (3).
     *
     * @return the range for observation (3).
     */
    public double getRange3() {
        return rho3;
    }

    /** Orbit got from Observed Three Lines of Sight (angles only).
     *
     * @param lineOfSight1 line of sight 1
     * @param T1 date of observation 1
     * @param lineOfSight2 line of sight 2
     * @param T2 date of observation 1
     * @param lineOfSight3 line of sight 3
     * @param T3 date of observation 1
     * @param rho1init initial guess of the range problem. range 1, in meters
     * @param rho3init initial guess of the range problem. range 3, in meters
     * @return an estimate of the Keplerian orbit
     */
    public KeplerianOrbit estimate(final Vector3D lineOfSight1, final AbsoluteDate T1,
                                   final Vector3D lineOfSight2, final AbsoluteDate T2,
                                   final Vector3D lineOfSight3, final AbsoluteDate T3,
                                   final double rho1init, final double rho3init)
    {
        this.date1 = T1;

        final int maxiter = 50; // maximum iter

        // normalizeing coefficients
        final double R = FastMath.max(rho1init, rho3init);
        final double V = FastMath.sqrt(mu / R);
        final double T = R / V;

        // solve the range problem
        SolveRangeProblem(0,
               rho1init / R, rho3init / R,
               T3.durationFrom(T1) / T, T2.durationFrom(T1) / T,
               lineOfSight1, lineOfSight2, lineOfSight3,
               maxiter);

        // use the Gibbs problem to get the orbit now that we have three
        // position vectors.
        final IodGibbs gibbs = new IodGibbs(mu);
        return gibbs.estimate(frame,
                       O1.add(lineOfSight1.scalarMultiply(rho1 * R)), T1,
                       O2.add(lineOfSight2.scalarMultiply(rho2 * R)), T2,
                       O3.add(lineOfSight3.scalarMultiply(rho3 * R)), T3);
    }

    /** Solve the range problem when three line of sight are given.
     *
     * @param nrev number of revolutions
     * @param rho1init   initial value for range R1, in meters
     * @param rho3init   initial value for range R3, in meters
     * @param T13   time of flight 1->3, in seconds
     * @param T12   time of flight 1->2, in seconds
     * @param lineOfSight1  line of sight 1
     * @param lineOfSight2  line of sight 2
     * @param lineOfSight3  line of sight 3
     * @param maxIterations         max iter
     * @return nothing
     */
    private boolean SolveRangeProblem(final int nrev,
                      final double rho1init, final double rho3init,
                      final double T13, final double T12,
                      final Vector3D lineOfSight1,
                      final Vector3D lineOfSight2,
                      final Vector3D lineOfSight3,
                      final int maxIterations) {
        //int NPDF = 0;
        final double ARBF = 1e-6;
        double HN = 0.;
        final double CRIVAL = 1e-24;
        final boolean direction = true;   // posigrade or retrograde

        rho1 = rho1init;
        rho3 = rho3init;
        double rho1old = rho1;
        double rho3old = rho3;

        double Fc;
        double FCold = 0;

        int iter = 0;
        while (iter < maxIterations) {
            facFiniteDiff = ARBF;

            // proposed in the original algorithm by Gooding.
            // We change the threshold to maxIterations / 2.
            if (iter >= (maxIterations / 2)) {
                HN = 0.5;
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
                final Vector3D C = P2.subtract(O2);
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
                Fc = P.dotProduct(C);
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
                                   HN,
                                   nrev,
                                   direction,
                                   FD, GD);

                // terms of the Jacobian
                final double FD1H = FD[0];
                final double FD3H = FD[1];
                final double GD1H = GD[0];
                final double GD3H = GD[1];
                // determinant of the Jacobian matrix
                final double DELH = FD1H * GD3H - FD3H * GD1H;

                // compute the Newton step
                D3 = -GD3H * Fc / DELH;
                D1 = GD1H * Fc / DELH;
                /*DELNM = DEL / (Math.abs(FD1 * GD3) + Math.abs(FD3 * GD1));
                if (Math.abs(DELNM) < 1e-3) {
                    D3 = Math.sqrt(Math.abs(D3NR * D3)) * Math.signum(D3NR);
                    D1 = Math.sqrt(Math.abs(D1NR * D1)) * Math.signum(D1NR);
                }*/

                // save current value of ranges
                rho1old = rho1;
                rho3old = rho3;

                // update current values of ranges
                rho1 = rho1 + D3;
                rho3 = rho3 + D1;

                // convergence tests
                final double DEN = Math.max(CR, R2);
                final double CRIT = Fc / DEN;
                if (CRIT * CRIT < CRIVAL) {
                    return true;
                }

                FCold = Fc;
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
        final Vector3D R13 = O3.subtract(O1);
        D1 = R13.dotProduct(lineOfSight1);
        D3 = R13.dotProduct(lineOfSight3);
        final double D2 = lineOfSight1.dotProduct(lineOfSight3);
        final double D4 = 1. - D2 * D2;
        rho1 = Math.max((D1 - D3 * D2) / D4, 0.);
        rho3 = Math.max((D1 * D2 - D3) / D4, 0.);
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
     * @param rho1_ .
     * @param rho3_ .
     * @param R10 .
     * @param R30 .
     * @param lineOfSight1  line of sight
     * @param lineOfSight3  line of sight
     * @param Pin basis vector
     * @param Ein basis vector
     * @param F .
     * @param T13 .
     * @param T12 .
     * @param HN .
     * @param nrev .
     * @param direction .
     * @param FD  fd of f
     * @param GD  fd of g
     */
    private void computeDerivatives(final double rho1_, final double rho3_,
                                    final double R10, final double R30,
                                    final Vector3D lineOfSight1, final Vector3D lineOfSight3,
                                    final Vector3D Pin,
                                    final Vector3D Ein,
                                    final double F,
                                    final double T13, final double T12,
                                    final double HN,
                                    final int nrev,
                                    final boolean direction,
                                    final double[] FD, final double[] GD) {

        final Vector3D P = Pin.normalize();
        final Vector3D EN = Ein.normalize();

        // Now get partials, by finite differences
        // steps for the differentiation
        final double hrho1 = facFiniteDiff * R10;
        final double hrho3 = facFiniteDiff * R30;

        Vector3D C = getPositionOnLoS2 (lineOfSight1, rho1 - hrho1,
                              lineOfSight3, rho3,
                              T13, T12, nrev, direction).subtract(O2);

        final double FM1 = P.dotProduct(C) - F;
        final double GM1 = EN.dotProduct(C);

        C = getPositionOnLoS2 (lineOfSight1, rho1 + hrho1,
                              lineOfSight3, rho3,
                              T13, T12, nrev, direction).subtract(O2);

        final double FP1  = P.dotProduct(C) - F;
        final double GP1 = EN.dotProduct(C);

        // derivatives df/drho1 and dg/drho1
        final double FD1 = (FP1 - FM1) / (2 * hrho1);
        final double GD1 = (GP1 - GM1) / (2 * hrho1);

        C = getPositionOnLoS2 (lineOfSight1, rho1,
                              lineOfSight3, rho3 - hrho3,
                              T13, T12, nrev, direction).subtract(O2);

        final double FM3 = P.dotProduct(C) - F;
        final double GM3 = EN.dotProduct(C);

        C = getPositionOnLoS2 (lineOfSight1, rho1,
                              lineOfSight3, rho3 + hrho3,
                              T13, T12, nrev, direction).subtract(O2);

        final double FP3 = P.dotProduct(C) - F;
        final double GP3 = EN.dotProduct(C);

        // derivatives df/drho3 and dg/drho3
        final double FD3 = (FP3 - FM3) / (2. * hrho3);
        final double GD3 = (GP3 - GM3) / (2. * hrho3);

        // Jacobian
        FD[0] = FD1;
        FD[1] = FD3;
        GD[0] = GD1;
        GD[1] = GD3;

        // compute a shift of Jacobian. This is useful to get out of a trap
        // in the Newton-Raphson process.
        if (HN > 0.) {
            //
            final double hrho1Sq = hrho1 * hrho1;
            final double hrho3Sq = hrho3 * hrho3;

            final double FDD1 = (FP1 + FM1) / hrho1Sq;
            final double GDD1 = (GP1 + GM1) / hrho1Sq;
            final double FDD3 = (FP3 + FM3) / hrho3Sq;
            final double GDD3 = (GP3 + GM3) / hrho3Sq;

            C = getPositionOnLoS2 (lineOfSight1, rho1 + hrho1,
                                  lineOfSight3, rho3 + hrho3,
                                  T13, T12, nrev, direction).subtract(O2);

            final double F13 = P.dotProduct(C) - F;
            final double G13 = EN.dotProduct(C);

            final double ROFAC = hrho1 / hrho3;
            final double FD13 = F13 / (hrho1 * hrho3) - (FD1 / hrho3 + FD3 / hrho1) -
                            0.5 * (FDD1 * ROFAC + FDD3 / ROFAC);
            final double GD13 = G13 / (hrho1 * hrho3) - (GD1 / hrho3 + GD3 / hrho1) -
                            0.5 * (GDD1 * ROFAC + GDD3 / ROFAC);

            //
            final double DEL = FD1 * GD3 - FD3 * GD1;
            final double D3NR = -GD3 * F / DEL;
            final double D1NR = GD1 * F / DEL;
            // terms of the Jacobian
            final double FD1H = FD1 + HN * (FDD1 * D3NR + FD13 * D1NR);
            final double FD3H = FD3 + HN * (FD13 * D3NR + FDD3 * D1NR);
            final double GD1H = GD1 + HN * (GDD1 * D3NR + GD13 * D1NR);
            final double GD3H = GD3 + HN * (GD13 * D3NR + GDD3 * D1NR);

            // Jacobian
            FD[0] = FD1H;
            FD[1] = FD3H;
            GD[0] = GD1H;
            GD[1] = GD3H;
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
                                      final double nRev, final boolean posigrade)
    {
        final Vector3D P1 = O1.add(E1.scalarMultiply(RO1));
        R1 = P1.getNorm();

        final Vector3D P3 = O3.add(E3.scalarMultiply(RO3));
        R3 = P3.getNorm();

        final Vector3D P13 = P1.crossProduct(P3);

        // sweep angle
        // (Fails only if either R1 or R2 is zero)
        double TH = FastMath.atan2(P13.getNorm(), P1.dotProduct(P3));

        // compute the number of revolutions
        if (!posigrade) {
            TH = Math.PI - TH;
        }
        TH = TH + nRev * Math.PI;

        // Solve the Lambert's problem to get the velocities at endpoints
        final double[] V1 = new double[2];
        final double[] V3 = new double[2];
        // work with non-dimensional units
        final double MU = 1; // this assume we have normalized before
        final double V = Math.sqrt(MU / R1);
        final double T = R1 / V;
        final boolean exitflag = lambert.SolveLambertPb(1., R3 / R1, TH,
                                       T13 / T, 0,
                                       V1, V3);

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
            final Vector3D Vel1 = P1.scalarMultiply(V * V1[0] / R1)
                            .add(Pt.scalarMultiply(V * V1[1] / RT));

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
    private Vector3D propagatePV(final Vector3D P1, final Vector3D V1,
                             final double tau)
    {
        final PVCoordinates pv1 = new PVCoordinates(P1, V1);
        // create a Keplerian orbit. Assume MU = 1.
        final KeplerianOrbit orbit = new KeplerianOrbit(pv1, frame, date1, 1.);
        return orbit.shiftedBy(tau).getPVCoordinates().getPosition();
    }

}
