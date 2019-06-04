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
    private double lDim;

    /** Orbital Velocity of m1, m/s. */
    private double vDim;

    /** Orbital Period of m1 and m2, seconds. */
    private double tDim;

    /** CR3BP System name. */
    private String name;

    /** Primary body. */
    private CelestialBody primaryBody;

    /** Secondary body. */
    private CelestialBody secondaryBody;

    /** Distance between the primary and the CR3BP System barycenter, meters. */
    private double barycenter;

    /** Simple constructor.
     * @param primaryBody Primary Body in the CR3BP System
     * @param secondaryBody Secondary Body in the CR3BP System
     * @param a Semi-Major Axis of the secondary body
     */
    public CR3BPSystem(final CelestialBody primaryBody, final CelestialBody secondaryBody, final double a) {

        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;

        this.name = primaryBody.getName() + "_" + secondaryBody.getName();

        final double mu1 = primaryBody.getGM();
        final double mu2 = secondaryBody.getGM();

        this.lDim = a;

        this.mu = mu2 / (mu1 + mu2);
        this.barycenter = lDim * mu;

        this.vDim = FastMath.sqrt(mu1 / (lDim - barycenter));
        this.tDim = 2 * FastMath.PI * lDim / vDim;

    }

    /** Get the CR3BP mass ratio of the system mu2/(mu1+mu2).
     * @return CR3BP mass ratio of the system mu2/(mu1+mu2)
     */
    public double getMu() {
        return mu;
    }

    /** Get the CR3BP distance between the two bodies.
     * @return CR3BP distance between the two bodies(m)
     */
    public double getLdim() {
        return lDim;
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
        final Frame baryFrame = new CR3BPRotatingFrame(lDim, barycenter, primaryBody, secondaryBody);
        return baryFrame;
    }

    /** Get the distance of the CR3BP Barycenter from the primary.
     * @return distance of the CR3BP Barycenter from the primary
     */
    public double getBarycenter() {
        return barycenter;
    }

    /**
     * Get the position of the Lagrangian point in the CR3BP Rotating frame.
     * @param lagrangianPoint Lagrangian Point to consider
     * @return position of the Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getLPosition(final LagrangianPoints lagrangianPoint) {
        final Vector3D lpos;
        final double baseR;
        final double[] searchInterval;
        final double r;
        final BracketingNthOrderBrentSolver solver;

        switch (lagrangianPoint) {

            case L1:
                baseR = 1 - FastMath.cbrt(mu / 3);
                final UnivariateFunction l1Equation = x -> {
                    final double leq1 =
                        x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
                    final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
                    final double leq3 = mu * (x + mu) * (x + mu);
                    return leq1 - leq2 + leq3;
                };
                searchInterval =
                    UnivariateSolverUtils.bracket(l1Equation, baseR, -mu,
                                                  1 - mu, 1E-6, 1,
                                                  MAX_EVALUATIONS);
                solver =
                    new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                                      ABSOLUTE_ACCURACY,
                                                      FUNCTION_ACCURACY,
                                                      MAX_ORDER);
                r =
                    solver.solve(MAX_EVALUATIONS, l1Equation, searchInterval[0],
                                 searchInterval[1], AllowedSolution.ANY_SIDE);
                lpos = new Vector3D(r * lDim, 0.0, 0.0);
                break;

            case L2:
                baseR = 1 + FastMath.cbrt(mu / 3);
                final UnivariateFunction l2Equation = x -> {
                    final double leq1 =
                        x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
                    final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
                    final double leq3 = mu * (x + mu) * (x + mu);
                    return leq1 - leq2 - leq3;
                };
                searchInterval =
                    UnivariateSolverUtils.bracket(l2Equation, baseR, 1 - mu, 2,
                                                  1E-6, 1, MAX_EVALUATIONS);
                solver =
                    new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                                      ABSOLUTE_ACCURACY,
                                                      FUNCTION_ACCURACY,
                                                      MAX_ORDER);
                r =
                    solver.solve(MAX_EVALUATIONS, l2Equation, searchInterval[0],
                                 searchInterval[1], AllowedSolution.ANY_SIDE);
                lpos = new Vector3D(r * lDim, 0.0, 0.0);
                break;

            case L3:
                baseR = -(1 + 5 * mu / 12);
                final UnivariateFunction l3Equation = x -> {
                    final double leq1 =
                        x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
                    final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
                    final double leq3 = mu * (x + mu) * (x + mu);
                    return leq1 + leq2 + leq3;
                };
                searchInterval =
                    UnivariateSolverUtils.bracket(l3Equation, baseR, -2, -mu,
                                                  1E-6, 1, MAX_EVALUATIONS);
                solver =
                    new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                                      ABSOLUTE_ACCURACY,
                                                      FUNCTION_ACCURACY,
                                                      MAX_ORDER);
                r =
                    solver.solve(MAX_EVALUATIONS, l3Equation, searchInterval[0],
                                 searchInterval[1], AllowedSolution.ANY_SIDE);
                lpos = new Vector3D(r * lDim, 0.0, 0.0);
                break;

            case L4:
                lpos =
                    new Vector3D((0.5 - mu) * lDim, FastMath.sqrt(3) / 2 * lDim,
                                 0);
                break;

            case L5:
                lpos =
                    new Vector3D((0.5 - mu) * lDim,
                                 -FastMath.sqrt(3) / 2 * lDim, 0);
                break;
            default:
                lpos = new Vector3D(0, 0, 0);
                break;
        }
        return lpos;
    }
}
