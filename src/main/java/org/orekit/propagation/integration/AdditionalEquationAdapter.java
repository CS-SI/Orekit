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

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Adapter from {@link AdditionalEquations} to {@link IntegrableGenerator}.
 * @since 11.1
 * @deprecated this adapter is temporary and will be removed when {@link AdditionalEquations} is removed
 */
public class AdditionalEquationAdapter implements IntegrableGenerator {

    /** Underlying equations. */
    private final AdditionalEquations equations;

    /** Dimension of the equations. */
    private int dimension;

    /** Simple constructor.
     * @param equations underlying equations
     */
    public AdditionalEquationAdapter(final AdditionalEquations equations) {
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
    public int getDimension() {
        return dimension;
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
    public  boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalState(getName());
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        equations.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public double[] generate(final SpacecraftState state) {
        final double[] yDot = new double[dimension];
        equations.computeDerivatives(state, yDot);
        return yDot;
    }

}
