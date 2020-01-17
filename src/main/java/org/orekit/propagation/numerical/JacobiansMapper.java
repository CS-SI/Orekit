/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see org.orekit.propagation.numerical.PartialDerivativesEquations
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class JacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6.
     * @since 9.0
     */
    public static final int STATE_DIMENSION = 6;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Name. */
    private String name;

    /** Orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param orbitType orbit type
     * @param angleType position angle type
     */
    JacobiansMapper(final String name, final ParameterDriversList parameters,
                    final OrbitType orbitType, final PositionAngle angleType) {
        super(name, parameters);
        this.orbitType  = orbitType;
        this.angleType  = angleType;
        this.parameters = parameters;
        this.name = name;
    }

    /** {@inheritDoc} */
    protected double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];

        // make sure the state is in the desired orbit type
        final Orbit orbit = orbitType.convertType(state.getOrbit());

        // compute the Jacobian, taking the position angle type into account
        orbit.getJacobianWrtCartesian(angleType, dYdC);

        return dYdC;

    }

    /** {@inheritDoc}
     * <p>
     * This method converts the Jacobians to Cartesian parameters and put the converted data
     * in the one-dimensional {@code p} array.
     * </p>
     */
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
                             final double[][] dY1dP, final double[] p) {

        // set up a converter
        final RealMatrix dY1dC1 = new Array2DRowRealMatrix(getConversionJacobian(state), false);
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

        if (parameters.getNbParams() != 0) {
            // convert the provided state Jacobian
            final RealMatrix dC1dP = solver.solve(new Array2DRowRealMatrix(dY1dP, false));

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                for (int j = 0; j < parameters.getNbParams(); ++j) {
                    p[index++] = dC1dP.getEntry(i, j);
                }
            }
        }

    }

    /** {@inheritDoc} */
    public void getStateJacobian(final SpacecraftState state,  final double[][] dYdY0) {

        // get the conversion Jacobian
        final double[][] dYdC = getConversionJacobian(state);

        // extract the additional state
        final double[] p = state.getAdditionalState(name);

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

    }

    /** {@inheritDoc} */
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP) {

        if (parameters.getNbParams() != 0) {

            // get the conversion Jacobian
            final double[][] dYdC = getConversionJacobian(state);

            // extract the additional state
            final double[] p = state.getAdditionalState(name);

            // compute dYdP = dYdC * dCdP, without allocating new arrays
            for (int i = 0; i < STATE_DIMENSION; i++) {
                final double[] rowC = dYdC[i];
                final double[] rowD = dYdP[i];
                for (int j = 0; j < parameters.getNbParams(); ++j) {
                    double sum = 0;
                    int pIndex = j + STATE_DIMENSION * STATE_DIMENSION;
                    for (int k = 0; k < STATE_DIMENSION; ++k) {
                        sum += rowC[k] * p[pIndex];
                        pIndex += parameters.getNbParams();
                    }
                    rowD[j] = sum;
                }
            }

        }

    }

    /** {@inheritDoc} */
    @Override
    public int getAdditionalStateDimension() {
        return STATE_DIMENSION * (STATE_DIMENSION + parameters.getNbParams());
    }

}
