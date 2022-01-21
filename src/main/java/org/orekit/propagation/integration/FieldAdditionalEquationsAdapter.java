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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** Temporary adapter from {@link FieldAdditionalEquations} to {@link FieldAdditionalDerivativesProvider}.
 * @since 11.1
 * @deprecated must be removed in 12.0 when {@link AdditionalEquations} is removed
 */
@Deprecated
public class FieldAdditionalEquationsAdapter<T extends CalculusFieldElement<T>> implements FieldAdditionalDerivativesProvider<T> {

    /** Wrapped equations. */
    private final FieldAdditionalEquations<T> equations;

    /** Supplier for reference state. */
    private final Supplier<FieldSpacecraftState<T>> stateSupplier;

    /** Dimension. */
    private int dimension;

    /** Simple constructor.
     * @param equations wrapped equations
     * @param stateSupplier supplier for reference state
     */
    public FieldAdditionalEquationsAdapter(final FieldAdditionalEquations<T> equations, final Supplier<FieldSpacecraftState<T>> stateSupplier) {
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
    public boolean yield(final FieldSpacecraftState<T> state) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        equations.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public T[] derivatives(final FieldSpacecraftState<T> state) {
        final T[] pDot = MathArrays.buildArray(state.getDate().getField(), getDimension());
        equations.computeDerivatives(state, pDot);
        return pDot;
    }

}
