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
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.utils.FieldAbsolutePVCoordinates;

/**
 * Abstract class for integrator builder using variable step size.
 *
 * @param <T> type of the field elements
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractVariableStepFieldIntegratorBuilder<T extends CalculusFieldElement<T>, W extends AdaptiveStepsizeFieldIntegrator<T>>
        extends FieldAbstractIntegratorBuilder<T, W> {

    /** Minimum step size (s). */
    private final double minStep;

    /** Maximum step size (s). */
    private final double maxStep;

    /** Integration tolerance provider. */
    private final ToleranceProvider toleranceProvider;

    /**
     * Constructor with expected velocity error.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param toleranceProvider integration tolerance provider
     * @since 13.0
     */
    protected AbstractVariableStepFieldIntegratorBuilder(final double minStep, final double maxStep,
                                                         final ToleranceProvider toleranceProvider) {
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.toleranceProvider = toleranceProvider;
    }

    /**
     * Getter for the maximum step.
     * @return max stepsize
     * @since 13.0
     */
    public double getMaxStep() {
        return maxStep;
    }

    /**
     * Getter for the minimum step.
     * @return min stepsize
     * @since 13.0
     */
    public double getMinStep() {
        return minStep;
    }

    /**
     * Getter for the integration tolerance provider.
     * @return tolerance provider
     * @since 13.0
     */
    public ToleranceProvider getToleranceProvider() {
        return toleranceProvider;
    }

    /**
     * Computes tolerances.
     * @param orbit initial orbit
     * @param orbitType orbit type to use
     * @param angleType position angle type to use
     * @return integrator tolerances
     * @since 13.0
     */
    protected double[][] getTolerances(final Orbit orbit, final OrbitType orbitType, final PositionAngleType angleType) {
        return toleranceProvider.getTolerances(orbit, orbitType, angleType);
    }

    /**
     * Computes tolerances.
     * @param fieldAbsolutePVCoordinates position-velocity vector
     * @return integrator tolerances
     * @since 13.0
     */
    protected double[][] getTolerances(final FieldAbsolutePVCoordinates<T> fieldAbsolutePVCoordinates) {
        return toleranceProvider.getTolerances(fieldAbsolutePVCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public W buildIntegrator(final Field<T> field, final Orbit orbit,
                             final OrbitType orbitType, final PositionAngleType angleType) {
        return buildIntegrator(field, getTolerances(orbit, orbitType, angleType));
    }

    /** {@inheritDoc} */
    @Override
    public W buildIntegrator(final FieldAbsolutePVCoordinates<T> fieldAbsolutePVCoordinates) {
        return buildIntegrator(fieldAbsolutePVCoordinates.getDate().getField(), getTolerances(fieldAbsolutePVCoordinates));
    }

    /**
     * Build integrator from absolute and relative tolerances.
     * @param field field
     * @param tolerances array of tolerances
     * @return integrator
     * @since 13.0
     */
    protected abstract W buildIntegrator(Field<T> field, double[][] tolerances);

    /**
     * Get a default tolerance provider.
     * @param dP expected position error (m)
     * @return tolerance provider
     * @since 13.0
     */
    protected static ToleranceProvider getDefaultToleranceProvider(final double dP) {
        return ToleranceProvider.getDefaultToleranceProvider(dP);
    }
}
