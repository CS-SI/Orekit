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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.AbstractFieldIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;

/**
 * Abstract class for integrator builder using variable step size.
 *
 * @param <T> type of the field elements
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractVariableStepFieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractFieldIntegratorBuilder<T> {

    // CHECKSTYLE: stop VisibilityModifier check
    /** Minimum step size (s). */
    protected final double minStep;

    /** Maximum step size (s). */
    protected final double maxStep;

    /** Position error (m). */
    protected final double dP;
    // CHECKSTYLE: resume VisibilityModifier check

    /**
     * Constructor.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     */
    AbstractVariableStepFieldIntegratorBuilder(final double minStep, final double maxStep, final double dP) {
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.dP      = dP;
    }

    /** {@inheritDoc} */
    @Override
    public abstract AbstractFieldIntegrator<T> buildIntegrator(Field<T> field, Orbit orbit, OrbitType orbitType);
}
