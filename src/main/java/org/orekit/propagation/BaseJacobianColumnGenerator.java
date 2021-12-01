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
package org.orekit.propagation;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;

/** Base generator for one column of a Jacobian matrix.
 * @author Luc Maisonobe
 * @since 11.1
 */
public class BaseJacobianColumnGenerator {

    /** Space dimension. */
    private static final int SPACE_DIMENSION = 3;

    /** State dimension. */
    private static final int STATE_DIMENSION = 2 * SPACE_DIMENSION;

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Name of the parameter corresponding to the column. */
    private final String columnName;

    /** Simple constructor.
     * @param columnName name of the parameter corresponding to the column
     */
    protected BaseJacobianColumnGenerator(final String columnName) {
        this.columnName = columnName;
    }

    /** Get the name of the additional state.
     * @return name of the additional state (names containing "orekit"
     * with any case are reserved for the library internal use)
     */
    public String getName() {
        return columnName;
    }

    /** Get the dimension of the generated column.
     * @return dimension of the generated column
     */
    public int getDimension() {
        return STATE_DIMENSION;
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

}

