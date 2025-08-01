/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;

import java.util.List;

/** Generator for State Transition Matrix.
 * The state is made up of the Cartesian position and velocity vectors.
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @since 11.1
 */
class StateTransitionMatrixGenerator extends AbstractStateTransitionMatrixGenerator {

    /**
     * State dimension.
     */
    public static final int STATE_DIMENSION = 2 * SPACE_DIMENSION;

    /**
     * Simple constructor.
     *
     * @param stmName          name of the Cartesian STM additional state
     * @param forceModels      force models used in propagation
     * @param attitudeProvider attitude provider used in propagation
     */
    StateTransitionMatrixGenerator(final String stmName, final List<ForceModel> forceModels,
                                   final AttitudeProvider attitudeProvider) {
        super(stmName, forceModels, attitudeProvider, STATE_DIMENSION);
    }

    /** {@inheritDoc} */
    @Override
    protected void multiplyMatrix(final double[] factor, final double[] x, final double[] y, final int columns) {
        staticMultiplyMatrix(factor, x, y, columns);
    }

    /** Compute evolution matrix product.
     * <p>
     * This method computes \(Y = F \times X\) where the factor matrix is:
     * \[F = \begin{matrix}
     *               0         &             0         &             0         &             1         &             0         &             0        \\
     *               0         &             0         &             0         &             0         &             1         &             0        \\
     *               0         &             0         &             0         &             0         &             0         &             1        \\
     *  \sum \frac{da_x}{dp_x} & \sum\frac{da_x}{dp_y} & \sum\frac{da_x}{dp_z} & \sum\frac{da_x}{dv_x} & \sum\frac{da_x}{dv_y} & \sum\frac{da_x}{dv_z}\\
     *  \sum \frac{da_y}{dp_x} & \sum\frac{da_y}{dp_y} & \sum\frac{da_y}{dp_z} & \sum\frac{da_y}{dv_x} & \sum\frac{da_y}{dv_y} & \sum\frac{da_y}{dv_z}\\
     *  \sum \frac{da_z}{dp_x} & \sum\frac{da_z}{dp_y} & \sum\frac{da_z}{dp_z} & \sum\frac{da_z}{dv_x} & \sum\frac{da_z}{dv_y} & \sum\frac{da_z}{dv_z}
     * \end{matrix}\]
     * </p>
     * @param factor factor matrix
     * @param x right factor of the multiplication, as a flatten array in row major order
     * @param y placeholder where to put the result, as a flatten array in row major order
     * @param columns number of columns of both x and y (so their dimensions are 6 x columns)
     */
    static void staticMultiplyMatrix(final double[] factor, final double[] x, final double[] y, final int columns) {

        final int n = SPACE_DIMENSION * columns;

        // handle first three rows by a simple copy
        System.arraycopy(x, n, y, 0, n);

        // regular multiplication for the last three rows
        for (int j = 0; j < columns; ++j) {
            y[n + j              ] = factor[ 0] * x[j              ] + factor[ 1] * x[j +     columns] + factor[ 2] * x[j + 2 * columns] +
                    factor[ 3] * x[j + 3 * columns] + factor[ 4] * x[j + 4 * columns] + factor[ 5] * x[j + 5 * columns];
            y[n + j +     columns] = factor[ 6] * x[j              ] + factor[ 7] * x[j +     columns] + factor[ 8] * x[j + 2 * columns] +
                    factor[ 9] * x[j + 3 * columns] + factor[10] * x[j + 4 * columns] + factor[11] * x[j + 5 * columns];
            y[n + j + 2 * columns] = factor[12] * x[j              ] + factor[13] * x[j +     columns] + factor[14] * x[j + 2 * columns] +
                    factor[15] * x[j + 3 * columns] + factor[16] * x[j + 4 * columns] + factor[17] * x[j + 5 * columns];
        }

    }

    /** {@inheritDoc} */
    @Override
    Gradient[] computeRatesPartialsAndUpdateFactor(final ForceModel forceModel,
                                                   final FieldSpacecraftState<Gradient> fieldState,
                                                   final Gradient[] parameters, final double[] factor) {
        final FieldVector3D<Gradient> acceleration = forceModel.acceleration(fieldState, parameters);
        final double[]                       gradX        = acceleration.getX().getGradient();
        final double[]                       gradY        = acceleration.getY().getGradient();
        final double[]                       gradZ        = acceleration.getZ().getGradient();

        // lower left part of the factor matrix
        factor[ 0] += gradX[0];
        factor[ 1] += gradX[1];
        factor[ 2] += gradX[2];
        factor[ 6] += gradY[0];
        factor[ 7] += gradY[1];
        factor[ 8] += gradY[2];
        factor[12] += gradZ[0];
        factor[13] += gradZ[1];
        factor[14] += gradZ[2];

        if (!forceModel.dependsOnPositionOnly()) {
            // lower right part of the factor matrix
            factor[ 3] += gradX[3];
            factor[ 4] += gradX[4];
            factor[ 5] += gradX[5];
            factor[ 9] += gradY[3];
            factor[10] += gradY[4];
            factor[11] += gradY[5];
            factor[15] += gradZ[3];
            factor[16] += gradZ[4];
            factor[17] += gradZ[5];
        }
        return acceleration.toArray();
    }
}

