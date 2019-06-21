/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.bodies;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.CR3BPRotatingFrame;
import org.orekit.frames.Frame;
import org.orekit.utils.LagrangianPoints;

/**
 * Class creating, from two different celestial bodies, the corresponding system
 * with respect to the Circular Restricted Three Body problem hypotheses.
 * @see "Dynamical systems, the three-body problem, and space mission design, Koon, Lo, Marsden, Ross"
 * @author Vincent Mouraux
 */
public class CR3BPSystem {

    /** Relative accuracy on position for solver. */
    private static final double RELATIVE_ACCURACY = 1e-14;

    /** Absolute accuracy on position for solver (1mm). */
    private static final double ABSOLUTE_ACCURACY = 1e-3;

    /** Function value accuracy for solver (set to 0 so we rely only on position for convergence). */
    private static final double FUNCTION_ACCURACY = 0;

    /** Maximal order for solver. */
    private static final int MAX_ORDER = 5;

    /** Maximal number of evaluations for solver. */
    private static final int MAX_EVALUATIONS = 10000;

    /** Mass ratio. */
    private final double mu;

    /** Distance between the two primaries, meters. */
    private final double dDim;

    /** Orbital Velocity of m1, m/s. */
    private final double vDim;

    /** Orbital Period of m1 and m2, seconds. */
    private final double tDim;

    /** CR3BP System name. */
    private final String name;

    /** Primary body. */
    private final CelestialBody primaryBody;

    /** Secondary body. */
    private final CelestialBody secondaryBody;

    /** L1 Point position in the rotating frame. */
    private final Vector3D l1Position;

    /** L2 Point position in the rotating frame. */
    private final Vector3D l2Position;

    /** L3 Point position in the rotating frame. */
    private final Vector3D l3Position;

    /** L4 Point position in the rotating frame. */
    private final Vector3D l4Position;

    /** L5 Point position in the rotating frame. */
    private final Vector3D l5Position;

    /** Distance between a L1 and the secondaryBody. */
    private final double gamma1;

    /** Distance between a L2 and the secondaryBody. */
    private final double gamma2;

    /** Distance between a L3 and the primaryBody. */
    private final double gamma3;

    /**
     * Simple constructor.
     * <p>
     * Standard constructor to use to define a CR3BP System. Mass ratio is
     * calculated from JPL data.
     * </p>
     * @param primaryBody Primary Body in the CR3BP System
     * @param secondaryBody Secondary Body in the CR3BP System
     * @param a Semi-Major Axis of the secondary body
     */
    public CR3BPSystem(final CelestialBody primaryBody, final CelestialBody secondaryBody, final double a) {
        this(primaryBody, secondaryBody, a, secondaryBody.getGM() / (secondaryBody.getGM() + primaryBody.getGM()));
    }

    /**
     * Simple constructor.
     * <p>
     * This constructor can be used to define a CR3BP System if the user wants
     * to use a specified mass ratio.
     * </p>
     * @param primaryBody Primary Body in the CR3BP System
     * @param secondaryBody Secondary Body in the CR3BP System
     * @param a Semi-Major Axis of the secondary body
     * @param mu Mass ratio of the CR3BPSystem
     */
    public CR3BPSystem(final CelestialBody primaryBody, final CelestialBody secondaryBody, final double a, final double mu) {
        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;

        this.name = primaryBody.getName() + "_" + secondaryBody.getName();

        final double mu1 = primaryBody.getGM();

        this.mu = mu;

        this.dDim = a;
        this.vDim = FastMath.sqrt(mu1 / (dDim - mu * dDim));
        this.tDim = 2 * FastMath.PI * dDim / vDim;

        // Calculation of Lagrangian Points position using CR3BP equations

        // L1
        final BracketingNthOrderBrentSolver solver =
            new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                              ABSOLUTE_ACCURACY,
                                              FUNCTION_ACCURACY, MAX_ORDER);

        final double baseR1 = 1 - FastMath.cbrt(mu / 3);
        final UnivariateFunction l1Equation = x -> {
            final double leq1 =
                x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
            final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
            final double leq3 = mu * (x + mu) * (x + mu);
            return leq1 - leq2 + leq3;
        };
        final double[] searchInterval1 =
            UnivariateSolverUtils.bracket(l1Equation, baseR1, -mu,
                                          1 - mu, 1E-6, 1,
                                          MAX_EVALUATIONS);

        final double r1 =
                        solver.solve(MAX_EVALUATIONS, l1Equation, searchInterval1[0],
                         searchInterval1[1], AllowedSolution.ANY_SIDE);

        l1Position = new Vector3D(r1, 0.0, 0.0);

        // L2

        final double baseR2 = 1 + FastMath.cbrt(mu / 3);
        final UnivariateFunction l2Equation = x -> {
            final double leq21 =
                x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
            final double leq22 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
            final double leq23 = mu * (x + mu) * (x + mu);
            return leq21 - leq22 - leq23;
        };
        final double[] searchInterval2 =
            UnivariateSolverUtils.bracket(l2Equation, baseR2, 1 - mu, 2, 1E-6,
                                          1, MAX_EVALUATIONS);

        final double r2 =
            solver.solve(MAX_EVALUATIONS, l2Equation, searchInterval2[0],
                         searchInterval2[1], AllowedSolution.ANY_SIDE);

        l2Position = new Vector3D(r2, 0.0, 0.0);

        // L3

        final double baseR3 = -(1 + 5 * mu / 12);
        final UnivariateFunction l3Equation = x -> {
            final double leq31 =
                x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
            final double leq32 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
            final double leq33 = mu * (x + mu) * (x + mu);
            return leq31 + leq32 + leq33;
        };
        final double[] searchInterval3 =
            UnivariateSolverUtils.bracket(l3Equation, baseR3, -2, -mu, 1E-6, 1,
                                          MAX_EVALUATIONS);

        final double r3 =
            solver.solve(MAX_EVALUATIONS, l3Equation, searchInterval3[0],
                         searchInterval3[1], AllowedSolution.ANY_SIDE);

        l3Position = new Vector3D(r3, 0.0, 0.0);

        // L4
        l4Position = new Vector3D(0.5 - mu, FastMath.sqrt(3) / 2, 0);

        // L5
        l5Position = new Vector3D(0.5 - mu, -FastMath.sqrt(3) / 2, 0);

        // Calculation of collinear points gamma

        // L1
        final double x1 = getLPosition(LagrangianPoints.L1).getX();
        final double DCP1 = 1 - mu;
        this.gamma1 = DCP1 - x1;

        // L2
        final double x2 = getLPosition(LagrangianPoints.L2).getX();
        final double DCP2 = 1 - mu;
        this.gamma2 = x2 - DCP2;

        // L3
        final double x3 = getLPosition(LagrangianPoints.L3).getX();
        final double DCP3 = -mu;
        this.gamma3 = DCP3 - x3;

    }

    /** Get the CR3BP mass ratio of the system mu2/(mu1+mu2).
     * @return CR3BP mass ratio of the system mu2/(mu1+mu2)
     */
    public double getMassRatio() {
        return mu;
    }

    /** Get the CR3BP distance between the two bodies.
     * @return CR3BP distance between the two bodies(m)
     */
    public double getDdim() {
        return dDim;
    }

    /** Get the CR3BP orbital velocity of m2.
     * @return CR3BP orbital velocity of m2(m/s)
     */
    public double getVdim() {
        return vDim;
    }

    /** Get the CR3BP orbital period of m2 around m1.
     * @return CR3BP orbital period of m2 around m1(s)
     */
    public double getTdim() {
        return tDim;
    }

    /** Get the name of the CR3BP system.
     * @return name of the CR3BP system
     */
    public String getName() {
        return name;
    }

    /** Get the primary CelestialBody.
     * @return primary CelestialBody
     */
    public CelestialBody getPrimary() {
        return primaryBody;
    }

    /** Get the secondary CelestialBody.
     * @return secondary CelestialBody
     */
    public CelestialBody getSecondary() {
        return secondaryBody;
    }

    /** Get the CR3BP Rotating Frame.
     * @return CR3BP Rotating Frame
     */
    public Frame getRotatingFrame() {
        final Frame baryFrame = new CR3BPRotatingFrame(dDim, mu * dDim, primaryBody, secondaryBody);
        return baryFrame;
    }

    /**
     * Get the position of the Lagrangian point in the CR3BP Rotating frame.
     * @param lagrangianPoint Lagrangian Point to consider
     * @return position of the Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getLPosition(final LagrangianPoints lagrangianPoint) {

        final Vector3D lPosition;

        switch (lagrangianPoint) {

            case L1:
                lPosition = l1Position;
                break;

            default:
                lPosition = l2Position;
                break;

            case L3:
                lPosition = l3Position;
                break;

            case L4:
                lPosition = l4Position;
                break;

            case L5:
                lPosition = l5Position;
                break;
        }
        return lPosition;
    }

    /**
     * Get the position of the Lagrangian point in the CR3BP Rotating frame.
     * @param lagrangianPoint Lagrangian Point to consider
     * @return Distance between a Lagrangian Point and its closest primary.
     */
    public double getGamma(final LagrangianPoints lagrangianPoint) {

        final double gamma;

        switch (lagrangianPoint) {

            case L1:
                gamma = gamma1;
                break;

            case L2:
                gamma = gamma2;
                break;

            case L3:
                gamma = gamma3;
                break;

            default:
                gamma = 0;
        }
        return gamma;
    }
}
