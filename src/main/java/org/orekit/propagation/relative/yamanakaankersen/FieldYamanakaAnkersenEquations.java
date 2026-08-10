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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;

/**
 * This class implements the state transition matrices of the Yamanaka-Ankersen equations of relative motion between two
 * spacecrafts. Matrices are state matrices in the derived LVLH CCSDS frame as described in the Reference paper.
 * Transformation from transformed frame to target LVLH CCSDS Local Orbital Frame is performed in the
 * YamanakaAnkersenProvider.
 * <p>
 * Source : Koji Yamanaka, Finn Ankersen, New State Transition Matrix for Relative Motion on an Arbitrary Elliptical
 * Orbit, Journal of Guidance, Control, and Dynamics, Vol. 25, No.1, January-February 2002
 * </p>
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */

public class FieldYamanakaAnkersenEquations<T extends CalculusFieldElement<T>> {

    /** Constructor. */
    public FieldYamanakaAnkersenEquations() {
    }

    /**
     * Computes rho as in reference paper.
     *
     * @param theta   true anomaly of the target.
     * @param targetE eccentricity of the target.
     * @return rho.
     */
    private T rho(final T theta, final T targetE) {
        return targetE.multiply(theta.cos()).add(1);
    }

    /**
     * Computes c as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return c
     */
    private T c(final T theta, final T targetE) {
        return rho(theta, targetE).multiply(theta.cos());
    }

    /**
     * Computes s as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return s
     */
    private T s(final T theta, final T targetE) {
        return rho(theta, targetE).multiply(theta.sin());
    }

    /**
     * Computes c_p as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return c_p
     */
    private T cP(final T theta, final T targetE) {
        return theta.sin().add(targetE.multiply(theta.multiply(2).sin())).multiply(-1.);
    }

    /**
     * Computes s_p as in reference paper.
     *
     * @param theta   true anomaly of the target
     * @param targetE eccentricity of the target
     * @return s_p
     */
    private T sP(final T theta, final T targetE) {
        return theta.cos().add(targetE.multiply(theta.multiply(2).cos()));
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
    public FieldYamanakaAnkersenMatrices<T> computeMatrices(final T timeSinceEpoch, final T targetA, final T targetE,
                                                            final T targetInitialTheta, final T targetTheta,
                                                            final T mu) {

        // Intermediate values
        final T e2 = targetE.pow(2);
        final T p = targetA.multiply(targetA.getField().getOne().subtract(e2));
        final T k2 = FastMath.sqrt(mu.divide(p.pow(3)));
        final T J = k2.multiply(timeSinceEpoch);
        final T rho = rho(targetTheta, targetE);
        final T c = c(targetTheta, targetE);
        final T s = s(targetTheta, targetE);
        final T cp = cP(targetTheta, targetE);
        final T sp = sP(targetTheta, targetE);
        final T rho0 = rho(targetInitialTheta, targetE);
        final T c0 = c(targetInitialTheta, targetE);
        final T s0 = s(targetInitialTheta, targetE);
        final T rho_delta = rho(targetTheta.subtract(targetInitialTheta), targetE);
        final T c_delta = c(targetTheta.subtract(targetInitialTheta), targetE);
        final T s_delta = s(targetTheta.subtract(targetInitialTheta), targetE);

        final T zero = targetA.getField().getZero();

        // First operand of in-plane state transition matrix (eq. 76 of Yamanaka-Ankersen paper)
        final T[][] phi_theta = MathArrays.buildArray(targetA.getField(), 4, 4);
        phi_theta[0][0] = zero.add(1);
        phi_theta[0][1] = c.multiply(-1.).add(c.multiply(-1.).divide(rho));
        phi_theta[0][2] = s.add(s.divide(rho));
        phi_theta[0][3] = rho.pow(2).multiply(J).multiply(3);
        phi_theta[1][0] = zero;
        phi_theta[1][1] = s;
        phi_theta[1][2] = c;
        phi_theta[1][3] = targetE.multiply(s).multiply(J).multiply(-3.).add(2);
        phi_theta[2][0] = zero;
        phi_theta[2][1] = s.multiply(2);
        phi_theta[2][2] = c.multiply(2).subtract(targetE);
        phi_theta[2][3] = targetE.multiply(s).multiply(J).multiply(-2.).add(1).multiply(3);
        phi_theta[3][0] = zero;
        phi_theta[3][1] = sp;
        phi_theta[3][2] = cp;
        phi_theta[3][3] = targetE.multiply(-3.).multiply(sp.multiply(J).add(s.divide(rho.pow(2))));

        // Second operand of in-plane state transition matrix (eq. 80 of Yamanaka-Ankersen paper)
        final T[][] phiInv_theta0 = MathArrays.buildArray(targetA.getField(), 4, 4);
        phiInv_theta0[0][0] = e2.multiply(-1.).add(1);
        phiInv_theta0[0][1] = targetE.multiply(s0).multiply(3).divide(rho0)
                                     .add(targetE.multiply(s0).multiply(3).divide(rho0.pow(2)));
        phiInv_theta0[0][2] = targetE.multiply(s0).multiply(-1.).add(targetE.multiply(s0).multiply(-1.).divide(rho0));
        phiInv_theta0[0][3] = targetE.multiply(c0).multiply(-1.).add(2);
        phiInv_theta0[1][0] = zero;
        phiInv_theta0[1][1] = s0.multiply(-3.).divide(rho0).add(s0.multiply(-3.).multiply(targetE.divide(rho0).pow(2)));
        phiInv_theta0[1][2] = s0.add(s0.divide(rho0));
        phiInv_theta0[1][3] = c0.subtract(targetE.multiply(2));
        phiInv_theta0[2][0] = zero;
        phiInv_theta0[2][1] = c0.divide(rho0).add(targetE).multiply(-3.);
        phiInv_theta0[2][2] = c0.add(c0.divide(rho0)).add(targetE);
        phiInv_theta0[2][3] = s0.multiply(-1.);
        phiInv_theta0[3][0] = zero;
        phiInv_theta0[3][1] = rho0.multiply(3).add(e2).add(-1.);
        phiInv_theta0[3][2] = rho0.pow(2).multiply(-1.);
        phiInv_theta0[3][3] = targetE.multiply(s0);

        // In-plane state transition matrix
        final FieldMatrix<T> inPlaneMatrix =
                        MatrixUtils.createFieldMatrix(phi_theta).multiply(MatrixUtils.createFieldMatrix(phiInv_theta0))
                                   .scalarMultiply(zero.add(1).divide(e2.multiply(-1).add(1)));

        // Compute the out-plane transition matrix (eq. 84 of Yamanaka-Ankersen paper)
        final T[][] outPlaneMatrix = MathArrays.buildArray(targetA.getField(), 2, 2);
        outPlaneMatrix[0][0] = c_delta.divide(rho_delta);
        outPlaneMatrix[0][1] = s_delta.divide(rho_delta);
        outPlaneMatrix[1][0] = s_delta.divide(rho_delta).multiply(-1.);
        outPlaneMatrix[1][1] = c_delta.divide(rho_delta);

        return new FieldYamanakaAnkersenMatrices<>(timeSinceEpoch, targetTheta, inPlaneMatrix,
                                                   MatrixUtils.createFieldMatrix(outPlaneMatrix));
    }
}
