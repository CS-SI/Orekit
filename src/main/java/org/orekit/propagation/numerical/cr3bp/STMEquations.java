/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.numerical.cr3bp;

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;

/** Class calculating the state transition matrix coefficient for CR3BP Computation.
 * @see "Dynamical systems, the three-body problem, and space mission design, Koon, Lo, Marsden, Ross"
 * @author Vincent Mouraux
 * @since 10.2
 */
public class STMEquations
    implements AdditionalDerivativesProvider {

    /** Matrix Dimension. */
    private static final int DIM = 6;

    /** Mass ratio of the considered CR3BP System. */
    private final CR3BPSystem syst;

    /** Name of the equations. */
    private final String name;

    /** Potential Hessian Matrix. */
    private final double[][] jacobian = new double[DIM][DIM];

    /** Simple constructor.
     * @param syst CR3BP System considered
     */
    public STMEquations(final CR3BPSystem syst) {
        this.syst = syst;
        this.name = "stmEquations";

        // Jacobian constant values initialization
        for (int j = 0; j < jacobian.length; ++j) {
            Arrays.fill(jacobian[j], 0.0);
        }

        jacobian[0][3] = 1.0;
        jacobian[1][4] = 1.0;
        jacobian[2][5] = 1.0;
        jacobian[3][4] = 2.0;
        jacobian[4][3] = -2.0;
    }

    /** Method adding the standard initial values of the additional state to the initial spacecraft state.
     * @param s Initial state of the system
     * @return s Initial augmented (with the additional equations) state
     */
    public SpacecraftState setInitialPhi(final SpacecraftState s) {
        final int stateDimension = 36;
        final double[] phi = new double[stateDimension];
        for (int i = 0; i < stateDimension; i = i + 7) {
            phi[i] = 1.0;
        }
        return s.addAdditionalState(name, phi);
    }

    /** {@inheritDoc} */
    public CombinedDerivatives combinedDerivatives(final SpacecraftState s) {

        // State Transition Matrix
        final double[] phi = s.getAdditionalState(getName());
        final double[] dPhi = new double[phi.length];

        // Spacecraft Potential
        final DerivativeStructure potential = new CR3BPForceModel(syst).getPotential(s);

        // Potential derivatives
        final double[] dU = potential.getAllDerivatives();

        // second order derivatives index
        final int idXX = potential.getFactory().getCompiler().getPartialDerivativeIndex(2, 0, 0);
        final int idXY = potential.getFactory().getCompiler().getPartialDerivativeIndex(1, 1, 0);
        final int idXZ = potential.getFactory().getCompiler().getPartialDerivativeIndex(1, 0, 1);
        final int idYY = potential.getFactory().getCompiler().getPartialDerivativeIndex(0, 2, 0);
        final int idYZ = potential.getFactory().getCompiler().getPartialDerivativeIndex(0, 1, 1);
        final int idZZ = potential.getFactory().getCompiler().getPartialDerivativeIndex(0, 0, 2);

        // New Jacobian values
        jacobian[3][0] = dU[idXX];
        jacobian[4][1] = dU[idYY];
        jacobian[5][2] = dU[idZZ];
        jacobian[3][1] = dU[idXY];
        jacobian[4][0] = jacobian[3][1];
        jacobian[3][2] = dU[idXZ];
        jacobian[5][0] = jacobian[3][2];
        jacobian[4][2] = dU[idYZ];
        jacobian[5][1] = jacobian[4][2];

        // STM derivatives computation : dPhi = Jacobian * Phi if both dPhi and Phi are defined as Matrix
        for (int k = 0; k < DIM; k++) {
            for (int l = 0; l < DIM; l++) {
                for (int i = 0; i < DIM; i++) {
                    dPhi[DIM * k + l] =
                        dPhi[DIM * k + l] + jacobian[k][i] * phi[DIM * i + l];
                }
            }
        }

        return new CombinedDerivatives(dPhi, null);

    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return DIM * DIM;
    }

    /** Method returning the State Transition Matrix.
     * @param s SpacecraftState of the system
     * @return phiM State Transition Matrix
     */
    public RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final double[][] phi2dA = new double[DIM][DIM];
        final double[] stm = s.getAdditionalState(getName());
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < 6; j++) {
                phi2dA[i][j] = stm[DIM * i + j];
            }
        }
        return new Array2DRowRealMatrix(phi2dA, false);
    }
}
