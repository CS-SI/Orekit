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
package org.orekit.propagation.conversion;

import org.hipparchus.ode.AbstractIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.utils.AbsolutePVCoordinates;

/**
 * Abstract class for integrator builder using variable step size.
 *
 * @author Romain Serra
 * @since 12.2
 */
public abstract class AbstractVariableStepIntegratorBuilder implements ODEIntegratorBuilder {

    /** Minimum step size (s). */
    private final double minStep;

    /** Maximum step size (s). */
    private final double maxStep;

    /** Integration tolerance provider. */
    private final ToleranceProvider toleranceProvider;

    /**
     * Constructor.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param toleranceProvider integration tolerance provider
     * @since 13.0
     */
    protected AbstractVariableStepIntegratorBuilder(final double minStep, final double maxStep,
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
     * Computes tolerances.
     * @param orbit initial orbit
     * @param orbitType orbit type for integration
     * @return integrator tolerances
     */
    protected double[][] getTolerances(final Orbit orbit, final OrbitType orbitType) {
        return toleranceProvider.getTolerances(orbit, orbitType, PositionAngleType.MEAN);
    }

    /**
     * Computes tolerances.
     * @param absolutePVCoordinates position-velocity vector
     * @return integrator tolerances
     * @since 13.0
     */
    protected double[][] getTolerances(final AbsolutePVCoordinates absolutePVCoordinates) {
        return toleranceProvider.getTolerances(absolutePVCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractIntegrator buildIntegrator(final Orbit orbit, final OrbitType orbitType,
                                              final PositionAngleType angleType) {
        return buildIntegrator(getTolerances(orbit, orbitType));
    }

    /** {@inheritDoc} */
    @Override
    public AbstractIntegrator buildIntegrator(final AbsolutePVCoordinates absolutePVCoordinates) {
        return buildIntegrator(getTolerances(absolutePVCoordinates));
    }

    /**
     * Builds an integrator from input absolute and relative tolerances.
     * @param tolerances tolerance array
     * @return integrator
     * @since 13.0
     */
    protected abstract AbstractIntegrator buildIntegrator(double[][] tolerances);

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
