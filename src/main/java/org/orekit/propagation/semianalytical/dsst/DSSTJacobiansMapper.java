/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst;

import org.orekit.errors.OrekitException;
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
 * @see org.orekit.propagation.semianalytical.DSSTPartialDerivativesEquations
 * @see org.orekit.propagation.semianalytical.DSSTPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class DSSTJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6.
     * @since 9.0
     */
    public static final int STATE_DIMENSION = 6;

    /** Name. */
    private String name;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     */
    DSSTJacobiansMapper(final String name, final ParameterDriversList parameters) {

        super(name, parameters);
        this.parameters = parameters;
        this.name = name;
    }

    /** {@inheritDoc} */
    protected double[][] getJacobianConversion(final SpacecraftState state) {

        final double[][] identity = new double[STATE_DIMENSION][STATE_DIMENSION];

        for (int i = 0; i < STATE_DIMENSION; ++i) {
            identity[i][i] = 1.0;
        }

        return identity;

    }

    /** {@inheritDoc} */
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
                                    final double[][] dY1dP, final double[] p) {

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                p[index++] = (i == j) ? 1.0 : 0.0;
            }
        }

        if (parameters.getNbParams() != 0) {

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                for (int j = 0; j < parameters.getNbParams(); ++j) {
                    p[index++] = dY1dP[i][j];
                }
            }
        }

    }

    /** {@inheritDoc} */
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0)
        throws OrekitException {

        // extract additional state
        final double[] p = state.getAdditionalState(name);

        for (int i = 0; i < STATE_DIMENSION; i++) {
            final double[] row = dYdY0[i];
            for (int j = 0; j < STATE_DIMENSION; j++) {
                row[j] = p[i * STATE_DIMENSION + j];
            }
        }

    }


    /** {@inheritDoc} */
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP)
        throws OrekitException {

        if (parameters.getNbParams() != 0) {

            // extract the additional state
            final double[] p = state.getAdditionalState(name);

            for (int i = 0; i < STATE_DIMENSION; i++) {
                final double[] row = dYdP[i];
                for (int j = 0; j < parameters.getNbParams(); j++) {
                    row[j] = p[STATE_DIMENSION * STATE_DIMENSION + (j + parameters.getNbParams() * i)];
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
