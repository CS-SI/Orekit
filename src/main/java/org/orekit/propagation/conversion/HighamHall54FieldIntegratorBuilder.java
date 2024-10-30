/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

import org.hipparchus.ode.nonstiff.HighamHall54FieldIntegrator;
import org.orekit.propagation.ToleranceProvider;

/**
 * Builder for HighamHall54Integrator.
 *
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 * @since 12.0
 * @param <T> type of the field elements
 */
public class HighamHall54FieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractVariableStepFieldIntegratorBuilder<T> {

    /**
     * Build a new instance using default integration tolerances.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     *
     * @see HighamHall54FieldIntegrator
     */
    public HighamHall54FieldIntegratorBuilder(final double minStep, final double maxStep, final double dP) {
        this(minStep, maxStep, getDefaultToleranceProvider(dP));
    }

    /**
     * Build a new instance.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param toleranceProvider integration tolerance provider
     *
     * @since 13.0
     * @see HighamHall54FieldIntegrator
     */
    public HighamHall54FieldIntegratorBuilder(final double minStep, final double maxStep,
                                              final ToleranceProvider toleranceProvider) {
        super(minStep, maxStep, toleranceProvider);
    }

    /** {@inheritDoc} */
    @Override
    protected HighamHall54FieldIntegrator<T> buildIntegrator(final Field<T> field, final double[][] tolerances) {
        return new HighamHall54FieldIntegrator<>(field, getMinStep(), getMaxStep(), tolerances[0], tolerances[1]);
    }

    /** {@inheritDoc} */
    @Override
    public HighamHall54IntegratorBuilder toODEIntegratorBuilder() {
        return new HighamHall54IntegratorBuilder(getMinStep(), getMaxStep(), getToleranceProvider());
    }
}
