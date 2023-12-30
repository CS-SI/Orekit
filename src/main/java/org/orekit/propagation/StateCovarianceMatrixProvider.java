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
package org.orekit.propagation;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;

/**
 * Additional state provider for state covariance matrix.
 * <p>
 * This additional state provider allows computing a propagated covariance matrix based on a user defined input state
 * covariance matrix. The computation of the propagated covariance matrix uses the State Transition Matrix between the
 * propagated spacecraft state and the initial state. As a result, the user must define the name
 * {@link #stmName of the provider for the State Transition Matrix}.
 * <p>
 * As the State Transition Matrix and the input state covariance matrix can be expressed in different orbit types, the
 * user must specify both orbit types when building the covariance provider. In addition, the position angle used in
 * both matrices must also be specified.
 * <p>
 * In order to add this additional state provider to an orbit propagator, user must use the
 * {@link Propagator#addAdditionalStateProvider(AdditionalStateProvider)} method.
 * <p>
 * For a given propagated spacecraft {@code state}, the propagated state covariance matrix is accessible through the
 * method {@link #getStateCovariance(SpacecraftState)}
 *
 * @author Bryan Cazabonne
 * @author Vincent Cucchietti
 * @since 11.3
 */
public class StateCovarianceMatrixProvider implements AdditionalStateProvider {

    /** Dimension of the state. */
    private static final int STATE_DIMENSION = 6;

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Matrix harvester to access the State Transition Matrix. */
    private final MatricesHarvester harvester;

    /** Name of the additional state. */
    private final String additionalName;

    /** Orbit type used for the State Transition Matrix. */
    private final OrbitType stmOrbitType;

    /** Position angle used for State Transition Matrix. */
    private final PositionAngleType stmAngleType;

    /** Orbit type for the covariance matrix. */
    private final OrbitType covOrbitType;

    /** Position angle used for the covariance matrix. */
    private final PositionAngleType covAngleType;

    /** Initial state covariance. */
    private StateCovariance covInit;

    /** Initial state covariance matrix. */
    private RealMatrix covMatrixInit;

    /**
     * Constructor.
     *
     * @param additionalName name of the additional state
     * @param stmName name of the state for State Transition Matrix
     * @param harvester matrix harvester as returned by
     * {@code propagator.setupMatricesComputation(stmName, null, null)}
     * @param covInit initial state covariance
     */
    public StateCovarianceMatrixProvider(final String additionalName, final String stmName,
                                         final MatricesHarvester harvester, final StateCovariance covInit) {
        // Initialize fields
        this.additionalName = additionalName;
        this.stmName = stmName;
        this.harvester = harvester;
        this.covInit = covInit;
        this.covOrbitType = covInit.getOrbitType();
        this.covAngleType = covInit.getPositionAngleType();
        this.stmOrbitType = harvester.getOrbitType();
        this.stmAngleType = harvester.getPositionAngleType();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return additionalName;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        // Convert the initial state covariance in the same orbit type as the STM
        covInit = covInit.changeCovarianceType(initialState.getOrbit(), stmOrbitType, stmAngleType);

        // Express covariance matrix in the same frame as the STM
        final Orbit           initialOrbit      = initialState.getOrbit();
        final StateCovariance covInitInSTMFrame = covInit.changeCovarianceFrame(initialOrbit, initialOrbit.getFrame());

        covMatrixInit = covInitInSTMFrame.getMatrix();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The covariance matrix can be computed only if the State Transition Matrix state is available.
     * </p>
     */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !state.hasAdditionalState(stmName);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {

        // State transition matrix for the input state
        final RealMatrix dYdY0 = harvester.getStateTransitionMatrix(state);

        // Compute the propagated covariance matrix
        RealMatrix propCov = dYdY0.multiply(covMatrixInit.multiplyTransposed(dYdY0));
        final StateCovariance propagated = new StateCovariance(propCov, state.getDate(), state.getFrame(), stmOrbitType, stmAngleType);

        // Update to the user defined type
        propCov = propagated.changeCovarianceType(state.getOrbit(), covOrbitType, covAngleType).getMatrix();

        // Return the propagated covariance matrix
        return toArray(propCov);

    }

    /**
     * Get the orbit type in which the covariance matrix is expressed.
     *
     * @return the orbit type
     */
    public OrbitType getCovarianceOrbitType() {
        return covOrbitType;
    }

    /**
     * Get the state covariance in the same frame/local orbital frame, orbit type and position angle as the initial
     * covariance.
     *
     * @param state spacecraft state to which the covariance matrix should correspond
     * @return the state covariance
     * @see #getStateCovariance(SpacecraftState, Frame)
     * @see #getStateCovariance(SpacecraftState, OrbitType, PositionAngleType)
     */
    public StateCovariance getStateCovariance(final SpacecraftState state) {

        // Get the current propagated covariance
        final RealMatrix covarianceMatrix = toRealMatrix(getAdditionalState(state));

        // Create associated state covariance
        final StateCovariance covariance =
                new StateCovariance(covarianceMatrix, state.getDate(), state.getFrame(), covOrbitType, covAngleType);

        // Return the state covariance in same frame/lof as initial covariance
        if (covInit.getLOF() == null) {
            return covariance;
        }
        else {
            return covariance.changeCovarianceFrame(state.getOrbit(), covInit.getLOF());
        }

    }

    /**
     * Get the state covariance expressed in a given frame.
     * <p>
     * The output covariance matrix is expressed in the same orbit type as {@link #getCovarianceOrbitType()}.
     *
     * @param state spacecraft state to which the covariance matrix should correspond
     * @param frame output frame for which the output covariance matrix must be expressed (must be inertial)
     * @return the state covariance expressed in <code>frame</code>
     * @see #getStateCovariance(SpacecraftState)
     * @see #getStateCovariance(SpacecraftState, OrbitType, PositionAngleType)
     */
    public StateCovariance getStateCovariance(final SpacecraftState state, final Frame frame) {
        // Return the converted covariance
        return getStateCovariance(state).changeCovarianceFrame(state.getOrbit(), frame);
    }

    /**
     * Get the state covariance expressed in a given orbit type.
     *
     * @param state spacecraft state to which the covariance matrix should correspond
     * @param orbitType output orbit type
     * @param angleType output position angle (not used if orbitType equals {@code CARTESIAN})
     * @return the state covariance in <code>orbitType</code> and <code>angleType</code>
     * @see #getStateCovariance(SpacecraftState)
     * @see #getStateCovariance(SpacecraftState, Frame)
     */
    public StateCovariance getStateCovariance(final SpacecraftState state, final OrbitType orbitType,
                                              final PositionAngleType angleType) {
        // Return the converted covariance
        return getStateCovariance(state).changeCovarianceType(state.getOrbit(), orbitType, angleType);
    }

    /**
     * Set the covariance data into an array.
     *
     * @param covariance covariance matrix
     * @return an array containing the covariance data
     */
    private double[] toArray(final RealMatrix covariance) {
        final double[] array = new double[STATE_DIMENSION * STATE_DIMENSION];
        int            index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                array[index++] = covariance.getEntry(i, j);
            }
        }
        return array;
    }

    /**
     * Convert an array to a matrix (6x6 dimension).
     *
     * @param array input array
     * @return the corresponding matrix
     */
    private RealMatrix toRealMatrix(final double[] array) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        int              index  = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                matrix.setEntry(i, j, array[index++]);
            }
        }
        return matrix;

    }

}
