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
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;

import java.util.List;

/** Generator for State Transition Matrix with the mass included on top of the Cartesian variables.
 * @author Romain Serra
 * @since 13.1
 */
class ExtendedStateTransitionMatrixGenerator extends AbstractStateTransitionMatrixGenerator {

    /** Positional state dimension. */
    private static final int EXTENDED_STATE_DIMENSION = SPACE_DIMENSION * 2 + 1;

    /** Simple constructor.
     * @param stmName name of the Cartesian STM additional state
     * @param forceModels force models used in propagation
     * @param attitudeProvider attitude provider used in propagation
     */
    ExtendedStateTransitionMatrixGenerator(final String stmName, final List<ForceModel> forceModels,
                                           final AttitudeProvider attitudeProvider) {
        super(stmName, forceModels, attitudeProvider, EXTENDED_STATE_DIMENSION);
    }

    /** {@inheritDoc} */
    @Override
    void multiplyMatrix(final double[] factor, final double[] x, final double[] y, final int columns) {
        staticMultiplyMatrix(factor, x, y, columns);
    }

    /** Compute evolution matrix product.
     * <p>
     * This method computes \(Y = F \times X\) where the factor matrix is:
     * \[F = \begin{matrix}
     *               0         &             0         &             0         &             1         &             0         &             0        &          0           \\
     *               0         &             0         &             0         &             0         &             1         &             0        &          0           \\
     *               0         &             0         &             0         &             0         &             0         &             1        &          0           \\
     *  \sum \frac{da_x}{dp_x} & \sum\frac{da_x}{dp_y} & \sum\frac{da_x}{dp_z} & \sum\frac{da_x}{dv_x} & \sum\frac{da_x}{dv_y} & \sum\frac{da_x}{dv_z} & \sum\frac{da_x}{dm}\\
     *  \sum \frac{da_y}{dp_x} & \sum\frac{da_y}{dp_y} & \sum\frac{da_y}{dp_z} & \sum\frac{da_y}{dv_x} & \sum\frac{da_y}{dv_y} & \sum\frac{da_y}{dv_z} & \sum\frac{da_y}{dm}\\
     *  \sum \frac{da_z}{dp_x} & \sum\frac{da_z}{dp_y} & \sum\frac{da_z}{dp_z} & \sum\frac{da_z}{dv_x} & \sum\frac{da_z}{dv_y} & \sum\frac{da_z}{dv_z} & \sum\frac{da_z}{dm}\\
     *  \sum \frac{dmr}{dp_x} & \sum\frac{dmr}{dp_y} & \sum\frac{dmr}{dp_z} & \sum\frac{dmr}{dv_x} & \sum\frac{dmr}{dv_y} & \sum\frac{dmr}{dv_z}
     * \end{matrix}\]
     * </p>
     * @param factor factor matrix
     * @param x right factor of the multiplication, as a flatten array in row major order
     * @param y placeholder where to put the result, as a flatten array in row major order
     * @param columns number of columns of both x and y (so their dimensions are 7 x columns)
     */
    static void staticMultiplyMatrix(final double[] factor, final double[] x, final double[] y, final int columns) {

        final int n = SPACE_DIMENSION * columns;

        // handle first three rows by a simple copy
        System.arraycopy(x, n, y, 0, n);

        // regular multiplication for the last rows
        for (int j = 0; j < columns; ++j) {
            y[n + j              ] = factor[ 0] * x[j              ] + factor[ 1] * x[j +     columns] + factor[ 2] * x[j + 2 * columns] +
                                     factor[ 3] * x[j + 3 * columns] + factor[ 4] * x[j + 4 * columns] + factor[ 5] * x[j + 5 * columns] +
                                     factor[ 6] * x[j + 6 * columns];
            y[n + j +     columns] = factor[ 7] * x[j              ] + factor[ 8] * x[j +     columns] + factor[ 9] * x[j + 2 * columns] +
                                     factor[10] * x[j + 3 * columns] + factor[11] * x[j + 4 * columns] + factor[12] * x[j + 5 * columns] +
                                     factor[13] * x[j + 6 * columns];
            y[n + j + 2 * columns] = factor[14] * x[j              ] + factor[15] * x[j +     columns] + factor[16] * x[j + 2 * columns] +
                                     factor[17] * x[j + 3 * columns] + factor[18] * x[j + 4 * columns] + factor[19] * x[j + 5 * columns] +
                                     factor[20] * x[j + 6 * columns];
            y[n + j + 3 * columns] = factor[21] * x[j              ] + factor[22] * x[j +     columns] + factor[23] * x[j + 2 * columns] +
                                     factor[24] * x[j + 3 * columns] + factor[25] * x[j + 4 * columns] + factor[26] * x[j + 5 * columns] +
                                     factor[27] * x[j + 6 * columns];
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
        factor[ 7] += gradY[0];
        factor[ 8] += gradY[1];
        factor[ 9] += gradY[2];
        factor[14] += gradZ[0];
        factor[15] += gradZ[1];
        factor[16] += gradZ[2];

        if (!forceModel.dependsOnPositionOnly()) {
            // lower right part of the factor matrix
            factor[ 3] += gradX[3];
            factor[ 4] += gradX[4];
            factor[ 5] += gradX[5];
            factor[ 6] += gradX[6];
            factor[10] += gradY[3];
            factor[11] += gradY[4];
            factor[12] += gradY[5];
            factor[13] += gradY[6];
            factor[17] += gradZ[3];
            factor[18] += gradZ[4];
            factor[19] += gradZ[5];
            factor[20] += gradZ[6];
        }

        // deal with mass w.r.t. state
        final Gradient massRate = forceModel.getMassDerivative(fieldState, parameters);
        if (massRate.getValue() != 0.) {
            final double[] massRateDerivatives = massRate.getGradient();
            for (int i = 0; i < EXTENDED_STATE_DIMENSION; i++) {
                factor[21 + i] += massRateDerivatives[i];
            }
        }


        // stack result
        final Gradient[] partials = MathArrays.buildArray(massRate.getField(), 4);
        partials[0] = acceleration.getX();
        partials[1] = acceleration.getY();
        partials[2] = acceleration.getZ();
        partials[3] = massRate;
        return partials;
    }
}

