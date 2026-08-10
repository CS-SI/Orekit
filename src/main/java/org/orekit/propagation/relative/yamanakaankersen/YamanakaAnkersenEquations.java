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

package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;

/**
 * This class implements the state transition matrices of the Yamanaka-Ankersen equations of relative motion between two
 * spacecrafts. Matrices are state matrices in the derived LVLH CCSDS frame as described in the Reference paper.
 * Transformation from transformed frame to target LVLH CCSDS Local Orbital Frame is performed in the
 * YamanakaAnkersenProvider.
 *
 * <p>
 *     Source : Koji Yamanaka, Finn Ankersen, New State Transition Matrix for Relative Motion on
 *     an Arbitrary Elliptical Orbit, Journal of Guidance, Control, and Dynamics, Vol. 25, No.1, January-February 2002
 * </p>
 * @author Romain Cuvillon
 * @since 14.0
 */

public class YamanakaAnkersenEquations {

    /**
     * Private constructor to prevent instantiation.
     */
    private YamanakaAnkersenEquations() {
    }

    /**
     * Computes rho as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return rho
     */
    private static double rho(final double theta, final double targetE) {
        return 1 + targetE * FastMath.cos(theta);
    }

    /**
     * Computes c as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return c
     */
    private static double c(final double theta, final double targetE) {
        return rho(theta, targetE) * FastMath.cos(theta);
    }

    /**
     * Computes s as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return s
     */
    private static double s(final double theta, final double targetE) {
        return rho(theta, targetE) * FastMath.sin(theta);
    }

    /**
     * Computes c_p as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return c_p
     */
    private static double cP(final double theta, final double targetE) {
        return -(FastMath.sin(theta) + targetE * FastMath.sin(2 * theta));
    }

    /**
     * Computes s_p as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return s_p
     */
    private static double sP(final double theta, final double targetE) {
        return FastMath.cos(theta) + targetE * FastMath.cos(2 * theta);
    }

    /**
     * Computes the state transition matrices provided by the Yamanaka-Ankersen equations at time t from an initial
     * state.
     *
     * @param timeSinceEpoch     duration in seconds since epoch
     * @param targetA            semi-major axis of the target orbit
     * @param targetE            eccentricity of the target orbit
     * @param targetInitialTheta true-anomaly of the target at epoch
     * @param targetTheta        current true-anomaly of the target
     * @param mu                 gravitational constant
     * @return state transition matrices at a given time t from an initial state
     */
    public static YamanakaAnkersenMatrices computeMatrices(final double timeSinceEpoch, final double targetA,
                                                           final double targetE, final double targetInitialTheta,
                                                           final double targetTheta, final double mu) {
        // Intermediate values
        final double p = targetA * (1 - targetE * targetE);
        final double k2 = FastMath.sqrt(mu / (p * p * p));
        final double J = k2 * timeSinceEpoch;
        final double rho = rho(targetTheta, targetE);
        final double c = c(targetTheta, targetE);
        final double s = s(targetTheta, targetE);
        final double cp = cP(targetTheta, targetE);
        final double sp = sP(targetTheta, targetE);

        final double rho0 = rho(targetInitialTheta, targetE);
        final double c0 = c(targetInitialTheta, targetE);
        final double s0 = s(targetInitialTheta, targetE);
        final double rho_delta = rho(targetTheta - targetInitialTheta, targetE);
        final double c_delta = c(targetTheta - targetInitialTheta, targetE);
        final double s_delta = s(targetTheta - targetInitialTheta, targetE);

        // First operand of in-plane state transition matrix (eq. 76 of Yamanaka-Ankersen paper)
        final double[][] phi_theta = {
                {1, -c * (1 + 1 / rho), s * (1 + 1 / rho), 3 * rho * rho * J},
                {0, s, c, 2 - 3 * targetE * s * J},
                {0, 2 * s, 2 * c - targetE, 3 * (1 - 2 * targetE * s * J)},
                {0, sp, cp, -3 * targetE * (sp * J + s / (rho * rho))}
        };
        // Second operand of in-plane state transition matrix (eq. 80 of Yamanaka-Ankersen paper)
        final double[][] phiInv_theta0 = {
                {1 - targetE * targetE,
                 3 * targetE * s0 * (1 / rho0 + 1 / (rho0 * rho0)),
                 -targetE * s0 * (1 + 1 / rho0),
                 -targetE * c0 + 2},
                {0,
                 -3 * s0 / rho0 * (1. + targetE * targetE / rho0),
                 s0 * (1 + 1 / rho0),
                 c0 - 2 * targetE},
                {0,
                 -3 * (c0 / rho0 + targetE),
                 c0 * (1 + 1 / rho0) + targetE,
                 -s0},
                {0,
                 3 * rho0 + targetE * targetE - 1,
                 -rho0 * rho0, targetE * s0}
        };

        // In-plane state transition matrix
        final RealMatrix inPlaneMatrix = MatrixUtils.createRealMatrix(phi_theta)
                                                    .multiply(MatrixUtils.createRealMatrix(phiInv_theta0)
                                                                         .scalarMultiply(1 / (1 - targetE * targetE)));

        // Compute the out-plane transition matrix (eq. 84 of Yamanaka-Ankersen paper)
        final double[][] outPlaneMatrix = {
                {c_delta / rho_delta, s_delta / rho_delta},
                {-s_delta / rho_delta, c_delta / rho_delta}
        };
        return new YamanakaAnkersenMatrices(timeSinceEpoch, targetTheta, inPlaneMatrix,
                                            MatrixUtils.createRealMatrix(outPlaneMatrix));
    }
}
