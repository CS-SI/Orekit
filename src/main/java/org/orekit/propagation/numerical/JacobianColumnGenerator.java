/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.numerical;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.IntegrableGenerator;

/** Generator for one column of a Jacobian matrix.
 * @author Luc Maisonobe
 * @since 11.1
 */
class JacobianColumnGenerator implements IntegrableGenerator {

    /** Space dimension. */
    private static final int SPACE_DIMENSION = 3;

    /** State dimension. */
    private static final int STATE_DIMENSION = 2 * SPACE_DIMENSION;

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Generator for State Transition Matrix. */
    private final StateTransitionMatrixGenerator stmGenerator;

    /** Name of the parameter corresponding to the column. */
    private final String columnName;

    /** Simple constructor.
     * <p>
     * The generator for State Transition Matrix <em>must</em> be registered as
     * an integrable generator to the same propagator as the instance, as it
     * must be scheduled to update the state before the instance
     * </p>
     * @param stmGenerator generator for State Transition Matrix
     * @param columnName name of the parameter corresponding to the column
     */
    JacobianColumnGenerator(final StateTransitionMatrixGenerator stmGenerator, final String columnName) {
        this.stmGenerator = stmGenerator;
        this.columnName   = columnName;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return columnName;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return STATE_DIMENSION;
    }

    /** {@inheritDoc}
     * <p>
     * The column derivative can be computed only if the State Transition Matrix derivatives
     * are available, as it implies the STM generator has already been run.
     * </p>
     */
    @Override
    public boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalStateDerivative(stmGenerator.getName());
    }

    /** Set the initial value of the column.
     * <p>
     * The returned state must be added to the propagator.
     * </p>
     * @param state initial state (must already contain the Cartesian State Transition Matrix)
     * @param dYdQ column of the Jacobian ∂Y/∂qₘ for the {@link #getName()} named parameter},
     * if null (which is the most frequent case), assumed to be 0
     * @param orbitType orbit type used for states Y and Y₀ in {@code dYdQ}
     * @param positionAngle position angle used states Y and Y₀ in {@code dYdQ}
     * @return state with Jacobian column (converted to Cartesian ∂C/∂Q) added
     */
    public SpacecraftState setInitialColumn(final SpacecraftState state, final double[] dYdQ,
                                            final OrbitType orbitType, final PositionAngle positionAngle) {

        final double[] column;
        if (dYdQ == null) {
            // initial Jacobian is null (this is the most frequent case)
            column = new double[STATE_DIMENSION];
        } else {

            if (dYdQ.length != STATE_DIMENSION) {
                throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                          dYdQ.length, STATE_DIMENSION);
            }

            // convert to Cartesian Jacobian
            final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];
            orbitType.convertType(state.getOrbit()).getJacobianWrtCartesian(positionAngle, dYdC);
            column = new QRDecomposition(MatrixUtils.createRealMatrix(dYdC), THRESHOLD).
                     getSolver().
                     solve(MatrixUtils.createRealVector(dYdQ)).
                     toArray();

        }


        // set additional state
        return state.addAdditionalState(columnName, column);

    }

    /** {@inheritDoc} */
    public double[] generate(final SpacecraftState s) {

        // Assuming position is (px, py, pz), velocity is (vx, vy, vz) and the acceleration
        // due to the force models is (Σ ax, Σ ay, Σ az), the differential equation for
        // Jacobian matrix with respect to parameter q is:
        //                  [     0          0          0            1          0          0   ]            [    0   ]
        //                  [     0          0          0            0          1          0   ]            [    0   ]
        //  d(∂C/∂Q)/dt  =  [     0          0          0            1          0          1   ] ⨯ ∂C/∂Q +  [    0   ]
        //                  [Σ dax/dpx  Σ dax/dpy  Σ dax/dpz    Σ dax/dvx  Σ dax/dvy  Σ dax/dvz]            [ dax/dq ]
        //                  [Σ day/dpx  Σ day/dpy  Σ dax/dpz    Σ day/dvx  Σ day/dvy  Σ dax/dvz]            [ day/dq ]
        //                  [Σ daz/dpx  Σ daz/dpy  Σ dax/dpz    Σ daz/dvx  Σ daz/dvy  Σ dax/dvz]            [ daz/dq ]
        // the factor matrix has already been computed by the STM generator before this generator is called

        // retrieve current Jacobian column
        final double[] p    = s.getAdditionalState(getName());
        final double[] pDot = new double[p.length];

        // retrieve partial derivatives of the acceleration with respect to column parameter
        final double[] partials = stmGenerator.getAccelerationPartials(getName());

        // compute time derivative of the Jacobian column
        stmGenerator.multiplyMatrix(p, pDot, 1);
        pDot[3] += partials[0];
        pDot[4] += partials[1];
        pDot[5] += partials[2];

        return pDot;

    }

}

