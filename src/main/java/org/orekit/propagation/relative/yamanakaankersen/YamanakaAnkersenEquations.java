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

import org.hipparchus.analysis.function.Power;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;


/**
 * This class implements the state transition matrices of the Yamanaka-Ankersen equations of relative motion between two spacecrafts.
 * Matrices are state matrices in the derived LVLH CCSDS frame as described in the Reference paper. Transformation from transformed frame to target LVLH CCSDS Local Orbital Frame is performed in the YamanakaAnkersenProvider.
 * Source : Koji Yamanaka, Finn Ankersen, New State Transition Matrix for Relative Motion on an Arbitrary Elliptical Orbit, Journal of Guidance, Control, and Dynamics, Vol. 25, No.1, January-February 2002
 *
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
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return rho.
     */
    private static double rho(final double theta, final double targetE) {
        return 1 + targetE * FastMath.cos(theta);
    }

    /**
     * Computes c as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return c.
     */
    private static double c(final double theta, final double targetE) {
        return rho(theta, targetE) * FastMath.cos(theta);
    }

    /**
     * Computes s as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return s.
     */
    private static double s(final double theta, final double targetE) {
        return rho(theta, targetE) * FastMath.sin(theta);
    }

    /**
     * Computes c_p as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return c_p.
     */
    private static double c_p(final double theta, final double targetE) {
        return -(FastMath.sin(theta) + targetE * FastMath.sin(2 * theta));
    }

    /**
     * Computes s_p as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return s_p.
     */
    private static double s_p(final double theta, final double targetE) {
        return FastMath.cos(theta) + targetE * FastMath.cos(2 * theta);
    }

    /**
     * Computes the state transition matrices provided by the Yamanaka-Ankersen equations at time t from an initial state.
     * @param timeSinceEpoch duration in seconds since epoch.
     * @param targetA semi-major axis of the target orbit.
     * @param targetE eccentricity of the target orbit.
     * @param targetInitialTheta true-anomaly of the target at epoch.
     * @param targetTheta current true-anomaly of the target.
     * @param mu gravitational constant.
     * @return state transition matrices at a given time t from an initial state.
     */
    public static YamanakaAnkersenMatrices computeMatrices(final double timeSinceEpoch, final double targetA, final double targetE, final double targetInitialTheta, final double targetTheta, final double mu) {
        final Power power = new Power(  (double) 1 / 4);
        final double p = targetA * (1 - targetE * targetE);
        final double k = power.value(mu / FastMath.pow(p, 3));
        final double J = k * k * timeSinceEpoch;
        final double rho_theta = rho(targetTheta, targetE);
        final double c_theta = c(targetTheta, targetE);
        final double s_theta = s(targetTheta, targetE);
        final double c_p_theta = c_p(targetTheta, targetE);
        final double s_p_theta = s_p(targetTheta, targetE);

        final double rho_theta0 = rho(targetInitialTheta, targetE);
        final double c_theta0 = c(targetInitialTheta, targetE);
        final double s_theta0 = s(targetInitialTheta, targetE);
        final double rho_deltaTheta = rho(targetTheta - targetInitialTheta, targetE);
        final double c_deltaTheta = c(targetTheta - targetInitialTheta, targetE);
        final double s_deltaTheta = s(targetTheta - targetInitialTheta, targetE);

        final double[][] phi_theta =  { {1, -c_theta * ( 1 + 1 / rho_theta), s_theta * ( 1 + 1 / rho_theta),                                       3 * rho_theta * rho_theta * J},
                                        {0,                         s_theta,                        c_theta,                                       2 - 3 * targetE * s_theta * J},
                                        {0,                     2 * s_theta,          2 * c_theta - targetE,                                3 * ( 1 - 2 * targetE * s_theta * J)},
                                        {0,                       s_p_theta,                      c_p_theta,  -3 * targetE * (s_p_theta * J + s_theta / (rho_theta * rho_theta))}};
        final double[][] phiInv_theta0 = { {1 - targetE * targetE,      3 * targetE * s_theta0 * (1 / rho_theta0 + 1 / (rho_theta0 * rho_theta0)), -targetE * s_theta0 * (1 + 1 / rho_theta0), -targetE * c_theta0 + 2},
                                           {                    0, -3 * s_theta0 * (1 / rho_theta0 + FastMath.pow(targetE / rho_theta0, 2)),            s_theta0 * (1 + 1 / rho_theta0), c_theta0 - 2 *  targetE},
                                           {                    0,                                         -3 * (c_theta0 / rho_theta0 + targetE), c_theta0 * ( 1 + 1 / rho_theta0) + targetE,               -s_theta0},
                                           {                    0,                               3 * rho_theta0 + FastMath.pow(targetE, 2) - 1,                   -rho_theta0 * rho_theta0,     targetE * s_theta0}};

        final RealMatrix inPlaneMatrix = MatrixUtils.createRealMatrix(phi_theta).multiply(MatrixUtils.createRealMatrix(phiInv_theta0).scalarMultiply(1 / (1 - targetE * targetE)));

        final double[][] outPlaneMatrix = { { c_deltaTheta / rho_deltaTheta, s_deltaTheta / rho_deltaTheta},
                                            {-s_deltaTheta / rho_deltaTheta, c_deltaTheta / rho_deltaTheta}};
        return new YamanakaAnkersenMatrices(timeSinceEpoch, targetTheta, inPlaneMatrix, MatrixUtils.createRealMatrix(outPlaneMatrix));
    }
}
