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
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.List;
import java.util.Map;

/** Generator for State Transition Matrix with the mass included in the variables.
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

    /** Set the initial value of the State Transition Matrix.
     * <p>
     * The returned state must be added to the propagator.
     * </p>
     * @param state initial state
     * @param dYdY0 initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param orbitType orbit type used for states Y and Y₀ in {@code dYdY0}
     * @param positionAngleType position angle used states Y and Y₀ in {@code dYdY0}
     * @return state with initial STM (converted to Cartesian ∂C/∂Y₀) added
     */
    SpacecraftState setInitialStateTransitionMatrix(final SpacecraftState state, final RealMatrix dYdY0,
                                                    final OrbitType orbitType,
                                                    final PositionAngleType positionAngleType) {

        final RealMatrix nonNullDYdY0;
        if (dYdY0 == null) {
            nonNullDYdY0 = MatrixUtils.createRealIdentityMatrix(getStateDimension());
        } else {
            if (dYdY0.getRowDimension() != EXTENDED_STATE_DIMENSION ||
                            dYdY0.getColumnDimension() != EXTENDED_STATE_DIMENSION) {
                throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                          dYdY0.getRowDimension(), dYdY0.getColumnDimension(),
                        EXTENDED_STATE_DIMENSION, EXTENDED_STATE_DIMENSION);
            }
            nonNullDYdY0 = dYdY0;
        }

        // convert to Cartesian STM
        final RealMatrix dCdY0;
        if (state.isOrbitDefined()) {
            final RealMatrix dYdC = MatrixUtils.createRealIdentityMatrix(EXTENDED_STATE_DIMENSION);
            final Orbit orbit = orbitType.convertType(state.getOrbit());
            final double[][] jacobian = new double[EXTENDED_STATE_DIMENSION - 1][EXTENDED_STATE_DIMENSION - 1];
            orbit.getJacobianWrtCartesian(positionAngleType, jacobian);
            dYdC.setSubMatrix(jacobian, 0, 0);
            final DecompositionSolver decomposition = getDecompositionSolver(dYdC);
            dCdY0 = decomposition.solve(nonNullDYdY0);
        } else {
            dCdY0 = nonNullDYdY0;
        }

        // set additional state
        return state.addAdditionalData(getName(), flatten(dCdY0));

    }

    @Override
    void multiplyMatrix(final double[] factor, final double[] x, final double[] y, final int columns) {
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

    /** Compute the various partial derivatives.
     * @param state current spacecraft state
     * @return factor matrix
     */
    protected double[] computePartials(final SpacecraftState state) {

        // set up containers for partial derivatives
        final double[]              factor               = new double[4 * EXTENDED_STATE_DIMENSION];
        final DoubleArrayDictionary partials = new DoubleArrayDictionary();

        // evaluate contribution of all force models
        final AttitudeProvider equivalentAttitudeProvider = wrapAttitudeProviderIfPossible();
        final boolean isThereAnyForceNotDependingOnlyOnPosition = getForceModels().stream().anyMatch(force -> !force.dependsOnPositionOnly());
        final NumericalGradientConverter posOnlyConverter = new NumericalGradientConverter(state, SPACE_DIMENSION, equivalentAttitudeProvider);
        final NumericalGradientConverter fullConverter = isThereAnyForceNotDependingOnlyOnPosition ?
            new NumericalGradientConverter(state, EXTENDED_STATE_DIMENSION, equivalentAttitudeProvider) : posOnlyConverter;

        for (final ForceModel forceModel : getForceModels()) {

            final NumericalGradientConverter     converter    = forceModel.dependsOnPositionOnly() ? posOnlyConverter : fullConverter;
            final FieldSpacecraftState<Gradient> dsState      = converter.getState(forceModel);
            final Gradient[]                     parameters   = converter.getParametersAtStateDate(dsState, forceModel);
            final FieldVector3D<Gradient>        acceleration = forceModel.acceleration(dsState, parameters);
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
            final long activeDrivers = forceModel.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count();
            Gradient massRate = Gradient.constant(EXTENDED_STATE_DIMENSION + (int) activeDrivers, 0.);
            double[] massRateDerivatives = massRate.getGradient();
            if (forceModel instanceof Maneuver) {
                final Maneuver maneuver = (Maneuver) forceModel;
                massRate = massRate.add(maneuver.getPropulsionModel().getMassDerivatives(dsState, parameters));
                massRateDerivatives = massRate.getGradient();
                for (int i = 0; i < EXTENDED_STATE_DIMENSION; i++) {
                    factor[21 + i] += massRateDerivatives[i];
                }
            }

            // partials derivatives with respect to parameters
            int paramsIndex = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {

                    // for each span (for each estimated value) corresponding name is added
                    for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                        // get the partials derivatives for this driver
                        DoubleArrayDictionary.Entry entry = partials.getEntry(span.getData());
                        if (entry == null) {
                            // create an entry filled with zeroes
                            partials.put(span.getData(), new double[4]);
                            entry = partials.getEntry(span.getData());
                        }

                        // add the contribution of the current force model
                        entry.increment(new double[] {
                            gradX[paramsIndex], gradY[paramsIndex], gradZ[paramsIndex], massRateDerivatives[paramsIndex]
                        });
                        ++paramsIndex;
                    }
                }
            }

            // notify observers
            for (Map.Entry<String, PartialsObserver> observersEntry : getPartialsObservers().entrySet()) {
                final DoubleArrayDictionary.Entry entry = partials.getEntry(observersEntry.getKey());
                observersEntry.getValue().partialsComputed(state, factor, entry == null ? new double[4] : entry.getValue());
            }

        }

        return factor;

    }

}

