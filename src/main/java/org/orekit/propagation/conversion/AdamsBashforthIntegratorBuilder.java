/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.numerical.NumericalPropagator;

/** Builder for AdamsBashforthIntegrator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class AdamsBashforthIntegratorBuilder implements ODEIntegratorBuilder {

    /** Number of steps. */
    private final int nSteps;

    /** Minimum step size (s). */
    private final double minStep;

    /** Maximum step size (s). */
    private final double maxStep;

    /** Minimum step size (s). */
    private final double dP;

    /** Build a new instance.
     * @param nSteps number of steps
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     * @see AdamsBashforthIntegrator
     * @see NumericalPropagator#tolerances(double, Orbit, OrbitType)
     */
    public AdamsBashforthIntegratorBuilder(final int nSteps, final double minStep,
                                           final double maxStep, final double dP) {
        this.nSteps  = nSteps;
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.dP      = dP;
    }

    /** {@inheritDoc} */
    public AbstractIntegrator buildIntegrator(final Orbit orbit, final OrbitType orbitType)
        throws OrekitException {
        final double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        return new AdamsBashforthIntegrator(nSteps, minStep, maxStep, tol[0], tol[1]);
    }

}
