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
package org.orekit.propagation.integration;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** Adapter from {@link FieldAdditionalEquations} to {@link FieldIntegrableGeneratorTest}.
 * @param <T> the type of the field elements
 * @since 11.1
 * @deprecated this adapter is temporary and will be removed when {@link FieldAdditionalEquations} is removed
 */
public class FieldAdditionalEquationAdapter<T extends CalculusFieldElement<T>> implements FieldIntegrableGenerator<T> {

    /** Underlying equations. */
    private final FieldAdditionalEquations<T> equations;

    /** Dimension of the equations. */
    private int dimension;

    /** Simple constructor.
     * @param equations underlying equations
     */
    public FieldAdditionalEquationAdapter(final FieldAdditionalEquations<T> equations) {
        this.equations = equations;
    }

    /** Set the dimension.
     * @param dimension dimension of the equations
     */
    void setDimension(final int dimension) {
        this.dimension = dimension;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return equations.getName();
    }

    /** {@inheritDoc}
     * <p>
     * In order to compute the derivative of state {@link #getName()},
     * we must have the state itself, so we yield if it is not available.
     * </p>
     */
    @Override
    public  boolean yield(final FieldSpacecraftState<T> state) {
        return !state.hasAdditionalState(getName());
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        equations.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public T[] generate(final FieldSpacecraftState<T> state) {
        final T[] yDot = MathArrays.buildArray(state.getDate().getField(), dimension);
        equations.computeDerivatives(state, yDot);
        return yDot;
    }

}
