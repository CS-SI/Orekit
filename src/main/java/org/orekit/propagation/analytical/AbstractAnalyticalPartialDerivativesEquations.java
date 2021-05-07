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

    /** Simple constructor.
     *
     * @param name name of the partial derivatives equations
     */
    protected AbstractAnalyticalPartialDerivativesEquations(final String name) {
        this.name = name;
        this.initialized = false;
    }

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

    /**
     * Getter of the flag of Jacobian matrices initialization.
     * @return true if Jacobian matrices are initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Setter of the flag of Jacobian matrices initialization.
     * @param initialized true if Jacobian matrices are initialized
     */
    protected void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }
}
