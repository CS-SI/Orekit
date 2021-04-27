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
import org.orekit.propagation.integration.AdditionalEquations;

/** Abstract class used to Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state for Analytical propagator.
 * <p>
 * This set of equations are automatically added to an {@link AbstractAnalyticalPropagator analytical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * <p>
 * The partial derivatives with respect to initial state are dimension 6 (orbit only).
 * </p>
 * @author Nicolas Fialton
 */
public abstract class AbstractAnalyticalPartialDerivativesEquations {

    /** Name. */
    private final String name;

    /** Flag for Jacobian matrices initialization. */
    private boolean initialized;

    protected AbstractAnalyticalPartialDerivativesEquations(final String name,
                                                            final boolean initialized) {
        this.name = name;
        this.setInitialized(initialized);
    }

    /** Set the initial value of the Jacobian with respect to state.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(SpacecraftState,
     * double[][], double[][])} with dYdY0 set to the identity matrix and dYdP set
     * to a zero matrix.
     * </p>
     * @param s0 initial state
     * @return state with initial Jacobians added
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0) {

        final int stateDimension = 6;
        final double[][] dYdY0 = new double[stateDimension][stateDimension];
        for (int i = 0; i < stateDimension; ++i) {
            dYdY0[i][i] = 1.0;
        }
        return setInitialJacobians(s0, dYdY0);
    }

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * The returned state must be added to the propagator (it is not done
     * automatically, as the user may need to add more states to it).
     * </p>
     * @param s1 current state
     * @param dY1dY0 Jacobian of current state at time t₁ with respect
     * to state at some previous time t₀ (must be 6x6)
     * @return state with initial Jacobians added
     */
    protected abstract SpacecraftState setInitialJacobians(SpacecraftState s1,
                                                           double[][] dY1dY0);
    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @see #setInitialJacobians(SpacecraftState)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    protected abstract AbstractAnalyticalJacobiansMapper getMapper();

    /** Get the name of the additional state.
     * @return name of the additional state
     */
    public String getName() {
        return name;
    }

    public boolean getInitialized() {
        return initialized;
    }

    protected void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }


}
