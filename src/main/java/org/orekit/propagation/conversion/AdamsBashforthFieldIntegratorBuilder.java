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
import org.hipparchus.ode.nonstiff.AdamsBashforthFieldIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Builder for AdamsBashforthFieldIntegrator.
 *
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 * @since 12.0
 * @param <T> type of the field elements
 */
public class AdamsBashforthFieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractLimitedVariableStepFieldIntegratorBuilder<T> {

    /**
     * Build a new instance.
     *
     * @param nSteps number of steps
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     *
     * @see AdamsBashforthFieldIntegrator
     * @see NumericalPropagator#tolerances(double, Orbit, OrbitType)
     */
    public AdamsBashforthFieldIntegratorBuilder(final int nSteps, final double minStep,
                                                final double maxStep, final double dP) {
        super(nSteps, minStep, maxStep, dP);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractFieldIntegrator<T> buildIntegrator(final Field<T> field, final Orbit orbit, final OrbitType orbitType) {
        final double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        return new AdamsBashforthFieldIntegrator<>(field, nSteps, minStep, maxStep, tol[0], tol[1]);
    }
}
