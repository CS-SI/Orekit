/* Copyright 2002-2017 CS Systèmes d'Information
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
import org.hipparchus.ode.nonstiff.MidpointIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;

/** Builder for MidpointIntegrator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class MidpointIntegratorBuilder implements ODEIntegratorBuilder {

    /** Step size (s). */
    private final double step;

    /** Build a new instance.
     * @param step step size (s)
     * @see MidpointIntegrator
     */
    public MidpointIntegratorBuilder(final double step) {
        this.step = step;
    }

    /** {@inheritDoc} */
    public AbstractIntegrator buildIntegrator(final Orbit orbit, final OrbitType orbitType) {
        return new MidpointIntegrator(step);
    }

}
