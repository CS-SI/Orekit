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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;

/** Generator for State Transition Matrix.
 * @author Luc Maisonobe
 * @since 11.1
 */
class StateTransitionMatrixGenerator implements AdditionalEquations {

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Space dimension. */
    private static final int SPACE_DIMENSION = 3;

    /** State dimension. */
    public static final int STATE_DIMENSION = 2 * SPACE_DIMENSION;

    /** Name of the Cartesian STM additional state. */
    private final String stmName;

    /** Force models used in propagation. */
    private final List<ForceModel> forceModels;

    /** Attitude provider used in propagation. */
    private final AttitudeProvider attitudeProvider;

    /** Observers for partial derivatives. */
    private final Map<String, PartialsObserver> partialsObservers;

    /** Simple constructor.
     * @param stmName name of the Cartesian STM additional state
     * @param forceModels force models used in propagation
     * @param attitudeProvider attitude provider used in propagation
     */
    StateTransitionMatrixGenerator(final String stmName, final List<ForceModel> forceModels,
                                   final AttitudeProvider attitudeProvider) {
        this.stmName           = stmName;
        this.forceModels       = forceModels;
        this.attitudeProvider  = attitudeProvider;
        this.partialsObservers = new HashMap<>();
    }

    /** Register an observer for partial derivatives.
     * <p>
     * The observer {@link PartialsObserver#partialsComputed(double[], double[]) partialsComputed}
     * method will be called when partial derivatives are computed, as a side effect of
     * calling {@link #generate(SpacecraftState)}
     * </p>
     * @param name name of the parameter driver this observer is interested in (may be null)
     * @param observer observer to register
     */
    public void addObserver(final String name, final PartialsObserver observer) {
        partialsObservers.put(name, observer);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return stmName;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return STATE_DIMENSION * STATE_DIMENSION;
    }

    /** {@inheritDoc} */
    @Override
    public boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalState(getName());
    }

    /** Set the initial value of the State Transition Matrix.
     * <p>
     * The returned state must be added to the propagator.
     * </p>
     * @param state initial state
     * @param dYdY0 initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param orbitType orbit type used for states Y and Y₀ in {@code dYdY0}
     * @param positionAngle position angle used states Y and Y₀ in {@code dYdY0}
     * @return state with initial STM (converted to Cartesian ∂C/∂Y₀) added
     */
    public SpacecraftState setInitialStateTransitionMatrix(final SpacecraftState state,
                                                           final RealMatrix dYdY0,
                                                           final OrbitType orbitType,
                                                           final PositionAngle positionAngle) {

        if (dYdY0 != null) {
            if (dYdY0.getRowDimension() != STATE_DIMENSION ||
                            dYdY0.getColumnDimension() != STATE_DIMENSION) {
                throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                          dYdY0.getRowDimension(), dYdY0.getColumnDimension(),
                                          STATE_DIMENSION, STATE_DIMENSION);
            }
        }

        // convert to Cartesian STM
        final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];
        orbitType.convertType(state.getOrbit()).getJacobianWrtCartesian(positionAngle, dYdC);
        final RealMatrix dCdY0 = new QRDecomposition(MatrixUtils.createRealMatrix(dYdC), THRESHOLD).
                                 getSolver().
                                 solve(dYdY0 == null ?
                                       MatrixUtils.createRealIdentityMatrix(STATE_DIMENSION) :
                                       dYdY0);

        // flatten matrix
        final double[] flat = new double[STATE_DIMENSION * STATE_DIMENSION];
        int k = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                flat[k++] = dCdY0.getEntry(i, j);
            }
        }

        // set additional state
        return state.addAdditionalState(stmName, flat);

    }

    /** {@inheritDoc} */
    public double[] derivatives(final SpacecraftState state) {

        // Assuming position is (px, py, pz), velocity is (vx, vy, vz) and the acceleration
        // due to the force models is (Σ ax, Σ ay, Σ az), the differential equation for
        // Cartesian State Transition Matrix ∂C/∂Y₀ for the contribution of all force models is:
        //                   [     0          0          0            1          0          0   ]
        //                   [     0          0          0            0          1          0   ]
        //  d(∂C/∂Y₀)/dt  =  [     0          0          0            1          0          1   ] ⨯ ∂C/∂Y₀
        //                   [Σ dax/dpx  Σ dax/dpy  Σ dax/dpz    Σ dax/dvx  Σ dax/dvy  Σ dax/dvz]
        //                   [Σ day/dpx  Σ day/dpy  Σ dax/dpz    Σ day/dvx  Σ day/dvy  Σ dax/dvz]
        //                   [Σ daz/dpx  Σ daz/dpy  Σ dax/dpz    Σ daz/dvx  Σ daz/dvy  Σ dax/dvz]
        // some force models depend on velocity (either directly or through attitude),
        // whereas some other force models depend only on position.
        // For the latter, the lower right part of the matrix is zero
        final double[] factor = computePartials(state);

        // retrieve current State Transition Matrix
        final double[] p    = state.getAdditionalState(getName());
        final double[] pDot = new double[p.length];

        // perform multiplication
        multiplyMatrix(factor, p, pDot, STATE_DIMENSION);

        return pDot;

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
     * <p>
     * The factor matrix used corresponds to the last call to {@link #generate(SpacecraftState)}
     * </p>
     * @param factor factor matrix
     * @param x right factor of the multiplication, as a flatten array in row major order
     * @param y placeholder where to put the result, as a flatten array in row major order
     * @param columns number of columns of both x and y (so their dimensions are 6 x columns)
     */
    public static void multiplyMatrix(final double[] factor, final double[] x, final double[] y, final int columns) {

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

    /** Compute the various partial derivatives.
     * @param state current spacecraft state
     * @return factor matrix
     */
    private double[] computePartials(final SpacecraftState state) {

        // set up containers for partial derivatives
        final double[]              factor               = new double[SPACE_DIMENSION * STATE_DIMENSION];
        final DoubleArrayDictionary accelerationPartials = new DoubleArrayDictionary();

        // evaluate contribution of all force models
        final NumericalGradientConverter fullConverter    = new NumericalGradientConverter(state, 6, attitudeProvider);
        final NumericalGradientConverter posOnlyConverter = new NumericalGradientConverter(state, 3, attitudeProvider);
        for (final ForceModel forceModel : forceModels) {

            final NumericalGradientConverter     converter    = forceModel.dependsOnPositionOnly() ? posOnlyConverter : fullConverter;
            final FieldSpacecraftState<Gradient> dsState      = converter.getState(forceModel);
            final Gradient[]                     parameters   = converter.getParameters(dsState, forceModel);
            final FieldVector3D<Gradient>        acceleration = forceModel.acceleration(dsState, parameters);
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

            int paramsIndex;
            if (forceModel.dependsOnPositionOnly()) {
                paramsIndex = 3;
            } else {
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
                paramsIndex = 6;
            }

            // partials derivatives with respect to parameters
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {

                    // get the partials derivatives for this driver
                    DoubleArrayDictionary.Entry entry = accelerationPartials.getEntry(driver.getName());
                    if (entry == null) {
                        // create an entry filled with zeroes
                        accelerationPartials.put(driver.getName(), new double[3]);
                        entry = accelerationPartials.getEntry(driver.getName());
                    }

                    // add the contribution of the current force model
                    entry.increment(new double[] {
                        gradX[paramsIndex], gradY[paramsIndex], gradZ[paramsIndex]
                    });
                    ++paramsIndex;

                }
            }

            // notify observers
            for (Map.Entry<String, PartialsObserver> observersEntry : partialsObservers.entrySet()) {
                final DoubleArrayDictionary.Entry entry = accelerationPartials.getEntry(observersEntry.getKey());
                observersEntry.getValue().partialsComputed(state, factor, entry == null ? new double[3] : entry.getValue());
            }

        }

        return factor;

    }

    /** Interface for observing partials derivatives. */
    public interface PartialsObserver {

        /** Callback called when partial derivatives have been computed.
         * <p>
         * The factor matrix is:
         * \[F = \begin{matrix}
         *               0         &             0         &             0         &             1         &             0         &             0        \\
         *               0         &             0         &             0         &             0         &             1         &             0        \\
         *               0         &             0         &             0         &             0         &             0         &             1        \\
         *  \sum \frac{da_x}{dp_x} & \sum\frac{da_x}{dp_y} & \sum\frac{da_x}{dp_z} & \sum\frac{da_x}{dv_x} & \sum\frac{da_x}{dv_y} & \sum\frac{da_x}{dv_z}\\
         *  \sum \frac{da_y}{dp_x} & \sum\frac{da_y}{dp_y} & \sum\frac{da_y}{dp_z} & \sum\frac{da_y}{dv_x} & \sum\frac{da_y}{dv_y} & \sum\frac{da_y}{dv_z}\\
         *  \sum \frac{da_z}{dp_x} & \sum\frac{da_z}{dp_y} & \sum\frac{da_z}{dp_z} & \sum\frac{da_z}{dv_x} & \sum\frac{da_z}{dv_y} & \sum\frac{da_z}{dv_z}
         * \end{matrix}\]
         * </p>
         * The factor matrix used corresponds to the last call to {@link #generate(SpacecraftState)}
         * @param state current spacecrzft state
         * @param factor factor matrix, flattened along rows
         * @param accelerationPartials partials derivatives of acceleration with respect to the parameter driver
         * that was registered (zero if no parameters were not selected or parameter is unknown)
         */
        void partialsComputed(SpacecraftState state, double[] factor, double[] accelerationPartials);

    }

}

