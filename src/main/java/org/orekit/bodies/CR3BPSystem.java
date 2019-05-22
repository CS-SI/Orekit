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
import org.orekit.frames.CR3BPLFrame;
import org.orekit.frames.CR3BPRotatingFrame;
import org.orekit.frames.Frame;

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

    /** Primary body GM. (m³/s²) */
    private double mu1;

    /** Secondary body GM. (m³/s²) */
    private double mu2;

    /** Distance between the primary and the CR3BP System barycenter, meters. */
    private double barycenter;

    /** Simple constructor.
     * @param primaryBody Primary Body in the CR3BP System
     * @param secondaryBody Secondary Body in the CR3BP System
     */
    public CR3BPSystem(final CelestialBody primaryBody, final CelestialBody secondaryBody) {

        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;

        this.name = primaryBody.getName() + "_" + secondaryBody.getName();

        this.mu1 = primaryBody.getGM();
        this.mu2 = secondaryBody.getGM();

        switch (name) {
            case "Sun_Jupiter":
                lDim = 7.78340821E11;
                vDim = 13064.059343815603;
                tDim = 3.7434456486914164E8;
                break;
            case "Sun_Earth":
                lDim = 1.4959262E11;
                vDim = 29785.259280799997;
                tDim = 3.1556487159820825E7;
                break;
            case "Earth_Moon":
                lDim = 384399000.0;
                vDim = 1024.5481799056872;
                tDim = 2357380.742325712;
                break;
            default:
                break;
        }

        this.mu = mu2 / (mu1 + mu2);
        this.barycenter = lDim * mu;
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

    /** Get the CR3BP L1 centered Frame.
     * @return CR3BP L1 centered Frame
     */
    public Frame getL1Frame() {
        final Frame l1Frame =
            new CR3BPLFrame(getRotatingFrame(), getL1Position());
        return l1Frame;
    }

    /** Get the CR3BP L2 centered Frame.
     * @return CR3BP L2 centered Frame
     */
    public Frame getL2Frame() {
        final Frame l2Frame =
            new CR3BPLFrame(getRotatingFrame(), getL2Position());
        return l2Frame;
    }

    /** Get the CR3BP L3 centered Frame.
     * @return CR3BP L3 centered Frame
     */
    public Frame getL3Frame() {
        final Frame l3Frame =
            new CR3BPLFrame(getRotatingFrame(), getL3Position());
        return l3Frame;
    }

    /** Get the distance of the CR3BP Barycenter from the primary.
     * @return distance of the CR3BP Barycenter from the primary
     */
    public double getBarycenter() {
        return barycenter;
    }

    /**
     * Get the position of the first Lagrangian point in the CR3BP Rotating frame.
     * @return position of the first Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getL1Position() {

        final double baseR = 1 - FastMath.cbrt(mu / 3);
        final UnivariateFunction l1Equation = x -> {
            final double leq1 =
                x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
            final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
            final double leq3 = mu * (x + mu) * (x + mu);
            return leq1 - leq2 + leq3;
        };
        final double[] searchInterval =
            UnivariateSolverUtils.bracket(l1Equation, baseR, -mu, 1 - mu, 1E-6,
                                          1, MAX_EVALUATIONS);
        final BracketingNthOrderBrentSolver solver =
            new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                              ABSOLUTE_ACCURACY,
                                              FUNCTION_ACCURACY, MAX_ORDER);
        final double r =
            solver.solve(MAX_EVALUATIONS, l1Equation, searchInterval[0],
                         searchInterval[1], AllowedSolution.ANY_SIDE);
        final Vector3D l1 = new Vector3D(r * lDim, 0.0, 0.0);
        return l1;
    }

    /**
     * Get the position of the second Lagrangian point in the CR3BP Rotating frame.
     * @return position of the second Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getL2Position() {

        final double baseR = 1 + FastMath.cbrt(mu / 3);
        final UnivariateFunction l2Equation = x -> {
            final double leq1 =
                x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
            final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
            final double leq3 = mu * (x + mu) * (x + mu);
            return leq1 - leq2 - leq3;
        };
        final double[] searchInterval =
            UnivariateSolverUtils.bracket(l2Equation, baseR, 1 - mu, 2, 1E-6,
                                          1, MAX_EVALUATIONS);
        final BracketingNthOrderBrentSolver solver =
            new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                              ABSOLUTE_ACCURACY,
                                              FUNCTION_ACCURACY, MAX_ORDER);
        final double r =
            solver.solve(MAX_EVALUATIONS, l2Equation, searchInterval[0],
                         searchInterval[1], AllowedSolution.ANY_SIDE);
        final Vector3D l2 = new Vector3D(r * lDim, 0.0, 0.0);
        return l2;
    }

    /**
     * Get the position of the third Lagrangian point in the CR3BP Rotating frame.
     * @return position of the third Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getL3Position() {

        final double baseR = -(1 + 5 * mu / 12);
        final UnivariateFunction l3Equation = x -> {
            final double leq1 =
                x * (x + mu) * (x + mu) * (x + mu - 1) * (x + mu - 1);
            final double leq2 = (1 - mu) * (x + mu - 1) * (x + mu - 1);
            final double leq3 = mu * (x + mu) * (x + mu);
            return leq1 + leq2 + leq3;
        };
        final double[] searchInterval =
            UnivariateSolverUtils.bracket(l3Equation, baseR, -2, -mu, 1E-6, 1,
                                          MAX_EVALUATIONS);
        final BracketingNthOrderBrentSolver solver =
            new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                              ABSOLUTE_ACCURACY,
                                              FUNCTION_ACCURACY, MAX_ORDER);
        final double r =
            solver.solve(MAX_EVALUATIONS, l3Equation, searchInterval[0],
                         searchInterval[1], AllowedSolution.ANY_SIDE);
        final Vector3D l3 = new Vector3D(r * lDim, 0.0, 0.0);
        return l3;
    }

    /**
     * Get the position of the fourth Lagrangian point in the CR3BP Rotating frame.
     * @return position of the fourth Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getL4Position() {
        final Vector3D l4 =
            new Vector3D((0.5 - mu) * lDim, FastMath.sqrt(3) / 2 * lDim, 0);
        return l4;
    }

    /**
     * Get the position of the fifth Lagrangian point in the CR3BP Rotating frame.
     * @return position of the fifth Lagrangian point in the CR3BP Rotating frame (m)
     */
    public Vector3D getL5Position() {
        final Vector3D l5 =
            new Vector3D((0.5 - mu) * lDim, -FastMath.sqrt(3) / 2 * lDim, 0);
        return l5;
    }

}
