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
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.numerical.NumericalPropagator;

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

    /** Velocity error (m/s). */
    protected final double dV;
    // CHECKSTYLE: resume VisibilityModifier check

    /**
     * Constructor. Should only use this constructor with {@link Orbit}.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     */
    AbstractVariableStepFieldIntegratorBuilder(final double minStep, final double maxStep, final double dP) {
        this(minStep, maxStep, dP, Double.NaN);
    }

    /**
     * Constructor with expected velocity error.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     * @param dV velocity error (m/s)
     * @since 12.2
     */
    AbstractVariableStepFieldIntegratorBuilder(final double minStep, final double maxStep, final double dP,
                                               final double dV) {
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.dP      = dP;
        this.dV      = dV;
    }

    /**
     * Computes tolerances.
     * @param orbit initial orbit
     * @param orbitType orbit type for integration
     * @return integrator tolerances
     */
    protected double[][] getTolerances(final Orbit orbit, final OrbitType orbitType) {
        if (Double.isNaN(dV)) {
            return NumericalPropagator.tolerances(dP, orbit, orbitType);
        } else {
            return NumericalPropagator.tolerances(dP, dV, orbit, orbitType);
        }
    }
}
