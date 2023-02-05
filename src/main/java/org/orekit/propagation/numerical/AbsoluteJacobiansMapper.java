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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Vincent Mouraux
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 * @since 10.2
 */
public class AbsoluteJacobiansMapper implements MatricesHarvester {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = 6;

    /** Name. */
    private String name;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Simple constructor.
     * @param name name of the State Transition Matrix additional state
     * @param parameters selected parameters for Jacobian computation
     */
    public AbsoluteJacobiansMapper(final String name, final ParameterDriversList parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /** Compute the length of the one-dimensional additional state array needed.
     * @return length of the one-dimensional additional state array
     */
    public int getAdditionalStateDimension() {
        return STATE_DIMENSION * (STATE_DIMENSION + parameters.getNbValuesToEstimate());
    }

    /** Set the Jacobian with respect to state into a one-dimensional additional state array.
     * <p>
     * This method converts the Jacobians to Cartesian parameters and put the converted data
     * in the one-dimensional {@code p} array.
     * </p>
     * @param state spacecraft state
     * @param dY1dY0 Jacobian of current state at time t₁
     * with respect to state at some previous time t₀
     * @param dY1dP Jacobian of current state at time t₁
     * with respect to parameters (may be null if there are no parameters)
     * @param p placeholder where to put the one-dimensional additional state
     */
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
                                    final double[][] dY1dP, final double[] p) {

        // set up a converter
        final RealMatrix dY1dC1 = MatrixUtils.createRealIdentityMatrix(STATE_DIMENSION);
        final DecompositionSolver solver = new QRDecomposition(dY1dC1).getSolver();

        // convert the provided state Jacobian
        final RealMatrix dC1dY0 = solver.solve(new Array2DRowRealMatrix(dY1dY0, false));

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                p[index++] = dC1dY0.getEntry(i, j);
            }
        }

        if (parameters.getNbValuesToEstimate() != 0) {
            // convert the provided state Jacobian
            final RealMatrix dC1dP = solver.solve(new Array2DRowRealMatrix(dY1dP, false));

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                for (int j = 0; j < parameters.getNbValuesToEstimate(); ++j) {
                    p[index++] = dC1dP.getEntry(i, j);
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {
        // nothing by default
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        // initialize the state transition matrix
        final double[][] dYdY0 = new double[STATE_DIMENSION][STATE_DIMENSION];

        // get the conversion Jacobian
        final double[][] dYdC = getIdentity();

        // extract the additional state
        final double[] p = s.getAdditionalState(name);

        // compute dYdY0 = dYdC * dCdY0, without allocating new arrays
        for (int i = 0; i < STATE_DIMENSION; i++) {
            final double[] rowC = dYdC[i];
            final double[] rowD = dYdY0[i];
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                double sum = 0;
                int pIndex = j;
                for (int k = 0; k < STATE_DIMENSION; ++k) {
                    sum += rowC[k] * p[pIndex];
                    pIndex += STATE_DIMENSION;
                }
                rowD[j] = sum;
            }
        }

        return new Array2DRowRealMatrix(dYdY0, false);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState s) {
        if (parameters.getNbValuesToEstimate() == 0) {
            return null;
        } else {

            // initialize
            final double[][] dYdP = new double[STATE_DIMENSION][parameters.getNbValuesToEstimate()];

            // get the conversion Jacobian
            final double[][] dYdC = getIdentity();

            // extract the additional state
            final double[] p = s.getAdditionalState(name);

            // compute dYdP = dYdC * dCdP, without allocating new arrays
            for (int i = 0; i < STATE_DIMENSION; i++) {
                final double[] rowC = dYdC[i];
                final double[] rowD = dYdP[i];
                for (int j = 0; j < parameters.getNbValuesToEstimate(); ++j) {
                    double sum = 0;
                    int pIndex = j + STATE_DIMENSION * STATE_DIMENSION;
                    for (int k = 0; k < STATE_DIMENSION; ++k) {
                        sum += rowC[k] * p[pIndex];
                        pIndex += parameters.getNbValuesToEstimate();
                    }
                    rowD[j] = sum;
                }
            }

            return new Array2DRowRealMatrix(dYdP, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getJacobiansColumnsNames() {
        final List<String> driversNames = new ArrayList<>();
        for (DelegatingDriver driver : parameters.getDrivers()) {
            final TimeSpanMap<String> driverNameSpanMap = driver.getNamesSpanMap();
            Span<String> currentNameSpan = driverNameSpanMap.getFirstSpan();
            // Add the driver name if it has not been added yet and the number of estimated values for this param
            driversNames.add(currentNameSpan.getData());
            // jacobian columns names contains the name of each value to be estimated that is why
            // each span name for each driver is added
            for (int spanNumber = 1; spanNumber < driverNameSpanMap.getSpansNumber(); ++spanNumber) {
                currentNameSpan = driverNameSpanMap.getSpan(currentNameSpan.getEnd());
                driversNames.add(currentNameSpan.getData());
            }
        }
        return driversNames;
    }

    /** Get an identity matrix.
     * @return an identity matrix
     */
    private double[][] getIdentity() {
        final double[][] identity = new double[STATE_DIMENSION][STATE_DIMENSION];
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            identity[i][i] = 1.0;
        }
        return identity;
    }

}
