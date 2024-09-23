/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.conversion.DormandPrince54FieldIntegratorBuilder;

/**
 * Integration settings using the Dormand-Prince 5(4) scheme.
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator
 */
public class DormandPrince54IntegrationSettings implements ShootingIntegrationSettings {

    /** Minimum step-size for integrator builders. */
    private final double minStep;

    /** Maximum step-size for integrator builders. */
    private final double maxStep;

    /** Expected position error for integrator builders. */
    private final double dP;

    /** Expected velocity error for integrator builders. */
    private final double dV;

    /**
     * Constructor.
     * @param minStep minimum step-size for integrator
     * @param maxStep maximum step-size for integrator
     * @param dP expected position error for integrator
     * @param dV expected velocity error for integrator
     */
    public DormandPrince54IntegrationSettings(final double minStep, final double maxStep,
                                              final double dP, final double dV) {
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.dP = dP;
        this.dV = dV;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> DormandPrince54FieldIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
        return new DormandPrince54FieldIntegratorBuilder<>(minStep, maxStep, dP, dV);
    }
}
