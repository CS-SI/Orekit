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

import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.numerical.NumericalPropagator;

/** Builder for DormandPrince853Integrator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class DormandPrince853IntegratorBuilder extends AbstractVariableStepIntegratorBuilder {

    /** Build a new instance. Should only use this constructor with {@link Orbit}.
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     * @see DormandPrince853Integrator
     * @see NumericalPropagator#tolerances(double, Orbit, OrbitType)
     */
    public DormandPrince853IntegratorBuilder(final double minStep, final double maxStep, final double dP) {
        super(minStep, maxStep, dP);
    }

    /** Build a new instance.
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     * @param dV velocity error (m/s)
     *
     * @since 12.2
     * @see DormandPrince853Integrator
     * @see NumericalPropagator#tolerances(double, double, Orbit, OrbitType)
     */
    public DormandPrince853IntegratorBuilder(final double minStep, final double maxStep, final double dP,
                                             final double dV) {
        super(minStep, maxStep, dP, dV);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractIntegrator buildIntegrator(final Orbit orbit, final OrbitType orbitType) {
        final double[][] tol = getTolerances(orbit, orbitType);
        return new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
    }

}
