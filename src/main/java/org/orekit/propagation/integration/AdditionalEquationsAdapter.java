/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation.integration;

import java.util.function.Supplier;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


/** Temporary adapter from {@link AdditionalEquations} to {@link AdditionalDerivativesProvider}.
 * @since 11.1
 * @deprecated must be removed in 12.0 when {@link AdditionalEquations} is removed
 */
@Deprecated
public class AdditionalEquationsAdapter implements AdditionalDerivativesProvider {

    /** Wrapped equations. */
    private final AdditionalEquations equations;

    /** Supplier for reference state. */
    private final Supplier<SpacecraftState> stateSupplier;

    /** Dimension. */
    private int dimension;

    /** Simple constructor.
     * @param equations wrapped equations
     * @param stateSupplier supplier for reference state
     */
    public AdditionalEquationsAdapter(final AdditionalEquations equations, final Supplier<SpacecraftState> stateSupplier) {
        this.equations     = equations;
        this.stateSupplier = stateSupplier;
        this.dimension     = -1;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return equations.getName();
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        if (dimension < 0) {
            // retrieve the dimension the first time we need it
            dimension = stateSupplier.get().getAdditionalState(getName()).length;
        }
        return dimension;
    }

    /** {@inheritDoc} */
    @Override
    public boolean yield(final SpacecraftState state) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        equations.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public double[] derivatives(final SpacecraftState state) {
        return combinedDerivatives(state).getAdditionalDerivatives();
    }

    /** {@inheritDoc} */
    @Override
    public CombinedDerivatives combinedDerivatives(final SpacecraftState state) {
        final double[] pDot    = new double[getDimension()];
        final double[] mainDot = equations.computeDerivatives(state, pDot);
        return new CombinedDerivatives(pDot, mainDot);
    }

}
