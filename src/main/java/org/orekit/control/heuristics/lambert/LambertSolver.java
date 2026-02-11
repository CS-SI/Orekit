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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.KeplerianMotionCartesianUtility;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Lambert position-based Initial Orbit Determination (IOD) algorithm, assuming Keplerian motion.
 * The method is also used for trajectory design, specially in interplanetary missions.
 * This solver combines Dario Izzo's algorithm with Gim Der design to find all possible solutions.
 * <p>
 * An orbit is determined from two position vectors.
 *
 * References:
 *  Battin, R.H., An Introduction to the Mathematics and Methods of Astrodynamics, AIAA Education, 1999.
 *  Lancaster, E.R. and Blanchard, R.C., A Unified Form of Lambert’s Theorem, Goddard Space Flight Center, 1968.
 *  Dario Izzo. Revisiting Lambert’s problem. Celestial Mechanics and Dynamical Astronomy, 2015. https://arxiv.org/abs/1403.2705
 *  Gim J. Der. The Superior Lambert Algorithm. Advanced Maui Optical and Space Surveillance Technologies, 2011. https://amostech.com/TechnicalPapers/2011/Poster/DER.pdf
 * </p>
 * @author Joris Olympio
 * @author Romain Serra
 * @author Rafael Ayala
 * @since 14.0
 */
public class LambertSolver {

    /** gravitational constant. */
    private final double mu;

    /** parameters for the Householder solver. */
    private final HouseholderParameters householderParameters;

    /** auxiliary variable x. */
    private double x;

    /** auxiliary variable y. */
    private double y;

    /** auxiliary variable sigma. */
    private double sigma;

    /** auxiliary variable rho. */
    private double rho;

    /** auxiliary variable zeta. */
    private double zeta;

    /** auxiliary variable gamma. */
    private double gamma;

    /** time of flight variable. */
    private double tau;

    /** time of flight for minimum energy path variable. */
    private double tauME;

    /** geometry vector ir1. */
    private Vector3D ir1;

    /** geometry vector ir2. */
    private Vector3D ir2;

    /** geometry vector it1. */
    private Vector3D it1;

    /** geometry vector it2. */
    private Vector3D it2;

    /** orbit type. */
    private LambertOrbitType orbitType;

    /** path type for the shortest transfer with 0 complete revolutions. */
    private LambertPathType shortestPathType;

    /** maximum possible number of revolutions for the given time of transfer. */
    private int nMax;

    /** Constructor from Householder parameters object.
     *
     * @param mu gravitational constant
     * @param householderParameters parameters for the Householder solver
     */
    public LambertSolver(final double mu, final HouseholderParameters householderParameters) {
        this.mu = mu;
        this.householderParameters = householderParameters;
        this.x = 0.0;
        this.y = 0.0;
        this.sigma = 0.0;
        this.rho = 0.0;
        this.zeta = 0.0;
        this.gamma = 0.0;
        this.tau = 0.0;
        this.tauME = 0.0;
        this.ir1 = Vector3D.ZERO;
        this.ir2 = Vector3D.ZERO;
        this.it1 = Vector3D.ZERO;
        this.it2 = Vector3D.ZERO;
        this.orbitType = LambertOrbitType.ELLIPTIC;
        this.shortestPathType = LambertPathType.LOW_PATH;
    }

    /** Constructor from direct Householder parameter values.
     *
     * @param mu gravitational constant
     * @param householderMaxIterations maximum number of iterations for the Householder solver
     * @param householderAtol absolute tolerance for the Householder solver
     * @param householderRtol relative tolerance for the Householder solver
     */
    public LambertSolver(final double mu, final int householderMaxIterations,
                         final double householderAtol, final double householderRtol) {
        this(mu, new HouseholderParameters(householderMaxIterations, householderAtol, householderRtol));
    }

    /** Constructor with default Householder solver parameters.
     *
     * @param mu gravitational constant
     */
    public LambertSolver(final double mu) {
        this(mu, 2000, 1.0e-5, 1.0e-7);
    }

    /** Lambert's solver.
     * @param posigrade flag indicating the direction of motion
     * @param boundaryConditions LambertBoundaryConditions holding the boundary conditions
     * @return a list of solutions
     */
    public List<LambertSolution> solve(final boolean posigrade,
                           final LambertBoundaryConditions boundaryConditions) {
        final int maxIterations = this.householderParameters.getMaxIterations();
        final double atol = this.householderParameters.getAbsoluteTolerance();
        final double rtol = this.householderParameters.getRelativeTolerance();
        initialSetup(posigrade, boundaryConditions);
        // deal with backward case recursively
        if (tau < 0.0) {
            final LambertBoundaryConditions backwardConditions = new LambertBoundaryConditions(boundaryConditions.getTerminalDate(),
                    boundaryConditions.getTerminalPosition(), boundaryConditions.getInitialDate(), boundaryConditions.getInitialPosition(),
                    boundaryConditions.getReferenceFrame());
            final List<LambertSolution> solutionsForward = solve(posigrade, backwardConditions);
            final ArrayList<LambertSolution> solutionsForwardReversed = new ArrayList<>();
            for (final LambertSolution solutionForward : solutionsForward) {
                final LambertSolution reversed = new LambertSolution(solutionForward.getNRev(),
                        solutionForward.getPathType(), solutionForward.getOrbitType(),
                        posigrade,
                        boundaryConditions,
                        solutionForward.getBoundaryVelocities().getTerminalVelocity(),
                        solutionForward.getBoundaryVelocities().getInitialVelocity());
                solutionsForwardReversed.add(reversed);
            }
            return solutionsForwardReversed;
        }
        final ArrayList<LambertSolution> solutions = new ArrayList<>();
        boolean lowPath = shortestPathType.equals(LambertPathType.LOW_PATH);
        double x0 = initialGuessX(tau, sigma, 0, lowPath);
        x = householderSolver(x0, tau, sigma, 0, maxIterations, atol, rtol);
        y = calculateY(x, sigma);
        final Vector3D p1 = boundaryConditions.getInitialPosition();
        final Vector3D p2 = boundaryConditions.getTerminalPosition();
        final double r1 = p1.getNorm();
        final double r2 = p2.getNorm();
        double[] vrvt = reconstructVrVt(x, y, r1, r2, sigma, gamma, rho, zeta);
        Vector3D v1 = ir1.scalarMultiply(vrvt[0]).add(it1.scalarMultiply(vrvt[1]));
        Vector3D v2 = ir2.scalarMultiply(vrvt[2]).add(it2.scalarMultiply(vrvt[3]));
        // we create the solution and add it to the list
        LambertSolution solution = new LambertSolution(0, shortestPathType, orbitType, posigrade, boundaryConditions, v1, v2);
        solutions.add(solution);
        if (nMax >= 1) {
            // then we need to iterate over every value from 1 to nMax, both included:
            for (int nRevs = 1; nRevs <= nMax; nRevs++) {
                // first for the low path
                lowPath = true;
                x0 = initialGuessX(tau, sigma, nRevs, lowPath);
                x = householderSolver(x0, tau, sigma, nRevs, maxIterations, atol, rtol);
                y = calculateY(x, sigma);
                vrvt = reconstructVrVt(x, y, r1, r2, sigma, gamma, rho, zeta);
                v1 = ir1.scalarMultiply(vrvt[0]).add(it1.scalarMultiply(vrvt[1]));
                v2 = ir2.scalarMultiply(vrvt[2]).add(it2.scalarMultiply(vrvt[3]));
                // we create the solution and add it to the list
                solution = new LambertSolution(nRevs, LambertPathType.LOW_PATH, orbitType, posigrade, boundaryConditions, v1, v2);
                solutions.add(solution);
                // and now for the high path
                lowPath = false;
                x0 = initialGuessX(tau, sigma, nRevs, lowPath);
                x = householderSolver(x0, tau, sigma, nRevs, maxIterations, atol, rtol);
                y = calculateY(x, sigma);
                vrvt = reconstructVrVt(x, y, r1, r2, sigma, gamma, rho, zeta);
                v1 = ir1.scalarMultiply(vrvt[0]).add(it1.scalarMultiply(vrvt[1]));
                v2 = ir2.scalarMultiply(vrvt[2]).add(it2.scalarMultiply(vrvt[3]));
                // we create the solution and add it to the list
                solution = new LambertSolution(nRevs, LambertPathType.HIGH_PATH, orbitType, posigrade, boundaryConditions, v1, v2);
                solutions.add(solution);
            }
        }
        return solutions;
    }

    /** Lambert's solver, with user provided number of complete revolutions.
     * @param posigrade flag indicating the direction of motion
     * @param nRevs number of complete revolutions
     * @param boundaryConditions LambertBoundaryConditions holding the boundary conditions
     * @return  a list of solutions
     */
    public List<LambertSolution> solve(final boolean posigrade, final int nRevs,
                           final LambertBoundaryConditions boundaryConditions) {
        final int maxIterations = this.householderParameters.getMaxIterations();
        final double atol = this.householderParameters.getAbsoluteTolerance();
        final double rtol = this.householderParameters.getRelativeTolerance();
        initialSetup(posigrade, boundaryConditions);
        // deal with backward case recursively
        if (tau < 0.0) {
            final LambertBoundaryConditions backwardConditions = new LambertBoundaryConditions(boundaryConditions.getTerminalDate(),
                    boundaryConditions.getTerminalPosition(), boundaryConditions.getInitialDate(), boundaryConditions.getInitialPosition(),
                    boundaryConditions.getReferenceFrame());
            final List<LambertSolution> solutionsForward = solve(posigrade, backwardConditions);
            final ArrayList<LambertSolution> solutionsForwardReversed = new ArrayList<>();
            for (final LambertSolution solutionForward : solutionsForward) {
                final LambertSolution reversed = new LambertSolution(solutionForward.getNRev(),
                        solutionForward.getPathType(), solutionForward.getOrbitType(),
                        posigrade,
                        boundaryConditions,
                        solutionForward.getBoundaryVelocities().getTerminalVelocity(),
                        solutionForward.getBoundaryVelocities().getInitialVelocity());
                solutionsForwardReversed.add(reversed);
            }
            return solutionsForwardReversed;
        }
        if (nRevs > nMax) {
            throw new OrekitException(OrekitMessages.LAMBERT_INVALID_NUMBER_OF_REVOLUTIONS, nRevs, nMax);
        }
        final ArrayList<LambertSolution> solutions = new ArrayList<>();
        final Vector3D p1 = boundaryConditions.getInitialPosition();
        final Vector3D p2 = boundaryConditions.getTerminalPosition();
        final double r1 = p1.getNorm();
        final double r2 = p2.getNorm();
        double x0;
        double[] vrvt;
        Vector3D v1;
        Vector3D v2;
        LambertSolution solution;
        if (nRevs == 0) {
            // then there is a single solution
            final boolean lowPath = shortestPathType.equals(LambertPathType.LOW_PATH);
            x0 = initialGuessX(tau, sigma, 0, lowPath);
            x = householderSolver(x0, tau, sigma, 0, maxIterations, atol, rtol);
            y = calculateY(x, sigma);
            vrvt = reconstructVrVt(x, y, r1, r2, sigma, gamma, rho, zeta);
            v1 = ir1.scalarMultiply(vrvt[0]).add(it1.scalarMultiply(vrvt[1]));
            v2 = ir2.scalarMultiply(vrvt[2]).add(it2.scalarMultiply(vrvt[3]));
            // we create the single solution and add it to the list
            solution = new LambertSolution(0, shortestPathType, orbitType, posigrade, boundaryConditions, v1, v2);
            solutions.add(solution);
        } else {
            // then we have two solutions, one for the high path and one for the low path
            // first for the low path
            x0 = initialGuessX(tau, sigma, nRevs, true);
            x = householderSolver(x0, tau, sigma, nRevs, maxIterations, atol, rtol);
            y = calculateY(x, sigma);
            vrvt = reconstructVrVt(x, y, r1, r2, sigma, gamma, rho, zeta);
            v1 = ir1.scalarMultiply(vrvt[0]).add(it1.scalarMultiply(vrvt[1]));
            v2 = ir2.scalarMultiply(vrvt[2]).add(it2.scalarMultiply(vrvt[3]));
            solution = new LambertSolution(nRevs, LambertPathType.LOW_PATH, orbitType, posigrade, boundaryConditions, v1, v2);
            solutions.add(solution);
            // and now for the high path
            x0 = initialGuessX(tau, sigma, nRevs, false);
            x = householderSolver(x0, tau, sigma, nRevs, maxIterations, atol, rtol);
            y = calculateY(x, sigma);
            vrvt = reconstructVrVt(x, y, r1, r2, sigma, gamma, rho, zeta);
            v1 = ir1.scalarMultiply(vrvt[0]).add(it1.scalarMultiply(vrvt[1]));
            v2 = ir2.scalarMultiply(vrvt[2]).add(it2.scalarMultiply(vrvt[3]));
            solution = new LambertSolution(nRevs, LambertPathType.HIGH_PATH, orbitType, posigrade, boundaryConditions, v1, v2);
            solutions.add(solution);
        }
        return solutions;
    }

    /** Initial set up of geometry and auxiliary variables.
     * @param posigrade flag indicating the direction of motion
     * @param boundaryConditions LambertBoundaryConditions holding the boundary conditions
     */
    public void initialSetup(final boolean posigrade,
                             final LambertBoundaryConditions boundaryConditions) {
        final AbsoluteDate t1 = boundaryConditions.getInitialDate();
        final AbsoluteDate t2 = boundaryConditions.getTerminalDate();
        final Vector3D p1 = boundaryConditions.getInitialPosition();
        final Vector3D p2 = boundaryConditions.getTerminalPosition();
        final double r1 = p1.getNorm();
        final double r2 = p2.getNorm();
        final double timeDiff = t2.durationFrom(t1);
        final double distance = p2.subtract(p1).getNorm();
        final double s = (r1 + r2 + distance) / 2.0;
        ir1 = p1.normalize();
        ir2 = p2.normalize();
        final Vector3D ih = ir1.crossProduct(ir2).normalize();
        sigma = FastMath.sqrt(1.0 - FastMath.min(1.0, distance / s));
        final int sign = (int) FastMath.signum(ih.getZ());
        if (sign < 0) {
            sigma *= -1;
            it1 = ir1.crossProduct(ih);
            it2 = ir2.crossProduct(ih);
        } else {
            it1 = ih.crossProduct(ir1);
            it2 = ih.crossProduct(ir2);
        }
        if (!posigrade) {
            sigma *= -1;
            it1 = it1.negate();
            it2 = it2.negate();
        }
        tauME = FastMath.acos(sigma) + sigma * FastMath.sqrt(1.0 - sigma * sigma);
        tau = FastMath.sqrt(2 * mu / (s * s * s)) * timeDiff;
        gamma = FastMath.sqrt(mu * s / 2.0);
        rho = (r1 - r2) / distance;
        // note that the following variable zeta is sigma from Izzo's algorithm, and sigma is lambda
        zeta = FastMath.sqrt(1.0 - rho * rho);
        final double tauParabolic = 2.0 / 3.0 * (1.0 - (sigma * sigma * sigma));
        final double diffTauParabolic = tau - tauParabolic;
        if (FastMath.abs(diffTauParabolic) <= Precision.EPSILON) {
            orbitType = LambertOrbitType.PARABOLIC;
        } else if (diffTauParabolic > 0) {
            orbitType = LambertOrbitType.ELLIPTIC;
        } else {
            orbitType = LambertOrbitType.HYPERBOLIC;
        }
        nMax = orbitType.equals(LambertOrbitType.ELLIPTIC) ? (int) FastMath.floor(tau / FastMath.PI) : 0;
        if (FastMath.abs(tauME - tau) <= Precision.EPSILON) {
            shortestPathType = LambertPathType.MIN_ENERGY_PATH;
        } else if (tau < tauME) {
            shortestPathType = LambertPathType.LOW_PATH;
        } else {
            shortestPathType = LambertPathType.HIGH_PATH;
        }
    }

    /** Initial guess for x0.
    * @param tau value of tau
    * @param sigma value of sigma
    * @param nRevs number of complete revolutions
    * @param lowPath flag indicating low path
    * @return initial guess for x0
    */
    public static double initialGuessX(final double tau, final double sigma,
                                  final int nRevs, final boolean lowPath) {
        final double x0;
        if (nRevs == 0) {
            final double tau0 = FastMath.acos(sigma) + sigma * FastMath.sqrt(1.0 - sigma * sigma);
            final double tau1 = 2.0 * (1.0 - (sigma * sigma * sigma)) / 3.0;
            if (tau >= tau0) {
                x0 = FastMath.pow(tau0 / tau, 2.0 / 3.0) - 1.0;
            } else if (tau < tau1) {
                x0 = 2.5 * tau1 / tau * (tau1 - tau) / (1.0 - FastMath.pow(sigma, 5)) + 1.0;
            } else {
                x0 = FastMath.exp(FastMath.log(tau / tau0) / FastMath.log(tau1 / tau0) * FastMath.log(2.0)) - 1.0;
            }
        } else {
            final double x0l = (FastMath.pow((nRevs * FastMath.PI + FastMath.PI) / (8.0 * tau), 2.0 / 3.0) - 1.0) /
                (FastMath.pow((nRevs * FastMath.PI + FastMath.PI) / (8.0 * tau), 2.0 / 3.0) + 1.0);
            final double x0r = (FastMath.pow((8.0 * tau) / (nRevs * FastMath.PI), 2.0 / 3.0) - 1.0) /
                (FastMath.pow((8.0 * tau) / (nRevs * FastMath.PI), 2.0 / 3.0) + 1.0);
            if (lowPath) {
                x0 = FastMath.max(x0l, x0r);
            } else {
                x0 = FastMath.min(x0l, x0r);
            }
        }
        return x0;
    }

    /** Calculate value of y from x (and sigma).
    * @param x value of x
    * @param sigma value of sigma
    * @return value of y
    */
    public static double calculateY(final double x, final double sigma) {
        return FastMath.sqrt(1.0 - sigma * sigma * (1.0 - x * x));
    }

    /** Householder solver for the Lambert problem.
    * @param x0 initial guess for x
    * @param tau0 value of tau0
    * @param sigma value of sigma
    * @param nRevs number of complete revolutions
    * @param maxIterations maximum number of iterations
    * @param atol absolute tolerance for convergence
    * @param rtol relative tolerance for convergence
    * @return value of x
    */
    public static double householderSolver(final double x0,
                                final double tau0,
                                final double sigma,
                                final int nRevs,
                                final int maxIterations,
                                final double atol,
                                final double rtol) {
        double x = x0;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            final double y  = calculateY(x, sigma);
            final double F0 = calculateF0(x, y, nRevs, tau0, sigma);
            final double F0plusTau0 = F0 + tau0;
            final double F1 = calculateF1(x, y, F0plusTau0, sigma);
            final double F2 = calculateF2(x, y, F0plusTau0, F1, sigma);
            final double F3 = calculateF3(x, y, F1, F2, sigma);

            // Independent variable update
            final double numerator   = F1 * F1 - (F0 * F2 / 2.0);
            final double denominator = F1 * (F1 * F1 - F0 * F2) + (F3 * F0 * F0 / 6.0);
            final double xNew        = x - F0 * (numerator / denominator);

            // Convergence check
            if (FastMath.abs(xNew - x) < (rtol * FastMath.abs(x) + atol)) {
                return xNew;
            }
            x = xNew;
        }
        throw new OrekitException(OrekitMessages.LAMBERT_HOUSEHOLDER_DID_NOT_CONVERGE, maxIterations);
    }

    /** Calculate the value of F0 (estimate of TOF).
    * @param x value of x
    * @param y value of y
    * @param nRevs number of complete revolutions
    * @param tau value of tau
    * @param sigma value of sigma
    * @return value of F0
    */
    private static double calculateF0(final double x, final double y,
                                    final int nRevs, final double tau,
                                    final double sigma) {
        final double F0;
        if (nRevs == 0 && x > FastMath.sqrt(0.6) && x < FastMath.sqrt(1.4)) {
            final double eta = y - sigma * x;
            final double S1 = (1.0 - sigma - x * eta) / 2.0;
            final double Q = (4.0 / 3.0) * hyperg2F1(3.0, 1.0, 2.5, S1, Precision.EPSILON, 30000);
            F0 = (eta * eta * eta * Q + 4.0 * sigma * eta) / 2.0 - tau;
        } else {
            final double psi = calculatePsi(x, y, sigma);
            F0 = ( (psi + nRevs * FastMath.PI) / (FastMath.sqrt(FastMath.abs(1.0 - x * x))) - x + sigma * y) / (1.0 - x * x) - tau;
        }
        return F0;
    }

    /**
     * Calculate the value of F1.
     * @param x value of x
     * @param y value of y
     * @param F0 value of F0
     * @param sigma value of sigma
     * @return value of F1
     */
    private static double calculateF1(final double x, final double y,
                                    final double F0, final double sigma) {
        return (3.0 * F0 * x - 2.0 + 2.0 * sigma * sigma * sigma * (x / y)) / (1.0 - x * x);
    }

    /**
     * Calculate the value of F2.
     * @param x value of x
     * @param y value of y
     * @param F0 value of F0
     * @param F1 value of F1
     * @param sigma value of sigma
     * @return value of F2
     */
    private static double calculateF2(final double x, final double y,
                                    final double F0, final double F1,
                                    final double sigma) {
        return (3.0 * F0 + 5.0 * x * F1 + 2.0 * (1.0 - sigma * sigma) * FastMath.pow(sigma / y, 3)) / (1.0 - x * x);
    }

    /**
     * Calculate the value of F3.
     * @param x value of x
     * @param y value of y
     * @param F1 value of F1
     * @param F2 value of F2
     * @param sigma value of sigma
     * @return value of F3
     */
    private static double calculateF3(final double x, final double y,
                                    final double F1, final double F2,
                                    final double sigma) {
        return (7.0 * x * F2 + 8.0 * F1 - 6.0 * (1.0 - sigma * sigma) * FastMath.pow(sigma / y, 5) * x) / (1.0 - x * x);
    }

    /** Calculate the value of psi.
    * @param x value of x
    * @param y value of y
    * @param sigma value of sigma
    * @return value of psi
    */
    private static double calculatePsi(final double x, final double y, final double sigma) {
        if (x >= -1.0 && x < 1.0) {
            return FastMath.acos(x * y + sigma * (1.0 - x * x));
        } else if (x > 1.0) {
            return FastMath.asinh((y - x * sigma) * FastMath.sqrt(x * x - 1.0));
        } else {
            return 0.0;
        }
    }

    /** Reconstruct the values of Vr and Vt (radial and transversal components of velocity).
     * These are used together with the ir and it vectors to compute the velocity vectors at
     * the beginning and end of the transfer.
     * @param x value of x
     * @param y value of y
     * @param r1 value of r1
     * @param r2 value of r2
     * @param sigma value of sigma
     * @param gamma value of gamma
     * @param rho value of rho
     * @param zeta value of zeta
     * @return an array containing the values of Vr and Vt at the begginning and end of the transfer
     */
    public static double[] reconstructVrVt(final double x, final double y,
                                final double r1, final double r2,
                                final double sigma, final double gamma,
                                final double rho, final double zeta) {
        final double Vr1 = gamma * ((sigma * y - x) - rho * (sigma * y + x)) / r1;
        final double Vr2 = -gamma * ((sigma * y - x) + rho * (sigma * y + x)) / r2;
        final double Vt1 = gamma * zeta * (y + sigma * x) / r1;
        final double Vt2 = gamma * zeta * (y + sigma * x) / r2;
        return new double[] {Vr1, Vt1, Vr2, Vt2};
    }

    /**
    * Calculate the value of Gaussian hypergeometric function 2F1.
    * Currently we use the raw series expansion. This means we have the following
    * constraints: |z| smaller than 1, c larger than 0, c != 0.
    * Implementation based on Taylor series expansion method (a) in John Pearson's thesis
    * https://people.maths.ox.ac.uk/porterm/research/pearson_final.pdf , page 31.
    * @param a value of a
    * @param b value of b
    * @param c value of c
    * @param z value of z (|z| smaller than 1)
    * @param eps convergence threshold
    * @param maxIter maximum number of iterations
    * @return value of the 2F1 hypergeometric function
    */
    public static double hyperg2F1(final double a, final double b, final double c, final double z,
                            final double eps, final int maxIter) {

        if (FastMath.abs(z) >= 1.0) {
            throw new IllegalArgumentException("abs(z) must be < 1");
        }
        if (c <= 0.0 || FastMath.abs(c) < Precision.EPSILON) {
            throw new IllegalArgumentException("c must be positive and non-zero");
        }

        double c0 = 1.0;      // C0
        double s0 = c0;       // S0

        // we have to keep track of the last 3 terms to apply Pearson's convergence criterion
        double prevC1 = 0.0;  // C(j-1)
        double prevC2 = 0.0;  // C(j-2)
        double prevS1 = 0.0;  // S(j-1)
        double prevS2 = 0.0;  // S(j-2)

        for (int j = 1; j < maxIter; j++) {
            final double numerator = (a + j - 1) * (b + j - 1);
            final double denominator = (c + j - 1) * j;
            c0 *= (numerator / denominator) * z;
            final double Sj1 = s0 + c0;

            // we have to check 3 ratios for convergence
            if (j >= 3) {
                final double r1 = FastMath.abs(c0 / s0);
                final double r2 = FastMath.abs(prevC1 / prevS1);
                final double r3 = FastMath.abs(prevC2 / prevS2);

                if (r1 < eps && r2 < eps && r3 < eps) {
                    return Sj1;
                }
            }

            prevC2 = prevC1;
            prevC1 = c0;
            prevS2 = prevS1;
            prevS1 = s0;
            s0 = Sj1;
        }
        throw new ArithmeticException("Hypergeometric function 2F1 did not converge after max iterations");
    }

    /**
     * Computes the Jacobian matrix of the Lambert solution.
     * The rows represent the initial and terminal velocity vectors.
     * The columns represent the parameters: initial time, initial position, terminal time, terminal velocity.
     * <p>
     * Reference:
     * Di Lizia, P., Armellin, R., Zazzera, F. B., and Berz, M.
     * High Order Expansion of the Solution of Two-Point Boundary Value Problems using Differential Algebra:
     * Applications to Spacecraft Dynamics.
     * </p>
     * @param boundaryConditions Lambert boundary conditions
     * @param velocities velocities of a Lambert solution to compute the Jacobian for
     * @return Jacobian matrix
     */
    public RealMatrix computeJacobian(final LambertBoundaryConditions boundaryConditions,
                                      final LambertBoundaryVelocities velocities) {
        if (velocities != null) {
            return computeNonTrivialCase(boundaryConditions, velocities);
        } else {
            final RealMatrix nanMatrix = MatrixUtils.createRealMatrix(8, 8);
            for (int i = 0; i < nanMatrix.getRowDimension(); i++) {
                for (int j = 0; j < nanMatrix.getColumnDimension(); j++) {
                    nanMatrix.setEntry(i, j, Double.NaN);
                }
            }
            return nanMatrix;
        }
    }

    /**
     * Compute Jacobian matrix assuming there is a solution.
     * @param boundaryConditions Lambert boundary conditions
     * @param velocities velocities of a Lambert solution
     * @return Jacobian matrix
     */
    private RealMatrix computeNonTrivialCase(final LambertBoundaryConditions boundaryConditions,
                                             final LambertBoundaryVelocities velocities) {
        // propagate with automatic differentiation, using initial position as independent variables
        final int freeParameters = 8;
        final Vector3D nominalInitialPosition = boundaryConditions.getInitialPosition();
        final FieldVector3D<Gradient> initialPosition = new FieldVector3D<>(Gradient.variable(freeParameters, 5, nominalInitialPosition.getX()),
                Gradient.variable(freeParameters, 6, nominalInitialPosition.getY()),
                Gradient.variable(freeParameters, 7, nominalInitialPosition.getZ()));
        final Vector3D nominalInitialVelocity = velocities.getInitialVelocity();
        final FieldVector3D<Gradient> initialVelocity = new FieldVector3D<>(Gradient.variable(freeParameters, 1, nominalInitialVelocity.getX()),
                Gradient.variable(freeParameters, 2, nominalInitialVelocity.getY()),
                Gradient.variable(freeParameters, 3, nominalInitialVelocity.getZ()));
        final double dt = boundaryConditions.getTerminalDate().durationFrom(boundaryConditions.getInitialDate());
        final Gradient fieldDt = Gradient.variable(freeParameters, 4, dt).subtract(Gradient.variable(freeParameters, 0, 0));
        final FieldPVCoordinates<Gradient> terminalPV = KeplerianMotionCartesianUtility.predictPositionVelocity(fieldDt,
                initialPosition, initialVelocity, fieldDt.newInstance(mu));
        // fill in intermediate Jacobian matrix
        final RealMatrix intermediate = MatrixUtils.createRealMatrix(6, freeParameters);
        final FieldVector3D<Gradient> terminalVelocity = terminalPV.getVelocity();
        intermediate.setRow(0, initialVelocity.getX().getGradient());
        intermediate.setRow(1, initialVelocity.getY().getGradient());
        intermediate.setRow(2, initialVelocity.getZ().getGradient());
        intermediate.setRow(3, terminalVelocity.getX().getGradient());
        intermediate.setRow(4, terminalVelocity.getY().getGradient());
        intermediate.setRow(5, terminalVelocity.getZ().getGradient());
        // swap variables (position becomes dependent)
        final RealMatrix matrixToInvert = MatrixUtils.createRealIdentityMatrix(freeParameters);
        matrixToInvert.setRow(1, initialPosition.getX().getGradient());
        matrixToInvert.setRow(2, initialPosition.getY().getGradient());
        matrixToInvert.setRow(3, initialPosition.getZ().getGradient());
        final FieldVector3D<Gradient> terminalPosition = terminalPV.getPosition();
        matrixToInvert.setRow(5, terminalPosition.getX().getGradient());
        matrixToInvert.setRow(6, terminalPosition.getY().getGradient());
        matrixToInvert.setRow(7, terminalPosition.getZ().getGradient());
        final DecompositionSolver solver = new LUDecomposition(matrixToInvert).getSolver();
        final RealMatrix inverse = solver.getInverse();
        return intermediate.multiply(inverse);
    }

    /* Get the gravitational constant. */
    public double getMu() {
        return mu;
    }
}
