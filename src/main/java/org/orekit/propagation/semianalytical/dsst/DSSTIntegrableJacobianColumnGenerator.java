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
package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;

/** Generator for one column of a Jacobian matrix.
 * <p>
 * This generator is based on variational equations, so
 * it implements {@link AdditionalDerivativesProvider} and
 * computes only the derivative of the Jacobian column, to
 * be integrated by the propagator alongside the primary state.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 */
class DSSTIntegrableJacobianColumnGenerator
    implements AdditionalDerivativesProvider, DSSTStateTransitionMatrixGenerator.DSSTPartialsObserver {

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Name of the parameter corresponding to the column. */
    private final String columnName;

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
    DSSTIntegrableJacobianColumnGenerator(final DSSTStateTransitionMatrixGenerator stmGenerator, final String columnName) {
        this.stmName    = stmGenerator.getName();
        this.columnName = columnName;
        this.pDot       = new double[getDimension()];
        stmGenerator.addObserver(columnName, this);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return columnName;
    }

    /** Get the dimension of the generated column.
     * @return dimension of the generated column
     */
    public int getDimension() {
        return 6;
    }

    /** {@inheritDoc}
     * <p>
     * The column derivative can be computed only if the State Transition Matrix derivatives
     * are available, as it implies the STM generator has already been run.
     * </p>
     */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !state.hasAdditionalStateDerivative(stmName);
    }

    /** {@inheritDoc} */
    @Override
    public void partialsComputed(final SpacecraftState state, final RealMatrix factor, final double[] meanElementsPartials) {
        // retrieve current Jacobian column
        final double[] p = state.getAdditionalState(getName());

        // compute time derivative of the Jacobian column
        System.arraycopy(factor.operate(p), 0, pDot, 0, pDot.length);
        for (int i = 0; i < pDot.length; ++i) {
            pDot[i] += meanElementsPartials[i];
        }

    }

    /** {@inheritDoc} */
    @Override
    public CombinedDerivatives combinedDerivatives(final SpacecraftState s) {
        return new CombinedDerivatives(pDot, null);
    }

}

