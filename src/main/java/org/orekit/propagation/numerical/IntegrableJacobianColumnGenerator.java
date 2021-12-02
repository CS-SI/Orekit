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
package org.orekit.propagation.numerical;

import org.orekit.propagation.BaseJacobianColumnGenerator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;

/** Generator for one column of a Jacobian matrix.
 * @author Luc Maisonobe
 * @since 11.1
 */
class IntegrableJacobianColumnGenerator
    extends BaseJacobianColumnGenerator
    implements AdditionalDerivativesProvider, StateTransitionMatrixGenerator.PartialsObserver {

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Last value computed for the partial derivatives. */
    private final double[] pDot;

    /** Simple constructor.
     * <p>
     * The generator for State Transition Matrix <em>must</em> be registered as
     * an integrable generator to the same propagator as the instance, as it
     * must be scheduled to update the state before the instance
     * </p>
     * @param stmGenerator generator for State Transition Matrix
     * @param columnName name of the parameter corresponding to the column
     */
    IntegrableJacobianColumnGenerator(final StateTransitionMatrixGenerator stmGenerator, final String columnName) {
        super(columnName);
        this.stmName    = stmGenerator.getName();
        this.pDot       = new double[getDimension()];
        stmGenerator.addObserver(columnName, this);
    }

    /** {@inheritDoc}
     * <p>
     * The column derivative can be computed only if the State Transition Matrix derivatives
     * are available, as it implies the STM generator has already been run.
     * </p>
     */
    @Override
    public boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalStateDerivative(stmName);
    }

    /** {@inheritDoc} */
    @Override
    public void partialsComputed(final SpacecraftState state, final double[] factor, final double[] accelerationPartials) {
        // retrieve current Jacobian column
        final double[] p = state.getAdditionalState(getName());

        // compute time derivative of the Jacobian column
        StateTransitionMatrixGenerator.multiplyMatrix(factor, p, pDot, 1);
        pDot[3] += accelerationPartials[0];
        pDot[4] += accelerationPartials[1];
        pDot[5] += accelerationPartials[2];
    }

    /** {@inheritDoc} */
    @Override
    public double[] derivatives(final SpacecraftState s) {
        return pDot;
    }

}

