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

    /** State dimension. */
    private final int stateDimension;

    /** Simple constructor.
     *
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param stateDimension the dimension of the state vector
     */
    protected AbstractAnalyticalJacobiansMapper(final String name,
                                                final ParameterDriversList parameters,
                                                final int stateDimension) {
        super(name, parameters);
        this.stateDimension = stateDimension;
    }

    /** {@inheritDoc} */
    @Override
    public void setInitialJacobians(final SpacecraftState state,
                                    final double[][] dY1dY0,
                                    final double[][] dY1dP,
                                    final double[] p) {

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < getStateDimension(); ++i) {
            for (int j = 0; j < getStateDimension(); ++j) {
                p[index++] = (i == j) ? 1.0 : 0.0;
            }
        }

        if (getParameters() != 0) {
            // map the converted parameters Jacobian to one dimensional array
            for (int i = 0; i < getStateDimension(); i++ ) {
                for (int j = 0; j < getParameters(); ++j) {
                    p[index++] = dY1dP[i][j];
                }
            }
        }

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for e, 2 for i, 3 for RAAN, 4 for PA, 5 for TrueAnomaly)
     * @param grad Jacobian of mean elements rate with respect to mean elements
     */
    protected void addToRow(final double[] derivatives,
                          final int index,
                          final double[][] grad) {
        for (int i = 0; i < getStateDimension(); i++) {
            grad[index][i] += derivatives[i];
        }
    }

    // Get the dimension of the state vector.//
    public int getStateDimension() {
        return stateDimension;
    }
}
