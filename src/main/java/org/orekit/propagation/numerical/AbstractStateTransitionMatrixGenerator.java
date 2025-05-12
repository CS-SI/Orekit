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

import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudeProviderModifier;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Abstract generator for numerical State Transition Matrix.
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Romain Serra
 * @since 13.1
 */
abstract class AbstractStateTransitionMatrixGenerator implements AdditionalDerivativesProvider {

    /** Space dimension. */
    protected static final int SPACE_DIMENSION = 3;

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Name of the Cartesian STM additional state. */
    private final String stmName;

    /** Force models used in propagation. */
    private final List<ForceModel> forceModels;

    /** Attitude provider used in propagation. */
    private final AttitudeProvider attitudeProvider;

    /** Observers for partial derivatives. */
    private final Map<String, PartialsObserver> partialsObservers;

    /** Number of state variables. */
    private final int stateDimension;

    /** Dimension of flatten STM. */
    private final int dimension;

    /** Simple constructor.
     * @param stmName name of the Cartesian STM additional state
     * @param forceModels force models used in propagation
     * @param attitudeProvider attitude provider used in propagation
     * @param stateDimension dimension of state vector
     */
    AbstractStateTransitionMatrixGenerator(final String stmName, final List<ForceModel> forceModels,
                                           final AttitudeProvider attitudeProvider, final int stateDimension) {
        this.stmName           = stmName;
        this.forceModels       = forceModels;
        this.attitudeProvider  = attitudeProvider;
        this.stateDimension    = stateDimension;
        this.dimension         = stateDimension * stateDimension;
        this.partialsObservers = new HashMap<>();
    }

    /** Register an observer for partial derivatives.
     * <p>
     * The observer {@link PartialsObserver#partialsComputed(SpacecraftState, double[], double[])} partialsComputed}
     * method will be called when partial derivatives are computed, as a side effect of
     * calling {@link #computePartials(SpacecraftState)} (SpacecraftState)}
     * </p>
     * @param name name of the parameter driver this observer is interested in (may be null)
     * @param observer observer to register
     */
    void addObserver(final String name, final PartialsObserver observer) {
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
        return dimension;
    }

    /**
     * Getter for the number of state variables.
     * @return state vector dimension
     */
    public int getStateDimension() {
        return stateDimension;
    }

    /**
     * Protected getter for the force models.
     * @return forces
     */
    protected List<ForceModel> getForceModels() {
        return forceModels;
    }

    /**
     * Protected getter for the partials observers map.
     * @return map
     */
    protected Map<String, PartialsObserver> getPartialsObservers() {
        return partialsObservers;
    }

    /**
     * Method to build a linear system solver.
     * @param matrix equations matrix
     * @return solver
     */
    DecompositionSolver getDecompositionSolver(final RealMatrix matrix) {
        return new QRDecomposition(matrix, THRESHOLD).getSolver();
    }

    /**
     * Flattens a matrix into an 1-D array.
     * @param matrix matrix to be flatten
     * @return array
     */
    double[] flatten(final RealMatrix matrix) {
        final double[] flat = new double[getDimension()];
        int k = 0;
        for (int i = 0; i < getStateDimension(); ++i) {
            for (int j = 0; j < getStateDimension(); ++j) {
                flat[k++] = matrix.getEntry(i, j);
            }
        }
        return flat;
    }

    /** {@inheritDoc} */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !state.hasAdditionalData(getName());
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
    abstract SpacecraftState setInitialStateTransitionMatrix(SpacecraftState state, RealMatrix dYdY0,
                                                             OrbitType orbitType, PositionAngleType positionAngleType);

    /** {@inheritDoc} */
    public CombinedDerivatives combinedDerivatives(final SpacecraftState state) {
        final double[] factor = computePartials(state);

        // retrieve current State Transition Matrix
        final double[] p    = state.getAdditionalState(getName());
        final double[] pDot = new double[p.length];

        // perform multiplication
        multiplyMatrix(factor, p, pDot, getStateDimension());

        return new CombinedDerivatives(pDot, null);

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
    abstract void multiplyMatrix(double[] factor, double[] x, double[] y, int columns);

    /** Compute the various partial derivatives.
     * @param state current spacecraft state
     * @return factor matrix
     */
    abstract double[] computePartials(SpacecraftState state);

    /**
     * Method that first checks if it is possible to replace the attitude provider with a computationally cheaper one
     * to evaluate. If applicable, the new provider only computes the rotation and uses dummy rate and acceleration,
     * since they should not be used later on.
     * @return same provider if at least one forces used attitude derivatives, otherwise one wrapping the old one for
     * the rotation
     */
    AttitudeProvider wrapAttitudeProviderIfPossible() {
        if (forceModels.stream().anyMatch(ForceModel::dependsOnAttitudeRate)) {
            // at least one force uses an attitude rate, need to keep the original provider
            return attitudeProvider;
        } else {
            // the original provider can be replaced by a lighter one for performance
            return AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        }
    }

    /** Interface for observing partials derivatives. */
    @FunctionalInterface
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
         * @param state current spacecraft state
         * @param factor factor matrix, flattened along rows
         * @param partials partials derivatives of acceleration and mass rate with respect to the parameter driver
         * that was registered (zero if no parameters were not selected or parameter is unknown)
         */
        void partialsComputed(SpacecraftState state, double[] factor, double[] partials);

    }

}

