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
package org.orekit.propagation.analytical;

import java.util.Arrays;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/**
 * Base class harvester between two-dimensional Jacobian
 * matrices and analytical orbit propagator.
 * @author Thomas Paulet
 * @author Bryan Cazabonne
 * @since 11.1
 */
public abstract class AbstractAnalyticalMatricesHarvester extends AbstractMatricesHarvester implements AdditionalStateProvider {

    /** Columns names for parameters. */
    private List<String> columnsNames;

    /** Epoch of the last computed state transition matrix. */
    private AbsoluteDate epoch;

    /** Analytical derivatives that apply to State Transition Matrix. */
    private final double[][] analyticalDerivativesStm;

    /** Analytical derivatives that apply to Jacobians columns. */
    private final DoubleArrayDictionary analyticalDerivativesJacobianColumns;

    /** Propagator bound to this harvester. */
    private final AbstractAnalyticalPropagator propagator;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * @param propagator propagator bound to this harvester
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    protected AbstractAnalyticalMatricesHarvester(final AbstractAnalyticalPropagator propagator, final String stmName,
                                                  final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        super(stmName, initialStm, initialJacobianColumns);
        this.propagator                           = propagator;
        this.epoch                                = propagator.getInitialState().getDate();
        this.columnsNames                         = null;
        this.analyticalDerivativesStm             = getInitialStateTransitionMatrix().getData();
        this.analyticalDerivativesJacobianColumns = new DoubleArrayDictionary();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getJacobiansColumnsNames() {
        return columnsNames == null ? propagator.getJacobiansColumnsNames() : columnsNames;
    }

    /** {@inheritDoc} */
    @Override
    public void freezeColumnsNames() {
        columnsNames = getJacobiansColumnsNames();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getStmName();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {
        // Update the partial derivatives if needed
        updateDerivativesIfNeeded(state);
        // Return the state transition matrix in an array
        return toArray(analyticalDerivativesStm);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState state) {
        // Check if additional state is defined
        if (!state.hasAdditionalState(getName())) {
            return null;
        }
        // Return the state transition matrix
        return toRealMatrix(state.getAdditionalState(getName()));
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState state) {
        // Update the partial derivatives if needed
        updateDerivativesIfNeeded(state);

        // Estimated parameters
        final List<String> names = getJacobiansColumnsNames();
        if (names == null || names.isEmpty()) {
            return null;
        }

        // Initialize Jacobian
        final RealMatrix dYdP = MatrixUtils.createRealMatrix(STATE_DIMENSION, names.size());

        // Add the derivatives
        for (int j = 0; j < names.size(); ++j) {
            final double[] column = analyticalDerivativesJacobianColumns.get(names.get(j));
            if (column != null) {
                for (int i = 0; i < STATE_DIMENSION; i++) {
                    dYdP.addToEntry(i, j, column[i]);
                }
            }
        }

        // Return
        return dYdP;
    }

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {

        // reset derivatives to zero
        for (final double[] row : analyticalDerivativesStm) {
            Arrays.fill(row, 0.0);
        }
        analyticalDerivativesJacobianColumns.clear();

        final AbstractAnalyticalGradientConverter converter           = getGradientConverter();
        final FieldSpacecraftState<Gradient> gState                   = converter.getState();
        final Gradient[] gParameters                                  = converter.getParameters(gState, converter);
        final FieldAbstractAnalyticalPropagator<Gradient> gPropagator = converter.getPropagator(gState, gParameters);

        // Compute Jacobian
        final AbsoluteDate target               = reference.getDate();
        final FieldAbsoluteDate<Gradient> start = gPropagator.getInitialState().getDate();
        final double dt                         = target.durationFrom(start.toAbsoluteDate());
        final FieldOrbit<Gradient> gOrbit       = gPropagator.propagateOrbit(start.shiftedBy(dt), gParameters);
        final FieldPVCoordinates<Gradient> gPv  = gOrbit.getPVCoordinates();

        final double[] derivativesX   = gPv.getPosition().getX().getGradient();
        final double[] derivativesY   = gPv.getPosition().getY().getGradient();
        final double[] derivativesZ   = gPv.getPosition().getZ().getGradient();
        final double[] derivativesVx  = gPv.getVelocity().getX().getGradient();
        final double[] derivativesVy  = gPv.getVelocity().getY().getGradient();
        final double[] derivativesVz  = gPv.getVelocity().getZ().getGradient();

        // Update Jacobian with respect to state
        addToRow(derivativesX,  0);
        addToRow(derivativesY,  1);
        addToRow(derivativesZ,  2);
        addToRow(derivativesVx, 3);
        addToRow(derivativesVy, 4);
        addToRow(derivativesVz, 5);

        // Partial derivatives of the state with respect to propagation parameters
        int paramsIndex = converter.getFreeStateParameters();
        for (ParameterDriver driver : converter.getParametersDrivers()) {
            if (driver.isSelected()) {

                final TimeSpanMap<String> driverNameSpanMap = driver.getNamesSpanMap();
                // for each span (for each estimated value) corresponding name is added
                for (Span<String> span = driverNameSpanMap.getFirstSpan(); span != null; span = span.next()) {
                    // get the partials derivatives for this driver
                    DoubleArrayDictionary.Entry entry = analyticalDerivativesJacobianColumns.getEntry(span.getData());
                    if (entry == null) {
                        // create an entry filled with zeroes
                        analyticalDerivativesJacobianColumns.put(span.getData(), new double[STATE_DIMENSION]);
                        entry = analyticalDerivativesJacobianColumns.getEntry(span.getData());
                    }

                    // add the contribution of the current force model
                    entry.increment(new double[] {
                        derivativesX[paramsIndex], derivativesY[paramsIndex], derivativesZ[paramsIndex],
                        derivativesVx[paramsIndex], derivativesVy[paramsIndex], derivativesVz[paramsIndex]
                    });
                    ++paramsIndex;
                }
            }
        }

        // Update the epoch of the last computed partial derivatives
        epoch = target;

    }

    /** Update the partial derivatives (if needed).
     * @param state current spacecraft state
     */
    private void updateDerivativesIfNeeded(final SpacecraftState state) {
        if (!state.getDate().isEqualTo(epoch)) {
            setReferenceState(state);
        }
    }

    /** Fill State Transition Matrix rows.
     * @param derivatives derivatives of a component
     * @param index component index
     */
    private void addToRow(final double[] derivatives, final int index) {
        for (int i = 0; i < 6; i++) {
            analyticalDerivativesStm[index][i] += derivatives[i];
        }
    }

    /** Convert an array to a matrix (6x6 dimension).
     * @param array input array
     * @return the corresponding matrix
     */
    private RealMatrix toRealMatrix(final double[] array) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                matrix.setEntry(i, j, array[index++]);
            }
        }
        return matrix;
    }

    /** Set the STM data into an array.
     * @param matrix STM matrix
     * @return an array containing the STM data
     */
    private double[] toArray(final double[][] matrix) {
        final double[] array = new double[STATE_DIMENSION * STATE_DIMENSION];
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            final double[] row = matrix[i];
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                array[index++] = row[j];
            }
        }
        return array;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        // Set to CARTESIAN because analytical gradient converter uses cartesian representation
        return OrbitType.CARTESIAN;
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        // Irrelevant: set a default value
        return PositionAngleType.MEAN;
    }

    /**
     * Get the gradient converter related to the analytical orbit propagator.
     * @return the gradient converter
     */
    public abstract AbstractAnalyticalGradientConverter getGradientConverter();

}
