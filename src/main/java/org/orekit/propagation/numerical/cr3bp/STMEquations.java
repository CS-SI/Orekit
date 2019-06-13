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
package org.orekit.propagation.numerical.cr3bp;

import java.util.Arrays;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;

/** Class calculating the state transition matrix coefficient.
 * @author Vincent Mouraux
 */
public class STMEquations
    implements
    AdditionalEquations {

    /** Mass ratio of the considered CR3BP System. */
    private final double mu;

    /** Name of the equations. */
    private final String name;

    /** Simple constructor.
     * @param syst CR3BP System considered
     */
    public STMEquations(final CR3BPSystem syst) {
        this.mu = syst.getMu();
        this.name = "stmEquations";
    }

    /** Method adding the standard initial values of the additional state to the initial spacecraft state.
     * @param s Initial state of the system
     * @return s Initial augmented (with the additional equations) state
     */
    public SpacecraftState setInitialPhi(final SpacecraftState s) {
        final int stateDimension = 36;
        final double[] phi = new double[stateDimension];
        for (int i = 0; i < stateDimension; ++i) {
            phi[i] = 0.0;
        }

        for (int i = 0; i < stateDimension; i = i + 7) {
            phi[i] = 1.0;
        }
        return s.addAdditionalState(name, phi);
    }

    /** {@inheritDoc} */
    public double[] computeDerivatives(final SpacecraftState s, final double[] dPhi) {

        final double[][] jacobian = new double[6][6];

        final double[][] u = new double[3][3];
        final double[][] phi2d = new double[6][6];

        final double[] phi = s.getAdditionalState(getName());

        for (int j = 0; j < jacobian.length; ++j) {
            Arrays.fill(jacobian[j], 0.0);
        }

        jacobian[0][3] = 1.0;
        jacobian[1][4] = 1.0;
        jacobian[2][5] = 1.0;
        jacobian[3][4] = 2.0;
        jacobian[4][3] = -2.0;

        final double x = s.getPVCoordinates().getPosition().getX();
        final double y = s.getPVCoordinates().getPosition().getY();
        final double z = s.getPVCoordinates().getPosition().getZ();

        final double r1 = FastMath.sqrt((x + mu) * (x + mu) + y * y + z * z);
        final double r2 = FastMath.sqrt((x - (1 - mu)) * (x - (1 - mu)) + y * y + z * z);

        u[0][0] = 1 - (1 - mu) * (1 / (r1 * r1 * r1) -
            3 * (x + mu) * (x + mu) / (r1 * r1 * r1 * r1 * r1)) -
            mu * (1 / (r2 * r2 * r2) -
            3 * (x - (1 - mu)) * (x - (1 - mu)) / (r2 * r2 * r2 * r2 * r2));

        u[1][1] = 1 - (1 - mu) * (1 / (r1 * r1 * r1) - 3 * y * y / (r1 * r1 * r1 * r1 * r1)) -
            mu * (1 / (r2 * r2 * r2) - 3 * y * y / (r2 * r2 * r2 * r2 * r2));

        u[2][2] = -(1 - mu) * (1 / (r1 * r1 * r1) - 3 * z * z / (r1 * r1 * r1 * r1 * r1)) -
            mu * (1 / (r2 * r2 * r2) - 3 * z * z / (r2 * r2 * r2 * r2 * r2));

        u[0][1] =
            3 * (1 - mu) * y * (x + mu) / (r1 * r1 * r1 * r1 * r1) +
                  3 * mu * y * (x - (1 - mu)) / (r2 * r2 * r2 * r2 * r2);

        u[1][0] = u[0][1];

        u[0][2] =
            3 * (1 - mu) * z * (x + mu) / (r1 * r1 * r1 * r1 * r1) +
                  3 * mu * z * (x - (1 - mu)) / (r2 * r2 * r2 * r2 * r2);

        u[2][0] = u[0][2];

        u[1][2] =
            3 * (1 - mu) * y * z / (r1 * r1 * r1 * r1 * r1) +
                  3 * mu * y * z / (r2 * r2 * r2 * r2 * r2);

        u[2][1] = u[1][2];

        for (int k = 3; k < 6; ++k) {
            jacobian[k][0] = u[k - 3][0];
            jacobian[k][1] = u[k - 3][1];
            jacobian[k][2] = u[k - 3][2];
        }

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                phi2d[i][j] = phi[6 * i + j];
            }
        }

        final RealMatrix phiM = new Array2DRowRealMatrix(phi2d, false);
        final RealMatrix jacobianM = new Array2DRowRealMatrix(jacobian, false);
        final RealMatrix phidM = jacobianM.multiply(phiM);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                dPhi[6 * i + j] = phidM.getEntry(i, j);
            }
        }

     // these equations have no effect on the main state itself
        return null;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** Method returning the STM.
     * @param s SpacecraftState of the system
     * @return phiM State Transition Matrix
     */
    public RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final double[][] phi2dA = new double[6][6];
        final double[] stm = s.getAdditionalState(getName());
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                phi2dA[i][j] = stm[6 * i + j];
            }
        }
        final RealMatrix phiM = new Array2DRowRealMatrix(phi2dA, false);
        return phiM;
    }
}
