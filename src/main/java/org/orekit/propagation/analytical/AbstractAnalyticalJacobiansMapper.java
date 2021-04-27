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
package org.orekit.propagation.analytical;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Abstract class to get a Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays} for Analytical propagator.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Nicolas Fialton
 */
public abstract class AbstractAnalyticalJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6. */
    private final int STATE_DIMENSION;

    /** Placeholder for the derivatives of state. */
    private double[] stateTransition;

    protected AbstractAnalyticalJacobiansMapper(final String name,
                                        final ParameterDriversList parameters,
                                        final int STATE_DIMENSION,
                                        final double[] stateTransition) {
        super(name, parameters);
        this.STATE_DIMENSION = STATE_DIMENSION;
        this.stateTransition = stateTransition;
    }

    /** {@inheritDoc} */
    @Override
    public void setInitialJacobians(final SpacecraftState state,
                                    final double[][] dY1dY0,
                                    final double[][] dY1dP,
                                    final double[] p) {

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < getSTATE_DIMENSION(); ++i) {
            for (int j = 0; j < getSTATE_DIMENSION(); ++j) {
                p[index++] = (i == j) ? 1.0 : 0.0;
            }
        }

        // No propagator parameters therefore there is no dY1dP

    }

    /** {@inheritDoc} */
    @Override
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0) {

        for (int i = 0; i < getSTATE_DIMENSION(); i++) {
            final double[] row = dYdY0[i];
            for (int j = 0; j < getSTATE_DIMENSION(); j++) {
                row[j] = getStateTransition()[i * getSTATE_DIMENSION() + j];
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP) {

     // No propagator parameters therefore there is no dY1dP

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for e, 2 for i, 3 for RAAN, 4 for PA, 5 for TrueAnomaly)
     * @param grad Jacobian of mean elements rate with respect to mean elements
     */
    public void addToRow(final double[] derivatives,
                          final int index,
                          final double[][] grad) {

        for (int i = 0; i < getSTATE_DIMENSION(); i++) {
            grad[index][i] += derivatives[i];
        }
    }

    public double[] getStateTransition() {
        return stateTransition;
    }

    public void setStateTransition(final double[] stateTransition) {
        this.stateTransition = stateTransition;
    }

    public int getSTATE_DIMENSION() {
        return STATE_DIMENSION;
    }

}
