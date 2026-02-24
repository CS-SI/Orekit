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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.MathArrays;

/**
 * This class implements the state transition matrices of the Yamanaka-Ankersen equations of relative motion between two spacecrafts.
 * Matrices are state matrices in the derived LVLH CCSDS frame as described in the Reference paper. Transformation from transformed frame to target LVLH CCSDS Local Orbital Frame is performed in the YamanakaAnkersenProvider.
 * Source : Koji Yamanaka, Finn Ankersen, New State Transition Matrix for Relative Motion on an Arbitrary Elliptical Orbit, Journal of Guidance, Control, and Dynamics, Vol. 25, No.1, January-February 2002
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */

public class FieldYamanakaAnkersenEquations<T extends CalculusFieldElement<T>> {

    /**
     * Private constructor to prevent instantiation.
     */
    public FieldYamanakaAnkersenEquations() {
    }

    /**
     * Computes rho as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return rho.
     */
    private T rho(final T theta, final T targetE) { return targetE.multiply(theta.cos()).add(1); }

    /**
     * Computes c as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return c.
     */
    private T c(final T theta, final T targetE) {
        return rho(theta, targetE).multiply(theta.cos());
    }

    /**
     * Computes s as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return s.
     */
    public T s(final T theta, final T targetE) {
        return rho(theta, targetE).multiply(theta.sin());
    }

    /**
     * Computes c_p as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return c_p.
     */
    private T c_p(final T theta, final T targetE) {
        return theta.sin().add(targetE.multiply(theta.multiply(2).sin())).multiply(-1.);
    }

    /**
     * Computes s_p as in reference paper.
     * @param theta true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return s_p.
     */
    private T s_p(final T theta, final T targetE) {
        return theta.cos().add(targetE.multiply(theta.multiply(2).cos()));
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
    public FieldYamanakaAnkersenMatrices<T> computeMatrices(final T timeSinceEpoch, final T targetA, final T targetE, final T targetInitialTheta, final T targetTheta, final T mu) {

        final T k = mu.divide(targetA.pow(3)).pow((double) 1 / 4);
        final T J = k.pow(2).multiply(timeSinceEpoch);
        final T rho_theta = rho(targetTheta, targetE);
        final T c_theta = c(targetTheta, targetE);
        final T s_theta = s(targetTheta, targetE);
        final T c_p_theta = c_p(targetTheta, targetE);
        final T s_p_theta = s_p(targetTheta, targetE);
        final T rho_theta0 = rho(targetInitialTheta, targetE);
        final T c_theta0 = c(targetInitialTheta, targetE);
        final T s_theta0 = s(targetInitialTheta, targetE);
        final T rho_deltaTheta = rho(targetTheta.subtract(targetInitialTheta), targetE);
        final T c_deltaTheta = c(targetTheta.subtract(targetInitialTheta), targetE);
        final T s_deltaTheta = s(targetTheta.subtract(targetInitialTheta), targetE);

        final T zero = targetA.getField().getZero();

        final T[][] phi_theta = MathArrays.buildArray(targetA.getField(), 4, 4);
        phi_theta[0][0] = zero.add(1);
        phi_theta[0][1] = c_theta.multiply(-1.).add(c_theta.multiply(-1.).divide(rho_theta));
        phi_theta[0][2] = s_theta.add(s_theta.divide(rho_theta));
        phi_theta[0][3] = rho_theta.pow(2).multiply(J).multiply(3);
        phi_theta[1][0] = zero;
        phi_theta[1][1] = s_theta;
        phi_theta[1][2] = c_theta;
        phi_theta[1][3] = targetE.multiply(s_theta).multiply(J).multiply(-3.).add(2);
        phi_theta[2][0] = zero;
        phi_theta[2][1] = s_theta.multiply(2);
        phi_theta[2][2] = c_theta.multiply(2).subtract(targetE);
        phi_theta[2][3] = targetE.multiply(s_theta).multiply(J).multiply(-2.).add(1).multiply(3);
        phi_theta[3][0] = zero;
        phi_theta[3][1] = s_p_theta;
        phi_theta[3][2] = c_p_theta;
        phi_theta[3][3] = targetE.multiply(-3.).multiply(s_p_theta.multiply(J).add(s_theta.divide(rho_theta.pow(2))));


        final T[][] phiInv_theta0 = MathArrays.buildArray(targetA.getField(), 4, 4);
        phiInv_theta0[0][0] = targetE.pow(2).multiply(-1.).add(1);
        phiInv_theta0[0][1] = targetE.multiply(s_theta0).multiply(3).divide(rho_theta0).add(targetE.multiply(s_theta0).multiply(3).divide(rho_theta0.pow(2)));
        phiInv_theta0[0][2] = targetE.multiply(s_theta0).multiply(-1.).add(targetE.multiply(s_theta0).multiply(-1.).divide(rho_theta0));
        phiInv_theta0[0][3] = targetE.multiply(c_theta0).multiply(-1.).add(2);
        phiInv_theta0[1][0] = zero;
        phiInv_theta0[1][1] = s_theta0.multiply(-3.).divide(rho_theta0).add(s_theta0.multiply(-3.).multiply(targetE.divide(rho_theta0).pow(2)));
        phiInv_theta0[1][2] = s_theta0.add(s_theta0.divide(rho_theta0));
        phiInv_theta0[1][3] = c_theta0.subtract(targetE.multiply(2));
        phiInv_theta0[2][0] = zero;
        phiInv_theta0[2][1] = c_theta0.divide(rho_theta0).add(targetE).multiply(-3.);
        phiInv_theta0[2][2] = c_theta0.add(c_theta0.divide(rho_theta0)).add(targetE);
        phiInv_theta0[2][3] = s_theta0.multiply(-1.);
        phiInv_theta0[3][0] = zero;
        phiInv_theta0[3][1] = rho_theta0.multiply(3).add(targetE.pow(2)).add(-1.);
        phiInv_theta0[3][2] = rho_theta0.pow(2).multiply(-1.);
        phiInv_theta0[3][3] = targetE.multiply(s_theta0);


        final FieldMatrix<T> inPlaneMatrix = MatrixUtils.createFieldMatrix(phi_theta).multiply(MatrixUtils.createFieldMatrix(phiInv_theta0)).scalarMultiply(zero.add(1).divide(targetE.pow(2).multiply(-1).add(1)));

        // Compute the out-plane transition matrix.
        final T[][] outPlaneMatrix = MathArrays.buildArray(targetA.getField(), 2, 2);
        outPlaneMatrix[0][0] = c_deltaTheta.divide(rho_deltaTheta);
        outPlaneMatrix[0][1] = s_deltaTheta.divide(rho_deltaTheta);
        outPlaneMatrix[1][0] = s_deltaTheta.divide(rho_deltaTheta).multiply(-1.);
        outPlaneMatrix[1][1] = c_deltaTheta.divide(rho_deltaTheta);


        return new FieldYamanakaAnkersenMatrices<>(timeSinceEpoch, targetTheta, inPlaneMatrix, MatrixUtils.createFieldMatrix(outPlaneMatrix));
    }
}

